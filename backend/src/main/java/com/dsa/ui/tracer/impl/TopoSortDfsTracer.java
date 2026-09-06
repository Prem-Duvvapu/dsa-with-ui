package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Topological sort by DFS: a vertex is only pushed onto the stack once every vertex
 * reachable from it has already been pushed, so popping the stack yields an order where
 * every edge points from an earlier-popped vertex to a later one. Unlike Kahn's BFS this
 * needs no indegree array - the post-order push does the same job implicitly.
 */
@Component
public class TopoSortDfsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "topo-sort-dfs";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("DAG")
                        .help("Vertex count plus directed edges [from, to]. Assumed acyclic.")
                        .directed()
                        .constraint("maxVertices", 20)
                        .constraint("maxEdges", 48)
                        .defaultValue(Map.of(
                                "vertices", 6,
                                "edges", List.of(
                                        List.of(0, 1), List.of(0, 2), List.of(1, 3),
                                        List.of(2, 4), List.of(3, 5), List.of(4, 5))))
                        .build());
    }

    /** Three independent sources converging on one sink - no chain of dependent pushes. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 4,
                "edges", List.of(List.of(0, 3), List.of(1, 3), List.of(2, 3))));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] topoSort(int v, List<List<Integer>> adj) {
                   boolean[] vis = new boolean[v];
                   Deque<Integer> stack = new ArrayDeque<>();
                   for (int i = 0; i < v; i++) {
                       if (!vis[i]) {
                           // @a component
                           dfs(i, adj, vis, stack);
                       }
                   }
                   int[] order = new int[v];
                   int idx = 0;
                   // @a unwind
                   while (!stack.isEmpty()) {
                       order[idx++] = stack.pop();
                   }
                   return order;
               }

               private void dfs(int node, List<List<Integer>> adj, boolean[] vis, Deque<Integer> stack) {
                   // @a visit
                   vis[node] = true;
                   for (int next : adj.get(node)) {
                       if (!vis[next]) {
                           // @a recurse
                           dfs(next, adj, vis, stack);
                       }
                   }
                   // @a push
                   stack.push(node);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency(true);
        int v = graph.vertices();
        boolean[] vis = new boolean[v];
        Deque<Integer> stack = new ArrayDeque<>();
        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) states.put(i, "unvisited");

        for (int i = 0; i < v; i++) {
            if (vis[i]) {
                continue;
            }
            emit.at("component")
                    .say("%d starts a new DFS. Every vertex it reaches gets pushed on the way back out.", i)
                    .var("src", i).graph(layout.nodes(), layout.edges()).nodes(states)
                    .stack(stack).step();
            dfs(i, adj, vis, states, stack, layout, emit);
        }

        int[] order = new int[v];
        int idx = 0;
        while (!stack.isEmpty()) {
            order[idx++] = stack.pop();
        }
        emit.at("unwind")
                .say("Pop the stack: %s. Every edge points from an earlier position to a later one.", Arrays.toString(order))
                .var("order", Arrays.toString(order))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }

    private void dfs(int node, List<List<Integer>> adj, boolean[] vis, Map<Integer, String> states,
                      Deque<Integer> stack, GraphLayout.Layout layout, StepEmitter emit) {
        emit.push("dfs(" + node + ")");
        vis[node] = true;
        states.put(node, "visiting");
        emit.at("visit").say("Enter %d.", node)
                .var("node", node).graph(layout.nodes(), layout.edges()).nodes(states).stack(stack).step();

        for (int next : adj.get(node)) {
            if (!vis[next]) {
                emit.at("recurse").say("%d -> %d is unvisited. Descend into it.", node, next)
                        .var("node", node).var("neighbour", next)
                        .graph(layout.nodes(), layout.edges()).nodes(states)
                        .edges(List.of(node + "-" + next)).stack(stack).step();
                dfs(next, adj, vis, states, stack, layout, emit);
            }
        }

        stack.push(node);
        states.put(node, "visited");
        emit.at("push").say("%d has no unvisited neighbours left. Push it - everything it can reach is already on the stack.", node)
                .var("node", node).graph(layout.nodes(), layout.edges()).nodes(states).stack(stack).step();
        emit.pop();
    }
}
