package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

/**
 * Single-pass maximum: carry the largest value seen so far and let every later
 * element try to beat it.
 */
@Component
public class LargestElementTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "largest-element";
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
                        .help("best keeps the largest value seen so far; every element challenges it once.")
                        .length(1, 40).values(-999, 999)
                        .defaultValue(List.of(7, 2, 35, 4, 18))
                        .build());
    }

    /** Strictly decreasing: nothing ever beats the first element. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(9, 8, 7, 6));
    }

    @Override
    public String annotatedCode() {
        return """
               public int largestElement(int[] nums) {
                   // @a init
                   int best = nums[0];
                   for (int i = 1; i < nums.length; i++) {
                       // @a compare
                       if (nums[i] > best) {
                           // @a beat
                           best = nums[i];
                       }
                   }
                   // @a done
                   return best;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int best = nums[0];

        emit.at("init").say("Start by trusting the first element: best = %d.", best)
                .var("best", best).array(nums, 0).step();

        for (int i = 1; i < nums.length; i++) {
            if (nums[i] > best) {
                emit.at("compare").say("i=%d: nums[%d]=%d challenges best=%d.", i, i, nums[i], best)
                        .var("i", i).var("best", best).array(nums, i, 0).step();
                best = nums[i];
                emit.at("beat").say("%d wins - it becomes the new best.", best)
                        .var("i", i).var("best", best).array(nums, i, 0).step();
            } else {
                emit.at("compare")
                        .say("i=%d: nums[%d]=%d cannot beat best=%d, so best stays.", i, i, nums[i], best)
                        .var("i", i).var("best", best).array(nums, i, 0).step();
            }
        }

        emit.at("done").say("One pass, one variable - the largest element is %d.", best)
                .var("best", best).array(nums).step();
    }
}
