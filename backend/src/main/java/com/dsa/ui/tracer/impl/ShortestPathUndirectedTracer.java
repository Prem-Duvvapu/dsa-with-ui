package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * When every edge has the same weight, BFS already visits vertices in nondecreasing
 * distance order, so the first time a vertex is reached IS its shortest distance - no
 * relaxation or priority queue is needed, unlike the general weighted case.
 */
@Component
public class ShortestPathUndirectedTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "shortest-path-undirected";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Unweighted graph")
                        .help("Vertex count plus undirected edges, all of unit weight.")
                        .constraint("maxVertices", 24)
                        .constraint("maxEdges", 60)
                        .defaultValue(Map.of(
                                "vertices", 6,
                                "edges", List.of(
                                        List.of(0, 1), List.of(0, 2), List.of(1, 3),
                                        List.of(2, 3), List.of(3, 4), List.of(4, 5))))
                        .build(),
                InputField.of("start", FieldType.INT)
                        .label("Source vertex")
                        .range(0, 23)
                        .defaultValue(0)
                        .build());
    }

    /** A disconnected pair of vertices the source can never reach. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of(
                        "vertices", 5,
                        "edges", List.of(List.of(0, 1), List.of(1, 2))),
                "start", 0);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] shortestPath(int v, List<List<Integer>> adj, int src) {
                   // @a init
                   int[] dist = new int[v];
                   Arrays.fill(dist, -1);
                   dist[src] = 0;
                   Queue<Integer> queue = new LinkedList<>();
                   queue.add(src);

                   while (!queue.isEmpty()) {
                       // @a poll
                       int node = queue.poll();
                       for (int next : adj.get(node)) {
                           if (dist[next] == -1) {
                               // @a relax
                               dist[next] = dist[node] + 1;
                               queue.add(next);
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
        if (start >= graph.vertices()) {
            throw new InputValidationException(Map.of("start",
                    "This graph only has vertices 0.." + (graph.vertices() - 1) + "."));
        }

        List<List<Integer>> adj = graph.adjacency();
        int v = graph.vertices();
        int[] dist = new int[v];
        Arrays.fill(dist, -1);
        dist[start] = 0;

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) states.put(i, "unvisited");
        states.put(start, "queued");

        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(start);

        emit.at("init").say("%d vertices. dist[%d] = 0, everything else unreached (-1). Enqueue the source.",
                        v, start)
                .var("dist", Arrays.toString(dist)).graph(graph).nodes(states).queue(queue).step();

        while (!queue.isEmpty()) {
            int node = queue.poll();
            states.put(node, "visited");
            emit.at("poll").say("Dequeue %d, already finalized at distance %d.", node, dist[node])
                    .var("node", node).var("dist", Arrays.toString(dist))
                    .graph(graph).nodes(states).queue(queue).step();

            for (int next : adj.get(node)) {
                if (dist[next] == -1) {
                    dist[next] = dist[node] + 1;
                    queue.add(next);
                    states.put(next, "queued");
                    emit.at("relax").say("%d is unreached - its shortest distance is %d + 1 = %d.",
                                    next, dist[node], dist[next])
                            .var("node", node).var("neighbour", next).var("newDist", dist[next])
                            .var("dist", Arrays.toString(dist))
                            .graph(graph).nodes(states).edges(List.of(node + "-" + next)).queue(queue).step();
                }
            }
        }

        emit.at("done").say("Queue empty. Final shortest distances from %d: %s.", start, Arrays.toString(dist))
                .var("dist", Arrays.toString(dist)).graph(graph).nodes(states).step();
    }
}
