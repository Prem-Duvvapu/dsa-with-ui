package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unique paths with obstacles: the same two-neighbour recurrence as
 * {@link GridUniquePathsTracer}, generalised so a missing neighbour defaults to 0 instead of
 * being stamped to 1 along the edges.
 *
 * <p>That generalisation is the entire lesson. Once "no neighbour above" and "blocked
 * neighbour above" both contribute 0, the open-grid edges stop needing their own rule — only
 * the single true base case, an unblocked (0,0), is special. An obstacle is traced as
 * permanently excluded rather than merely zero, so it never looks like an ordinary computed
 * cell that happened to work out to zero.
 */
@Component
public class UniquePaths2Tracer implements AlgorithmTracer {

    private static final String FORMULA = "dp[i][j] = dp[i-1][j] + dp[i][j-1]";

    @Override
    public String id() {
        return "unique-paths-2";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Grid")
                        .help("1 is an obstacle. The robot starts top-left and may only move "
                                + "right or down.")
                        .constraint("maxRows", 8)
                        .constraint("maxCols", 8)
                        .values(0, 1)
                        .defaultValue(List.of(
                                List.of(0, 0, 0),
                                List.of(0, 1, 0),
                                List.of(0, 0, 0)))
                        .build());
    }

    /** An obstacle on the starting cell, so every path is blocked and the answer is 0. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 0),
                List.of(0, 0)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int uniquePathsWithObstacles(int[][] grid) {
                   int m = grid.length, n = grid[0].length;
                   // @a init
                   int[][] dp = new int[m][n];
                   for (int i = 0; i < m; i++) {
                       for (int j = 0; j < n; j++) {
                           if (grid[i][j] == 1) {
                               // @a blocked
                               dp[i][j] = 0;
                           } else if (i == 0 && j == 0) {
                               // @a base
                               dp[i][j] = 1;
                           } else {
                               int fromAbove = i > 0 ? dp[i - 1][j] : 0;
                               int fromLeft = j > 0 ? dp[i][j - 1] : 0;
                               // @a combine
                               dp[i][j] = fromAbove + fromLeft;
                           }
                       }
                   }
                   // @a done
                   return dp[m - 1][n - 1];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;
        int[][] dp = new int[rows][cols];
        boolean[][] settled = new boolean[rows][cols];
        boolean[][] blocked = new boolean[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                blocked[i][j] = grid[i][j] == 1;
            }
        }

        emit.at("init")
                .say("A %d by %d grid, obstacles marked in black. dp[i][j] will hold the "
                        + "number of distinct paths from the top-left corner to (i,j) — 0 "
                        + "wherever an obstacle makes the cell unreachable.", rows, cols)
                .var("rows", rows).var("cols", cols)
                .dpTable(table(dp, settled, blocked, null, "?", Set.of(), false)).step();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                GridDpTable.Coord here = new GridDpTable.Coord(i, j);
                if (blocked[i][j]) {
                    settled[i][j] = true;
                    emit.at("blocked")
                            .say("Cell (%d,%d) is an obstacle. No path may pass through it, so "
                                    + "it is excluded entirely rather than merely holding 0.",
                                    i, j)
                            .var("row", i).var("col", j)
                            .dpTable(table(dp, settled, blocked, null, "?", Set.of(), false)).step();
                    continue;
                }

                if (i == 0 && j == 0) {
                    dp[i][j] = 1;
                    emit.at("base")
                            .say("(0,0) is open and it is the start: no moves have been made, "
                                    + "so there is exactly one way to be here.")
                            .var("row", 0).var("col", 0)
                            .dpTable(table(dp, settled, blocked, here, "1", Set.of(), false)).step();
                } else {
                    boolean hasAbove = i > 0;
                    boolean hasLeft = j > 0;
                    int fromAbove = hasAbove ? dp[i - 1][j] : 0;
                    int fromLeft = hasLeft ? dp[i][j - 1] : 0;
                    int total = fromAbove + fromLeft;

                    String aboveClause = !hasAbove ? "no cell above"
                            : blocked[i - 1][j] ? "an obstacle above" : "%d from above".formatted(fromAbove);
                    String leftClause = !hasLeft ? "no cell to the left"
                            : blocked[i][j - 1] ? "an obstacle to the left" : "%d from the left".formatted(fromLeft);

                    Set<GridDpTable.Coord> reads = new java.util.HashSet<>();
                    if (hasAbove) reads.add(new GridDpTable.Coord(i - 1, j));
                    if (hasLeft) reads.add(new GridDpTable.Coord(i, j - 1));

                    com.dsa.ui.model.DpTable combineTable = table(dp, settled, blocked, here,
                            String.valueOf(total), reads, false);
                    if (hasAbove && hasLeft) {
                        String substitution = String.format(
                                "dp[%d][%d] = dp[%d][%d] + dp[%d][%d] = %d + %d = %d",
                                i, j, i - 1, j, i, j - 1, fromAbove, fromLeft, total);
                        combineTable = combineTable.withFormula(FORMULA, substitution);
                    }

                    emit.at("combine")
                            .say("Cell (%d,%d): %s and %s, each contributing 0 when it does not "
                                    + "exist or is blocked. dp[%d][%d] = %d + %d = %d.",
                                    i, j, aboveClause, leftClause, i, j, fromAbove, fromLeft,
                                    total)
                            .var("row", i).var("col", j)
                            .var("fromAbove", fromAbove).var("fromLeft", fromLeft)
                            .var("dp[i][j]", total)
                            .dpTable(combineTable).step();
                    dp[i][j] = total;
                }
                settled[i][j] = true;
            }
        }

        emit.at("done")
                .say("dp[%d][%d] = %d. %s", rows - 1, cols - 1, dp[rows - 1][cols - 1],
                        dp[rows - 1][cols - 1] == 0
                                ? "Every route to the far corner is cut off by an obstacle."
                                : "That count already has every obstacle's effect folded in — "
                                        + "no separate check is needed at the end.")
                .var("answer", dp[rows - 1][cols - 1])
                .dpTable(table(dp, settled, blocked, null, "?", Set.of(), true)).step();
    }

    private static com.dsa.ui.model.DpTable table(int[][] dp, boolean[][] settled,
                                                  boolean[][] blocked, GridDpTable.Coord probe,
                                                  String probeValue,
                                                  Set<GridDpTable.Coord> reads, boolean done) {
        return GridDpTable.of(dp, settled, blocked, probe, probeValue, reads, done);
    }
}
