package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Rows and columns are each sorted independently — no global order like
 * {@link Search2DMatrixTracer} can rely on — so the search starts at the top-right corner,
 * the one cell where "too big" and "too small" point in unambiguous, opposite directions:
 * a bigger value means step left, a smaller one means step down. Every step discards a
 * whole row or column, giving O(rows + cols) instead of a nested search.
 */
@Component
public class Search2DMatrix2Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "search-2d-matrix-2";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("matrix", FieldType.INT_GRID)
                        .label("Row- and column-sorted matrix")
                        .help("Each row is sorted left to right, each column top to bottom — independently.")
                        .constraint("maxRows", 8).constraint("maxCols", 8)
                        .values(-999, 999)
                        .defaultValue(List.of(
                                List.of(1, 4, 7, 11),
                                List.of(2, 5, 8, 12),
                                List.of(3, 6, 9, 16),
                                List.of(10, 13, 14, 17)))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-999, 999)
                        .defaultValue(0)
                        .build());
    }

    /** Target present: the staircase moves down at least once before landing on it. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("matrix", List.of(
                List.of(1, 4, 7, 11),
                List.of(2, 5, 8, 12),
                List.of(3, 6, 9, 16),
                List.of(10, 13, 14, 17)), "target", 5);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean searchMatrix(int[][] matrix, int target) {
                   // @a init
                   int row = 0, col = matrix[0].length - 1;
                   while (row < matrix.length && col >= 0) {
                       // @a probe
                       int val = matrix[row][col];
                       if (val == target) {
                           // @a hit
                           return true;
                       } else if (val > target) {
                           // @a moveLeft
                           col--;
                       } else {
                           // @a moveDown
                           row++;
                       }
                   }
                   // @a miss
                   return false;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] matrix = in.getGrid("matrix");
        int target = in.getInt("target");
        int row = 0, col = matrix[0].length - 1;

        emit.at("init")
                .say("Start at the top-right corner (row 0, col %d) — the one cell where "
                        + "'too big' and 'too small' each rule out a whole line.", col)
                .var("target", target).var("row", row).var("col", col).grid(matrix).step();

        while (row < matrix.length && col >= 0) {
            int val = matrix[row][col];
            emit.at("probe")
                    .say("(row %d, col %d) = %d.", row, col, val)
                    .var("row", row).var("col", col).var("value", val).grid(matrix).step();

            if (val == target) {
                emit.at("hit").say("%d found at (row %d, col %d).", target, row, col)
                        .var("result", true).grid(matrix).step();
                return;
            } else if (val > target) {
                emit.at("moveLeft")
                        .say("%d > %d — everything below in this column is even bigger. Step left.", val, target)
                        .var("col", col - 1).grid(matrix).step();
                col--;
            } else {
                emit.at("moveDown")
                        .say("%d < %d — everything left in this row is even smaller. Step down.", val, target)
                        .var("row", row + 1).grid(matrix).step();
                row++;
            }
        }

        emit.at("miss").say("Walked off the matrix without ever landing on %d — it is absent.", target)
                .var("result", false).grid(matrix).step();
    }
}
