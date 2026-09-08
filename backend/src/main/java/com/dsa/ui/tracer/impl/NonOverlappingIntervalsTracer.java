package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Keeps intervals with the earliest finishing times; every overlap is removed. */
@Component
public class NonOverlappingIntervalsTracer implements AlgorithmTracer {
    @Override public String id() { return "non-overlapping-intervals"; }
    @Override public DsType dsType() { return DsType.INTERVAL; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("intervals", FieldType.INT_GRID).label("Intervals")
                .help("Each row is [start, end]. Touching endpoints are allowed.")
                .constraint("maxRows", 30).constraint("maxCols", 2).values(-1000, 1000)
                .defaultValue(List.of(List.of(1, 2), List.of(2, 3), List.of(3, 4), List.of(1, 3)))
                .build());
    }
    @Override public Map<String, Object> alternateInput() {
        return Map.of("intervals", List.of(List.of(1, 2), List.of(1, 2), List.of(1, 2),
                List.of(2, 4), List.of(4, 5)));
    }
    @Override public String annotatedCode() {
        return """
               public int eraseOverlapIntervals(int[][] intervals) {
                   // @a sort
                   Arrays.sort(intervals, Comparator.comparingInt(a -> a[1]));
                   int removals = 0, lastEnd = Integer.MIN_VALUE;
                   for (int[] interval : intervals) {
                       // @a consider
                       if (interval[0] >= lastEnd) {
                           // @a keep
                           lastEnd = interval[1];
                       } else {
                           // @a remove
                           removals++;
                       }
                   }
                   // @a done
                   return removals;
               }""";
    }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[][] intervals = in.getGrid("intervals");
        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i].length != 2 || intervals[i][0] > intervals[i][1]) {
                throw new InputValidationException(Map.of("intervals", "Row " + i + " must be [start, end] with start <= end."));
            }
        }
        Arrays.sort(intervals, Comparator.comparingInt(a -> a[1]));
        String[] states = new String[intervals.length];
        Arrays.fill(states, "default");
        emit.at("sort").say("Sort by finishing time: %s. Earliest finish preserves the most room for later intervals.", text(intervals))
                .var("lastEnd", "-∞").var("removals", 0).arrayState(board(intervals, states, -1)).step();
        int lastEnd = Integer.MIN_VALUE, removals = 0;
        for (int i = 0; i < intervals.length; i++) {
            emit.at("consider").say("Consider [%d,%d] against last kept end %s.", intervals[i][0], intervals[i][1],
                            lastEnd == Integer.MIN_VALUE ? "-∞" : String.valueOf(lastEnd))
                    .var("lastEnd", lastEnd == Integer.MIN_VALUE ? "-∞" : lastEnd).var("removals", removals)
                    .arrayState(board(intervals, states, i)).step();
            if (intervals[i][0] >= lastEnd) {
                lastEnd = intervals[i][1]; states[i] = "settled";
                emit.at("keep").say("It does not overlap. Keep it and move lastEnd to %d.", lastEnd)
                        .var("lastEnd", lastEnd).var("removals", removals)
                        .arrayState(board(intervals, states, i)).step();
            } else {
                removals++; states[i] = "rejected";
                emit.at("remove").say("It starts before %d, so remove it. The earlier-finishing kept interval still wins.", lastEnd)
                        .var("lastEnd", lastEnd).var("removals", removals)
                        .arrayState(board(intervals, states, i)).step();
            }
        }
        emit.at("done").say("Remove %d interval(s); the remainder is non-overlapping.", removals)
                .var("lastEnd", lastEnd).var("removals", removals)
                .arrayState(board(intervals, states, -1)).step();
    }
    private static List<ArrayElement> board(int[][] intervals, String[] states, int current) {
        List<ArrayElement> out = new ArrayList<>();
        for (int i = 0; i < intervals.length; i++) out.add(new ArrayElement(i, intervals[i][1],
                i == current && "default".equals(states[i]) ? "current" : states[i],
                "[" + intervals[i][0] + "," + intervals[i][1] + "]"));
        return out;
    }
    private static String text(int[][] intervals) {
        return Arrays.deepToString(intervals);
    }
}
