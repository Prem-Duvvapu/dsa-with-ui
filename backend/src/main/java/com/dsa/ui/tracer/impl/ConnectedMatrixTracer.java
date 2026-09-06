package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Connected components of a 0/1 matrix: every group of 1-cells that touch each other
 * 4-directionally is one component.
 *
 * <p>The outer double loop is a plain scan; the work happens the moment it lands on a
 * 1-cell nothing has claimed yet. That cell opens a component, and a BFS claims everything
 * reachable from it, which is exactly what stops the same component being counted again
 * when the scan reaches its other cells later.
 *
 * <p>Component k repaints its cells as {@code k + 1}, so an unclaimed cell (1) stays
 * visually distinct from the first component (2) and the components stay distinct from each
 * other as the scan proceeds.
 */
@Component
public class ConnectedMatrixTracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "connected-matrix";
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
                        .help("1 is a filled cell, 0 is empty. Cells claimed by component k are "
                                + "redrawn as k + 1.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(0, 1)
                        // Two components: a 3-cell block top-left and a 5-cell strip bottom-right.
                        .defaultValue(List.of(
                                List.of(1, 1, 0, 0),
                                List.of(1, 0, 0, 1),
                                List.of(0, 0, 1, 1),
                                List.of(0, 1, 1, 0)))
                        .build());
    }

    /**
     * A completely filled 3x5 grid: one component swallows every cell, so the very first
     * BFS does all the work and every later scan position is already claimed - the opposite
     * of the default, where the scan has to open a second component halfway through.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 1, 1, 1, 1),
                List.of(1, 1, 1, 1, 1),
                List.of(1, 1, 1, 1, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int countComponents(int[][] grid) {
                   // @a init
                   int n = grid.length, m = grid[0].length;
                   boolean[][] claimed = new boolean[n][m];
                   int components = 0;

                   for (int i = 0; i < n; i++) {
                       for (int j = 0; j < m; j++) {
                           if (grid[i][j] == 0 || claimed[i][j]) {
                               // @a nothingHere
                               continue;
                           }
                           // @a newComponent
                           components++;
                           Queue<int[]> queue = new LinkedList<>();
                           queue.add(new int[]{i, j});
                           claimed[i][j] = true;

                           while (!queue.isEmpty()) {
                               // @a claim
                               int[] cell = queue.poll();
                               for (int[] d : DIRECTIONS) {
                                   int r = cell[0] + d[0], c = cell[1] + d[1];
                                   if (r < 0 || r >= n || c < 0 || c >= m
                                           || grid[r][c] == 0 || claimed[r][c]) {
                                       // @a notPart
                                       continue;
                                   }
                                   // @a join
                                   claimed[r][c] = true;
                                   queue.add(new int[]{r, c});
                               }
                           }
                       }
                   }
                   // @a done
                   return components;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;
        boolean[][] claimed = new boolean[rows][cols];
        int[][] display = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            display[r] = grid[r].clone();
        }
        int components = 0;

        emit.at("init").say("Scan a %dx%d grid. Every filled cell nothing has claimed yet opens a "
                        + "new component.", rows, cols)
                .var("components", 0).grid(display).step();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] == 0) {
                    emit.at("nothingHere").say("(%d,%d) is empty - nothing to start here.", i, j)
                            .var("cell", "(" + i + "," + j + ")").var("components", components)
                            .grid(display).step();
                    continue;
                }
                if (claimed[i][j]) {
                    emit.at("nothingHere").say("(%d,%d) is filled but component #%d already claimed "
                                    + "it - counting it again would double-count.",
                                    i, j, display[i][j] - 1)
                            .var("cell", "(" + i + "," + j + ")").var("components", components)
                            .grid(display).step();
                    continue;
                }

                components++;
                claimed[i][j] = true;
                display[i][j] = components + 1;
                Deque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{i, j});

                emit.at("newComponent").say("(%d,%d) is filled and unclaimed, so it opens component "
                                + "#%d. BFS out from it.", i, j, components)
                        .var("cell", "(" + i + "," + j + ")").var("components", components)
                        .grid(display).queue(cells(queue)).step();

                int size = 0;
                while (!queue.isEmpty()) {
                    int[] cell = queue.poll();
                    size++;

                    emit.at("claim").say("(%d,%d) belongs to component #%d. %d cell(s) claimed so far.",
                                    cell[0], cell[1], components, size)
                            .var("cell", "(" + cell[0] + "," + cell[1] + ")")
                            .var("size", size).var("components", components)
                            .grid(display).queue(cells(queue)).step();

                    for (int[] d : DIRECTIONS) {
                        int r = cell[0] + d[0];
                        int c = cell[1] + d[1];
                        if (r < 0 || r >= rows || c < 0 || c >= cols
                                || grid[r][c] == 0 || claimed[r][c]) {
                            emit.at("notPart").say("(%d,%d) is off the grid, empty, or already "
                                            + "claimed - not a new member.", r, c)
                                    .var("cell", "(" + r + "," + c + ")")
                                    .grid(display).queue(cells(queue)).step();
                            continue;
                        }
                        claimed[r][c] = true;
                        display[r][c] = components + 1;
                        queue.add(new int[]{r, c});

                        emit.at("join").say("(%d,%d) is filled and touches component #%d - it joins.",
                                        r, c, components)
                                .var("cell", "(" + r + "," + c + ")").var("components", components)
                                .grid(display).queue(cells(queue)).step();
                    }
                }
            }
        }

        emit.at("done").say("Scan finished. The grid holds %d connected component(s).", components)
                .var("components", components).grid(display).step();
    }

    private static List<String> cells(Deque<int[]> queue) {
        List<String> out = new ArrayList<>();
        for (int[] cell : queue) {
            out.add("(" + cell[0] + "," + cell[1] + ")");
        }
        return out;
    }
}
