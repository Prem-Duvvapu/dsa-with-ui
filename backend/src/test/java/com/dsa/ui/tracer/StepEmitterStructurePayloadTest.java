package com.dsa.ui.tracer;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.GraphEdge;
import com.dsa.ui.model.GraphNode;
import com.dsa.ui.model.TrieNodeModel;
import com.dsa.ui.tracer.wire.DeltaStep;
import com.dsa.ui.tracer.wire.TraceEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StepEmitterStructurePayloadTest {

    @Test
    void callFramesAndAlgorithmStackAreCommittedToSeparateFields() {
        StepEmitter emitter = emitter(DsType.STACK);

        emitter.push("solve(3)");
        emitter.at("step").stack(List.of(10, 20)).step();
        emitter.pop();
        emitter.at("step").queue(List.of("next", "last")).step();

        ExecutionStep first = emitter.collected().get(0);
        assertEquals(List.of("solve(3)"), first.getCallStack());
        assertEquals(List.of("10", "20"), first.getQueueOrStackState());

        ExecutionStep second = emitter.collected().get(1);
        assertEquals(List.of(), second.getCallStack());
        assertEquals(List.of("next", "last"), second.getQueueOrStackState());
    }

    @Test
    void trieHelperCommitsADeepIndependentSnapshotAndChargesForIt() {
        StepEmitter emitter = emitter(DsType.TRIE);
        Map<String, Integer> children = new LinkedHashMap<>();
        children.put("a", 1);
        TrieNodeModel root = new TrieNodeModel(
                0, "root", false, 100, 20, children, "known");

        emitter.at("step").trie(List.of(root)).step();

        root.setState("active");
        children.put("b", 2);
        emitter.at("step").trie(List.of(root)).step();

        TrieNodeModel first = emitter.collected().get(0).getTrieState().get(0);
        TrieNodeModel second = emitter.collected().get(1).getTrieState().get(0);
        assertEquals("known", first.getState(),
                "mutating a tracer's working node must not rewrite an earlier step");
        assertEquals(Map.of("a", 1), first.getChildren(),
                "the mutable child map must be copied along with the node");
        assertEquals("active", second.getState());
        assertEquals(Map.of("a", 1, "b", 2), second.getChildren());
        assertThrows(UnsupportedOperationException.class,
                () -> first.getChildren().put("c", 3),
                "a committed snapshot must not expose its copied child map for mutation");
        assertTrue(StepEmitter.estimateBytes(emitter.collected().get(0)) > 290,
                "the byte budget must charge trie nodes and their child map");
    }

    @Test
    void charsAndBitsPopulateLabelsWithoutBreakingTheOldArrayConstructor() {
        StepEmitter chars = emitter(DsType.STRING);
        chars.at("step").chars("Az", 1).step();

        List<ArrayElement> characters = chars.collected().get(0).getArrayState();
        assertEquals(List.of("A", "z"),
                characters.stream().map(ArrayElement::getLabel).toList());
        assertEquals(List.of((int) 'A', (int) 'z'),
                characters.stream().map(ArrayElement::getValue).toList());
        assertEquals("current", characters.get(1).getState());

        StepEmitter bits = emitter(DsType.BITS);
        bits.at("step").bits(5, 2).step();
        List<ArrayElement> bitState = bits.collected().get(0).getArrayState();
        assertEquals(32, bitState.size());
        assertEquals("31", bitState.get(0).getLabel());
        assertEquals("0", bitState.get(31).getLabel());
        ArrayElement activeBit = bitState.stream()
                .filter(element -> element.getIndex() == 2)
                .findFirst()
                .orElseThrow();
        assertEquals(1, activeBit.getValue());
        assertEquals("current", activeBit.getState());

        ArrayElement legacy = new ArrayElement(0, 7, "default");
        assertNull(legacy.getLabel());
        assertEquals(legacy, new ArrayElement(0, 7, "default", null));

        ExecutionStep legacyStep = new ExecutionStep(
                1, 1, "legacy", List.of("item"), Map.of(), List.of(), Map.of(),
                "Stack", null);
        assertEquals(List.of("item"), legacyStep.getQueueOrStackState());
        assertNull(legacyStep.getCallStack());
    }

    @Test
    void graphHelperCommitsADeepIndependentSnapshot() {
        StepEmitter emitter = emitter(DsType.GRAPH);
        List<GraphNode> nodes = new ArrayList<>(List.of(
                new GraphNode(0, "source", 10.5, 20.5, "known"),
                new GraphNode(1, "target", 30.5, 40.5, "probe")));
        List<GraphEdge> edges = new ArrayList<>(List.of(
                new GraphEdge(0, 1, 7, true, true)));

        emitter.at("step").graph(nodes, edges).step();

        nodes.get(0).setLabel("mutated");
        nodes.get(1).setState("mutated");
        edges.get(0).setWeight(99);
        edges.get(0).setHighlighted(false);
        nodes.add(new GraphNode(2, "late", 50, 60, "known"));
        edges.add(new GraphEdge(1, 2, 3, false, false));

        ExecutionStep first = emitter.collected().get(0);
        assertEquals(List.of("source", "target"),
                first.getGraphNodes().stream().map(GraphNode::getLabel).toList(),
                "mutating caller-owned graph nodes must not rewrite a committed step");
        assertEquals(List.of("known", "probe"),
                first.getGraphNodes().stream().map(GraphNode::getState).toList());
        assertEquals(7, first.getGraphEdges().get(0).getWeight());
        assertTrue(first.getGraphEdges().get(0).isHighlighted());
        assertEquals(2, first.getGraphNodes().size(),
                "mutating the source list must not grow a committed step");
        assertEquals(1, first.getGraphEdges().size());
        assertThrows(UnsupportedOperationException.class,
                () -> first.getGraphNodes().add(new GraphNode()),
                "a committed graph-node list must not expose structural mutation");
        assertThrows(UnsupportedOperationException.class,
                () -> first.getGraphEdges().add(new GraphEdge()),
                "a committed graph-edge list must not expose structural mutation");
    }

    @Test
    void equalRebuiltGraphTopologyIsCarriedRatherThanResent() {
        StepEmitter emitter = emitter(DsType.GRAPH);
        emitter.at("step").graph(
                List.of(
                        new GraphNode(0, "source", 10.5, 20.5, "known"),
                        new GraphNode(1, "target", 30.5, 40.5, "probe")),
                List.of(new GraphEdge(0, 1, 7, true, true)))
                .step();
        emitter.at("step").graph(
                List.of(
                        new GraphNode(0, "source", 10.5, 20.5, "known"),
                        new GraphNode(1, "target", 30.5, 40.5, "probe")),
                List.of(new GraphEdge(0, 1, 7, true, true)))
                .step();

        DeltaStep secondDelta = TraceEncoder.encode(emitter.collected()).get(1);
        assertNull(secondDelta.getGraphNodes(),
                "fresh graph nodes with equal content should be carried, not resent");
        assertNull(secondDelta.getGraphEdges(),
                "fresh graph edges with equal content should be carried, not resent");
    }

    @Test
    void byteEstimateCoversEscapedUtf8AcrossNewStructureFields() throws Exception {
        String hostile = "line\\path\n\"quote\" 雪 🙂 ".repeat(120);
        StepEmitter emitter = emitter(DsType.TRIE);
        emitter.push("frame:" + hostile);
        emitter.at("step")
                .arrayState(List.of(new ArrayElement(0, 1, "known", "🙂")))
                .queue(List.of("queue:" + hostile))
                .trie(List.of(new TrieNodeModel(
                        0,
                        "node:" + hostile,
                        false,
                        10,
                        20,
                        Map.of("edge:" + hostile, 1),
                        "state:" + hostile)))
                .step();

        ExecutionStep step = emitter.collected().get(0);
        long estimated = StepEmitter.estimateBytes(step);
        long actual = new ObjectMapper().writeValueAsBytes(step).length;

        assertTrue(estimated >= actual,
                "the byte ceiling must conservatively count escaped UTF-8 strings: estimated "
                        + estimated + ", actual " + actual);
        assertTrue(estimated <= actual * 2,
                "the focused estimate should remain calibrated enough to preserve useful traces: "
                        + "estimated " + estimated + ", actual " + actual);
    }

    private static StepEmitter emitter(DsType dsType) {
        return new StepEmitter(
                AnnotatedCode.parse("// @a step\nvisit();"),
                10,
                100_000,
                dsType);
    }
}
