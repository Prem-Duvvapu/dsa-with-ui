package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Rotten Oranges: how many minutes until no fresh orange is left.
 *
 * <p>Rot spreads from every already-rotten orange at once, so this is multi-source BFS -
 * each of them enters the queue stamped with minute 0. BFS then visits cells in
 * nondecreasing minute order, which is what makes the minute stamped on a cell the FIRST
 * (and therefore smallest) minute any rotten neighbour could have reached it. The answer is
 * the largest stamp handed out, and -1 if any fresh orange was never reached at all.
 *
 * <p>The grid is mutated as it goes: a cell flips 1 -&gt; 2 the instant it is spoiled, which
 * is also what stops it being queued twice.
 */
@Component
public class RottenOrangesTracer implements AlgorithmTracer {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    @Override
    public String id() {
        return "rotten-oranges";
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
               public int orangesRotting(int[][] grid) {
                   // @a survey
                   int rows = grid.length, cols = grid[0].length;
                   Queue<int[]> queue = new LinkedList<>();
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

                   int elapsed = 0, spoiled = 0;
                   while (!queue.isEmpty()) {
                       // @a dequeue
                       int[] cell = queue.poll();
                       int r = cell[0], c = cell[1], minute = cell[2];
                       elapsed = Math.max(elapsed, minute);

                       for (int[] d : DIRECTIONS) {
                           int nr = r + d[0], nc = c + d[1];
                           if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || grid[nr][nc] != 1) {
                               // @a immune
                               continue;
                           }
                           // @a spread
                           grid[nr][nc] = 2;
                           spoiled++;
                           queue.add(new int[]{nr, nc, minute + 1});
                       }
                   }

                   if (spoiled == fresh) {
                       // @a cleared
                       return elapsed;
                   }
                   // @a stranded
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

        emit.at("survey").say("A %dx%d crate holding %d fresh orange(s) and %d rotten one(s). Every "
                        + "rotten orange starts the clock at minute 0.",
                        rows, cols, fresh, queue.size())
                .var("fresh", fresh).var("sources", queue.size())
                .grid(grid).queue(stamps(queue)).step();

        int elapsed = 0;
        int spoiled = 0;
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int r = cell[0];
            int c = cell[1];
            int minute = cell[2];
            elapsed = Math.max(elapsed, minute);

            emit.at("dequeue").say("Take (%d,%d), rotten as of minute %d. Check its four neighbours.",
                            r, c, minute)
                    .var("cell", "(" + r + "," + c + ")").var("minute", minute)
                    .var("remaining", fresh - spoiled)
                    .grid(grid).queue(stamps(queue)).step();

            for (int[] d : DIRECTIONS) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || grid[nr][nc] != 1) {
                    emit.at("immune").say("(%d,%d) is off the crate, empty, or already rotten - the "
                                    + "rot has nothing to do there.", nr, nc)
                            .var("cell", "(" + nr + "," + nc + ")")
                            .grid(grid).queue(stamps(queue)).step();
                    continue;
                }
                grid[nr][nc] = 2;
                spoiled++;
                queue.add(new int[]{nr, nc, minute + 1});

                emit.at("spread").say("(%d,%d) is fresh and shares an edge, so it spoils at minute "
                                + "%d. %d fresh orange(s) left.",
                                nr, nc, minute + 1, fresh - spoiled)
                        .var("cell", "(" + nr + "," + nc + ")").var("minute", minute + 1)
                        .var("remaining", fresh - spoiled)
                        .grid(grid).queue(stamps(queue)).step();
            }
        }

        if (spoiled == fresh) {
            emit.at("cleared").say("All %d fresh orange(s) spoiled; the last one went at minute %d. "
                            + "Answer: %d.", fresh, elapsed, elapsed)
                    .var("answer", elapsed).var("spoiled", spoiled)
                    .grid(grid).step();
        } else {
            emit.at("stranded").say("The queue is empty but %d fresh orange(s) were never reached - "
                            + "no amount of waiting rots them. Answer: -1.", fresh - spoiled)
                    .var("answer", -1).var("unreachable", fresh - spoiled)
                    .grid(grid).step();
        }
    }

    /** Queue entries carry the minute they were stamped with, which is the whole trick. */
    private static List<String> stamps(Deque<int[]> queue) {
        List<String> out = new ArrayList<>();
        for (int[] cell : queue) {
            out.add("(" + cell[0] + "," + cell[1] + ") min " + cell[2]);
        }
        return out;
    }
}
