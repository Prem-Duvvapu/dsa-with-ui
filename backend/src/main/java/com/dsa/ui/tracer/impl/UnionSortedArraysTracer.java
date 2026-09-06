package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Union of two sorted arrays using a two-pointer merge scan.
 *
 * <p>Both arrays are already sorted. Two pointers advance through them,
 * always picking the smaller element and skipping duplicates already in
 * the union list. Once one array is exhausted, the remainder of the other
 * is drained — exactly like the merge step of merge sort, but with
 * duplicate suppression.
 */
@Component
public class UnionSortedArraysTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "union-sorted-arrays";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("a", FieldType.INT_ARRAY)
                        .label("Array A (sorted)")
                        .help("A sorted array of integers.")
                        .length(1, 20).values(-100, 100).sorted()
                        .defaultValue(List.of(1, 2, 3, 4, 6))
                        .build(),
                InputField.of("b", FieldType.INT_ARRAY)
                        .label("Array B (sorted)")
                        .help("A sorted array of integers.")
                        .length(1, 20).values(-100, 100).sorted()
                        .defaultValue(List.of(2, 3, 5))
                        .build());
    }

    /**
     * Completely disjoint arrays with different lengths, so only the
     * drain-remainder branches fire and no duplicate is ever skipped.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "a", List.of(1, 2, 3),
                "b", List.of(2, 4, 5, 6));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> findUnion(int[] a, int[] b) {
                   // @a init
                   List<Integer> union = new ArrayList<>();
                   int i = 0, j = 0;
                   while (i < a.length && j < b.length) {
                       if (a[i] <= b[j]) {
                           // @a pickA
                           if (union.isEmpty() || union.get(union.size() - 1) != a[i])
                               union.add(a[i]);
                           i++;
                       } else {
                           // @a pickB
                           if (union.isEmpty() || union.get(union.size() - 1) != b[j])
                               union.add(b[j]);
                           j++;
                       }
                   }
                   while (i < a.length) {
                       // @a drainA
                       if (union.isEmpty() || union.get(union.size() - 1) != a[i])
                           union.add(a[i]);
                       i++;
                   }
                   while (j < b.length) {
                       // @a drainB
                       if (union.isEmpty() || union.get(union.size() - 1) != b[j])
                           union.add(b[j]);
                       j++;
                   }
                   // @a done
                   return union;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] a = in.getIntArray("a");
        int[] b = in.getIntArray("b");
        List<Integer> union = new ArrayList<>();
        int i = 0, j = 0;

        // Combine both arrays for visualization: a followed by b
        int[] combined = new int[a.length + b.length];
        System.arraycopy(a, 0, combined, 0, a.length);
        System.arraycopy(b, 0, combined, a.length, b.length);

        emit.at("init")
                .say("Two sorted arrays: A = %s, B = %s. Two pointers i=0, j=0 walk them simultaneously.",
                        java.util.Arrays.toString(a), java.util.Arrays.toString(b))
                .var("i", 0).var("j", 0).var("union", "[]")
                .array(combined, 0, a.length)
                .step();

        while (i < a.length && j < b.length) {
            if (a[i] <= b[j]) {
                boolean added = union.isEmpty() || union.get(union.size() - 1) != a[i];
                if (added) union.add(a[i]);
                emit.at("pickA")
                        .say("a[%d]=%d <= b[%d]=%d, so pick from A.%s Advance i.",
                                i, a[i], j, b[j],
                                added ? " Added " + a[i] + " to union." : " Duplicate — skip.")
                        .var("i", i).var("j", j).var("union", union.toString())
                        .array(combined, i, a.length + j)
                        .step();
                i++;
            } else {
                boolean added = union.isEmpty() || union.get(union.size() - 1) != b[j];
                if (added) union.add(b[j]);
                emit.at("pickB")
                        .say("b[%d]=%d < a[%d]=%d, so pick from B.%s Advance j.",
                                j, b[j], i, a[i],
                                added ? " Added " + b[j] + " to union." : " Duplicate — skip.")
                        .var("i", i).var("j", j).var("union", union.toString())
                        .array(combined, i, a.length + j)
                        .step();
                j++;
            }
        }

        while (i < a.length) {
            boolean added = union.isEmpty() || union.get(union.size() - 1) != a[i];
            if (added) union.add(a[i]);
            emit.at("drainA")
                    .say("B exhausted. Drain A: a[%d]=%d.%s",
                            i, a[i], added ? " Added." : " Duplicate — skip.")
                    .var("i", i).var("union", union.toString())
                    .array(combined, i)
                    .step();
            i++;
        }

        while (j < b.length) {
            boolean added = union.isEmpty() || union.get(union.size() - 1) != b[j];
            if (added) union.add(b[j]);
            emit.at("drainB")
                    .say("A exhausted. Drain B: b[%d]=%d.%s",
                            j, b[j], added ? " Added." : " Duplicate — skip.")
                    .var("j", j).var("union", union.toString())
                    .array(combined, a.length + j)
                    .step();
            j++;
        }

        emit.at("done")
                .say("Union complete: %s (%d elements).", union, union.size())
                .var("union", union.toString())
                .array(combined)
                .step();
    }
}
