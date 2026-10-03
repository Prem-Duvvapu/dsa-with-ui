package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Making A Large Island (LeetCode 827), traced on the owner's own accepted submission over their
 * DisjointSet. First every land cell is unioned with its land neighbours, so each island is one
 * set whose root knows its size. Then each water cell is imagined flipped: it joins the distinct
 * islands around it, 1 + the sum of their sizes. A grid that is all land has nothing to flip, so
 * the answer is n * n. O(n^2 * alpha).
 *
 * <p>The code numbers cell (i, j) as n*i + j, so the grid must be square (LeetCode guarantees n x n);
 * the input field declares {@code square} and InputValidator refuses any other shape. During the second pass the canvas shows each land cell as the size
 * of its island, and the water cell being tried as the size flipping it would give.
 */
@Component
public class MakingLargeIslandTracer implements AlgorithmTracer {

    // The code's own direction order: up, right, down, left.
    private static final int[] D_ROW = {-1, 0, 1, 0};
    private static final int[] D_COL = {0, 1, 0, -1};

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
                        .square()
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
        return OwnerDisjointSet.code(Set.of()) + "\n\n" + """
               class Solution {
                   public int[] dRow = {-1,0,1,0};
                   public int[] dCol = {0,1,0,-1};

                   public int largestIsland(int[][] grid) {
                       // @a init
                       int n = grid.length;
                       DisjointSet ds = new DisjointSet(n*n);
                       int maxSize = 0;

                       for (int i=0;i<n;i++) {
                           for (int j=0;j<n;j++) {
                               if (grid[i][j] == 0)
                                   continue;

                               int node = n*i + j;
                               for (int k=0;k<4;k++) {
                                   int newRow = i + dRow[k];
                                   int newCol = j + dCol[k];
                                   int newNode = n*newRow + newCol;

                                   if (newRow>=0 && newRow<n && newCol>=0 && newCol<n && grid[newRow][newCol]==1)
                                       // @a union
                                       ds.unionBySize(node, newNode);
                               }
                           }
                       }

                       int oneCnt = 0;
                       for (int i=0;i<n;i++) {
                           for (int j=0;j<n;j++) {
                               if (grid[i][j] == 1) {
                                   oneCnt++;
                                   continue;
                               }

                               Set<Integer> set = new HashSet<>();
                               for (int k=0;k<4;k++) {
                                   int newRow = i + dRow[k];
                                   int newCol = j+ dCol[k];
                                   int newNode = n*newRow + newCol;

                                   if (newRow>=0 && newRow<n && newCol>=0 && newCol<n && grid[newRow][newCol]==1) {
                                       set.add(ds.getUltimateParent(newNode));
                                   }
                               }

                               int currSize = 1;
                               for (int uniqueParent: set)
                                   currSize += ds.size[uniqueParent];

                               // @a flip
                               maxSize = Math.max(maxSize, currSize);
                           }
                       }

                       if (oneCnt == n*n)
                           // @a allLand
                           return n*n;

                       // @a done
                       return maxSize;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int n = grid.length;
        OwnerDisjointSet ds = new OwnerDisjointSet(n * n);
        int maxSize = 0;

        emit.at("init").say("A %dx%d grid. First join every land cell to its land neighbours, so each island "
                        + "becomes one set that knows its size.", n, n)
                .var("maxSize", 0).grid(grid).step();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 0) continue;
                int node = n * i + j;
                for (int k = 0; k < 4; k++) {
                    int newRow = i + D_ROW[k];
                    int newCol = j + D_COL[k];
                    int newNode = n * newRow + newCol;
                    if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n && grid[newRow][newCol] == 1) {
                        OwnerDisjointSet.Union result = ds.union(node, newNode);
                        if (!OwnerDisjointSet.SAME_SET.equals(result.branch())) {
                            emit.at("union").say("Land (%d,%d) and land (%d,%d) touch, so they are one island. %s",
                                            i, j, newRow, newCol, result.narrate(node, newNode))
                                    .var("node", node).var("newNode", newNode)
                                    .grid(grid).step();
                        }
                    }
                }
            }
        }

        int oneCnt = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    oneCnt++;
                    continue;
                }
                Set<Integer> set = new LinkedHashSet<>();
                for (int k = 0; k < 4; k++) {
                    int newRow = i + D_ROW[k];
                    int newCol = j + D_COL[k];
                    int newNode = n * newRow + newCol;
                    if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n && grid[newRow][newCol] == 1) {
                        set.add(ds.find(newNode, new ArrayList<>()));
                    }
                }
                int currSize = 1;
                List<String> parts = new ArrayList<>();
                for (int uniqueParent : set) {
                    currSize += ds.size[uniqueParent];
                    parts.add(String.valueOf(ds.size[uniqueParent]));
                }
                maxSize = Math.max(maxSize, currSize);
                emit.at("flip").say(set.isEmpty()
                                ? "Flipping water (" + i + "," + j + ") would give an island of just 1: no land touches it."
                                : "Flipping water (" + i + "," + j + ") joins " + set.size() + " distinct island"
                                        + Narration.s(set.size()) + ": 1 + " + String.join(" + ", parts) + " = " + currSize
                                        + ". maxSize = " + maxSize + ".")
                        .var("i", i).var("j", j).var("currSize", currSize).var("maxSize", maxSize)
                        .grid(sizes(grid, ds, n, i, j, currSize)).step();
            }
        }

        if (oneCnt == n * n) {
            emit.at("allLand").say("Every cell is already land - there is no water to flip, and the whole grid is "
                            + "one island of %d. Return n * n.", n * n)
                    .var("oneCnt", oneCnt).var("answer", n * n).grid(grid).step();
            return;
        }
        emit.at("done").say("The best flip gives an island of %d. Return maxSize.", maxSize)
                .var("maxSize", maxSize).var("answer", maxSize).grid(sizes(grid, ds, n, -1, -1, 0)).step();
    }

    /**
     * Land drawn as the size of its island; the water cell being tried as the size its flip would
     * produce; other water as 0.
     */
    private static int[][] sizes(int[][] grid, OwnerDisjointSet ds, int n, int ti, int tj, int flipSize) {
        int[][] out = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    int r = n * i + j;
                    while (ds.parent[r] != r) r = ds.parent[r];
                    out[i][j] = ds.size[r];
                } else if (i == ti && j == tj) {
                    out[i][j] = flipSize;
                }
            }
        }
        return out;
    }
}
