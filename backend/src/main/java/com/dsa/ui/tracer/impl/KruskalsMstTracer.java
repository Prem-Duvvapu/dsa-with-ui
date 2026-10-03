package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Minimum spanning tree weight by Kruskal's algorithm, written in the owner's style on their own
 * DisjointSet (they have no Kruskal submission of their own): sort the edges by weight, and take
 * an edge only when its ends are in different components - otherwise it would close a cycle.
 * O(E log E).
 *
 * <p>Java's object sort is stable, so equal weights keep their input order, exactly as the code's
 * {@code Arrays.sort} would. The tree edges taken so far stay highlighted.
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
        return OwnerDisjointSet.code(Set.of()) + "\n\n" + """
               // Written in your style on your DisjointSet - your repo has no Kruskal submission of its own.
               class Solution {
                   public int spanningTree(int V, int[][] edges) {
                       // @a init
                       DisjointSet ds = new DisjointSet(V);
                       int mstWeight = 0;

                       Arrays.sort(edges, (x,y) -> Integer.compare(x[2], y[2]));

                       for (int[] e: edges) {
                           int u = e[0];
                           int v = e[1];
                           int wt = e[2];

                           if (ds.getUltimateParent(u) == ds.getUltimateParent(v))
                               // @a skip
                               continue;

                           // @a take
                           ds.unionBySize(u, v);
                           mstWeight += wt;
                       }

                       // @a done
                       return mstWeight;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int V = graph.vertices();
        List<int[]> edges = new ArrayList<>(Arrays.asList(graph.edges()));
        edges.sort((x, y) -> Integer.compare(x[2], y[2]));
        OwnerDisjointSet ds = new OwnerDisjointSet(V);
        int mstWeight = 0;
        List<String> taken = new ArrayList<>();
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < V; i++) {
            states.put(i, "unvisited");
        }

        List<String> order = new ArrayList<>();
        for (int[] e : edges) order.add(e[0] + "-" + e[1] + " (" + e[2] + ")");
        emit.at("init").say("%d vertices, each its own component. Sorted by weight, the edges are: %s.",
                        V, String.join(", ", order))
                .var("mstWeight", 0).var("parent[]", ds.parents()).var("size[]", ds.sizes())
                .graph(graph).nodes(states).step();

        for (int[] e : edges) {
            int u = e[0];
            int v = e[1];
            int wt = e[2];
            String key = u + "-" + v;
            List<String> compressed = new ArrayList<>();
            if (ds.find(u, compressed) == ds.find(v, compressed)) {
                List<String> shown = new ArrayList<>(taken);
                shown.add(key);
                emit.at("skip").say("Edge %d-%d (weight %d): %d and %d are already connected by cheaper edges, "
                                + "so taking it would close a cycle. Skip it.", u, v, wt, u, v)
                        .var("mstWeight", mstWeight).var("parent[]", ds.parents()).var("size[]", ds.sizes())
                        .graph(graph).nodes(states).edges(shown).step();
                continue;
            }
            OwnerDisjointSet.Union result = ds.union(u, v);
            mstWeight += wt;
            taken.add(key);
            states.put(u, "done");
            states.put(v, "done");
            emit.at("take").say("Edge %d-%d (weight %d) joins two different components - take it. mstWeight = %d. %s",
                            u, v, wt, mstWeight, result.narrate(u, v))
                    .var("mstWeight", mstWeight).var("parent[]", ds.parents()).var("size[]", ds.sizes())
                    .graph(graph).nodes(states).edges(taken).step();
        }

        emit.at("done").say("Every edge has been considered. The %d tree edge%s weigh %d in total.",
                        taken.size(), Narration.s(taken.size()), mstWeight)
                .var("mstWeight", mstWeight).graph(graph).nodes(states).edges(taken).step();
    }
}
