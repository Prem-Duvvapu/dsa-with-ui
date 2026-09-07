package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Materialises the level-order array everything else in this category is typed into as
 * actual linked {@code TreeNode} objects, one {@code new} and one pointer assignment at a
 * time. The array is an ADDRESS scheme - index {@code i}'s parent is at {@code (i - 1) / 2},
 * odd indices are left children, even ones are right - while the class is a POINTER scheme,
 * where a child exists only because some parent's field references it.
 *
 * <p>The two disagree in exactly one place, and that is the point of tracing it: a value
 * whose parent slot is empty has a valid array address and no object to hang off, so it
 * cannot enter the tree at all.
 */
@Component
public class TreeRepJavaTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "tree-rep-java";
    }

    @Override
    public DsType dsType() {
        return DsType.TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tree", FieldType.BINARY_TREE)
                        .label("Level-order values")
                        .help("Level order, with null where a child is absent.")
                        .length(1, 31).values(-99, 99)
                        .defaultValue(Arrays.asList(1, 2, 3, null, 4, 5, 6))
                        .build());
    }

    /** Index 2 is empty, so 5 and 6 at indices 5 and 6 address a parent that was never built. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(9, 8, null, 7, 6, 5, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               class TreeNode {
                   int val;
                   TreeNode left, right;
                   TreeNode(int val) { this.val = val; }
               }

               public TreeNode build(Integer[] level) {
                   TreeNode[] nodes = new TreeNode[level.length];
                   for (int i = 0; i < level.length; i++) {
                       if (level[i] == null) {
                           // @a emptySlot
                           continue;                    // that child pointer stays null
                       }
                       if (i == 0) {
                           // @a newRoot
                           nodes[0] = new TreeNode(level[0]);
                           continue;
                       }
                       int parent = (i - 1) / 2;
                       if (nodes[parent] == null) {
                           // @a orphan
                           continue;                    // no parent object to hang it off
                       }
                       nodes[i] = new TreeNode(level[i]);
                       if (i % 2 == 1) {
                           // @a linkLeft
                           nodes[parent].left = nodes[i];
                       } else {
                           // @a linkRight
                           nodes[parent].right = nodes[i];
                       }
                   }
                   // @a done
                   return nodes.length == 0 ? null : nodes[0];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Integer[] level = in.getBinaryTree("tree");
        Integer[] built = new Integer[level.length];
        Map<Integer, String> states = new LinkedHashMap<>();
        int created = 0;

        for (int i = 0; i < level.length; i++) {
            if (level[i] == null) {
                emit.at("emptySlot")
                        .say("Index %d holds null. No object is created, and whichever child "
                                        + "pointer of %s would have referenced it simply stays null.",
                                i, i == 0 ? "the root" : "node " + describeParent(built, i))
                        .var("i", i).var("nodesCreated", created)
                        .tree(render(built, states)).step();
                continue;
            }

            if (i == 0) {
                built[0] = level[0];
                created++;
                states.put(0, "target");
                emit.at("newRoot")
                        .say("new TreeNode(%d) at index 0. This one object is the whole tree so "
                                + "far - every other node has to be reachable from it.", level[0])
                        .var("i", 0).var("nodesCreated", created)
                        .tree(render(built, states)).step();
                continue;
            }

            int parent = (i - 1) / 2;
            if (built[parent] == null) {
                emit.at("orphan")
                        .say("Index %d holds %d and addresses parent index %d - but nothing was "
                                        + "ever built there, so there is no left/right field to "
                                        + "assign. %d cannot join the tree.",
                                i, level[i], parent, level[i])
                        .var("i", i).var("parentIndex", parent).var("nodesCreated", created)
                        .tree(render(built, states)).step();
                continue;
            }

            built[i] = level[i];
            created++;
            states.put(parent, "visited");
            states.put(i, "visiting");
            if (i % 2 == 1) {
                emit.at("linkLeft")
                        .say("Index %d is odd, so it is a LEFT child: new TreeNode(%d), then "
                                        + "node(%d).left = it.",
                                i, level[i], built[parent])
                        .var("i", i).var("parent", built[parent]).var("side", "left")
                        .var("nodesCreated", created)
                        .tree(render(built, states)).step();
            } else {
                emit.at("linkRight")
                        .say("Index %d is even, so it is a RIGHT child: new TreeNode(%d), then "
                                        + "node(%d).right = it.",
                                i, level[i], built[parent])
                        .var("i", i).var("parent", built[parent]).var("side", "right")
                        .var("nodesCreated", created)
                        .tree(render(built, states)).step();
            }
        }

        for (int i = 0; i < built.length; i++) {
            if (built[i] != null) {
                states.put(i, "done");
            }
        }
        emit.at("done")
                .say("%d TreeNode object%s linked. The array is gone from here on - the "
                                + "algorithms that follow only ever follow .left and .right.",
                        created, created == 1 ? "" : "s")
                .var("nodesCreated", created)
                .tree(render(built, states)).step();
    }

    private java.util.List<com.dsa.ui.model.TreeNode> render(Integer[] built,
                                                             Map<Integer, String> states) {
        return new BinaryTreeLayout(built).render(states);
    }

    private String describeParent(Integer[] built, int i) {
        int parent = (i - 1) / 2;
        if (parent < built.length && built[parent] != null) {
            return String.valueOf(built[parent]);
        }
        return "index " + parent;
    }
}
