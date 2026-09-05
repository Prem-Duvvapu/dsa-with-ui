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
 * Same table shape as {@link Knapsack01Tracer}, and the same single-line diff decides
 * everything: taking item i reads dp[i][w - weight[i]] - the SAME row, allowing item i to
 * be taken again - instead of 0/1's dp[i-1][w - weight[i]], which can never see item i
 * again once row i has been left behind.
 */
@Component
public class UnboundedKnapsackTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "unbounded-knapsack";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("weights", FieldType.INT_ARRAY)
                        .label("Weights")
                        .help("One weight per item type, paired by index with values. Any item type may be reused.")
                        .length(1, 8).values(1, 20)
                        .defaultValue(List.of(2, 4, 6))
                        .build(),
                InputField.of("values", FieldType.INT_ARRAY)
                        .label("Values")
                        .length(1, 8).values(1, 100)
                        .defaultValue(List.of(5, 11, 13))
                        .build(),
                InputField.of("capacity", FieldType.INT)
                        .label("Capacity")
                        .range(1, 30)
                        .defaultValue(10)
                        .build());
    }

    /** One item type only: the trace becomes pure repetition, taking the same item as many times as fits. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("weights", List.of(3), "values", List.of(10), "capacity", 8);
    }

    @Override
    public String annotatedCode() {
        return """
               public int unboundedKnapsack(int[] wt, int[] val, int n, int W) {
                   int[][] dp = new int[n + 1][W + 1];
                   for (int i = 1; i <= n; i++) {
                       for (int w = 0; w <= W; w++) {
                           if (wt[i - 1] <= w) {
                               // @a fits
                               dp[i][w] = Math.max(val[i - 1] + dp[i][w - wt[i - 1]], dp[i - 1][w]);
                           } else {
                               // @a doesntFit
                               dp[i][w] = dp[i - 1][w];
                           }
                       }
                   }
                   // @a done
                   return dp[n][W];
               }""";
    }

    private DpTable table(int[][] dp, int n, int W, int probeI, int probeW, int readIsSameRow, int readW) {
        List<String> rowLabels = new ArrayList<>(n + 1);
        rowLabels.add("none");
        for (int i = 1; i <= n; i++) rowLabels.add("item " + i);
        List<String> colLabels = new ArrayList<>(W + 1);
        for (int w = 0; w <= W; w++) colLabels.add(String.valueOf(w));

        List<List<DpCell>> rows = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) {
            List<DpCell> row = new ArrayList<>(W + 1);
            for (int w = 0; w <= W; w++) {
                String state;
                if (i == probeI && w == probeW) {
                    state = "probe";
                } else if (readIsSameRow == 1 && i == probeI && w == readW) {
                    state = "read";
                } else if (i == probeI - 1 && (w == readW || w == probeW)) {
                    state = "read";
                } else {
                    state = "known";
                }
                row.add(new DpCell(String.valueOf(dp[i][w]), state));
            }
            rows.add(row);
        }
        return new DpTable(rowLabels, colLabels, rows);
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] wt = in.getIntArray("weights");
        int[] val = in.getIntArray("values");
        int n = wt.length;
        int W = in.getInt("capacity");
        int[][] dp = new int[n + 1][W + 1];

        for (int i = 1; i <= n; i++) {
            for (int w = 0; w <= W; w++) {
                if (wt[i - 1] <= w) {
                    int withIt = val[i - 1] + dp[i][w - wt[i - 1]];
                    int withoutIt = dp[i - 1][w];
                    dp[i][w] = Math.max(withIt, withoutIt);
                    emit.at("fits")
                            .say("Item %d (weight %d, value %d) fits in capacity %d: take it "
                                    + "again (%d + dp[%d][%d]=%d, same row - item %d can "
                                    + "still be reused) vs skip it for good (dp[%d][%d]=%d) -> %d.",
                                    i, wt[i - 1], val[i - 1], w, val[i - 1], i,
                                    w - wt[i - 1], withIt, i, i - 1, w, withoutIt, dp[i][w])
                            .var("i", i).var("w", w).var("value", dp[i][w])
                            .dpTable(table(dp, n, W, i, w, 1, w - wt[i - 1])).step();
                } else {
                    dp[i][w] = dp[i - 1][w];
                    emit.at("doesntFit")
                            .say("Item %d (weight %d) does not fit in capacity %d - carry "
                                    + "forward dp[%d][%d]=%d unchanged.",
                                    i, wt[i - 1], w, i - 1, w, dp[i][w])
                            .var("i", i).var("w", w).var("value", dp[i][w])
                            .dpTable(table(dp, n, W, i, w, 0, w)).step();
                }
            }
        }

        emit.at("done")
                .say("Every item type considered at every capacity, any number of reuses. "
                        + "Best value within capacity %d: %d.", W, dp[n][W])
                .var("answer", dp[n][W])
                .dpTable(table(dp, n, W, -1, -1, 0, -1)).step();
    }
}
