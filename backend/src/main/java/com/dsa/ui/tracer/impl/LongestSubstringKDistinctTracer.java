package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Longest Substring With At Most K Distinct Characters — O(N) sliding window.
 * Expand the right pointer and maintain character frequencies in the window.
 * When the number of distinct characters exceeds K, shrink from the left until
 * distinct character count is at most K again.
 */
@Component
public class LongestSubstringKDistinctTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "longest-substring-k-distinct";
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
                        .help("String of characters.")
                        .length(1, 40)
                        .defaultValue("eceba")
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K (distinct characters)")
                        .help("Maximum distinct characters allowed in the substring.")
                        .range(1, 40)
                        .defaultValue(2)
                        .build());
    }

    /** Alternate input where string has fewer distinct characters than k. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "aa", "k", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public int lengthOfLongestSubstringKDistinct(String s, int k) {
                   Map<Character, Integer> map = new LinkedHashMap<>();
                   int left = 0, maxLen = 0;

                   for (int right = 0; right < s.length(); right++) {
                       // @a expand
                       char c = s.charAt(right);
                       map.merge(c, 1, Integer::sum);

                       while (map.size() > k) {
                           // @a shrink
                           char leftChar = s.charAt(left);
                           int rem = map.merge(leftChar, -1, Integer::sum);
                           if (rem == 0) map.remove(leftChar);
                           left++;
                       }

                       // @a windowComplete
                       maxLen = Math.max(maxLen, right - left + 1);
                   }
                   // @a done
                   return maxLen;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int k = in.getInt("k");
        Map<Character, Integer> map = new LinkedHashMap<>();
        int left = 0, maxLen = 0;

        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            map.merge(c, 1, Integer::sum);
            emit.at("expand")
                    .say("Add '%c' at index %d. Window [%d,%d] holds %d distinct char%s (budget K=%d).",
                            c, right, left, right, map.size(), map.size() == 1 ? "" : "s", k)
                    .var("left", left).var("right", right).var("distinct", map.size()).var("k", k)
                    .arrayState(windowState(s, left, right))
                    .step();

            while (map.size() > k) {
                char leftChar = s.charAt(left);
                int rem = map.merge(leftChar, -1, Integer::sum);
                if (rem == 0) map.remove(leftChar);
                left++;
                emit.at("shrink")
                        .say("Distinct count (%d) > K (%d) → drop '%c' from left. Window now starts at %d.",
                                map.size() + (rem == 0 ? 1 : 0), k, leftChar, left)
                        .var("left", left).var("right", right).var("distinct", map.size())
                        .arrayState(windowState(s, left, right))
                        .step();
            }

            maxLen = Math.max(maxLen, right - left + 1);
            emit.at("windowComplete")
                    .say("Window [%d,%d]=\"%s\" is valid (len %d). Best so far: %d.",
                            left, right, s.substring(left, right + 1), right - left + 1, maxLen)
                    .var("left", left).var("right", right).var("windowLen", right - left + 1).var("maxLen", maxLen)
                    .arrayState(windowState(s, left, right))
                    .step();
        }

        emit.at("done")
                .say("Scan complete. Longest substring with at most %d distinct characters has length %d.", k, maxLen)
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
