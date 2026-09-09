package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * With duplicates allowed, {@link RotatedArraySearchTracer}'s "one half is always sorted"
 * rule can break: when {@code nums[low] == nums[mid] == nums[high]}, both halves LOOK
 * sorted and neither comparison can tell which one truly is. The fix shrinks the window by
 * one from each end instead of guessing — the only extra step this version needs.
 */
@Component
public class SearchRotatedSorted2Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "search-rotated-sorted-2";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Rotated sorted array (with duplicates)")
                        .length(1, 40).values(-99, 99)
                        .defaultValue(List.of(2, 5, 6, 0, 0, 1, 2))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-99, 99)
                        .defaultValue(6)
                        .build());
    }

    /**
     * All three of low/mid/high tie, forcing the shrink branch — and since the target is
     * absent, the window shrinks all the way to nothing rather than ever finding a match.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 0, 1, 1, 1), "target", 5);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean search(int[] nums, int target) {
                   // @a init
                   int low = 0, high = nums.length - 1;
                   while (low <= high) {
                       // @a mid
                       int mid = low + (high - low) / 2;
                       // @a hit
                       if (nums[mid] == target) return true;
                       if (nums[low] == nums[mid] && nums[mid] == nums[high]) {
                           // @a shrink
                           low++; high--; continue;
                       }
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
                   return false;
               }""";
    }

    private List<ArrayElement> window(int[] nums, int low, int high, int mid) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s;
            if (i < low || i > high) s = "visited";
            else if (i == mid) s = "current";
            else s = "target";
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int target = in.getInt("target");
        int low = 0, high = nums.length - 1;

        emit.at("init")
                .say("A sorted array with duplicates was rotated. Find %d in [%d..%d].", target, low, high)
                .var("target", target).var("low", low).var("high", high)
                .arrayState(window(nums, low, high, -1)).step();

        while (low <= high) {
            int mid = low + (high - low) / 2;
            emit.at("mid")
                    .say("Probe the middle of [%d..%d]: nums[%d] = %d.", low, high, mid, nums[mid])
                    .var("low", low).var("high", high).var("mid", mid)
                    .arrayState(window(nums, low, high, mid)).step();

            if (nums[mid] == target) {
                emit.at("hit").say("nums[%d] = %d is the target.", mid, target)
                        .var("result", true).arrayState(window(nums, mid, mid, mid)).step();
                return;
            }

            if (nums[low] == nums[mid] && nums[mid] == nums[high]) {
                emit.at("shrink")
                        .say("nums[low]=nums[mid]=nums[high]=%d — neither half is provably sorted. "
                                + "Shrink from both ends instead of guessing.", nums[mid])
                        .var("low", low + 1).var("high", high - 1)
                        .arrayState(window(nums, low + 1, high - 1, -1)).step();
                low++;
                high--;
                continue;
            }

            if (nums[low] <= nums[mid]) {
                boolean inside = nums[low] <= target && target < nums[mid];
                emit.at("leftSorted")
                        .say("Left half [%d..%d] is sorted (%d…%d). %s", low, mid, nums[low], nums[mid],
                                inside ? "Target lies inside it — search there."
                                        : "Target is outside it — search the right half.")
                        .var("leftSorted", true)
                        .arrayState(window(nums, inside ? low : mid + 1, inside ? mid - 1 : high, -1)).step();
                if (inside) high = mid - 1; else low = mid + 1;
            } else {
                boolean inside = nums[mid] < target && target <= nums[high];
                emit.at("rightSorted")
                        .say("Right half [%d..%d] is sorted (%d…%d). %s", mid, high, nums[mid], nums[high],
                                inside ? "Target lies inside it — search there."
                                        : "Target is outside it — search the left half.")
                        .var("rightSorted", true)
                        .arrayState(window(nums, inside ? mid + 1 : low, inside ? high : mid - 1, -1)).step();
                if (inside) low = mid + 1; else high = mid - 1;
            }
        }

        emit.at("miss").say("The window closed without ever landing on %d — it is not in the array.", target)
                .var("result", false).arrayState(window(nums, 0, -1, -1)).step();
    }
}
