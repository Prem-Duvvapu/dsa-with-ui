package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TwoSumTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "two-sum";
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
                        .help("The numbers to search.")
                        .length(2, 40).values(-999, 999)
                        .defaultValue(List.of(2, 7, 11, 15))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target sum")
                        .range(-2000, 2000)
                        .defaultValue(9)
                        .build());
    }

    /** No pair sums to 20, so this is the input that reaches the not-found branch. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(3, 2, 4, 8, 1), "target", 20);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] twoSum(int[] nums, int target) {
                   // @a init
                   Map<Integer, Integer> seen = new HashMap<>();
                   for (int i = 0; i < nums.length; i++) {
                       // @a complement
                       int need = target - nums[i];
                       if (seen.containsKey(need)) {
                           // @a found
                           return new int[] { seen.get(need), i };
                       }
                       // @a remember
                       seen.put(nums[i], i);
                   }
                   // @a none
                   return new int[0];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int target = in.getInt("target");
        Map<Integer, Integer> seen = new HashMap<>();

        emit.at("init").say("Start with an empty map. Looking for two numbers adding to %d.", target)
                .var("target", target).array(nums).step();

        for (int i = 0; i < nums.length; i++) {
            int need = target - nums[i];

            emit.at("complement")
                    .say("i = %d: nums[%d] = %d, so we need %d - %d = %d.", i, i, nums[i], target, nums[i], need)
                    .var("i", i).var("nums[i]", nums[i]).var("need", need).var("seen", seen)
                    .array(nums, i).step();

            if (seen.containsKey(need)) {
                int j = seen.get(need);
                emit.at("found")
                        .say("Found it: %d is already at index %d. %d + %d = %d.", need, j, nums[j], nums[i], target)
                        .var("answer", "[" + j + ", " + i + "]")
                        .array(nums, i, j).step();
                return;
            }

            emit.at("remember").say("%d is not in the map yet. Record nums[%d] = %d.", need, i, nums[i])
                    .var("i", i).var("seen", seen).array(nums, i).step();
            seen.put(nums[i], i);
        }

        emit.at("none").say("Scanned the whole array with no pair summing to %d.", target)
                .var("result", "[]").array(nums).step();
    }
}
