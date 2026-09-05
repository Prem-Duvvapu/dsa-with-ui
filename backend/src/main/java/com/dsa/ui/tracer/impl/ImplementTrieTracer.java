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
 * Port of {@code com.dsa.ui.algorithm.trie.ImplementTrie} (LeetCode 208): the same
 * character-by-character insert — create a child when the current node has none for
 * this character, otherwise walk into the existing one, then mark the final node as an
 * end of word. The legacy version called this once per word with no visualization of
 * the resulting structure; this tracer calls it once per word in the input list so the
 * shared-prefix branching the problem is actually about is visible.
 *
 * <p>Node layout (x/y) is computed here, server-side, using the same recursive
 * leaf-count width algorithm the pre-RCA-012 canvas used to compute client-side — the
 * canvas now only draws, per {@code TreeCanvas}'s convention for {@code treeNodes}.
 */
@Component
public class ImplementTrieTracer implements AlgorithmTracer {

    private static final double NODE_W = 60;
    private static final double NODE_H = 80;

    @Override
    public String id() {
        return "implement-trie";
    }

    @Override
    public DsType dsType() {
        return DsType.TRIE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("words", FieldType.STRING)
                        .label("Words to insert")
                        .help("Comma-separated lowercase words, inserted into the trie in order.")
                        .constraint("minLength", 1).constraint("maxLength", 60)
                        .constraint("pattern", "[a-z]{1,10}(,[a-z]{1,10}){0,4}")
                        .constraint("patternHint", "1-5 comma-separated lowercase words, each 1-10 letters.")
                        .defaultValue("cat,car,dog")
                        .build());
    }

    /** Four words instead of three, with a word that extends past another word's end. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("words", "to,ten,in,inn");
    }

    @Override
    public String annotatedCode() {
        return """
               public void buildTrie(String[] words) {
                   Node root = new Node();
                   for (String word : words) {
                       Node curr = root;
                       for (int i = 0; i < word.length(); i++) {
                           char ch = word.charAt(i);
                           if (!curr.children.containsKey(ch)) {
                               // @a create
                               curr.children.put(ch, new Node());
                           } else {
                               // @a traverse
                           }
                           curr = curr.children.get(ch);
                       }
                       // @a markEnd
                       curr.isEnd = true;
                   }
               }""";
    }

    /** One node under construction: the character on its incoming edge, and its children. */
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
        List<String> words = List.of(in.getString("words").split(","));

        List<BuildNode> nodes = new ArrayList<>();
        BuildNode root = new BuildNode(0, null);
        root.state = "visited";
        nodes.add(root);

        for (String word : words) {
            BuildNode curr = root;
            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                curr.state = "visited";

                Integer childId = curr.children.get(ch);
                BuildNode child;
                boolean created = childId == null;
                if (created) {
                    child = new BuildNode(nodes.size(), ch);
                    nodes.add(child);
                    curr.children.put(ch, child.id);
                } else {
                    child = nodes.get(childId);
                }
                child.state = "active";
                layout(nodes);

                if (created) {
                    emit.at("create")
                            .say("\"%s\"[%d]='%c': no child here yet - create a new trie node.",
                                    word, i, ch)
                            .var("word", word).var("char", String.valueOf(ch)).var("action", "created")
                            .trie(snapshot(nodes)).step();
                } else {
                    emit.at("traverse")
                            .say("\"%s\"[%d]='%c': child already exists - traverse into it.",
                                    word, i, ch)
                            .var("word", word).var("char", String.valueOf(ch)).var("action", "found")
                            .trie(snapshot(nodes)).step();
                }
                curr = child;
            }
            curr.end = true;
            curr.state = "done";
            layout(nodes);
            emit.at("markEnd")
                    .say("Word \"%s\" fully inserted - mark its final node as end of word.", word)
                    .var("word", word).var("endOfWord", "true")
                    .trie(snapshot(nodes)).step();
        }
    }

    /** Recomputes every node's x/y from the current tree shape (children may have grown). */
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
