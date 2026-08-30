package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

@Component
public class KadaneTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "kadane-algo";
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
                        .help("Negative values are what make this interesting.")
                        .length(1, 40).values(-999, 999)
                        .defaultValue(List.of(-2, 1, -3, 4, -1, 2, 1, -5, 4))
                        .build());
    }

    /** A different length and a different answer, with the running sum forced to reset. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(5, -1, 5, -20, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public int maxSubArray(int[] nums) {
                   // @a init
                   int best = nums[0], running = 0;
                   for (int i = 0; i < nums.length; i++) {
                       // @a extend
                       running += nums[i];
                       // @a best
                       if (running > best) best = running;
                       // @a reset
                       if (running < 0) running = 0;
                   }
                   // @a done
                   return best;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int best = nums[0];
        int running = 0;

        emit.at("init").say("Track the best sum seen (%d) and a running sum starting at 0.", best)
                .var("best", best).var("running", running).array(nums).step();

        for (int i = 0; i < nums.length; i++) {
            running += nums[i];
            emit.at("extend").say("i = %d: add %d, running sum is now %d.", i, nums[i], running)
                    .var("i", i).var("running", running).var("best", best)
                    .array(nums, i).step();

            if (running > best) {
                best = running;
                emit.at("best").say("That beats the best so far. best = %d.", best)
                        .var("i", i).var("running", running).var("best", best)
                        .array(nums, i).step();
            }

            if (running < 0) {
                emit.at("reset")
                        .say("Running sum went negative (%d), so any subarray is better off starting fresh. Reset to 0.", running)
                        .var("i", i).var("running", 0).var("best", best)
                        .array(nums, i).step();
                running = 0;
            }
        }

        emit.at("done").say("Largest contiguous subarray sum is %d.", best)
                .var("best", best).array(nums).step();
    }
}
