package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * At time t every cell whose elevation is at most t is swimmable, and movement within the
 * swimmable region is free - so the earliest arrival time is the smallest t for which some
 * path exists, which is the LARGEST elevation on that path. That makes this a bottleneck
 * shortest path, not a sum-of-costs one, and Dijkstra still applies with max(...) replacing
 * the usual addition.
 *
 * <p>It differs from {@code path-min-effort} in what the bottleneck measures: there the cost
 * of a move is the height DIFFERENCE across it, here the cost of entering a cell is that
 * cell's OWN elevation - so the start already costs grid[0][0] rather than 0.
 */
@Component
public class SwimInRisingWaterTracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "swim-in-rising-water";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Elevation map")
                        .help("grid[r][c] is that cell's elevation. Swim from (0,0) to the bottom-right corner.")
                        .constraint("maxRows", 8)
                        .constraint("maxCols", 8)
                        .values(0, 99)
                        .defaultValue(List.of(
                                List.of(0, 1, 2, 3, 4),
                                List.of(24, 23, 22, 21, 5),
                                List.of(12, 13, 14, 15, 16),
                                List.of(11, 17, 18, 19, 20),
                                List.of(10, 9, 8, 7, 6)))
                        .build());
    }

    /** LeetCode 778's first example - a 2x2 board whose answer is 3, not 16. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(0, 2),
                List.of(1, 3)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int swimInWater(int[][] grid) {
                   int n = grid.length, m = grid[0].length;
                   // @a init
                   int[][] time = new int[n][m];
                   for (int[] row : time) Arrays.fill(row, Integer.MAX_VALUE);
                   time[0][0] = grid[0][0];
                   PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[2] - b[2]);
                   pq.add(new int[]{0, 0, grid[0][0]});

                   while (!pq.isEmpty()) {
                       // @a extract
                       int[] top = pq.poll();
                       int row = top[0], col = top[1], t = top[2];
                       if (row == n - 1 && col == m - 1) {
                           // @a arrived
                           return t;
                       }
                       for (int[] d : DIRECTIONS) {
                           int nrow = row + d[0], ncol = col + d[1];
                           if (nrow < 0 || nrow >= n || ncol < 0 || ncol >= m) continue;
                           int wait = Math.max(t, grid[nrow][ncol]);
                           if (wait < time[nrow][ncol]) {
                               // @a relax
                               time[nrow][ncol] = wait;
                               pq.add(new int[]{nrow, ncol, wait});
                           }
                       }
                   }
                   return time[n - 1][m - 1];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;
        int[][] time = new int[rows][cols];
        for (int[] row : time) Arrays.fill(row, Integer.MAX_VALUE);
        time[0][0] = grid[0][0];

        PriorityQueue<int[]> pq = new PriorityQueue<>(
                Comparator.<int[]>comparingInt(a -> a[2])
                        .thenComparingInt(a -> a[0])
                        .thenComparingInt(a -> a[1]));
        pq.add(new int[]{0, 0, grid[0][0]});

        emit.at("init")
                .say("%dx%d elevation map. Entering (0,0) already costs its own elevation %d, so "
                                + "time(0,0) = %d and every other cell starts unreachable.",
                        rows, cols, grid[0][0], grid[0][0])
                .var("time", timeString(time)).var("target", "(" + (rows - 1) + "," + (cols - 1) + ")")
                .grid(grid).step();

        while (!pq.isEmpty()) {
            int[] top = pq.poll();
            int row = top[0];
            int col = top[1];
            int t = top[2];

            if (row == rows - 1 && col == cols - 1) {
                emit.at("arrived")
                        .say("The bottom-right corner (%d,%d) comes off the queue at time %d. The "
                                        + "smallest bottleneck always surfaces first, so %d is the answer.",
                                row, col, t, t)
                        .var("answer", t).var("time", timeString(time))
                        .grid(grid).step();
                return;
            }

            emit.at("extract")
                    .say("Pop the earliest-reachable cell: (%d,%d), swimmable from time %d "
                                    + "(its own elevation is %d).",
                            row, col, t, grid[row][col])
                    .var("cell", "(" + row + "," + col + ")").var("t", t)
                    .var("time", timeString(time))
                    .grid(grid).step();

            for (int[] d : DIRECTIONS) {
                int nrow = row + d[0];
                int ncol = col + d[1];
                if (nrow < 0 || nrow >= rows || ncol < 0 || ncol >= cols) {
                    continue;
                }
                int wait = Math.max(t, grid[nrow][ncol]);
                if (wait < time[nrow][ncol]) {
                    time[nrow][ncol] = wait;
                    pq.add(new int[]{nrow, ncol, wait});
                    emit.at("relax")
                            .say("(%d,%d) -> (%d,%d): elevation %d, so this route reaches it at "
                                            + "max(%d, %d) = %d, beating its old best. Enqueue it.",
                                    row, col, nrow, ncol, grid[nrow][ncol], t, grid[nrow][ncol], wait)
                            .var("cell", "(" + nrow + "," + ncol + ")").var("t", wait)
                            .var("time", timeString(time))
                            .grid(grid).step();
                }
            }
        }
    }

    private static String timeString(int[][] time) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < time.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("[");
            for (int j = 0; j < time[i].length; j++) {
                if (j > 0) sb.append(", ");
                sb.append(time[i][j] == Integer.MAX_VALUE ? "∞" : String.valueOf(time[i][j]));
            }
            sb.append("]");
        }
        return sb.append(']').toString();
    }
}
