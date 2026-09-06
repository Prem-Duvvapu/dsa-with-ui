package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Print the actual subarray with maximum sum, not just the sum itself.
 *
 * <p>Uses Kadane's algorithm with boundary tracking: {@code start} marks
 * where the current candidate subarray begins (reset whenever running sum
 * goes negative), and {@code ansStart}/{@code ansEnd} record the best
 * window seen so far.
 */
@Component
public class PrintMaxSubarrayTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "print-max-subarray";
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
                        .help("Negative values make this interesting — they force resets.")
                        .length(1, 40).values(-999, 999)
                        .defaultValue(List.of(1, -2, 6, -1, 3, -4, 2))
                        .build());
    }

    /**
     * All negatives except one: reset fires every step, the answer is the
     * lone positive element, and the subarray is length 1.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(-3, -5, 4, -1, -2));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] maxSubarrayWithPrint(int[] arr) {
                   // @a init
                   int maxi = Integer.MIN_VALUE, sum = 0;
                   int start = 0, ansStart = -1, ansEnd = -1;
               
                   for (int i = 0; i < arr.length; i++) {
                       // @a resetStart
                       if (sum == 0) start = i;
                       // @a extend
                       sum += arr[i];
                       if (sum > maxi) {
                           // @a newBest
                           maxi = sum;
                           ansStart = start;
                           ansEnd = i;
                       }
                       if (sum < 0) {
                           // @a reset
                           sum = 0;
                       }
                   }
                   // @a done
                   return Arrays.copyOfRange(arr, ansStart, ansEnd + 1);
               }""";
    }

    private List<ArrayElement> buildState(int[] arr, int ansStart, int ansEnd, int current) {
        List<ArrayElement> state = new ArrayList<>(arr.length);
        for (int idx = 0; idx < arr.length; idx++) {
            String s;
            if (idx == current) s = "current";
            else if (ansStart >= 0 && idx >= ansStart && idx <= ansEnd) s = "target";
            else s = "default";
            state.add(new ArrayElement(idx, arr[idx], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("nums");
        int maxi = Integer.MIN_VALUE;
        int sum = 0;
        int start = 0, ansStart = -1, ansEnd = -1;

        emit.at("init")
                .say("Track running sum and the best window [ansStart..ansEnd]. maxi = -INF, sum = 0.")
                .var("maxi", "-INF").var("sum", 0).var("ansStart", -1).var("ansEnd", -1)
                .arrayState(buildState(arr, ansStart, ansEnd, -1))
                .step();

        for (int i = 0; i < arr.length; i++) {
            if (sum == 0) {
                start = i;
                emit.at("resetStart")
                        .say("Running sum is 0, so a new candidate subarray starts at index %d.", i)
                        .var("i", i).var("start", start)
                        .arrayState(buildState(arr, ansStart, ansEnd, i))
                        .step();
            }

            sum += arr[i];
            emit.at("extend")
                    .say("i=%d: add arr[%d]=%d, running sum = %d. Current candidate: arr[%d..%d].",
                            i, i, arr[i], sum, start, i)
                    .var("i", i).var("sum", sum).var("start", start)
                    .arrayState(buildState(arr, ansStart, ansEnd, i))
                    .step();

            if (sum > maxi) {
                maxi = sum;
                ansStart = start;
                ansEnd = i;
                emit.at("newBest")
                        .say("New best! sum %d > previous best %s. Best subarray is now arr[%d..%d].",
                                sum, maxi == sum ? "-INF" : String.valueOf(maxi), ansStart, ansEnd)
                        .var("maxi", maxi).var("ansStart", ansStart).var("ansEnd", ansEnd)
                        .arrayState(buildState(arr, ansStart, ansEnd, i))
                        .step();
            }

            if (sum < 0) {
                emit.at("reset")
                        .say("Running sum went negative (%d) — any subarray starting fresh beats extending this. Reset to 0.", sum)
                        .var("sum", 0).var("i", i)
                        .arrayState(buildState(arr, ansStart, ansEnd, i))
                        .step();
                sum = 0;
            }
        }

        // Build final state highlighting the answer subarray as "sorted"
        List<ArrayElement> finalState = new ArrayList<>(arr.length);
        for (int idx = 0; idx < arr.length; idx++) {
            String s = (ansStart >= 0 && idx >= ansStart && idx <= ansEnd) ? "sorted" : "default";
            finalState.add(new ArrayElement(idx, arr[idx], s));
        }

        int[] result = (ansStart >= 0) ? java.util.Arrays.copyOfRange(arr, ansStart, ansEnd + 1) : new int[0];
        emit.at("done")
                .say("Max subarray sum = %d, from arr[%d..%d] = %s.",
                        maxi, ansStart, ansEnd, java.util.Arrays.toString(result))
                .var("maxi", maxi).var("result", java.util.Arrays.toString(result))
                .arrayState(finalState)
                .step();
    }
}
