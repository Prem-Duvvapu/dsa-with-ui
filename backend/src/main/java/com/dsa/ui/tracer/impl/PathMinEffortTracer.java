package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * "Effort" along a path is its largest single step, not the sum of steps - a bottleneck
 * shortest path. Dijkstra still applies: relax a neighbour whenever routing through the
 * current cell would give it a smaller bottleneck than its best known one, using
 * max(currentEffort, heightDifference) in place of the usual currentDist + weight.
 */
@Component
public class PathMinEffortTracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "path-min-effort";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("heights", FieldType.INT_GRID)
                        .label("Height map")
                        .help("Effort between adjacent cells is the absolute height difference. Start (0,0), end at the bottom-right corner.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(0, 100)
                        .defaultValue(List.of(
                                List.of(1, 2, 2),
                                List.of(3, 8, 2),
                                List.of(5, 3, 5)))
                        .build());
    }

    /** Same size grid, one height changed - a completely different minimum-effort path wins. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("heights", List.of(
                List.of(1, 2, 3),
                List.of(3, 8, 4),
                List.of(5, 3, 5)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int minimumEffortPath(int[][] heights) {
                   int n = heights.length, m = heights[0].length;
                   // @a init
                   int[][] effort = new int[n][m];
                   for (int[] row : effort) Arrays.fill(row, Integer.MAX_VALUE);
                   effort[0][0] = 0;
                   PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[2] - b[2]);
                   pq.add(new int[]{0, 0, 0});

                   while (!pq.isEmpty()) {
                       // @a extract
                       int[] top = pq.poll();
                       int row = top[0], col = top[1], e = top[2];
                       if (row == n - 1 && col == m - 1) {
                           // @a arrived
                           return e;
                       }
                       for (int[] d : DIRECTIONS) {
                           int nrow = row + d[0], ncol = col + d[1];
                           if (nrow < 0 || nrow >= n || ncol < 0 || ncol >= m) continue;
                           int newEffort = Math.max(e, Math.abs(heights[row][col] - heights[nrow][ncol]));
                           if (newEffort < effort[nrow][ncol]) {
                               // @a relax
                               effort[nrow][ncol] = newEffort;
                               pq.add(new int[]{nrow, ncol, newEffort});
                           }
                       }
                   }
                   return effort[n - 1][m - 1];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] heights = in.getGrid("heights");
        int rows = heights.length;
        int cols = heights[0].length;
        int[][] effort = new int[rows][cols];
        for (int[] row : effort) Arrays.fill(row, Integer.MAX_VALUE);
        effort[0][0] = 0;

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[2]));
        pq.add(new int[]{0, 0, 0});

        emit.at("init").say("%dx%d height map. effort(0,0) = 0, everywhere else infinite. Enqueue the start.",
                        rows, cols)
                .var("effort", effortString(effort)).grid(heights).step();

        while (!pq.isEmpty()) {
            int[] top = pq.poll();
            int row = top[0], col = top[1], e = top[2];

            if (row == rows - 1 && col == cols - 1) {
                emit.at("arrived").say("Reached (%d,%d) with effort %d - the smallest bottleneck reaches it first.", row, col, e)
                        .var("answer", e).grid(heights).step();
                return;
            }

            emit.at("extract").say("Pop the smallest-effort entry: (%d,%d) at effort %d.", row, col, e)
                    .var("cell", "(" + row + "," + col + ")").var("effort", e)
                    .grid(heights).step();

            for (int[] d : DIRECTIONS) {
                int nrow = row + d[0], ncol = col + d[1];
                if (nrow < 0 || nrow >= rows || ncol < 0 || ncol >= cols) continue;
                int newEffort = Math.max(e, Math.abs(heights[row][col] - heights[nrow][ncol]));
                if (newEffort < effort[nrow][ncol]) {
                    effort[nrow][ncol] = newEffort;
                    pq.add(new int[]{nrow, ncol, newEffort});
                    emit.at("relax").say("(%d,%d) -> (%d,%d): height gap %d, bottleneck max(%d, %d) = %d beats its old best. Update and enqueue.",
                                    row, col, nrow, ncol, Math.abs(heights[row][col] - heights[nrow][ncol]), e,
                                    Math.abs(heights[row][col] - heights[nrow][ncol]), newEffort)
                            .var("cell", "(" + nrow + "," + ncol + ")").var("newEffort", newEffort)
                            .var("effort", effortString(effort))
                            .grid(heights).step();
                }
            }
        }
    }

    private static String effortString(int[][] effort) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < effort.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("[");
            for (int j = 0; j < effort[i].length; j++) {
                if (j > 0) sb.append(", ");
                sb.append(effort[i][j] == Integer.MAX_VALUE ? "∞" : String.valueOf(effort[i][j]));
            }
            sb.append("]");
        }
        return sb.append(']').toString();
    }
}
