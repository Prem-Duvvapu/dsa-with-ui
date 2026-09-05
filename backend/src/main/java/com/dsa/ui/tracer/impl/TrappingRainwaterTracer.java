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
 * A monotonic decreasing stack of indices: whenever a taller bar arrives, everything
 * shorter than it that's still on the stack has a left wall (the next thing under it) and
 * a right wall (the arriving bar) - so trapped water above that popped bar can be computed
 * immediately, without ever needing a second pass over prefix/suffix maxima.
 */
@Component
public class TrappingRainwaterTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "trapping-rainwater";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("height", FieldType.INT_ARRAY)
                        .label("Elevation map")
                        .help("Bar heights; water pools between taller bars on either side.")
                        .length(1, 24).values(0, 100)
                        .defaultValue(List.of(0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1))
                        .build());
    }

    /** A different profile: the pools are wider and deeper, no zero-height dips. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("height", List.of(4, 2, 0, 3, 2, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public int trap(int[] height) {
                   // @a init
                   Deque<Integer> stack = new ArrayDeque<>();
                   int water = 0;
                   for (int i = 0; i < height.length; i++) {
                       while (!stack.isEmpty() && height[i] > height[stack.peek()]) {
                           // @a pop
                           int top = stack.pop();
                           if (stack.isEmpty()) {
                               // @a noLeftWall
                               break;
                           }
                           int left = stack.peek();
                           int distance = i - left - 1;
                           int boundedHeight = Math.min(height[i], height[left]) - height[top];
                           // @a addWater
                           water += distance * boundedHeight;
                       }
                       // @a push
                       stack.push(i);
                   }
                   // @a done
                   return water;
               }""";
    }

    private List<ArrayElement> barState(int[] height, Deque<Integer> stack, int current, int popped) {
        List<ArrayElement> state = new ArrayList<>(height.length);
        for (int i = 0; i < height.length; i++) {
            String s = i == current ? "current" : i == popped ? "swapping"
                    : stack.contains(i) ? "sorted" : "default";
            state.add(new ArrayElement(i, height[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] height = in.getIntArray("height");
        Deque<Integer> stack = new ArrayDeque<>();
        int water = 0;

        emit.at("init")
                .say("An empty stack of indices, kept so heights never increase bottom to "
                        + "top. Running trapped water starts at 0.")
                .var("water", water)
                .arrayState(barState(height, stack, -1, -1)).stack(stack).step();

        for (int i = 0; i < height.length; i++) {
            while (!stack.isEmpty() && height[i] > height[stack.peek()]) {
                int top = stack.pop();
                emit.at("pop")
                        .say("height[%d]=%d is taller than height[%d]=%d - pop the shorter "
                                + "bar; it will form the floor of a pool.", i, height[i], top,
                                height[top])
                        .var("i", i).var("popped", top)
                        .arrayState(barState(height, stack, i, top)).stack(stack).step();

                if (stack.isEmpty()) {
                    emit.at("noLeftWall")
                            .say("Nothing remains under index %d - there is no left wall, "
                                    + "so no water can pool above it.", top)
                            .arrayState(barState(height, stack, i, top)).stack(stack).step();
                    break;
                }

                int left = stack.peek();
                int distance = i - left - 1;
                int boundedHeight = Math.min(height[i], height[left]) - height[top];
                water += distance * boundedHeight;
                emit.at("addWater")
                        .say("Pool between the wall at %d (height %d) and the wall at %d "
                                + "(height %d), floor at %d (height %d): width %d x depth %d "
                                + "= %d units. Running total %d.",
                                left, height[left], i, height[i], top, height[top], distance,
                                boundedHeight, distance * boundedHeight, water)
                        .var("water", water)
                        .arrayState(barState(height, stack, i, top)).stack(stack).step();
            }

            stack.push(i);
            emit.at("push")
                    .say("Push index %d (height %d) onto the stack.", i, height[i])
                    .var("i", i)
                    .arrayState(barState(height, stack, i, -1)).stack(stack).step();
        }

        emit.at("done")
                .say("Every bar processed. Total trapped water: %d units.", water)
                .var("answer", water)
                .arrayState(barState(height, stack, -1, -1)).stack(stack).step();
    }
}
