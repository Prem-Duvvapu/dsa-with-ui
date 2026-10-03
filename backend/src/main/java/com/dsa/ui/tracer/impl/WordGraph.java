package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.GraphEdge;
import com.dsa.ui.model.GraphNode;
import com.dsa.ui.tracer.Inputs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The picture both Word Ladder tracers draw: beginWord and every listed word as a node, joined
 * when the two words differ in exactly one letter. The code never builds this graph - it tries
 * all 26 letters at every position - but the graph is exactly the set of moves those tries can
 * find, so it shows where the search can go.
 */
final class WordGraph {

    final Map<String, Integer> id = new LinkedHashMap<>();
    final String[] words;
    final List<GraphNode> nodes = new ArrayList<>();
    final List<GraphEdge> edges = new ArrayList<>();
    final Map<Integer, String> states = new LinkedHashMap<>();

    WordGraph(String beginWord, List<String> wordList) {
        id.put(beginWord, 0);
        for (String w : wordList) {
            id.putIfAbsent(w, id.size());
        }
        int n = id.size();
        words = new String[n];
        for (Map.Entry<String, Integer> e : id.entrySet()) {
            words[e.getValue()] = e.getKey();
        }
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (oneLetterApart(words[i], words[j])) {
                    pairs.add(new int[]{i, j});
                }
            }
        }
        // Positions only: GraphLayout.directed marks its own edges directed, and this graph is not.
        GraphLayout.Layout positions = GraphLayout.directed(new Inputs.GraphInput(n, new int[0][]));
        for (GraphNode base : positions.nodes()) {
            nodes.add(new GraphNode(base.getId(), words[base.getId()], base.getX(), base.getY(), "unvisited"));
        }
        for (int[] p : pairs) {
            edges.add(new GraphEdge(p[0], p[1], null, false, false));
        }
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }
    }

    void mark(String word, String state) {
        Integer i = id.get(word);
        if (i != null) states.put(i, state);
    }

    /** The canvas's key for the edge between two words, or none if either is not drawn. */
    List<String> edge(String a, String b) {
        Integer i = id.get(a);
        Integer j = id.get(b);
        if (i == null || j == null) return List.of();
        return List.of(Math.min(i, j) + "-" + Math.max(i, j));
    }

    private static boolean oneLetterApart(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i) && ++diff > 1) return false;
        }
        return diff == 1;
    }
}
