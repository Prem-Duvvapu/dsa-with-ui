package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Find Median from Data Stream (LeetCode 295), traced on the owner's own accepted submission: a
 * max-heap holds the smaller half and a min-heap the larger half. Each number passes through one heap
 * and its top moves to the other, so the halves stay ordered; isEven decides which way. With an odd
 * count the min-heap holds the extra value and its top is the median. O(log n) per add.
 *
 * <p>The canvas shows the two halves side by side - the max-heap's values then the min-heap's - with
 * the heaps themselves kept as {@link ArrayHeap}s, which sift as java.util.PriorityQueue does.
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

    /** A stream of equal values, odd in length, so the median is read off minHeap alone. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(2, 2, 2, 2, 2));
    }

    @Override
    public String annotatedCode() {
        return """
               class MedianFinder {
                   PriorityQueue<Integer> minHeap;
                   PriorityQueue<Integer> maxHeap;
                   boolean isEven;

                   public MedianFinder() {
                       // @a init
                       minHeap=new PriorityQueue<>();
                       maxHeap=new PriorityQueue<>((x,y) -> Integer.compare(y,x));
                       isEven=true;
                   }

                   public void addNum(int num) {
                       if (isEven) { //curr size is even
                           // @a viaMax
                           maxHeap.add(num);
                           minHeap.add(maxHeap.poll());
                       } else { //curr size is odd
                           // @a viaMin
                           minHeap.add(num);
                           maxHeap.add(minHeap.poll());
                       }

                       isEven=!isEven;
                   }

                   public double findMedian() {
                       double median=0.0;

                       if (isEven) {
                           // @a evenMedian
                           int first=maxHeap.peek();
                           int second=minHeap.peek();
                           median=(first+second)/2.0;
                       } else {
                           // @a oddMedian
                           median=(double)minHeap.peek();
                       }

                       return median;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        ArrayHeap<Integer> minHeap = ArrayHeap.minHeap();
        ArrayHeap<Integer> maxHeap = new ArrayHeap<>((x, y) -> Integer.compare(y, x));
        boolean isEven = true;
        emit.at("init").say("maxHeap keeps the smaller half (largest on top), minHeap the larger half (smallest on top).")
                .arrayState(render(maxHeap, minHeap)).step();
        for (int num : nums) {
            if (isEven) {
                maxHeap.offer(num);
                int moved = maxHeap.poll();
                minHeap.offer(moved);
                emit.at("viaMax").say("addNum(%d): the count was even. Push it into maxHeap, then move maxHeap's top, %d, "
                                + "to minHeap - minHeap now holds the extra value.", num, moved)
                        .var("num", num).var("maxHeap", sorted(maxHeap, true)).var("minHeap", sorted(minHeap, false))
                        .arrayState(render(maxHeap, minHeap)).step();
            } else {
                minHeap.offer(num);
                int moved = minHeap.poll();
                maxHeap.offer(moved);
                emit.at("viaMin").say("addNum(%d): the count was odd. Push it into minHeap, then move minHeap's top, %d, "
                                + "to maxHeap - the halves are equal again.", num, moved)
                        .var("num", num).var("maxHeap", sorted(maxHeap, true)).var("minHeap", sorted(minHeap, false))
                        .arrayState(render(maxHeap, minHeap)).step();
            }
            isEven = !isEven;
        }
        double median;
        if (isEven) {
            int first = maxHeap.peek();
            int second = minHeap.peek();
            median = (first + second) / 2.0;
            emit.at("evenMedian").say("An even count: the median is the average of the two middle values, (%d + %d) / 2 = %s.",
                            first, second, median)
                    .var("median", String.valueOf(median)).arrayState(render(maxHeap, minHeap)).step();
        } else {
            median = (double) minHeap.peek();
            emit.at("oddMedian").say("An odd count: minHeap holds the extra value, so its top, %d, is the median.", minHeap.peek())
                    .var("median", String.valueOf(median)).arrayState(render(maxHeap, minHeap)).step();
        }
    }

    /**
     * The two halves side by side, each sorted: maxHeap's values labelled "max", then minHeap's
     * labelled "min". The heap canvas draws one heap, and two heaps drawn as one tree would be wrong.
     */
    private static List<ArrayElement> render(ArrayHeap<Integer> maxHeap, ArrayHeap<Integer> minHeap) {
        List<ArrayElement> state = new ArrayList<>();
        int i = 0;
        for (int v : sortedList(maxHeap)) state.add(new ArrayElement(i++, v, "known", "max"));
        for (int v : sortedList(minHeap)) state.add(new ArrayElement(i++, v, "current", "min"));
        return state;
    }

    private static List<Integer> sortedList(ArrayHeap<Integer> heap) {
        List<Integer> out = new ArrayList<>(heap.slots());
        java.util.Collections.sort(out);
        return out;
    }

    private static String sorted(ArrayHeap<Integer> heap, boolean descending) {
        List<Integer> out = sortedList(heap);
        if (descending) java.util.Collections.reverse(out);
        return out.toString();
    }
}
