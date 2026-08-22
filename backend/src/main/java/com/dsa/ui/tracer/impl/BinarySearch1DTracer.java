package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * The first genuinely distinct trace in the Binary Search category, where 31 of 32
 * problems previously delegated to a single hardcoded search over {1,3,5,7,9,11,13}.
 */
@Component
public class BinarySearch1DTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "binary-search-1d";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Sorted array")
                        .help("Binary search only works on sorted input, so this is enforced.")
                        .length(1, 64).values(-999, 999).sorted()
                        .defaultValue(List.of(1, 3, 5, 7, 9, 11, 13))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-999, 999)
                        .defaultValue(13)
                        .build());
    }

    /** 15 is absent, so the search narrows to nothing and reports a miss. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(2, 4, 6, 8, 10, 12, 14, 16), "target", 15);
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
                       // @a right
                       if (nums[mid] < target) low = mid + 1;
                       // @a left
                       else high = mid - 1;
                   }
                   // @a miss
                   return -1;
               }""";
    }

    /** Eliminated halves stay visible so the halving is legible, not just implied. */
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

        emit.using("Array");
        emit.at("init").say("Search the whole array for %d. low = 0, high = %d.", target, high)
                .var("target", target).var("low", low).var("high", high)
                .arrayState(window(nums, low, high, -1)).step();

        while (low <= high) {
            int mid = low + (high - low) / 2;

            emit.at("mid").say("Range is [%d, %d] — %d value%s left. Probe the middle: nums[%d] = %d.",
                            low, high, high - low + 1, high == low ? "" : "s", mid, nums[mid])
                    .var("low", low).var("high", high).var("mid", mid).var("nums[mid]", nums[mid])
                    .arrayState(window(nums, low, high, mid)).step();

            if (nums[mid] == target) {
                emit.at("hit").say("nums[%d] = %d is the target. Found at index %d.", mid, target, mid)
                        .var("mid", mid).var("result", mid)
                        .arrayState(window(nums, mid, mid, mid)).step();
                return;
            }

            if (nums[mid] < target) {
                emit.at("right").say("%d < %d, so the target is to the right. Discard indices %d..%d.",
                                nums[mid], target, low, mid)
                        .var("low", mid + 1).var("high", high)
                        .arrayState(window(nums, mid + 1, high, -1)).step();
                low = mid + 1;
            } else {
                emit.at("left").say("%d > %d, so the target is to the left. Discard indices %d..%d.",
                                nums[mid], target, mid, high)
                        .var("low", low).var("high", mid - 1)
                        .arrayState(window(nums, low, mid - 1, -1)).step();
                high = mid - 1;
            }
        }

        emit.at("miss").say("low passed high with nothing left to check. %d is not in the array.", target)
                .var("result", -1).arrayState(window(nums, 0, -1, -1)).step();
    }
}
