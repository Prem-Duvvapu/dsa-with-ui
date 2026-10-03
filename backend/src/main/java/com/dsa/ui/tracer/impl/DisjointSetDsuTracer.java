package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Disjoint Set (Union-Find), on the owner's own {@code DisjointSet}: path compression in
 * {@code getUltimateParent} and union by size. Elements are numbered 0 to n - 1, and each union
 * hangs the smaller set's root under the larger one's, so trees stay shallow; with path
 * compression every operation is close to O(1) amortised.
 */
@Component
public class DisjointSetDsuTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "disjoint-set-dsu";
    }

    @Override
    public DsType dsType() {
        return DsType.DSU;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("dsu", FieldType.GRAPH)
                        .label("Elements and unions")
                        .help("Vertex count is the number of elements, numbered 0 to n - 1. Edges are "
                                + "the unionBySize(u, v) operations to perform, in order.")
                        .constraint("maxVertices", 21)
                        .constraint("maxEdges", 20)
                        .defaultValue(Map.of(
                                "vertices", 8,
                                "edges", List.of(
                                        List.of(1, 2), List.of(2, 3), List.of(4, 5),
                                        List.of(6, 7), List.of(5, 6), List.of(3, 7))))
                        .build());
    }

    /**
     * Two independent pairs, an untouched element, and one union of two elements that are already
     * together - the early return the default never takes.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("dsu", Map.of(
                "vertices", 6,
                "edges", List.of(List.of(1, 2), List.of(3, 4), List.of(2, 1))));
    }

    private static final Set<String> ANCHORED = Set.of(
            OwnerDisjointSet.SAME_SET, OwnerDisjointSet.ATTACH_TO_U, OwnerDisjointSet.ATTACH_TO_V);

    @Override
    public String annotatedCode() {
        return OwnerDisjointSet.code(ANCHORED) + "\n\n" + """
               class Solution {
                   public void process(int n, int[][] unions) {
                       // @a init
                       DisjointSet ds = new DisjointSet(n);

                       for (int[] op: unions)
                           ds.unionBySize(op[0], op[1]);
                   // @a done
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput input = in.getGraph("dsu");
        int n = input.vertices();
        OwnerDisjointSet ds = new OwnerDisjointSet(n);

        emit.at("init").say("%d elements, each in a set of its own: parent[i] = i and size[i] = 1.", n)
                .var("Operation", "DisjointSet(" + n + ")").var("Disjoint Sets", ds.sets())
                .var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();

        for (int[] op : input.edges()) {
            OwnerDisjointSet.Union result = ds.union(op[0], op[1]);
            emit.at(result.branch()).say(result.narrate(op[0], op[1]))
                    .var("Operation", "unionBySize(" + op[0] + ", " + op[1] + ")")
                    .var("Disjoint Sets", ds.sets())
                    .var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();
        }

        emit.at("done").say("Every union is done. The elements fall into these sets: %s.", ds.sets())
                .var("Operation", "done").var("Disjoint Sets", ds.sets())
                .var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();
    }
}
