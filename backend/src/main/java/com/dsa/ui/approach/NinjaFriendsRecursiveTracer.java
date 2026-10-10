package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

final class NinjaFriendsRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    NinjaFriendsRecursiveTracer(boolean memo) { this.memo = memo; }
    public String id() { return "ninja-and-his-friends"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/" + id()
            + "/" + (memo ? "memoization" : "recursion") + ".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("grid", FieldType.INT_GRID).label("Chocolate Grid")
                .constraint("maxRows", memo ? 5 : 4).constraint("maxCols", memo ? 4 : 6).values(0, 100)
                .defaultValue(List.of(List.of(3, 1, 1), List.of(2, 5, 1), List.of(1, 5, 5), List.of(2, 1, 1)))
                .help("State (row,col1,col2): both robots move down with column change −1, 0 or +1. "
                        + "Shared cells count once. " + (memo ? "Memo is 3D; the companion shows only the labelled current-row slice." : "Up to 4 rows keeps every legal pair of moves visible.")).build());
    }
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(List.of(1, 2, 3, 4), List.of(5, 6, 7, 8), List.of(9, 10, 11, 12)));
    }
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        var trace = new RecursiveDpExecution(emit, memo, grid.length,
                RecursiveDpExecution.labels("col1=", grid[0].length), RecursiveDpExecution.labels("col2=", grid[0].length), "row ");
        var root = state(0, 0, grid[0].length - 1);
        trace.start(root, "Start at opposite ends of row 0. Each recursive state includes BOTH columns and the row; shared chocolates count once.");
        trace.done(root, solve(0, 0, grid[0].length - 1, grid, trace));
    }
    private int solve(int row, int c1, int c2, int[][] grid, RecursiveDpExecution trace) {
        return trace.evaluate(state(row, c1, c2), row == grid.length - 1,
                "Try every legal pair of −1/0/+1 column moves to row " + (row + 1) + "; out-of-grid moves are not called.", () -> {
            int collected = grid[row][c1] + (c1 == c2 ? 0 : grid[row][c2]);
            if (row == grid.length - 1) return collected;
            int best = 0;
            for (int d1 = -1; d1 <= 1; d1++) for (int d2 = -1; d2 <= 1; d2++) {
                int n1 = c1 + d1, n2 = c2 + d2;
                if (n1 >= 0 && n1 < grid[0].length && n2 >= 0 && n2 < grid[0].length)
                    best = Math.max(best, solve(row + 1, n1, n2, grid, trace));
            }
            return collected + best;
        });
    }
    private static RecursiveDpExecution.State state(int row, int c1, int c2) {
        return new RecursiveDpExecution.State(row, c1, c2, "row=" + row + ",col1=" + c1 + ",col2=" + c2);
    }
}
