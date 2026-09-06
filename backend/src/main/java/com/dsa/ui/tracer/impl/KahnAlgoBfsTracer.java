package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Kahn's algorithm builds a topological order breadth-first: a vertex is only safe to place
 * once every prerequisite of it (every edge pointing into it) has already been placed, and
 * indegree reaching zero is exactly that condition. Seeding the queue with every indegree-0
 * vertex and repeatedly draining it produces the order directly, with no recursion or
 * explicit stack unwind the way DFS-based topo sort needs.
 */
@Component
public class KahnAlgoBfsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "kahn-algo-bfs";
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
                                        List.of(5, 2), List.of(5, 0), List.of(4, 0),
                                        List.of(4, 1), List.of(2, 3), List.of(3, 1))))
                        .build());
    }

    /** A single linear chain - the topological order is forced, with no branching indegree ties. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 4,
                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 3))));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] topoSort(int v, List<List<Integer>> adj) {
                   // @a indegree
                   int[] indegree = new int[v];
                   for (int node = 0; node < v; node++) {
                       for (int next : adj.get(node)) indegree[next]++;
                   }

                   // @a seed
                   Queue<Integer> queue = new LinkedList<>();
                   for (int i = 0; i < v; i++) {
                       if (indegree[i] == 0) queue.add(i);
                   }

                   int[] order = new int[v];
                   int idx = 0;
                   while (!queue.isEmpty()) {
                       // @a poll
                       int node = queue.poll();
                       order[idx++] = node;
                       for (int next : adj.get(node)) {
                           // @a decrement
                           indegree[next]--;
                           if (indegree[next] == 0) {
                               // @a enqueue
                               queue.add(next);
                           }
                       }
                   }
                   // @a done
                   return order;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency(true);
        int v = graph.vertices();
        int[] indegree = new int[v];
        for (int node = 0; node < v; node++) {
            for (int next : adj.get(node)) indegree[next]++;
        }

        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) states.put(i, "unvisited");

        emit.at("indegree")
                .say("%d vertices, %d directed edges. Indegree of each: %s.",
                        v, graph.edges().length, Arrays.toString(indegree))
                .var("indegree", Arrays.toString(indegree))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < v; i++) {
            if (indegree[i] == 0) {
                queue.add(i);
                states.put(i, "queued");
            }
        }
        emit.at("seed")
                .say("Every vertex with indegree 0 has no unplaced prerequisite - seed the queue with %s.",
                        queue)
                .var("queue", queue.toString())
                .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();

        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int node = queue.poll();
            order.add(node);
            states.put(node, "visited");
            emit.at("poll")
                    .say("Dequeue %d and place it next in the order: %s.", node, order)
                    .var("node", node).var("order", order.toString())
                    .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();

            for (int next : adj.get(node)) {
                indegree[next]--;
                emit.at("decrement")
                        .say("%d -> %d: %d's remaining indegree drops to %d.", node, next, next, indegree[next])
                        .var("node", node).var("neighbour", next).var("indegreeLeft", indegree[next])
                        .graph(layout.nodes(), layout.edges()).nodes(states)
                        .edges(List.of(node + "-" + next)).queue(queue).step();

                if (indegree[next] == 0) {
                    queue.add(next);
                    states.put(next, "queued");
                    emit.at("enqueue")
                            .say("%d has no prerequisite left - enqueue it.", next)
                            .var("enqueued", next)
                            .graph(layout.nodes(), layout.edges()).nodes(states).queue(queue).step();
                }
            }
        }

        emit.at("done")
                .say("Topological order: %s.", order)
                .var("order", order.toString())
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }
}
