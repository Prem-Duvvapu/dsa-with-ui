package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The number of right rotations equals the index of the minimum element — rotating a
 * sorted array k times moves the original index-0 element to index k. So the same
 * "which half contains the minimum" halving used to find the minimum's value here just
 * reports its index instead.
 */
@Component
public class CountRotationsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-rotations";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Rotated sorted array")
                        .length(1, 40).values(-99, 99).distinct()
                        .defaultValue(List.of(5, 6, 7, 1, 2, 3, 4))
                        .build());
    }

    /** Not rotated at all: the minimum sits at index 0, and the left half is always taken. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3, 4, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public int findKRotation(int[] nums) {
                   // @a init
                   int low = 0, high = nums.length - 1, minVal = Integer.MAX_VALUE, index = -1;
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       if (nums[low] <= nums[mid]) {
                           // @a takeLeft
                           if (nums[low] < minVal) { minVal = nums[low]; index = low; }
                           low = mid + 1;
                       } else {
                           // @a takeRight
                           if (nums[mid] < minVal) { minVal = nums[mid]; index = mid; }
                           high = mid - 1;
                       }
                   }
                   // @a done
                   return index;
               }""";
    }

    private List<ArrayElement> window(int[] nums, int low, int high, int mid, int index) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s;
            if (i == mid) s = "current";
            else if (i == index) s = "done";
            else if (i < low || i > high) s = "visited";
            else s = "target";
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int low = 0, high = nums.length - 1, minVal = Integer.MAX_VALUE, index = -1;

        emit.at("init")
                .say("The minimum's own index equals the number of right rotations. Search [%d..%d] for it.",
                        low, high)
                .var("low", low).var("high", high)
                .arrayState(window(nums, low, high, -1, -1)).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Probe [%d..%d]: nums[%d] = %d.", low, high, mid, nums[mid])
                    .var("low", low).var("high", high).var("mid", mid)
                    .arrayState(window(nums, low, high, mid, index)).step();

            if (nums[low] <= nums[mid]) {
                if (nums[low] < minVal) {
                    minVal = nums[low];
                    index = low;
                }
                emit.at("takeLeft")
                        .say("nums[%d]=%d <= nums[%d]=%d — the left half is sorted, so its own start "
                                + "(index %d) is the smallest candidate in this half. Search the right half next.",
                                low, nums[low], mid, nums[mid], low)
                        .var("index", index).var("low", mid + 1)
                        .arrayState(window(nums, mid + 1, high, -1, index)).step();
                low = mid + 1;
            } else {
                if (nums[mid] < minVal) {
                    minVal = nums[mid];
                    index = mid;
                }
                emit.at("takeRight")
                        .say("nums[%d]=%d > nums[%d]=%d — the rotation point is in the left half, so "
                                + "index %d is the smallest candidate seen. Search the left half next.",
                                low, nums[low], mid, nums[mid], mid)
                        .var("index", index).var("high", mid - 1)
                        .arrayState(window(nums, low, mid - 1, -1, index)).step();
                high = mid - 1;
            }
        }

        emit.at("done")
                .say("The minimum (%d) sits at index %d, so the array was rotated %d times.", minVal, index, index)
                .var("rotations", index)
                .arrayState(window(nums, 0, -1, -1, index)).step();
    }
}
