package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.GraphEdge;
import com.dsa.ui.model.GraphNode;
import com.dsa.ui.tracer.Inputs;

import java.util.ArrayList;
import java.util.List;

/**
 * Circular layout for a directed graph, shared by tracers whose {@code InputSpec} marks the
 * graph field {@code .directed()}.
 *
 * {@code StepEmitter.Step#graph(Inputs.GraphInput)} lays out the same circle but hardcodes
 * every {@code GraphEdge} as {@code directed = false}, so the frontend never draws an
 * arrowhead ({@code GraphCanvas} keys the marker off {@code edge.directed}). That overload
 * is correct for the undirected tracers that came before this one; a directed tracer needs
 * its own edges built with the flag set, passed through the emitter's
 * {@code graph(List<GraphNode>, List<GraphEdge>)} overload instead.
 */
final class GraphLayout {

    record Layout(List<GraphNode> nodes, List<GraphEdge> edges) {}

    private GraphLayout() {}

    static Layout directed(Inputs.GraphInput graph) {
        List<GraphNode> nodes = new ArrayList<>(graph.vertices());
        double centerX = 180;
        double centerY = 160;
        double radius = graph.vertices() <= 2 ? 80 : 120;
        for (int id = 0; id < graph.vertices(); id++) {
            double angle = -Math.PI / 2 + (2 * Math.PI * id / graph.vertices());
            double x = graph.vertices() == 1 ? centerX : centerX + radius * Math.cos(angle);
            double y = graph.vertices() == 1 ? centerY : centerY + radius * Math.sin(angle);
            nodes.add(new GraphNode(id, String.valueOf(id), x, y, "unvisited"));
        }

        List<GraphEdge> edges = new ArrayList<>(graph.edges().length);
        for (int[] edge : graph.edges()) {
            Integer weight = edge.length == 3 ? edge[2] : null;
            edges.add(new GraphEdge(edge[0], edge[1], weight, true, false));
        }
        return new Layout(nodes, edges);
    }
}
