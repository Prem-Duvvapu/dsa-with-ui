package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A window stays valid as long as {@code windowLength - mostFrequentCharCount <= k}: every
 * other character in it can be replaced with the majority one within budget. The window
 * only ever grows, even past a character that breaks validity - it just stops growing the
 * ANSWER while sliding both ends together, since a shorter valid window later can never
 * beat the longest one already found.
 */
@Component
public class LongestRepeatingCharacterReplacementTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "longest-repeating-character-replacement";
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
                        .length(1, 40)
                        .defaultValue("ABAB")
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K")
                        .help("Characters allowed to replace.")
                        .range(0, 40)
                        .defaultValue(2)
                        .build());
    }

    /** A different string and a smaller K - the window is bounded by a run, not free replacement. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "AABABBA", "k", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public int characterReplacement(String s, int k) {
                   int[] freq = new int[26];
                   int left = 0, maxFreq = 0, maxLen = 0;

                   for (int right = 0; right < s.length(); right++) {
                       // @a expand
                       freq[s.charAt(right) - 'A']++;
                       maxFreq = Math.max(maxFreq, freq[s.charAt(right) - 'A']);

                       int windowLen = right - left + 1;
                       if (windowLen - maxFreq > k) {
                           // @a shrink
                           freq[s.charAt(left) - 'A']--;
                           left++;
                       }

                       // @a windowComplete
                       maxLen = Math.max(maxLen, right - left + 1);
                   }
                   // @a done
                   return maxLen;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int k = in.getInt("k");
        int[] freq = new int[128];
        int left = 0;
        int maxFreq = 0;
        int maxLen = 0;

        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            freq[c]++;
            maxFreq = Math.max(maxFreq, freq[c]);
            emit.at("expand")
                    .say("Add '%c'. Most frequent character in the window now appears %d time%s.",
                            c, maxFreq, maxFreq == 1 ? "" : "s")
                    .var("right", right).var("maxFreq", maxFreq)
                    .arrayState(windowState(s, left, right)).step();

            int windowLen = right - left + 1;
            if (windowLen - maxFreq > k) {
                char dropped = s.charAt(left);
                freq[dropped]--;
                left++;
                emit.at("shrink")
                        .say("Window of %d needs more than %d replacements - shrink past '%c'. "
                                        + "Window now starts at %d.",
                                windowLen, k, dropped, left)
                        .var("left", left)
                        .arrayState(windowState(s, left, right)).step();
            }

            maxLen = Math.max(maxLen, right - left + 1);
            emit.at("windowComplete")
                    .say("Window [%d,%d] is valid with at most %d replacements. Best so far: %d.",
                            left, right, k, maxLen)
                    .var("windowLen", right - left + 1).var("maxLen", maxLen)
                    .arrayState(windowState(s, left, right)).step();
        }

        emit.at("done")
                .say("Every character processed. Longest window achievable with %d replacements: %d.", k, maxLen)
                .var("answer", maxLen)
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
