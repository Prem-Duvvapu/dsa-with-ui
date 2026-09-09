package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Floor and ceil are two independent binary searches over the same array — the largest
 * value not above the target, and the smallest value not below it. Neither can be derived
 * from the other in general (the target may sit between two elements, or beyond either end).
 */
@Component
public class FloorCeilSortedArrayTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "floor-ceil-sorted-array";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Sorted array")
                        .length(1, 64).values(-999, 999).sorted().distinct()
                        .defaultValue(List.of(1, 2, 8, 10, 11, 12, 19))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-999, 999)
                        .defaultValue(5)
                        .build());
    }

    /** Target beyond the array's max: floor exists but ceil does not. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 8, 10, 11, 12, 19), "target", 25);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] getFloorAndCeil(int[] a, int x) {
                   // @a init
                   int floor = findFloor(a, x);
                   int ceil = findCeil(a, x);
                   // @a done
                   return new int[]{floor, ceil};
               }

               private int findFloor(int[] a, int x) {
                   int low = 0, high = a.length - 1, ans = -1;
                   while (low <= high) {
                       // @a floorMid
                       int mid = (low + high) / 2;
                       if (a[mid] <= x) { ans = a[mid]; low = mid + 1; }
                       else high = mid - 1;
                   }
                   return ans;
               }

               private int findCeil(int[] a, int x) {
                   int low = 0, high = a.length - 1, ans = -1;
                   while (low <= high) {
                       // @a ceilMid
                       int mid = (low + high) / 2;
                       if (a[mid] >= x) { ans = a[mid]; high = mid - 1; }
                       else low = mid + 1;
                   }
                   return ans;
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
        int target = in.getInt("target");

        emit.at("init")
                .say("Find the floor (largest value <= %d) and ceil (smallest value >= %d) "
                        + "in the sorted array, each with its own binary search.", target, target)
                .var("target", target).arrayState(window(nums, 0, nums.length - 1, -1)).step();

        int low = 0, high = nums.length - 1, floor = -1;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (nums[mid] <= target) {
                floor = nums[mid];
                emit.at("floorMid")
                        .say("nums[%d]=%d <= %d — candidate floor, look right for a closer one.",
                                mid, nums[mid], target)
                        .var("floor", floor).var("low", mid + 1)
                        .arrayState(window(nums, mid + 1, high, mid)).step();
                low = mid + 1;
            } else {
                emit.at("floorMid")
                        .say("nums[%d]=%d > %d — too big for floor, look left.", mid, nums[mid], target)
                        .var("high", mid - 1)
                        .arrayState(window(nums, low, mid - 1, mid)).step();
                high = mid - 1;
            }
        }

        low = 0; high = nums.length - 1;
        int ceil = -1;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (nums[mid] >= target) {
                ceil = nums[mid];
                emit.at("ceilMid")
                        .say("nums[%d]=%d >= %d — candidate ceil, look left for a closer one.",
                                mid, nums[mid], target)
                        .var("ceil", ceil).var("high", mid - 1)
                        .arrayState(window(nums, low, mid - 1, mid)).step();
                high = mid - 1;
            } else {
                emit.at("ceilMid")
                        .say("nums[%d]=%d < %d — too small for ceil, look right.", mid, nums[mid], target)
                        .var("low", mid + 1)
                        .arrayState(window(nums, mid + 1, high, mid)).step();
                low = mid + 1;
            }
        }

        emit.at("done")
                .say("floor(%d) = %s, ceil(%d) = %s.", target, floor == -1 ? "none" : String.valueOf(floor),
                        target, ceil == -1 ? "none" : String.valueOf(ceil))
                .var("floor", floor).var("ceil", ceil)
                .arrayState(window(nums, 0, nums.length - 1, -1)).step();
    }
}
