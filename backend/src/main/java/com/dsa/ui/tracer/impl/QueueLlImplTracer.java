package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implement Queue using a linked list: {@code head} is the front, {@code tail} is the rear.
 *
 * <p>A queue touches both ends, and a singly linked list is O(1) only where it holds a
 * reference — so it holds two. Serving from the head is a pointer move; arriving at the tail
 * is a single {@code tail.next = node}. Keeping the tail pointer is the whole point: without
 * it, every enqueue would walk the chain to find the end.
 *
 * <p>The two pointers make emptying the queue a special case in both directions. The first
 * enqueue has no tail to append to, so it sets BOTH pointers; the last dequeue leaves
 * {@code tail} dangling at a node that is no longer reachable, so it must be cleared too — a
 * stale tail would make the next enqueue append onto a detached node and lose it.
 */
@Component
public class QueueLlImplTracer implements AlgorithmTracer {

    private static final String OP = "(enqueue -?\\d{1,3}|dequeue|front)";

    @Override
    public String id() {
        return "queue-ll-impl";
    }

    @Override
    public DsType dsType() {
        return DsType.LINKED_LIST;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("operations", FieldType.STRING)
                        .label("Queue operations")
                        .help("Semicolon-separated, each \"enqueue V\", \"dequeue\" or \"front\".")
                        .length(1, 200)
                        .constraint("pattern", OP + "(;" + OP + ")*")
                        .constraint("patternHint",
                                "Semicolon-separated ops, each \"enqueue V\", \"dequeue\" or "
                                        + "\"front\", e.g. \"enqueue 10;dequeue;front\".")
                        .defaultValue("enqueue 10;enqueue 20;enqueue 30;front;dequeue;dequeue;"
                                + "enqueue 40;dequeue;dequeue")
                        .build());
    }

    /**
     * Reads and serves an empty queue before anything arrives, and rebuilds it afterwards, so
     * it exercises the null-head branches and re-runs the first-node case a second time —
     * neither of which the default reaches. It also ends with the chain still standing.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("operations", "dequeue;front;enqueue 7;dequeue;enqueue 8;enqueue 9;front");
    }

    @Override
    public String annotatedCode() {
        return """
               class LinkedQueue {
                   // @a init
                   Node head = null, tail = null;
                   int size = 0;

                   void enqueue(int value) {
                       Node node = new Node(value);
                       if (tail == null) {
                           // @a first
                           head = tail = node;
                       } else {
                           // @a append
                           tail.next = node;
                           tail = node;
                       }
                       size++;
                   }

                   int dequeue() {
                       if (head == null) {
                           // @a underflow
                           throw new NoSuchElementException();
                       }
                       // @a dequeue
                       int value = head.value;
                       head = head.next;
                       if (head == null) {
                           // @a clearTail
                           tail = null;
                       }
                       size--;
                       return value;
                   }

                   int front() {
                       if (head == null) {
                           // @a frontEmpty
                           throw new NoSuchElementException();
                       }
                       // @a front
                       return head.value;
                   }

                   int size() {
                       // @a done
                       return size;
                   }
               }""";
    }

    /** The chain from head (front) to tail (rear); ids are allocation order, not position. */
    private List<ListNode> chain(List<int[]> nodes, int highlightId) {
        List<ListNode> out = new ArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            int id = nodes.get(i)[0];
            Integer nextId = i + 1 < nodes.size() ? nodes.get(i + 1)[0] : null;
            out.add(new ListNode(id, String.valueOf(nodes.get(i)[1]), nextId, null,
                    id == highlightId ? "active" : "default"));
        }
        return out;
    }

    private static String describe(List<int[]> nodes, int index) {
        return index < 0 || index >= nodes.size() ? "null" : String.valueOf(nodes.get(index)[1]);
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String operations = in.getString("operations");
        // Front first: index 0 is head, the last index is tail.
        List<int[]> nodes = new ArrayList<>();
        int nextId = 0;

        emit.at("init")
                .say("head = tail = null. Two pointers, one at each end, so both serving and "
                        + "arriving stay O(1) with no traversal.")
                .var("head", "null").var("tail", "null").var("size", 0)
                .list(chain(nodes, -1)).step();

        for (String rawOp : operations.split(";")) {
            String[] parts = rawOp.trim().split("\\s+");
            switch (parts[0]) {
                case "enqueue" -> {
                    int value = Integer.parseInt(parts[1]);
                    int id = nextId++;
                    boolean first = nodes.isEmpty();
                    String previousTail = describe(nodes, nodes.size() - 1);
                    nodes.add(new int[]{id, value});
                    emit.at(first ? "first" : "append")
                            .say(first
                                    ? String.format("enqueue(%d): the queue is empty, so there is no "
                                            + "tail to append to. This node becomes BOTH head and "
                                            + "tail.", value)
                                    : String.format("enqueue(%d): link it off the current tail (%s) "
                                            + "and move tail to it. head does not move — the front "
                                            + "of the queue is unaffected by an arrival.",
                                            value, previousTail))
                            .var("enqueued", value).var("head", describe(nodes, 0))
                            .var("tail", value).var("size", nodes.size())
                            .list(chain(nodes, id)).step();
                }
                case "dequeue" -> {
                    if (nodes.isEmpty()) {
                        emit.at("underflow")
                                .say("dequeue(): head is null, so the queue is empty. Nothing to serve.")
                                .var("head", "null").var("tail", "null").var("size", 0)
                                .list(chain(nodes, -1)).step();
                    } else {
                        int[] served = nodes.remove(0);
                        emit.at("dequeue")
                                .say("dequeue(): the head node holds %d. Serve it and move head "
                                        + "along to %s.", served[1], describe(nodes, 0))
                                .var("dequeued", served[1]).var("head", describe(nodes, 0))
                                .var("tail", describe(nodes, nodes.size() - 1)).var("size", nodes.size())
                                .list(chain(nodes, nodes.isEmpty() ? -1 : nodes.get(0)[0])).step();

                        if (nodes.isEmpty()) {
                            emit.at("clearTail")
                                    .say("head is now null, which means that was the last node — "
                                            + "so tail has to be cleared too. Leaving it pointing "
                                            + "at the served node would make the next enqueue "
                                            + "append onto a node nothing can reach.")
                                    .var("head", "null").var("tail", "null").var("size", 0)
                                    .list(chain(nodes, -1)).step();
                        }
                    }
                }
                default -> {
                    if (nodes.isEmpty()) {
                        emit.at("frontEmpty")
                                .say("front(): head is null, so there is no front value to read.")
                                .var("head", "null").var("size", 0)
                                .list(chain(nodes, -1)).step();
                    } else {
                        emit.at("front")
                                .say("front(): the head node holds %d — the oldest value still "
                                        + "queued. Read it without unlinking anything.",
                                        nodes.get(0)[1])
                                .var("peeked", nodes.get(0)[1]).var("size", nodes.size())
                                .list(chain(nodes, nodes.get(0)[0])).step();
                    }
                }
            }
        }

        emit.at("done")
                .say("Sequence finished with %d node(s) between head and tail.", nodes.size())
                .var("head", describe(nodes, 0))
                .var("tail", describe(nodes, nodes.size() - 1))
                .var("size", nodes.size())
                .list(chain(nodes, -1)).step();
    }
}
