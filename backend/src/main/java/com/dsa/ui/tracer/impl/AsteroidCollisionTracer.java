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
 * Positive values move right, negative move left, and a collision can only ever happen
 * between a right-mover already on the stack and a left-mover arriving next - two movers
 * headed the same direction never meet. So each arriving asteroid either destroys survivors
 * off the top of the stack one at a time, gets destroyed itself, or (moving right, or
 * nothing in its way) simply joins the stack.
 */
@Component
public class AsteroidCollisionTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "asteroid-collision";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("asteroids", FieldType.INT_ARRAY)
                        .label("Asteroids")
                        .help("Positive size moves right, negative moves left. Same size means both explode.")
                        .length(1, 16).values(-100, 100)
                        .defaultValue(List.of(10, 2, -5))
                        .build());
    }

    /** Equal and opposite: both explode, leaving nothing - a case the default never reaches. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("asteroids", List.of(8, -8));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] asteroidCollision(int[] asteroids) {
                   // @a init
                   Deque<Integer> stack = new ArrayDeque<>();
                   for (int a : asteroids) {
                       boolean alive = true;
                       while (alive && a < 0 && !stack.isEmpty() && stack.peek() > 0) {
                           if (stack.peek() < -a) {
                               // @a topExplodes
                               stack.pop();
                           } else if (stack.peek() == -a) {
                               // @a bothExplode
                               stack.pop();
                               alive = false;
                           } else {
                               // @a currentExplodes
                               alive = false;
                           }
                       }
                       if (alive) {
                           // @a push
                           stack.push(a);
                       }
                   }
                   // @a done
                   return toArray(stack);
               }""";
    }

    private List<ArrayElement> state(int[] asteroids, Deque<Integer> stackIndices, int current) {
        List<ArrayElement> out = new ArrayList<>(asteroids.length);
        for (int i = 0; i < asteroids.length; i++) {
            String s = i == current ? "current" : stackIndices.contains(i) ? "sorted" : "visited";
            out.add(new ArrayElement(i, asteroids[i], s));
        }
        return out;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] asteroids = in.getIntArray("asteroids");
        Deque<Integer> stackIndices = new ArrayDeque<>();

        emit.at("init")
                .say("An empty stack of surviving right-movers. Each new asteroid can only "
                        + "collide with whatever survives on top of the stack.")
                .arrayState(state(asteroids, stackIndices, -1)).stack(surface(asteroids, stackIndices)).step();

        for (int i = 0; i < asteroids.length; i++) {
            int a = asteroids[i];
            boolean alive = true;

            while (alive && a < 0 && !stackIndices.isEmpty() && asteroids[stackIndices.peek()] > 0) {
                int topIdx = stackIndices.peek();
                int top = asteroids[topIdx];

                if (top < -a) {
                    stackIndices.pop();
                    emit.at("topExplodes")
                            .say("Asteroid %d (size %d, moving right) is smaller than the "
                                    + "incoming %d - it explodes. The incoming asteroid keeps going.",
                                    top, top, a)
                            .var("exploded", top)
                            .arrayState(state(asteroids, stackIndices, i)).stack(surface(asteroids, stackIndices)).step();
                } else if (top == -a) {
                    stackIndices.pop();
                    alive = false;
                    emit.at("bothExplode")
                            .say("Asteroid %d and the incoming %d are equal and opposite - "
                                    + "both explode.", top, a)
                            .arrayState(state(asteroids, stackIndices, i)).stack(surface(asteroids, stackIndices)).step();
                } else {
                    alive = false;
                    emit.at("currentExplodes")
                            .say("Asteroid %d (size %d, moving right) is larger than the "
                                    + "incoming %d - the incoming asteroid explodes instead.",
                                    top, top, a)
                            .arrayState(state(asteroids, stackIndices, i)).stack(surface(asteroids, stackIndices)).step();
                }
            }

            if (alive) {
                stackIndices.push(i);
                emit.at("push")
                        .say("Asteroid %d survives every collision in its way - add it to "
                                + "the stack.", a)
                        .var("value", a)
                        .arrayState(state(asteroids, stackIndices, i)).stack(surface(asteroids, stackIndices)).step();
            }
        }

        List<Integer> survivors = new ArrayList<>(stackIndices.size());
        for (int idx : stackIndices) {
            survivors.add(0, asteroids[idx]);
        }
        emit.at("done")
                .say("Every asteroid processed. Survivors, left to right: %s.", survivors)
                .var("answer", survivors.toString())
                .arrayState(state(asteroids, stackIndices, -1)).stack(surface(asteroids, stackIndices)).step();
    }

    private List<Integer> surface(int[] asteroids, Deque<Integer> stackIndices) {
        List<Integer> values = new ArrayList<>(stackIndices.size());
        for (int idx : stackIndices) {
            values.add(asteroids[idx]);
        }
        return values;
    }
}
