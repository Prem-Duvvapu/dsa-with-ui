package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Path With Minimum Effort (LeetCode 1631), traced on the owner's own accepted submission. A
 * path's effort is its largest single height step, not the sum, so this is a bottleneck shortest
 * path: Dijkstra where a neighbour's candidate is max(minEffort[current], step) instead of
 * dist + weight. The first time the bottom-right cell leaves the queue its effort is final.
 * Optimal: O(m*n log(m*n)).
 *
 * <p>The canvas shows the heights; {@code minEffort} is listed as a variable, "inf" for a cell no
 * route has reached yet.
 */
@Component
public class PathMinEffortTracer implements AlgorithmTracer {

    // The code's own direction order: up, right, down, left.
    private static final int[] D_ROW = {-1, 0, 1, 0};
    private static final int[] D_COL = {0, 1, 0, -1};

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
               class Tuple {
                   int effort;
                   int row;
                   int col;

                   Tuple(int effort,int row,int col) {
                       this.effort = effort;
                       this.row = row;
                       this.col = col;
                   }
               }

               class Solution {
                   public static int[] dRow = {-1,0,1,0};
                   public static int[] dCol = {0,1,0,-1};

                   public int minimumEffortPath(int[][] heights) {
                       // @a init
                       int m = heights.length;
                       int n = heights[0].length;
                       int[][] minEffort = new int[m][n];
                       PriorityQueue<Tuple> pq = new PriorityQueue<>((x,y) -> Integer.compare(x.effort,y.effort));

                       for (int[] arr: minEffort)
                           Arrays.fill(arr, Integer.MAX_VALUE);

                       minEffort[0][0] = 0;
                       pq.add(new Tuple(0,0,0));

                       while (!pq.isEmpty()) {
                           // @a poll
                           Tuple curr = pq.poll();
                           int currEffort = curr.effort;
                           int currRow = curr.row;
                           int currCol = curr.col;

                           if (currRow == m-1 && currCol == n-1)
                               // @a reached
                               return minEffort[currRow][currCol];

                           for (int i=0;i<4;i++) {
                               int newRow = currRow + dRow[i];
                               int newCol = currCol + dCol[i];

                               if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n) {
                                   int currDiff = Math.abs(heights[currRow][currCol] - heights[newRow][newCol]);
                                   int currRouteMaxDiff= Math.max(minEffort[currRow][currCol], currDiff);

                                   if (minEffort[newRow][newCol] > currRouteMaxDiff) {
                                       // @a relax
                                       minEffort[newRow][newCol] = currRouteMaxDiff;
                                       pq.add(new Tuple(currRouteMaxDiff,newRow,newCol));
                                   }
                               }
                           }
                       }

                       return minEffort[m-1][n-1];
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] heights = in.getGrid("heights");
        int m = heights.length;
        int n = heights[0].length;
        int[][] minEffort = new int[m][n];
        for (int[] arr : minEffort) {
            Arrays.fill(arr, Integer.MAX_VALUE);
        }
        PriorityQueue<int[]> pq = new PriorityQueue<>((x, y) -> Integer.compare(x[0], y[0]));
        minEffort[0][0] = 0;
        pq.add(new int[]{0, 0, 0});

        emit.at("init").say("Start at (0,0) with effort 0. A route's effort is its single biggest height "
                        + "step, so every other cell starts at infinity and the queue hands back the "
                        + "smallest effort first.")
                .var("minEffort", show(minEffort)).grid(heights).queue(tuples(pq)).step();

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int currEffort = curr[0];
            int currRow = curr[1];
            int currCol = curr[2];
            emit.at("poll").say("Poll (%d,%d) with effort %d - the cheapest route still waiting.",
                            currRow, currCol, currEffort)
                    .var("currRow", currRow).var("currCol", currCol).var("currEffort", currEffort)
                    .var("minEffort", show(minEffort)).grid(heights).queue(tuples(pq)).step();

            if (currRow == m - 1 && currCol == n - 1) {
                emit.at("reached").say("That is the bottom-right corner. Nothing left in the queue is cheaper, "
                                + "so its effort %d is final. Return it.", minEffort[currRow][currCol])
                        .var("minEffort", show(minEffort)).var("answer", minEffort[currRow][currCol])
                        .grid(heights).queue(tuples(pq)).step();
                return;
            }

            for (int i = 0; i < 4; i++) {
                int newRow = currRow + D_ROW[i];
                int newCol = currCol + D_COL[i];
                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n) {
                    int currDiff = Math.abs(heights[currRow][currCol] - heights[newRow][newCol]);
                    int currRouteMaxDiff = Math.max(minEffort[currRow][currCol], currDiff);
                    if (minEffort[newRow][newCol] > currRouteMaxDiff) {
                        int before = minEffort[newRow][newCol];
                        minEffort[newRow][newCol] = currRouteMaxDiff;
                        pq.add(new int[]{currRouteMaxDiff, newRow, newCol});
                        emit.at("relax").say("Step to (%d,%d) is |%d - %d| = %d, so this route's effort is "
                                        + "max(%d, %d) = %d, better than %s. minEffort[%d][%d] = %d.",
                                        newRow, newCol, heights[currRow][currCol], heights[newRow][newCol], currDiff,
                                        minEffort[currRow][currCol], currDiff, currRouteMaxDiff,
                                        before == Integer.MAX_VALUE ? "infinity" : String.valueOf(before),
                                        newRow, newCol, currRouteMaxDiff)
                                .var("newRow", newRow).var("newCol", newCol).var("currRouteMaxDiff", currRouteMaxDiff)
                                .var("minEffort", show(minEffort)).grid(heights).queue(tuples(pq)).step();
                    }
                }
            }
        }

        // Unreachable: every cell of a grid is connected to every other, so the bottom-right corner
        // is always polled and returned above. The code's final return is a compiler fallback.
        throw new IllegalStateException("the bottom-right cell was never polled");
    }

    private static String show(int[][] grid) {
        StringBuilder sb = new StringBuilder("[");
        for (int r = 0; r < grid.length; r++) {
            if (r > 0) sb.append(',');
            sb.append('[');
            for (int c = 0; c < grid[r].length; c++) {
                if (c > 0) sb.append(',');
                sb.append(grid[r][c] == Integer.MAX_VALUE ? "inf" : String.valueOf(grid[r][c]));
            }
            sb.append(']');
        }
        return sb.append(']').toString();
    }

    /** The queue, smallest effort first, as "(row,col) e=effort". */
    private static List<String> tuples(PriorityQueue<int[]> pq) {
        List<int[]> items = new ArrayList<>(pq);
        items.sort((x, y) -> Integer.compare(x[0], y[0]));
        List<String> out = new ArrayList<>();
        for (int[] t : items) {
            out.add("(" + t[1] + "," + t[2] + ") e=" + t[0]);
        }
        return out;
    }
}
