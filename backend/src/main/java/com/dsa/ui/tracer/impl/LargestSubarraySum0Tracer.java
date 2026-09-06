package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Largest subarray with sum 0 using prefix-sum + HashMap.
 *
 * <p>If {@code prefixSum[i] == prefixSum[j]}, then subarray {@code [j+1..i]}
 * sums to 0. The HashMap stores the FIRST index of each prefix sum, so
 * the span {@code i - map.get(sum)} gives the longest possible subarray
 * ending at i.
 */
@Component
public class LargestSubarraySum0Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "largest-subarray-sum-0";
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
                        .help("Find the longest contiguous subarray summing to 0.")
                        .length(1, 30).values(-50, 50)
                        .defaultValue(List.of(15, -2, 2, -8, 1, 7, 10, 23))
                        .build());
    }

    /** Entire array sums to 0, exercising the sum==0 path. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, -3, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public int maxLen(int[] arr) {
                   // @a init
                   Map<Integer, Integer> map = new HashMap<>();
                   int maxi = 0, sum = 0;
               
                   for (int i = 0; i < arr.length; i++) {
                       // @a addElement
                       sum += arr[i];
                       if (sum == 0) {
                           // @a wholePrefix
                           maxi = i + 1;
                       } else {
                           if (map.containsKey(sum)) {
                               // @a foundMatch
                               maxi = Math.max(maxi, i - map.get(sum));
                           } else {
                               // @a storePrefix
                               map.put(sum, i);
                           }
                       }
                   }
                   // @a done
                   return maxi;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("nums");
        Map<Integer, Integer> map = new HashMap<>();
        int maxi = 0, sum = 0;

        emit.at("init")
                .say("Find longest subarray with sum 0 in %s. HashMap stores first index of each prefix sum.",
                        java.util.Arrays.toString(arr))
                .var("sum", 0).var("maxi", 0)
                .array(arr)
                .step();

        for (int i = 0; i < arr.length; i++) {
            sum += arr[i];
            emit.at("addElement")
                    .say("i=%d: add %d → prefix sum = %d.", i, arr[i], sum)
                    .var("i", i).var("sum", sum).var("maxi", maxi)
                    .array(arr, i)
                    .step();

            if (sum == 0) {
                maxi = i + 1;
                emit.at("wholePrefix")
                        .say("Prefix sum is 0! Entire subarray arr[0..%d] sums to 0, length %d. maxi = %d.", i, maxi, maxi)
                        .var("maxi", maxi)
                        .array(arr, i)
                        .step();
            } else {
                if (map.containsKey(sum)) {
                    int prevIdx = map.get(sum);
                    int len = i - prevIdx;
                    maxi = Math.max(maxi, len);
                    emit.at("foundMatch")
                            .say("Prefix sum %d was seen at index %d. Subarray arr[%d..%d] sums to 0 (length %d).%s",
                                    sum, prevIdx, prevIdx + 1, i, len,
                                    len >= maxi ? " New best!" : " Not longer than current best " + maxi + ".")
                            .var("maxi", maxi).var("prevIdx", prevIdx).var("len", len)
                            .array(arr, i, prevIdx)
                            .step();
                } else {
                    map.put(sum, i);
                    emit.at("storePrefix")
                            .say("First time seeing prefix sum %d — store index %d.", sum, i)
                            .var("sum", sum).var("map", map.toString())
                            .array(arr, i)
                            .step();
                }
            }
        }

        emit.at("done")
                .say("Longest subarray with sum 0 has length %d.", maxi)
                .var("maxi", maxi)
                .array(arr)
                .step();
    }
}
