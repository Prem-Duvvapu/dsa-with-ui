package com.dsa.ui.tracer.wire;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.tracer.AlgorithmTracer;
import com.dsa.ui.tracer.ExecutionTrace;
import com.dsa.ui.tracer.TraceRunner;
import com.dsa.ui.tracer.TracerRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The delta encoding is a compression, so the only thing that really matters is that it
 * loses nothing. Every test here works by decoding and comparing against the original.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TraceEncoderTest {

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    private final ObjectMapper json = new ObjectMapper();

    Stream<String> tracerIds() {
        return registry.tracedIds().stream().sorted();
    }

    @ParameterizedTest(name = "{0} survives an encode/decode round trip")
    @MethodSource("tracerIds")
    @DisplayName("Decoding a delta stream reproduces the original steps exactly")
    void deltaRoundTripsToTheOriginalSteps(String id) throws Exception {
        List<ExecutionStep> original = trace(id).getSteps();

        List<ExecutionStep> decoded = decode(TraceEncoder.encode(original));

        assertEquals(original.size(), decoded.size(), id + " lost or gained steps");
        for (int i = 0; i < original.size(); i++) {
            assertEquals(json.writeValueAsString(original.get(i)),
                    json.writeValueAsString(decoded.get(i)),
                    id + " step " + (i + 1) + " did not survive the round trip");
        }
    }

    @ParameterizedTest(name = "{0} graph topology survives delta encoding")
    @ValueSource(strings = {"bfs-traversal", "dfs-traversal", "dijkstra-min-heap"})
    @DisplayName("Graph topology is carried once and reconstructed on every step")
    void graphTopologySurvivesDeltaEncoding(String id) throws Exception {
        List<ExecutionStep> original = trace(id).getSteps();
        List<DeltaStep> encoded = TraceEncoder.encode(original);
        List<ExecutionStep> decoded = decode(encoded);

        assertNotNull(encoded.get(0).getGraphNodes());
        assertNotNull(encoded.get(0).getGraphEdges());
        for (int i = 1; i < encoded.size(); i++) {
            if (!Boolean.TRUE.equals(encoded.get(i).getKeyframe())) {
                assertNull(encoded.get(i).getGraphNodes(),
                        id + " redundantly sent unchanged graph nodes on step " + (i + 1));
                assertNull(encoded.get(i).getGraphEdges(),
                        id + " redundantly sent unchanged graph edges on step " + (i + 1));
            }
        }
        for (int i = 0; i < decoded.size(); i++) {
            assertEquals(json.writeValueAsString(original.get(i).getGraphNodes()),
                    json.writeValueAsString(decoded.get(i).getGraphNodes()));
            assertEquals(json.writeValueAsString(original.get(i).getGraphEdges()),
                    json.writeValueAsString(decoded.get(i).getGraphEdges()));
        }
    }

    /**
     * The regression this encoding is most likely to introduce, and the reason "absent"
     * cannot simply mean "unchanged": bfs-traversal empties activeEdges on 6 of its 21
     * steps and the tree traversals empty their call stack on the last one. Carrying
     * forward there would leave the canvas highlighting edges the algorithm had left.
     */
    @ParameterizedTest(name = "{0} sends a field that empties, rather than omitting it")
    @MethodSource("tracerIds")
    @DisplayName("A field that changes to empty is transmitted explicitly")
    void aFieldThatEmptiesIsTransmitted(String id) {
        List<ExecutionStep> original = trace(id).getSteps();
        List<DeltaStep> encoded = TraceEncoder.encode(original);

        for (int i = 1; i < original.size(); i++) {
            List<String> before = original.get(i - 1).getActiveEdges();
            List<String> now = original.get(i).getActiveEdges();
            boolean emptied = before != null && !before.isEmpty() && now != null && now.isEmpty();
            if (emptied) {
                DeltaStep step = encoded.get(i);
                assertTrue(Boolean.TRUE.equals(step.getKeyframe()) || step.getActiveEdges() != null,
                        id + " step " + (i + 1) + " emptied activeEdges but the delta omits the"
                                + " field, so a client carrying state forward would keep showing"
                                + " edges the algorithm has already left");
            }
        }
    }

    @ParameterizedTest(name = "{0} keyframes stand alone")
    @MethodSource("tracerIds")
    @DisplayName("Every keyframe reconstructs without the steps before it")
    void keyframesStandAlone(String id) throws Exception {
        List<ExecutionStep> original = trace(id).getSteps();
        List<DeltaStep> encoded = TraceEncoder.encode(original);

        for (int i = 0; i < encoded.size(); i++) {
            if (!Boolean.TRUE.equals(encoded.get(i).getKeyframe())) {
                continue;
            }
            // Decode starting AT the keyframe, as a viewer seeking to it would.
            List<ExecutionStep> fromHere = decode(encoded.subList(i, encoded.size()));
            assertEquals(json.writeValueAsString(original.get(i)),
                    json.writeValueAsString(fromHere.get(0)),
                    id + " keyframe at step " + (i + 1) + " does not stand on its own, so"
                            + " scrubbing to it would render an incomplete frame");
        }
    }

    @ParameterizedTest(name = "{0} costs fewer bytes as a delta")
    @MethodSource("tracerIds")
    @DisplayName("The delta encoding is smaller than the snapshot encoding")
    void deltaIsSmallerThanFull(String id) throws Exception {
        ExecutionTrace trace = trace(id);
        int full = json.writeValueAsBytes(TraceResponse.full(trace)).length;
        int delta = json.writeValueAsBytes(TraceResponse.delta(trace)).length;

        assertTrue(delta < full, String.format(
                "%s encodes to %d bytes as a delta and %d as snapshots — the encoding is not"
                        + " earning its complexity", id, delta, full));
    }

    private ExecutionTrace trace(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        return runner.runDefaults(tracer);
    }

    /**
     * What a client does. Kept deliberately literal — this is the reference implementation
     * the frontend decoder has to match, so it should be readable as a specification.
     */
    private static List<ExecutionStep> decode(List<DeltaStep> deltas) {
        List<ExecutionStep> out = new ArrayList<>(deltas.size());
        ExecutionStep carried = null;

        for (DeltaStep delta : deltas) {
            ExecutionStep previous = Boolean.TRUE.equals(delta.getKeyframe()) ? null : carried;

            ExecutionStep step = new ExecutionStep();
            step.setStepNumber(delta.getStepNumber());
            step.setActiveLine(delta.getActiveLine());
            step.setDescription(delta.getDescription());
            step.setQueueOrStackState(pick(delta.getQueueOrStackState(),
                    previous == null ? null : previous.getQueueOrStackState()));
            step.setNodeStates(pick(delta.getNodeStates(),
                    previous == null ? null : previous.getNodeStates()));
            step.setActiveEdges(pick(delta.getActiveEdges(),
                    previous == null ? null : previous.getActiveEdges()));
            step.setVariables(pick(delta.getVariables(),
                    previous == null ? null : previous.getVariables()));
            step.setDsType(pick(delta.getDsType(),
                    previous == null ? null : previous.getDsType()));
            step.setGridState(pick(delta.getGridState(),
                    previous == null ? null : previous.getGridState()));
            step.setArrayState(pick(delta.getArrayState(),
                    previous == null ? null : previous.getArrayState()));
            step.setListState(pick(delta.getListState(),
                    previous == null ? null : previous.getListState()));
            step.setTreeNodes(pick(delta.getTreeNodes(),
                    previous == null ? null : previous.getTreeNodes()));
            step.setGraphNodes(pick(delta.getGraphNodes(),
                    previous == null ? null : previous.getGraphNodes()));
            step.setGraphEdges(pick(delta.getGraphEdges(),
                    previous == null ? null : previous.getGraphEdges()));

            out.add(step);
            carried = step;
        }
        return out;
    }

    /** Present in the delta wins; otherwise carry the previous value forward. */
    private static <T> T pick(T sent, T carried) {
        return sent != null ? sent : carried;
    }
}
