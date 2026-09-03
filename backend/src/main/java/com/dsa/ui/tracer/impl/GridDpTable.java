package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The genuine two-dimensional view every grid-shaped recurrence here animates: rows and
 * columns of the DP table map directly onto rows and columns of the problem's own grid, so
 * the table on screen has the same shape a learner would draw on paper.
 *
 * <p>A cell can be permanently excluded — an obstacle, a triangle's ragged edge — in which
 * case it always renders void regardless of {@code settled}, on every step including the
 * closing one.
 */
final class GridDpTable {

    private GridDpTable() {}

    /** One cell address, used only to name which predecessors a step read. */
    record Coord(int row, int col) {}

    /**
     * @param excluded   true for a cell that can never hold a value (an obstacle, a
     *                   ragged triangle edge); always void, independent of settled/done
     * @param probe      the cell currently being decided, or null
     * @param probeValue what to show in the probe cell (a running best, or "?")
     * @param reads      cells whose settled value the current transition depends on
     * @param done       true on the closing step, when every included cell is final
     */
    static DpTable of(int[][] dp, boolean[][] settled, boolean[][] excluded,
                      Coord probe, String probeValue, Set<Coord> reads, boolean done) {
        int rows = dp.length;
        int cols = dp[0].length;

        List<String> rowLabels = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            rowLabels.add("r" + r);
        }
        List<String> colLabels = new ArrayList<>(cols);
        for (int c = 0; c < cols; c++) {
            colLabels.add("c" + c);
        }

        List<List<DpCell>> cells = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            List<DpCell> row = new ArrayList<>(cols);
            for (int c = 0; c < cols; c++) {
                if (excluded != null && excluded[r][c]) {
                    row.add(new DpCell("#", "void"));
                    continue;
                }

                Coord here = new Coord(r, c);
                String value;
                String state;
                if (done) {
                    state = "resolved";
                    value = String.valueOf(dp[r][c]);
                } else if (here.equals(probe)) {
                    state = "probe";
                    value = probeValue;
                } else if (reads.contains(here)) {
                    state = "read";
                    value = String.valueOf(dp[r][c]);
                } else if (settled[r][c]) {
                    state = "known";
                    value = String.valueOf(dp[r][c]);
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
