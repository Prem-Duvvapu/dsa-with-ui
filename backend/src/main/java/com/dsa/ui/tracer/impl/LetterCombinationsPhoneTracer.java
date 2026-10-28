package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Executes each real keypad branch; output and the exploration trace are distinct. */
@Component
public class LetterCombinationsPhoneTracer implements AlgorithmTracer {
    private static final String SOURCE = SolutionSource.read("/solutions/core/letter-combinations-phone.java");
    private static final String[] KEYPAD = {
            "", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"
    };
    public String id() { return "letter-combinations-phone"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SOURCE; }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("digits", FieldType.STRING).label("Digits")
                .help("Digits 2-9 only. Long sequences, especially with 7 or 9, may create too much animation data; try fewer digits if refused. Random samples use up to five digits.")
                .length(1, 6).constraint("pattern", "[2-9]+")
                .constraint("patternHint", "Digits 2-9 only.")
                .constraint("alphabet", "23456789").constraint("randomMaxLength", 5)
                .defaultValue("23").build());
    }
    public Map<String, Object> alternateInput() { return Map.of("digits", "2"); }
    static String letters(char digit) { return KEYPAD[digit - '0']; }
    static String chooseMessage(char digit, char letter, String prefix, int nextIndex) {
        return String.format(Locale.ROOT,
                "Digit '%c' maps to letter '%c' - append it to \"%s\" and recurse into digit index %d.",
                digit, letter, prefix, nextIndex);
    }
    static String captureMessage(String value, int count) {
        return String.format(Locale.ROOT, "Every digit mapped - capture \"%s\" as combination #%d.", value, count);
    }
    static String doneMessage(int count) {
        return String.format(Locale.ROOT, "Every digit's letters tried at every depth. %d combination%s generated.",
                count, count == 1 ? "" : "s");
    }
    public void run(Inputs in, StepEmitter emit) {
        String digits = in.getString("digits");
        // InputValidator owns field validation; refuse oversized trace shapes before any event.
        PhoneTraceBudget.requireFits(digits, inputSpec());
        List<String> results = new ArrayList<>();
        backtrack(0, digits, new StringBuilder(), results, emit);
        emit.at("done").say(doneMessage(results.size()))
                .var("combinations", results.size()).var("results", results).chars("").step();
    }
    private void backtrack(int index, String digits, StringBuilder path, List<String> results, StepEmitter emit) {
        emit.push("backtrack(idx=" + index + ")");
        if (index == digits.length()) {
            String value = path.toString();
            results.add(value);
            emit.at("capture").say(captureMessage(value, results.size()))
                    .var("combination", value).var("count", results.size()).chars(value).step();
            emit.pop();
            return;
        }
        char digit = digits.charAt(index);
        for (char letter : letters(digit).toCharArray()) {
            var choice = emit.at("choose").say(chooseMessage(digit, letter, path.toString(), index + 1));
            path.append(letter);
            choice.var("digit", digit).var("letter", letter).chars(path.toString()).step();
            backtrack(index + 1, digits, path, results, emit);
            path.deleteCharAt(path.length() - 1);
        }
        emit.pop();
    }
}
