package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Maximum Sum Combination (GfG), traced on the owner's C++ submission ported to Java: sort both
 * arrays, start from the two largest elements, and each time the heap's largest sum is taken push its
 * two neighbours (i-1, j) and (i, j-1) unless already seen. The submission skipped the i > 0 / j > 0
 * checks and read A[-1] once a row or column was exhausted; the displayed code says so.
 */
@Component
public class MaximumSumCombinationTracer implements AlgorithmTracer {
    @Override public String id() { return "maximum-sum-combination"; }
    @Override public DsType dsType() { return DsType.HEAP; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("a", FieldType.INT_ARRAY).label("First array")
                        .length(1, 20).values(-1000, 1000).defaultValue(List.of(1, 4, 2, 3))
                        .workScalesWith("count").build(),
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
               class Solution {
                   public List<Integer> maxCombinations(int k, int[] a, int[] b) {
                       List<Integer> ans=new ArrayList<>();
                       int n=a.length;
                       int m=b.length;
                       // @a sort
                       Arrays.sort(a);
                       Arrays.sort(b);
                       // {sum, i, j}, largest first; ties as C++'s pair ordering breaks them
                       PriorityQueue<int[]> pq=new PriorityQueue<>((x,y) -> x[0]!=y[0] ? Integer.compare(y[0],x[0])
                           : x[1]!=y[1] ? Integer.compare(y[1],x[1]) : Integer.compare(y[2],x[2]));
                       Set<String> s=new HashSet<>();
                       // @a seed
                       pq.add(new int[]{a[n-1]+b[m-1],n-1,m-1});
                       s.add((n-1)+","+(m-1));

                       while (k-->0) {
                           // @a pop
                           int[] p=pq.poll();
                           int sum=p[0];
                           int i=p[1];
                           int j=p[2];
                           ans.add(sum);

                           if (i>0 && !s.contains((i-1)+","+j)) {
                               // @a pushUp
                               pq.add(new int[]{a[i-1]+b[j],i-1,j});
                               s.add((i-1)+","+j);
                           }
                           if (j>0 && !s.contains(i+","+(j-1))) {
                               // @a pushLeft
                               pq.add(new int[]{a[i]+b[j-1],i,j-1});
                               s.add(i+","+(j-1));
                           }
                       }
                       // @a done
                       return ans;
                   }
               }""";
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[] a = in.getIntArray("a");
        int[] b = in.getIntArray("b");
        int count = in.getInt("count");
        if ((long) count > (long) a.length * b.length) {
            throw new InputValidationException(Map.of("count", "Must not exceed a.length × b.length."));
        }
        Arrays.sort(a);
        Arrays.sort(b);
        emit.at("sort").say("Sort both arrays: a = %s, b = %s. The largest sum is a's last plus b's last.",
                        Arrays.toString(a), Arrays.toString(b))
                .var("a", Arrays.toString(a)).var("b", Arrays.toString(b)).array(a).step();

        ArrayHeap<Pair> heap = new ArrayHeap<>(Comparator.comparingInt(Pair::sum)
                .thenComparingInt(Pair::i).thenComparingInt(Pair::j).reversed());
        Set<Long> seen = new HashSet<>();
        Pair first = pair(a, b, a.length - 1, b.length - 1);
        heap.offer(first); seen.add(key(first.i(), first.j()));
        emit.at("seed").say("Seed the heap with the two largest: a[%d] + b[%d] = %d + %d = %d, and mark (%d,%d) seen.",
                        first.i(), first.j(), a[first.i()], b[first.j()], first.sum(), first.i(), first.j())
                .var("sum", first.sum()).arrayState(render(heap)).step();

        List<Integer> answer = new ArrayList<>();
        while (answer.size() < count) {
            Pair best = heap.poll();
            answer.add(best.sum());
            emit.at("pop").say("Poll the largest sum in the heap, %d = a[%d] + b[%d]: answer %d of %d.",
                            best.sum(), best.i(), best.j(), answer.size(), count)
                    .var("rank", answer.size()).var("sum", best.sum()).arrayState(render(heap)).step();
            int[][] neighbours = {{best.i() - 1, best.j()}, {best.i(), best.j() - 1}};
            String[] anchors = {"pushUp", "pushLeft"};
            for (int t = 0; t < 2; t++) {
                int[] at = neighbours[t];
                if (at[0] < 0 || at[1] < 0 || !seen.add(key(at[0], at[1]))) continue;
                Pair next = pair(a, b, at[0], at[1]);
                heap.offer(next);
                emit.at(anchors[t]).say("Push (%d,%d): a[%d] + b[%d] = %d + %d = %d, not seen before.",
                                next.i(), next.j(), next.i(), next.j(), a[next.i()], b[next.j()], next.sum())
                        .var("candidate", next.sum()).arrayState(render(heap)).step();
            }
        }
        emit.at("done").say("Top %d pair sums: %s.", count, answer)
                .var("answer", answer).array(answer.stream().mapToInt(Integer::intValue).toArray()).step();
    }

    private static Pair pair(int[] a, int[] b, int i, int j) { return new Pair(i, j, a[i] + b[j]); }
    private static long key(int i, int j) { return ((long) i << 32) ^ (j & 0xffffffffL); }

    /** The heap's own array - see ArrayHeap for why this must not be sorted first. */
    private static List<ArrayElement> render(ArrayHeap<Pair> heap) {
        List<Pair> snapshot = heap.slots();
        List<ArrayElement> out = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            Pair p = snapshot.get(i);
            // The sum, not "(i,j)": the heap is ordered by sum, and a label would hide it. The
            // narration names each pair as it is pushed and polled.
            out.add(new ArrayElement(i, p.sum(), i == 0 ? "target" : "default"));
        }
        return out;
    }

    private record Pair(int i, int j, int sum) {}
}
