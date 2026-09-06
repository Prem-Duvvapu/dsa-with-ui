package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Compares {@code s[i]} against its mirror {@code s[n-i-1]} and recurses inward; the first
 * mismatch answers false immediately, and reaching the midpoint answers true.
 */
@Component
public class PalindromeStringRecursionTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "palindrome-string-recursion";
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
                        .help("Letters only.")
                        .length(1, 20)
                        .constraint("pattern", "[A-Za-z]+")
                        .constraint("patternHint", "Letters only.")
                        .defaultValue("MADAM")
                        .build());
    }

    /** Not a palindrome — the very first comparison mismatches. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "HELLO");
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isPalindrome(int i, String s) {
                   if (i >= s.length() / 2) {
                       // @a base
                       return true;
                   }
                   if (s.charAt(i) != s.charAt(s.length() - i - 1)) {
                       // @a mismatch
                       return false;
                   }
                   // @a recurse
                   return isPalindrome(i + 1, s);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        recurse(0, s, emit);
    }

    private boolean recurse(int i, String s, StepEmitter emit) {
        emit.push("isPalindrome(i=" + i + ")");
        int half = s.length() / 2;
        boolean result;

        if (i >= half) {
            emit.at("base")
                    .say("i=%d reached the halfway point (%d) — every pair matched, so \"%s\" is a palindrome.",
                            i, half, s)
                    .var("i", i).var("half", half)
                    .chars(s).step();
            result = true;
        } else {
            int mirror = s.length() - i - 1;
            char left = s.charAt(i);
            char right = s.charAt(mirror);

            if (left != right) {
                emit.at("mismatch")
                        .say("s[%d]='%c' does not match s[%d]='%c' — \"%s\" is not a palindrome.",
                                i, left, mirror, right, s)
                        .var("i", i).var("left", String.valueOf(left)).var("right", String.valueOf(right))
                        .chars(s, i, mirror).step();
                result = false;
            } else {
                emit.at("recurse")
                        .say("s[%d]='%c' matches s[%d]='%c' — recurse into i=%d.", i, left, mirror, right, i + 1)
                        .var("i", i).var("left", String.valueOf(left)).var("right", String.valueOf(right))
                        .chars(s, i, mirror).step();
                result = recurse(i + 1, s, emit);
            }
        }

        emit.pop();
        return result;
    }
}
