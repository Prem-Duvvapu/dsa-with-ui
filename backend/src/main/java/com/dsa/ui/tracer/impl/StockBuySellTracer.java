package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;
import java.util.Map;

/**
 * One pass with two variables: the cheapest price seen so far is the only
 * worth-while buy candidate, and each later day asks what selling today
 * would have earned over it.
 */
@Component
public class StockBuySellTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "stock-buy-sell";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("prices", FieldType.INT_ARRAY)
                        .label("Prices")
                        .help("minPrice tracks the cheapest day so far; every day measures profit if sold today.")
                        .length(1, 40).values(1, 999)
                        .defaultValue(List.of(7, 1, 5, 3, 6, 4))
                        .build());
    }

    /** Strictly falling prices: min keeps moving and no sell ever beats profit 0. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("prices", List.of(9, 7, 4, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public int maxProfit(int[] prices) {
                   // @a init
                   int minPrice = prices[0], best = 0;
                   for (int i = 1; i < prices.length; i++) {
                       // @a cheaper
                       if (prices[i] < minPrice) {
                           minPrice = prices[i];
                       }
                       // @a sell
                       int profit = prices[i] - minPrice;
                       if (profit > best) {
                           best = profit;
                       }
                   }
                   // @a done
                   return best;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] prices = in.getIntArray("prices");
        int minPrice = prices[0];
        int best = 0;

        emit.at("init")
                .say("Day 0 costs %d, so it is both the cheapest buy so far and the only candidate. Selling later must beat profit 0.",
                        minPrice)
                .var("minPrice", minPrice).var("best", 0).array(prices, 0).step();

        for (int i = 1; i < prices.length; i++) {
            if (prices[i] < minPrice) {
                minPrice = prices[i];
                emit.at("cheaper")
                        .say("Day %d: price %d undercuts everything before it - new best buying day.", i, prices[i])
                        .var("i", i).var("minPrice", minPrice).var("best", best)
                        .array(prices, i, 0).step();
            }
            int profit = prices[i] - minPrice;
            if (profit > best) {
                best = profit;
                emit.at("sell")
                        .say("Day %d: selling at %d over a buy at %d earns %d - best trade so far.",
                                i, prices[i], minPrice, profit)
                        .var("i", i).var("profit", profit).var("best", best)
                        .array(prices, i, 0).step();
            } else {
                emit.at("sell")
                        .say("Day %d: selling at %d over %d earns only %d, not better than %d.",
                                i, prices[i], minPrice, profit, best)
                        .var("i", i).var("profit", profit).var("best", best)
                        .array(prices, i, 0).step();
            }
        }

        emit.at("done")
                .say(best == 0
                        ? "Prices never rose above their running minimum - staying out of the market (0) was optimal."
                        : "Best trade: buy at %d, sell later for a profit of %d.", minPrice, best)
                .var("best", best).array(prices).step();
    }
}
