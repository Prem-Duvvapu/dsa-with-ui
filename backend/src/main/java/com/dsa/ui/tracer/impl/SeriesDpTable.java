package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The two-row view every one-dimensional recurrence here animates: the fixed input series
 * above the DP row being filled left to right.
 *
 * <p>Keeping the input on screen is the point. These recurrences cost something derived
 * from the input — a height difference, a house value — so a learner cannot check a
 * transition without seeing the numbers it was computed from.
 */
final class SeriesDpTable {

    private SeriesDpTable() {}

    /**
     * @param probe      index whose DP value is currently being decided, or -1
     * @param probeValue what to show in the probe cell (a running best, or "?")
     * @param reads      indices whose settled energy the current transition depends on
     * @param done       true on the closing step, when every DP value is final
     */
    static DpTable of(String inputLabel, int[] inputs, String dpLabel, int[] dp,
                      boolean[] settled, int probe, String probeValue, Set<Integer> reads,
                      boolean done) {
        int n = inputs.length;
        List<String> colLabels = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            colLabels.add(String.valueOf(i));
        }

        List<DpCell> inputRow = new ArrayList<>(n);
        List<DpCell> dpRow = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            inputRow.add(new DpCell(String.valueOf(inputs[i]), "known"));

            String state;
            String value;
            if (done) {
                state = "resolved";
                value = String.valueOf(dp[i]);
            } else if (i == probe) {
                state = "probe";
                value = probeValue;
            } else if (reads.contains(i)) {
                state = "read";
                value = String.valueOf(dp[i]);
            } else if (settled[i]) {
                state = "known";
                value = String.valueOf(dp[i]);
            } else {
                state = "void";
                value = "·";
            }
            dpRow.add(new DpCell(value, state));
        }

        return new DpTable(List.of(inputLabel, dpLabel), colLabels, List.of(inputRow, dpRow));
    }
}
