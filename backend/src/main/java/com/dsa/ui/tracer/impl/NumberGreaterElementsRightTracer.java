package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Number of greater elements to the right for each position.
 *
 * <p>Brute-force O(N²) counting: for each index i, count how many elements in
 * i+1..n-1 are strictly greater than nums[i]. The trace shows both pointers
 * and the running count.
 */
@Component
public class NumberGreaterElementsRightTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "number-greater-elements-right";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Values")
                        .help("For each element, count how many elements to its right are strictly greater.")
                        .length(1, 16).values(-999, 999)
                        .defaultValue(List.of(3, 4, 2, 7, 5, 8, 10, 6))
                        .build());
    }

    /** All equal: every count is 0, unlike the default which has varying counts. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(5, 5, 5, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] countGreaterRight(int[] nums) {
                   int n = nums.length;
                   // @a init
                   int[] result = new int[n];
                   for (int i = 0; i < n; i++) {
                       int count = 0;
                       for (int j = i + 1; j < n; j++) {
                           if (nums[j] > nums[i]) {
                               // @a found
                               count++;
                           }
                       }
                       // @a record
                       result[i] = count;
                   }
                   // @a done
                   return result;
               }""";
    }

    private List<ArrayElement> state(int[] nums, int i, int j) {
        List<ArrayElement> s = new ArrayList<>(nums.length);
        for (int k = 0; k < nums.length; k++) {
            String st = k == i ? "current" : k == j ? "target" : k < i ? "sorted" : "default";
            s.add(new ArrayElement(k, nums[k], st));
        }
        return s;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int[] result = new int[n];

        emit.at("init")
                .say("Count greater elements to the right for each index in %s.", Arrays.toString(nums))
                .var("n", n)
                .arrayState(state(nums, -1, -1)).step();

        for (int i = 0; i < n; i++) {
            int count = 0;
            for (int j = i + 1; j < n; j++) {
                if (nums[j] > nums[i]) {
                    count++;
                    emit.at("found")
                            .say("nums[%d]=%d > nums[%d]=%d — count for index %d is now %d.",
                                    j, nums[j], i, nums[i], i, count)
                            .var("i", i).var("j", j).var("count", count)
                            .arrayState(state(nums, i, j)).step();
                }
            }
            result[i] = count;
            emit.at("record")
                    .say("Index %d (value %d): %d greater elements to the right.",
                            i, nums[i], count)
                    .var("i", i).var("result[i]", count)
                    .arrayState(state(nums, i, -1)).step();
        }

        emit.at("done")
                .say("Complete. Counts: %s.", Arrays.toString(result))
                .var("answer", Arrays.toString(result))
                .arrayState(state(nums, -1, -1)).step();
    }
}
