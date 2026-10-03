package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Longest Substring Without Repeating Characters (LeetCode 3), traced on the owner's own accepted
 * submission: {@code lastSeen[ch]} remembers where each character last appeared, and when the
 * window's next character was seen at or after {@code left}, {@code left} jumps just past it. Each
 * index enters once and left never moves back, so O(n).
 *
 * <p>The owner has two submissions with the same idea; this is the later {@code lastSeen[256]}
 * one. Indexing by character needs ASCII (LeetCode's constraint), so the input is limited to
 * printable ASCII.
 */
@Component
public class LongestSubstringWithoutRepeatingTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "longest-substring-without-repeating";
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
                        .help("Printable ASCII characters (letters, digits, symbols, spaces).")
                        .length(1, 40)
                        .constraint("pattern", "[ -~]{1,40}")
                        .constraint("patternHint", "Printable ASCII characters only (letters, digits, symbols, spaces).")
                        .defaultValue("abcabcbb")
                        .build());
    }

    /** Alternate string exercising consecutive duplicates and middle longest window. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "pwwkew");
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public int lengthOfLongestSubstring(String s) {
                       // @a init
                       int n = s.length();
                       int left = 0;
                       int right = 0;
                       int maxLength = 0;
                       int currLength = 0;
                       int[] lastSeen = new int[256];
                       Arrays.fill(lastSeen,-1);

                       while (right < n) {
                           char ch = s.charAt(right);
                           if (lastSeen[ch] != -1) {
                               // @a seenBefore
                               left = Math.max(left, lastSeen[ch]+1);
                           }

                           // @a extend
                           currLength = (right - left + 1);
                           maxLength = Math.max(maxLength, currLength);
                           lastSeen[ch] = right;
                           right++;
                       }

                       // @a done
                       return maxLength;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int n = s.length();
        int left = 0;
        int right = 0;
        int maxLength = 0;
        int[] lastSeen = new int[256];
        Arrays.fill(lastSeen, -1);

        emit.at("init").say("The window is s[left..right]. lastSeen remembers the last index of every character.")
                .var("left", 0).var("maxLength", 0).arrayState(WindowCells.of(s, -1, -1)).step();

        while (right < n) {
            char ch = s.charAt(right);
            if (lastSeen[ch] != -1) {
                int before = left;
                left = Math.max(left, lastSeen[ch] + 1);
                emit.at("seenBefore").say(left == before
                                ? String.format("'%c' was last at index %d, before the window starts (%d) - no repeat inside. left stays %d.",
                                        ch, lastSeen[ch], before, left)
                                : String.format("'%c' already appears at index %d inside the window. Jump left past it: left = %d.",
                                        ch, lastSeen[ch], left))
                        .var("left", left).var("right", right).var("maxLength", maxLength)
                        .arrayState(WindowCells.of(s, left, right)).step();
            }
            int currLength = right - left + 1;
            maxLength = Math.max(maxLength, currLength);
            lastSeen[ch] = right;
            emit.at("extend").say("Window [%d, %d] = \"%s\" has no repeat, length %d. maxLength = %d.",
                            left, right, s.substring(left, right + 1), currLength, maxLength)
                    .var("left", left).var("right", right).var("currLength", currLength).var("maxLength", maxLength)
                    .arrayState(WindowCells.of(s, left, right)).step();
            right++;
        }

        emit.at("done").say("Every index has been the right end once. The longest window without a repeat has "
                        + "length %d.", maxLength)
                .var("maxLength", maxLength).var("answer", maxLength).arrayState(WindowCells.of(s, -1, -1)).step();
    }
}
