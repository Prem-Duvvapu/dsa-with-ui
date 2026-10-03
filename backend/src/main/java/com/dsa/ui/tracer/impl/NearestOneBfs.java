package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.InputValidationException;
import com.dsa.ui.tracer.StepEmitter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Distance of the nearest cell having 1, for every cell of a 0/1 grid - shared by
 * {@code nearest-cell-1} and {@code distance-nearest-1}, which are the same problem catalogued
 * from two sheets.
 *
 * <p>The code is the owner's accepted LeetCode 542 (01 Matrix) submission with the roles of 0
 * and 1 swapped: 542 measures the distance to the nearest 0, this problem to the nearest 1.
 * Everything else is as written - a {@code Pair} queue seeded with every source cell, a
 * level-by-level BFS whose {@code dist} grows by one per level, and {@code res == 0} as the
 * "not reached yet" test. One BFS from all sources at once is O(m*n); a BFS per cell would be
 * O((m*n)^2).
 *
 * <p>The canvas draws a 0-cell no level has reached as -1. The code's {@code res} still holds 0
 * there, which is exactly why it can use {@code res[r][c] == 0} as its unvisited check.
 */
final class NearestOneBfs {

    // The code's own direction order: up, right, down, left.
    private static final int[] D_ROW = {-1, 0, 1, 0};
    private static final int[] D_COL = {0, 1, 0, -1};

    private NearestOneBfs() {
    }

    static String annotatedCode() {
        return """
               class Pair {
                   int row;
                   int col;

                   Pair(int row,int col) {
                       this.row = row;
                       this.col = col;
                   }
               }

               class Solution {
                   public static int[] dRow = {-1,0,1,0};
                   public static int[] dCol = {0,1,0,-1};

                   public int[][] nearest(int[][] grid) {
                       // @a init
                       int m = grid.length;
                       int n = grid[0].length;
                       Queue<Pair> q = new LinkedList<>();
                       int[][] res = new int[m][n];

                       for (int i=0;i<m;i++) {
                           for (int j=0;j<n;j++) {
                               if (grid[i][j] == 1)
                                   q.add(new Pair(i,j));
                           }
                       }

                       bfs(q,res,grid,m,n);
                       // @a done
                       return res;
                   }

                   public void bfs(Queue<Pair> q, int[][] res,int[][] grid,int m,int n) {
                       int dist = 1;

                       while (!q.isEmpty()) {
                           // @a level
                           int qlen = q.size();

                           while (qlen-- > 0) {
                               // @a poll
                               Pair curr = q.poll();
                               int currRow = curr.row;
                               int currCol = curr.col;

                               for (int i=0;i<4;i++) {
                                   int newRow = currRow + dRow[i];
                                   int newCol = currCol + dCol[i];

                                   if (newRow>=0 && newRow<m && newCol>=0 && newCol<n && grid[newRow][newCol]==0 && res[newRow][newCol]==0) {
                                       // @a reach
                                       res[newRow][newCol] = dist;
                                       q.add(new Pair(newRow, newCol));
                                   }
                               }
                           }

                           // @a next
                           dist++;
                       }
                   }
               }""";
    }

    static void run(int[][] grid, StepEmitter emit) {
        int m = grid.length;
        int n = grid[0].length;
        Deque<int[]> q = new ArrayDeque<>();
        int[][] res = new int[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    q.add(new int[]{i, j});
                }
            }
        }
        if (q.isEmpty()) {
            // The problem guarantees at least one 1. Without one, res would stay all 0 - every
            // cell claiming a distance of 0 to a 1 that does not exist - so refuse the input.
            throw new InputValidationException(Map.of("grid",
                    "needs at least one 1: with no 1 there is no nearest one to measure to"));
        }

        emit.at("init").say("Every 1 is its own nearest 1, at distance 0: put all %d of them in the "
                        + "queue together. The BFS then moves outward one step per level, so the "
                        + "first level to reach a 0-cell gives its distance.", q.size())
                .var("sources", q.size()).grid(view(grid, res)).queue(cells(q)).step();

        int dist = 1;
        while (!q.isEmpty()) {
            int qlen = q.size();
            emit.at("level").say("Level %d: %d cell%s in the queue. Any 0-neighbour of theirs that no "
                            + "earlier level reached is %d step%s from a 1.", dist, qlen, Narration.s(qlen), dist, Narration.s(dist))
                    .var("dist", dist).var("qlen", qlen).grid(view(grid, res)).queue(cells(q)).step();

            while (qlen-- > 0) {
                int[] curr = q.poll();
                int currRow = curr[0];
                int currCol = curr[1];
                emit.at("poll").say("Take (%d,%d) off the queue and look at its four neighbours.",
                                currRow, currCol)
                        .var("dist", dist).var("currRow", currRow).var("currCol", currCol)
                        .grid(view(grid, res)).queue(cells(q)).step();

                for (int i = 0; i < 4; i++) {
                    int newRow = currRow + D_ROW[i];
                    int newCol = currCol + D_COL[i];
                    if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                            && grid[newRow][newCol] == 0 && res[newRow][newCol] == 0) {
                        res[newRow][newCol] = dist;
                        q.add(new int[]{newRow, newCol});
                        emit.at("reach").say("(%d,%d) is a 0 no earlier level reached, so its nearest 1 "
                                        + "is %d step%s away: res[%d][%d] = %d.",
                                        newRow, newCol, dist, Narration.s(dist), newRow, newCol, dist)
                                .var("dist", dist).var("newRow", newRow).var("newCol", newCol)
                                .grid(view(grid, res)).queue(cells(q)).step();
                    }
                }
            }

            dist++;
            emit.at("next").say(q.isEmpty()
                            ? "The level reached nothing new and the queue is empty: every cell has its distance."
                            : "Level done. The cells it reached form the next level, one step farther: dist = " + dist + ".")
                    .var("dist", dist).grid(view(grid, res)).queue(cells(q)).step();
        }

        emit.at("done").say("Return res: each cell's distance to its nearest 1.")
                .var("res", GridText.of(res)).grid(view(grid, res)).step();
    }

    /** res as the canvas draws it: a 0-cell not reached yet is -1, not res's placeholder 0. */
    private static int[][] view(int[][] grid, int[][] res) {
        int[][] out = new int[grid.length][grid[0].length];
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                out[r][c] = grid[r][c] == 0 && res[r][c] == 0 ? -1 : res[r][c];
            }
        }
        return out;
    }

    private static List<String> cells(Deque<int[]> q) {
        List<String> out = new ArrayList<>();
        for (int[] cell : q) {
            out.add("(" + cell[0] + "," + cell[1] + ")");
        }
        return out;
    }
}
