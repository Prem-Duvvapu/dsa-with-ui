package com.dsa.ui.algorithm.graph;

import com.dsa.ui.trace.SnapshotUtil;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.*;

/**
 * Problem: Rotting Oranges (Multi-Source BFS on Grid - LeetCode 994)
 *
 * 0 = empty cell, 1 = fresh orange, 2 = rotten orange.
 * Returns minimum time units for all fresh oranges to rot.
 */
public class RottingOranges {

    public int solve(int[][] grid, TraceRecorder recorder) {
        int rows = grid.length;
        int cols = grid[0].length;
        int[][] state = SnapshotUtil.clone2DGrid(grid);
        Queue<int[]> queue = new LinkedList<>();
        int freshCount = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (state[r][c] == 2) {
                    queue.add(new int[]{r, c});
                } else if (state[r][c] == 1) {
                    freshCount++;
                }
            }
        }

        recorder.record(new TraceEvent(
            "start", 15,
            String.format("Rotting Oranges: Enqueue %d initial rotten oranges (val=2). Fresh oranges count = %d.", queue.size(), freshCount),
            Map.of("RottenInitial", String.valueOf(queue.size()), "FreshInitial", String.valueOf(freshCount)),
            "Matrix", SnapshotUtil.clone2DGrid(state),
            getQueueStrings(queue), Map.of(), List.of()
        ));

        if (freshCount == 0) return 0;

        int minutes = 0;
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        while (!queue.isEmpty() && freshCount > 0) {
            int size = queue.size();
            minutes++;

            for (int i = 0; i < size; i++) {
                int[] curr = queue.poll();
                int r = curr[0];
                int c = curr[1];

                for (int d = 0; d < 4; d++) {
                    int nr = r + dr[d];
                    int nc = c + dc[d];

                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && state[nr][nc] == 1) {
                        state[nr][nc] = 2; // Rotted!
                        freshCount--;
                        queue.add(new int[]{nr, nc});

                        recorder.record(new TraceEvent(
                            "orange_rotted", 35,
                            String.format("Minute %d: Orange at (%d, %d) rots adjacent fresh orange at (%d, %d)! Remaining fresh = %d.",
                                minutes, r, c, nr, nc, freshCount),
                            Map.of("minute", String.valueOf(minutes), "rotted", "(" + nr + "," + nc + ")", "freshLeft", String.valueOf(freshCount)),
                            "Matrix", SnapshotUtil.clone2DGrid(state),
                            getQueueStrings(queue), Map.of(), List.of()
                        ));
                    }
                }
            }
        }

        int result = freshCount == 0 ? minutes : -1;

        recorder.record(new TraceEvent(
            "complete", 50,
            String.format("Rotting Oranges Complete! Minimum minutes elapsed = %d. All fresh oranges rotted: %s",
                result, freshCount == 0 ? "YES" : "NO (IMPOSSIBLE)"),
            Map.of("Minutes", String.valueOf(result), "FreshRemaining", String.valueOf(freshCount)),
            "Matrix", SnapshotUtil.clone2DGrid(state),
            List.of(), Map.of(), List.of()
        ));

        return result;
    }

    private List<String> getQueueStrings(Queue<int[]> q) {
        List<String> list = new ArrayList<>();
        for (int[] p : q) list.add("(" + p[0] + "," + p[1] + ")");
        return list;
    }
}
