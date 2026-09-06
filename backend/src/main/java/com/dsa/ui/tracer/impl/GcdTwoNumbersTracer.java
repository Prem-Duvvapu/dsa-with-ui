package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The Euclidean algorithm: repeatedly replace the larger of the two with its remainder
 * modulo the smaller, until one of them hits zero.
 */
@Component
public class GcdTwoNumbersTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "gcd-two-numbers";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("a", FieldType.INT).label("A").range(1, 100_000).defaultValue(52).build(),
                InputField.of("b", FieldType.INT).label("B").range(1, 100_000).defaultValue(12).build());
    }

    /** Coprime inputs — the loop runs a different number of times and ends in GCD 1. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("a", 17, "b", 5);
    }

    @Override
    public String annotatedCode() {
        return """
               public int findGCD(int a, int b) {
                   // @a start
                   while (a > 0 && b > 0) {
                       if (a > b) {
                           // @a modA
                           a = a % b;
                       } else {
                           // @a modB
                           b = b % a;
                       }
                   }
                   // @a done
                   if (a == 0) return b;
                   return a;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int a = in.getInt("a");
        int b = in.getInt("b");

        emit.at("start")
                .say("Find GCD(%d, %d) with the Euclidean algorithm.", a, b)
                .var("a", a).var("b", b)
                .array(new int[]{a, b}).step();

        while (a > 0 && b > 0) {
            if (a > b) {
                int before = a;
                a = a % b;
                emit.at("modA")
                        .say("a (%d) > b (%d) -> a = %d %% %d = %d.", before, b, before, b, a)
                        .var("a", a).var("b", b)
                        .array(new int[]{a, b}).step();
            } else {
                int before = b;
                b = b % a;
                emit.at("modB")
                        .say("b (%d) >= a (%d) -> b = %d %% %d = %d.", before, a, before, a, b)
                        .var("a", a).var("b", b)
                        .array(new int[]{a, b}).step();
            }
        }

        int gcd = (a == 0) ? b : a;
        emit.at("done")
                .say("One side reached 0 — GCD = %d.", gcd)
                .var("gcd", gcd)
                .array(new int[]{gcd}).step();
    }
}
