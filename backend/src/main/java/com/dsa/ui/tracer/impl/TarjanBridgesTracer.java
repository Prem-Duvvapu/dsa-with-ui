package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * A bridge is an edge whose removal disconnects the graph. Tarjan finds them all in one DFS
 * by timestamping each vertex as it is discovered ({@code disc}) and computing, for each
 * vertex, the OLDEST discovery time reachable from its subtree using at most one back-edge
 * ({@code low}). The tree edge u-v is then a bridge exactly when {@code low[v] > disc[u]}:
 * nothing in v's subtree can climb back to u or above it, so that edge is the subtree's only
 * way out.
 *
 * <p>The parent edge is skipped rather than treated as a back-edge - in an undirected graph
 * every tree edge appears twice in the adjacency list, and counting the return copy as a
 * back-edge would make low[v] collapse to disc[u] and hide every bridge.
 */
@Component
public class TarjanBridgesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "tarjan-bridges";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Undirected graph")
                        .help("Vertex count plus undirected edges [u, v].")
                        .constraint("maxVertices", 12)
                        .constraint("maxEdges", 24)
                        .defaultValue(Map.of(
                                "vertices", 5,
                                "edges", List.of(
                                        List.of(1, 0), List.of(0, 2), List.of(2, 1),
                                        List.of(0, 3), List.of(3, 4))))
                        .build());
    }

    /** A single cycle: every edge has an alternative route around, so nothing is a bridge. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 4,
                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 3), List.of(3, 0))));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<int[]> bridges(int v, List<List<Integer>> adj) {
                   // @a init
                   int[] disc = new int[v], low = new int[v];
                   Arrays.fill(disc, -1);
                   List<int[]> found = new ArrayList<>();
                   for (int i = 0; i < v; i++) {
                       if (disc[i] == -1) dfs(i, -1, adj, disc, low, found);
                   }
                   // @a done
                   return found;
               }

               private void dfs(int u, int parent, List<List<Integer>> adj,
                                 int[] disc, int[] low, List<int[]> found) {
                   // @a visit
                   disc[u] = low[u] = timer++;
                   for (int w : adj.get(u)) {
                       if (w == parent) continue;
                       if (disc[w] == -1) {
                           // @a treeEdge
                           dfs(w, u, adj, disc, low, found);
                           // @a childReturn
                           low[u] = Math.min(low[u], low[w]);
                           if (low[w] > disc[u]) {
                               // @a bridge
                               found.add(new int[]{u, w});
                           }
                       } else {
                           // @a backEdge
                           low[u] = Math.min(low[u], disc[w]);
                       }
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency();
        int v = graph.vertices();
        int[] disc = new int[v];
        int[] low = new int[v];
        Arrays.fill(disc, -1);
        Arrays.fill(low, -1);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) {
            states.put(i, "unvisited");
        }
        List<String> bridges = new ArrayList<>();

        emit.at("init")
                .say("%d vertices, %d undirected edges. disc[u] is when the DFS first sees u; "
                                + "low[u] is the oldest disc reachable from u's subtree via one back-edge.",
                        v, graph.edges().length)
                .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                .var("bridges", bridges.toString())
                .graph(graph).nodes(states).step();

        int[] timer = {0};
        for (int i = 0; i < v; i++) {
            if (disc[i] == -1) {
                dfs(i, -1, adj, disc, low, timer, bridges, states, graph, emit);
            }
        }

        String summary;
        if (bridges.isEmpty()) {
            summary = "No edge is a bridge - every edge sits on a cycle, so removing any one of "
                    + "them leaves the graph connected.";
        } else {
            summary = "Bridges: " + String.join(", ", bridges)
                    + ". Removing any one of them splits the graph.";
        }
        emit.at("done")
                .say("DFS complete. %s", summary)
                .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                .var("bridges", bridges.isEmpty() ? "none" : String.join(", ", bridges))
                .graph(graph).nodes(states).step();
    }

    private void dfs(int u, int parent, List<List<Integer>> adj, int[] disc, int[] low,
                      int[] timer, List<String> bridges, Map<Integer, String> states,
                      Inputs.GraphInput graph, StepEmitter emit) {
        emit.push("dfs(" + u + ", parent=" + parent + ")");
        disc[u] = timer[0];
        low[u] = timer[0];
        timer[0]++;
        states.put(u, "visiting");

        emit.at("visit")
                .say("Discover %d as the %s vertex: disc[%d] = low[%d] = %d.",
                        u, ordinal(disc[u] + 1), u, u, disc[u])
                .var("u", u).var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                .var("bridges", bridges.isEmpty() ? "none" : String.join(", ", bridges))
                .graph(graph).nodes(states).step();

        for (int w : adj.get(u)) {
            if (w == parent) {
                continue;
            }
            if (disc[w] == -1) {
                emit.at("treeEdge")
                        .say("%d - %d is unexplored, so it is a DFS tree edge. Descend into %d.", u, w, w)
                        .var("u", u).var("child", w)
                        .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                        .graph(graph).nodes(states).edges(List.of(u + "-" + w)).step();

                dfs(w, u, adj, disc, low, timer, bridges, states, graph, emit);

                int before = low[u];
                low[u] = Math.min(low[u], low[w]);
                emit.at("childReturn")
                        .say("Back at %d from child %d: low[%d] = min(%d, low[%d] = %d) = %d.",
                                u, w, u, before, w, low[w], low[u])
                        .var("u", u).var("child", w)
                        .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                        .graph(graph).nodes(states).edges(List.of(u + "-" + w)).step();

                if (low[w] > disc[u]) {
                    bridges.add(u + "-" + w);
                    emit.at("bridge")
                            .say("low[%d] = %d > disc[%d] = %d: nothing under %d can climb back to "
                                            + "%d or higher, so %d - %d is a BRIDGE.",
                                    w, low[w], u, disc[u], w, u, u, w)
                            .var("bridge", u + " - " + w)
                            .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                            .var("bridges", String.join(", ", bridges))
                            .graph(graph).nodes(states).edges(List.of(u + "-" + w)).step();
                }
            } else {
                int before = low[u];
                low[u] = Math.min(low[u], disc[w]);
                emit.at("backEdge")
                        .say("%d - %d is a back-edge to already-discovered %d: low[%d] = min(%d, "
                                        + "disc[%d] = %d) = %d.",
                                u, w, w, u, before, w, disc[w], low[u])
                        .var("u", u).var("back", w)
                        .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                        .graph(graph).nodes(states).edges(List.of(u + "-" + w)).step();
            }
        }

        states.put(u, "visited");
        emit.pop();
    }

    private static String ordinal(int n) {
        return switch (n % 100) {
            case 11, 12, 13 -> n + "th";
            default -> switch (n % 10) {
                case 1 -> n + "st";
                case 2 -> n + "nd";
                case 3 -> n + "rd";
                default -> n + "th";
            };
        };
    }
}
