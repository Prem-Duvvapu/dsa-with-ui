package com.dsa.ui.tracer;

import com.dsa.ui.model.ExecutionStep;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every input a DP problem accepts must reach its answer.
 *
 * An independent verification of all 56 DP problems (2026-10-05) found 13 whose allowed inputs
 * made a trace over the 2 MB response budget: the run stopped, marked "cut short", BEFORE the
 * answer step, so a learner who entered a legal input never saw the result. Each row here is a
 * problem's largest allowed input - a 15 x 15 table, or the new length limit - and must finish
 * with the right answer (expected values from a brute-force reference, not from the tracer).
 * The second test pins the other side: one step over the limit is refused with a field error.
 */
@SpringBootTest
class DpTableBudgetTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired TracerRegistry tracers;
    @Autowired TraceRunner runner;

    @ParameterizedTest(name = "{0} at its limit finishes with {2} = {3}")
    @CsvSource(delimiter = '|', textBlock = """
            count-subsets-with-sum-k    | {"nums":[1,2,3,4,5,6,7,8,9,10],"k":19}                                         | answer | 29
            count-subsets-with-sum-k    | {"nums":[1,3,5,7,9,11,13,15],"k":24}                                           | answer | 7
            subset-sum-equal-target     | {"nums":[20,20,20,20,20,20,20,20,20,19],"target":19}                           | answer | true
            count-partitions-given-diff | {"nums":[1,2,3,4,5,6,7,1,2,1],"d":6}                                          | answer | 58
            partition-equal-subset-sum  | {"nums":[4,4,4,4,4,4,4,4,3,3]}                                                 | answer | true
            partition-set-min-abs-diff  | {"nums":[2,2,2,2,2,2,2,2,2,1]}                                                 | answer | 1
            target-sum-dp               | {"nums":[1,1,1,1,1,1,1,1,1,0],"target":3}                                      | answer | 168
            knapsack-01                 | {"weights":[1,2,3,4,5,6,7,8],"values":[3,5,8,9,13,14,17,20],"capacity":24}     | answer | 61
            unbounded-knapsack          | {"weights":[1,2,3,4,5,6,7,8],"values":[3,5,8,9,13,14,17,20],"capacity":24}     | answer | 72
            coin-change-2               | {"coins":[1,2,3,4,5,6,7,8],"amount":24}                                        | answer | 919
            minimum-coins-dp            | {"coins":[3,5,7,8,9,10,11,12],"amount":24}                                     | answer | 2
            edit-distance               | {"word1":"aaaaaaaaaaaaaa","word2":"bbbbbbbbbbbbbb"}                            | answer | 14
            wildcard-matching           | {"s":"aaaaaaaaaaaaaa","p":"*a*a*a*a*a*a*a"}                                    | answer | true
            print-lis                   | {"nums":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21]}             | lis    | [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21]
            """)
    void largestAllowedInputReachesItsAnswer(String id, String input, String variable, String expected) throws Exception {
        AlgorithmTracer tracer = tracers.find(id).orElseThrow(() -> new AssertionError("no tracer " + id));
        Map<String, Object> in = JSON.readValue(input, new TypeReference<>() { });
        ExecutionTrace trace = runner.run(tracer, in);
        assertThat(trace.isTruncated()).as("%s on %s was cut short: %s", id, input, trace.getTruncationReason()).isFalse();
        List<ExecutionStep> steps = trace.getSteps();
        assertThat(steps.get(steps.size() - 1).getVariables().get(variable)).as("%s %s", id, variable).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} one step over its limit is refused")
    @CsvSource(delimiter = '|', textBlock = """
            count-subsets-with-sum-k    | {"nums":[1,2,3,4,5,6,7,8,9,10],"k":20}
            subset-sum-equal-target     | {"nums":[20,20,20,20,20,20,20,20,20,19],"target":20}
            count-partitions-given-diff | {"nums":[1,2,3,4,5,6,7,1,2,1],"d":8}
            partition-equal-subset-sum  | {"nums":[4,4,4,4,4,4,4,4,4,4]}
            partition-set-min-abs-diff  | {"nums":[2,2,2,2,2,2,2,2,2,2]}
            target-sum-dp               | {"nums":[1,1,1,1,1,1,1,1,1,1],"target":0}
            knapsack-01                 | {"weights":[1,2,3,4,5,6,7,8],"values":[1,1,1,1,1,1,1,1],"capacity":25}
            unbounded-knapsack          | {"weights":[1,2,3,4,5,6,7,8],"values":[1,1,1,1,1,1,1,1],"capacity":25}
            coin-change-2               | {"coins":[1,2,3,4,5,6,7,8],"amount":25}
            minimum-coins-dp            | {"coins":[1,2,3,4,5,6,7,8],"amount":25}
            edit-distance               | {"word1":"aaaaaaaaaaaaaaa","word2":"b"}
            wildcard-matching           | {"s":"aaaaaaaaaaaaaaa","p":"*"}
            print-lis                   | {"nums":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22]}
            """)
    void oneStepOverTheLimitIsRefused(String id, String input) throws Exception {
        AlgorithmTracer tracer = tracers.find(id).orElseThrow(() -> new AssertionError("no tracer " + id));
        Map<String, Object> in = JSON.readValue(input, new TypeReference<>() { });
        assertThatThrownBy(() -> runner.run(tracer, in)).as(id).isInstanceOf(InputValidationException.class);
    }
}
