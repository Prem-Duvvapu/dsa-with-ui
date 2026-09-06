package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Fewest multiplications turning {@code start} into {@code end}, where each step multiplies
 * the current value by one of the given numbers and keeps the result modulo 100000.
 *
 * <p>There is no graph to look at here — the graph is implicit, one vertex per residue
 * 0..99999 and one edge per multiplier — which is exactly why the queue IS the picture.
 * Because every edge costs the same single multiplication, plain BFS is optimal: the first
 * time a value is produced, it is produced in the fewest possible steps, so a value already
 * seen is never worth queueing again. That "already seen" rule is what keeps a space of
 * 100000 residues from exploding, and it is narrated rather than assumed.
 */
@Component
public class MinMultiplicationsReachEndTracer implements AlgorithmTracer {

    private static final int MODULUS = 100_000;

    @Override
    public String id() {
        return "min-multiplications-reach-end";
    }

    @Override
    public DsType dsType() {
        return DsType.QUEUE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("arr", FieldType.INT_ARRAY)
                        .label("Multipliers")
                        .help("Each step multiplies the current value by one of these, modulo 100000.")
                        .length(1, 6)
                        .values(1, 10_000)
                        .defaultValue(List.of(3, 4, 65))
                        .build(),
                InputField.of("start", FieldType.INT)
                        .label("Start value")
                        .range(0, 99_999)
                        .defaultValue(7)
                        .build(),
                InputField.of("end", FieldType.INT)
                        .label("Target value")
                        .range(0, 99_999)
                        .defaultValue(66_175)
                        .build());
    }

    /**
     * A single multiplier whose powers cycle without ever producing the target, so the
     * queue drains and the answer is -1 - the branch the reachable default never enters.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "arr", List.of(5),
                "start", 5,
                "end", 7);
    }

    @Override
    public String annotatedCode() {
        return """
               public int minimumMultiplications(int[] arr, int start, int end) {
                   // @a init
                   int mod = 100000;
                   if (start == end) return 0;
                   int[] steps = new int[mod];
                   Arrays.fill(steps, -1);
                   steps[start] = 0;
                   Queue<Integer> queue = new ArrayDeque<>();
                   queue.add(start);

                   while (!queue.isEmpty()) {
                       // @a dequeue
                       int value = queue.poll();
                       for (int multiplier : arr) {
                           int next = (int) ((long) value * multiplier % mod);
                           if (next == end) {
                               // @a reached
                               return steps[value] + 1;
                           }
                           if (steps[next] == -1) {
                               // @a enqueue
                               steps[next] = steps[value] + 1;
                               queue.add(next);
                           } else {
                               // @a seen
                               continue;
                           }
                       }
                   }
                   // @a exhausted
                   return -1;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("arr");
        int start = in.getInt("start");
        int end = in.getInt("end");

        int[] steps = new int[MODULUS];
        Arrays.fill(steps, -1);
        steps[start] = 0;

        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(start);

        if (start == end) {
            emit.at("init").say("Start %d already equals the target, so zero multiplications are needed.",
                            start)
                    .var("start", start).var("end", end).var("answer", 0)
                    .queue(labels(queue, steps)).step();
            return;
        }

        emit.at("init").say(
                        "Breadth-first search over the values 0..%d. Seed the queue with %d at depth 0; "
                                + "every multiplier costs one step, so the first time a value appears it "
                                + "appears at its minimum step count. Target: %d.",
                        MODULUS - 1, start, end)
                .var("start", start).var("end", end).var("multipliers", Arrays.toString(arr))
                .var("discovered", 1)
                .queue(labels(queue, steps)).step();

        int discovered = 1;

        while (!queue.isEmpty()) {
            int value = queue.poll();
            int depth = steps[value];

            emit.at("dequeue").say(
                            "Dequeue %d, reached in %d multiplication(s). Try every multiplier on it.",
                            value, depth)
                    .var("value", value).var("steps", depth).var("discovered", discovered)
                    .queue(labels(queue, steps)).step();

            for (int multiplier : arr) {
                int next = (int) ((long) value * multiplier % MODULUS);

                if (next == end) {
                    emit.at("reached").say(
                                    "%d x %d = %d (mod %d) - that is the target. It was produced from a value "
                                            + "at depth %d, so the answer is %d multiplication(s).",
                                    value, multiplier, next, MODULUS, depth, depth + 1)
                            .var("value", value).var("multiplier", multiplier).var("product", next)
                            .var("answer", depth + 1).var("discovered", discovered)
                            .queue(labels(queue, steps)).step();
                    return;
                }

                if (steps[next] == -1) {
                    steps[next] = depth + 1;
                    queue.add(next);
                    discovered++;
                    emit.at("enqueue").say(
                                    "%d x %d = %d (mod %d), a value never produced before. Record it at depth "
                                            + "%d and queue it.",
                                    value, multiplier, next, MODULUS, steps[next])
                            .var("value", value).var("multiplier", multiplier).var("product", next)
                            .var("steps", steps[next]).var("discovered", discovered)
                            .queue(labels(queue, steps)).step();
                } else {
                    emit.at("seen").say(
                                    "%d x %d = %d (mod %d), but %d was already produced at depth %d - no later "
                                            + "route to it can be shorter, so it is not queued again.",
                                    value, multiplier, next, MODULUS, next, steps[next])
                            .var("value", value).var("multiplier", multiplier).var("product", next)
                            .var("discovered", discovered)
                            .queue(labels(queue, steps)).step();
                }
            }
        }

        emit.at("exhausted").say(
                        "The queue is empty after producing %d distinct value(s), and %d was never among "
                                + "them. Multiplying %d by %s can never reach it: the answer is -1.",
                        discovered, end, start, Arrays.toString(arr))
                .var("discovered", discovered).var("answer", -1)
                .queue(labels(queue, steps)).step();
    }

    /** Queue contents as "value (depth)", front first — the order {@code poll()} will use. */
    private static List<String> labels(Deque<Integer> queue, int[] steps) {
        List<String> out = new ArrayList<>(queue.size());
        for (int value : queue) {
            out.add(value + " (" + steps[value] + ")");
        }
        return out;
    }
}
