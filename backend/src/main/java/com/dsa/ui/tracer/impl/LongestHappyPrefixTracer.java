package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * The "happy prefix" is not a separate algorithm - it is the KMP failure function (the
 * same table {@code kmp-lps-algo} builds) applied to the string itself, read off its very
 * last cell. lps[n-1] names the length of the longest prefix that reappears, intact, as a
 * suffix somewhere later in the same string.
 */
@Component
public class LongestHappyPrefixTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "longest-happy-prefix";
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
                        .length(1, 20)
                        .constraint("pattern", "[a-z]+")
                        .constraint("patternHint", "Lowercase letters a-z only.")
                        .defaultValue("ababcabab")
                        .build());
    }

    /** A different repeating shape - still exercises every branch, a different happy prefix. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "aabaaab");
    }

    @Override
    public String annotatedCode() {
        return """
               public String longestPrefix(String s) {
                   int n = s.length();
                   int[] lps = new int[n];
                   // @a init
                   int len = 0;
                   int i = 1;
                   while (i < n) {
                       if (s.charAt(i) == s.charAt(len)) {
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
                   // @a done
                   return s.substring(0, lps[n - 1]);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int n = s.length();
        int[] lps = new int[n];
        int len = 0;
        int i = 1;

        emit.at("init")
                .say("lps[0] = 0 always - a single character has no shorter prefix to match against itself.")
                .var("len", len).var("i", i).var("lps", Arrays.toString(lps))
                .chars(s, 0, -1).step();

        while (i < n) {
            if (s.charAt(i) == s.charAt(len)) {
                len++;
                lps[i] = len;
                emit.at("match")
                        .say("s[%d]='%c' matches s[%d]='%c' - extend the match to length %d and record lps[%d]=%d.",
                                i, s.charAt(i), len - 1, s.charAt(len - 1), len, i, len)
                        .var("len", len).var("i", i).var("lps", Arrays.toString(lps))
                        .chars(s, i, len - 1).step();
                i++;
            } else if (len != 0) {
                int before = len;
                len = lps[len - 1];
                emit.at("fallback")
                        .say("s[%d]='%c' breaks the match of length %d - fall back to the next-best "
                                + "recorded match length %d (lps[%d]) without moving i forward.",
                                i, s.charAt(i), before, len, before - 1)
                        .var("len", len).var("i", i).var("lps", Arrays.toString(lps))
                        .chars(s, i, len).step();
            } else {
                lps[i] = 0;
                emit.at("noMatch")
                        .say("s[%d]='%c' does not match s[0]='%c', and there is no shorter match left to "
                                + "fall back to - lps[%d] = 0.",
                                i, s.charAt(i), s.charAt(0), i)
                        .var("len", len).var("i", i).var("lps", Arrays.toString(lps))
                        .chars(s, i, 0).step();
                i++;
            }
        }

        String prefix = s.substring(0, lps[n - 1]);
        emit.at("done")
                .say("The LPS array's last cell, lps[%d]=%d, names the longest prefix of s that reappears "
                        + "as a proper suffix: \"%s\".", n - 1, lps[n - 1], prefix)
                .var("lps", Arrays.toString(lps)).var("answer", prefix)
                .chars(s).step();
    }
}
