package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Search Insert Position is the lower bound of the target: the leftmost index whose value
 * is not below it. When the target is present that index IS its position; when absent it
 * is exactly where an insert would keep the array sorted — the same invariant, no separate
 * "not found" case to special-case.
 */
@Component
public class SearchInsertPositionTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "search-insert-position";
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
                        .defaultValue(List.of(1, 3, 5, 6))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-999, 999)
                        .defaultValue(5)
                        .build());
    }

    /** Target absent: the answer is an insertion point, not a match. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 3, 5, 6), "target", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public int searchInsert(int[] nums, int target) {
                   // @a init
                   int low = 0, high = nums.length - 1, ans = nums.length;
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       if (nums[mid] >= target) {
                           // @a take
                           ans = mid;
                           high = mid - 1;
                       } else {
                           // @a reject
                           low = mid + 1;
                       }
                   }
                   // @a done
                   return ans;
               }""";
    }

    private List<ArrayElement> window(int[] nums, int low, int high, int mid, int ans) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s;
            if (i == mid) s = "current";
            else if (i == ans) s = "done";
            else if (i < low || i > high) s = "visited";
            else s = "target";
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int target = in.getInt("target");
        int low = 0, high = nums.length - 1, ans = nums.length;

        emit.at("init")
                .say("Find where %d belongs in the sorted array — its index if present, "
                        + "otherwise the index it would be inserted at.", target)
                .var("target", target).var("low", low).var("high", high)
                .arrayState(window(nums, low, high, -1, -1)).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Range [%d, %d]. nums[%d] = %d.", low, high, mid, nums[mid])
                    .var("low", low).var("high", high).var("mid", mid)
                    .arrayState(window(nums, low, high, mid, ans)).step();

            if (nums[mid] >= target) {
                ans = mid;
                emit.at("take")
                        .say("%d >= %d, so index %d is not too small — record it and look left "
                                + "for an even earlier fit.", nums[mid], target, mid)
                        .var("ans", ans).var("high", mid - 1)
                        .arrayState(window(nums, low, mid - 1, -1, ans)).step();
                high = mid - 1;
            } else {
                emit.at("reject")
                        .say("%d < %d, so index %d is too small — the answer is to the right.",
                                nums[mid], target, mid)
                        .var("low", mid + 1)
                        .arrayState(window(nums, mid + 1, high, -1, ans)).step();
                low = mid + 1;
            }
        }

        emit.at("done")
                .say(ans < nums.length && nums[ans] == target
                        ? String.format("%d found at index %d.", target, ans)
                        : String.format("%d is absent — it would be inserted at index %d.", target, ans))
                .var("answer", ans)
                .arrayState(window(nums, 0, -1, -1, ans)).step();
    }
}
