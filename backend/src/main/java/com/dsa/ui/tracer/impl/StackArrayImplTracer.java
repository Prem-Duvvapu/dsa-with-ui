package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implement Stack using a fixed-size array.
 *
 * <p>The whole data structure is one integer: {@code top}, the index of the last slot
 * written. Push writes at {@code top + 1}; pop reads at {@code top} and steps back. Nothing
 * is erased on pop — the value stays in the array as garbage above {@code top}, which is why
 * the emitted stack shows only slots 0..top and the raw buffer is carried alongside it.
 *
 * <p>A fixed array is why overflow exists at all: the linked-list implementation
 * (stack-ll-impl) has the same interface and no capacity, and that contrast is the point of
 * having both problems.
 *
 * <p>Like lru-cache, a stateful object with no single before/after answer is fitted to the
 * one-input-one-trace contract by encoding the whole call sequence as one STRING.
 */
@Component
public class StackArrayImplTracer implements AlgorithmTracer {

    private static final String OP = "(push -?\\d{1,3}|pop|peek)";

    @Override
    public String id() {
        return "stack-array-impl";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("capacity", FieldType.INT)
                        .label("Array capacity")
                        .help("Slots in the backing array. Pushing past it overflows.")
                        .range(1, 8)
                        .defaultValue(4)
                        .build(),
                InputField.of("operations", FieldType.STRING)
                        .label("Operations")
                        .help("Semicolon-separated, each \"push V\", \"pop\" or \"peek\".")
                        .length(1, 200)
                        .constraint("pattern", OP + "(;" + OP + ")*")
                        .constraint("patternHint",
                                "Semicolon-separated ops, each \"push V\", \"pop\" or \"peek\", "
                                        + "e.g. \"push 10;push 20;pop\".")
                        .defaultValue("push 10;push 20;push 30;peek;pop;pop;push 40;push 50;push 60;push 70")
                        .build());
    }

    /**
     * Half the capacity and a sequence that starts empty and drains back to empty before
     * refilling, so it reaches both empty-stack branches — peek and pop with
     * {@code top == -1} — which the default never does. The default ends full and
     * overflowing; this one ends with room to spare.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "capacity", 2,
                "operations", "peek;push 7;pop;pop;push 8;push 9;peek");
    }

    @Override
    public String annotatedCode() {
        return """
               class ArrayStack {
                   int[] slots;
                   int top = -1;

                   ArrayStack(int capacity) {
                       // @a init
                       slots = new int[capacity];
                   }

                   void push(int value) {
                       if (top == slots.length - 1) {
                           // @a overflow
                           throw new StackOverflowError();
                       }
                       // @a push
                       slots[++top] = value;
                   }

                   int pop() {
                       if (top == -1) {
                           // @a underflow
                           throw new NoSuchElementException();
                       }
                       // @a pop
                       return slots[top--];
                   }

                   int peek() {
                       if (top == -1) {
                           // @a peekEmpty
                           throw new NoSuchElementException();
                       }
                       // @a peek
                       return slots[top];
                   }

                   int size() {
                       // @a done
                       return top + 1;
                   }
               }""";
    }

    /** Slots 0..top are live; anything above is stale data pop never bothered to erase. */
    private List<ArrayElement> buffer(int[] slots, int top, int highlight) {
        List<ArrayElement> state = new ArrayList<>(slots.length);
        for (int i = 0; i < slots.length; i++) {
            String s = i == highlight ? "current" : i <= top ? "sorted" : "visited";
            state.add(new ArrayElement(i, slots[i], s));
        }
        return state;
    }

    /** The live stack, top first — the order StepEmitter.stack() expects. */
    private List<String> live(int[] slots, int top) {
        List<String> out = new ArrayList<>();
        for (int i = top; i >= 0; i--) {
            out.add(String.valueOf(slots[i]));
        }
        return out;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int capacity = in.getInt("capacity");
        String operations = in.getString("operations");
        int[] slots = new int[capacity];
        int top = -1;

        emit.at("init")
                .say("A %d-slot array and one integer, top = -1, meaning the stack is empty. "
                        + "Every operation is just arithmetic on top.", capacity)
                .var("capacity", capacity).var("top", top)
                .arrayState(buffer(slots, top, -1)).stack(live(slots, top)).step();

        for (String rawOp : operations.split(";")) {
            String[] parts = rawOp.trim().split("\\s+");
            switch (parts[0]) {
                case "push" -> {
                    int value = Integer.parseInt(parts[1]);
                    if (top == capacity - 1) {
                        emit.at("overflow")
                                .say("push(%d): top is %d, the last slot — the array is full at "
                                        + "capacity %d. Overflow; %d is rejected.",
                                        value, top, capacity, value)
                                .var("rejected", value).var("top", top).var("size", top + 1)
                                .arrayState(buffer(slots, top, top)).stack(live(slots, top)).step();
                    } else {
                        slots[++top] = value;
                        emit.at("push")
                                .say("push(%d): step top to %d and write %d into slot %d.",
                                        value, top, value, top)
                                .var("pushed", value).var("top", top).var("size", top + 1)
                                .arrayState(buffer(slots, top, top)).stack(live(slots, top)).step();
                    }
                }
                case "pop" -> {
                    if (top == -1) {
                        emit.at("underflow")
                                .say("pop(): top is -1, so there is nothing to remove. Underflow.")
                                .var("top", top).var("size", 0)
                                .arrayState(buffer(slots, top, -1)).stack(live(slots, top)).step();
                    } else {
                        int popped = slots[top];
                        top--;
                        emit.at("pop")
                                .say("pop(): read slot %d to get %d, then step top back to %d. "
                                        + "%d is still sitting in the array, but it is above top "
                                        + "now, so it no longer counts.",
                                        top + 1, popped, top, popped)
                                .var("popped", popped).var("top", top).var("size", top + 1)
                                .arrayState(buffer(slots, top, top + 1)).stack(live(slots, top)).step();
                    }
                }
                default -> {
                    if (top == -1) {
                        emit.at("peekEmpty")
                                .say("peek(): top is -1, so there is no top element to read.")
                                .var("top", top).var("size", 0)
                                .arrayState(buffer(slots, top, -1)).stack(live(slots, top)).step();
                    } else {
                        emit.at("peek")
                                .say("peek(): slot %d holds %d. Read it without moving top.",
                                        top, slots[top])
                                .var("peeked", slots[top]).var("top", top).var("size", top + 1)
                                .arrayState(buffer(slots, top, top)).stack(live(slots, top)).step();
                    }
                }
            }
        }

        emit.at("done")
                .say("Sequence finished. top = %d, so %d value(s) remain in the %d-slot array.",
                        top, top + 1, capacity)
                .var("top", top).var("size", top + 1)
                .arrayState(buffer(slots, top, -1)).stack(live(slots, top)).step();
    }
}
