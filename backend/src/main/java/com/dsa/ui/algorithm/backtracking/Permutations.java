package com.dsa.ui.algorithm.backtracking;

import com.dsa.ui.trace.SnapshotUtil;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Problem: Permutations of Array (LeetCode 46)
 *
 * Generate all N! permutations of an array using in-place swapping.
 */
public class Permutations {

    public List<List<Integer>> solve(int[] nums, TraceRecorder recorder) {
        List<List<Integer>> res = new ArrayList<>();
        List<String> callStack = new ArrayList<>();

        recorder.record(new TraceEvent(
            "start", 15,
            String.format("Permutations: Input nums = %s. Total permutations to generate: %d! = %d.",
                Arrays.toString(nums), nums.length, factorial(nums.length)),
            Map.of("nums", Arrays.toString(nums), "N!", String.valueOf(factorial(nums.length))),
            "Array", SnapshotUtil.createArrayState(nums, -1, -1),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        permute(0, nums, res, recorder, callStack);

        recorder.record(new TraceEvent(
            "complete", 40,
            String.format("Permutations Complete! Total %d permutations generated: %s", res.size(), res.toString()),
            Map.of("Total Permutations", String.valueOf(res.size()), "Permutations", res.toString()),
            "Array", SnapshotUtil.createDetailedArrayState(nums, -1, -1, -1, nums.length),
            List.of(), Map.of(), List.of()
        ));

        return res;
    }

    private void permute(int index, int[] nums, List<List<Integer>> res, TraceRecorder recorder, List<String> callStack) {
        callStack.add(String.format("permute(idx=%d)", index));

        if (index == nums.length) {
            List<Integer> perm = new ArrayList<>();
            for (int x : nums) perm.add(x);
            res.add(perm);

            recorder.record(new TraceEvent(
                "permutation_captured", 22,
                String.format("PERMUTATION CAPTURED: %s", perm.toString()),
                Map.of("permutation", perm.toString()),
                "Array", SnapshotUtil.createDetailedArrayState(nums, -1, -1, -1, nums.length),
                new ArrayList<>(callStack), Map.of(), List.of()
            ));

            callStack.remove(callStack.size() - 1);
            return;
        }

        for (int i = index; i < nums.length; i++) {
            // Swap
            swap(nums, index, i);

            recorder.record(new TraceEvent(
                "swap_elements", 28,
                String.format("idx=%d, i=%d: SWAP nums[%d] (%d) <-> nums[%d] (%d). Current state: %s. Recurse to idx=%d...",
                    index, i, index, nums[i], i, nums[index], Arrays.toString(nums), index + 1),
                Map.of("swap", String.format("nums[%d]<->nums[%d]", index, i), "state", Arrays.toString(nums)),
                "Array", SnapshotUtil.createArrayState(nums, index, i),
                new ArrayList<>(callStack), Map.of(), List.of()
            ));

            permute(index + 1, nums, res, recorder, callStack);

            // Backtrack swap
            swap(nums, index, i);

            recorder.record(new TraceEvent(
                "backtrack_swap", 35,
                String.format("BACKTRACK SWAP at idx=%d, i=%d: Restore nums[%d] <-> nums[%d]. Restored state: %s.",
                    index, i, index, i, Arrays.toString(nums)),
                Map.of("backtrack_swap", String.format("nums[%d]<->nums[%d]", index, i), "state", Arrays.toString(nums)),
                "Array", SnapshotUtil.createArrayState(nums, index, i),
                new ArrayList<>(callStack), Map.of(), List.of()
            ));
        }

        callStack.remove(callStack.size() - 1);
    }

    private void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    private int factorial(int n) {
        int fact = 1;
        for (int i = 1; i <= n; i++) fact *= i;
        return fact;
    }
}
