package com.dsa.ui.tracer;

import com.dsa.ui.model.ExecutionStep;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Wrong answers found by running every tracer against an independent reference while writing
 * the problem statements. Each row is the exact input, the final-step variable that holds the
 * answer, and the CORRECT value - every one of them failed before its fix.
 *
 * <p>The contract tests could not see these: a trace that is well-formed, numbered, anchored
 * and input-sensitive can still compute the wrong answer on an edge of its input space.
 */
@SpringBootTest
class TracerAnswerRegressionTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired TracerRegistry tracers;
    @Autowired TraceRunner runner;

    /**
     * second-largest-element answers -1 for "no second largest", so a -1 in the input made the
     * answer ambiguous: [-1, -5] gave -1 instead of -5. The input now follows the problem's
     * domain (positive values) and a negative is refused, not silently mis-answered.
     */
    @Test
    void secondLargestRefusesValuesItsSentinelCouldCollideWith() {
        AlgorithmTracer tracer = tracers.find("second-largest-element").orElseThrow();
        assertThatThrownBy(() -> runner.run(tracer, Map.of("nums", List.of(-1, -5))))
                .isInstanceOf(InputValidationException.class);
    }

    /**
     * The nearest-1 BFS seeds its queue with every 1. With no 1 at all, res would stay all 0 -
     * every cell claiming distance 0 to a 1 that does not exist - so the input is refused.
     */
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"nearest-cell-1", "distance-nearest-1"})
    void nearestOneRefusesAGridWithNoOne(String id) {
        AlgorithmTracer tracer = tracers.find(id).orElseThrow();
        assertThatThrownBy(() -> runner.run(tracer, Map.of("grid", List.of(List.of(0, 0), List.of(0, 0)))))
                .isInstanceOf(InputValidationException.class);
    }

    /**
     * Shortest Path in DAG marks an unreachable vertex -1, as the GfG problem does. With a negative
     * weight a real distance could also be -1, so weights follow GfG and must be non-negative.
     */
    @org.junit.jupiter.api.Test
    void shortestPathDagRefusesNegativeWeights() {
        AlgorithmTracer tracer = tracers.find("shortest-path-dag").orElseThrow();
        assertThatThrownBy(() -> runner.run(tracer, Map.of("start", 0,
                "graph", Map.of("vertices", 2, "edges", List.of(List.of(0, 1, -1))))))
                .isInstanceOf(InputValidationException.class);
    }

    @ParameterizedTest(name = "{0} {1} -> {2} = {3}")
    @CsvSource(delimiter = '|', textBlock = """
            # '^' is right-associative: A^B^C is A^(B^C).
            infix-to-postfix       | {"expression":"A^B^C"}                     | answer        | ABC^^
            infix-to-prefix        | {"expression":"A^B^C"}                     | answer        | ^A^BC
            # Left-associative operators must keep working the way they did.
            infix-to-postfix       | {"expression":"A-B-C"}                     | answer        | AB-C-
            infix-to-prefix        | {"expression":"A-B-C"}                     | answer        | --ABC
            infix-to-postfix       | {"expression":"A+B*C^D^E"}                 | answer        | ABCDE^^*+
            infix-to-prefix        | {"expression":"A+B*C^D^E"}                 | answer        | +A*B^C^DE
            # More students than books: no valid allocation.
            book-allocation        | {"pages":[12,34,67,90],"m":5}              | answer        | -1
            # No row contains a 1 (GfG: return -1).
            row-max-ones           | {"matrix":[[0,0],[0,0]]}                   | answer        | -1
            # A target past every element: the bound is the array length, not the letter n.
            lower-bound            | {"nums":[2,4,6,8,10,12,14],"target":100}   | ans           | 7
            upper-bound            | {"nums":[2,4,6,8,10,12,14],"target":100}   | ans           | 7
            # Even positions take 5 digits and odd positions 4: 5^ceil(n/2) * 4^floor(n/2).
            count-good-numbers     | {"n":1}                                    | answer        | 5
            count-good-numbers     | {"n":3}                                    | answer        | 100
            count-good-numbers     | {"n":4}                                    | answer        | 400
            # A product of zero is 0, never -0.
            max-product-subarray   | {"nums":[-2,0,-1]}                         | result        | 0
            # No second largest at all: the -1 sentinel.
            second-largest-element | {"nums":[7,7]}                             | secondLargest | -1
            # A leader is greater than or equal to everything on its right.
            leaders-in-array       | {"nums":[10,4,2,4,1]}                      | leaders       | [10, 4, 4, 1]
            # A zero-valued item is valid input, not a server error.
            subset-sum-equal-target    | {"nums":[1,0],"target":1}              | answer        | true
            partition-equal-subset-sum | {"nums":[0,2,2]}                       | answer        | true
            partition-set-min-abs-diff | {"nums":[0,3]}                         | answer        | 3
            target-sum-dp              | {"nums":[0,1],"target":1}              | answer        | 2
            # Owner-code rewrites (grid BFS): no orange at all needs no time; a fresh orange the rot
            # cannot reach makes it impossible; the last level must not count as a minute.
            rotting-oranges        | {"grid":[[0]]}                             | answer        | 0
            rotting-oranges        | {"grid":[[2,1,1],[0,1,1],[1,0,1]]}         | answer        | -1
            rotting-oranges        | {"grid":[[2,1,1],[1,1,0],[0,1,1]]}         | answer        | 4
            nearest-cell-1         | {"grid":[[0,1,1,0],[1,1,0,0],[0,0,1,1]]}   | res           | [[1,0,0,1],[0,0,1,1],[1,1,0,0]]
            distance-nearest-1     | {"grid":[[1,0,0],[0,0,0],[0,0,0]]}         | res           | [[0,1,2],[1,2,3],[2,3,4]]
            # Shortest paths on the owner's code: a source other than 0 leaves earlier vertices at -1; the
            # signal time is the LAST arrival, not the sum; effort is the biggest single step.
            shortest-path-dag      | {"graph":{"vertices":4,"edges":[[0,1,1],[1,2,2],[2,3,3]]},"start":1} | distance | [-1, 0, 2, 5]
            network-delay-time     | {"graph":{"vertices":4,"edges":[[0,1,1],[0,2,4],[1,2,1],[2,3,1]]},"source":0} | answer | 3
            path-min-effort        | {"heights":[[1,10,1],[1,10,1],[1,1,1]]}    | answer        | 0
            """)
    void tracerComputesTheCorrectAnswer(String id, String input, String variable, String expected) throws Exception {
        AlgorithmTracer tracer = tracers.find(id).orElseThrow(() -> new AssertionError("no tracer " + id));
        Map<String, Object> in = JSON.readValue(input, new TypeReference<>() { });
        List<ExecutionStep> steps = runner.run(tracer, in).getSteps();
        Map<String, String> finalVariables = steps.get(steps.size() - 1).getVariables();
        assertThat(finalVariables).as("%s final variables", id).isNotNull();
        assertThat(finalVariables.get(variable)).as("%s on %s: %s (final variables %s)", id, input, variable, finalVariables)
                .isEqualTo(expected);
    }
}
