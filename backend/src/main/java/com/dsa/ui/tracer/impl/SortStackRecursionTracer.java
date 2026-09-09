package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * No loops, no auxiliary array — only the call stack itself holds elements while they wait.
 * {@code sortStack} pops everything off, recursively sorts what remains, then inserts the
 * popped value back in; {@code insertSorted} pops elements smaller than the value being
 * inserted, recurses one level further down, inserts, then pushes those smaller elements
 * back on top in the same order they came off.
 */
@Component
public class SortStackRecursionTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "sort-stack-recursion";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("stack", FieldType.INT_ARRAY)
                        .label("Stack (bottom to top)")
                        .length(1, 10).values(-99, 99)
                        .defaultValue(List.of(3, 1, 4, 1, 5))
                        .build());
    }

    /** Already sorted — every insertion takes the immediate top-is-not-smaller shortcut. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("stack", List.of(1, 2, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public void sortStack(Deque<Integer> stack) {
                   if (stack.isEmpty()) {
                       // @a baseEmpty
                       return;
                   }
                   int top = stack.pop();
                   // @a popTop
                   sortStack(stack);
                   insertSorted(stack, top);
               }

               private void insertSorted(Deque<Integer> stack, int val) {
                   if (stack.isEmpty() || stack.peek() <= val) {
                       // @a place
                       stack.push(val);
                       return;
                   }
                   int top = stack.pop();
                   // @a makeRoom
                   insertSorted(stack, val);
                   stack.push(top);
                   // @a restore
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] input = in.getIntArray("stack");
        List<Integer> stack = new ArrayList<>();
        for (int v : input) {
            stack.add(v);
        }
        sortStack(stack, emit);

        emit.at("baseEmpty")
                .say("Every element has been popped, sorted, and reinserted. Final order (bottom to top): %s.",
                        stack)
                .var("stack", stack.toString()).stack(stack).step();
    }

    private void sortStack(List<Integer> stack, StepEmitter emit) {
        emit.push("sortStack(size=" + stack.size() + ")");

        if (stack.isEmpty()) {
            emit.at("baseEmpty")
                    .say("Stack is empty - nothing left to sort at this depth.")
                    .var("stack", stack.toString()).stack(stack).step();
            emit.pop();
            return;
        }

        int top = stack.remove(stack.size() - 1);
        emit.at("popTop")
                .say("Pop %d off the top. Recursively sort the remaining %d element(s) before reinserting it.",
                        top, stack.size())
                .var("popped", top).var("stack", stack.toString()).stack(stack).step();

        sortStack(stack, emit);
        insertSorted(stack, top, emit);

        emit.pop();
    }

    private void insertSorted(List<Integer> stack, int val, StepEmitter emit) {
        emit.push("insertSorted(val=" + val + ")");

        if (stack.isEmpty() || stack.get(stack.size() - 1) <= val) {
            stack.add(val);
            emit.at("place")
                    .say(stack.size() == 1
                            ? String.format("Stack is empty - push %d directly.", val)
                            : String.format("Top (%d) is <= %d - push %d here, it belongs on top.",
                                    stack.get(stack.size() - 2), val, val))
                    .var("inserted", val).var("stack", stack.toString()).stack(stack).step();
            emit.pop();
            return;
        }

        int top = stack.remove(stack.size() - 1);
        emit.at("makeRoom")
                .say("Top (%d) is bigger than %d - pop it aside and keep looking deeper for %d's spot.",
                        top, val, val)
                .var("displaced", top).var("stack", stack.toString()).stack(stack).step();

        insertSorted(stack, val, emit);

        stack.add(top);
        emit.at("restore")
                .say("Push %d back on top, now that %d has been inserted below it.", top, val)
                .var("restored", top).var("stack", stack.toString()).stack(stack).step();

        emit.pop();
    }
}
