package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The window only ever grows on the right and shrinks from the left, and shrinking is
 * always tried the moment the window covers every needed character - a window that covers
 * t can only get shorter by dropping characters from the front, never by looking for a
 * better start from scratch. {@code have == need} tracks coverage in O(1): it changes only
 * when a character's own count crosses from matching to not, not on every insertion.
 */
@Component
public class MinimumWindowSubstringTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "minimum-window-substring";
    }

    @Override
    public DsType dsType() {
        return DsType.STRING;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("s", FieldType.STRING)
                        .label("String s")
                        .length(1, 40)
                        .defaultValue("ADOBECODEBANC")
                        .build(),
                InputField.of("t", FieldType.STRING)
                        .label("Pattern t")
                        .help("Every character (with multiplicity) must appear in the window.")
                        .length(1, 15)
                        .defaultValue("ABC")
                        .build());
    }

    /** A single-character s and t - the window can only ever be the whole string or none of it. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "a", "t", "a");
    }

    @Override
    public String annotatedCode() {
        return """
               public String minWindow(String s, String t) {
                   Map<Character, Integer> need = new HashMap<>();
                   for (char c : t.toCharArray()) need.merge(c, 1, Integer::sum);

                   Map<Character, Integer> window = new HashMap<>();
                   int have = 0, needCount = need.size();
                   int left = 0, bestLen = Integer.MAX_VALUE, bestStart = 0;

                   for (int right = 0; right < s.length(); right++) {
                       // @a expand
                       char c = s.charAt(right);
                       window.merge(c, 1, Integer::sum);
                       if (need.containsKey(c) && window.get(c).intValue() == need.get(c).intValue()) have++;

                       while (have == needCount) {
                           // @a recordBest
                           if (right - left + 1 < bestLen) {
                               bestLen = right - left + 1;
                               bestStart = left;
                           }
                           // @a shrink
                           char leftChar = s.charAt(left);
                           window.merge(leftChar, -1, Integer::sum);
                           if (need.containsKey(leftChar) && window.get(leftChar) < need.get(leftChar)) have--;
                           left++;
                       }
                   }
                   // @a done
                   return bestLen == Integer.MAX_VALUE ? "" : s.substring(bestStart, bestStart + bestLen);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        String t = in.getString("t");

        Map<Character, Integer> need = new HashMap<>();
        for (char c : t.toCharArray()) need.merge(c, 1, Integer::sum);
        int needCount = need.size();

        Map<Character, Integer> window = new HashMap<>();
        int have = 0;
        int left = 0;
        int bestLen = Integer.MAX_VALUE;
        int bestStart = 0;

        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            window.merge(c, 1, Integer::sum);
            if (need.containsKey(c) && window.get(c).intValue() == need.get(c).intValue()) have++;
            emit.at("expand")
                    .say("Add '%c'. Covering %d of %d required distinct characters.", c, have, needCount)
                    .var("right", right).var("have", have).var("need", needCount)
                    .arrayState(windowState(s, left, right)).step();

            while (have == needCount) {
                if (right - left + 1 < bestLen) {
                    bestLen = right - left + 1;
                    bestStart = left;
                    emit.at("recordBest")
                            .say("Window [%d,%d] (\"%s\") covers t and beats the best so far: length %d.",
                                    left, right, s.substring(left, right + 1), bestLen)
                            .var("bestLen", bestLen).var("bestWindow", s.substring(left, right + 1))
                            .arrayState(windowState(s, left, right)).step();
                }

                char leftChar = s.charAt(left);
                window.merge(leftChar, -1, Integer::sum);
                if (need.containsKey(leftChar) && window.get(leftChar) < need.get(leftChar)) have--;
                left++;
                emit.at("shrink")
                        .say("Shrink past '%c' - window now starts at %d (covering %d of %d).",
                                leftChar, left, have, needCount)
                        .var("left", left).var("have", have)
                        .arrayState(windowState(s, left, right)).step();
            }
        }

        String answer = bestLen == Integer.MAX_VALUE ? "" : s.substring(bestStart, bestStart + bestLen);
        emit.at("done")
                .say(answer.isEmpty()
                        ? "No window of s covers every character of t."
                        : "Shortest window covering t: \"%s\".", answer)
                .var("answer", answer)
                .arrayState(windowState(s, -1, -1)).step();
    }

    private static List<ArrayElement> windowState(String s, int left, int right) {
        List<ArrayElement> state = new ArrayList<>(s.length());
        for (int i = 0; i < s.length(); i++) {
            String st = i == right ? "current" : i == left ? "target" : (i > left && i < right) ? "active" : "default";
            state.add(new ArrayElement(i, s.charAt(i), st, String.valueOf(s.charAt(i))));
        }
        return state;
    }
}
