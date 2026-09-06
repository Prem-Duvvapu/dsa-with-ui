package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Brian Kernighan's algorithm: {@code n & (n - 1)} clears the rightmost set bit each
 * time, so the loop runs exactly once per set bit rather than once per bit position.
 */
@Component
public class CountSetBitsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-set-bits";
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
                        .help("Non-negative integer to count the set bits of.")
                        .range(0, 1_000_000_000)
                        .defaultValue(29)
                        .build());
    }

    /** N = 0 skips the loop entirely — the branch profile the default never reaches. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 0);
    }

    @Override
    public String annotatedCode() {
        return """
               public int countSetBits(int n) {
                   // @a init
                   int count = 0;
                   while (n != 0) {
                       // @a clear
                       n = n & (n - 1);
                       // @a increment
                       count++;
                   }
                   // @a done
                   return count;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        int count = 0;

        emit.at("init")
                .say("Count the set bits of %d using Brian Kernighan's trick: N & (N - 1) "
                        + "clears the rightmost set bit each pass.", n)
                .var("N", n).var("count", count).bits(n).step();

        while (n != 0) {
            int before = n;
            n = n & (n - 1);
            emit.at("clear")
                    .say("N & (N - 1) = %d & %d = %d — the rightmost set bit is gone.",
                            before, before - 1, n)
                    .var("N", n).bits(n).step();

            count++;
            emit.at("increment")
                    .say("That was one set bit. count = %d.", count)
                    .var("count", count).bits(n).step();
        }

        emit.at("done")
                .say("N reached 0 — every set bit has been cleared. Total set bits = %d.", count)
                .var("totalSetBits", count).bits(n).step();
    }
}
