package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Every integer's least-significant bit is 1 exactly when it is odd — two's complement
 * makes {@code N & 1} correct for negative N too, so no sign handling is needed.
 */
@Component
public class CheckNumberOddTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "check-number-odd";
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
                        .help("Integer to test.")
                        .range(-1_000_000, 1_000_000)
                        .defaultValue(13)
                        .build());
    }

    /** 8 is even — the branch the odd default never reaches. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 8);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isOdd(int n) {
                   // @a check
                   boolean odd = (n & 1) == 1;
                   // @a done
                   return odd;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");

        int lsb = n & 1;
        emit.at("check")
                .say("N & 1 = %d & 1 = %d — the least-significant bit alone.", n, lsb)
                .var("N", n).var("N & 1", lsb).bits(n, 0).step();

        boolean odd = lsb == 1;
        emit.at("done")
                .say(odd ? String.format("%d is ODD.", n) : String.format("%d is EVEN.", n))
                .var("isOdd", odd).bits(n, 0).step();
    }
}
