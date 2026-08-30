package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Max rectangle of all 1s (LC 85). Row by row, heights[] accumulates consecutive
 * 1s upward; each row then reduces to "largest rectangle in a histogram", solved
 * with a monotonic increasing stack. A sentinel column of height 0 flushes the stack.
 */
@Component
public class MaxRectangleAreaTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "max-rectangle-area-all-ones";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Grid")
                        .help("Each row turns the problem into largest-rectangle-in-histogram over running heights.")
                        .constraint("maxRows", 6)
                        .constraint("maxCols", 8)
                        .values(0, 1)
                        .defaultValue(List.of(
                                List.of(1, 0, 1, 0, 0),
                                List.of(1, 0, 1, 1, 1),
                                List.of(1, 1, 1, 1, 1),
                                List.of(1, 0, 0, 1, 0)))
                        .build());
    }

    /** A thin all-ones tower: heights only grow and the best rectangle is the whole column. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1),
                List.of(1),
                List.of(1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int maximalRectangle(int[][] matrix) {
                   // @a init
                   int cols = matrix[0].length;
                   int[] heights = new int[cols];
                   int best = 0;
                   for (int r = 0; r < matrix.length; r++) {
                       for (int c = 0; c < cols; c++) {
                           // @a heights
                           heights[c] = matrix[r][c] == 0 ? 0 : heights[c] + 1;
                       }
                       Deque<Integer> stack = new ArrayDeque<>();
                       for (int i = 0; i <= cols; i++) {
                           int bar = (i == cols) ? 0 : heights[i];
                           while (!stack.isEmpty() && heights[stack.peek()] >= bar) {
                               // @a pop
                               int top = stack.pop();
                               int left = stack.isEmpty() ? -1 : stack.peek();
                               best = Math.max(best, heights[top] * (i - left - 1));
                           }
                           // @a push
                           if (i < cols) stack.push(i);
                       }
                   }
                   // @a done
                   return best;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;
        int[] heights = new int[cols];
        int best = 0;

        emit.at("init").say("%dx%d grid. heights[c] will count consecutive 1s ending at the current row - each row is then a histogram problem.",
                        rows, cols)
                .var("best", 0).grid(grid).step();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                heights[c] = grid[r][c] == 1 ? heights[c] + 1 : 0;
                emit.at("heights")
                        .say("row %d, col %d: grid=%d so heights[%d]=%d%s.", r, c, grid[r][c], c, heights[c],
                                grid[r][c] == 1 ? " (grew)" : " (zeroed - any rectangle using this column breaks here)")
                        .var("r", r).var("c", c).var("best", best)
                        .array(heights, c).step();
            }

            Deque<Integer> stack = new ArrayDeque<>();
            for (int i = 0; i <= cols; i++) {
                boolean sentinel = i == cols;
                int bar = sentinel ? 0 : heights[i];
                while (!stack.isEmpty() && (sentinel || heights[stack.peek()] >= bar)) {
                    int topIdx = stack.pop();
                    int h = heights[topIdx];
                    int left = stack.isEmpty() ? -1 : stack.peek();
                    int width = i - left - 1;
                    int area = h * width;
                    boolean improves = area > best;
                    best = Math.max(best, area);
                    emit.at("pop")
                            .say("row %d: bar %d at col %d ends taller-or-equal bars. Pop col %d (h=%d), width %d−(%d)−1=%d, area=%d%s%s.",
                                    r, bar, i, topIdx, h, i, left, width, area,
                                    improves ? " - new best" : "",
                                    sentinel ? " - sentinel flushes what remains" : "")
                            .var("r", r).var("poppedCol", topIdx).var("h", h)
                            .var("width", width).var("area", area).var("best", best)
                            .var("stack", stackString(stack))
                            .array(heights, topIdx, Math.min(i, cols - 1)).step();
                }
                if (!sentinel) {
                    stack.push(i);
                    emit.at("push")
                            .say("row %d: push col %d (h=%d); stack stays increasing bottom→top: %s.",
                                    r, i, heights[i], stackString(stack))
                            .var("r", r).var("i", i).var("best", best)
                            .var("stack", stackString(stack))
                            .array(heights, i).step();
                }
            }
        }

        emit.at("done").say("Largest all-1s rectangle in the grid has area %d.", best)
                .var("best", best).grid(grid).step();
    }

    private static String stackString(Deque<Integer> stack) {
        List<Integer> asc = new ArrayList<>(stack);
        java.util.Collections.reverse(asc);
        return asc.toString();
    }
}
