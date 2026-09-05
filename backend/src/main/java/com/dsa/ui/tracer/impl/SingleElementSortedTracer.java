package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every duplicate pair sits at (even, odd) indices until the single element throws the
 * parity off - after it, every pair shifts to (odd, even). So checking whether {@code mid}'s
 * duplicate partner is where a same-parity pair "should" be tells you which side of the
 * single element you are standing on, without ever comparing mid against the target value
 * itself (there is no target value - the single element is discovered by parity alone).
 */
@Component
public class SingleElementSortedTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "single-element-sorted";
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
                        .help("Sorted, with every value appearing exactly twice except one. "
                                + "(Not machine-enforced: a generic sortedness constraint "
                                + "would let the contract harness pad this with distinct "
                                + "values and break the pairing invariant at the edge.)")
                        .length(1, 24).values(-999, 999)
                        .defaultValue(List.of(1, 1, 2, 3, 3, 4, 4, 8, 8))
                        .build());
    }

    /** The single element sits later, past index low..mid, so the search goes right instead of left. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 1, 2, 2, 3, 3, 4, 5, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public int singleNonDuplicate(int[] nums) {
                   int n = nums.length;
                   if (n == 1) return nums[0];
                   if (nums[0] != nums[1]) return nums[0];
                   if (nums[n - 1] != nums[n - 2]) return nums[n - 1];

                   // @a init
                   int low = 1, high = n - 2;
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       if (nums[mid] != nums[mid - 1] && nums[mid] != nums[mid + 1]) {
                           // @a foundSingle
                           return nums[mid];
                       }
                       if ((mid % 2 == 1 && nums[mid] == nums[mid - 1])
                               || (mid % 2 == 0 && nums[mid] == nums[mid + 1])) {
                           // @a goRight
                           low = mid + 1;
                       } else {
                           // @a goLeft
                           high = mid - 1;
                       }
                   }
                   return -1;
               }""";
    }

    private List<ArrayElement> window(int[] nums, int low, int high, int mid) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s = i == mid ? "current" : (i < low || i > high) ? "visited" : "default";
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;

        if (n == 1 || nums[0] != nums[1] || nums[n - 1] != nums[n - 2]) {
            int answer = n == 1 ? nums[0] : nums[0] != nums[1] ? nums[0] : nums[n - 1];
            int at = n == 1 ? 0 : nums[0] != nums[1] ? 0 : n - 1;
            emit.at("foundSingle")
                    .say(n == 1
                            ? "Only one element - it is trivially the single one."
                            : nums[0] != nums[1]
                                    ? "The first two elements already differ, so index 0 "
                                            + "never got a partner - it is the single element."
                                    : "The last two elements already differ, so the last "
                                            + "index never got a partner - it is the single "
                                            + "element.")
                    .var("answer", answer)
                    .arrayState(window(nums, at, at, at)).step();
            return;
        }

        int low = 1;
        int high = n - 2;

        emit.at("init")
                .say("The very first and last elements are already confirmed paired, so "
                        + "search the interior [%d,%d]. In a correctly-paired region every "
                        + "duplicate sits at (even,odd) - once that parity breaks, the "
                        + "single element is nearby.", low, high)
                .var("low", low).var("high", high)
                .arrayState(window(nums, low, high, -1)).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Range [%d,%d]. Probe index %d = %d.", low, high, mid, nums[mid])
                    .var("low", low).var("high", high).var("mid", mid)
                    .arrayState(window(nums, low, high, mid)).step();

            if (nums[mid] != nums[mid - 1] && nums[mid] != nums[mid + 1]) {
                emit.at("foundSingle")
                        .say("nums[%d]=%d matches neither neighbor - it is the single element.",
                                mid, nums[mid])
                        .var("answer", nums[mid])
                        .arrayState(window(nums, mid, mid, mid)).step();
                return;
            }

            boolean pairedAtExpectedParity = (mid % 2 == 1 && nums[mid] == nums[mid - 1])
                    || (mid % 2 == 0 && nums[mid] == nums[mid + 1]);
            if (pairedAtExpectedParity) {
                emit.at("goRight")
                        .say("Index %d pairs exactly where an (even,odd) pair should - "
                                + "everything up to here is still correctly paired, so the "
                                + "single element is to the right.", mid)
                        .var("low", mid + 1)
                        .arrayState(window(nums, low, high, mid)).step();
                low = mid + 1;
            } else {
                emit.at("goLeft")
                        .say("Index %d's pairing does not match the expected parity - the "
                                + "single element already happened before here, so it is to "
                                + "the left.", mid)
                        .var("high", mid - 1)
                        .arrayState(window(nums, low, high, mid)).step();
                high = mid - 1;
            }
        }
    }
}
