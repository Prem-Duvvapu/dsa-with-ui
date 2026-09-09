package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Three XOR assignments swap two integers without a third variable: the first XOR folds
 * both values into {@code a}; the second recovers the original {@code a} into {@code b}
 * (XOR with itself cancels); the third recovers the original {@code b} into {@code a} the
 * same way. {@code a} and {@code b} are independent locals here, not aliases of the same
 * memory, so this is safe for every input including {@code a == b}.
 */
@Component
public class SwapTwoNumbersTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "swap-two-numbers";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("a", FieldType.INT)
                        .label("A")
                        .help("First value.")
                        .range(-1_000_000, 1_000_000)
                        .defaultValue(5)
                        .build(),
                InputField.of("b", FieldType.INT)
                        .label("B")
                        .help("Second value.")
                        .range(-1_000_000, 1_000_000)
                        .defaultValue(9)
                        .build());
    }

    /** A = 0: the first XOR is a no-op (a ^ 0 = a), the branch the default never shows. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("a", 0, "b", 15);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] swap(int a, int b) {
                   // @a foldB
                   a = a ^ b;
                   // @a recoverA
                   b = a ^ b;
                   // @a recoverB
                   a = a ^ b;
                   // @a done
                   return new int[]{a, b};
               }""";
    }

    private List<ArrayElement> pair(int a, int b) {
        return List.of(new ArrayElement(0, a, "current"), new ArrayElement(1, b, "target"));
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int a = in.getInt("a");
        int b = in.getInt("b");

        emit.at("foldB")
                .say("Start: a = %d, b = %d. a = a ^ b = %d ^ %d = %d — both values are now folded into a.",
                        a, b, a, b, a ^ b)
                .var("a", a).var("b", b).arrayState(pair(a, b)).step();
        a = a ^ b;

        emit.at("recoverA")
                .say("b = a ^ b = %d ^ %d = %d — XORing the fold against the still-original b cancels b, "
                        + "leaving the original a.", a, b, a ^ b)
                .var("a", a).var("b", b).arrayState(pair(a, b)).step();
        b = a ^ b;

        emit.at("recoverB")
                .say("a = a ^ b = %d ^ %d = %d — XORing the fold against the recovered original a cancels a, "
                        + "leaving the original b.", a, b, a ^ b)
                .var("a", a).var("b", b).arrayState(pair(a, b)).step();
        a = a ^ b;

        emit.at("done")
                .say("Swap complete, no third variable used: a = %d, b = %d.", a, b)
                .var("a", a).var("b", b).arrayState(pair(a, b)).step();
    }
}
