package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Every row is sorted with 0s before 1s, so the count of 1s in a row is just
 * {@code cols - (index of the first 1)} — a lower-bound search per row, not a linear scan.
 * Tracking the best row seen so far turns N row searches into one answer.
 */
@Component
public class RowMaxOnesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "row-max-ones";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("matrix", FieldType.INT_GRID)
                        .label("Row-sorted binary matrix")
                        .help("Every row's 0s come before its 1s.")
                        .constraint("maxRows", 8).constraint("maxCols", 8)
                        .values(0, 1)
                        .defaultValue(List.of(
                                List.of(0, 0, 0, 1),
                                List.of(0, 1, 1, 1),
                                List.of(0, 0, 1, 1)))
                        .build());
    }

    /** No row has a single 1 — every row's count is 0, so the first row wins by default. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("matrix", List.of(List.of(0, 0), List.of(0, 0)));
    }

    @Override
    public String annotatedCode() {
        return """
               public int rowWithMax1s(int[][] matrix) {
                   int bestRow = -1, bestCount = -1;
                   for (int r = 0; r < matrix.length; r++) {
                       // @a rowSearch
                       int firstOne = lowerBoundOfOne(matrix[r]);
                       int count = matrix[r].length - firstOne;
                       if (count > bestCount) {
                           // @a newBest
                           bestCount = count;
                           bestRow = r;
                       }
                   }
                   // @a done
                   return bestRow;
               }

               private int lowerBoundOfOne(int[] row) {
                   int low = 0, high = row.length;
                   while (low < high) {
                       int mid = (low + high) / 2;
                       if (row[mid] == 1) high = mid; else low = mid + 1;
                   }
                   return low;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] matrix = in.getGrid("matrix");
        int bestRow = -1, bestCount = -1;

        for (int r = 0; r < matrix.length; r++) {
            int[] row = matrix[r];
            int low = 0, high = row.length;
            while (low < high) {
                int mid = (low + high) / 2;
                if (row[mid] == 1) high = mid; else low = mid + 1;
            }
            int count = row.length - low;
            emit.at("rowSearch")
                    .say("Row %d: first 1 at index %s -> %d ones.", r,
                            low == row.length ? "none" : String.valueOf(low), count)
                    .var("row", r).var("count", count).grid(matrix).step();

            if (count > bestCount) {
                int previousBest = bestCount;
                bestCount = count;
                bestRow = r;
                emit.at("newBest")
                        .say("Row %d's %d ones beats the previous best (%s) — new best row.", r, count,
                                previousBest == -1 ? "none" : String.valueOf(previousBest))
                        .var("bestRow", bestRow).var("bestCount", bestCount).grid(matrix).step();
            }
        }

        emit.at("done")
                .say("Row %d has the most 1s (%d).", bestRow, bestCount)
                .var("answer", bestRow).grid(matrix).step();
    }
}
