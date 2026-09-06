package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Maximum product subarray using simultaneous prefix and suffix scanning.
 *
 * <p>The key insight: the maximum product subarray must either start at index 0
 * or end at index N-1, unless a zero breaks the chain. Why? An even number of
 * negatives multiplied together stays positive, so the whole array wins — unless
 * there's an odd number of negatives, in which case one of the two subarrays
 * (prefix up to the last negative, or suffix after the first negative) is optimal.
 * Scanning both directions simultaneously covers all cases.
 */
@Component
public class MaxProductSubarrayTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "max-product-subarray";
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
                        .help("Integers including negatives and zeroes. Find max product subarray.")
                        .length(1, 30).values(-50, 50)
                        .defaultValue(List.of(2, 3, -2, 4))
                        .build());
    }

    /**
     * Contains a zero (forces prefix/suffix reset) and odd negatives
     * so the suffix scan wins over the prefix scan.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(-1, -3, -10, 0, 6, 2));
    }

    @Override
    public String annotatedCode() {
        return """
               public int maxProduct(int[] nums) {
                   int n = nums.length;
                   // @a init
                   double pre = 1, suff = 1;
                   double maxi = Integer.MIN_VALUE;
               
                   for (int i = 0; i < n; i++) {
                       // @a resetCheck
                       if (pre == 0) pre = 1;
                       if (suff == 0) suff = 1;
                       // @a multiply
                       pre *= nums[i];
                       suff *= nums[n - 1 - i];
                       // @a updateMax
                       maxi = Math.max(maxi, Math.max(pre, suff));
                   }
                   // @a done
                   return (int) maxi;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        double pre = 1, suff = 1;
        double maxi = Integer.MIN_VALUE;

        emit.at("init")
                .say("Scan from both ends simultaneously. prefix product = 1, suffix product = 1, maxi = -INF.")
                .var("pre", 1).var("suff", 1).var("maxi", "-INF")
                .array(nums)
                .step();

        for (int i = 0; i < n; i++) {
            boolean preReset = false, suffReset = false;
            if (pre == 0) { pre = 1; preReset = true; }
            if (suff == 0) { suff = 1; suffReset = true; }

            if (preReset || suffReset) {
                emit.at("resetCheck")
                        .say("i=%d:%s%s Zero hit → reset to 1 so multiplication can resume.",
                                i,
                                preReset ? " prefix was 0, reset." : "",
                                suffReset ? " suffix was 0, reset." : "")
                        .var("pre", String.format("%.0f", pre))
                        .var("suff", String.format("%.0f", suff))
                        .array(nums, i, n - 1 - i)
                        .step();
            }

            pre *= nums[i];
            suff *= nums[n - 1 - i];

            emit.at("multiply")
                    .say("i=%d: prefix *= nums[%d](%d) = %.0f, suffix *= nums[%d](%d) = %.0f.",
                            i, i, nums[i], pre, n - 1 - i, nums[n - 1 - i], suff)
                    .var("i", i).var("pre", String.format("%.0f", pre))
                    .var("suff", String.format("%.0f", suff))
                    .array(nums, i, n - 1 - i)
                    .step();

            double oldMaxi = maxi;
            maxi = Math.max(maxi, Math.max(pre, suff));
            emit.at("updateMax")
                    .say("max(prefix=%.0f, suffix=%.0f) = %.0f.%s maxi = %.0f.",
                            pre, suff, Math.max(pre, suff),
                            maxi > oldMaxi ? " New best!" : "", maxi)
                    .var("maxi", String.format("%.0f", maxi))
                    .array(nums, i, n - 1 - i)
                    .step();
        }

        emit.at("done")
                .say("Maximum product subarray = %.0f.", maxi)
                .var("result", String.format("%.0f", maxi))
                .array(nums)
                .step();
    }
}
