package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Two binary searches bracket every occurrence of the target: the first hunts left after
 * every match, the second hunts right. If the first search comes back empty, the target is
 * absent altogether and the second search never needs to run.
 */
@Component
public class FirstLastOccurrenceTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "first-last-occurrence";
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
                        .help("Duplicates are the point — first and last occurrence only differ when the target repeats.")
                        .length(1, 64).values(-999, 999).sorted()
                        .defaultValue(List.of(5, 7, 7, 8, 8, 8, 10))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-999, 999)
                        .defaultValue(8)
                        .build());
    }

    /** Target absent: the first search finds nothing, so the last search is skipped entirely. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(5, 7, 7, 8, 8, 8, 10), "target", 6);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] searchRange(int[] nums, int target) {
                   int first = firstOccurrence(nums, target);
                   if (first == -1) {
                       // @a absent
                       return new int[]{-1, -1};
                   }
                   int last = lastOccurrence(nums, target);
                   // @a done
                   return new int[]{first, last};
               }

               private int firstOccurrence(int[] nums, int target) {
                   int low = 0, high = nums.length - 1, ans = -1;
                   while (low <= high) {
                       // @a firstMid
                       int mid = (low + high) / 2;
                       if (nums[mid] == target) { ans = mid; high = mid - 1; }
                       else if (nums[mid] < target) low = mid + 1;
                       else high = mid - 1;
                   }
                   return ans;
               }

               private int lastOccurrence(int[] nums, int target) {
                   int low = 0, high = nums.length - 1, ans = -1;
                   while (low <= high) {
                       // @a lastMid
                       int mid = (low + high) / 2;
                       if (nums[mid] == target) { ans = mid; low = mid + 1; }
                       else if (nums[mid] < target) low = mid + 1;
                       else high = mid - 1;
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

        int low = 0, high = nums.length - 1, first = -1;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (nums[mid] == target) {
                first = mid;
                emit.at("firstMid")
                        .say("nums[%d]=%d matches %d — record it, then keep hunting left for an earlier one.",
                                mid, nums[mid], target)
                        .var("first", first).var("high", mid - 1)
                        .arrayState(window(nums, low, mid - 1, mid)).step();
                high = mid - 1;
            } else if (nums[mid] < target) {
                emit.at("firstMid")
                        .say("nums[%d]=%d < %d — too small, look right.", mid, nums[mid], target)
                        .var("low", mid + 1)
                        .arrayState(window(nums, mid + 1, high, mid)).step();
                low = mid + 1;
            } else {
                emit.at("firstMid")
                        .say("nums[%d]=%d > %d — too big, look left.", mid, nums[mid], target)
                        .var("high", mid - 1)
                        .arrayState(window(nums, low, mid - 1, mid)).step();
                high = mid - 1;
            }
        }

        if (first == -1) {
            emit.at("absent")
                    .say("The first search never matched — %d is not in the array. Skip the last-occurrence search entirely.", target)
                    .var("result", "[-1, -1]")
                    .arrayState(window(nums, 0, -1, -1)).step();
            return;
        }

        low = 0; high = nums.length - 1;
        int last = -1;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (nums[mid] == target) {
                last = mid;
                emit.at("lastMid")
                        .say("nums[%d]=%d matches %d — record it, then keep hunting right for a later one.",
                                mid, nums[mid], target)
                        .var("last", last).var("low", mid + 1)
                        .arrayState(window(nums, mid + 1, high, mid)).step();
                low = mid + 1;
            } else if (nums[mid] < target) {
                emit.at("lastMid")
                        .say("nums[%d]=%d < %d — too small, look right.", mid, nums[mid], target)
                        .var("low", mid + 1)
                        .arrayState(window(nums, mid + 1, high, mid)).step();
                low = mid + 1;
            } else {
                emit.at("lastMid")
                        .say("nums[%d]=%d > %d — too big, look left.", mid, nums[mid], target)
                        .var("high", mid - 1)
                        .arrayState(window(nums, low, mid - 1, mid)).step();
                high = mid - 1;
            }
        }

        emit.at("done")
                .say("%d spans indices [%d, %d].", target, first, last)
                .var("first", first).var("last", last)
                .arrayState(window(nums, first, last, -1)).step();
    }
}
