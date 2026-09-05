package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pick-with-reuse: taking a candidate does not advance the index, since it may be reused
 * as many times as needed. Skipping it does advance the index, so every candidate is
 * eventually left behind for good. A path only becomes a result at the very end, and only
 * if the running target hit exactly zero - overshooting is pruned before it can recurse.
 */
@Component
public class CombinationSumTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "combination-sum-i";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("candidates", FieldType.INT_ARRAY)
                        .label("Candidates (distinct, positive)")
                        .length(1, 5).values(1, 9).distinct()
                        .defaultValue(List.of(2, 3, 6, 7))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target sum")
                        .range(1, 30)
                        .defaultValue(7)
                        .build());
    }

    /** A different candidate set and target - three combinations instead of two, one four deep. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("candidates", List.of(2, 3, 5), "target", 8);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> combinationSum(int[] candidates, int target) {
                   List<List<Integer>> res = new ArrayList<>();
                   find(0, candidates, target, new ArrayList<>(), res);
                   // @a done
                   return res;
               }

               private void find(int idx, int[] candidates, int target,
                                  List<Integer> current, List<List<Integer>> res) {
                   if (idx == candidates.length) {
                       if (target == 0) {
                           // @a match
                           res.add(new ArrayList<>(current));
                       }
                       return;
                   }
                   if (candidates[idx] <= target) {
                       current.add(candidates[idx]);
                       // @a pick
                       find(idx, candidates, target - candidates[idx], current, res);
                       current.remove(current.size() - 1);
                       // @a backtrack
                   }
                   // @a skip
                   find(idx + 1, candidates, target, current, res);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] candidates = in.getIntArray("candidates");
        int target = in.getInt("target");
        List<List<Integer>> res = new ArrayList<>();
        List<Integer> current = new ArrayList<>();

        find(0, candidates, target, current, res, emit);

        emit.at("done")
                .say("Every candidate exhausted at every branch. %d combination%s found summing to %d.",
                        res.size(), res.size() == 1 ? "" : "s", target)
                .var("combinations", res.size()).stack(List.of()).step();
    }

    private void find(int idx, int[] candidates, int target, List<Integer> current,
                       List<List<Integer>> res, StepEmitter emit) {
        emit.push("find(idx=" + idx + ", target=" + target + ")");

        if (idx == candidates.length) {
            if (target == 0) {
                res.add(new ArrayList<>(current));
                emit.at("match")
                        .say("Every candidate considered and the running target hit exactly 0 - %s is combination #%d.",
                                current, res.size())
                        .var("combination", current.toString()).var("count", res.size())
                        .stack(current).step();
            }
            emit.pop();
            return;
        }

        if (candidates[idx] <= target) {
            current.add(candidates[idx]);
            emit.at("pick")
                    .say("Take %d again (reuse allowed) - remaining target %d -> %d. Path: %s.",
                            candidates[idx], target, target - candidates[idx], current)
                    .var("picked", candidates[idx]).var("remaining", target - candidates[idx])
                    .stack(current).step();
            find(idx, candidates, target - candidates[idx], current, res, emit);

            current.remove(current.size() - 1);
            emit.at("backtrack")
                    .say("Undo the last %d - restore the path to try leaving this candidate out. Path: %s.",
                            candidates[idx], current)
                    .var("undone", candidates[idx]).stack(current).step();
        }

        emit.at("skip")
                .say("%d is done for good at this branch (%s) - move to the next candidate.",
                        candidates[idx],
                        candidates[idx] > target ? "exceeds the remaining target " + target
                                : "every reuse of it here has been tried")
                .var("index", idx + 1).stack(current).step();
        find(idx + 1, candidates, target, current, res, emit);

        emit.pop();
    }
}
