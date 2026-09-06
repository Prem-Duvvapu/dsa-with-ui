package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Number of Enclaves (LeetCode 1020): count the land cells from which you cannot walk off
 * the grid, moving one step at a time between 4-directionally adjacent land.
 *
 * <p>Asking that question of each land cell separately repeats the same search over and
 * over. Ask the complement instead - which land CAN escape - and the answer falls out of a
 * single multi-source BFS seeded from every land cell already sitting on the boundary.
 * Whatever that flood fails to reach is, by definition, an enclave.
 *
 * <p>Display encoding: 0 sea, 1 land, 2 land that reaches the boundary, 3 land counted as
 * an enclave. The finished grid therefore shows which cells the answer is made of, not just
 * how many there were.
 */
@Component
public class NumberOfEnclavesTracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "number-of-enclaves";
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
                        .help("1 is land, 0 is sea. Land that can reach the boundary is redrawn as "
                                + "2; a counted enclave cell is redrawn as 3.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(0, 1)
                        // LeetCode 1020 example 1: (1,0) walks off the left edge; the three
                        // cells (1,2), (2,1) and (2,2) cannot. Answer: 3.
                        .defaultValue(List.of(
                                List.of(0, 0, 0, 0),
                                List.of(1, 0, 1, 0),
                                List.of(0, 1, 1, 0),
                                List.of(0, 0, 0, 0)))
                        .build());
    }

    /**
     * A wider board on which every land cell is either on the boundary or joined to one, so
     * the flood claims all of it and the answer is 0 - where the default leaves an enclave
     * behind.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 1, 0, 0, 0),
                List.of(0, 1, 0, 1, 1),
                List.of(0, 0, 0, 0, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int numEnclaves(int[][] grid) {
                   // @a init
                   int n = grid.length, m = grid[0].length;
                   boolean[][] escapes = new boolean[n][m];
                   Queue<int[]> queue = new LinkedList<>();

                   for (int i = 0; i < n; i++) {
                       for (int j = 0; j < m; j++) {
                           boolean edge = i == 0 || i == n - 1 || j == 0 || j == m - 1;
                           if (edge && grid[i][j] == 1 && !escapes[i][j]) {
                               // @a boundary
                               escapes[i][j] = true;
                               queue.add(new int[]{i, j});
                           }
                       }
                   }

                   while (!queue.isEmpty()) {
                       // @a walk
                       int[] cell = queue.poll();
                       for (int[] d : DIRECTIONS) {
                           int r = cell[0] + d[0], c = cell[1] + d[1];
                           if (r < 0 || r >= n || c < 0 || c >= m
                                   || grid[r][c] == 0 || escapes[r][c]) {
                               // @a stuck
                               continue;
                           }
                           // @a follow
                           escapes[r][c] = true;
                           queue.add(new int[]{r, c});
                       }
                   }

                   int enclaves = 0;
                   for (int i = 0; i < n; i++) {
                       for (int j = 0; j < m; j++) {
                           if (grid[i][j] == 1 && !escapes[i][j]) {
                               // @a trapped
                               enclaves++;
                           }
                       }
                   }
                   // @a done
                   return enclaves;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;
        boolean[][] escapes = new boolean[rows][cols];
        int[][] display = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            display[r] = grid[r].clone();
        }

        emit.at("init").say("A %dx%d map. Instead of asking which land is trapped, flood from the "
                        + "boundary and see which land escapes; the rest is the answer.", rows, cols)
                .var("rows", rows).var("cols", cols).grid(display).step();

        Deque<int[]> queue = new ArrayDeque<>();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                boolean edge = i == 0 || i == rows - 1 || j == 0 || j == cols - 1;
                if (!edge || grid[i][j] != 1 || escapes[i][j]) {
                    continue;
                }
                escapes[i][j] = true;
                display[i][j] = 2;
                queue.add(new int[]{i, j});

                emit.at("boundary").say("(%d,%d) is land on the boundary - one step and you are off "
                                + "the map. Seed the flood there.", i, j)
                        .var("cell", "(" + i + "," + j + ")").var("seeds", queue.size())
                        .grid(display).queue(cells(queue)).step();
            }
        }

        int escaped = queue.size();
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();

            emit.at("walk").say("Walk out from (%d,%d): any land next to it can reach the boundary "
                            + "the same way.", cell[0], cell[1])
                    .var("cell", "(" + cell[0] + "," + cell[1] + ")").var("escaped", escaped)
                    .grid(display).queue(cells(queue)).step();

            for (int[] d : DIRECTIONS) {
                int r = cell[0] + d[0];
                int c = cell[1] + d[1];
                if (r < 0 || r >= rows || c < 0 || c >= cols
                        || grid[r][c] == 0 || escapes[r][c]) {
                    emit.at("stuck").say("(%d,%d) is off the map, sea, or already known to escape - "
                                    + "nothing to add.", r, c)
                            .var("cell", "(" + r + "," + c + ")")
                            .grid(display).queue(cells(queue)).step();
                    continue;
                }
                escapes[r][c] = true;
                display[r][c] = 2;
                escaped++;
                queue.add(new int[]{r, c});

                emit.at("follow").say("(%d,%d) is land joined to escaping land, so it escapes too.",
                                r, c)
                        .var("cell", "(" + r + "," + c + ")").var("escaped", escaped)
                        .grid(display).queue(cells(queue)).step();
            }
        }

        int enclaves = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] != 1 || escapes[i][j]) {
                    continue;
                }
                enclaves++;
                display[i][j] = 3;
                emit.at("trapped").say("(%d,%d) is land the boundary flood never reached - enclave "
                                + "cell #%d.", i, j, enclaves)
                        .var("cell", "(" + i + "," + j + ")").var("enclaves", enclaves)
                        .grid(display).step();
            }
        }

        emit.at("done").say("%d land cell(s) can walk off the map and %d cannot. Answer: %d.",
                        escaped, enclaves, enclaves)
                .var("escaped", escaped).var("enclaves", enclaves)
                .grid(display).step();
    }

    private static List<String> cells(Deque<int[]> queue) {
        List<String> out = new ArrayList<>();
        for (int[] cell : queue) {
            out.add("(" + cell[0] + "," + cell[1] + ")");
        }
        return out;
    }
}
