package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Upper bound: the leftmost index whose value is strictly greater than the target. Same
 * "record a candidate, then keep narrowing left" shape as {@link LowerBoundTracer}, but the
 * predicate excludes an exact match rather than accepting it - a value equal to the target
 * is rejected, not recorded.
 */
@Component
public class UpperBoundTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "upper-bound";
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
                        .help("Duplicates equal to the target must all be rejected, not just the first one.")
                        .length(1, 64).values(-999, 999).sorted()
                        .defaultValue(List.of(1, 2, 2, 2, 3, 5, 8))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-999, 999)
                        .defaultValue(2)
                        .build());
    }

    /** Every element is at or below the target, so every step rejects and the answer is n (not found). */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(2, 4, 6, 8, 10, 12, 14), "target", 100);
    }

    @Override
    public String annotatedCode() {
        return """
               public int upperBound(int[] arr, int n, int x) {
                   // @a init
                   int low = 0, high = n - 1, ans = n;
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       if (arr[mid] > x) {
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

    /** The best candidate so far, if any, stays visibly marked while the probe moves on. */
    private List<ArrayElement> window(int[] nums, int low, int high, int mid, int ans) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s;
            if (i == mid) {
                s = "current";
            } else if (i == ans) {
                s = "done";
            } else if (i < low || i > high) {
                s = "visited";
            } else {
                s = "target";
            }
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int target = in.getInt("target");
        int n = nums.length;
        int low = 0;
        int high = n - 1;
        int ans = n;

        emit.at("init")
                .say("Find the leftmost index whose value is strictly greater than %d. No "
                        + "candidate found yet, so ans defaults to %d (one past the end).",
                        target, n)
                .var("target", target).var("low", low).var("high", high).var("ans", "n")
                .arrayState(window(nums, low, high, -1, -1)).step();

        while (low <= high) {
            int mid = (low + high) / 2;

            emit.at("mid")
                    .say("Range is [%d, %d]. Probe the middle: nums[%d] = %d.",
                            low, high, mid, nums[mid])
                    .var("low", low).var("high", high).var("mid", mid).var("nums[mid]", nums[mid])
                    .arrayState(window(nums, low, high, mid, ans)).step();

            if (nums[mid] > target) {
                ans = mid;
                emit.at("take")
                        .say("%d > %d, so index %d is strictly greater - record it as the best "
                                + "candidate so far, then keep looking left for an earlier one.",
                                nums[mid], target, mid)
                        .var("ans", ans).var("high", mid - 1)
                        .arrayState(window(nums, low, mid - 1, -1, ans)).step();
                high = mid - 1;
            } else {
                emit.at("reject")
                        .say("%d <= %d, so index %d does not qualify (equal is not enough for "
                                + "upper bound). The answer must be to the right.",
                                nums[mid], target, mid)
                        .var("low", mid + 1)
                        .arrayState(window(nums, mid + 1, high, -1, ans)).step();
                low = mid + 1;
            }
        }

        emit.at("done")
                .say("low passed high with nothing left to check. %s",
                        ans == n
                                ? "Every element is at or below the target, so there is no upper bound."
                                : "ans=%d is the leftmost index strictly greater than %d.".formatted(ans, target))
                .var("ans", ans == n ? "n" : String.valueOf(ans))
                .arrayState(window(nums, 0, -1, -1, ans)).step();
    }
}
