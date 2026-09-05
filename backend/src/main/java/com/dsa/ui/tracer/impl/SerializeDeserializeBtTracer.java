package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Encodes a tree to a comma-separated string with a level-order BFS, then decodes that same
 * string back into a tree built from nothing but the tokens - no shared state with the
 * original. Reconstruction assigns each token the same implicit index (2*i+1 / 2*i+2 for a
 * node at i) that the level-order input already uses, so the round trip can be checked
 * value-for-value against the source tree.
 */
@Component
public class SerializeDeserializeBtTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "serialize-deserialize-bt";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tree", FieldType.BINARY_TREE)
                        .label("Tree (level order)")
                        .help("Level order, with null where a child is absent.")
                        .length(1, 31).values(-99, 99)
                        .defaultValue(Arrays.asList(3, 9, 20, null, null, 15, 7))
                        .build());
    }

    /** A left-heavy shape: every enqueue/assign pair sees a different null/value mix than the default. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, 2, 3, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public String serialize(TreeNode root) {
                   StringBuilder sb = new StringBuilder();
                   Queue<TreeNode> queue = new LinkedList<>();
                   queue.add(root);
                   while (!queue.isEmpty()) {
                       TreeNode node = queue.poll();
                       if (node == null) {
                           // @a serializeNull
                           sb.append("null,");
                           continue;
                       }
                       // @a serializeNode
                       sb.append(node.val).append(",");
                       queue.add(node.left);
                       queue.add(node.right);
                   }
                   return sb.toString();
               }

               public TreeNode deserialize(String data) {
                   String[] tokens = data.split(",");
                   if (tokens[0].equals("null")) {
                       return null;
                   }
                   TreeNode root = new TreeNode(Integer.parseInt(tokens[0]));
                   Queue<TreeNode> queue = new LinkedList<>();
                   queue.add(root);
                   int i = 1;
                   while (!queue.isEmpty()) {
                       TreeNode node = queue.poll();
                       String leftToken = tokens[i++];
                       if (!leftToken.equals("null")) {
                           // @a assignLeft
                           node.left = new TreeNode(Integer.parseInt(leftToken));
                           queue.add(node.left);
                       }
                       String rightToken = tokens[i++];
                       if (!rightToken.equals("null")) {
                           // @a assignRight
                           node.right = new TreeNode(Integer.parseInt(rightToken));
                           queue.add(node.right);
                       }
                   }
                   // @a done
                   return root;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        List<TreeNode> originalNodes = tree.render(Map.of());

        List<String> tokens = new ArrayList<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(tree.isEmpty() ? null : tree.root());
        Map<Integer, String> states = new LinkedHashMap<>();

        while (!queue.isEmpty()) {
            Integer idx = queue.poll();
            if (idx == null) {
                tokens.add("null");
                emit.at("serializeNull")
                        .say("Dequeued an absent child - write \"null\" and enqueue nothing for it.")
                        .var("tokens", String.join(",", tokens))
                        .tree(tree.render(states)).step();
                continue;
            }
            tokens.add(String.valueOf(tree.value(idx)));
            states.put(idx, "visited");
            emit.at("serializeNode")
                    .say("Dequeued node %d - write its value, then enqueue both children whether present or not.",
                            tree.value(idx))
                    .var("tokens", String.join(",", tokens))
                    .tree(tree.render(states)).step();
            queue.add(tree.left(idx));
            queue.add(tree.right(idx));
        }

        String data = String.join(",", tokens);

        Queue<String> tokenQueue = new LinkedList<>(tokens);
        Map<Integer, Integer> rebuilt = new LinkedHashMap<>();

        String rootToken = tokenQueue.poll();
        if (rootToken.equals("null")) {
            emit.at("done")
                    .say("Encoded string \"%s\" starts with \"null\" - the tree was empty; nothing to rebuild.",
                            data)
                    .var("data", data).tree(List.of()).step();
            return;
        }

        rebuilt.put(0, Integer.parseInt(rootToken));
        Queue<Integer> indexQueue = new LinkedList<>();
        indexQueue.add(0);

        while (!indexQueue.isEmpty()) {
            int idx = indexQueue.poll();
            String leftToken = tokenQueue.poll();
            if (!leftToken.equals("null")) {
                int leftIdx = 2 * idx + 1;
                rebuilt.put(leftIdx, Integer.parseInt(leftToken));
                emit.at("assignLeft")
                        .say("Token \"%s\" is node %d's left child - a brand new node built from nothing "
                                + "but this string.", leftToken, rebuilt.get(idx))
                        .var("assigned", leftToken)
                        .tree(new BinaryTreeLayout(toArray(rebuilt)).render(Map.of())).step();
                indexQueue.add(leftIdx);
            }
            String rightToken = tokenQueue.poll();
            if (!rightToken.equals("null")) {
                int rightIdx = 2 * idx + 2;
                rebuilt.put(rightIdx, Integer.parseInt(rightToken));
                emit.at("assignRight")
                        .say("Token \"%s\" is node %d's right child.", rightToken, rebuilt.get(idx))
                        .var("assigned", rightToken)
                        .tree(new BinaryTreeLayout(toArray(rebuilt)).render(Map.of())).step();
                indexQueue.add(rightIdx);
            }
        }

        boolean matches = originalNodes.size() == rebuilt.size();
        if (matches) {
            for (TreeNode node : originalNodes) {
                Integer rebuiltVal = rebuilt.get(node.getId());
                if (rebuiltVal == null || !String.valueOf(rebuiltVal).equals(node.getVal())) {
                    matches = false;
                    break;
                }
            }
        }

        emit.at("done")
                .say(matches
                        ? "Every reconstructed node matches the original tree value-for-value. Round trip succeeded."
                        : "Reconstructed tree does not match the original.")
                .var("data", data).var("matches", matches)
                .tree(new BinaryTreeLayout(toArray(rebuilt)).render(Map.of())).step();
    }

    private static Integer[] toArray(Map<Integer, Integer> rebuilt) {
        int maxIndex = 0;
        for (int idx : rebuilt.keySet()) {
            maxIndex = Math.max(maxIndex, idx);
        }
        Integer[] arr = new Integer[maxIndex + 1];
        rebuilt.forEach((idx, val) -> arr[idx] = val);
        return arr;
    }
}
