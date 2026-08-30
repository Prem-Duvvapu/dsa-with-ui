package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

/**
 * Longest run of consecutive 1s: a running count grows while the values keep
 * being 1 and is compared against the best every step.
 */
@Component
public class MaxConsecutiveOnesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "max-consecutive-ones";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Binary array")
                        .help("Only 0s and 1s. The counter resets on every 0 - that reset is the whole algorithm.")
                        .length(1, 40).values(0, 1)
                        .defaultValue(List.of(1, 1, 0, 1, 1, 1, 0, 1))
                        .build());
    }

    /** Alternating values: the reset branch fires four times and no run reaches 2. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 0, 1, 0, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public int findMaxConsecutiveOnes(int[] nums) {
                   // @a init
                   int best = 0, run = 0;
                   for (int i = 0; i < nums.length; i++) {
                       // @a count
                       if (nums[i] == 1) {
                           run++;
                           // @a best
                           if (run > best) best = run;
                       } else {
                           // @a reset
                           run = 0;
                       }
                   }
                   // @a done
                   return best;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int best = 0;
        int run = 0;

        emit.at("init").say("run counts the current streak of 1s; best remembers the longest streak seen.")
                .var("best", 0).var("run", 0).array(nums).step();

        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == 1) {
                run++;
                emit.at("count").say("i=%d: nums[%d]=1 extends the streak to %d.", i, i, run)
                        .var("i", i).var("run", run).var("best", best)
                        .array(nums, i).step();
                if (run > best) {
                    best = run;
                    emit.at("best").say("Streak %d beats the old best - best=%d.", run, best)
                            .var("i", i).var("run", run).var("best", best)
                            .array(nums, i).step();
                }
            } else {
                run = 0;
                emit.at("reset")
                        .say("i=%d: nums[%d]=0 breaks the streak. Consecutive means consecutive - back to 0.", i, i)
                        .var("i", i).var("run", 0).var("best", best)
                        .array(nums, i).step();
            }
        }

        emit.at("done").say("Longest consecutive run of 1s is %d.", best)
                .var("best", best).array(nums).step();
    }
}
