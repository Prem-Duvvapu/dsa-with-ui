package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Unique paths on an open grid: the first genuinely two-dimensional recurrence traced here.
 * Every 1-D recurrence so far has been a single row; this one's rows and columns are the
 * problem's own rows and columns, and a cell reads two neighbours in different directions —
 * above and to the left — rather than two positions along one axis.
 *
 * <p>The edges are traced through the same "combine" reasoning as the interior, not hardcoded
 * to 1: dp[i][j] adds a neighbour above and a neighbour to the left, and along an edge one of
 * those two simply does not exist. Seeing the edge cells filled by the general rule, rather
 * than stamped, is what makes {@link UniquePaths2Tracer}'s generalisation to
 * missing neighbours read as the same idea rather than a new one.
 */
@Component
public class GridUniquePathsTracer implements AlgorithmTracer {

    private static final String FORMULA = "dp[i][j] = dp[i-1][j] + dp[i][j-1]";

    @Override
    public String id() {
        return "grid-unique-paths";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("rows", FieldType.INT)
                        .label("Rows")
                        .help("The robot starts top-left and may only move right or down.")
                        .range(1, 8)
                        .defaultValue(3)
                        .build(),
                InputField.of("cols", FieldType.INT)
                        .label("Columns")
                        .range(1, 8)
                        .defaultValue(3)
                        .build());
    }

    /** Taller and narrower, so the shape of the table — not just its size — differs. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("rows", 4, "cols", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public int uniquePaths(int m, int n) {
                   // @a init
                   int[][] dp = new int[m][n];
                   for (int i = 0; i < m; i++) {
                       for (int j = 0; j < n; j++) {
                           if (i == 0 || j == 0) {
                               // @a base
                               dp[i][j] = 1;
                           } else {
                               // @a combine
                               dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
                           }
                       }
                   }
                   // @a done
                   return dp[m - 1][n - 1];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int rows = in.getInt("rows");
        int cols = in.getInt("cols");
        int[][] dp = new int[rows][cols];
        boolean[][] settled = new boolean[rows][cols];

        emit.at("init")
                .say("A %d by %d grid. dp[i][j] will hold the number of distinct paths from "
                        + "the top-left corner to cell (i,j), moving only right or down.",
                        rows, cols)
                .var("rows", rows).var("cols", cols)
                .dpTable(table(dp, settled, null, "?", Set.of(), false)).step();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                GridDpTable.Coord here = new GridDpTable.Coord(i, j);
                if (i == 0 || j == 0) {
                    dp[i][j] = 1;
                    String reason = (i == 0 && j == 0)
                            ? "the starting cell needs no moves at all, so there is exactly "
                                    + "one way to be here"
                            : i == 0
                                    ? "the top row is reachable only by moving right the "
                                            + "whole way, so there is exactly one path"
                                    : "the left column is reachable only by moving down the "
                                            + "whole way, so there is exactly one path";
                    emit.at("base")
                            .say("Cell (%d,%d): %s. dp[%d][%d] = 1.", i, j, reason, i, j)
                            .var("row", i).var("col", j)
                            .dpTable(table(dp, settled, here, "1", Set.of(), false)).step();
                } else {
                    int above = dp[i - 1][j];
                    int left = dp[i][j - 1];
                    int total = above + left;
                    String substitution = String.format(
                            "dp[%d][%d] = dp[%d][%d] + dp[%d][%d] = %d + %d = %d",
                            i, j, i - 1, j, i, j - 1, above, left, total);
                    emit.at("combine")
                            .say("Cell (%d,%d): every path arrives either from above, (%d,%d) "
                                    + "with %s, or from the left, (%d,%d) with %s — no path can "
                                    + "do both, so they add. dp[%d][%d] = %d + %d = %d.",
                                    i, j, i - 1, j, pathCount(above), i, j - 1, pathCount(left),
                                    i, j, above, left, total)
                            .var("row", i).var("col", j)
                            .var("above", above).var("left", left).var("dp[i][j]", total)
                            .dpTable(table(dp, settled, here, String.valueOf(total),
                                    Set.of(new GridDpTable.Coord(i - 1, j),
                                            new GridDpTable.Coord(i, j - 1)), false)
                                    .withFormula(FORMULA, substitution)).step();
                    dp[i][j] = total;
                }
                settled[i][j] = true;
            }
        }

        emit.at("done")
                .say("dp[%d][%d] = %d is the answer: every path to the far corner, and no "
                        + "path counted more than once because each one arrives from exactly "
                        + "one direction.", rows - 1, cols - 1, dp[rows - 1][cols - 1])
                .var("answer", dp[rows - 1][cols - 1])
                .dpTable(table(dp, settled, null, "?", Set.of(), true)).step();
    }

    private static String pathCount(int n) {
        return n == 1 ? "1 path" : n + " paths";
    }

    private static com.dsa.ui.model.DpTable table(int[][] dp, boolean[][] settled,
                                                  GridDpTable.Coord probe, String probeValue,
                                                  Set<GridDpTable.Coord> reads, boolean done) {
        return GridDpTable.of(dp, settled, null, probe, probeValue, reads, done);
    }
}
