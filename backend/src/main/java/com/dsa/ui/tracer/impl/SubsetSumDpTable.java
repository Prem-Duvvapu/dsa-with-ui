package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The reachability view shared by {@link SubsetSumEqualTargetTracer} and
 * {@link PartitionEqualSubsetSumTracer}: rows are "how many items have been considered",
 * columns are "which sum", and a cell holds {@code T}/{@code F} for whether some subset of
 * the first {@code i} items reaches that sum — never a running total, unlike
 * {@link GridDpTable}'s numeric accumulation.
 *
 * <p>There is no excluded/obstacle concept here — every cell in the rectangle is a real,
 * reachable state to decide, unlike a triangle's ragged edge or a blocked grid cell.
 */
final class SubsetSumDpTable {

    private SubsetSumDpTable() {}

    /** One cell address: {@code item} items considered, target sum {@code sum}. */
    record Coord(int item, int sum) {}

    /**
     * @param dp         reachability so far; {@code dp[i][s]} true once some subset of the
     *                   first i items is known to sum to s
     * @param settled    true once a cell's final value is known, independent of its truth
     * @param nums       the values behind each row, for the row labels (row 0 has none)
     * @param probe      the cell currently being decided, or null
     * @param probeValue what to show in the probe cell ("T", "F", or "?" before a verdict)
     * @param reads      predecessor cells this decision depends on
     * @param done       true on the closing step, when every cell is final
     */
    static DpTable of(boolean[][] dp, boolean[][] settled, int[] nums,
                      Coord probe, String probeValue, Set<Coord> reads, boolean done) {
        int rows = dp.length;
        int cols = dp[0].length;

        List<String> rowLabels = new ArrayList<>(rows);
        rowLabels.add("item 0 (none)");
        for (int i = 1; i < rows; i++) {
            rowLabels.add("item " + i + " (" + nums[i - 1] + ")");
        }
        List<String> colLabels = new ArrayList<>(cols);
        for (int s = 0; s < cols; s++) {
            colLabels.add("s=" + s);
        }

        List<List<DpCell>> cells = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            List<DpCell> row = new ArrayList<>(cols);
            for (int c = 0; c < cols; c++) {
                Coord here = new Coord(r, c);
                String value;
                String state;
                if (done) {
                    state = "resolved";
                    value = bool(dp[r][c]);
                } else if (here.equals(probe)) {
                    state = "probe";
                    value = probeValue;
                } else if (reads.contains(here)) {
                    state = "read";
                    value = bool(dp[r][c]);
                } else if (settled[r][c]) {
                    state = "known";
                    value = bool(dp[r][c]);
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

    private static String bool(boolean b) {
        return b ? "T" : "F";
    }
}
