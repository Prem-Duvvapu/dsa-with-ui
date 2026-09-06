package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Stock Span Problem. For each day, find how many consecutive days (including today)
 * the price was less than or equal to today's price.
 *
 * <p>A monotonic decreasing stack of indices: when a new price arrives, pop every index
 * whose price is at most the current price. The span is current_index - whatever is
 * left on top (or all the way back to -1 if the stack empties).
 */
@Component
public class StockSpanTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "stock-span-problem";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("prices", FieldType.INT_ARRAY)
                        .label("Stock prices")
                        .help("Daily stock prices. Span = consecutive days where price ≤ today.")
                        .length(1, 16).values(1, 999)
                        .defaultValue(List.of(100, 80, 60, 70, 60, 75, 85))
                        .build());
    }

    /** Strictly increasing: every day's span is its index + 1. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("prices", List.of(10, 20, 30, 40));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] stockSpan(int[] prices) {
                   int n = prices.length;
                   // @a init
                   int[] span = new int[n];
                   Deque<Integer> stack = new ArrayDeque<>();
                   for (int i = 0; i < n; i++) {
                       while (!stack.isEmpty() && prices[stack.peek()] <= prices[i]) {
                           // @a pop
                           stack.pop();
                       }
                       // @a record
                       span[i] = stack.isEmpty() ? i + 1 : i - stack.peek();
                       // @a push
                       stack.push(i);
                   }
                   // @a done
                   return span;
               }""";
    }

    private List<ArrayElement> state(int[] prices, int current, Deque<Integer> stack) {
        List<ArrayElement> s = new ArrayList<>(prices.length);
        for (int i = 0; i < prices.length; i++) {
            String st = i == current ? "current" : stack.contains(i) ? "sorted" : i < current ? "visited" : "default";
            s.add(new ArrayElement(i, prices[i], st));
        }
        return s;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] prices = in.getIntArray("prices");
        int n = prices.length;
        int[] span = new int[n];
        Deque<Integer> stack = new ArrayDeque<>();

        emit.at("init")
                .say("Stock prices: %s. Use a monotonic decreasing stack of indices to compute spans.",
                        Arrays.toString(prices))
                .var("n", n)
                .arrayState(state(prices, -1, stack)).stack(stack).step();

        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && prices[stack.peek()] <= prices[i]) {
                int popped = stack.pop();
                emit.at("pop")
                        .say("prices[%d]=%d ≤ prices[%d]=%d — pop index %d.",
                                popped, prices[popped], i, prices[i], popped)
                        .var("i", i).var("popped", popped)
                        .arrayState(state(prices, i, stack)).stack(stack).step();
            }

            span[i] = stack.isEmpty() ? i + 1 : i - stack.peek();
            emit.at("record")
                    .say("Span for day %d (price %d) = %d consecutive days.",
                            i, prices[i], span[i])
                    .var("i", i).var("span[i]", span[i])
                    .arrayState(state(prices, i, stack)).stack(stack).step();

            stack.push(i);
            emit.at("push")
                    .say("Push index %d (price %d).", i, prices[i])
                    .var("i", i)
                    .arrayState(state(prices, i, stack)).stack(stack).step();
        }

        emit.at("done")
                .say("All days processed. Spans: %s.", Arrays.toString(span))
                .var("answer", Arrays.toString(span))
                .arrayState(state(prices, -1, stack)).stack(stack).step();
    }
}
