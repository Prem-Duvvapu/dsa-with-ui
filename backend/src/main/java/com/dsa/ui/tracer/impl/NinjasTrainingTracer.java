package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ninja's training: a three-way choice per day rather than the two-way choice
 * {@link MaxSumNonAdjacentTracer} and {@link HouseRobber2Tracer} trace. The constraint is
 * not "don't pick an adjacent index" but "don't repeat yesterday's activity" — so a cell
 * excludes exactly one same-column predecessor and reads the other two, rather than reading
 * a fixed neighbour shape the way every grid tracer so far has.
 *
 * <p>The table is fixed at three columns because the problem itself is: three named
 * activities, never more. Rows are days.
 */
@Component
public class NinjasTrainingTracer implements AlgorithmTracer {

    private static final int ACTIVITIES = 3;

    @Override
    public String id() {
        return "ninjas-training";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("points", FieldType.INT_GRID)
                        .label("Daily merit points")
                        .help("Three activities per day. The same activity cannot be done on "
                                + "two consecutive days.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", ACTIVITIES)
                        .values(0, 100)
                        .defaultValue(List.of(
                                List.of(10, 40, 70),
                                List.of(20, 50, 80),
                                List.of(30, 10, 20)))
                        .build());
    }

    /** More days, and a different activity ends up winning the final day. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("points", List.of(
                List.of(1, 2, 5),
                List.of(3, 1, 1),
                List.of(3, 3, 3),
                List.of(5, 1, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int ninjaTraining(int n, int[][] points) {
                   // @a init
                   int[][] dp = new int[n][3];
                   for (int task = 0; task < 3; task++) {
                       // @a base
                       dp[0][task] = points[0][task];
                   }
                   for (int day = 1; day < n; day++) {
                       for (int task = 0; task < 3; task++) {
                           int best = 0;
                           for (int prev = 0; prev < 3; prev++) {
                               if (prev != task) best = Math.max(best, dp[day - 1][prev]);
                           }
                           // @a combine
                           dp[day][task] = points[day][task] + best;
                       }
                   }
                   int answer = 0;
                   for (int task = 0; task < 3; task++) {
                       // @a reduce
                       answer = Math.max(answer, dp[n - 1][task]);
                   }
                   // @a done
                   return answer;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] points = in.getGrid("points");
        int days = points.length;
        int[][] dp = new int[days][ACTIVITIES];
        boolean[][] settled = new boolean[days][ACTIVITIES];

        emit.at("init")
                .say("%d days, three named activities per day. dp[day][task] will hold the "
                        + "best total through that day, ending on that activity.", days)
                .var("days", days)
                .dpTable(table(dp, settled, points, null, "?", Set.of(), false)).step();

        for (int task = 0; task < ACTIVITIES; task++) {
            dp[0][task] = points[0][task];
            settled[0][task] = true;
            emit.at("base")
                    .say("Day 0, activity %d: no prior day to conflict with, so the score is "
                            + "just this activity's own points, %d.", task, points[0][task])
                    .var("day", 0).var("task", task)
                    .dpTable(table(dp, settled, points, new GridDpTable.Coord(0, task),
                            String.valueOf(points[0][task]), Set.of(), false)).step();
        }

        for (int day = 1; day < days; day++) {
            for (int task = 0; task < ACTIVITIES; task++) {
                List<GridDpTable.Coord> reads = new ArrayList<>(2);
                int best = 0;
                int bestPrev = -1;
                for (int prev = 0; prev < ACTIVITIES; prev++) {
                    if (prev == task) {
                        continue;
                    }
                    reads.add(new GridDpTable.Coord(day - 1, prev));
                    if (dp[day - 1][prev] > best) {
                        best = dp[day - 1][prev];
                        bestPrev = prev;
                    }
                }
                int total = points[day][task] + best;

                emit.at("combine")
                        .say("Day %d, activity %d: yesterday's activity %d is off-limits, so "
                                + "the best carried-forward score is whichever of the other "
                                + "two is higher — activity %d at %d. dp[%d][%d] = %d + %d "
                                + "= %d.",
                                day, task, task, bestPrev, best, day, task, points[day][task],
                                best, total)
                        .var("day", day).var("task", task)
                        .var("best", best).var("dp[day][task]", total)
                        .dpTable(table(dp, settled, points, new GridDpTable.Coord(day, task),
                                String.valueOf(total), Set.copyOf(reads), false)).step();

                dp[day][task] = total;
                settled[day][task] = true;
            }
        }

        int answer = 0;
        Set<GridDpTable.Coord> seen = new java.util.LinkedHashSet<>();
        for (int task = 0; task < ACTIVITIES; task++) {
            int before = answer;
            boolean improves = dp[days - 1][task] > before;
            if (improves) {
                answer = dp[days - 1][task];
            }
            seen.add(new GridDpTable.Coord(days - 1, task));
            emit.at("reduce")
                    .say("The last day's activity is unconstrained by anything after it, so "
                            + "the answer is the best of all three finishing activities. "
                            + "Comparing activity %d (%d) against the running best so far "
                            + "(%d): %s.",
                            task, dp[days - 1][task], before,
                            improves ? "activity %d is better, so the best rises to %d"
                                    .formatted(task, answer)
                                    : "no improvement, the best stays %d".formatted(before))
                    .var("task", task).var("running", answer)
                    .dpTable(table(dp, settled, points, null, String.valueOf(answer),
                            Set.copyOf(seen), false)).step();
        }

        emit.at("done")
                .say("The most merit points achievable over %d days is %d.", days, answer)
                .var("answer", answer)
                .dpTable(table(dp, settled, points, null, "?", Set.of(), true)).step();
    }

    private static DpTable table(int[][] dp, boolean[][] settled, int[][] points,
                                 GridDpTable.Coord probe, String probeValue,
                                 Set<GridDpTable.Coord> reads, boolean done) {
        int days = dp.length;
        List<String> rowLabels = new ArrayList<>(days);
        for (int d = 0; d < days; d++) {
            rowLabels.add("day " + d);
        }
        List<String> colLabels = List.of("activity 0", "activity 1", "activity 2");

        List<List<DpCell>> cells = new ArrayList<>(days);
        for (int d = 0; d < days; d++) {
            List<DpCell> row = new ArrayList<>(ACTIVITIES);
            for (int t = 0; t < ACTIVITIES; t++) {
                GridDpTable.Coord here = new GridDpTable.Coord(d, t);
                String value;
                String state;
                if (done) {
                    state = "resolved";
                    value = String.valueOf(dp[d][t]);
                } else if (here.equals(probe)) {
                    state = "probe";
                    value = probeValue;
                } else if (reads.contains(here)) {
                    state = "read";
                    value = String.valueOf(dp[d][t]);
                } else if (settled[d][t]) {
                    state = "known";
                    value = String.valueOf(dp[d][t]);
                } else {
                    state = "void";
                    value = "·";
                }
                row.add(new DpCell(value, state));
            }
            cells.add(row);
        }
        return new DpTable(rowLabels, colLabels, cells);
    }
}
