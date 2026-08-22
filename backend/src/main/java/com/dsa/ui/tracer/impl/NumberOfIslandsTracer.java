package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Island counting by flood fill, over a caller-editable grid.
 *
 * <p>Sinking each island as it is counted (writing 2 into the grid) is what the
 * algorithm actually does, and showing it is the point — it is why no cell is ever
 * counted twice.
 */
@Component
public class NumberOfIslandsTracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "number-of-islands";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Grid")
                        .help("1 is land, 0 is water. Counted cells are redrawn as 2.")
                        .constraint("maxRows", 12)
                        .constraint("maxCols", 12)
                        .values(0, 1)
                        .defaultValue(List.of(
                                List.of(1, 1, 0, 0, 0),
                                List.of(1, 1, 0, 0, 0),
                                List.of(0, 0, 1, 0, 0),
                                List.of(0, 0, 0, 1, 1)))
                        .build());
    }

    /** Four single-cell islands instead of a few large ones. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 0, 1),
                List.of(0, 0, 0),
                List.of(1, 0, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int numIslands(int[][] grid) {
                   // @a init
                   int islands = 0;
                   for (int r = 0; r < grid.length; r++) {
                       for (int c = 0; c < grid[0].length; c++) {
                           // @a found
                           if (grid[r][c] == 1) {
                               islands++;
                               // @a sink
                               sink(grid, r, c);
                           }
                       }
                   }
                   // @a done
                   return islands;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;
        int islands = 0;

        emit.using("Matrix");
        emit.at("init").say("Scan a %dx%d grid. Each unvisited land cell starts a new island.", rows, cols)
                .var("islands", 0).grid(grid).step();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != 1) {
                    continue;
                }
                islands++;
                emit.at("found").say("Land at (%d,%d) that is not part of a counted island. That makes island #%d.",
                                r, c, islands)
                        .var("r", r).var("c", c).var("islands", islands)
                        .grid(grid).step();

                // Flood the whole island so its cells cannot start another one.
                Deque<int[]> stack = new ArrayDeque<>();
                stack.push(new int[]{r, c});
                grid[r][c] = 2;
                int sunk = 0;

                while (!stack.isEmpty()) {
                    int[] cell = stack.pop();
                    sunk++;
                    for (int[] d : DIRECTIONS) {
                        int nr = cell[0] + d[0];
                        int nc = cell[1] + d[1];
                        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || grid[nr][nc] != 1) {
                            continue;
                        }
                        grid[nr][nc] = 2;
                        stack.push(new int[]{nr, nc});
                    }
                    emit.at("sink").say("Sink (%d,%d) into island #%d. %d cell(s) claimed, %d still queued.",
                                    cell[0], cell[1], islands, sunk, stack.size())
                            .var("islands", islands).var("claimed", sunk).var("frontier", stack.size())
                            .grid(grid).step();
                }
            }
        }

        emit.at("done").say("Grid fully scanned. %d island(s).", islands)
                .var("islands", islands).grid(grid).step();
    }
}
