package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Counts maximal connected regions with an outer scan plus DFS. */
@Component
public class ConnectedComponentsIntroTracer implements AlgorithmTracer {
    @Override public String id() { return "connected-components-intro"; }
    @Override public DsType dsType() { return DsType.GRAPH; }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("graph", FieldType.GRAPH)
                .label("Undirected graph")
                .help("Disconnected regions are discovered by an outer vertex scan and DFS.")
                .constraint("maxVertices", 16).constraint("maxEdges", 40)
                .defaultValue(Map.of("vertices", 7, "edges", List.of(
                        List.of(0, 1), List.of(1, 2), List.of(3, 4), List.of(4, 5))))
                .build());
    }

    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of("vertices", 6, "edges", List.of(
                List.of(0, 1), List.of(1, 2), List.of(2, 3),
                List.of(3, 4), List.of(4, 5), List.of(5, 0))));
    }

    @Override
    public String annotatedCode() {
        return """
               public int components(int vertices, List<List<Integer>> adj) {
                   // @a init
                   boolean[] seen = new boolean[vertices];
                   int count = 0;
                   for (int node = 0; node < vertices; node++) {
                       if (seen[node]) {
                           // @a skip
                           continue;
                       }
                       // @a start
                       count++;
                       dfs(node, adj, seen);
                   }
                   // @a done
                   return count;
               }

               private void dfs(int node, List<List<Integer>> adj, boolean[] seen) {
                   // @a visit
                   seen[node] = true;
                   for (int next : adj.get(node)) {
                       if (!seen[next]) {
                           // @a descend
                           dfs(next, adj, seen);
                       }
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency();
        boolean[] seen = new boolean[graph.vertices()];
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < graph.vertices(); i++) states.put(i, "unvisited");

        emit.at("init").say("Scan all %d vertices. Each unseen vertex begins one new component.", graph.vertices())
                .var("components", 0).graph(graph).nodes(states).step();

        int count = 0;
        for (int node = 0; node < graph.vertices(); node++) {
            if (seen[node]) {
                emit.at("skip").say("Vertex %d was already reached, so it belongs to an existing component.", node)
                        .var("node", node).var("components", count).graph(graph).nodes(states).step();
                continue;
            }
            count++;
            emit.at("start").say("Vertex %d is unseen: open component #%d and flood it with DFS.", node, count)
                    .var("node", node).var("components", count).graph(graph).nodes(states).step();
            dfs(node, count, adj, seen, states, graph, emit);
        }

        emit.at("done").say("Every vertex is claimed. Component count: %d.", count)
                .var("components", count).graph(graph).nodes(states).step();
    }

    private void dfs(int node, int component, List<List<Integer>> adj, boolean[] seen,
                     Map<Integer, String> states, Inputs.GraphInput graph, StepEmitter emit) {
        seen[node] = true;
        states.put(node, "visiting");
        emit.push("dfs(" + node + ")");
        emit.at("visit").say("Claim vertex %d for component #%d.", node, component)
                .var("node", node).var("component", component).graph(graph).nodes(states).step();

        for (int next : adj.get(node)) {
            if (!seen[next]) {
                emit.at("descend").say("%d reaches unseen neighbour %d; descend.", node, next)
                        .var("from", node).var("to", next).graph(graph).nodes(states)
                        .edges(List.of(node + "-" + next)).step();
                dfs(next, component, adj, seen, states, graph, emit);
            }
        }
        states.put(node, "visited");
        emit.pop();
    }
}
