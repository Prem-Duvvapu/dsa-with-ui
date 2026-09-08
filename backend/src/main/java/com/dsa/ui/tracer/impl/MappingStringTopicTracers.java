package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
class IsomorphicStringsTracer extends StringTracerSupport {
    public String id() { return "isomorphic-strings"; }

    public InputSpec inputSpec() {
        return InputSpec.of(
                patternedText("s", "Source", "egg", 1, 30, "[a-z]+", "Use lowercase letters a-z."),
                patternedText("t", "Target", "add", 1, 30, "[a-z]+", "Use lowercase letters a-z."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "foo", "t", "bar"); }

    public String annotatedCode() {
        return """
               public boolean isIsomorphic(String s, String t) {
                   Map<Character, Character> forward = new HashMap<>(), reverse = new HashMap<>();
                   for (int i = 0; i < s.length(); i++) {
                       // @a map
                       if (forward.getOrDefault(s.charAt(i), t.charAt(i)) != t.charAt(i) ||
                           reverse.getOrDefault(t.charAt(i), s.charAt(i)) != s.charAt(i)) return false;
                       forward.put(s.charAt(i), t.charAt(i));
                       reverse.put(t.charAt(i), s.charAt(i));
                   }
                   // @a done
                   return s.length() == t.length();
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        String t = in.getString("t");
        Map<Character, Character> forward = new LinkedHashMap<>();
        Map<Character, Character> reverse = new LinkedHashMap<>();
        boolean valid = s.length() == t.length();
        for (int i = 0; valid && i < s.length(); i++) {
            char a = s.charAt(i);
            char b = t.charAt(i);
            Character mapped = forward.get(a);
            Character reversed = reverse.get(b);
            valid = (mapped == null || mapped == b) && (reversed == null || reversed == a);
            if (valid) {
                forward.put(a, b);
                reverse.put(b, a);
            }
            emit.at("map").say("Index %d compares '%c' → '%c': %s.", i, a, b,
                            valid ? "both directions remain one-to-one" : "an existing mapping conflicts")
                    .var("index", i).var("forward", forward).var("reverse", reverse).var("valid", valid)
                    .chars(s, i).step();
        }
        emit.at("done").say("The strings are %sisomorphic.", valid ? "" : "not ")
                .var("answer", valid).chars(s).step();
    }
}

@Component
class RotateStringTracer extends StringTracerSupport {
    public String id() { return "rotate-string"; }

    public InputSpec inputSpec() {
        return InputSpec.of(
                patternedText("s", "Source", "abcde", 1, 30, "[a-z]+", "Use lowercase letters a-z."),
                patternedText("goal", "Goal", "cdeab", 1, 30, "[a-z]+", "Use lowercase letters a-z."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "abcde", "goal", "abced"); }

    public String annotatedCode() {
        return """
               public boolean rotateString(String s, String goal) {
                   if (s.length() != goal.length()) return false;
                   for (int shift = 0; shift < s.length(); shift++) {
                       // @a compare
                       if ((s.substring(shift) + s.substring(0, shift)).equals(goal)) return true;
                   }
                   // @a done
                   return false;
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        String goal = in.getString("goal");
        boolean found = false;
        if (s.length() == goal.length()) {
            for (int shift = 0; shift < s.length(); shift++) {
                String candidate = s.substring(shift) + s.substring(0, shift);
                found = candidate.equals(goal);
                emit.at("compare").say("Left rotation by %d gives \"%s\": %s goal \"%s\".",
                                shift, candidate, found ? "matches" : "does not match", goal)
                        .var("shift", shift).var("candidate", candidate).var("matches", found)
                        .chars(candidate).step();
                if (found) break;
            }
        } else {
            emit.at("compare").say("Lengths differ (%d vs %d), so no rotation can match.",
                            s.length(), goal.length())
                    .var("sourceLength", s.length()).var("goalLength", goal.length()).chars(s).step();
        }
        emit.at("done").say("Rotation check result: %s.", found)
                .var("answer", found).chars(s).step();
    }
}

@Component
class SortCharactersFrequencyTracer extends StringTracerSupport {
    public String id() { return "sort-characters-frequency"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("s", "String", "tree", 1, 40,
                "[A-Za-z0-9]+", "Use letters and digits only."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "cccaaa"); }

    public String annotatedCode() {
        return """
               public String frequencySort(String s) {
                   Map<Character, Integer> counts = new HashMap<>();
                   for (char ch : s.toCharArray()) {
                       // @a count
                       counts.merge(ch, 1, Integer::sum);
                   }
                   List<Character> order = new ArrayList<>(counts.keySet());
                   order.sort((a, b) -> counts.get(b) - counts.get(a));
                   StringBuilder answer = new StringBuilder();
                   for (char ch : order) {
                       // @a append
                       answer.append(String.valueOf(ch).repeat(counts.get(ch)));
                   }
                   // @a done
                   return answer.toString();
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        Map<Character, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < s.length(); i++) {
            counts.merge(s.charAt(i), 1, Integer::sum);
            emit.at("count").say("Count '%c' at index %d; frequency is now %d.",
                            s.charAt(i), i, counts.get(s.charAt(i)))
                    .var("index", i).var("counts", counts).chars(s, i).step();
        }
        List<Character> order = new ArrayList<>(counts.keySet());
        order.sort(Comparator.<Character>comparingInt(counts::get).reversed().thenComparingInt(ch -> ch));
        StringBuilder answer = new StringBuilder();
        for (char ch : order) {
            answer.append(String.valueOf(ch).repeat(counts.get(ch)));
            emit.at("append").say("Append '%c' %d time(s); output is now \"%s\".",
                            ch, counts.get(ch), answer)
                    .var("character", ch).var("frequency", counts.get(ch)).var("output", answer)
                    .chars(s).step();
        }
        emit.at("done").say("Characters sorted by descending frequency: \"%s\".", answer)
                .var("answer", answer).chars(s).step();
    }
}

@Component
class MaxNestingDepthParenthesesTracer extends StringTracerSupport {
    public String id() { return "max-nesting-depth-parentheses"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("expression", "Expression", "(1+(2*3)+((8)/4))+1", 1, 50,
                "[A-Za-z0-9()+*/+ -]+", "Use letters, digits, spaces, arithmetic operators, and parentheses."));
    }

    public Map<String, Object> alternateInput() { return Map.of("expression", "(a+(b+c))"); }

    public String annotatedCode() {
        return """
               public int maxDepth(String expression) {
                   int depth = 0, maximum = 0;
                   for (char ch : expression.toCharArray()) {
                       // @a scan
                       if (ch == '(') maximum = Math.max(maximum, ++depth);
                       else if (ch == ')') depth--;
                   }
                   // @a done
                   return maximum;
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String expression = in.getString("expression");
        int depth = 0;
        int maximum = 0;
        boolean valid = true;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(') maximum = Math.max(maximum, ++depth);
            else if (ch == ')') {
                depth--;
                if (depth < 0) valid = false;
            }
            emit.at("scan").say("Index %d '%c': current depth=%d, maximum=%d.", i, ch, depth, maximum)
                    .var("index", i).var("depth", depth).var("maximum", maximum)
                    .chars(expression, i).step();
        }
        valid &= depth == 0;
        emit.at("done").say(valid ? "Maximum parenthesis nesting depth is %d."
                        : "Parentheses are unbalanced; measured maximum before the mismatch was %d.", maximum)
                .var("valid", valid).var("answer", maximum).chars(expression).step();
    }
}

@Component
class RomanToIntegerTracer extends StringTracerSupport {
    public String id() { return "roman-to-integer"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("roman", "Roman numeral", "MCMXCIV", 1, 15,
                "[IVXLCDM]+", "Use uppercase Roman symbols I, V, X, L, C, D, and M."));
    }

    public Map<String, Object> alternateInput() { return Map.of("roman", "LVIII"); }

    public String annotatedCode() {
        return """
               public int romanToInt(String roman) {
                   int total = 0, previous = 0;
                   for (int i = roman.length() - 1; i >= 0; i--) {
                       int value = valueOf(roman.charAt(i));
                       // @a inspect
                       total += value < previous ? -value : value;
                       previous = Math.max(previous, value);
                   }
                   // @a done
                   return total;
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String roman = in.getString("roman");
        int total = 0;
        int previous = 0;
        for (int i = roman.length() - 1; i >= 0; i--) {
            int value = valueOf(roman.charAt(i));
            boolean subtract = value < previous;
            total += subtract ? -value : value;
            previous = Math.max(previous, value);
            emit.at("inspect").say("Symbol %c is worth %d and is %s; running total=%d.",
                            roman.charAt(i), value, subtract ? "smaller than the symbol to its right, so subtract it" : "added",
                            total)
                    .var("index", i).var("value", value).var("operation", subtract ? "subtract" : "add")
                    .var("total", total).chars(roman, i).step();
        }
        emit.at("done").say("Roman numeral %s converts to %d.", roman, total)
                .var("answer", total).chars(roman).step();
    }

    private static int valueOf(char symbol) {
        return switch (symbol) {
            case 'I' -> 1;
            case 'V' -> 5;
            case 'X' -> 10;
            case 'L' -> 50;
            case 'C' -> 100;
            case 'D' -> 500;
            case 'M' -> 1000;
            default -> throw new IllegalArgumentException("Unsupported Roman symbol: " + symbol);
        };
    }
}
