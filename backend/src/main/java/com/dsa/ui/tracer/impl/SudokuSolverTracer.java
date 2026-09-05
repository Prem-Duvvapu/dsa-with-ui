package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Backtracking over the first empty cell found, in order. A digit that repeats in its
 * row, column, or 3x3 box is skipped outright; one that does not gets placed and the
 * search recurses. Only when that deeper search reports failure does the digit come back
 * out - the placement was locally legal, but the board it led to could not be completed.
 *
 * <p>The puzzle travels as an 81-character row-major string rather than a 9x9 grid field:
 * a real Sudoku board is fixed at 9x9 by the box rule, and a grid-typed field would be
 * silently reshaped by the input-scaling check every tracer is put through.
 */
@Component
public class SudokuSolverTracer implements AlgorithmTracer {

    private static final String DEFAULT_PUZZLE =
            "..4678912" + ".72195348" + "198342567" + "859761423" + "426853791"
                    + "713924856" + "961537284" + "287419635" + "345286179";

    private static final String ALTERNATE_PUZZLE =
            "53467.912" + "6.2195.48" + "198342567" + "859761.23" + "4.685379."
                    + "7.3.24856" + "9615.7284" + "28741.6.5" + "34528.179";

    @Override
    public String id() {
        return "sudoku-solver";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("puzzle", FieldType.STRING)
                        .label("Puzzle (81 characters, row-major, '.' for empty)")
                        .help("Exactly 81 characters: digits 1-9, or '.' for an empty cell.")
                        .length(81, 81)
                        .constraint("pattern", "[1-9.]{81}")
                        .constraint("patternHint", "Use only digits 1-9 and '.', exactly 81 characters long.")
                        .defaultValue(DEFAULT_PUZZLE)
                        .build());
    }

    /** Twelve blanks arranged so one wrong-but-locally-valid guess forces a real backtrack,
     *  not just elimination by inspection - while staying small enough to finish inside budget. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("puzzle", ALTERNATE_PUZZLE);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean solveSudoku(int[][] board) {
                   return solve(board);
               }

               private boolean solve(int[][] board) {
                   int[] empty = findEmpty(board);
                   if (empty == null) {
                       // @a done
                       return true;
                   }
                   int row = empty[0], col = empty[1];
                   for (int digit = 1; digit <= 9; digit++) {
                       if (!isValid(board, row, col, digit)) {
                           // @a conflict
                           continue;
                       }
                       board[row][col] = digit;
                       // @a place
                       if (solve(board)) {
                           // @a propagate
                           return true;
                       }
                       board[row][col] = 0;
                       // @a backtrack
                   }
                   // @a deadEnd
                   return false;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] board = parse(in.getString("puzzle"));
        solve(board, emit);
    }

    private boolean solve(int[][] board, StepEmitter emit) {
        int[] empty = findEmpty(board);
        if (empty == null) {
            emit.at("done").say("No empty cell remains - the puzzle is solved.")
                    .grid(copy(board)).step();
            return true;
        }

        int row = empty[0];
        int col = empty[1];

        for (int digit = 1; digit <= 9; digit++) {
            if (!isValid(board, row, col, digit)) {
                emit.at("conflict")
                        .say("%d at (%d,%d) repeats in its row, column, or 3x3 box - skip it.",
                                digit, row, col)
                        .var("row", row).var("col", col).var("digit", digit)
                        .grid(copy(board)).step();
                continue;
            }

            board[row][col] = digit;
            emit.at("place")
                    .say("%d at (%d,%d) breaks no row, column, or box rule - place it and move to the next empty cell.",
                            digit, row, col)
                    .var("row", row).var("col", col).var("digit", digit)
                    .grid(copy(board)).step();

            if (solve(board, emit)) {
                emit.at("propagate")
                        .say("The rest of the board completed with %d still at (%d,%d) - keep it and report success upward.",
                                digit, row, col)
                        .var("row", row).var("col", col).grid(copy(board)).step();
                return true;
            }

            board[row][col] = 0;
            emit.at("backtrack")
                    .say("%d at (%d,%d) led to a dead end deeper in the board - undo it and try the next digit.",
                            digit, row, col)
                    .var("row", row).var("col", col).grid(copy(board)).step();
        }

        emit.at("deadEnd")
                .say("No digit 1-9 fits (%d,%d) given the current board - this branch cannot be completed.",
                        row, col)
                .var("row", row).var("col", col).grid(copy(board)).step();
        return false;
    }

    private int[] findEmpty(int[][] board) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    private boolean isValid(int[][] board, int row, int col, int digit) {
        for (int c = 0; c < 9; c++) {
            if (board[row][c] == digit) {
                return false;
            }
        }
        for (int r = 0; r < 9; r++) {
            if (board[r][col] == digit) {
                return false;
            }
        }
        int boxRow = (row / 3) * 3;
        int boxCol = (col / 3) * 3;
        for (int r = boxRow; r < boxRow + 3; r++) {
            for (int c = boxCol; c < boxCol + 3; c++) {
                if (board[r][c] == digit) {
                    return false;
                }
            }
        }
        return true;
    }

    private int[][] parse(String puzzle) {
        int[][] board = new int[9][9];
        for (int i = 0; i < 81; i++) {
            char ch = puzzle.charAt(i);
            board[i / 9][i % 9] = ch == '.' ? 0 : ch - '0';
        }
        return board;
    }

    private int[][] copy(int[][] board) {
        int[][] out = new int[9][9];
        for (int r = 0; r < 9; r++) {
            out[r] = board[r].clone();
        }
        return out;
    }
}
