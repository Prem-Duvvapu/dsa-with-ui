package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Find the majority element (> N/2 times) using Moore's Voting Algorithm.
 */
@Component
public class MajorityElementTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "majority-element";
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
                        .help("Array containing a majority element appearing > N/2 times.")
                        .length(1, 40).values(-999, 999)
                        .defaultValue(List.of(2, 2, 1, 1, 1, 2, 2))
                        .build());
    }

    /** Candidate el=5 is set once at i=0 and count never resets to 0. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(5, 5, 5, 3, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public int majorityElement(int[] nums) {
                   // @a init
                   int count = 0, el = 0;
                   for (int i = 0; i < nums.length; i++) {
                       // @a elect
                       if (count == 0) {
                           count = 1; el = nums[i];
                       // @a match
                       } else if (nums[i] == el) {
                           count++;
                       // @a cancel
                       } else {
                           count--;
                       }
                   }
                   // @a done
                   return el;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int count = 0;
        int el = 0;

        emit.at("init")
                .say("Moore's Voting: Initialize count = 0, candidate el = 0.")
                .var("count", 0).var("el", 0).array(nums).step();

        for (int i = 0; i < nums.length; i++) {
            if (count == 0) {
                count = 1;
                el = nums[i];
                emit.at("elect")
                        .say("i=%d: count is 0; elect new candidate el = %d, count = 1.", i, el)
                        .var("i", i).var("candidate", el).var("count", count)
                        .array(nums, i).step();
            } else if (nums[i] == el) {
                count++;
                emit.at("match")
                        .say("i=%d: nums[%d]=%d matches candidate %d; increment count to %d.", i, i, nums[i], el, count)
                        .var("i", i).var("candidate", el).var("count", count)
                        .array(nums, i).step();
            } else {
                count--;
                emit.at("cancel")
                        .say("i=%d: nums[%d]=%d != candidate %d; decrement count to %d.", i, i, nums[i], el, count)
                        .var("i", i).var("candidate", el).var("count", count)
                        .array(nums, i).step();
            }
        }

        emit.at("done")
                .say("Moore's Voting complete. Majority element (> N/2 times) is %d.", el)
                .var("majorityElement", el).array(nums).step();
    }
}
