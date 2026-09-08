package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Replaces every value with its one-based rank among the array's distinct values. */
@Component
public class ReplaceRankArrayTracer implements AlgorithmTracer {
    @Override public String id() { return "replace-rank-array"; }
    @Override public DsType dsType() { return DsType.ARRAY; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("values", FieldType.INT_ARRAY).label("Values")
                .length(1, 60).values(-1000, 1000)
                .defaultValue(List.of(20, 15, 26, 2, 98, 6, 2)).build());
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of("values", List.of(40, 10, 20, 30, 20, 10, 50, 40));
    }

    @Override public String annotatedCode() {
        return """
               public int[] replaceWithRank(int[] values) {
                   // @a sort
                   int[] ordered = values.clone();
                   Arrays.sort(ordered);
                   Map<Integer, Integer> rank = new HashMap<>();
                   for (int value : ordered) {
                       if (!rank.containsKey(value)) {
                           // @a rank
                           rank.put(value, rank.size() + 1);
                       }
                   }
                   for (int i = 0; i < values.length; i++) {
                       // @a apply
                       values[i] = rank.get(values[i]);
                   }
                   // @a done
                   return values;
               }""";
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[] values = in.getIntArray("values");
        int[] ordered = values.clone();
        Arrays.sort(ordered);
        emit.at("sort").say("Sort a copy so distinct values appear in rank order: %s.", Arrays.toString(ordered))
                .var("ordered", Arrays.toString(ordered)).array(ordered).step();

        Map<Integer, Integer> ranks = new LinkedHashMap<>();
        for (int value : ordered) {
            if (!ranks.containsKey(value)) {
                int rank = ranks.size() + 1;
                ranks.put(value, rank);
                emit.at("rank").say("First occurrence of %d gets rank %d.", value, rank)
                        .var("value", value).var("rank", rank).array(ordered).step();
            }
        }

        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = ranks.get(values[i]);
            emit.at("apply").say("Replace values[%d] = %d with rank %d.", i, values[i], result[i])
                    .var("index", i).var("rank", result[i])
                    .arrayState(render(values, result, i)).step();
        }
        emit.at("done").say("Rank replacement complete: %s.", Arrays.toString(result))
                .var("result", Arrays.toString(result)).array(result).step();
    }

    private static List<ArrayElement> render(int[] values, int[] ranks, int through) {
        List<ArrayElement> out = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            String label = i <= through ? values[i] + "→" + ranks[i] : String.valueOf(values[i]);
            out.add(new ArrayElement(i, i <= through ? ranks[i] : values[i],
                    i == through ? "current" : "default", label));
        }
        return out;
    }
}
