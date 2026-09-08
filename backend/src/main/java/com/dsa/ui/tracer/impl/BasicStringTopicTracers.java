package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

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
               public boolean isAnagram(String s, String t) {
                   int[] frequency = new int[26];
                   for (int i = 0; i < s.length(); i++) {
                       // @a scan
                       frequency[s.charAt(i) - 'a']++;
                       frequency[t.charAt(i) - 'a']--;
                   }
                   // @a done
                   return s.length() == t.length() && allZero(frequency);
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        String t = in.getString("t");
        int[] counts = new int[26];
        int limit = Math.min(s.length(), t.length());
        for (int i = 0; i < limit; i++) {
            counts[s.charAt(i) - 'a']++;
            counts[t.charAt(i) - 'a']--;
            emit.at("scan").say("Index %d adds '%c' from s and removes '%c' from t.",
                            i, s.charAt(i), t.charAt(i))
                    .var("index", i).var("frequencyDelta", Arrays.toString(counts))
                    .chars(s, i).step();
        }
        boolean answer = s.length() == t.length() && Arrays.stream(counts).allMatch(value -> value == 0);
        emit.at("done").say(answer
                        ? "Both strings have equal length and every frequency delta is zero: they are anagrams."
                        : "A length or frequency mismatch remains: the strings are not anagrams.")
                .var("sLength", s.length()).var("tLength", t.length()).var("answer", answer)
                .chars(s).step();
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

@Component
class LargestOddNumberStringTracer extends StringTracerSupport {
    public String id() { return "largest-odd-number-string"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("number", "Decimal number", "35427", 1, 40,
                "[0-9]+", "Use decimal digits only."));
    }

    public Map<String, Object> alternateInput() { return Map.of("number", "4206"); }

    public String annotatedCode() {
        return """
               public String largestOddNumber(String number) {
                   for (int i = number.length() - 1; i >= 0; i--) {
                       // @a inspect
                       if ((number.charAt(i) - '0') % 2 == 1) return number.substring(0, i + 1);
                   }
                   // @a done
                   return "";
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String number = in.getString("number");
        String answer = "";
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            emit.at("inspect").say("Digit %d at index %d is %s.", digit, i,
                            digit % 2 == 1 ? "odd, so this prefix is the largest possible odd number"
                                    : "even, so remove it from the candidate suffix")
                    .var("index", i).var("digit", digit).chars(number, i).step();
            if (digit % 2 == 1) {
                answer = number.substring(0, i + 1);
                break;
            }
        }
        emit.at("done").say(answer.isEmpty()
                        ? "No odd digit exists, so there is no non-empty odd prefix."
                        : "Largest odd-valued prefix is \"%s\".", answer)
                .var("answer", answer).chars(number).step();
    }
}

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
               public String longestCommonPrefix(String[] words) {
                   String prefix = words[0];
                   for (int i = 1; i < words.length; i++) {
                       while (!words[i].startsWith(prefix)) {
                           // @a trim
                           prefix = prefix.substring(0, prefix.length() - 1);
                           if (prefix.isEmpty()) break;
                       }
                   }
                   // @a done
                   return prefix;
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String raw = in.getString("words");
        String[] words = raw.split(",");
        String prefix = words[0];
        for (int i = 1; i < words.length; i++) {
            while (!words[i].startsWith(prefix) && !prefix.isEmpty()) {
                String before = prefix;
                prefix = prefix.substring(0, prefix.length() - 1);
                emit.at("trim").say("\"%s\" does not start with \"%s\"; trim the candidate to \"%s\".",
                                words[i], before, prefix)
                        .var("wordIndex", i).var("word", words[i]).var("prefix", prefix)
                        .chars(words[i]).step();
            }
        }
        emit.at("done").say("Longest prefix shared by %s is \"%s\".", Arrays.toString(words), prefix)
                .var("answer", prefix).chars(raw).step();
    }
}
