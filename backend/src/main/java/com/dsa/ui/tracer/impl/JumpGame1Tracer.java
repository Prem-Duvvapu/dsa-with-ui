package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Jump Game I — the greedy horizon. Never plan an actual route; just track the farthest
 * index reachable so far, and the moment the walk arrives somewhere past that horizon it
 * is stranded for good.
 */
@Component
public class JumpGame1Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "jump-game-1";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Jump lengths")
                        .help("nums[i] is the farthest you may jump forward from index i.")
                        .length(1, 40).values(0, 30)
                        .defaultValue(List.of(2, 3, 1, 1, 4))
                        .build());
    }

    /** Same shape, but index 3's zero jump strands every later index — unreachable. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(3, 2, 1, 0, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean canJump(int[] nums) {
                   // @a init
                   int maxReach = 0;
                   for (int i = 0; i < nums.length; i++) {
                       // @a check
                       if (i > maxReach) {
                           // @a stuck
                           return false;
                       }
                       // @a extend
                       maxReach = Math.max(maxReach, i + nums[i]);
                   }
                   // @a done
                   return true;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int maxReach = 0;

        emit.at("init").say("Track the farthest index reachable so far. Start at index 0 with maxReach = 0.")
                .var("maxReach", maxReach)
                .array(nums).step();

        for (int i = 0; i < n; i++) {
            emit.at("check").say("At index %d: is it still within reach? maxReach is currently %d.", i, maxReach)
                    .var("i", i).var("maxReach", maxReach)
                    .array(nums, i).step();

            if (i > maxReach) {
                emit.at("stuck").say("Index %d is beyond maxReach (%d) — nothing earlier could jump this far. The end is unreachable.", i, maxReach)
                        .var("i", i).var("maxReach", maxReach)
                        .array(nums, i).step();
                return;
            }

            int reachFromHere = i + nums[i];
            int before = maxReach;
            maxReach = Math.max(maxReach, reachFromHere);
            emit.at("extend").say("From index %d you can jump up to %d away, landing at %d. maxReach = max(%d, %d) = %d.",
                            i, nums[i], reachFromHere, before, reachFromHere, maxReach)
                    .var("i", i).var("maxReach", maxReach)
                    .array(nums, i).step();
        }

        emit.at("done").say("Every index from 0 to %d fell within reach as we went — the last index is reachable.", n - 1)
                .var("maxReach", maxReach)
                .array(nums).step();
    }
}
