package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The mirror image of {@code kth-largest-element}: a size-k MAX-heap holds the k smallest
 * values seen so far, so its root is always the current k-th smallest. Shown as the plain
 * array a max-heap really keeps internally, with the heap as internal bookkeeping rather
 * than the star of the picture - a genuine binary tree here would look identical to the
 * other heap tracer's, so the array is the more honest choice for this one.
 */
@Component
public class KthSmallestElementTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "kth-smallest-element";
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
               public int findKthSmallest(int[] nums, int k) {
                   List<Integer> heap = new ArrayList<>(); // max-heap: parent >= children
                   for (int num : nums) {
                       // @a push
                       heap.add(num);
                       siftUp(heap, heap.size() - 1);
                       if (heap.size() > k) {
                           // @a evict
                           swap(heap, 0, heap.size() - 1);
                           heap.remove(heap.size() - 1);
                           siftDown(heap, 0);
                       }
                   }
                   // @a done
                   return heap.get(0);
               }

               private int siftUp(List<Integer> heap, int i) {
                   while (i > 0 && heap.get(i) > heap.get((i - 1) / 2)) {
                       swap(heap, i, (i - 1) / 2);
                       i = (i - 1) / 2;
                   }
                   return i;
               }

               private int siftDown(List<Integer> heap, int i) {
                   int n = heap.size();
                   while (true) {
                       int l = 2 * i + 1, r = 2 * i + 2, largest = i;
                       if (l < n && heap.get(l) > heap.get(largest)) largest = l;
                       if (r < n && heap.get(r) > heap.get(largest)) largest = r;
                       if (largest == i) return i;
                       swap(heap, i, largest);
                       i = largest;
                   }
               }

               private void swap(List<Integer> heap, int i, int j) {
                   Integer tmp = heap.get(i);
                   heap.set(i, heap.get(j));
                   heap.set(j, tmp);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");
        List<Integer> heap = new ArrayList<>();

        for (int num : nums) {
            heap.add(num);
            int at = siftUp(heap, heap.size() - 1);
            emit.at("push")
                    .say("Insert %d. Heap now holds %d value%s; its root, %d, is the largest "
                                    + "of them.",
                            num, heap.size(), heap.size() == 1 ? "" : "s", heap.get(0))
                    .var("inserted", num).var("heapSize", heap.size())
                    .array(toArray(heap), at).step();

            if (heap.size() > k) {
                int evicted = heap.get(0);
                swap(heap, 0, heap.size() - 1);
                heap.remove(heap.size() - 1);
                int settledAt = heap.isEmpty() ? -1 : siftDown(heap, 0);
                emit.at("evict")
                        .say("Heap exceeds size %d - evict the largest value, %d, keeping only "
                                        + "the %d smallest seen so far.",
                                k, evicted, k)
                        .var("evicted", evicted).var("heapSize", heap.size())
                        .array(toArray(heap), settledAt).step();
            }
        }

        emit.at("done")
                .say("Every value processed. The heap's root, %d, is the %d-th smallest.",
                        heap.get(0), k)
                .var("answer", heap.get(0))
                .array(toArray(heap), 0).step();
    }

    private static int[] toArray(List<Integer> heap) {
        int[] out = new int[heap.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = heap.get(i);
        }
        return out;
    }

    private static int siftUp(List<Integer> heap, int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (heap.get(i) > heap.get(parent)) {
                swap(heap, i, parent);
                i = parent;
            } else {
                break;
            }
        }
        return i;
    }

    private static int siftDown(List<Integer> heap, int i) {
        int n = heap.size();
        while (true) {
            int l = 2 * i + 1, r = 2 * i + 2, largest = i;
            if (l < n && heap.get(l) > heap.get(largest)) largest = l;
            if (r < n && heap.get(r) > heap.get(largest)) largest = r;
            if (largest == i) break;
            swap(heap, i, largest);
            i = largest;
        }
        return i;
    }

    private static void swap(List<Integer> heap, int i, int j) {
        Integer tmp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, tmp);
    }
}
