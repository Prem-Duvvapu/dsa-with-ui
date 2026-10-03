package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Minimum Window Substring (LeetCode 76), traced on the owner's own accepted submission. freq starts
 * at minus the count of each character of t; adding s[right] raises it, and a character that is still
 * at or below 0 afterwards was one t needed, so cnt rises. When cnt reaches t's length the window
 * covers t: record it and shrink from the left until it no longer does. O(n + m).
 *
 * <p>{@code freq[256]} is indexed by character, so both strings are limited to printable ASCII
 * (LeetCode's constraint is letters).
 */
@Component
public class MinimumWindowSubstringTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "minimum-window-substring";
    }

    @Override
    public DsType dsType() {
        return DsType.WINDOW;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("s", FieldType.STRING)
                        .label("String s")
                        .length(1, 40)
                        .constraint("pattern", "[ -~]{1,40}")
                        .constraint("patternHint", "Printable ASCII characters only.")
                        .defaultValue("ADOBECODEBANC")
                        .build(),
                InputField.of("t", FieldType.STRING)
                        .label("Pattern t")
                        .help("Every character (with multiplicity) must appear in the window.")
                        .length(1, 15)
                        .constraint("pattern", "[ -~]{1,15}")
                        .constraint("patternHint", "Printable ASCII characters only.")
                        .defaultValue("ABC")
                        .build());
    }

    /**
     * LeetCode's third example: t needs two a's and s has one, so no window ever covers t - the
     * empty-answer branch the default never reaches.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "a", "t", "aa");
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public String minWindow(String s, String t) {
                       // @a init
                       int n=s.length();
                       int m=t.length();
                       int[] freq=new int[256];
                       int left=0;
                       int right=0;
                       int cnt=0;
                       int minLen=(int)1e6;
                       int currLen=0;
                       int startIndex=-1;

                       for (char ch: t.toCharArray())
                           freq[ch]--;

                       while (right<n) {
                           // @a add
                           char ch=s.charAt(right);
                           freq[ch]++;

                           if (freq[ch]<=0)
                               cnt++;

                           while (cnt==m) {
                               // @a covers
                               currLen=right-left+1;
                               if (currLen<minLen) {
                                   minLen=currLen;
                                   startIndex=left;
                               }

                               char leftChar=s.charAt(left);
                               freq[leftChar]--;

                               if (freq[leftChar]<0)
                                   cnt--;

                               left++;
                           }

                           right++;
                       }

                       if (startIndex==-1)
                           // @a none
                           return "";

                       // @a done
                       return s.substring(startIndex,startIndex+minLen);
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        String t = in.getString("t");
        int n = s.length();
        int m = t.length();
        int[] freq = new int[256];
        int left = 0;
        int right = 0;
        int cnt = 0;
        int minLen = (int) 1e6;
        int startIndex = -1;
        for (char ch : t.toCharArray()) freq[ch]--;

        emit.at("init").say("freq starts at minus each character's count in t = \"%s\". cnt counts the characters of t "
                        + "the window has; it covers t when cnt = %d.", t, m)
                .var("cnt", 0).var("m", m).arrayState(WindowCells.of(s, -1, -1)).step();

        while (right < n) {
            char ch = s.charAt(right);
            freq[ch]++;
            boolean needed = freq[ch] <= 0;
            if (needed) cnt++;
            emit.at("add").say(needed
                            ? String.format("Take in '%c' at %d: t needed it. cnt = %d of %d.", ch, right, cnt, m)
                            : String.format("Take in '%c' at %d: t does not need another one. cnt stays %d.", ch, right, cnt))
                    .var("left", left).var("right", right).var("cnt", cnt).var("best", best(s, startIndex, minLen))
                    .arrayState(WindowCells.of(s, left, right)).step();
            while (cnt == m) {
                int currLen = right - left + 1;
                if (currLen < minLen) {
                    minLen = currLen;
                    startIndex = left;
                }
                char leftChar = s.charAt(left);
                freq[leftChar]--;
                boolean lost = freq[leftChar] < 0;
                if (lost) cnt--;
                emit.at("covers").say("Window [%d, %d] = \"%s\" covers t (length %d; best so far \"%s\"). Drop '%c' from "
                                + "the left%s.", left, right, s.substring(left, right + 1), currLen, best(s, startIndex, minLen),
                                leftChar, lost ? ", which t needed - cnt = " + cnt : "; t did not need it")
                        .var("left", left + 1).var("right", right).var("cnt", cnt).var("best", best(s, startIndex, minLen))
                        .arrayState(WindowCells.of(s, left, right)).step();
                left++;
            }
            right++;
        }

        if (startIndex == -1) {
            emit.at("none").say("No window ever covered t. Return \"\".")
                    .var("answer", "").arrayState(WindowCells.of(s, -1, -1)).step();
            return;
        }
        String answer = s.substring(startIndex, startIndex + minLen);
        emit.at("done").say("The smallest window that covers t is \"%s\".", answer)
                .var("answer", answer).arrayState(WindowCells.of(s, startIndex, startIndex + minLen - 1)).step();
    }

    private static String best(String s, int start, int len) {
        return start == -1 ? "none" : s.substring(start, start + len);
    }
}
