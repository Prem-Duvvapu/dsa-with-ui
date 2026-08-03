package com.dsa.ui.algorithm.binarysearch;

import com.dsa.ui.trace.SnapshotUtil;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.Map;

/**
 * Problem: Search in Rotated Sorted Array (Binary Search)
 *
 * Given a rotated sorted array of distinct integers, find the index of target in O(log N) time.
 *
 * Approach: At any step mid, at least one half [low..mid] or [mid..high] must be sorted.
 * Determine which half is sorted and check if target lies within its bounds to eliminate half the array.
 *
 * Time Complexity:  O(log N) - Binary Search eliminates half the remaining elements each step.
 * Space Complexity: O(1) - Iterative binary search using two pointers.
 */
public class RotatedSortedArraySearch {

    public int solve(int[] arr, int target, TraceRecorder recorder) {
        int low = 0;
        int high = arr.length - 1;

        recorder.record(new TraceEvent(
            "start", 20,
            String.format("Search in Rotated Sorted Array: target = %d, range [%d..%d]. Input array: %s", target, low, high, java.util.Arrays.toString(arr)),
            Map.of("target", String.valueOf(target), "low", "0", "high", String.valueOf(high)),
            "Array", SnapshotUtil.createDetailedArrayState(arr, low, -1, high, 0)
        ));

        while (low <= high) {
            int mid = low + (high - low) / 2;

            recorder.record(new TraceEvent(
                "calc_mid", 25,
                String.format("Calculate mid = low + (high - low) / 2 = %d + (%d - %d) / 2 = %d (val = %d). Compare with target %d.", low, high, low, mid, arr[mid], target),
                Map.of("low", String.valueOf(low), "mid", String.valueOf(mid), "high", String.valueOf(high), "arr[mid]", String.valueOf(arr[mid])),
                "Array", SnapshotUtil.createDetailedArrayState(arr, low, mid, high, 0)
            ));

            if (arr[mid] == target) {
                recorder.record(new TraceEvent(
                    "found", 28,
                    String.format("TARGET FOUND! arr[mid=%d] == target (%d). Search complete!", mid, target),
                    Map.of("Found Index", String.valueOf(mid), "target", String.valueOf(target)),
                    "Array", SnapshotUtil.createDetailedArrayState(arr, -1, mid, -1, arr.length)
                ));
                return mid;
            }

            // Check if left half is sorted
            if (arr[low] <= arr[mid]) {
                if (target >= arr[low] && target < arr[mid]) {
                    recorder.record(new TraceEvent(
                        "eliminate_right", 34,
                        String.format("Left half [%d..%d] is sorted (%d <= %d). Target %d lies in left half [%d <= %d < %d]. Set high = mid - 1 = %d.", low, mid, arr[low], arr[mid], target, arr[low], target, arr[mid], mid - 1),
                        Map.of("low", String.valueOf(low), "high", String.valueOf(mid - 1)),
                        "Array", SnapshotUtil.createDetailedArrayState(arr, low, mid, high, 0)
                    ));
                    high = mid - 1;
                } else {
                    recorder.record(new TraceEvent(
                        "eliminate_left", 38,
                        String.format("Left half [%d..%d] is sorted (%d <= %d). Target %d DOES NOT lie in left half. Set low = mid + 1 = %d.", low, mid, arr[low], arr[mid], target, mid + 1),
                        Map.of("low", String.valueOf(mid + 1), "high", String.valueOf(high)),
                        "Array", SnapshotUtil.createDetailedArrayState(arr, low, mid, high, 0)
                    ));
                    low = mid + 1;
                }
            } else { // Right half is sorted
                if (target > arr[mid] && target <= arr[high]) {
                    recorder.record(new TraceEvent(
                        "eliminate_left", 44,
                        String.format("Right half [%d..%d] is sorted (%d <= %d). Target %d lies in right half [%d < %d <= %d]. Set low = mid + 1 = %d.", mid, high, arr[mid], arr[high], target, arr[mid], target, arr[high], mid + 1),
                        Map.of("low", String.valueOf(mid + 1), "high", String.valueOf(high)),
                        "Array", SnapshotUtil.createDetailedArrayState(arr, low, mid, high, 0)
                    ));
                    low = mid + 1;
                } else {
                    recorder.record(new TraceEvent(
                        "eliminate_right", 48,
                        String.format("Right half [%d..%d] is sorted (%d <= %d). Target %d DOES NOT lie in right half. Set high = mid - 1 = %d.", mid, high, arr[mid], arr[high], target, mid - 1),
                        Map.of("low", String.valueOf(low), "high", String.valueOf(mid - 1)),
                        "Array", SnapshotUtil.createDetailedArrayState(arr, low, mid, high, 0)
                    ));
                    high = mid - 1;
                }
            }
        }

        recorder.record(new TraceEvent(
            "not_found", 54,
            String.format("Target %d NOT FOUND in rotated sorted array. Search terminated with low > high.", target),
            Map.of("Result", "-1", "target", String.valueOf(target)),
            "Array", SnapshotUtil.createDetailedArrayState(arr, -1, -1, -1, 0)
        ));

        return -1;
    }
}
