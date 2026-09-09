package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.tracer.AlgorithmTracer;
import com.dsa.ui.tracer.FieldType;
import com.dsa.ui.tracer.InputField;
import com.dsa.ui.tracer.StepEmitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared identity-preserving model used by the Linked List topic tracers. */
abstract class LinkedListTopicTracer implements AlgorithmTracer {

    @Override
    public final DsType dsType() {
        return DsType.LINKED_LIST;
    }

    protected final InputField listField(String name, String label, List<Integer> defaults) {
        return listField(name, label, defaults, -999, 999, false);
    }

    protected final InputField listField(
            String name,
            String label,
            List<Integer> defaults,
            int minValue,
            int maxValue,
            boolean sorted) {
        InputField.Builder field = InputField.of(name, FieldType.LINKED_LIST)
                .label(label)
                .help("Values are read from head to tail.")
                .length(1, 16)
                .values(minValue, maxValue)
                .defaultValue(defaults);
        if (sorted) {
            field.sorted();
        }
        return field.build();
    }

    protected final InputField intField(String name, String label, int min, int max, int value) {
        return InputField.of(name, FieldType.INT)
                .label(label)
                .range(min, max)
                .defaultValue(value)
                .build();
    }

    protected final TraceListNode[] singly(int[] values) {
        return build(values, false, 0);
    }

    protected final TraceListNode[] singly(int[] values, int firstId) {
        return build(values, false, firstId);
    }

    protected final TraceListNode[] doubly(int[] values) {
        return build(values, true, 0);
    }

    protected final TraceListNode[] doubly(int[] values, int firstId) {
        return build(values, true, firstId);
    }

    private TraceListNode[] build(int[] values, boolean doubly, int firstId) {
        TraceListNode[] nodes = new TraceListNode[values.length];
        for (int i = 0; i < values.length; i++) {
            nodes[i] = new TraceListNode(firstId + i, values[i]);
        }
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].next = i + 1 < nodes.length ? nodes[i + 1] : null;
            nodes[i].prev = doubly && i > 0 ? nodes[i - 1] : null;
        }
        return nodes;
    }

    protected final List<TraceListNode> all(TraceListNode[]... groups) {
        List<TraceListNode> nodes = new ArrayList<>();
        for (TraceListNode[] group : groups) {
            nodes.addAll(Arrays.asList(group));
        }
        return nodes;
    }

    protected final void traceInput(
            StepEmitter emit,
            String anchor,
            String label,
            Collection<TraceListNode> nodes) {
        int index = 0;
        for (TraceListNode node : nodes) {
            emit.at(anchor)
                    .say("Read %s node %d with value %d and preserve identity #%d.",
                            label, index, node.value, node.id)
                    .var("index", index)
                    .var("value", node.value)
                    .list(snapshot(nodes, states(node, "active")))
                    .step();
            index++;
        }
    }

    protected final List<ListNode> snapshot(
            Collection<TraceListNode> nodes,
            Map<Integer, String> states) {
        List<ListNode> out = new ArrayList<>(nodes.size());
        for (TraceListNode node : nodes) {
            out.add(new ListNode(
                    node.id,
                    String.valueOf(node.value),
                    node.next == null ? null : node.next.id,
                    node.prev == null ? null : node.prev.id,
                    states.getOrDefault(node.id, "default")));
        }
        return out;
    }

    protected final List<ListNode> snapshot(Collection<TraceListNode> nodes) {
        return snapshot(nodes, Map.of());
    }

    protected final Map<Integer, String> states(Object... nodeAndState) {
        Map<Integer, String> states = new LinkedHashMap<>();
        for (int i = 0; i + 1 < nodeAndState.length; i += 2) {
            TraceListNode node = (TraceListNode) nodeAndState[i];
            if (node != null) {
                states.put(node.id, String.valueOf(nodeAndState[i + 1]));
            }
        }
        return states;
    }

    protected final List<Integer> values(TraceListNode head, int limit) {
        List<Integer> values = new ArrayList<>();
        TraceListNode current = head;
        while (current != null && values.size() < limit) {
            values.add(current.value);
            current = current.next;
        }
        return values;
    }

    protected final void relink(List<TraceListNode> order, boolean doubly) {
        for (int i = 0; i < order.size(); i++) {
            TraceListNode node = order.get(i);
            node.next = i + 1 < order.size() ? order.get(i + 1) : null;
            node.prev = doubly && i > 0 ? order.get(i - 1) : null;
        }
    }

    protected final String value(TraceListNode node) {
        return node == null ? "null" : String.valueOf(node.value);
    }
}

/** Mutable algorithm node whose id never changes, even when its links do. */
final class TraceListNode {
    final int id;
    int value;
    TraceListNode next;
    TraceListNode prev;

    TraceListNode(int id, int value) {
        this.id = id;
        this.value = value;
    }
}
