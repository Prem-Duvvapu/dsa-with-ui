package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.TreeNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tail recursion printing 1..n: each call prints its own index, then calls the next one.
 * The call chain is a straight line, not a branching tree, but it is still a real
 * recursion tree — every node has exactly one child until the base case.
 */
@Component
public class Print1ToNTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "print-1-to-n";
    }

    @Override
    public DsType dsType() {
        return DsType.RECURSION_TREE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("n", FieldType.INT)
                        .label("N")
                        .help("How high to count.")
                        .range(1, 8)
                        .defaultValue(5)
                        .build());
    }

    /** A shorter count — fewer frames, a different printed sequence. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public void print1ToN(int i, int n) {
                   if (i > n) {
                       // @a base
                       return;
                   }
                   // @a print
                   System.out.print(i + " ");
                   // @a unwind
                   print1ToN(i + 1, n);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        List<TreeNode> nodes = buildChain(n);
        Map<Integer, String> states = new LinkedHashMap<>();
        for (TreeNode node : nodes) {
            states.put(node.getId(), "unvisited");
        }

        recurse(1, n, nodes, states, emit);
    }

    private void recurse(int i, int n, List<TreeNode> nodes, Map<Integer, String> states, StepEmitter emit) {
        emit.push("print1ToN(i=" + i + ")");

        if (i > n) {
            states.put(i, "visited");
            emit.at("base")
                    .say("i=%d exceeds n=%d — base case: stop, nothing more to print.", i, n)
                    .var("i", i).var("n", n)
                    .tree(nodes).nodes(states).step();
            emit.pop();
            return;
        }

        states.put(i, "calling");
        emit.at("print")
                .say("print1ToN(i=%d, n=%d) prints %d, then calls i=%d next.", i, n, i, i + 1)
                .var("i", i).var("n", n)
                .tree(nodes).nodes(states).step();

        recurse(i + 1, n, nodes, states, emit);

        states.put(i, "visited");
        emit.at("unwind")
                .say("print1ToN(i=%d) returns now that its recursive call has finished.", i)
                .var("i", i)
                .tree(nodes).nodes(states).step();
        emit.pop();
    }

    /** One node per call frame, i = 1..n+1 (the last is the base case), chained top to bottom. */
    private static List<TreeNode> buildChain(int n) {
        List<TreeNode> nodes = new ArrayList<>();
        for (int i = 1; i <= n + 1; i++) {
            Integer child = i <= n ? i + 1 : null;
            nodes.add(new TreeNode(i, "print1ToN(" + i + ")", 200, 40 + 55.0 * (i - 1), child, null, "unvisited"));
        }
        return nodes;
    }
}
