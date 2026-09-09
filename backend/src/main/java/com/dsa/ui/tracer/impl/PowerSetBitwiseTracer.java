package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every integer mask from 0 to 2^N - 1, read as N bits, is a distinct yes/no choice for
 * every element — bit i set means element i is included. Counting through every mask
 * therefore enumerates every subset exactly once, with no recursion and no explicit
 * include/exclude branching.
 */
@Component
public class PowerSetBitwiseTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "power-set-bitwise";
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
                        .help("Elements to enumerate every subset of.")
                        .length(1, 5).values(-99, 99)
                        .defaultValue(List.of(1, 2, 3))
                        .build());
    }

    /** A single element: only 2 subsets, exercising the mask-with-one-bit edge the default's 8 skip past. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(5));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> subsets(int[] nums) {
                   int n = nums.length, limit = 1 << n;
                   List<List<Integer>> ans = new ArrayList<>();
                   for (int mask = 0; mask < limit; mask++) {
                       // @a buildSubset
                       List<Integer> subset = new ArrayList<>();
                       for (int i = 0; i < n; i++) {
                           if ((mask & (1 << i)) != 0) subset.add(nums[i]);
                       }
                       ans.add(subset);
                   }
                   // @a done
                   return ans;
               }""";
    }

    private List<ArrayElement> maskState(int[] nums, int mask) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            boolean included = (mask & (1 << i)) != 0;
            state.add(new ArrayElement(i, nums[i], included ? "current" : "default"));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int limit = 1 << n;

        for (int mask = 0; mask < limit; mask++) {
            List<Integer> subset = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(nums[i]);
                }
            }
            emit.at("buildSubset")
                    .say("Mask %s: bit i set means nums[i] is included. Subset = %s.",
                            String.format("%" + n + "s", Integer.toBinaryString(mask)).replace(' ', '0'), subset)
                    .var("mask", mask).var("subset", subset.toString())
                    .arrayState(maskState(nums, mask)).step();
        }

        emit.at("done")
                .say("All %d masks enumerated. %d subsets generated, none by recursion.", limit, limit)
                .var("totalSubsets", limit).arrayState(maskState(nums, limit - 1)).step();
    }
}
