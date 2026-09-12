package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BitManipulationService implements ProblemProvider {

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
            {"intro-bits-tricks", "Introduction to Bits and Tricks", "Bit Manipulation - Easy", "Easy", "Learn what each bitwise operator does to the bits of a number."},
            {"check-ith-bit-set", "Check if i-th Bit is Set or Not", "Bit Manipulation - Easy", "Easy", "Decide whether the bit at position i of N is a 1 or a 0."},
            {"check-number-odd", "Check if Number is Odd or Not", "Bit Manipulation - Easy", "Easy", "Decide whether N is odd or even."},
            {"check-power-of-2", "Check if Number is Power of 2", "Bit Manipulation - Easy", "Easy", "Decide whether N is an exact power of two."},
            {"count-set-bits", "Count Number of Set Bits", "Bit Manipulation - Easy", "Easy", "Brian Kernighan's Algorithm N & (N - 1)."},
            {"set-unset-rightmost-bit", "Set / Unset Rightmost Unset Bit", "Bit Manipulation - Easy", "Easy", "Set the rightmost 0 bit of N, or clear its rightmost 1 bit."},
            {"swap-two-numbers", "Swap Two Numbers Using XOR", "Bit Manipulation - Easy", "Easy", "Swap a and b without third variable using XOR."},
            {"divide-two-numbers-bitwise", "Divide Two Numbers Without *, /", "Bit Manipulation - Medium", "Medium", "Divide two integers without using multiplication, division or modulo."},
            {"min-bit-flips", "Minimum Bit Flips to Convert Number", "Bit Manipulation - Medium", "Easy", "Find the fewest single-bit flips that turn start into goal."},
            {"xor-numbers-in-range", "XOR of Numbers in Range [L..R]", "Bit Manipulation - Medium", "Medium", "Find the XOR of every integer from 1 to N."},
            {"single-number-3", "Single Number III (Two Unique Numbers)", "Bit Manipulation - Medium", "Medium", "Find two numbers appearing once using rightmost set bit bucket partition."},
            {"print-prime-factors", "Print Prime Factors of a Number", "Advanced Maths", "Medium", "Find every prime factor of N."},
            {"divisors-of-number", "Print All Divisors of a Number", "Advanced Maths", "Easy", "Find every divisor of N."},
            {"count-primes-range-sieve", "Count Primes (Sieve of Eratosthenes)", "Advanced Maths", "Medium", "Count how many prime numbers are strictly less than N."},
            {"prime-factorisation-queries", "Prime Factorisation of a Number", "Advanced Maths", "Medium", "Smallest Prime Factor (SPF) array factorisation."},
            {"pow-x-n-math", "Pow(x, n) Binary Exponentiation", "Advanced Maths", "Medium", "Binary Exponentiation O(log N) power computation."}
        };

        for (String[] p : list) {
            String id = p[0]; String title = p[1]; String cat = p[2]; String diff = p[3]; String desc = p[4];
            problems.put(id, new ProblemDetail(
                id, title, cat, "Bit Manipulation", diff, desc,
                String.format("// Java Implementation for %s\npublic int solve(int n) {\n    return n;\n}", title),
                null, null, null, createArrayState(new int[]{1, 0, 1, 1}, -1, -1), null, null, null,
                new ComplexityDetail("O(1) / O(log N)", "Time Complexity: Bitwise operation or binary exponentiation.", "Bitwise", "O(1)", "Space Complexity: Constant memory.", "Memory", "Auxiliary Space: O(1)", "Memory"), bulkDsType(id).wireValue()
            ));
        }
    }

    /**
     * Every bulk-registered id previously hardcoded {@code "Array"} regardless of what it
     * actually operates on — the same category of gap {@code TreeService.bulkDsType} and
     * {@code DpService.bulkDsType} had before their own batches fixed it. The four ids now
     * traced as {@link DsType#BITS} (rendered via {@code emit.bits(...)}, a single number's
     * binary track) would otherwise mismatch their tracer's {@code dsType()} and fail
     * {@code CatalogTracerMetadataTest}; the rest keep reporting {@code Array} unchanged.
     */
    private static DsType bulkDsType(String id) {
        return switch (id) {
            case "intro-bits-tricks", "check-ith-bit-set", "check-number-odd",
                    "check-power-of-2", "count-set-bits", "set-unset-rightmost-bit",
                    "min-bit-flips", "xor-numbers-in-range", "divide-two-numbers-bitwise",
                    "pow-x-n-math" -> DsType.BITS;
            default -> DsType.ARRAY;
        };
    }

    private ExecutionStep createStep(int stepNum, int line, String desc, List<ArrayElement> arrayState, Map<String, String> vars) {
        return new ExecutionStep(
            stepNum, line, desc,
            List.of(), Map.of(), List.of(), vars,
            "Array", null, arrayState, null, null
        );
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
