package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Runs BFS and DFS over the same graph so their frontier order can be compared directly. */
@Component
public class BfsDfsIntroTracer implements AlgorithmTracer {
    @Override public String id() { return "bfs-dfs-intro"; }
    @Override public DsType dsType() { return DsType.GRAPH; }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Undirected graph")
                        .help("Both traversals start from the same vertex and use adjacency order.")
                        .constraint("maxVertices", 16).constraint("maxEdges", 40)
                        .defaultValue(Map.of("vertices", 7, "edges", List.of(
                                List.of(0, 1), List.of(0, 2), List.of(1, 3),
                                List.of(1, 4), List.of(2, 5), List.of(4, 6))))
                        .build(),
                InputField.of("start", FieldType.INT).label("Start vertex")
                        .range(0, 15).defaultValue(0).build());
    }

    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "graph", Map.of("vertices", 6, "edges", List.of(
                        List.of(0, 1), List.of(0, 2), List.of(1, 4),
                        List.of(2, 3), List.of(3, 5), List.of(4, 5))),
                "start", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public void compareTraversals(List<List<Integer>> adj, int start) {
                   // @a bfsInit
                   Queue<Integer> queue = new LinkedList<>();
                   queue.add(start);
                   while (!queue.isEmpty()) {
                       // @a bfsVisit
                       int node = queue.poll();
                       for (int next : adj.get(node)) {
                           if (!bfsSeen[next]) {
                               // @a bfsAdd
                               queue.add(next);
                           }
                       }
                   }

                   // @a dfsInit
                   Deque<Integer> stack = new ArrayDeque<>();
                   stack.push(start);
                   while (!stack.isEmpty()) {
                       // @a dfsVisit
                       int node = stack.pop();
                       for (int next : reverse(adj.get(node))) {
                           if (!dfsSeen[next]) {
                               // @a dfsAdd
                               stack.push(next);
                           }
                       }
                   }
                   // @a done
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
        List<Integer> bfs = runBfs(graph, adj, start, emit);
        List<Integer> dfs = runDfs(graph, adj, start, emit);
        Map<Integer, String> done = states(graph.vertices(), "visited");
        emit.at("done").say("Same start, different frontier rule. BFS: %s. DFS: %s.", bfs, dfs)
                .var("bfsOrder", bfs).var("dfsOrder", dfs).graph(graph).nodes(done).step();
    }

    private List<Integer> runBfs(Inputs.GraphInput graph, List<List<Integer>> adj, int start,
                                 StepEmitter emit) {
        boolean[] seen = new boolean[graph.vertices()];
        Map<Integer, String> states = states(graph.vertices(), "unvisited");
        Deque<Integer> queue = new ArrayDeque<>();
        List<Integer> order = new ArrayList<>();
        queue.add(start);
        seen[start] = true;
        states.put(start, "queued");
        emit.at("bfsInit").say("BFS uses FIFO: enqueue start vertex %d.", start)
                .var("phase", "BFS").graph(graph).nodes(states).queue(queue).step();

        while (!queue.isEmpty()) {
            int node = queue.remove();
            order.add(node);
            states.put(node, "visiting");
            emit.at("bfsVisit").say("BFS dequeues %d, so shallower discoveries leave first.", node)
                    .var("phase", "BFS").var("order", order)
                    .graph(graph).nodes(states).queue(queue).step();
            for (int next : adj.get(node)) {
                if (!seen[next]) {
                    seen[next] = true;
                    queue.add(next);
                    states.put(next, "queued");
                    emit.at("bfsAdd").say("Discover %d from %d and enqueue it at the back.", next, node)
                            .var("phase", "BFS").graph(graph).nodes(states)
                            .edges(List.of(node + "-" + next)).queue(queue).step();
                }
            }
            states.put(node, "visited");
        }
        return order;
    }

    private List<Integer> runDfs(Inputs.GraphInput graph, List<List<Integer>> adj, int start,
                                 StepEmitter emit) {
        boolean[] seen = new boolean[graph.vertices()];
        Map<Integer, String> states = states(graph.vertices(), "unvisited");
        Deque<Integer> stack = new ArrayDeque<>();
        List<Integer> order = new ArrayList<>();
        stack.push(start);
        emit.at("dfsInit").say("Reset. DFS uses LIFO: push start vertex %d.", start)
                .var("phase", "DFS").graph(graph).nodes(states).stack(stack).step();

        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (seen[node]) continue;
            seen[node] = true;
            order.add(node);
            states.put(node, "visiting");
            emit.at("dfsVisit").say("DFS pops %d and immediately follows its deepest pending branch.", node)
                    .var("phase", "DFS").var("order", order)
                    .graph(graph).nodes(states).stack(stack).step();

            List<Integer> neighbours = adj.get(node);
            for (int i = neighbours.size() - 1; i >= 0; i--) {
                int next = neighbours.get(i);
                if (!seen[next]) {
                    stack.push(next);
                    states.put(next, "queued");
                    emit.at("dfsAdd").say("Push %d from %d. It is now above older branches.", next, node)
                            .var("phase", "DFS").graph(graph).nodes(states)
                            .edges(List.of(node + "-" + next)).stack(stack).step();
                }
            }
            states.put(node, "visited");
        }
        return order;
    }

    private static Map<Integer, String> states(int vertices, String value) {
        Map<Integer, String> out = new LinkedHashMap<>();
        for (int i = 0; i < vertices; i++) out.put(i, value);
        return out;
    }
}
