package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Number of Enclaves (LeetCode 1020): count the land cells from which you cannot walk off
 * the grid, moving one step at a time between 4-directionally adjacent land.
 *
 * <p>Traced on the owner's own accepted submission. Asking each land cell separately whether it
 * can escape repeats the same search; asking the complement - which land CAN escape - needs one
 * pass: DFS from every land cell on the border, marking {@code visited}. Interior land that no
 * border DFS reached is, by definition, an enclave. O(m*n) time.
 *
 * <p>Display encoding: 0 sea, 1 land, 2 land a border DFS has visited (it can escape), 3 land
 * counted as an enclave. The finished grid shows which cells the answer is made of.
 */
@Component
public class NumberOfEnclavesTracer implements AlgorithmTracer {

    // The code's own direction order: up, right, down, left.
    private static final int[] D_ROW = {-1, 0, 1, 0};
    private static final int[] D_COL = {0, 1, 0, -1};

    @Override
    public String id() {
        return "number-of-enclaves";
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
                        .help("1 is land, 0 is sea. Land a border DFS has visited (it can walk off) is drawn as "
                                + "2; land counted as an enclave is drawn as 3.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(0, 1)
                        // LeetCode 1020 example 1: (1,0) walks off the left edge; the three
                        // cells (1,2), (2,1) and (2,2) cannot. Answer: 3.
                        .defaultValue(List.of(
                                List.of(0, 0, 0, 0),
                                List.of(1, 0, 1, 0),
                                List.of(0, 1, 1, 0),
                                List.of(0, 0, 0, 0)))
                        .build());
    }

    /**
     * A wider board on which every land cell is either on the boundary or joined to one, so
     * the flood claims all of it and the answer is 0 - where the default leaves an enclave
     * behind.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 1, 0, 0, 0),
                List.of(0, 1, 0, 1, 1),
                List.of(0, 0, 0, 0, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public static int[] dRow = {-1, 0, 1, 0};
                   public static int[] dCol = {0, 1, 0, -1};

                   public int numEnclaves(int[][] grid) {
                       // @a init
                       int m = grid.length;
                       int n = grid[0].length;
                       int cnt = 0;
                       boolean[][] visited = new boolean[m][n];

                       for (int i=0;i<m;i++) {
                           for (int j=0;j<n;j++) {
                               if ((i == 0 || i == m-1 || j == 0 || j == n-1) && grid[i][j] == 1 && !visited[i][j])
                                   // @a border
                                   dfs(i, j, visited, grid, m, n);
                           }
                       }

                       for (int i=1;i<m-1;i++) {
                           for (int j=1;j<n-1;j++) {
                               if (grid[i][j] == 1 && !visited[i][j])
                                   // @a count
                                   cnt++;
                           }
                       }

                       // @a done
                       return cnt;
                   }

                   private void dfs(int r,int c,boolean[][] visited,int[][] grid,int m,int n) {
                       // @a visit
                       visited[r][c] = true;

                       for (int i=0;i<4;i++) {
                           int newRow = r + dRow[i];
                           int newCol = c + dCol[i];

                           // @a recurse
                           if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n && grid[newRow][newCol] == 1 && !visited[newRow][newCol])
                               dfs(newRow, newCol, visited, grid, m, n);
                       }
                   // @a return
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int m = grid.length;
        int n = grid[0].length;
        boolean[][] visited = new boolean[m][n];
        boolean[][] counted = new boolean[m][n];
        int cnt = 0;

        emit.at("init").say("Land on the border can walk straight off the grid, and so can any land "
                        + "joined to it. Mark all of that first with a DFS from each border land cell; "
                        + "whatever interior land is left unmarked is an enclave.")
                .var("cnt", 0).grid(view(grid, visited, counted)).step();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if ((i == 0 || i == m - 1 || j == 0 || j == n - 1) && grid[i][j] == 1 && !visited[i][j]) {
                    emit.at("border").say("(%d,%d) is unvisited land on the border - it can walk off. "
                                    + "dfs(%d,%d) marks everything joined to it.", i, j, i, j)
                            .var("i", i).var("j", j).grid(view(grid, visited, counted)).step();
                    dfs(i, j, visited, grid, m, n, counted, emit);
                }
            }
        }

        for (int i = 1; i < m - 1; i++) {
            for (int j = 1; j < n - 1; j++) {
                if (grid[i][j] == 1 && !visited[i][j]) {
                    cnt++;
                    counted[i][j] = true;
                    emit.at("count").say("(%d,%d) is interior land no border DFS reached, so it cannot walk "
                                    + "off: an enclave cell. cnt = %d.", i, j, cnt)
                            .var("i", i).var("j", j).var("cnt", cnt)
                            .grid(view(grid, visited, counted)).step();
                }
            }
        }

        emit.at("done").say("%d land cell%s cannot reach the border.", cnt, Narration.s(cnt))
                .var("cnt", cnt).grid(view(grid, visited, counted)).step();
    }

    private static void dfs(int r, int c, boolean[][] visited, int[][] grid, int m, int n,
                            boolean[][] counted, StepEmitter emit) {
        emit.push("dfs(" + r + "," + c + ")");
        visited[r][c] = true;
        emit.at("visit").say("dfs(%d,%d): mark it visited - it can reach the border.", r, c)
                .var("r", r).var("c", c).grid(view(grid, visited, counted)).step();

        for (int i = 0; i < 4; i++) {
            int newRow = r + D_ROW[i];
            int newCol = c + D_COL[i];
            if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                    && grid[newRow][newCol] == 1 && !visited[newRow][newCol]) {
                emit.at("recurse").say("(%d,%d) is unvisited land joined to (%d,%d), so it can escape too. "
                                + "Recurse into dfs(%d,%d).", newRow, newCol, r, c, newRow, newCol)
                        .var("r", r).var("c", c).var("newRow", newRow).var("newCol", newCol)
                        .grid(view(grid, visited, counted)).step();
                dfs(newRow, newCol, visited, grid, m, n, counted, emit);
            }
        }

        emit.at("return").say("dfs(%d,%d) is done. Return to the caller.", r, c)
                .var("r", r).var("c", c).grid(view(grid, visited, counted)).step();
        emit.pop();
    }

    /** 0 sea, 1 land, 2 visited from the border, 3 counted as an enclave. */
    private static int[][] view(int[][] grid, boolean[][] visited, boolean[][] counted) {
        int[][] out = new int[grid.length][];
        for (int r = 0; r < grid.length; r++) {
            out[r] = grid[r].clone();
            for (int c = 0; c < grid[r].length; c++) {
                if (visited[r][c]) out[r][c] = 2;
                else if (counted[r][c]) out[r][c] = 3;
            }
        }
        return out;
    }
}
