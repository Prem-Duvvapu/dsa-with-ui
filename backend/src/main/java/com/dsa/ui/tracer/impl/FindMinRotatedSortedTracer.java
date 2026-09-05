package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A rotated sorted array splits into two halves at every midpoint, and exactly one of
 * them is genuinely sorted (nums[low] &lt;= nums[mid]). The minimum of the whole array must
 * be the smaller of "the first element of the sorted half" and "whatever the unsorted half
 * still holds," so each iteration folds one comparison into the running answer and then
 * discards the half that cannot contain anything smaller.
 */
@Component
public class FindMinRotatedSortedTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "find-min-rotated-sorted";
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
                        .help("Unique values, sorted then rotated at some pivot.")
                        .length(1, 20).values(-999, 999).distinct()
                        .defaultValue(List.of(6, 7, 1, 2, 3, 4, 5))
                        .build());
    }

    /** No rotation at all: nums[low] <= nums[mid] holds every time, so the unsorted branch never fires. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3, 4, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public int findMin(int[] nums) {
                   // @a init
                   int low = 0, high = nums.length - 1, ans = Integer.MAX_VALUE;
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       if (nums[low] <= nums[mid]) {
                           // @a leftSorted
                           ans = Math.min(ans, nums[low]);
                           low = mid + 1;
                       } else {
                           // @a rightUnsorted
                           ans = Math.min(ans, nums[mid]);
                           high = mid - 1;
                       }
                   }
                   // @a done
                   return ans;
               }""";
    }

    private List<ArrayElement> window(int[] nums, int low, int high, int mid) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s = i == mid ? "current" : i == low ? "target" : (i < low || i > high) ? "visited" : "default";
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int low = 0;
        int high = nums.length - 1;
        int ans = Integer.MAX_VALUE;

        emit.at("init")
                .say("Search [0,%d]. The minimum could be nums[low] of whichever half is "
                        + "genuinely sorted, so track the best candidate seen so far.", high)
                .var("low", low).var("high", high).var("ans", "infinity")
                .arrayState(window(nums, low, high, -1)).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Range [%d,%d]. nums[low]=%d, nums[mid]=%d.", low, high, nums[low], nums[mid])
                    .var("low", low).var("high", high).var("mid", mid)
                    .arrayState(window(nums, low, high, mid)).step();

            if (nums[low] <= nums[mid]) {
                ans = Math.min(ans, nums[low]);
                emit.at("leftSorted")
                        .say("nums[%d]=%d <= nums[%d]=%d: the left half [%d,%d] is genuinely "
                                + "sorted, so its own first element is its minimum. Fold it "
                                + "in (ans=%d) and discard the whole sorted half.",
                                low, nums[low], mid, nums[mid], low, mid, ans)
                        .var("ans", ans).var("low", mid + 1)
                        .arrayState(window(nums, low, high, mid)).step();
                low = mid + 1;
            } else {
                ans = Math.min(ans, nums[mid]);
                emit.at("rightUnsorted")
                        .say("nums[%d]=%d > nums[%d]=%d: the left half is NOT sorted, so the "
                                + "rotation point - and the true minimum - is at or before "
                                + "mid. Fold nums[mid] in (ans=%d) and search the left half.",
                                low, nums[low], mid, nums[mid], ans)
                        .var("ans", ans).var("high", mid - 1)
                        .arrayState(window(nums, low, high, mid)).step();
                high = mid - 1;
            }
        }

        emit.at("done")
                .say("low passed high. The minimum is %d.", ans)
                .var("answer", ans)
                .arrayState(window(nums, 0, -1, -1)).step();
    }
}
