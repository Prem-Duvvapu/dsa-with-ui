package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * Shortest Palindrome (LeetCode 214), in the owner's style. Adding characters only at the front means
 * keeping the longest palindromic PREFIX of s and putting the reverse of the rest in front. That
 * prefix is the longest prefix of s that is also a suffix of reverse(s) - which is lps[last] of
 * s + "#" + reverse(s). O(n).
 *
 * <p>The owner's submission found the prefix by comparing rolling hashes; that passes, but two
 * different strings can share a hash, so it is not exact. The traced code uses KMP's lps table with
 * the owner's own lps loop from their Rotate String solution, and their {@code longestPalindromeLen}
 * and result-building lines; a comment in the code says so.
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
               // Changed from your submission: it compared rolling hashes, which can collide and give a
               // wrong answer on unlucky inputs. KMP's lps table on s + "#" + reverse(s) is exact and
               // still O(n) - written with the same lps loop as your Rotate String solution.
               class Solution {
                   public String shortestPalindrome(String s) {
                       // @a init
                       String pattern=s+"#"+new StringBuilder(s).reverse().toString();
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
                       int longestPalindromeLen=lps[m-1];
                       StringBuilder res=new StringBuilder(s.substring(longestPalindromeLen));
                       res.reverse();
                       res.append(s);

                       return res.toString();
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        String pattern = s + "#" + new StringBuilder(s).reverse();
        emit.at("init").say("pattern = s + \"#\" + (s reversed) = \"%s\". The lps value at its last letter is the "
                        + "longest prefix of s that reads the same backwards - the '#' stops a match from running "
                        + "past s.", pattern)
                .chars(pattern).step();
        int[] lps = OwnerLps.build(pattern, emit, "lps");
        int longestPalindromeLen = lps[pattern.length() - 1];
        String res = new StringBuilder(s.substring(longestPalindromeLen)).reverse() + s;
        emit.at("done").say("lps[last] = %d, so s[0..%d] = \"%s\" is already a palindrome. Put the reverse of the "
                        + "rest, \"%s\", in front: \"%s\".", longestPalindromeLen, longestPalindromeLen - 1,
                        s.substring(0, longestPalindromeLen), new StringBuilder(s.substring(longestPalindromeLen)).reverse(), res)
                .var("longestPalindromeLen", longestPalindromeLen).var("answer", res).chars(res).step();
    }
}
