package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Cheapest Flights Within K Stops (LeetCode 787), traced on the owner's own accepted submission.
 * The queue is ordered by the number of flights taken, not by price, so routes are explored in
 * layers of 1, 2, ... flights; a route that already used k + 1 flights is not extended. Each
 * queue entry carries its own price, so a cheaper route found later with more flights never
 * corrupts a route with fewer. O(E * K).
 *
 * <p>The priority queue is a real {@link PriorityQueue} with the submission's comparator, so
 * entries with equal stop counts come out in the same order the submission's would.
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
               class Pair {
                   int price;
                   int node;

                   Pair(int price,int node) {
                       this.price = price;
                       this.node = node;
                   }
               }

               class Tuple {
                   int stops;
                   int price;
                   int node;

                   Tuple(int stops,int price,int node) {
                       this.stops = stops;
                       this.price = price;
                       this.node = node;
                   }
               }

               class Solution {
                   public int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
                       // @a init
                       List<List<Pair>>  adjList = new ArrayList<>();
                       PriorityQueue<Tuple> pq = new PriorityQueue<>((x,y) -> Integer.compare(x.stops,y.stops));
                       int[] priceArr = new int[n];

                       Arrays.fill(priceArr, Integer.MAX_VALUE);

                       for (int i=0;i<n;i++)
                           adjList.add(new ArrayList<>());

                       for (int[] e: flights) {
                           int u = e[0];
                           int v = e[1];
                           int p = e[2];

                           adjList.get(u).add(new Pair(p,v));
                       }

                       pq.add(new Tuple(0,0,src));
                       priceArr[src] = 0;

                       while (!pq.isEmpty()) {
                           // @a poll
                           Tuple curr = pq.poll();
                           int currStops = curr.stops;
                           int currPrice = curr.price;
                           int currNode = curr.node;

                           if (currStops > k)
                               // @a tooMany
                               continue;

                           for (Pair ngbr: adjList.get(currNode)) {
                               if (currPrice + ngbr.price < priceArr[ngbr.node]) {
                                   // @a relax
                                   priceArr[ngbr.node] = currPrice + ngbr.price;
                                   pq.add(new Tuple(currStops+1,priceArr[ngbr.node],ngbr.node));
                               }
                           }
                       }

                       // @a done
                       return (priceArr[dst] != Integer.MAX_VALUE) ? priceArr[dst] : -1;
                   }
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

        List<List<int[]>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjList.add(new ArrayList<>());
        }
        for (int[] e : graph.edges()) {
            adjList.get(e[0]).add(new int[]{e[2], e[1]});
        }
        int[] priceArr = new int[n];
        Arrays.fill(priceArr, Integer.MAX_VALUE);
        // {stops, price, node}
        PriorityQueue<int[]> pq = new PriorityQueue<>((x, y) -> Integer.compare(x[0], y[0]));
        pq.add(new int[]{0, 0, src});
        priceArr[src] = 0;

        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }
        states.put(src, "queued");
        states.put(dst, "target");
        emit.at("init").say("Fly from %d to %d with at most %d stop%s, which means at most %d flight%s. The queue "
                        + "hands back routes with the fewest flights first.",
                        src, dst, k, Narration.s(k), k + 1, Narration.s(k + 1))
                .var("priceArr", show(priceArr)).graph(layout.nodes(), layout.edges()).nodes(states)
                .queue(entries(pq)).step();

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int currStops = curr[0];
            int currPrice = curr[1];
            int currNode = curr[2];
            emit.at("poll").say("Poll city %d, reached with %d flight%s for %d.",
                            currNode, currStops, Narration.s(currStops), currPrice)
                    .var("currNode", currNode).var("currStops", currStops).var("currPrice", currPrice)
                    .var("priceArr", show(priceArr)).graph(layout.nodes(), layout.edges()).nodes(states)
                    .queue(entries(pq)).step();

            if (currStops > k) {
                emit.at("tooMany").say("That route already used %d flights; one more would mean more than %d "
                                + "stop%s. Do not extend it.", currStops, k, Narration.s(k))
                        .var("currStops", currStops).graph(layout.nodes(), layout.edges()).nodes(states)
                        .queue(entries(pq)).step();
                continue;
            }

            for (int[] ngbr : adjList.get(currNode)) {
                if (currPrice + ngbr[0] < priceArr[ngbr[1]]) {
                    int before = priceArr[ngbr[1]];
                    priceArr[ngbr[1]] = currPrice + ngbr[0];
                    pq.add(new int[]{currStops + 1, priceArr[ngbr[1]], ngbr[1]});
                    if (ngbr[1] != dst) {
                        states.put(ngbr[1], "queued");
                    }
                    emit.at("relax").say("Flight %d -> %d costs %d + %d = %d, cheaper than %s. Queue it with %d "
                                    + "flight%s.", currNode, ngbr[1], currPrice, ngbr[0], priceArr[ngbr[1]],
                                    before == Integer.MAX_VALUE ? "any route so far" : String.valueOf(before),
                                    currStops + 1, Narration.s(currStops + 1))
                            .var("priceArr", show(priceArr)).graph(layout.nodes(), layout.edges()).nodes(states)
                            .edges(List.of(currNode + "-" + ngbr[1])).queue(entries(pq)).step();
                }
            }
        }

        int answer = priceArr[dst] != Integer.MAX_VALUE ? priceArr[dst] : -1;
        emit.at("done").say(answer == -1
                        ? "No route reaches " + dst + " within " + k + " stop" + Narration.s(k) + ". Return -1."
                        : "The cheapest price found for " + dst + " is " + answer + ". Return it.")
                .var("priceArr", show(priceArr)).var("answer", answer)
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }

    private static String show(int[] a) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(a[i] == Integer.MAX_VALUE ? "inf" : String.valueOf(a[i]));
        }
        return sb.append(']').toString();
    }

    /** The queue, fewest flights first, as "city (flights, price)". */
    private static List<String> entries(PriorityQueue<int[]> pq) {
        List<int[]> items = new ArrayList<>(pq);
        items.sort((x, y) -> Integer.compare(x[0], y[0]));
        List<String> out = new ArrayList<>();
        for (int[] t : items) {
            out.add(t[2] + " (" + t[0] + " fl, " + t[1] + ")");
        }
        return out;
    }
}
