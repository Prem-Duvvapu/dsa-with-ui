package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Word Ladder II (LeetCode 126), traced on the owner's own accepted submission. A BFS records each
 * word's level in {@code map} and stops when endWord is polled ({@code minSteps}). Then
 * {@code solve} walks back from endWord, only ever stepping to a word with a smaller level; a list
 * that reaches {@code minSteps} words has strictly decreasing levels from minSteps, so it can only
 * be minSteps, ..., 2, 1 - a shortest ladder ending at beginWord - and is reversed into res.
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
               class Solution {
                   List<List<String>> res = new ArrayList<>();
                   Set<String> set = new HashSet<>();
                   Map<String,Integer> map = new HashMap<>();
                   int minSteps = -1;

                   public List<List<String>> findLadders(String beginWord, String endWord, List<String> wordList) {
                       // @a init
                       Queue<String> q = new LinkedList<>();
                       for (String word: wordList)
                           set.add(word);

                       int level = 1;
                       map.put(beginWord, level);
                       q.add(beginWord);
                       set.remove(beginWord);

                       while (!q.isEmpty()) {
                           // @a poll
                           String curr = q.poll();

                           if (curr.equals(endWord)) {
                               // @a reached
                               minSteps = map.get(curr);
                               break;
                           }

                           level = map.get(curr)+1;
                           for (int i=0;i<curr.length();i++) {
                               for (char ch='a';ch<='z';ch++) {
                                   String newWord= curr.substring(0,i)+ch+curr.substring(i+1);
                                   if (set.contains(newWord)) {
                                       // @a discover
                                       q.add(newWord);
                                       map.put(newWord,level);
                                       set.remove(newWord);
                                   }
                               }
                           }
                       }

                       List<String> currList = new ArrayList<>();
                       currList.add(endWord);
                       solve(endWord,currList);

                       // @a done
                       return res;
                   }

                   private void solve(String word,List<String> currList) {
                       // @a enter
                       if (currList.size() == minSteps) {
                           List<String> temp = new ArrayList<>(currList);
                           Collections.reverse(temp);
                           // @a record
                           res.add(temp);
                           return;
                       }

                       String curr = word;
                       for (int i=0;i<curr.length();i++) {
                           for (char ch='a';ch<='z';ch++) {
                               String newWord= curr.substring(0,i)+ch+curr.substring(i+1);
                               if (map.containsKey(newWord) && map.get(newWord) < map.getOrDefault(curr, -1)) {
                                   // @a back
                                   currList.add(newWord);
                                   solve(newWord, currList);
                                   currList.remove(currList.size()-1);
                               }
                           }
                       }
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String beginWord = in.getString("beginWord");
        String endWord = in.getString("endWord");
        List<String> wordList = List.of(in.getString("wordList").split(","));
        Walk w = new Walk(beginWord, endWord, wordList, emit);
        w.findLadders();
    }

    private static final class Walk {
        final List<List<String>> res = new ArrayList<>();
        final Set<String> set = new HashSet<>();
        final Map<String, Integer> map = new HashMap<>();
        int minSteps = -1;
        final String beginWord;
        final String endWord;
        final List<String> wordList;
        final WordGraph g;
        final StepEmitter emit;

        Walk(String beginWord, String endWord, List<String> wordList, StepEmitter emit) {
            this.beginWord = beginWord;
            this.endWord = endWord;
            this.wordList = wordList;
            this.emit = emit;
            g = new WordGraph(beginWord, wordList);
        }

        void findLadders() {
            Deque<String> q = new ArrayDeque<>();
            set.addAll(wordList);
            int level = 1;
            map.put(beginWord, level);
            q.add(beginWord);
            set.remove(beginWord);
            g.mark(beginWord, "queued");
            g.mark(endWord, "target");

            emit.at("init").say("First a BFS from %s that records each word's level (its position in a shortest "
                            + "sequence). %s is level 1.", beginWord, beginWord)
                    .var("map", map.toString()).graph(g.nodes, g.edges).nodes(g.states).queue(q).step();

            while (!q.isEmpty()) {
                String curr = q.poll();
                g.mark(curr, "visiting");
                if (curr.equals(endWord)) {
                    minSteps = map.get(curr);
                    g.mark(curr, "done");
                    emit.at("reached").say("Polled %s at level %d: every shortest ladder has minSteps = %d words. Stop "
                                    + "the BFS.", curr, minSteps, minSteps)
                            .var("minSteps", minSteps).var("map", map.toString())
                            .graph(g.nodes, g.edges).nodes(g.states).queue(q).step();
                    break;
                }
                emit.at("poll").say("Poll %s at level %d. Any listed word one letter away is at level %d.",
                                curr, map.get(curr), map.get(curr) + 1)
                        .var("curr", curr).var("map", map.toString())
                        .graph(g.nodes, g.edges).nodes(g.states).queue(q).step();
                level = map.get(curr) + 1;
                for (int i = 0; i < curr.length(); i++) {
                    for (char ch = 'a'; ch <= 'z'; ch++) {
                        String newWord = curr.substring(0, i) + ch + curr.substring(i + 1);
                        if (set.contains(newWord)) {
                            q.add(newWord);
                            map.put(newWord, level);
                            set.remove(newWord);
                            if (!newWord.equals(endWord)) g.mark(newWord, "queued");
                            emit.at("discover").say("%s -> %s: %s gets level %d.", curr, newWord, newWord, level)
                                    .var("curr", curr).var("map", map.toString())
                                    .graph(g.nodes, g.edges).nodes(g.states).edges(g.edge(curr, newWord))
                                    .queue(q).step();
                        }
                    }
                }
                g.mark(curr, "visited");
            }

            List<String> currList = new ArrayList<>();
            currList.add(endWord);
            solve(endWord, currList);

            emit.at("done").say(res.isEmpty()
                            ? endWord + " was never reached, so there is no ladder. Return []."
                            : "Return res: " + res.size() + " shortest ladder" + Narration.s(res.size()) + ".")
                    .var("res", show(res)).graph(g.nodes, g.edges).nodes(g.states).step();
        }

        void solve(String word, List<String> currList) {
            emit.push("solve(" + word + ")");
            emit.at("enter").say(currList.size() == minSteps
                            ? "solve(" + word + "): the list holds " + minSteps + " words - a full ladder."
                            : "solve(" + word + "): the list " + currList + " holds " + currList.size() + " of "
                                    + minSteps + " words. Look for a word one letter away with a smaller level.")
                    .var("currList", currList.toString()).var("res", show(res))
                    .graph(g.nodes, g.edges).nodes(g.states).step();
            if (currList.size() == minSteps) {
                List<String> temp = new ArrayList<>(currList);
                Collections.reverse(temp);
                res.add(temp);
                emit.at("record").say("Reverse it into %s and add it to res.", String.join(" -> ", temp))
                        .var("res", show(res)).graph(g.nodes, g.edges).nodes(g.states).step();
                emit.pop();
                return;
            }
            String curr = word;
            for (int i = 0; i < curr.length(); i++) {
                for (char ch = 'a'; ch <= 'z'; ch++) {
                    String newWord = curr.substring(0, i) + ch + curr.substring(i + 1);
                    if (map.containsKey(newWord) && map.get(newWord) < map.getOrDefault(curr, -1)) {
                        currList.add(newWord);
                        emit.at("back").say("%s (level %d) is one letter from %s (level %d): step back to it.",
                                        newWord, map.get(newWord), curr, map.getOrDefault(curr, -1))
                                .var("currList", currList.toString())
                                .graph(g.nodes, g.edges).nodes(g.states).edges(g.edge(curr, newWord)).step();
                        solve(newWord, currList);
                        currList.remove(currList.size() - 1);
                    }
                }
            }
            emit.pop();
        }

        /** "hit -> hot -> cog; ..." in res order, "[]" when empty. */
        static String show(List<List<String>> res) {
            if (res.isEmpty()) return "[]";
            List<String> out = new ArrayList<>();
            for (List<String> l : res) out.add(String.join(" -> ", l));
            return String.join("; ", out);
        }
    }
}
