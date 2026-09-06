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

    public List<ExecutionStep> generateSteps(String problemId) {
        switch (problemId) {
            // single-number, single-number-1, check-power-of-2, count-set-bits,
            // xor-numbers-in-range, single-number-3 and pow-x-n-math have real tracers
            // (tracer/impl) now. Refuse rather than let default: serve
            // generateSingleNumberSteps()'s unrelated steps under these ids.
            case "single-number":
            case "single-number-1":
            case "check-power-of-2":
            case "count-set-bits":
            case "xor-numbers-in-range":
            case "single-number-3":
            case "pow-x-n-math":
                throw new LegacyTraceRetiredException(problemId);
            case "subsets-bitmasking":
            case "power-set-bitwise": return generateBitmaskSubsetsSteps();
            case "intro-bits-tricks": return generateIntroBitsTricksSteps();
            case "check-ith-bit-set": return generateCheckIthBitSetSteps();
            case "check-number-odd": return generateCheckNumberOddSteps();
            case "set-unset-rightmost-bit": return generateSetUnsetRightmostBitSteps();
            case "swap-two-numbers": return generateSwapTwoNumbersSteps();
            case "divide-two-numbers-bitwise": return generateDivideTwoNumbersBitwiseSteps();
            case "min-bit-flips": return generateMinBitFlipsSteps();
            case "print-prime-factors": return generatePrintPrimeFactorsSteps();
            case "divisors-of-number": return generateDivisorsOfNumberSteps();
            case "count-primes-range-sieve": return generateCountPrimesRangeSieveSteps();
            case "prime-factorisation-queries": return generatePrimeFactorisationQueriesSteps();
            default: return generateCheckNumberOddSteps();
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

    // Step Generators
    private List<ExecutionStep> generateBitmaskSubsetsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 2, 3};
        int stepNum = 1;

        steps.add(createStep(stepNum++, 4, "Bitmasking Subsets: Array = [1, 2, 3] (N = 3). Total Subsets = 2^3 = 8. Bitmasks range from 0 (000) to 7 (111).", createArrayState(nums, -1, -1), Map.of("Subsets Count", "8")));
        steps.add(createStep(stepNum++, 10, "Bitmask 0 (000_2): No bits set -> Subsets: [].", createArrayState(nums, -1, -1), Map.of("bitmask", "000", "subset", "[]")));
        steps.add(createStep(stepNum++, 10, "Bitmask 1 (001_2): Bit 0 set -> Subsets: [1].", createArrayState(nums, 0, -1), Map.of("bitmask", "001", "subset", "[1]")));
        steps.add(createStep(stepNum++, 10, "Bitmask 3 (011_2): Bits 0, 1 set -> Subsets: [1, 2].", createArrayState(nums, 0, 1), Map.of("bitmask", "011", "subset", "[1, 2]")));
        steps.add(createStep(stepNum++, 15, "Bitmasking Subsets Complete! All 8 power set subsets generated without recursion!", createArrayState(nums, -1, -1), Map.of("Total Generated", "8")));
        return steps;
    }

    private List<ExecutionStep> generateIntroBitsTricksSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] bits = new int[]{1, 1, 0, 1}; // 13 in binary (1101)
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Input N = 13 (Binary: 1101_2). Demonstrate bitwise operations: &, |, ^, ~, <<, >>.", createArrayState(bits, -1, -1), Map.of("N", "13", "binary", "1101")));
        steps.add(createStep(stepNum++, 5, "Left Shift 13 << 1 = 26 (11010_2). Right Shift 13 >> 1 = 6 (0110_2).", createArrayState(bits, -1, -1), Map.of("13 << 1", "26", "13 >> 1", "6")));
        return steps;
    }

    private List<ExecutionStep> generateCheckIthBitSetSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 13, i = 2; // 13 = 1101_2
        int[] bits = new int[]{1, 1, 0, 1};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Check if " + i + "-th bit is set in N = 13 (1101_2).", createArrayState(bits, 2, -1), Map.of("N", "13", "i", String.valueOf(i))));
        int mask = 1 << i;
        boolean isSet = (n & mask) != 0;
        steps.add(createStep(stepNum++, 5, "Bitmask 1 << 2 = 4 (0100_2). Compute N & mask = 13 & 4 = " + (n & mask), createArrayState(bits, 2, -1), Map.of("mask", Integer.toBinaryString(mask), "result", String.valueOf(isSet))));
        steps.add(createStep(stepNum++, 7, "2-nd bit is " + (isSet ? "SET (1)" : "UNSET (0)") + "! Return " + isSet, createArrayState(bits, 2, -1), Map.of("isSet", String.valueOf(isSet))));
        return steps;
    }

    private List<ExecutionStep> generateCheckNumberOddSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 13;
        int[] bits = new int[]{1, 1, 0, 1};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Check if N = 13 is Odd using Bitwise AND (N & 1).", createArrayState(bits, 3, -1), Map.of("N", "13")));
        boolean isOdd = (n & 1) == 1;
        steps.add(createStep(stepNum++, 5, "13 & 1 = 1 (LSB is 1). Result: 13 is ODD!", createArrayState(bits, 3, -1), Map.of("isOdd", String.valueOf(isOdd))));
        return steps;
    }

    private List<ExecutionStep> generateSetUnsetRightmostBitSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 12; // 1100_2
        int[] bits = new int[]{1, 1, 0, 0};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Input N = 12 (1100_2). Unset rightmost set bit using N & (N - 1).", createArrayState(bits, -1, -1), Map.of("N", "12")));
        int ans = n & (n - 1);
        steps.add(createStep(stepNum++, 5, "12 & 11 = 8 (1000_2). Rightmost set bit unset! Result = 8.", createArrayState(bits, 0, -1), Map.of("result", String.valueOf(ans))));
        return steps;
    }

    private List<ExecutionStep> generateSwapTwoNumbersSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int a = 5, b = 9;
        int[] vals = new int[]{5, 9};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Swap a = 5, b = 9 without extra memory using XOR.", createArrayState(vals, 0, 1), Map.of("a", "5", "b", "9")));
        a = a ^ b;
        steps.add(createStep(stepNum++, 5, "Step 1: a = a ^ b = 5 ^ 9 = " + a, createArrayState(vals, 0, 1), Map.of("a", String.valueOf(a), "b", "9")));
        b = a ^ b;
        steps.add(createStep(stepNum++, 6, "Step 2: b = a ^ b = 12 ^ 9 = " + b, createArrayState(vals, 0, 1), Map.of("a", String.valueOf(a), "b", String.valueOf(b))));
        a = a ^ b;
        steps.add(createStep(stepNum++, 7, "Step 3: a = a ^ b = 12 ^ 5 = " + a + ". Swap complete! a=" + a + ", b=" + b, createArrayState(vals, 0, 1), Map.of("a", String.valueOf(a), "b", String.valueOf(b))));
        return steps;
    }

    private List<ExecutionStep> generateDivideTwoNumbersBitwiseSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int dividend = 22, divisor = 3;
        int[] vals = new int[]{22, 3};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Divide 22 by 3 without *, / operators using bitwise shifts.", createArrayState(vals, -1, -1), Map.of("dividend", "22", "divisor", "3")));
        steps.add(createStep(stepNum++, 7, "22 = 3 * 2^2 (12) + 3 * 2^1 (6) + 3 * 2^0 (3) + 1. Quotient = 7.", createArrayState(vals, -1, -1), Map.of("quotient", "7")));
        return steps;
    }

    private List<ExecutionStep> generateMinBitFlipsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int start = 10, goal = 7; // 10 = 1010, 7 = 0111
        int[] vals = new int[]{10, 7};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Minimum Bit Flips to convert start = 10 (1010_2) to goal = 7 (0111_2).", createArrayState(vals, 0, 1), Map.of("start", "10", "goal", "7")));
        int xor = start ^ goal; // 1010 ^ 0111 = 1101 (13)
        steps.add(createStep(stepNum++, 5, "XOR difference: 10 ^ 7 = 13 (1101_2). Count set bits in 13.", createArrayState(vals, 0, 1), Map.of("xor", "13", "differingBits", "3")));
        steps.add(createStep(stepNum++, 7, "Min Bit Flips = 3.", createArrayState(vals, -1, -1), Map.of("flips", "3")));
        return steps;
    }

    private List<ExecutionStep> generatePrintPrimeFactorsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 60;
        int[] vals = new int[]{2, 2, 3, 5};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Find prime factors of N = 60 up to sqrt(60).", createArrayState(vals, -1, -1), Map.of("N", "60")));
        steps.add(createStep(stepNum++, 6, "60 / 2 = 30 -> 30 / 2 = 15 -> 15 / 3 = 5 -> Prime Factors: [2, 2, 3, 5]", createArrayState(vals, -1, -1), Map.of("factors", "[2, 2, 3, 5]")));
        return steps;
    }

    private List<ExecutionStep> generateDivisorsOfNumberSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 36;
        int[] vals = new int[]{1, 2, 3, 4, 6, 9, 12, 18, 36};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Find all divisors of N = 36 in O(sqrt(36)) time.", createArrayState(vals, -1, -1), Map.of("N", "36")));
        steps.add(createStep(stepNum++, 6, "Divisors: [1, 2, 3, 4, 6, 9, 12, 18, 36]", createArrayState(vals, -1, -1), Map.of("divisors", "[1, 2, 3, 4, 6, 9, 12, 18, 36]")));
        return steps;
    }

    private List<ExecutionStep> generateCountPrimesRangeSieveSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 10;
        int[] primes = new int[]{2, 3, 5, 7};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Count primes strictly less than N = 10 using Sieve of Eratosthenes.", createArrayState(primes, -1, -1), Map.of("N", "10")));
        steps.add(createStep(stepNum++, 7, "Primes in [2..9]: 2, 3, 5, 7 -> Total = 4", createArrayState(primes, -1, -1), Map.of("countPrimes", "4")));
        return steps;
    }

    private List<ExecutionStep> generatePrimeFactorisationQueriesSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 30;
        int[] spf = new int[]{2, 3, 5};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Prime Factorisation of N = 30 using Smallest Prime Factor (SPF) array.", createArrayState(spf, -1, -1), Map.of("N", "30")));
        steps.add(createStep(stepNum++, 6, "SPF[30] = 2 -> 30/2=15 -> SPF[15] = 3 -> 15/3=5 -> Factors: 2 * 3 * 5", createArrayState(spf, -1, -1), Map.of("factors", "2 * 3 * 5")));
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
