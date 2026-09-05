package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Single-source shortest paths tolerating negative edge weights, by brute-force relaxing
 * every edge V-1 times rather than greedily extracting a frontier the way Dijkstra does.
 *
 * A V-1-round relaxation is guaranteed to have found every shortest path, assuming none of
 * them loop through a negative cycle forever getting cheaper. One extra full pass checks for
 * exactly that: if any edge can still be relaxed after V-1 rounds already ran, a negative
 * cycle reachable from the source exists and the distances found are not a real answer.
 */
@Component
public class BellmanFordTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bellman-ford";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Directed, weighted graph")
                        .help("Vertex count plus directed edges [from, to, weight]. Weights "
                                + "may be negative - that is the entire reason this "
                                + "algorithm exists over Dijkstra.")
                        .directed()
                        .weighted()
                        .weights(-1000, 1000)
                        .constraint("maxVertices", 12)
                        .constraint("maxEdges", 24)
                        .defaultValue(Map.of(
                                "vertices", 5,
                                "edges", List.of(
                                        List.of(0, 1, 4), List.of(0, 2, 1), List.of(2, 1, -2),
                                        List.of(1, 3, 2), List.of(2, 3, 5), List.of(3, 4, 1))))
                        .build(),
                InputField.of("start", FieldType.INT)
                        .label("Source vertex")
                        .range(0, 11)
                        .defaultValue(0)
                        .build());
    }

    /** A negative cycle reachable from the source - distances are undefined, a genuinely different outcome. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of(
                        "vertices", 3,
                        "edges", List.of(
                                List.of(0, 1, 1), List.of(1, 2, -1), List.of(2, 0, -1))),
                "start", 0);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] bellmanFord(int v, int[][] edges, int src) {
                   // @a init
                   int[] dist = new int[v];
                   Arrays.fill(dist, Integer.MAX_VALUE);
                   dist[src] = 0;

                   for (int round = 0; round < v - 1; round++) {
                       for (int[] edge : edges) {
                           int from = edge[0], to = edge[1], weight = edge[2];
                           if (dist[from] != Integer.MAX_VALUE
                                   && dist[from] + weight < dist[to]) {
                               // @a relax
                               dist[to] = dist[from] + weight;
                           } else {
                               // @a noImprovement
                               continue;
                           }
                       }
                       // @a roundComplete
                   }

                   for (int[] edge : edges) {
                       int from = edge[0], to = edge[1], weight = edge[2];
                       if (dist[from] != Integer.MAX_VALUE && dist[from] + weight < dist[to]) {
                           // @a negativeCycleDetected
                           return null;
                       }
                   }
                   // @a done
                   return dist;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int start = in.getInt("start");

        if (start >= graph.vertices()) {
            throw new InputValidationException(Map.of("start",
                    "This graph only has vertices 0.." + (graph.vertices() - 1) + "."));
        }

        GraphLayout.Layout layout = GraphLayout.directed(graph);
        int v = graph.vertices();
        int[] dist = new int[v];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) {
            states.put(i, "unvisited");
        }
        states.put(start, "visiting");

        emit.at("init").say("%d vertices. dist[%d] = 0, everything else infinite.", v, start)
                .var("dist", distString(dist))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        for (int round = 0; round < v - 1; round++) {
            for (int[] edge : graph.edges()) {
                int from = edge[0], to = edge[1], weight = edge[2];
                String edgeKey = from + "-" + to;

                if (dist[from] != Integer.MAX_VALUE && dist[from] + weight < dist[to]) {
                    int old = dist[to];
                    dist[to] = dist[from] + weight;
                    emit.at("relax").say(
                                    "Round %d, edge %d-%d (weight %d): %d + %d = %d beats %s. Update dist[%d].",
                                    round + 1, from, to, weight, dist[from], weight, dist[to],
                                    old == Integer.MAX_VALUE ? "infinity" : String.valueOf(old), to)
                            .var("edge", edgeKey).var("dist", distString(dist))
                            .graph(layout.nodes(), layout.edges()).nodes(states).edges(List.of(edgeKey)).step();
                } else {
                    emit.at("noImprovement").say(
                                    "Round %d, edge %d-%d (weight %d): no improvement to dist[%d].",
                                    round + 1, from, to, weight, to)
                            .var("edge", edgeKey).var("dist", distString(dist))
                            .graph(layout.nodes(), layout.edges()).nodes(states).edges(List.of(edgeKey)).step();
                }
            }
            emit.at("roundComplete").say("Round %d complete. Distances so far: %s.", round + 1, distString(dist))
                    .var("dist", distString(dist))
                    .graph(layout.nodes(), layout.edges()).nodes(states).step();
        }

        boolean negativeCycle = false;
        for (int[] edge : graph.edges()) {
            int from = edge[0], to = edge[1], weight = edge[2];
            if (dist[from] != Integer.MAX_VALUE && dist[from] + weight < dist[to]) {
                negativeCycle = true;
                String edgeKey = from + "-" + to;
                emit.at("negativeCycleDetected").say(
                                "Edge %d-%d (weight %d) can still be relaxed after %d rounds - a "
                                        + "negative cycle is reachable from %d. Distances are undefined.",
                                from, to, weight, v - 1, start)
                        .var("edge", edgeKey)
                        .graph(layout.nodes(), layout.edges()).nodes(states).edges(List.of(edgeKey)).step();
                break;
            }
        }

        if (!negativeCycle) {
            for (int i = 0; i < v; i++) {
                states.put(i, "visited");
            }
            emit.at("done").say("No negative cycle. Final shortest distances from %d: %s.", start, distString(dist))
                    .var("dist", distString(dist))
                    .graph(layout.nodes(), layout.edges()).nodes(states).step();
        }
    }

    private static String distString(int[] dist) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dist.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(dist[i] == Integer.MAX_VALUE ? "∞" : String.valueOf(dist[i]));
        }
        return sb.append(']').toString();
    }
}
