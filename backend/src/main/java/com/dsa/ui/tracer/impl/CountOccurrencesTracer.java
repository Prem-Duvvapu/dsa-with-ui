package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Counting occurrences never needs to touch every matching element — the span's two
 * endpoints (via lower and upper bound) subtract straight into a count. Missing entirely
 * collapses to 0 without a special case, since an empty span has last &lt; first.
 */
@Component
public class CountOccurrencesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-occurrences";
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
                        .length(1, 64).values(-999, 999).sorted()
                        .defaultValue(List.of(1, 2, 2, 2, 2, 3, 4))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-999, 999)
                        .defaultValue(2)
                        .build());
    }

    /** Target absent: the lower bound lands one past every element that is too small, and never equals target. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 2, 2, 2, 3, 4), "target", 5);
    }

    @Override
    public String annotatedCode() {
        return """
               public int count(int[] arr, int x) {
                   int lb = lowerBound(arr, x);
                   if (lb == arr.length || arr[lb] != x) {
                       // @a absent
                       return 0;
                   }
                   int ub = upperBound(arr, x);
                   // @a done
                   return ub - lb;
               }

               private int lowerBound(int[] arr, int x) {
                   int low = 0, high = arr.length - 1, ans = arr.length;
                   while (low <= high) {
                       // @a lowerMid
                       int mid = (low + high) / 2;
                       if (arr[mid] >= x) { ans = mid; high = mid - 1; }
                       else low = mid + 1;
                   }
                   return ans;
               }

               private int upperBound(int[] arr, int x) {
                   int low = 0, high = arr.length - 1, ans = arr.length;
                   while (low <= high) {
                       // @a upperMid
                       int mid = (low + high) / 2;
                       if (arr[mid] > x) { ans = mid; high = mid - 1; }
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
        int n = nums.length;

        int low = 0, high = n - 1, lb = n;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (nums[mid] >= target) {
                lb = mid;
                emit.at("lowerMid")
                        .say("nums[%d]=%d >= %d — candidate span start, keep looking left.", mid, nums[mid], target)
                        .var("lb", lb).var("high", mid - 1)
                        .arrayState(window(nums, low, mid - 1, mid)).step();
                high = mid - 1;
            } else {
                emit.at("lowerMid")
                        .say("nums[%d]=%d < %d — too small, look right.", mid, nums[mid], target)
                        .var("low", mid + 1)
                        .arrayState(window(nums, mid + 1, high, mid)).step();
                low = mid + 1;
            }
        }

        if (lb == n || nums[lb] != target) {
            emit.at("absent")
                    .say("The lower bound (index %s) is not %d itself — %d never appears. Count = 0.",
                            lb == n ? "n" : String.valueOf(lb), target, target)
                    .var("count", 0)
                    .arrayState(window(nums, 0, -1, -1)).step();
            return;
        }

        low = 0; high = n - 1;
        int ub = n;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (nums[mid] > target) {
                ub = mid;
                emit.at("upperMid")
                        .say("nums[%d]=%d > %d — candidate span end, keep looking left.", mid, nums[mid], target)
                        .var("ub", ub).var("high", mid - 1)
                        .arrayState(window(nums, low, mid - 1, mid)).step();
                high = mid - 1;
            } else {
                emit.at("upperMid")
                        .say("nums[%d]=%d <= %d — still in span, look right.", mid, nums[mid], target)
                        .var("low", mid + 1)
                        .arrayState(window(nums, mid + 1, high, mid)).step();
                low = mid + 1;
            }
        }

        int count = ub - lb;
        emit.at("done")
                .say("Span [%d, %d) holds %d occurrences of %d.", lb, ub, count, target)
                .var("count", count)
                .arrayState(window(nums, lb, ub - 1, -1)).step();
    }
}
