package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Rotting Oranges (LeetCode 994), traced on the owner's own accepted submission: every rotten
 * orange is queued at the start, then a level-by-level BFS where one level is one minute.
 * Already optimal - O(m*n), each orange queued at most once.
 *
 * <p>The code never writes into {@code grid}; it marks {@code visited} and counts
 * {@code freshCnt} down. The canvas draws a fresh orange the BFS has reached as 2 (rotten), so
 * the spread is visible. {@code minutes} grows only when another level is waiting - the last
 * level rots nothing new, and counting it would be one minute too many.
 */
@Component
public class RottingOrangesTracer implements AlgorithmTracer {

    // The code's own direction order: up, right, down, left.
    private static final int[] D_ROW = {-1, 0, 1, 0};
    private static final int[] D_COL = {0, 1, 0, -1};

    @Override
    public String id() {
        return "rotting-oranges";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("grid", FieldType.INT_GRID)
                        .label("Crate")
                        .help("0 is an empty cell, 1 is a fresh orange, 2 is a rotten one. Rot "
                                + "crosses one shared edge per minute.")
                        .constraint("maxRows", 10)
                        .constraint("maxCols", 10)
                        .values(0, 2)
                        // Two rotten corners, nine fresh oranges, every one of them reachable.
                        // Hand-checked: the last to spoil is (2,1) at minute 3.
                        .defaultValue(List.of(
                                List.of(2, 1, 1, 0),
                                List.of(1, 1, 0, 1),
                                List.of(0, 1, 1, 1),
                                List.of(0, 0, 1, 2)))
                        .build());
    }

    /**
     * A single rotten orange walled off from three of the four fresh ones, so the queue
     * empties with fresh fruit still on the board and the answer is -1 - the branch the
     * default never reaches.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(2, 1, 0, 0),
                List.of(0, 0, 0, 1),
                List.of(1, 1, 0, 0)));
    }

    @Override
    public String annotatedCode() {
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

                   public int orangesRotting(int[][] grid) {
                       // @a init
                       int m = grid.length;
                       int n = grid[0].length;
                       int freshCnt = 0;
                       boolean[][] visited = new boolean[m][n];
                       Queue<Pair> q = new LinkedList<>();

                       for (int i=0;i<m;i++) {
                           for (int j=0;j<n;j++) {
                               if (grid[i][j] == 1) {
                                   freshCnt++;
                               } else if (grid[i][j] == 2) {
                                   visited[i][j] = true;
                                   q.add(new Pair(i,j));
                               }
                           }
                       }

                       return bfs(freshCnt,visited,q,grid,m,n);
                   }

                   private int bfs(int freshCnt,boolean[][] visited,Queue<Pair> q,int[][] grid,int m,int n) {
                       int minutes = 0;

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

                                   if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n && grid[newRow][newCol]==1 && !visited[newRow][newCol]) {
                                       // @a rot
                                       visited[newRow][newCol] = true;
                                       q.add(new Pair(newRow, newCol));
                                       freshCnt--;
                                   }
                               }
                           }

                           if (!q.isEmpty())
                               // @a minute
                               minutes++;
                       }

                       // @a done
                       return (freshCnt == 0) ? minutes : -1;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int m = grid.length;
        int n = grid[0].length;
        int freshCnt = 0;
        boolean[][] visited = new boolean[m][n];
        Deque<int[]> q = new ArrayDeque<>();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    freshCnt++;
                } else if (grid[i][j] == 2) {
                    visited[i][j] = true;
                    q.add(new int[]{i, j});
                }
            }
        }

        emit.at("init").say("%d fresh orange%s and %d rotten one%s. Every rotten orange goes in the "
                        + "queue at minute 0; each BFS level after that is one more minute.",
                        freshCnt, Narration.s(freshCnt), q.size(), Narration.s(q.size()))
                .var("freshCnt", freshCnt).var("minutes", 0)
                .grid(view(grid, visited)).queue(cells(q)).step();

        int minutes = 0;
        while (!q.isEmpty()) {
            int qlen = q.size();
            emit.at("level").say("Minute %d: %d rotten orange%s in the queue. Each one rots any fresh "
                            + "orange it shares an edge with.", minutes, qlen, Narration.s(qlen))
                    .var("qlen", qlen).var("minutes", minutes).var("freshCnt", freshCnt)
                    .grid(view(grid, visited)).queue(cells(q)).step();

            while (qlen-- > 0) {
                int[] curr = q.poll();
                int currRow = curr[0];
                int currCol = curr[1];
                emit.at("poll").say("Take (%d,%d) off the queue and look at its four neighbours.",
                                currRow, currCol)
                        .var("currRow", currRow).var("currCol", currCol).var("freshCnt", freshCnt)
                        .grid(view(grid, visited)).queue(cells(q)).step();

                for (int i = 0; i < 4; i++) {
                    int newRow = currRow + D_ROW[i];
                    int newCol = currCol + D_COL[i];
                    if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                            && grid[newRow][newCol] == 1 && !visited[newRow][newCol]) {
                        visited[newRow][newCol] = true;
                        q.add(new int[]{newRow, newCol});
                        freshCnt--;
                        emit.at("rot").say("(%d,%d) is a fresh orange next to (%d,%d), so it rots: mark it "
                                        + "visited and queue it. %d fresh left.",
                                        newRow, newCol, currRow, currCol, freshCnt)
                                .var("newRow", newRow).var("newCol", newCol).var("freshCnt", freshCnt)
                                .grid(view(grid, visited)).queue(cells(q)).step();
                    }
                }
            }

            if (!q.isEmpty()) {
                minutes++;
                emit.at("minute").say("Oranges rotted this minute, and they will spread next: minutes = %d.",
                                minutes)
                        .var("minutes", minutes).var("freshCnt", freshCnt)
                        .grid(view(grid, visited)).queue(cells(q)).step();
            }
        }

        int answer = freshCnt == 0 ? minutes : -1;
        emit.at("done").say(freshCnt == 0
                        ? "The queue is empty and no fresh orange is left, so return minutes = " + minutes + "."
                        : "The queue is empty but " + freshCnt + " fresh orange" + Narration.s(freshCnt)
                                + " never touched a rotten one - they can never rot. Return -1.")
                .var("freshCnt", freshCnt).var("minutes", minutes).var("answer", answer)
                .grid(view(grid, visited)).step();
    }

    /** A fresh orange the BFS has reached is drawn as 2: rotten. */
    private static int[][] view(int[][] grid, boolean[][] visited) {
        int[][] out = new int[grid.length][];
        for (int r = 0; r < grid.length; r++) {
            out[r] = grid[r].clone();
            for (int c = 0; c < grid[r].length; c++) {
                if (visited[r][c]) {
                    out[r][c] = 2;
                }
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
