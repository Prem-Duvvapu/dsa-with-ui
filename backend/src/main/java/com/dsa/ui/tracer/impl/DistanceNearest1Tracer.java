package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * For every cell of a 0/1 grid, the distance (in orthogonal steps) to the nearest 1 - by
 * multi-source BFS seeded from every 1-cell at distance 0, the same shape as
 * {@code rotting-oranges} but computing a distance grid instead of a rot time.
 *
 * <p>The visualised grid starts at -1 ("not yet reached") everywhere except the 1-cells,
 * which seed at 0, and fills in as BFS expands outward - the working state, not merely the
 * finished answer.
 */
@Component
public class DistanceNearest1Tracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "distance-nearest-1";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Grid")
                        .help("0/1 grid. Every cell's distance to the nearest 1 is computed.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(0, 1)
                        .defaultValue(List.of(
                                List.of(0, 0, 0),
                                List.of(0, 1, 0),
                                List.of(1, 0, 1)))
                        .build());
    }

    /** A single seed in a corner of a larger grid, instead of three seeds in a small one. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 0, 0, 0),
                List.of(0, 0, 0, 0),
                List.of(0, 0, 0, 0),
                List.of(0, 0, 0, 0)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[][] nearest(int[][] grid) {
                   // @a init
                   int n = grid.length, m = grid[0].length;
                   boolean[][] seen = new boolean[n][m];
                   int[][] dist = new int[n][m];
                   Queue<int[]> queue = new LinkedList<>();

                   for (int i = 0; i < n; i++) {
                       for (int j = 0; j < m; j++) {
                           if (grid[i][j] == 1) {
                               // @a seed
                               queue.add(new int[]{i, j, 0});
                               seen[i][j] = true;
                           }
                       }
                   }

                   int[] delrow = {-1, 0, 1, 0};
                   int[] delcol = {0, 1, 0, -1};

                   while (!queue.isEmpty()) {
                       // @a poll
                       int[] cell = queue.poll();
                       int row = cell[0], col = cell[1], steps = cell[2];
                       dist[row][col] = steps;

                       for (int k = 0; k < 4; k++) {
                           int nrow = row + delrow[k], ncol = col + delcol[k];
                           if (nrow < 0 || nrow >= n || ncol < 0 || ncol >= m || seen[nrow][ncol]) {
                               // @a skip
                               continue;
                           }
                           // @a expand
                           seen[nrow][ncol] = true;
                           queue.add(new int[]{nrow, ncol, steps + 1});
                       }
                   }
                   // @a done
                   return dist;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;
        boolean[][] seen = new boolean[rows][cols];
        int[][] display = new int[rows][cols];
        for (int[] row : display) {
            Arrays.fill(row, -1);
        }

        Deque<int[]> queue = new ArrayDeque<>();
        int seeds = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] == 1) {
                    queue.add(new int[]{i, j, 0});
                    seen[i][j] = true;
                    display[i][j] = 0;
                    seeds++;
                }
            }
        }

        emit.at("init").say("%dx%d grid, %d cell(s) already at distance 0. -1 means \"not yet reached\".",
                        rows, cols, seeds)
                .var("seeds", seeds).grid(display).step();

        emit.at("seed").say("Seed the queue with every 1-cell at distance 0.")
                .var("queueSize", queue.size()).grid(display).step();

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int row = cell[0], col = cell[1], steps = cell[2];
            display[row][col] = steps;

            emit.at("poll").say("Dequeue (%d,%d) at distance %d.", row, col, steps)
                    .var("cell", "(" + row + "," + col + ")").var("distance", steps)
                    .grid(display).step();

            for (int[] d : DIRECTIONS) {
                int nrow = row + d[0], ncol = col + d[1];
                if (nrow < 0 || nrow >= rows || ncol < 0 || ncol >= cols || seen[nrow][ncol]) {
                    emit.at("skip").say("(%d,%d) is out of bounds or already reached - skip it.", nrow, ncol)
                            .var("cell", "(" + nrow + "," + ncol + ")")
                            .grid(display).step();
                    continue;
                }
                seen[nrow][ncol] = true;
                display[nrow][ncol] = steps + 1;
                queue.add(new int[]{nrow, ncol, steps + 1});

                emit.at("expand").say("(%d,%d) is new. Its nearest 1 is %d step(s) away.",
                                nrow, ncol, steps + 1)
                        .var("cell", "(" + nrow + "," + ncol + ")").var("distance", steps + 1)
                        .grid(display).step();
            }
        }

        emit.at("done").say("Every cell reached. Distances to the nearest 1 are final.")
                .grid(display).step();
    }
}
