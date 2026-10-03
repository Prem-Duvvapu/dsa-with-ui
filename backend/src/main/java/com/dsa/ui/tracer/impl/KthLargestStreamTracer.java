package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Kth Largest Element in a Stream (LeetCode 703), traced on the owner's own accepted submission: a
 * min-heap trimmed to k values after every add holds the k largest so far, so its top is the
 * answer after each add. O(log k) per add.
 *
 * <p>One change: the submission's constructor added every initial number without trimming, so the
 * heap held all of them until the first add. Trimming there too keeps it at k values - O(k) memory;
 * a comment in the displayed code says so.
 */
@Component
public class KthLargestStreamTracer implements AlgorithmTracer {
    @Override public String id() { return "kth-largest-stream"; }
    @Override public DsType dsType() { return DsType.HEAP; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("initial", FieldType.INT_ARRAY).label("Initial stream values")
                        .length(1, 30).values(-1000, 1000).defaultValue(List.of(4, 5, 8, 2)).build(),
                InputField.of("additions", FieldType.INT_ARRAY).label("Values to add")
                        .length(1, 30).values(-1000, 1000).defaultValue(List.of(3, 5, 10, 9, 4)).build(),
                InputField.of("k", FieldType.INT).label("K").range(1, 30).defaultValue(3).build());
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of("initial", List.of(7, 1, 6, 4), "additions", List.of(8, 2, 10, 5, 11, 3), "k", 2);
    }

    @Override public String annotatedCode() {
        return """
               // Changed from your submission: the constructor now trims the heap to k as well, so it
               // never holds more than k values (O(k) memory). add() is unchanged.
               class KthLargest {
                   int k;
                   PriorityQueue<Integer> pq;

                   public KthLargest(int k, int[] nums) {
                       // @a init
                       this.k=k;
                       pq=new PriorityQueue<>((a,b) -> Integer.compare(a,b)); //min heap

                       for (int val: nums) {
                           pq.add(val);
                           if (pq.size()>k)
                               // @a trim
                               pq.poll();
                       }
                   }

                   public int add(int val) {
                       // @a add
                       pq.add(val);

                       while (pq.size()>k)
                           // @a evict
                           pq.poll();

                       // @a answer
                       return pq.peek();
                   }
               }""";
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[] initial = in.getIntArray("initial");
        int[] additions = in.getIntArray("additions");
        int k = in.getInt("k");
        if (k > initial.length) {
            throw new InputValidationException(Map.of("k", "Must not exceed initial.length (" + initial.length + ")."));
        }
        ArrayHeap<Integer> pq = ArrayHeap.minHeap();
        emit.at("init").say("Build the stream with k = %d from %s. A min-heap of the k largest values has the "
                        + "k-th largest on top.", k, java.util.Arrays.toString(initial))
                .var("k", k).array(initial).step();
        for (int val : initial) {
            int at = pq.offer(val);
            if (pq.size() > k) {
                int out = pq.poll();
                emit.at("trim").say("Adding %d makes %d values; drop the smallest, %d.", val, k + 1, out)
                        .var("size", pq.size()).array(slots(pq), 0).step();
            } else {
                emit.at("init").say("Add %d to the heap.", val).var("size", pq.size()).array(slots(pq), at).step();
            }
        }
        List<Integer> answers = new ArrayList<>();
        for (int val : additions) {
            int at = pq.offer(val);
            emit.at("add").say("add(%d): push it.", val).var("val", val).array(slots(pq), at).step();
            while (pq.size() > k) {
                int out = pq.poll();
                emit.at("evict").say("More than %d values: drop the smallest, %d.", k, out)
                        .var("evicted", out).array(slots(pq), 0).step();
            }
            answers.add(pq.peek());
            emit.at("answer").say("The top, %d, is the %s largest so far. add(%d) returns %d.",
                            pq.peek(), Narration.ordinal(k), val, pq.peek())
                    .var("answers", answers.toString()).array(slots(pq), 0).step();
        }
    }

    private static int[] slots(ArrayHeap<Integer> heap) {
        int[] out = new int[heap.size()];
        for (int i = 0; i < out.length; i++) out[i] = heap.slots().get(i);
        return out;
    }
}
