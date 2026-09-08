package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Minimum jumps by greedily finishing one reachable layer at a time. */
@Component
public class JumpGame2Tracer implements AlgorithmTracer {
    @Override public String id() { return "jump-game-2"; }
    @Override public DsType dsType() { return DsType.ARRAY; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("nums", FieldType.INT_ARRAY).label("Jump lengths")
                .length(1, 40).values(0, 30).defaultValue(List.of(2, 3, 1, 1, 4)).build());
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("nums", List.of(3, 2, 1, 0, 4)); }
    @Override public String annotatedCode() {
        return """
               public int jump(int[] nums) {
                   // @a init
                   int jumps = 0, layerEnd = 0, farthest = 0;
                   for (int i = 0; i < nums.length - 1; i++) {
                       // @a scan
                       farthest = Math.max(farthest, i + nums[i]);
                       if (i == layerEnd) {
                           if (farthest == layerEnd) {
                               // @a stuck
                               return -1;
                           }
                           // @a jump
                           jumps++;
                           layerEnd = farthest;
                       }
                   }
                   // @a done
                   return jumps;
               }""";
    }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int jumps = 0, end = 0, farthest = 0;
        emit.at("init").say("The first reachable layer ends at index 0.")
                .var("jumps", jumps).var("layerEnd", end).array(nums).step();
        for (int i = 0; i < nums.length - 1; i++) {
            farthest = Math.max(farthest, i + nums[i]);
            emit.at("scan").say("Index %d can reach %d; the next layer can extend through %d.", i, i + nums[i], farthest)
                    .var("i", i).var("farthest", farthest).var("layerEnd", end).array(nums, i).step();
            if (i == end) {
                if (farthest == end) {
                    emit.at("stuck").say("This layer cannot extend beyond %d. The last index is unreachable.", end)
                            .var("jumps", -1).array(nums, i).step();
                    return;
                }
                jumps++;
                end = farthest;
                emit.at("jump").say("Finish the layer: commit jump #%d and widen its end to %d.", jumps, end)
                        .var("jumps", jumps).var("layerEnd", end).array(nums, i, Math.min(end, nums.length - 1)).step();
            }
        }
        emit.at("done").say("The last index is inside layer %d, so the minimum is %d jump(s).", jumps, jumps)
                .var("jumps", jumps).array(nums, nums.length - 1).step();
    }
}
