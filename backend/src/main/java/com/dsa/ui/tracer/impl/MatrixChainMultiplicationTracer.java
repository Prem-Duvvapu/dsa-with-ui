package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interval DP: f(i, j) is the min cost to multiply the chain of matrices between
 * dimension-array positions i and j. Every possible split point k partitions that chain
 * into two independently-solved sub-chains, so the recurrence tries every k and keeps the
 * cheapest split, plus the cost of the final multiplication joining the two halves.
 */
@Component
public class MatrixChainMultiplicationTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "matrix-chain-multiplication";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("dims", FieldType.INT_ARRAY)
                        .label("Matrix dimensions")
                        .help("N+1 numbers describe N matrices: dims[i-1] x dims[i] is matrix i.")
                        .length(2, 8).values(1, 100)
                        .defaultValue(List.of(10, 20, 30, 40, 30))
                        .build());
    }

    /** Only two matrices: exactly one way to multiply them, no split choice to make. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("dims", List.of(10, 20, 30));
    }

    @Override
    public String annotatedCode() {
        return """
               public int matrixMultiplication(int[] dims) {
                   int n = dims.length;
                   int[][] dp = new int[n][n];
                   for (int[] row : dp) Arrays.fill(row, -1);
                   return solve(1, n - 1, dims, dp);
               }

               private int solve(int i, int j, int[] dims, int[][] dp) {
                   if (i == j) {
                       // @a base
                       return 0;
                   }
                   if (dp[i][j] != -1) {
                       // @a memoHit
                       return dp[i][j];
                   }
                   int mini = Integer.MAX_VALUE;
                   for (int k = i; k < j; k++) {
                       int cost = solve(i, k, dims, dp) + solve(k + 1, j, dims, dp)
                               + dims[i - 1] * dims[k] * dims[j];
                       if (cost < mini) {
                           // @a newBest
                           mini = cost;
                       }
                   }
                   dp[i][j] = mini;
                   // @a storeMemo
                   return mini;
               }""";
    }

    private DpTable table(int[][] dp, int n, int probeI, int probeJ, int readI1, int readJ1,
                          int readI2, int readJ2) {
        List<String> labels = new ArrayList<>(n);
        for (int i = 0; i < n; i++) labels.add(String.valueOf(i));

        List<List<DpCell>> rows = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            List<DpCell> row = new ArrayList<>(n);
            for (int j = 0; j < n; j++) {
                boolean invalid = i == 0 || j == 0 || i > j;
                String value = invalid ? "·" : dp[i][j] == -1 ? "·" : String.valueOf(dp[i][j]);
                String state;
                if (invalid) {
                    state = "void";
                } else if (i == probeI && j == probeJ) {
                    state = "probe";
                } else if (i == readI1 && j == readJ1) {
                    state = "read";
                } else if (i == readI2 && j == readJ2) {
                    state = "read";
                } else if (dp[i][j] != -1) {
                    state = "known";
                } else {
                    state = "void";
                }
                row.add(new DpCell(value, state));
            }
            rows.add(row);
        }
        return new DpTable(labels, labels, rows);
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] dims = in.getIntArray("dims");
        int n = dims.length;
        int[][] dp = new int[n][n];
        for (int[] row : dp) java.util.Arrays.fill(row, -1);

        int answer = solve(1, n - 1, dims, dp, emit);

        emit.at("storeMemo")
                .say("f(1,%d) computed: minimum scalar multiplications = %d.", n - 1, answer)
                .var("answer", answer)
                .dpTable(table(dp, n, -1, -1, -1, -1, -1, -1)).step();
    }

    private int solve(int i, int j, int[] dims, int[][] dp, StepEmitter emit) {
        if (i == j) {
            emit.at("base")
                    .say("f(%d,%d): a single matrix needs no multiplication - cost 0.", i, j)
                    .var("i", i).var("j", j)
                    .dpTable(table(dp, dims.length, i, j, -1, -1, -1, -1)).step();
            return 0;
        }
        if (dp[i][j] != -1) {
            emit.at("memoHit")
                    .say("f(%d,%d) was already solved - reuse %d instead of recomputing.",
                            i, j, dp[i][j])
                    .var("i", i).var("j", j).var("cached", dp[i][j])
                    .dpTable(table(dp, dims.length, i, j, -1, -1, -1, -1)).step();
            return dp[i][j];
        }

        int mini = Integer.MAX_VALUE;
        emit.push("f(" + i + "," + j + ")");
        for (int k = i; k < j; k++) {
            int left = solve(i, k, dims, dp, emit);
            int right = solve(k + 1, j, dims, dp, emit);
            int joinCost = dims[i - 1] * dims[k] * dims[j];
            int cost = left + right + joinCost;

            if (cost < mini) {
                mini = cost;
                emit.at("newBest")
                        .say("Split f(%d,%d) at k=%d: f(%d,%d)=%d + f(%d,%d)=%d + join cost "
                                + "%d*%d*%d=%d -> total %d, the best split found so far.",
                                i, j, k, i, k, left, k + 1, j, right, dims[i - 1], dims[k],
                                dims[j], joinCost, cost)
                        .var("k", k).var("cost", cost)
                        .dpTable(table(dp, dims.length, i, j, i, k, k + 1, j)).step();
            }
        }
        dp[i][j] = mini;
        emit.at("storeMemo")
                .say("f(%d,%d) = %d - the cheapest of every split point tried.", i, j, mini)
                .var("i", i).var("j", j).var("value", mini)
                .dpTable(table(dp, dims.length, i, j, -1, -1, -1, -1)).step();
        emit.pop();
        return mini;
    }
}
