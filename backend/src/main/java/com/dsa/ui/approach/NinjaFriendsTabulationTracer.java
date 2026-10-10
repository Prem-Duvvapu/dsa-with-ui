package com.dsa.ui.approach;

import com.dsa.ui.model.*;
import com.dsa.ui.tracer.*;
import java.util.*;

/** Full 3D counterpart to the recursive memo. Render one explicitly labelled row slice at a time. */
final class NinjaFriendsTabulationTracer implements AlgorithmTracer {
    public String id() { return "ninja-and-his-friends"; }
    public DsType dsType() { return DsType.DP_TABLE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/" + id() + "/tabulation.java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("grid", FieldType.INT_GRID).label("Chocolate Grid")
                .constraint("maxRows", 8).constraint("maxCols", 6).values(0, 100)
                .defaultValue(List.of(List.of(3, 1, 1), List.of(2, 5, 1), List.of(1, 5, 5), List.of(2, 1, 1)))
                .help("Full dp[row][col1][col2], filled from the last row upward. The diagram shows a labelled row slice, not the entire 3D table.").build());
    }
    public Map<String, Object> alternateInput() { return new NinjaFriendsRecursiveTracer(true).alternateInput(); }
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length, cols = grid[0].length;
        Integer[][][] dp = new Integer[rows][cols][cols];
        for (int c1 = 0; c1 < cols; c1++) for (int c2 = 0; c2 < cols; c2++) {
            dp[rows - 1][c1][c2] = grid[rows - 1][c1] + (c1 == c2 ? 0 : grid[rows - 1][c2]);
            emit.at("base").say("Last row %d, columns (%d,%d): collect %d%s.", rows - 1, c1, c2, dp[rows - 1][c1][c2],
                    c1 == c2 ? "; the shared cell counts once" : " from the two different cells")
                    .var("row", rows - 1).var("col1", c1).var("col2", c2).var("value", dp[rows - 1][c1][c2])
                    .dpTable(table(dp, rows - 1, c1, c2)).step();
        }
        for (int row = rows - 2; row >= 0; row--) {
            emit.at("rowStart").say("Fill row %d's slice. All dependencies are in completed row %d; other row slices remain stored.", row, row + 1)
                    .var("row", row).dpTable(table(dp, row, -1, -1)).step();
            for (int c1 = 0; c1 < cols; c1++) for (int c2 = 0; c2 < cols; c2++) {
                int best = -1, best1 = -1, best2 = -1;
                for (int d1 = -1; d1 <= 1; d1++) for (int d2 = -1; d2 <= 1; d2++) {
                    int n1 = c1 + d1, n2 = c2 + d2;
                    if (n1 >= 0 && n1 < cols && n2 >= 0 && n2 < cols && dp[row + 1][n1][n2] > best) {
                        best = dp[row + 1][n1][n2]; best1 = n1; best2 = n2;
                    }
                }
                int collected = grid[row][c1] + (c1 == c2 ? 0 : grid[row][c2]);
                dp[row][c1][c2] = collected + best;
                emit.at("fill").say("dp[%d][%d][%d]: collect %d plus best legal next state dp[%d][%d][%d]=%d, giving %d. The read is in the next-row slice, not this slice.",
                        row, c1, c2, collected, row + 1, best1, best2, best, dp[row][c1][c2])
                        .var("row", row).var("col1", c1).var("col2", c2).var("value", dp[row][c1][c2])
                        .var("bestNextCol1", best1).var("bestNextCol2", best2).var("bestBelow", best)
                        .dpTable(table(dp, row, c1, c2).withFormula("dp[row][col1][col2] = collected once + max(legal next-row states)",
                                collected + " + dp[" + (row + 1) + "][" + best1 + "][" + best2 + "] = " + dp[row][c1][c2])).step();
            }
        }
        emit.at("done").say("Read the full 3D table at dp[0][0][%d]: %d chocolates.", cols - 1, dp[0][0][cols - 1])
                .var("answer", dp[0][0][cols - 1]).var("row", 0).dpTable(table(dp, 0, 0, cols - 1)).step();
    }
    private static DpTable table(Integer[][][] dp, int layer, int r, int c) {
        List<List<DpCell>> cells = new ArrayList<>();
        for (int c1 = 0; c1 < dp[layer].length; c1++) {
            List<DpCell> row = new ArrayList<>();
            for (int c2 = 0; c2 < dp[layer][c1].length; c2++) {
                Integer value = dp[layer][c1][c2];
                row.add(new DpCell(value == null ? "·" : String.valueOf(value), value == null ? "void" : c1 == r && c2 == c ? "probe" : "known"));
            }
            cells.add(row);
        }
        return new DpTable(RecursiveDpExecution.labels("row=" + layer + ",col1=", dp[layer].length),
                RecursiveDpExecution.labels("col2=", dp[layer].length), cells);
    }
}
