package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * The ONLINE version of counting islands: land arrives one cell at a time and the answer must
 * be reported after every single addition, so re-running a flood fill per operation would cost
 * O(k * m * n). DSU makes each addition local instead - a new land cell starts as its own
 * island (count++), and each of its up-to-four already-land neighbours that is not yet in the
 * same set merges into it (count--). Islands never split, only merge, which is precisely the
 * shape DSU supports and why the reverse problem (removing land) has no equally cheap answer.
 */
@Component
public class NumberOfIslands2Tracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "number-of-islands-2";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("rows", FieldType.INT)
                        .label("Rows")
                        .help("Height of the water-only starting board.")
                        .range(1, 8)
                        .defaultValue(3)
                        .build(),
                InputField.of("cols", FieldType.INT)
                        .label("Columns")
                        .help("Width of the water-only starting board.")
                        .range(1, 8)
                        .defaultValue(3)
                        .build(),
                InputField.of("operations", FieldType.INT_GRID)
                        .label("Add-land positions")
                        .help("Each row is one [row, col] position turned into land, in order.")
                        .constraint("maxRows", 16)
                        .constraint("maxCols", 2)
                        .values(0, 7)
                        .defaultValue(List.of(
                                List.of(0, 0), List.of(0, 1), List.of(1, 2), List.of(2, 1)))
                        .build());
    }

    /**
     * Positions chosen so one addition bridges two existing islands at once and a later one
     * touches the same island twice - neither happens on the defaults, where every step merges
     * at most one neighbour.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "rows", 3,
                "cols", 3,
                "operations", List.of(
                        List.of(0, 0), List.of(1, 1), List.of(0, 1), List.of(1, 0)));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> numIslands2(int m, int n, int[][] positions) {
                   // @a init
                   int[] parent = new int[m * n];
                   Arrays.fill(parent, -1);          // -1 means "still water"
                   int count = 0;
                   List<Integer> answer = new ArrayList<>();

                   for (int[] p : positions) {
                       int r = p[0], c = p[1], cell = r * n + c;
                       // @a addLand
                       if (parent[cell] == -1) {
                           parent[cell] = cell;
                           count++;
                       }

                       for (int[] d : DIRECTIONS) {
                           int nr = r + d[0], nc = c + d[1];
                           if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;
                           int nb = nr * n + nc;
                           if (parent[nb] == -1) continue;
                           if (find(cell, parent) != find(nb, parent)) {
                               // @a union
                               parent[find(nb, parent)] = find(cell, parent);
                               count--;
                           } else {
                               // @a sameSet
                               continue;                 // already one island
                           }
                       }

                       // @a report
                       answer.add(count);
                   }

                   // @a done
                   return answer;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int rows = in.getInt("rows");
        int cols = in.getInt("cols");
        int[][] operations = in.getGrid("operations");

        int[][] grid = new int[rows][cols];
        int[] parent = new int[rows * cols];
        Arrays.fill(parent, -1);
        int count = 0;
        List<Integer> answer = new ArrayList<>();

        emit.at("init")
                .say("A %dx%d board of water and %d add-land operation(s). parent[cell] = -1 marks "
                                + "water; the island count is reported after every single addition.",
                        rows, cols, operations.length)
                .var("islands", 0).var("answer", answer.toString())
                .grid(grid).step();

        for (int[] op : operations) {
            int r = op[0];
            int c = op.length > 1 ? op[1] : -1;

            if (r < 0 || r >= rows || c < 0 || c >= cols) {
                emit.at("addLand")
                        .say("(%d,%d) lies outside the %dx%d board - skip it, the count is unchanged at %d.",
                                r, c, rows, cols, count)
                        .var("islands", count).var("answer", answer.toString())
                        .grid(grid).step();
                continue;
            }

            int cell = r * cols + c;
            if (parent[cell] != -1) {
                emit.at("addLand")
                        .say("(%d,%d) is already land - the operation changes nothing and the count stays %d.",
                                r, c, count)
                        .var("islands", count).var("answer", answer.toString())
                        .grid(grid).step();
                answer.add(count);
                emit.at("report")
                        .say("Report the count after this operation: %d island(s). answer = %s.",
                                count, answer)
                        .var("islands", count).var("answer", answer.toString())
                        .grid(grid).step();
                continue;
            }

            parent[cell] = cell;
            grid[r][c] = 1;
            count++;
            emit.at("addLand")
                    .say("Turn (%d,%d) into land. On its own it is a brand-new island, so the count "
                                    + "rises to %d before any merging.", r, c, count)
                    .var("cell", "(" + r + "," + c + ")").var("islands", count)
                    .var("answer", answer.toString())
                    .grid(grid).step();

            for (int[] d : DIRECTIONS) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) {
                    continue;
                }
                int nb = nr * cols + nc;
                if (parent[nb] == -1) {
                    continue;
                }

                int ru = find(cell, parent);
                int rv = find(nb, parent);
                if (ru != rv) {
                    parent[rv] = ru;
                    count--;
                    emit.at("union")
                            .say("Neighbour (%d,%d) is land in a different island (root %d vs %d) - "
                                            + "merge them and drop the count to %d.",
                                    nr, nc, rv, ru, count)
                            .var("cell", "(" + r + "," + c + ")")
                            .var("neighbour", "(" + nr + "," + nc + ")")
                            .var("islands", count).var("answer", answer.toString())
                            .grid(grid).step();
                } else {
                    emit.at("sameSet")
                            .say("Neighbour (%d,%d) is land but already shares root %d with (%d,%d) - "
                                            + "no merge, so the count stays %d.",
                                    nr, nc, ru, r, c, count)
                            .var("cell", "(" + r + "," + c + ")")
                            .var("neighbour", "(" + nr + "," + nc + ")")
                            .var("islands", count).var("answer", answer.toString())
                            .grid(grid).step();
                }
            }

            answer.add(count);
            emit.at("report")
                    .say("Report the count after this operation: %d island(s). answer = %s.",
                            count, answer)
                    .var("islands", count).var("answer", answer.toString())
                    .grid(grid).step();
        }

        emit.at("done")
                .say("All %d operation(s) applied. Island count after each: %s.",
                        operations.length, answer)
                .var("islands", count).var("answer", answer.toString())
                .grid(grid).step();
    }

    private static int find(int x, int[] parent) {
        if (parent[x] != x) {
            parent[x] = find(parent[x], parent);
        }
        return parent[x];
    }
}
