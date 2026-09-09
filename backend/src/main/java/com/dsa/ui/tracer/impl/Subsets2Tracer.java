package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The same start-index capture-every-node shape as {@link PowerSetTracer}, but with an array
 * that may repeat values: sorting groups equal values together, and skipping a candidate
 * that equals the one immediately before it AT THE SAME LOOP DEPTH prevents two structurally
 * identical subsets (both "pick the first 2, skip the second" and "skip the first 2, pick
 * the second" would otherwise produce the same [..., 2] subset twice).
 */
@Component
public class Subsets2Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "subsets-2";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array (may repeat)")
                        .length(1, 8).values(-10, 10)
                        .defaultValue(List.of(1, 2, 2))
                        .build());
    }

    /** Three copies of the same value plus one distinct one - heavier deduplication. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(4, 4, 4, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> subsetsWithDup(int[] nums) {
                   Arrays.sort(nums);
                   List<List<Integer>> res = new ArrayList<>();
                   backtrack(0, nums, new ArrayList<>(), res);
                   return res;
               }

               private void backtrack(int start, int[] nums, List<Integer> path, List<List<Integer>> res) {
                   // @a capture
                   res.add(new ArrayList<>(path));
                   for (int i = start; i < nums.length; i++) {
                       if (i > start && nums[i] == nums[i - 1]) {
                           // @a skipDuplicate
                           continue;
                       }
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
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        backtrack(0, nums, new ArrayList<>(), res, emit);

        emit.at("extend")
                .say("Search tree fully explored. %d unique subset%s of %s captured.",
                        res.size(), res.size() == 1 ? "" : "s", Arrays.toString(nums))
                .var("subsets", res.size()).stack(List.of()).step();
    }

    private void backtrack(int start, int[] nums, List<Integer> path, List<List<Integer>> res, StepEmitter emit) {
        emit.push("backtrack(start=" + start + ")");

        res.add(new ArrayList<>(path));
        emit.at("capture")
                .say("Capture the current path %s as subset #%d.", path, res.size())
                .var("subset", path.toString()).var("count", res.size()).stack(path).step();

        for (int i = start; i < nums.length; i++) {
            if (i > start && nums[i] == nums[i - 1]) {
                emit.at("skipDuplicate")
                        .say("nums[%d]=%d equals the previous candidate already tried at this depth - skip it "
                                + "to avoid capturing the same subset twice.", i, nums[i])
                        .var("i", i).var("skipped", nums[i]).stack(path).step();
                continue;
            }

            path.add(nums[i]);
            emit.at("extend")
                    .say("Extend with nums[%d]=%d, then recurse from index %d.", i, nums[i], i + 1)
                    .var("added", nums[i]).stack(path).step();
            backtrack(i + 1, nums, path, res, emit);

            path.remove(path.size() - 1);
            emit.at("undo")
                    .say("Remove %d - try the next distinct candidate at this depth.", nums[i])
                    .var("removed", nums[i]).stack(path).step();
        }

        emit.pop();
    }
}
