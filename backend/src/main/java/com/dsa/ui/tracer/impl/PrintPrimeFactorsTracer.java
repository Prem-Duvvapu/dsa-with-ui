package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Trial division only needs candidates up to sqrt(remaining N): any factor larger than that
 * must be paired with one smaller than it, which trial division would already have found.
 * Dividing out every occurrence of each candidate (not just testing it once) is what makes
 * the result a full factorisation with multiplicity, not just a primality test.
 */
@Component
public class PrintPrimeFactorsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "print-prime-factors";
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
                        .help("Integer to factorise, at least 2.")
                        .range(2, 1_000_000)
                        .defaultValue(60)
                        .build());
    }

    /** 17 is prime: trial division finds nothing up to its own sqrt, so the leftover-N step supplies the only factor. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 17);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> primeFactors(int n) {
                   List<Integer> factors = new ArrayList<>();
                   int remaining = n;
                   for (int i = 2; (long) i * i <= remaining; i++) {
                       // @a candidate
                       while (remaining % i == 0) {
                           // @a divide
                           factors.add(i);
                           remaining /= i;
                       }
                   }
                   if (remaining > 1) {
                       // @a leftover
                       factors.add(remaining);
                   }
                   // @a done
                   return factors;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        List<Integer> factors = new ArrayList<>();
        int remaining = n;

        for (int i = 2; (long) i * i <= remaining; i++) {
            emit.at("candidate")
                    .say("Candidate i = %d (i*i = %d <= remaining %d). Divide out every occurrence.",
                            i, i * i, remaining)
                    .var("i", i).var("remaining", remaining)
                    .arrayState(factorState(factors)).step();

            while (remaining % i == 0) {
                factors.add(i);
                remaining /= i;
                emit.at("divide")
                        .say("%d divides evenly. factors = %s, remaining = %d.", i, factors, remaining)
                        .var("factors", factors.toString()).var("remaining", remaining)
                        .arrayState(factorState(factors)).step();
            }
        }

        if (remaining > 1) {
            factors.add(remaining);
            emit.at("leftover")
                    .say("Remaining %d has no factor up to its own sqrt, so it is prime itself. factors = %s.",
                            remaining, factors)
                    .var("factors", factors.toString())
                    .arrayState(factorState(factors)).step();
        }

        emit.at("done")
                .say("Prime factorisation of %d = %s.", n, factors)
                .var("factors", factors.toString())
                .arrayState(factorState(factors)).step();
    }

    private List<ArrayElement> factorState(List<Integer> factors) {
        List<ArrayElement> state = new ArrayList<>(factors.size());
        for (int i = 0; i < factors.size(); i++) {
            state.add(new ArrayElement(i, factors.get(i), "current"));
        }
        return state;
    }
}
