package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ListNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Three-pointer list reversal.
 *
 * <p>The Linked List category held 31 problems of which 29 delegated to a three-step
 * hardcoded narration whose {@code activeLine} values (51 and 56) pointed past the end
 * of its own nine-line snippet. Anchors make that impossible here.
 */
@Component
public class ReverseLinkedListTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "reverse-linked-list";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("values", FieldType.LINKED_LIST)
                        .label("List values")
                        .help("Node values from head to tail.")
                        .length(0, 24).values(-999, 999)
                        .defaultValue(List.of(1, 2, 3, 4))
                        .build());
    }

    @Override
    public String annotatedCode() {
        return """
               public ListNode reverse(ListNode head) {
                   // @a init
                   ListNode prev = null, curr = head;
                   while (curr != null) {
                       // @a save
                       ListNode next = curr.next;
                       // @a flip
                       curr.next = prev;
                       // @a advance
                       prev = curr;
                       curr = next;
                   }
                   // @a done
                   return prev;
               }""";
    }

    /**
     * Renders the list as it stands mid-reversal: {@code order} is the current physical
     * chain and {@code currIndex} marks the node being flipped.
     */
    private List<ListNode> render(List<Integer> order, int currIndex, int prevIndex) {
        List<ListNode> nodes = new ArrayList<>(order.size());
        for (int i = 0; i < order.size(); i++) {
            String state = i == currIndex ? "curr" : i == prevIndex ? "prev" : "default";
            Integer nextId = i + 1 < order.size() ? i + 1 : null;
            nodes.add(new ListNode(i, String.valueOf(order.get(i)), nextId, null, state));
        }
        return nodes;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] values = in.getLinkedList("values");
        emit.using("LinkedList");

        if (values.length == 0) {
            emit.at("done").say("The list is empty, so the reversal is empty.")
                    .var("head", "null").list(List.of()).step();
            return;
        }

        // reversed holds the already-flipped prefix; remaining holds what is still to come.
        List<Integer> reversed = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>();
        for (int v : values) {
            remaining.add(v);
        }

        emit.at("init").say("prev = null, curr = head (%d). Nothing is reversed yet.", remaining.get(0))
                .var("prev", "null").var("curr", remaining.get(0))
                .list(render(remaining, 0, -1)).step();

        int stepCount = 0;
        while (!remaining.isEmpty()) {
            int curr = remaining.remove(0);
            String next = remaining.isEmpty() ? "null" : String.valueOf(remaining.get(0));

            emit.at("save").say("Save curr.next (%s) before overwriting it — otherwise the rest of the list is lost.", next)
                    .var("curr", curr).var("next", next)
                    .list(render(concat(reversed, curr, remaining), reversed.size(), reversed.size() - 1)).step();

            reversed.add(0, curr);

            emit.at("flip").say("Point %d backwards at %s.", curr,
                            reversed.size() > 1 ? String.valueOf(reversed.get(1)) : "null")
                    .var("curr", curr)
                    .var("prev", reversed.size() > 1 ? reversed.get(1) : "null")
                    .list(concatRendered(reversed, remaining)).step();

            emit.at("advance").say("Move the window forward: prev = %d, curr = %s.", curr, next)
                    .var("prev", curr).var("curr", next)
                    .list(concatRendered(reversed, remaining)).step();
            stepCount++;
        }

        emit.at("done").say("%d links flipped. The new head is %d.", stepCount, reversed.get(0))
                .var("newHead", reversed.get(0)).var("result", reversed)
                .list(render(reversed, -1, 0)).step();
    }

    private List<Integer> concat(List<Integer> reversed, int curr, List<Integer> remaining) {
        List<Integer> all = new ArrayList<>(reversed);
        all.add(curr);
        all.addAll(remaining);
        return all;
    }

    /** Reversed prefix first, then the untouched remainder. */
    private List<ListNode> concatRendered(List<Integer> reversed, List<Integer> remaining) {
        List<Integer> all = new ArrayList<>(reversed);
        all.addAll(remaining);
        return render(all, reversed.size() < all.size() ? reversed.size() : -1, 0);
    }
}
