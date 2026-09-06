package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Implement Queue using Stacks (LeetCode 232), the amortised two-stack version.
 *
 * <p>One stack reverses an order; two stacks reverse it back. Arrivals pile onto {@code in},
 * newest on top — the wrong end to serve from. Pouring {@code in} into {@code out} one pop
 * at a time flips it, so {@code out}'s top is the OLDEST element, which is exactly what a
 * queue serves. The pour only happens when {@code out} runs dry, so each element is moved
 * at most once and both operations are O(1) amortised even though one dequeue can be O(n).
 *
 * <p>The step's structure is the LOGICAL queue, front to back, with each element labelled by
 * the stack physically holding it — {@code 3·out}, {@code 4·in}. The pour is then visible as
 * a whole run of labels flipping at once, which is the only moment any real work happens.
 *
 * <p>Mirror of stack-queue-impl, which makes the opposite trade: there push is O(n) and pop
 * is O(1).
 */
@Component
public class QueueStackImplTracer implements AlgorithmTracer {

    private static final String OP = "(enqueue -?\\d{1,3}|dequeue|peek)";

    @Override
    public String id() {
        return "queue-stack-impl";
    }

    @Override
    public DsType dsType() {
        return DsType.QUEUE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("operations", FieldType.STRING)
                        .label("Queue operations")
                        .help("Semicolon-separated, each \"enqueue V\", \"dequeue\" or \"peek\".")
                        .length(1, 200)
                        .constraint("pattern", OP + "(;" + OP + ")*")
                        .constraint("patternHint",
                                "Semicolon-separated ops, each \"enqueue V\", \"dequeue\" or "
                                        + "\"peek\", e.g. \"enqueue 1;enqueue 2;dequeue\".")
                        .defaultValue("enqueue 1;enqueue 2;enqueue 3;dequeue;enqueue 4;dequeue;"
                                + "peek;dequeue;dequeue;dequeue")
                        .build());
    }

    /**
     * Reads before anything has arrived, then interleaves reads with arrivals so {@code out}
     * is refilled while {@code in} is still being added to — three separate pours of one or
     * two elements each, against the default's single pour of three. It also ends with the
     * queue non-empty, where the default ends underflowing.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("operations",
                "peek;enqueue 7;peek;enqueue 8;dequeue;enqueue 9;dequeue;enqueue 6");
    }

    @Override
    public String annotatedCode() {
        return """
               class StackQueue {
                   // @a init
                   Deque<Integer> in = new ArrayDeque<>();
                   Deque<Integer> out = new ArrayDeque<>();

                   void enqueue(int value) {
                       // @a enqueue
                       in.push(value);
                   }

                   private void pourIfNeeded() {
                       if (!out.isEmpty()) return;
                       while (!in.isEmpty()) {
                           // @a pour
                           out.push(in.pop());
                       }
                   }

                   int dequeue() {
                       pourIfNeeded();
                       if (out.isEmpty()) {
                           // @a underflow
                           throw new NoSuchElementException();
                       }
                       // @a dequeue
                       return out.pop();
                   }

                   int peek() {
                       pourIfNeeded();
                       if (out.isEmpty()) {
                           // @a peekEmpty
                           throw new NoSuchElementException();
                       }
                       // @a peek
                       return out.peek();
                   }

                   int size() {
                       // @a done
                       return in.size() + out.size();
                   }
               }""";
    }

    /**
     * The logical queue, front to back, each element labelled with the stack holding it.
     *
     * <p>Neither stack's own iteration order can be used directly, and which end of the queue
     * {@code out} holds depends on WHEN you look. Settled, {@code out} holds the OLDEST
     * elements — the front. Half-way through a pour it holds the NEWEST, because the pour
     * takes them off the top of {@code in} first. Deriving the row from the stacks would
     * therefore print the queue backwards for exactly as long as the pour runs, which is the
     * only stretch of the animation anyone is watching closely.
     *
     * <p>So the order comes from {@code queue}, which is maintained directly and is always
     * correct, and only the labels are derived from the split.
     */
    private List<String> view(List<Integer> queue, int outSize, boolean midPour) {
        int n = queue.size();
        List<String> row = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            boolean held = midPour ? i >= n - outSize : i < outSize;
            row.add(queue.get(i) + (held ? "·out" : "·in"));
        }
        return row;
    }

    /**
     * Pours {@code in} into {@code out}, one emitted step per move, but only when {@code out}
     * has run dry — pouring while {@code out} still holds anything would interleave newer
     * arrivals in front of older ones. Returns whether any element moved.
     */
    private boolean pour(Deque<Integer> in, Deque<Integer> out, List<Integer> queue,
                         StepEmitter emit, String caller) {
        if (!out.isEmpty()) {
            return false;
        }
        int total = in.size();
        int moves = 0;
        while (!in.isEmpty()) {
            int moved = in.pop();
            out.push(moved);
            moves++;
            emit.at("pour")
                    .say("%s: out is empty, so pour in into out. Move %d of %d — %d comes off "
                            + "in and goes onto out, %s.",
                            caller, moves, total, moved,
                            in.isEmpty()
                                    ? "ending on top, which is where the oldest element belongs"
                                    : "to be buried by the older values still on in")
                    .var("moved", moved).var("in", in.size()).var("out", out.size())
                    .queue(view(queue, out.size(), true)).step();
        }
        return moves > 0;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String operations = in.getString("operations");
        Deque<Integer> inStack = new ArrayDeque<>();
        Deque<Integer> outStack = new ArrayDeque<>();
        // The queue itself, front to back. Maintained alongside the two stacks purely so the
        // emitted row is right mid-pour, when neither stack's order is the queue's.
        List<Integer> queue = new ArrayList<>();

        emit.at("init")
                .say("Two empty stacks. Arrivals land on in (newest on top, the wrong end to "
                        + "serve); out holds them already reversed, so its top is the oldest. "
                        + "Each element is labelled with the stack currently holding it.")
                .var("in", 0).var("out", 0)
                .queue(view(queue, outStack.size(), false)).step();

        for (String rawOp : operations.split(";")) {
            String[] parts = rawOp.trim().split("\\s+");
            switch (parts[0]) {
                case "enqueue" -> {
                    int value = Integer.parseInt(parts[1]);
                    inStack.push(value);
                    queue.add(value);
                    emit.at("enqueue")
                            .say("enqueue(%d): push onto in. It joins the back of the queue, "
                                    + "and out is untouched — arrivals never cost more than "
                                    + "one push.", value)
                            .var("enqueued", value).var("in", inStack.size()).var("out", outStack.size())
                            .queue(view(queue, outStack.size(), false)).step();
                }
                case "dequeue" -> {
                    boolean poured = pour(inStack, outStack, queue, emit, "dequeue()");
                    if (outStack.isEmpty()) {
                        emit.at("underflow")
                                .say("dequeue(): both stacks are empty, so the queue is empty. "
                                        + "Nothing to serve.")
                                .var("in", 0).var("out", 0)
                                .queue(view(queue, outStack.size(), false)).step();
                    } else {
                        int served = outStack.pop();
                        queue.remove(0);
                        emit.at("dequeue")
                                .say(poured
                                        ? String.format("dequeue(): out's top is now %d, the oldest "
                                                + "element queued. Pop it — the pour that just ran "
                                                + "is what put it there.", served)
                                        : String.format("dequeue(): out already held the reversed "
                                                + "older values, so no pour was needed. Its top is "
                                                + "%d — pop it in O(1).", served))
                                .var("dequeued", served).var("in", inStack.size()).var("out", outStack.size())
                                .queue(view(queue, outStack.size(), false)).step();
                    }
                }
                default -> {
                    pour(inStack, outStack, queue, emit, "peek()");
                    if (outStack.isEmpty()) {
                        emit.at("peekEmpty")
                                .say("peek(): both stacks are empty, so there is no front to read.")
                                .var("in", 0).var("out", 0)
                                .queue(view(queue, outStack.size(), false)).step();
                    } else {
                        emit.at("peek")
                                .say("peek(): out's top is %d — the front of the queue, read "
                                        + "without removing it.", outStack.peek())
                                .var("front", outStack.peek())
                                .var("in", inStack.size()).var("out", outStack.size())
                                .queue(view(queue, outStack.size(), false)).step();
                    }
                }
            }
        }

        emit.at("done")
                .say("Sequence finished. %d value(s) still queued, front first: %s.",
                        queue.size(), view(queue, outStack.size(), false))
                .var("in", inStack.size()).var("out", outStack.size())
                .queue(view(queue, outStack.size(), false)).step();
    }
}
