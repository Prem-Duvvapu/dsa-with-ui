package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Same O(n^2) LIS DP as longest-increasing-subsequence, plus a parent[] so the
 * actual subsequence can be walked out backwards from where the best run ends.
 */
@Component
public class PrintLisTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "print-lis";
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
                        .help("parent[i] remembers which index i's best subsequence came from.")
                        .length(1, 30).values(-999, 999)
                        .defaultValue(List.of(10, 9, 2, 5, 3, 7, 101, 18))
                        .build());
    }

    /** Different length, different answer, and ties in the middle of the array. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(3, 1, 4, 1, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] lisWithParents(int[] nums) {
                   // @a init
                   int n = nums.length;
                   int[] dp = new int[n], parent = new int[n];
                   Arrays.fill(dp, 1);
                   Arrays.fill(parent, -1);
                   int bestEnd = 0;
                   for (int i = 1; i < n; i++) {
                       for (int j = 0; j < i; j++) {
                           // @a compare
                           if (nums[j] < nums[i] && dp[j] + 1 > dp[i]) {
                               // @a take
                               dp[i] = dp[j] + 1;
                               parent[i] = j;
                           }
                       }
                       // @a best
                       if (dp[i] > dp[bestEnd]) bestEnd = i;
                   }
                   // @a backlink
                   while (bestEnd != -1) {
                       collect(nums[bestEnd]);
                       bestEnd = parent[bestEnd];
                   }
                   // @a done
                   return answer();
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int[] dp = new int[n];
        int[] parent = new int[n];
        java.util.Arrays.fill(dp, 1);
        java.util.Arrays.fill(parent, -1);

        emit.at("init").say("dp starts all 1s (each element alone) and every parent is -1 (no predecessor yet). The displayed values are dp[].")
                .var("bestEnd", 0)
                .arrayState(states(nums, dp, parent, -1, -1)).step();

        int bestEnd = 0;
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[j] < nums[i] && dp[j] + 1 > dp[i]) {
                    emit.at("compare")
                            .say("i=%d, j=%d: nums[%d]=%d < nums[%d]=%d and dp[%d]+1=%d beats dp[%d]=%d.",
                                    i, j, j, nums[j], i, nums[i], j, dp[j] + 1, i, dp[i])
                            .var("i", i).var("j", j)
                            .arrayState(states(nums, dp, parent, i, j)).step();
                    dp[i] = dp[j] + 1;
                    parent[i] = j;
                    emit.at("take")
                            .say("Record it: dp[%d]=%d with parent[%d]=%d - when printing we will step back to %d next.",
                                    i, dp[i], i, j, j)
                            .var("i", i).var("j", j).var("dp[i]", dp[i]).var("parent[i]", j)
                            .arrayState(states(nums, dp, parent, i, j)).step();
                } else if (nums[j] >= nums[i]) {
                    emit.at("compare")
                            .say("i=%d, j=%d: nums[%d]=%d is not below nums[%d]=%d, so nothing ending at j can extend to i.",
                                    i, j, j, nums[j], i, nums[i])
                            .var("i", i).var("j", j)
                            .arrayState(states(nums, dp, parent, i, j)).step();
                } else {
                    emit.at("compare")
                            .say("i=%d, j=%d: extending through j would give length %d, not better than the %d already recorded.",
                                    i, j, dp[j] + 1, dp[i])
                            .var("i", i).var("j", j)
                            .arrayState(states(nums, dp, parent, i, j)).step();
                }
            }
            if (dp[i] > dp[bestEnd]) {
                bestEnd = i;
                emit.at("best")
                        .say("A length-%d subsequence ends at i=%d - that becomes the print start.",
                                dp[i], i)
                        .var("i", i).var("bestEnd", bestEnd)
                        .arrayState(states(nums, dp, parent, i, -1)).step();
            }
        }

        List<Integer> reversed = new ArrayList<>();
        Set<Integer> chain = new HashSet<>();
        int cursor = bestEnd;
        while (cursor != -1) {
            reversed.add(nums[cursor]);
            chain.add(cursor);
            emit.at("backlink")
                    .say("Take nums[%d]=%d, then follow parent to index %s. Collected so far: %s.",
                            cursor, nums[cursor],
                            parent[cursor] == -1 ? "stop" : String.valueOf(parent[cursor]),
                            reversed)
                    .var("cursor", cursor)
                    .arrayState(chainStates(nums, dp, chain, cursor)).step();
            cursor = parent[cursor];
        }

        StringBuilder sb = new StringBuilder("[");
        for (int k = reversed.size() - 1; k >= 0; k--) {
            sb.append(reversed.get(k));
            if (k > 0) sb.append(", ");
        }
        sb.append(']');
        String sequence = sb.toString();

        emit.at("done").say("Reversed the collected values into the LIS: %s (length %d).", sequence, reversed.size())
                .var("lis", sequence).var("length", reversed.size())
                .arrayState(chainStates(nums, dp, chain, -1)).step();
    }

    private static List<ArrayElement> states(int[] nums, int[] dp, int[] parent, int current, int target) {
        List<ArrayElement> out = new ArrayList<>(nums.length);
        for (int k = 0; k < nums.length; k++) {
            String s = k == current ? "current" : k == target ? "target" : "default";
            out.add(new ArrayElement(k, dp[k], s));
        }
        return out;
    }

    /** Marks the real parent-chain members as target and the walk cursor as current. */
    private static List<ArrayElement> chainStates(int[] nums, int[] dp, Set<Integer> chain, int cursor) {
        List<ArrayElement> out = new ArrayList<>(nums.length);
        for (int k = 0; k < nums.length; k++) {
            String s;
            if (k == cursor) s = "current";
            else if (chain.contains(k)) s = "target";
            else s = "default";
            out.add(new ArrayElement(k, dp[k], s));
        }
        return out;
    }
}
