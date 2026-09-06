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
 * Count frequencies first, then keep only the k most frequent values in a size-k min-heap
 * ordered by frequency, not by value. The array shown on the wire carries the candidate
 * VALUES (what a viewer actually cares about); frequency - the field the heap really orders
 * by - travels alongside as a step variable instead.
 */
@Component
public class TopKFrequentElementsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "top-k-frequent-elements";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
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
               public int[] topKFrequent(int[] nums, int k) {
                   Map<Integer, Integer> freq = new LinkedHashMap<>();
                   for (int num : nums) freq.merge(num, 1, Integer::sum);

                   // @a counted
                   List<int[]> heap = new ArrayList<>(); // {value, frequency}, min-heap by frequency
                   for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
                       // @a push
                       heap.add(new int[]{e.getKey(), e.getValue()});
                       siftUp(heap, heap.size() - 1);
                       if (heap.size() > k) {
                           // @a evict
                           swap(heap, 0, heap.size() - 1);
                           heap.remove(heap.size() - 1);
                           siftDown(heap, 0);
                       }
                   }
                   // @a done
                   return sortedByFrequencyDesc(heap);
               }

               private void siftUp(List<int[]> heap, int i) {
                   while (i > 0 && less(heap.get(i), heap.get((i - 1) / 2))) {
                       swap(heap, i, (i - 1) / 2);
                       i = (i - 1) / 2;
                   }
               }

               private void siftDown(List<int[]> heap, int i) {
                   int n = heap.size();
                   while (true) {
                       int l = 2 * i + 1, r = 2 * i + 2, smallest = i;
                       if (l < n && less(heap.get(l), heap.get(smallest))) smallest = l;
                       if (r < n && less(heap.get(r), heap.get(smallest))) smallest = r;
                       if (smallest == i) return;
                       swap(heap, i, smallest);
                       i = smallest;
                   }
               }

               // Ordered by frequency ascending; ties broken by value for a deterministic shape.
               private boolean less(int[] a, int[] b) {
                   return a[1] != b[1] ? a[1] < b[1] : a[0] < b[0];
               }

               private void swap(List<int[]> heap, int i, int j) {
                   int[] tmp = heap.get(i);
                   heap.set(i, heap.get(j));
                   heap.set(j, tmp);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");

        Map<Integer, Integer> freq = new LinkedHashMap<>();
        for (int num : nums) {
            freq.merge(num, 1, Integer::sum);
        }

        emit.at("counted")
                .say("Counted frequencies for %d distinct value%s: %s.",
                        freq.size(), freq.size() == 1 ? "" : "s", freq)
                .var("frequencies", freq.toString())
                .array(nums).step();

        List<int[]> heap = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
            int value = e.getKey();
            int count = e.getValue();
            heap.add(new int[]{value, count});
            int at = siftUp(heap, heap.size() - 1);
            emit.at("push")
                    .say("Push %d (frequency %d) onto the heap - it now holds %d candidate%s.",
                            value, count, heap.size(), heap.size() == 1 ? "" : "s")
                    .var("value", value).var("frequency", count).var("heapSize", heap.size())
                    .array(valuesOf(heap), at).step();

            if (heap.size() > k) {
                int[] evicted = heap.get(0);
                swap(heap, 0, heap.size() - 1);
                heap.remove(heap.size() - 1);
                int settledAt = heap.isEmpty() ? -1 : siftDown(heap, 0);
                emit.at("evict")
                        .say("More than %d candidates - drop %d (least frequent, at %d), "
                                        + "keeping only the top %d so far.",
                                k, evicted[0], evicted[1], k)
                        .var("evicted", evicted[0]).var("evictedFrequency", evicted[1])
                        .array(valuesOf(heap), settledAt).step();
            }
        }

        int[] answer = sortedByFrequencyDesc(heap);
        emit.at("done")
                .say("Heap holds exactly the %d most frequent values: %s.", k, Arrays.toString(answer))
                .var("answer", Arrays.toString(answer))
                .array(answer).step();
    }

    private static int[] sortedByFrequencyDesc(List<int[]> heap) {
        return heap.stream()
                .sorted((a, b) -> b[1] != a[1] ? Integer.compare(b[1], a[1]) : Integer.compare(a[0], b[0]))
                .mapToInt(pair -> pair[0])
                .toArray();
    }

    private static int[] valuesOf(List<int[]> heap) {
        int[] out = new int[heap.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = heap.get(i)[0];
        }
        return out;
    }

    private static int siftUp(List<int[]> heap, int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (less(heap.get(i), heap.get(parent))) {
                swap(heap, i, parent);
                i = parent;
            } else {
                break;
            }
        }
        return i;
    }

    private static int siftDown(List<int[]> heap, int i) {
        int n = heap.size();
        while (true) {
            int l = 2 * i + 1, r = 2 * i + 2, smallest = i;
            if (l < n && less(heap.get(l), heap.get(smallest))) smallest = l;
            if (r < n && less(heap.get(r), heap.get(smallest))) smallest = r;
            if (smallest == i) break;
            swap(heap, i, smallest);
            i = smallest;
        }
        return i;
    }

    /** Ordered by frequency ascending; ties broken by value for a deterministic heap shape. */
    private static boolean less(int[] a, int[] b) {
        return a[1] != b[1] ? a[1] < b[1] : a[0] < b[0];
    }

    private static void swap(List<int[]> heap, int i, int j) {
        int[] tmp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, tmp);
    }
}
