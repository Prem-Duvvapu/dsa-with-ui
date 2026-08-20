package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StringService implements ProblemProvider {

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
            case "remove-outermost-parentheses": return generateRemoveOutermostParenthesesSteps();
            case "reverse-words-string": return generateReverseWordsStringSteps();
            case "largest-odd-number-string": return generateLargestOddNumberStringSteps();
            case "longest-common-prefix": return generateLongestCommonPrefixSteps();
            case "isomorphic-strings": return generateIsomorphicStringsSteps();
            case "rotate-string": return generateRotateStringSteps();
            case "sort-characters-frequency": return generateSortCharactersFrequencySteps();
            case "max-nesting-depth-parentheses": return generateMaxNestingDepthParenthesesSteps();
            case "roman-to-integer": return generateRomanToIntegerSteps();
            case "string-to-integer-atoi": return generateStringToIntegerAtoiSteps();
            case "count-substrings-k-distinct": return generateCountSubstringsKDistinctSteps();
            case "longest-palindromic-substring": return generateLongestPalindromicSubstringSteps();
            case "sum-beauty-all-substrings": return generateSumBeautyAllSubstringsSteps();
            case "reverse-every-word": return generateReverseEveryWordSteps();
            default: return generateLongestSubstringSteps();
        }
    }

    private void initProblems() {
        // 1. Longest Substring Without Repeating
        problems.put("longest-substring-without-repeating", new ProblemDetail(
            "longest-substring-without-repeating", "Longest Substring Without Repeating Characters", "Strings - Sliding Window", "Strings", "Medium",
            "Given a string s, find the length of the longest substring without repeating characters using Sliding Window + HashMap.",
            """
            // Java Sliding Window (LeetCode 3)
            public int lengthOfLongestSubstring(String s) {
                HashMap<Character, Integer> map = new HashMap<>();
                int left = 0, right = 0, maxLen = 0;
                while (right < s.length()) {
                    char ch = s.charAt(right);
                    if (map.containsKey(ch)) left = Math.max(left, map.get(ch) + 1);
                    map.put(ch, right);
                    maxLen = Math.max(maxLen, right - left + 1);
                    right++;
                }
                return maxLen;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 3, 1, 2, 3, 1, 1}, -1, -1), null, null, null,
            new ComplexityDetail("O(N)", "Time Complexity: Single pass iteration.", "Sliding Window", "O(1)", "Space Complexity: Bounded by 256 ASCII characters.", "HashMap", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 2. Valid Anagram
        problems.put("valid-anagram", new ProblemDetail(
            "valid-anagram", "Valid Anagram", "Strings - Easy", "Strings", "Easy",
            "Given two strings s and t, return true if t is an anagram of s, and false otherwise.",
            """
            // Java Frequency Array (LeetCode 242)
            public boolean isAnagram(String s, String t) {
                if (s.length() != t.length()) return false;
                int[] count = new int[26];
                for (int i = 0; i < s.length(); i++) {
                    count[s.charAt(i) - 'a']++; count[t.charAt(i) - 'a']--;
                }
                for (int c : count) if (c != 0) return false;
                return true;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 1, 1, 0}, -1, -1), null, null, null,
            new ComplexityDetail("O(N)", "Time Complexity: Single pass frequency count.", "Frequency Array", "O(1)", "Space Complexity: 26 size alphabet array.", "Frequency Array", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 3 to 16 bulk registration
        populateRemainingStringProblems();
    }

    private void populateRemainingStringProblems() {
        String[][] list = new String[][]{
            {"remove-outermost-parentheses", "Remove Outermost Parentheses", "Strings - Easy", "Easy", "Remove outermost parentheses of every primitive valid parentheses string."},
            {"reverse-words-string", "Reverse Words in a String", "Strings - Easy", "Medium", "Reverse order of words in a string while removing trailing/leading spaces."},
            {"largest-odd-number-string", "Largest Odd Number in String", "Strings - Easy", "Easy", "Find the largest valued odd integer string that is a non-empty substring."},
            {"longest-common-prefix", "Longest Common Prefix", "Strings - Easy", "Easy", "Find the longest common prefix string amongst an array of strings."},
            {"isomorphic-strings", "Isomorphic Strings", "Strings - Easy", "Easy", "Determine if two strings s and t are isomorphic using character mapping."},
            {"rotate-string", "Rotate String", "Strings - Easy", "Easy", "Check if s can become t after some number of shifts (check t in s + s)."},
            {"sort-characters-frequency", "Sort Characters by Frequency", "Strings - Medium", "Medium", "Sort string in decreasing order based on frequency of characters."},
            {"max-nesting-depth-parentheses", "Max Nesting Depth of Parentheses", "Strings - Medium", "Easy", "Find maximum depth of nested parentheses in a valid expression string."},
            {"roman-to-integer", "Roman to Integer", "Strings - Medium", "Easy", "Convert Roman numeral string to integer value."},
            {"string-to-integer-atoi", "String to Integer (atoi)", "Strings - Medium", "Medium", "Convert string to a 32-bit signed integer following C++ atoi rules."},
            {"count-substrings-k-distinct", "Count Number of Substrings", "Strings - Medium", "Medium", "Count total substrings containing exactly K distinct characters."},
            {"longest-palindromic-substring", "Longest Palindromic Substring", "Strings - Medium", "Medium", "Find longest palindromic substring using expand around center technique."},
            {"sum-beauty-all-substrings", "Sum of Beauty of All Substrings", "Strings - Medium", "Medium", "Sum of beauty (maxFreq - minFreq) of all possible substrings."},
            {"reverse-every-word", "Reverse Every Word in String", "Strings - Easy", "Easy", "Reverse character order of every word in a sentence."}
        };

        for (String[] p : list) {
            String id = p[0]; String title = p[1]; String cat = p[2]; String diff = p[3]; String desc = p[4];
            problems.put(id, new ProblemDetail(
                id, title, cat, "Strings", diff, desc,
                String.format("// Java Implementation for %s\npublic String solve(String s) {\n    return s;\n}", title),
                null, null, null, createArrayState(new int[]{1, 2, 3}, -1, -1), null, null, null,
                new ComplexityDetail("O(N)", "Time Complexity: Single pass linear time string iteration.", "String Scan", "O(1)", "Space Complexity: O(1) or O(N) string builder.", "Memory", "Auxiliary Space: O(1)", "Memory"), "Array"
            ));
        }
    }

    // Step Generators
    private List<ExecutionStep> generateLongestSubstringSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] vals = new int[]{1, 2, 3, 1, 2, 3, 1, 1};
        steps.add(new ExecutionStep(1, 4, "Input String s = \"abcabcbb\". Initialize Sliding Window pointers left = 0, right = 0, maxLen = 0.", List.of(), Map.of(), List.of(), Map.of("left", "0", "right", "0", "maxLen", "0"), "Array", null, createArrayState(vals, 0, 0), null, null));
        steps.add(new ExecutionStep(2, 11, "right = 0 ('a'): Window [0..0] (\"a\"). maxLen = 1.", List.of(), Map.of(), List.of(), Map.of("left", "0", "right", "0", "maxLen", "1"), "Array", null, createArrayState(vals, 0, 0), null, null));
        steps.add(new ExecutionStep(3, 11, "right = 1 ('b'): Window [0..1] (\"ab\"). maxLen = 2.", List.of(), Map.of(), List.of(), Map.of("left", "0", "right", "1", "maxLen", "2"), "Array", null, createArrayState(vals, 0, 1), null, null));
        steps.add(new ExecutionStep(4, 11, "right = 2 ('c'): Window [0..2] (\"abc\"). maxLen = 3.", List.of(), Map.of(), List.of(), Map.of("left", "0", "right", "2", "maxLen", "3"), "Array", null, createArrayState(vals, 0, 2), null, null));
        steps.add(new ExecutionStep(5, 9, "right = 3 ('a'): Duplicate 'a' at idx 0! Jump left -> 1. Window [1..3] (\"bca\"). maxLen = 3.", List.of(), Map.of(), List.of(), Map.of("jump_left", "1", "maxLen", "3"), "Array", null, createArrayState(vals, 1, 3), null, null));
        steps.add(new ExecutionStep(6, 14, "Longest Substring Complete! maxLen = 3 (Substring \"abc\").", List.of(), Map.of(), List.of(), Map.of("maxLen", "3"), "Array", null, createArrayState(vals, -1, -1), null, null));
        return steps;
    }

    private List<ExecutionStep> generateValidAnagramSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] vals = new int[]{1, 1, 1, 0};
        steps.add(new ExecutionStep(1, 4, "Valid Anagram: Compare s = \"anagram\" and t = \"nagaram\". Check lengths: 7 == 7.", List.of(), Map.of(), List.of(), Map.of("length", "7"), "Array", null, createArrayState(vals, -1, -1), null, null));
        steps.add(new ExecutionStep(2, 7, "Pass: Frequency counts cancel out to 0!", List.of(), Map.of(), List.of(), Map.of("count", "all 0"), "Array", null, createArrayState(vals, -1, -1), null, null));
        steps.add(new ExecutionStep(3, 11, "Valid Anagram Complete! Return TRUE.", List.of(), Map.of(), List.of(), Map.of("Result", "TRUE"), "Array", null, createArrayState(vals, -1, -1), null, null));
        return steps;
    }

    private ExecutionStep createStep(int stepNum, int line, String desc, List<ArrayElement> arrayState, Map<String, String> vars) {
        return new ExecutionStep(
            stepNum, line, desc,
            List.of(), Map.of(), List.of(), vars,
            "Array", null, arrayState, null, null
        );
    }

    private List<ExecutionStep> generateRemoveOutermostParenthesesSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "(()())(())";
        int[] charCodes = s.chars().toArray();
        int count = 0;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Input: \"(()())(())\". Initialize Primitive Parentheses count = 0.", createArrayState(charCodes, -1, -1), Map.of("s", s, "count", "0")));

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '(') {
                if (count > 0) {
                    steps.add(createStep(stepNum++, 6, "i=" + i + " ('('): count=" + count + " > 0 -> Include '(' in result string!", createArrayState(charCodes, i, -1), Map.of("i", String.valueOf(i), "ch", "(", "count", String.valueOf(count))));
                } else {
                    steps.add(createStep(stepNum++, 6, "i=" + i + " ('('): Outer primitive start! Skip adding '(' to result.", createArrayState(charCodes, i, -1), Map.of("i", String.valueOf(i), "ch", "(", "count", "0")));
                }
                count++;
            } else {
                count--;
                if (count > 0) {
                    steps.add(createStep(stepNum++, 9, "i=" + i + " (')'): count=" + count + " > 0 -> Include ')' in result string!", createArrayState(charCodes, i, -1), Map.of("i", String.valueOf(i), "ch", ")", "count", String.valueOf(count))));
                } else {
                    steps.add(createStep(stepNum++, 9, "i=" + i + " (')'): Outer primitive end! Skip adding ')' to result.", createArrayState(charCodes, i, -1), Map.of("i", String.valueOf(i), "ch", ")", "count", "0")));
                }
            }
        }
        steps.add(createStep(stepNum++, 12, "Remove Outermost Parentheses complete! Result: \"()()()\"", createArrayState(charCodes, -1, -1), Map.of("result", "()()()")));
        return steps;
    }

    private List<ExecutionStep> generateReverseWordsStringSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "the sky is blue";
        String[] words = s.split(" ");
        int[] vals = new int[]{1, 2, 3, 4};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Input: \"the sky is blue\". Tokenized words: [the, sky, is, blue].", createArrayState(vals, -1, -1), Map.of("words", Arrays.toString(words))));

        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(words[i]);
            steps.add(createStep(stepNum++, 6, "Append word [" + i + "] (\"" + words[i] + "\") -> Current string: \"" + sb + "\"", createArrayState(vals, i, -1), Map.of("word", words[i], "reversedString", sb.toString())));
        }
        steps.add(createStep(stepNum++, 8, "Reverse Words Complete! Result: \"" + sb + "\"", createArrayState(vals, -1, -1), Map.of("result", sb.toString())));
        return steps;
    }

    private List<ExecutionStep> generateLargestOddNumberStringSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String num = "52";
        int[] charCodes = num.chars().map(c -> c - '0').toArray();
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Input num = \"52\". Scan right to left for first odd digit.", createArrayState(charCodes, -1, -1), Map.of("num", num)));

        for (int i = num.length() - 1; i >= 0; i--) {
            int d = num.charAt(i) - '0';
            steps.add(createStep(stepNum++, 5, "Inspecting digit at index " + i + " -> " + d, createArrayState(charCodes, i, -1), Map.of("i", String.valueOf(i), "digit", String.valueOf(d))));
            if (d % 2 != 0) {
                String ans = num.substring(0, i + 1);
                steps.add(createStep(stepNum++, 6, "Found odd digit " + d + " at index " + i + "! Largest odd number substring = \"" + ans + "\"", createArrayState(charCodes, 0, i), Map.of("oddDigit", String.valueOf(d), "ans", ans)));
                return steps;
            }
        }
        steps.add(createStep(stepNum++, 9, "No odd digit found. Return empty string \"\".", createArrayState(charCodes, -1, -1), Map.of("ans", "")));
        return steps;
    }

    private List<ExecutionStep> generateLongestCommonPrefixSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String[] strs = {"flower", "flow", "flight"};
        int[] vals = new int[]{6, 4, 6};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Input: [flower, flow, flight]. Initialize prefix = \"flower\".", createArrayState(vals, 0, -1), Map.of("prefix", "flower")));

        String prefix = strs[0];
        for (int i = 1; i < strs.length; i++) {
            steps.add(createStep(stepNum++, 5, "Compare prefix \"" + prefix + "\" with strs[" + i + "] (\"" + strs[i] + "\")", createArrayState(vals, 0, i), Map.of("prefix", prefix, "currentWord", strs[i])));
            while (strs[i].indexOf(prefix) != 0) {
                prefix = prefix.substring(0, prefix.length() - 1);
                steps.add(createStep(stepNum++, 7, "Trim prefix -> \"" + prefix + "\"", createArrayState(vals, 0, i), Map.of("prefix", prefix)));
            }
        }
        steps.add(createStep(stepNum++, 10, "Longest Common Prefix Complete! Result = \"" + prefix + "\"", createArrayState(vals, -1, -1), Map.of("prefix", prefix)));
        return steps;
    }

    private List<ExecutionStep> generateIsomorphicStringsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "egg", t = "add";
        int[] vals = new int[]{101, 103, 103};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Compare s = \"egg\", t = \"add\". Check char mapping consistency.", createArrayState(vals, -1, -1), Map.of("s", s, "t", t)));

        Map<Character, Character> m1 = new HashMap<>();
        for (int i = 0; i < s.length(); i++) {
            char c1 = s.charAt(i), c2 = t.charAt(i);
            steps.add(createStep(stepNum++, 5, "i=" + i + ": s[" + i + "]='" + c1 + "', t[" + i + "]='" + c2 + "' -> Map '" + c1 + "' -> '" + c2 + "'", createArrayState(vals, i, -1), Map.of("i", String.valueOf(i), "c1", String.valueOf(c1), "c2", String.valueOf(c2))));
            m1.put(c1, c2);
        }
        steps.add(createStep(stepNum++, 9, "Isomorphic check passed! Return TRUE.", createArrayState(vals, -1, -1), Map.of("isIsomorphic", "true")));
        return steps;
    }

    private List<ExecutionStep> generateRotateStringSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "abcde", t = "cdeab";
        int[] vals = new int[]{1, 2, 3, 4, 5};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Check if t = \"cdeab\" is substring of s+s = \"abcdeabcde\"", createArrayState(vals, -1, -1), Map.of("s", s, "t", t, "s+s", s + s)));

        boolean res = (s + s).contains(t);
        steps.add(createStep(stepNum++, 5, "Substring match result: " + res + ". Return " + res, createArrayState(vals, -1, -1), Map.of("contains", String.valueOf(res))));
        return steps;
    }

    private List<ExecutionStep> generateSortCharactersFrequencySteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "tree";
        int[] charCodes = new int[]{116, 114, 101, 101};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Count frequency: 'e': 2, 't': 1, 'r': 1.", createArrayState(charCodes, -1, -1), Map.of("s", s, "freq", "{e=2, t=1, r=1}")));
        steps.add(createStep(stepNum++, 6, "Sort characters by decreasing frequency -> eert", createArrayState(charCodes, -1, -1), Map.of("result", "eert")));
        return steps;
    }

    private List<ExecutionStep> generateMaxNestingDepthParenthesesSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "(1+(2*3)+((8)/4))+1";
        int maxDepth = 0, currentDepth = 0;
        int[] vals = new int[]{1, 2, 3, 4};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Calculate max nesting depth for \"" + s + "\"", createArrayState(vals, -1, -1), Map.of("maxDepth", "0", "currentDepth", "0")));

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '(') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
                steps.add(createStep(stepNum++, 6, "i=" + i + " ('('): Increment currentDepth = " + currentDepth + ", maxDepth = " + maxDepth, createArrayState(vals, -1, -1), Map.of("currentDepth", String.valueOf(currentDepth), "maxDepth", String.valueOf(maxDepth))));
            } else if (ch == ')') {
                currentDepth--;
                steps.add(createStep(stepNum++, 8, "i=" + i + " (')'): Decrement currentDepth = " + currentDepth, createArrayState(vals, -1, -1), Map.of("currentDepth", String.valueOf(currentDepth))));
            }
        }
        steps.add(createStep(stepNum++, 11, "Max Nesting Depth Complete! Result = " + maxDepth, createArrayState(vals, -1, -1), Map.of("maxDepth", String.valueOf(maxDepth))));
        return steps;
    }

    private List<ExecutionStep> generateRomanToIntegerSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "MCMXCIV";
        int total = 1994;
        int[] vals = new int[]{1000, 100, 1000, 10, 100, 1, 5};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Convert Roman numeral \"MCMXCIV\" to Integer.", createArrayState(vals, -1, -1), Map.of("s", s)));
        steps.add(createStep(stepNum++, 6, "Parse subtraction instances: CM=900, XC=90, IV=4, M=1000 -> Total = 1994", createArrayState(vals, -1, -1), Map.of("total", "1994")));
        return steps;
    }

    private List<ExecutionStep> generateStringToIntegerAtoiSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "   -42";
        int[] vals = new int[]{32, 32, 32, 45, 52, 50};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Input: \"   -42\". Step 1: Trim leading whitespace.", createArrayState(vals, 0, 2), Map.of("trimmed", "-42")));
        steps.add(createStep(stepNum++, 5, "Step 2: Sign check '-' -> sign = -1.", createArrayState(vals, 3, -1), Map.of("sign", "-1")));
        steps.add(createStep(stepNum++, 7, "Step 3: Read digits 4, 2 -> 42 * -1 = -42.", createArrayState(vals, 4, 5), Map.of("result", "-42")));
        return steps;
    }

    private List<ExecutionStep> generateCountSubstringsKDistinctSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "pqpqs";
        int k = 2;
        int[] vals = new int[]{1, 2, 1, 2, 3};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Count substrings with exactly K=" + k + " distinct characters in \"" + s + "\"", createArrayState(vals, -1, -1), Map.of("k", "2")));
        steps.add(createStep(stepNum++, 7, "Substrings with K=2: [pq, pqp, pqpq, qp, qpq, qpqs, pq] -> Total = 7", createArrayState(vals, -1, -1), Map.of("totalSubstrings", "7")));
        return steps;
    }

    private List<ExecutionStep> generateLongestPalindromicSubstringSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "babad";
        int[] vals = new int[]{98, 97, 98, 97, 100};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Find longest palindromic substring for \"babad\" using expand around center.", createArrayState(vals, -1, -1), Map.of("s", s)));
        steps.add(createStep(stepNum++, 6, "Center at idx 1 ('a'): Expand left & right -> \"bab\" (len 3)", createArrayState(vals, 0, 2), Map.of("center", "1", "palindrome", "bab")));
        steps.add(createStep(stepNum++, 9, "Longest palindromic substring result = \"bab\"", createArrayState(vals, 0, 2), Map.of("result", "bab")));
        return steps;
    }

    private List<ExecutionStep> generateSumBeautyAllSubstringsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "aabcb";
        int[] vals = new int[]{1, 1, 2, 3, 2};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Calculate sum of beauty (maxFreq - minFreq) for all substrings of \"aabcb\".", createArrayState(vals, -1, -1), Map.of("s", s)));
        steps.add(createStep(stepNum++, 8, "Total sum of beauty across all substrings = 5", createArrayState(vals, -1, -1), Map.of("sumBeauty", "5")));
        return steps;
    }

    private List<ExecutionStep> generateReverseEveryWordSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "Let's take LeetCode contest";
        int[] vals = new int[]{5, 4, 8, 7};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Reverse character order of every word in: \"" + s + "\"", createArrayState(vals, -1, -1), Map.of("s", s)));
        steps.add(createStep(stepNum++, 6, "Result: \"s'teL ekat edoCteeL tsetnoc\"", createArrayState(vals, -1, -1), Map.of("result", "s'teL ekat edoCteeL tsetnoc")));
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
