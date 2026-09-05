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
 * The full (item, capacity) table, not the space-optimised single-row version: every cell
 * dp[i][w] decides once between skipping item i (copy dp[i-1][w]) and taking it (its value
 * plus whatever capacity w - weight[i] could already do WITHOUT item i, dp[i-1][...]) -
 * that "without item i" dependency is the entire reason 0/1 knapsack cannot reuse an item,
 * and it disappears from view the moment the table is collapsed to one row.
 */
@Component
public class Knapsack01Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "knapsack-01";
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
                        .help("One weight per item, paired by index with values.")
                        .length(1, 8).values(1, 20)
                        .defaultValue(List.of(1, 2, 4, 5))
                        .build(),
                InputField.of("values", FieldType.INT_ARRAY)
                        .label("Values")
                        .length(1, 8).values(1, 100)
                        .defaultValue(List.of(5, 4, 8, 6))
                        .build(),
                InputField.of("capacity", FieldType.INT)
                        .label("Capacity")
                        .range(1, 30)
                        .defaultValue(5)
                        .build());
    }

    /** Two items instead of four, and neither combination fills the capacity exactly. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("weights", List.of(2, 3), "values", List.of(4, 5), "capacity", 4);
    }

    @Override
    public String annotatedCode() {
        return """
               public int knapsack(int[] wt, int[] val, int n, int W) {
                   int[][] dp = new int[n + 1][W + 1];
                   for (int i = 1; i <= n; i++) {
                       for (int w = 0; w <= W; w++) {
                           if (wt[i - 1] <= w) {
                               // @a fits
                               dp[i][w] = Math.max(val[i - 1] + dp[i - 1][w - wt[i - 1]], dp[i - 1][w]);
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

    private DpTable table(int[][] dp, int n, int W, int probeI, int probeW, int readW) {
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
                    int withIt = val[i - 1] + dp[i - 1][w - wt[i - 1]];
                    int withoutIt = dp[i - 1][w];
                    dp[i][w] = Math.max(withIt, withoutIt);
                    emit.at("fits")
                            .say("Item %d (weight %d, value %d) fits in capacity %d: take it "
                                    + "(%d + dp[%d][%d]=%d) vs skip it (dp[%d][%d]=%d) -> %d.",
                                    i, wt[i - 1], val[i - 1], w, val[i - 1], i - 1,
                                    w - wt[i - 1], withIt, i - 1, w, withoutIt, dp[i][w])
                            .var("i", i).var("w", w).var("value", dp[i][w])
                            .dpTable(table(dp, n, W, i, w, w - wt[i - 1])).step();
                } else {
                    dp[i][w] = dp[i - 1][w];
                    emit.at("doesntFit")
                            .say("Item %d (weight %d) does not fit in capacity %d - carry "
                                    + "forward dp[%d][%d]=%d unchanged.",
                                    i, wt[i - 1], w, i - 1, w, dp[i][w])
                            .var("i", i).var("w", w).var("value", dp[i][w])
                            .dpTable(table(dp, n, W, i, w, w)).step();
                }
            }
        }

        emit.at("done")
                .say("Every item considered at every capacity. Best value within capacity "
                        + "%d: %d.", W, dp[n][W])
                .var("answer", dp[n][W])
                .dpTable(table(dp, n, W, -1, -1, -1)).step();
    }
}
