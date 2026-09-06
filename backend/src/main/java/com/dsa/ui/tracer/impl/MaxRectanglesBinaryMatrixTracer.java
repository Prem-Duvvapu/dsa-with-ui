package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Maximal Rectangle in a Binary Matrix (LeetCode 85). Build a histogram of heights
 * row by row, then apply largest-rectangle-in-histogram on each row. The maximum
 * across all rows is the answer.
 */
@Component
public class MaxRectanglesBinaryMatrixTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "maximum-rectangles-binary-matrix";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("matrix", FieldType.INT_GRID)
                        .label("Binary matrix")
                        .help("0s and 1s only. Find the largest rectangle containing only 1s.")
                        .constraint("minRows", 1).constraint("maxRows", 8)
                        .constraint("minCols", 1).constraint("maxCols", 8)
                        .constraint("minValue", 0).constraint("maxValue", 1)
                        .defaultValue(List.of(
                                List.of(1, 0, 1, 0, 0),
                                List.of(1, 0, 1, 1, 1),
                                List.of(1, 1, 1, 1, 1),
                                List.of(1, 0, 0, 1, 0)))
                        .build());
    }

    /** Single row: no histogram stacking possible, tests the base case. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("matrix", List.of(
                List.of(1, 1, 0),
                List.of(1, 1, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int maximalRectangle(int[][] matrix) {
                   int cols = matrix[0].length;
                   // @a init
                   int[] heights = new int[cols];
                   int maxArea = 0;
                   for (int row = 0; row < matrix.length; row++) {
                       for (int col = 0; col < cols; col++) {
                           // @a buildHeight
                           heights[col] = matrix[row][col] == 1 ? heights[col] + 1 : 0;
                       }
                       // @a histogramStart
                       Deque<Integer> stack = new ArrayDeque<>();
                       for (int i = 0; i <= cols; i++) {
                           int h = (i == cols) ? 0 : heights[i];
                           while (!stack.isEmpty() && h < heights[stack.peek()]) {
                               int top = stack.pop();
                               int width = stack.isEmpty() ? i : i - stack.peek() - 1;
                               int area = heights[top] * width;
                               // @a popRect
                               if (area > maxArea) maxArea = area;
                           }
                           // @a pushCol
                           stack.push(i);
                       }
                   }
                   // @a done
                   return maxArea;
               }""";
    }

    private List<ArrayElement> heightState(int[] heights, Deque<Integer> stack, int current) {
        List<ArrayElement> s = new ArrayList<>(heights.length);
        for (int i = 0; i < heights.length; i++) {
            String st = i == current ? "current" : stack.contains(i) ? "sorted" : "default";
            s.add(new ArrayElement(i, heights[i], st));
        }
        return s;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] matrix = in.getGrid("matrix");
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[] heights = new int[cols];
        int maxArea = 0;

        emit.at("init")
                .say("Binary matrix %dx%d. Build histograms row by row, apply largest-rectangle on each.",
                        rows, cols)
                .var("rows", rows).var("cols", cols)
                .grid(matrix).step();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                heights[col] = matrix[row][col] == 1 ? heights[col] + 1 : 0;
            }
            emit.at("buildHeight")
                    .say("Row %d: heights updated to %s.", row, java.util.Arrays.toString(heights))
                    .var("row", row)
                    .arrayState(heightState(heights, new ArrayDeque<>(), -1)).grid(matrix).step();

            // Largest rectangle in histogram
            Deque<Integer> stack = new ArrayDeque<>();
            emit.at("histogramStart")
                    .say("Run histogram algorithm on row %d heights.", row)
                    .var("row", row)
                    .arrayState(heightState(heights, stack, -1)).stack(stack).step();

            for (int i = 0; i <= cols; i++) {
                int h = (i == cols) ? 0 : heights[i];

                while (!stack.isEmpty() && h < heights[stack.peek()]) {
                    int top = stack.pop();
                    int width = stack.isEmpty() ? i : i - stack.peek() - 1;
                    int area = heights[top] * width;
                    if (area > maxArea) maxArea = area;
                    emit.at("popRect")
                            .say("Pop index %d (h=%d), width=%d, area=%d. maxArea=%d.",
                                    top, heights[top], width, area, maxArea)
                            .var("popped", top).var("area", area).var("maxArea", maxArea)
                            .arrayState(heightState(heights, stack, i < cols ? i : -1)).stack(stack).step();
                }

                if (i < cols) {
                    stack.push(i);
                    emit.at("pushCol")
                            .say("Push column %d (height %d).", i, heights[i])
                            .var("i", i)
                            .arrayState(heightState(heights, stack, i)).stack(stack).step();
                }
            }
        }

        emit.at("done")
                .say("All rows processed. Largest rectangle of all 1s has area %d.", maxArea)
                .var("answer", maxArea)
                .grid(matrix).step();
    }
}
