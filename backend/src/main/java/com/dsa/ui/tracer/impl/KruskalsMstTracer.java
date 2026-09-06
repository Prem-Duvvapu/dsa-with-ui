package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Kruskal's sorts every edge by weight up front, then greedily accepts the cheapest one
 * that does not close a cycle - DSU answers "would this close a cycle?" in near-O(1) as
 * "do these two endpoints already share a root?". Accepting v-1 such edges is provably a
 * minimum spanning tree, the same cut-property argument Prim's relies on, applied globally
 * instead of one growing frontier at a time.
 */
@Component
public class KruskalsMstTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "kruskals-mst";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Weighted graph")
                        .help("Vertex count plus undirected, weighted edges [from, to, weight].")
                        .weighted()
                        .weights(0, 1_000_000)
                        .constraint("maxVertices", 16)
                        .constraint("maxEdges", 40)
                        .defaultValue(Map.of(
                                "vertices", 4,
                                "edges", List.of(
                                        List.of(0, 1, 10), List.of(0, 2, 6), List.of(0, 3, 5),
                                        List.of(1, 3, 15), List.of(2, 3, 4))))
                        .build());
    }

    /** A different shape and weights - a different MST weight and accepted edge set. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 5,
                "edges", List.of(
                        List.of(0, 1, 2), List.of(0, 2, 1), List.of(1, 2, 1),
                        List.of(1, 3, 2), List.of(2, 3, 4), List.of(3, 4, 2))));
    }

    @Override
    public String annotatedCode() {
        return """
               public int kruskalsMst(int v, int[][] edges) {
                   // @a sort
                   Arrays.sort(edges, (a, b) -> a[2] - b[2]);
                   int[] parent = new int[v];
                   int[] rank = new int[v];
                   for (int i = 0; i < v; i++) parent[i] = i;

                   int mstWeight = 0;
                   for (int[] edge : edges) {
                       // @a consider
                       int u = edge[0], node = edge[1], w = edge[2];
                       int ru = find(u, parent), rv = find(node, parent);
                       if (ru == rv) {
                           // @a cycle
                           continue;
                       }
                       // @a accept
                       mstWeight += w;
                       if (rank[ru] < rank[rv]) parent[ru] = rv;
                       else if (rank[ru] > rank[rv]) parent[rv] = ru;
                       else { parent[rv] = ru; rank[ru]++; }
                   }
                   // @a done
                   return mstWeight;
               }

               private int find(int x, int[] parent) {
                   if (parent[x] != x) parent[x] = find(parent[x], parent);
                   return parent[x];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int v = graph.vertices();
        int[][] edges = graph.edges().clone();
        Arrays.sort(edges, Comparator.comparingInt(e -> e[2]));

        int[] parent = new int[v];
        int[] rank = new int[v];
        for (int i = 0; i < v; i++) parent[i] = i;

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) states.put(i, "unvisited");

        emit.at("sort").say("%d vertices, %d edges. Sorted by weight ascending: %s.",
                        v, edges.length, edgesString(edges))
                .var("mstWeight", 0).graph(graph).nodes(states).step();

        int mstWeight = 0;
        int accepted = 0;
        for (int[] edge : edges) {
            int u = edge[0], node = edge[1], w = edge[2];
            String edgeKey = u + "-" + node;
            int ru = find(u, parent);
            int rv = find(node, parent);

            emit.at("consider").say("Consider %d-%d (weight %d). Roots: %d and %d.", u, node, w, ru, rv)
                    .var("edge", edgeKey).var("mstWeight", mstWeight)
                    .graph(graph).nodes(states).edges(List.of(edgeKey)).step();

            if (ru == rv) {
                emit.at("cycle").say("%d and %d are already in the same component - accepting this edge would close a cycle. Reject.",
                                u, node)
                        .var("edge", edgeKey)
                        .graph(graph).nodes(states).edges(List.of(edgeKey)).step();
                continue;
            }

            mstWeight += w;
            accepted++;
            states.put(u, "visited");
            states.put(node, "visited");
            if (rank[ru] < rank[rv]) {
                parent[ru] = rv;
            } else if (rank[ru] > rank[rv]) {
                parent[rv] = ru;
            } else {
                parent[rv] = ru;
                rank[ru]++;
            }
            emit.at("accept").say("Different components - accept %d-%d. Total weight so far: %d (%d/%d edges).",
                            u, node, mstWeight, accepted, v - 1)
                    .var("edge", edgeKey).var("mstWeight", mstWeight)
                    .graph(graph).nodes(states).edges(List.of(edgeKey)).step();

            if (accepted == v - 1) {
                break;
            }
        }

        emit.at("done").say("%d edges accepted. Minimum spanning tree weight: %d.", accepted, mstWeight)
                .var("mstWeight", mstWeight).graph(graph).nodes(states).step();
    }

    private static int find(int x, int[] parent) {
        if (parent[x] != x) {
            parent[x] = find(parent[x], parent);
        }
        return parent[x];
    }

    private static String edgesString(int[][] edges) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < edges.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(edges[i][0]).append("-").append(edges[i][1]).append("(w").append(edges[i][2]).append(")");
        }
        return sb.append(']').toString();
    }
}
