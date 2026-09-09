package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * In-place swapping avoids a separate "used" set: index {@code idx} tries every element from
 * {@code idx} onward in that position by swapping it there, recurses on the next index, then
 * swaps back — so the array always returns to its original order once a branch finishes,
 * ready for the next candidate at {@code idx} to get its own honest turn.
 */
@Component
public class PermutationsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "permutations";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array (distinct values)")
                        .length(1, 5).values(-20, 20).distinct()
                        .defaultValue(List.of(1, 2, 3))
                        .build());
    }

    /** Two elements instead of three - two permutations, not six. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(7, 8));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> permute(int[] nums) {
                   List<List<Integer>> res = new ArrayList<>();
                   backtrack(nums, 0, res);
                   // @a done
                   return res;
               }

               private void backtrack(int[] nums, int idx, List<List<Integer>> res) {
                   if (idx == nums.length) {
                       // @a capture
                       res.add(box(nums));
                       return;
                   }
                   for (int i = idx; i < nums.length; i++) {
                       swap(nums, idx, i);
                       // @a swap
                       backtrack(nums, idx + 1, res);
                       swap(nums, idx, i);
                       // @a swapBack
                   }
               }""";
    }

    private List<ArrayElement> state(int[] nums, int fixedUpTo, int highlight) {
        List<ArrayElement> s = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String st = i == highlight ? "current" : i < fixedUpTo ? "sorted" : "default";
            s.add(new ArrayElement(i, nums[i], st));
        }
        return s;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        List<List<Integer>> res = new ArrayList<>();
        backtrack(nums, 0, res, emit);

        emit.at("done")
                .say("Every position fixed at every leaf. %d permutation%s generated (%d!).",
                        res.size(), res.size() == 1 ? "" : "s", nums.length)
                .var("permutations", res.size()).arrayState(state(nums, nums.length, -1)).step();
    }

    private void backtrack(int[] nums, int idx, List<List<Integer>> res, StepEmitter emit) {
        emit.push("backtrack(idx=" + idx + ")");

        if (idx == nums.length) {
            List<Integer> boxed = new ArrayList<>(nums.length);
            for (int v : nums) {
                boxed.add(v);
            }
            res.add(boxed);
            emit.at("capture")
                    .say("Every position 0..%d fixed - capture %s as permutation #%d.", idx - 1, boxed, res.size())
                    .var("permutation", boxed.toString()).var("count", res.size())
                    .arrayState(state(nums, idx, -1)).step();
            emit.pop();
            return;
        }

        for (int i = idx; i < nums.length; i++) {
            swap(nums, idx, i);
            emit.at("swap")
                    .say("Swap positions %d and %d to bring %d into position %d - recurse into position %d.",
                            idx, i, nums[idx], idx, idx + 1)
                    .var("idx", idx).var("i", i).arrayState(state(nums, idx + 1, idx)).step();

            backtrack(nums, idx + 1, res, emit);

            swap(nums, idx, i);
            emit.at("swapBack")
                    .say("Swap positions %d and %d back - restore the order before trying the next candidate.",
                            idx, i)
                    .var("idx", idx).var("i", i).arrayState(state(nums, idx, -1)).step();
        }

        emit.pop();
    }

    private void swap(int[] nums, int a, int b) {
        int tmp = nums[a];
        nums[a] = nums[b];
        nums[b] = tmp;
    }
}
