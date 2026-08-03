package com.dsa.ui.algorithm.trie;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.*;

/**
 * Problem: Implement Trie (Prefix Tree - LeetCode 208)
 */
public class ImplementTrie {

    public static class Node {
        public Map<Character, Node> children = new HashMap<>();
        public boolean isEnd = false;
    }

    public void insert(Node root, String word, TraceRecorder recorder) {
        Node curr = root;

        recorder.record(new TraceEvent(
            "start", 10,
            String.format("Trie Insert: Insert word \"%s\" character by character.", word),
            Map.of("word", word),
            "Trie", null
        ));

        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);

            if (!curr.children.containsKey(ch)) {
                curr.children.put(ch, new Node());
                recorder.record(new TraceEvent(
                    "create_node", 18,
                    String.format("Char '%c' (idx=%d): Child node missing. Create new Trie node for '%c'.", ch, i, ch),
                    Map.of("char", String.valueOf(ch), "action", "created"),
                    "Trie", null
                ));
            } else {
                recorder.record(new TraceEvent(
                    "traverse_node", 22,
                    String.format("Char '%c' (idx=%d): Child node already exists. Traverse to child node '%c'.", ch, i, ch),
                    Map.of("char", String.valueOf(ch), "action", "found"),
                    "Trie", null
                ));
            }

            curr = curr.children.get(ch);
        }

        curr.isEnd = true;

        recorder.record(new TraceEvent(
            "complete", 28,
            String.format("Trie Insert Complete! Word \"%s\" fully inserted and marked isEnd = true.", word),
            Map.of("insertedWord", word, "isEnd", "true"),
            "Trie", null
        ));
    }
}
