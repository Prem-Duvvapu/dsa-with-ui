package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The two-dimensional "rows = items considered, columns = target sums" table shared by the
 * subset-sum counting recurrences ({@link CountSubsetsWithSumKTracer} and
 * {@link CountPartitionsGivenDiffTracer}): {@code dp[i][s]} holds how MANY subsets of the
 * first i items sum to exactly s.
 *
 * <p>This is deliberately the counting sibling of the boolean reachability tables traced
 * elsewhere for the same family of problems — the shape (rows = items, columns = sums) is
 * the same, but every cell here is a count, never a yes/no. Sharing only this shape-building
 * helper (not a shared run loop) mirrors how {@code SeriesDpTable} is shared by
 * {@code FrogJumpTracer} and {@code FrogJumpKDistanceTracer}: each tracer still runs and
 * narrates its own algorithm.
 */
final class SubsetCountDpTable {

    private SubsetCountDpTable() {}

    /** One cell address, used only to name which predecessors a step read. */
    record Coord(int row, int col) {}

    /** Row i (i &gt;= 1) is labelled with the value of the item it considers. */
    static List<String> rowLabels(int[] nums) {
        List<String> labels = new ArrayList<>(nums.length + 1);
        labels.add("item 0 (none)");
        for (int i = 0; i < nums.length; i++) {
            labels.add("item " + (i + 1) + " (val=" + nums[i] + ")");
        }
        return labels;
    }

    /** Column s is the target sum being asked about at that column. */
    static List<String> colLabels(int k) {
        List<String> labels = new ArrayList<>(k + 1);
        for (int s = 0; s <= k; s++) {
            labels.add("s=" + s);
        }
        return labels;
    }

    /**
     * @param probe      the cell currently being decided, or null
     * @param probeValue what to show in the probe cell (the running count, or "?")
     * @param reads      cells whose settled count the current transition depends on
     * @param done       true on the closing step, when every cell is final
     */
    static DpTable of(List<String> rowLabels, List<String> colLabels, int[][] dp,
                      boolean[][] settled, Coord probe, String probeValue, Set<Coord> reads,
                      boolean done) {
        int rows = dp.length;
        int cols = dp[0].length;

        List<List<DpCell>> cells = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            List<DpCell> row = new ArrayList<>(cols);
            for (int c = 0; c < cols; c++) {
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
