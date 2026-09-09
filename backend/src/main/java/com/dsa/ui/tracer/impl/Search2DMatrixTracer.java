package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * When every row's first value exceeds the previous row's last, the whole matrix reads as
 * one sorted list in row-major order — so a single flat binary search works, translating a
 * 1D index back to (row, col) only when it needs to compare or report.
 */
@Component
public class Search2DMatrixTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "search-2d-matrix";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("matrix", FieldType.INT_GRID)
                        .label("Globally sorted matrix")
                        .help("Each row is sorted, and every row's first value exceeds the previous row's last.")
                        .constraint("maxRows", 8).constraint("maxCols", 8)
                        .values(-999, 999)
                        .defaultValue(List.of(
                                List.of(1, 3, 5, 7),
                                List.of(10, 11, 16, 20),
                                List.of(23, 30, 34, 60)))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target")
                        .range(-999, 999)
                        .defaultValue(5)
                        .build());
    }

    /** Target absent: the search closes without ever landing on a match. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("matrix", List.of(
                List.of(1, 3, 5, 7),
                List.of(10, 11, 16, 20),
                List.of(23, 30, 34, 60)), "target", 13);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean searchMatrix(int[][] matrix, int target) {
                   int rows = matrix.length, cols = matrix[0].length;
                   // @a init
                   int low = 0, high = rows * cols - 1;
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       int r = mid / cols, c = mid % cols;
                       int val = matrix[r][c];
                       if (val == target) {
                           // @a hit
                           return true;
                       } else if (val < target) {
                           // @a less
                           low = mid + 1;
                       } else {
                           // @a greater
                           high = mid - 1;
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
        int rows = matrix.length;
        int cols = matrix[0].length;
        for (int[] row : matrix) {
            if (row.length != cols) {
                throw new InputValidationException(Map.of("matrix", "Every row must have the same number of columns."));
            }
        }

        int low = 0, high = rows * cols - 1;
        emit.at("init")
                .say("Treat the matrix as one flat sorted list of %d elements. Find %d in [%d..%d].",
                        rows * cols, target, low, high)
                .var("target", target).var("low", low).var("high", high).grid(matrix).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            int r = mid / cols, c = mid % cols;
            int val = matrix[r][c];
            emit.at("mid")
                    .say("Flat index %d = (row %d, col %d) = %d.", mid, r, c, val)
                    .var("low", low).var("high", high).var("mid", mid).var("value", val).grid(matrix).step();

            if (val == target) {
                emit.at("hit").say("%d found at (row %d, col %d).", target, r, c)
                        .var("result", true).grid(matrix).step();
                return;
            } else if (val < target) {
                emit.at("less").say("%d < %d — search the upper half of the flat index.", val, target)
                        .var("low", mid + 1).grid(matrix).step();
                low = mid + 1;
            } else {
                emit.at("greater").say("%d > %d — search the lower half of the flat index.", val, target)
                        .var("high", mid - 1).grid(matrix).step();
                high = mid - 1;
            }
        }

        emit.at("miss").say("The window closed without ever landing on %d — it is not in the matrix.", target)
                .var("result", false).grid(matrix).step();
    }
}
