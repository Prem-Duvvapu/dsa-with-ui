package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.AlgorithmTracer;
import com.dsa.ui.tracer.FieldType;
import com.dsa.ui.tracer.InputField;

import java.util.ArrayList;
import java.util.List;

/** Shared presentation helpers for the string-topic tracer batch. */
abstract class StringTracerSupport implements AlgorithmTracer {

    @Override
    public DsType dsType() {
        return DsType.STRING;
    }

    protected static InputField text(String name, String label, String value, int min, int max) {
        return InputField.of(name, FieldType.STRING)
                .label(label)
                .length(min, max)
                .defaultValue(value)
                .build();
    }

    protected static InputField patternedText(
            String name, String label, String value, int min, int max, String pattern, String hint) {
        return InputField.of(name, FieldType.STRING)
                .label(label)
                .length(min, max)
                .constraint("pattern", pattern)
                .constraint("patternHint", hint)
                .defaultValue(value)
                .build();
    }

    protected static DpTable intervalTable(
            String source, long[][] values, boolean[][] settled, int activeRow, int activeCol, boolean done) {
        List<String> labels = source.codePoints()
                .mapToObj(cp -> new String(Character.toChars(cp)))
                .toList();
        List<List<DpCell>> cells = new ArrayList<>(values.length);
        for (int row = 0; row < values.length; row++) {
            List<DpCell> rendered = new ArrayList<>(values[row].length);
            for (int col = 0; col < values[row].length; col++) {
                String state = done ? "resolved"
                        : row == activeRow && col == activeCol ? "probe"
                        : settled[row][col] ? "known" : "void";
                rendered.add(new DpCell(settled[row][col] || done
                        ? String.valueOf(values[row][col]) : "·", state));
            }
            cells.add(rendered);
        }
        return new DpTable(labels, labels, cells);
    }
}
