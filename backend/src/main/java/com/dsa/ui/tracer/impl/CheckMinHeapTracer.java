package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Checks every parent-child edge instead of stopping after the first min-heap violation. */
@Component
public class CheckMinHeapTracer implements AlgorithmTracer {
    @Override public String id() { return "check-min-heap"; }
    @Override public DsType dsType() { return DsType.TREE; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("values", FieldType.INT_ARRAY).label("Heap array")
                .help("Index i has children 2i+1 and 2i+2 when those indices exist.")
                .length(1, 63).values(-1000, 1000)
                .defaultValue(List.of(1, 3, 5, 7, 9, 8, 10)).build());
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of("values", List.of(1, 4, 2, 8, 3, 6, 7));
    }

    @Override public String annotatedCode() {
        return """
               public boolean isMinHeap(int[] values) {
                   // @a start
                   boolean valid = true;
                   for (int parent = 0; parent < values.length / 2; parent++) {
                       int[] children = {2 * parent + 1, 2 * parent + 2};
                       for (int child : children) {
                           if (child >= values.length) continue;
                           // @a compare
                           if (values[parent] > values[child]) {
                               // @a violation
                               valid = false;
                           } else {
                               // @a valid
                               valid = valid;
                           }
                       }
                   }
                   // @a done
                   return valid;
               }""";
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[] values = in.getIntArray("values");
        boolean valid = true;
        int violations = 0;
        emit.at("start").say("A min-heap requires parent <= child on every tree edge.")
                .var("valid", true).tree(render(values, -1, -1)).step();

        for (int parent = 0; parent < values.length / 2; parent++) {
            int[] children = {2 * parent + 1, 2 * parent + 2};
            for (int child : children) {
                if (child >= values.length) continue;
                emit.at("compare").say("Compare parent %d at index %d with child %d at index %d.",
                                values[parent], parent, values[child], child)
                        .var("parent", parent).var("child", child)
                        .tree(render(values, parent, child)).step();
                if (values[parent] > values[child]) {
                    valid = false;
                    violations++;
                    emit.at("violation").say("Violation: %d > %d. This array is not a min-heap.",
                                    values[parent], values[child])
                            .var("violations", violations).var("valid", false)
                            .tree(render(values, parent, child)).step();
                } else {
                    emit.at("valid").say("Edge satisfies min-heap order: %d <= %d.",
                                    values[parent], values[child])
                            .var("violations", violations).var("valid", valid)
                            .tree(render(values, parent, child)).step();
                }
            }
        }
        emit.at("done").say(valid
                        ? "Every parent-child edge is ordered, so this is a min-heap."
                        : "Found %d violating edge(s), so this is not a min-heap.",
                        valid ? new Object[]{} : new Object[]{violations})
                // Preserve the same variable insertion order as the preceding comparison
                // steps so delta decoding round-trips to an identical LinkedHashMap.
                .var("violations", violations).var("valid", valid)
                .tree(render(values, 0, -1)).step();
    }

    private static List<TreeNode> render(int[] values, int current, int target) {
        Integer[] boxed = Arrays.stream(values).boxed().toArray(Integer[]::new);
        Map<Integer, String> states = new LinkedHashMap<>();
        if (target >= 0) states.put(target, "target");
        if (current >= 0) states.put(current, "current");
        return new BinaryTreeLayout(boxed).render(states);
    }
}
