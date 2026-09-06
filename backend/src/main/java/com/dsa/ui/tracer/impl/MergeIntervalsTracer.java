package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Merge overlapping intervals: sort by start time, then linear scan.
 *
 * <p>After sorting, overlapping intervals are contiguous in the array.
 * Each new interval either extends the last merged interval (overlap)
 * or starts a fresh non-overlapping segment.
 *
 * <p>Note: this problem id is claimed by both ArrayService and GreedyService
 * (one of the 7 duplicates surfaced in stats.duplicateIds). This tracer
 * serves the ArrayService version.
 */
@Component
public class MergeIntervalsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "merge-intervals";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("intervals", FieldType.INT_GRID)
                        .label("Intervals")
                        .help("Each row is [start, end]. They will be sorted by start time.")
                        .constraint("maxRows", 20)
                        .constraint("maxCols", 2)
                        .values(0, 100)
                        .defaultValue(List.of(
                                List.of(1, 3), List.of(2, 6), List.of(8, 10), List.of(15, 18)))
                        .build());
    }

    /**
     * All intervals overlap into one big merge, plus a disjoint one at the end.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("intervals", List.of(
                List.of(1, 4), List.of(4, 5), List.of(2, 3), List.of(10, 12)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[][] merge(int[][] intervals) {
                   // @a sort
                   Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));
                   List<int[]> res = new ArrayList<>();
               
                   for (int[] interval : intervals) {
                       if (res.isEmpty() || res.get(res.size() - 1)[1] < interval[0]) {
                           // @a addNew
                           res.add(interval);
                       } else {
                           // @a merge
                           res.get(res.size() - 1)[1] =
                               Math.max(res.get(res.size() - 1)[1], interval[1]);
                       }
                   }
                   // @a done
                   return res.toArray(new int[res.size()][]);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] raw = in.getGrid("intervals");
        int n = raw.length;
        int[][] intervals = new int[n][2];
        for (int i = 0; i < n; i++) {
            intervals[i][0] = raw[i][0];
            intervals[i][1] = raw[i][1];
        }

        Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));

        // Flatten intervals into a 1D array for visualization: [s1,e1,s2,e2,...]
        int[] flat = new int[n * 2];
        for (int i = 0; i < n; i++) {
            flat[i * 2] = intervals[i][0];
            flat[i * 2 + 1] = intervals[i][1];
        }

        emit.at("sort")
                .say("Sorted %d intervals by start time: %s.", n, intervalsToString(intervals))
                .var("count", n)
                .array(flat)
                .step();

        List<int[]> res = new ArrayList<>();

        for (int idx = 0; idx < n; idx++) {
            int[] interval = intervals[idx];
            if (res.isEmpty() || res.get(res.size() - 1)[1] < interval[0]) {
                res.add(new int[]{interval[0], interval[1]});
                emit.at("addNew")
                        .say("[%d, %d] does not overlap with the last merged interval%s → add as new segment. Merged: %s.",
                                interval[0], interval[1],
                                res.size() > 1 ? " [" + res.get(res.size() - 2)[0] + ", " + res.get(res.size() - 2)[1] + "]" : "",
                                resToString(res))
                        .var("current", "[" + interval[0] + "," + interval[1] + "]")
                        .var("merged", resToString(res))
                        .array(flat, idx * 2, idx * 2 + 1)
                        .step();
            } else {
                int oldEnd = res.get(res.size() - 1)[1];
                res.get(res.size() - 1)[1] = Math.max(oldEnd, interval[1]);
                int newEnd = res.get(res.size() - 1)[1];
                emit.at("merge")
                        .say("[%d, %d] overlaps with last [%d, %d] → extend end to max(%d, %d) = %d. Merged: %s.",
                                interval[0], interval[1],
                                res.get(res.size() - 1)[0], oldEnd,
                                oldEnd, interval[1], newEnd,
                                resToString(res))
                        .var("current", "[" + interval[0] + "," + interval[1] + "]")
                        .var("merged", resToString(res))
                        .array(flat, idx * 2, idx * 2 + 1)
                        .step();
            }
        }

        emit.at("done")
                .say("Merge complete: %d intervals → %d non-overlapping segments: %s.",
                        n, res.size(), resToString(res))
                .var("result", resToString(res))
                .array(flat)
                .step();
    }

    private String intervalsToString(int[][] intervals) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < intervals.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("[").append(intervals[i][0]).append(",").append(intervals[i][1]).append("]");
        }
        return sb.append("]").toString();
    }

    private String resToString(List<int[]> res) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < res.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("[").append(res.get(i)[0]).append(",").append(res.get(i)[1]).append("]");
        }
        return sb.append("]").toString();
    }
}
