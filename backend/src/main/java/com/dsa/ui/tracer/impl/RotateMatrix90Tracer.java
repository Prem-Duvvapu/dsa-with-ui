package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Rotate an N×N matrix 90 degrees clockwise in-place via two passes:
 * transpose (swap across the main diagonal), then reverse each row.
 *
 * <p>Why this works: transposition maps {@code (i,j)} to {@code (j,i)},
 * and reversing each row maps column {@code j} to {@code N-1-j}.
 * Combined: {@code (i,j) → (j, N-1-i)}, which is exactly a 90° clockwise
 * rotation.
 */
@Component
public class RotateMatrix90Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "rotate-matrix-90";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Square Matrix")
                        .help("Must be N×N. Rotated 90° clockwise in place.")
                        .constraint("maxRows", 8)
                        .constraint("maxCols", 8)
                        .values(-50, 50)
                        .defaultValue(List.of(
                                List.of(1, 2, 3),
                                List.of(4, 5, 6),
                                List.of(7, 8, 9)))
                        .build());
    }

    /** 4×4 matrix — different size means different step count. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(5, 1, 9, 11),
                List.of(2, 4, 8, 10),
                List.of(13, 3, 6, 7),
                List.of(15, 14, 12, 16)));
    }

    @Override
    public String annotatedCode() {
        return """
               public void rotate(int[][] matrix) {
                   // @a init
                   int n = matrix.length;
                   // Pass 1: Transpose — swap (i,j) with (j,i)
                   for (int i = 0; i < n; i++) {
                       for (int j = i + 1; j < n; j++) {
                           // @a transpose
                           int temp = matrix[i][j];
                           matrix[i][j] = matrix[j][i];
                           matrix[j][i] = temp;
                       }
                   }
                   // Pass 2: Reverse each row
                   for (int i = 0; i < n; i++) {
                       for (int j = 0; j < n / 2; j++) {
                           // @a reverse
                           int temp = matrix[i][j];
                           matrix[i][j] = matrix[i][n - 1 - j];
                           matrix[i][n - 1 - j] = temp;
                       }
                   }
                   // @a done
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] orig = in.getGrid("grid");
        int n = orig.length;
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++)
            System.arraycopy(orig[i], 0, matrix[i], 0, n);

        emit.at("init")
                .say("Rotate %d×%d matrix 90° clockwise. Step 1: transpose, Step 2: reverse rows.", n, n)
                .var("n", n)
                .grid(matrix)
                .step();

        // Pass 1: Transpose
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int temp = matrix[i][j];
                matrix[i][j] = matrix[j][i];
                matrix[j][i] = temp;
                emit.at("transpose")
                        .say("Transpose: swap (%d,%d)↔(%d,%d). Values %d ↔ %d.", i, j, j, i, matrix[j][i], matrix[i][j])
                        .var("i", i).var("j", j)
                        .grid(matrix)
                        .step();
            }
        }

        // Pass 2: Reverse each row
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n / 2; j++) {
                int temp = matrix[i][j];
                matrix[i][j] = matrix[i][n - 1 - j];
                matrix[i][n - 1 - j] = temp;
                emit.at("reverse")
                        .say("Reverse row %d: swap col %d ↔ col %d. Values %d ↔ %d.",
                                i, j, n - 1 - j, matrix[i][n - 1 - j], matrix[i][j])
                        .var("i", i).var("j", j)
                        .grid(matrix)
                        .step();
            }
        }

        emit.at("done")
                .say("Rotation complete.")
                .grid(matrix)
                .step();
    }
}
