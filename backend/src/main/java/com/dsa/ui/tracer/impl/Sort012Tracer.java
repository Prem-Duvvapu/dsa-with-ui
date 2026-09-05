package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dutch National Flag partitioning: three pointers maintain the invariant
 * [0,low) all 0s, [low,mid) all 1s, (high,n) all 2s, so a single pass sorts the array
 * without ever counting elements first.
 */
@Component
public class Sort012Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "sort-0-1-2";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array of 0s, 1s and 2s")
                        .help("Only 0, 1, or 2 are valid values.")
                        .length(1, 40).values(0, 2)
                        .defaultValue(List.of(2, 0, 2, 1, 1, 0))
                        .build());
    }

    /**
     * Already sorted: every pointer still visits all three branches (each region gets
     * probed once), but every swap this time is an identity swap - nothing actually
     * changes position, unlike the default's real reordering.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(0, 0, 1, 1, 2, 2));
    }

    @Override
    public String annotatedCode() {
        return """
               public void sortColors(int[] nums) {
                   // @a init
                   int low = 0, mid = 0, high = nums.length - 1;
                   while (mid <= high) {
                       if (nums[mid] == 0) {
                           // @a zero
                           int temp = nums[low]; nums[low] = nums[mid]; nums[mid] = temp;
                           low++; mid++;
                       } else if (nums[mid] == 1) {
                           // @a one
                           mid++;
                       } else {
                           // @a two
                           int temp = nums[mid]; nums[mid] = nums[high]; nums[high] = temp;
                           high--;
                       }
                   }
                   // @a done
               }""";
    }

    /** [0,low) is the resolved 0-region, [low,mid) the resolved 1-region, (high,n) the resolved 2-region. */
    private List<ArrayElement> window(int[] nums, int low, int mid, int high) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s;
            if (mid <= high && i == mid) {
                s = "current";
            } else if (i < low || i > high) {
                s = "sorted";
            } else if (i < mid) {
                s = "target";
            } else {
                s = "visited";
            }
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int low = 0;
        int mid = 0;
        int high = n - 1;

        emit.at("init")
                .say("low and mid start at 0, high at the last index (%d). Invariant: "
                        + "[0,low) holds only 0s and [low,mid) only 1s - both empty so far - "
                        + "and (high,n) will hold only 2s.", high)
                .var("low", low).var("mid", mid).var("high", high)
                .arrayState(window(nums, low, mid, high)).step();

        while (mid <= high) {
            if (nums[mid] == 0) {
                emit.at("zero")
                        .say("nums[%d]=0 belongs in the 0-region - swap it with nums[%d], "
                                + "then advance both low and mid.", mid, low)
                        .var("mid", mid).var("low", low)
                        .arrayState(window(nums, low, mid, high)).step();
                int temp = nums[low];
                nums[low] = nums[mid];
                nums[mid] = temp;
                low++;
                mid++;
            } else if (nums[mid] == 1) {
                emit.at("one")
                        .say("nums[%d]=1 is already in the 1-region - just advance mid.", mid)
                        .var("mid", mid)
                        .arrayState(window(nums, low, mid, high)).step();
                mid++;
            } else {
                emit.at("two")
                        .say("nums[%d]=2 belongs in the 2-region - swap it with nums[%d], "
                                + "then shrink high. mid does not advance: the value just "
                                + "swapped in from the high end is still unexamined.",
                                mid, high)
                        .var("mid", mid).var("high", high)
                        .arrayState(window(nums, low, mid, high)).step();
                int temp = nums[mid];
                nums[mid] = nums[high];
                nums[high] = temp;
                high--;
            }
        }

        emit.at("done")
                .say("mid has passed high, so every index is in its region. Result: %s.",
                        java.util.Arrays.toString(nums))
                .var("result", java.util.Arrays.toString(nums))
                .arrayState(window(nums, low, mid, high)).step();
    }
}
