package com.dsa.ui.algorithm.backtracking;

import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Problem: Palindrome Partitioning (LeetCode 131)
 *
 * Partition string S such that every substring of partition is a palindrome.
 */
public class PalindromePartitioning {

    public List<List<String>> solve(String s, TraceRecorder recorder) {
        List<List<String>> res = new ArrayList<>();
        List<String> path = new ArrayList<>();
        List<String> callStack = new ArrayList<>();

        recorder.record(new TraceEvent(
            "start", 15,
            String.format("Palindrome Partitioning: S = \"%s\". Generate all valid palindrome partitions.", s),
            Map.of("S", s),
            "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
        ));

        partition(0, s, path, res, recorder, callStack);

        recorder.record(new TraceEvent(
            "complete", 40,
            String.format("Palindrome Partitioning Complete! Total Partitions found = %d: %s", res.size(), res.toString()),
            Map.of("Total Partitions", String.valueOf(res.size()), "Partitions", res.toString()),
            "Stack", null, List.of(), Map.of(), List.of()
        ));

        return res;
    }

    private void partition(int index, String s, List<String> path, List<List<String>> res, TraceRecorder recorder, List<String> callStack) {
        callStack.add(String.format("solve(idx=%d)", index));

        if (index == s.length()) {
            res.add(new ArrayList<>(path));
            recorder.record(new TraceEvent(
                "partition_found", 22,
                String.format("VALID PARTITION FOUND! Partition: %s", path.toString()),
                Map.of("partition", path.toString()),
                "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
            ));
            callStack.remove(callStack.size() - 1);
            return;
        }

        for (int i = index; i < s.length(); i++) {
            if (isPalindrome(s, index, i)) {
                String sub = s.substring(index, i + 1);
                path.add(sub);

                recorder.record(new TraceEvent(
                    "cut_palindrome", 28,
                    String.format("Substring [%d..%d] (\"%s\") is a PALINDROME! Cut \"%s\". Current path: %s. Recurse to idx=%d...",
                        index, i, sub, sub, path.toString(), i + 1),
                    Map.of("cut", sub, "path", path.toString()),
                    "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
                ));

                partition(i + 1, s, path, res, recorder, callStack);

                // Backtrack
                path.remove(path.size() - 1);

                recorder.record(new TraceEvent(
                    "backtrack_partition", 35,
                    String.format("BACKTRACK: Remove cut \"%s\". Restore path: %s.", sub, path.toString()),
                    Map.of("backtrack", sub, "path", path.toString()),
                    "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
                ));
            } else {
                String sub = s.substring(index, i + 1);
                recorder.record(new TraceEvent(
                    "not_palindrome", 38,
                    String.format("Substring [%d..%d] (\"%s\") is NOT a palindrome. Skip cut.", index, i, sub),
                    Map.of("skip", sub),
                    "Stack", null, new ArrayList<>(callStack), Map.of(), List.of()
                ));
            }
        }

        callStack.remove(callStack.size() - 1);
    }

    private boolean isPalindrome(String s, int start, int end) {
        while (start < end) {
            if (s.charAt(start++) != s.charAt(end--)) return false;
        }
        return true;
    }
}
