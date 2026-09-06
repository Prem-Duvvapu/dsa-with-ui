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
 * Longest Substring Without Repeating Characters — O(N) sliding window.
 * Two pointers left and right define the current window of unique characters.
 * An index map stores the most recent position of each character. When a duplicate
 * is found within the current window, left jumps past the duplicate in O(1).
 */
@Component
public class LongestSubstringWithoutRepeatingTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "longest-substring-without-repeating";
    }

    @Override
    public DsType dsType() {
        return DsType.STRING;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("s", FieldType.STRING)
                        .label("String")
                        .help("String of characters to find longest substring without repeating characters.")
                        .length(1, 40)
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
               public int lengthOfLongestSubstring(String s) {
                   int[] lastSeen = new int[256];
                   Arrays.fill(lastSeen, -1);
                   int left = 0, maxLen = 0;

                   for (int right = 0; right < s.length(); right++) {
                       char c = s.charAt(right);
                       if (lastSeen[c] >= left) {
                           // @a shrink
                           left = lastSeen[c] + 1;
                       }
                       // @a expand
                       lastSeen[c] = right;
                       maxLen = Math.max(maxLen, right - left + 1);
                   }
                   // @a done
                   return maxLen;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int[] lastSeen = new int[256];
        Arrays.fill(lastSeen, -1);
        int left = 0, maxLen = 0;

        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (lastSeen[c] >= left) {
                int oldPos = lastSeen[c];
                left = oldPos + 1;
                emit.at("shrink")
                        .say("Duplicate '%c' previously seen at index %d. Slide window left to %d.", c, oldPos, left)
                        .var("left", left).var("right", right).var("char", String.valueOf(c))
                        .arrayState(windowState(s, left, right))
                        .step();
            }

            lastSeen[c] = right;
            maxLen = Math.max(maxLen, right - left + 1);
            emit.at("expand")
                    .say("Add '%c' at index %d. Window [%d,%d]=\"%s\" (len %d). Best so far: %d.",
                            c, right, left, right, s.substring(left, right + 1), right - left + 1, maxLen)
                    .var("left", left).var("right", right).var("windowLen", right - left + 1).var("maxLen", maxLen)
                    .arrayState(windowState(s, left, right))
                    .step();
        }

        emit.at("done")
                .say("Scan complete. Longest substring without repeating characters has length %d.", maxLen)
                .var("answer", maxLen)
                .arrayState(windowState(s, -1, -1))
                .step();
    }

    private static List<ArrayElement> windowState(String s, int left, int right) {
        List<ArrayElement> state = new ArrayList<>(s.length());
        for (int i = 0; i < s.length(); i++) {
            String st = (i == right) ? "current"
                    : (i == left) ? "target"
                    : (i > left && i < right) ? "active"
                    : "default";
            state.add(new ArrayElement(i, s.charAt(i), st, String.valueOf(s.charAt(i))));
        }
        return state;
    }
}
