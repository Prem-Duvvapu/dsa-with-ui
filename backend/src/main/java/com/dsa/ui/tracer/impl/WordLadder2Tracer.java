package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.GraphEdge;
import com.dsa.ui.model.GraphNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Every SHORTEST transformation sequence, not just one. A plain BFS that records a single
 * parent per word finds one ladder and silently discards the rest, so this keeps a LIST of
 * predecessors per word and fills it layer by layer: a word discovered for the first time in
 * layer L+1 records the layer-L word that found it, and any other layer-L word that also
 * reaches it appends itself as an additional predecessor. Words already settled in an earlier
 * layer are never re-recorded - that is what keeps the reconstructed paths shortest.
 *
 * <p>BFS stops at the end of the layer that first reaches endWord; going further would collect
 * predecessors for longer ladders. The answers are then read out by DFS backtracking from
 * endWord through the predecessor lists, which branches exactly where two words tie.
 *
 * <p>The word list travels as one comma-separated {@link FieldType#STRING}, matching
 * {@code word-ladder-1}: a comma cannot appear inside a lowercase word, and the bounded
 * regex repetition caps both the word length and the word count.
 */
@Component
public class WordLadder2Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "word-ladder-2";
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
                        .help("Comma-separated lowercase words, up to 10.")
                        .length(1, 120)
                        .constraint("pattern", "[a-z]{1,10}(,[a-z]{1,10}){0,9}")
                        .constraint("patternHint", "Comma-separated lowercase words (max 10), letters only.")
                        .defaultValue("hot,dot,dog,lot,log,cog")
                        .build());
    }

    /**
     * LeetCode 126's second example: the same ladder with "cog" missing from the dictionary,
     * so the BFS drains every layer and there is nothing to reconstruct - the opposite outcome
     * of the defaults' two tied ladders.
     */
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
               public List<List<String>> findLadders(String beginWord, String endWord,
                                                      Map<String, List<String>> adjacency) {
                   Map<String, Integer> level = new HashMap<>();
                   Map<String, List<String>> preds = new HashMap<>();
                   List<String> layer = List.of(beginWord);
                   level.put(beginWord, 0);
                   // @a init

                   int depth = 0;
                   boolean found = false;
                   while (!layer.isEmpty() && !found) {
                       // @a layerStart
                       List<String> next = new ArrayList<>();
                       for (String word : layer) {
                           for (String nb : adjacency.getOrDefault(word, List.of())) {
                               if (!level.containsKey(nb)) {
                                   // @a expand
                                   level.put(nb, depth + 1);
                                   preds.computeIfAbsent(nb, k -> new ArrayList<>()).add(word);
                                   next.add(nb);
                               } else if (level.get(nb) == depth + 1) {
                                   // @a altPred
                                   preds.get(nb).add(word);   // a tie, not a longer route
                               }
                           }
                       }
                       depth++;
                       layer = next;
                       found = level.containsKey(endWord);
                   }

                   if (!found) {
                       // @a unreachable
                       return List.of();
                   }
                   // @a found
                   List<List<String>> ladders = new ArrayList<>();
                   backtrack(endWord, new ArrayDeque<>(), preds, beginWord, ladders);
                   // @a done
                   return ladders;
               }

               private void backtrack(String word, Deque<String> path,
                                       Map<String, List<String>> preds,
                                       String beginWord, List<List<String>> ladders) {
                   path.addFirst(word);
                   if (word.equals(beginWord)) {
                       // @a pathFound
                       ladders.add(new ArrayList<>(path));
                   } else {
                       for (String p : preds.get(word)) {
                           // @a buildPath
                           backtrack(p, path, preds, beginWord, ladders);
                       }
                   }
                   path.removeFirst();
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

        // Node positions only; the word graph is undirected, so the edges are built here
        // rather than taken from GraphLayout.directed(), which always sets the arrowhead flag.
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

        Map<Integer, Integer> level = new HashMap<>();
        Map<Integer, List<Integer>> preds = new HashMap<>();
        level.put(beginId, 0);
        states.put(beginId, "visited");
        List<Integer> layer = new ArrayList<>(List.of(beginId));

        emit.at("init")
                .say("Built the one-letter-transformation graph over %d word(s). BFS outward from "
                                + "'%s' one whole layer at a time, remembering EVERY predecessor that "
                                + "reaches a word first.", n, beginWord)
                .var("layer 0", words(layer, idToWord))
                .var("target", endWord)
                .graph(nodes, edges).nodes(states).queue(words(layer, idToWord)).step();

        int depth = 0;
        boolean found = endId != null && level.containsKey(endId);
        while (!layer.isEmpty() && !found) {
            emit.at("layerStart")
                    .say("Layer %d holds %s. Expand all of it before looking at layer %d.",
                            depth, words(layer, idToWord), depth + 1)
                    .var("depth", depth).var("layer", words(layer, idToWord))
                    .graph(nodes, edges).nodes(states).queue(words(layer, idToWord)).step();

            List<Integer> next = new ArrayList<>();
            for (int cur : layer) {
                for (int nb : adjacency.get(cur)) {
                    if (!level.containsKey(nb)) {
                        level.put(nb, depth + 1);
                        preds.computeIfAbsent(nb, k -> new ArrayList<>()).add(cur);
                        next.add(nb);
                        states.put(nb, "queued");
                        emit.at("expand")
                                .say("'%s' -> '%s': first time '%s' is seen, so it lands in layer %d "
                                                + "with predecessor '%s'.",
                                        idToWord[cur], idToWord[nb], idToWord[nb], depth + 1, idToWord[cur])
                                .var("word", idToWord[nb]).var("depth", depth + 1)
                                .var("preds(" + idToWord[nb] + ")", words(preds.get(nb), idToWord))
                                .graph(nodes, edges).nodes(states)
                                .edges(List.of(cur + "-" + nb))
                                .queue(words(next, idToWord)).step();
                    } else if (level.get(nb) == depth + 1) {
                        preds.get(nb).add(cur);
                        emit.at("altPred")
                                .say("'%s' also reaches '%s', which is already in layer %d - a TIE, "
                                                + "so record '%s' as an extra predecessor: %s.",
                                        idToWord[cur], idToWord[nb], depth + 1, idToWord[cur],
                                        words(preds.get(nb), idToWord))
                                .var("word", idToWord[nb])
                                .var("preds(" + idToWord[nb] + ")", words(preds.get(nb), idToWord))
                                .graph(nodes, edges).nodes(states)
                                .edges(List.of(cur + "-" + nb))
                                .queue(words(next, idToWord)).step();
                    }
                }
            }

            for (int id : next) {
                states.put(id, "visited");
            }
            depth++;
            layer = next;
            found = endId != null && level.containsKey(endId);
        }

        if (!found) {
            emit.at("unreachable")
                    .say("Every layer drained without ever reaching '%s' - no transformation "
                            + "sequence exists, so the answer is the empty list.", endWord)
                    .var("ladders", 0)
                    .graph(nodes, edges).nodes(states).step();
            emit.at("done")
                    .say("0 shortest transformation sequence(s) from '%s' to '%s'.", beginWord, endWord)
                    .var("ladders", 0)
                    .graph(nodes, edges).nodes(states).step();
            return;
        }

        states.put(endId, "visiting");
        emit.at("found")
                .say("'%s' first appears in layer %d, so every shortest ladder has %d word(s). Stop "
                                + "expanding and walk the predecessor lists back from '%s'.",
                        endWord, level.get(endId), level.get(endId) + 1, endWord)
                .var("length", level.get(endId) + 1)
                .graph(nodes, edges).nodes(states).step();

        List<String> ladders = new ArrayList<>();
        Deque<Integer> path = new ArrayDeque<>();
        backtrack(endId, beginId, path, preds, idToWord, ladders, nodes, edges, states, emit);

        emit.at("done")
                .say("%d shortest transformation sequence(s) of %d word(s) each: %s.",
                        ladders.size(), level.get(endId) + 1, String.join("; ", ladders))
                .var("ladders", ladders.size())
                .var("answer", String.join("; ", ladders))
                .graph(nodes, edges).nodes(states).step();
    }

    private void backtrack(int word, int beginId, Deque<Integer> path,
                            Map<Integer, List<Integer>> preds, String[] idToWord,
                            List<String> ladders, List<GraphNode> nodes, List<GraphEdge> edges,
                            Map<Integer, String> states, StepEmitter emit) {
        path.addFirst(word);
        emit.push("backtrack(" + idToWord[word] + ")");

        if (word == beginId) {
            String ladder = String.join(" -> ", words(path, idToWord));
            ladders.add(ladder);
            emit.at("pathFound")
                    .say("Reached the start word, so this branch is a complete ladder: %s.", ladder)
                    .var("ladder", ladder).var("ladders so far", ladders.size())
                    .graph(nodes, edges).nodes(states).stack(words(path, idToWord)).step();
        } else {
            for (int p : preds.get(word)) {
                emit.at("buildPath")
                        .say("'%s' can be reached from '%s' in the previous layer - step back to it. "
                                        + "Partial ladder so far: %s.",
                                idToWord[word], idToWord[p], String.join(" -> ", words(path, idToWord)))
                        .var("word", idToWord[word]).var("predecessor", idToWord[p])
                        .graph(nodes, edges).nodes(states)
                        .edges(List.of(p + "-" + word))
                        .stack(words(path, idToWord)).step();
                backtrack(p, beginId, path, preds, idToWord, ladders, nodes, edges, states, emit);
            }
        }

        emit.pop();
        path.removeFirst();
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

    private static List<String> words(Iterable<Integer> ids, String[] idToWord) {
        List<String> out = new ArrayList<>();
        for (int id : ids) {
            out.add(idToWord[id]);
        }
        return out;
    }
}
