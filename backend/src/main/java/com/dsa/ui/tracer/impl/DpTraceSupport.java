package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.tracer.FieldType;
import com.dsa.ui.tracer.InputField;
import com.dsa.ui.tracer.InputValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared, immutable snapshots for the final family of caller-driven DP tracers. */
final class DpTraceSupport {

    /**
     * The largest DP table a step-by-step trace can show within the response budget
     * (InputSpec.DEFAULT_MAX_BYTES). Every step carries the whole table and the number of steps
     * grows with the number of cells, so the payload grows with cells squared: measured on the
     * live catalogue, tables up to 15 x 15 finish, and larger ones stopped at 2 MB BEFORE the
     * answer step - the reader saw a run cut short with no result.
     */
    static final int MAX_TABLE_CELLS = 225;

    /**
     * Refuses, as a field error, an input whose table would not fit, instead of starting a run
     * that cannot reach its answer. The limit is on the table, not on each field, so a few large
     * values or many small ones are both fine.
     */
    static void requireTableFits(int rows, int cols, String field, String advice) {
        long cells = (long) rows * cols;
        if (cells > MAX_TABLE_CELLS) {
            throw new InputValidationException(Map.of(field, String.format(
                    "This input needs a %d × %d table (%d cells); a step-by-step trace can show at most %d. %s",
                    rows, cols, cells, MAX_TABLE_CELLS, advice)));
        }
    }

    record Coord(int row, int col) {}

    private DpTraceSupport() {}

    static InputField intArray(String name, String label, List<Integer> defaults, int minLength,
                               int maxLength, int minValue, int maxValue) {
        return InputField.of(name, FieldType.INT_ARRAY).label(label)
                .length(minLength, maxLength).values(minValue, maxValue)
                .defaultValue(defaults).build();
    }

    static InputField text(String name, String label, String defaults, int maxLength) {
        return InputField.of(name, FieldType.STRING).label(label)
                .length(1, maxLength).constraint("pattern", "[a-z]+")
                .constraint("patternHint", "Lowercase letters a-z only.")
                .defaultValue(defaults).build();
    }

    static DpTable table(long[][] values, boolean[][] known, Coord probe, Set<Coord> reads,
                         List<String> rowLabels, List<String> colLabels, boolean done,
                         String formula, String substitution) {
        List<List<DpCell>> cells = new ArrayList<>(values.length);
        for (int r = 0; r < values.length; r++) {
            List<DpCell> row = new ArrayList<>(values[r].length);
            for (int c = 0; c < values[r].length; c++) {
                Coord here = new Coord(r, c);
                String state = done ? "resolved"
                        : here.equals(probe) ? "probe"
                        : reads.contains(here) ? "read"
                        : known[r][c] ? "known" : "void";
                row.add(new DpCell(known[r][c] || done || here.equals(probe)
                        ? String.valueOf(values[r][c]) : "·", state));
            }
            cells.add(row);
        }
        return new DpTable(rowLabels, colLabels, cells, formula, substitution);
    }

    static DpTable table(long[][] values, boolean[][] known, Coord probe, Set<Coord> reads,
                         boolean done, String formula, String substitution) {
        return table(values, known, probe, reads, labels("i", values.length),
                labels("j", values[0].length), done, formula, substitution);
    }

    static List<String> labels(String prefix, int size) {
        List<String> labels = new ArrayList<>(size);
        for (int i = 0; i < size; i++) labels.add(prefix + i);
        return labels;
    }

    static List<String> chars(String value) {
        List<String> labels = new ArrayList<>(value.length() + 1);
        labels.add("∅");
        for (int i = 0; i < value.length(); i++) labels.add(String.valueOf(value.charAt(i)));
        return labels;
    }

    static List<Integer> list(int... values) {
        return java.util.Arrays.stream(values).boxed().toList();
    }
}
