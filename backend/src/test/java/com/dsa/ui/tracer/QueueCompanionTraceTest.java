package com.dsa.ui.tracer;

import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROMPT-F-visual-fidelity.md, slice F1: bfs-traversal and dijkstra-min-heap narrate a
 * queue ("Seed the queue with vertex 0", "Pop the smallest entry in the queue") while
 * their Graph-hero canvas has nowhere to draw one. The frontend queue companion pane
 * (canvas/companions.js) reads queueOrStackState to fill that gap — this test is the
 * backend half: proving the field it reads is actually populated.
 *
 * Regression check: reverting the `.queue(...)` calls these two tracers add makes
 * {@code queueOrStackState} null on every step, per StepEmitter's default. This test was
 * run against that reverted state and failed with "no step carried a non-empty queue" —
 * see PROMPT-F-visual-fidelity.md's slice F1 for the working notes.
 */
@SpringBootTest
class QueueCompanionTraceTest {

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    @ParameterizedTest(name = "{0} carries a non-empty queue at least once")
    @ValueSource(strings = {"bfs-traversal", "dijkstra-min-heap"})
    @DisplayName("Graph-hero tracers narrating a queue actually populate queueOrStackState")
    void narratedQueueIsOnTheWire(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        List<ExecutionStep> steps = runner.runDefaults(tracer).getSteps();

        boolean anyNonEmpty = steps.stream()
                .map(ExecutionStep::getQueueOrStackState)
                .anyMatch(queue -> queue != null && !queue.isEmpty());

        assertTrue(anyNonEmpty, id + ": no step carried a non-empty queue, "
                + "so the companion pane would always render its empty state");
    }

    @ParameterizedTest(name = "{0} drains its queue back to empty once the algorithm finishes")
    @ValueSource(strings = {"bfs-traversal", "dijkstra-min-heap"})
    void queueIsEmptyOnceTheLastStepRuns(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        List<ExecutionStep> steps = runner.runDefaults(tracer).getSteps();
        ExecutionStep last = steps.get(steps.size() - 1);
        List<String> finalQueue = last.getQueueOrStackState();

        assertTrue(finalQueue == null || finalQueue.isEmpty(),
                id + ": the algorithm's own loop condition is \"queue non-empty\", "
                        + "so the final step must show it drained, but it still holds "
                        + finalQueue);
    }
}
