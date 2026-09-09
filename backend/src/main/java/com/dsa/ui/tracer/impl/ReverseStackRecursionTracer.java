package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The call stack does the work an auxiliary stack would otherwise need: {@code reverse} pops
 * every element off (the last one popped is what was originally at the bottom), then
 * {@code insertAtBottom} threads each one back in underneath everything already reinserted —
 * so the very first element popped (originally the top) ends up truly at the bottom.
 */
@Component
public class ReverseStackRecursionTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "reverse-stack-recursion";
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
                        .defaultValue(List.of(1, 2, 3, 4))
                        .build());
    }

    /** A single element — reversing it is a no-op, exercising the base case with nothing to reinsert. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("stack", List.of(9));
    }

    @Override
    public String annotatedCode() {
        return """
               public void reverse(Deque<Integer> stack) {
                   if (stack.isEmpty()) {
                       // @a baseEmpty
                       return;
                   }
                   int top = stack.pop();
                   // @a popTop
                   reverse(stack);
                   insertAtBottom(stack, top);
               }

               private void insertAtBottom(Deque<Integer> stack, int val) {
                   if (stack.isEmpty()) {
                       // @a place
                       stack.push(val);
                       return;
                   }
                   int top = stack.pop();
                   // @a makeRoom
                   insertAtBottom(stack, val);
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
        reverse(stack, emit);

        emit.at("baseEmpty")
                .say("Every element popped and reinserted at the bottom. Final order (bottom to top): %s.", stack)
                .var("stack", stack.toString()).stack(stack).step();
    }

    private void reverse(List<Integer> stack, StepEmitter emit) {
        emit.push("reverse(size=" + stack.size() + ")");

        if (stack.isEmpty()) {
            emit.at("baseEmpty")
                    .say("Stack is empty - nothing left to reverse at this depth.")
                    .var("stack", stack.toString()).stack(stack).step();
            emit.pop();
            return;
        }

        int top = stack.remove(stack.size() - 1);
        emit.at("popTop")
                .say("Pop %d off the top. Reverse the remaining %d element(s) first, then insert %d at the bottom.",
                        top, stack.size(), top)
                .var("popped", top).var("stack", stack.toString()).stack(stack).step();

        reverse(stack, emit);
        insertAtBottom(stack, top, emit);

        emit.pop();
    }

    private void insertAtBottom(List<Integer> stack, int val, StepEmitter emit) {
        emit.push("insertAtBottom(val=" + val + ")");

        if (stack.isEmpty()) {
            stack.add(val);
            emit.at("place")
                    .say("Stack is empty - %d becomes the new bottom.", val)
                    .var("inserted", val).var("stack", stack.toString()).stack(stack).step();
            emit.pop();
            return;
        }

        int top = stack.remove(stack.size() - 1);
        emit.at("makeRoom")
                .say("Pop %d aside so %d can go in underneath it.", top, val)
                .var("displaced", top).var("stack", stack.toString()).stack(stack).step();

        insertAtBottom(stack, val, emit);

        stack.add(top);
        emit.at("restore")
                .say("Push %d back on top, now that %d sits below it.", top, val)
                .var("restored", top).var("stack", stack.toString()).stack(stack).step();

        emit.pop();
    }
}
