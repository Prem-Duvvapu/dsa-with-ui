package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Strongly connected components via Kosaraju's algorithm: a DFS over the original graph that
 * records each vertex's finish order, then a second DFS over the transpose graph (every edge
 * reversed) that pops that finish-order stack, launching one fresh DFS tree per unvisited
 * vertex. Each such tree is provably exactly one SCC - that fact is what the whole algorithm
 * rests on, so the narration says it plainly at the moment it happens rather than only at
 * the end.
 */
@Component
public class KosarajuSccTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "kosaraju-scc";
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
                        .constraint("maxVertices", 12)
                        .constraint("maxEdges", 24)
                        .defaultValue(Map.of(
                                "vertices", 5,
                                "edges", List.of(
                                        List.of(0, 1), List.of(1, 2), List.of(2, 0),
                                        List.of(1, 3), List.of(3, 4))))
                        .build());
    }

    /** A single SCC spanning every vertex - one component instead of the default's three. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of(
                        "vertices", 4,
                        "edges", List.of(
                                List.of(0, 1), List.of(1, 2), List.of(2, 3), List.of(3, 0))));
    }

    @Override
    public String annotatedCode() {
        return """
               List<List<Integer>> kosaraju(int v, List<List<Integer>> adj) {
                   boolean[] visited = new boolean[v];
                   Deque<Integer> finishOrder = new ArrayDeque<>();

                   for (int i = 0; i < v; i++) {
                       if (!visited[i]) dfs(i, adj, visited, finishOrder);
                   }

                   // @a buildTranspose
                   List<List<Integer>> transpose = reverse(v, adj);

                   boolean[] visited2 = new boolean[v];
                   List<List<Integer>> sccs = new ArrayList<>();
                   while (!finishOrder.isEmpty()) {
                       int u = finishOrder.pop();
                       if (!visited2[u]) {
                           // @a startNewComponent
                           List<Integer> component = new ArrayList<>();
                           dfsTranspose(u, transpose, visited2, component);
                           sccs.add(component);
                       }
                   }
                   // @a done
                   return sccs;
               }

               void dfs(int u, List<List<Integer>> adj, boolean[] visited, Deque<Integer> finishOrder) {
                   visited[u] = true;
                   // @a dfsVisit
                   for (int next : adj.get(u)) {
                       if (!visited[next]) dfs(next, adj, visited, finishOrder);
                   }
                   finishOrder.push(u);
                   // @a pushFinishOrder
               }

               void dfsTranspose(int u, List<List<Integer>> transpose, boolean[] visited, List<Integer> component) {
                   visited[u] = true;
                   component.add(u);
                   // @a transposeDfsVisit
                   for (int next : transpose.get(u)) {
                       if (!visited[next]) dfsTranspose(next, transpose, visited, component);
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int v = graph.vertices();
        List<List<Integer>> adj = graph.adjacency(true);
        GraphLayout.Layout layout = GraphLayout.directed(graph);

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) {
            states.put(i, "unvisited");
        }

        boolean[] visited = new boolean[v];
        Deque<Integer> finishOrder = new ArrayDeque<>();
        for (int i = 0; i < v; i++) {
            if (!visited[i]) {
                dfs(i, adj, visited, finishOrder, states, layout, emit);
            }
        }

        int[][] transposedEdges = new int[graph.edges().length][];
        for (int i = 0; i < graph.edges().length; i++) {
            int[] e = graph.edges()[i];
            transposedEdges[i] = new int[]{e[1], e[0]};
        }
        Inputs.GraphInput transposeGraph = new Inputs.GraphInput(v, transposedEdges);
        List<List<Integer>> transpose = transposeGraph.adjacency(true);
        GraphLayout.Layout transposeLayout = GraphLayout.directed(transposeGraph);

        for (int i = 0; i < v; i++) {
            states.put(i, "unvisited");
        }
        emit.at("buildTranspose").say(
                        "Original DFS finished. Reverse every edge to build the transpose graph, "
                                + "then replay the finish-order stack on it.")
                .graph(transposeLayout.nodes(), transposeLayout.edges()).nodes(states).stack(finishOrder).step();

        boolean[] visited2 = new boolean[v];
        List<List<Integer>> sccs = new ArrayList<>();
        while (!finishOrder.isEmpty()) {
            int u = finishOrder.pop();
            if (!visited2[u]) {
                emit.at("startNewComponent").say(
                                "Pop %d off the finish-order stack - unvisited, so it starts a new "
                                        + "strongly connected component.", u)
                        .var("node", u)
                        .graph(transposeLayout.nodes(), transposeLayout.edges()).nodes(states)
                        .stack(finishOrder).step();

                List<Integer> component = new ArrayList<>();
                dfsTranspose(u, transpose, visited2, component, states, transposeLayout, emit);
                sccs.add(component);
            }
        }

        emit.at("done").say("%d strongly connected component(s) found: %s.", sccs.size(), sccs)
                .var("sccs", String.valueOf(sccs))
                .graph(transposeLayout.nodes(), transposeLayout.edges()).nodes(states).step();
    }

    private void dfs(int u, List<List<Integer>> adj, boolean[] visited, Deque<Integer> finishOrder,
                      Map<Integer, String> states, GraphLayout.Layout layout, StepEmitter emit) {
        visited[u] = true;
        states.put(u, "visiting");
        emit.at("dfsVisit").say("Visit %d (phase 1 DFS on the original graph).", u)
                .var("node", u)
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        for (int next : adj.get(u)) {
            if (!visited[next]) {
                dfs(next, adj, visited, finishOrder, states, layout, emit);
            }
        }

        states.put(u, "visited");
        finishOrder.push(u);
        emit.at("pushFinishOrder").say(
                        "%d has no more unvisited neighbours - push it onto the finish-order stack.", u)
                .var("node", u)
                .graph(layout.nodes(), layout.edges()).nodes(states).stack(finishOrder).step();
    }

    private void dfsTranspose(int u, List<List<Integer>> transpose, boolean[] visited, List<Integer> component,
                               Map<Integer, String> states, GraphLayout.Layout layout, StepEmitter emit) {
        visited[u] = true;
        component.add(u);
        states.put(u, "visited");
        emit.at("transposeDfsVisit").say(
                        "Visit %d on the transpose graph - part of the current component %s.", u, component)
                .var("node", u).var("component", String.valueOf(component))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        for (int next : transpose.get(u)) {
            if (!visited[next]) {
                dfsTranspose(next, transpose, visited, component, states, layout, emit);
            }
        }
    }
}
