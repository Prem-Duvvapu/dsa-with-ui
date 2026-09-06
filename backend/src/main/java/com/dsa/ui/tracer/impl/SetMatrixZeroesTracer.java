package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Set Matrix Zeroes — in-place O(1) space using the first row and first
 * column as marker arrays, with a separate {@code col0} flag for column 0.
 *
 * <p>Pass 1 (top-down): for every zero found, mark its row header
 * {@code matrix[i][0]=0} and column header {@code matrix[0][j]=0}.
 * Pass 2 (bottom-up): propagate zeroes from markers, handling column 0 last.
 */
@Component
public class SetMatrixZeroesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "set-matrix-zeroes";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Matrix")
                        .help("Any M×N integer matrix. Zeroes will propagate to their entire row and column.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(-50, 50)
                        .defaultValue(List.of(
                                List.of(1, 1, 1),
                                List.of(1, 0, 1),
                                List.of(1, 1, 1)))
                        .build());
    }

    /**
     * Larger matrix with zero in column 0 to exercise the col0 flag,
     * and two zeroes in different rows/cols to show cross-propagation.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(0, 1, 2, 0),
                List.of(3, 4, 5, 2),
                List.of(1, 3, 1, 5)));
    }

    @Override
    public String annotatedCode() {
        return """
               public void setZeroes(int[][] matrix) {
                   // @a init
                   int col0 = 1, rows = matrix.length, cols = matrix[0].length;
                   // Pass 1: mark zeroes using first row and column as flags
                   for (int i = 0; i < rows; i++) {
                       if (matrix[i][0] == 0) {
                           // @a markCol0
                           col0 = 0;
                       }
                       for (int j = 1; j < cols; j++) {
                           if (matrix[i][j] == 0) {
                               // @a markHeaders
                               matrix[i][0] = 0;
                               matrix[0][j] = 0;
                           }
                       }
                   }
                   // Pass 2: propagate zeroes bottom-up
                   for (int i = rows - 1; i >= 0; i--) {
                       for (int j = cols - 1; j >= 1; j--) {
                           if (matrix[i][0] == 0 || matrix[0][j] == 0) {
                               // @a zeroCell
                               matrix[i][j] = 0;
                           }
                       }
                       if (col0 == 0) {
                           // @a zeroCol0
                           matrix[i][0] = 0;
                       }
                   }
                   // @a done
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] orig = in.getGrid("grid");
        int rows = orig.length;
        int cols = orig[0].length;
        int[][] matrix = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            System.arraycopy(orig[i], 0, matrix[i], 0, cols);

        int col0 = 1;

        emit.at("init")
                .say("Matrix is %d×%d. col0 flag = 1. Pass 1: scan for zeroes, mark headers.", rows, cols)
                .var("col0", 1).var("rows", rows).var("cols", cols)
                .grid(matrix)
                .step();

        // Pass 1: mark
        for (int i = 0; i < rows; i++) {
            if (matrix[i][0] == 0) {
                col0 = 0;
                emit.at("markCol0")
                        .say("matrix[%d][0] is 0 → set col0 flag to 0. Entire column 0 will be zeroed in pass 2.", i)
                        .var("col0", 0).var("i", i)
                        .grid(matrix)
                        .step();
            }
            for (int j = 1; j < cols; j++) {
                if (matrix[i][j] == 0) {
                    matrix[i][0] = 0;
                    matrix[0][j] = 0;
                    emit.at("markHeaders")
                            .say("Found zero at (%d,%d) → mark row header matrix[%d][0]=0 and column header matrix[0][%d]=0.",
                                    i, j, i, j)
                            .var("i", i).var("j", j)
                            .grid(matrix)
                            .step();
                }
            }
        }

        // Pass 2: propagate bottom-up
        for (int i = rows - 1; i >= 0; i--) {
            for (int j = cols - 1; j >= 1; j--) {
                if (matrix[i][0] == 0 || matrix[0][j] == 0) {
                    if (matrix[i][j] != 0) {
                        matrix[i][j] = 0;
                        emit.at("zeroCell")
                                .say("Row header matrix[%d][0]=%d or col header matrix[0][%d]=%d is 0 → zero out (%d,%d).",
                                        i, matrix[i][0], j, matrix[0][j], i, j)
                                .var("i", i).var("j", j)
                                .grid(matrix)
                                .step();
                    }
                }
            }
            if (col0 == 0) {
                if (matrix[i][0] != 0) {
                    matrix[i][0] = 0;
                    emit.at("zeroCol0")
                            .say("col0 flag is 0 → zero out matrix[%d][0].", i)
                            .var("i", i).var("col0", 0)
                            .grid(matrix)
                            .step();
                }
            }
        }

        emit.at("done")
                .say("Matrix zeroing complete.")
                .grid(matrix)
                .step();
    }
}
