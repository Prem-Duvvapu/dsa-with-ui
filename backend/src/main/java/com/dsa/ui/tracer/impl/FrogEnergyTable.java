package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The two-row view both frog-jump variants animate: the fixed stair heights above the
 * min-energy DP row being filled left to right.
 *
 * <p>Keeping the heights on screen is the point — the recurrence costs {@code |h[i]-h[j]|},
 * so a learner cannot check a transition without seeing the two heights it subtracts.
 */
final class FrogEnergyTable {

    private FrogEnergyTable() {}

    /**
     * @param probe      index whose energy is currently being decided, or -1
     * @param probeValue what to show in the probe cell (a running best, or "?")
     * @param reads      indices whose settled energy the current transition depends on
     * @param done       true on the closing step, when every energy is final
     */
    static DpTable of(int[] heights, int[] energy, boolean[] settled,
                      int probe, String probeValue, Set<Integer> reads, boolean done) {
        int n = heights.length;
        List<String> colLabels = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            colLabels.add(String.valueOf(i));
        }

        List<DpCell> heightRow = new ArrayList<>(n);
        List<DpCell> energyRow = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            heightRow.add(new DpCell(String.valueOf(heights[i]), "known"));

            String state;
            String value;
            if (done) {
                state = "resolved";
                value = String.valueOf(energy[i]);
            } else if (i == probe) {
                state = "probe";
                value = probeValue;
            } else if (reads.contains(i)) {
                state = "read";
                value = String.valueOf(energy[i]);
            } else if (settled[i]) {
                state = "known";
                value = String.valueOf(energy[i]);
            } else {
                state = "void";
                value = "·";
            }
            energyRow.add(new DpCell(value, state));
        }

        return new DpTable(List.of("height", "min energy"), colLabels,
                List.of(heightRow, energyRow));
    }
}
