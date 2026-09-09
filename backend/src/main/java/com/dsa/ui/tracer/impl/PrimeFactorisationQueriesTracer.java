package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A smallest-prime-factor (SPF) sieve, built once up to a fixed bound, turns repeated
 * factorisation into repeated lookup: spf[x] is x's smallest prime factor, so dividing x by
 * it and looking up again peels off one prime per step with no trial division at query time.
 * The sieve build itself is not narrated — it is preparation, not the algorithm being taught —
 * only the lookup loop is.
 */
@Component
public class PrimeFactorisationQueriesTracer implements AlgorithmTracer {

    private static final int MAX = 100_000;

    @Override
    public String id() {
        return "prime-factorisation-queries";
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
                        .help("Integer to factorise using a precomputed smallest-prime-factor table.")
                        .range(1, MAX)
                        .defaultValue(30)
                        .build());
    }

    /** N = 1: x > 1 is false immediately, so the lookup loop never runs — zero factors. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> primeFactorsViaSieve(int n) {
                   int[] spf = buildSmallestPrimeFactorSieve(MAX);
                   // @a init
                   List<Integer> factors = new ArrayList<>();
                   int x = n;
                   while (x > 1) {
                       int factor = spf[x];
                       // @a lookup
                       factors.add(factor);
                       x /= factor;
                   }
                   // @a done
                   return factors;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        int[] spf = buildSmallestPrimeFactorSieve(MAX);

        List<Integer> factors = new ArrayList<>();
        emit.at("init")
                .say("Smallest-prime-factor table built once for every value up to %d. Look up spf[%d] repeatedly.",
                        MAX, n)
                .var("n", n).arrayState(factorState(factors)).step();

        int x = n;
        while (x > 1) {
            int factor = spf[x];
            factors.add(factor);
            emit.at("lookup")
                    .say("spf[%d] = %d. Divide it out: x = %d / %d = %d.", x, factor, x, factor, x / factor)
                    .var("x", x).var("factor", factor).var("factors", factors.toString())
                    .arrayState(factorState(factors)).step();
            x /= factor;
        }

        emit.at("done")
                .say("x reached 1. Prime factorisation of %d = %s.", n, factors)
                .var("factors", factors.toString()).arrayState(factorState(factors)).step();
    }

    private int[] buildSmallestPrimeFactorSieve(int max) {
        int[] spf = new int[max + 1];
        for (int i = 2; i <= max; i++) {
            if (spf[i] == 0) {
                for (int j = i; j <= max; j += i) {
                    if (spf[j] == 0) {
                        spf[j] = i;
                    }
                }
            }
        }
        return spf;
    }

    private List<ArrayElement> factorState(List<Integer> factors) {
        List<ArrayElement> state = new ArrayList<>(factors.size());
        for (int i = 0; i < factors.size(); i++) {
            state.add(new ArrayElement(i, factors.get(i), "current"));
        }
        return state;
    }
}
