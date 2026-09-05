package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Column-by-column backtracking. Every row in the current column is tried; a row that
 * shares no earlier queen's row or either diagonal gets a queen and the search recurses
 * one column deeper, then the queen is removed regardless of what that recursion found -
 * the next row in this same column still needs its own honest attempt.
 */
@Component
public class NQueensTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "n-queens";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("n", FieldType.INT)
                        .label("Board size (N)")
                        .help("Places N non-attacking queens on an NxN board.")
                        .range(1, 8)
                        .defaultValue(4)
                        .build());
    }

    /** A 5x5 board - ten solutions instead of four's two, and every row gets a real trial. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 5);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<String>> solveNQueens(int n) {
                   List<List<String>> solutions = new ArrayList<>();
                   boolean[][] board = new boolean[n][n];
                   place(0, board, solutions, n);
                   // @a done
                   return solutions;
               }

               private void place(int col, boolean[][] board, List<List<String>> solutions, int n) {
                   if (col == n) {
                       // @a solutionFound
                       solutions.add(snapshot(board, n));
                       return;
                   }
                   for (int row = 0; row < n; row++) {
                       if (!isSafe(board, row, col, n)) {
                           // @a conflict
                           continue;
                       }
                       board[row][col] = true;
                       // @a placeQueen
                       place(col + 1, board, solutions, n);
                       board[row][col] = false;
                       // @a backtrack
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        boolean[][] board = new boolean[n][n];
        List<List<String>> solutions = new ArrayList<>();

        place(0, board, solutions, n, emit);

        emit.at("done")
                .say("Every column exhausted. %d distinct arrangement%s found for %d queens.",
                        solutions.size(), solutions.size() == 1 ? "" : "s", n)
                .var("solutions", solutions.size()).grid(toIntGrid(board, n)).step();
    }

    private void place(int col, boolean[][] board, List<List<String>> solutions, int n, StepEmitter emit) {
        if (col == n) {
            solutions.add(snapshot(board, n));
            emit.at("solutionFound")
                    .say("Column %d reached with every earlier queen placed safely - arrangement #%d recorded.",
                            col, solutions.size())
                    .var("solutions", solutions.size()).grid(toIntGrid(board, n)).step();
            return;
        }

        for (int row = 0; row < n; row++) {
            if (!isSafe(board, row, col, n)) {
                emit.at("conflict")
                        .say("Row %d in column %d shares a row or diagonal with an earlier queen - skip it.",
                                row, col)
                        .var("col", col).var("row", row).grid(toIntGrid(board, n)).step();
                continue;
            }

            board[row][col] = true;
            emit.at("placeQueen")
                    .say("Row %d in column %d is safe - place a queen there and recurse into column %d.",
                            row, col, col + 1)
                    .var("col", col).var("row", row).grid(toIntGrid(board, n)).step();

            place(col + 1, board, solutions, n, emit);

            board[row][col] = false;
            emit.at("backtrack")
                    .say("Remove the queen at (%d,%d) - column %d still has other rows left to try.",
                            row, col, col)
                    .var("col", col).var("row", row).grid(toIntGrid(board, n)).step();
        }
    }

    private boolean isSafe(boolean[][] board, int row, int col, int n) {
        for (int c = 0; c < col; c++) {
            if (board[row][c]) {
                return false;
            }
        }
        for (int r = row - 1, c = col - 1; r >= 0 && c >= 0; r--, c--) {
            if (board[r][c]) {
                return false;
            }
        }
        for (int r = row + 1, c = col - 1; r < n && c >= 0; r++, c--) {
            if (board[r][c]) {
                return false;
            }
        }
        return true;
    }

    private List<String> snapshot(boolean[][] board, int n) {
        List<String> rows = new ArrayList<>(n);
        for (int r = 0; r < n; r++) {
            StringBuilder sb = new StringBuilder(n);
            for (int c = 0; c < n; c++) {
                sb.append(board[r][c] ? 'Q' : '.');
            }
            rows.add(sb.toString());
        }
        return rows;
    }

    private int[][] toIntGrid(boolean[][] board, int n) {
        int[][] grid = new int[n][n];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                grid[r][c] = board[r][c] ? 1 : 0;
            }
        }
        return grid;
    }
}
