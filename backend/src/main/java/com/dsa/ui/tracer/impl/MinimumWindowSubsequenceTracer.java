package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Minimum Window Subsequence — two pointers with forward expansion and reverse contraction.
 * 1. Forward pass: advance pointer in S1 to find a subsequence matching S2.
 * 2. Backward pass: once a match ends at S1[end], scan backwards to locate the latest
 *    possible starting index, shrinking the window to minimum length for this end position.
 * 3. Repeat to find the global minimum across all windows.
 */
@Component
public class MinimumWindowSubsequenceTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "minimum-window-subsequence";
    }

    @Override
    public DsType dsType() {
        return DsType.STRING;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("s1", FieldType.STRING)
                        .label("Source String (S1)")
                        .help("String to search within.")
                        .length(1, 40)
                        .defaultValue("abcdebdde")
                        .build(),
                InputField.of("s2", FieldType.STRING)
                        .label("Pattern Subsequence (S2)")
                        .help("Subsequence to find minimum window for.")
                        .length(1, 10)
                        .defaultValue("bde")
                        .build());
    }

    /** Alternate strings with distinct match positions. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s1", "fgrqmstsfvfreokqmstsf", "s2", "kmt");
    }

    @Override
    public String annotatedCode() {
        return """
               public String minWindow(String s1, String s2) {
                   int m = s1.length(), n = s2.length();
                   int s1Index = 0, s2Index = 0;
                   int minLen = Integer.MAX_VALUE, startIdx = -1;

                   while (s1Index < m) {
                       // @a forwardScan
                       if (s1.charAt(s1Index) == s2.charAt(s2Index)) {
                           s2Index++;
                           if (s2Index == n) {
                               // @a matchFound
                               int end = s1Index;
                               s2Index--;
                               while (s2Index >= 0) {
                                   if (s1.charAt(s1Index) == s2.charAt(s2Index)) s2Index--;
                                   s1Index--;
                               }
                               s1Index++;
                               s2Index = 0;
                               // @a shrinkWindow
                               int len = end - s1Index + 1;
                               if (len < minLen) {
                                   minLen = len;
                                   startIdx = s1Index;
                               }
                           }
                       }
                       s1Index++;
                   }
                   // @a done
                   return startIdx == -1 ? "" : s1.substring(startIdx, startIdx + minLen);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s1 = in.getString("s1");
        String s2 = in.getString("s2");
        int m = s1.length(), n = s2.length();
        int s1Index = 0, s2Index = 0;
        int minLen = Integer.MAX_VALUE;
        int startIdx = -1;

        while (s1Index < m) {
            char c1 = s1.charAt(s1Index);
            char c2 = s2.charAt(s2Index);
            boolean match = (c1 == c2);

            emit.at("forwardScan")
                    .say("Forward scan: s1[%d]='%c' %s s2[%d]='%c'. Matched %d/%d chars.",
                            s1Index, c1, match ? "==" : "!=", s2Index, c2, match ? s2Index + 1 : s2Index, n)
                    .var("s1Index", s1Index).var("s2Index", s2Index).var("minLen", minLen == Integer.MAX_VALUE ? "none" : minLen)
                    .arrayState(windowState(s1, -1, s1Index))
                    .step();

            if (match) {
                s2Index++;
                if (s2Index == n) {
                    int end = s1Index;
                    emit.at("matchFound")
                            .say("All %d characters of S2 matched! Window ends at %d. Reverse scan to find minimal start.",
                                    n, end)
                            .var("end", end)
                            .arrayState(windowState(s1, -1, end))
                            .step();

                    s2Index--;
                    while (s2Index >= 0) {
                        if (s1.charAt(s1Index) == s2.charAt(s2Index)) {
                            s2Index--;
                        }
                        s1Index--;
                    }
                    s1Index++;
                    s2Index = 0;

                    int len = end - s1Index + 1;
                    if (len < minLen) {
                        minLen = len;
                        startIdx = s1Index;
                    }

                    emit.at("shrinkWindow")
                            .say("Reverse scan optimized window: [%d,%d]=\"%s\" (length %d). Best so far: %d (\"%s\").",
                                    s1Index, end, s1.substring(s1Index, end + 1), len, minLen, s1.substring(startIdx, startIdx + minLen))
                            .var("start", s1Index).var("end", end).var("len", len).var("minLen", minLen)
                            .arrayState(windowState(s1, s1Index, end))
                            .step();
                }
            }
            s1Index++;
        }

        String best = startIdx == -1 ? "" : s1.substring(startIdx, startIdx + minLen);
        emit.at("done")
                .say("Search complete. Minimum window subsequence: \"%s\" (length %d).",
                        best, startIdx == -1 ? 0 : minLen)
                .var("answer", best)
                .arrayState(windowState(s1, startIdx, startIdx == -1 ? -1 : startIdx + minLen - 1))
                .step();
    }

    private static List<ArrayElement> windowState(String s1, int left, int right) {
        List<ArrayElement> state = new ArrayList<>(s1.length());
        for (int i = 0; i < s1.length(); i++) {
            String st = (left >= 0 && right >= 0 && i >= left && i <= right)
                    ? (i == right ? "current" : i == left ? "target" : "active")
                    : (i == right ? "current" : "default");
            state.add(new ArrayElement(i, s1.charAt(i), st, String.valueOf(s1.charAt(i))));
        }
        return state;
    }
}
