package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Distance of the nearest cell having 1, for every cell of a 0/1 matrix.
 *
 * <p>Run one BFS per cell and this is O((n*m)^2). Turn it inside out instead: push EVERY
 * 1-cell into the queue at distance 0 and expand once. BFS still visits in nondecreasing
 * distance order, so the first source to reach a cell is necessarily its nearest one, and
 * marking a cell settled the moment it is queued is what keeps a farther source from
 * overwriting a nearer one later.
 *
 * <p>The animation shows the distance grid being filled, not just its final state: -1 means
 * "no source has arrived yet".
 */
@Component
public class NearestCell1Tracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "nearest-cell-1";
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
                        .help("0/1 matrix. Every 1 is a source at distance 0; the answer for a cell "
                                + "is the number of orthogonal steps to the closest 1.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(0, 1)
                        // Six scattered sources across a 3x4 board; every 0-cell sits one
                        // step from a 1, so the finished distance grid is all 0s and 1s.
                        .defaultValue(List.of(
                                List.of(0, 1, 1, 0),
                                List.of(1, 1, 0, 0),
                                List.of(1, 0, 0, 1)))
                        .build());
    }

    /**
     * One source alone in the middle of a 5x5 board. The wavefront has to travel out four
     * rings to reach the corners, where the default never gets past distance 1.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0),
                List.of(0, 0, 1, 0, 0),
                List.of(0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[][] nearest(int[][] grid) {
                   // @a scan
                   int n = grid.length, m = grid[0].length;
                   int[][] dist = new int[n][m];
                   boolean[][] settled = new boolean[n][m];
                   Queue<int[]> queue = new LinkedList<>();

                   for (int i = 0; i < n; i++) {
                       for (int j = 0; j < m; j++) {
                           if (grid[i][j] == 1) {
                               // @a source
                               settled[i][j] = true;
                               queue.add(new int[]{i, j});
                           }
                       }
                   }

                   while (!queue.isEmpty()) {
                       // @a front
                       int[] cell = queue.poll();
                       int r = cell[0], c = cell[1];
                       for (int[] d : DIRECTIONS) {
                           int nr = r + d[0], nc = c + d[1];
                           if (nr < 0 || nr >= n || nc < 0 || nc >= m || settled[nr][nc]) {
                               // @a fixed
                               continue;
                           }
                           // @a measure
                           settled[nr][nc] = true;
                           dist[nr][nc] = dist[r][c] + 1;
                           queue.add(new int[]{nr, nc});
                       }
                   }
                   // @a finished
                   return dist;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;
        int[][] dist = new int[rows][cols];
        boolean[][] settled = new boolean[rows][cols];
        int[][] display = new int[rows][cols];
        for (int[] row : display) {
            Arrays.fill(row, -1);
        }

        int sources = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] == 1) {
                    sources++;
                }
            }
        }

        emit.at("scan").say("A %dx%d matrix with %d source cell(s). -1 marks a cell no wavefront "
                        + "has reached yet.", rows, cols, sources)
                .var("sources", sources).grid(display).step();

        Deque<int[]> queue = new ArrayDeque<>();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] != 1) {
                    continue;
                }
                settled[i][j] = true;
                display[i][j] = 0;
                queue.add(new int[]{i, j});

                emit.at("source").say("(%d,%d) holds a 1, so its own answer is 0. It enters the "
                                + "queue as a source.", i, j)
                        .var("cell", "(" + i + "," + j + ")").var("queued", queue.size())
                        .grid(display).queue(cells(queue)).step();
            }
        }

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int r = cell[0];
            int c = cell[1];

            emit.at("front").say("Expand from (%d,%d), which sits %d step(s) from its nearest 1.",
                            r, c, dist[r][c])
                    .var("cell", "(" + r + "," + c + ")").var("distance", dist[r][c])
                    .grid(display).queue(cells(queue)).step();

            for (int[] d : DIRECTIONS) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || settled[nr][nc]) {
                    emit.at("fixed").say("(%d,%d) is off the matrix or already has its answer - a "
                                    + "later arrival can only be farther away.", nr, nc)
                            .var("cell", "(" + nr + "," + nc + ")")
                            .grid(display).queue(cells(queue)).step();
                    continue;
                }
                settled[nr][nc] = true;
                dist[nr][nc] = dist[r][c] + 1;
                display[nr][nc] = dist[nr][nc];
                queue.add(new int[]{nr, nc});

                emit.at("measure").say("(%d,%d) is reached for the first time, one step past "
                                + "(%d,%d): its answer is %d.", nr, nc, r, c, dist[nr][nc])
                        .var("cell", "(" + nr + "," + nc + ")").var("distance", dist[nr][nc])
                        .grid(display).queue(cells(queue)).step();
            }
        }

        emit.at("finished").say("The queue is empty, so every cell has been reached. The grid now "
                        + "holds each cell's distance to its nearest 1.")
                .var("sources", sources).grid(display).step();
    }

    private static List<String> cells(Deque<int[]> queue) {
        List<String> out = new ArrayList<>();
        for (int[] cell : queue) {
            out.add("(" + cell[0] + "," + cell[1] + ")");
        }
        return out;
    }
}
