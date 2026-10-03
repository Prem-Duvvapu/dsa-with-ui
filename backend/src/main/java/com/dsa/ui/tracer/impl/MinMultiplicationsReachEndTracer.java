package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Minimum Multiplications to Reach End (GfG), traced on the owner's own accepted submission: a
 * BFS over the 100000 possible remainders. Every multiplication is one step, so the first time
 * BFS reaches {@code end} is the fewest steps; {@code opsCnt} doubles as the visited check.
 * O(100000 * arr.length).
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
               class Solution {
                   int minimumMultiplications(int[] arr, int start, int end) {
                       // @a init
                       int mod=(int)(1e5);
                       int[] opsCnt=new int[mod];
                       for (int i=0;i<mod;i++)
                           opsCnt[i]=(int)(1e9);

                       Queue<Integer> q=new ArrayDeque<>();
                       q.add(start);
                       opsCnt[start]=0;

                       if (start==end)
                           return 0;

                       while (!q.isEmpty())
                       {
                           // @a poll
                           int front=q.poll();

                           for (int i=0;i<arr.length;i++)
                           {
                               int currVal=(front*arr[i])%mod;

                               if (opsCnt[currVal]>opsCnt[front]+1)
                               {
                                   // @a discover
                                   opsCnt[currVal]=opsCnt[front]+1;

                                   if (currVal==end)
                                       // @a reached
                                       return opsCnt[currVal];

                                   q.add(currVal);
                               }
                           }
                       }

                       // @a none
                       return -1;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("arr");
        int start = in.getInt("start");
        int end = in.getInt("end");
        int mod = (int) 1e5;
        int[] opsCnt = new int[mod];
        Arrays.fill(opsCnt, (int) 1e9);
        Deque<Integer> q = new ArrayDeque<>();
        q.add(start);
        opsCnt[start] = 0;

        emit.at("init").say("Every value is a remainder mod 100000, so there are at most 100000 states. BFS from "
                        + "%d: each multiplication by one of %s is one step.", start, Arrays.toString(arr))
                .var("start", start).var("end", end).array(arr).queue(labels(q, opsCnt)).step();

        if (start == end) {
            // Neither contract input has start == end, so this branch carries no highlight of its own.
            emit.at("init").say("start is already end, so the check right after the setup returns 0: no "
                            + "multiplication is needed.")
                    .var("answer", 0).array(arr).queue(labels(q, opsCnt)).step();
            return;
        }

        while (!q.isEmpty()) {
            int front = q.poll();
            emit.at("poll").say("Take %d, reached in %d step%s. Multiply it by each value in arr.",
                            front, opsCnt[front], Narration.s(opsCnt[front]))
                    .var("front", front).var("steps", opsCnt[front]).array(arr).queue(labels(q, opsCnt)).step();

            for (int i = 0; i < arr.length; i++) {
                int currVal = (int) (((long) front * arr[i]) % mod);
                if (opsCnt[currVal] > opsCnt[front] + 1) {
                    opsCnt[currVal] = opsCnt[front] + 1;
                    if (currVal == end) {
                        emit.at("reached").say("%d x %d mod 100000 = %d, which is end, reached in %d step%s. BFS "
                                        + "found it first, so that is the minimum. Return %d.",
                                        front, arr[i], currVal, opsCnt[currVal], Narration.s(opsCnt[currVal]), opsCnt[currVal])
                                .var("currVal", currVal).var("answer", opsCnt[currVal])
                                .array(arr, i).queue(labels(q, opsCnt)).step();
                        return;
                    }
                    q.add(currVal);
                    emit.at("discover").say("%d x %d mod 100000 = %d, new: it takes %d step%s. Queue it.",
                                    front, arr[i], currVal, opsCnt[currVal], Narration.s(opsCnt[currVal]))
                            .var("currVal", currVal).var("steps", opsCnt[currVal])
                            .array(arr, i).queue(labels(q, opsCnt)).step();
                }
            }
        }

        emit.at("none").say("The queue is empty: every reachable value has been tried and %d was never produced. "
                        + "Return -1.", end)
                .var("answer", -1).array(arr).step();
    }

    private static List<String> labels(Deque<Integer> q, int[] opsCnt) {
        List<String> out = new ArrayList<>();
        for (int v : q) {
            out.add(v + " (" + opsCnt[v] + ")");
        }
        return out;
    }
}
