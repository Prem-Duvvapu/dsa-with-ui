package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Counts actual squares independently of the DP recurrence and its narration. */
class CountSquaresNarrationTest {
    private final CountSquareSubmatricesTracer tracer = new CountSquareSubmatricesTracer();
    private final TraceRunner runner = new TraceRunner();
    private static final Pattern COUNT = Pattern.compile(
            "\\((\\d+),(\\d+)\\) contributes (\\d+) new all-ones squares? ending here: "
            + "one of each side length from 1 through (\\d+)\\. total=(\\d+)\\.");

    @Test
    void countNarrationNamesEveryNewSquareEndingAtTheCurrentCell() {
        for (int[][] grid : List.of(new int[][]{{1,1},{1,1}},
                new int[][]{{1,1,1},{1,1,1},{1,1,1}}, new int[][]{{1,0},{1,1}})) {
            var trace = runner.run(tracer, input(grid));
            int[][] expected = contributions(grid);
            int total = 0;
            for (var step : trace.getSteps()) if (step.getActiveLine() == trace.getAnchors().get("count")) {
                int r = Integer.parseInt(step.getVariables().get("r"));
                int c = Integer.parseInt(step.getVariables().get("c"));
                total += expected[r][c];
                var words = COUNT.matcher(step.getDescription());
                assertTrue(words.matches(), step.getDescription()); // RED on the old false explanation.
                assertEquals(r, Integer.parseInt(words.group(1)));
                assertEquals(c, Integer.parseInt(words.group(2)));
                assertEquals(expected[r][c], Integer.parseInt(words.group(3)));
                assertEquals(expected[r][c], Integer.parseInt(words.group(4)));
                assertEquals(total, Integer.parseInt(words.group(5)));
            }
        }
        assertEquals(2, contributions(new int[][]{{1,1},{1,1}})[1][1]);
        assertEquals(3, contributions(new int[][]{{1,1,1},{1,1,1},{1,1,1}})[2][2]);
    }

    @Test
    void allTwoByTwoMatricesAndLargerControlsPreserveActualTablesTotalsAnchorsAndEventTiming() {
        for (int mask = 0; mask < 16; mask++) {
            int[][] grid = new int[2][2];
            for (int cell = 0; cell < 4; cell++) grid[cell / 2][cell % 2] = (mask >>> cell) & 1;
            checkMath(grid);
        }
        checkMath(new int[][]{{1,1,1},{1,1,1},{1,1,1}});
        checkMath(new int[][]{{0,1,1,1},{1,1,1,1},{0,1,1,1}});
        int[][] max = new int[8][8];
        for (var row : max) Arrays.fill(row, 1);
        assertEquals(204, checkMath(max));
    }

    @Test
    void tinyBudgetStillPublishesOnlyAnHonestPartialRun() {
        AlgorithmTracer limited = new AlgorithmTracer() {
            public String id() { return tracer.id(); }
            public DsType dsType() { return tracer.dsType(); }
            public InputSpec inputSpec() { return tracer.inputSpec().withMaxSteps(3); }
            public Map<String,Object> alternateInput() { return tracer.alternateInput(); }
            public String annotatedCode() { return tracer.annotatedCode(); }
            public void run(Inputs in, StepEmitter emit) { tracer.run(in, emit); }
        };
        var trace = runner.run(limited, input(new int[][]{{1,1},{1,1}}));
        assertTrue(trace.isTruncated());
        assertEquals(3, trace.getStepCount());
        for (var step : trace.getSteps()) assertNotEquals(trace.getAnchors().get("done").intValue(), step.getActiveLine());
    }

    private int checkMath(int[][] original) {
        int[][] expected = contributions(original);
        int[][] current = Arrays.stream(original).map(int[]::clone).toArray(int[][]::new);
        var trace = runner.run(tracer, input(original));
        assertFalse(trace.isTruncated());
        var code = AnnotatedCode.parse(tracer.annotatedCode());
        assertEquals(Set.of("init", "compute", "count", "done"), code.getAnchors().keySet());
        assertEquals(code.getDisplayCode(), trace.getCode());
        int total = 0, counts = 0, computes = 0;
        for (var step : trace.getSteps()) {
            if (step.getActiveLine() == code.resolve("compute")) {
                int r = Integer.parseInt(step.getVariables().get("r"));
                int c = Integer.parseInt(step.getVariables().get("c"));
                assertEquals(expected[r][c], Integer.parseInt(step.getVariables().get("side")));
                assertArrayEquals(current, step.getGridState(), "compute stays before assignment");
                computes++;
            } else if (step.getActiveLine() == code.resolve("count")) {
                int r = Integer.parseInt(step.getVariables().get("r"));
                int c = Integer.parseInt(step.getVariables().get("c"));
                assertTrue(expected[r][c] > 0);
                current[r][c] = expected[r][c];
                total += expected[r][c];
                assertEquals(String.valueOf(total), step.getVariables().get("total"));
                assertArrayEquals(current, step.getGridState());
                counts++;
            } else if (step.getActiveLine() == code.resolve("init")) {
                assertArrayEquals(original, step.getGridState());
            } else {
                assertEquals(code.resolve("done"), step.getActiveLine());
                assertArrayEquals(expected, step.getGridState());
                assertEquals(String.valueOf(total), step.getVariables().get("total"));
            }
        }
        assertEquals(2 + counts + computes, trace.getStepCount());
        int ones = 0, interiorOnes = 0;
        for (int r = 0; r < original.length; r++) for (int c = 0; c < original[0].length; c++) {
            if (original[r][c] == 1) { ones++; if (r > 0 && c > 0) interiorOnes++; }
        }
        assertEquals(ones, counts);
        assertEquals(interiorOnes, computes);
        assertEquals(Arrays.stream(expected).flatMapToInt(Arrays::stream).sum(), total);
        return total;
    }

    /** Enumerates actual cells for every top-left/side, never uses the DP recurrence. */
    private static int[][] contributions(int[][] grid) {
        int[][] count = new int[grid.length][grid[0].length];
        for (int r = 0; r < grid.length; r++) for (int c = 0; c < grid[0].length; c++) {
            for (int side = 1; r + side <= grid.length && c + side <= grid[0].length; side++) {
                boolean all = true;
                for (int i = r; i < r + side; i++) for (int j = c; j < c + side; j++) all &= grid[i][j] == 1;
                if (all) count[r + side - 1][c + side - 1]++;
            }
        }
        return count;
    }
    private static Map<String,Object> input(int[][] grid) {
        return Map.of("grid", Arrays.stream(grid).map(row -> Arrays.stream(row).boxed().toList()).toList());
    }
}
