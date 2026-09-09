package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Divisors come in pairs {@code (i, n/i)} that straddle sqrt(n), so testing candidates only
 * up to sqrt(n) still finds every divisor — the larger half of each pair is read off, not
 * searched for. The one case that is not a pair is {@code i == n/i} itself, at the square
 * root exactly, which must be recorded once rather than twice.
 */
@Component
public class DivisorsOfNumberTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "divisors-of-number";
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
                        .help("Positive integer to find the divisors of.")
                        .range(1, 1_000_000)
                        .defaultValue(36)
                        .build());
    }

    /** N = 1: the only candidate IS n/i, so i != n/i is false and the pair half is correctly skipped. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> divisors(int n) {
                   List<Integer> divs = new ArrayList<>();
                   for (int i = 1; (long) i * i <= n; i++) {
                       // @a check
                       if (n % i == 0) {
                           // @a recordLow
                           divs.add(i);
                           if (i != n / i) {
                               // @a recordHigh
                               divs.add(n / i);
                           }
                       }
                   }
                   Collections.sort(divs);
                   // @a done
                   return divs;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        List<Integer> divs = new ArrayList<>();

        for (int i = 1; (long) i * i <= n; i++) {
            emit.at("check")
                    .say("Candidate i = %d (i*i = %d <= %d). Does %d divide %d evenly?", i, i * i, n, i, n)
                    .var("i", i).arrayState(sortedState(divs)).step();

            if (n % i == 0) {
                divs.add(i);
                emit.at("recordLow")
                        .say("Yes — %d is a divisor.", i)
                        .var("divisors", sortedCopy(divs).toString()).arrayState(sortedState(divs)).step();

                int paired = n / i;
                if (i != paired) {
                    divs.add(paired);
                    emit.at("recordHigh")
                            .say("Its pair %d / %d = %d is a divisor too.", n, i, paired)
                            .var("divisors", sortedCopy(divs).toString()).arrayState(sortedState(divs)).step();
                }
            }
        }

        Collections.sort(divs);
        emit.at("done")
                .say("Every divisor of %d, in order: %s.", n, divs)
                .var("divisors", divs.toString()).arrayState(sortedState(divs)).step();
    }

    private List<Integer> sortedCopy(List<Integer> divs) {
        List<Integer> copy = new ArrayList<>(divs);
        Collections.sort(copy);
        return copy;
    }

    private List<ArrayElement> sortedState(List<Integer> divs) {
        List<Integer> sorted = sortedCopy(divs);
        List<ArrayElement> state = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            state.add(new ArrayElement(i, sorted.get(i), "current"));
        }
        return state;
    }
}
