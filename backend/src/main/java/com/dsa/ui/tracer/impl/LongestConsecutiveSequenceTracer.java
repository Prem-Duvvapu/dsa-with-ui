package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Longest consecutive sequence in O(N) time using a HashSet.
 *
 * <p>The key insight: only start counting from sequence starters — numbers
 * whose predecessor {@code (num - 1)} is NOT in the set. This guarantees each
 * element is visited at most twice (once in the outer loop, once in the
 * inner while), giving O(N) total despite nested loops.
 */
@Component
public class LongestConsecutiveSequenceTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "longest-consecutive-sequence";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("Unsorted integers. Duplicates are allowed but ignored.")
                        .length(1, 30).values(-1000, 1000)
                        .defaultValue(List.of(100, 4, 200, 1, 3, 2))
                        .build());
    }

    /**
     * Two separate streaks of different lengths, plus a duplicate to ensure
     * the dedup path is exercised.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(9, 1, 4, 7, 3, 2, 6, 5, 8));
    }

    @Override
    public String annotatedCode() {
        return """
               public int longestConsecutive(int[] nums) {
                   // @a buildSet
                   Set<Integer> set = new HashSet<>();
                   for (int num : nums) set.add(num);
                   int longest = 0;
               
                   for (int it : set) {
                       if (!set.contains(it - 1)) {
                           // @a startStreak
                           int cnt = 1, x = it;
                           while (set.contains(x + 1)) {
                               // @a extend
                               x++; cnt++;
                           }
                           // @a updateBest
                           longest = Math.max(longest, cnt);
                       } else {
                           // @a skip
                       }
                   }
                   // @a done
                   return longest;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        Set<Integer> set = new HashSet<>();
        for (int num : nums) set.add(num);

        emit.at("buildSet")
                .say("Build HashSet from %s → unique values: %s (%d elements).",
                        java.util.Arrays.toString(nums), set, set.size())
                .var("setSize", set.size())
                .array(nums)
                .step();

        int longest = 0;
        // Iterate in sorted order for deterministic traces
        List<Integer> sorted = new ArrayList<>(set);
        Collections.sort(sorted);

        for (int it : sorted) {
            if (!set.contains(it - 1)) {
                emit.at("startStreak")
                        .say("%d has no predecessor (%d not in set) → it's a sequence starter. Begin counting.",
                                it, it - 1)
                        .var("starter", it).var("longest", longest)
                        .array(nums, findIndex(nums, it))
                        .step();

                int cnt = 1, x = it;
                while (set.contains(x + 1)) {
                    x++;
                    cnt++;
                    emit.at("extend")
                            .say("Set contains %d → extend streak to length %d.", x, cnt)
                            .var("x", x).var("cnt", cnt)
                            .array(nums, findIndex(nums, x))
                            .step();
                }

                longest = Math.max(longest, cnt);
                emit.at("updateBest")
                        .say("Streak starting at %d ended at %d (length %d).%s",
                                it, x, cnt,
                                cnt >= longest ? " New best!" : " Not longer than current best " + longest + ".")
                        .var("longest", longest).var("streakLen", cnt)
                        .array(nums, findIndex(nums, it))
                        .step();
            } else {
                emit.at("skip")
                        .say("%d has predecessor %d in set — not a sequence starter, skip.", it, it - 1)
                        .var("it", it)
                        .array(nums, findIndex(nums, it))
                        .step();
            }
        }

        emit.at("done")
                .say("Longest consecutive sequence has length %d.", longest)
                .var("longest", longest)
                .array(nums)
                .step();
    }

    private int findIndex(int[] nums, int value) {
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == value) return i;
        }
        return -1;
    }
}
