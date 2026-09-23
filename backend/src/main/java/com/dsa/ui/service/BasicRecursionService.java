package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BasicRecursionService implements ProblemProvider {

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
            "RecursionTree"
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
            "RecursionTree"
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
            "RecursionTree"
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
            "String"
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
            "RecursionTree"
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

}
