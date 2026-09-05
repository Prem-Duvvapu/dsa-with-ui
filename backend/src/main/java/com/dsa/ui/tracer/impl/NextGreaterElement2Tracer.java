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
 * The array is circular, so index n-1's next greater element might sit back at index 0.
 * Simulating that by walking the index space twice (from 2n-1 down to 0, taking i % n)
 * primes the monotonic stack with the wrap-around portion before any real answer is
 * recorded - answers only get written once i drops below n, by which point the stack
 * already reflects everything that circularly follows that index.
 */
@Component
public class NextGreaterElement2Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "next-greater-element-2";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Circular array")
                        .help("The array wraps around: the last element's next greater element can be near the start.")
                        .length(1, 16).values(-999, 999)
                        .defaultValue(List.of(1, 2, 1))
                        .build());
    }

    /** Longer, with a strictly-increasing run then a drop - most elements have a real next greater one this time. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3, 4, 3));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] nextGreaterElements(int[] nums) {
                   int n = nums.length;
                   // @a init
                   int[] result = new int[n];
                   Arrays.fill(result, -1);
                   Deque<Integer> stack = new ArrayDeque<>();
                   for (int i = 2 * n - 1; i >= 0; i--) {
                       int idx = i % n;
                       while (!stack.isEmpty() && nums[stack.peek()] <= nums[idx]) {
                           // @a pop
                           stack.pop();
                       }
                       if (i < n) {
                           // @a record
                           result[idx] = stack.isEmpty() ? -1 : nums[stack.peek()];
                       }
                       // @a push
                       stack.push(idx);
                   }
                   // @a done
                   return result;
               }""";
    }

    private List<ArrayElement> numsState(int[] nums, Deque<Integer> stack, int current) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s = i == current ? "current" : stack.contains(i) ? "sorted" : "default";
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int[] result = new int[n];
        java.util.Arrays.fill(result, -1);
        Deque<Integer> stack = new ArrayDeque<>();

        emit.at("init")
                .say("Walk the index space twice (i from %d down to 0, using i mod %d) so "
                        + "the circular wrap-around is primed into the stack before any "
                        + "answer is recorded. All results start at -1.", 2 * n - 1, n)
                .var("n", n)
                .arrayState(numsState(nums, stack, -1)).stack(stack).step();

        for (int i = 2 * n - 1; i >= 0; i--) {
            int idx = i % n;

            while (!stack.isEmpty() && nums[stack.peek()] <= nums[idx]) {
                int popped = stack.pop();
                emit.at("pop")
                        .say("nums[%d]=%d is not greater than nums[%d]=%d - it can never be "
                                + "anyone's next greater element from here on, pop it.",
                                popped, nums[popped], idx, nums[idx])
                        .var("popped", popped)
                        .arrayState(numsState(nums, stack, idx)).stack(stack).step();
            }

            if (i < n) {
                result[idx] = stack.isEmpty() ? -1 : nums[stack.peek()];
                emit.at("record")
                        .say(stack.isEmpty()
                                ? "Nothing greater remains on the stack - index %d has no next greater element.".formatted(idx)
                                : "The nearest survivor on the stack is nums[%d]=%d - that is index %d's next greater element.".formatted(stack.peek(), nums[stack.peek()], idx))
                        .var("idx", idx).var("result", result[idx])
                        .arrayState(numsState(nums, stack, idx)).stack(stack).step();
            }

            stack.push(idx);
            emit.at("push")
                    .say("Push index %d (value %d) onto the stack.", idx, nums[idx])
                    .var("idx", idx)
                    .arrayState(numsState(nums, stack, idx)).stack(stack).step();
        }

        emit.at("done")
                .say("Every index visited twice. Result: %s.", java.util.Arrays.toString(result))
                .var("answer", java.util.Arrays.toString(result))
                .arrayState(numsState(nums, stack, -1)).stack(stack).step();
    }
}
