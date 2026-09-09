package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Binary search over columns, not cells: the row-wise maximum of the middle column is
 * guaranteed to beat both of ITS row neighbors already (nothing in its own row is larger),
 * so only its left and right column neighbors decide anything. If a neighboring column
 * beats it, a peak provably exists somewhere in that direction — the search never needs to
 * examine every cell to guarantee one exists.
 */
@Component
public class FindPeakElement2DTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "find-peak-element-2d";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("matrix", FieldType.INT_GRID)
                        .label("Matrix")
                        .help("A 2D peak is strictly greater than its up/down/left/right neighbors.")
                        .constraint("maxRows", 8).constraint("maxCols", 8)
                        .values(-99, 99).distinct()
                        .defaultValue(List.of(
                                List.of(9, -22, -17),
                                List.of(47, -13, -37)))
                        .build());
    }

    /** A rising-left-to-right layout, so the search must move right before finding its peak. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("matrix", List.of(
                List.of(1, 4, 3),
                List.of(2, 5, 6),
                List.of(7, 8, 9)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] findPeakGrid(int[][] matrix) {
                   int rows = matrix.length, cols = matrix[0].length;
                   // @a init
                   int low = 0, high = cols - 1;
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       int maxRow = 0;
                       for (int r = 1; r < rows; r++) {
                           if (matrix[r][mid] > matrix[maxRow][mid]) maxRow = r;
                       }
                       int left = mid > 0 ? matrix[maxRow][mid - 1] : -1;
                       int right = mid < cols - 1 ? matrix[maxRow][mid + 1] : -1;
                       if (matrix[maxRow][mid] < left) {
                           // @a moveLeft
                           high = mid - 1;
                       } else if (matrix[maxRow][mid] < right) {
                           // @a moveRight
                           low = mid + 1;
                       } else {
                           // @a found
                           return new int[]{maxRow, mid};
                       }
                   }
                   return null;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] matrix = in.getGrid("matrix");
        int rows = matrix.length;
        int cols = matrix[0].length;
        int low = 0, high = cols - 1;

        emit.at("init")
                .say("Binary search columns [%d..%d]. Each column's own row-max already beats its "
                        + "row neighbors — only its column neighbors can still beat it.", low, high)
                .var("low", low).var("high", high).grid(matrix).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            int maxRow = 0;
            for (int r = 1; r < rows; r++) {
                if (matrix[r][mid] > matrix[maxRow][mid]) {
                    maxRow = r;
                }
            }
            int left = mid > 0 ? matrix[maxRow][mid - 1] : Integer.MIN_VALUE;
            int right = mid < cols - 1 ? matrix[maxRow][mid + 1] : Integer.MIN_VALUE;

            emit.at("mid")
                    .say("Column %d's max is %d at row %d. Left neighbor = %s, right neighbor = %s.",
                            mid, matrix[maxRow][mid], maxRow,
                            mid > 0 ? String.valueOf(left) : "none",
                            mid < cols - 1 ? String.valueOf(right) : "none")
                    .var("low", low).var("high", high).var("mid", mid).var("maxRow", maxRow)
                    .grid(matrix).step();

            if (matrix[maxRow][mid] < left) {
                emit.at("moveLeft")
                        .say("The left neighbor (%d) is bigger — a peak provably exists to the left.", left)
                        .var("high", mid - 1).grid(matrix).step();
                high = mid - 1;
            } else if (matrix[maxRow][mid] < right) {
                emit.at("moveRight")
                        .say("The right neighbor (%d) is bigger — a peak provably exists to the right.", right)
                        .var("low", mid + 1).grid(matrix).step();
                low = mid + 1;
            } else {
                emit.at("found")
                        .say("%d beats both column neighbors (and its own row) — a confirmed 2D peak at (%d, %d).",
                                matrix[maxRow][mid], maxRow, mid)
                        .var("answer", String.format("[%d, %d]", maxRow, mid)).grid(matrix).step();
                return;
            }
        }
    }
}
