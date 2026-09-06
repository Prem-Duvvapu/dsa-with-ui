package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Functional recursion {@code sumN(n) = n + sumN(n - 1)}: nothing is added until the base
 * case returns and the call stack starts unwinding.
 */
@Component
public class SumFirstNTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "sum-first-n";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("n", FieldType.INT)
                        .label("N")
                        .help("How many numbers to sum, starting from n down to 1.")
                        .range(0, 500)
                        .defaultValue(5)
                        .build());
    }

    /** A longer chain and a different total. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 8);
    }

    @Override
    public String annotatedCode() {
        return """
               public int sumN(int n) {
                   if (n == 0) {
                       // @a base
                       return 0;
                   }
                   // @a call
                   return n + sumN(n - 1);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        recurse(n, new ArrayList<>(), emit);
    }

    private int recurse(int n, List<String> frames, StepEmitter emit) {
        frames.add("sumN(" + n + ")");
        emit.push("sumN(" + n + ")");

        int result;
        if (n == 0) {
            emit.at("base")
                    .say("sumN(0) is the base case — returns 0, nothing left to add.")
                    .var("n", 0).var("returns", 0)
                    .stack(frames).step();
            result = 0;
        } else {
            emit.at("call")
                    .say("sumN(%d) must call sumN(%d) before it can add %d to the total.", n, n - 1, n)
                    .var("n", n)
                    .stack(frames).step();

            int inner = recurse(n - 1, frames, emit);
            result = n + inner;

            emit.at("call")
                    .say("sumN(%d) received %d back from sumN(%d) — returns %d + %d = %d.",
                            n, inner, n - 1, n, inner, result)
                    .var("n", n).var("inner", inner).var("result", result)
                    .stack(frames).step();
        }

        frames.remove(frames.size() - 1);
        emit.pop();
        return result;
    }
}
