package com.dsa.ui.tracer;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A tracer's {@code dsType()} must be backed by the payload it actually emits.
 *
 * <p>{@code CatalogTracerMetadataTest} already checks that the catalogue entry and the
 * tracer agree on a dsType. That is one half of the contract, and on its own it is
 * satisfied by two files agreeing with each other while both are wrong about the trace:
 * {@code celebrity-problem} declared {@code STACK}, was routed to {@code StackCanvas} by
 * both the catalogue and the registry, and never called {@code emit.stack(...)} on any
 * step. Every existing test passed. The animation was a permanently empty stack panel
 * beside narration describing an elimination the viewer could not see — the exact failure
 * mode {@code StackCanvas}'s own header describes ("the actual stack was computed but never
 * drawn"), inverted.
 *
 * <p>So this closes the other half: the dsType names a canvas, that canvas reads one
 * particular field off the step, and this asserts the tracer populates it. The mapping
 * below mirrors {@code frontend/src/canvas/registry.js}; a dsType whose canvas reads a
 * field that is genuinely optional is skipped rather than passed, so a skip in the report
 * is visible instead of looking like coverage.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DsTypePayloadContractTest {

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    /** What the canvas behind each dsType reads off a step, per canvas/registry.js. */
    private record Requirement(String field, Predicate<ExecutionStep> populated) {}

    private static final Map<DsType, Requirement> REQUIRED = new EnumMap<>(DsType.class);

    static {
        Requirement queueOrStack = new Requirement("queueOrStackState",
                s -> s.getQueueOrStackState() != null && !s.getQueueOrStackState().isEmpty());
        REQUIRED.put(DsType.STACK, queueOrStack);
        REQUIRED.put(DsType.QUEUE, queueOrStack);
        REQUIRED.put(DsType.MATRIX, new Requirement("gridState", s -> s.getGridState() != null));
        REQUIRED.put(DsType.LINKED_LIST, new Requirement("listState",
                s -> s.getListState() != null && !s.getListState().isEmpty()));
        REQUIRED.put(DsType.TREE, new Requirement("treeNodes",
                s -> s.getTreeNodes() != null && !s.getTreeNodes().isEmpty()));
        REQUIRED.put(DsType.GRAPH, new Requirement("graphNodes",
                s -> s.getGraphNodes() != null && !s.getGraphNodes().isEmpty()));
        REQUIRED.put(DsType.DP_TABLE, new Requirement("dpTable", s -> s.getDpTable() != null));
        REQUIRED.put(DsType.TRIE, new Requirement("trieState",
                s -> s.getTrieState() != null && !s.getTrieState().isEmpty()));
    }

    Stream<String> tracerIds() {
        return registry.tracedIds().stream().sorted();
    }

    @ParameterizedTest(name = "{0} emits the structure its dsType promises")
    @MethodSource("tracerIds")
    @DisplayName("The declared dsType is backed by the payload the trace actually carries")
    void declaredDsTypeIsBackedByEmittedPayload(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        Requirement requirement = REQUIRED.get(tracer.dsType());
        assumeTrue(requirement != null,
                id + " declares " + tracer.dsType() + ", whose canvas has no single required"
                        + " structure field to check");

        List<ExecutionStep> steps = runner.runDefaults(tracer).getSteps();
        boolean everPopulated = steps.stream().anyMatch(requirement.populated());

        assertTrue(everPopulated, id + " declares dsType " + tracer.dsType() + ", so the UI"
                + " routes it to the canvas that reads " + requirement.field() + " — but no"
                + " step of its trace populates that field, so the canvas renders its empty"
                + " state for the whole animation. Either emit the structure, or declare the"
                + " dsType of the structure it does emit (and update bulkDsType/the catalogue"
                + " entry to match).");
    }
}
