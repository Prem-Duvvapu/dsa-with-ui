package com.dsa.ui.algorithm.sorting;

import com.dsa.ui.model.TreeNode;
import com.dsa.ui.trace.SnapshotUtil;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.ArrayList;
import java.util.HashMap;
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

    private final Map<Integer, String> nodeStates = new HashMap<>();

    public void solve(int[] arr, TraceRecorder recorder) {
        List<String> callStack = new ArrayList<>();
        nodeStates.clear();

        recorder.record(new TraceEvent(
            "start", 20,
            String.format("Merge Sort: Start Divide & Conquer on input array of size N = %d.", arr.length),
            Map.of("N", String.valueOf(arr.length)),
            "Array", SnapshotUtil.createArrayState(arr, -1, -1),
            new ArrayList<>(callStack), new HashMap<>(nodeStates), List.of(), buildTreeNodes(arr)
        ));

        mergeSortRecursive(arr, 0, arr.length - 1, recorder, callStack);

        // Mark root as sorted
        nodeStates.put(1, "sorted");

        recorder.record(new TraceEvent(
            "complete", 45,
            "Merge Sort Complete! Array is fully sorted in ascending order.",
            Map.of("Status", "SORTED"),
            "Array", SnapshotUtil.createDetailedArrayState(arr, -1, -1, -1, arr.length),
            List.of(), new HashMap<>(nodeStates), List.of(), buildTreeNodes(arr)
        ));
    }

    private void mergeSortRecursive(int[] arr, int low, int high, TraceRecorder recorder, List<String> callStack) {
        String callFrame = String.format("mergeSort(%d, %d)", low, high);
        callStack.add(callFrame);
        int nodeId = getNodeId(low, high);

        if (low >= high) {
            nodeStates.put(nodeId, "sorted");
            recorder.record(new TraceEvent(
                "base_case", 26,
                String.format("Base Case reached: Subarray range [%d..%d] has 1 element (val=%d). Already sorted.", low, high, arr[low]),
                Map.of("low", String.valueOf(low), "high", String.valueOf(high)),
                "Array", SnapshotUtil.createArrayState(arr, low, high),
                new ArrayList<>(callStack), new HashMap<>(nodeStates), List.of(), buildTreeNodes(arr)
            ));
            callStack.remove(callStack.size() - 1);
            return;
        }

        int mid = low + (high - low) / 2;
        nodeStates.put(nodeId, "active");

        recorder.record(new TraceEvent(
            "split", 29,
            String.format("Divide: Subarray [%d..%d] split at mid=%d -> Left [%d..%d], Right [%d..%d]", low, high, mid, low, mid, mid + 1, high),
            Map.of("low", String.valueOf(low), "mid", String.valueOf(mid), "high", String.valueOf(high)),
            "Array", SnapshotUtil.createArrayState(arr, low, high),
            new ArrayList<>(callStack), new HashMap<>(nodeStates), List.of(), buildTreeNodes(arr)
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
        int nodeId = getNodeId(low, high);
        nodeStates.put(nodeId, "merging");

        int[] temp = new int[high - low + 1];
        int left = low;
        int right = mid + 1;
        int k = 0;

        recorder.record(new TraceEvent(
            "merge_start", 36,
            String.format("Merge Phase: Merging sorted left [%d..%d] and sorted right [%d..%d]", low, mid, mid + 1, high),
            Map.of("left", String.valueOf(low), "right", String.valueOf(mid + 1)),
            "Array", SnapshotUtil.createArrayState(arr, left, right),
            new ArrayList<>(callStack), new HashMap<>(nodeStates), List.of(), buildTreeNodes(arr)
        ));

        while (left <= mid && right <= high) {
            boolean pickLeft = arr[left] <= arr[right];
            if (pickLeft) {
                recorder.record(new TraceEvent(
                    "compare", 39,
                    String.format("Compare arr[%d]=%d <= arr[%d]=%d -> TRUE. Pick left element %d.", left, arr[left], right, arr[right], arr[left]),
                    Map.of("leftVal", String.valueOf(arr[left]), "rightVal", String.valueOf(arr[right])),
                    "Array", SnapshotUtil.createArrayState(arr, left, right),
                    new ArrayList<>(callStack), new HashMap<>(nodeStates), List.of(), buildTreeNodes(arr)
                ));
                temp[k++] = arr[left++];
            } else {
                recorder.record(new TraceEvent(
                    "compare", 42,
                    String.format("Compare arr[%d]=%d <= arr[%d]=%d -> FALSE. Pick right element %d.", left, arr[left], right, arr[right], arr[right]),
                    Map.of("leftVal", String.valueOf(arr[left]), "rightVal", String.valueOf(arr[right])),
                    "Array", SnapshotUtil.createArrayState(arr, left, right),
                    new ArrayList<>(callStack), new HashMap<>(nodeStates), List.of(), buildTreeNodes(arr)
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

        nodeStates.put(nodeId, "sorted");

        recorder.record(new TraceEvent(
            "merge_complete", 52,
            String.format("Merged Subarray [%d..%d] placed back into main array: sorted range [%d..%d]", low, high, low, high),
            Map.of("mergedRange", String.format("[%d..%d]", low, high)),
            "Array", SnapshotUtil.createDetailedArrayState(arr, low, high, -1, high + 1),
            new ArrayList<>(callStack), new HashMap<>(nodeStates), List.of(), buildTreeNodes(arr)
        ));

        callStack.remove(callStack.size() - 1);
    }

    private int getNodeId(int low, int high) {
        if (low == 0 && high == 5) return 1;
        if (low == 0 && high == 2) return 2;
        if (low == 3 && high == 5) return 3;
        if (low == 0 && high == 1) return 4;
        if (low == 2 && high == 2) return 5;
        if (low == 3 && high == 4) return 6;
        if (low == 5 && high == 5) return 7;
        if (low == 0 && high == 0) return 8;
        if (low == 1 && high == 1) return 9;
        if (low == 3 && high == 3) return 10;
        if (low == 4 && high == 4) return 11;
        return 1;
    }

    private List<TreeNode> buildTreeNodes(int[] arr) {
        return List.of(
            createNode(1, 0, 5, 190, 35, 2, 3, arr),
            createNode(2, 0, 2, 100, 90, 4, 5, arr),
            createNode(3, 3, 5, 280, 90, 6, 7, arr),
            createNode(4, 0, 1, 60, 145, 8, 9, arr),
            createNode(5, 2, 2, 140, 145, null, null, arr),
            createNode(6, 3, 4, 240, 145, 10, 11, arr),
            createNode(7, 5, 5, 320, 145, null, null, arr),
            createNode(8, 0, 0, 35, 200, null, null, arr),
            createNode(9, 1, 1, 85, 200, null, null, arr),
            createNode(10, 3, 3, 215, 200, null, null, arr),
            createNode(11, 4, 4, 265, 200, null, null, arr)
        );
    }

    private TreeNode createNode(int id, int low, int high, double x, double y, Integer leftId, Integer rightId, int[] arr) {
        String state = nodeStates.getOrDefault(id, "unvisited");
        StringBuilder sb = new StringBuilder();
        if (low == high) {
            sb.append("[").append(low).append("]:").append(arr[low]);
        } else {
            sb.append("[").append(low).append("..").append(high).append("]:");
            for (int i = low; i <= high; i++) {
                sb.append(arr[i]);
                if (i < high) sb.append(",");
            }
        }
        return new TreeNode(id, sb.toString(), x, y, leftId, rightId, state);
    }
}
