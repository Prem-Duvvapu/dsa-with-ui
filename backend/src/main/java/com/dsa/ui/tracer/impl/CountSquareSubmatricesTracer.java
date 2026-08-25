package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Count square submatrices with all 1s (LC 1277). The grid is rewritten in place:
 * cell (r,c) ends up holding the side of the largest all-ones square whose
 * bottom-right corner is exactly (r,c). The answer is the sum of every value.
 */
@Component
public class CountSquareSubmatricesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-square-submatrices";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Grid")
                        .help("Cells are rewritten in place with the largest square ending at each position.")
                        .constraint("maxRows", 8)
                        .constraint("maxCols", 8)
                        .values(0, 1)
                        .defaultValue(List.of(
                                List.of(0, 1, 1, 1),
                                List.of(1, 1, 1, 1),
                                List.of(0, 1, 1, 1)))
                        .build());
    }

    /** Checkerboard: five single-cell squares, never a 2x2. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 0, 1),
                List.of(0, 1, 0),
                List.of(1, 0, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int countSquares(int[][] matrix) {
                   // @a init
                   int total = 0;
                   for (int r = 0; r < matrix.length; r++) {
                       for (int c = 0; c < matrix[0].length; c++) {
                           // @a compute
                           if (matrix[r][c] == 1 && r > 0 && c > 0) {
                               matrix[r][c] = 1 + Math.min(matrix[r][c - 1],
                                                   Math.min(matrix[r - 1][c], matrix[r - 1][c - 1]));
                           }
                           // @a count
                           total += matrix[r][c];
                       }
                   }
                   // @a done
                   return total;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;
        int total = 0;

        emit.using("Matrix");
        emit.at("init").say("Scan row by row. A 1 in the top row or left column can only end a 1x1 square; deeper cells grow from their three neighbours.")
                .var("total", 0).grid(grid).step();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 1 && r > 0 && c > 0) {
                    int grown = 1 + Math.min(grid[r][c - 1], Math.min(grid[r - 1][c], grid[r - 1][c - 1]));
                    emit.at("compute")
                            .say("(%d,%d): neighbours left=%d, top=%d, diag=%d allow squares of side %d to end here.",
                                    r, c, grid[r][c - 1], grid[r - 1][c], grid[r - 1][c - 1], grown)
                            .var("r", r).var("c", c).var("side", grown)
                            .grid(grid).step();
                    grid[r][c] = grown;
                }
                total += grid[r][c];
                if (grid[r][c] > 0) {
                    emit.at("count")
                            .say("(%d,%d) contributes %d - a %dx%d square ends here, and every smaller one inside it was counted earlier. total=%d.",
                                    r, c, grid[r][c], grid[r][c], grid[r][c], total)
                            .var("r", r).var("c", c).var("total", total)
                            .grid(grid).step();
                }
            }
        }

        emit.at("done").say("The rewritten values sum to %d square submatrices of all 1s.", total)
                .var("total", total).grid(grid).step();
    }
}
