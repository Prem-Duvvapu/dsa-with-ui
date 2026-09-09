package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * A bit flip turns start into goal exactly where the two disagree, so XOR marks every
 * position that needs flipping in one pass; counting THAT value's set bits (Brian
 * Kernighan's {@code x & (x - 1)}, the same clearing identity {@link CountSetBitsTracer}
 * uses standalone) is the answer. The XOR step is what tells the two tracers apart —
 * {@code count-set-bits} counts one number's own bits, this counts how two numbers differ.
 */
@Component
public class MinBitFlipsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "min-bit-flips";
    }

    @Override
    public DsType dsType() {
        return DsType.BITS;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("start", FieldType.INT)
                        .label("Start")
                        .help("Value to convert from.")
                        .range(0, 1_000_000_000)
                        .defaultValue(10)
                        .build(),
                InputField.of("goal", FieldType.INT)
                        .label("Goal")
                        .help("Value to convert to.")
                        .range(0, 1_000_000_000)
                        .defaultValue(7)
                        .build());
    }

    /** start == goal: XOR is 0, the clearing loop never runs — zero flips needed. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("start", 10, "goal", 10);
    }

    @Override
    public String annotatedCode() {
        return """
               public int minBitFlips(int start, int goal) {
                   // @a xor
                   int diff = start ^ goal;
                   int flips = 0;
                   while (diff != 0) {
                       // @a clearBit
                       diff = diff & (diff - 1);
                       // @a tally
                       flips++;
                   }
                   // @a done
                   return flips;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int start = in.getInt("start");
        int goal = in.getInt("goal");

        int diff = start ^ goal;
        emit.at("xor")
                .say("start ^ goal = %d ^ %d = %d — a 1 exactly where start and goal disagree; "
                        + "flipping those bits turns start into goal.", start, goal, diff)
                .var("start", start).var("goal", goal).var("diff", diff).bits(diff).step();

        int flips = 0;
        while (diff != 0) {
            int before = diff;
            diff = diff & (diff - 1);
            emit.at("clearBit")
                    .say("diff & (diff - 1) = %d & %d = %d — one disagreeing bit accounted for.",
                            before, before - 1, diff)
                    .var("diff", diff).bits(diff).step();

            flips++;
            emit.at("tally")
                    .say("flips = %d.", flips)
                    .var("flips", flips).bits(diff).step();
        }

        emit.at("done")
                .say("Every disagreeing bit found. Minimum flips to turn %d into %d = %d.", start, goal, flips)
                .var("answer", flips).bits(diff).step();
    }
}
