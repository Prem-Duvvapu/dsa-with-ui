package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The table shared by string-alignment recurrences: row {@code i} is the length-{@code i}
 * prefix of one input string, column {@code j} the length-{@code j} prefix of the other, so
 * rows and columns are labelled by the strings' own characters (plus an empty-prefix row and
 * column 0) rather than plain indices - the same "the table looks like what a learner would
 * draw on paper" goal {@link GridDpTable} serves for grid-shaped recurrences.
 *
 * <p>Cell values are passed in already stringified, since edit-distance's table holds ints
 * and wildcard-matching's holds booleans - the two have nothing in common but the shape.
 */
final class StringDpTable {

    /** One cell address, used only to name which predecessors a step read. */
    record Coord(int row, int col) {}

    private StringDpTable() {}

    static DpTable of(String[][] values, boolean[][] settled, String rowSource, String colSource,
                      Coord probe, String probeValue, Set<Coord> reads, boolean done) {
        int rows = values.length;
        int cols = values[0].length;

        List<String> rowLabels = new ArrayList<>(rows);
        rowLabels.add("∅");
        for (int i = 0; i < rowSource.length(); i++) {
            rowLabels.add(String.valueOf(rowSource.charAt(i)));
        }
        List<String> colLabels = new ArrayList<>(cols);
        colLabels.add("∅");
        for (int j = 0; j < colSource.length(); j++) {
            colLabels.add(String.valueOf(colSource.charAt(j)));
        }

        List<List<DpCell>> cells = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            List<DpCell> row = new ArrayList<>(cols);
            for (int c = 0; c < cols; c++) {
                Coord here = new Coord(r, c);
                String state;
                String value;
                if (done) {
                    state = "resolved";
                    value = values[r][c];
                } else if (here.equals(probe)) {
                    state = "probe";
                    value = probeValue;
                } else if (reads.contains(here)) {
                    state = "read";
                    value = values[r][c];
                } else if (settled[r][c]) {
                    state = "known";
                    value = values[r][c];
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
