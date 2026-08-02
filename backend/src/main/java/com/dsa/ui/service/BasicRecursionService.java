package com.dsa.ui.service;

import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BasicRecursionService {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public BasicRecursionService() {
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
            case "print-1-to-n": return generatePrint1ToNSteps();
            case "print-n-to-1": return generatePrintNTo1Steps();
            case "sum-first-n": return generateSumFirstNSteps();
            case "factorial-number": return generateFactorialSteps();
            case "reverse-array-recursion": return generateReverseArrayRecursionSteps();
            case "palindrome-string-recursion": return generatePalindromeStringRecursionSteps();
            case "fibonacci-recursion": return generateFibonacciRecursionSteps();
            default: return generatePrint1ToNSteps();
        }
    }

    private void initProblems() {
        // 1. Print 1 to N using Recursion
        problems.put("print-1-to-n", new ProblemDetail(
            "print-1-to-n", "Print 1 to N using Recursion", "Learn the Basics - Recursion", "Learn the Basics", "Easy",
            "Print numbers from 1 to N recursively without using loops.",
            """
            // Java Code - Print 1 to N
            public void print1ToN(int i, int n) {
                if (i > n) return;
                System.out.print(i + " ");
                print1ToN(i + 1, n);
            }
            """,
            null, null, createTreeNodes("print1ToN"), createArrayState(new int[]{1, 2, 3, 4, 5}), null, null, null,
            new ComplexityDetail("O(N)", "Makes N recursive function calls.", "Direct call sequence from 1 to N.",
                                "O(N)", "Auxiliary call stack space of depth N.", "Stack frames allocated for N active recursive calls.", "O(N)", "O(1)"),
            "Stack"
        ));

        // 2. Print N to 1 using Recursion
        problems.put("print-n-to-1", new ProblemDetail(
            "print-n-to-1", "Print N to 1 using Recursion", "Learn the Basics - Recursion", "Learn the Basics", "Easy",
            "Print numbers from N down to 1 recursively.",
            """
            // Java Code - Print N to 1
            public void printNTo1(int n) {
                if (n == 0) return;
                System.out.print(n + " ");
                printNTo1(n - 1);
            }
            """,
            null, null, createTreeNodes("printNTo1"), createArrayState(new int[]{5, 4, 3, 2, 1}), null, null, null,
            new ComplexityDetail("O(N)", "Makes N recursive calls.", "Reduces N by 1 until base case n = 0.",
                                "O(N)", "Auxiliary recursion stack depth N.", "Call stack frames.", "O(N)", "O(1)"),
            "Stack"
        ));

        // 3. Sum of First N Numbers
        problems.put("sum-first-n", new ProblemDetail(
            "sum-first-n", "Sum of First N Numbers", "Learn the Basics - Recursion", "Learn the Basics", "Easy",
            "Compute sum of first N natural numbers using functional recursion: f(n) = n + f(n-1).",
            """
            // Java Code - Sum of First N Numbers
            public int sumN(int n) {
                if (n == 0) return 0;
                return n + sumN(n - 1);
            }
            """,
            null, null, createTreeNodes("sumN"), createArrayState(new int[]{1, 2, 3, 4, 5}), null, null, null,
            new ComplexityDetail("O(N)", "Makes N recursive stack calls.", "Decrements N by 1 at each level.",
                                "O(N)", "Call stack depth of size N.", "Stack frame push per call.", "O(N)", "O(1)"),
            "Stack"
        ));

        // 4. Factorial of N
        problems.put("factorial-number", new ProblemDetail(
            "factorial-number", "Factorial of N", "Learn the Basics - Recursion", "Learn the Basics", "Easy",
            "Compute N! = N * (N - 1)! recursively with base case f(0) = 1.",
            """
            // Java Code - Factorial
            public int factorial(int n) {
                if (n == 0 || n == 1) return 1;
                return n * factorial(n - 1);
            }
            """,
            null, null, createTreeNodes("factorial"), createArrayState(new int[]{1, 2, 6, 24, 120}), null, null, null,
            new ComplexityDetail("O(N)", "N recursive steps.", "Multiplies numbers from 1 to N.",
                                "O(N)", "Recursion call stack depth N.", "Stack frames for recursive depth.", "O(N)", "O(1)"),
            "Stack"
        ));

        // 5. Reverse an Array (Recursive)
        problems.put("reverse-array-recursion", new ProblemDetail(
            "reverse-array-recursion", "Reverse an Array (Recursive)", "Learn the Basics - Recursion", "Learn the Basics", "Easy",
            "Reverse array in-place using two recursive pointers l and r.",
            """
            // Java Code - Reverse Array Recursively
            public void reverseArray(int l, int r, int[] arr) {
                if (l >= r) return;
                int temp = arr[l];
                arr[l] = arr[r];
                arr[r] = temp;
                reverseArray(l + 1, r - 1, arr);
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 3, 4, 5}), null, null, null,
            new ComplexityDetail("O(N)", "N/2 recursive swaps.", "Processes two pointers moving inward.",
                                "O(N/2)", "Call stack depth N/2.", "Auxiliary call stack memory.", "O(N/2)", "O(1)"),
            "Array"
        ));

        // 6. Palindrome String Check (Recursive)
        problems.put("palindrome-string-recursion", new ProblemDetail(
            "palindrome-string-recursion", "Check Palindrome String (Recursive)", "Learn the Basics - Recursion", "Learn the Basics", "Easy",
            "Check if string is palindrome by comparing s[i] with s[n-i-1] recursively.",
            """
            // Java Code - Palindrome String Recursion
            public boolean isPalindrome(int i, String s) {
                if (i >= s.length() / 2) return true;
                if (s.charAt(i) != s.charAt(s.length() - i - 1)) return false;
                return isPalindrome(i + 1, s);
            }
            """,
            null, null, null, createArrayState(new int[]{77, 65, 68, 65, 77}), null, null, null,
            new ComplexityDetail("O(N/2)", "Compares characters from ends inward.", "Matches N/2 characters.",
                                "O(N/2)", "Recursion stack depth N/2.", "Call stack frames.", "O(N/2)", "O(1)"),
            "Array"
        ));

        // 7. Fibonacci Number (Recursive)
        problems.put("fibonacci-recursion", new ProblemDetail(
            "fibonacci-recursion", "Fibonacci Number (Recursion Tree)", "Learn the Basics - Recursion", "Learn the Basics", "Easy",
            "Compute Nth Fibonacci number: fib(n) = fib(n-1) + fib(n-2) demonstrating binary call tree.",
            """
            // Java Code - Fibonacci Recursion
            public int fib(int n) {
                if (n <= 1) return n;
                return fib(n - 1) + fib(n - 2);
            }
            """,
            null, null, createFibonacciTreeNodes(), createArrayState(new int[]{0, 1, 1, 2, 3, 5, 8}), null, null, null,
            new ComplexityDetail("O(2^N)", "Exponential call tree branches.", "Binary tree calls double at each level.",
                                "O(N)", "Maximum call stack depth N.", "Auxiliary recursion stack.", "O(N)", "O(1)"),
            "Stack"
        ));
    }

    private List<TreeNode> createTreeNodes(String label) {
        List<TreeNode> list = new ArrayList<>();
        list.add(new TreeNode(1, label + "(5)", 200, 40, 2, 3, "unvisited"));
        list.add(new TreeNode(2, label + "(4)", 140, 110, null, null, "unvisited"));
        list.add(new TreeNode(3, label + "(3)", 260, 110, null, null, "unvisited"));
        return list;
    }

    private List<TreeNode> createFibonacciTreeNodes() {
        List<TreeNode> list = new ArrayList<>();
        list.add(new TreeNode(1, "fib(4)", 200, 40, 2, 3, "unvisited"));
        list.add(new TreeNode(2, "fib(3)", 120, 110, 4, 5, "unvisited"));
        list.add(new TreeNode(3, "fib(2)", 280, 110, 6, 7, "unvisited"));
        return list;
    }

    private List<ArrayElement> createArrayState(int[] arr) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            list.add(new ArrayElement(i, arr[i], "default"));
        }
        return list;
    }

    private List<ExecutionStep> generatePrint1ToNSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 5;
        int stepNum = 1;
        List<String> stack = new ArrayList<>();

        for (int i = 1; i <= n; i++) {
            stack.add("print1ToN(" + i + ")");
            steps.add(new ExecutionStep(stepNum++, 3, String.format("Call print1ToN(i = %d, n = 5). Output: %d", i, i), new ArrayList<>(stack), Map.of(), List.of(), Map.of("i", String.valueOf(i), "n", "5"), "Stack", null, createArrayState(new int[]{1, 2, 3, 4, 5}), null, null));
        }

        steps.add(new ExecutionStep(stepNum++, 2, "Base Case reached (i > 5)! Unwinding call stack...", new ArrayList<>(stack), Map.of(), List.of(), Map.of("Status", "COMPLETED"), "Stack", null, createArrayState(new int[]{1, 2, 3, 4, 5}), null, null));
        return steps;
    }

    private List<ExecutionStep> generatePrintNTo1Steps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 5;
        int stepNum = 1;
        List<String> stack = new ArrayList<>();

        for (int i = n; i >= 1; i--) {
            stack.add("printNTo1(" + i + ")");
            steps.add(new ExecutionStep(stepNum++, 3, String.format("Call printNTo1(n = %d). Output: %d", i, i), new ArrayList<>(stack), Map.of(), List.of(), Map.of("n", String.valueOf(i)), "Stack", null, createArrayState(new int[]{5, 4, 3, 2, 1}), null, null));
        }

        steps.add(new ExecutionStep(stepNum++, 2, "Base Case reached (n == 0)! Execution complete.", new ArrayList<>(stack), Map.of(), List.of(), Map.of("Status", "COMPLETED"), "Stack", null, createArrayState(new int[]{5, 4, 3, 2, 1}), null, null));
        return steps;
    }

    private List<ExecutionStep> generateSumFirstNSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 5;
        int stepNum = 1;
        List<String> stack = new ArrayList<>();

        for (int i = n; i >= 1; i--) {
            stack.add("sumN(" + i + ")");
            steps.add(new ExecutionStep(stepNum++, 3, String.format("Push sumN(%d) onto call stack. Computation: %d + sumN(%d)", i, i, i - 1), new ArrayList<>(stack), Map.of(), List.of(), Map.of("n", String.valueOf(i)), "Stack", null, createArrayState(new int[]{1, 2, 3, 4, 5}), null, null));
        }

        stack.add("sumN(0)");
        steps.add(new ExecutionStep(stepNum++, 2, "Base Case sumN(0) returns 0. Now unwinding call stack & accumulating sum...", new ArrayList<>(stack), Map.of(), List.of(), Map.of("return", "0"), "Stack", null, createArrayState(new int[]{1, 2, 3, 4, 5}), null, null));

        int currentSum = 0;
        for (int i = 1; i <= n; i++) {
            currentSum += i;
            if (!stack.isEmpty()) stack.remove(stack.size() - 1);
            steps.add(new ExecutionStep(stepNum++, 3, String.format("Pop sumN(%d) -> Accumulated Sum = %d", i, currentSum), new ArrayList<>(stack), Map.of(), List.of(), Map.of("returned_sum", String.valueOf(currentSum)), "Stack", null, createArrayState(new int[]{1, 2, 3, 4, 5}), null, null));
        }

        return steps;
    }

    private List<ExecutionStep> generateFactorialSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 5;
        int stepNum = 1;
        List<String> stack = new ArrayList<>();

        for (int i = n; i >= 1; i--) {
            stack.add("factorial(" + i + ")");
            steps.add(new ExecutionStep(stepNum++, 3, String.format("Push factorial(%d). Computation: %d * factorial(%d)", i, i, i - 1), new ArrayList<>(stack), Map.of(), List.of(), Map.of("n", String.valueOf(i)), "Stack", null, createArrayState(new int[]{1, 2, 6, 24, 120}), null, null));
        }

        long fact = 1;
        for (int i = 1; i <= n; i++) {
            fact *= i;
            if (!stack.isEmpty()) stack.remove(stack.size() - 1);
            steps.add(new ExecutionStep(stepNum++, 3, String.format("Pop factorial(%d) -> Returned %d! = %d", i, i, fact), new ArrayList<>(stack), Map.of(), List.of(), Map.of("factorial", String.valueOf(fact)), "Stack", null, createArrayState(new int[]{1, 2, 6, 24, 120}), null, null));
        }

        return steps;
    }

    private List<ExecutionStep> generateReverseArrayRecursionSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = {1, 2, 3, 4, 5};
        int l = 0, r = arr.length - 1;
        int stepNum = 1;

        steps.add(new ExecutionStep(stepNum++, 2, "Start reverseArray(l = 0, r = 4, arr)", List.of(), Map.of(), List.of(), Map.of("l", "0", "r", "4"), "Array", null, createArrayState(arr), null, null));

        while (l < r) {
            int temp = arr[l];
            arr[l] = arr[r];
            arr[r] = temp;
            steps.add(new ExecutionStep(stepNum++, 5, String.format("Swap arr[%d] (%d) ↔ arr[%d] (%d). Recursive call reverseArray(%d, %d)", l, arr[r], r, arr[l], l + 1, r - 1), List.of(), Map.of(), List.of(), Map.of("swapped_l", String.valueOf(l), "swapped_r", String.valueOf(r)), "Array", null, createArrayState(arr), null, null));
            l++;
            r--;
        }

        steps.add(new ExecutionStep(stepNum++, 2, "Base Case (l >= r): Reversal Complete! Final Array = [5, 4, 3, 2, 1]", List.of(), Map.of(), List.of(), Map.of("Status", "REVERSED"), "Array", null, createArrayState(arr), null, null));
        return steps;
    }

    private List<ExecutionStep> generatePalindromeStringRecursionSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        String s = "MADAM";
        int len = s.length();
        int stepNum = 1;

        steps.add(new ExecutionStep(stepNum++, 2, "Start recursive palindrome check for string \"MADAM\"", List.of(), Map.of(), List.of(), Map.of("s", "MADAM"), "Array", null, createArrayState(new int[]{77, 65, 68, 65, 77}), null, null));

        for (int i = 0; i < len / 2; i++) {
            char left = s.charAt(i);
            char right = s.charAt(len - i - 1);
            boolean match = (left == right);
            steps.add(new ExecutionStep(stepNum++, 3, String.format("Compare s[%d] ('%c') == s[%d] ('%c') -> %b ✓. Recurse i = %d", i, left, len - i - 1, right, match, i + 1), List.of(), Map.of(), List.of(), Map.of("left", String.valueOf(left), "right", String.valueOf(right), "match", String.valueOf(match)), "Array", null, createArrayState(new int[]{77, 65, 68, 65, 77}), null, null));
        }

        steps.add(new ExecutionStep(stepNum++, 2, "Base Case (i >= len/2): All character pairs matched -> \"MADAM\" IS A PALINDROME!", List.of(), Map.of(), List.of(), Map.of("Result", "PALINDROME"), "Array", null, createArrayState(new int[]{77, 65, 68, 65, 77}), null, null));
        return steps;
    }

    private List<ExecutionStep> generateFibonacciRecursionSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int stepNum = 1;
        List<String> stack = new ArrayList<>();

        stack.add("fib(4)");
        steps.add(new ExecutionStep(stepNum++, 3, "Call fib(4) = fib(3) + fib(2)", new ArrayList<>(stack), Map.of(1, "active"), List.of(), Map.of("n", "4"), "Stack", null, createArrayState(new int[]{0, 1, 1, 2, 3, 5, 8}), null, null));

        stack.add("fib(3)");
        steps.add(new ExecutionStep(stepNum++, 3, "Call fib(3) = fib(2) + fib(1)", new ArrayList<>(stack), Map.of(2, "active"), List.of(), Map.of("n", "3"), "Stack", null, createArrayState(new int[]{0, 1, 1, 2, 3, 5, 8}), null, null));

        stack.add("fib(2)");
        steps.add(new ExecutionStep(stepNum++, 3, "Call fib(2) = fib(1) + fib(0) -> Returns 1 + 0 = 1", new ArrayList<>(stack), Map.of(3, "active"), List.of(), Map.of("fib(2)", "1"), "Stack", null, createArrayState(new int[]{0, 1, 1, 2, 3, 5, 8}), null, null));

        stack.remove(stack.size() - 1);
        stack.remove(stack.size() - 1);
        steps.add(new ExecutionStep(stepNum++, 3, "Unwind stack: fib(3) = 2, fib(4) = fib(3) + fib(2) = 2 + 1 = 3!", new ArrayList<>(stack), Map.of(1, "visited"), List.of(), Map.of("fib(4)", "3"), "Stack", null, createArrayState(new int[]{0, 1, 1, 2, 3, 5, 8}), null, null));

        return steps;
    }
}
