package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.function.IntFunction;
import java.util.*;

/**
 * Most Stones Removed with Same Row or Column (LeetCode 947), traced on the owner's own submission:
 * rows and columns are DSU nodes (row x is node x, column y is node y + colStart), and each stone
 * unions its row with its column. Every connected group of k stones can be removed down to one,
 * so the answer is stones minus groups. O(n * alpha).
 *
 * <p>One change: the submission's {@code unionBySize} had no "already in the same set" check, so a
 * repeated union doubled that root's size and skewed union-by-size balancing (the answer was still
 * right). The DisjointSet shown is the owner's own later version, which has the check; a comment
 * in the displayed code says so.
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
        return OwnerDisjointSet.code(Set.of()) + "\n\n" + """
               // Changed from your submission: its unionBySize had no "already in the same set" check.
               // The answer was still right, but each repeated union doubled that root's size[] and
               // skewed the balancing. The DisjointSet above is your own later version with the check.
               class Solution {
                   public int removeStones(int[][] stones) {
                       // @a init
                       int n = stones.length;
                       int maxRow = 0;
                       int maxCol = 0;

                       for (int[] coordinates: stones) {
                           int x = coordinates[0];
                           int y = coordinates[1];

                           maxRow = Math.max(maxRow, x);
                           maxCol = Math.max(maxCol, y);
                       }

                       int colStart = maxRow + 1;
                       int colEnd = maxCol + colStart;

                       DisjointSet ds = new DisjointSet(colEnd+1);
                       boolean[] visited = new boolean[colEnd+1];
                       for (int[] coordinates: stones) {
                           int x = coordinates[0];
                           int y = coordinates[1] + colStart;

                           visited[x] = true;
                           visited[y] = true;

                           // @a union
                           ds.unionBySize(x,y);
                       }

                       int components = 0;
                       for (int i=0;i<=colEnd;i++) {
                           if (visited[i] && ds.parent[i] == i)
                               // @a component
                               components++;
                       }

                       // @a done
                       return (n - components);
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] stones = in.getGrid("stones");
        int n = stones.length;
        int maxRow = 0;
        int maxCol = 0;
        for (int[] c : stones) {
            maxRow = Math.max(maxRow, c[0]);
            maxCol = Math.max(maxCol, c[1]);
        }
        int colStart = maxRow + 1;
        int colEnd = maxCol + colStart;
        OwnerDisjointSet ds = new OwnerDisjointSet(colEnd + 1);
        boolean[] visited = new boolean[colEnd + 1];
        IntFunction<String> label = i -> i < colStart ? "r" + i : "c" + (i - colStart);

        emit.at("init").say("%d stone%s. Rows 0..%d are DSU nodes 0..%d and columns 0..%d are nodes %d..%d, so "
                        + "a stone joins its row node to its column node.",
                        n, Narration.s(n), maxRow, maxRow, maxCol, colStart, colEnd)
                .var("Operation", "DisjointSet(" + (colEnd + 1) + ")").var("Disjoint Sets", "")
                .var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();

        for (int[] c : stones) {
            int x = c[0];
            int y = c[1] + colStart;
            visited[x] = true;
            visited[y] = true;
            OwnerDisjointSet.Union result = ds.union(x, y);
            emit.at("union").say("Stone (%d,%d) links row %d (node %d) with column %d (node %d). %s",
                            c[0], c[1], c[0], x, c[1], y, result.narrate(x, y))
                    .var("Operation", "stone (" + c[0] + "," + c[1] + "): unionBySize(" + x + ", " + y + ")")
                    .var("Disjoint Sets", ds.sets(i -> visited[i], label))
                    .var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();
        }

        int components = 0;
        for (int i = 0; i <= colEnd; i++) {
            if (visited[i] && ds.parent[i] == i) {
                components++;
                emit.at("component").say("Node %d (%s) is used by a stone and is its own parent: the root of "
                                + "group %d.", i, label.apply(i), components)
                        .var("components", components).var("Operation", "count roots")
                        .var("Disjoint Sets", ds.sets(j -> visited[j], label))
                        .var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();
            }
        }

        int answer = n - components;
        emit.at("done").say("Each group of stones can be removed down to its last one: %d - %d = %d.",
                        n, components, answer)
                .var("components", components).var("answer", answer).var("Operation", "done")
                .var("Disjoint Sets", ds.sets(j -> visited[j], label))
                .var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();
    }
}
