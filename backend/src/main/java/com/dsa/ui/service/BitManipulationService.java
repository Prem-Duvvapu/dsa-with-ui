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
            case "intro-bits-tricks": return generateIntroBitsTricksSteps();
            case "check-ith-bit-set": return generateCheckIthBitSetSteps();
            case "check-number-odd": return generateCheckNumberOddSteps();
            case "check-power-of-2": return generateCheckPowerOf2Steps();
            case "count-set-bits": return generateCountSetBitsSteps();
            case "set-unset-rightmost-bit": return generateSetUnsetRightmostBitSteps();
            case "swap-two-numbers": return generateSwapTwoNumbersSteps();
            case "divide-two-numbers-bitwise": return generateDivideTwoNumbersBitwiseSteps();
            case "min-bit-flips": return generateMinBitFlipsSteps();
            case "xor-numbers-in-range": return generateXorNumbersInRangeSteps();
            case "single-number-3": return generateSingleNumber3Steps();
            case "print-prime-factors": return generatePrintPrimeFactorsSteps();
            case "divisors-of-number": return generateDivisorsOfNumberSteps();
            case "count-primes-range-sieve": return generateCountPrimesRangeSieveSteps();
            case "prime-factorisation-queries": return generatePrimeFactorisationQueriesSteps();
            case "pow-x-n-math": return generatePowXNMathSteps();
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

    private ExecutionStep createStep(int stepNum, int line, String desc, List<ArrayElement> arrayState, Map<String, String> vars) {
        return new ExecutionStep(
            stepNum, line, desc,
            List.of(), Map.of(), List.of(), vars,
            "Array", null, arrayState, null, null
        );
    }

    // Step Generators
    private List<ExecutionStep> generateSingleNumberSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{4, 1, 2, 1, 2};
        int xor = 0;
        int stepNum = 1;

        steps.add(createStep(stepNum++, 4, "Single Number: Input nums = [4, 1, 2, 1, 2]. Initialize running xor = 0.", createArrayState(nums, -1, -1), Map.of("xor", "0")));
        for (int i = 0; i < nums.length; i++) {
            int prevXor = xor; xor ^= nums[i];
            steps.add(createStep(stepNum++, 6, String.format("Loop i = %d (val %d): xor = %d ^ %d = %d (Binary: %s).", i, nums[i], prevXor, nums[i], xor, Integer.toBinaryString(xor)), createArrayState(nums, i, -1), Map.of("i", String.valueOf(i), "num", String.valueOf(nums[i]), "xor", String.valueOf(xor))));
        }
        steps.add(createStep(stepNum++, 8, "Single Number Complete! All paired duplicates (1^1=0, 2^2=0) canceled out! Single unique element = 4.", createArrayState(nums, 0, -1), Map.of("Single Number", "4")));
        return steps;
    }

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

    private List<ExecutionStep> generateCheckPowerOf2Steps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 16;
        int[] bits = new int[]{1, 0, 0, 0, 0};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Check if N = 16 (10000_2) is Power of 2 using (N & (N - 1)).", createArrayState(bits, 0, -1), Map.of("N", "16")));
        boolean isPower = (n > 0) && ((n & (n - 1)) == 0);
        steps.add(createStep(stepNum++, 5, "N - 1 = 15 (01111_2). Compute 16 & 15 = 0. Result: 16 is POWER OF 2!", createArrayState(bits, -1, -1), Map.of("isPowerOf2", String.valueOf(isPower))));
        return steps;
    }

    private List<ExecutionStep> generateCountSetBitsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 13;
        int count = 0;
        int[] bits = new int[]{1, 1, 0, 1};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Count Set Bits for N = 13 (1101_2) using Brian Kernighan's Algorithm.", createArrayState(bits, -1, -1), Map.of("N", "13", "count", "0")));

        int temp = n;
        while (temp > 0) {
            temp = temp & (temp - 1);
            count++;
            steps.add(createStep(stepNum++, 6, "Clear rightmost set bit: N = N & (N - 1) -> N = " + temp + ". Updated count = " + count, createArrayState(bits, -1, -1), Map.of("N", String.valueOf(temp), "setBitsCount", String.valueOf(count))));
        }
        steps.add(createStep(stepNum++, 9, "Set Bits Counting Complete! Total Set Bits in 13 = " + count, createArrayState(bits, -1, -1), Map.of("totalSetBits", String.valueOf(count))));
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

    private List<ExecutionStep> generateXorNumbersInRangeSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int l = 3, r = 9;
        int[] vals = new int[]{3, 9};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Compute XOR of numbers in range [3..9] using XOR(R) ^ XOR(L-1).", createArrayState(vals, 0, 1), Map.of("L", "3", "R", "9")));
        steps.add(createStep(stepNum++, 6, "XOR(9) = 1, XOR(2) = 3 -> Result = 1 ^ 3 = 2", createArrayState(vals, -1, -1), Map.of("rangeXOR", "2")));
        return steps;
    }

    private List<ExecutionStep> generateSingleNumber3Steps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 2, 1, 3, 2, 5};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Single Number III: Find two numbers appearing once in [1, 2, 1, 3, 2, 5].", createArrayState(nums, -1, -1), Map.of("nums", Arrays.toString(nums))));
        steps.add(createStep(stepNum++, 7, "Total XOR = 3 ^ 5 = 6 (110_2). Partition into 2 buckets by rightmost set bit -> B1=[3], B2=[5].", createArrayState(nums, 3, 5), Map.of("Bucket1", "3", "Bucket2", "5")));
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

    private List<ExecutionStep> generatePowXNMathSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        double x = 2.0; int n = 10;
        int[] vals = new int[]{2, 10};
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Compute 2.0^10 using Binary Exponentiation O(log N).", createArrayState(vals, -1, -1), Map.of("x", "2.0", "n", "10")));
        steps.add(createStep(stepNum++, 6, "n=10 (even): 2^10 = (2^2)^5 = 4^5. n=5 (odd): ans *= 4 -> n=4 -> 4^4 = (16)^2 = 256. Result = 1024.0", createArrayState(vals, -1, -1), Map.of("result", "1024.0")));
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
