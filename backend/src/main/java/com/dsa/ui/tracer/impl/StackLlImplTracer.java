package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implement Stack using a singly linked list, with the head as the top.
 *
 * <p>Choosing the head is the entire design decision. A singly linked list can only insert
 * and delete in O(1) where it already holds a reference, and it holds one to the head — so
 * pushing means creating a node whose {@code next} is the old head, and popping means moving
 * {@code head} one link along. Making the TAIL the top would cost a full traversal per
 * operation, and there is no capacity to overflow, which is the difference from
 * stack-array-impl's fixed buffer.
 *
 * <p>Traced as {@link DsType#LINKED_LIST} because the chain of nodes IS the structure being
 * taught here; the array version already covers "a stack drawn as a pile of slots".
 */
@Component
public class StackLlImplTracer implements AlgorithmTracer {

    private static final String OP = "(push -?\\d{1,3}|pop|peek)";

    @Override
    public String id() {
        return "stack-ll-impl";
    }

    @Override
    public DsType dsType() {
        return DsType.LINKED_LIST;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("operations", FieldType.STRING)
                        .label("Stack operations")
                        .help("Semicolon-separated, each \"push V\", \"pop\" or \"peek\".")
                        .length(1, 200)
                        .constraint("pattern", OP + "(;" + OP + ")*")
                        .constraint("patternHint",
                                "Semicolon-separated ops, each \"push V\", \"pop\" or \"peek\", "
                                        + "e.g. \"push 10;push 20;pop\".")
                        .defaultValue("push 10;push 20;push 30;peek;pop;pop;push 40;pop;pop")
                        .build());
    }

    /**
     * Never grows past two nodes and starts by popping an empty list, so it reaches the
     * null-head branches the default never does, and ends with the chain still standing
     * rather than fully unwound.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("operations", "pop;peek;push 5;pop;push 6;push 7;peek");
    }

    @Override
    public String annotatedCode() {
        return """
               class LinkedStack {
                   // @a init
                   Node head = null;
                   int size = 0;

                   void push(int value) {
                       // @a push
                       Node node = new Node(value);
                       node.next = head;
                       head = node;
                       size++;
                   }

                   int pop() {
                       if (head == null) {
                           // @a popEmpty
                           throw new NoSuchElementException();
                       }
                       // @a pop
                       int value = head.value;
                       head = head.next;
                       size--;
                       return value;
                   }

                   int peek() {
                       if (head == null) {
                           // @a peekEmpty
                           throw new NoSuchElementException();
                       }
                       // @a peek
                       return head.value;
                   }

                   int size() {
                       // @a done
                       return size;
                   }
               }""";
    }

    /**
     * The chain from the head, which is the top. Node ids are allocation order rather than
     * position, so a node keeps its identity as the head moves past it.
     */
    private List<ListNode> chain(List<int[]> nodes, int highlightId) {
        List<ListNode> out = new ArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            int id = nodes.get(i)[0];
            int value = nodes.get(i)[1];
            Integer nextId = i + 1 < nodes.size() ? nodes.get(i + 1)[0] : null;
            out.add(new ListNode(id, String.valueOf(value), nextId, null,
                    id == highlightId ? "active" : "default"));
        }
        return out;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String operations = in.getString("operations");
        // Head first: index 0 is the node `head` points at.
        List<int[]> nodes = new ArrayList<>();
        int nextId = 0;

        emit.at("init")
                .say("head = null: an empty stack. There is no capacity here — the structure "
                        + "grows a node at a time, so nothing can overflow.")
                .var("head", "null").var("size", 0)
                .list(chain(nodes, -1)).step();

        for (String rawOp : operations.split(";")) {
            String[] parts = rawOp.trim().split("\\s+");
            switch (parts[0]) {
                case "push" -> {
                    int value = Integer.parseInt(parts[1]);
                    int id = nextId++;
                    Integer oldHead = nodes.isEmpty() ? null : nodes.get(0)[0];
                    nodes.add(0, new int[]{id, value});
                    emit.at("push")
                            .say(oldHead == null
                                    ? String.format("push(%d): a new node, its next pointing at the "
                                            + "old head — which was null. head now points at it.", value)
                                    : String.format("push(%d): a new node, its next pointing at the old "
                                            + "head (%d). head moves to the new node, so the whole "
                                            + "chain hangs off it untouched.",
                                            value, nodes.get(1)[1]))
                            .var("pushed", value).var("head", value).var("size", nodes.size())
                            .list(chain(nodes, id)).step();
                }
                case "pop" -> {
                    if (nodes.isEmpty()) {
                        emit.at("popEmpty")
                                .say("pop(): head is null, so there is no node to unlink.")
                                .var("head", "null").var("size", 0)
                                .list(chain(nodes, -1)).step();
                    } else {
                        int[] removed = nodes.remove(0);
                        emit.at("pop")
                                .say("pop(): read %d out of the head node, then move head one "
                                        + "link along to %s. The old node is unreachable now.",
                                        removed[1],
                                        nodes.isEmpty() ? "null" : String.valueOf(nodes.get(0)[1]))
                                .var("popped", removed[1])
                                .var("head", nodes.isEmpty() ? "null" : nodes.get(0)[1])
                                .var("size", nodes.size())
                                .list(chain(nodes, nodes.isEmpty() ? -1 : nodes.get(0)[0])).step();
                    }
                }
                default -> {
                    if (nodes.isEmpty()) {
                        emit.at("peekEmpty")
                                .say("peek(): head is null, so there is no top value to read.")
                                .var("head", "null").var("size", 0)
                                .list(chain(nodes, -1)).step();
                    } else {
                        emit.at("peek")
                                .say("peek(): the head node holds %d. Read it and leave every "
                                        + "pointer alone.", nodes.get(0)[1])
                                .var("peeked", nodes.get(0)[1]).var("size", nodes.size())
                                .list(chain(nodes, nodes.get(0)[0])).step();
                    }
                }
            }
        }

        emit.at("done")
                .say("Sequence finished with %d node(s) on the chain, head first.", nodes.size())
                .var("size", nodes.size())
                .var("head", nodes.isEmpty() ? "null" : nodes.get(0)[1])
                .list(chain(nodes, -1)).step();
    }
}
