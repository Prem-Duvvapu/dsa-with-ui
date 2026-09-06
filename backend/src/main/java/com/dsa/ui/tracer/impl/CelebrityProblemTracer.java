package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Celebrity Problem. Given an N×N matrix where knows[i][j]==1 means person i knows
 * person j, find the celebrity: the person everyone knows but who knows nobody.
 *
 * <p>Two-pointer elimination: compare candidate A and B. If A knows B, A is not the
 * celebrity (eliminates A). If A does not know B, B is not the celebrity (eliminates B).
 * One candidate survives; verify it in a second pass.
 */
@Component
public class CelebrityProblemTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "celebrity-problem";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("matrix", FieldType.INT_GRID)
                        .label("Knows matrix")
                        .help("N×N matrix. knows[i][j]=1 means i knows j. Diagonal is 0.")
                        .constraint("minRows", 2).constraint("maxRows", 8)
                        .constraint("minCols", 2).constraint("maxCols", 8)
                        .constraint("minValue", 0).constraint("maxValue", 1)
                        .defaultValue(List.of(
                                List.of(0, 1, 0),
                                List.of(0, 0, 0),
                                List.of(0, 1, 0)))
                        .build());
    }

    /** No celebrity exists: person 0 and 1 know each other. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("matrix", List.of(
                List.of(0, 1, 0),
                List.of(1, 0, 0),
                List.of(1, 1, 0)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int findCelebrity(int[][] knows) {
                   int n = knows.length;
                   // @a init
                   int a = 0, b = n - 1;
                   while (a < b) {
                       if (knows[a][b] == 1) {
                           // @a elimA
                           a++;
                       } else {
                           // @a elimB
                           b--;
                       }
                   }
                   // @a verify
                   int candidate = a;
                   for (int i = 0; i < n; i++) {
                       if (i == candidate) continue;
                       if (knows[candidate][i] == 1 || knows[i][candidate] == 0) {
                           // @a noCeleb
                           return -1;
                       }
                   }
                   // @a found
                   return candidate;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] knows = in.getGrid("matrix");
        int n = knows.length;

        int a = 0, b = n - 1;
        emit.at("init")
                .say("Celebrity problem with %d people. Two-pointer elimination: a=%d, b=%d.", n, a, b)
                .var("n", n).var("a", a).var("b", b)
                .grid(knows).step();

        while (a < b) {
            if (knows[a][b] == 1) {
                emit.at("elimA")
                        .say("Person %d knows person %d — %d cannot be a celebrity. Advance a.",
                                a, b, a)
                        .var("a", a).var("b", b).var("eliminated", a)
                        .grid(knows).step();
                a++;
            } else {
                emit.at("elimB")
                        .say("Person %d does not know person %d — %d cannot be a celebrity. Retreat b.",
                                a, b, b)
                        .var("a", a).var("b", b).var("eliminated", b)
                        .grid(knows).step();
                b--;
            }
        }

        int candidate = a;
        emit.at("verify")
                .say("One candidate survives: person %d. Verify that everyone knows them and they know nobody.",
                        candidate)
                .var("candidate", candidate)
                .grid(knows).step();

        boolean valid = true;
        for (int i = 0; i < n; i++) {
            if (i == candidate) continue;
            if (knows[candidate][i] == 1 || knows[i][candidate] == 0) {
                emit.at("noCeleb")
                        .say("Verification failed at person %d: %s. No celebrity exists.",
                                i, knows[candidate][i] == 1
                                        ? "candidate knows them"
                                        : "they don't know the candidate")
                        .var("failedAt", i).var("answer", -1)
                        .grid(knows).step();
                valid = false;
                break;
            }
        }

        if (valid) {
            emit.at("found")
                    .say("Verification passed. Person %d is the celebrity — known by all, knows nobody.",
                            candidate)
                    .var("answer", candidate)
                    .grid(knows).step();
        }
    }
}
