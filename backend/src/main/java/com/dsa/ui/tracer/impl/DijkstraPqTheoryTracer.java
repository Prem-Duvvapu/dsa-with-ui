package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Demonstrates why Dijkstra's frontier must be ordered by tentative distance. */
@Component
public class DijkstraPqTheoryTracer implements AlgorithmTracer {
    @Override public String id() { return "dijkstra-pq-theory"; }
    @Override public DsType dsType() { return DsType.GRAPH; }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Non-negative weighted graph")
                        .help("The priority queue always exposes the unsettled vertex with the cheapest distance.")
                        .weighted().weights(0, 1_000_000)
                        .constraint("maxVertices", 16).constraint("maxEdges", 40)
                        .defaultValue(Map.of("vertices", 6, "edges", List.of(
                                List.of(0, 1, 8), List.of(0, 2, 2), List.of(2, 1, 1),
                                List.of(1, 3, 3), List.of(2, 4, 7), List.of(3, 5, 2),
                                List.of(4, 5, 1))))
                        .build(),
                InputField.of("source", FieldType.INT).label("Source vertex")
                        .range(0, 15).defaultValue(0).build());
    }

    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of("vertices", 5, "edges", List.of(
                        List.of(0, 1, 4), List.of(0, 2, 6), List.of(1, 2, 1),
                        List.of(1, 3, 7), List.of(2, 4, 2), List.of(3, 4, 1))),
                "source", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] shortestPaths(int n, List<List<int[]>> adj, int source) {
                   // @a init
                   int[] dist = new int[n];
                   Arrays.fill(dist, Integer.MAX_VALUE);
                   dist[source] = 0;
                   PriorityQueue<int[]> frontier = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
                   frontier.add(new int[]{source, 0});
                   while (!frontier.isEmpty()) {
                       // @a cheapest
                       int[] current = frontier.poll();
                       if (current[1] != dist[current[0]]) {
                           // @a stale
                           continue;
                       }
                       for (int[] edge : adj.get(current[0])) {
                           int candidate = current[1] + edge[1];
                           if (candidate < dist[edge[0]]) {
                               // @a improve
                               dist[edge[0]] = candidate;
                               frontier.add(new int[]{edge[0], candidate});
                           } else {
                               // @a keep
                               continue;
                           }
                       }
                   }
                   // @a done
                   return dist;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int source = in.getInt("source");
        if (source >= graph.vertices()) {
            throw new InputValidationException(Map.of("source",
                    "This graph only has vertices 0.." + (graph.vertices() - 1) + "."));
        }
        List<List<Inputs.GraphInput.Neighbor>> adj = graph.weightedAdjacency();
        int[] dist = new int[graph.vertices()];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[source] = 0;
        boolean[] settled = new boolean[graph.vertices()];
        Map<Integer, String> states = states(graph.vertices());
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.add(new int[]{source, 0});
        states.put(source, "queued");

        emit.at("init").say("Only source %d has a known distance (0). It opens the priority frontier.", source)
                .var("dist", distances(dist)).graph(graph).nodes(states).queue(snapshot(pq)).step();

        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int node = current[0], queuedDistance = current[1];
            emit.at("cheapest").say("The priority queue exposes %d at distance %d before every costlier option.",
                            node, queuedDistance)
                    .var("node", node).var("queuedDistance", queuedDistance).var("dist", distances(dist))
                    .graph(graph).nodes(states).queue(snapshot(pq)).step();
            if (queuedDistance != dist[node]) {
                emit.at("stale").say("That entry is stale: dist[%d] is now %d, so discard the old %d.",
                                node, dist[node], queuedDistance)
                        .var("node", node).var("dist", distances(dist))
                        .graph(graph).nodes(states).queue(snapshot(pq)).step();
                continue;
            }
            settled[node] = true;
            states.put(node, "visited");
            for (Inputs.GraphInput.Neighbor edge : adj.get(node)) {
                int next = edge.to();
                int candidate = dist[node] + edge.weight();
                String edgeKey = node + "-" + next;
                if (candidate < dist[next]) {
                    int old = dist[next];
                    dist[next] = candidate;
                    pq.add(new int[]{next, candidate});
                    if (!settled[next]) states.put(next, "queued");
                    emit.at("improve").say("%d -> %d offers %d, beating %s. Queue the improved key.",
                                    node, next, candidate, old == Integer.MAX_VALUE ? "infinity" : old)
                            .var("dist", distances(dist)).graph(graph).nodes(states)
                            .edges(List.of(edgeKey)).queue(snapshot(pq)).step();
                } else {
                    emit.at("keep").say("%d -> %d offers %d; current distance %s is no worse.",
                                    node, next, candidate,
                                    dist[next] == Integer.MAX_VALUE ? "infinity" : String.valueOf(dist[next]))
                            .var("dist", distances(dist)).graph(graph).nodes(states)
                            .edges(List.of(edgeKey)).queue(snapshot(pq)).step();
                }
            }
        }

        emit.at("done").say("Frontier empty. Final distances from %d: %s.", source, distances(dist))
                .var("dist", distances(dist)).graph(graph).nodes(states).queue(List.of()).step();
    }

    private static Map<Integer, String> states(int n) {
        Map<Integer, String> out = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) out.put(i, "unvisited");
        return out;
    }

    private static List<String> snapshot(PriorityQueue<int[]> pq) {
        List<int[]> entries = new ArrayList<>(pq);
        entries.sort(Comparator.comparingInt(a -> a[1]));
        return entries.stream().map(e -> e[0] + ":" + e[1]).toList();
    }

    private static String distances(int[] values) {
        List<String> out = new ArrayList<>();
        for (int value : values) out.add(value == Integer.MAX_VALUE ? "∞" : String.valueOf(value));
        return out.toString();
    }
}
