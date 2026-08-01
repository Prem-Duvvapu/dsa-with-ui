package com.dsa.ui.algorithm.sorting;

import com.dsa.ui.trace.SnapshotUtil;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Problem: Merge Sort (Divide & Conquer)
 *
 * Recursively divide array into halves, sort each half, and merge sorted halves.
 *
 * Time Complexity:  O(N log N) - log N recursive levels, O(N) merge work per level.
 * Space Complexity: O(N) auxiliary memory for temporary merge array + O(log N) stack.
 */
public class MergeSort {

    public void solve(int[] arr, TraceRecorder recorder) {
        List<String> callStack = new ArrayList<>();

        recorder.record(new TraceEvent(
            "start", 20,
            String.format("Merge Sort: Start Divide & Conquer on input array of size N = %d.", arr.length),
            Map.of("N", String.valueOf(arr.length)),
            "Array", SnapshotUtil.createArrayState(arr, -1, -1),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        mergeSortRecursive(arr, 0, arr.length - 1, recorder, callStack);

        recorder.record(new TraceEvent(
            "complete", 45,
            "Merge Sort Complete! Array is fully sorted in ascending order.",
            Map.of("Status", "SORTED"),
            "Array", SnapshotUtil.createDetailedArrayState(arr, -1, -1, -1, arr.length),
            List.of(), Map.of(), List.of()
        ));
    }

    private void mergeSortRecursive(int[] arr, int low, int high, TraceRecorder recorder, List<String> callStack) {
        String callFrame = String.format("mergeSort(%d, %d)", low, high);
        callStack.add(callFrame);

        if (low >= high) {
            recorder.record(new TraceEvent(
                "base_case", 26,
                String.format("Base Case reached: Subarray range [%d..%d] has 1 element (val=%d). Already sorted.", low, high, arr[low]),
                Map.of("low", String.valueOf(low), "high", String.valueOf(high)),
                "Array", SnapshotUtil.createArrayState(arr, low, high),
                new ArrayList<>(callStack), Map.of(), List.of()
            ));
            callStack.remove(callStack.size() - 1);
            return;
        }

        int mid = low + (high - low) / 2;

        recorder.record(new TraceEvent(
            "split", 29,
            String.format("Divide: Subarray [%d..%d] split at mid=%d -> Left [%d..%d], Right [%d..%d]", low, high, mid, low, mid, mid + 1, high),
            Map.of("low", String.valueOf(low), "mid", String.valueOf(mid), "high", String.valueOf(high)),
            "Array", SnapshotUtil.createArrayState(arr, low, high),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        // Sort left half
        mergeSortRecursive(arr, low, mid, recorder, callStack);
        // Sort right half
        mergeSortRecursive(arr, mid + 1, high, recorder, callStack);

        // Merge two sorted halves
        merge(arr, low, mid, high, recorder, callStack);

        callStack.remove(callStack.size() - 1);
    }

    private void merge(int[] arr, int low, int mid, int high, TraceRecorder recorder, List<String> callStack) {
        callStack.add(String.format("merge(%d, %d, %d)", low, mid, high));
        int[] temp = new int[high - low + 1];
        int left = low;
        int right = mid + 1;
        int k = 0;

        recorder.record(new TraceEvent(
            "merge_start", 36,
            String.format("Merge Phase: Merging sorted left [%d..%d] and sorted right [%d..%d]", low, mid, mid + 1, high),
            Map.of("left", String.valueOf(low), "right", String.valueOf(mid + 1)),
            "Array", SnapshotUtil.createArrayState(arr, left, right),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        while (left <= mid && right <= high) {
            boolean pickLeft = arr[left] <= arr[right];
            if (pickLeft) {
                recorder.record(new TraceEvent(
                    "compare", 39,
                    String.format("Compare arr[%d]=%d <= arr[%d]=%d -> TRUE. Pick left element %d.", left, arr[left], right, arr[right], arr[left]),
                    Map.of("leftVal", String.valueOf(arr[left]), "rightVal", String.valueOf(arr[right])),
                    "Array", SnapshotUtil.createArrayState(arr, left, right),
                    new ArrayList<>(callStack), Map.of(), List.of()
                ));
                temp[k++] = arr[left++];
            } else {
                recorder.record(new TraceEvent(
                    "compare", 42,
                    String.format("Compare arr[%d]=%d <= arr[%d]=%d -> FALSE. Pick right element %d.", left, arr[left], right, arr[right], arr[right]),
                    Map.of("leftVal", String.valueOf(arr[left]), "rightVal", String.valueOf(arr[right])),
                    "Array", SnapshotUtil.createArrayState(arr, left, right),
                    new ArrayList<>(callStack), Map.of(), List.of()
                ));
                temp[k++] = arr[right++];
            }
        }

        while (left <= mid) {
            temp[k++] = arr[left++];
        }

        while (right <= high) {
            temp[k++] = arr[right++];
        }

        // Copy back to original array
        for (int i = 0; i < temp.length; i++) {
            arr[low + i] = temp[i];
        }

        recorder.record(new TraceEvent(
            "merge_complete", 52,
            String.format("Merged Subarray [%d..%d] placed back into main array: sorted range [%d..%d]", low, high, low, high),
            Map.of("mergedRange", String.format("[%d..%d]", low, high)),
            "Array", SnapshotUtil.createDetailedArrayState(arr, low, high, -1, high + 1),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        callStack.remove(callStack.size() - 1);
    }
}
