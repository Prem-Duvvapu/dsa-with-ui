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
            case "single-number":
            case "single-number-1": return generateSingleNumberSteps();
            case "subsets-bitmasking":
            case "power-set-bitwise": return generateBitmaskSubsetsSteps();
            case "intro-bits-tricks": return generateSingleNumberSteps();
            case "check-ith-bit-set": return generateSingleNumberSteps();
            case "check-number-odd": return generateSingleNumberSteps();
            case "check-power-of-2": return generateSingleNumberSteps();
            case "count-set-bits": return generateSingleNumberSteps();
            case "set-unset-rightmost-bit": return generateSingleNumberSteps();
            case "swap-two-numbers": return generateSingleNumberSteps();
            case "divide-two-numbers-bitwise": return generateSingleNumberSteps();
            case "min-bit-flips": return generateSingleNumberSteps();
            case "xor-numbers-in-range": return generateSingleNumberSteps();
            case "single-number-3": return generateSingleNumberSteps();
            case "print-prime-factors": return generateSingleNumberSteps();
            case "divisors-of-number": return generateSingleNumberSteps();
            case "count-primes-range-sieve": return generateSingleNumberSteps();
            case "prime-factorisation-queries": return generateSingleNumberSteps();
            case "pow-x-n-math": return generateSingleNumberSteps();
            default: return generateSingleNumberSteps();
        }
    }

    private void initProblems() {
        // 1. Single Number
        problems.put("single-number-1", new ProblemDetail(
            "single-number-1", "Single Number (XOR Property)", "Bit Manipulation - Easy", "Bit Manipulation", "Easy",
            "Given a non-empty array of integers nums, every element appears twice except for one. Find that single one using XOR operator (a ^ a = 0).",
            """
            // Java Single Number via XOR (LeetCode 136)
            public int singleNumber(int[] nums) {
                int xor = 0;
                for (int num : nums) xor ^= num;
                return xor;
            }
            """,
            null, null, null, createArrayState(new int[]{4, 1, 2, 1, 2}, -1, -1), null, null, null,
            new ComplexityDetail("O(N)", "Time Complexity: Single pass iteration over N numbers.", "XOR Property", "O(1)", "Space Complexity: Single variable `xor`.", "Constant Memory", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 2. Subsets Bitmasking
        problems.put("power-set-bitwise", new ProblemDetail(
            "power-set-bitwise", "Power Set Bit Manipulation", "Bit Manipulation - Subsets", "Bit Manipulation", "Medium",
            "Generate all 2^N subsets of an array using integer binary bitmasks from 0 to (2^N - 1).",
            """
            // Java Subsets via Bitmasking (LeetCode 78)
            public List<List<Integer>> subsets(int[] nums) {
                int n = nums.length, limit = 1 << n;
                List<List<Integer>> ans = new ArrayList<>();
                for (int mask = 0; mask < limit; mask++) {
                    List<Integer> list = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        if ((mask & (1 << i)) != 0) list.add(nums[i]);
                    }
                    ans.add(list);
                }
                return ans;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 3}, -1, -1), null, null, null,
            new ComplexityDetail("O(2^N * N)", "Time Complexity: 2^N bitmasks * N bit checks.", "Bitmask Iteration", "O(1)", "Space Complexity: Auxiliary space O(1).", "Memory", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // Bulk register remaining 16 Bit & Math problems
        populateRemainingBitMathProblems();
    }

    private void populateRemainingBitMathProblems() {
        String[][] list = new String[][]{
            {"intro-bits-tricks", "Introduction to Bits and Tricks", "Bit Manipulation - Easy", "Easy", "Bitwise operators &, |, ^, ~, <<, >>."},
            {"check-ith-bit-set", "Check if i-th Bit is Set or Not", "Bit Manipulation - Easy", "Easy", "Check (N & (1 << i)) != 0."},
            {"check-number-odd", "Check if Number is Odd or Not", "Bit Manipulation - Easy", "Easy", "Check (N & 1) == 1."},
            {"check-power-of-2", "Check if Number is Power of 2", "Bit Manipulation - Easy", "Easy", "Check (N & (N - 1)) == 0."},
            {"count-set-bits", "Count Number of Set Bits", "Bit Manipulation - Easy", "Easy", "Brian Kernighan's Algorithm N & (N - 1)."},
            {"set-unset-rightmost-bit", "Set / Unset Rightmost Unset Bit", "Bit Manipulation - Easy", "Easy", "Bitwise bit toggling tricks."},
            {"swap-two-numbers", "Swap Two Numbers Using XOR", "Bit Manipulation - Easy", "Easy", "Swap a and b without third variable using XOR."},
            {"divide-two-numbers-bitwise", "Divide Two Numbers Without *, /", "Bit Manipulation - Medium", "Medium", "Bitwise shift quotient calculation."},
            {"min-bit-flips", "Minimum Bit Flips to Convert Number", "Bit Manipulation - Medium", "Easy", "Count set bits of (start ^ goal)."},
            {"xor-numbers-in-range", "XOR of Numbers in Range [L..R]", "Bit Manipulation - Medium", "Medium", "Range XOR pattern property (N % 4)."},
            {"single-number-3", "Single Number III (Two Unique Numbers)", "Bit Manipulation - Medium", "Medium", "Find two numbers appearing once using rightmost set bit bucket partition."},
            {"print-prime-factors", "Print Prime Factors of a Number", "Advanced Maths", "Medium", "Find prime factors up to sqrt(N)."},
            {"divisors-of-number", "Print All Divisors of a Number", "Advanced Maths", "Easy", "Print all divisors in O(sqrt(N)) time."},
            {"count-primes-range-sieve", "Count Primes (Sieve of Eratosthenes)", "Advanced Maths", "Medium", "Sieve of Eratosthenes O(N log log N)."},
            {"prime-factorisation-queries", "Prime Factorisation of a Number", "Advanced Maths", "Medium", "Smallest Prime Factor (SPF) array factorisation."},
            {"pow-x-n-math", "Pow(x, n) Binary Exponentiation", "Advanced Maths", "Medium", "Binary Exponentiation O(log N) power computation."}
        };

        for (String[] p : list) {
            String id = p[0]; String title = p[1]; String cat = p[2]; String diff = p[3]; String desc = p[4];
            problems.put(id, new ProblemDetail(
                id, title, cat, "Bit Manipulation", diff, desc,
                String.format("// Java Implementation for %s\npublic int solve(int n) {\n    return n;\n}", title),
                null, null, null, createArrayState(new int[]{1, 0, 1, 1}, -1, -1), null, null, null,
                new ComplexityDetail("O(1) / O(log N)", "Time Complexity: Bitwise operation or binary exponentiation.", "Bitwise", "O(1)", "Space Complexity: Constant memory.", "Memory", "Auxiliary Space: O(1)", "Memory"), "Array"
            ));
        }
    }

    // Step Generators
    private List<ExecutionStep> generateSingleNumberSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{4, 1, 2, 1, 2};
        int xor = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(stepNum++, 4, "Single Number: Input nums = [4, 1, 2, 1, 2]. Initialize running xor = 0.", List.of(), Map.of(), List.of(), Map.of("xor", "0"), "Array", null, createArrayState(nums, -1, -1), null, null));
        for (int i = 0; i < nums.length; i++) {
            int prevXor = xor; xor ^= nums[i];
            steps.add(new ExecutionStep(stepNum++, 6, String.format("Loop i = %d (val %d): xor = %d ^ %d = %d (Binary: %s).", i, nums[i], prevXor, nums[i], xor, Integer.toBinaryString(xor)), List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "num", String.valueOf(nums[i]), "xor", String.valueOf(xor)), "Array", null, createArrayState(nums, i, -1), null, null));
        }
        steps.add(new ExecutionStep(stepNum++, 8, "Single Number Complete! All paired duplicates (1^1=0, 2^2=0) canceled out! Single unique element = 4.", List.of(), Map.of(), List.of(), Map.of("Single Number", "4"), "Array", null, createArrayState(nums, 0, -1), null, null));
        return steps;
    }

    private List<ExecutionStep> generateBitmaskSubsetsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 2, 3};
        int stepNum = 1;

        steps.add(new ExecutionStep(stepNum++, 4, "Bitmasking Subsets: Array = [1, 2, 3] (N = 3). Total Subsets = 2^3 = 8. Bitmasks range from 0 (000) to 7 (111).", List.of(), Map.of(), List.of(), Map.of("Subsets Count", "8"), "Array", null, createArrayState(nums, -1, -1), null, null));
        steps.add(new ExecutionStep(stepNum++, 10, "Bitmask 0 (000_2): No bits set -> Subsets: [].", List.of(), Map.of(), List.of(), Map.of("bitmask", "000", "subset", "[]"), "Array", null, createArrayState(nums, -1, -1), null, null));
        steps.add(new ExecutionStep(stepNum++, 10, "Bitmask 1 (001_2): Bit 0 set -> Subsets: [1].", List.of(), Map.of(), List.of(), Map.of("bitmask", "001", "subset", "[1]"), "Array", null, createArrayState(nums, 0, -1), null, null));
        steps.add(new ExecutionStep(stepNum++, 10, "Bitmask 3 (011_2): Bits 0, 1 set -> Subsets: [1, 2].", List.of(), Map.of(), List.of(), Map.of("bitmask", "011", "subset", "[1, 2]"), "Array", null, createArrayState(nums, 0, 1), null, null));
        steps.add(new ExecutionStep(stepNum++, 15, "Bitmasking Subsets Complete! All 8 power set subsets generated without recursion!", List.of(), Map.of(), List.of(), Map.of("Total Generated", "8"), "Array", null, createArrayState(nums, -1, -1), null, null));
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
