package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * A node is "safe" if every path out of it eventually reaches a terminal node (no path
 * loops forever). Reversing every edge turns "every outgoing path is safe" into "every
 * predecessor becomes reachable once I'm confirmed safe" - exactly Kahn's BFS on the
 * reversed graph, seeded from the terminal nodes (outdegree 0) instead of indegree 0. A
 * node stuck in a cycle never has its reversed-indegree count reach zero, so it is never
 * dequeued and never marked safe.
 */
@Component
public class FindEventualSafeStatesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "find-eventual-safe-states";
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
                                "vertices", 7,
                                "edges", List.of(
                                        List.of(0, 1), List.of(0, 2), List.of(1, 2), List.of(1, 3),
                                        List.of(2, 5), List.of(3, 0), List.of(4, 5))))
                        .build());
    }

    /** A pure 3-cycle plus one disjoint safe pair - the cycle members can never be safe. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 5,
                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 0), List.of(3, 4))));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> eventualSafeNodes(int v, List<List<Integer>> adj) {
                   // @a reverse
                   List<List<Integer>> radj = new ArrayList<>();
                   int[] outdegree = new int[v];
                   for (int i = 0; i < v; i++) radj.add(new ArrayList<>());
                   for (int node = 0; node < v; node++) {
                       outdegree[node] = adj.get(node).size();
                       for (int next : adj.get(node)) radj.get(next).add(node);
                   }

                   // @a seed
                   Queue<Integer> queue = new LinkedList<>();
                   boolean[] safe = new boolean[v];
                   for (int i = 0; i < v; i++) {
                       if (outdegree[i] == 0) queue.add(i);
                   }

                   while (!queue.isEmpty()) {
                       // @a poll
                       int node = queue.poll();
                       safe[node] = true;
                       for (int pred : radj.get(node)) {
                           // @a decrement
                           outdegree[pred]--;
                           if (outdegree[pred] == 0) {
                               // @a enqueue
                               queue.add(pred);
                           }
                       }
                   }

                   // @a done
                   List<Integer> result = new ArrayList<>();
                   for (int i = 0; i < v; i++) if (safe[i]) result.add(i);
                   return result;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency(true);
        int v = graph.vertices();

        List<List<Integer>> radj = new ArrayList<>();
        int[] outdegree = new int[v];
        for (int i = 0; i < v; i++) radj.add(new ArrayList<>());
        for (int node = 0; node < v; node++) {
            outdegree[node] = adj.get(node).size();
            for (int next : adj.get(node)) radj.get(next).add(node);
        }

        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) states.put(i, "unvisited");

        emit.at("reverse")
                .say("%d vertices, %d edges. Reverse every edge; a node's outdegree becomes its reversed indegree: %s.",
                        v, graph.edges().length, Arrays.toString(outdegree))
                .var("outdegree", Arrays.toString(outdegree))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        Deque<Integer> queue = new ArrayDeque<>();
        boolean[] safe = new boolean[v];
        for (int i = 0; i < v; i++) {
            if (outdegree[i] == 0) {
                queue.add(i);
                states.put(i, "queued");
            }
        }
        emit.at("seed")
                .say("Terminal nodes (outdegree 0) have no way to loop - they are safe by definition. Seed the queue with %s.", queue)
                .var("queue", queue.toString())
                .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();

        while (!queue.isEmpty()) {
            int node = queue.poll();
            safe[node] = true;
            states.put(node, "visited");
            emit.at("poll")
                    .say("Dequeue %d and mark it safe - every path out of it reaches a terminal.", node)
                    .var("node", node)
                    .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();

            for (int pred : radj.get(node)) {
                outdegree[pred]--;
                emit.at("decrement")
                        .say("%d -> %d in the original graph: %d's remaining unsafe out-edges drops to %d.",
                                pred, node, pred, outdegree[pred])
                        .var("node", node).var("predecessor", pred).var("outdegreeLeft", outdegree[pred])
                        .graph(layout.nodes(), layout.edges()).nodes(states)
                        .edges(List.of(pred + "-" + node)).queue(queue).step();

                if (outdegree[pred] == 0) {
                    queue.add(pred);
                    states.put(pred, "queued");
                    emit.at("enqueue")
                            .say("Every out-edge of %d now leads to a safe node - enqueue it.", pred)
                            .var("enqueued", pred)
                            .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();
                }
            }
        }

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < v; i++) if (safe[i]) result.add(i);
        emit.at("done")
                .say("Safe nodes: %s.", result)
                .var("safeNodes", result.toString())
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }
}
