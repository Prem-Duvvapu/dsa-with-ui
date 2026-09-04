package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimum falling path sum: the first recurrence traced here with up to three live
 * predecessors — up-left, directly up, up-right — rather than two. It is also the first
 * where the answer is not one fixed cell: a fall may *start* anywhere in the top row, so
 * every cell of row 0 seeds the table, and the answer is the minimum across the *entire*
 * bottom row, not a single corner.
 *
 * <p>Because either diagonal predecessor can fall off the matrix, the number of live
 * candidates genuinely varies by column — column 0 and the last column each lose one — and
 * that count is carried in the trace so a learner can see the edge case rather than infer it.
 */
@Component
public class MinimumFallingPathSumTracer implements AlgorithmTracer {

    private static final String FORMULA =
            "dp[i][j] = matrix[i][j] + min(dp[i-1][j-1], dp[i-1][j], dp[i-1][j+1])";

    @Override
    public String id() {
        return "minimum-falling-path-sum";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("matrix", FieldType.INT_GRID)
                        .label("Matrix")
                        .help("A fall may start at any column of the top row and move down, "
                                + "each step landing directly below or one column to either "
                                + "side.")
                        .constraint("maxRows", 8)
                        .constraint("maxCols", 8)
                        .values(-100, 100)
                        .defaultValue(List.of(
                                List.of(2, 1, 3),
                                List.of(6, 5, 4),
                                List.of(7, 8, 9)))
                        .build());
    }

    /** Smaller, with a lopsided winner, so the falling path favours one edge, not the middle. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("matrix", List.of(
                List.of(100, 100),
                List.of(1, 100)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int minFallingPathSum(int[][] matrix) {
                   int rows = matrix.length, cols = matrix[0].length;
                   // @a init
                   int[][] dp = new int[rows][cols];
                   for (int j = 0; j < cols; j++) {
                       // @a base
                       dp[0][j] = matrix[0][j];
                   }
                   for (int i = 1; i < rows; i++) {
                       for (int j = 0; j < cols; j++) {
                           int best = dp[i - 1][j];
                           if (j > 0) best = Math.min(best, dp[i - 1][j - 1]);
                           if (j < cols - 1) best = Math.min(best, dp[i - 1][j + 1]);
                           // @a combine
                           dp[i][j] = matrix[i][j] + best;
                       }
                   }
                   int answer = dp[rows - 1][0];
                   for (int j = 1; j < cols; j++) {
                       // @a reduce
                       answer = Math.min(answer, dp[rows - 1][j]);
                   }
                   // @a done
                   return answer;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] matrix = in.getGrid("matrix");
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[][] dp = new int[rows][cols];
        boolean[][] settled = new boolean[rows][cols];

        emit.at("init")
                .say("A %d by %d matrix. A fall may start at any column of row 0, so every "
                        + "cell of the top row seeds the table with its own value — there is "
                        + "no single starting cell.", rows, cols)
                .var("rows", rows).var("cols", cols)
                .dpTable(table(dp, settled, null, "?", Set.of(), false)).step();

        for (int j = 0; j < cols; j++) {
            dp[0][j] = matrix[0][j];
            settled[0][j] = true;
            emit.at("base")
                    .say("Cell (0,%d): a fall may begin here with no cost paid yet, so "
                            + "dp[0][%d] is just the cell's own value, %d.", j, j, matrix[0][j])
                    .var("row", 0).var("col", j)
                    .dpTable(table(dp, settled, new GridDpTable.Coord(0, j),
                            String.valueOf(matrix[0][j]), Set.of(), false)).step();
        }

        for (int i = 1; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                List<GridDpTable.Coord> reads = new ArrayList<>(3);
                reads.add(new GridDpTable.Coord(i - 1, j));
                int best = dp[i - 1][j];
                if (j > 0) {
                    reads.add(new GridDpTable.Coord(i - 1, j - 1));
                    best = Math.min(best, dp[i - 1][j - 1]);
                }
                if (j < cols - 1) {
                    reads.add(new GridDpTable.Coord(i - 1, j + 1));
                    best = Math.min(best, dp[i - 1][j + 1]);
                }
                int total = matrix[i][j] + best;

                String shape = reads.size() == 3 ? "all three diagonals above it"
                        : j == 0 ? "only the two above it and to its upper-right — there is "
                                + "no upper-left, this is the first column"
                                : "only the two above it and to its upper-left — there is no "
                                        + "upper-right, this is the last column";

                com.dsa.ui.model.DpTable combineTable = table(dp, settled,
                        new GridDpTable.Coord(i, j), String.valueOf(total), Set.copyOf(reads),
                        false);
                if (reads.size() == 3) {
                    String substitution = String.format(
                            "dp[%d][%d] = matrix[%d][%d] + min(dp[%d][%d], dp[%d][%d], dp[%d][%d]) "
                                    + "= %d + min(%d, %d, %d) = %d + %d = %d",
                            i, j, i, j, i - 1, j - 1, i - 1, j, i - 1, j + 1,
                            matrix[i][j], dp[i - 1][j - 1], dp[i - 1][j], dp[i - 1][j + 1],
                            matrix[i][j], best, total);
                    combineTable = combineTable.withFormula(FORMULA, substitution);
                }

                emit.at("combine")
                        .say("Cell (%d,%d) can only have fallen from directly above or one "
                                + "column over, so it reads %s: the cheapest of those is %d. "
                                + "dp[%d][%d] = %d + %d = %d.",
                                i, j, shape, best, i, j, matrix[i][j], best, total)
                        .var("row", i).var("col", j)
                        .var("candidates", reads.size())
                        .var("best", best).var("dp[i][j]", total)
                        .dpTable(combineTable).step();

                dp[i][j] = total;
                settled[i][j] = true;
            }
        }

        int answer = dp[rows - 1][0];
        Set<GridDpTable.Coord> seen = new java.util.LinkedHashSet<>();
        seen.add(new GridDpTable.Coord(rows - 1, 0));
        for (int j = 1; j < cols; j++) {
            int before = answer;
            boolean improves = dp[rows - 1][j] < before;
            if (improves) {
                answer = dp[rows - 1][j];
            }
            seen.add(new GridDpTable.Coord(rows - 1, j));
            emit.at("reduce")
                    .say("A fall can end at any column of the last row, so the answer is the "
                            + "minimum across the whole row, not one fixed cell. Comparing "
                            + "column %d (%d) against the running minimum so far (%d): %s.",
                            j, dp[rows - 1][j], before,
                            improves ? "column %d is cheaper, so the minimum drops to %d"
                                    .formatted(j, answer)
                                    : "no improvement, the minimum stays %d".formatted(before))
                    .var("row", rows - 1).var("col", j).var("running", answer)
                    .dpTable(table(dp, settled, null, String.valueOf(answer), Set.copyOf(seen),
                            false)).step();
        }

        emit.at("done")
                .say("The cheapest fall costs %d. It was found without ever tracking which "
                        + "column it passed through — only the running minimum mattered.",
                        answer)
                .var("answer", answer)
                .dpTable(table(dp, settled, null, "?", Set.of(), true)).step();
    }

    private static com.dsa.ui.model.DpTable table(int[][] dp, boolean[][] settled,
                                                  GridDpTable.Coord probe, String probeValue,
                                                  Set<GridDpTable.Coord> reads, boolean done) {
        return GridDpTable.of(dp, settled, null, probe, probeValue, reads, done);
    }
}
