package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * A DAG's topological order is exactly the one processing order where, by the time a vertex
 * is relaxed, every predecessor that could improve its distance has already been finalized -
 * so a single linear pass over that order (no priority queue, no revisits) suffices, even
 * with negative edge weights that would break Dijkstra.
 */
@Component
public class ShortestPathDagTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "shortest-path-dag";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Weighted DAG")
                        .help("Vertex count plus directed, weighted edges [from, to, weight]. Assumed acyclic.")
                        .directed()
                        .weighted()
                        .weights(-100, 100)
                        .constraint("maxVertices", 20)
                        .constraint("maxEdges", 48)
                        .defaultValue(Map.of(
                                "vertices", 5,
                                "edges", List.of(
                                        List.of(0, 1, 2), List.of(0, 2, 4), List.of(1, 2, 1),
                                        List.of(1, 3, 7), List.of(2, 3, 3), List.of(3, 4, 2))))
                        .build(),
                InputField.of("start", FieldType.INT)
                        .label("Source vertex")
                        .range(0, 19)
                        .defaultValue(0)
                        .build());
    }

    /** A DAG with a vertex the source can never reach. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of(
                        "vertices", 4,
                        "edges", List.of(List.of(0, 1, 5), List.of(1, 2, 3))),
                "start", 0);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] shortestPath(int v, List<List<int[]>> adj, int src) {
                   // @a topo
                   boolean[] vis = new boolean[v];
                   Deque<Integer> order = new ArrayDeque<>();
                   for (int i = 0; i < v; i++) {
                       if (!vis[i]) dfs(i, adj, vis, order);
                   }

                   // @a init
                   int[] dist = new int[v];
                   Arrays.fill(dist, Integer.MAX_VALUE);
                   dist[src] = 0;

                   while (!order.isEmpty()) {
                       // @a pop
                       int node = order.pop();
                       if (dist[node] == Integer.MAX_VALUE) {
                           // @a unreached
                           continue;
                       }
                       for (int[] edge : adj.get(node)) {
                           int next = edge[0], weight = edge[1];
                           if (dist[node] + weight < dist[next]) {
                               // @a relax
                               dist[next] = dist[node] + weight;
                           }
                       }
                   }
                   // @a done
                   return dist;
               }

               private void dfs(int node, List<List<int[]>> adj, boolean[] vis, Deque<Integer> order) {
                   vis[node] = true;
                   for (int[] edge : adj.get(node)) {
                       if (!vis[edge[0]]) dfs(edge[0], adj, vis, order);
                   }
                   order.push(node);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int start = in.getInt("start");
        int v = graph.vertices();
        if (start >= v) {
            throw new InputValidationException(Map.of("start",
                    "This graph only has vertices 0.." + (v - 1) + "."));
        }

        List<List<Inputs.GraphInput.Neighbor>> adj = graph.weightedAdjacency(true);
        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) states.put(i, "unvisited");

        boolean[] vis = new boolean[v];
        Deque<Integer> order = new ArrayDeque<>();
        for (int i = 0; i < v; i++) {
            if (!vis[i]) dfsTopo(i, adj, vis, order);
        }
        emit.at("topo").say("Topological order (pop from stack): %s.", order)
                .var("order", order.toString())
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        int[] dist = new int[v];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;
        emit.at("init").say("dist[%d] = 0, everything else infinite.", start)
                .var("dist", distString(dist)).graph(layout.nodes(), layout.edges()).nodes(states).step();

        while (!order.isEmpty()) {
            int node = order.pop();
            states.put(node, "visiting");
            if (dist[node] == Integer.MAX_VALUE) {
                emit.at("unreached").say("%d is still unreached - nothing before it in topo order can improve it. Skip.", node)
                        .var("node", node).var("dist", distString(dist))
                        .graph(layout.nodes(), layout.edges()).nodes(states).step();
                states.put(node, "visited");
                continue;
            }
            emit.at("pop").say("Process %d (already finalized at %d) - relax every outgoing edge.", node, dist[node])
                    .var("node", node).var("dist", distString(dist))
                    .graph(layout.nodes(), layout.edges()).nodes(states).step();

            for (Inputs.GraphInput.Neighbor neighbour : adj.get(node)) {
                int next = neighbour.to();
                int weight = neighbour.weight();
                if (dist[node] + weight < dist[next]) {
                    int old = dist[next];
                    dist[next] = dist[node] + weight;
                    emit.at("relax").say("Edge %d -> %d (weight %d): %d + %d = %d beats %s. Update dist[%d].",
                                    node, next, weight, dist[node], weight, dist[next],
                                    old == Integer.MAX_VALUE ? "infinity" : String.valueOf(old), next)
                            .var("node", node).var("neighbour", next).var("newDist", dist[next])
                            .var("dist", distString(dist))
                            .graph(layout.nodes(), layout.edges()).nodes(states)
                            .edges(List.of(node + "-" + next)).step();
                }
            }
            states.put(node, "visited");
        }

        emit.at("done").say("Every vertex processed. Final shortest distances from %d: %s.", start, distString(dist))
                .var("dist", distString(dist)).graph(layout.nodes(), layout.edges()).nodes(states).step();
    }

    private void dfsTopo(int node, List<List<Inputs.GraphInput.Neighbor>> adj, boolean[] vis, Deque<Integer> order) {
        vis[node] = true;
        for (Inputs.GraphInput.Neighbor neighbour : adj.get(node)) {
            if (!vis[neighbour.to()]) dfsTopo(neighbour.to(), adj, vis, order);
        }
        order.push(node);
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
