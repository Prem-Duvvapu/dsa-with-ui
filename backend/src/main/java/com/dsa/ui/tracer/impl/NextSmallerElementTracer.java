package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Next Smaller Element — mirror of Next Greater Element.
 *
 * <p>Scanned right to left with a monotonic increasing stack (bottom to top). Any value
 * on top that is >= the current element is popped, because it can never be somebody's
 * "next smaller" — the current element is both smaller and closer. Whatever survives
 * on top is the nearest smaller value to the right.
 */
@Component
public class NextSmallerElementTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "next-smaller-element";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Values")
                        .help("For each value, find the first strictly smaller value to its right.")
                        .length(1, 16).values(-999, 999)
                        .defaultValue(List.of(4, 5, 2, 10, 8))
                        .build());
    }

    /** Strictly decreasing: nothing is ever popped — every element's NSE is simply its neighbour. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(5, 4, 3, 2, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] nextSmallerElement(int[] nums) {
                   int n = nums.length;
                   // @a init
                   int[] nse = new int[n];
                   Deque<Integer> stack = new ArrayDeque<>();
                   for (int i = n - 1; i >= 0; i--) {
                       while (!stack.isEmpty() && stack.peek() >= nums[i]) {
                           // @a pop
                           stack.pop();
                       }
                       // @a record
                       nse[i] = stack.isEmpty() ? -1 : stack.peek();
                       // @a push
                       stack.push(nums[i]);
                   }
                   // @a done
                   return nse;
               }""";
    }

    private List<ArrayElement> numsState(int[] nums, int current, int settledFrom) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s = i == current ? "current" : i > settledFrom ? "sorted" : "default";
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int[] nse = new int[n];
        Arrays.fill(nse, -1);
        Deque<Integer> stack = new ArrayDeque<>();

        emit.at("init")
                .say("Walk %s from the right with a monotonic increasing stack. Pop anything >= the current value.", Arrays.toString(nums))
                .var("n", n)
                .arrayState(numsState(nums, -1, n)).stack(stack).step();

        for (int i = n - 1; i >= 0; i--) {
            while (!stack.isEmpty() && stack.peek() >= nums[i]) {
                int popped = stack.pop();
                emit.at("pop")
                        .say("%d on top is not smaller than nums[%d]=%d — pop it.",
                                popped, i, nums[i])
                        .var("i", i).var("popped", popped)
                        .arrayState(numsState(nums, i, i)).stack(stack).step();
            }

            nse[i] = stack.isEmpty() ? -1 : stack.peek();
            emit.at("record")
                    .say(stack.isEmpty()
                            ? String.format("Stack empty — no value to the right of nums[%d]=%d is smaller. Record -1.", i, nums[i])
                            : String.format("Top is %d — the nearest smaller value to the right of nums[%d]=%d. Record %d.",
                                    stack.peek(), i, nums[i], nse[i]))
                    .var("i", i).var("nse[i]", nse[i])
                    .arrayState(numsState(nums, i, i)).stack(stack).step();

            stack.push(nums[i]);
            emit.at("push")
                    .say("Push nums[%d]=%d onto the stack.", i, nums[i])
                    .var("i", i).var("pushed", nums[i])
                    .arrayState(numsState(nums, i, i - 1)).stack(stack).step();
        }

        emit.at("done")
                .say("Every index answered. Next smaller elements: %s.", Arrays.toString(nse))
                .var("answer", Arrays.toString(nse))
                .arrayState(numsState(nums, -1, -1)).stack(stack).step();
    }
}
