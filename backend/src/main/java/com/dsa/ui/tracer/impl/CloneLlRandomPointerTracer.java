package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deep-copies a linked list where each node also carries a random pointer (LeetCode 138),
 * using the two-pass hash-map approach: create every clone first, then wire each clone's
 * next/random to the OTHER clones (never back to an original node).
 *
 * <p>Unlike {@link FlatteningLlTracer}, the "next" chain here is always a plain sequential
 * list — only the random pointer is caller-specified — so an {@code INT_GRID} row {@code
 * [val, randomIndex]} (-1 = no random pointer) is both a faithful input contract and
 * growth-safe: both passes visit every declared row unconditionally in position order, never
 * by chasing a pointer, so a row {@code TracerContractTest}'s auto-grower appends is still
 * visited and genuinely adds work — unlike a graph/pointer-chasing traversal, position-order
 * iteration cannot strand a duplicated row as unreachable.
 */
@Component
public class CloneLlRandomPointerTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "clone-ll-random-pointer";
    }

    @Override
    public DsType dsType() {
        return DsType.LINKED_LIST;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nodes", FieldType.INT_GRID)
                        .label("Nodes")
                        .help("One row per node, head first: [value, randomIndex]. randomIndex "
                                + "is the 0-indexed row this node's random pointer targets, or -1 "
                                + "for none. Must be a valid row index — not checked.")
                        .constraint("maxRows", 12)
                        .constraint("maxCols", 2)
                        .values(-1, 999)
                        .defaultValue(List.of(
                                // LeetCode 138's own Example 1.
                                List.of(7, -1), List.of(13, 0), List.of(11, 4),
                                List.of(10, 2), List.of(1, 0)))
                        .build());
    }

    /** LeetCode 138's own Example 3 — fewer nodes, repeated values, a single random pointer. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nodes", List.of(List.of(3, -1), List.of(3, 0), List.of(3, -1)));
    }

    @Override
    public String annotatedCode() {
        return """
               public Node copyRandomList(Node head) {
                   // @a init
                   Map<Node, Node> clones = new LinkedHashMap<>();
                   Node curr = head;
                   while (curr != null) {
                       // @a createClone
                       clones.put(curr, new Node(curr.val));
                       curr = curr.next;
                   }
                   curr = head;
                   while (curr != null) {
                       // @a pass2Start
                       Node clone = clones.get(curr);
                       if (curr.next != null) {
                           // @a wireNext
                           clone.next = clones.get(curr.next);
                       }
                       if (curr.random != null) {
                           // @a wireRandom
                           clone.random = clones.get(curr.random);
                       }
                       curr = curr.next;
                   }
                   // @a done
                   return clones.get(head);
               }""";
    }

    /**
     * Each node's visibility is tracked by its own two booleans rather than a single
     * "wired up to index" cutoff — {@code wireNext} and {@code wireRandom} land as separate
     * steps within the same iteration, and a shared cutoff would have revealed both edges
     * the moment either one fired, showing an edge before the step that narrates it.
     */
    private List<ListNode> render(int[] vals, int[] randomIdx, boolean[] created,
                                   boolean[] nextWired, boolean[] randomWired, int highlight) {
        List<ListNode> nodes = new ArrayList<>(vals.length);
        for (int i = 0; i < vals.length; i++) {
            if (!created[i]) {
                nodes.add(new ListNode(i, "?", null, null, null, null, "default"));
                continue;
            }
            String state = i == highlight ? "curr" : "visited";
            Integer nextId = (nextWired[i] && i + 1 < vals.length) ? i + 1 : null;
            Integer randomId = (randomWired[i] && randomIdx[i] != -1) ? randomIdx[i] : null;
            nodes.add(new ListNode(i, String.valueOf(vals[i]), nextId, null, null, randomId, state));
        }
        return nodes;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[][] rows = in.getGrid("nodes");
        int n = rows.length;
        int[] vals = new int[n];
        int[] randomIdx = new int[n];
        for (int i = 0; i < n; i++) {
            vals[i] = rows[i][0];
            randomIdx[i] = rows[i][1];
        }

        boolean[] created = new boolean[n];
        boolean[] nextWired = new boolean[n];
        boolean[] randomWired = new boolean[n];

        emit.at("init")
                .say("Walk the original list once, creating an empty clone for each of its %d nodes.", n)
                .var("count", n)
                .list(render(vals, randomIdx, created, nextWired, randomWired, -1)).step();

        for (int i = 0; i < n; i++) {
            created[i] = true;
            emit.at("createClone")
                    .say("Create a clone of node %s (val %d).", i, vals[i])
                    .var("cloned", vals[i])
                    .list(render(vals, randomIdx, created, nextWired, randomWired, i)).step();
        }

        for (int i = 0; i < n; i++) {
            emit.at("pass2Start")
                    .say("Look up the clone for original node %s (val %d); wire its pointers to other clones only.",
                            i, vals[i])
                    .var("curr", vals[i])
                    .list(render(vals, randomIdx, created, nextWired, randomWired, i)).step();

            if (i + 1 < n) {
                nextWired[i] = true;
                emit.at("wireNext")
                        .say("clone(%d).next = clone(%d).", vals[i], vals[i + 1])
                        .var("next", vals[i + 1])
                        .list(render(vals, randomIdx, created, nextWired, randomWired, i)).step();
            }
            if (randomIdx[i] != -1) {
                randomWired[i] = true;
                emit.at("wireRandom")
                        .say("clone(%d).random = clone(%d), following the original's own random pointer.",
                                vals[i], vals[randomIdx[i]])
                        .var("random", vals[randomIdx[i]])
                        .list(render(vals, randomIdx, created, nextWired, randomWired, i)).step();
            }
        }

        emit.at("done")
                .say("Both passes complete — the clone list is fully wired and shares no node with the original.")
                .var("head", vals[0])
                .list(render(vals, randomIdx, created, nextWired, randomWired, -1)).step();
    }
}
