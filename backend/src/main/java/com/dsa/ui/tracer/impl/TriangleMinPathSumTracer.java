package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Triangle minimum path sum: a bottom-up recurrence where a cell reads two *children* below
 * it rather than two predecessors above — the first tracer here where dependency flows
 * upward through the table instead of down or across.
 *
 * <p>The wire format has no ragged-row shape, so the triangle is carried as a square
 * {@code n x n} grid and only its lower-left half — row i, columns 0..i — is ever read or
 * rendered as live; the rest is permanently excluded, the same "never holds a value"
 * treatment {@link UniquePaths2Tracer} gives an obstacle. Filling runs from the last row
 * upward so every read lands on an already-settled cell, mirroring how a learner would
 * actually work the problem by hand.
 */
@Component
public class TriangleMinPathSumTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "triangle-min-path-sum";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("triangle", FieldType.INT_GRID)
                        .label("Triangle")
                        .help("Row i has i+1 real entries; only columns 0..i of each row are "
                                + "used, submitted as a square grid — the rest is ignored.")
                        .constraint("maxRows", 8)
                        .constraint("maxCols", 8)
                        .values(-100, 100)
                        .defaultValue(List.of(
                                List.of(2, 0, 0, 0),
                                List.of(3, 4, 0, 0),
                                List.of(6, 5, 7, 0),
                                List.of(4, 1, 8, 3)))
                        .build());
    }

    /** Fewer rows and negative values, so a right-hand child wins where the default's did not. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("triangle", List.of(
                List.of(-1, 0, 0),
                List.of(2, 3, 0),
                List.of(1, -1, -3)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int minimumTotal(int[][] triangle) {
                   // A triangle's row i needs i+1 real entries, so its true depth cannot
                   // exceed the width of the rectangular grid it is submitted as.
                   int n = Math.min(triangle.length, triangle[0].length);
                   // @a init
                   int[][] dp = new int[n][n];
                   for (int j = 0; j < n; j++) {
                       // @a base
                       dp[n - 1][j] = triangle[n - 1][j];
                   }
                   for (int i = n - 2; i >= 0; i--) {
                       for (int j = 0; j <= i; j++) {
                           // @a combine
                           dp[i][j] = triangle[i][j] + Math.min(dp[i + 1][j], dp[i + 1][j + 1]);
                       }
                   }
                   // @a done
                   return dp[0][0];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] triangle = in.getGrid("triangle");
        int n = Math.min(triangle.length, triangle[0].length);
        int[][] dp = new int[n][n];
        boolean[][] settled = new boolean[n][n];
        boolean[][] outside = new boolean[n][n];
        for (int row = 0; row < n; row++) {
            for (int col = row + 1; col < n; col++) {
                outside[row][col] = true;
            }
        }

        emit.at("init")
                .say("A triangle with %d rows, row i holding i+1 real entries. dp[i][j] will "
                        + "hold the cheapest path from (i,j) down to the base — filled from "
                        + "the bottom row upward, since that is the direction every read "
                        + "points.", n)
                .var("n", n)
                .dpTable(table(dp, settled, outside, null, "?", Set.of(), false)).step();

        for (int j = 0; j < n; j++) {
            dp[n - 1][j] = triangle[n - 1][j];
            settled[n - 1][j] = true;
            emit.at("base")
                    .say("Cell (%d,%d) is on the base row: there is nowhere further down to "
                            + "go, so the cheapest path from here is just its own value, %d.",
                            n - 1, j, triangle[n - 1][j])
                    .var("row", n - 1).var("col", j)
                    .dpTable(table(dp, settled, outside, new GridDpTable.Coord(n - 1, j),
                            String.valueOf(triangle[n - 1][j]), Set.of(), false)).step();
        }

        for (int i = n - 2; i >= 0; i--) {
            for (int j = 0; j <= i; j++) {
                int leftChild = dp[i + 1][j];
                int rightChild = dp[i + 1][j + 1];
                boolean leftWins = leftChild <= rightChild;
                int chosen = triangle[i][j] + Math.min(leftChild, rightChild);

                emit.at("combine")
                        .say("Cell (%d,%d): the path continues to whichever child is "
                                + "cheaper — (%d,%d) at %d, or (%d,%d) at %d. %s wins, so "
                                + "dp[%d][%d] = %d + %d = %d.",
                                i, j, i + 1, j, leftChild, i + 1, j + 1, rightChild,
                                leftWins ? "the left child" : "the right child",
                                i, j, triangle[i][j], Math.min(leftChild, rightChild), chosen)
                        .var("row", i).var("col", j)
                        .var("left", leftChild).var("right", rightChild)
                        .var("dp[i][j]", chosen)
                        .dpTable(table(dp, settled, outside, new GridDpTable.Coord(i, j),
                                String.valueOf(chosen),
                                Set.of(new GridDpTable.Coord(i + 1, j),
                                        new GridDpTable.Coord(i + 1, j + 1)), false)).step();

                dp[i][j] = chosen;
                settled[i][j] = true;
            }
        }

        emit.at("done")
                .say("dp[0][0] = %d is the cheapest path from the apex to the base. Every cell "
                        + "below it was settled first, so the apex's own decision only ever "
                        + "compared two already-final numbers.", dp[0][0])
                .var("answer", dp[0][0])
                .dpTable(table(dp, settled, outside, null, "?", Set.of(), true)).step();
    }

    private static com.dsa.ui.model.DpTable table(int[][] dp, boolean[][] settled,
                                                  boolean[][] outside, GridDpTable.Coord probe,
                                                  String probeValue,
                                                  Set<GridDpTable.Coord> reads, boolean done) {
        return GridDpTable.of(dp, settled, outside, probe, probeValue, reads, done);
    }
}
