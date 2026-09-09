package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Two counters shrink together instead of one: {@code remainingCount} tracks how many more
 * digits the combination still needs, {@code remainingSum} tracks how much they still need
 * to add up to. A leaf is only a valid answer when BOTH hit zero at once — running out of
 * digits with sum left over, or hitting the sum with digits left over, are both dead ends
 * that capture nothing.
 */
@Component
public class CombinationSum3Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "combination-sum-3";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("k", FieldType.INT)
                        .label("Digit count (K)")
                        .range(1, 9)
                        .defaultValue(3)
                        .build(),
                InputField.of("n", FieldType.INT)
                        .label("Target sum (N)")
                        .range(1, 45)
                        .defaultValue(7)
                        .build());
    }

    /** A larger target with the same digit count - three combinations instead of one. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("k", 3, "n", 9);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> combinationSum3(int k, int n) {
                   List<List<Integer>> res = new ArrayList<>();
                   backtrack(1, k, n, new ArrayList<>(), res);
                   // @a done
                   return res;
               }

               private void backtrack(int start, int remainingCount, int remainingSum,
                                       List<Integer> path, List<List<Integer>> res) {
                   if (remainingCount == 0) {
                       if (remainingSum == 0) {
                           // @a capture
                           res.add(new ArrayList<>(path));
                       }
                       // @a deadEnd
                       return;
                   }
                   for (int digit = start; digit <= 9; digit++) {
                       if (digit > remainingSum) {
                           // @a pruneRest
                           break;
                       }
                       path.add(digit);
                       // @a choose
                       backtrack(digit + 1, remainingCount - 1, remainingSum - digit, path, res);
                       path.remove(path.size() - 1);
                       // @a undo
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int k = in.getInt("k");
        int n = in.getInt("n");
        List<List<Integer>> res = new ArrayList<>();
        backtrack(1, k, n, new ArrayList<>(), res, emit);

        emit.at("done")
                .say("Search tree fully explored. %d combination%s of %d distinct digits summing to %d found.",
                        res.size(), res.size() == 1 ? "" : "s", k, n)
                .var("combinations", res.size()).stack(List.of()).step();
    }

    private void backtrack(int start, int remainingCount, int remainingSum, List<Integer> path,
                            List<List<Integer>> res, StepEmitter emit) {
        emit.push("backtrack(remainingCount=" + remainingCount + ",remainingSum=" + remainingSum + ")");

        if (remainingCount == 0) {
            if (remainingSum == 0) {
                res.add(new ArrayList<>(path));
                emit.at("capture")
                        .say("Digit count and sum both hit 0 together - capture %s as combination #%d.",
                                path, res.size())
                        .var("combination", path.toString()).var("count", res.size()).stack(path).step();
            } else {
                emit.at("deadEnd")
                        .say("Out of digits with %d still needed in the sum - dead end, nothing captured.",
                                remainingSum)
                        .var("remainingSum", remainingSum).stack(path).step();
            }
            emit.pop();
            return;
        }

        for (int digit = start; digit <= 9; digit++) {
            if (digit > remainingSum) {
                emit.at("pruneRest")
                        .say("Digit %d exceeds the remaining sum %d - every later digit is bigger still. Stop this loop.",
                                digit, remainingSum)
                        .var("digit", digit).var("remainingSum", remainingSum).stack(path).step();
                break;
            }

            path.add(digit);
            emit.at("choose")
                    .say("Choose digit %d - %d digit(s) and sum %d left. Recurse from digit %d.",
                            digit, remainingCount - 1, remainingSum - digit, digit + 1)
                    .var("digit", digit).var("remainingCount", remainingCount - 1)
                    .var("remainingSum", remainingSum - digit).stack(path).step();
            backtrack(digit + 1, remainingCount - 1, remainingSum - digit, path, res, emit);

            path.remove(path.size() - 1);
            emit.at("undo")
                    .say("Undo choosing digit %d - try the next larger digit.", digit)
                    .var("digit", digit).stack(path).step();
        }

        emit.pop();
    }
}
