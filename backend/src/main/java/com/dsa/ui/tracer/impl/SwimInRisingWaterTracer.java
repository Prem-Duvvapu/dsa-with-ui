package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Swim in Rising Water (LeetCode 778), traced on the owner's own accepted submission over their
 * DisjointSet. Elevations are 0 .. n*n - 1, each once, so at time t exactly one new cell (the one
 * with elevation t) goes under water; join it to every neighbour already under water, and stop at
 * the first time (0,0) and the bottom-right corner share a set. O(n^2 * alpha).
 *
 * <p>The code indexes {@code arr[grid[i][j]]}, which relies on LeetCode's guarantee that the grid is
 * n x n with every value from 0 to n*n - 1 exactly once; the input field declares that
 * ({@code squarePermutation}) and InputValidator refuses a grid that breaks it. The
 * n == 1 early return carries no highlight: neither contract input is 1x1.
 */
@Component
public class SwimInRisingWaterTracer implements AlgorithmTracer {

    // The code's own direction order: up, right, down, left.
    private static final int[] D_ROW = {-1, 0, 1, 0};
    private static final int[] D_COL = {0, 1, 0, -1};

    @Override
    public String id() {
        return "swim-in-rising-water";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Elevation map")
                        .help("grid[r][c] is that cell's elevation. Swim from (0,0) to the bottom-right corner.")
                        .squarePermutation()
                        .constraint("maxRows", 8)
                        .constraint("maxCols", 8)
                        .values(0, 99)
                        .defaultValue(List.of(
                                List.of(0, 1, 2, 3, 4),
                                List.of(24, 23, 22, 21, 5),
                                List.of(12, 13, 14, 15, 16),
                                List.of(11, 17, 18, 19, 20),
                                List.of(10, 9, 8, 7, 6)))
                        .build());
    }

    /** LeetCode 778's first example - a 2x2 board whose answer is 3, not 16. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(0, 2),
                List.of(1, 3)));
    }

    @Override
    public String annotatedCode() {
        return OwnerDisjointSet.code(Set.of()) + "\n\n" + """
               class Solution {
                   static int[] dRow = {-1, 0, 1, 0};
                   static int[] dCol = {0, 1, 0, -1};

                   public int swimInWater(int[][] grid) {
                       // @a init
                       int n = grid.length;
                       if (n == 1)
                           return 0;

                       DisjointSet ds = new DisjointSet(n*n);
                       int[][] arr = new int[n*n][2];

                       for (int i=0;i<n;i++)
                           for (int j=0;j<n;j++)
                               arr[grid[i][j]] = new int[]{i,j};

                       int time = 0;
                       while (ds.getUltimateParent(0) != ds.getUltimateParent(n*n-1)) {
                           // @a rise
                           int[] curr = arr[time];
                           int x = curr[0];
                           int y = curr[1];
                           int node = x*n + y;

                           for (int i=0;i<4;i++) {
                               int newRow = x + dRow[i];
                               int newCol = y + dCol[i];
                               int newNode = newRow*n + newCol;

                               if (newRow>=0 && newRow<n && newCol>=0 && newCol<n && grid[newRow][newCol]<=time)
                                   // @a join
                                   ds.unionBySize(node,newNode);
                           }

                           time++;
                       }

                       // @a done
                       return time-1;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int n = grid.length;
        if (n == 1) {
            emit.at("init").say("A 1x1 grid: the start is already the end, so the check right after the setup "
                            + "returns 0.")
                    .var("answer", 0).grid(grid).step();
            return;
        }

        OwnerDisjointSet ds = new OwnerDisjointSet(n * n);
        int[][] arr = new int[n * n][2];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                arr[grid[i][j]] = new int[]{i, j};
            }
        }

        emit.at("init").say("Elevations run 0 to %d, each once, so arr[t] is the cell that goes under water at "
                        + "time t. Join each newly flooded cell to its flooded neighbours until (0,0) and (%d,%d) "
                        + "are in one set.", n * n - 1, n - 1, n - 1)
                .var("time", 0).grid(grid).step();

        int time = 0;
        while (root(ds, 0) != root(ds, n * n - 1)) {
            int x = arr[time][0];
            int y = arr[time][1];
            int node = x * n + y;
            emit.at("rise").say("Time %d: the water reaches elevation %d, flooding (%d,%d).", time, time, x, y)
                    .var("time", time).var("x", x).var("y", y).grid(grid).step();
            for (int i = 0; i < 4; i++) {
                int newRow = x + D_ROW[i];
                int newCol = y + D_COL[i];
                int newNode = newRow * n + newCol;
                if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n && grid[newRow][newCol] <= time) {
                    OwnerDisjointSet.Union result = ds.union(node, newNode);
                    emit.at("join").say("Neighbour (%d,%d), elevation %d, is under water too. %s",
                                    newRow, newCol, grid[newRow][newCol], result.narrate(node, newNode))
                            .var("time", time).grid(grid).step();
                }
            }
            time++;
        }

        emit.at("done").say("(0,0) and (%d,%d) are now in one set. The loop already moved time on to %d, so the "
                        + "answer is time - 1 = %d.", n - 1, n - 1, time, time - 1)
                .var("time", time).var("answer", time - 1).grid(grid).step();
    }

    private static int root(OwnerDisjointSet ds, int x) {
        return ds.find(x, new ArrayList<>());
    }
}
