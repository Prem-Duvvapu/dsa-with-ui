package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Surrounded Regions (LeetCode 130): every region of 'O' that is completely enclosed by 'X'
 * is captured; a region touching the border survives.
 *
 * <p>Traced on the owner's own accepted submission. Searching each region and asking "did it
 * touch an edge?" is the awkward way round. Invert it: the only regions that survive are those
 * reachable FROM the border, so DFS from every border 'O' first, marking {@code visited}, then
 * flip every interior 'O' the DFS never reached. O(m*n) time.
 *
 * <p>Input encoding: 0 is 'X', 1 is 'O' (the code works on the char board). The canvas draws an
 * 'O' a border DFS has marked safe as 2; a captured 'O' becomes 0 ('X'). The answer variable is
 * the finished board in the code's own characters.
 */
@Component
public class SurroundedRegionsTracer implements AlgorithmTracer {

    // The code's own direction order: up, right, down, left.
    private static final int[] D_ROW = {-1, 0, 1, 0};
    private static final int[] D_COL = {0, 1, 0, -1};

    @Override
    public String id() {
        return "surrounded-regions";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("board", FieldType.INT_GRID)
                        .label("Board")
                        .help("0 is 'X', 1 is 'O'. A cell shown as 2 is an 'O' a border DFS has reached, so it "
                                + "is safe.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(0, 1)
                        // LeetCode 130 example 1. Only the 'O' on the bottom row survives.
                        .defaultValue(List.of(
                                List.of(0, 0, 0, 0),
                                List.of(0, 1, 1, 0),
                                List.of(0, 0, 1, 0),
                                List.of(0, 1, 0, 0)))
                        .build());
    }

    /**
     * A taller board whose border 'O' is the head of a two-cell region reaching inward, so
     * the flood actually has somewhere to spread - where the default's lone border 'O' is
     * boxed in on all four sides and the flood stops immediately.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("board", List.of(
                List.of(1, 0, 0, 0),
                List.of(1, 0, 1, 0),
                List.of(0, 0, 1, 0),
                List.of(0, 0, 0, 0),
                List.of(0, 0, 0, 0)));
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public static int[] dRow = {-1, 0, 1, 0};
                   public static int[] dCol = {0, 1, 0, -1};

                   public void solve(char[][] board) {
                       // @a init
                       int m = board.length;
                       int n = board[0].length;
                       boolean[][] visited = new boolean[m][n];

                       for (int i=0;i<m;i++) {
                           for (int j=0;j<n;j++) {
                               if ((i == 0 || i == m-1 || j == 0 || j == n-1) && board[i][j] == 'O' && !visited[i][j])
                                   // @a border
                                   dfs(i,j,visited,board,m,n);
                           }
                       }

                       for (int i=1;i<m-1;i++) {
                           for (int j=1;j<n-1;j++) {
                               if (board[i][j]=='O' && !visited[i][j])
                                   // @a capture
                                   board[i][j] = 'X';
                           }
                       }
                   // @a done
                   }

                   private void dfs(int r,int c,boolean[][] visited,char[][] board,int m,int n) {
                       // @a visit
                       visited[r][c] = true;

                       for (int i=0;i<4;i++) {
                           int newRow = r + dRow[i];
                           int newCol = c + dCol[i];

                           // @a recurse
                           if (newRow>=0 && newRow<m && newCol>=0 && newCol<n && board[newRow][newCol]=='O' && !visited[newRow][newCol])
                               dfs(newRow, newCol, visited, board, m, n);
                       }
                   // @a return
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] cells = in.getGrid("board");
        int m = cells.length;
        int n = cells[0].length;
        char[][] board = new char[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                board[i][j] = cells[i][j] == 1 ? 'O' : 'X';
            }
        }
        boolean[][] visited = new boolean[m][n];

        emit.at("init").say("An 'O' region survives only if it touches the border. So first DFS from "
                        + "every border 'O' and mark what it reaches; afterwards, any interior 'O' left "
                        + "unmarked is surrounded and becomes 'X'.")
                .grid(view(board, visited)).step();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if ((i == 0 || i == m - 1 || j == 0 || j == n - 1) && board[i][j] == 'O' && !visited[i][j]) {
                    emit.at("border").say("(%d,%d) is an unvisited 'O' on the border, so its region is safe. "
                                    + "dfs(%d,%d) marks all of it.", i, j, i, j)
                            .var("i", i).var("j", j).grid(view(board, visited)).step();
                    dfs(i, j, visited, board, m, n, emit);
                }
            }
        }

        int captured = 0;
        for (int i = 1; i < m - 1; i++) {
            for (int j = 1; j < n - 1; j++) {
                if (board[i][j] == 'O' && !visited[i][j]) {
                    board[i][j] = 'X';
                    captured++;
                    emit.at("capture").say("(%d,%d) is an 'O' no border DFS reached - it is surrounded. "
                                    + "Flip it to 'X'.", i, j)
                            .var("i", i).var("j", j).var("captured", captured)
                            .grid(view(board, visited)).step();
                }
            }
        }

        emit.at("done").say("%d surrounded 'O'%s flipped to 'X'; every 'O' left touches the border "
                        + "through other 'O's.", captured, captured == 1 ? " was" : "s were")
                .var("captured", captured).var("board", GridText.ofChars(board))
                .grid(view(board, visited)).step();
    }

    private static void dfs(int r, int c, boolean[][] visited, char[][] board, int m, int n, StepEmitter emit) {
        emit.push("dfs(" + r + "," + c + ")");
        visited[r][c] = true;
        emit.at("visit").say("dfs(%d,%d): mark this 'O' visited - it is connected to the border.", r, c)
                .var("r", r).var("c", c).grid(view(board, visited)).step();

        for (int i = 0; i < 4; i++) {
            int newRow = r + D_ROW[i];
            int newCol = c + D_COL[i];
            if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                    && board[newRow][newCol] == 'O' && !visited[newRow][newCol]) {
                emit.at("recurse").say("(%d,%d) is an unvisited 'O' joined to (%d,%d), so it is safe too. "
                                + "Recurse into dfs(%d,%d).", newRow, newCol, r, c, newRow, newCol)
                        .var("r", r).var("c", c).var("newRow", newRow).var("newCol", newCol)
                        .grid(view(board, visited)).step();
                dfs(newRow, newCol, visited, board, m, n, emit);
            }
        }

        emit.at("return").say("dfs(%d,%d) is done. Return to the caller.", r, c)
                .var("r", r).var("c", c).grid(view(board, visited)).step();
        emit.pop();
    }

    /** 0 'X', 1 'O', 2 an 'O' a border DFS has reached. */
    private static int[][] view(char[][] board, boolean[][] visited) {
        int[][] out = new int[board.length][board[0].length];
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                out[r][c] = board[r][c] == 'X' ? 0 : visited[r][c] ? 2 : 1;
            }
        }
        return out;
    }
}
