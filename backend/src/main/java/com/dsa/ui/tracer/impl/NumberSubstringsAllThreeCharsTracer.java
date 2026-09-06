package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Number of Substrings Containing All Three Characters — O(N) sliding window.
 * Track the last seen index of 'a', 'b', and 'c'. At each character index i,
 * the shortest valid window ending at i starts at {@code min(last[a], last[b], last[c])}.
 * Every prefix of that window ending at i is also valid, adding {@code 1 + minLast} substrings.
 */
@Component
public class NumberSubstringsAllThreeCharsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "number-substrings-all-three-chars";
    }

    @Override
    public DsType dsType() {
        return DsType.STRING;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("s", FieldType.STRING)
                        .label("String (a, b, c)")
                        .help("String consisting only of 'a', 'b', and 'c'.")
                        .length(1, 40)
                        .defaultValue("abcabc")
                        .build());
    }

    /** Alternate string with a run of 'a' before 'c' and 'b'. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "aaacb");
    }

    @Override
    public String annotatedCode() {
        return """
               public int numberOfSubstrings(String s) {
                   int[] last = new int[]{-1, -1, -1};
                   int count = 0;

                   for (int i = 0; i < s.length(); i++) {
                       // @a updateLastSeen
                       last[s.charAt(i) - 'a'] = i;
                       if (last[0] != -1 && last[1] != -1 && last[2] != -1) {
                           // @a validWindow
                           int minLast = Math.min(last[0], Math.min(last[1], last[2]));
                           count += 1 + minLast;
                       }
                   }
                   // @a done
                   return count;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int[] last = new int[]{-1, -1, -1};
        int count = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'c') {
                last[c - 'a'] = i;
            }
            emit.at("updateLastSeen")
                    .say("Index %d: char '%c'. Last seen: a=%d, b=%d, c=%d.",
                            i, c, last[0], last[1], last[2])
                    .var("i", i).var("char", String.valueOf(c))
                    .var("lastA", last[0]).var("lastB", last[1]).var("lastC", last[2])
                    .arrayState(windowState(s, -1, i))
                    .step();

            if (last[0] != -1 && last[1] != -1 && last[2] != -1) {
                int minLast = Math.min(last[0], Math.min(last[1], last[2]));
                int added = 1 + minLast;
                count += added;
                emit.at("validWindow")
                        .say("All 3 characters present! Shortest window ending at %d is [%d,%d]. "
                                        + "Substrings starting at 0..%d ending at %d are valid (+%d). Total: %d.",
                                i, minLast, i, minLast, i, added, count)
                        .var("minLast", minLast).var("i", i).var("added", added).var("total", count)
                        .arrayState(windowState(s, minLast, i))
                        .step();
            }
        }

        emit.at("done")
                .say("Finished string. Total substrings containing at least one 'a', 'b', and 'c': %d.", count)
                .var("answer", count)
                .arrayState(windowState(s, -1, -1))
                .step();
    }

    private static List<ArrayElement> windowState(String s, int left, int right) {
        List<ArrayElement> state = new ArrayList<>(s.length());
        for (int i = 0; i < s.length(); i++) {
            String st = (i == right) ? "current"
                    : (left >= 0 && i == left) ? "target"
                    : (left >= 0 && i > left && i < right) ? "active"
                    : "default";
            state.add(new ArrayElement(i, s.charAt(i), st, String.valueOf(s.charAt(i))));
        }
        return state;
    }
}
