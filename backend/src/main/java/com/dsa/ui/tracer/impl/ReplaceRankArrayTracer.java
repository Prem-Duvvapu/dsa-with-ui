package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Rank Transform of an Array (LeetCode 1331), traced on the owner's own accepted submission: pair
 * each value with its index, sort the pairs by value, then walk them - a value equal to the one
 * before it keeps the rank, a larger one takes the next rank. O(n log n), no map.
 *
 * <p>The canvas is the sorted pairs: bar height is the value, the label the index it came from.
 */
@Component
public class ReplaceRankArrayTracer implements AlgorithmTracer {
    @Override public String id() { return "replace-rank-array"; }
    @Override public DsType dsType() { return DsType.ARRAY; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("values", FieldType.INT_ARRAY).label("Values")
                .length(1, 60).values(-1000, 1000)
                .defaultValue(List.of(20, 15, 26, 2, 98, 6, 2)).build());
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of("values", List.of(40, 10, 20, 30, 20, 10, 50, 40));
    }

    @Override public String annotatedCode() {
        return """
               class Solution {
                   public int[] arrayRankTransform(int[] arr) {
                       int n=arr.length;
                       int[][] sortedArr=new int[n][2];
                       int[] res=new int[n];

                       if (n==0)
                           return res;

                       // @a pair
                       for (int i=0;i<n;i++) {
                           sortedArr[i][0]=arr[i];
                           sortedArr[i][1]=i;
                       }

                       // @a sort
                       Arrays.sort(sortedArr, (x,y) -> Integer.compare(x[0],y[0]));

                       int rank=1;
                       // @a first
                       res[sortedArr[0][1]]=rank;

                       for (int i=1;i<n;i++) {
                           if (sortedArr[i][0]==sortedArr[i-1][0])
                               // @a same
                               res[sortedArr[i][1]]=rank;
                           else
                               // @a next
                               res[sortedArr[i][1]]=++rank;
                       }

                       // @a done
                       return res;
                   }
               }""";
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        int[] arr = in.getIntArray("values");
        int n = arr.length;
        int[][] sortedArr = new int[n][2];
        int[] res = new int[n];
        for (int i = 0; i < n; i++) {
            sortedArr[i][0] = arr[i];
            sortedArr[i][1] = i;
        }
        emit.at("pair").say("Pair every value with its index, so the rank can be written back to the right slot.")
                .arrayState(render(sortedArr, -1, -1)).step();
        Arrays.sort(sortedArr, (x, y) -> Integer.compare(x[0], y[0]));
        emit.at("sort").say("Sort the pairs by value: %s. Each label is the index the value came from.", values(sortedArr))
                .arrayState(render(sortedArr, -1, -1)).step();
        int rank = 1;
        res[sortedArr[0][1]] = rank;
        emit.at("first").say("The smallest value, %d (index %d), gets rank 1.", sortedArr[0][0], sortedArr[0][1])
                .var("rank", rank).var("res", Arrays.toString(res)).arrayState(render(sortedArr, 0, 0)).step();
        for (int i = 1; i < n; i++) {
            if (sortedArr[i][0] == sortedArr[i - 1][0]) {
                res[sortedArr[i][1]] = rank;
                emit.at("same").say("%d equals the value before it, so index %d keeps rank %d.",
                                sortedArr[i][0], sortedArr[i][1], rank)
                        .var("i", i).var("rank", rank).var("res", Arrays.toString(res))
                        .arrayState(render(sortedArr, i, i - 1)).step();
            } else {
                res[sortedArr[i][1]] = ++rank;
                emit.at("next").say("%d is larger than %d, so index %d gets the next rank, %d.",
                                sortedArr[i][0], sortedArr[i - 1][0], sortedArr[i][1], rank)
                        .var("i", i).var("rank", rank).var("res", Arrays.toString(res))
                        .arrayState(render(sortedArr, i, i - 1)).step();
            }
        }
        emit.at("done").say("Every index has its rank: %s.", Arrays.toString(res))
                .var("result", Arrays.toString(res)).array(res).step();
    }

    private static String values(int[][] pairs) {
        int[] v = new int[pairs.length];
        for (int i = 0; i < pairs.length; i++) v[i] = pairs[i][0];
        return Arrays.toString(v);
    }

    /** The pairs as bars labelled with their original index; {@code current} is compared with {@code previous}. */
    private static List<ArrayElement> render(int[][] pairs, int current, int previous) {
        List<ArrayElement> out = new ArrayList<>(pairs.length);
        for (int i = 0; i < pairs.length; i++) {
            String state = i == current ? "current" : i == previous ? "target" : i < current ? "done" : "default";
            out.add(new ArrayElement(i, pairs[i][0], state, "@" + pairs[i][1]));
        }
        return out;
    }
}
