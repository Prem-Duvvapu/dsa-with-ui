package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Counts factors up to {@code sqrt(n)}, doubling the count for a divisor pair. Exactly
 * two factors total (1 and n itself) means n is prime.
 */
@Component
public class CheckPrimeTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "check-prime";
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
                        .help("An integer greater than 1.")
                        .range(2, 1_000_000)
                        .defaultValue(29)
                        .build());
    }

    /** A composite with several factor pairs, so the count settles well above 2. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 100);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isPrime(int n) {
                   if (n <= 1) return false;
                   // @a init
                   int count = 0;
                   for (int i = 1; i * i <= n; i++) {
                       if (n % i == 0) {
                           // @a factor
                           count++;
                           if ((n / i) != i) count++;
                       }
                   }
                   // @a check
                   return count == 2;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        int count = 0;

        emit.at("init")
                .say("Count factors of %d for i from 1 up to sqrt(%d) ~= %d.", n, n, (int) Math.sqrt(n))
                .var("n", n).var("count", count)
                .array(new int[]{n}).step();

        for (int i = 1; (long) i * i <= n; i++) {
            if (n % i == 0) {
                count++;
                boolean paired = (n / i) != i;
                if (paired) {
                    count++;
                }
                emit.at("factor")
                        .say("i=%d divides %d%s. Factor count is now %d.",
                                i, n, paired ? " (paired with " + (n / i) + ")" : "", count)
                        .var("i", i).var("count", count)
                        .array(new int[]{n, i}).step();
            }
        }

        boolean prime = count == 2;
        emit.at("check")
                .say("Factor count = %d -> %s.", count, prime ? "PRIME (exactly 1 and n)" : "NOT PRIME")
                .var("result", prime ? "PRIME" : "NOT PRIME")
                .array(new int[]{n}).step();
    }
}
