package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Recursive depth-first traversal over a caller-supplied graph.
 *
 * Was one of the 60 "Advanced Graphs" problems delegating to a 2-step placeholder
 * (generateGraphIntroSteps) shared with Dijkstra, MST, and everything else in the
 * category. This is a genuine recursive walk, mirroring bfs-traversal's structure
 * with a stack instead of a queue.
 */
@Component
public class DfsTraversalTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "dfs-traversal";
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
                        .help("Vertex count plus undirected edges.")
                        .constraint("maxVertices", 24)
                        .constraint("maxEdges", 60)
                        .defaultValue(Map.of(
                                "vertices", 6,
                                "edges", List.of(
                                        List.of(0, 1), List.of(0, 2), List.of(1, 3),
                                        List.of(2, 3), List.of(3, 4), List.of(4, 5))))
                        .build(),
                InputField.of("start", FieldType.INT)
                        .label("Start vertex")
                        .range(0, 23)
                        .defaultValue(0)
                        .build());
    }

    /** A path rather than a branching graph, and a mid-path start rather than an endpoint. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of(
                        "vertices", 5,
                        "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 3), List.of(3, 4))),
                "start", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public void dfs(int node, boolean[] vis, List<List<Integer>> adj, List<Integer> order) {
                   // @a visit
                   vis[node] = true;
                   order.add(node);
                   for (int next : adj.get(node)) {
                       if (vis[next]) {
                           // @a check
                           continue;
                       }
                       // @a recurse
                       dfs(next, vis, adj, order);
                   }
                   // @a backtrack
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
        boolean[] seen = new boolean[graph.vertices()];
        Map<Integer, String> states = new LinkedHashMap<>();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < graph.vertices(); i++) {
            states.put(i, "unvisited");
        }

        walk(start, true, graph, adj, seen, states, order, emit);
    }

    private void walk(int node, boolean isRoot, Inputs.GraphInput graph,
                      List<List<Integer>> adj, boolean[] seen,
                      Map<Integer, String> states, List<Integer> order, StepEmitter emit) {
        emit.push("dfs(" + node + ")");
        seen[node] = true;
        states.put(node, "visiting");
        order.add(node);

        String intro = isRoot
                ? String.format("%d vertices, %d edges. Enter %d and mark it visited.",
                        graph.vertices(), graph.edges().length, node)
                : String.format("Enter %d and mark it visited — position %d in the order.", node, order.size());
        emit.at("visit").say(intro)
                .var("node", node).var("order", order).graph(graph).nodes(states).step();

        for (int next : adj.get(node)) {
            if (seen[next]) {
                emit.at("check").say("%d is already visited — skip it, or this would recurse forever.", next)
                        .var("node", node).var("neighbour", next).var("order", order)
                        .graph(graph).nodes(states).edges(List.of(node + "-" + next)).step();
                continue;
            }
            emit.at("recurse").say("%d is unvisited. Descend into it before checking %d's other neighbours.", next, node)
                    .var("node", node).var("neighbour", next).var("order", order)
                    .graph(graph).nodes(states).edges(List.of(node + "-" + next)).step();
            walk(next, false, graph, adj, seen, states, order, emit);
        }

        states.put(node, "visited");
        String outro = isRoot
                ? String.format("%d has no unvisited neighbours left. Recursion fully unwound. DFS order: %s.", node, order)
                : String.format("%d has no unvisited neighbours left — backtrack.", node);
        emit.at("backtrack").say(outro).var("node", node).var("order", order)
                .graph(graph).nodes(states).step();
        emit.pop();
    }
}
