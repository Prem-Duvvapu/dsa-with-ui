package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Cycle detection in an undirected graph by recursive DFS that does not stop at "yes" -
 * it threads a {@code parent[]} chain down the recursion so that when a back edge is found
 * it can climb that chain and name the cycle's vertices.
 *
 * <p>{@link UndirectedCycleDfsTracer} answers the same question with the same parent-skip
 * test and returns a boolean. The two used to be byte-identical copies down to the
 * narration. They are kept apart deliberately: knowing a cycle exists and being able to
 * point at it are different skills, and the climb is what turns the parent pointer from
 * bookkeeping ("is this vertex my caller?") into a data structure you can walk. The default
 * input here therefore contains a cycle with tails on both ends, so the climb has to skip
 * vertices that are visited but not on the cycle.
 */
@Component
public class CycleUndirectedDfsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "cycle-undirected-dfs";
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
                        // A triangle 1-2-3 with a tail at each end: 0 leads in, 4-5 lead out.
                        // The climb must stop at 1 rather than running back to 0, which is the
                        // part a bare "a cycle exists" answer never has to get right.
                        .defaultValue(Map.of(
                                "vertices", 6,
                                "edges", List.of(
                                        List.of(0, 1), List.of(1, 2), List.of(2, 3),
                                        List.of(3, 1), List.of(3, 4), List.of(4, 5))))
                        .build());
    }

    /** A pure tree - every edge is a tree edge, so the search ends with nothing to report. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 5,
                "edges", List.of(List.of(0, 1), List.of(0, 2), List.of(2, 3), List.of(2, 4))));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<Integer> findCycle(int v, List<List<Integer>> adj) {
                   // @a init
                   boolean[] vis = new boolean[v];
                   int[] parent = new int[v];
                   Arrays.fill(parent, -1);
                   for (int i = 0; i < v; i++) {
                       if (!vis[i]) {
                           // @a component
                           List<Integer> cycle = dfs(i, -1, adj, vis, parent);
                           if (!cycle.isEmpty()) {
                               return cycle;
                           }
                       }
                   }
                   // @a noCycle
                   return List.of();
               }

               private List<Integer> dfs(int node, int par, List<List<Integer>> adj,
                                         boolean[] vis, int[] parent) {
                   // @a visit
                   vis[node] = true;
                   parent[node] = par;
                   for (int next : adj.get(node)) {
                       if (!vis[next]) {
                           // @a recurse
                           List<Integer> found = dfs(next, node, adj, vis, parent);
                           if (!found.isEmpty()) {
                               return found;
                           }
                       } else if (next != par) {
                           // @a backEdge
                           return climb(node, next, parent);
                       }
                   }
                   // @a deadEnd
                   return List.of();
               }

               private List<Integer> climb(int from, int to, int[] parent) {
                   List<Integer> cycle = new ArrayList<>();
                   for (int at = from; at != to; at = parent[at]) {
                       // @a climbStep
                       cycle.add(at);
                   }
                   cycle.add(to);
                   Collections.reverse(cycle);
                   return cycle;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency();
        int v = graph.vertices();
        boolean[] vis = new boolean[v];
        int[] parent = new int[v];
        Arrays.fill(parent, -1);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) {
            states.put(i, "unvisited");
        }

        emit.at("init")
                .say("%d vertices, %d edges. DFS each component, recording every vertex's parent so"
                        + " a cycle can be reconstructed rather than merely reported.",
                        v, graph.edges().length)
                .var("parent[]", Arrays.toString(parent))
                .graph(graph).nodes(states).step();

        List<Integer> cycle = List.of();
        for (int i = 0; i < v && cycle.isEmpty(); i++) {
            if (vis[i]) {
                continue;
            }
            emit.at("component").say("%d is unvisited, so it begins a component. dfs(%d, parent=-1).", i, i)
                    .var("root", i).graph(graph).nodes(states).step();

            cycle = dfs(i, -1, adj, vis, parent, states, graph, emit);
        }

        if (cycle.isEmpty()) {
            emit.at("noCycle")
                    .say("Every edge led either to an unvisited vertex or straight back to the caller."
                            + " No back edge, so there is nothing to reconstruct: this graph is a forest.")
                    .var("parent[]", Arrays.toString(parent))
                    .graph(graph).nodes(states).step();
        } else {
            emit.at("climbStep")
                    .say("The cycle is %s, closed by the back edge %d - %d. %d of the %d visited"
                            + " vertices are on it; the rest are tails hanging off it.",
                            cycle, cycle.get(cycle.size() - 1), cycle.get(0),
                            cycle.size(), countVisited(vis))
                    .var("cycle", cycle.toString()).var("length", cycle.size())
                    .var("parent[]", Arrays.toString(parent))
                    .graph(graph).nodes(states).step();
        }
    }

    private List<Integer> dfs(int node, int par, List<List<Integer>> adj, boolean[] vis, int[] parent,
                              Map<Integer, String> states, Inputs.GraphInput graph, StepEmitter emit) {
        emit.push("dfs(" + node + ", parent=" + par + ")");
        vis[node] = true;
        parent[node] = par;
        states.put(node, "visiting");
        emit.at("visit")
                .say("Enter %d from %s. parent[%d] = %d is the thread the climb will follow back.",
                        node, par < 0 ? "the outer loop" : String.valueOf(par), node, par)
                .var("node", node).var("parent", par).var("parent[]", Arrays.toString(parent))
                .graph(graph).nodes(states).step();

        for (int next : adj.get(node)) {
            if (!vis[next]) {
                emit.at("recurse")
                        .say("%d - %d leads somewhere unvisited: a tree edge. Descend with parent=%d.",
                                node, next, node)
                        .var("node", node).var("neighbour", next)
                        .graph(graph).nodes(states).edges(List.of(node + "-" + next)).step();
                List<Integer> found = dfs(next, node, adj, vis, parent, states, graph, emit);
                if (!found.isEmpty()) {
                    emit.pop();
                    return found;
                }
            } else if (next != par) {
                emit.at("backEdge")
                        .say("%d - %d reaches a visited vertex that is not %d's parent (%d). That is a"
                                + " back edge, so %d is an ancestor and the cycle runs from %d down to %d.",
                                node, next, node, par, next, next, node)
                        .var("node", node).var("neighbour", next).var("parent[]", Arrays.toString(parent))
                        .graph(graph).nodes(states).edges(List.of(node + "-" + next)).step();
                List<Integer> found = climb(node, next, parent, states, graph, emit);
                emit.pop();
                return found;
            }
        }

        states.put(node, "visited");
        emit.at("deadEnd")
                .say("%d has no edges left except back to %s. Nothing found here; return empty and unwind.",
                        node, par < 0 ? "nowhere" : String.valueOf(par))
                .var("node", node).graph(graph).nodes(states).step();
        emit.pop();
        return List.of();
    }

    /** Walks the parent chain from the back edge's tail up to its head, naming the cycle. */
    private List<Integer> climb(int from, int to, int[] parent, Map<Integer, String> states,
                                Inputs.GraphInput graph, StepEmitter emit) {
        List<Integer> cycle = new ArrayList<>();
        for (int at = from; at != to; at = parent[at]) {
            cycle.add(at);
            states.put(at, "cycle");
            emit.at("climbStep")
                    .say("Climb: %d is on the cycle. parent[%d] = %d, so step there next%s.",
                            at, at, parent[at],
                            parent[at] == to ? " - which is " + to + ", where the climb stops" : "")
                    .var("at", at).var("collected", new ArrayList<>(cycle).toString())
                    .graph(graph).nodes(states).edges(List.of(at + "-" + parent[at])).step();
        }
        cycle.add(to);
        states.put(to, "cycle");
        Collections.reverse(cycle);
        return cycle;
    }

    private static int countVisited(boolean[] vis) {
        int n = 0;
        for (boolean b : vis) {
            if (b) {
                n++;
            }
        }
        return n;
    }
}
