package com.dsa.ui.algorithm.backtracking;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Problem: Combination Sum (LeetCode 39)
 *
 * Find all unique combinations in candidates that sum to target. Infinite element reuse allowed.
 */
public class CombinationSum {

    public List<List<Integer>> solve(int[] candidates, int target, TraceRecorder recorder) {
        List<List<Integer>> res = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        List<String> callStack = new ArrayList<>();

        recorder.record(new TraceEvent(
            "start", 15,
            String.format("Combination Sum: candidates = %s, target = %d. Infinite reuse permitted.", Arrays.toString(candidates), target),
            Map.of("candidates", Arrays.toString(candidates), "target", String.valueOf(target)),
            "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
        ));

        findCombinations(0, candidates, target, current, res, recorder, callStack);

        recorder.record(new TraceEvent(
            "complete", 40,
            String.format("Combination Sum Complete! Valid combinations found: %d. Result: %s", res.size(), res.toString()),
            Map.of("Total Combinations", String.valueOf(res.size()), "Result", res.toString()),
            "Stack", null, List.of(), Map.of(), List.of()
        ));

        return res;
    }

    private void findCombinations(int idx, int[] candidates, int target, List<Integer> current,
                                  List<List<Integer>> res, TraceRecorder recorder, List<String> callStack) {
        callStack.add(String.format("find(%d, target=%d)", idx, target));

        if (idx == candidates.length) {
            if (target == 0) {
                res.add(new ArrayList<>(current));
                recorder.record(new TraceEvent(
                    "combination_match", 22,
                    String.format("TARGET MATCHED! Combination found: %s", current.toString()),
                    Map.of("combination", current.toString()),
                    "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
                ));
            }
            callStack.remove(callStack.size() - 1);
            return;
        }

        // Branch 1: Pick candidate if candidate <= target
        if (candidates[idx] <= target) {
            current.add(candidates[idx]);

            recorder.record(new TraceEvent(
                "pick_candidate", 28,
                String.format("idx=%d (val=%d <= target %d): PICK candidate %d. Remaining target = %d. Current path: %s",
                    idx, candidates[idx], target, candidates[idx], target - candidates[idx], current.toString()),
                Map.of("pick", String.valueOf(candidates[idx]), "newTarget", String.valueOf(target - candidates[idx])),
                "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
            ));

            findCombinations(idx, candidates, target - candidates[idx], current, res, recorder, callStack);

            // Backtrack
            current.remove(current.size() - 1);

            recorder.record(new TraceEvent(
                "backtrack_candidate", 35,
                String.format("BACKTRACK candidate %d. Current path: %s", candidates[idx], current.toString()),
                Map.of("backtrack", String.valueOf(candidates[idx])),
                "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
            ));
        }

        // Branch 2: Skip candidate to next index
        recorder.record(new TraceEvent(
            "skip_candidate", 38,
            String.format("idx=%d (val=%d): SKIP candidate %d. Move to next index %d...", idx, candidates[idx], candidates[idx], idx + 1),
            Map.of("skip", String.valueOf(candidates[idx]), "nextIdx", String.valueOf(idx + 1)),
            "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
        ));

        findCombinations(idx + 1, candidates, target, current, res, recorder, callStack);

        callStack.remove(callStack.size() - 1);
    }
}
