package com.dsa.ui.service;

import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StringService {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public StringService() {
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
            case "longest-substring-without-repeating": return generateLongestSubstringSteps();
            case "valid-anagram": return generateValidAnagramSteps();
            default: return generateLongestSubstringSteps();
        }
    }

    private void initProblems() {
        // 1. Longest Substring Without Repeating Characters
        problems.put("longest-substring-without-repeating", new ProblemDetail(
            "longest-substring-without-repeating", "Longest Substring Without Repeating Characters", "Strings - Sliding Window", "Strings", "Medium",
            "Given a string s, find the length of the longest substring without repeating characters using Sliding Window + HashMap.",
            """
            // Java Sliding Window (LeetCode 3)
            public int lengthOfLongestSubstring(String s) {
                HashMap<Character, Integer> map = new HashMap<>();
                int left = 0, right = 0, maxLen = 0;
                int n = s.length();

                while (right < n) {
                    char ch = s.charAt(right);
                    if (map.containsKey(ch)) {
                        left = Math.max(left, map.get(ch) + 1);
                    }
                    map.put(ch, right);
                    maxLen = Math.max(maxLen, right - left + 1);
                    right++;
                }
                return maxLen;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 3, 1, 2, 3, 1, 1}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass with two pointers `left` and `right` traversing string of length N.",
                "Why HashMap jump works? When character `ch` repeats, `left` pointer instantly jumps past its last seen index (`map.get(ch) + 1`).",
                "O(256) = O(1)",
                "Space Complexity: Auxiliary HashMap space bounded by character set size (256 ASCII characters).",
                "Why O(1)? Map size never exceeds 256.",
                "Auxiliary Space: O(1)",
                "Return Length: O(1)"
            ),
            "Array"
        ));

        // 2. Valid Anagram
        problems.put("valid-anagram", new ProblemDetail(
            "valid-anagram", "Valid Anagram", "Strings - Basics", "Strings", "Easy",
            "Given two strings s and t, return true if t is an anagram of s, and false otherwise.",
            """
            // Java Frequency Array (LeetCode 242)
            public boolean isAnagram(String s, String t) {
                if (s.length() != t.length()) return false;
                int[] count = new int[26];
                for (int i = 0; i < s.length(); i++) {
                    count[s.charAt(i) - 'a']++;
                    count[t.charAt(i) - 'a']--;
                }
                for (int c : count) {
                    if (c != 0) return false;
                }
                return true;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 1, 1, 0}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass iteration over string length N.",
                "Why frequency array works? Increments count for chars in `s` and decrements count for chars in `t`. All counts must equal 0.",
                "O(1)",
                "Space Complexity: Constant array of size 26 for English lowercase letters.",
                "Why O(1)? Fixed array size 26.",
                "Auxiliary Space: O(26) = O(1)",
                "Return Boolean: O(1)"
            ),
            "Array"
        ));
    }

    private List<ExecutionStep> generateLongestSubstringSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] vals = new int[]{1, 2, 3, 1, 2, 3, 1, 1}; // "abcabcbb"
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Input String s = \"abcabcbb\". Initialize Sliding Window pointers left = 0, right = 0, maxLen = 0.",
            List.of(), Map.of(), List.of(), Map.of("left", "0", "right", "0", "maxLen", "0"),
            "Array", null, createArrayState(vals, 0, 0), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 11,
            "right = 0 ('a'): Map put ('a', 0). Window [0..0] (\"a\"). maxLen = 1.",
            List.of(), Map.of(), List.of(), Map.of("left", "0", "right", "0", "maxLen", "1"),
            "Array", null, createArrayState(vals, 0, 0), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 11,
            "right = 1 ('b'): Map put ('b', 1). Window [0..1] (\"ab\"). maxLen = 2.",
            List.of(), Map.of(), List.of(), Map.of("left", "0", "right", "1", "maxLen", "2"),
            "Array", null, createArrayState(vals, 0, 1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 11,
            "right = 2 ('c'): Map put ('c', 2). Window [0..2] (\"abc\"). maxLen = 3.",
            List.of(), Map.of(), List.of(), Map.of("left", "0", "right", "2", "maxLen", "3"),
            "Array", null, createArrayState(vals, 0, 2), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 9,
            "right = 3 ('a'): Duplicate 'a' detected at index 0! Jump left = 0 + 1 = 1. Window [1..3] (\"bca\"). maxLen remains 3.",
            List.of(), Map.of(), List.of(), Map.of("jump_left", "1", "maxLen", "3"),
            "Array", null, createArrayState(vals, 1, 3), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 14,
            "Longest Substring Without Repeating Complete! maxLen = 3 (Substring \"abc\").",
            List.of(), Map.of(), List.of(), Map.of("maxLen", "3"),
            "Array", null, createArrayState(vals, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateValidAnagramSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] vals = new int[]{1, 1, 1, 0};
        steps.add(new ExecutionStep(1, 4, "Valid Anagram: Compare s = \"anagram\" and t = \"nagaram\". Check lengths: 7 == 7.", List.of(), Map.of(), List.of(), Map.of("length", "7"), "Array", null, createArrayState(vals, -1, -1), null, null));
        steps.add(new ExecutionStep(2, 7, "Pass: Increment counts for 's' and decrement for 't'. All 26 frequency counts cancel out to 0!", List.of(), Map.of(), List.of(), Map.of("count", "all 0"), "Array", null, createArrayState(vals, -1, -1), null, null));
        steps.add(new ExecutionStep(3, 11, "Valid Anagram Complete! Return TRUE. Strings are valid anagrams.", List.of(), Map.of(), List.of(), Map.of("Result", "TRUE"), "Array", null, createArrayState(vals, -1, -1), null, null));
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
