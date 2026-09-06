package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Cheapest route from src to dst using at most k stops (LeetCode 787).
 *
 * <p>Dijkstra is the wrong tool here, and this trace is built to show why. Dijkstra
 * finalizes a vertex the moment it is popped at its cheapest price, but "cheapest so far"
 * is not the only thing that matters once hops are rationed: a dearer route reaching the
 * same city in fewer hops can still be the only one that finishes within budget. The
 * greedy exchange argument Dijkstra rests on simply does not hold.
 *
 * <p>So this relaxes every edge in Bellman-Ford fashion, but only k + 1 times — one round
 * per flight the route is allowed to take. The load-bearing detail is the array copy at
 * the top of each round: relaxations write into {@code next} while reading {@code dist},
 * the previous round's snapshot. Relax in place instead and a price found this round is
 * immediately extendable in the same round, chaining two flights where the budget allowed
 * one, and the hop cap silently stops meaning anything.
 */
@Component
public class CheapestFlightsKStopsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "cheapest-flights-k-stops";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Flight network")
                        .help("City count plus directed flights [from, to, price]. "
                                + "The default is LeetCode 787's first example, whose answer is 700.")
                        .directed()
                        .weighted()
                        .weights(0, 10_000)
                        .constraint("maxVertices", 12)
                        .constraint("maxEdges", 30)
                        .defaultValue(Map.of(
                                "vertices", 4,
                                "edges", List.of(
                                        List.of(0, 1, 100), List.of(1, 2, 100), List.of(2, 0, 100),
                                        List.of(1, 3, 600), List.of(2, 3, 200))))
                        .build(),
                InputField.of("src", FieldType.INT)
                        .label("Departure city")
                        .range(0, 11)
                        .defaultValue(0)
                        .build(),
                InputField.of("dst", FieldType.INT)
                        .label("Destination city")
                        .range(0, 11)
                        .defaultValue(3)
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Maximum stops")
                        .help("Stops, not flights - a route with k stops uses k + 1 flights, "
                                + "so the relaxation runs k + 1 rounds.")
                        .range(0, 10)
                        .defaultValue(1)
                        .build());
    }

    /**
     * LeetCode 787's third example: the same three cities, but zero stops allowed. The
     * two-hop 200 route is now illegal and the direct 500 flight wins, so the budget is
     * doing visible work rather than being a formality.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of(
                        "vertices", 3,
                        "edges", List.of(
                                List.of(0, 1, 100), List.of(1, 2, 100), List.of(0, 2, 500))),
                "src", 0,
                "dst", 2,
                "k", 0);
    }

    @Override
    public String annotatedCode() {
        return """
               public int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
                   // @a init
                   int[] dist = new int[n];
                   Arrays.fill(dist, Integer.MAX_VALUE);
                   dist[src] = 0;

                   for (int round = 0; round <= k; round++) {
                       // @a roundStart
                       int[] next = dist.clone();
                       for (int[] flight : flights) {
                           int from = flight[0], to = flight[1], price = flight[2];
                           if (dist[from] != Integer.MAX_VALUE
                                   && dist[from] + price < next[to]) {
                               // @a relax
                               next[to] = dist[from] + price;
                           } else {
                               // @a noImprovement
                               continue;
                           }
                       }
                       // @a roundEnd
                       dist = next;
                   }
                   // @a done
                   return dist[dst] == Integer.MAX_VALUE ? -1 : dist[dst];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int src = in.getInt("src");
        int dst = in.getInt("dst");
        int k = in.getInt("k");
        int n = graph.vertices();

        Map<String, String> errors = new LinkedHashMap<>();
        if (src >= n) {
            errors.put("src", "This network only has cities 0.." + (n - 1) + ".");
        }
        if (dst >= n) {
            errors.put("dst", "This network only has cities 0.." + (n - 1) + ".");
        }
        if (!errors.isEmpty()) {
            throw new InputValidationException(errors);
        }

        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }
        states.put(src, "visiting");
        states.put(dst, "target");

        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;

        emit.at("init").say(
                        "%d cities, %d flights. dist[%d] = 0 and every other city is unreachable. "
                                + "At most %d stops means at most %d flights, so the relaxation gets exactly %d rounds.",
                        n, graph.edges().length, src, k, k + 1, k + 1)
                .var("src", src).var("dst", dst).var("k", k).var("dist", priceString(dist))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        for (int round = 0; round <= k; round++) {
            int[] next = dist.clone();

            emit.at("roundStart").say(
                            "Round %d of %d: copy dist into next. Every relaxation below reads the "
                                    + "frozen dist, so a price discovered in this round cannot be extended "
                                    + "again until the next one - that copy is the whole hop cap.",
                            round + 1, k + 1)
                    .var("round", round + 1).var("dist", priceString(dist)).var("next", priceString(next))
                    .graph(layout.nodes(), layout.edges()).nodes(states).step();

            for (int[] flight : graph.edges()) {
                int from = flight[0], to = flight[1], price = flight[2];
                String edgeKey = from + "-" + to;

                if (dist[from] != Integer.MAX_VALUE && dist[from] + price < next[to]) {
                    String old = priceLabel(next[to]);
                    next[to] = dist[from] + price;
                    if (to != src && to != dst) {
                        states.put(to, "queued");
                    }
                    emit.at("relax").say(
                                    "Flight %d -> %d costs %d. Reaching %d for %d and paying %d gets to %d "
                                            + "for %d, beating %s within this round's budget.",
                                    from, to, price, from, dist[from], price, to, next[to], old)
                            .var("round", round + 1).var("flight", edgeKey)
                            .var("dist", priceString(dist)).var("next", priceString(next))
                            .graph(layout.nodes(), layout.edges()).nodes(states)
                            .edges(List.of(edgeKey)).step();
                } else {
                    String reason = dist[from] == Integer.MAX_VALUE
                            ? String.format("city %d is not reachable in %d flight(s) yet", from, round)
                            : String.format("%d + %d = %d does not beat next[%d] = %s",
                                    dist[from], price, dist[from] + price, to, priceLabel(next[to]));
                    emit.at("noImprovement").say("Flight %d -> %d costs %d, but %s. Leave next[%d] alone.",
                                    from, to, price, reason, to)
                            .var("round", round + 1).var("flight", edgeKey)
                            .var("dist", priceString(dist)).var("next", priceString(next))
                            .graph(layout.nodes(), layout.edges()).nodes(states)
                            .edges(List.of(edgeKey)).step();
                }
            }

            dist = next;
            emit.at("roundEnd").say(
                            "Round %d done. dist = %s - these are the cheapest prices using at most %d flight(s).",
                            round + 1, priceString(dist), round + 1)
                    .var("round", round + 1).var("dist", priceString(dist))
                    .graph(layout.nodes(), layout.edges()).nodes(states).step();
        }

        int answer = dist[dst] == Integer.MAX_VALUE ? -1 : dist[dst];
        if (answer >= 0) {
            states.put(dst, "done");
        }
        String verdict = answer < 0
                ? String.format("%d is not reachable from %d within %d stop(s), so the answer is -1", dst, src, k)
                : String.format("the cheapest %d -> %d route within %d stop(s) costs %d", src, dst, k, answer);
        emit.at("done").say("All %d round(s) complete. dist = %s, so %s.", k + 1, priceString(dist), verdict)
                .var("answer", answer).var("dist", priceString(dist))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }

    private static String priceLabel(int value) {
        return value == Integer.MAX_VALUE ? "unreachable" : String.valueOf(value);
    }

    private static String priceString(int[] dist) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dist.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(dist[i] == Integer.MAX_VALUE ? "-" : String.valueOf(dist[i]));
        }
        return sb.append(']').toString();
    }
}
