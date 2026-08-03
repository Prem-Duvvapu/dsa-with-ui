package com.dsa.ui.service;

import com.dsa.ui.algorithm.trie.*;
import com.dsa.ui.model.*;
import com.dsa.ui.trace.ListTraceRecorder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TrieService {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public TrieService() {
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
            case "implement-trie": return generateImplementTrieSteps();
            case "longest-common-prefix": return generateLcpSteps();
            case "word-break-trie": return generateWordBreakSteps();
            default: return generateImplementTrieSteps();
        }
    }

    private void initProblems() {
        // 1. Implement Trie (Prefix Tree)
        problems.put("implement-trie", new ProblemDetail(
            "implement-trie", "Implement Trie (Prefix Tree)", "Tries - Basics", "Tries & Prefixes", "Medium",
            "A Trie or Prefix Tree is a tree data structure used to efficiently store and retrieve keys in a dataset of strings.",
            """
            // Java Trie Node Structure (LeetCode 208)
            class Node {
                Node links[] = new Node[26];
                boolean flag = false;

                boolean containsKey(char ch) { return links[ch - 'a'] != null; }
                void put(char ch, Node node) { links[ch - 'a'] = node; }
                Node get(char ch) { return links[ch - 'a']; }
                void setEnd() { flag = true; }
                boolean isEnd() { return flag; }
            }

            public class Trie {
                private static Node root;
                public Trie() { root = new Node(); }

                public void insert(String word) {
                    Node node = root;
                    for (int i = 0; i < word.length(); i++) {
                        if (!node.containsKey(word.charAt(i))) {
                            node.put(word.charAt(i), new Node());
                        }
                        node = node.get(word.charAt(i));
                    }
                    node.setEnd();
                }

                public boolean search(String word) {
                    Node node = root;
                    for (int i = 0; i < word.length(); i++) {
                        if (!node.containsKey(word.charAt(i))) return false;
                        node = node.get(word.charAt(i));
                    }
                    return node.isEnd();
                }

                public boolean startsWith(String prefix) {
                    Node node = root;
                    for (int i = 0; i < prefix.length(); i++) {
                        if (!node.containsKey(prefix.charAt(i))) return false;
                        node = node.get(prefix.charAt(i));
                    }
                    return true;
                }
            }
            """,
            null, null, createTrieTreeNodes(), null, null, null, null,
            new ComplexityDetail(
                "O(L)",
                "Time Complexity: Insertion, Search, and Prefix checking take O(L) time where L is word length.",
                "Why O(L) instead of O(N)? Trie checks key characters one by one via array index links[ch - 'a'] in O(1) time per character.",
                "O(N x L)",
                "Space Complexity: In worst case, storing N words of max length L requires up to O(N x L x 26) node references.",
                "Why 26 pointers per node? Each node has an array of size 26 for English lowercase letters 'a' through 'z'.",
                "Auxiliary Space: O(26) per node",
                "Total Trie Space: O(N x L)"
            ),
            "Stack"
        ));

        // 2. Longest Common Prefix
        problems.put("longest-common-prefix", new ProblemDetail(
            "longest-common-prefix", "Longest Common Prefix", "Tries - Applications", "Tries & Prefixes", "Easy",
            "Find the longest common prefix string amongst an array of strings using Trie.",
            """
            // Java Longest Common Prefix via Trie (LeetCode 14)
            public String longestCommonPrefix(String[] strs) {
                if (strs == null || strs.length == 0) return "";
                Trie trie = new Trie();
                for (String word : strs) trie.insert(word);

                StringBuilder prefix = new StringBuilder();
                Node curr = trie.root;
                while (curr != null && countChildren(curr) == 1 && !curr.isEnd()) {
                    int childIdx = getOnlyChild(curr);
                    prefix.append((char)('a' + childIdx));
                    curr = curr.links[childIdx];
                }
                return prefix.toString();
            }
            """,
            null, null, createLcpTreeNodes(), null, null, null, null,
            new ComplexityDetail(
                "O(N x L)",
                "Time Complexity: O(N x L) to build Trie + O(L) to traverse single-child branch.",
                "Why Trie for LCP? Single-child branches in the Trie directly represent the shared prefix of all inserted strings.",
                "O(N x L)",
                "Space Complexity: Trie storage space for string array.",
                "Why O(N x L)? Trie nodes store character links for all strings.",
                "Auxiliary Space: O(N x L)",
                "Prefix Output: O(L)"
            ),
            "Stack"
        ));

        // 3. Word Break Problem using Trie
        problems.put("word-break-trie", new ProblemDetail(
            "word-break-trie", "Word Break Problem", "Tries - Hard", "Tries & Prefixes", "Medium",
            "Given a string s and a dictionary of strings wordDict, return true if s can be segmented into a space-separated sequence of dictionary words.",
            """
            // Java Word Break with Trie + DP (LeetCode 139)
            public boolean wordBreak(String s, List<String> wordDict) {
                Trie trie = new Trie();
                for (String w : wordDict) trie.insert(w);

                int n = s.length();
                boolean[] dp = new boolean[n + 1];
                dp[0] = true;

                for (int i = 0; i < n; i++) {
                    if (!dp[i]) continue;
                    Node curr = trie.root;
                    for (int j = i; j < n; j++) {
                        char ch = s.charAt(j);
                        if (!curr.containsKey(ch)) break;
                        curr = curr.get(ch);
                        if (curr.isEnd()) dp[j + 1] = true;
                    }
                }
                return dp[n];
            }
            """,
            null, null, createWordBreakTreeNodes(), null, null, null, null,
            new ComplexityDetail(
                "O(N^2 + W x L)",
                "Time Complexity: O(W x L) to insert dictionary + O(N^2) DP loop with Trie matching.",
                "Why Trie accelerates Word Break? Stops inner loop instantly as soon as curr.containsKey(ch) is false.",
                "O(N + W x L)",
                "Space Complexity: Trie memory O(W x L) + DP boolean array O(N).",
                "Why O(N)? dp[i] records whether prefix s[0..i-1] can be segmented.",
                "Auxiliary Space: O(N)",
                "Trie Memory: O(W x L)"
            ),
            "Stack"
        ));
    }

    // Step Generators
    private List<ExecutionStep> generateImplementTrieSteps() {
        ImplementTrie.Node root = new ImplementTrie.Node();
        ListTraceRecorder recorder = new ListTraceRecorder();
        new ImplementTrie().insert(root, "apple", recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateLcpSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        steps.add(new ExecutionStep(1, 4, "Longest Common Prefix: Insert strings [\"flower\", \"flow\", \"flight\"] into Trie.", List.of("LCP"), Map.of(), List.of(), Map.of("Input", "[\"flower\", \"flow\", \"flight\"]"), "Stack", null));
        steps.add(new ExecutionStep(2, 9, "Traverse Trie from root: Node 'f' has 1 child ('l'). Node 'l' has 1 child ('o' and 'i' split at node 'l'!).", List.of("LCP traversal"), Map.of(), List.of(), Map.of("Common Prefix", "\"fl\""), "Stack", null));
        steps.add(new ExecutionStep(3, 13, "Longest Common Prefix Complete! Result = \"fl\".", List.of(), Map.of(), List.of(), Map.of("LCP", "\"fl\""), "Stack", null));
        return steps;
    }

    private List<ExecutionStep> generateWordBreakSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        steps.add(new ExecutionStep(1, 4, "Word Break: s = \"leetcode\", wordDict = [\"leet\", \"code\"]. Insert dict into Trie.", List.of("wordBreak"), Map.of(), List.of(), Map.of("s", "leetcode"), "Stack", null));
        steps.add(new ExecutionStep(2, 11, "Match s[0..3] (\"leet\") in Trie -> isEnd() TRUE! Set dp[4] = true.", List.of("dp[4] = true"), Map.of(), List.of(), Map.of("match1", "\"leet\""), "Stack", null));
        steps.add(new ExecutionStep(3, 11, "Match s[4..7] (\"code\") in Trie -> isEnd() TRUE! Set dp[8] = true.", List.of("dp[8] = true"), Map.of(), List.of(), Map.of("match2", "\"code\""), "Stack", null));
        steps.add(new ExecutionStep(4, 15, "Word Break Complete! dp[8] is TRUE. String \"leetcode\" can be segmented!", List.of(), Map.of(), List.of(), Map.of("Result", "TRUE"), "Stack", null));
        return steps;
    }

    // Helper tree nodes
    private List<TreeNode> createTrieTreeNodes() {
        return List.of(
            new TreeNode(1, "root", 190, 40, 2, 3, "unvisited"),
            new TreeNode(2, "'a'", 110, 110, 4, null, "unvisited"),
            new TreeNode(3, "'b'", 270, 110, null, null, "unvisited"),
            new TreeNode(4, "'p'", 70, 180, null, null, "unvisited")
        );
    }

    private List<TreeNode> createLcpTreeNodes() {
        return List.of(
            new TreeNode(1, "root", 190, 40, 2, null, "unvisited"),
            new TreeNode(2, "'f'", 190, 100, 3, null, "unvisited"),
            new TreeNode(3, "'l'", 190, 160, null, null, "unvisited")
        );
    }

    private List<TreeNode> createWordBreakTreeNodes() {
        return List.of(
            new TreeNode(1, "root", 190, 40, 2, 3, "unvisited"),
            new TreeNode(2, "\"leet\"", 110, 110, null, null, "unvisited"),
            new TreeNode(3, "\"code\"", 270, 110, null, null, "unvisited")
        );
    }
}
