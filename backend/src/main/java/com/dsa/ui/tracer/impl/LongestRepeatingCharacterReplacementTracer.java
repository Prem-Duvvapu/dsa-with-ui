package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Longest Repeating Character Replacement (LeetCode 424), traced on the owner's own accepted
 * submission. A window is good when its length minus its most frequent letter's count is at most k
 * - those are the letters to replace. The window never shrinks: an over-budget window slides one
 * step. {@code maxFreq} is never lowered when the window slides; it does not need to be, because
 * only a window with a HIGHER maxFreq can beat maxLen. O(n).
 *
 * <p>The code indexes {@code freq[ch - 'A']}, so the input is LeetCode's: uppercase A-Z only.
 */
@Component
public class LongestRepeatingCharacterReplacementTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "longest-repeating-character-replacement";
    }

    @Override
    public DsType dsType() {
        return DsType.WINDOW;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("s", FieldType.STRING)
                        .label("String")
                        .help("Uppercase letters A-Z only.")
                        .length(1, 40)
                        .constraint("pattern", "[A-Z]{1,40}")
                        .constraint("patternHint", "Uppercase letters A-Z only.")
                        // LeetCode's own second example, and it has to be something like
                        // it: "ABAB" with k=2 is entirely replaceable, so the window grew
                        // from 0 to 3 and left never moved. Nine steps of a sliding window
                        // that never slid, with the shrink branch - the slide itself - dead
                        // on the input every visitor sees first. This one slides three
                        // times on the way to the same answer of 4 (the window slides, never shrinks).
                        .defaultValue("AABABBA")
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K")
                        .help("Characters allowed to replace.")
                        .range(0, 40)
                        .defaultValue(1)
                        .build());
    }

    /** A different string and a smaller K - the window is bounded by a run, not free replacement. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "ABBBAAAB", "k", 0);
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public int characterReplacement(String s, int k) {
                       // @a init
                       int n=s.length();
                       int left=0;
                       int right=0;
                       int maxLen=0;
                       int[] freq=new int[26];
                       int maxFreq=0;

                       while (right<n) {
                           // @a add
                           char ch=s.charAt(right);
                           freq[ch-'A']++;
                           maxFreq=Math.max(maxFreq,freq[ch-'A']);

                           if ((right-left+1)-maxFreq > k) {
                               // @a slide
                               freq[s.charAt(left)-'A']--;
                               left++;
                           }

                           if ((right-left+1)-maxFreq <= k)
                               // @a valid
                               maxLen=Math.max(maxLen,right-left+1);

                           right++;
                       }

                       // @a done
                       return maxLen;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int k = in.getInt("k");
        int n = s.length();
        int left = 0;
        int right = 0;
        int maxLen = 0;
        int[] freq = new int[26];
        int maxFreq = 0;

        emit.at("init").say("A window can be made one repeated letter when (its length - the count of its most "
                        + "common letter) <= k = %d.", k)
                .var("k", k).var("maxLen", 0).arrayState(WindowCells.of(s, -1, -1)).step();

        while (right < n) {
            char ch = s.charAt(right);
            freq[ch - 'A']++;
            maxFreq = Math.max(maxFreq, freq[ch - 'A']);
            emit.at("add").say("Take in '%c' at %d: it appears %d time%s in the window; maxFreq = %d.",
                            ch, right, freq[ch - 'A'], Narration.s(freq[ch - 'A']), maxFreq)
                    .var("left", left).var("right", right).var("maxFreq", maxFreq).var("maxLen", maxLen)
                    .arrayState(WindowCells.of(s, left, right)).step();

            if ((right - left + 1) - maxFreq > k) {
                char out = s.charAt(left);
                freq[out - 'A']--;
                left++;
                emit.at("slide").say("Length %d - maxFreq %d = %d replacements, more than k = %d. Slide: drop '%c' at %d, "
                                + "so the window keeps length %d.", right - left + 2, maxFreq, right - left + 2 - maxFreq,
                                k, out, left - 1, right - left + 1)
                        .var("left", left).var("right", right).var("maxFreq", maxFreq).var("maxLen", maxLen)
                        .arrayState(WindowCells.of(s, left, right)).step();
            }
            if ((right - left + 1) - maxFreq <= k) {
                maxLen = Math.max(maxLen, right - left + 1);
                emit.at("valid").say("Window [%d, %d] needs %d replacement%s, within k: maxLen = %d.",
                                left, right, (right - left + 1) - maxFreq, Narration.s((right - left + 1) - maxFreq), maxLen)
                        .var("left", left).var("right", right).var("maxFreq", maxFreq).var("maxLen", maxLen)
                        .arrayState(WindowCells.of(s, left, right)).step();
            }
            right++;
        }

        emit.at("done").say("The longest window that %d replacement%s can turn into one letter has length %d.",
                        k, Narration.s(k), maxLen)
                .var("maxLen", maxLen).var("answer", maxLen).arrayState(WindowCells.of(s, -1, -1)).step();
    }
}
