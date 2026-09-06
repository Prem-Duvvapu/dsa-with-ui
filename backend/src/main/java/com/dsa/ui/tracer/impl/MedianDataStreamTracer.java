package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Two heaps split the stream in half: a max-heap holds the smaller half (its top is the
 * largest of the small values), a min-heap holds the larger half (its top is the smallest
 * of the large values), and the two tops are the only values ever compared to find the
 * median - the rest of each heap never needs to be looked at. Shown as one sorted array
 * with the split point marked, since that is the multiset the two heaps together represent.
 */
@Component
public class MedianDataStreamTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "median-data-stream";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Stream of numbers, in arrival order")
                        .length(1, 30).values(-1000, 1000)
                        .defaultValue(List.of(5, 15, 1, 3))
                        .build());
    }

    /** A longer stream with a different value shape, not a permutation of the defaults. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(2, 2, 2, 2, 2, 2));
    }

    @Override
    public String annotatedCode() {
        return """
               PriorityQueue<Integer> small = new PriorityQueue<>(Collections.reverseOrder());
               PriorityQueue<Integer> large = new PriorityQueue<>();

               public void addNum(int num) {
                   // @a insert
                   if (small.isEmpty() || num <= small.peek()) small.add(num);
                   else large.add(num);

                   // @a rebalance
                   if (small.size() > large.size() + 1) large.add(small.poll());
                   else if (large.size() > small.size()) small.add(large.poll());
               }

               public double findMedian() {
                   // @a median
                   if (small.size() > large.size()) return small.peek();
                   return (small.peek() + large.peek()) / 2.0;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");

        List<Integer> small = new ArrayList<>(); // max-heap, kept sorted descending
        List<Integer> large = new ArrayList<>(); // min-heap, kept sorted ascending

        for (int num : nums) {
            boolean intoSmall = small.isEmpty() || num <= small.get(0);
            if (intoSmall) {
                insertDescending(small, num);
            } else {
                insertAscending(large, num);
            }
            emit.at("insert")
                    .say("Insert %d into the %s half.", num, intoSmall ? "smaller" : "larger")
                    .var("inserted", num).var("smallSize", small.size()).var("largeSize", large.size())
                    .arrayState(render(small, large)).step();

            if (small.size() > large.size() + 1) {
                int moved = small.remove(0);
                insertAscending(large, moved);
                emit.at("rebalance")
                        .say("Smaller half outgrew the larger half by more than one - move its top "
                                        + "(%d) across.", moved)
                        .var("moved", moved)
                        .arrayState(render(small, large)).step();
            } else if (large.size() > small.size()) {
                int moved = large.remove(0);
                insertDescending(small, moved);
                emit.at("rebalance")
                        .say("Larger half outgrew the smaller half - move its top (%d) across.", moved)
                        .var("moved", moved)
                        .arrayState(render(small, large)).step();
            }

            double median = small.size() > large.size()
                    ? small.get(0)
                    : (small.get(0) + large.get(0)) / 2.0;
            emit.at("median")
                    .say("Both tops are now the only values that matter. Median so far: %s.",
                            median == Math.floor(median) ? String.valueOf((int) median) : String.valueOf(median))
                    .var("median", median)
                    .arrayState(render(small, large)).step();
        }
    }

    private static void insertDescending(List<Integer> heap, int value) {
        int i = 0;
        while (i < heap.size() && heap.get(i) > value) i++;
        heap.add(i, value);
    }

    private static void insertAscending(List<Integer> heap, int value) {
        int i = 0;
        while (i < heap.size() && heap.get(i) < value) i++;
        heap.add(i, value);
    }

    /** small (descending) reversed, then large (ascending) - one ascending run, split marked. */
    private static List<ArrayElement> render(List<Integer> small, List<Integer> large) {
        List<Integer> combined = new ArrayList<>();
        for (int i = small.size() - 1; i >= 0; i--) combined.add(small.get(i));
        combined.addAll(large);

        List<ArrayElement> state = new ArrayList<>(combined.size());
        for (int i = 0; i < combined.size(); i++) {
            String s = i < small.size() ? "known" : "current";
            state.add(new ArrayElement(i, combined.get(i), s));
        }
        return state;
    }
}
