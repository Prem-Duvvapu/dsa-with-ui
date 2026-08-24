package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Single-source shortest paths on a weighted undirected graph, via a min-heap.
 *
 * Was one of the 60 "Advanced Graphs" problems delegating to generateGraphIntroSteps(),
 * a 2-step placeholder shared with everything else in the category — including, until
 * this tracer, Dijkstra itself.
 *
 * Lazy deletion is shown explicitly: a vertex can sit in the queue at more than one
 * distance (an earlier, since-beaten one and the current best), and the stale entry is
 * popped and discarded later rather than removed on the spot. That is a real source of
 * confusion for anyone learning this algorithm from pseudocode, and the default input is
 * chosen so it actually happens rather than being a footnote in the prose.
 */
@Component
public class DijkstraTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "dijkstra-min-heap";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Weighted graph")
                        .help("Vertex count plus undirected, weighted edges [from, to, weight].")
                        .weighted()
                        .constraint("maxVertices", 16)
                        .constraint("maxEdges", 40)
                        .defaultValue(Map.of(
                                "vertices", 5,
                                "edges", List.of(
                                        List.of(0, 1, 4), List.of(0, 2, 1), List.of(2, 1, 2),
                                        List.of(1, 3, 1), List.of(2, 3, 5), List.of(3, 4, 3))))
                        .build(),
                InputField.of("start", FieldType.INT)
                        .label("Source vertex")
                        .range(0, 15)
                        .defaultValue(0)
                        .build());
    }

    /** A different shape, weights, and answer — the short detour beats the direct edge. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of(
                        "vertices", 4,
                        "edges", List.of(
                                List.of(0, 1, 2), List.of(1, 2, 2), List.of(2, 3, 2), List.of(0, 3, 10))),
                "start", 0);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] dijkstra(int v, List<List<int[]>> adj, int src) {
                   // @a init
                   int[] dist = new int[v];
                   Arrays.fill(dist, Integer.MAX_VALUE);
                   dist[src] = 0;
                   PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]);
                   pq.add(new int[]{src, 0});

                   while (!pq.isEmpty()) {
                       // @a extract
                       int[] top = pq.poll();
                       int node = top[0], d = top[1];
                       if (d > dist[node]) {
                           // @a stale
                           continue;
                       }
                       for (int[] edge : adj.get(node)) {
                           int next = edge[0], weight = edge[1];
                           if (dist[node] + weight < dist[next]) {
                               // @a relax
                               dist[next] = dist[node] + weight;
                               pq.add(new int[]{next, dist[next]});
                           } else {
                               // @a skip
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
        int start = in.getInt("start");
        emit.using("Graph");

        if (start >= graph.vertices()) {
            throw new InputValidationException(Map.of("start",
                    "This graph only has vertices 0.." + (graph.vertices() - 1) + "."));
        }

        List<List<Inputs.GraphInput.Neighbor>> adj = graph.weightedAdjacency();
        int[] dist = new int[graph.vertices()];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < graph.vertices(); i++) {
            states.put(i, "unvisited");
        }
        states.put(start, "queued");

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.add(new int[]{start, 0});

        emit.at("init").say("%d vertices. dist[%d] = 0, everything else infinite. Enqueue the source.",
                        graph.vertices(), start)
                .var("dist", distString(dist)).nodes(states).step();

        while (!pq.isEmpty()) {
            int[] top = pq.poll();
            int node = top[0];
            int d = top[1];

            emit.at("extract").say("Pop the smallest entry in the queue: %d at distance %d.", node, d)
                    .var("node", node).var("poppedDist", d).var("dist", distString(dist))
                    .nodes(states).step();

            if (d > dist[node]) {
                emit.at("stale").say(
                                "%d was already finalized at %d, better than this entry's %d — it was enqueued before that update. Discard it.",
                                node, dist[node], d)
                        .var("node", node).var("poppedDist", d).var("dist", distString(dist))
                        .nodes(states).step();
                continue;
            }

            states.put(node, "visiting");

            for (Inputs.GraphInput.Neighbor neighbour : adj.get(node)) {
                int next = neighbour.to();
                int weight = neighbour.weight();
                String edgeKey = node + "-" + next;

                if (dist[node] + weight < dist[next]) {
                    int old = dist[next];
                    dist[next] = dist[node] + weight;
                    pq.add(new int[]{next, dist[next]});
                    if (!"visited".equals(states.get(next))) {
                        states.put(next, "queued");
                    }

                    emit.at("relax").say("Edge %d-%d (weight %d): %d + %d = %d beats %s. Update dist[%d] and enqueue.",
                                    node, next, weight, dist[node], weight, dist[next],
                                    old == Integer.MAX_VALUE ? "infinity" : String.valueOf(old), next)
                            .var("edge", edgeKey).var("newDist", dist[next]).var("dist", distString(dist))
                            .nodes(states).edges(List.of(edgeKey)).step();
                } else {
                    emit.at("skip").say("Edge %d-%d (weight %d): %d + %d = %d does not beat dist[%d]=%d. No update.",
                                    node, next, weight, dist[node], weight, dist[node] + weight, next, dist[next])
                            .var("edge", edgeKey).var("dist", distString(dist))
                            .nodes(states).edges(List.of(edgeKey)).step();
                }
            }

            states.put(node, "done");
        }

        emit.at("done").say("Priority queue empty. Final shortest distances from %d: %s.", start, distString(dist))
                .var("dist", distString(dist)).nodes(states).step();
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
