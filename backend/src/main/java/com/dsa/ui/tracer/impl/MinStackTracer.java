package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * getMin() in O(1) needs the running minimum available without rescanning the stack, so a
 * second, parallel stack tracks it: every push also pushes "the minimum so far, including
 * this value" onto it, and every pop discards both together - popping the value can never
 * un-teach the min stack what the minimum was one level down, because that level's own
 * entry is still sitting right there underneath.
 */
@Component
public class MinStackTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "min-stack";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("values", FieldType.INT_ARRAY)
                        .label("Values to push, in order")
                        .help("Pushed one at a time, then popped back off one at a time.")
                        .length(1, 12).values(-1000, 1000)
                        .defaultValue(List.of(3, 5, 1, 4, 1, 2))
                        .build());
    }

    /** Monotonically increasing: the minimum is set once on the first push and never changes again. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("values", List.of(1, 2, 3, 4, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               class MinStack {
                   Deque<Integer> stack = new ArrayDeque<>();
                   Deque<Integer> minStack = new ArrayDeque<>();

                   void push(int val) {
                       stack.push(val);
                       if (minStack.isEmpty() || val <= minStack.peek()) {
                           // @a newMin
                           minStack.push(val);
                       } else {
                           // @a sameMin
                           minStack.push(minStack.peek());
                       }
                   }

                   void pop() {
                       // @a pop
                       stack.pop();
                       minStack.pop();
                       // @a done
                   }

                   int getMin() {
                       return minStack.peek();
                   }
               }""";
    }

    private List<ArrayElement> state(int[] values, int size, int current) {
        List<ArrayElement> out = new ArrayList<>(values.length);
        for (int i = 0; i < values.length; i++) {
            String s = i == current ? "current" : i < size ? "sorted" : "visited";
            out.add(new ArrayElement(i, values[i], s));
        }
        return out;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] values = in.getIntArray("values");
        Deque<Integer> stack = new ArrayDeque<>();
        Deque<Integer> minStack = new ArrayDeque<>();

        for (int i = 0; i < values.length; i++) {
            int val = values[i];
            stack.push(val);
            if (minStack.isEmpty() || val <= minStack.peek()) {
                minStack.push(val);
                emit.at("newMin")
                        .say("Push %d. It is now <= the running minimum - the new minimum is %d.",
                                val, val)
                        .var("pushed", val).var("min", val)
                        .arrayState(state(values, i + 1, i)).stack(new ArrayList<>(stack)).step();
            } else {
                minStack.push(minStack.peek());
                emit.at("sameMin")
                        .say("Push %d. The running minimum stays %d.", val, minStack.peek())
                        .var("pushed", val).var("min", minStack.peek())
                        .arrayState(state(values, i + 1, i)).stack(new ArrayList<>(stack)).step();
            }
        }

        for (int i = values.length - 1; i >= 0; i--) {
            int popped = stack.pop();
            minStack.pop();
            Integer newMin = minStack.peek();
            emit.at("pop")
                    .say(newMin == null
                            ? "Pop %d. The stack is now empty."
                            : "Pop %d. The running minimum is now %d - the level underneath.",
                            popped, newMin)
                    .var("popped", popped).var("min", newMin == null ? "none" : newMin)
                    .arrayState(state(values, i, i)).stack(new ArrayList<>(stack)).step();
        }

        emit.at("done")
                .say("Every value pushed and popped back off. The stack is empty.")
                .arrayState(state(values, 0, -1)).stack(new ArrayList<>(stack)).step();
    }
}
