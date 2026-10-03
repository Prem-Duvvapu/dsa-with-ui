package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Max Consecutive Ones III (LeetCode 1004), traced on the owner's own accepted submission: a window
 * that never shrinks. When it holds more than k zeroes it slides one step (left and right both move),
 * keeping its length; it only grows when it is valid, so maxLen ends as the longest valid window.
 * O(n).
 */
@Component
public class MaxConsecutiveOnes3Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "max-consecutive-ones-3";
    }

    @Override
    public DsType dsType() {
        return DsType.WINDOW;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Binary Array")
                        .help("Array of 0s and 1s.")
                        .length(1, 30).values(0, 1)
                        .defaultValue(List.of(1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K (max zeroes to flip)")
                        .help("Maximum number of consecutive zeroes you can flip to 1.")
                        .range(0, 30)
                        .defaultValue(2)
                        .build());
    }

    /** Alternate input with different distribution and k=3. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "nums", List.of(0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1),
                "k", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public int longestOnes(int[] nums, int k) {
                       // @a init
                       int n=nums.length;
                       int maxLen=0;
                       int left=0;
                       int right=0;
                       int zeroesCnt=0;

                       while (right<n) {
                           if (nums[right]==0)
                               // @a zero
                               zeroesCnt++;

                           if (zeroesCnt > k) {
                               if (nums[left]==0)
                                   zeroesCnt--;

                               // @a slide
                               left++;
                           } else {
                               // @a valid
                               int currLen=right-left+1;
                               maxLen=Math.max(maxLen,currLen);
                           }

                           right++;
                       }

                       // @a done
                       return maxLen;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");
        int n = nums.length;
        int maxLen = 0;
        int left = 0;
        int right = 0;
        int zeroesCnt = 0;

        emit.at("init").say("Flip at most k = %d zeroes. The window nums[left..right] may hold at most %d zero%s.",
                        k, k, Narration.s(k))
                .var("k", k).var("maxLen", 0).arrayState(WindowCells.of(nums, -1, -1)).step();

        while (right < n) {
            if (nums[right] == 0) {
                zeroesCnt++;
                emit.at("zero").say("nums[%d] is 0: the window now holds %d zero%s.", right, zeroesCnt, Narration.s(zeroesCnt))
                        .var("left", left).var("right", right).var("zeroesCnt", zeroesCnt).var("maxLen", maxLen)
                        .arrayState(WindowCells.of(nums, left, right)).step();
            }
            if (zeroesCnt > k) {
                boolean droppedZero = nums[left] == 0;
                if (droppedZero) zeroesCnt--;
                left++;
                emit.at("slide").say("%d zeros is more than k = %d. Slide the window instead of growing it: drop "
                                + "nums[%d]%s. Its length stays %d.", zeroesCnt + (droppedZero ? 1 : 0), k, left - 1,
                                droppedZero ? " (a zero, so zeroesCnt = " + zeroesCnt + ")" : "", right - left + 1)
                        .var("left", left).var("right", right).var("zeroesCnt", zeroesCnt).var("maxLen", maxLen)
                        .arrayState(WindowCells.of(nums, left, right)).step();
            } else {
                int currLen = right - left + 1;
                maxLen = Math.max(maxLen, currLen);
                emit.at("valid").say("The window [%d, %d] holds %d zero%s, within k: length %d. maxLen = %d.",
                                left, right, zeroesCnt, Narration.s(zeroesCnt), currLen, maxLen)
                        .var("left", left).var("right", right).var("zeroesCnt", zeroesCnt).var("maxLen", maxLen)
                        .arrayState(WindowCells.of(nums, left, right)).step();
            }
            right++;
        }

        emit.at("done").say("The longest run of 1s with at most %d zero%s flipped is %d.", k, Narration.s(k), maxLen)
                .var("maxLen", maxLen).var("answer", maxLen).arrayState(WindowCells.of(nums, -1, -1)).step();
    }
}
