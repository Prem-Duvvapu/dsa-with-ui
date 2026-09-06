package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Counts connected components (provinces) of an undirected graph by DFS, restarting the
 * search from every still-unvisited vertex.
 *
 * <p>Modelled as a caller-supplied {@code graph} (vertex count plus edges) rather than the
 * classic N x N adjacency matrix so it reuses the same {@code GraphCanvas} topology as
 * {@code bfs-traversal}/{@code dfs-traversal} — the algorithm (count components) is
 * identical either way the neighbours are described.
 */
@Component
public class NumberOfProvincesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "number-of-provinces";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Graph")
                        .help("Vertex count plus undirected edges. Each connected component is one province.")
                        .constraint("maxVertices", 24)
                        .constraint("maxEdges", 60)
                        .defaultValue(Map.of(
                                "vertices", 4,
                                "edges", List.of(List.of(0, 1), List.of(2, 3))))
                        .build());
    }

    /** No edges at all: six isolated vertices, so every one is its own province and DFS never recurses. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of("vertices", 6, "edges", List.of()));
    }

    @Override
    public String annotatedCode() {
        return """
               public int findCircleNum(int v, List<List<Integer>> adj) {
                   // @a init
                   boolean[] vis = new boolean[v];
                   int provinces = 0;
                   for (int i = 0; i < v; i++) {
                       if (vis[i]) {
                           // @a skip
                           continue;
                       }
                       // @a found
                       provinces++;
                       dfs(i, adj, vis, provinces);
                   }
                   // @a done
                   return provinces;
               }

               private void dfs(int node, List<List<Integer>> adj, boolean[] vis, int province) {
                   // @a visit
                   vis[node] = true;
                   for (int next : adj.get(node)) {
                       if (vis[next]) {
                           // @a already
                           continue;
                       }
                       // @a recurse
                       dfs(next, adj, vis, province);
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency();
        int v = graph.vertices();
        boolean[] vis = new boolean[v];
        int provinces = 0;

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) {
            states.put(i, "unvisited");
        }

        emit.at("init").say("%d vertices, %d edges. Every connected component found is one province.",
                        v, graph.edges().length)
                .var("provinces", 0).graph(graph).nodes(states).step();

        for (int i = 0; i < v; i++) {
            if (vis[i]) {
                emit.at("skip").say("%d is already counted in an earlier province - skip it.", i)
                        .var("i", i).var("provinces", provinces)
                        .graph(graph).nodes(states).step();
                continue;
            }
            provinces++;
            emit.at("found").say("%d starts a new, uncounted component. Province #%d. dfs(%d).",
                            i, provinces, i)
                    .var("i", i).var("provinces", provinces)
                    .graph(graph).nodes(states).step();

            dfs(i, adj, vis, provinces, states, graph, emit);
        }

        emit.at("done").say("Every vertex visited. Total provinces = %d.", provinces)
                .var("provinces", provinces).graph(graph).nodes(states).step();
    }

    private void dfs(int node, List<List<Integer>> adj, boolean[] vis, int province,
                      Map<Integer, String> states, Inputs.GraphInput graph, StepEmitter emit) {
        emit.push("dfs(" + node + ")");
        vis[node] = true;
        states.put(node, "visiting");
        emit.at("visit").say("Enter %d - part of province #%d.", node, province)
                .var("node", node).var("province", province)
                .graph(graph).nodes(states).step();

        for (int next : adj.get(node)) {
            if (vis[next]) {
                emit.at("already").say("%d is already part of this province - skip it.", next)
                        .var("node", node).var("neighbour", next)
                        .graph(graph).nodes(states).edges(List.of(node + "-" + next)).step();
                continue;
            }
            emit.at("recurse").say("%d is unvisited - it belongs to the same province as %d.", next, node)
                    .var("node", node).var("neighbour", next)
                    .graph(graph).nodes(states).edges(List.of(node + "-" + next)).step();
            dfs(next, adj, vis, province, states, graph, emit);
        }

        states.put(node, "visited");
        emit.pop();
    }
}
