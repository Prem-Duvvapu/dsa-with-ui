package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Treating both array boundaries as negative infinity means a peak always exists and no
 * special-case check is needed at either end. Comparing {@code nums[mid]} only to its right
 * neighbor is enough: on a rising edge the peak is to the right, on a falling edge mid
 * itself could BE the peak, so it stays in play rather than being excluded.
 */
@Component
public class FindPeakElementTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "find-peak-element";
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
                        .help("Adjacent elements are never equal. A peak is strictly greater than both neighbors.")
                        .length(1, 40).values(-99, 99)
                        .defaultValue(List.of(1, 2, 3, 1))
                        .build());
    }

    /** A single descending run before the true peak, then a rise — exercises both branches. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 1, 3, 5, 6, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public int findPeakElement(int[] nums) {
                   // @a init
                   int low = 0, high = nums.length - 1;
                   while (low < high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       if (nums[mid] > nums[mid + 1]) {
                           // @a descend
                           high = mid;
                       } else {
                           // @a ascend
                           low = mid + 1;
                       }
                   }
                   // @a done
                   return low;
               }""";
    }

    private List<ArrayElement> window(int[] nums, int low, int high, int mid) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s = i == mid ? "current" : (i < low || i > high) ? "visited" : "target";
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int low = 0, high = nums.length - 1;

        emit.at("init")
                .say("Treat both ends as -infinity, so a peak always exists. Search [%d..%d].", low, high)
                .var("low", low).var("high", high)
                .arrayState(window(nums, low, high, -1)).step();

        while (low < high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Range [%d..%d]. Compare nums[%d]=%d to its right neighbor nums[%d]=%d.",
                            low, high, mid, nums[mid], mid + 1, nums[mid + 1])
                    .var("low", low).var("high", high).var("mid", mid)
                    .arrayState(window(nums, low, high, mid)).step();

            if (nums[mid] > nums[mid + 1]) {
                emit.at("descend")
                        .say("%d > %d — a falling edge. A peak lies at or before mid, so mid stays in play.",
                                nums[mid], nums[mid + 1])
                        .var("high", mid)
                        .arrayState(window(nums, low, mid, -1)).step();
                high = mid;
            } else {
                emit.at("ascend")
                        .say("%d <= %d — a rising edge. The peak must be to the right of mid.",
                                nums[mid], nums[mid + 1])
                        .var("low", mid + 1)
                        .arrayState(window(nums, mid + 1, high, -1)).step();
                low = mid + 1;
            }
        }

        emit.at("done")
                .say("low caught up to high at index %d (value %d) — a confirmed peak.", low, nums[low])
                .var("answer", low)
                .arrayState(window(nums, low, low, low)).step();
    }
}
