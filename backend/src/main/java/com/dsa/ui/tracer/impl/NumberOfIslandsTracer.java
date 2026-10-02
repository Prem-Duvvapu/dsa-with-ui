package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Number of Islands (LeetCode 200), traced on the owner's own accepted submission: a
 * {@code visited} grid and a recursive DFS over {@code dRow}/{@code dCol}. Already optimal -
 * O(n*m) time, every land cell visited once.
 *
 * <p>The code never writes into {@code grid}; it marks {@code visited[r][c]}. The canvas shows
 * that mark by drawing visited land as 2, so "already part of a counted island" is visible -
 * which is the whole reason no cell is ever counted twice. The input is entered as 0/1;
 * LeetCode passes the same grid as the chars '0' and '1', and the code compares against '1'.
 */
@Component
public class NumberOfIslandsTracer implements AlgorithmTracer {

    // The code's own direction order: up, right, down, left.
    private static final int[] D_ROW = {-1, 0, 1, 0};
    private static final int[] D_COL = {0, 1, 0, -1};

    @Override
    public String id() {
        return "number-of-islands";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Grid")
                        .help("1 is land, 0 is water. Land the DFS has visited is drawn as 2 (the code keeps "
                                + "that in visited[][], not in the grid).")
                        .constraint("maxRows", 12)
                        .constraint("maxCols", 12)
                        .values(0, 1)
                        .defaultValue(List.of(
                                List.of(1, 1, 0, 0, 0),
                                List.of(1, 1, 0, 0, 0),
                                List.of(0, 0, 1, 0, 0),
                                List.of(0, 0, 0, 1, 1)))
                        .build());
    }

    /** Four single-cell islands instead of a few large ones. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 0, 1),
                List.of(0, 0, 0),
                List.of(1, 0, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   private static final int[] dRow = {-1,0,1,0};
                   private static final int[] dCol = {0,1,0,-1};

                   public int numIslands(char[][] grid) {
                       // @a init
                       int n = grid.length;
                       int m = grid[0].length;
                       boolean[][] visited = new boolean[n][m];
                       int numIslands = 0;

                       for (int i=0;i<n;i++) {
                           for (int j=0;j<m;j++) {
                               if (grid[i][j] == '1' && !visited[i][j]) {
                                   // @a found
                                   numIslands++;
                                   dfs(i,j,visited,grid);
                               }
                           }
                       }

                       // @a done
                       return numIslands;
                   }

                   private void dfs(int r,int c,boolean[][] visited,char[][] grid) {
                       // @a visit
                       visited[r][c] = true;

                       for (int i=0;i<4;i++) {
                           int newRow = r + dRow[i];
                           int newCol = c + dCol[i];

                           // @a recurse
                           if (newRow>=0 && newRow<grid.length && newCol>=0 && newCol<grid[0].length && grid[newRow][newCol]=='1' && !visited[newRow][newCol]) {
                               dfs(newRow,newCol,visited,grid);
                           }
                       }
                   // @a return
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int n = grid.length;
        int m = grid[0].length;
        boolean[][] visited = new boolean[n][m];
        int numIslands = 0;

        emit.at("init").say("Scan the %dx%d grid row by row. Each land cell that no earlier DFS has "
                        + "visited starts a new island.", n, m)
                .var("numIslands", 0).grid(view(grid, visited)).step();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (grid[i][j] == 1 && !visited[i][j]) {
                    numIslands++;
                    emit.at("found").say("(%d,%d) is land that no DFS has reached yet, so it belongs to a new "
                                    + "island: numIslands = %d. Run dfs(%d,%d) to mark the whole island.",
                                    i, j, numIslands, i, j)
                            .var("i", i).var("j", j).var("numIslands", numIslands)
                            .grid(view(grid, visited)).step();
                    dfs(i, j, visited, grid, emit);
                }
            }
        }

        emit.at("done").say("Every cell has been scanned. The grid holds %d island%s.",
                        numIslands, Narration.s(numIslands))
                .var("numIslands", numIslands).grid(view(grid, visited)).step();
    }

    private static void dfs(int r, int c, boolean[][] visited, int[][] grid, StepEmitter emit) {
        emit.push("dfs(" + r + "," + c + ")");
        visited[r][c] = true;
        emit.at("visit").say("dfs(%d,%d): mark it visited, then try its four neighbours.", r, c)
                .var("r", r).var("c", c).grid(view(grid, visited)).step();

        for (int i = 0; i < 4; i++) {
            int newRow = r + D_ROW[i];
            int newCol = c + D_COL[i];
            if (newRow >= 0 && newRow < grid.length && newCol >= 0 && newCol < grid[0].length
                    && grid[newRow][newCol] == 1 && !visited[newRow][newCol]) {
                emit.at("recurse").say("(%d,%d) is unvisited land next to (%d,%d) - the same island. "
                                + "Recurse into dfs(%d,%d).", newRow, newCol, r, c, newRow, newCol)
                        .var("r", r).var("c", c).var("newRow", newRow).var("newCol", newCol)
                        .grid(view(grid, visited)).step();
                dfs(newRow, newCol, visited, grid, emit);
            }
        }

        emit.at("return").say("dfs(%d,%d) is done: no unvisited land touches it. Return to the caller.", r, c)
                .var("r", r).var("c", c).grid(view(grid, visited)).step();
        emit.pop();
    }

    /** The grid as the canvas draws it: visited land as 2, so the visited[][] mark is visible. */
    private static int[][] view(int[][] grid, boolean[][] visited) {
        int[][] out = new int[grid.length][];
        for (int r = 0; r < grid.length; r++) {
            out[r] = grid[r].clone();
            for (int c = 0; c < grid[r].length; c++) {
                if (visited[r][c]) {
                    out[r][c] = 2;
                }
            }
        }
        return out;
    }
}
