package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Minutes until every fresh orange rots, by multi-source BFS: every already-rotten orange
 * seeds the queue at minute 0, so the first time BFS reaches a fresh cell is guaranteed to
 * be the minimum number of minutes it takes.
 */
@Component
public class RottingOrangesTracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

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
                        .label("Grid")
                        .help("0 is empty, 1 is fresh, 2 is rotten. A fresh orange adjacent to a "
                                + "rotten one turns rotten every minute.")
                        .constraint("maxRows", 12)
                        .constraint("maxCols", 12)
                        .values(0, 2)
                        .defaultValue(List.of(
                                List.of(2, 1, 1),
                                List.of(1, 1, 0),
                                List.of(0, 1, 1)))
                        .build());
    }

    /** An isolated fresh orange that no rotten one can ever reach - the impossible branch. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("grid", List.of(
                List.of(2, 1, 0),
                List.of(0, 0, 0),
                List.of(0, 0, 1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int orangesRotting(int[][] grid) {
                   // @a init
                   int rows = grid.length, cols = grid[0].length;
                   Queue<int[]> queue = new LinkedList<>();
                   int fresh = 0;
                   for (int r = 0; r < rows; r++) {
                       for (int c = 0; c < cols; c++) {
                           if (grid[r][c] == 2) queue.add(new int[]{r, c, 0});
                           else if (grid[r][c] == 1) fresh++;
                       }
                   }

                   int minutes = 0, rotted = 0;
                   while (!queue.isEmpty()) {
                       // @a poll
                       int[] cell = queue.poll();
                       int r = cell[0], c = cell[1], t = cell[2];
                       minutes = Math.max(minutes, t);

                       for (int[] d : DIRECTIONS) {
                           int nr = r + d[0], nc = c + d[1];
                           if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || grid[nr][nc] != 1) {
                               // @a skip
                               continue;
                           }
                           // @a rot
                           grid[nr][nc] = 2;
                           rotted++;
                           queue.add(new int[]{nr, nc, t + 1});
                       }
                   }

                   if (rotted == fresh) {
                       // @a done
                       return minutes;
                   }
                   // @a impossible
                   return -1;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] grid = in.getGrid("grid");
        int rows = grid.length;
        int cols = grid[0].length;

        Deque<int[]> queue = new ArrayDeque<>();
        int fresh = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 2) {
                    queue.add(new int[]{r, c, 0});
                } else if (grid[r][c] == 1) {
                    fresh++;
                }
            }
        }

        emit.at("init").say("%dx%d grid, %d fresh orange(s). Seed the queue with every already-rotten cell at minute 0.",
                        rows, cols, fresh)
                .var("fresh", fresh).grid(grid).queue(minuteLabels(queue)).step();

        int minutes = 0;
        int rotted = 0;
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int r = cell[0], c = cell[1], t = cell[2];
            minutes = Math.max(minutes, t);

            emit.at("poll").say("Dequeue (%d,%d) at minute %d.", r, c, t)
                    .var("cell", "(" + r + "," + c + ")").var("minute", t)
                    .grid(grid).queue(minuteLabels(queue)).step();

            for (int[] d : DIRECTIONS) {
                int nr = r + d[0], nc = c + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || grid[nr][nc] != 1) {
                    emit.at("skip").say("(%d,%d) is out of bounds, empty, or already rotten - nothing to do.",
                                    nr, nc)
                            .var("cell", "(" + nr + "," + nc + ")")
                            .grid(grid).queue(minuteLabels(queue)).step();
                    continue;
                }
                grid[nr][nc] = 2;
                rotted++;
                queue.add(new int[]{nr, nc, t + 1});

                emit.at("rot").say("(%d,%d) was fresh and touches a rotten cell. It rots at minute %d.",
                                nr, nc, t + 1)
                        .var("cell", "(" + nr + "," + nc + ")").var("minute", t + 1).var("rotted", rotted)
                        .grid(grid).queue(minuteLabels(queue)).step();
            }
        }

        if (rotted == fresh) {
            emit.at("done").say("All %d fresh orange(s) rotted. Total time: %d minute(s).", fresh, minutes)
                    .var("minutes", minutes).grid(grid).step();
        } else {
            emit.at("impossible").say("%d of %d fresh orange(s) were never reached - impossible.",
                            fresh - rotted, fresh)
                    .var("unreachable", fresh - rotted).grid(grid).step();
        }
    }

    private static List<String> minuteLabels(Deque<int[]> queue) {
        List<String> out = new ArrayList<>();
        for (int[] cell : queue) {
            out.add("(" + cell[0] + "," + cell[1] + ")@" + cell[2]);
        }
        return out;
    }
}
