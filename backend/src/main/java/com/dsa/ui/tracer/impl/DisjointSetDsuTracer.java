package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Union by rank keeps every tree shallow on its own (attach the shorter tree under the
 * taller one, so height only ever grows on a tie), and path compression in {@code find}
 * flattens whatever height still accumulates by re-pointing every visited node straight at
 * the root the moment it is discovered - the two together are what keep near-O(1) find/union
 * instead of degrading to a linked list of unioned elements.
 *
 * <p>Elements are 1-indexed to match {@code DsuCanvas}; index 0 is unused padding.
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
                        .help("Vertex count is elements + 1 (index 0 is padding). Edges are the "
                                + "union(u, v) operations to perform, in order.")
                        .constraint("maxVertices", 21)
                        .constraint("maxEdges", 20)
                        .defaultValue(Map.of(
                                "vertices", 8,
                                "edges", List.of(
                                        List.of(1, 2), List.of(2, 3), List.of(4, 5),
                                        List.of(6, 7), List.of(5, 6), List.of(3, 7))))
                        .build());
    }

    /** Two independent pairs plus an isolated element - a partial merge, never a single set. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("dsu", Map.of(
                "vertices", 6,
                "edges", List.of(List.of(1, 2), List.of(3, 4))));
    }

    @Override
    public String annotatedCode() {
        return """
               class DisjointSet {
                   int[] parent, rank;

                   public DisjointSet(int n) {
                       // @a init
                       parent = new int[n + 1];
                       rank = new int[n + 1];
                       for (int i = 0; i <= n; i++) parent[i] = i;
                   }

                   public int find(int x) {
                       if (parent[x] != x) {
                           parent[x] = find(parent[x]); // path compression
                       }
                       return parent[x];
                   }

                   public void union(int u, int v) {
                       // @a find
                       int ru = find(u), rv = find(v);
                       if (ru == rv) return;

                       // @a attach
                       if (rank[ru] < rank[rv]) {
                           parent[ru] = rv;
                       } else if (rank[ru] > rank[rv]) {
                           parent[rv] = ru;
                       } else {
                           parent[rv] = ru;
                           rank[ru]++;
                       }
                   }
               }

               // @a done
               for (int[] op : unions) dsu.union(op[0], op[1]);
               """;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput dsu = in.getGraph("dsu");
        int n = dsu.vertices() - 1;
        int[][] unions = dsu.edges();

        int[] parent = new int[n + 1];
        int[] rank = new int[n + 1];
        for (int i = 0; i <= n; i++) parent[i] = i;

        emit.at("init")
                .say("Initialize DSU with %d elements: parent[i] = i, rank[i] = 0 for every i. "
                        + "Each element starts as its own root.", n)
                .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                .var("Disjoint Sets", formatSets(parent, n)).var("Operation", "Initialize DSU(" + n + ")")
                .step();

        for (int[] op : unions) {
            int u = op[0];
            int v = op[1];
            int ru = find(u, parent);
            int rv = find(v, parent);

            emit.at("find")
                    .say("union(%d, %d): find(%d) = %d (rank %d), find(%d) = %d (rank %d).",
                            u, v, u, ru, rank[ru], v, rv, rank[rv])
                    .var("u", u).var("v", v).var("ru", ru).var("rv", rv)
                    .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                    .var("Disjoint Sets", formatSets(parent, n)).var("Operation", "find(" + u + ", " + v + ")")
                    .step();

            if (ru == rv) {
                emit.at("attach")
                        .say("%d and %d are already in the same set (root %d) - union(%d, %d) is a no-op.",
                                u, v, ru, u, v)
                        .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                        .var("Disjoint Sets", formatSets(parent, n)).var("Operation", "union(" + u + ", " + v + ") no-op")
                        .step();
                continue;
            }

            String reason;
            if (rank[ru] < rank[rv]) {
                parent[ru] = rv;
                reason = String.format("rank[%d] (%d) < rank[%d] (%d). Attach %d under %d.", ru, rank[ru], rv, rank[rv], ru, rv);
            } else if (rank[ru] > rank[rv]) {
                parent[rv] = ru;
                reason = String.format("rank[%d] (%d) < rank[%d] (%d). Attach %d under %d.", rv, rank[rv], ru, rank[ru], rv, ru);
            } else {
                parent[rv] = ru;
                rank[ru]++;
                reason = String.format("Ranks tie (%d == %d). Attach %d under %d and bump rank[%d] to %d.",
                        rank[rv], rank[ru] - 1, rv, ru, ru, rank[ru]);
            }

            String sets = formatSets(parent, n);
            emit.at("attach")
                    .say("Operation: union(%d, %d). %s", u, v, reason)
                    .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                    .var("Disjoint Sets", sets).var("Operation", "union(" + u + ", " + v + ")")
                    .step();
        }

        String finalSets = formatSets(parent, n);
        boolean single = countRoots(parent, n) == 1;
        emit.at("done")
                .say(single
                        ? "Every element is now in one connected component: %s."
                        : "Final disjoint sets: %s.", finalSets)
                .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                .var("Disjoint Sets", single ? finalSets + " (Single Component!)" : finalSets)
                .var("Operation", "Done")
                .step();
    }

    private static int find(int x, int[] parent) {
        if (parent[x] != x) {
            parent[x] = find(parent[x], parent);
        }
        return parent[x];
    }

    /** Read-only root lookup for display, so building the sets string never mutates parent[]. */
    private static int rootOf(int x, int[] parent) {
        while (parent[x] != x) {
            x = parent[x];
        }
        return x;
    }

    private static int countRoots(int[] parent, int n) {
        Set<Integer> roots = new HashSet<>();
        for (int i = 1; i <= n; i++) roots.add(rootOf(i, parent));
        return roots.size();
    }

    private static String formatSets(int[] parent, int n) {
        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int i = 1; i <= n; i++) {
            groups.computeIfAbsent(rootOf(i, parent), k -> new ArrayList<>()).add(i);
        }
        List<List<Integer>> ordered = new ArrayList<>(groups.values());
        ordered.sort(Comparator.comparingInt(g -> g.get(0)));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ordered.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("{");
            List<Integer> group = ordered.get(i);
            for (int j = 0; j < group.size(); j++) {
                if (j > 0) sb.append(", ");
                sb.append(group.get(j));
            }
            sb.append("}");
        }
        return sb.toString();
    }
}
