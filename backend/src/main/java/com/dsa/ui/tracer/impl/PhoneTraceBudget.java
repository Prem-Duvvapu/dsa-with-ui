package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.tracer.InputSpec;
import com.dsa.ui.tracer.InputValidationException;
import com.dsa.ui.tracer.StepEmitter;
import java.util.ArrayList;
import java.util.Map;

/** Safety-only shape accounting: no result generation and no execution events. */
final class PhoneTraceBudget {
    record Cost(long steps, long bytes) {}
    static void requireFits(String digits, InputSpec spec) {
        Cost cost = estimate(digits);
        if (cost.steps() > spec.getMaxSteps() || cost.bytes() > spec.getMaxBytes()) {
            throw new InputValidationException(Map.of("digits",
                    "This input creates too much animation data for one run. Try fewer digits, especially 7 or 9."));
        }
    }
    static Cost estimate(String digits) {
        long leaves = 1, edges = 0, bytes = 0;
        for (int depth = 0; depth < digits.length(); depth++) {
            leaves *= LetterCombinationsPhoneTracer.letters(digits.charAt(depth)).length();
            edges += leaves;
            String prefix = "a".repeat(depth);
            bytes += leaves * cost(depth, depth + 1,
                    LetterCombinationsPhoneTracer.chooseMessage(digits.charAt(depth), 'a', prefix, depth + 1),
                    Map.of("digit", String.valueOf(digits.charAt(depth)), "letter", "a"));
        }
        String value = "a".repeat(digits.length());
        // Captures differ in cost only when their decimal count gains a digit.
        for (long first = 1; first <= leaves; first *= 10) {
            long count = Math.min(leaves, first * 10 - 1) - first + 1;
            bytes += count * cost(digits.length(), digits.length(),
                    LetterCombinationsPhoneTracer.captureMessage(value, (int) first),
                    Map.of("combination", value, "count", String.valueOf(first)));
        }
        // ASCII words and List.toString's brackets/separators occupy exactly P*(N+2) chars.
        int outputLength = Math.toIntExact(leaves * (digits.length() + 2L));
        bytes += cost(-1, 0, LetterCombinationsPhoneTracer.doneMessage((int) leaves),
                Map.of("combinations", String.valueOf(leaves), "results", "a".repeat(outputLength)));
        return new Cost(edges + leaves + 1, bytes);
    }
    private static long cost(int depth, int characters, String message, Map<String, String> variables) {
        var snapshot = new ExecutionStep();
        snapshot.setDescription(message);
        snapshot.setVariables(variables);
        var frames = new ArrayList<String>();
        for (int index = 0; index <= depth; index++) frames.add("backtrack(idx=" + index + ")");
        snapshot.setCallStack(frames);
        var track = new ArrayList<ArrayElement>();
        for (int index = 0; index < characters; index++) track.add(new ArrayElement(index, 97, "default", "a"));
        snapshot.setArrayState(track);
        // Template characters measure byte lengths only; they are never emitted as data.
        return StepEmitter.estimateBytes(snapshot);
    }
}
