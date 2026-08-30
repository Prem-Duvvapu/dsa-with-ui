package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

/**
 * Missing number by sum arithmetic: the array should hold 0..N where N is its
 * length. The missing value falls out of expected - actual, no matter what
 * order the numbers arrive in.
 */
@Component
public class FindMissingNumberTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "find-missing-number";
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
                        .help("Distinct values from 0..N with exactly one missing; N equals the array length.")
                        .length(1, 40).values(0, 1000)
                        .defaultValue(List.of(9, 6, 4, 2, 3, 5, 7, 0, 1))
                        .build());
    }

    /** The missing value sits past the end of the data - nothing inside looks absent. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(0, 1, 2));
    }

    @Override
    public String annotatedCode() {
        return """
               public int missingNumber(int[] nums) {
                   // @a init
                   int n = nums.length;
                   long expected = (long) n * (n + 1) / 2;
                   // @a loop
                   long actual = 0;
                   for (int x : nums) {
                       actual += x;
                   }
                   // @a done
                   return (int) (expected - actual);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        long expected = (long) n * (n + 1) / 2;

        emit.at("init")
                .say("Length %d means the full set is 0..%d, which sums to %d*%d/2 = %d.",
                        n, n, n, n + 1, expected)
                .var("n", n).var("expected", expected).array(nums).step();

        long actual = 0;
        for (int i = 0; i < nums.length; i++) {
            actual += nums[i];
            emit.at("loop")
                    .say("Added nums[%d]=%d. Running sum %d; if nothing were missing it would reach %d only at the end.",
                            i, nums[i], actual, expected)
                    .var("i", i).var("actual", actual).var("expected", expected)
                    .array(nums, i).step();
        }

        long missing = expected - actual;
        emit.at("done")
                .say("Expected %d but got %d: the gap %d is exactly the missing number.",
                        expected, actual, missing)
                .var("actual", actual).var("missing", missing).array(nums).step();
    }
}
