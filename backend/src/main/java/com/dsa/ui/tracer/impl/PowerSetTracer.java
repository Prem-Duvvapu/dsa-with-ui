package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A different shape from {@link SubsetsTracer}'s binary pick/non-pick tree: here every call
 * captures its current path immediately — a node IS a subset, not just its leaves — then
 * loops over every remaining index as a candidate next element to extend it with. The same
 * 2^N subsets come out, but the recursion itself never makes an explicit "leave it out"
 * choice; skipping an element is just not looping back to try it after moving past it.
 */
@Component
public class PowerSetTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "power-set";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array (distinct values)")
                        .length(1, 6).values(-20, 20).distinct()
                        .defaultValue(List.of(1, 2, 3))
                        .build());
    }

    /** Two elements instead of three - four subsets, not eight. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(7, 8));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> powerSet(int[] nums) {
                   List<List<Integer>> res = new ArrayList<>();
                   backtrack(0, nums, new ArrayList<>(), res);
                   return res;
               }

               private void backtrack(int start, int[] nums, List<Integer> path, List<List<Integer>> res) {
                   // @a capture
                   res.add(new ArrayList<>(path));
                   for (int i = start; i < nums.length; i++) {
                       path.add(nums[i]);
                       // @a extend
                       backtrack(i + 1, nums, path, res);
                       path.remove(path.size() - 1);
                       // @a undo
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        List<List<Integer>> res = new ArrayList<>();
        backtrack(0, nums, new ArrayList<>(), res, emit);

        emit.at("extend")
                .say("Every start index exhausted at every depth. %d subsets captured (2^%d).",
                        res.size(), nums.length)
                .var("subsets", res.size()).stack(List.of()).step();
    }

    private void backtrack(int start, int[] nums, List<Integer> path, List<List<Integer>> res, StepEmitter emit) {
        emit.push("backtrack(start=" + start + ")");

        res.add(new ArrayList<>(path));
        emit.at("capture")
                .say("Capture the current path %s as subset #%d before trying any further extension.",
                        path, res.size())
                .var("subset", path.toString()).var("count", res.size()).stack(path).step();

        for (int i = start; i < nums.length; i++) {
            path.add(nums[i]);
            emit.at("extend")
                    .say("Extend %s with nums[%d] = %d, then recurse from index %d.",
                            path.subList(0, path.size() - 1), i, nums[i], i + 1)
                    .var("added", nums[i]).stack(path).step();
            backtrack(i + 1, nums, path, res, emit);

            path.remove(path.size() - 1);
            emit.at("undo")
                    .say("Remove %d - try the next candidate to extend %s with instead.", nums[i], path)
                    .var("removed", nums[i]).stack(path).step();
        }

        emit.pop();
    }
}
