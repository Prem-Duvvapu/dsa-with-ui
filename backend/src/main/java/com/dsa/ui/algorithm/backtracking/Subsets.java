package com.dsa.ui.algorithm.backtracking;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Problem: Subsets / Power Set (LeetCode 78)
 *
 * Generate all 2^N subsets of an array using Pick / Non-Pick binary decision tree.
 */
public class Subsets {

    public List<List<Integer>> solve(int[] nums, TraceRecorder recorder) {
        List<List<Integer>> res = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        List<String> callStack = new ArrayList<>();

        recorder.record(new TraceEvent(
            "start", 15,
            String.format("Subsets (Power Set): Input nums = %s (N=%d). Total subsets to generate: 2^%d = %d.",
                Arrays.toString(nums), nums.length, nums.length, (1 << nums.length)),
            Map.of("nums", Arrays.toString(nums), "N", String.valueOf(nums.length)),
            "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
        ));

        generate(0, nums, current, res, recorder, callStack);

        recorder.record(new TraceEvent(
            "complete", 40,
            String.format("Subsets Complete! Generated all %d subsets: %s", res.size(), res.toString()),
            Map.of("Total Subsets", String.valueOf(res.size()), "Subsets", res.toString()),
            "Stack", null, List.of(), Map.of(), List.of()
        ));

        return res;
    }

    private void generate(int index, int[] nums, List<Integer> current, List<List<Integer>> res,
                          TraceRecorder recorder, List<String> callStack) {
        callStack.add(String.format("solve(idx=%d)", index));

        if (index == nums.length) {
            res.add(new ArrayList<>(current));
            recorder.record(new TraceEvent(
                "subset_captured", 22,
                String.format("Leaf node reached (idx=%d)! Captured Subset: %s", index, current.toString()),
                Map.of("subset", current.toString()),
                "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
            ));
            callStack.remove(callStack.size() - 1);
            return;
        }

        // Branch 1: PICK element nums[index]
        current.add(nums[index]);
        recorder.record(new TraceEvent(
            "pick_element", 28,
            String.format("idx=%d (val=%d): PICK element %d. Current subset: %s. Recurse to idx=%d...",
                index, nums[index], nums[index], current.toString(), index + 1),
            Map.of("action", "PICK " + nums[index], "subset", current.toString()),
            "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
        ));

        generate(index + 1, nums, current, res, recorder, callStack);

        // Branch 2: NON-PICK (Backtrack & Exclude element)
        current.remove(current.size() - 1);
        recorder.record(new TraceEvent(
            "non_pick_element", 35,
            String.format("idx=%d (val=%d): NON-PICK / BACKTRACK element %d. Current subset: %s. Recurse to idx=%d...",
                index, nums[index], nums[index], current.toString(), index + 1),
            Map.of("action", "NON-PICK " + nums[index], "subset", current.toString()),
            "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
        ));

        generate(index + 1, nums, current, res, recorder, callStack);

        callStack.remove(callStack.size() - 1);
    }
}
