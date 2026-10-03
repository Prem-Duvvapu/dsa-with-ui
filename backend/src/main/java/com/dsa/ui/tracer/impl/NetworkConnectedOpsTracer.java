package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Number of Operations to Make Network Connected (LeetCode 1319), traced on the owner's own
 * accepted submission over their DisjointSet: a cable between two computers already in one
 * component is spare; every other cable joins two components. Connecting k components needs k - 1
 * cables, so the answer is k - 1 if there are that many spares, else -1. O(E * alpha).
 *
 * <p>The submission called the find method {@code getParent}; it is the same method their other DSU
 * solutions call {@code getUltimateParent}, which is the name the shared class uses.
 */
@Component
public class NetworkConnectedOpsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "network-connected-ops";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("graph", FieldType.GRAPH)
                        .label("Computers and cables")
                        .help("Vertex count is the number of computers; edges are existing direct cables.")
                        .constraint("maxVertices", 20)
                        .constraint("maxEdges", 48)
                        .defaultValue(Map.of(
                                "vertices", 4,
                                "edges", List.of(List.of(0, 1), List.of(0, 2), List.of(1, 2))))
                        .build());
    }

    /** Too few cables to ever connect every computer, however they are rearranged. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("graph", Map.of(
                "vertices", 6,
                "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 3))));
    }

    @Override
    public String annotatedCode() {
        return OwnerDisjointSet.code(java.util.Set.of()) + "\n\n" + """
               // Your submission named the find method getParent; it is the same method as
               // getUltimateParent in your other DSU solutions, which is what the class above calls it.
               class Solution {
                   public int makeConnected(int n, int[][] connections) {
                       // @a init
                       DisjointSet ds = new DisjointSet(n);
                       int extraCables = 0;
                       int numOfComponents = 0;

                       for (int[] connection : connections) {
                           int src = connection[0];
                           int dest = connection[1];

                           if (ds.getUltimateParent(src) == ds.getUltimateParent(dest))
                               // @a extra
                               extraCables++;
                           else
                               // @a join
                               ds.unionBySize(src, dest);
                       }

                       for (int i=0;i<n;i++)
                           if (ds.parent[i] == i)
                               // @a component
                               numOfComponents++;

                       // @a done
                       int requiredCables = numOfComponents-1;

                       return (extraCables >= requiredCables) ? requiredCables : -1;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("graph");
        int n = graph.vertices();
        OwnerDisjointSet ds = new OwnerDisjointSet(n);
        int extraCables = 0;
        int numOfComponents = 0;
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }

        emit.at("init").say("%d computers, each its own component so far, and %d cable%s to look at.",
                        n, graph.edges().length, Narration.s(graph.edges().length))
                .var("extraCables", 0).var("parent[]", ds.parents()).var("size[]", ds.sizes())
                .graph(graph).nodes(states).step();

        for (int[] connection : graph.edges()) {
            int src = connection[0];
            int dest = connection[1];
            List<String> compressed = new ArrayList<>();
            boolean same = ds.find(src, compressed) == ds.find(dest, compressed);
            states.put(src, "visited");
            states.put(dest, "visited");
            if (same) {
                extraCables++;
                emit.at("extra").say("Cable %d-%d joins two computers already in one component, so it is spare: "
                                + "extraCables = %d.", src, dest, extraCables)
                        .var("extraCables", extraCables).var("parent[]", ds.parents()).var("size[]", ds.sizes())
                        .graph(graph).nodes(states).edges(List.of(src + "-" + dest)).step();
            } else {
                OwnerDisjointSet.Union result = ds.union(src, dest);
                emit.at("join").say("Cable %d-%d joins two separate components. %s", src, dest, result.narrate(src, dest))
                        .var("extraCables", extraCables).var("parent[]", ds.parents()).var("size[]", ds.sizes())
                        .graph(graph).nodes(states).edges(List.of(src + "-" + dest)).step();
            }
        }

        for (int i = 0; i < n; i++) {
            if (ds.parent[i] == i) {
                numOfComponents++;
                states.put(i, "done");
                emit.at("component").say("Computer %d is its own parent: the root of component %d.", i, numOfComponents)
                        .var("numOfComponents", numOfComponents).var("parent[]", ds.parents())
                        .graph(graph).nodes(states).step();
            }
        }

        int requiredCables = numOfComponents - 1;
        int answer = extraCables >= requiredCables ? requiredCables : -1;
        emit.at("done").say(answer == -1
                        ? numOfComponents + " components need " + requiredCables + " cables to connect, but only "
                                + extraCables + " are spare. Return -1."
                        : numOfComponents + " component" + Narration.s(numOfComponents) + " need" + (numOfComponents == 1 ? "s" : "")
                                + " " + requiredCables + " cable" + Narration.s(requiredCables) + " to connect, and there "
                                + Narration.is(extraCables) + " " + extraCables + " spare. Return " + requiredCables + ".")
                .var("numOfComponents", numOfComponents).var("extraCables", extraCables).var("answer", answer)
                .graph(graph).nodes(states).step();
    }
}
