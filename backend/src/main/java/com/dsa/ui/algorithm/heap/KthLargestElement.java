package com.dsa.ui.algorithm.heap;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.*;

/**
 * Problem: Kth Largest Element in an Array (Min-Heap / PriorityQueue)
 *
 * Find Kth largest element using Min-Heap of size K.
 */
public class KthLargestElement {

    public int solve(int[] nums, int k, TraceRecorder recorder) {
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        recorder.record(new TraceEvent(
            "start", 12,
            String.format("Kth Largest Element: Find %d-th largest in %s using Min-Heap of max size %d.", k, Arrays.toString(nums), k),
            Map.of("nums", Arrays.toString(nums), "k", String.valueOf(k)),
            "PriorityQueue", null, getHeapState(minHeap), Map.of(), List.of()
        ));

        for (int i = 0; i < nums.length; i++) {
            minHeap.add(nums[i]);

            recorder.record(new TraceEvent(
                "push_heap", 18,
                String.format("Element nums[%d] = %d: Push to Min-Heap. Current Heap: %s", i, nums[i], minHeap.toString()),
                Map.of("pushed", String.valueOf(nums[i]), "heapSize", String.valueOf(minHeap.size())),
                "PriorityQueue", null, getHeapState(minHeap), Map.of(), List.of()
            ));

            if (minHeap.size() > k) {
                int popped = minHeap.poll();
                recorder.record(new TraceEvent(
                    "pop_min", 22,
                    String.format("Heap size (%d) > k (%d): Extract Min element %d. Remaining Heap: %s", minHeap.size() + 1, k, popped, minHeap.toString()),
                    Map.of("extractedMin", String.valueOf(popped), "heapSize", String.valueOf(minHeap.size())),
                    "PriorityQueue", null, getHeapState(minHeap), Map.of(), List.of()
                ));
            }
        }

        int kthLargest = minHeap.peek();

        recorder.record(new TraceEvent(
            "complete", 30,
            String.format("Kth Largest Element Complete! Top of Min-Heap (peek) = %d is the %d-th largest element.", kthLargest, k),
            Map.of("KthLargest", String.valueOf(kthLargest)),
            "PriorityQueue", null, getHeapState(minHeap), Map.of(), List.of()
        ));

        return kthLargest;
    }

    private List<String> getHeapState(PriorityQueue<Integer> pq) {
        List<String> list = new ArrayList<>();
        for (Integer val : pq) list.add(String.valueOf(val));
        return list;
    }
}
