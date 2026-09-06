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
 * The textbook exponential recursion tree: {@code fib(n) = fib(n-1) + fib(n-2)}, with no
 * memoization, so {@code fib(3)} really is computed from scratch every time it is called.
 * Unlike the tail-recursive tracers in this batch, this one genuinely branches — the
 * clearest case in "Learn the Basics" for {@code RecursionTree}'s split/merge states.
 */
@Component
public class FibonacciRecursionTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "fibonacci-recursion";
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
                        .help("Which Fibonacci number to compute.")
                        .range(2, 10)
                        .defaultValue(5)
                        .build());
    }

    /** One step further out — a visibly larger tree and a different answer. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 6);
    }

    @Override
    public String annotatedCode() {
        return """
               public int fib(int n) {
                   if (n <= 1) {
                       // @a base
                       return n;
                   }
                   // @a call
                   return fib(n - 1) + fib(n - 2);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        Map<Integer, NodeInfo> struct = new LinkedHashMap<>();
        buildStructure(n, 0, struct, new int[]{1}, new double[]{0});

        Map<Integer, String> states = new LinkedHashMap<>();
        for (Integer id : struct.keySet()) {
            states.put(id, "unvisited");
        }

        realFib(n, struct, states, new int[]{1}, emit);
    }

    /** One record per call frame: which n it computes, where it sits, and its children's ids. */
    private static final class NodeInfo {
        int id;
        int n;
        double x;
        double y;
        Integer leftId;
        Integer rightId;
    }

    /**
     * Pre-computes the tree's shape (ids, depths, in-order x positions) with no side
     * effects, in the exact same traversal order {@link #realFib} will later use — so the
     * second pass can hand out ids from a matching counter and land on the right node.
     */
    private int buildStructure(int n, int depth, Map<Integer, NodeInfo> struct, int[] idGen, double[] xGen) {
        int id = idGen[0]++;
        NodeInfo info = new NodeInfo();
        info.id = id;
        info.n = n;
        info.y = 40 + depth * 55.0;
        struct.put(id, info);

        if (n > 1) {
            info.leftId = buildStructure(n - 1, depth + 1, struct, idGen, xGen);
        }
        info.x = 40 + (xGen[0]++) * 52;
        if (n > 1) {
            info.rightId = buildStructure(n - 2, depth + 1, struct, idGen, xGen);
        }
        return id;
    }

    private int realFib(int n, Map<Integer, NodeInfo> struct, Map<Integer, String> states, int[] idGen,
                         StepEmitter emit) {
        int id = idGen[0]++;
        emit.push("fib(" + n + ")");

        int result;
        if (n <= 1) {
            states.put(id, "calling");
            emit.at("base")
                    .say("fib(%d) is a base case — returns %d directly, no further calls.", n, n)
                    .var("n", n).var("returns", n)
                    .tree(render(struct, states)).nodes(states).step();
            result = n;
        } else {
            states.put(id, "calling");
            emit.at("call")
                    .say("fib(%d) splits into fib(%d) + fib(%d).", n, n - 1, n - 2)
                    .var("n", n)
                    .tree(render(struct, states)).nodes(states).step();

            int left = realFib(n - 1, struct, states, idGen, emit);
            int right = realFib(n - 2, struct, states, idGen, emit);
            result = left + right;

            states.put(id, "merging");
            emit.at("call")
                    .say("fib(%d) = fib(%d) + fib(%d) = %d + %d = %d.", n, n - 1, n - 2, left, right, result)
                    .var("n", n).var("left", left).var("right", right).var("result", result)
                    .tree(render(struct, states)).nodes(states).step();
        }

        states.put(id, "visited");
        emit.pop();
        return result;
    }

    /** Rebuilds a fresh, immutable node list every step so earlier steps never retroactively change. */
    private static List<TreeNode> render(Map<Integer, NodeInfo> struct, Map<Integer, String> states) {
        List<TreeNode> out = new ArrayList<>(struct.size());
        for (NodeInfo info : struct.values()) {
            out.add(new TreeNode(info.id, "fib(" + info.n + ")", info.x, info.y, info.leftId, info.rightId,
                    states.getOrDefault(info.id, "unvisited")));
        }
        return out;
    }
}
