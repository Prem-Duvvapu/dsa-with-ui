package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The six bitwise primitives every later Bit Manipulation tracer builds on, run once each
 * against the caller's own pair of operands so this is a real demonstration rather than a
 * static slide: AND, OR, XOR, NOT, and both shifts, each shown as the 32-bit track the
 * operation actually produces.
 */
@Component
public class IntroBitsTricksTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "intro-bits-tricks";
    }

    @Override
    public DsType dsType() {
        return DsType.BITS;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("n", FieldType.INT)
                        .label("N")
                        .help("First operand.")
                        .range(0, 1_000_000)
                        .defaultValue(12)
                        .build(),
                InputField.of("m", FieldType.INT)
                        .label("M")
                        .help("Second operand, for AND/OR/XOR.")
                        .range(0, 1_000_000)
                        .defaultValue(10)
                        .build(),
                InputField.of("shift", FieldType.INT)
                        .label("Shift amount")
                        .help("Bit positions to shift N by.")
                        .range(0, 31)
                        .defaultValue(2)
                        .build());
    }

    /** A different bit pattern throughout, not a relabelled 12/10/2. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 5, "m", 3, "shift", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public void bitwiseTricks(int n, int m, int shift) {
                   // @a and
                   int and = n & m;
                   // @a or
                   int or = n | m;
                   // @a xor
                   int xor = n ^ m;
                   // @a not
                   int not = ~n;
                   // @a leftShift
                   int left = n << shift;
                   // @a rightShift
                   int right = n >> shift;
                   // @a done
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        int m = in.getInt("m");
        int shift = in.getInt("shift");

        int and = n & m;
        emit.at("and")
                .say("N & M = %d & %d = %d — a bit survives only where BOTH operands have it set.", n, m, and)
                .var("N", n).var("M", m).var("N & M", and).bits(and).step();

        int or = n | m;
        emit.at("or")
                .say("N | M = %d | %d = %d — a bit survives if EITHER operand has it set.", n, m, or)
                .var("N | M", or).bits(or).step();

        int xor = n ^ m;
        emit.at("xor")
                .say("N ^ M = %d ^ %d = %d — a bit survives only where the operands DISAGREE.", n, m, xor)
                .var("N ^ M", xor).bits(xor).step();

        int not = ~n;
        emit.at("not")
                .say("~N = ~%d = %d — every bit of N flips, including the sign bit.", n, not)
                .var("~N", not).bits(not).step();

        int left = n << shift;
        emit.at("leftShift")
                .say("N << %d = %d << %d = %d — every bit moves left, multiplying by 2^%d.", shift, n, shift, left, shift)
                .var("N << shift", left).bits(left).step();

        int right = n >> shift;
        emit.at("rightShift")
                .say("N >> %d = %d >> %d = %d — every bit moves right, dividing by 2^%d.", shift, n, shift, right, shift)
                .var("N >> shift", right).bits(right).step();

        emit.at("done")
                .say("Six primitives, one operand pair: AND=%d, OR=%d, XOR=%d, NOT=%d, N<<%d=%d, N>>%d=%d.",
                        and, or, xor, not, shift, left, shift, right)
                .var("summary", String.format("AND=%d OR=%d XOR=%d NOT=%d L=%d R=%d", and, or, xor, not, left, right))
                .bits(n).step();
    }
}
