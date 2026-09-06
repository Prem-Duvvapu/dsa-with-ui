package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SlidingWindowService implements ProblemProvider {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public SlidingWindowService() {
        initProblems();
    }

    public List<ProblemDetail> getAllProblems() {
        return new ArrayList<>(problems.values());
    }

    public ProblemDetail getProblemById(String id) {
        return problems.get(id);
    }

    public List<ExecutionStep> generateSteps(String problemId) {
        switch (problemId) {
            // All 12 sliding window problems now have real tracers (tracer/impl).
            case "fruit-into-baskets":
            case "longest-repeating-character-replacement":
            case "minimum-window-substring":
            case "subarrays-k-different-integers":
            case "longest-substring-without-repeating":
            case "max-consecutive-ones-3":
            case "binary-subarrays-with-sum":
            case "count-nice-subarrays":
            case "number-substrings-all-three-chars":
            case "maximum-points-cards":
            case "longest-substring-k-distinct":
            case "minimum-window-subsequence":
                throw new LegacyTraceRetiredException(problemId);
            default:
                throw new LegacyTraceRetiredException(problemId);
        }
    }

    private void initProblems() {
        // 1. Longest Substring Without Repeating
        problems.put("longest-substring-without-repeating", new ProblemDetail(
            "longest-substring-without-repeating", "Longest Substring Without Repeating Characters", "Sliding Window - Medium", "Sliding Window", "Medium",
            "Find length of longest substring without repeating characters using 2-pointer Sliding Window.",
            """
            // Java Sliding Window (LeetCode 3)
            public int lengthOfLongestSubstring(String s) {
                int left = 0, right = 0, maxLen = 0;
                HashMap<Character, Integer> map = new HashMap<>();
                while (right < s.length()) {
                    char c = s.charAt(right);
                    if (map.containsKey(c)) left = Math.max(left, map.get(c) + 1);
                    map.put(c, right);
                    maxLen = Math.max(maxLen, right - left + 1);
                    right++;
                }
                return maxLen;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 3, 1, 2, 3, 1, 1}, -1, -1), null, null, null,
            new ComplexityDetail("O(N)", "Time Complexity: Single pass with two pointers left & right.", "Sliding Window", "O(1)", "Space Complexity: Bounded by 256 character map.", "HashMap", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // Bulk register remaining 11 Sliding Window problems
        populateRemainingSlidingWindowProblems();
    }

    private void populateRemainingSlidingWindowProblems() {
        String[][] list = new String[][]{
            {"max-consecutive-ones-3", "Max Consecutive Ones III", "Sliding Window - Medium", "Medium", "Flip at most K zeroes to maximize consecutive 1s."},
            {"fruit-into-baskets", "Fruit Into Baskets", "Sliding Window - Medium", "Medium", "Collect maximum fruits using at most 2 basket types (K=2)."},
            {"longest-repeating-character-replacement", "Longest Repeating Character Replacement", "Sliding Window - Medium", "Medium", "Replace at most K characters to maximize repeating character length."},
            {"binary-subarrays-with-sum", "Binary Subarrays With Sum", "Sliding Window - Medium", "Medium", "Count subarrays with sum equal to target using Sliding Window atMost(Goal)."},
            {"count-nice-subarrays", "Count Number of Nice Subarrays", "Sliding Window - Medium", "Medium", "Count subarrays containing exactly K odd numbers."},
            {"number-substrings-all-three-chars", "Number of Substrings Containing All Three Characters", "Sliding Window - Medium", "Medium", "Count substrings containing at least one 'a', 'b', and 'c'."},
            {"maximum-points-cards", "Maximum Points You Can Obtain from Cards", "Sliding Window - Medium", "Medium", "Pick K cards from start or end to maximize total score."},
            {"longest-substring-k-distinct", "Longest Substring With At Most K Distinct", "Sliding Window - Hard", "Medium", "Longest substring containing at most K distinct characters."},
            {"subarrays-k-different-integers", "Subarrays with K Different Integers", "Sliding Window - Hard", "Hard", "Count subarrays with exactly K different integers using atMost(K) - atMost(K-1)."},
            {"minimum-window-substring", "Minimum Window Substring", "Sliding Window - Hard", "Hard", "Find minimum window substring containing all characters of pattern T."},
            {"minimum-window-subsequence", "Minimum Window Subsequence", "Sliding Window - Hard", "Hard", "Find minimum window subsequence matching S2 in S1."}
        };

        // These two now have real tracers that trace a string window, not an int array.
        Set<String> stringDsType = Set.of("longest-repeating-character-replacement", "minimum-window-substring");

        for (String[] p : list) {
            String id = p[0]; String title = p[1]; String cat = p[2]; String diff = p[3]; String desc = p[4];
            String dsType = stringDsType.contains(id) ? "String" : "Array";
            problems.put(id, new ProblemDetail(
                id, title, cat, "Sliding Window", diff, desc,
                String.format("// Java Implementation for %s\npublic int solve() {\n    // Sliding Window Striver A2Z Implementation\n    return 0;\n}", title),
                null, null, null, createArrayState(new int[]{1, 1, 0, 1, 1}, -1, -1), null, null, null,
                new ComplexityDetail("O(N)", "Time Complexity: Two pointers left & right move linearly.", "Sliding Window", "O(1)", "Space Complexity: Character frequency map space.", "Memory", "Auxiliary Space: O(1)", "Memory"), dsType
            ));
        }
    }

    private ExecutionStep createStep(int stepNum, int line, String desc, List<ArrayElement> arrayState, Map<String, String> vars) {
        return new ExecutionStep(
            stepNum, line, desc,
            List.of(), Map.of(), List.of(), vars,
            "Array", null, arrayState, null, null
        );
    }

    private List<ArrayElement> createRangeArrayState(int[] vals, int start, int end) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            String state = (i >= start && i <= end) ? "active" : "default";
            list.add(new ArrayElement(i, vals[i], state));
        }
        return list;
    }

    // Step Generators
    private List<ExecutionStep> generateLongestSubstringSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] vals = new int[]{1, 2, 3, 1, 2, 3, 1, 1};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 4, "Sliding Window: Input s = \"abcabcbb\". Initialize left = 0, right = 0, maxLen = 0.", createRangeArrayState(vals, 0, 0), Map.of("left", "0", "right", "0", "maxLen", "0")));
        steps.add(createStep(stepNum++, 8, "Expand window right -> 2 (\"abc\"). All characters unique! maxLen = 3.", createRangeArrayState(vals, 0, 2), Map.of("left", "0", "right", "2", "maxLen", "3")));
        steps.add(createStep(stepNum++, 10, "Duplicate 'a' found at right=3. Shrink window left -> 1 (\"bca\"). maxLen remains 3.", createRangeArrayState(vals, 1, 3), Map.of("left", "1", "right", "3", "maxLen", "3")));
        steps.add(createStep(stepNum++, 12, "Sliding Window Complete! maxLen = 3 (Substring \"abc\").", createRangeArrayState(vals, 0, 2), Map.of("maxLen", "3")));
        return steps;
    }

    private List<ExecutionStep> generateMaxConsecutiveOnesSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0};
        int k = 2;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Max Consecutive Ones III: nums = [1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0], K = 2 (Flips allowed).", createRangeArrayState(nums, 0, 0), Map.of("K", "2", "zeroCount", "0")));
        steps.add(createStep(stepNum++, 6, "Expand window right -> 5: Zero count = 3 > K=2! Shrink left -> 3.", createRangeArrayState(nums, 3, 5), Map.of("left", "3", "right", "5", "zeroCount", "3")));
        steps.add(createStep(stepNum++, 9, "Expand window to index 9 [1, 1, 1, 1] with 2 zero flips! Max Consecutive Ones = 6.", createRangeArrayState(nums, 4, 9), Map.of("maxConsecutiveOnes", "6")));
        return steps;
    }

    private List<ArrayElement> createArrayState(int[] vals, int idx1, int idx2) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            String st = (i == idx1 || i == idx2) ? "active" : "default";
            list.add(new ArrayElement(i, vals[i], st));
        }
        return list;
    }
}
