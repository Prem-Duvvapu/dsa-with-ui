package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * Build s + '#' + reverse(s) and run the exact same failure-function computation
 * {@code kmp-lps-algo} runs on a plain pattern. The separator stops the match from ever
 * crossing from one half into the other. What comes out the far end - the last cell of
 * the resulting LPS array - names the longest prefix of s that is already a palindrome
 * anchored at its start; everything s is missing to become one gets mirrored onto its front.
 */
@Component
public class ShortestPalindromeTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "shortest-palindrome";
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
                        .help("Lowercase letters only.")
                        .length(1, 12)
                        .constraint("pattern", "[a-z]+")
                        .constraint("patternHint", "Lowercase letters a-z only.")
                        .defaultValue("aacecaaa")
                        .build());
    }

    /** Shares no palindromic prefix with its own reverse beyond a single character. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "abcd");
    }

    @Override
    public String annotatedCode() {
        return """
               public String shortestPalindrome(String s) {
                   // @a build
                   String combined = s + "#" + reverse(s);
                   int n = combined.length();
                   int[] lps = new int[n];
                   // @a init
                   int len = 0;
                   int i = 1;
                   while (i < n) {
                       if (combined.charAt(i) == combined.charAt(len)) {
                           // @a match
                           len++;
                           lps[i] = len;
                           i++;
                       } else if (len != 0) {
                           // @a fallback
                           len = lps[len - 1];
                       } else {
                           // @a noMatch
                           lps[i] = 0;
                           i++;
                       }
                   }
                   int k = lps[n - 1];
                   // @a done
                   return reverse(s.substring(k)) + s;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        String combined = s + "#" + new StringBuilder(s).reverse();
        int n = combined.length();
        int[] lps = new int[n];
        int len = 0;
        int i = 1;

        emit.at("build")
                .say("Join s to its own reverse with a separator so a match can never cross from one "
                        + "half into the other: \"%s\".", combined)
                .var("combined", combined)
                .chars(combined).step();

        emit.at("init")
                .say("lps[0] = 0 always - a single character has no shorter prefix to match against itself.")
                .var("len", len).var("i", i).var("lps", Arrays.toString(lps))
                .chars(combined, 0, -1).step();

        while (i < n) {
            if (combined.charAt(i) == combined.charAt(len)) {
                len++;
                lps[i] = len;
                emit.at("match")
                        .say("combined[%d]='%c' matches combined[%d]='%c' - extend the match to length %d "
                                + "and record lps[%d]=%d.",
                                i, combined.charAt(i), len - 1, combined.charAt(len - 1), len, i, len)
                        .var("len", len).var("i", i).var("lps", Arrays.toString(lps))
                        .chars(combined, i, len - 1).step();
                i++;
            } else if (len != 0) {
                int before = len;
                len = lps[len - 1];
                emit.at("fallback")
                        .say("combined[%d]='%c' breaks the match of length %d - fall back to the next-best "
                                + "recorded match length %d (lps[%d]) without moving i forward.",
                                i, combined.charAt(i), before, len, before - 1)
                        .var("len", len).var("i", i).var("lps", Arrays.toString(lps))
                        .chars(combined, i, len).step();
            } else {
                lps[i] = 0;
                emit.at("noMatch")
                        .say("combined[%d]='%c' does not match combined[0]='%c', and there is no shorter "
                                + "match left to fall back to - lps[%d] = 0.",
                                i, combined.charAt(i), combined.charAt(0), i)
                        .var("len", len).var("i", i).var("lps", Arrays.toString(lps))
                        .chars(combined, i, 0).step();
                i++;
            }
        }

        int k = lps[n - 1];
        String answer = new StringBuilder(s.substring(k)).reverse() + s;
        emit.at("done")
                .say("lps[%d]=%d - the first %d characters of s are already a palindrome anchored at the "
                        + "start. Mirror everything after that onto the front: \"%s\".", n - 1, k, k, answer)
                .var("k", k).var("answer", answer)
                .chars(combined).step();
    }
}
