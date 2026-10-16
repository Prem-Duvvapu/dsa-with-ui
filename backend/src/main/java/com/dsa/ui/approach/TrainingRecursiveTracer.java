package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

/** Same ending-activity state as canonical tabulation; three real roots, not a forbidden-task substitution. */
final class TrainingRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    TrainingRecursiveTracer(boolean memo) { this.memo = memo; }
    public String id() { return "ninjas-training"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/" + id() + "/" + (memo ? "memoization" : "recursion") + ".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("points", FieldType.INT_GRID).label("Daily merit points")
                .constraint("minRows", 1).constraint("maxRows", memo ? 10 : 6)
                .constraint("minCols", 3).constraint("maxCols", 3).values(0, 100)
                .defaultValue(List.of(List.of(10, 40, 70), List.of(20, 50, 80), List.of(30, 10, 20)))
                .help("Exactly three activities per day; no consecutive repetition. "
                        + (memo ? "Cache by day and ending activity, including zero." : "Up to 6 days keeps all three root trees visible.")).build());
    }
    public Map<String,Object> alternateInput() { return Map.of("points", List.of(List.of(1,2,5), List.of(3,1,1), List.of(3,3,3), List.of(5,1,1))); }
    public void run(Inputs in, StepEmitter emit) {
        int[][] points = in.getGrid("points");
        var execution = new RecursiveDpExecution(emit, memo, 1, RecursiveDpExecution.labels("day=", points.length),
                RecursiveDpExecution.labels("task=", 3), null, "score");
        var context = new RecursiveDpExecution.State(0, -1, -1, "best of three ending activities");
        execution.start(context, "score(day,task) ends on that task. Evaluate each of the three possible final activities as a separate root call.");
        int answer = 0;
        for (int task = 0; task < 3; task++) {
            execution.note("reduce", context, "Evaluate the final-day root ending on activity " + task + "; take the maximum of all three roots.");
            answer = Math.max(answer, score(points.length - 1, task, points, execution));
        }
        execution.done(context, answer);
    }
    private int score(int day, int task, int[][] points, RecursiveDpExecution execution) {
        var state = new RecursiveDpExecution.State(0, day, task, "day=" + day + ",task=" + task);
        return execution.evaluate(state, day == 0, "Keep today's ending activity fixed; compare only the two different preceding-day activities.", () -> {
            if (day == 0) return points[0][task];
            int previous = 0;
            for (int other = 0; other < 3; other++) if (other != task) previous = Math.max(previous, score(day - 1, other, points, execution));
            return points[day][task] + previous;
        });
    }
}
