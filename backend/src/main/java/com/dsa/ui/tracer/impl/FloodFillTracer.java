package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Flood Fill (LeetCode 733): recolour the pixel at (sr, sc) and every pixel reachable from
 * it through 4-directional neighbours that share its ORIGINAL colour.
 *
 * <p>Two details decide whether an implementation is correct, and both are visible here.
 * The colour compared against is the start pixel's colour read once up front - reading
 * {@code image[r][c]} again after painting would compare against the new colour and stop
 * immediately. And when the new colour already equals the start colour there is nothing to
 * do: painting would leave every neighbour still "matching", so the fill would revisit
 * cells forever. That guard is a branch of the algorithm, not an optimisation.
 */
@Component
public class FloodFillTracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "flood-fill";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("image", FieldType.INT_GRID)
                        .label("Image")
                        .help("Each cell is a pixel colour.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(0, 9)
                        // LeetCode 733 example 1.
                        .defaultValue(List.of(
                                List.of(1, 1, 1),
                                List.of(1, 1, 0),
                                List.of(1, 0, 1)))
                        .build(),
                InputField.of("sr", FieldType.INT)
                        .label("Start row")
                        .help("Row of the starting pixel. Clamped to the image if it falls outside.")
                        .range(0, 9)
                        .defaultValue(1)
                        .build(),
                InputField.of("sc", FieldType.INT)
                        .label("Start column")
                        .help("Column of the starting pixel. Clamped to the image if it falls outside.")
                        .range(0, 9)
                        .defaultValue(1)
                        .build(),
                InputField.of("newColor", FieldType.INT)
                        .label("New colour")
                        .help("The colour painted over the connected region.")
                        .range(0, 9)
                        .defaultValue(2)
                        .build());
    }

    /**
     * LeetCode 733 example 2: a uniform image repainted with the colour it already has, on
     * a differently shaped canvas and starting from the corner. Nothing is painted at all -
     * the guard branch the default never touches.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "image", List.of(
                        List.of(0, 0, 0),
                        List.of(0, 0, 0)),
                "sr", 0,
                "sc", 0,
                "newColor", 0);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[][] floodFill(int[][] image, int sr, int sc, int newColor) {
                   // @a start
                   int startColor = image[sr][sc];
                   if (startColor == newColor) {
                       // @a alreadyPainted
                       return image;
                   }

                   Deque<int[]> stack = new ArrayDeque<>();
                   stack.push(new int[]{sr, sc});
                   image[sr][sc] = newColor;

                   while (!stack.isEmpty()) {
                       // @a pop
                       int[] pixel = stack.pop();
                       for (int[] d : DIRECTIONS) {
                           int r = pixel[0] + d[0], c = pixel[1] + d[1];
                           if (r < 0 || r >= image.length || c < 0 || c >= image[0].length
                                   || image[r][c] != startColor) {
                               // @a reject
                               continue;
                           }
                           // @a paint
                           image[r][c] = newColor;
                           stack.push(new int[]{r, c});
                       }
                   }
                   // @a done
                   return image;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] image = in.getGrid("image");
        int rows = image.length;
        int cols = image[0].length;
        // The spec bounds sr/sc independently of the image, so a caller can name a pixel a
        // smaller image does not have. Clamping keeps that a nudged start rather than a 500.
        int sr = Math.min(in.getInt("sr"), rows - 1);
        int sc = Math.min(in.getInt("sc"), cols - 1);
        int newColor = in.getInt("newColor");
        int startColor = image[sr][sc];

        emit.at("start").say("Start at (%d,%d). Its colour is %d, and every pixel connected to it "
                        + "through colour %d becomes %d.", sr, sc, startColor, startColor, newColor)
                .var("start", "(" + sr + "," + sc + ")")
                .var("startColor", startColor).var("newColor", newColor)
                .grid(image).step();

        if (startColor == newColor) {
            emit.at("alreadyPainted").say("The new colour %d is the colour the region already has, "
                            + "so there is nothing to repaint - and painting anyway would keep "
                            + "matching its own output forever.", newColor)
                    .var("painted", 0).grid(image).step();
            return;
        }

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{sr, sc});
        image[sr][sc] = newColor;
        int painted = 1;

        while (!stack.isEmpty()) {
            int[] pixel = stack.pop();

            emit.at("pop").say("Take (%d,%d) off the stack and look at its four neighbours.",
                            pixel[0], pixel[1])
                    .var("pixel", "(" + pixel[0] + "," + pixel[1] + ")").var("painted", painted)
                    .grid(image).stack(pixels(stack)).step();

            for (int[] d : DIRECTIONS) {
                int r = pixel[0] + d[0];
                int c = pixel[1] + d[1];
                if (r < 0 || r >= rows || c < 0 || c >= cols || image[r][c] != startColor) {
                    emit.at("reject").say("(%d,%d) is off the image or is not colour %d - the region "
                                    + "stops here.", r, c, startColor)
                            .var("pixel", "(" + r + "," + c + ")")
                            .grid(image).stack(pixels(stack)).step();
                    continue;
                }
                image[r][c] = newColor;
                painted++;
                stack.push(new int[]{r, c});

                emit.at("paint").say("(%d,%d) still carries the original colour %d, so it belongs to "
                                + "the region. Paint it %d.", r, c, startColor, newColor)
                        .var("pixel", "(" + r + "," + c + ")").var("painted", painted)
                        .grid(image).stack(pixels(stack)).step();
            }
        }

        emit.at("done").say("The stack is empty: %d pixel(s) repainted from %d to %d.",
                        painted, startColor, newColor)
                .var("painted", painted).grid(image).step();
    }

    private static List<String> pixels(Deque<int[]> stack) {
        List<String> out = new ArrayList<>();
        for (int[] pixel : stack) {
            out.add("(" + pixel[0] + "," + pixel[1] + ")");
        }
        return out;
    }
}
