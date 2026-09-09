package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
class StringToIntegerAtoiTracer extends StringTracerSupport {
    public String id() { return "string-to-integer-atoi"; }

    public InputSpec inputSpec() {
        return InputSpec.of(text("s", "Input text", "   -42", 1, 40));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "4193 with words"); }

    public String annotatedCode() {
        return """
               public int myAtoi(String s) {
                   int i = 0, sign = 1;
                   long value = 0;
                   // @a scan
                   while (i < s.length() && s.charAt(i) == ' ') i++;
                   if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-'))
                       sign = s.charAt(i++) == '-' ? -1 : 1;
                   while (i < s.length() && Character.isDigit(s.charAt(i))) {
                       value = Math.min((long) Integer.MAX_VALUE + 1, value * 10 + s.charAt(i++) - '0');
                   }
                   // @a done
                   return clamp(sign * value);
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int index = 0;
        while (index < s.length() && s.charAt(index) == ' ') {
            emit.at("scan").say("Index %d is leading whitespace; skip it.", index)
                    .var("index", index).var("phase", "whitespace").chars(s, index).step();
            index++;
        }
        int sign = 1;
        if (index < s.length() && (s.charAt(index) == '+' || s.charAt(index) == '-')) {
            sign = s.charAt(index) == '-' ? -1 : 1;
            emit.at("scan").say("Index %d supplies sign '%c'; sign=%d.", index, s.charAt(index), sign)
                    .var("index", index).var("phase", "sign").var("sign", sign).chars(s, index).step();
            index++;
        }
        long magnitude = 0;
        while (index < s.length() && Character.isDigit(s.charAt(index))) {
            int digit = s.charAt(index) - '0';
            magnitude = Math.min((long) Integer.MAX_VALUE + 1, magnitude * 10 + digit);
            emit.at("scan").say("Consume digit %d at index %d; magnitude=%d.", digit, index, magnitude)
                    .var("index", index).var("phase", "digits").var("magnitude", magnitude)
                    .chars(s, index).step();
            index++;
        }
        long signed = sign * magnitude;
        int answer = signed > Integer.MAX_VALUE ? Integer.MAX_VALUE
                : signed < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) signed;
        emit.at("done").say("Parsing stops at index %d; clamped 32-bit result is %d.", index, answer)
                .var("stopIndex", index).var("answer", answer).chars(s).step();
    }
}

@Component
class CountSubstringsKDistinctTracer extends StringTracerSupport {
    public String id() { return "count-substrings-k-distinct"; }

    public InputSpec inputSpec() {
        return InputSpec.of(
                patternedText("s", "String", "pqpqs", 1, 24, "[a-z]+", "Use lowercase letters a-z."),
                InputField.of("k", FieldType.INT).label("Distinct characters")
                        .range(1, 10).defaultValue(2).build());
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "abcbaa", "k", 3); }

    public String annotatedCode() {
        return """
               public long substringsWithKDistinct(String s, int k) {
                   // @a window
                   long atMostK = atMost(s, k);
                   long atMostKMinusOne = atMost(s, k - 1);
                   // @a done
                   return atMostK - atMostKMinusOne;
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int k = in.getInt("k");
        long atMostK = atMost(s, k, "k", emit);
        long atMostPrevious = atMost(s, k - 1, "k-1", emit);
        long answer = atMostK - atMostPrevious;
        emit.at("done").say("Exactly %d distinct = atMost(%d) %d − atMost(%d) %d = %d substrings.",
                        k, k, atMostK, k - 1, atMostPrevious, answer)
                .var("atMostK", atMostK).var("atMostKMinusOne", atMostPrevious).var("answer", answer)
                .chars(s).step();
    }

    private static long atMost(String s, int limit, String pass, StepEmitter emit) {
        if (limit < 0) return 0;
        Map<Character, Integer> frequency = new HashMap<>();
        int left = 0;
        long total = 0;
        for (int right = 0; right < s.length(); right++) {
            frequency.merge(s.charAt(right), 1, Integer::sum);
            while (frequency.size() > limit) {
                char removed = s.charAt(left++);
                frequency.compute(removed, (key, count) -> count == 1 ? null : count - 1);
            }
            long added = right - left + 1L;
            total += added;
            emit.at("window").say("%s pass: window [%d,%d] has %d distinct; add %d suffixes (total %d).",
                            pass, left, right, frequency.size(), added, total)
                    .var("pass", pass).var("left", left).var("right", right)
                    .var("distinct", frequency.size()).var("total", total)
                    .chars(s, left, right).step();
        }
        return total;
    }
}

@Component
class LongestPalindromicSubstringTracer extends StringTracerSupport {
    public String id() { return "longest-palindromic-substring"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("s", "String", "babad", 1, 30,
                "[A-Za-z0-9]+", "Use letters and digits only."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "cbbd"); }

    public String annotatedCode() {
        return """
               public String longestPalindrome(String s) {
                   int bestLeft = 0, bestRight = 0;
                   for (int center = 0; center < s.length(); center++) {
                       // @a expand
                       int[] odd = expand(s, center, center);
                       int[] even = expand(s, center, center + 1);
                       if (longer(odd, bestLeft, bestRight)) { bestLeft = odd[0]; bestRight = odd[1]; }
                       if (longer(even, bestLeft, bestRight)) { bestLeft = even[0]; bestRight = even[1]; }
                   }
                   // @a done
                   return s.substring(bestLeft, bestRight + 1);
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int bestLeft = 0;
        int bestRight = 0;
        for (int center = 0; center < s.length(); center++) {
            int[][] starts = {{center, center}, {center, center + 1}};
            for (int[] start : starts) {
                int left = start[0];
                int right = start[1];
                while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
                    if (right - left > bestRight - bestLeft) {
                        bestLeft = left;
                        bestRight = right;
                    }
                    emit.at("expand").say("Center %s expands to [%d,%d] \"%s\"; best is \"%s\".",
                                    start[0] == start[1] ? String.valueOf(center) : center + "/" + (center + 1),
                                    left, right, s.substring(left, right + 1),
                                    s.substring(bestLeft, bestRight + 1))
                            .var("left", left).var("right", right)
                            .var("best", s.substring(bestLeft, bestRight + 1))
                            .chars(s, left, right).step();
                    left--;
                    right++;
                }
            }
        }
        String answer = s.substring(bestLeft, bestRight + 1);
        emit.at("done").say("Longest palindromic substring is \"%s\" at [%d,%d].",
                        answer, bestLeft, bestRight)
                .var("left", bestLeft).var("right", bestRight).var("answer", answer)
                .chars(s, bestLeft, bestRight).step();
    }
}

@Component
class SumBeautyAllSubstringsTracer extends StringTracerSupport {
    public String id() { return "sum-beauty-all-substrings"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("s", "String", "aabcb", 1, 12,
                "[a-z]+", "Use lowercase letters a-z."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "aabcbaa"); }

    public String annotatedCode() {
        return """
               public int beautySum(String s) {
                   int total = 0;
                   for (int left = 0; left < s.length(); left++) {
                       int[] frequency = new int[26];
                       for (int right = left; right < s.length(); right++) {
                           frequency[s.charAt(right) - 'a']++;
                           // @a extend
                           total += maximum(frequency) - minimumPositive(frequency);
                       }
                   }
                   // @a done
                   return total;
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int total = 0;
        for (int left = 0; left < s.length(); left++) {
            int[] frequency = new int[26];
            for (int right = left; right < s.length(); right++) {
                frequency[s.charAt(right) - 'a']++;
                int maximum = 0;
                int minimum = Integer.MAX_VALUE;
                for (int count : frequency) {
                    if (count > 0) {
                        maximum = Math.max(maximum, count);
                        minimum = Math.min(minimum, count);
                    }
                }
                int beauty = maximum - minimum;
                total += beauty;
                emit.at("extend").say("Substring [%d,%d] \"%s\": max frequency %d − min positive %d = %d; total=%d.",
                                left, right, s.substring(left, right + 1), maximum, minimum, beauty, total)
                        .var("left", left).var("right", right).var("beauty", beauty).var("total", total)
                        .chars(s, left, right).step();
            }
        }
        emit.at("done").say("Sum of beauty over every substring of \"%s\" is %d.", s, total)
                .var("answer", total).chars(s).step();
    }
}

@Component
class ReverseEveryWordTracer extends StringTracerSupport {
    public String id() { return "reverse-every-word"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("s", "Sentence", "Let's take LeetCode contest", 1, 60,
                "[A-Za-z' ]+", "Use letters, apostrophes, and spaces only."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "God Ding"); }

    public String annotatedCode() {
        return """
               public String reverseEveryWord(String s) {
                   char[] chars = s.toCharArray();
                   int start = 0;
                   while (start < chars.length) {
                       int end = start;
                       while (end < chars.length && chars[end] != ' ') end++;
                       // @a reverse
                       reverse(chars, start, end - 1);
                       start = end + 1;
                   }
                   // @a done
                   return new String(chars);
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        char[] chars = s.toCharArray();
        int start = 0;
        while (start < chars.length) {
            if (chars[start] == ' ') {
                start++;
                continue;
            }
            int end = start;
            while (end < chars.length && chars[end] != ' ') end++;
            int left = start;
            int right = end - 1;
            while (left < right) {
                char temporary = chars[left];
                chars[left++] = chars[right];
                chars[right--] = temporary;
            }
            String current = new String(chars);
            emit.at("reverse").say("Reverse word [%d,%d]; sentence becomes \"%s\".", start, end - 1, current)
                    .var("wordStart", start).var("wordEnd", end - 1).var("current", current)
                    .chars(current, start, end - 1).step();
            start = end + 1;
        }
        String answer = new String(chars);
        emit.at("done").say("Every word reversed in place while spaces stay fixed: \"%s\".", answer)
                .var("answer", answer).chars(answer).step();
    }
}
