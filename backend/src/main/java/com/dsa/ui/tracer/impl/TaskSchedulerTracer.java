package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A max-heap of remaining per-task-type counts, plus a cooldown queue holding tasks that
 * just ran and cannot be scheduled again for {@code n} ticks. Each simulated CPU tick either
 * schedules the currently-most-frequent task type, or idles when everything schedulable is
 * still cooling down - idling is the branch every naive "just divide by n+1" formula misses.
 * The array shown is the heap of remaining counts, the actual bookkeeping structure.
 */
@Component
public class TaskSchedulerTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "task-scheduler";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("tasks", FieldType.INT_ARRAY)
                        .label("Tasks (encoded as small task-type ids)")
                        .help("Each number is a task type; repeats mean the same type again.")
                        .length(1, 40).values(0, 25)
                        .defaultValue(List.of(0, 0, 0, 1, 1, 1))
                        .build(),
                InputField.of("n", FieldType.INT)
                        .label("Cooldown")
                        .help("Ticks that must pass before the same task type can repeat.")
                        .range(0, 10)
                        .defaultValue(2)
                        .build());
    }

    /** Five task types (one appearing 5 times) instead of two, so idle ticks show up too. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("tasks", List.of(0, 0, 0, 0, 0, 1, 1, 1, 4, 4, 5, 5, 6, 6), "n", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public int leastInterval(int[] tasks, int n) {
                   Map<Integer, Integer> freq = new LinkedHashMap<>();
                   for (int t : tasks) freq.merge(t, 1, Integer::sum);

                   List<Integer> heap = new ArrayList<>();
                   for (int count : freq.values()) {
                       // @a seed
                       heap.add(count);
                       siftUp(heap, heap.size() - 1);
                   }

                   Deque<int[]> cooling = new ArrayDeque<>(); // {remainingCount, availableAtTime}
                   int time = 0;
                   while (!heap.isEmpty() || !cooling.isEmpty()) {
                       time++;
                       if (!heap.isEmpty()) {
                           // @a schedule
                           int remaining = removeMax(heap) - 1;
                           if (remaining > 0) cooling.addLast(new int[]{remaining, time + n});
                       } else {
                           // @a idle
                       }
                       if (!cooling.isEmpty() && cooling.peekFirst()[1] == time) {
                           // @a release
                           heap.add(cooling.pollFirst()[0]);
                           siftUp(heap, heap.size() - 1);
                       }
                   }
                   // @a done
                   return time;
               }

               private int removeMax(List<Integer> heap) {
                   int max = heap.get(0);
                   int last = heap.remove(heap.size() - 1);
                   if (!heap.isEmpty()) {
                       heap.set(0, last);
                       siftDown(heap, 0);
                   }
                   return max;
               }

               private int siftUp(List<Integer> heap, int i) {
                   while (i > 0 && heap.get(i) > heap.get((i - 1) / 2)) {
                       swap(heap, i, (i - 1) / 2);
                       i = (i - 1) / 2;
                   }
                   return i;
               }

               private int siftDown(List<Integer> heap, int i) {
                   int n = heap.size();
                   while (true) {
                       int l = 2 * i + 1, r = 2 * i + 2, largest = i;
                       if (l < n && heap.get(l) > heap.get(largest)) largest = l;
                       if (r < n && heap.get(r) > heap.get(largest)) largest = r;
                       if (largest == i) return i;
                       swap(heap, i, largest);
                       i = largest;
                   }
               }

               private void swap(List<Integer> heap, int i, int j) {
                   Integer tmp = heap.get(i);
                   heap.set(i, heap.get(j));
                   heap.set(j, tmp);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] tasks = in.getIntArray("tasks");
        int n = in.getInt("n");

        Map<Integer, Integer> freq = new LinkedHashMap<>();
        for (int t : tasks) {
            freq.merge(t, 1, Integer::sum);
        }

        List<Integer> heap = new ArrayList<>();
        for (int count : freq.values()) {
            heap.add(count);
            int at = siftUp(heap, heap.size() - 1);
            emit.at("seed")
                    .say("A task type with count %d joins the max-heap (size now %d).",
                            count, heap.size())
                    .var("count", count).var("heapSize", heap.size())
                    .array(toArray(heap), at).step();
        }

        Deque<int[]> cooling = new ArrayDeque<>();
        int time = 0;
        while (!heap.isEmpty() || !cooling.isEmpty()) {
            time++;
            if (!heap.isEmpty()) {
                int remaining = removeMax(heap) - 1;
                if (remaining > 0) {
                    cooling.addLast(new int[]{remaining, time + n});
                }
                emit.at("schedule")
                        .say("Time %d: run the most frequent remaining task type (%d instance%s "
                                        + "left after this run%s).",
                                time, remaining, remaining == 1 ? "" : "s",
                                remaining > 0 ? " - cools down until t=" + (time + n) : "")
                        .var("time", time).var("heapSize", heap.size()).var("cooling", cooling.size())
                        .array(toArray(heap)).step();
            } else {
                emit.at("idle")
                        .say("Time %d: nothing is schedulable - every remaining task is still "
                                        + "cooling down. The CPU sits idle.",
                                time)
                        .var("time", time).var("cooling", cooling.size())
                        .array(toArray(heap)).step();
            }

            if (!cooling.isEmpty() && cooling.peekFirst()[1] == time) {
                int[] freed = cooling.pollFirst();
                heap.add(freed[0]);
                int at = siftUp(heap, heap.size() - 1);
                emit.at("release")
                        .say("Time %d: a task type's cooldown ends - %d instance%s rejoin the heap.",
                                time, freed[0], freed[0] == 1 ? "" : "s")
                        .var("time", time).var("released", freed[0])
                        .array(toArray(heap), at).step();
            }
        }

        emit.at("done")
                .say("The CPU finished at time %d - the minimum number of intervals needed.", time)
                .var("answer", time)
                .array(toArray(heap)).step();
    }

    private static int[] toArray(List<Integer> heap) {
        int[] out = new int[heap.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = heap.get(i);
        }
        return out;
    }

    private static int removeMax(List<Integer> heap) {
        int max = heap.get(0);
        int last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            siftDown(heap, 0);
        }
        return max;
    }

    private static int siftUp(List<Integer> heap, int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (heap.get(i) > heap.get(parent)) {
                swap(heap, i, parent);
                i = parent;
            } else {
                break;
            }
        }
        return i;
    }

    private static int siftDown(List<Integer> heap, int i) {
        int n = heap.size();
        while (true) {
            int l = 2 * i + 1, r = 2 * i + 2, largest = i;
            if (l < n && heap.get(l) > heap.get(largest)) largest = l;
            if (r < n && heap.get(r) > heap.get(largest)) largest = r;
            if (largest == i) break;
            swap(heap, i, largest);
            i = largest;
        }
        return i;
    }

    private static void swap(List<Integer> heap, int i, int j) {
        Integer tmp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, tmp);
    }
}
