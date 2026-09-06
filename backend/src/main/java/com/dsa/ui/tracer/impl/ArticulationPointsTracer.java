package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * An articulation point (cut vertex) is one whose removal increases the number of connected
 * components. The same {@code disc}/{@code low} DFS that finds bridges finds them, with a
 * subtly different test: u is a cut vertex when some DFS child v satisfies
 * {@code low[v] >= disc[u]} - v's subtree can reach u at best, never anything discovered
 * before u, so deleting u strands it.
 *
 * <p>Note {@code >=} here against the bridge condition's {@code >}: a child that can climb
 * back exactly to u still depends on u, even though the edge u-v is then not a bridge.
 *
 * <p>The DFS root needs its own rule and is the classic trap. Every other vertex has a parent
 * edge proving it is reachable from elsewhere; the root has none, so {@code low[v] >= disc[u]}
 * holds trivially for its first child and would brand every root a cut vertex. The root is one
 * only when it has TWO OR MORE DFS children, because a second child means the first subtree
 * could not reach it any other way.
 */
@Component
public class ArticulationPointsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "articulation-points";
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
                                "vertices", 7,
                                "edges", List.of(
                                        List.of(0, 1), List.of(1, 2), List.of(2, 0),
                                        List.of(1, 3), List.of(1, 4), List.of(1, 6),
                                        List.of(3, 5), List.of(4, 5))))
                        .build());
    }

    /**
     * A star. Its centre is vertex 0, which the DFS starts from - the only input shape that
     * exercises the root rule, since here the answer comes from the root's child COUNT rather
     * than from any low/disc comparison.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 5,
                "edges", List.of(List.of(0, 1), List.of(0, 2), List.of(0, 3), List.of(0, 4))));
    }

    @Override
    public String annotatedCode() {
        return """
               public Set<Integer> articulationPoints(int v, List<List<Integer>> adj) {
                   // @a init
                   int[] disc = new int[v], low = new int[v];
                   Arrays.fill(disc, -1);
                   Set<Integer> cut = new TreeSet<>();
                   for (int i = 0; i < v; i++) {
                       if (disc[i] == -1) dfs(i, -1, adj, disc, low, cut);
                   }
                   // @a done
                   return cut;
               }

               private void dfs(int u, int parent, List<List<Integer>> adj,
                                 int[] disc, int[] low, Set<Integer> cut) {
                   // @a visit
                   disc[u] = low[u] = timer++;
                   int children = 0;
                   for (int w : adj.get(u)) {
                       if (w == parent) continue;
                       if (disc[w] == -1) {
                           children++;
                           // @a treeEdge
                           dfs(w, u, adj, disc, low, cut);
                           // @a childReturn
                           low[u] = Math.min(low[u], low[w]);
                           if (parent != -1 && low[w] >= disc[u]) {
                               // @a childAp
                               cut.add(u);
                           }
                       } else {
                           // @a backEdge
                           low[u] = Math.min(low[u], disc[w]);
                       }
                   }
                   if (parent == -1 && children > 1) {
                       // @a rootAp
                       cut.add(u);
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
        TreeSet<Integer> cut = new TreeSet<>();

        emit.at("init")
                .say("%d vertices, %d undirected edges. Same disc[]/low[] DFS as bridges, but the "
                                + "test is low[child] >= disc[u] - and the DFS root is judged by its child count.",
                        v, graph.edges().length)
                .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                .var("articulation points", "none")
                .graph(graph).nodes(states).step();

        int[] timer = {0};
        for (int i = 0; i < v; i++) {
            if (disc[i] == -1) {
                dfs(i, -1, adj, disc, low, timer, cut, states, graph, emit);
            }
        }

        String summary;
        if (cut.isEmpty()) {
            summary = "No vertex is an articulation point - every vertex has a route around it, so "
                    + "removing any one leaves the rest connected.";
        } else {
            summary = "Articulation points: " + cut + ". Removing any one of them breaks the graph "
                    + "into more components.";
        }
        emit.at("done")
                .say("DFS complete. %s", summary)
                .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                .var("articulation points", cut.isEmpty() ? "none" : cut.toString())
                .graph(graph).nodes(states).step();
    }

    private void dfs(int u, int parent, List<List<Integer>> adj, int[] disc, int[] low,
                      int[] timer, TreeSet<Integer> cut, Map<Integer, String> states,
                      Inputs.GraphInput graph, StepEmitter emit) {
        emit.push("dfs(" + u + ", parent=" + parent + ")");
        disc[u] = timer[0];
        low[u] = timer[0];
        timer[0]++;
        states.put(u, "visiting");
        int children = 0;

        emit.at("visit")
                .say("Discover %d%s: disc[%d] = low[%d] = %d.",
                        u, parent == -1 ? " (a DFS root)" : " from parent " + parent, u, u, disc[u])
                .var("u", u).var("parent", parent)
                .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                .var("articulation points", cut.isEmpty() ? "none" : cut.toString())
                .graph(graph).nodes(states).step();

        for (int w : adj.get(u)) {
            if (w == parent) {
                continue;
            }
            if (disc[w] == -1) {
                children++;
                emit.at("treeEdge")
                        .say("%d - %d is unexplored: DFS tree child number %d of %d. Descend into it.",
                                u, w, children, u)
                        .var("u", u).var("child", w).var("children", children)
                        .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                        .graph(graph).nodes(states).edges(List.of(u + "-" + w)).step();

                dfs(w, u, adj, disc, low, timer, cut, states, graph, emit);

                int before = low[u];
                low[u] = Math.min(low[u], low[w]);
                emit.at("childReturn")
                        .say("Back at %d from child %d: low[%d] = min(%d, low[%d] = %d) = %d.",
                                u, w, u, before, w, low[w], low[u])
                        .var("u", u).var("child", w)
                        .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                        .graph(graph).nodes(states).edges(List.of(u + "-" + w)).step();

                if (parent != -1 && low[w] >= disc[u]) {
                    boolean isNew = cut.add(u);
                    String tail = isNew
                            ? String.format("%d is an ARTICULATION POINT.", u)
                            : String.format("%d was already recorded as an articulation point.", u);
                    emit.at("childAp")
                            .say("low[%d] = %d >= disc[%d] = %d, so subtree %d cannot reach anything "
                                            + "discovered before %d. %s",
                                    w, low[w], u, disc[u], w, u, tail)
                            .var("u", u).var("child", w)
                            .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                            .var("articulation points", cut.toString())
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

        if (parent == -1 && children > 1) {
            cut.add(u);
            emit.at("rootAp")
                    .say("%d is a DFS root with %d children, and no edge joins those subtrees, so "
                                    + "%d is an ARTICULATION POINT.", u, children, u)
                    .var("u", u).var("children", children)
                    .var("disc[]", Arrays.toString(disc)).var("low[]", Arrays.toString(low))
                    .var("articulation points", cut.toString())
                    .graph(graph).nodes(states).step();
        }

        states.put(u, "visited");
        emit.pop();
    }
}
