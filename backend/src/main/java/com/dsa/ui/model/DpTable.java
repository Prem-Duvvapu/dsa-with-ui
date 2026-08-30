package com.dsa.ui.model;

import java.util.List;
import java.util.Objects;

/** A labelled rectangular DP table carried by one execution step. */
public record DpTable(List<String> rowLabels, List<String> colLabels,
                      List<List<DpCell>> cells) {

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
}
