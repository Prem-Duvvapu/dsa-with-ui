package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spiral traversal of an M×N matrix using four boundary pointers.
 *
 * <p>The bounding box shrinks inward after each side of the spiral is
 * completed: top++ after traversing right, right-- after traversing down,
 * bottom-- after traversing left, left++ after traversing up. The inner
 * guards ({@code top <= bottom} before left-traversal, {@code left <= right}
 * before up-traversal) prevent double-counting on single-row or single-column
 * remainders.
 */
@Component
public class SpiralMatrixTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "spiral-matrix";
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
                        .help("M×N matrix to traverse in spiral order.")
                        .constraint("maxRows", 8)
                        .constraint("maxCols", 8)
                        .values(-50, 50)
                        .defaultValue(List.of(
                                List.of(1, 2, 3, 4),
                                List.of(5, 6, 7, 8),
                                List.of(9, 10, 11, 12)))
                        .build());
    }

    /** 2×3 matrix — narrower, so the up-traversal guard fires differently. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 2, 3),
                List.of(4, 5, 6)));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> spiralOrder(int[][] matrix) {
                   // @a init
                   List<Integer> ans = new ArrayList<>();
                   int n = matrix.length, m = matrix[0].length;
                   int top = 0, left = 0, bottom = n - 1, right = m - 1;
               
                   while (top <= bottom && left <= right) {
                       // @a goRight
                       for (int i = left; i <= right; i++) ans.add(matrix[top][i]);
                       top++;
                       // @a goDown
                       for (int i = top; i <= bottom; i++) ans.add(matrix[i][right]);
                       right--;
                       if (top <= bottom) {
                           // @a goLeft
                           for (int i = right; i >= left; i--) ans.add(matrix[bottom][i]);
                           bottom--;
                       }
                       if (left <= right) {
                           // @a goUp
                           for (int i = bottom; i >= top; i--) ans.add(matrix[i][left]);
                           left++;
                       }
                   }
                   // @a done
                   return ans;
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

        List<Integer> ans = new ArrayList<>();
        int top = 0, left = 0, bottom = rows - 1, right = cols - 1;

        emit.at("init")
                .say("Spiral traverse %d×%d matrix. Boundaries: top=%d, bottom=%d, left=%d, right=%d.",
                        rows, cols, top, bottom, left, right)
                .var("top", top).var("bottom", bottom).var("left", left).var("right", right)
                .grid(matrix)
                .step();

        while (top <= bottom && left <= right) {
            // Go right
            List<Integer> picked = new ArrayList<>();
            for (int i = left; i <= right; i++) {
                ans.add(matrix[top][i]);
                picked.add(matrix[top][i]);
            }
            emit.at("goRight")
                    .say("→ Right along row %d from col %d to %d: %s. Then top++ → %d.",
                            top, left, right, picked, top + 1)
                    .var("top", top).var("ans", ans.toString())
                    .grid(matrix)
                    .step();
            top++;

            // Go down
            picked.clear();
            for (int i = top; i <= bottom; i++) {
                ans.add(matrix[i][right]);
                picked.add(matrix[i][right]);
            }
            emit.at("goDown")
                    .say("↓ Down along col %d from row %d to %d: %s. Then right-- → %d.",
                            right, top, bottom, picked, right - 1)
                    .var("right", right).var("ans", ans.toString())
                    .grid(matrix)
                    .step();
            right--;

            if (top <= bottom) {
                picked.clear();
                for (int i = right; i >= left; i--) {
                    ans.add(matrix[bottom][i]);
                    picked.add(matrix[bottom][i]);
                }
                emit.at("goLeft")
                        .say("← Left along row %d from col %d to %d: %s. Then bottom-- → %d.",
                                bottom, right, left, picked, bottom - 1)
                        .var("bottom", bottom).var("ans", ans.toString())
                        .grid(matrix)
                        .step();
                bottom--;
            }

            if (left <= right) {
                picked.clear();
                for (int i = bottom; i >= top; i--) {
                    ans.add(matrix[i][left]);
                    picked.add(matrix[i][left]);
                }
                emit.at("goUp")
                        .say("↑ Up along col %d from row %d to %d: %s. Then left++ → %d.",
                                left, bottom, top, picked, left + 1)
                        .var("left", left).var("ans", ans.toString())
                        .grid(matrix)
                        .step();
                left++;
            }
        }

        emit.at("done")
                .say("Spiral order: %s (%d elements).", ans, ans.size())
                .var("result", ans.toString())
                .grid(matrix)
                .step();
    }
}
