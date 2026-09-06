package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Surrounded Regions (LeetCode 130): every region of 'O' that is completely enclosed by 'X'
 * is captured; a region touching the border survives.
 *
 * <p>Searching each region and asking "did it touch an edge?" is the obvious approach and
 * the awkward one. Invert it: the only regions that survive are those reachable FROM the
 * border, so flood outward from every border 'O' first, mark what it reaches, and then flip
 * every 'O' the flood never marked. One pass, no per-region bookkeeping.
 *
 * <p>Encoding: 0 is 'X', 1 is 'O', and 2 is an 'O' the border flood has marked safe. The
 * final step restores the marked cells to 1 so the last frame is the answer board itself.
 */
@Component
public class SurroundedRegionsTracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "surrounded-regions";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("board", FieldType.INT_GRID)
                        .label("Board")
                        .help("0 is 'X', 1 is 'O'. A cell shown as 2 is an 'O' the border flood has "
                                + "proved safe.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(0, 1)
                        // LeetCode 130 example 1. Only the 'O' on the bottom row survives.
                        .defaultValue(List.of(
                                List.of(0, 0, 0, 0),
                                List.of(0, 1, 1, 0),
                                List.of(0, 0, 1, 0),
                                List.of(0, 1, 0, 0)))
                        .build());
    }

    /**
     * A taller board whose border 'O' is the head of a two-cell region reaching inward, so
     * the flood actually has somewhere to spread - where the default's lone border 'O' is
     * boxed in on all four sides and the flood stops immediately.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("board", List.of(
                List.of(1, 0, 0, 0),
                List.of(1, 0, 1, 0),
                List.of(0, 0, 1, 0),
                List.of(0, 0, 0, 0),
                List.of(0, 0, 0, 0)));
    }

    @Override
    public String annotatedCode() {
        return """
               public void solve(char[][] board) {
                   // @a init
                   int n = board.length, m = board[0].length;
                   boolean[][] safe = new boolean[n][m];

                   for (int i = 0; i < n; i++) {
                       for (int j = 0; j < m; j++) {
                           boolean edge = i == 0 || i == n - 1 || j == 0 || j == m - 1;
                           if (edge && board[i][j] == 'O' && !safe[i][j]) {
                               // @a borderSeed
                               floodFromBorder(i, j, board, safe);
                           }
                       }
                   }

                   for (int i = 0; i < n; i++) {
                       for (int j = 0; j < m; j++) {
                           if (board[i][j] != 'O') {
                               continue;
                           }
                           if (safe[i][j]) {
                               // @a survives
                               continue;
                           }
                           // @a capture
                           board[i][j] = 'X';
                       }
                   }
                   // @a done
               }

               private void floodFromBorder(int r, int c, char[][] board, boolean[][] safe) {
                   safe[r][c] = true;
                   Deque<int[]> stack = new ArrayDeque<>();
                   stack.push(new int[]{r, c});

                   while (!stack.isEmpty()) {
                       // @a reach
                       int[] cell = stack.pop();
                       for (int[] d : DIRECTIONS) {
                           int nr = cell[0] + d[0], nc = cell[1] + d[1];
                           if (nr < 0 || nr >= board.length || nc < 0 || nc >= board[0].length
                                   || board[nr][nc] != 'O' || safe[nr][nc]) {
                               // @a blocked
                               continue;
                           }
                           // @a spread
                           safe[nr][nc] = true;
                           stack.push(new int[]{nr, nc});
                       }
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] board = in.getGrid("board");
        int rows = board.length;
        int cols = board[0].length;
        boolean[][] safe = new boolean[rows][cols];
        int[][] display = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            display[r] = board[r].clone();
        }

        emit.at("init").say("A %dx%d board. Only an 'O' the border can reach survives, so flood "
                        + "inward from the edges first and capture whatever the flood misses.",
                        rows, cols)
                .var("rows", rows).var("cols", cols).grid(display).step();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                boolean edge = i == 0 || i == rows - 1 || j == 0 || j == cols - 1;
                if (!edge || board[i][j] != 1 || safe[i][j]) {
                    continue;
                }
                emit.at("borderSeed").say("(%d,%d) is an 'O' sitting on the border, so its whole "
                                + "region can walk off the board. Flood from it.", i, j)
                        .var("cell", "(" + i + "," + j + ")")
                        .grid(display).step();
                flood(i, j, board, safe, display, rows, cols, emit);
            }
        }

        int captured = 0;
        int survivors = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] != 1) {
                    continue;
                }
                if (safe[i][j]) {
                    survivors++;
                    display[i][j] = 1;
                    emit.at("survives").say("(%d,%d) was marked by the border flood, so it stays 'O'.",
                                    i, j)
                            .var("cell", "(" + i + "," + j + ")").var("survivors", survivors)
                            .grid(display).step();
                    continue;
                }
                board[i][j] = 0;
                display[i][j] = 0;
                captured++;
                emit.at("capture").say("(%d,%d) is an 'O' the border never reached - it is "
                                + "surrounded, so it flips to 'X'.", i, j)
                        .var("cell", "(" + i + "," + j + ")").var("captured", captured)
                        .grid(display).step();
            }
        }

        emit.at("done").say("Board settled: %d 'O' captured, %d survived on a border-connected "
                        + "region.", captured, survivors)
                .var("captured", captured).var("survivors", survivors)
                .grid(display).step();
    }

    private void flood(int r, int c, int[][] board, boolean[][] safe, int[][] display,
                        int rows, int cols, StepEmitter emit) {
        safe[r][c] = true;
        display[r][c] = 2;
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{r, c});

        while (!stack.isEmpty()) {
            int[] cell = stack.pop();

            emit.at("reach").say("(%d,%d) is safe. Look for neighbouring 'O' cells that inherit "
                            + "that escape route.", cell[0], cell[1])
                    .var("cell", "(" + cell[0] + "," + cell[1] + ")")
                    .grid(display).stack(cells(stack)).step();

            for (int[] d : DIRECTIONS) {
                int nr = cell[0] + d[0];
                int nc = cell[1] + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols
                        || board[nr][nc] != 1 || safe[nr][nc]) {
                    emit.at("blocked").say("(%d,%d) is off the board, an 'X', or already marked - "
                                    + "the flood does not go there.", nr, nc)
                            .var("cell", "(" + nr + "," + nc + ")")
                            .grid(display).stack(cells(stack)).step();
                    continue;
                }
                safe[nr][nc] = true;
                display[nr][nc] = 2;
                stack.push(new int[]{nr, nc});

                emit.at("spread").say("(%d,%d) is an 'O' joined to a border cell, so it is safe too.",
                                nr, nc)
                        .var("cell", "(" + nr + "," + nc + ")")
                        .grid(display).stack(cells(stack)).step();
            }
        }
    }

    private static List<String> cells(Deque<int[]> stack) {
        List<String> out = new ArrayList<>();
        for (int[] cell : stack) {
            out.add("(" + cell[0] + "," + cell[1] + ")");
        }
        return out;
    }
}
