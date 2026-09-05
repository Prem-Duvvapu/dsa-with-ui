package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Reverses every full run of k consecutive nodes in place. A group only gets flipped once
 * it is confirmed to actually hold k nodes - checking that count first is what keeps a
 * trailing partial group (fewer than k nodes left at the very end) from being reversed on
 * its own, which would scramble the list's own final order instead of leaving it untouched.
 */
@Component
public class ReverseLlGroupKTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "reverse-ll-group-k";
    }

    @Override
    public DsType dsType() {
        return DsType.LINKED_LIST;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("values", FieldType.LINKED_LIST)
                        .label("List values")
                        .help("Node values from head to tail.")
                        .length(1, 15).values(-999, 999)
                        .defaultValue(List.of(1, 2, 3, 4, 5))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Group size")
                        .range(1, 15)
                        .defaultValue(2)
                        .build());
    }

    /** A length that is an exact multiple of k: no trailing partial group this time. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("values", List.of(1, 2, 3, 4, 5, 6), "k", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public ListNode reverseKGroup(ListNode head, int k) {
                   ListNode node = head;
                   List<Integer> result = new ArrayList<>();

                   while (node != null) {
                       // @a countGroup
                       int count = 0;
                       ListNode check = node;
                       while (check != null && count < k) {
                           check = check.next;
                           count++;
                       }

                       if (count == k) {
                           // @a reverseWithinGroup
                           ListNode prev = check;
                           ListNode curr = node;
                           for (int i = 0; i < k; i++) {
                               ListNode next = curr.next;
                               curr.next = prev;
                               prev = curr;
                               curr = next;
                           }
                           // @a groupComplete
                           node = check;
                       } else {
                           // @a partialGroupLeftAsIs
                           node = null;
                       }
                   }
                   // @a done
                   return head;
               }""";
    }

    private List<ListNode> render(List<Integer> order, int groupStart, int groupEnd) {
        List<ListNode> nodes = new ArrayList<>(order.size());
        for (int i = 0; i < order.size(); i++) {
            String state = (i >= groupStart && i <= groupEnd) ? "curr" : "default";
            Integer nextId = i + 1 < order.size() ? i + 1 : null;
            nodes.add(new ListNode(i, String.valueOf(order.get(i)), nextId, null, state));
        }
        return nodes;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] values = in.getLinkedList("values");
        int k = in.getInt("k");
        int n = values.length;
        List<Integer> result = new ArrayList<>();

        int i = 0;
        while (i < n) {
            boolean fullGroup = i + k <= n;
            emit.at("countGroup")
                    .say(fullGroup
                            ? "Starting at index %d: %d more nodes remain - a full group of %d."
                            : "Starting at index %d: only %d nodes remain - fewer than %d, this is a partial group.",
                            i, Math.min(k, n - i), k)
                    .var("groupStart", i).var("count", Math.min(k, n - i))
                    .list(render(concat(result, values, i, n), i, Math.min(i + k, n) - 1)).step();

            if (fullGroup) {
                List<Integer> group = new ArrayList<>();
                for (int j = i; j < i + k; j++) {
                    group.add(values[j]);
                }
                Collections.reverse(group);
                emit.at("reverseWithinGroup")
                        .say("Reverse this group of %d in place: %s.", k, group)
                        .var("reversedGroup", group.toString())
                        .list(render(concat(result, values, i, n), i, i + k - 1, group)).step();

                result.addAll(group);
                emit.at("groupComplete")
                        .say("Group complete. Result so far: %s.", result)
                        .var("result", result.toString())
                        .list(render(concat(result, values, i + k, n), -1, -1)).step();
                i += k;
            } else {
                for (int j = i; j < n; j++) {
                    result.add(values[j]);
                }
                emit.at("partialGroupLeftAsIs")
                        .say("Fewer than %d nodes left - leave this trailing group in its "
                                + "original order. Result so far: %s.", k, result)
                        .var("result", result.toString())
                        .list(render(result, i, n - 1)).step();
                i = n;
            }
        }

        emit.at("done")
                .say("Every group processed. Final list: %s.", result)
                .var("answer", result.toString())
                .list(render(result, -1, -1)).step();
    }

    private List<Integer> concat(List<Integer> done, int[] values, int fromIndex, int n) {
        List<Integer> all = new ArrayList<>(done);
        for (int j = fromIndex; j < n; j++) {
            all.add(values[j]);
        }
        return all;
    }

    private List<ListNode> render(List<Integer> order, int groupStart, int groupEnd, List<Integer> reversedPreview) {
        List<ListNode> nodes = new ArrayList<>(order.size());
        int previewIdx = 0;
        for (int i = 0; i < order.size(); i++) {
            int value = (i >= groupStart && i <= groupEnd) ? reversedPreview.get(previewIdx++) : order.get(i);
            String state = (i >= groupStart && i <= groupEnd) ? "curr" : "default";
            Integer nextId = i + 1 < order.size() ? i + 1 : null;
            nodes.add(new ListNode(i, String.valueOf(value), nextId, null, state));
        }
        return nodes;
    }
}
