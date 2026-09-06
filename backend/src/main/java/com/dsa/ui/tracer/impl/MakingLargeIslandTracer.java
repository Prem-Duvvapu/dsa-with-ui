package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Flipping one 0 to 1 can join up to four DIFFERENT islands at once, so the answer is not
 * "biggest island + 1". The trick is to do the work once instead of once per candidate: label
 * every existing island with a DSU root and record each root's size, then each 0-cell is
 * answered in O(1) by summing the sizes of its DISTINCT neighbouring roots and adding itself.
 * Deduplicating the roots is the whole subtlety - two neighbours of a 0-cell are very often
 * the same island, and counting it twice inflates the answer.
 *
 * <p>The all-land grid is a real case, not an edge case to shrug at: there is no 0 to flip, so
 * the answer is the whole board rather than 0.
 */
@Component
public class MakingLargeIslandTracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "making-large-island";
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
                        .help("1 is land, 0 is water. At most one 0 may be flipped to 1.")
                        .constraint("maxRows", 6)
                        .constraint("maxCols", 6)
                        .values(0, 1)
                        .defaultValue(List.of(
                                List.of(1, 1, 0),
                                List.of(1, 0, 1),
                                List.of(0, 1, 1)))
                        .build());
    }

    /** All land: there is no 0 to flip, so the answer is the whole board (LeetCode 827, example 3). */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(1, 1),
                List.of(1, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int largestIsland(int[][] grid) {
                   int n = grid.length, m = grid[0].length;
                   // @a init
                   int[] parent = new int[n * m];
                   for (int i = 0; i < n * m; i++) parent[i] = i;

                   for (int r = 0; r < n; r++) {
                       for (int c = 0; c < m; c++) {
                           if (grid[r][c] == 0) continue;
                           for (int[] d : new int[][]{{0, 1}, {1, 0}}) {
                               int nr = r + d[0], nc = c + d[1];
                               if (nr >= n || nc >= m || grid[nr][nc] == 0) continue;
                               // @a union
                               union(r * m + c, nr * m + nc, parent);
                           }
                       }
                   }

                   Map<Integer, Integer> size = new HashMap<>();
                   for (int cell = 0; cell < n * m; cell++) {
                       if (grid[cell / m][cell % m] == 1) {
                           // @a sizes
                           size.merge(find(cell, parent), 1, Integer::sum);
                       }
                   }

                   int best = 0;
                   for (int r = 0; r < n; r++) {
                       for (int c = 0; c < m; c++) {
                           if (grid[r][c] == 1) continue;
                           Set<Integer> roots = new HashSet<>();
                           for (int[] d : DIRECTIONS) {
                               int nr = r + d[0], nc = c + d[1];
                               if (nr < 0 || nr >= n || nc < 0 || nc >= m) continue;
                               if (grid[nr][nc] == 1) roots.add(find(nr * m + nc, parent));
                           }
                           // @a tryZero
                           int area = 1;
                           for (int root : roots) area += size.get(root);
                           if (area > best) {
                               // @a best
                               best = area;
                           }
                       }
                   }

                   if (best == 0) {
                       // @a allLand
                       return n * m;               // no 0 anywhere to flip
                   }
                   // @a done
                   return best;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;
        int[] parent = new int[rows * cols];
        for (int i = 0; i < parent.length; i++) parent[i] = i;

        int land = 0;
        int water = 0;
        for (int[] row : grid) {
            for (int v : row) {
                if (v == 1) land++;
                else water++;
            }
        }

        emit.at("init")
                .say("%dx%d grid: %d land cell(s), %d water cell(s). Every land cell starts as its "
                                + "own island; the right and down neighbours are enough to union them all.",
                        rows, cols, land, water)
                .var("land", land).var("water", water)
                .grid(labelled(grid, parent, rows, cols)).step();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 0) {
                    continue;
                }
                for (int[] d : new int[][]{{0, 1}, {1, 0}}) {
                    int nr = r + d[0];
                    int nc = c + d[1];
                    if (nr >= rows || nc >= cols || grid[nr][nc] == 0) {
                        continue;
                    }
                    int ru = find(r * cols + c, parent);
                    int rv = find(nr * cols + nc, parent);
                    if (ru == rv) {
                        continue;
                    }
                    parent[rv] = ru;
                    emit.at("union")
                            .say("(%d,%d) and (%d,%d) are both land and adjacent - union them under root %d.",
                                    r, c, nr, nc, ru)
                            .var("cell", "(" + r + "," + c + ")")
                            .var("neighbour", "(" + nr + "," + nc + ")")
                            .grid(labelled(grid, parent, rows, cols)).step();
                }
            }
        }

        Map<Integer, Integer> size = new LinkedHashMap<>();
        for (int cell = 0; cell < rows * cols; cell++) {
            if (grid[cell / cols][cell % cols] == 1) {
                size.merge(find(cell, parent), 1, Integer::sum);
            }
        }
        for (Map.Entry<Integer, Integer> island : size.entrySet()) {
            emit.at("sizes")
                    .say("Island %d (root cell %d) covers %d cell(s).",
                            labelOf(island.getKey(), size), island.getKey(), island.getValue())
                    .var("island", labelOf(island.getKey(), size))
                    .var("size", island.getValue())
                    .var("sizes", sizesString(size))
                    .grid(labelled(grid, parent, rows, cols)).step();
        }

        if (water == 0) {
            emit.at("allLand")
                    .say("There is no water cell to flip, so no move can grow anything - the answer "
                                    + "is the whole %dx%d board: %d.", rows, cols, rows * cols)
                    .var("answer", rows * cols)
                    .var("sizes", sizesString(size))
                    .grid(labelled(grid, parent, rows, cols)).step();
            emit.at("done")
                    .say("Largest island reachable by flipping at most one 0: %d.", rows * cols)
                    .var("answer", rows * cols)
                    .grid(labelled(grid, parent, rows, cols)).step();
            return;
        }

        int best = 0;
        String bestCell = "-";
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 1) {
                    continue;
                }
                LinkedHashSet<Integer> roots = new LinkedHashSet<>();
                for (int[] d : DIRECTIONS) {
                    int nr = r + d[0];
                    int nc = c + d[1];
                    if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || grid[nr][nc] == 0) {
                        continue;
                    }
                    roots.add(find(nr * cols + nc, parent));
                }
                int area = 1;
                StringBuilder parts = new StringBuilder("1");
                for (int root : roots) {
                    area += size.get(root);
                    parts.append(" + ").append(size.get(root));
                }

                emit.at("tryZero")
                        .say("Flip (%d,%d): it touches %d distinct island(s) %s, so the merged area "
                                        + "would be %s = %d.",
                                r, c, roots.size(), labelsOf(roots, size), parts, area)
                        .var("cell", "(" + r + "," + c + ")")
                        .var("islands", labelsOf(roots, size))
                        .var("area", area).var("best", best)
                        .grid(labelled(grid, parent, rows, cols)).step();

                if (area > best) {
                    best = area;
                    bestCell = "(" + r + "," + c + ")";
                    emit.at("best")
                            .say("%d beats the previous best, so (%d,%d) is the flip to remember.",
                                    area, r, c)
                            .var("cell", "(" + r + "," + c + ")")
                            .var("best", best)
                            .grid(labelled(grid, parent, rows, cols)).step();
                }
            }
        }

        emit.at("done")
                .say("Every water cell tried. Flipping %s gives the largest island: %d cell(s).",
                        bestCell, best)
                .var("answer", best).var("cell", bestCell)
                .grid(labelled(grid, parent, rows, cols)).step();
    }

    private static int find(int x, int[] parent) {
        if (parent[x] != x) {
            parent[x] = find(parent[x], parent);
        }
        return parent[x];
    }

    private static int rootOf(int x, int[] parent) {
        while (parent[x] != x) {
            x = parent[x];
        }
        return x;
    }

    /** Water stays 0; every land cell shows its island's 1-based label, so islands read apart. */
    private static int[][] labelled(int[][] grid, int[] parent, int rows, int cols) {
        Map<Integer, Integer> labels = new LinkedHashMap<>();
        int[][] out = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 0) {
                    continue;
                }
                int root = rootOf(r * cols + c, parent);
                out[r][c] = labels.computeIfAbsent(root, k -> labels.size() + 1);
            }
        }
        return out;
    }

    private static int labelOf(int root, Map<Integer, Integer> size) {
        int label = 1;
        for (Integer key : size.keySet()) {
            if (key == root) {
                return label;
            }
            label++;
        }
        return label;
    }

    private static String labelsOf(Collection<Integer> roots, Map<Integer, Integer> size) {
        List<Integer> labels = new ArrayList<>();
        for (int root : roots) {
            labels.add(labelOf(root, size));
        }
        Collections.sort(labels);
        return labels.isEmpty() ? "{}" : labels.toString();
    }

    private static String sizesString(Map<Integer, Integer> size) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> e : size.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("island ").append(labelOf(e.getKey(), size)).append(" = ").append(e.getValue());
        }
        return sb.toString();
    }
}
