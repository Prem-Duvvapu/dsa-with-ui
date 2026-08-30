package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Search in a rotated sorted array — the binary search where the usual "is the target on
 * the left" question needs one extra step, because exactly one half of any mid is still
 * sorted. The trace names which half that is every time, then shows the range check
 * deciding between its two halves.
 */
@Component
public class RotatedArraySearchTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "search-rotated-sorted";
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
                        .help("A sorted array rotated at some pivot, e.g. [4,5,6,7,0,1,2]. Distinct values.")
                        .length(1, 64).values(-999, 999).distinct()
                        .defaultValue(List.of(4, 5, 6, 7, 0, 1, 2))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-999, 999)
                        .defaultValue(0)
                        .build());
    }

    /**
     * Target absent: the search must cross both the sorted-left and sorted-right cases
     * and still terminate at the miss line.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(6, 7, 1, 2, 3, 4, 5), "target", 10);
    }

    @Override
    public String annotatedCode() {
        return """
               public int search(int[] nums, int target) {
                   // @a init
                   int low = 0, high = nums.length - 1;
                   while (low <= high) {
                       // @a mid
                       int mid = low + (high - low) / 2;
                       // @a hit
                       if (nums[mid] == target) return mid;
                       // @a leftSorted
                       if (nums[low] <= nums[mid]) {
                           if (nums[low] <= target && target < nums[mid]) high = mid - 1;
                           else low = mid + 1;
                       } else {
                           // @a rightSorted
                           if (nums[mid] < target && target <= nums[high]) low = mid + 1;
                           else high = mid - 1;
                       }
                   }
                   // @a miss
                   return -1;
               }""";
    }

    /** Eliminated halves stay dimmed so the halving stays legible across the rotation. */
    private List<ArrayElement> window(int[] nums, int low, int high, int mid) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s;
            if (i < low || i > high) {
                s = "visited";          // eliminated
            } else if (i == mid) {
                s = "current";          // the probe
            } else {
                s = "target";           // still in range
            }
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int target = in.getInt("target");
        int low = 0;
        int high = nums.length - 1;

        emit.at("init").say("A sorted array was cut at a pivot and rotated. Find %d in [%d..%d].",
                        target, low, high)
                .var("target", target).var("low", low).var("high", high)
                .arrayState(window(nums, low, high, -1)).step();

        while (low <= high) {
            int mid = low + (high - low) / 2;

            emit.at("mid").say("Probe the middle of [%d..%d]: nums[%d] = %d.", low, high, mid, nums[mid])
                    .var("low", low).var("high", high).var("mid", mid).var("nums[mid]", nums[mid])
                    .arrayState(window(nums, low, high, mid)).step();

            if (nums[mid] == target) {
                emit.at("hit").say("nums[%d] = %d is the target. Found at index %d.", mid, target, mid)
                        .var("mid", mid).var("result", mid)
                        .arrayState(window(nums, mid, mid, mid)).step();
                return;
            }

            if (nums[low] <= nums[mid]) {
                boolean inside = nums[low] <= target && target < nums[mid];
                emit.at("leftSorted").say(
                                "%d ≤ nums[%d]=%d, so the LEFT half [%d..%d] is the sorted one (%d…%d). %s",
                                nums[low], mid, nums[mid], low, mid, nums[low], nums[mid],
                                inside
                                        ? String.format("%d lies inside it — search there.", target)
                                        : String.format("%d is not inside it, so it can only be to the right of %d.", target, mid))
                        .var("leftSorted", true)
                        .arrayState(window(nums, inside ? low : mid + 1, inside ? mid - 1 : high, -1)).step();
                if (inside) {
                    high = mid - 1;
                } else {
                    low = mid + 1;
                }
            } else {
                boolean inside = nums[mid] < target && target <= nums[high];
                emit.at("rightSorted").say(
                                "nums[%d]=%d < %d, so the rotation point sits in the left half and the RIGHT half [%d..%d] is sorted (%d…%d). %s",
                                mid, nums[mid], nums[low], mid, high, nums[mid], nums[high],
                                inside
                                        ? String.format("%d lies inside it — search there.", target)
                                        : String.format("%d is not inside it, so it can only be to the left of %d.", target, mid))
                        .var("rightSorted", true)
                        .arrayState(window(nums, inside ? mid + 1 : low, inside ? high : mid - 1, -1)).step();
                if (inside) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
        }

        emit.at("miss").say("The window closed around %d without ever landing on it — it is not in the array.", target)
                .var("result", -1).arrayState(window(nums, 0, -1, -1)).step();
    }
}
