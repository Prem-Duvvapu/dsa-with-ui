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
 * A monotonic deque of indices, kept strictly decreasing by value, answers every window's
 * maximum without ever rescanning the window: whatever sits at the front is always the
 * largest survivor still inside it, because anything smaller was evicted the moment a
 * bigger later value arrived - it could never win a comparison again.
 */
@Component
public class SlidingWindowMaximumTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "sliding-window-maximum";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .length(1, 12).values(-1000, 1000)
                        .defaultValue(List.of(1, 3, -1, -3, 5, 3, 6, 7))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Window size")
                        .range(1, 12)
                        .defaultValue(3)
                        .build());
    }

    /** A short array whose front index falls out of the window before it is ever beaten by a later value. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(10, 2, -4, -7, 5, -10), "k", 5);
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] maxSlidingWindow(int[] nums, int k) {
                   Deque<Integer> deque = new ArrayDeque<>(); // indices, decreasing values
                   List<Integer> result = new ArrayList<>();

                   for (int i = 0; i < nums.length; i++) {
                       while (!deque.isEmpty() && nums[deque.peekLast()] <= nums[i]) {
                           // @a popBackSmaller
                           deque.pollLast();
                       }
                       // @a pushIndex
                       deque.addLast(i);

                       if (deque.peekFirst() <= i - k) {
                           // @a popFrontOutOfWindow
                           deque.pollFirst();
                       }

                       if (i >= k - 1) {
                           // @a windowMax
                           result.add(nums[deque.peekFirst()]);
                       }
                   }
                   // @a done
                   return result.stream().mapToInt(Integer::intValue).toArray();
               }""";
    }

    private List<ArrayElement> state(int[] nums, Deque<Integer> deque, int current) {
        List<ArrayElement> out = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s = i == current ? "current" : deque.contains(i) ? "sorted" : "visited";
            out.add(new ArrayElement(i, nums[i], s));
        }
        return out;
    }

    private List<Integer> surface(int[] nums, Deque<Integer> deque) {
        List<Integer> out = new ArrayList<>(deque.size());
        for (int idx : deque) {
            out.add(nums[idx]);
        }
        return out;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");
        Deque<Integer> deque = new ArrayDeque<>();
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < nums.length; i++) {
            while (!deque.isEmpty() && nums[deque.peekLast()] <= nums[i]) {
                int evicted = deque.pollLast();
                emit.at("popBackSmaller")
                        .say("nums[%d]=%d can never beat nums[%d]=%d again - evict it from the back.",
                                evicted, nums[evicted], i, nums[i])
                        .arrayState(state(nums, deque, i)).stack(surface(nums, deque)).step();
            }
            deque.addLast(i);
            emit.at("pushIndex")
                    .say("Add index %d (value %d) to the back of the deque.", i, nums[i])
                    .arrayState(state(nums, deque, i)).stack(surface(nums, deque)).step();

            if (deque.peekFirst() <= i - k) {
                int fallenOut = deque.pollFirst();
                emit.at("popFrontOutOfWindow")
                        .say("Index %d has fallen out of the window [%d,%d] - evict it from the front.",
                                fallenOut, i - k + 1, i)
                        .arrayState(state(nums, deque, i)).stack(surface(nums, deque)).step();
            }

            if (i >= k - 1) {
                int max = nums[deque.peekFirst()];
                result.add(max);
                emit.at("windowMax")
                        .say("Window [%d,%d] is complete - its maximum is %d.", i - k + 1, i, max)
                        .var("windowMax", max).var("result", result.toString())
                        .arrayState(state(nums, deque, i)).stack(surface(nums, deque)).step();
            }
        }

        emit.at("done")
                .say("Every window processed. Maximums in order: %s.", result)
                .var("answer", result.toString())
                .arrayState(state(nums, deque, -1)).stack(surface(nums, deque)).step();
    }
}
