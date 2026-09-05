package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.GraphEdge;
import com.dsa.ui.model.GraphNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Shortest transformation sequence from {@code beginWord} to {@code endWord}, where every
 * step changes exactly one letter and the intermediate word must appear in the supplied
 * word list. The transformation graph is never handed to the algorithm directly - it is
 * implicit, built here by comparing every pair of candidate words - but it is still a
 * genuine graph (words are vertices, a one-letter difference is an edge), so this tracer
 * builds that graph explicitly and runs plain BFS over it, exactly the way the algorithm
 * conceptually works.
 *
 * <p>The word list travels as one comma-separated {@link FieldType#STRING}, not a new
 * field kind: this problem's alphabet is lowercase letters only, so a comma can never
 * appear inside a word, and the existing {@code .constraint("pattern", ...)} mechanism
 * already bounds both the per-word length and the word count (a bounded regex repetition,
 * {@code {0,11}}) without touching {@link InputValidator}.
 */
@Component
public class WordLadder1Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "word-ladder-1";
    }

    @Override
    public DsType dsType() {
        return DsType.GRAPH;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("beginWord", FieldType.STRING)
                        .label("Start word")
                        .help("Lowercase letters only.")
                        .length(1, 10)
                        .constraint("pattern", "[a-z]{1,10}")
                        .constraint("patternHint", "Lowercase letters a-z only.")
                        .defaultValue("hit")
                        .build(),
                InputField.of("endWord", FieldType.STRING)
                        .label("Target word")
                        .help("Lowercase letters only.")
                        .length(1, 10)
                        .constraint("pattern", "[a-z]{1,10}")
                        .constraint("patternHint", "Lowercase letters a-z only.")
                        .defaultValue("cog")
                        .build(),
                InputField.of("wordList", FieldType.STRING)
                        .label("Word list")
                        .help("Comma-separated lowercase words, up to 12.")
                        .length(1, 140)
                        .constraint("pattern", "[a-z]{1,10}(,[a-z]{1,10}){0,11}")
                        .constraint("patternHint", "Comma-separated lowercase words (max 12), letters only.")
                        .defaultValue("hot,dot,dog,lot,log,cog")
                        .build());
    }

    /** Removes "cog" from the word list - no path can reach endWord, the opposite outcome of the default. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "beginWord", "hit",
                "endWord", "cog",
                "wordList", "hot,dot,dog,lot,log");
    }

    @Override
    public String annotatedCode() {
        return """
               public int wordLadderLength(String beginWord, String endWord,
                                            Map<String, List<String>> adjacency) {
                   Deque<String> queue = new ArrayDeque<>();
                   Map<String, Integer> length = new HashMap<>();
                   queue.add(beginWord);
                   length.put(beginWord, 1);
                   // @a init

                   while (!queue.isEmpty()) {
                       String word = queue.poll();
                       // @a visit
                       if (word.equals(endWord)) {
                           // @a found
                           return length.get(word);
                       }
                       for (String neighbor : adjacency.getOrDefault(word, List.of())) {
                           if (!length.containsKey(neighbor)) {
                               length.put(neighbor, length.get(word) + 1);
                               queue.add(neighbor);
                               // @a enqueueNeighbor
                           }
                       }
                   }
                   // @a unreachable
                   return 0;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String beginWord = in.getString("beginWord");
        String endWord = in.getString("endWord");
        String[] listed = in.getString("wordList").split(",");

        LinkedHashMap<String, Integer> wordToId = new LinkedHashMap<>();
        wordToId.put(beginWord, 0);
        for (String w : listed) {
            wordToId.putIfAbsent(w, wordToId.size());
        }
        int n = wordToId.size();
        String[] idToWord = new String[n];
        for (Map.Entry<String, Integer> e : wordToId.entrySet()) {
            idToWord[e.getValue()] = e.getKey();
        }

        List<int[]> edgePairs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (oneLetterApart(idToWord[i], idToWord[j])) {
                    edgePairs.add(new int[]{i, j});
                }
            }
        }
        List<List<Integer>> adjacency = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjacency.add(new ArrayList<>());
        }
        for (int[] e : edgePairs) {
            adjacency.get(e[0]).add(e[1]);
            adjacency.get(e[1]).add(e[0]);
        }

        // Node positions only - this word graph is undirected, so the edges are built
        // below rather than taken from GraphLayout.directed(), which always marks
        // edges directed for the arrowhead it draws.
        GraphLayout.Layout positionOnly =
                GraphLayout.directed(new Inputs.GraphInput(n, new int[0][]));
        List<GraphNode> nodes = new ArrayList<>();
        for (GraphNode base : positionOnly.nodes()) {
            nodes.add(new GraphNode(base.getId(), idToWord[base.getId()], base.getX(), base.getY(), "unvisited"));
        }
        List<GraphEdge> edges = new ArrayList<>();
        for (int[] e : edgePairs) {
            edges.add(new GraphEdge(e[0], e[1], null, false, false));
        }

        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            states.put(i, "unvisited");
        }

        int beginId = 0;
        Integer endId = wordToId.get(endWord);

        Deque<Integer> queue = new ArrayDeque<>();
        Map<Integer, Integer> length = new HashMap<>();
        queue.add(beginId);
        length.put(beginId, 1);
        states.put(beginId, "queued");

        emit.at("init").say(
                        "Built the one-letter-transformation graph over %d words. Seed the queue with "
                                + "'%s' at length 1.", n, beginWord)
                .var("length", 1)
                .graph(nodes, edges).nodes(states).queue(idsToWords(queue, idToWord)).step();

        boolean found = false;
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            states.put(cur, "visiting");

            emit.at("visit").say("Dequeue '%s' (length %d).", idToWord[cur], length.get(cur))
                    .var("word", idToWord[cur]).var("length", length.get(cur))
                    .graph(nodes, edges).nodes(states).queue(idsToWords(queue, idToWord)).step();

            if (endId != null && cur == endId) {
                states.put(cur, "visited");
                emit.at("found").say(
                                "'%s' is the target word - the transformation sequence has %d words.",
                                idToWord[cur], length.get(cur))
                        .var("length", length.get(cur))
                        .graph(nodes, edges).nodes(states).step();
                found = true;
                break;
            }

            for (int next : adjacency.get(cur)) {
                if (!length.containsKey(next)) {
                    length.put(next, length.get(cur) + 1);
                    queue.add(next);
                    states.put(next, "queued");

                    emit.at("enqueueNeighbor").say(
                                    "'%s' differs from '%s' by one letter and is unvisited - enqueue it at length %d.",
                                    idToWord[next], idToWord[cur], length.get(next))
                            .var("neighbor", idToWord[next]).var("length", length.get(next))
                            .graph(nodes, edges).nodes(states)
                            .edges(List.of(cur + "-" + next))
                            .queue(idsToWords(queue, idToWord)).step();
                }
            }
            states.put(cur, "visited");
        }

        if (!found) {
            emit.at("unreachable").say(
                            "Queue emptied without reaching '%s' - no transformation sequence exists.", endWord)
                    .graph(nodes, edges).nodes(states).step();
        }
    }

    private static boolean oneLetterApart(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                diff++;
                if (diff > 1) {
                    return false;
                }
            }
        }
        return diff == 1;
    }

    private static List<String> idsToWords(Iterable<Integer> ids, String[] idToWord) {
        List<String> out = new ArrayList<>();
        for (int id : ids) {
            out.add(idToWord[id]);
        }
        return out;
    }
}
