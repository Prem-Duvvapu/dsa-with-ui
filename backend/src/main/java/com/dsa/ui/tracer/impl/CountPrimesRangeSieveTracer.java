package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Sieve of Eratosthenes: walk i upward, and whenever i has not already been marked
 * composite by a smaller prime, it IS prime — count it and mark every multiple of it
 * starting from i*i (every smaller multiple already has a smaller prime factor that marked
 * it first). Starting each inner sweep at i*i, not 2i, is what keeps the sieve
 * O(n log log n) instead of O(n log n).
 */
@Component
public class CountPrimesRangeSieveTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-primes-range-sieve";
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
                        .help("Count primes strictly less than N.")
                        .range(0, 200)
                        .defaultValue(10)
                        .build());
    }

    /** N = 1: the loop range [2, n) is empty, so nothing is ever marked or counted. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public int countPrimes(int n) {
                   boolean[] composite = new boolean[Math.max(n, 1)];
                   int count = 0;
                   for (int i = 2; i < n; i++) {
                       if (!composite[i]) {
                           // @a primeFound
                           count++;
                           for (int j = i * i; j < n; j += i) {
                               // @a markComposite
                               composite[j] = true;
                           }
                       }
                   }
                   // @a done
                   return count;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        boolean[] composite = new boolean[Math.max(n, 1)];
        int count = 0;

        for (int i = 2; i < n; i++) {
            if (!composite[i]) {
                count++;
                emit.at("primeFound")
                        .say("%d is not yet marked composite by any smaller prime — it IS prime. count = %d.",
                                i, count)
                        .var("i", i).var("count", count).arrayState(sieveState(composite)).step();

                for (int j = i * i; j < n; j += i) {
                    composite[j] = true;
                    emit.at("markComposite")
                            .say("Mark %d = %d * %d as composite.", j, i, j / i)
                            .var("marked", j).arrayState(sieveState(composite)).step();
                }
            }
        }

        emit.at("done")
                .say("Every candidate below %d checked. Primes strictly less than %d = %d.", n, n, count)
                .var("answer", count).arrayState(sieveState(composite)).step();
    }

    private List<ArrayElement> sieveState(boolean[] composite) {
        List<ArrayElement> state = new ArrayList<>(composite.length);
        for (int i = 0; i < composite.length; i++) {
            state.add(new ArrayElement(i, composite[i] ? 1 : 0, composite[i] ? "default" : "current"));
        }
        return state;
    }
}
