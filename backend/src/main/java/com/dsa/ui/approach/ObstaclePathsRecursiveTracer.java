package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

/** Obstacle zeros and the open origin are actual memoized bases; no out-of-grid calls. */
final class ObstaclePathsRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    ObstaclePathsRecursiveTracer(boolean memo) { this.memo = memo; }
    public String id() { return "unique-paths-2"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/" + id() + "/" + (memo ? "memoization" : "recursion") + ".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("grid", FieldType.INT_GRID).label("Grid")
                .constraint("minRows", 1).constraint("maxRows", memo ? 8 : 4)
                .constraint("minCols", 1).constraint("maxCols", memo ? 8 : 4).values(0, 1)
                .defaultValue(List.of(List.of(0,0,0), List.of(0,1,0), List.of(0,0,0)))
                .help("1 blocks a cell; 0 is open. Move right/down only. "
                        + (memo ? "Cache unreachable zero counts, not just positive paths." : "Up to 4 rows and columns keeps every call visible.")).build());
    }
    public Map<String,Object> alternateInput() { return Map.of("grid", List.of(List.of(1,0),List.of(0,0))); }
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        var execution = new RecursiveDpExecution(emit, memo, 1, RecursiveDpExecution.labels("row=", grid.length),
                RecursiveDpExecution.labels("col=", grid[0].length), null, "paths");
        var root = state(grid.length - 1, grid[0].length - 1);
        execution.start(root, "Count paths from the open origin. Obstacles contribute zero; missing neighbours contribute zero without making invalid recursive calls.");
        execution.done(root, paths(grid.length - 1, grid[0].length - 1, grid, execution));
    }
    private int paths(int row, int col, int[][] grid, RecursiveDpExecution execution) {
        return execution.evaluate(state(row,col), grid[row][col] == 1 || row == 0 && col == 0,
                "Add paths from each existing neighbour above/left; an obstacle stops recursion at a known zero.", () -> {
                    if (grid[row][col] == 1) return 0;
                    if (row == 0 && col == 0) return 1;
                    int above = row > 0 ? paths(row - 1, col, grid, execution) : 0;
                    int left = col > 0 ? paths(row, col - 1, grid, execution) : 0;
                    return above + left;
                });
    }
    private static RecursiveDpExecution.State state(int row,int col) { return new RecursiveDpExecution.State(0,row,col,"row="+row+",col="+col); }
}
