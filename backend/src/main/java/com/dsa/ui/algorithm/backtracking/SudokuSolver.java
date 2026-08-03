package com.dsa.ui.algorithm.backtracking;

import com.dsa.ui.trace.SnapshotUtil;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Problem: Sudoku Solver (LeetCode 37)
 *
 * Fill a 9x9 board empty cells (represented by 0) with digits 1-9
 * such that every row, column, and 3x3 sub-box contains digits 1-9 without repetition.
 *
 * Approach: Backtracking cell by cell.
 * Time Complexity:  O(9^(N)) where N is number of empty cells.
 * Space Complexity: O(N) recursion stack depth.
 */
public class SudokuSolver {

    public boolean solve(int[][] board, TraceRecorder recorder) {
        List<String> callStack = new ArrayList<>();

        recorder.record(new TraceEvent(
            "start", 20,
            "Sudoku Solver: Start backtracking solver on 9x9 grid with 3x3 sub-box constraints.",
            Map.of("grid", "9x9 Board"),
            "Matrix", SnapshotUtil.clone2DGrid(board),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        boolean solved = solveRecursive(board, recorder, callStack);

        recorder.record(new TraceEvent(
            "complete", 55,
            String.format("Sudoku Solver Complete! Board status: %s.", solved ? "FULLY SOLVED" : "NO SOLUTION"),
            Map.of("Status", solved ? "SOLVED" : "FAILED"),
            "Matrix", SnapshotUtil.clone2DGrid(board),
            List.of(), Map.of(), List.of()
        ));

        return solved;
    }

    private boolean solveRecursive(int[][] board, TraceRecorder recorder, List<String> callStack) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) { // Empty cell
                    String callFrame = String.format("solveCell(%d, %d)", r, c);
                    callStack.add(callFrame);

                    for (int digit = 1; digit <= 9; digit++) {
                        boolean valid = isValid(board, r, c, digit);

                        if (valid) {
                            board[r][c] = digit;

                            recorder.record(new TraceEvent(
                                "place_digit", 35,
                                String.format("Cell (%d, %d) is empty (0). Try digit %d: Row, Col, and 3x3 box valid! Set board[%d][%d] = %d. Recurse...",
                                    r, c, digit, r, c, digit),
                                Map.of("row", String.valueOf(r), "col", String.valueOf(c), "digit", String.valueOf(digit)),
                                "Matrix", SnapshotUtil.clone2DGrid(board),
                                new ArrayList<>(callStack), Map.of(), List.of()
                            ));

                            if (solveRecursive(board, recorder, callStack)) {
                                callStack.remove(callStack.size() - 1);
                                return true;
                            }

                            // Backtrack
                            board[r][c] = 0;

                            recorder.record(new TraceEvent(
                                "backtrack_digit", 42,
                                String.format("BACKTRACK at Cell (%d, %d): Digit %d led to dead end. Reset board[%d][%d] = 0.",
                                    r, c, digit, r, c),
                                Map.of("row", String.valueOf(r), "col", String.valueOf(c), "reset", "0"),
                                "Matrix", SnapshotUtil.clone2DGrid(board),
                                new ArrayList<>(callStack), Map.of(), List.of()
                            ));
                        } else {
                            recorder.record(new TraceEvent(
                                "invalid_digit", 45,
                                String.format("Cell (%d, %d): Try digit %d -> INVALID! Constraint conflict in row %d, col %d, or 3x3 sub-box.",
                                    r, c, digit, r, c),
                                Map.of("row", String.valueOf(r), "col", String.valueOf(c), "conflict_digit", String.valueOf(digit)),
                                "Matrix", SnapshotUtil.clone2DGrid(board),
                                new ArrayList<>(callStack), Map.of(), List.of()
                            ));
                        }
                    }

                    callStack.remove(callStack.size() - 1);
                    return false; // Backtrack
                }
            }
        }
        return true; // All cells filled
    }

    private boolean isValid(int[][] board, int row, int col, int digit) {
        for (int i = 0; i < 9; i++) {
            if (board[row][i] == digit) return false; // Check row
            if (board[i][col] == digit) return false; // Check col
            if (board[3 * (row / 3) + i / 3][3 * (col / 3) + i % 3] == digit) return false; // Check 3x3 box
        }
        return true;
    }
}
