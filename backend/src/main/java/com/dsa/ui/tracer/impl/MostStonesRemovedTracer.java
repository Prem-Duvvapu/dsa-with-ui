package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * A stone can be removed as long as some other stone still shares its row or column, so the
 * only stones that can NEVER be removed are one representative per connected group (sharing
 * a row/column is transitive - group A-B, B-C means A and C are removable via that chain even
 * without a direct shared line). DSU over stone indices counts exactly that: stones minus the
 * number of groups is the maximum removable.
 */
@Component
public class MostStonesRemovedTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "most-stones-removed";
    }

    @Override
    public DsType dsType() {
        return DsType.DSU;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("stones", FieldType.INT_GRID)
                        .label("Stones")
                        .help("Each row is a stone's [x, y] coordinate.")
                        .constraint("maxRows", 12)
                        .constraint("maxCols", 2)
                        .values(0, 20)
                        .defaultValue(List.of(
                                List.of(0, 0), List.of(0, 1), List.of(1, 0),
                                List.of(1, 2), List.of(2, 1), List.of(2, 2)))
                        .build());
    }

    /** Two disjoint groups instead of one - not every stone can chain into a single group. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("stones", List.of(
                List.of(0, 0), List.of(0, 2), List.of(1, 1), List.of(2, 0), List.of(2, 2)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int removeStones(int[][] stones) {
                   int n = stones.length;
                   int[] parent = new int[n + 1];
                   int[] rank = new int[n + 1];
                   // @a init
                   for (int i = 0; i <= n; i++) parent[i] = i;

                   for (int i = 0; i < n; i++) {
                       for (int j = i + 1; j < n; j++) {
                           boolean sameRow = stones[i][0] == stones[j][0];
                           boolean sameCol = stones[i][1] == stones[j][1];
                           if (sameRow || sameCol) {
                               // @a union
                               union(i + 1, j + 1, parent, rank);
                           }
                       }
                   }

                   // @a count
                   Set<Integer> roots = new HashSet<>();
                   for (int i = 1; i <= n; i++) roots.add(find(i, parent));

                   // @a done
                   return n - roots.size();
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] stones = in.getGrid("stones");
        int n = stones.length;
        int[] parent = new int[n + 1];
        int[] rank = new int[n + 1];
        for (int i = 0; i <= n; i++) parent[i] = i;

        emit.at("init")
                .say("%d stones. Union any pair sharing a row or column (1-indexed to match the element display).", n)
                .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                .var("Disjoint Sets", formatSets(parent, n)).var("Operation", "Initialize DSU(" + n + ")")
                .step();

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                boolean sameRow = stones[i][0] == stones[j][0];
                boolean sameCol = stones[i][1] == stones[j][1];
                if (!(sameRow || sameCol)) {
                    continue;
                }

                int u = i + 1, w = j + 1;
                int ru = find(u, parent);
                int rv = find(w, parent);
                if (ru == rv) {
                    continue;
                }

                if (rank[ru] < rank[rv]) {
                    parent[ru] = rv;
                } else if (rank[ru] > rank[rv]) {
                    parent[rv] = ru;
                } else {
                    parent[rv] = ru;
                    rank[ru]++;
                }

                emit.at("union")
                        .say("Stone %d (%d,%d) and stone %d (%d,%d) share a %s - union them.",
                                u, stones[i][0], stones[i][1], w, stones[j][0], stones[j][1],
                                sameRow ? "row" : "column")
                        .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                        .var("Disjoint Sets", formatSets(parent, n))
                        .var("Operation", "union(" + u + ", " + w + ")")
                        .step();
            }
        }

        Set<Integer> roots = new HashSet<>();
        for (int i = 1; i <= n; i++) roots.add(find(i, parent));
        emit.at("count")
                .say("Count distinct roots across all %d stones: %d group(s) - %s.", n, roots.size(), formatSets(parent, n))
                .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                .var("Disjoint Sets", formatSets(parent, n)).var("Operation", "Count groups")
                .step();
        int answer = n - roots.size();

        emit.at("done")
                .say("%d group(s) formed. One stone per group is unremovable - the rest can go. Max removable: %d.",
                        roots.size(), answer)
                .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                .var("Disjoint Sets", formatSets(parent, n))
                .var("Operation", "Done: " + answer + " removable")
                .step();
    }

    private static int find(int x, int[] parent) {
        if (parent[x] != x) {
            parent[x] = find(parent[x], parent);
        }
        return parent[x];
    }

    private static String formatSets(int[] parent, int n) {
        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int i = 1; i <= n; i++) {
            groups.computeIfAbsent(find(i, parent), k -> new ArrayList<>()).add(i);
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
