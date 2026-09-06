package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deleting a leaf or a one-child node is a straight relink: the sole child (if any) takes
 * the deleted node's place. Two children is the interesting case - no single child can take
 * over without breaking the ordering property, so the node borrows its inorder successor's
 * value (the smallest value still bigger than everything to its left) and then deletes that
 * successor instead, which by construction can never itself have a left child, so it never
 * needs this trick a second time.
 */
@Component
public class BstDeleteTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "bst-delete";
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
                        .help("A BST, level order, with null where a child is absent.")
                        .length(1, 31).values(-99, 99)
                        .defaultValue(Arrays.asList(5, 3, 8, 1, 4, 7, 9))
                        .build(),
                InputField.of("key", FieldType.INT)
                        .label("Key to delete")
                        .range(-99, 99)
                        .defaultValue(5)
                        .build());
    }

    /** A one-child (left only) delete instead of the default's two-children successor swap. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "tree", Arrays.asList(8, 4, 12, 2, 6, null, null, null, null, 5, null),
                "key", 6);
    }

    @Override
    public String annotatedCode() {
        return """
               public TreeNode deleteNode(TreeNode root, int key) {
                   if (root == null) {
                       return null;
                   }
                   if (key < root.val) {
                       // @a goLeft
                       root.left = deleteNode(root.left, key);
                   } else if (key > root.val) {
                       // @a goRight
                       root.right = deleteNode(root.right, key);
                   } else if (root.left == null) {
                       // @a replaceWithRight
                       return root.right;
                   } else if (root.right == null) {
                       // @a replaceWithLeft
                       return root.left;
                   } else {
                       TreeNode succ = root.right;
                       while (succ.left != null) {
                           // @a findSuccessor
                           succ = succ.left;
                       }
                       // @a copySuccessor
                       root.val = succ.val;
                       // @a deleteSuccessor
                       root.right = deleteNode(root.right, succ.val);
                   }
                   // @a returnRoot
                   return root;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Integer[] treeArr = in.getBinaryTree("tree");
        int key = in.getInt("key");
        MutableBst bst = new MutableBst();
        MutableBst.Node root = bst.build(treeArr);
        Map<MutableBst.Node, String> states = new LinkedHashMap<>();

        MutableBst.Node result = delete(root, root, key, bst, states, emit);
        if (result != root) {
            // The root itself was the match and had fewer than two children, so it was
            // relinked away entirely rather than falling through to its own returnRoot line.
            String message = result == null
                    ? String.format("%d was the root with no children - the tree is now empty.", key)
                    : String.format("%d was the root with one child - %d becomes the new root.", key, result.val);
            emit.at("returnRoot").say(message).tree(bst.render(result, states)).step();
        }
    }

    private MutableBst.Node delete(MutableBst.Node overallRoot, MutableBst.Node node, int key,
                                    MutableBst bst, Map<MutableBst.Node, String> states, StepEmitter emit) {
        if (node == null) {
            return null;
        }
        emit.push("deleteNode(" + node.val + ")");
        states.put(node, "visiting");

        if (key < node.val) {
            emit.at("goLeft")
                    .say("%d < %d - the key to delete is in %d's left subtree.", key, node.val, node.val)
                    .var("key", key).var("current", node.val)
                    .tree(bst.render(overallRoot, states)).step();
            node.left = delete(overallRoot, node.left, key, bst, states, emit);
        } else if (key > node.val) {
            emit.at("goRight")
                    .say("%d > %d - the key to delete is in %d's right subtree.", key, node.val, node.val)
                    .var("key", key).var("current", node.val)
                    .tree(bst.render(overallRoot, states)).step();
            node.right = delete(overallRoot, node.right, key, bst, states, emit);
        } else if (node.left == null) {
            String replacement = node.right == null ? "nothing - it was a leaf" : String.valueOf(node.right.val);
            emit.at("replaceWithRight")
                    .say("%d found with no left child - its right child (%s) takes its place.", key, replacement)
                    .var("removed", key)
                    .tree(bst.render(overallRoot, states)).step();
            emit.pop();
            return node.right;
        } else if (node.right == null) {
            emit.at("replaceWithLeft")
                    .say("%d found with no right child - its left child (%d) takes its place.",
                            key, node.left.val)
                    .var("removed", key)
                    .tree(bst.render(overallRoot, states)).step();
            emit.pop();
            return node.left;
        } else {
            MutableBst.Node succ = node.right;
            while (succ.left != null) {
                emit.at("findSuccessor")
                        .say("Looking for %d's inorder successor - keep descending left from %d.",
                                key, succ.val)
                        .tree(bst.render(overallRoot, states)).step();
                succ = succ.left;
            }
            emit.at("copySuccessor")
                    .say("%d has two children - its inorder successor is %d. Copy %d's value up in place of %d.",
                            key, succ.val, succ.val, key)
                    .var("copiedFrom", succ.val)
                    .tree(bst.render(overallRoot, states)).step();
            node.val = succ.val;
            emit.at("deleteSuccessor")
                    .say("Now delete %d from the right subtree so its value isn't duplicated.", succ.val)
                    .tree(bst.render(overallRoot, states)).step();
            node.right = delete(overallRoot, node.right, succ.val, bst, states, emit);
        }

        states.put(node, "visited");
        emit.at("returnRoot")
                .say("%d's subtree is settled - return it upward unchanged.", node.val)
                .tree(bst.render(overallRoot, states)).step();
        emit.pop();
        return node;
    }
}
