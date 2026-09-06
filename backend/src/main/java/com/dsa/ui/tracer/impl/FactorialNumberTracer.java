package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Functional recursion {@code factorial(n) = n * factorial(n - 1)}: the multiplication
 * only happens once the base case returns and the stack starts unwinding.
 */
@Component
public class FactorialNumberTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "factorial-number";
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
                        .help("12! is the largest value that still fits a 32-bit int.")
                        .range(0, 12)
                        .defaultValue(5)
                        .build());
    }

    /** A longer chain and a much larger product. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 7);
    }

    @Override
    public String annotatedCode() {
        return """
               public int factorial(int n) {
                   if (n == 0 || n == 1) {
                       // @a base
                       return 1;
                   }
                   // @a call
                   return n * factorial(n - 1);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        recurse(n, new ArrayList<>(), emit);
    }

    private int recurse(int n, List<String> frames, StepEmitter emit) {
        frames.add("factorial(" + n + ")");
        emit.push("factorial(" + n + ")");

        int result;
        if (n == 0 || n == 1) {
            emit.at("base")
                    .say("factorial(%d) is the base case — returns 1.", n)
                    .var("n", n).var("returns", 1)
                    .stack(frames).step();
            result = 1;
        } else {
            emit.at("call")
                    .say("factorial(%d) must call factorial(%d) before it can multiply by %d.", n, n - 1, n)
                    .var("n", n)
                    .stack(frames).step();

            int inner = recurse(n - 1, frames, emit);
            result = n * inner;

            emit.at("call")
                    .say("factorial(%d) received %d back from factorial(%d) — returns %d * %d = %d.",
                            n, inner, n - 1, n, inner, result)
                    .var("n", n).var("inner", inner).var("result", result)
                    .stack(frames).step();
        }

        frames.remove(frames.size() - 1);
        emit.pop();
        return result;
    }
}
