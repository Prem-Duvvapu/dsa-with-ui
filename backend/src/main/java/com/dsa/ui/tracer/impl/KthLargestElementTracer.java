package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kth Largest Element in an Array (LeetCode 215), traced on the owner's own accepted submission: a
 * min-heap that never holds more than k values keeps exactly the k largest seen so far, so its top
 * is the k-th largest. O(n log k).
 *
 * <p>The canvas draws the heap's real array ({@link ArrayHeap}, which sifts exactly as
 * {@code java.util.PriorityQueue} does for add and poll).
 */
@Component
public class KthLargestElementTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "kth-largest-element";
    }

    @Override
    public DsType dsType() {
        return DsType.HEAP;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("Values are processed left to right.")
                        .length(1, 40).values(-1000, 1000)
                        .defaultValue(List.of(3, 2, 1, 5, 6, 4))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K")
                        .help("Must be at most nums.length to mean anything.")
                        .range(1, 40)
                        .defaultValue(2)
                        .build());
    }

    /** A longer array and a bigger k, so the heap grows past 2 entries and evicts repeatedly. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(7, 10, 4, 3, 20, 15), "k", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public int findKthLargest(int[] nums, int k) {
                       // @a init
                       int kle=0;
                       PriorityQueue<Integer> minHeap=new PriorityQueue<>();

                       for (int val: nums)
                       {
                           if (minHeap.size()<=k)
                               // @a add
                               minHeap.add(val);

                           if (minHeap.size()>k)
                               // @a evict
                               minHeap.poll();
                       }

                       // @a done
                       return minHeap.peek();
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");
        if (k > nums.length) {
            throw new InputValidationException(Map.of("k", "can be at most the array length, " + nums.length));
        }
        ArrayHeap<Integer> minHeap = ArrayHeap.minHeap();
        emit.at("init").say("Keep a min-heap of at most k = %d values: then it holds the %d largest seen so far, "
                        + "and its top is the smallest of those.", k, k)
                .var("k", k).array(nums).step();
        for (int val : nums) {
            if (minHeap.size() <= k) {
                int at = minHeap.offer(val);
                emit.at("add").say("Add %d. The heap holds %d value%s; the top is %d.",
                                val, minHeap.size(), Narration.s(minHeap.size()), minHeap.peek())
                        .var("val", val).var("size", minHeap.size()).array(slots(minHeap), at).step();
            }
            if (minHeap.size() > k) {
                int out = minHeap.poll();
                emit.at("evict").say("More than %d values: remove the smallest, %d - it cannot be among the %d largest.",
                                k, out, k)
                        .var("evicted", out).var("size", minHeap.size()).array(slots(minHeap), 0).step();
            }
        }
        emit.at("done").say("The heap holds the %d largest values, and its top, %d, is the %s largest.",
                        k, minHeap.peek(), Narration.ordinal(k))
                .var("answer", minHeap.peek()).array(slots(minHeap), 0).step();
    }

    private static int[] slots(ArrayHeap<Integer> heap) {
        int[] out = new int[heap.size()];
        for (int i = 0; i < out.length; i++) out[i] = heap.slots().get(i);
        return out;
    }
}
