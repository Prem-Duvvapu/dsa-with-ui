package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every route from the root down to a leaf, collected with one shared buffer rather than a
 * fresh list per branch.
 *
 * <p>That single buffer is the whole lesson. The path is pushed on the way in and popped on
 * the way out, so at the instant any leaf is reached the buffer holds exactly that leaf's
 * route and nothing else - and a COPY has to be taken right then, because the buffer is
 * about to be unwound and reused by the next branch. Forgetting either the pop or the copy
 * produces answers that look plausible and are wrong in opposite directions: without the pop
 * the paths grow forever, without the copy every recorded path ends up empty.
 */
@Component
public class RootToLeafPathTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "root-to-leaf-path";
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
                        .defaultValue(Arrays.asList(1, 2, 3, null, 5))
                        .build());
    }

    /** A perfect tree: four leaves rather than two, and every backtrack unwinds two levels instead of one. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tree", Arrays.asList(1, 2, 3, 4, 5, 6, 7));
    }

    @Override
    public String annotatedCode() {
        return """
               public List<String> binaryTreePaths(TreeNode root) {
                   List<String> paths = new ArrayList<>();
                   if (root != null) dfs(root, new ArrayList<>(), paths);
                   // @a done
                   return paths;
               }

               private void dfs(TreeNode node, List<Integer> path, List<String> paths) {
                   // @a push
                   path.add(node.val);

                   if (node.left == null && node.right == null) {
                       // @a leafCopyPath
                       paths.add(join(path, "->"));    // a COPY: the buffer is about to unwind
                   } else {
                       if (node.left != null) dfs(node.left, path, paths);
                       if (node.right != null) dfs(node.right, path, paths);
                   }

                   // @a popBacktrack
                   path.remove(path.size() - 1);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        BinaryTreeLayout tree = new BinaryTreeLayout(in.getBinaryTree("tree"));
        Map<Integer, String> states = new LinkedHashMap<>();
        List<String> paths = new ArrayList<>();

        if (!tree.isEmpty()) {
            dfs(tree, tree.root(), new ArrayList<>(), paths, states, emit);
        }

        emit.at("done")
                .say("%d root-to-leaf path%s: %s.",
                        paths.size(), paths.size() == 1 ? "" : "s", paths)
                .var("paths", paths.toString())
                .tree(tree.render(states)).step();
    }

    private void dfs(BinaryTreeLayout tree, int index, List<Integer> path, List<String> paths,
                     Map<Integer, String> states, StepEmitter emit) {
        emit.push("dfs(" + tree.value(index) + ")");
        path.add(tree.value(index));
        states.put(index, "visiting");

        emit.at("push")
                .say("Push %d onto the shared buffer. The buffer now holds the route from the "
                        + "root down to here: %s.", tree.value(index), join(path))
                .var("buffer", join(path)).var("paths", paths.toString())
                .tree(tree.render(states)).step();

        Integer left = tree.left(index);
        Integer right = tree.right(index);

        if (left == null && right == null) {
            paths.add(join(path));
            states.put(index, "target");
            emit.at("leafCopyPath")
                    .say("%d is a leaf, so the buffer is a finished path right now. Record a "
                                    + "COPY of it - the buffer itself is about to be unwound "
                                    + "and reused: %s.",
                            tree.value(index), join(path))
                    .var("buffer", join(path)).var("paths", paths.toString())
                    .tree(tree.render(states)).step();
        } else {
            if (left != null) {
                dfs(tree, left, path, paths, states, emit);
            }
            if (right != null) {
                dfs(tree, right, path, paths, states, emit);
            }
            states.put(index, "visited");
        }

        path.remove(path.size() - 1);
        emit.at("popBacktrack")
                .say("Every route through %d is recorded. Pop it back off so the buffer is "
                                + "exactly what the next branch needs: %s.",
                        tree.value(index), path.isEmpty() ? "empty" : join(path))
                .var("buffer", path.isEmpty() ? "[]" : join(path))
                .var("paths", paths.toString())
                .tree(tree.render(states)).step();

        emit.pop();
    }

    private String join(List<Integer> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                sb.append("->");
            }
            sb.append(path.get(i));
        }
        return sb.toString();
    }
}
