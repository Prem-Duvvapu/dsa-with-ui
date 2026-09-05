package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cherry Pickup II (LeetCode 1463): two robots start on row 0, one at column 0 and one at
 * column cols-1, and both move down one row per step, each independently choosing column-1,
 * column, or column+1. State is genuinely three-dimensional - (row, col1, col2) - which does
 * not fit this project's {@link DpTable} model (rowLabels x colLabels x one cell each).
 *
 * <p>The way out: the recurrence for row r only ever reads row r+1's already-finished table,
 * never anything further down. So instead of one 3D table, this renders one col1-by-col2
 * <b>slice</b> at a time - the table for the row currently being computed - and lets the row
 * index driving which slice is on screen advance from the last row up to row 0. Each slice is
 * filled cell by cell exactly like every other {@code DpTable} tracer; the only difference is
 * that a "read" here comes from the PREVIOUS slice (already retired from view), so it is named
 * in the narration rather than highlighted in the visible table.
 */
@Component
public class NinjaAndHisFriendsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "ninja-and-his-friends";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Chocolate Grid")
                        .help("Robot 1 starts at (row 0, col 0), robot 2 at (row 0, col "
                                + "cols-1). Each row, both move to column-1, column, or "
                                + "column+1. A cell both robots occupy is collected once.")
                        .constraint("maxRows", 8)
                        .constraint("maxCols", 6)
                        .values(0, 100)
                        .defaultValue(List.of(
                                List.of(3, 1, 1),
                                List.of(2, 5, 1),
                                List.of(1, 5, 5),
                                List.of(2, 1, 1)))
                        .build());
    }

    /**
     * A differently-shaped grid (3 rows x 4 cols instead of 4 x 3) with strictly increasing
     * values, so every cell, every row transition and the final answer (42, verified by
     * exhaustive brute force over every path pair) all differ from the defaults.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 2, 3, 4),
                List.of(5, 6, 7, 8),
                List.of(9, 10, 11, 12)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int cherryPickup(int[][] grid) {
                   int rows = grid.length, cols = grid[0].length;
                   int[][] dp = new int[cols][cols];

                   for (int c1 = 0; c1 < cols; c1++) {
                       for (int c2 = 0; c2 < cols; c2++) {
                           // @a base
                           dp[c1][c2] = grid[rows - 1][c1] + (c1 == c2 ? 0 : grid[rows - 1][c2]);
                       }
                   }

                   for (int row = rows - 2; row >= 0; row--) {
                       // @a rowStart
                       int[][] next = new int[cols][cols];
                       for (int c1 = 0; c1 < cols; c1++) {
                           for (int c2 = 0; c2 < cols; c2++) {
                               int best = 0;
                               for (int d1 = -1; d1 <= 1; d1++) {
                                   for (int d2 = -1; d2 <= 1; d2++) {
                                       int n1 = c1 + d1, n2 = c2 + d2;
                                       if (n1 >= 0 && n1 < cols && n2 >= 0 && n2 < cols) {
                                           best = Math.max(best, dp[n1][n2]);
                                       }
                                   }
                               }
                               int collected = grid[row][c1] + (c1 == c2 ? 0 : grid[row][c2]);
                               // @a transition
                               next[c1][c2] = collected + best;
                           }
                       }
                       // @a rowDone
                       dp = next;
                   }

                   // @a done
                   return dp[0][cols - 1];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;
        int lastRow = rows - 1;

        int[][] dp = new int[cols][cols];
        boolean[][] filled = new boolean[cols][cols];

        for (int c1 = 0; c1 < cols; c1++) {
            for (int c2 = 0; c2 < cols; c2++) {
                int collected = grid[lastRow][c1] + (c1 == c2 ? 0 : grid[lastRow][c2]);
                String note = c1 == c2
                        ? "both robots share column " + c1 + ", so its chocolates count once"
                        : "robot 1 is at column " + c1 + " and robot 2 at column " + c2
                                + " - different cells, so both count";

                emit.at("base")
                        .say("Showing row %d (the last row): no further moves are possible, "
                                + "so %s. dp[%d][%d] = %d.",
                                lastRow, note, c1, c2, collected)
                        .var("row", lastRow).var("col1", c1).var("col2", c2)
                        .var("value", collected)
                        .dpTable(table(dp, filled, cols, c1, c2, String.valueOf(collected), false))
                        .step();

                dp[c1][c2] = collected;
                filled[c1][c2] = true;
            }
        }

        for (int row = lastRow - 1; row >= 0; row--) {
            int[][] next = new int[cols][cols];
            boolean[][] nextFilled = new boolean[cols][cols];

            emit.at("rowStart")
                    .say("Row %d's table (just shown) is finished and retired. Now showing a "
                            + "fresh table for row %d - every cell here will read only from "
                            + "row %d's finished values, never anything further down.",
                            row + 1, row, row + 1)
                    .var("row", row)
                    .dpTable(table(next, nextFilled, cols, -1, -1, "?", false)).step();

            for (int c1 = 0; c1 < cols; c1++) {
                for (int c2 = 0; c2 < cols; c2++) {
                    int best = 0;
                    for (int d1 = -1; d1 <= 1; d1++) {
                        int n1 = c1 + d1;
                        if (n1 < 0 || n1 >= cols) continue;
                        for (int d2 = -1; d2 <= 1; d2++) {
                            int n2 = c2 + d2;
                            if (n2 < 0 || n2 >= cols) continue;
                            best = Math.max(best, dp[n1][n2]);
                        }
                    }
                    int collected = grid[row][c1] + (c1 == c2 ? 0 : grid[row][c2]);
                    int value = collected + best;

                    emit.at("transition")
                            .say("Row %d, dp[%d][%d]: collect %d here, plus the best %d "
                                    + "reachable from row %d's table over every combination of "
                                    + "column-1/column/column+1 for both robots -> %d + %d = %d.",
                                    row, c1, c2, collected, best, row + 1, collected, best, value)
                            .var("row", row).var("col1", c1).var("col2", c2)
                            .var("collected", collected).var("bestBelow", best).var("value", value)
                            .dpTable(table(next, nextFilled, cols, c1, c2, String.valueOf(value), false))
                            .step();

                    next[c1][c2] = value;
                    nextFilled[c1][c2] = true;
                }
            }

            emit.at("rowDone")
                    .say("Row %d's table is complete. %s", row,
                            row == 0
                                    ? "This is row 0: dp[0][" + (cols - 1) + "] is the answer."
                                    : "It becomes the 'below' reference for row " + (row - 1) + ".")
                    .var("row", row)
                    .dpTable(table(next, nextFilled, cols, -1, -1, "?", false)).step();

            dp = next;
            filled = nextFilled;
        }

        int answer = dp[0][cols - 1];
        emit.at("done")
                .say("Robot 1 starts at (row 0, col 0), robot 2 at (row 0, col %d). "
                        + "dp[0][%d] = %d chocolates collected in total.",
                        cols - 1, cols - 1, answer)
                .var("answer", answer)
                .dpTable(table(dp, filled, cols, -1, -1, "?", true)).step();
    }

    private static DpTable table(int[][] values, boolean[][] filled, int cols,
                                 int probeC1, int probeC2, String probeValue, boolean done) {
        List<String> rowLabels = new ArrayList<>(cols);
        List<String> colLabels = new ArrayList<>(cols);
        for (int c = 0; c < cols; c++) {
            rowLabels.add("c1=" + c);
            colLabels.add("c2=" + c);
        }

        List<List<DpCell>> cells = new ArrayList<>(cols);
        for (int c1 = 0; c1 < cols; c1++) {
            List<DpCell> row = new ArrayList<>(cols);
            for (int c2 = 0; c2 < cols; c2++) {
                String state;
                String value;
                if (done) {
                    state = "resolved";
                    value = String.valueOf(values[c1][c2]);
                } else if (c1 == probeC1 && c2 == probeC2) {
                    state = "probe";
                    value = probeValue;
                } else if (filled[c1][c2]) {
                    state = "known";
                    value = String.valueOf(values[c1][c2]);
                } else {
                    state = "void";
                    value = "·";
                }
                row.add(new DpCell(value, state));
            }
            cells.add(row);
        }
        return new DpTable(rowLabels, colLabels, cells);
    }
}
