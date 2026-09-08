package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Grows a minimum spanning forest with Prim's cut rule, including disconnected inputs. */
@Component
public class MstTheoryTracer implements AlgorithmTracer {
    @Override public String id() { return "mst-theory"; }
    @Override public DsType dsType() { return DsType.GRAPH; }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("graph", FieldType.GRAPH)
                .label("Weighted graph")
                .help("At each cut, choose the cheapest edge joining the current tree to a new vertex.")
                .weighted().weights(0, 1_000_000)
                .constraint("maxVertices", 16).constraint("maxEdges", 40)
                .defaultValue(Map.of("vertices", 6, "edges", List.of(
                        List.of(0, 1, 4), List.of(0, 2, 3), List.of(1, 2, 1),
                        List.of(1, 3, 2), List.of(2, 3, 4), List.of(3, 4, 2),
                        List.of(4, 5, 6), List.of(3, 5, 3))))
                .build());
    }

    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of("vertices", 7, "edges", List.of(
                List.of(0, 1, 2), List.of(1, 2, 5), List.of(0, 2, 4),
                List.of(3, 4, 1), List.of(4, 5, 2), List.of(3, 5, 6))));
    }

    @Override
    public String annotatedCode() {
        return """
               public int minimumSpanningForest(int n, List<List<int[]>> adj) {
                   // @a init
                   boolean[] included = new boolean[n];
                   int total = 0;
                   for (int root = 0; root < n; root++) {
                       if (included[root]) continue;
                       // @a root
                       PriorityQueue<int[]> cut = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));
                       cut.add(new int[]{0, root, -1});
                       while (!cut.isEmpty()) {
                           // @a choose
                           int[] edge = cut.poll();
                           if (included[edge[1]]) {
                               // @a reject
                               continue;
                           }
                           // @a accept
                           included[edge[1]] = true;
                           total += edge[0];
                           for (int[] next : adj.get(edge[1])) {
                               if (!included[next[0]]) {
                                   // @a crossCut
                                   cut.add(new int[]{next[1], next[0], edge[1]});
                               }
                           }
                       }
                   }
                   // @a done
                   return total;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        List<List<Inputs.GraphInput.Neighbor>> adj = graph.weightedAdjacency();
        boolean[] included = new boolean[graph.vertices()];
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < graph.vertices(); i++) states.put(i, "unvisited");
        int total = 0, trees = 0, accepted = 0;

        emit.at("init").say("Start with an empty forest. The cut separates included vertices from the rest.")
                .var("weight", total).graph(graph).nodes(states).step();

        for (int root = 0; root < graph.vertices(); root++) {
            if (included[root]) continue;
            trees++;
            PriorityQueue<int[]> cut = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));
            cut.add(new int[]{0, root, -1});
            emit.at("root").say("Vertex %d is outside every existing tree. Start tree #%d there.", root, trees)
                    .var("trees", trees).var("weight", total)
                    .graph(graph).nodes(states).queue(snapshot(cut)).step();

            while (!cut.isEmpty()) {
                int[] edge = cut.poll();
                int weight = edge[0], node = edge[1], parent = edge[2];
                emit.at("choose").say(parent < 0
                                ? String.format("Take root %d at zero cost.", node)
                                : String.format("Cheapest edge across the cut is %d-%d (weight %d).", parent, node, weight))
                        .var("weight", total).graph(graph).nodes(states).queue(snapshot(cut))
                        .edges(parent < 0 ? List.of() : List.of(parent + "-" + node)).step();
                if (included[node]) {
                    emit.at("reject").say("Vertex %d already crossed the cut; this queued edge is stale.", node)
                            .var("weight", total).graph(graph).nodes(states).queue(snapshot(cut)).step();
                    continue;
                }

                included[node] = true;
                states.put(node, "visited");
                total += weight;
                if (parent >= 0) accepted++;
                emit.at("accept").say(parent < 0
                                ? String.format("Include root %d. The tree now has its first vertex.", node)
                                : String.format("Accept %d-%d. The forest weight becomes %d.", parent, node, total))
                        .var("acceptedEdges", accepted).var("weight", total)
                        .graph(graph).nodes(states)
                        .edges(parent < 0 ? List.of() : List.of(parent + "-" + node)).queue(snapshot(cut)).step();

                for (Inputs.GraphInput.Neighbor next : adj.get(node)) {
                    if (!included[next.to()]) {
                        cut.add(new int[]{next.weight(), next.to(), node});
                        emit.at("crossCut").say("%d-%d (weight %d) now crosses the cut; add it to the frontier.",
                                        node, next.to(), next.weight())
                                .var("weight", total).graph(graph).nodes(states)
                                .edges(List.of(node + "-" + next.to())).queue(snapshot(cut)).step();
                    }
                }
            }
        }

        emit.at("done").say("Minimum spanning forest complete: %d tree(s), %d accepted edge(s), weight %d.",
                        trees, accepted, total)
                .var("trees", trees).var("acceptedEdges", accepted).var("weight", total)
                .graph(graph).nodes(states).step();
    }

    private static List<String> snapshot(PriorityQueue<int[]> pq) {
        List<int[]> entries = new ArrayList<>(pq);
        entries.sort(Comparator.comparingInt(e -> e[0]));
        return entries.stream().map(e -> e[2] < 0
                ? "root:" + e[1]
                : e[2] + "-" + e[1] + ":" + e[0]).toList();
    }
}
