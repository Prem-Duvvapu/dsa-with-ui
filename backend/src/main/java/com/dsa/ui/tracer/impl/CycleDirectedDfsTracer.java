package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Cycle detection in a DIRECTED graph by DFS with a recursion-stack marker ({@code pathVis}),
 * since in a directed graph reaching an already-visited vertex only proves a cycle when that
 * vertex is still on the CURRENT call path - reachability through a different, already-
 * finished branch is not a back-edge.
 */
@Component
public class CycleDirectedDfsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "cycle-directed-dfs";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Directed graph")
                        .help("Vertex count plus directed edges [from, to].")
                        .directed()
                        .constraint("maxVertices", 20)
                        .constraint("maxEdges", 48)
                        .defaultValue(Map.of(
                                "vertices", 4,
                                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 3), List.of(3, 0))))
                        .build());
    }

    /** A linear DAG chain instead of a cycle - the recursion unwinds cleanly for every node. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 5,
                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 3), List.of(3, 4))));
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isCyclic(int v, List<List<Integer>> adj) {
                   // @a init
                   boolean[] vis = new boolean[v];
                   boolean[] pathVis = new boolean[v];
                   for (int i = 0; i < v; i++) {
                       if (!vis[i]) {
                           // @a component
                           if (dfsCheck(i, adj, vis, pathVis)) {
                               return true;
                           }
                       }
                   }
                   // @a noCycle
                   return false;
               }

               private boolean dfsCheck(int node, List<List<Integer>> adj, boolean[] vis, boolean[] pathVis) {
                   // @a visit
                   vis[node] = true;
                   pathVis[node] = true;
                   for (int next : adj.get(node)) {
                       if (!vis[next]) {
                           // @a recurse
                           if (dfsCheck(next, adj, vis, pathVis)) {
                               return true;
                           }
                       } else if (pathVis[next]) {
                           // @a cycleDetected
                           return true;
                       }
                   }
                   // @a backtrack
                   pathVis[node] = false;
                   return false;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency(true);
        int v = graph.vertices();
        boolean[] vis = new boolean[v];
        boolean[] pathVis = new boolean[v];
        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) {
            states.put(i, "unvisited");
        }

        emit.at("init").say("%d vertices, %d directed edges. Track an overall visited set plus the current recursion path.",
                        v, graph.edges().length)
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        boolean cycle = false;
        for (int i = 0; i < v && !cycle; i++) {
            if (vis[i]) {
                continue;
            }
            emit.at("component").say("%d starts a new component. dfsCheck(%d).", i, i)
                    .var("src", i).graph(layout.nodes(), layout.edges()).nodes(states).step();

            cycle = dfsCheck(i, adj, vis, pathVis, states, layout, emit);
        }

        if (!cycle) {
            emit.at("noCycle").say("Every node's recursion path unwound cleanly. No directed cycle.")
                    .graph(layout.nodes(), layout.edges()).nodes(states).step();
        }
    }

    private boolean dfsCheck(int node, List<List<Integer>> adj, boolean[] vis, boolean[] pathVis,
                              Map<Integer, String> states, GraphLayout.Layout layout, StepEmitter emit) {
        emit.push("dfsCheck(" + node + ")");
        vis[node] = true;
        pathVis[node] = true;
        states.put(node, "visiting");
        emit.at("visit").say("Enter %d. Mark it visited and add it to the current recursion path.", node)
                .var("node", node).graph(layout.nodes(), layout.edges()).nodes(states).step();

        for (int next : adj.get(node)) {
            if (!vis[next]) {
                emit.at("recurse").say("%d -> %d is unvisited. Descend into it.", node, next)
                        .var("node", node).var("neighbour", next)
                        .graph(layout.nodes(), layout.edges()).nodes(states).edges(List.of(node + "-" + next)).step();
                if (dfsCheck(next, adj, vis, pathVis, states, layout, emit)) {
                    emit.pop();
                    return true;
                }
            } else if (pathVis[next]) {
                states.put(next, "cycle");
                emit.at("cycleDetected").say(
                                "%d -> %d: %d is already on the CURRENT recursion path - a directed cycle exists.",
                                node, next, next)
                        .var("node", node).var("neighbour", next)
                        .graph(layout.nodes(), layout.edges()).nodes(states).edges(List.of(node + "-" + next)).step();
                emit.pop();
                return true;
            }
        }

        pathVis[node] = false;
        states.put(node, "visited");
        emit.at("backtrack").say("%d has no unexplored or on-path neighbours left. Remove it from the recursion path.", node)
                .var("node", node).graph(layout.nodes(), layout.edges()).nodes(states).step();
        emit.pop();
        return false;
    }
}
