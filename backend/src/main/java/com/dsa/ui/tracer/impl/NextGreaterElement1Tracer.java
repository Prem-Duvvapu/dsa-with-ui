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
 * Next Greater Element (LeetCode 496) on a single array.
 *
 * <p>Scanned right to left so that everything already seen is everything to the right. The
 * stack holds only the values that could still be somebody's answer, kept strictly
 * decreasing from bottom to top: when nums[i] arrives, any value at or below it is
 * permanently useless — anything further left that reaches past nums[i] would meet nums[i]
 * first — so those get popped, and whatever survives on top is the nearest greater value to
 * the right.
 *
 * <p>Sibling problems, deliberately different: next-greater-element-2 walks the index space
 * twice for a circular array, and next-smaller-element keeps the stack increasing instead.
 */
@Component
public class NextGreaterElement1Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "next-greater-element-1";
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
                        .help("For each value, the first strictly greater value to its right.")
                        .length(1, 16).values(-999, 999)
                        .defaultValue(List.of(4, 5, 2, 10, 8))
                        .build());
    }

    /**
     * Strictly increasing, so nothing is ever popped: every element's next greater one is
     * simply its right-hand neighbour, and only the last has none. The default pops twice.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] nextGreaterElement(int[] nums) {
                   int n = nums.length;
                   // @a init
                   int[] nge = new int[n];
                   Deque<Integer> stack = new ArrayDeque<>();
                   for (int i = n - 1; i >= 0; i--) {
                       while (!stack.isEmpty() && stack.peek() <= nums[i]) {
                           // @a pop
                           stack.pop();
                       }
                       // @a record
                       nge[i] = stack.isEmpty() ? -1 : stack.peek();
                       // @a push
                       stack.push(nums[i]);
                   }
                   // @a done
                   return nge;
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
        int[] nge = new int[n];
        Arrays.fill(nge, -1);
        Deque<Integer> stack = new ArrayDeque<>();

        emit.at("init")
                .say("Walk %s from the right. Everything already scanned is everything to the "
                        + "right of the current index, and the stack keeps only the values "
                        + "that could still be an answer.", Arrays.toString(nums))
                .var("n", n)
                .arrayState(numsState(nums, -1, n)).stack(stack).step();

        for (int i = n - 1; i >= 0; i--) {
            while (!stack.isEmpty() && stack.peek() <= nums[i]) {
                int popped = stack.pop();
                emit.at("pop")
                        .say("%d on top is not greater than nums[%d]=%d, so nothing further "
                                + "left can ever pick it — nums[%d] blocks the view. Pop it.",
                                popped, i, nums[i], i)
                        .var("i", i).var("popped", popped)
                        .arrayState(numsState(nums, i, i)).stack(stack).step();
            }

            nge[i] = stack.isEmpty() ? -1 : stack.peek();
            emit.at("record")
                    .say(stack.isEmpty()
                            ? String.format("Nothing is left on the stack, so no value to the right of "
                                    + "nums[%d]=%d is greater. Record -1.", i, nums[i])
                            : String.format("The survivor on top is %d — the nearest value to the right "
                                    + "of nums[%d]=%d that beats it. Record %d.",
                                    stack.peek(), i, nums[i], nge[i]))
                    .var("i", i).var("nge[i]", nge[i])
                    .arrayState(numsState(nums, i, i)).stack(stack).step();

            stack.push(nums[i]);
            emit.at("push")
                    .say("Push nums[%d]=%d — it is now the candidate for everything further left.",
                            i, nums[i])
                    .var("i", i).var("pushed", nums[i])
                    .arrayState(numsState(nums, i, i - 1)).stack(stack).step();
        }

        emit.at("done")
                .say("Every index answered, right to left. Next greater elements: %s.",
                        Arrays.toString(nge))
                .var("answer", Arrays.toString(nge))
                .arrayState(numsState(nums, -1, -1)).stack(stack).step();
    }
}
