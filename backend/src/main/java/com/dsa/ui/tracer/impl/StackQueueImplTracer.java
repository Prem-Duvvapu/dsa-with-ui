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
 * Implement Stack using Queue (LeetCode 225), the single-queue variant.
 *
 * <p>A queue serves its oldest element and a stack serves its newest, so the trick is to
 * make the newest element the oldest: push enqueues at the back and then rotates the queue
 * {@code size - 1} times, moving everything that was already there around behind it. The
 * queue's FRONT is therefore always the stack's TOP, and pop and top become plain queue
 * operations with no further work.
 *
 * <p>Push pays for that: it is O(n), while pop is O(1). The two-stack queue
 * (queue-stack-impl) makes the opposite trade, amortising the cost onto the reads instead.
 *
 * <p>Traced as {@link DsType#QUEUE} on purpose: the structure on screen is the queue, and
 * the rotation — take from the left, put back on the right — is what the animation is for.
 */
@Component
public class StackQueueImplTracer implements AlgorithmTracer {

    private static final String OP = "(push -?\\d{1,3}|pop|top)";

    @Override
    public String id() {
        return "stack-queue-impl";
    }

    @Override
    public DsType dsType() {
        return DsType.QUEUE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("operations", FieldType.STRING)
                        .label("Stack operations")
                        .help("Semicolon-separated, each \"push V\", \"pop\" or \"top\".")
                        .length(1, 200)
                        .constraint("pattern", OP + "(;" + OP + ")*")
                        .constraint("patternHint",
                                "Semicolon-separated ops, each \"push V\", \"pop\" or \"top\", "
                                        + "e.g. \"push 1;push 2;pop\".")
                        .defaultValue("push 1;push 2;push 3;top;pop;pop;push 4;pop;pop")
                        .build());
    }

    /**
     * Starts by popping an empty stack and never lets the queue grow past two, so the
     * rotation runs at most once per push — where the default builds to three and rotates
     * twice. Different length, different depth, and it reaches the empty branches.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("operations", "pop;push 9;top;pop;top;push 8;push 7;top");
    }

    @Override
    public String annotatedCode() {
        return """
               class QueueStack {
                   // @a init
                   Deque<Integer> q = new ArrayDeque<>();

                   void push(int value) {
                       // @a enqueue
                       q.addLast(value);
                       for (int i = q.size() - 1; i > 0; i--) {
                           // @a rotate
                           q.addLast(q.pollFirst());
                       }
                   }

                   int pop() {
                       if (q.isEmpty()) {
                           // @a popEmpty
                           throw new NoSuchElementException();
                       }
                       // @a pop
                       return q.pollFirst();
                   }

                   int top() {
                       if (q.isEmpty()) {
                           // @a topEmpty
                           throw new NoSuchElementException();
                       }
                       // @a top
                       return q.peekFirst();
                   }

                   int size() {
                       // @a done
                       return q.size();
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String operations = in.getString("operations");
        Deque<Integer> q = new ArrayDeque<>();

        emit.at("init")
                .say("One empty queue, and an invariant to maintain: its front must always be "
                        + "the most recently pushed value. Push is what pays to keep that true.")
                .var("size", 0)
                .queue(snapshot(q)).step();

        for (String rawOp : operations.split(";")) {
            String[] parts = rawOp.trim().split("\\s+");
            switch (parts[0]) {
                case "push" -> {
                    int value = Integer.parseInt(parts[1]);
                    q.addLast(value);
                    emit.at("enqueue")
                            .say("push(%d): enqueue %d at the BACK, where a queue puts new "
                                    + "arrivals. It is last in line, which is the opposite of "
                                    + "where a stack wants it.", value, value)
                            .var("pushed", value).var("size", q.size())
                            .queue(snapshot(q)).step();

                    for (int i = q.size() - 1; i > 0; i--) {
                        int moved = q.pollFirst();
                        q.addLast(moved);
                        emit.at("rotate")
                                .say("Rotate %d of %d: move %d from the front around to the "
                                        + "back. %d is now %s.",
                                        q.size() - i, q.size() - 1, moved, value,
                                        i == 1 ? "at the front, so the queue's front is the stack's top"
                                                : "one place closer to the front")
                                .var("moved", moved).var("rotationsLeft", i - 1)
                                .queue(snapshot(q)).step();
                    }
                }
                case "pop" -> {
                    if (q.isEmpty()) {
                        emit.at("popEmpty")
                                .say("pop(): the queue is empty, so the stack is empty. Nothing to remove.")
                                .var("size", 0)
                                .queue(snapshot(q)).step();
                    } else {
                        int popped = q.pollFirst();
                        emit.at("pop")
                                .say("pop(): the front holds %d, and push already arranged for "
                                        + "the front to be the most recently pushed value. "
                                        + "Serve it — one dequeue, no rearranging.", popped)
                                .var("popped", popped).var("size", q.size())
                                .queue(snapshot(q)).step();
                    }
                }
                default -> {
                    if (q.isEmpty()) {
                        emit.at("topEmpty")
                                .say("top(): the queue is empty, so there is no top to read.")
                                .var("size", 0)
                                .queue(snapshot(q)).step();
                    } else {
                        emit.at("top")
                                .say("top(): the front is %d — the stack's top, read without "
                                        + "removing it.", q.peekFirst())
                                .var("top", q.peekFirst()).var("size", q.size())
                                .queue(snapshot(q)).step();
                    }
                }
            }
        }

        emit.at("done")
                .say("Sequence finished. The queue holds %d value(s), front first: %s.",
                        q.size(), snapshot(q))
                .var("size", q.size())
                .queue(snapshot(q)).step();
    }

    /** Front first, which for this implementation is also top-of-stack first. */
    private List<String> snapshot(Deque<Integer> q) {
        List<String> out = new ArrayList<>(q.size());
        for (int value : q) {
            out.add(String.valueOf(value));
        }
        return out;
    }
}
