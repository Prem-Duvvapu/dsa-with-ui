package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Generalizes {@link ThreeSumTracer} by one more index: after sorting, fix two indices i
 * and j with nested loops, then close a two-pointer scan over what remains for the target
 * sum. Each of the four positions gets its own duplicate-skip - i and j guard the nested
 * loops, k and l guard the two-pointer close - because a repeated value at any one of them
 * would otherwise report the same quadruplet more than once.
 */
@Component
public class FourSumTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "four-sum";
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
                        .help("Finds every unique quadruplet summing to the target.")
                        .length(1, 10).values(-50, 50)
                        .defaultValue(List.of(1, 0, -1, 0, -2, 2))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target sum")
                        .range(-100, 100)
                        .defaultValue(0)
                        .build());
    }

    /** Different target and enough repeats to hit both k/l duplicate-skip branches this default misses. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "nums", List.of(-3, -3, -3, -3, -2, 3, 3),
                "target", -6);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> fourSum(int[] nums, int target) {
                   // @a sort
                   Arrays.sort(nums);
                   List<List<Integer>> ans = new ArrayList<>();
                   int n = nums.length;

                   for (int i = 0; i < n; i++) {
                       if (i > 0 && nums[i] == nums[i - 1]) {
                           // @a iSkipDup
                           continue;
                       }
                       for (int j = i + 1; j < n; j++) {
                           if (j > i + 1 && nums[j] == nums[j - 1]) {
                               // @a jSkipDup
                               continue;
                           }
                           int k = j + 1, l = n - 1;
                           while (k < l) {
                               long sum = (long) nums[i] + nums[j] + nums[k] + nums[l];
                               if (sum == target) {
                                   // @a sumTarget
                                   ans.add(List.of(nums[i], nums[j], nums[k], nums[l]));
                                   k++; l--;
                                   while (k < l && nums[k] == nums[k - 1]) {
                                       // @a kSkipDupLeft
                                       k++;
                                   }
                                   while (k < l && nums[l] == nums[l + 1]) {
                                       // @a kSkipDupRight
                                       l--;
                                   }
                               } else if (sum < target) {
                                   // @a sumLess
                                   k++;
                               } else {
                                   // @a sumMore
                                   l--;
                               }
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
        int target = in.getInt("target");
        Arrays.sort(nums);
        int n = nums.length;
        List<List<Integer>> ans = new ArrayList<>();

        emit.at("sort")
                .say("Sort the array first: %s. Target sum is %d.", Arrays.toString(nums), target)
                .array(nums).step();

        for (int i = 0; i < n; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) {
                emit.at("iSkipDup")
                        .say("nums[%d]=%d repeats nums[%d] - skip to avoid a duplicate quadruplet set.",
                                i, nums[i], i - 1)
                        .array(nums, i).step();
                continue;
            }
            for (int j = i + 1; j < n; j++) {
                if (j > i + 1 && nums[j] == nums[j - 1]) {
                    emit.at("jSkipDup")
                            .say("nums[%d]=%d repeats nums[%d] - skip to avoid a duplicate quadruplet set.",
                                    j, nums[j], j - 1)
                            .array(nums, i, j).step();
                    continue;
                }
                int k = j + 1, l = n - 1;
                while (k < l) {
                    long sum = (long) nums[i] + nums[j] + nums[k] + nums[l];
                    if (sum == target) {
                        ans.add(List.of(nums[i], nums[j], nums[k], nums[l]));
                        emit.at("sumTarget")
                                .say("nums[%d]+nums[%d]+nums[%d]+nums[%d] = %d = target. Quadruplet found: %s.",
                                        i, j, k, l, sum, List.of(nums[i], nums[j], nums[k], nums[l]))
                                .var("quadruplets", ans.size())
                                .array(nums, k, l).step();
                        k++;
                        l--;
                        while (k < l && nums[k] == nums[k - 1]) {
                            emit.at("kSkipDupLeft")
                                    .say("nums[%d]=%d repeats the value just used - advance past it.",
                                            k, nums[k])
                                    .array(nums, k, l).step();
                            k++;
                        }
                        while (k < l && nums[l] == nums[l + 1]) {
                            emit.at("kSkipDupRight")
                                    .say("nums[%d]=%d repeats the value just used - retreat past it.",
                                            l, nums[l])
                                    .array(nums, k, l).step();
                            l--;
                        }
                    } else if (sum < target) {
                        emit.at("sumLess")
                                .say("nums[%d]+nums[%d]+nums[%d]+nums[%d] = %d < target - move k right.",
                                        i, j, k, l, sum)
                                .array(nums, k, l).step();
                        k++;
                    } else {
                        emit.at("sumMore")
                                .say("nums[%d]+nums[%d]+nums[%d]+nums[%d] = %d > target - move l left.",
                                        i, j, k, l, sum)
                                .array(nums, k, l).step();
                        l--;
                    }
                }
            }
        }

        emit.at("done")
                .say("Scan complete. Found %d unique quadruplet%s.", ans.size(), ans.size() == 1 ? "" : "s")
                .var("answer", ans.toString())
                .array(nums).step();
    }
}
