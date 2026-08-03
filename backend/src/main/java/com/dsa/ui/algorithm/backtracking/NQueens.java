package com.dsa.ui.algorithm.backtracking;

import com.dsa.ui.trace.SnapshotUtil;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Problem: N-Queens (LeetCode 51)
 *
 * Place N non-attacking queens on an N x N chessboard.
 * No two queens share the same row, column, or diagonal.
 *
 * Approach: Column-by-column backtracking with O(1) safety checks
 * using three hash arrays (leftRow, lowerDiagonal, upperDiagonal).
 *
 * Time Complexity:  O(N!) - At most N choices for col 0, N-1 for col 1, etc.
 * Space Complexity: O(N^2) for board + O(N) auxiliary recursion stack depth.
 */
public class NQueens {

    public List<List<String>> solve(int n, TraceRecorder recorder) {
        List<List<String>> solutions = new ArrayList<>();
        int[][] board = new int[n][n];

        int[] leftRow = new int[n];
        int[] lowerDiagonal = new int[2 * n - 1];
        int[] upperDiagonal = new int[2 * n - 1];
        List<String> callStack = new ArrayList<>();

        recorder.record(new TraceEvent(
            "start", 50,
            String.format("N-Queens (%dx%d Chessboard): Initialize empty board and O(1) safety hash arrays. Start search at col = 0.", n, n),
            Map.of("N", String.valueOf(n), "board", n + "x" + n + " empty"),
            "Matrix", SnapshotUtil.clone2DGrid(board),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        solveRecursive(0, board, solutions, leftRow, lowerDiagonal, upperDiagonal, n, recorder, callStack);

        recorder.record(new TraceEvent(
            "complete", 77,
            String.format("N-Queens Backtracking Complete! Total distinct non-attacking solutions found = %d.", solutions.size()),
            Map.of("Total Solutions", String.valueOf(solutions.size())),
            "Matrix", SnapshotUtil.clone2DGrid(board),
            List.of(), Map.of(), List.of()
        ));

        return solutions;
    }

    private void solveRecursive(int col, int[][] board, List<List<String>> solutions,
                                int[] leftRow, int[] lowerDiagonal, int[] upperDiagonal,
                                int n, TraceRecorder recorder, List<String> callStack) {
        String callFrame = "solve(col=" + col + ")";
        callStack.add(callFrame);

        if (col == n) {
            solutions.add(constructBoardSolution(board, n));
            recorder.record(new TraceEvent(
                "solution", 61,
                String.format("SOLUTION FOUND! All %d queens placed safely with zero attacks. Solution #%d recorded.", n, solutions.size()),
                Map.of("Status", "SOLUTION_FOUND", "Solution #", String.valueOf(solutions.size())),
                "Matrix", SnapshotUtil.clone2DGrid(board),
                new ArrayList<>(callStack), Map.of(), List.of()
            ));
            callStack.remove(callStack.size() - 1);
            return;
        }

        for (int row = 0; row < n; row++) {
            int lowerDiagIdx = row + col;
            int upperDiagIdx = n - 1 + col - row;

            boolean rowOccupied = leftRow[row] == 1;
            boolean lowerDiagOccupied = lowerDiagonal[lowerDiagIdx] == 1;
            boolean upperDiagOccupied = upperDiagonal[upperDiagIdx] == 1;

            if (rowOccupied || lowerDiagOccupied || upperDiagOccupied) {
                String conflictReason = rowOccupied ? "Same Row " + row
                        : (lowerDiagOccupied ? "Lower Diagonal (row+col=" + lowerDiagIdx + ")"
                        : "Upper Diagonal (n-1+col-row=" + upperDiagIdx + ")");

                recorder.record(new TraceEvent(
                    "conflict", 66,
                    String.format("Try col=%d, row=%d -> CONFLICT! Attacked via %s. Skip placement.", col, row, conflictReason),
                    Map.of("col", String.valueOf(col), "row", String.valueOf(row), "conflict", conflictReason),
                    "Matrix", SnapshotUtil.clone2DGrid(board),
                    new ArrayList<>(callStack), Map.of(), List.of()
                ));
            } else {
                // Place Queen
                board[row][col] = 1;
                leftRow[row] = 1;
                lowerDiagonal[lowerDiagIdx] = 1;
                upperDiagonal[upperDiagIdx] = 1;

                recorder.record(new TraceEvent(
                    "place", 67,
                    String.format("Try col=%d, row=%d -> SAFE! Place Queen Q%d at (%d, %d). Recurse to col=%d...", col, row, col + 1, row, col, col + 1),
                    Map.of("col", String.valueOf(col), "row", String.valueOf(row), "Q" + (col + 1), "(" + row + "," + col + ")"),
                    "Matrix", SnapshotUtil.clone2DGrid(board),
                    new ArrayList<>(callStack), Map.of(), List.of()
                ));

                solveRecursive(col + 1, board, solutions, leftRow, lowerDiagonal, upperDiagonal, n, recorder, callStack);

                // Backtrack
                board[row][col] = 0;
                leftRow[row] = 0;
                lowerDiagonal[lowerDiagIdx] = 0;
                upperDiagonal[upperDiagIdx] = 0;

                recorder.record(new TraceEvent(
                    "backtrack", 72,
                    String.format("BACKTRACK from col=%d: Remove Queen Q%d from (%d, %d). Restore safety hash arrays.", col + 1, col + 1, row, col),
                    Map.of("Backtrack", "Removed Queen from (" + row + "," + col + ")", "col", String.valueOf(col)),
                    "Matrix", SnapshotUtil.clone2DGrid(board),
                    new ArrayList<>(callStack), Map.of(), List.of()
                ));
            }
        }

        callStack.remove(callStack.size() - 1);
    }

    private List<String> constructBoardSolution(int[][] board, int n) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                sb.append(board[i][j] == 1 ? 'Q' : '.');
            }
            res.add(sb.toString());
        }
        return res;
    }
}
