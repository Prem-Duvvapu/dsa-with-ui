package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * Valid Anagram (LeetCode 242), traced on the owner's own accepted submission: different lengths
 * can never be anagrams; otherwise count s's letters up and t's letters down, and they are anagrams
 * exactly when every count ends at zero. O(n).
 */
@Component
class ValidAnagramTracer extends StringTracerSupport {
    public String id() { return "valid-anagram"; }

    public InputSpec inputSpec() {
        return InputSpec.of(
                patternedText("s", "First string", "anagram", 1, 30, "[a-z]+", "Use lowercase letters a-z."),
                patternedText("t", "Second string", "nagaram", 1, 30, "[a-z]+", "Use lowercase letters a-z."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "rat", "t", "car"); }

    public String annotatedCode() {
        return """
               class Solution {
                   public boolean isAnagram(String s, String t) {
                       // @a lengths
                       if (s.length()!=t.length())
                           return false;

                       int n=s.length();
                       int[] freq=new int[26];

                       for (int i=0;i<n;i++) {
                           // @a countS
                           char ch=s.charAt(i);
                           freq[ch-'a']++;
                       }

                       for (int i=0;i<n;i++) {
                           // @a countT
                           char ch=t.charAt(i);
                           freq[ch-'a']--;
                       }

                       for (int i=0;i<26;i++)
                           if (freq[i]!=0)
                               // @a mismatch
                               return false;

                       // @a done
                       return true;
                   }
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        String t = in.getString("t");
        if (s.length() != t.length()) {
            emit.at("lengths").say("s has %d letters and t has %d. Different lengths can never be anagrams: return false.",
                            s.length(), t.length())
                    .var("answer", false).chars(s).step();
            return;
        }
        emit.at("lengths").say("Both have %d letters, so count them.", s.length()).chars(s).step();
        int n = s.length();
        int[] freq = new int[26];
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            freq[ch - 'a']++;
            emit.at("countS").say("s[%d] = '%c': freq['%c'] = %d.", i, ch, ch, freq[ch - 'a'])
                    .var("i", i).var("freq", counts(freq)).chars(s, i).step();
        }
        for (int i = 0; i < n; i++) {
            char ch = t.charAt(i);
            freq[ch - 'a']--;
            emit.at("countT").say("t[%d] = '%c': freq['%c'] = %d.", i, ch, ch, freq[ch - 'a'])
                    .var("i", i).var("freq", counts(freq)).chars(t, i).step();
        }
        for (int i = 0; i < 26; i++) {
            if (freq[i] != 0) {
                emit.at("mismatch").say("freq['%c'] = %d, not 0: the strings do not use the same letters equally often. "
                                + "Return false.", (char) ('a' + i), freq[i])
                        .var("freq", counts(freq)).var("answer", false).chars(t).step();
                return;
            }
        }
        emit.at("done").say("Every count is back to 0: t uses exactly s's letters. Return true.")
                .var("freq", counts(freq)).var("answer", true).chars(t).step();
    }

    /** The non-zero counts, "{a=2, n=-1}". */
    private static String counts(int[] freq) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < 26; i++) {
            if (freq[i] == 0) continue;
            if (sb.length() > 1) sb.append(", ");
            sb.append((char) ('a' + i)).append('=').append(freq[i]);
        }
        return sb.append('}').toString();
    }
}

@Component
class RemoveOutermostParenthesesTracer extends StringTracerSupport {
    public String id() { return "remove-outermost-parentheses"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("s", "Parentheses", "(()())(())", 2, 30,
                "[()]+", "Use only '(' and ')' and provide a valid parentheses string."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "(()())(())(()(()))"); }

    public String annotatedCode() {
        return """
               public String removeOuterParentheses(String s) {
                   int depth = 0;
                   StringBuilder answer = new StringBuilder();
                   for (char ch : s.toCharArray()) {
                       // @a scan
                       if (ch == '(' && depth++ > 0) answer.append(ch);
                       if (ch == ')' && --depth > 0) answer.append(ch);
                   }
                   // @a done
                   return answer.toString();
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        StringBuilder answer = new StringBuilder();
        int depth = 0;
        boolean valid = true;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            boolean append;
            if (ch == '(') {
                append = depth > 0;
                depth++;
            } else {
                depth--;
                append = depth > 0;
                if (depth < 0) valid = false;
            }
            if (append) answer.append(ch);
            emit.at("scan").say("Index %d '%c' changes depth to %d and is %s the result.",
                            i, ch, depth, append ? "added to" : "an outer bracket omitted from")
                    .var("index", i).var("depth", depth).var("result", answer)
                    .chars(s, i).step();
        }
        valid &= depth == 0;
        emit.at("done").say(valid
                        ? "All primitives processed; removing their outer pairs gives \"%s\"."
                        : "The input is not balanced, so no valid primitive decomposition exists.", answer)
                .var("valid", valid).var("answer", valid ? answer : "invalid")
                .chars(s).step();
    }
}

@Component
class ReverseWordsStringTracer extends StringTracerSupport {
    public String id() { return "reverse-words-string"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("s", "Sentence", "  the sky is blue  ", 1, 60,
                "[A-Za-z ]+", "Use letters and spaces only."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "hello   world"); }

    public String annotatedCode() {
        return """
               public String reverseWords(String s) {
                   String[] words = s.trim().split("\\s+");
                   StringBuilder answer = new StringBuilder();
                   for (int i = words.length - 1; i >= 0; i--) {
                       // @a append
                       if (!answer.isEmpty()) answer.append(' ');
                       answer.append(words[i]);
                   }
                   // @a done
                   return answer.toString();
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        String trimmed = s.trim();
        String[] words = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
        StringBuilder answer = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            if (!answer.isEmpty()) answer.append(' ');
            answer.append(words[i]);
            emit.at("append").say("Take word %d, \"%s\", from the right; result is now \"%s\".",
                            i, words[i], answer)
                    .var("wordIndex", i).var("word", words[i]).var("result", answer)
                    .chars(s).step();
        }
        emit.at("done").say("Words reversed and whitespace normalized: \"%s\".", answer)
                .var("answer", answer).chars(s).step();
    }
}

/**
 * Largest Odd Number in String (LeetCode 1903), traced on the owner's own accepted submission: a
 * number is odd exactly when its last digit is, and a longer prefix is a larger number, so scan
 * from the right and return the prefix ending at the first odd digit found. O(n).
 */
@Component
class LargestOddNumberStringTracer extends StringTracerSupport {
    public String id() { return "largest-odd-number-string"; }

    public InputSpec inputSpec() {
        // Trailing zeros, deliberately. "35427" ends in an odd digit, so the right-to-left
        // scan succeeded on its very first probe: two steps, and the walk leftwards past
        // even digits - the only thing this algorithm does - never happened.
        return InputSpec.of(patternedText("number", "Decimal number", "35427000", 1, 40,
                "[0-9]+", "Use decimal digits only."));
    }

    public Map<String, Object> alternateInput() { return Map.of("number", "4206"); }

    public String annotatedCode() {
        return """
               class Solution {
                   public String largestOddNumber(String num) {
                       // @a init
                       int n=num.length();

                       for (int i=n-1;i>=0;i--)
                       {
                           // @a check
                           char curr=num.charAt(i);
                           if ((curr-'0')%2==1)
                               // @a found
                               return num.substring(0,i+1);
                       }

                       // @a none
                       return "";
                   }
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String num = in.getString("number");
        int n = num.length();
        emit.at("init").say("A number is odd when its last digit is odd, and the longest prefix is the largest number. "
                        + "Scan from the right.")
                .chars(num).step();
        for (int i = n - 1; i >= 0; i--) {
            char curr = num.charAt(i);
            if ((curr - '0') % 2 == 1) {
                String answer = num.substring(0, i + 1);
                emit.at("found").say("'%c' at %d is odd. The longest prefix ending here is \"%s\".", curr, i, answer)
                        .var("i", i).var("answer", answer).chars(num, i).step();
                return;
            }
            emit.at("check").say("'%c' at %d is even - no odd number can end here.", curr, i)
                    .var("i", i).chars(num, i).step();
        }
        emit.at("none").say("Every digit is even, so no prefix is odd. Return \"\".")
                .var("answer", "").chars(num).step();
    }
}

/**
 * Longest Common Prefix (LeetCode 14), traced on the owner's own accepted submission: start with the
 * first word as the prefix and cut it back against each later word - at the first mismatch, or to
 * the word's length if the word is shorter. O(total characters).
 */
@Component
class LongestCommonPrefixTracer extends StringTracerSupport {
    public String id() { return "longest-common-prefix"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("words", "Comma-separated words", "flower,flow,flight", 3, 80,
                "[a-z]+(,[a-z]+)+", "Enter at least two lowercase words separated by commas."));
    }

    public Map<String, Object> alternateInput() { return Map.of("words", "dog,racecar,car"); }

    public String annotatedCode() {
        return """
               class Solution {
                   public String longestCommonPrefix(String[] strs) {
                       // @a init
                       int n=strs.length;
                       String res=strs[0];

                       for (int i=1;i<n;i++) {
                           // @a word
                           int j=0; //res pointer
                           int k=0; //strs[i] pointer

                           while (j<res.length() && k<strs[i].length()) {
                               if (res.charAt(j)!=strs[i].charAt(k)) {
                                   if (j==0)
                                       // @a empty
                                       return "";

                                   // @a cut
                                   res=res.substring(0,j);
                                   break;
                               }

                               j++;
                               k++;
                           }

                           if (k<res.length())
                               // @a shorter
                               res=res.substring(0,k);

                       }

                       // @a done
                       return res;
                   }
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String[] strs = in.getString("words").split(",");
        int n = strs.length;
        String res = strs[0];
        emit.at("init").say("Start with the whole first word as the prefix: res = \"%s\".", res)
                .var("res", res).chars(res).step();
        for (int i = 1; i < n; i++) {
            emit.at("word").say("Compare res = \"%s\" with \"%s\" letter by letter.", res, strs[i])
                    .var("i", i).var("res", res).chars(strs[i]).step();
            int j = 0;
            int k = 0;
            boolean cut = false;
            while (j < res.length() && k < strs[i].length()) {
                if (res.charAt(j) != strs[i].charAt(k)) {
                    if (j == 0) {
                        emit.at("empty").say("The very first letters differ ('%c' vs '%c'): nothing is common. Return \"\".",
                                        res.charAt(0), strs[i].charAt(0))
                                .var("res", "").var("answer", "").chars(strs[i], 0).step();
                        return;
                    }
                    char had = res.charAt(j);
                    res = res.substring(0, j);
                    cut = true;
                    emit.at("cut").say("Letter %d differs ('%c' vs '%c'): cut res to \"%s\".",
                                    j, had, strs[i].charAt(k), res)
                            .var("res", res).chars(strs[i], j).step();
                    break;
                }
                j++;
                k++;
            }
            if (!cut && k < res.length()) {
                res = res.substring(0, k);
                emit.at("shorter").say("\"%s\" ran out after %d letters, all matching: cut res to \"%s\".",
                                strs[i], k, res)
                        .var("res", res).chars(strs[i]).step();
            }
        }
        emit.at("done").say("Every word starts with \"%s\". Return it.", res)
                .var("res", res).var("answer", res).chars(res).step();
    }
}
