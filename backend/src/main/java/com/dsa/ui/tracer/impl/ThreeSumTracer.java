package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Finds every unique triplet summing to zero by sorting first, then fixing one index and
 * closing a two-pointer scan over the rest - the same "sort, then converge from both ends"
 * idea {@link com.dsa.ui.tracer.impl.MergeTwoSortedArraysTracer} uses for a different
 * purpose. Sorting turns "does some pair sum to a target" into a monotone search: moving
 * the left pointer only ever increases the running sum, moving the right pointer only ever
 * decreases it, so neither pointer needs to backtrack.
 */
@Component
public class ThreeSumTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "three-sum";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("Finds every unique triplet that sums to zero.")
                        .length(1, 12).values(-100, 100)
                        .defaultValue(List.of(-1, 0, 1, 2, -1, -4))
                        .build());
    }

    /** Many repeated values: exercises the outer AND both inner duplicate-skip branches. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(-4, 0, 0, 1, 4, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> threeSum(int[] nums) {
                   // @a sort
                   Arrays.sort(nums);
                   List<List<Integer>> ans = new ArrayList<>();
                   int n = nums.length;

                   for (int i = 0; i < n; i++) {
                       if (i > 0 && nums[i] == nums[i - 1]) {
                           // @a outerSkipDup
                           continue;
                       }
                       int j = i + 1, k = n - 1;
                       while (j < k) {
                           int sum = nums[i] + nums[j] + nums[k];
                           if (sum == 0) {
                               // @a sumZero
                               ans.add(List.of(nums[i], nums[j], nums[k]));
                               j++; k--;
                               while (j < k && nums[j] == nums[j - 1]) {
                                   // @a innerSkipDupLeft
                                   j++;
                               }
                               while (j < k && nums[k] == nums[k + 1]) {
                                   // @a innerSkipDupRight
                                   k--;
                               }
                           } else if (sum < 0) {
                               // @a sumNeg
                               j++;
                           } else {
                               // @a sumPos
                               k--;
                           }
                       }
                   }
                   // @a done
                   return ans;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        Arrays.sort(nums);
        int n = nums.length;
        List<List<Integer>> ans = new ArrayList<>();

        emit.at("sort")
                .say("Sort the array first: %s. Now a fixed i plus a two-pointer scan can "
                        + "find every pair summing to -nums[i] without backtracking.",
                        Arrays.toString(nums))
                .array(nums).step();

        for (int i = 0; i < n; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) {
                emit.at("outerSkipDup")
                        .say("nums[%d]=%d repeats nums[%d] - skipping to avoid a duplicate triplet set.",
                                i, nums[i], i - 1)
                        .array(nums, i).step();
                continue;
            }
            int j = i + 1, k = n - 1;
            while (j < k) {
                int sum = nums[i] + nums[j] + nums[k];
                if (sum == 0) {
                    ans.add(List.of(nums[i], nums[j], nums[k]));
                    emit.at("sumZero")
                            .say("nums[%d]+nums[%d]+nums[%d] = %d+%d+%d = 0. Triplet found: %s.",
                                    i, j, k, nums[i], nums[j], nums[k],
                                    List.of(nums[i], nums[j], nums[k]))
                            .var("triplets", ans.size())
                            .array(nums, j, k).step();
                    j++;
                    k--;
                    while (j < k && nums[j] == nums[j - 1]) {
                        emit.at("innerSkipDupLeft")
                                .say("nums[%d]=%d repeats the value just used - advance past it.",
                                        j, nums[j])
                                .array(nums, j, k).step();
                        j++;
                    }
                    while (j < k && nums[k] == nums[k + 1]) {
                        emit.at("innerSkipDupRight")
                                .say("nums[%d]=%d repeats the value just used - retreat past it.",
                                        k, nums[k])
                                .array(nums, j, k).step();
                        k--;
                    }
                } else if (sum < 0) {
                    emit.at("sumNeg")
                            .say("nums[%d]+nums[%d]+nums[%d] = %d < 0 - move the left pointer right.",
                                    i, j, k, sum)
                            .array(nums, j, k).step();
                    j++;
                } else {
                    emit.at("sumPos")
                            .say("nums[%d]+nums[%d]+nums[%d] = %d > 0 - move the right pointer left.",
                                    i, j, k, sum)
                            .array(nums, j, k).step();
                    k--;
                }
            }
        }

        emit.at("done")
                .say("Scan complete. Found %d unique triplet%s.", ans.size(), ans.size() == 1 ? "" : "s")
                .var("answer", ans.toString())
                .array(nums).step();
    }
}
