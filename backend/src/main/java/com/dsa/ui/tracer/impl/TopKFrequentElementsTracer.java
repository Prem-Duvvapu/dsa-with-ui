package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Top K Frequent Elements (LeetCode 347), in the owner's style: count with a HashMap, then keep a
 * min-heap ordered by frequency at no more than k values, so it holds the k most frequent; fill
 * the answer from the back as the heap empties, least frequent first. O(n log k).
 *
 * <p>The owner's submission put every distinct value in a max-heap - O(n log n). Keeping the heap at
 * size k is the stronger answer; a comment in the displayed code says so. Real HashMap iteration
 * and {@link ArrayHeap} (which sifts as java.util.PriorityQueue does) keep tie order the code's.
 */
@Component
public class TopKFrequentElementsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "top-k-frequent-elements";
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
                        .help("Duplicates are expected - frequency is the whole point.")
                        .length(1, 40).values(-100, 100)
                        .defaultValue(List.of(1, 1, 1, 2, 2, 3))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K")
                        .help("How many of the most frequent values to keep.")
                        .range(1, 20)
                        .defaultValue(2)
                        .build());
    }

    /** A different value/frequency shape entirely - four distinct values, not three. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(4, 4, 4, 6, 6, 7, 7, 7, 7), "k", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               // Changed from your submission: it kept every distinct value in a max-heap, O(n log n). A
               // min-heap by frequency trimmed to k keeps only the k most frequent - O(n log k).
               class Solution {
                   public int[] topKFrequent(int[] nums, int k) {
                       // @a count
                       int[] res=new int[k];
                       Map<Integer,Integer> map=new HashMap<>();

                       for (int val: nums)
                           map.put(val,map.getOrDefault(val,0)+1);

                       PriorityQueue<Integer> pq=new PriorityQueue<>((x,y) -> Integer.compare(map.get(x),map.get(y)));
                       for (int val: map.keySet()) {
                           // @a add
                           pq.add(val);
                           if (pq.size()>k)
                               // @a evict
                               pq.poll();
                       }

                       int pos=k-1;
                       while (pos>=0) {
                           // @a take
                           res[pos]=pq.poll();
                           pos--;
                       }

                       // @a done
                       return res;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");
        Map<Integer, Integer> map = new java.util.HashMap<>();
        for (int val : nums) map.put(val, map.getOrDefault(val, 0) + 1);
        if (k > map.size()) {
            throw new InputValidationException(Map.of("k", "can be at most the number of distinct values, " + map.size()));
        }
        int[] res = new int[k];
        emit.at("count").say("Count each value: %s.", map).var("map", map.toString()).array(nums).step();
        ArrayHeap<Integer> pq = new ArrayHeap<>((x, y) -> Integer.compare(map.get(x), map.get(y)));
        for (int val : map.keySet()) {
            int at = pq.offer(val);
            emit.at("add").say("Add %d (count %d). The least frequent kept value is on top.", val, map.get(val))
                    .var("map", map.toString()).array(slots(pq), at).step();
            if (pq.size() > k) {
                int out = pq.poll();
                emit.at("evict").say("More than %d values: drop the least frequent, %d (count %d).", k, out, map.get(out))
                        .var("map", map.toString()).array(slots(pq), 0).step();
            }
        }
        for (int pos = k - 1; pos >= 0; pos--) {
            res[pos] = pq.poll();
            emit.at("take").say("Take %d (count %d) into res[%d] - the least frequent of the rest goes last.",
                            res[pos], map.get(res[pos]), pos)
                    .var("res", java.util.Arrays.toString(res)).array(slots(pq), 0).step();
        }
        emit.at("done").say("Return %s, most frequent first.", java.util.Arrays.toString(res))
                .var("answer", java.util.Arrays.toString(res)).array(res).step();
    }

    private static int[] slots(ArrayHeap<Integer> heap) {
        int[] out = new int[heap.size()];
        for (int i = 0; i < out.length; i++) out[i] = heap.slots().get(i);
        return out;
    }
}
