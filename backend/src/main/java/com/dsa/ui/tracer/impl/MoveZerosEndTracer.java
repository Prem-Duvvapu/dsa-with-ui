package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

/**
 * Stable two-pointer partition: pos marks where the next non-zero belongs.
 * Every non-zero is swapped forward into that slot; zeros are left behind,
 * which naturally collects them at the end in original relative order.
 */
@Component
public class MoveZerosEndTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "move-zeros-end";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("pos waits for the next non-zero; scanning i swaps every non-zero into place.")
                        .length(1, 40).values(0, 999)
                        .defaultValue(List.of(0, 1, 0, 3, 12))
                        .build());
    }

    /** No zeros at all: the swap branch never fires and the array comes back untouched. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(4, 2, 7));
    }

    @Override
    public String annotatedCode() {
        return """
               public void moveZeroes(int[] nums) {
                   // @a init
                   int pos = 0;
                   for (int i = 0; i < nums.length; i++) {
                       // @a scan
                       if (nums[i] != 0) {
                           // @a swap
                           int tmp = nums[pos];
                           nums[pos] = nums[i];
                           nums[i] = tmp;
                           pos++;
                       }
                   }
                   // @a done
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int pos = 0;

        emit.using("Array");
        emit.at("init")
                .say("pos is the landing spot for the next non-zero. Everything left of pos is already a packed, ordered prefix of non-zeros.")
                .var("pos", 0).array(nums, -1, 0).step();

        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != 0) {
                emit.at("scan").say("i=%d: nums[%d]=%d is non-zero - it belongs at position %d.", i, i, nums[i], pos)
                        .var("i", i).var("pos", pos).array(nums, i, pos).step();
                if (pos != i) {
                    int tmp = nums[pos];
                    nums[pos] = nums[i];
                    nums[i] = tmp;
                    emit.at("swap")
                            .say("Swap it with the %d sitting at %d. Non-zeros keep their order; the zero slides right.",
                                    tmp, pos)
                            .var("i", i).var("pos", pos).array(nums, pos, i).step();
                } else {
                    emit.at("swap")
                            .say("It already sits at %d - swapping with itself would be theatre, so just advance pos.", pos)
                            .var("i", i).var("pos", pos).array(nums, pos, i).step();
                }
                pos++;
            } else {
                emit.at("scan")
                        .say("i=%d: nums[%d]=0 stays for now; pos=%d does not move, so the next value will land before this zero.",
                                i, i, pos)
                        .var("i", i).var("pos", pos).array(nums, i, pos).step();
            }
        }

        emit.at("done").say("All non-zeros packed to the front in order; every zero was pushed past position %d.", pos)
                .var("pos", pos).array(nums).step();
    }
}
