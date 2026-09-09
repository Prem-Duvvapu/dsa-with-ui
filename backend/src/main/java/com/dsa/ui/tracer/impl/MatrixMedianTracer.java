package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Binary search over the VALUE range, not a position: for a candidate value v, counting how
 * many entries across every row are &lt;= v (one binary search per row) tells you where v
 * would rank overall. The smallest v whose rank reaches the {@code (r*c+1)/2}-th position is
 * the median — found without ever merging or sorting the matrix's r*c elements. That rank
 * formula is the lower median for an even element count, and the exact middle for an odd one,
 * so no special case is needed either way.
 */
@Component
public class MatrixMedianTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "matrix-median";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("matrix", FieldType.INT_GRID)
                        .label("Row-sorted matrix")
                        .help("Each row is sorted left to right.")
                        .constraint("maxRows", 8).constraint("maxCols", 8)
                        .values(0, 999)
                        .defaultValue(List.of(
                                List.of(12, 19, 25),
                                List.of(17, 18, 21)))
                        .build());
    }

    /** A single-element matrix: the search window starts and ends on the same value, no iterations needed. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("matrix", List.of(List.of(7)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int findMedian(int[][] matrix) {
                   int rows = matrix.length, cols = matrix[0].length;
                   int desired = (rows * cols + 1) / 2;
                   // @a init
                   int low = minOfFirstColumn(matrix), high = maxOfLastColumn(matrix);
                   while (low < high) {
                       // @a mid
                       int mid = low + (high - low) / 2;
                       int count = 0;
                       for (int[] row : matrix) count += countLessEqual(row, mid);
                       if (count < desired) {
                           // @a tooFew
                           low = mid + 1;
                       } else {
                           // @a enough
                           high = mid;
                       }
                   }
                   // @a done
                   return low;
               }

               private int countLessEqual(int[] row, int x) {
                   int low = 0, high = row.length;
                   while (low < high) {
                       int mid = (low + high) / 2;
                       if (row[mid] <= x) low = mid + 1; else high = mid;
                   }
                   return low;
               }""";
    }

    private int countLessEqual(int[] row, int x) {
        int low = 0, high = row.length;
        while (low < high) {
            int mid = (low + high) / 2;
            if (row[mid] <= x) low = mid + 1; else high = mid;
        }
        return low;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] matrix = in.getGrid("matrix");
        int rows = matrix.length;
        int cols = matrix[0].length;
        for (int[] row : matrix) {
            if (row.length != cols) {
                throw new InputValidationException(Map.of("matrix", "Every row must have the same number of columns."));
            }
        }

        int desired = (rows * cols + 1) / 2;
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int[] row : matrix) {
            low = Math.min(low, row[0]);
            high = Math.max(high, row[row.length - 1]);
        }

        emit.at("init")
                .say("The median is the %d-th smallest of %d elements (the lower of the two middle "
                        + "values when the count is even). Binary search values [%d, %d].",
                        desired, rows * cols, low, high)
                .var("desired", desired).var("low", low).var("high", high).grid(matrix).step();

        while (low < high) {
            int mid = low + (high - low) / 2;
            int count = 0;
            for (int[] row : matrix) {
                count += countLessEqual(row, mid);
            }
            emit.at("mid")
                    .say("Test value %d: %d elements across the matrix are <= %d.", mid, count, mid)
                    .var("low", low).var("high", high).var("mid", mid).var("count", count).grid(matrix).step();

            if (count < desired) {
                emit.at("tooFew")
                        .say("%d < %d — %d ranks too low. Try larger.", count, desired, mid)
                        .var("low", mid + 1).grid(matrix).step();
                low = mid + 1;
            } else {
                emit.at("enough")
                        .say("%d >= %d — %d already reaches the median rank. Try smaller.", count, desired, mid)
                        .var("high", mid).grid(matrix).step();
                high = mid;
            }
        }

        emit.at("done")
                .say("low caught up to high. The matrix median is %d.", low)
                .var("answer", low).grid(matrix).step();
    }
}
