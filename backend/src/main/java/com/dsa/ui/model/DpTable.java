package com.dsa.ui.model;

import java.util.List;
import java.util.Objects;

/**
 * A labelled rectangular DP table carried by one execution step.
 *
 * <p>{@code formula} and {@code substitution} are optional (null when a tracer hasn't
 * been updated to supply them — see PROMPT-F-visual-fidelity.md design D3):
 * {@code formula} is the symbolic recurrence, constant for the problem
 * ({@code "ways[i] = ways[i-1] + ways[i-2]"}); {@code substitution} is that same
 * recurrence with THIS step's actual numbers plugged in
 * ({@code "ways[4] = ways[3] + ways[2] = 3 + 2 = 5"}). Binding code, formula and table
 * in one glance is the highest-value, lowest-cost teaching addition this canvas can make
 * — the tracer already computes every one of those numbers to build the table itself.
 */
public record DpTable(List<String> rowLabels, List<String> colLabels,
                      List<List<DpCell>> cells, String formula, String substitution) {

    public DpTable {
        rowLabels = List.copyOf(Objects.requireNonNull(rowLabels, "rowLabels"));
        colLabels = List.copyOf(Objects.requireNonNull(colLabels, "colLabels"));
        cells = Objects.requireNonNull(cells, "cells").stream().map(List::copyOf).toList();

        if (cells.size() != rowLabels.size()) {
            throw new IllegalArgumentException(
                    "DP table has " + cells.size() + " rows but " + rowLabels.size()
                            + " row labels");
        }
        for (int row = 0; row < cells.size(); row++) {
            if (cells.get(row).size() != colLabels.size()) {
                throw new IllegalArgumentException(
                        "DP table row " + row + " has " + cells.get(row).size()
                                + " cells but " + colLabels.size() + " column labels");
            }
        }
    }

    /** Existing call sites (five shared table-shape helpers, 21 tracers) are unaffected. */
    public DpTable(List<String> rowLabels, List<String> colLabels, List<List<DpCell>> cells) {
        this(rowLabels, colLabels, cells, null, null);
    }

    /** Same table, with the recurrence attached. Records are immutable — this is a copy. */
    public DpTable withFormula(String formula, String substitution) {
        return new DpTable(rowLabels, colLabels, cells,
                Objects.requireNonNull(formula, "formula"),
                Objects.requireNonNull(substitution, "substitution"));
    }
}
