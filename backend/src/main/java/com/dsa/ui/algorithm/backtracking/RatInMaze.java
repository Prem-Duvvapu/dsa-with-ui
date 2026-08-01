package com.dsa.ui.algorithm.backtracking;

import com.dsa.ui.trace.SnapshotUtil;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Problem: Rat in a Maze (Backtracking 2D Grid Pathfinding)
 *
 * Find all valid paths from (0,0) to (N-1, N-1) moving in 4 directions: D, L, R, U.
 * 1 represents open path, 0 represents wall block, 2 represents rat visited path.
 *
 * Time Complexity:  O(4^(N^2)) worst case.
 * Space Complexity: O(N^2) recursion stack + visited matrix.
 */
public class RatInMaze {

    public List<String> solve(int[][] maze, TraceRecorder recorder) {
        int n = maze.length;
        List<String> paths = new ArrayList<>();
        int[][] visited = SnapshotUtil.clone2DGrid(maze);
        List<String> callStack = new ArrayList<>();

        recorder.record(new TraceEvent(
            "start", 15,
            String.format("Rat in a Maze: Start at (0,0). Destination (%d,%d). Directions: D-L-R-U.", n - 1, n - 1),
            Map.of("start", "(0,0)", "target", "(" + (n - 1) + "," + (n - 1) + ")"),
            "Matrix", SnapshotUtil.clone2DGrid(visited),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        if (maze[0][0] == 1) {
            solveRecursive(0, 0, maze, visited, "", paths, n, recorder, callStack);
        }

        recorder.record(new TraceEvent(
            "complete", 60,
            String.format("Rat in a Maze Complete! Total valid destination paths found: %d. Paths: %s", paths.size(), paths.toString()),
            Map.of("Total Paths", String.valueOf(paths.size()), "Paths", paths.toString()),
            "Matrix", SnapshotUtil.clone2DGrid(visited),
            List.of(), Map.of(), List.of()
        ));

        return paths;
    }

    private void solveRecursive(int r, int c, int[][] maze, int[][] visited, String path, List<String> paths,
                                int n, TraceRecorder recorder, List<String> callStack) {
        String callFrame = String.format("solve(%d, %d)", r, c);
        callStack.add(callFrame);

        if (r == n - 1 && c == n - 1) {
            paths.add(path);
            visited[r][c] = 2; // Visited rat path

            recorder.record(new TraceEvent(
                "destination_reached", 28,
                String.format("DESTINATION REACHED at (%d, %d)! Valid Path found: \"%s\". Recorded path #%d.", r, c, path, paths.size()),
                Map.of("Path", path, "Status", "REACHED"),
                "Matrix", SnapshotUtil.clone2DGrid(visited),
                new ArrayList<>(callStack), Map.of(), List.of()
            ));

            visited[r][c] = 1;
            callStack.remove(callStack.size() - 1);
            return;
        }

        // Mark rat visit
        visited[r][c] = 2;

        recorder.record(new TraceEvent(
            "visit_cell", 35,
            String.format("At cell (%d, %d): Current path string: \"%s\". Mark visited. Try 4-directional moves (D-L-R-U)...", r, c, path),
            Map.of("row", String.valueOf(r), "col", String.valueOf(c), "path", path),
            "Matrix", SnapshotUtil.clone2DGrid(visited),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        // Down
        if (isValid(r + 1, c, maze, visited, n)) {
            solveRecursive(r + 1, c, maze, visited, path + "D", paths, n, recorder, callStack);
        }
        // Left
        if (isValid(r, c - 1, maze, visited, n)) {
            solveRecursive(r, c - 1, maze, visited, path + "L", paths, n, recorder, callStack);
        }
        // Right
        if (isValid(r, c + 1, maze, visited, n)) {
            solveRecursive(r, c + 1, maze, visited, path + "R", paths, n, recorder, callStack);
        }
        // Up
        if (isValid(r - 1, c, maze, visited, n)) {
            solveRecursive(r - 1, c, maze, visited, path + "U", paths, n, recorder, callStack);
        }

        // Backtrack
        visited[r][c] = 1;

        recorder.record(new TraceEvent(
            "backtrack_rat", 52,
            String.format("BACKTRACK from cell (%d, %d): Unmark visited. Path string: \"%s\".", r, c, path),
            Map.of("row", String.valueOf(r), "col", String.valueOf(c), "backtrack", "true"),
            "Matrix", SnapshotUtil.clone2DGrid(visited),
            new ArrayList<>(callStack), Map.of(), List.of()
        ));

        callStack.remove(callStack.size() - 1);
    }

    private boolean isValid(int r, int c, int[][] maze, int[][] visited, int n) {
        return r >= 0 && r < n && c >= 0 && c < n && maze[r][c] == 1 && visited[r][c] != 2;
    }
}
