package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every open, unvisited cell reachable by one step is tried in a fixed Down/Left/Right/Up
 * order; each choice marks the cell visited, recurses, then un-marks it on the way back out
 * so a later path can still pass through it. Reaching the bottom-right corner captures the
 * path string built so far — the recursion never needs to know it was the only way there.
 */
@Component
public class RatInAMazeTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "rat-in-a-maze";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("maze", FieldType.INT_GRID)
                        .label("Maze (1 = open, 0 = blocked)")
                        .help("Square grid. The rat starts at (0,0) and must reach the bottom-right corner.")
                        .constraint("maxRows", 6).constraint("maxCols", 6)
                        .values(0, 1)
                        .defaultValue(List.of(
                                List.of(1, 0, 0, 0),
                                List.of(1, 1, 0, 1),
                                List.of(1, 1, 0, 0),
                                List.of(0, 1, 1, 1)))
                        .build());
    }

    /** No path exists at all — the corners are cut off from each other. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("maze", List.of(List.of(1, 0), List.of(0, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<String> findPath(int[][] maze) {
                   int n = maze.length;
                   List<String> paths = new ArrayList<>();
                   if (maze[0][0] == 1) {
                       visited[0][0] = true;
                       explore(0, 0, maze, n, "", paths);
                   }
                   // @a done
                   return paths;
               }

               private void explore(int r, int c, int[][] maze, int n, String path, List<String> paths) {
                   if (r == n - 1 && c == n - 1) {
                       // @a reachEnd
                       paths.add(path);
                       return;
                   }
                   int[][] dirs = {{1,0,'D'}, {0,-1,'L'}, {0,1,'R'}, {-1,0,'U'}};
                   for (int[] d : dirs) {
                       int nr = r + d[0], nc = c + d[1];
                       if (nr < 0 || nr >= n || nc < 0 || nc >= n
                               || maze[nr][nc] == 0 || visited[nr][nc]) {
                           // @a blocked
                           continue;
                       }
                       visited[nr][nc] = true;
                       // @a step
                       explore(nr, nc, maze, n, path + (char) d[2], paths);
                       visited[nr][nc] = false;
                       // @a backtrack
                   }
               }""";
    }

    private static final int[][] DIRS = {{1, 0, 'D'}, {0, -1, 'L'}, {0, 1, 'R'}, {-1, 0, 'U'}};

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] maze = in.getGrid("maze");
        int n = maze.length;
        for (int[] row : maze) {
            if (row.length != n) {
                throw new InputValidationException(Map.of("maze", "The maze must be square."));
            }
        }

        List<String> paths = new ArrayList<>();
        if (maze[0][0] == 1) {
            boolean[][] visited = new boolean[n][n];
            visited[0][0] = true;
            explore(0, 0, maze, n, visited, "", paths, emit);
        }

        emit.at("done")
                .say("Every direction from every reachable cell explored. %d complete path%s found.",
                        paths.size(), paths.size() == 1 ? "" : "s")
                .var("paths", paths).grid(maze).step();
    }

    private void explore(int r, int c, int[][] maze, int n, boolean[][] visited, String path,
                          List<String> paths, StepEmitter emit) {
        emit.push("explore(" + r + "," + c + ")");

        if (r == n - 1 && c == n - 1) {
            paths.add(path);
            emit.at("reachEnd")
                    .say("Reached the bottom-right corner - path \"%s\" captured as solution #%d.",
                            path, paths.size())
                    .var("path", path).var("solutions", paths.size()).grid(maze).step();
            emit.pop();
            return;
        }

        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            char move = (char) d[2];
            if (nr < 0 || nr >= n || nc < 0 || nc >= n || maze[nr][nc] == 0 || visited[nr][nc]) {
                emit.at("blocked")
                        .say("%c from (%d,%d) is out of bounds, blocked, or already on this path - skip it.",
                                move, r, c)
                        .var("from", "(" + r + "," + c + ")").var("move", String.valueOf(move))
                        .grid(maze).step();
                continue;
            }

            visited[nr][nc] = true;
            emit.at("step")
                    .say("%c from (%d,%d) reaches open cell (%d,%d) - recurse with path \"%s\".",
                            move, r, c, nr, nc, path + move)
                    .var("move", String.valueOf(move)).var("path", path + move).grid(maze).step();
            explore(nr, nc, maze, n, visited, path + move, paths, emit);

            visited[nr][nc] = false;
            emit.at("backtrack")
                    .say("Unmark (%d,%d) - it may still lie on a different path.", nr, nc)
                    .var("unmarked", "(" + nr + "," + nc + ")").grid(maze).step();
        }

        emit.pop();
    }
}
