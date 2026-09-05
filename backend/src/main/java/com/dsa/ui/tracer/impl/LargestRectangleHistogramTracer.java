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
 * A monotonic increasing stack of indices: a bar is only popped once something shorter
 * arrives, and at that moment its rectangle's width is exactly known - it spans from just
 * past whatever is left on the stack (the nearest bar still shorter) to the arriving index
 * (the nearest bar to the right that's shorter). A sentinel height of 0 past the end
 * flushes every bar still on the stack when the array runs out.
 */
@Component
public class LargestRectangleHistogramTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "largest-rectangle-histogram";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("heights", FieldType.INT_ARRAY)
                        .label("Histogram bar heights")
                        .help("Width-1 bars side by side; find the largest all-rectangle area.")
                        .length(1, 20).values(0, 100)
                        .defaultValue(List.of(2, 1, 5, 6, 2, 3))
                        .build());
    }

    /** Just two bars: the best rectangle is the shorter one spanning both, not the taller one alone. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("heights", List.of(2, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public int largestRectangleArea(int[] heights) {
                   // @a init
                   int n = heights.length;
                   Deque<Integer> stack = new ArrayDeque<>();
                   int maxArea = 0;
                   for (int i = 0; i <= n; i++) {
                       int currentHeight = (i == n) ? 0 : heights[i];
                       while (!stack.isEmpty() && currentHeight < heights[stack.peek()]) {
                           // @a pop
                           int height = heights[stack.pop()];
                           int width = stack.isEmpty() ? i : i - stack.peek() - 1;
                           int area = height * width;
                           if (area > maxArea) {
                               // @a newMax
                               maxArea = area;
                           }
                       }
                       if (i < n) {
                           // @a push
                           stack.push(i);
                       }
                   }
                   // @a done
                   return maxArea;
               }""";
    }

    private List<ArrayElement> barState(int[] heights, Deque<Integer> stack, int current, int popped) {
        List<ArrayElement> state = new ArrayList<>(heights.length);
        for (int i = 0; i < heights.length; i++) {
            String s = i == current ? "current" : i == popped ? "swapping"
                    : stack.contains(i) ? "sorted" : "default";
            state.add(new ArrayElement(i, heights[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] heights = in.getIntArray("heights");
        int n = heights.length;
        Deque<Integer> stack = new ArrayDeque<>();
        int maxArea = 0;

        emit.at("init")
                .say("An empty stack of indices, kept so bar heights never decrease bottom "
                        + "to top. A sentinel height of 0 past the last bar (index %d) will "
                        + "flush whatever remains.", n)
                .var("maxArea", maxArea)
                .arrayState(barState(heights, stack, -1, -1)).stack(stack).step();

        for (int i = 0; i <= n; i++) {
            int currentHeight = (i == n) ? 0 : heights[i];

            while (!stack.isEmpty() && currentHeight < heights[stack.peek()]) {
                int poppedIndex = stack.pop();
                int barHeight = heights[poppedIndex];
                int width = stack.isEmpty() ? i : i - stack.peek() - 1;
                int area = barHeight * width;

                emit.at("pop")
                        .say("%s is shorter than height[%d]=%d - pop it. With nothing "
                                + "shorter in the way between %s and here, its rectangle "
                                + "spans width %d: area %d x %d = %d.",
                                i == n ? "End of the array" : "height[" + i + "]=" + currentHeight,
                                poppedIndex, barHeight,
                                stack.isEmpty() ? "the start" : "index " + stack.peek(),
                                width, barHeight, width, area)
                        .var("popped", poppedIndex).var("width", width).var("area", area)
                        .arrayState(barState(heights, stack, i == n ? -1 : i, poppedIndex))
                        .stack(stack).step();

                if (area > maxArea) {
                    maxArea = area;
                    emit.at("newMax")
                            .say("%d beats the best so far - new maximum area %d.", area, maxArea)
                            .var("maxArea", maxArea)
                            .arrayState(barState(heights, stack, i == n ? -1 : i, poppedIndex))
                            .stack(stack).step();
                }
            }

            if (i < n) {
                stack.push(i);
                emit.at("push")
                        .say("Push index %d (height %d) onto the stack.", i, heights[i])
                        .var("i", i)
                        .arrayState(barState(heights, stack, i, -1)).stack(stack).step();
            }
        }

        emit.at("done")
                .say("Every bar processed, including the sentinel flush. Largest rectangle "
                        + "area: %d.", maxArea)
                .var("answer", maxArea)
                .arrayState(barState(heights, stack, -1, -1)).stack(stack).step();
    }
}
