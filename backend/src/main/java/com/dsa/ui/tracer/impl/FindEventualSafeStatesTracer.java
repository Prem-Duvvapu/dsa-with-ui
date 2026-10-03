package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Find Eventual Safe States (LeetCode 802), traced on the owner's own accepted submission. A
 * node is safe when every path from it ends at a terminal node. Reverse every edge; then a
 * node's {@code indegree} in the reversed graph is its number of outgoing edges in the original,
 * and Kahn's algorithm peels nodes from the terminals inward. A node on, or leading into, a
 * cycle always keeps an edge left, so it is never queued. Sort the result. O(V + E + V log V).
 *
 * <p>Input: the app's edge [u, v] is "u -> v", the same as v appearing in LeetCode's graph[u].
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
               class Solution {
                   public List<Integer> eventualSafeNodes(int[][] graph) {
                       // @a init
                       int n = graph.length;
                       List<Integer> res = new ArrayList<>();
                       List<List<Integer>> revAdjList = new ArrayList<>();
                       int[] indegree = new int[n];
                       Queue<Integer> q = new LinkedList<>();

                       for (int i=0;i<n;i++)
                           revAdjList.add(new ArrayList<>());

                       for (int i=0;i<n;i++) {
                           for (int ngbr: graph[i]) {
                               revAdjList.get(ngbr).add(i);
                               indegree[i]++;
                           }
                       }

                       // @a seed
                       for (int i=0;i<n;i++)
                           if (indegree[i] == 0)
                               q.add(i);

                       while (!q.isEmpty()) {
                           // @a poll
                           int curr = q.poll();
                           res.add(curr);

                           for (int ngbr: revAdjList.get(curr))  {
                               // @a decrement
                               indegree[ngbr]--;

                               if (indegree[ngbr] == 0)
                                   // @a enqueue
                                   q.add(ngbr);
                           }
                       }

                       // @a done
                       Collections.sort(res);
                       return res;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput input = in.getGraph("graph");
        List<List<Integer>> graph = input.adjacency(true);
        int n = input.vertices();
        List<Integer> res = new ArrayList<>();
        List<List<Integer>> revAdjList = new ArrayList<>();
        int[] indegree = new int[n];
        Deque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            revAdjList.add(new ArrayList<>());
        }
        for (int i = 0; i < n; i++) {
            for (int ngbr : graph.get(i)) {
                revAdjList.get(ngbr).add(i);
                indegree[i]++;
            }
        }

        GraphLayout.Layout layout = GraphLayout.directed(input);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }
        emit.at("init").say("Reverse every edge. In the reversed graph, indegree[i] is the number of edges "
                        + "leaving i in the original: %s. A node with none is terminal, so it is safe.",
                        Arrays.toString(indegree))
                .var("indegree", Arrays.toString(indegree))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        for (int i = 0; i < n; i++) {
            if (indegree[i] == 0) {
                q.add(i);
                states.put(i, "queued");
            }
        }
        emit.at("seed").say(q.isEmpty()
                        ? "No node is terminal - every node has an edge out - so no node can be safe."
                        : "Terminal nodes, safe by definition: queue " + q + ".")
                .var("q", q.toString()).graph(layout.nodes(), layout.edges()).nodes(states).queue(q).step();

        while (!q.isEmpty()) {
            int curr = q.poll();
            res.add(curr);
            states.put(curr, "safe");
            emit.at("poll").say("Node %d is safe: add it to res, then lower the count of each node with an "
                            + "edge into %d.", curr, curr)
                    .var("curr", curr).var("res", res.toString())
                    .graph(layout.nodes(), layout.edges()).nodes(states).queue(q).step();

            for (int ngbr : revAdjList.get(curr)) {
                indegree[ngbr]--;
                emit.at("decrement").say("%d -> %d leads to a safe node. %d now has %d edge%s not yet known to "
                                + "be safe.", ngbr, curr, ngbr, indegree[ngbr], Narration.s(indegree[ngbr]))
                        .var("curr", curr).var("ngbr", ngbr).var("indegree", Arrays.toString(indegree))
                        .graph(layout.nodes(), layout.edges()).nodes(states)
                        .edges(List.of(ngbr + "-" + curr)).queue(q).step();
                if (indegree[ngbr] == 0) {
                    q.add(ngbr);
                    states.put(ngbr, "queued");
                    emit.at("enqueue").say("Every edge out of %d leads to a safe node, so %d is safe too - queue it.",
                                    ngbr, ngbr)
                            .var("ngbr", ngbr).var("q", q.toString())
                            .graph(layout.nodes(), layout.edges()).nodes(states).queue(q).step();
                }
            }
        }

        Collections.sort(res);
        for (int i = 0; i < n; i++) {
            if (!"safe".equals(states.get(i))) {
                states.put(i, "cycle");
            }
        }
        emit.at("done").say("The queue is empty. Outlined nodes are on a cycle or lead into one, so they "
                        + "were never queued. Sorted, the safe nodes are %s.", res)
                .var("res", res.toString())
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
    }
}
