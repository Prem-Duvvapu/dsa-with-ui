package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kth Smallest Element (GfG), traced on the owner's own accepted submission: a max-heap that never
 * holds more than k values keeps the k smallest seen so far, so its top is the k-th smallest.
 * O(n log k).
 *
 * <p>One change: the submission's comparator ({@code a<b ? 1 : -1}) never returned 0, which breaks
 * Java's Comparator contract for equal values. {@code Integer.compare(b, a)} orders the same max-heap
 * safely; a comment in the displayed code says so.
 */
@Component
public class KthSmallestElementTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "kth-smallest-element";
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
                        .defaultValue(List.of(7, 10, 4, 3, 20, 15))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K")
                        .help("Must be at most nums.length to mean anything.")
                        .range(1, 40)
                        .defaultValue(3)
                        .build());
    }

    /** A different array and a smaller k, so eviction starts earlier and the answer changes. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(15, 5, 20, 1, 8), "k", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               // Changed from your submission: its comparator (a<b ? 1 : -1) never returned 0, which
               // breaks Java's Comparator contract for equal values. Integer.compare(b,a) builds the
               // same max-heap safely.
               class Solution{
                   public static int kthSmallest(int[] arr, int l, int r, int k)
                   {
                       // @a init
                       PriorityQueue<Integer> q=new PriorityQueue<>((a,b) -> Integer.compare(b,a)); //contains largest element at peek
                       for (int val: arr)
                       {
                           // @a add
                           q.add(val);
                           if (q.size()>k)
                               // @a evict
                               q.poll();
                       }

                       // @a done
                       return q.peek();
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("nums");
        int k = in.getInt("k");
        if (k > arr.length) {
            throw new InputValidationException(Map.of("k", "can be at most the array length, " + arr.length));
        }
        ArrayHeap<Integer> q = new ArrayHeap<>((a, b) -> Integer.compare(b, a));
        emit.at("init").say("Keep a max-heap of at most k = %d values: then it holds the %d smallest seen so far, "
                        + "and its top is the largest of those.", k, k)
                .var("k", k).array(arr).step();
        for (int val : arr) {
            int at = q.offer(val);
            emit.at("add").say("Add %d. The heap holds %d value%s; the top is %d.",
                            val, q.size(), Narration.s(q.size()), q.peek())
                    .var("val", val).var("size", q.size()).array(slots(q), at).step();
            if (q.size() > k) {
                int out = q.poll();
                emit.at("evict").say("More than %d values: remove the largest, %d - it cannot be among the %d smallest.",
                                k, out, k)
                        .var("evicted", out).var("size", q.size()).array(slots(q), 0).step();
            }
        }
        emit.at("done").say("The heap holds the %d smallest values, and its top, %d, is the %s smallest.",
                        k, q.peek(), Narration.ordinal(k))
                .var("answer", q.peek()).array(slots(q), 0).step();
    }

    private static int[] slots(ArrayHeap<Integer> heap) {
        int[] out = new int[heap.size()];
        for (int i = 0; i < out.length; i++) out[i] = heap.slots().get(i);
        return out;
    }
}
