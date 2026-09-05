package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * The failure function: at every index, either extend the current match by one (the
 * matched character also matches the one right after the prefix so far), fall back to a
 * shorter previously-recorded match without giving up any ground on the string itself, or
 * concede there is no match at all. Falling back never advances the scan - only a match or
 * a concession does.
 */
@Component
public class KmpLpsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "kmp-lps-algo";
    }

    @Override
    public DsType dsType() {
        return DsType.STRING;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("pattern", FieldType.STRING)
                        .label("Pattern")
                        .help("Lowercase letters only.")
                        .length(1, 20)
                        .constraint("pattern", "[a-z]+")
                        .constraint("patternHint", "Lowercase letters a-z only.")
                        .defaultValue("abababca")
                        .build());
    }

    /** No self-overlap anywhere - every lps value stays 0, the opposite profile of the default. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("pattern", "abcde");
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] computeLPS(String pattern) {
                   int n = pattern.length();
                   int[] lps = new int[n];
                   // @a init
                   int len = 0;
                   int i = 1;
                   while (i < n) {
                       if (pattern.charAt(i) == pattern.charAt(len)) {
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
                   return lps;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String pattern = in.getString("pattern");
        int n = pattern.length();
        int[] lps = new int[n];
        int len = 0;
        int i = 1;

        emit.at("init")
                .say("lps[0] = 0 always - a single character has no shorter prefix to match against itself. "
                        + "Start scanning from index 1.")
                .var("len", len).var("i", i).var("lps", Arrays.toString(lps))
                .chars(pattern, 0, -1).step();

        while (i < n) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                emit.at("match")
                        .say("pattern[%d]='%c' matches pattern[%d]='%c' - extend the match to length %d and record lps[%d]=%d.",
                                i, pattern.charAt(i), len - 1, pattern.charAt(len - 1), len, i, len)
                        .var("len", len).var("i", i).var("lps", Arrays.toString(lps))
                        .chars(pattern, i, len - 1).step();
                i++;
            } else if (len != 0) {
                int before = len;
                len = lps[len - 1];
                emit.at("fallback")
                        .say("pattern[%d]='%c' breaks the match of length %d - fall back to the next-best "
                                + "recorded match length %d (lps[%d]) without moving i forward.",
                                i, pattern.charAt(i), before, len, before - 1)
                        .var("len", len).var("i", i).var("lps", Arrays.toString(lps))
                        .chars(pattern, i, len).step();
            } else {
                lps[i] = 0;
                emit.at("noMatch")
                        .say("pattern[%d]='%c' does not match pattern[0]='%c', and there is no shorter match "
                                + "left to fall back to - lps[%d] = 0.",
                                i, pattern.charAt(i), pattern.charAt(0), i)
                        .var("len", len).var("i", i).var("lps", Arrays.toString(lps))
                        .chars(pattern, i, 0).step();
                i++;
            }
        }

        emit.at("done")
                .say("Every index considered. LPS array complete: %s.", Arrays.toString(lps))
                .var("lps", Arrays.toString(lps))
                .chars(pattern).step();
    }
}
