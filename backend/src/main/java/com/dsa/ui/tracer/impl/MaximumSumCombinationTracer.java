package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Finds the largest pair sums without materialising every Cartesian-product pair. */
@Component
public class MaximumSumCombinationTracer implements AlgorithmTracer {
    @Override public String id() { return "maximum-sum-combination"; }
    @Override public DsType dsType() { return DsType.HEAP; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("a", FieldType.INT_ARRAY).label("First array")
                        .length(1, 20).values(-1000, 1000).defaultValue(List.of(1, 4, 2, 3)).build(),
                InputField.of("b", FieldType.INT_ARRAY).label("Second array")
                        .length(1, 20).values(-1000, 1000).defaultValue(List.of(2, 5, 1, 6)).build(),
                InputField.of("count", FieldType.INT).label("Number of sums")
                        .range(1, 20).defaultValue(4).build());
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of("a", List.of(3, 2, 4, 1, 8), "b", List.of(4, 3, 1, 7, 2), "count", 6);
    }

    @Override public String annotatedCode() {
        return """
               public List<Integer> maxSums(int[] a, int[] b, int count) {
                   // @a sort
                   Arrays.sort(a); Arrays.sort(b);
                   // @a seed
                   heap.offer(pair(a.length - 1, b.length - 1));
                   while (answer.size() < count) {
                       // @a pop
                       Pair best = heap.poll();
                       answer.add(best.sum);
                       for (Pair neighbour : neighbours(best)) {
                           if (seen.add(neighbour.indices())) {
                               // @a push
                               heap.offer(neighbour);
                           }
                       }
                   }
                   // @a done
                   return answer;
               }""";
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[] a = in.getIntArray("a");
        int[] b = in.getIntArray("b");
        int count = in.getInt("count");
        if ((long) count > (long) a.length * b.length) {
            throw new InputValidationException(Map.of("count", "Must not exceed a.length × b.length."));
        }
        Arrays.sort(a); Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            emit.at("sort").say("Sorted A[%d] = %d.", i, a[i]).var("array", "A").var("index", i).array(a, i).step();
        }
        for (int i = 0; i < b.length; i++) {
            emit.at("sort").say("Sorted B[%d] = %d.", i, b[i]).var("array", "B").var("index", i).array(b, i).step();
        }

        PriorityQueue<Pair> heap = new PriorityQueue<>(Comparator.comparingInt(Pair::sum).reversed());
        Set<Long> seen = new HashSet<>();
        Pair first = pair(a, b, a.length - 1, b.length - 1);
        heap.offer(first); seen.add(key(first.i(), first.j()));
        emit.at("seed").say("Largest elements seed pair (%d,%d) with sum %d.", first.i(), first.j(), first.sum())
                .var("sum", first.sum()).arrayState(render(heap)).step();

        List<Integer> answer = new ArrayList<>();
        while (answer.size() < count) {
            Pair best = heap.poll();
            answer.add(best.sum());
            emit.at("pop").say("Take next-largest unseen sum %d from A[%d] + B[%d].", best.sum(), best.i(), best.j())
                    .var("rank", answer.size()).var("sum", best.sum()).arrayState(render(heap)).step();
            int[][] neighbours = {{best.i() - 1, best.j()}, {best.i(), best.j() - 1}};
            for (int[] at : neighbours) {
                if (at[0] < 0 || at[1] < 0 || !seen.add(key(at[0], at[1]))) continue;
                Pair next = pair(a, b, at[0], at[1]);
                heap.offer(next);
                emit.at("push").say("Add unseen neighbour (%d,%d), sum %d, to the candidate heap.",
                                next.i(), next.j(), next.sum())
                        .var("candidate", next.sum()).arrayState(render(heap)).step();
            }
        }
        emit.at("done").say("Top %d pair sums: %s.", count, answer)
                .var("answer", answer).array(answer.stream().mapToInt(Integer::intValue).toArray()).step();
    }

    private static Pair pair(int[] a, int[] b, int i, int j) { return new Pair(i, j, a[i] + b[j]); }
    private static long key(int i, int j) { return ((long) i << 32) ^ (j & 0xffffffffL); }

    private static List<ArrayElement> render(PriorityQueue<Pair> heap) {
        List<Pair> snapshot = new ArrayList<>(heap);
        snapshot.sort(Comparator.comparingInt(Pair::sum).reversed());
        List<ArrayElement> out = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            Pair p = snapshot.get(i);
            out.add(new ArrayElement(i, p.sum(), i == 0 ? "target" : "default",
                    "(" + p.i() + "," + p.j() + ")"));
        }
        return out;
    }

    private record Pair(int i, int j, int sum) {}
}
