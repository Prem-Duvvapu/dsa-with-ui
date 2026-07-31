package com.dsa.ui.service;

import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BitManipulationService {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public BitManipulationService() {
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
            case "single-number": return generateSingleNumberSteps();
            case "subsets-bitmasking": return generateBitmaskSubsetsSteps();
            default: return generateSingleNumberSteps();
        }
    }

    private void initProblems() {
        // 1. Single Number
        problems.put("single-number", new ProblemDetail(
            "single-number", "Single Number (XOR Property)", "Bit Manipulation - Easy", "Bit Manipulation", "Easy",
            "Given a non-empty array of integers nums, every element appears twice except for one. Find that single one using XOR operator (a ^ a = 0).",
            """
            // Java Single Number via XOR (LeetCode 136)
            public int singleNumber(int[] nums) {
                int xor = 0;
                for (int num : nums) {
                    xor = xor ^ num;
                }
                return xor;
            }
            """,
            null, null, null, createArrayState(new int[]{4, 1, 2, 1, 2}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass iteration over array of size N.",
                "Why XOR works? XOR satisfies properties: a ^ a = 0 and a ^ 0 = a. All paired duplicate numbers cancel out to 0, leaving the single unique number.",
                "O(1)",
                "Space Complexity: Constant O(1) space.",
                "Why O(1)? Uses single integer variable `xor`.",
                "Auxiliary Space: O(1)",
                "Return Value: O(1)"
            ),
            "Array"
        ));

        // 2. Subsets using Bitmasking
        problems.put("subsets-bitmasking", new ProblemDetail(
            "subsets-bitmasking", "Subsets using Bitmasking", "Bit Manipulation - Subsets", "Bit Manipulation", "Medium",
            "Generate all 2^N subsets of an array using integer binary bitmasks from 0 to (2^N - 1).",
            """
            // Java Subsets via Bitmasking (LeetCode 78)
            public List<List<Integer>> subsets(int[] nums) {
                int n = nums.length;
                int subsetsCount = 1 << n; // 2^N
                List<List<Integer>> ans = new ArrayList<>();

                for (int num = 0; num < subsetsCount; num++) {
                    List<Integer> list = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        if ((num & (1 << i)) != 0) { // Check if i-th bit is set!
                            list.add(nums[i]);
                        }
                    }
                    ans.add(list);
                }
                return ans;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 3}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(2^N * N)",
                "Time Complexity: Outer loop runs 2^N times. Inner loop checks N bits for each bitmask.",
                "Why Bitmasking generates power set? Integer representation of 0 to (2^N - 1) has bits corresponding 1-to-1 with picking/not-picking array elements.",
                "O(1)",
                "Space Complexity: Constant O(1) extra space (excluding returned subsets list).",
                "Why O(1)? No recursion call stack required.",
                "Auxiliary Space: O(1)",
                "Subsets List: O(2^N * N)"
            ),
            "Array"
        ));
    }

    private List<ExecutionStep> generateSingleNumberSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{4, 1, 2, 1, 2};
        int xor = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Single Number: Input nums = [4, 1, 2, 1, 2]. Initialize running xor = 0.",
            List.of(), Map.of(), List.of(), Map.of("xor", "0"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = 0; i < nums.length; i++) {
            int prevXor = xor;
            xor ^= nums[i];
            steps.add(new ExecutionStep(
                stepNum++, 6,
                String.format("Loop i = %d (val %d): xor = %d ^ %d = %d (Binary: %s).", i, nums[i], prevXor, nums[i], xor, Integer.toBinaryString(xor)),
                List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "num", String.valueOf(nums[i]), "xor", String.valueOf(xor)),
                "Array", null, createArrayState(nums, i, -1), null, null
            ));
        }

        steps.add(new ExecutionStep(
            stepNum++, 8,
            "Single Number Complete! All paired duplicates (1^1=0, 2^2=0) canceled out! Single unique element = 4.",
            List.of(), Map.of(), List.of(), Map.of("Single Number", "4"),
            "Array", null, createArrayState(nums, 0, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateBitmaskSubsetsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 2, 3};
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Bitmasking Subsets: Array = [1, 2, 3] (N = 3). Total Subsets = 2^3 = 8. Bitmasks range from 0 (000) to 7 (111).",
            List.of(), Map.of(), List.of(), Map.of("Subsets Count", "8"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 10,
            "Bitmask 0 (000_2): No bits set -> Subsets: [].",
            List.of(), Map.of(), List.of(), Map.of("bitmask", "000", "subset", "[]"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 10,
            "Bitmask 1 (001_2): Bit 0 set -> Subsets: [1].",
            List.of(), Map.of(), List.of(), Map.of("bitmask", "001", "subset", "[1]"),
            "Array", null, createArrayState(nums, 0, -1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 10,
            "Bitmask 3 (011_2): Bits 0, 1 set -> Subsets: [1, 2].",
            List.of(), Map.of(), List.of(), Map.of("bitmask", "011", "subset", "[1, 2]"),
            "Array", null, createArrayState(nums, 0, 1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 15,
            "Bitmasking Subsets Complete! All 8 power set subsets generated without recursion!",
            List.of(), Map.of(), List.of(), Map.of("Total Generated", "8"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

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
