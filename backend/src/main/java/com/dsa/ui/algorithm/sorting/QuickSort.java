package com.dsa.ui.algorithm.sorting;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.*;

/**
 * Problem: Quick Sort (Lomuto Partitioning - Pivot = arr[low])
 *
 * Pick pivot = arr[low]. Maintain index `i = low` for smaller elements.
 * Traverse `j` from `low + 1` to `high`. If arr[j] < pivot, increment `i` and swap arr[i] and arr[j].
 * Finally, swap arr[low] with arr[i] to place pivot in its correct sorted position.
 *
 * Time Complexity:  O(N log N) Average/Best case, O(N^2) Worst case.
 * Space Complexity: O(log N) Call stack depth.
 */
public class QuickSort {

    private final Set<Integer> sortedIndices = new HashSet<>();

    public void solve(int[] arr, TraceRecorder recorder) {
        List<String> callStack = new ArrayList<>();
        sortedIndices.clear();

        recorder.record(new TraceEvent(
            "start", 4,
            String.format("Quick Sort: Start partitioning input array of size N = %d (Lomuto Partitioning, Pivot = arr[low]).", arr.length),
            Map.of("N", String.valueOf(arr.length)),
            "Array", createArraySnapshot(arr, -1, -1, -1),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        quickSortRecursive(arr, 0, arr.length - 1, recorder, callStack);

        for (int i = 0; i < arr.length; i++) sortedIndices.add(i);

        recorder.record(new TraceEvent(
            "complete", 7,
            "Quick Sort Complete! All elements partitioned and placed in final sorted positions.",
            Map.of("Status", "SORTED"),
            "Array", createArraySnapshot(arr, -1, -1, -1),
            List.of(), Map.of(), List.of()
        ));
    }

    private void quickSortRecursive(int[] arr, int low, int high, TraceRecorder recorder, List<String> callStack) {
        if (low > high) return;

        if (low == high) {
            sortedIndices.add(low);
            String callFrame = String.format("quickSort(%d, %d)", low, high);
            callStack.add(callFrame);
            recorder.record(new TraceEvent(
                "base_case", 5,
                String.format("Base Case: Subarray [%d..%d] has 1 element (val=%d). Element at index %d is sorted.", low, high, arr[low], low),
                Map.of("low", String.valueOf(low), "high", String.valueOf(high)),
                "Array", createArraySnapshot(arr, -1, -1, -1),
                new ArrayList<>(callStack), Map.of(), List.of()
            ));
            callStack.remove(callStack.size() - 1);
            return;
        }

        String callFrame = String.format("quickSort(%d, %d)", low, high);
        callStack.add(callFrame);

        recorder.record(new TraceEvent(
            "call_partition", 6,
            String.format("QuickSort Call: Subarray range [%d..%d]. Preparing Lomuto partition with pivot = arr[%d] (%d).", low, high, low, arr[low]),
            Map.of("low", String.valueOf(low), "high", String.valueOf(high), "pivot", String.valueOf(arr[low])),
            "Array", createArraySnapshot(arr, low, -1, -1),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        int pIndex = partition(arr, low, high, recorder, callStack);
        sortedIndices.add(pIndex);

        recorder.record(new TraceEvent(
            "pivot_placed", 7,
            String.format("Pivot Placed: Pivot element %d is fixed at its sorted index %d.", arr[pIndex], pIndex),
            Map.of("pIndex", String.valueOf(pIndex), "pivotVal", String.valueOf(arr[pIndex])),
            "Array", createArraySnapshot(arr, pIndex, -1, -1),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        quickSortRecursive(arr, low, pIndex - 1, recorder, callStack);
        quickSortRecursive(arr, pIndex + 1, high, recorder, callStack);

        callStack.remove(callStack.size() - 1);
    }

    private int partition(int[] arr, int low, int high, TraceRecorder recorder, List<String> callStack) {
        String partitionFrame = String.format("partition(%d, %d)", low, high);
        callStack.add(partitionFrame);

        int pivot = arr[low];
        int i = low;

        recorder.record(new TraceEvent(
            "partition_init", 12,
            String.format("Lomuto Partition: Set pivot = arr[%d] (%d). Initialize index i = %d. Traverse j from %d to %d.", low, pivot, i, low + 1, high),
            Map.of("pivot", String.valueOf(pivot), "i", String.valueOf(i), "low", String.valueOf(low), "high", String.valueOf(high)),
            "Array", createArraySnapshot(arr, low, i, -1),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        for (int j = low + 1; j <= high; j++) {
            boolean isSmaller = arr[j] < pivot;
            if (isSmaller) {
                i++;
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;

                recorder.record(new TraceEvent(
                    "compare_swap", 17,
                    String.format("Compare arr[j=%d] (%d) < pivot (%d): TRUE! Increment i -> %d, Swap arr[%d] and arr[%d]. Array: %s.", j, arr[i], pivot, i, i, j, Arrays.toString(arr)),
                    Map.of("j", String.valueOf(j), "i", String.valueOf(i), "swap", String.format("%d <-> %d", temp, arr[i])),
                    "Array", createArraySnapshot(arr, low, i, j),
                    new ArrayList<>(callStack), Map.of(), List.of()
                ));
            } else {
                recorder.record(new TraceEvent(
                    "compare_skip", 16,
                    String.format("Compare arr[j=%d] (%d) < pivot (%d): FALSE. Keep i = %d, move to next element.", j, arr[j], pivot, i),
                    Map.of("j", String.valueOf(j), "arr[j]", String.valueOf(arr[j]), "pivot", String.valueOf(pivot)),
                    "Array", createArraySnapshot(arr, low, i, j),
                    new ArrayList<>(callStack), Map.of(), List.of()
                ));
            }
        }

        // Swap pivot arr[low] with arr[i]
        int temp = arr[low];
        arr[low] = arr[i];
        arr[i] = temp;

        recorder.record(new TraceEvent(
            "swap_pivot", 21,
            String.format("Lomuto Partition Complete: Swap pivot arr[low=%d] (%d) with arr[i=%d] (%d). Pivot %d is now in sorted position at index %d.", low, temp, i, arr[i], arr[i], i),
            Map.of("pivotIndex", String.valueOf(i), "pivotVal", String.valueOf(arr[i])),
            "Array", createArraySnapshot(arr, i, low, i),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        callStack.remove(callStack.size() - 1);
        return i;
    }

    private List<ArrayElement> createArraySnapshot(int[] arr, int pivotIdx, int iIdx, int jIdx) {
        List<ArrayElement> list = new ArrayList<>();
        for (int idx = 0; idx < arr.length; idx++) {
            String state = "default";
            if (sortedIndices.contains(idx)) {
                state = "sorted";
            } else if (idx == pivotIdx) {
                state = "pivot";
            } else if (idx == iIdx || idx == jIdx) {
                state = "comparing";
            }
            list.add(new ArrayElement(idx, arr[idx], state));
        }
        return list;
    }
}
