package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * Longest Happy Prefix (LeetCode 1392), in the owner's style: the longest proper prefix of s that is
 * also a suffix is exactly lps[n - 1] of KMP's prefix function. O(n).
 *
 * <p>The owner's submission compared two rolling hashes; that passes, but it is a probabilistic check.
 * The traced code computes the lps table with the owner's own lps loop from their Rotate String
 * solution and reads the answer off its last entry; a comment in the code says so.
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
               // Changed from your submission: it compared two rolling hashes, which is a probabilistic
               // check. KMP's lps table gives the answer exactly - lps[n-1] is the longest proper prefix
               // that is also a suffix - using the same lps loop as your Rotate String solution.
               class Solution {
                   public String longestPrefix(String s) {
                       // @a init
                       String pattern=s;
                       int m=pattern.length();
                       int[] lps=new int[m];
                       lps[0]=0;
                       int j=1;
                       int currLength=0;

                       while (j<m) {
                           // @a lps
                           if (pattern.charAt(j)==pattern.charAt(currLength)) {
                               currLength++;
                               lps[j]=currLength;
                               j++;
                           } else {
                               if (currLength==0) {
                                   lps[j]=currLength;
                                   j++;
                               } else {
                                   currLength=lps[currLength-1];
                               }
                           }
                       }

                       // @a done
                       int maxLength=lps[m-1];
                       return s.substring(0,maxLength);
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        emit.at("init").say("Build lps for \"%s\": lps[j] is the longest proper prefix of s[0..j] that is also its "
                        + "suffix. The last entry answers the question for the whole string.", s)
                .chars(s).step();
        int[] lps = OwnerLps.build(s, emit, "lps");
        int maxLength = lps[s.length() - 1];
        String answer = s.substring(0, maxLength);
        emit.at("done").say(maxLength == 0
                        ? "lps[last] = 0: no proper prefix is also a suffix. Return \"\"."
                        : "lps[last] = " + maxLength + ": \"" + answer + "\" is both a prefix and a suffix of s.")
                .var("maxLength", maxLength).var("answer", answer).chars(s, 0, maxLength - 1).step();
    }
}
