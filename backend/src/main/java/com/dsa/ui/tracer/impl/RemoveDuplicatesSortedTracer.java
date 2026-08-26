package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * In-place deduplication on a sorted array using two pointers:
 * i tracks the boundary of unique elements packed so far, and j scans forward.
 */
@Component
public class RemoveDuplicatesSortedTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "remove-duplicates-sorted";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("Sorted array to remove duplicates in-place.")
                        .sorted()
                        .length(1, 40).values(-999, 999)
                        .defaultValue(List.of(0, 0, 1, 1, 1, 2, 2, 3, 3, 4))
                        .build());
    }

    /** All-unique sorted array: the duplicate branch never fires and every element writes forward. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3, 4, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public int removeDuplicates(int[] nums) {
                   // @a init
                   int i = 0;
                   for (int j = 1; j < nums.length; j++) {
                       // @a compare
                       if (nums[j] != nums[i]) {
                           // @a write
                           i++;
                           nums[i] = nums[j];
                       }
                   }
                   // @a done
                   return i + 1;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int i = 0;

        emit.using("Array");
        emit.at("init")
                .say("i=0 tracks the last unique element (nums[0]=%d). j will scan from index 1.", nums[0])
                .var("i", 0).array(nums, 0).step();

        for (int j = 1; j < nums.length; j++) {
            if (nums[j] != nums[i]) {
                emit.at("compare")
                        .say("j=%d: nums[%d]=%d != nums[i=%d]=%d (new unique element).", j, j, nums[j], i, nums[i])
                        .var("i", i).var("j", j).array(nums, j, i).step();
                i++;
                nums[i] = nums[j];
                emit.at("write")
                        .say("Advance i to %d and write nums[%d] = %d.", i, i, nums[i])
                        .var("i", i).var("j", j).array(nums, i, j).step();
            } else {
                emit.at("compare")
                        .say("j=%d: nums[%d]=%d == nums[i=%d]=%d (duplicate, skip).", j, j, nums[j], i, nums[i])
                        .var("i", i).var("j", j).array(nums, j, i).step();
            }
        }

        emit.at("done")
                .say("Deduplication complete: %d unique elements (packed at indices 0..%d).", i + 1, i)
                .var("uniqueCount", i + 1).array(nums).step();
    }
}
