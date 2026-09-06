package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Fractional Knapsack — greedy is provably optimal here precisely because fractions are
 * allowed: rank every item by value-per-unit-weight and drain the bag from the best ratio
 * down, taking a partial slice of whichever item first exceeds the remaining capacity.
 */
@Component
public class FractionalKnapsackTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "fractional-knapsack";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("values", FieldType.INT_ARRAY)
                        .label("Item values")
                        .help("Value of each item, aligned with weights by position.")
                        .length(1, 20).values(1, 500)
                        .defaultValue(List.of(60, 100, 120))
                        .build(),
                InputField.of("weights", FieldType.INT_ARRAY)
                        .label("Item weights")
                        .help("Weight of each item, aligned with values by position.")
                        .length(1, 20).values(1, 100)
                        .defaultValue(List.of(10, 20, 30))
                        .build(),
                InputField.of("capacity", FieldType.INT)
                        .label("Knapsack capacity")
                        .help("Total weight the bag can carry.")
                        .range(1, 1000)
                        .defaultValue(50)
                        .build());
    }

    /** Ratios rank in a different order and the bag empties two items earlier — a very different run. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "values", List.of(500, 400, 300),
                "weights", List.of(30, 20, 10),
                "capacity", 25);
    }

    @Override
    public String annotatedCode() {
        return """
               public double fractionalKnapsack(int[] values, int[] weights, int capacity) {
                   // @a rank
                   Integer[] order = sortByRatioDescending(values, weights);
                   double totalValue = 0;
                   int remaining = capacity;
                   for (int idx : order) {
                       // @a check
                       if (remaining <= 0) {
                           // @a skip
                           continue;
                       }
                       if (weights[idx] <= remaining) {
                           // @a takeFull
                           totalValue += values[idx];
                           remaining -= weights[idx];
                       } else {
                           // @a takeFraction
                           double fraction = (double) remaining / weights[idx];
                           totalValue += fraction * values[idx];
                           remaining = 0;
                       }
                   }
                   // @a done
                   return totalValue;
               }""";
    }

    private List<ArrayElement> board(int[] values, int cursorItem, List<Integer> order, int orderPos) {
        List<ArrayElement> state = new ArrayList<>(order.size());
        for (int p = 0; p < order.size(); p++) {
            int item = order.get(p);
            String s = p < orderPos ? "sorted" : p == orderPos ? "current" : "target";
            state.add(new ArrayElement(item, values[item], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] values = in.getIntArray("values");
        int[] weights = in.getIntArray("weights");
        int capacity = in.getInt("capacity");

        if (values.length != weights.length) {
            throw new InputValidationException(Map.of("weights",
                    "You gave " + values.length + " values but " + weights.length + " weights — one per item."));
        }

        int n = values.length;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) order.add(i);
        order.sort((a, b) -> Double.compare(
                (double) values[b] / weights[b], (double) values[a] / weights[a]));

        StringBuilder ratios = new StringBuilder();
        for (int i = 0; i < order.size(); i++) {
            int item = order.get(i);
            if (i > 0) ratios.append(", ");
            ratios.append("#").append(item).append("(").append(values[item]).append('/').append(weights[item])
                    .append('=').append(String.format("%.2f", (double) values[item] / weights[item])).append(')');
        }
        emit.at("rank").say("Rank every item by value/weight ratio, best first: %s.", ratios)
                .var("order", ratios.toString())
                .arrayState(board(values, -1, order, -1)).step();

        double totalValue = 0;
        int remaining = capacity;

        for (int pos = 0; pos < order.size(); pos++) {
            int item = order.get(pos);

            emit.at("check").say("Item #%d weighs %d, worth %d. %d of capacity remains.",
                            item, weights[item], values[item], remaining)
                    .var("remaining", remaining).var("totalValue", trimmed(totalValue))
                    .arrayState(board(values, item, order, pos)).step();

            if (remaining <= 0) {
                emit.at("skip").say("No capacity left — item #%d cannot be taken at all.", item)
                        .var("remaining", remaining).var("totalValue", trimmed(totalValue))
                        .arrayState(board(values, item, order, pos)).step();
                continue;
            }

            if (weights[item] <= remaining) {
                totalValue += values[item];
                remaining -= weights[item];
                emit.at("takeFull").say("It fits whole: take all of item #%d for +%d value. Capacity left: %d.",
                                item, values[item], remaining)
                        .var("remaining", remaining).var("totalValue", trimmed(totalValue))
                        .arrayState(board(values, item, order, pos)).step();
            } else {
                int beforeRemaining = remaining;
                double fraction = (double) remaining / weights[item];
                double gained = fraction * values[item];
                totalValue += gained;
                remaining = 0;
                emit.at("takeFraction").say("Only %d/%d of item #%d fits. Take that fraction for +%.2f value, then the bag is full.",
                                beforeRemaining, weights[item], item, gained)
                        .var("fraction", String.format("%.2f", fraction)).var("totalValue", trimmed(totalValue))
                        .arrayState(board(values, item, order, pos)).step();
            }
        }

        emit.at("done").say("Bag finished with total value %s.", trimmed(totalValue))
                .var("totalValue", trimmed(totalValue))
                .arrayState(board(values, -1, order, order.size())).step();
    }

    private static String trimmed(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.2f", v);
    }
}
