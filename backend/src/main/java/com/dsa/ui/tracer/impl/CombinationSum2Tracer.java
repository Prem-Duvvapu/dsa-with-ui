package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Sorting first turns "no duplicate combinations" into a one-line check: at any recursion
 * depth, skip a candidate that equals the one immediately before it AT THAT SAME DEPTH
 * (never across depths — the array itself may repeat a value, and each copy is still usable
 * once). Sorting also lets a candidate bigger than what's left prune the rest of the loop
 * outright, since everything after it is only bigger still.
 */
@Component
public class CombinationSum2Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "combination-sum-2";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("candidates", FieldType.INT_ARRAY)
                        .label("Candidates (may repeat)")
                        .help("Each element may be used at most once per combination.")
                        .length(1, 10).values(1, 8)
                        .defaultValue(List.of(2, 5, 2, 1, 2, 7))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target sum")
                        .range(1, 100)
                        .defaultValue(8)
                        .build());
    }

    /** Three duplicate 2s in the pool - the same-depth skip rule does real work here. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("candidates", List.of(2, 5, 2, 1, 2), "target", 5);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> combinationSum2(int[] candidates, int target) {
                   Arrays.sort(candidates);
                   List<List<Integer>> res = new ArrayList<>();
                   backtrack(0, candidates, target, new ArrayList<>(), res);
                   // @a done
                   return res;
               }

               private void backtrack(int start, int[] c, int remaining, List<Integer> path, List<List<Integer>> res) {
                   if (remaining == 0) {
                       // @a capture
                       res.add(new ArrayList<>(path));
                       return;
                   }
                   for (int i = start; i < c.length; i++) {
                       if (i > start && c[i] == c[i - 1]) {
                           // @a skipDuplicate
                           continue;
                       }
                       if (c[i] > remaining) {
                           // @a pruneRest
                           break;
                       }
                       path.add(c[i]);
                       // @a choose
                       backtrack(i + 1, c, remaining - c[i], path, res);
                       path.remove(path.size() - 1);
                       // @a undo
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] candidates = in.getIntArray("candidates");
        int target = in.getInt("target");
        Arrays.sort(candidates);

        List<List<Integer>> res = new ArrayList<>();
        backtrack(0, candidates, target, new ArrayList<>(), res, emit);

        emit.at("done")
                .say("Search tree fully explored. %d unique combination%s of %s summing to %d found.",
                        res.size(), res.size() == 1 ? "" : "s", Arrays.toString(candidates), target)
                .var("combinations", res.size()).stack(List.of()).step();
    }

    private void backtrack(int start, int[] c, int remaining, List<Integer> path, List<List<Integer>> res,
                            StepEmitter emit) {
        emit.push("backtrack(start=" + start + ",remaining=" + remaining + ")");

        if (remaining == 0) {
            res.add(new ArrayList<>(path));
            emit.at("capture")
                    .say("Remaining reached exactly 0 - capture %s as combination #%d.", path, res.size())
                    .var("combination", path.toString()).var("count", res.size()).stack(path).step();
            emit.pop();
            return;
        }

        for (int i = start; i < c.length; i++) {
            if (i > start && c[i] == c[i - 1]) {
                emit.at("skipDuplicate")
                        .say("c[%d]=%d equals the previous candidate already tried at this depth - skip to avoid a duplicate combination.",
                                i, c[i])
                        .var("i", i).var("skipped", c[i]).stack(path).step();
                continue;
            }
            if (c[i] > remaining) {
                emit.at("pruneRest")
                        .say("c[%d]=%d exceeds the remaining %d - since the array is sorted, every later candidate is bigger still. Stop this loop.",
                                i, c[i], remaining)
                        .var("i", i).var("candidate", c[i]).stack(path).step();
                break;
            }

            path.add(c[i]);
            emit.at("choose")
                    .say("Choose c[%d]=%d - remaining drops from %d to %d. Recurse from index %d.",
                            i, c[i], remaining, remaining - c[i], i + 1)
                    .var("i", i).var("remaining", remaining - c[i]).stack(path).step();
            backtrack(i + 1, c, remaining - c[i], path, res, emit);

            path.remove(path.size() - 1);
            emit.at("undo")
                    .say("Undo choosing c[%d]=%d - try the next distinct candidate at this depth.", i, c[i])
                    .var("i", i).stack(path).step();
        }

        emit.pop();
    }
}
