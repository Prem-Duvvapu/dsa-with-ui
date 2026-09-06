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
 * Tail recursion counting n down to 1: the mirror image of {@code print-1-to-n} — same
 * straight-line call chain, values descending instead of ascending.
 */
@Component
public class PrintNTo1Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "print-n-to-1";
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
                        .help("Where to start counting down from.")
                        .range(1, 8)
                        .defaultValue(5)
                        .build());
    }

    /** A shorter countdown — fewer frames, a different printed sequence. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public void printNTo1(int n) {
                   if (n == 0) {
                       // @a base
                       return;
                   }
                   // @a print
                   System.out.print(n + " ");
                   // @a unwind
                   printNTo1(n - 1);
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

        recurse(n, n, nodes, states, emit);
    }

    private void recurse(int current, int total, List<TreeNode> nodes, Map<Integer, String> states,
                          StepEmitter emit) {
        int id = total - current + 1;
        emit.push("printNTo1(n=" + current + ")");

        if (current == 0) {
            states.put(id, "visited");
            emit.at("base")
                    .say("n=0 — base case: stop, nothing more to print.")
                    .var("n", current)
                    .tree(nodes).nodes(states).step();
            emit.pop();
            return;
        }

        states.put(id, "calling");
        emit.at("print")
                .say("printNTo1(n=%d) prints %d, then calls n=%d next.", current, current, current - 1)
                .var("n", current)
                .tree(nodes).nodes(states).step();

        recurse(current - 1, total, nodes, states, emit);

        states.put(id, "visited");
        emit.at("unwind")
                .say("printNTo1(n=%d) returns now that its recursive call has finished.", current)
                .var("n", current)
                .tree(nodes).nodes(states).step();
        emit.pop();
    }

    /** One node per call frame, n = total..0 (0 is the base case), chained top to bottom. */
    private static List<TreeNode> buildChain(int total) {
        List<TreeNode> nodes = new ArrayList<>();
        for (int id = 1; id <= total + 1; id++) {
            int n = total - id + 1;
            Integer child = id <= total ? id + 1 : null;
            nodes.add(new TreeNode(id, "printNTo1(" + n + ")", 200, 40 + 55.0 * (id - 1), child, null, "unvisited"));
        }
        return nodes;
    }
}
