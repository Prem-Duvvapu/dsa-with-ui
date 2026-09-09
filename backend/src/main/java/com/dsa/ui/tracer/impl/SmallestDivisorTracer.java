package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A larger divisor can only shrink every ceiling-division term (or leave it unchanged),
 * so the total sum falls monotonically as the divisor grows — exactly the shape binary
 * search needs. Divisor 1 gives the largest possible sum (the array total); the array's
 * own max gives the smallest (every element divides down to 1).
 */
@Component
public class SmallestDivisorTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "smallest-divisor";
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
                        .length(1, 30).values(1, 1_000_000)
                        .defaultValue(List.of(1, 2, 5, 9))
                        .build(),
                InputField.of("threshold", FieldType.INT)
                        .label("Threshold")
                        .range(1, 10_000_000)
                        .defaultValue(6)
                        .build());
    }

    /** A much larger array whose smallest divisor is its own maximum element. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(44, 22, 33, 11, 1), "threshold", 5);
    }

    @Override
    public String annotatedCode() {
        return """
               public int smallestDivisor(int[] nums, int threshold) {
                   // @a init
                   int low = 1, high = max(nums), ans = high;
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       long sum = 0;
                       for (int x : nums) {
                           // @a tally
                           sum += (x + mid - 1) / mid;
                       }
                       if (sum <= threshold) {
                           // @a feasible
                           ans = mid;
                           high = mid - 1;
                       } else {
                           // @a infeasible
                           low = mid + 1;
                       }
                   }
                   // @a done
                   return ans;
               }""";
    }

    private List<ArrayElement> baseState(int[] nums) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            state.add(new ArrayElement(i, nums[i], "default"));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int threshold = in.getInt("threshold");
        int max = 0;
        for (int x : nums) max = Math.max(max, x);
        int low = 1, high = max, ans = high;

        emit.at("init")
                .say("Binary search the smallest divisor whose ceiling-sum fits within %d. Range starts at [1, %d].",
                        threshold, max)
                .var("low", low).var("high", high)
                .arrayState(baseState(nums)).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Test divisor %d.", mid)
                    .var("low", low).var("high", high).var("mid", mid)
                    .arrayState(baseState(nums)).step();

            long sum = 0;
            for (int i = 0; i < nums.length; i++) {
                long term = (nums[i] + mid - 1) / mid;
                sum += term;
                List<ArrayElement> state = new ArrayList<>(nums.length);
                for (int j = 0; j < nums.length; j++) {
                    state.add(new ArrayElement(j, nums[j], j == i ? "current" : j < i ? "target" : "default"));
                }
                emit.at("tally")
                        .say("ceil(%d / %d) = %d. Running sum = %d.", nums[i], mid, term, sum)
                        .var("sum", sum)
                        .arrayState(state)
                        .step();
            }

            if (sum <= threshold) {
                ans = mid;
                emit.at("feasible")
                        .say("Sum %d <= %d — divisor %d works. Try smaller.", sum, threshold, mid)
                        .var("ans", ans).var("high", mid - 1)
                        .arrayState(baseState(nums)).step();
                high = mid - 1;
            } else {
                emit.at("infeasible")
                        .say("Sum %d > %d — divisor %d is too small. Try larger.", sum, threshold, mid)
                        .var("low", mid + 1)
                        .arrayState(baseState(nums)).step();
                low = mid + 1;
            }
        }

        emit.at("done")
                .say("low passed high. The smallest workable divisor is %d.", ans)
                .var("answer", ans).arrayState(baseState(nums)).step();
    }
}
