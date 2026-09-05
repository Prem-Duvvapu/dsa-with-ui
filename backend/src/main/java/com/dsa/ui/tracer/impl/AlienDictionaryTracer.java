package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.GraphEdge;
import com.dsa.ui.model.GraphNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Recovers the alien alphabet's letter order from a word list that is already sorted in
 * that (unknown) order. Comparing every pair of adjacent words for their first differing
 * character induces a character-precedence graph - never handed to the algorithm
 * directly, but a genuine directed graph all the same - which a topological sort (Kahn's
 * algorithm) turns into one valid ordering, or proves impossible if it contains a cycle.
 *
 * <p>The word list travels as one comma-separated {@link FieldType#STRING}, not a new
 * field kind: the alphabet here is lowercase letters only, so a comma can never appear
 * inside a word, and the existing {@code .constraint("pattern", ...)} mechanism already
 * bounds both the per-word length and the word count (a bounded regex repetition,
 * {@code {0,11}}) without touching {@link InputValidator}.
 */
@Component
public class AlienDictionaryTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "alien-dictionary";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("words", FieldType.STRING)
                        .label("Words, in alien sorted order")
                        .help("Comma-separated lowercase words, up to 12, already in the alien "
                                + "language's sorted order.")
                        .length(1, 140)
                        .constraint("pattern", "[a-z]{1,10}(,[a-z]{1,10}){0,11}")
                        .constraint("patternHint", "Comma-separated lowercase words (max 12), letters only.")
                        .defaultValue("wrt,wrf,er,ett,rftt")
                        .build());
    }

    /** A two-word cycle (a before b, then b before a) - no valid order exists, unlike the default. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("words", "ab,ba,ab");
    }

    @Override
    public String annotatedCode() {
        return """
               public String alienOrder(String[] words) {
                   Map<Character, List<Character>> adj = new HashMap<>();
                   Map<Character, Integer> indegree = new HashMap<>();
                   for (String w : words) {
                       for (char c : w.toCharArray()) {
                           adj.putIfAbsent(c, new ArrayList<>());
                           indegree.putIfAbsent(c, 0);
                       }
                   }
                   // @a init

                   for (int i = 0; i + 1 < words.length; i++) {
                       String w1 = words[i], w2 = words[i + 1];
                       int minLen = Math.min(w1.length(), w2.length());
                       boolean differed = false;
                       for (int j = 0; j < minLen; j++) {
                           if (w1.charAt(j) != w2.charAt(j)) {
                               if (!adj.get(w1.charAt(j)).contains(w2.charAt(j))) {
                                   adj.get(w1.charAt(j)).add(w2.charAt(j));
                                   indegree.merge(w2.charAt(j), 1, Integer::sum);
                                   // @a addEdge
                               }
                               differed = true;
                               break;
                           }
                       }
                       if (!differed && w1.length() > w2.length()) {
                           return ""; // w2 is an invalid shorter prefix of w1
                       }
                   }

                   Deque<Character> queue = new ArrayDeque<>();
                   for (char c : indegree.keySet()) {
                       if (indegree.get(c) == 0) queue.add(c);
                   }

                   StringBuilder order = new StringBuilder();
                   while (!queue.isEmpty()) {
                       char c = queue.poll();
                       order.append(c);
                       // @a visit
                       for (char next : adj.get(c)) {
                           indegree.put(next, indegree.get(next) - 1);
                           if (indegree.get(next) == 0) {
                               queue.add(next);
                               // @a enqueueNeighbor
                           }
                       }
                   }

                   if (order.length() < indegree.size()) {
                       // @a cycleDetected
                       return "";
                   }
                   // @a done
                   return order.toString();
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String[] words = in.getString("words").split(",");

        LinkedHashMap<Character, Integer> charToId = new LinkedHashMap<>();
        for (String w : words) {
            for (char c : w.toCharArray()) {
                charToId.putIfAbsent(c, charToId.size());
            }
        }
        int n = charToId.size();
        char[] idToChar = new char[n];
        for (Map.Entry<Character, Integer> e : charToId.entrySet()) {
            idToChar[e.getValue()] = e.getKey();
        }

        GraphLayout.Layout positionOnly =
                GraphLayout.directed(new Inputs.GraphInput(n, new int[0][]));
        List<GraphNode> nodes = new ArrayList<>();
        for (GraphNode base : positionOnly.nodes()) {
            nodes.add(new GraphNode(base.getId(), String.valueOf(idToChar[base.getId()]),
                    base.getX(), base.getY(), "unvisited"));
        }
        List<GraphEdge> edges = new ArrayList<>();

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }

        emit.at("init").say(
                        "%d distinct characters appear across %d words. Compare each adjacent pair to "
                                + "find which character must come before which.", n, words.length)
                .graph(nodes, edges).nodes(states).step();

        List<List<Integer>> adjacency = new ArrayList<>();
        int[] indegree = new int[n];
        for (int i = 0; i < n; i++) {
            adjacency.add(new ArrayList<>());
        }

        for (int i = 0; i + 1 < words.length; i++) {
            String w1 = words[i];
            String w2 = words[i + 1];
            int minLen = Math.min(w1.length(), w2.length());
            boolean differed = false;
            for (int j = 0; j < minLen; j++) {
                if (w1.charAt(j) != w2.charAt(j)) {
                    int from = charToId.get(w1.charAt(j));
                    int to = charToId.get(w2.charAt(j));
                    if (!adjacency.get(from).contains(to)) {
                        adjacency.get(from).add(to);
                        indegree[to]++;
                        edges.add(new GraphEdge(from, to, null, true, false));

                        emit.at("addEdge").say(
                                        "'%s' before '%s' (from comparing '%s' and '%s'): '%s' must "
                                                + "precede '%s' in the alien alphabet.",
                                        w1.charAt(j), w2.charAt(j), w1, w2, w1.charAt(j), w2.charAt(j))
                                .var("edge", w1.charAt(j) + "->" + w2.charAt(j))
                                .graph(nodes, edges).nodes(states)
                                .edges(List.of(from + "-" + to)).step();
                    }
                    differed = true;
                    break;
                }
            }
            if (!differed && w1.length() > w2.length()) {
                // "wrongly ordered": w2 is a shorter prefix of w1 but appears after it -
                // no valid alien alphabet can explain that. Neither the default nor the
                // alternate input exercises this branch (it carries no anchor of its own,
                // matching annotatedCode()'s unmarked `return ""` in the same spot) - the
                // topological-sort cycle check below is what both test inputs exercise.
                return;
            }
        }

        Deque<Integer> queue = new ArrayDeque<>();
        for (int c = 0; c < n; c++) {
            if (indegree[c] == 0) {
                queue.add(c);
                states.put(c, "queued");
            }
        }

        List<Character> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            order.add(idToChar[cur]);
            states.put(cur, "visited");

            emit.at("visit").say("'%s' has no unresolved predecessor left - append it to the order.",
                            idToChar[cur])
                    .var("order", charsToString(order))
                    .graph(nodes, edges).nodes(states).step();

            for (int next : adjacency.get(cur)) {
                indegree[next]--;
                if (indegree[next] == 0) {
                    queue.add(next);
                    states.put(next, "queued");

                    emit.at("enqueueNeighbor").say(
                                    "'%s' has no predecessors left unresolved - enqueue it.", idToChar[next])
                            .var("neighbor", idToChar[next])
                            .graph(nodes, edges).nodes(states)
                            .edges(List.of(cur + "-" + next)).step();
                }
            }
        }

        if (order.size() < n) {
            emit.at("cycleDetected").say(
                            "Only %d of %d characters were ever enqueued - the remaining ones form a "
                                    + "cycle, so no valid order exists.", order.size(), n)
                    .graph(nodes, edges).nodes(states).step();
            return;
        }

        emit.at("done").say("Every character resolved. Alien alphabet order: %s.", charsToString(order))
                .var("order", charsToString(order))
                .graph(nodes, edges).nodes(states).step();
    }

    private static String charsToString(List<Character> chars) {
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }
}
