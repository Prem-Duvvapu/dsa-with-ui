package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Every move in the maze costs the same one step, so - exactly as in the unweighted graph
 * case - plain BFS from the source visits cells in nondecreasing distance order, and the
 * first time it reaches the destination that distance is optimal. Source is fixed at
 * (0,0) and destination at the grid's bottom-right corner; 1 marks a walkable cell, 0 a wall.
 */
@Component
public class ShortestPathBinaryMazeTracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "shortest-path-binary-maze";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Maze")
                        .help("1 = walkable, 0 = wall. Source is (0,0), destination is the bottom-right corner.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(0, 1)
                        .defaultValue(List.of(
                                List.of(1, 1, 1, 1),
                                List.of(0, 0, 1, 0),
                                List.of(1, 1, 1, 1)))
                        .build());
    }

    /** The destination is walled off entirely - no path can reach it. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 0, 1),
                List.of(0, 0, 1),
                List.of(1, 0, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int shortestPath(int[][] grid) {
                   int n = grid.length, m = grid[0].length;
                   // @a init
                   boolean[][] seen = new boolean[n][m];
                   Queue<int[]> queue = new LinkedList<>();
                   queue.add(new int[]{0, 0, 0});
                   seen[0][0] = true;

                   int[] delrow = {-1, 1, 0, 0};
                   int[] delcol = {0, 0, -1, 1};

                   while (!queue.isEmpty()) {
                       // @a poll
                       int[] cell = queue.poll();
                       int row = cell[0], col = cell[1], steps = cell[2];
                       if (row == n - 1 && col == m - 1) {
                           // @a arrived
                           return steps;
                       }
                       for (int k = 0; k < 4; k++) {
                           int nrow = row + delrow[k], ncol = col + delcol[k];
                           if (nrow < 0 || nrow >= n || ncol < 0 || ncol >= m
                                   || seen[nrow][ncol] || grid[nrow][ncol] == 0) {
                               continue;
                           }
                           // @a expand
                           seen[nrow][ncol] = true;
                           queue.add(new int[]{nrow, ncol, steps + 1});
                       }
                   }
                   // @a unreachable
                   return -1;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;
        boolean[][] seen = new boolean[rows][cols];
        int[][] display = new int[rows][cols];
        for (int[] row : display) Arrays.fill(row, -1);

        if (grid[0][0] == 0 || grid[rows - 1][cols - 1] == 0) {
            emit.at("unreachable")
                    .say("Source or destination is a wall - no path can exist. Answer: -1.")
                    .var("answer", -1).grid(display).step();
            return;
        }

        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{0, 0, 0});
        seen[0][0] = true;
        display[0][0] = 0;

        emit.at("init").say("%dx%d maze. Source (0,0), destination (%d,%d). BFS from the source.",
                        rows, cols, rows - 1, cols - 1)
                .grid(display).step();

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int row = cell[0], col = cell[1], steps = cell[2];

            if (row == rows - 1 && col == cols - 1) {
                emit.at("arrived").say("Reached the destination (%d,%d) in %d step(s).", row, col, steps)
                        .var("answer", steps).grid(display).step();
                return;
            }

            emit.at("poll").say("Dequeue (%d,%d) at %d step(s).", row, col, steps)
                    .var("cell", "(" + row + "," + col + ")").var("steps", steps)
                    .grid(display).step();

            for (int[] d : DIRECTIONS) {
                int nrow = row + d[0], ncol = col + d[1];
                if (nrow < 0 || nrow >= rows || ncol < 0 || ncol >= cols
                        || seen[nrow][ncol] || grid[nrow][ncol] == 0) {
                    continue;
                }
                seen[nrow][ncol] = true;
                display[nrow][ncol] = steps + 1;
                queue.add(new int[]{nrow, ncol, steps + 1});
                emit.at("expand").say("(%d,%d) is walkable and unvisited - %d step(s) away.",
                                nrow, ncol, steps + 1)
                        .var("cell", "(" + nrow + "," + ncol + ")").var("steps", steps + 1)
                        .grid(display).step();
            }
        }

        emit.at("unreachable").say("Queue emptied without reaching the destination. Answer: -1.")
                .var("answer", -1).grid(display).step();
    }
}
