package com.dsa.ui.tracer;

import com.dsa.ui.algorithm.graph.BfsTraversal;
import com.dsa.ui.algorithm.graph.RottingOranges;
import com.dsa.ui.algorithm.heap.KthLargestElement;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.trace.ListTraceRecorder;
import com.dsa.ui.trace.TraceEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ListTraceRecorderStateSeparationTest {

    @Test
    void recursiveFramesBecomeCallStackRatherThanAlgorithmStackState() {
        ListTraceRecorder recorder = new ListTraceRecorder();
        recorder.record(new TraceEvent(
                "recurse", 7, "descend", Map.of(), "Tree", null,
                List.of("walk(1)", "walk(2)"), Map.of(), List.of()));

        ExecutionStep step = recorder.toExecutionSteps().get(0);

        assertEquals(List.of("walk(1)", "walk(2)"), step.getCallStack());
        assertNull(step.getQueueOrStackState());
    }

    @Test
    void breadthFirstQueueRemainsAlgorithmDataStructureState() {
        ListTraceRecorder recorder = new ListTraceRecorder();
        new BfsTraversal().solve(
                2,
                Map.of(0, List.of(1), 1, List.of()),
                recorder);

        ExecutionStep first = recorder.toExecutionSteps().get(0);
        assertEquals(List.of("0"), first.getQueueOrStackState());
        assertEquals(List.of(), first.getCallStack());
    }

    @Test
    void aQueueCanAccompanyANonQueuePrimaryVisualization() {
        ListTraceRecorder recorder = new ListTraceRecorder();
        new RottingOranges().solve(new int[][]{{2, 1}}, recorder);

        ExecutionStep first = recorder.toExecutionSteps().get(0);
        assertEquals(List.of("(0,0)"), first.getQueueOrStackState());
        assertEquals(List.of(), first.getCallStack());
    }

    @Test
    void priorityQueueRemainsAlgorithmDataStructureState() {
        ListTraceRecorder recorder = new ListTraceRecorder();
        new KthLargestElement().solve(new int[]{3, 1, 2}, 2, recorder);

        ExecutionStep afterFirstPush = recorder.toExecutionSteps().get(1);
        assertEquals(List.of("3"), afterFirstPush.getQueueOrStackState());
        assertEquals(List.of(), afterFirstPush.getCallStack());
    }
}
