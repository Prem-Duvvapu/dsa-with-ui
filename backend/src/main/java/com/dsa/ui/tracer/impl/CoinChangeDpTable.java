package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The DP table shared by the two unbounded coin-reuse recurrences ({@code minimum-coins-dp}
 * and {@code coin-change-2}): row {@code i} is "how many denominations are available so
 * far" and column {@code x} is "the amount being made". Row labels read "0 coins" then
 * "+coin=V" as each denomination joins; column labels read "x=0".."x=target".
 *
 * <p>This is deliberately <em>not</em> {@link GridDpTable}: an item's own grid supplies that
 * helper's row/column meaning, but here both axes are synthetic (denomination count, amount),
 * so the labels must be built from the problem's own coins rather than generic {@code r}/
 * {@code c} indices. There is also no "excluded" cell here — every cell is reachable in
 * principle, just possibly at value {@link #INFINITY} — so the shape is close to
 * {@link GridDpTable} but not identical.
 *
 * <p>The whole reason this differs from a 0/1-item table: taking the current denomination
 * again reads {@code dp[i][x - coin]}, the <em>same row</em>, because a coin may be reused
 * any number of times. Skipping it reads {@code dp[i-1][x]}, the row above. A step that wants
 * to show both transitions passes both coordinates in {@code reads}; the table renders both
 * as {@code read} so a learner sees "same row" and "row above" as two distinguishable cells,
 * not one blended one.
 */
final class CoinChangeDpTable {

    private CoinChangeDpTable() {}

    /** A sentinel standing in for "no combination of the coins considered so far reaches
     *  this amount". Comfortably below overflow for the caps this problem enforces
     *  (amount &le; 40), so {@code INFINITY + 1} never wraps. */
    static final int INFINITY = 1_000_000;

    /** One cell address, used only to name which predecessors a step read. */
    record Coord(int row, int col) {}

    static List<String> rowLabels(int[] coins) {
        List<String> labels = new ArrayList<>(coins.length + 1);
        labels.add("0 coins");
        for (int c : coins) {
            labels.add("+coin=" + c);
        }
        return labels;
    }

    static List<String> colLabels(int amount) {
        List<String> labels = new ArrayList<>(amount + 1);
        for (int x = 0; x <= amount; x++) {
            labels.add("x=" + x);
        }
        return labels;
    }

    /**
     * @param renderInfinity when true, a cell holding {@link #INFINITY} or more renders as
     *                       "∞" instead of the raw sentinel — used by minimum-coins-dp, not
     *                       by coin-change-2, whose cells are plain reachable counts
     * @param probe          the cell currently being decided, or null
     * @param reads          cells whose settled value the current transition depends on
     * @param done           true on the closing step, when every cell is final
     */
    static DpTable of(int[][] dp, boolean[][] settled, List<String> rowLabels,
                      List<String> colLabels, Coord probe, String probeValue,
                      Set<Coord> reads, boolean done, boolean renderInfinity) {
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
                    value = format(dp[r][c], renderInfinity);
                } else if (here.equals(probe)) {
                    state = "probe";
                    value = probeValue;
                } else if (reads.contains(here)) {
                    state = "read";
                    value = format(dp[r][c], renderInfinity);
                } else if (settled[r][c]) {
                    state = "known";
                    value = format(dp[r][c], renderInfinity);
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

    private static String format(int value, boolean renderInfinity) {
        return renderInfinity && value >= INFINITY ? "∞" : String.valueOf(value);
    }
}
