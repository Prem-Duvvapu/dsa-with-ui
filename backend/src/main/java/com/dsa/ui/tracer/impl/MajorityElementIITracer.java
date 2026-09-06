package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Majority Element II: find all elements appearing more than ⌊N/3⌋ times
 * using Extended Boyer-Moore Voting (at most 2 candidates).
 *
 * <p>Why at most 2? If an element appears more than N/3 times, there can
 * be at most 2 such elements (3 × (>N/3) > N). Pass 1 finds candidates
 * via paired cancellation; Pass 2 verifies their actual counts.
 */
@Component
public class MajorityElementIITracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "majority-element-ii";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("Find elements appearing > ⌊N/3⌋ times.")
                        .length(1, 30).values(-50, 50)
                        .defaultValue(List.of(3, 2, 3))
                        .build());
    }

    /** Two majority elements instead of one, different array length. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 1, 1, 3, 3, 2, 2, 2));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> majorityElement(int[] nums) {
                   // @a init
                   int cnt1 = 0, cnt2 = 0;
                   int el1 = Integer.MIN_VALUE, el2 = Integer.MIN_VALUE;
                   // Pass 1: find up to 2 candidates
                   for (int i = 0; i < nums.length; i++) {
                       if (cnt1 == 0 && nums[i] != el2) {
                           // @a newCandidate1
                           cnt1 = 1; el1 = nums[i];
                       } else if (cnt2 == 0 && nums[i] != el1) {
                           // @a newCandidate2
                           cnt2 = 1; el2 = nums[i];
                       } else if (nums[i] == el1) {
                           // @a boost1
                           cnt1++;
                       } else if (nums[i] == el2) {
                           // @a boost2
                           cnt2++;
                       } else {
                           // @a cancel
                           cnt1--; cnt2--;
                       }
                   }
                   // Pass 2: verify candidates
                   // @a startVerify
                   List<Integer> ls = new ArrayList<>();
                   cnt1 = 0; cnt2 = 0;
                   for (int v : nums) {
                       // @a count
                       if (v == el1) cnt1++;
                       if (v == el2) cnt2++;
                   }
                   int mini = nums.length / 3;
                   // @a result
                   if (cnt1 > mini) ls.add(el1);
                   if (cnt2 > mini) ls.add(el2);
                   return ls;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int cnt1 = 0, cnt2 = 0;
        int el1 = Integer.MIN_VALUE, el2 = Integer.MIN_VALUE;

        emit.at("init")
                .say("Find elements appearing > ⌊%d/3⌋ = %d times. Two candidate slots, both empty.",
                        nums.length, nums.length / 3)
                .var("el1", "∅").var("cnt1", 0).var("el2", "∅").var("cnt2", 0)
                .array(nums)
                .step();

        for (int i = 0; i < nums.length; i++) {
            if (cnt1 == 0 && nums[i] != el2) {
                cnt1 = 1;
                el1 = nums[i];
                emit.at("newCandidate1")
                        .say("Slot 1 is empty and %d ≠ el2 → new candidate el1 = %d, cnt1 = 1.", nums[i], el1)
                        .var("el1", el1).var("cnt1", cnt1).var("el2", el2 == Integer.MIN_VALUE ? "∅" : String.valueOf(el2)).var("cnt2", cnt2)
                        .array(nums, i)
                        .step();
            } else if (cnt2 == 0 && nums[i] != el1) {
                cnt2 = 1;
                el2 = nums[i];
                emit.at("newCandidate2")
                        .say("Slot 2 is empty and %d ≠ el1 → new candidate el2 = %d, cnt2 = 1.", nums[i], el2)
                        .var("el1", el1).var("cnt1", cnt1).var("el2", el2).var("cnt2", cnt2)
                        .array(nums, i)
                        .step();
            } else if (nums[i] == el1) {
                cnt1++;
                emit.at("boost1")
                        .say("nums[%d]=%d matches el1 → cnt1++ = %d.", i, nums[i], cnt1)
                        .var("el1", el1).var("cnt1", cnt1)
                        .array(nums, i)
                        .step();
            } else if (nums[i] == el2) {
                cnt2++;
                emit.at("boost2")
                        .say("nums[%d]=%d matches el2 → cnt2++ = %d.", i, nums[i], cnt2)
                        .var("el2", el2).var("cnt2", cnt2)
                        .array(nums, i)
                        .step();
            } else {
                cnt1--;
                cnt2--;
                emit.at("cancel")
                        .say("nums[%d]=%d matches neither candidate → cancel one vote from each: cnt1=%d, cnt2=%d.",
                                i, nums[i], cnt1, cnt2)
                        .var("cnt1", cnt1).var("cnt2", cnt2)
                        .array(nums, i)
                        .step();
            }
        }

        // Pass 2: verify
        emit.at("startVerify")
                .say("Candidates: el1=%d, el2=%d. Pass 2: count their actual frequencies.",
                        el1, el2 == Integer.MIN_VALUE ? -1 : el2)
                .var("el1", el1).var("el2", el2 == Integer.MIN_VALUE ? "∅" : String.valueOf(el2))
                .array(nums)
                .step();

        List<Integer> ls = new ArrayList<>();
        cnt1 = 0;
        cnt2 = 0;
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == el1) cnt1++;
            if (nums[i] == el2) cnt2++;
            emit.at("count")
                    .say("nums[%d]=%d: cnt1(%d)=%d, cnt2(%s)=%d.",
                            i, nums[i], el1, cnt1,
                            el2 == Integer.MIN_VALUE ? "∅" : String.valueOf(el2), cnt2)
                    .var("cnt1", cnt1).var("cnt2", cnt2)
                    .array(nums, i)
                    .step();
        }

        int mini = nums.length / 3;
        if (cnt1 > mini) ls.add(el1);
        if (cnt2 > mini && el2 != Integer.MIN_VALUE) ls.add(el2);

        emit.at("result")
                .say("Threshold = %d. el1=%d appears %d times%s. el2=%s appears %d times%s. Result: %s.",
                        mini, el1, cnt1, cnt1 > mini ? " ✓" : " ✗",
                        el2 == Integer.MIN_VALUE ? "∅" : String.valueOf(el2), cnt2,
                        cnt2 > mini ? " ✓" : " ✗", ls)
                .var("result", ls.toString())
                .array(nums)
                .step();
    }
}
