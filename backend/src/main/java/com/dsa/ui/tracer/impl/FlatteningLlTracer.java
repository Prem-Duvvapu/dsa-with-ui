package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Flattens a multilevel doubly linked list (LeetCode 430) by a stack-based DFS: a node's
 * child list is pushed before its own next, so the child is spliced in immediately after
 * the node rather than left for later — that ordering is the entire algorithm.
 *
 * <p>The input is a {@link FieldType#GRAPH}: vertex {@code i} displays as value {@code i+1}
 * (matching the official LeetCode numbering for this tracer's own default), a weight-0 edge
 * {@code [from,to,0]} is {@code from}'s next pointer, and a weight-1 edge {@code [from,to,1]}
 * is {@code from}'s child pointer — reusing the existing weighted-graph contract as a type
 * tag rather than inventing a new {@code FieldType}, since arbitrary childId/nextId pointers
 * (not a raster grid) would leave duplicated rows unreachable from the declared head under
 * {@code TracerContractTest}'s array/grid auto-grower (every added row would be an orphan no
 * edge points at), silently defeating {@code stepCountGrowsWithInput} regardless of which
 * defaults are chosen. {@code GRAPH} is not in that test's growable set, so it is skipped
 * for this tracer instead of failing — the documented, tolerated outcome for "nothing to
 * scale automatically."
 */
@Component
public class FlatteningLlTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "flattening-ll";
    }

    @Override
    public DsType dsType() {
        return DsType.LINKED_LIST;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("structure", FieldType.GRAPH)
                        .label("Multilevel structure")
                        .help("Vertex i displays as value i+1. A weight-0 edge [from,to,0] is "
                                + "from's next pointer; a weight-1 edge [from,to,1] is from's child "
                                + "pointer. Vertex 0 is the head. At most one outgoing edge of each "
                                + "type per vertex — not checked.")
                        .constraint("maxVertices", 20)
                        .constraint("maxEdges", 40)
                        .weighted().weights(0, 1)
                        .defaultValue(Map.of(
                                "vertices", 12,
                                "edges", List.of(
                                        // Level 1: 1-2-3-4-5-6, with 3 (vertex 2) carrying a child.
                                        List.of(0, 1, 0), List.of(1, 2, 0), List.of(2, 3, 0),
                                        List.of(3, 4, 0), List.of(4, 5, 0),
                                        // Level 2: 7-8-9-10, with 8 (vertex 7) carrying a child.
                                        List.of(6, 7, 0), List.of(7, 8, 0), List.of(8, 9, 0),
                                        // Level 3: 11-12.
                                        List.of(10, 11, 0),
                                        // Child edges: 3's child is 7; 8's child is 11.
                                        List.of(2, 6, 1), List.of(7, 10, 1))))
                        .build());
    }

    /**
     * A single level of nesting instead of two (LeetCode 430's own official example is the
     * default; this is a materially smaller, differently-shaped structure, not a permutation
     * of it) — hand-verified: 1-2-3 with 1's child being 4-5 flattens to 1,4,5,2,3.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "structure", Map.of(
                        "vertices", 5,
                        "edges", List.of(
                                List.of(0, 1, 0), List.of(1, 2, 0),
                                List.of(3, 4, 0),
                                List.of(0, 3, 1))));
    }

    @Override
    public String annotatedCode() {
        return """
               public Node flatten(Node head) {
                   // @a init
                   Deque<Node> stack = new ArrayDeque<>();
                   stack.push(head);
                   Node prev = null;
                   while (!stack.isEmpty()) {
                       // @a pop
                       Node curr = stack.pop();
                       // @a link
                       if (prev != null) prev.next = curr;
                       if (curr.next != null) {
                           // @a pushNext
                           stack.push(curr.next);
                       }
                       if (curr.child != null) {
                           // @a pushChild
                           stack.push(curr.child);
                           curr.child = null;
                       }
                       prev = curr;
                   }
                   // @a done
                   return head;
               }""";
    }

    private String val(int idx) {
        return String.valueOf(idx + 1);
    }

    private List<ListNode> render(int n, Integer[] nextOf, Integer[] childOf, boolean[] popped,
                                   List<Integer> placedOrder, int highlight) {
        List<Integer> order = new ArrayList<>(placedOrder);
        for (int i = 0; i < n; i++) {
            if (!popped[i]) {
                order.add(i);
            }
        }
        List<ListNode> nodes = new ArrayList<>(order.size());
        for (int idx : order) {
            String state = idx == highlight ? "curr" : popped[idx] ? "visited" : "default";
            nodes.add(new ListNode(idx, val(idx), nextOf[idx], null, childOf[idx], null, state));
        }
        return nodes;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        Inputs.GraphInput graph = in.getGraph("structure");
        int n = graph.vertices();

        Integer[] nextOf = new Integer[n];
        Integer[] childOf = new Integer[n];
        for (int[] e : graph.edges()) {
            int from = e[0], to = e[1], type = e[2];
            if (type == 0) {
                if (nextOf[from] == null) {
                    nextOf[from] = to;
                }
            } else if (childOf[from] == null) {
                childOf[from] = to;
            }
        }

        boolean[] popped = new boolean[n];
        List<Integer> placedOrder = new ArrayList<>();

        emit.at("init")
                .say("Push the head (%s) onto the stack. A child gets pushed before its owner's "
                        + "own next, so popping it splices it in immediately rather than leaving "
                        + "it for later.", val(0))
                .var("stack", "[" + val(0) + "]")
                .list(render(n, nextOf, childOf, popped, placedOrder, -1)).step();

        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(0);
        int prev = -1;
        while (!stack.isEmpty()) {
            int curr = stack.pop();
            emit.at("pop")
                    .say("Pop %s off the stack.", val(curr))
                    .var("curr", val(curr))
                    .list(render(n, nextOf, childOf, popped, placedOrder, curr)).step();

            popped[curr] = true;
            placedOrder.add(curr);
            if (prev != -1) {
                nextOf[prev] = curr;
            }
            emit.at("link")
                    .say(prev == -1
                            ? String.format("%s becomes the head of the flattened list.", val(curr))
                            : String.format("Link %s after %s.", val(curr), val(prev)))
                    .var("prev", prev == -1 ? "null" : val(prev)).var("curr", val(curr))
                    .list(render(n, nextOf, childOf, popped, placedOrder, curr)).step();

            if (nextOf[curr] != null) {
                stack.push(nextOf[curr]);
                emit.at("pushNext")
                        .say("%s's own next (%s) still needs visiting — push it for later.",
                                val(curr), val(nextOf[curr]))
                        .var("pushed", val(nextOf[curr]))
                        .list(render(n, nextOf, childOf, popped, placedOrder, curr)).step();
            }
            if (childOf[curr] != null) {
                int child = childOf[curr];
                stack.push(child);
                childOf[curr] = null;
                emit.at("pushChild")
                        .say("%s has a child list starting at %s — push it so it is processed "
                                + "before %s's own next.", val(curr), val(child), val(curr))
                        .var("pushed", val(child))
                        .list(render(n, nextOf, childOf, popped, placedOrder, curr)).step();
            }
            prev = curr;
        }

        List<String> finalVals = new ArrayList<>();
        for (int idx : placedOrder) {
            finalVals.add(val(idx));
        }
        emit.at("done")
                .say("Stack empty — flattening complete. Final order: %s.",
                        String.join(" -> ", finalVals))
                .var("result", String.join(",", finalVals))
                .list(render(n, nextOf, childOf, popped, placedOrder, -1)).step();
    }
}
