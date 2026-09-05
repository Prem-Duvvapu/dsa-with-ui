package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * A running box [l, r) records the furthest-reaching prefix match found so far. An index
 * still inside that box gets a head start from the box's mirror position instead of
 * comparing from scratch; every index, inside the box or not, still finishes by trying to
 * extend its match character by character, and only a match that reaches past the box's
 * current edge moves the box at all.
 */
@Component
public class ZFunctionTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "z-function-algo";
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
                        .length(1, 24)
                        .constraint("pattern", "[a-z]+")
                        .constraint("patternHint", "Lowercase letters a-z only.")
                        .defaultValue("aabxaabxcaabxaabxay")
                        .build());
    }

    /** Every character the same - the box grows maximally and every z-value is forced, no reuse needed. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "aaaaa");
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] zFunction(String s) {
                   int n = s.length();
                   int[] z = new int[n];
                   int l = 0, r = 0;
                   // @a init
                   for (int i = 1; i < n; i++) {
                       if (i < r) {
                           // @a reuseBox
                           z[i] = Math.min(r - i, z[i - l]);
                       }
                       while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) {
                           // @a extend
                           z[i]++;
                       }
                       if (i + z[i] > r) {
                           // @a expandBox
                           l = i;
                           r = i + z[i];
                       }
                   }
                   // @a done
                   return z;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int n = s.length();
        int[] z = new int[n];
        int l = 0;
        int r = 0;

        emit.at("init")
                .say("z[0] is left undefined - every position trivially matches itself as a prefix. "
                        + "Start scanning from index 1 with an empty box.")
                .var("l", l).var("r", r).var("z", Arrays.toString(z))
                .chars(s, 0, -1).step();

        for (int i = 1; i < n; i++) {
            if (i < r) {
                z[i] = Math.min(r - i, z[i - l]);
                emit.at("reuseBox")
                        .say("Index %d is inside the box [%d, %d) - reuse z[%d]=%d, capped by the box's "
                                + "remaining reach %d, for a head start of z[%d]=%d.",
                                i, l, r, i - l, z[i - l], r - i, i, z[i])
                        .var("l", l).var("r", r).var("i", i).var("z", Arrays.toString(z))
                        .chars(s, i, i - l).step();
            }

            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) {
                z[i]++;
                emit.at("extend")
                        .say("s[%d]='%c' matches s[%d]='%c' - extend the match at index %d to length %d.",
                                z[i] - 1, s.charAt(z[i] - 1), i + z[i] - 1, s.charAt(i + z[i] - 1), i, z[i])
                        .var("i", i).var("z", Arrays.toString(z))
                        .chars(s, i + z[i] - 1, z[i] - 1).step();
            }

            if (i + z[i] > r) {
                l = i;
                r = i + z[i];
                emit.at("expandBox")
                        .say("The match at index %d reaches past the current box - the box becomes "
                                + "[%d, %d), the furthest any match has reached so far.", i, l, r)
                        .var("l", l).var("r", r).var("z", Arrays.toString(z))
                        .chars(s, i, -1).step();
            }
        }

        emit.at("done")
                .say("Every index considered. Z array complete: %s.", Arrays.toString(z))
                .var("z", Arrays.toString(z))
                .chars(s).step();
    }
}
