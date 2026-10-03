package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Word Ladder (LeetCode 127), traced on the owner's own accepted submission: a level-by-level BFS
 * from beginWord where each level tries all 26 letters at every position of each word and keeps
 * the new words that are in the list. {@code res} counts the words in the sequence, so the level
 * at which endWord is polled is the answer; 0 if it never is. O(N * L * 26).
 *
 * <p>The canvas draws the words joined when they differ by one letter - the moves those 26 tries
 * can find.
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
               class Solution {
                   public int ladderLength(String beginWord, String endWord, List<String> wordList) {
                       // @a init
                       Set<String> set = new HashSet<>();
                       Queue<String> q = new LinkedList<>();
                       Set<String> wordSet = new HashSet<>(wordList);
                       int n = beginWord.length();
                       int res = 1;

                       set.add(beginWord);
                       q.add(beginWord);

                       while (!q.isEmpty()) {
                           int qlen = q.size();

                           while (qlen-- > 0) {
                               // @a poll
                               String curr = q.poll();

                               if (curr.equals(endWord))
                                   // @a found
                                   return res;

                               StringBuilder newWord = new StringBuilder(curr);
                               for (int i=0;i<n;i++) {
                                   char originalChar = newWord.charAt(i);

                                   for (char ch='a';ch<='z';ch++) {
                                       newWord.setCharAt(i,ch);

                                       if (wordSet.contains(newWord.toString()) && !set.contains(newWord.toString())) {
                                           // @a discover
                                           set.add(newWord.toString());
                                           q.add(newWord.toString());
                                       }
                                   }

                                   newWord.setCharAt(i,originalChar);
                               }
                           }

                           // @a level
                           res++;
                       }

                       // @a none
                       return 0;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String beginWord = in.getString("beginWord");
        String endWord = in.getString("endWord");
        List<String> wordList = List.of(in.getString("wordList").split(","));
        WordGraph g = new WordGraph(beginWord, wordList);
        Set<String> set = new HashSet<>();
        Deque<String> q = new ArrayDeque<>();
        Set<String> wordSet = new HashSet<>(wordList);
        int n = beginWord.length();
        int res = 1;
        set.add(beginWord);
        q.add(beginWord);
        g.mark(beginWord, "queued");
        g.mark(endWord, "target");

        emit.at("init").say("Start the sequence at %s: res = 1 word so far. Each BFS level is one more word.",
                        beginWord)
                .var("res", res).graph(g.nodes, g.edges).nodes(g.states).queue(q).step();

        while (!q.isEmpty()) {
            int qlen = q.size();
            while (qlen-- > 0) {
                String curr = q.poll();
                g.mark(curr, "visiting");
                if (curr.equals(endWord)) {
                    g.mark(curr, "done");
                    emit.at("found").say("Polled %s, which is endWord: the shortest sequence has res = %d word%s.",
                                    curr, res, Narration.s(res))
                            .var("res", res).graph(g.nodes, g.edges).nodes(g.states).queue(q).step();
                    return;
                }
                emit.at("poll").say("Poll %s (sequence length %d). Try every letter at every position.", curr, res)
                        .var("curr", curr).var("res", res).graph(g.nodes, g.edges).nodes(g.states).queue(q).step();

                StringBuilder newWord = new StringBuilder(curr);
                for (int i = 0; i < n; i++) {
                    char originalChar = newWord.charAt(i);
                    for (char ch = 'a'; ch <= 'z'; ch++) {
                        newWord.setCharAt(i, ch);
                        String w = newWord.toString();
                        if (wordSet.contains(w) && !set.contains(w)) {
                            set.add(w);
                            q.add(w);
                            if (!w.equals(endWord)) g.mark(w, "queued");
                            emit.at("discover").say("Changing letter %d of %s to '%c' gives %s, a listed word not seen "
                                            + "yet. Queue it.", i, curr, ch, w)
                                    .var("curr", curr).var("res", res)
                                    .graph(g.nodes, g.edges).nodes(g.states).edges(g.edge(curr, w)).queue(q).step();
                        }
                    }
                    newWord.setCharAt(i, originalChar);
                }
                g.mark(curr, "visited");
            }
            res++;
            emit.at("level").say(q.isEmpty()
                            ? "The level is done and nothing new was found."
                            : "Level done. The next level's words are " + (res) + " words into the sequence: res = " + res + ".")
                    .var("res", res).graph(g.nodes, g.edges).nodes(g.states).queue(q).step();
        }

        emit.at("none").say("The queue is empty and %s was never reached. Return 0.", endWord)
                .var("res", 0).graph(g.nodes, g.edges).nodes(g.states).step();
    }
}
