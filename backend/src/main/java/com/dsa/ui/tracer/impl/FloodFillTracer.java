package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Flood Fill (LeetCode 733): recolour the pixel at (sr, sc) and every pixel reachable from
 * it through 4-directional neighbours that share its ORIGINAL colour.
 *
 * <p>The code is the owner's own accepted submission, traced as written: copy the image into
 * {@code res}, then a recursive DFS over {@code dRow}/{@code dCol}. It is already optimal -
 * O(m*n) time, each pixel painted at most once.
 *
 * <p>Two details decide whether it is correct, and both are visible. The colour compared
 * against is the start pixel's colour, passed down once as {@code initialColor} - comparing
 * against the cell after painting would stop immediately. And when the new colour already
 * equals the start colour there is nothing to do: painting would leave every neighbour still
 * "matching", so the DFS would recurse forever. That guard is a branch of the algorithm, not
 * an optimisation.
 *
 * <p>The input field keeps its published name {@code newColor} so shared links still resolve;
 * the code calls it {@code color}, and the steps use the code's names.
 */
@Component
public class FloodFillTracer implements AlgorithmTracer {

    // The code's own direction order: up, right, down, left.
    private static final int[] D_ROW = {-1, 0, 1, 0};
    private static final int[] D_COL = {0, 1, 0, -1};

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
                        .label("New colour (color)")
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
               class Solution {
                   public static int[] dRow = {-1,0,1,0};
                   public static int[] dCol = {0,1,0,-1};

                   public int[][] floodFill(int[][] image, int sr, int sc, int color) {
                       // @a init
                       int m = image.length;
                       int n = image[0].length;
                       int[][] res = new int[m][n];

                       for (int i=0;i<m;i++)
                           for (int j=0;j<n;j++)
                               res[i][j] = image[i][j];

                       // @a same
                       if (image[sr][sc] == color)
                           return image;

                       // @a start
                       dfs(sr,sc,res[sr][sc],color,res,m,n);

                       // @a done
                       return res;
                   }

                   private void dfs(int r,int c,int initialColor,int finalColor,int[][] res,int m,int n) {
                       // @a paint
                       res[r][c] = finalColor;

                       for (int i=0;i<4;i++) {
                           int newRow = r + dRow[i];
                           int newCol = c + dCol[i];

                           // @a recurse
                           if (newRow >=0 && newRow < m && newCol >= 0 && newCol < n && res[newRow][newCol] == initialColor)
                               dfs(newRow,newCol,initialColor,finalColor,res,m,n);
                       }
                   // @a return
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] image = in.getGrid("image");
        int m = image.length;
        int n = image[0].length;
        // The spec bounds sr/sc independently of the image, so a caller can name a pixel a
        // smaller image does not have. Clamping keeps that a nudged start rather than a 500.
        int sr = Math.min(in.getInt("sr"), m - 1);
        int sc = Math.min(in.getInt("sc"), n - 1);
        int color = in.getInt("newColor");

        int[][] res = new int[m][n];
        for (int i = 0; i < m; i++) {
            res[i] = image[i].clone();
        }

        emit.at("init").say("Copy the %dx%d image into res, so the caller's image is left untouched. "
                        + "The fill starts at (%d,%d), which has colour %d.", m, n, sr, sc, image[sr][sc])
                .var("sr", sr).var("sc", sc).var("color", color)
                .grid(res).step();

        if (image[sr][sc] == color) {
            emit.at("same").say("(%d,%d) is already colour %d, so there is nothing to repaint - and "
                            + "painting anyway would keep matching its own output forever. Return "
                            + "the image unchanged.", sr, sc, color)
                    .var("color", color).var("image", GridText.of(image))
                    .grid(image).step();
            return;
        }

        int initialColor = res[sr][sc];
        emit.at("start").say("(%d,%d) has colour %d, not %d. Start dfs(%d,%d) with initialColor = %d "
                        + "and finalColor = %d.", sr, sc, initialColor, color, sr, sc, initialColor, color)
                .var("initialColor", initialColor).var("finalColor", color)
                .grid(res).step();

        int[] painted = {0};
        dfs(sr, sc, initialColor, color, res, m, n, painted, emit);

        emit.at("done").say("The DFS has returned: %d pixel%s repainted from %d to %d. Return res.",
                        painted[0], Narration.s(painted[0]), initialColor, color)
                .var("painted", painted[0]).var("res", GridText.of(res))
                .grid(res).step();
    }

    private static void dfs(int r, int c, int initialColor, int finalColor, int[][] res, int m, int n,
                            int[] painted, StepEmitter emit) {
        emit.push("dfs(" + r + "," + c + ")");
        res[r][c] = finalColor;
        painted[0]++;
        emit.at("paint").say("dfs(%d,%d): paint it %d, then try its four neighbours.", r, c, finalColor)
                .var("r", r).var("c", c).var("painted", painted[0])
                .grid(res).step();

        for (int i = 0; i < 4; i++) {
            int newRow = r + D_ROW[i];
            int newCol = c + D_COL[i];
            if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n && res[newRow][newCol] == initialColor) {
                emit.at("recurse").say("(%d,%d) is on the image and still colour %d, so it is part of the "
                                + "region - recurse into dfs(%d,%d).", newRow, newCol, initialColor, newRow, newCol)
                        .var("r", r).var("c", c).var("newRow", newRow).var("newCol", newCol)
                        .grid(res).step();
                dfs(newRow, newCol, initialColor, finalColor, res, m, n, painted, emit);
            }
        }

        emit.at("return").say("dfs(%d,%d) is done: none of its neighbours is still colour %d. Return "
                        + "to the caller.", r, c, initialColor)
                .var("r", r).var("c", c)
                .grid(res).step();
        emit.pop();
    }
}
