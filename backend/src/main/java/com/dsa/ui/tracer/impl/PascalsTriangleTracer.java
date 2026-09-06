package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pascal's Triangle: each element is the sum of the two elements directly
 * above it. Row 0 is just [1], and every row starts and ends with 1.
 *
 * <p>Visualized on a grid canvas where each row builds from the previous
 * row's values. The triangular shape is ragged (row i has i+1 elements),
 * so we pad each row to {@code numRows} columns for the grid display.
 */
@Component
public class PascalsTriangleTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "pascals-triangle";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("numRows", FieldType.INT)
                        .label("Number of rows")
                        .help("How many rows of Pascal's Triangle to generate.")
                        .range(1, 15)
                        .defaultValue(5)
                        .build());
    }

    /** Different size produces a different trace. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("numRows", 8);
    }

    @Override
    public String annotatedCode() {
        return """
               public List<List<Integer>> generate(int numRows) {
                   // @a init
                   List<List<Integer>> res = new ArrayList<>();
                   for (int i = 0; i < numRows; i++) {
                       // @a newRow
                       List<Integer> row = new ArrayList<>();
                       for (int j = 0; j <= i; j++) {
                           if (j == 0 || j == i) {
                               // @a boundary
                               row.add(1);
                           } else {
                               // @a sum
                               row.add(res.get(i - 1).get(j - 1) + res.get(i - 1).get(j));
                           }
                       }
                       // @a addRow
                       res.add(row);
                   }
                   // @a done
                   return res;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int numRows = in.getInt("numRows");
        List<List<Integer>> res = new ArrayList<>();

        // Build a grid for display — padded to numRows x numRows
        int[][] display = new int[numRows][numRows];

        emit.at("init")
                .say("Generate %d rows of Pascal's Triangle.", numRows)
                .var("numRows", numRows)
                .grid(display)
                .step();

        for (int i = 0; i < numRows; i++) {
            List<Integer> row = new ArrayList<>();
            emit.at("newRow")
                    .say("Start building row %d (will have %d elements).", i, i + 1)
                    .var("row", i)
                    .grid(display)
                    .step();

            for (int j = 0; j <= i; j++) {
                if (j == 0 || j == i) {
                    row.add(1);
                    display[i][j] = 1;
                    emit.at("boundary")
                            .say("Row %d, col %d is a boundary (first or last) → value = 1.", i, j)
                            .var("i", i).var("j", j).var("value", 1)
                            .grid(display)
                            .step();
                } else {
                    int above1 = res.get(i - 1).get(j - 1);
                    int above2 = res.get(i - 1).get(j);
                    int val = above1 + above2;
                    row.add(val);
                    display[i][j] = val;
                    emit.at("sum")
                            .say("Row %d, col %d = row[%d][%d] + row[%d][%d] = %d + %d = %d.",
                                    i, j, i - 1, j - 1, i - 1, j, above1, above2, val)
                            .var("i", i).var("j", j).var("value", val)
                            .grid(display)
                            .step();
                }
            }

            res.add(row);
            emit.at("addRow")
                    .say("Row %d complete: %s.", i, row)
                    .var("row", row.toString())
                    .grid(display)
                    .step();
        }

        emit.at("done")
                .say("Pascal's Triangle with %d rows is complete.", numRows)
                .var("numRows", numRows)
                .grid(display)
                .step();
    }
}
