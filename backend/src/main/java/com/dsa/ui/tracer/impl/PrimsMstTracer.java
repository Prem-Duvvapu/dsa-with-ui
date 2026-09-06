package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Prim's grows a single tree one cheapest frontier edge at a time: a min-heap always hands
 * back the lowest-weight edge leaving the current tree, and adding its far endpoint can only
 * shrink the frontier's minimum, never miss a cheaper option - the same greedy-exchange
 * argument that makes Dijkstra correct, but optimizing total tree weight instead of a
 * from-source distance.
 */
@Component
public class PrimsMstTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "prims-mst";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Weighted graph")
                        .help("Vertex count plus undirected, weighted edges [from, to, weight].")
                        .weighted()
                        .weights(0, 1_000_000)
                        .constraint("maxVertices", 16)
                        .constraint("maxEdges", 40)
                        .defaultValue(Map.of(
                                "vertices", 5,
                                "edges", List.of(
                                        List.of(0, 1, 2), List.of(0, 2, 1), List.of(1, 2, 1),
                                        List.of(1, 3, 2), List.of(2, 3, 4), List.of(3, 4, 2))))
                        .build());
    }

    /** A different shape and weights - a different MST weight and edge set wins. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 4,
                "edges", List.of(
                        List.of(0, 1, 10), List.of(0, 2, 6), List.of(0, 3, 5),
                        List.of(1, 3, 15), List.of(2, 3, 4))));
    }

    @Override
    public String annotatedCode() {
        return """
               public int primsMst(int v, List<List<int[]>> adj) {
                   // @a init
                   boolean[] inMst = new boolean[v];
                   PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
                   pq.add(new int[]{0, 0, -1});
                   int mstWeight = 0;

                   while (!pq.isEmpty()) {
                       // @a extract
                       int[] top = pq.poll();
                       int weight = top[0], node = top[1], parent = top[2];
                       if (inMst[node]) {
                           // @a stale
                           continue;
                       }
                       // @a add
                       inMst[node] = true;
                       mstWeight += weight;
                       for (int[] edge : adj.get(node)) {
                           int next = edge[0], w = edge[1];
                           if (!inMst[next]) {
                               // @a frontier
                               pq.add(new int[]{w, next, node});
                           }
                       }
                   }
                   // @a done
                   return mstWeight;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int v = graph.vertices();
        List<List<Inputs.GraphInput.Neighbor>> adj = graph.weightedAdjacency();
        boolean[] inMst = new boolean[v];
        int mstWeight = 0;
        List<String> mstEdges = new ArrayList<>();

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < v; i++) states.put(i, "unvisited");

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        pq.add(new int[]{0, 0, -1});

        emit.at("init").say("%d vertices. Start the tree at vertex 0 - enqueue it with entry cost 0.", v)
                .var("mstWeight", 0).graph(graph).nodes(states).queue(pqSnapshot(pq)).step();

        while (!pq.isEmpty()) {
            int[] top = pq.poll();
            int weight = top[0], node = top[1], parent = top[2];

            emit.at("extract").say("Pop the cheapest frontier entry: %d at weight %d.", node, weight)
                    .var("node", node).var("weight", weight).var("mstWeight", mstWeight)
                    .graph(graph).nodes(states).queue(pqSnapshot(pq)).step();

            if (inMst[node]) {
                emit.at("stale").say("%d is already in the tree - this is a stale, since-beaten entry. Discard.", node)
                        .var("node", node)
                        .graph(graph).nodes(states).queue(pqSnapshot(pq)).step();
                continue;
            }

            inMst[node] = true;
            mstWeight += weight;
            states.put(node, "visited");
            if (parent != -1) {
                mstEdges.add(parent + "-" + node);
            }
            emit.at("add").say(parent == -1
                            ? String.format("Add root vertex %d to the tree. Total weight so far: %d.", node, mstWeight)
                            : String.format("Add %d to the tree via edge %d-%d (weight %d). Total weight so far: %d.",
                                    node, parent, node, weight, mstWeight))
                    .var("mstWeight", mstWeight).var("treeEdges", mstEdges.toString())
                    .graph(graph).nodes(states)
                    .edges(parent == -1 ? List.of() : List.of(parent + "-" + node))
                    .queue(pqSnapshot(pq)).step();

            for (Inputs.GraphInput.Neighbor neighbour : adj.get(node)) {
                int next = neighbour.to();
                int w = neighbour.weight();
                if (!inMst[next]) {
                    pq.add(new int[]{w, next, node});
                    emit.at("frontier").say("%d -> %d (weight %d) extends the frontier - enqueue it.", node, next, w)
                            .var("node", node).var("neighbour", next).var("weight", w)
                            .graph(graph).nodes(states).edges(List.of(node + "-" + next))
                            .queue(pqSnapshot(pq)).step();
                }
            }
        }

        emit.at("done").say("Every vertex is in the tree. Minimum spanning tree weight: %d.", mstWeight)
                .var("mstWeight", mstWeight).var("treeEdges", mstEdges.toString())
                .graph(graph).nodes(states).step();
    }

    private static List<String> pqSnapshot(PriorityQueue<int[]> pq) {
        List<String> snapshot = new ArrayList<>(pq.size());
        for (int[] entry : pq) {
            snapshot.add(entry[1] + ":" + entry[0]);
        }
        return snapshot;
    }
}
