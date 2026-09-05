package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The pick/non-pick binary decision tree: every index is either included in the path or
 * left out, and both choices get explored before the index-that-decided is undone. A leaf
 * (every index decided) is a complete subset, whichever mix of picks got it there.
 */
@Component
public class SubsetsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "subsets-i";
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

    /** Two elements instead of three - four subsets, not eight, and a shorter path each way. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(7, 8));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> subsets(int[] nums) {
                   List<List<Integer>> res = new ArrayList<>();
                   generate(0, nums, new ArrayList<>(), res);
                   // @a done
                   return res;
               }

               private void generate(int index, int[] nums, List<Integer> current, List<List<Integer>> res) {
                   if (index == nums.length) {
                       // @a capture
                       res.add(new ArrayList<>(current));
                       return;
                   }
                   current.add(nums[index]);
                   // @a pick
                   generate(index + 1, nums, current, res);
                   current.remove(current.size() - 1);
                   // @a nonPick
                   generate(index + 1, nums, current, res);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        List<List<Integer>> res = new ArrayList<>();
        List<Integer> current = new ArrayList<>();

        generate(0, nums, current, res, emit);

        emit.at("done")
                .say("Every index decided at every leaf. %d subsets generated (2^%d).", res.size(), nums.length)
                .var("subsets", res.size()).stack(List.of()).step();
    }

    private void generate(int index, int[] nums, List<Integer> current, List<List<Integer>> res,
                           StepEmitter emit) {
        emit.push("generate(idx=" + index + ")");

        if (index == nums.length) {
            res.add(new ArrayList<>(current));
            emit.at("capture")
                    .say("Index %d reached the end of the array with every earlier index already decided - "
                            + "capture the accumulated path %s as subset #%d.", index, current, res.size())
                    .var("subset", current.toString()).var("count", res.size())
                    .stack(current).step();
            emit.pop();
            return;
        }

        current.add(nums[index]);
        emit.at("pick")
                .say("At index %d: include nums[%d] = %d in the path and recurse into index %d. Current path: %s.",
                        index, index, nums[index], index + 1, current)
                .var("index", index).var("picked", nums[index]).stack(current).step();
        generate(index + 1, nums, current, res, emit);

        current.remove(current.size() - 1);
        emit.at("nonPick")
                .say("Back at index %d: undo including nums[%d] = %d, then recurse into index %d again for the "
                        + "path that leaves it out entirely. Current path: %s.",
                        index, index, nums[index], index + 1, current)
                .var("index", index).var("excluded", nums[index]).stack(current).step();
        generate(index + 1, nums, current, res, emit);

        emit.pop();
    }
}
