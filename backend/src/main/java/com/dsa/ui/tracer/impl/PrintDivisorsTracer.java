package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Walks {@code i} from 1 up to {@code sqrt(n)}; every {@code i} that divides {@code n}
 * contributes both {@code i} and its pair {@code n / i}, unless they coincide.
 */
@Component
public class PrintDivisorsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "print-divisors";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("n", FieldType.INT)
                        .label("N")
                        .help("A positive whole number.")
                        .range(1, 1_000_000)
                        .defaultValue(36)
                        .build());
    }

    /** A prime — only 1 and n itself divide it, so the pair never grows past two entries. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 17);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> getDivisors(int n) {
                   // @a init
                   List<Integer> divisors = new ArrayList<>();
                   for (int i = 1; i * i <= n; i++) {
                       if (n % i == 0) {
                           // @a found
                           divisors.add(i);
                           if ((n / i) != i) {
                               divisors.add(n / i);
                           }
                       }
                   }
                   // @a done
                   Collections.sort(divisors);
                   return divisors;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        List<Integer> divisors = new ArrayList<>();

        emit.at("init")
                .say("Search for divisors of %d for i from 1 up to sqrt(%d) ~= %d.",
                        n, n, (int) Math.sqrt(n))
                .var("n", n)
                .array(new int[]{n}).step();

        for (int i = 1; (long) i * i <= n; i++) {
            if (n % i == 0) {
                divisors.add(i);
                boolean paired = (n / i) != i;
                if (paired) {
                    divisors.add(n / i);
                }
                emit.at("found")
                        .say("i=%d divides %d%s. Divisors so far: %s.",
                                i, n, paired ? " (paired with " + (n / i) + ")" : "", divisors)
                        .var("i", i).var("divisors", divisors.toString())
                        .array(toArray(divisors)).step();
            }
        }

        Collections.sort(divisors);
        emit.at("done")
                .say("All divisors of %d found and sorted: %s.", n, divisors)
                .var("divisors", divisors.toString())
                .array(toArray(divisors)).step();
    }

    private static int[] toArray(List<Integer> values) {
        int[] out = new int[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }
}
