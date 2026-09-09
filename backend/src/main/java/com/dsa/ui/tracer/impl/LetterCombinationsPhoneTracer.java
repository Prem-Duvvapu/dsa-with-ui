package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One digit, one recursion level: each call handles exactly the letters mapped to the digit
 * at its own index, appending one and recursing into the next digit, so the recursion depth
 * always equals the input length and the fan-out at each level is just that digit's letter
 * count — never more, never fewer.
 */
@Component
public class LetterCombinationsPhoneTracer implements AlgorithmTracer {

    private static final String[] KEYPAD = {
            "", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"
    };

    @Override
    public String id() {
        return "letter-combinations-phone";
    }

    @Override
    public DsType dsType() {
        return DsType.STRING;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("digits", FieldType.STRING)
                        .label("Digits")
                        .help("Digits 2-9 only (0 and 1 map to no letters).")
                        .length(1, 6)
                        .constraint("pattern", "[2-9]+")
                        .constraint("patternHint", "Digits 2-9 only.")
                        .defaultValue("23")
                        .build());
    }

    /** A single digit - three combinations instead of nine, no recursive depth at all beyond the one digit. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("digits", "2");
    }

    @Override
    public String annotatedCode() {
        return """
               public List<String> letterCombinations(String digits) {
                   List<String> res = new ArrayList<>();
                   backtrack(0, digits, "", res);
                   // @a done
                   return res;
               }

               private void backtrack(int idx, String digits, String cur, List<String> res) {
                   if (idx == digits.length()) {
                       // @a capture
                       res.add(cur);
                       return;
                   }
                   String letters = KEYPAD[digits.charAt(idx) - '0'];
                   for (char c : letters.toCharArray()) {
                       // @a choose
                       backtrack(idx + 1, digits, cur + c, res);
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String digits = in.getString("digits");
        List<String> res = new ArrayList<>();
        backtrack(0, digits, "", res, emit);

        emit.at("done")
                .say("Every digit's letters tried at every depth. %d combination%s generated.",
                        res.size(), res.size() == 1 ? "" : "s")
                .var("combinations", res.size()).chars("").step();
    }

    private void backtrack(int idx, String digits, String cur, List<String> res, StepEmitter emit) {
        emit.push("backtrack(idx=" + idx + ")");

        if (idx == digits.length()) {
            res.add(cur);
            emit.at("capture")
                    .say("Every digit mapped - capture \"%s\" as combination #%d.", cur, res.size())
                    .var("combination", cur).var("count", res.size()).chars(cur).step();
            emit.pop();
            return;
        }

        char digit = digits.charAt(idx);
        String letters = KEYPAD[digit - '0'];
        for (char c : letters.toCharArray()) {
            String next = cur + c;
            emit.at("choose")
                    .say("Digit '%c' maps to letter '%c' - append it to \"%s\" and recurse into digit index %d.",
                            digit, c, cur, idx + 1)
                    .var("digit", String.valueOf(digit)).var("letter", String.valueOf(c)).chars(next).step();
            backtrack(idx + 1, digits, next, res, emit);
        }

        emit.pop();
    }
}
