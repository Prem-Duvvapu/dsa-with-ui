package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Cycle detection in a DIRECTED graph by DFS, told as CLRS edge classification: every vertex
 * is WHITE (undiscovered), GRAY (discovered, still on the call path) or BLACK (finished), and
 * a directed graph has a cycle exactly when DFS finds a <em>back edge</em> - an edge into a
 * GRAY vertex.
 *
 * <p>{@link DirectedCycleDfsTracer} solves the same problem with the two-boolean
 * {@code vis[]}/{@code pathVis[]} formulation. The two used to be byte-identical copies down
 * to the narration. They are kept apart deliberately because they are the two standard ways
 * to teach this: the colours name <em>why</em> a second marker is needed (GRAY is precisely
 * "on the current path"), and they generalise to the edge classification that topological
 * sort and strongly-connected-components build on. Same algorithm, different mental model.
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

    /**
     * A DAG that also contains a cross edge, which is the case this formulation exists to
     * explain: DFS from 0 finishes 3 (BLACK) down the 0-1-3 branch, then reaches 3 again via
     * 2. An already-visited vertex that is BLACK rather than GRAY is <em>not</em> a cycle, and
     * a run that never meets one leaves the whole point unsaid.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 5,
                "edges", List.of(List.of(0, 1), List.of(0, 2), List.of(1, 3),
                        List.of(2, 3), List.of(3, 4))));
    }

    @Override
    public String annotatedCode() {
        return """
               static final int WHITE = 0, GRAY = 1, BLACK = 2;

               public boolean isCyclic(int v, List<List<Integer>> adj) {
                   // @a init
                   int[] colour = new int[v];          // all WHITE
                   for (int i = 0; i < v; i++) {
                       if (colour[i] == WHITE) {
                           // @a component
                           if (dfsVisit(i, adj, colour)) {
                               return true;
                           }
                       }
                   }
                   // @a noCycle
                   return false;
               }

               private boolean dfsVisit(int node, List<List<Integer>> adj, int[] colour) {
                   // @a discover
                   colour[node] = GRAY;                // discovered, still on the call path
                   for (int next : adj.get(node)) {
                       if (colour[next] == WHITE) {
                           // @a treeEdge
                           if (dfsVisit(next, adj, colour)) {
                               return true;
                           }
                       } else if (colour[next] == GRAY) {
                           // @a backEdge
                           return true;               // edge into the current path = cycle
                       }
                       // @a forwardOrCrossEdge
                   }
                   // @a finish
                   colour[node] = BLACK;               // finished, off the call path
                   return false;
               }""";
    }

    private static final int WHITE = 0, GRAY = 1, BLACK = 2;
    private static final String[] COLOUR_NAME = {"WHITE", "GRAY", "BLACK"};

    /** The canvas vocabulary each colour maps onto. */
    private static String stateFor(int colour) {
        return switch (colour) {
            case GRAY -> "visiting";
            case BLACK -> "visited";
            default -> "unvisited";
        };
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Integer>> adj = graph.adjacency(true);
        int v = graph.vertices();
        int[] colour = new int[v];
        GraphLayout.Layout layout = GraphLayout.directed(graph);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) {
            states.put(i, "unvisited");
        }

        emit.at("init")
                .say("%d vertices, %d directed edges. Every vertex starts WHITE. DFS will colour it"
                        + " GRAY on discovery and BLACK when it finishes; a cycle is an edge into GRAY.",
                        v, graph.edges().length)
                .var("colour[]", colours(colour))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        boolean cycle = false;
        for (int i = 0; i < v && !cycle; i++) {
            if (colour[i] != WHITE) {
                continue;
            }
            emit.at("component")
                    .say("%d is still WHITE, so it begins a fresh DFS tree.", i)
                    .var("root", i).var("colour[]", colours(colour))
                    .graph(layout.nodes(), layout.edges()).nodes(states).step();

            cycle = dfsVisit(i, adj, colour, states, layout, emit);
        }

        if (!cycle) {
            emit.at("noCycle")
                    .say("Every vertex reached BLACK without one edge ever pointing at a GRAY vertex."
                            + " No back edge means no directed cycle - this graph is a DAG.")
                    .var("colour[]", colours(colour))
                    .graph(layout.nodes(), layout.edges()).nodes(states).step();
        } else {
            emit.at("backEdge")
                    .say("A back edge was found, so the answer is settled and the remaining vertices"
                            + " never need colouring. The graph is cyclic.")
                    .var("colour[]", colours(colour))
                    .graph(layout.nodes(), layout.edges()).nodes(states).step();
        }
    }

    private boolean dfsVisit(int node, List<List<Integer>> adj, int[] colour,
                             Map<Integer, String> states, GraphLayout.Layout layout, StepEmitter emit) {
        emit.push("dfsVisit(" + node + ")");
        colour[node] = GRAY;
        states.put(node, stateFor(GRAY));
        emit.at("discover")
                .say("Discover %d: WHITE -> GRAY. GRAY means \"on the path I am standing on right now\".",
                        node)
                .var("node", node).var("colour[]", colours(colour))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();

        for (int next : adj.get(node)) {
            String edge = node + "-" + next;
            if (colour[next] == WHITE) {
                emit.at("treeEdge")
                        .say("%d -> %d reaches a WHITE vertex, so it is a TREE edge. Descend.", node, next)
                        .var("edge", node + " -> " + next).var("classified", "tree edge")
                        .var("colour[]", colours(colour))
                        .graph(layout.nodes(), layout.edges()).nodes(states).edges(List.of(edge)).step();
                if (dfsVisit(next, adj, colour, states, layout, emit)) {
                    emit.pop();
                    return true;
                }
            } else if (colour[next] == GRAY) {
                states.put(next, "cycle");
                emit.at("backEdge")
                        .say("%d -> %d reaches a GRAY vertex: %d is an ancestor still on this path, so"
                                + " this is a BACK edge - and a back edge is a cycle.", node, next, next)
                        .var("edge", node + " -> " + next).var("classified", "BACK edge")
                        .var("colour[]", colours(colour))
                        .graph(layout.nodes(), layout.edges()).nodes(states).edges(List.of(edge)).step();
                emit.pop();
                return true;
            } else {
                emit.at("forwardOrCrossEdge")
                        .say("%d -> %d reaches a BLACK vertex. %d already finished, so it is not on this"
                                + " path - a forward/cross edge, which proves nothing. Keep going.",
                                node, next, next)
                        .var("edge", node + " -> " + next).var("classified", "forward/cross edge")
                        .var("colour[]", colours(colour))
                        .graph(layout.nodes(), layout.edges()).nodes(states).edges(List.of(edge)).step();
            }
        }

        colour[node] = BLACK;
        states.put(node, stateFor(BLACK));
        emit.at("finish")
                .say("%d has no edges left. GRAY -> BLACK: it is finished and off the path, so a later"
                        + " edge into it can no longer mean a cycle.", node)
                .var("node", node).var("colour[]", colours(colour))
                .graph(layout.nodes(), layout.edges()).nodes(states).step();
        emit.pop();
        return false;
    }

    private static String colours(int[] colour) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < colour.length; i++) {
            sb.append(i > 0 ? ", " : "").append(COLOUR_NAME[colour[i]]);
        }
        return sb.append(']').toString();
    }
}
