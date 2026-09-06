package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Insert Interval — three greedy passes over an already-sorted, non-overlapping list.
 * Copy every interval that ends before the new one starts untouched, absorb every
 * interval that overlaps it into one widening merge, then copy whatever is left. Each
 * interval is visited exactly once and never revisited.
 */
@Component
public class InsertIntervalTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "insert-interval";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("intervals", FieldType.INT_GRID)
                        .label("Existing intervals")
                        .help("Already sorted by start time, with no two overlapping. Each row is [start, end].")
                        .constraint("maxRows", 20)
                        .constraint("maxCols", 2)
                        .values(0, 1000)
                        .defaultValue(List.of(List.of(1, 3), List.of(6, 9)))
                        .build(),
                InputField.of("newStart", FieldType.INT)
                        .label("New interval start")
                        .range(0, 1000)
                        .defaultValue(2)
                        .build(),
                InputField.of("newEnd", FieldType.INT)
                        .label("New interval end")
                        .range(0, 1000)
                        .defaultValue(5)
                        .build());
    }

    /** Five existing intervals, and the new one swallows three of them into one merge. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "intervals", List.of(List.of(1, 2), List.of(3, 5), List.of(6, 7), List.of(8, 10), List.of(15, 18)),
                "newStart", 4,
                "newEnd", 8);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[][] insert(int[][] intervals, int[] newInterval) {
                   List<int[]> result = new ArrayList<>();
                   int i = 0, n = intervals.length;
                   // @a before
                   while (i < n && intervals[i][1] < newInterval[0]) {
                       result.add(intervals[i]);
                       i++;
                   }
                   int mergedStart = newInterval[0], mergedEnd = newInterval[1];
                   // @a merge
                   while (i < n && intervals[i][0] <= mergedEnd) {
                       mergedStart = Math.min(mergedStart, intervals[i][0]);
                       mergedEnd = Math.max(mergedEnd, intervals[i][1]);
                       i++;
                   }
                   // @a addMerged
                   result.add(new int[]{mergedStart, mergedEnd});
                   // @a after
                   while (i < n) {
                       result.add(intervals[i]);
                       i++;
                   }
                   // @a done
                   return result.toArray(new int[0][]);
               }""";
    }

    /**
     * {@code i} advances monotonically through all three passes, so "already handled"
     * is simply "index < i" regardless of which phase produced that handling.
     */
    private List<ArrayElement> board(int[][] intervals, int i, int[] merged) {
        List<ArrayElement> state = new ArrayList<>(intervals.length + 1);
        for (int k = 0; k < intervals.length; k++) {
            String label = "[" + intervals[k][0] + "," + intervals[k][1] + "]";
            String s = k < i ? "sorted" : k == i ? "current" : "target";
            state.add(new ArrayElement(k, intervals[k][1], s, label));
        }
        if (merged != null) {
            state.add(new ArrayElement(intervals.length, merged[1], "visited",
                    "[" + merged[0] + "," + merged[1] + "]"));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] intervals = in.getGrid("intervals");
        int newStart = in.getInt("newStart");
        int newEnd = in.getInt("newEnd");

        for (int k = 0; k < intervals.length; k++) {
            if (intervals[k].length != 2) {
                throw new InputValidationException(Map.of("intervals",
                        "Row " + k + " must have exactly [start, end]."));
            }
            if (intervals[k][1] < intervals[k][0]) {
                throw new InputValidationException(Map.of("intervals",
                        "Interval " + k + " ends at " + intervals[k][1] + " before it starts at " + intervals[k][0] + "."));
            }
        }
        if (newEnd < newStart) {
            throw new InputValidationException(Map.of("newEnd",
                    "The new interval ends at " + newEnd + " before it starts at " + newStart + "."));
        }

        int n = intervals.length;
        List<int[]> result = new ArrayList<>();
        int i = 0;

        emit.at("before").say("New interval is [%d,%d]. Copy every existing interval that ends before it starts, untouched.",
                        newStart, newEnd)
                .var("i", i)
                .arrayState(board(intervals, i, null)).step();

        while (i < n && intervals[i][1] < newStart) {
            emit.at("before").say("[%d,%d] ends at %d, before %d — no overlap possible yet. Copy it as-is.",
                            intervals[i][0], intervals[i][1], intervals[i][1], newStart)
                    .var("i", i + 1).var("copied", result.size() + 1)
                    .arrayState(board(intervals, i, null)).step();
            result.add(intervals[i]);
            i++;
        }

        int mergedStart = newStart, mergedEnd = newEnd;
        emit.at("merge").say("Start merging from the new interval itself: [%d,%d].", mergedStart, mergedEnd)
                .var("mergedStart", mergedStart).var("mergedEnd", mergedEnd)
                .arrayState(board(intervals, i, new int[]{mergedStart, mergedEnd})).step();

        while (i < n && intervals[i][0] <= mergedEnd) {
            int beforeEnd = mergedEnd;
            mergedStart = Math.min(mergedStart, intervals[i][0]);
            mergedEnd = Math.max(mergedEnd, intervals[i][1]);
            emit.at("merge").say("[%d,%d] starts at or before %d, so it overlaps — absorb it. Merged range grows to [%d,%d].",
                            intervals[i][0], intervals[i][1], beforeEnd, mergedStart, mergedEnd)
                    .var("mergedStart", mergedStart).var("mergedEnd", mergedEnd)
                    .arrayState(board(intervals, i, new int[]{mergedStart, mergedEnd})).step();
            i++;
        }

        result.add(new int[]{mergedStart, mergedEnd});
        emit.at("addMerged").say("No more overlaps — commit the merged interval [%d,%d].", mergedStart, mergedEnd)
                .var("mergedStart", mergedStart).var("mergedEnd", mergedEnd)
                .arrayState(board(intervals, i, new int[]{mergedStart, mergedEnd})).step();

        while (i < n) {
            emit.at("after").say("[%d,%d] starts after the merged range ends — copy the rest untouched.",
                            intervals[i][0], intervals[i][1])
                    .var("i", i + 1).var("copied", result.size() + 1)
                    .arrayState(board(intervals, i, new int[]{mergedStart, mergedEnd})).step();
            result.add(intervals[i]);
            i++;
        }

        StringBuilder out = new StringBuilder("[");
        for (int k = 0; k < result.size(); k++) {
            if (k > 0) out.append(", ");
            out.append('[').append(result.get(k)[0]).append(',').append(result.get(k)[1]).append(']');
        }
        out.append(']');

        emit.at("done").say("Final merged list: %s.", out)
                .var("result", out.toString())
                .arrayState(board(intervals, n, null)).step();
    }
}
