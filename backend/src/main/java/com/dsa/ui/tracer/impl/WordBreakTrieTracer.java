package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TrieNodeModel;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Word Break (LeetCode 139), trie-accelerated: build a trie of the dictionary, then run
 * the {@code dp[i] = s[0..i)} segments-into-dictionary-words DP where the inner loop
 * walks the trie one character at a time instead of re-slicing and re-hashing every
 * candidate word. {@code dp[j+1]} is set the moment the trie walk reaches a node whose
 * {@code endOfWord} is true, and the walk stops the moment the trie has no edge for the
 * next character - that early stop is the entire reason this is faster than checking
 * every dictionary word against every substring.
 *
 * <p>This was previously three hand-written narration steps with no real DP or trie
 * behind them; this is a full rewrite, not a port.
 */
@Component
public class WordBreakTrieTracer implements AlgorithmTracer {

    private static final double NODE_W = 60;
    private static final double NODE_H = 80;

    @Override
    public String id() {
        return "word-break-trie";
    }

    @Override
    public DsType dsType() {
        return DsType.TRIE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("s", FieldType.STRING)
                        .label("String s")
                        .help("The string to segment.")
                        .constraint("minLength", 1).constraint("maxLength", 20)
                        .constraint("pattern", "[a-z]+")
                        .constraint("patternHint", "Lowercase letters a-z only.")
                        .defaultValue("leetcode")
                        .build(),
                InputField.of("wordDict", FieldType.STRING)
                        .label("Dictionary words")
                        .help("Comma-separated lowercase dictionary words.")
                        .constraint("minLength", 1).constraint("maxLength", 60)
                        .constraint("pattern", "[a-z]{1,10}(,[a-z]{1,10}){0,5}")
                        .constraint("patternHint", "1-6 comma-separated lowercase words, each 1-10 letters.")
                        .defaultValue("leet,code")
                        .build());
    }

    /** A longer string with no valid segmentation, and a dictionary with a shared prefix. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "catsandog", "wordDict", "cats,dog,sand,and,cat");
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean wordBreak(String s, List<String> wordDict) {
                   Trie trie = new Trie();
                   for (String w : wordDict) {
                       // @a buildDict
                       trie.insert(w);
                   }

                   int n = s.length();
                   boolean[] dp = new boolean[n + 1];
                   dp[0] = true;

                   for (int i = 0; i < n; i++) {
                       if (!dp[i]) {
                           // @a skip
                           continue;
                       }
                       Node curr = trie.root;
                       for (int j = i; j < n; j++) {
                           char ch = s.charAt(j);
                           if (!curr.containsKey(ch)) {
                               // @a miss
                               break;
                           }
                           curr = curr.get(ch);
                           if (curr.isEnd()) {
                               // @a mark
                               dp[j + 1] = true;
                           } else {
                               // @a advance
                           }
                       }
                   }
                   // @a done
                   return dp[n];
               }""";
    }

    private static final class BuildNode {
        final int id;
        final Character ch;
        boolean end;
        String state = "default";
        double x;
        double y;
        final Map<Character, Integer> children = new LinkedHashMap<>();

        BuildNode(int id, Character ch) {
            this.id = id;
            this.ch = ch;
        }
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        List<String> wordDict = List.of(in.getString("wordDict").split(","));

        List<BuildNode> nodes = new ArrayList<>();
        BuildNode root = new BuildNode(0, null);
        root.state = "visited";
        nodes.add(root);

        for (String w : wordDict) {
            BuildNode curr = root;
            for (int i = 0; i < w.length(); i++) {
                char ch = w.charAt(i);
                Integer childId = curr.children.get(ch);
                BuildNode child;
                if (childId == null) {
                    child = new BuildNode(nodes.size(), ch);
                    nodes.add(child);
                    curr.children.put(ch, child.id);
                } else {
                    child = nodes.get(childId);
                }
                child.state = "visited";
                curr = child;
            }
            curr.end = true;
            layout(nodes);
            emit.at("buildDict")
                    .say("Insert dictionary word \"%s\" into the trie.", w)
                    .var("dictWord", w)
                    .trie(snapshot(nodes)).step();
        }

        int n = s.length();
        boolean[] dp = new boolean[n + 1];
        dp[0] = true;

        for (int i = 0; i < n; i++) {
            if (!dp[i]) {
                emit.at("skip")
                        .say("dp[%d] is false - no valid split reaches index %d, skip starting here.", i, i)
                        .var("i", i).var("dp", dpString(dp))
                        .trie(snapshot(nodes)).step();
                continue;
            }
            BuildNode curr = root;
            for (int j = i; j < n; j++) {
                char ch = s.charAt(j);
                Integer childId = curr.children.get(ch);
                if (childId == null) {
                    layout(nodes);
                    emit.at("miss")
                            .say("s[%d..%d]=\"%s\" has no trie edge for '%c' - stop extending from i=%d.",
                                    i, j, s.substring(i, j + 1), ch, i)
                            .var("i", i).var("j", j).var("dp", dpString(dp))
                            .trie(snapshot(nodes)).step();
                    break;
                }
                if (curr != root) {
                    curr.state = "visited";
                }
                curr = nodes.get(childId);
                if (curr.end) {
                    dp[j + 1] = true;
                    curr.state = "done";
                    layout(nodes);
                    emit.at("mark")
                            .say("s[%d..%d]=\"%s\" matches a dictionary word - dp[%d] = true.",
                                    i, j, s.substring(i, j + 1), j + 1)
                            .var("i", i).var("j", j).var("dp", dpString(dp))
                            .trie(snapshot(nodes)).step();
                } else {
                    curr.state = "active";
                    layout(nodes);
                    emit.at("advance")
                            .say("s[%d..%d]=\"%s\" is a trie prefix - keep extending.",
                                    i, j, s.substring(i, j + 1))
                            .var("i", i).var("j", j).var("dp", dpString(dp))
                            .trie(snapshot(nodes)).step();
                }
            }
        }

        layout(nodes);
        emit.at("done")
                .say("s = \"%s\" -> dp[%d] = %b: %s be segmented using the dictionary.",
                        s, n, dp[n], dp[n] ? "CAN" : "CANNOT")
                .var("result", String.valueOf(dp[n])).var("dp", dpString(dp))
                .trie(snapshot(nodes)).step();
    }

    private String dpString(boolean[] dp) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dp.length; i++) {
            sb.append(dp[i] ? "T" : "F");
            if (i < dp.length - 1) {
                sb.append(",");
            }
        }
        return sb.append("]").toString();
    }

    /** Recomputes every node's x/y from the current tree shape. */
    private void layout(List<BuildNode> nodes) {
        Map<Integer, BuildNode> byId = new HashMap<>();
        for (BuildNode n : nodes) {
            byId.put(n.id, n);
        }
        assignPositions(nodes.get(0), byId, 0, 0);
    }

    private int leafCount(BuildNode node, Map<Integer, BuildNode> byId) {
        if (node.children.isEmpty()) {
            return 1;
        }
        int sum = 0;
        for (int childId : node.children.values()) {
            sum += leafCount(byId.get(childId), byId);
        }
        return sum;
    }

    private void assignPositions(BuildNode node, Map<Integer, BuildNode> byId, int depth, double leftEdge) {
        double width = leafCount(node, byId) * NODE_W;
        node.x = leftEdge + width / 2;
        node.y = depth * NODE_H + 40;

        double childLeft = leftEdge;
        for (int childId : node.children.values()) {
            BuildNode child = byId.get(childId);
            double childWidth = leafCount(child, byId) * NODE_W;
            assignPositions(child, byId, depth + 1, childLeft);
            childLeft += childWidth;
        }
    }

    private List<TrieNodeModel> snapshot(List<BuildNode> nodes) {
        List<TrieNodeModel> out = new ArrayList<>(nodes.size());
        for (BuildNode n : nodes) {
            Map<String, Integer> children = new LinkedHashMap<>();
            for (Map.Entry<Character, Integer> e : n.children.entrySet()) {
                children.put(String.valueOf(e.getKey()), e.getValue());
            }
            out.add(new TrieNodeModel(
                    n.id, n.ch == null ? null : String.valueOf(n.ch), n.end, n.x, n.y, children, n.state));
        }
        return out;
    }
}
