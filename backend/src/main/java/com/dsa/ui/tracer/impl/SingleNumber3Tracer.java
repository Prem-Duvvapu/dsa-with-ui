package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Two numbers appear exactly once; every other number appears exactly twice. XOR-ing
 * everything leaves {@code a ^ b} (the two uniques), and any set bit of that XOR must
 * differ between {@code a} and {@code b} — so partitioning the array by that one bit
 * puts {@code a} and {@code b} in different buckets, and a second XOR pass isolates each.
 */
@Component
public class SingleNumber3Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "single-number-3";
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
                        .help("Exactly two values appear once; every other value appears exactly twice.")
                        .length(2, 40).values(-999, 999)
                        .defaultValue(List.of(1, 2, 1, 3, 2, 5))
                        .build());
    }

    /** Longer array, negative values among the duplicates, and different unique values (15, 42). */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(10, 10, -7, -7, 15, 42, 99, 99));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] singleNumberIII(int[] nums) {
                   // @a xorAll
                   int xorAll = 0;
                   for (int num : nums) xorAll ^= num;
                   // @a diffBit
                   int diffBit = xorAll & (-xorAll);
                   int a = 0, b = 0;
                   for (int num : nums) {
                       // @a bucket
                       if ((num & diffBit) != 0) {
                           // @a bucketA
                           a ^= num;
                       } else {
                           // @a bucketB
                           b ^= num;
                       }
                   }
                   // @a done
                   return new int[]{a, b};
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int xorAll = 0;

        emit.at("xorAll")
                .say("XOR every element together. The two uniques survive as xorAll = a ^ b; "
                        + "every duplicate pair cancels.")
                .var("xorAll", 0).array(nums).step();
        for (int num : nums) xorAll ^= num;
        emit.at("xorAll")
                .say("xorAll = %d after the full pass.", xorAll)
                .var("xorAll", xorAll).array(nums).step();

        int diffBit = xorAll & (-xorAll);
        emit.at("diffBit")
                .say("diffBit = xorAll & (-xorAll) = %d isolates xorAll's rightmost set bit — "
                        + "a and b must disagree there, since it survived their XOR.", diffBit)
                .var("diffBit", diffBit).array(nums).step();

        int a = 0, b = 0;
        for (int i = 0; i < nums.length; i++) {
            int num = nums[i];
            boolean inBucketA = (num & diffBit) != 0;
            emit.at("bucket")
                    .say("nums[%d]=%d: (num & diffBit) %s 0, so it goes to bucket %s.",
                            i, num, inBucketA ? "!=" : "==", inBucketA ? "A" : "B")
                    .var("num", num).var("bucket", inBucketA ? "A" : "B")
                    .array(nums, i).step();
            if (inBucketA) {
                a ^= num;
                emit.at("bucketA").say("a = %d.", a).var("a", a).var("b", b).array(nums, i).step();
            } else {
                b ^= num;
                emit.at("bucketB").say("b = %d.", b).var("a", a).var("b", b).array(nums, i).step();
            }
        }

        emit.at("done")
                .say("Bucket A isolated %d, bucket B isolated %d. The two singles are %d and %d.", a, b, a, b)
                .var("a", a).var("b", b).array(nums).step();
    }
}
