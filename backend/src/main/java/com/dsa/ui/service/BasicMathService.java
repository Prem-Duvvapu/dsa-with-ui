package com.dsa.ui.service;

import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BasicMathService {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public BasicMathService() {
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
            case "count-digits": return generateCountDigitsSteps();
            case "reverse-number": return generateReverseNumberSteps();
            case "palindrome-number": return generatePalindromeNumberSteps();
            case "gcd-two-numbers": return generateGcdSteps();
            case "armstrong-check": return generateArmstrongSteps();
            case "print-divisors": return generatePrintDivisorsSteps();
            case "check-prime": return generateCheckPrimeSteps();
            default: return generateCountDigitsSteps();
        }
    }

    private void initProblems() {
        // 1. Count Digits
        problems.put("count-digits", new ProblemDetail(
            "count-digits", "Count Digits of a Number", "Learn the Basics - Maths", "Learn the Basics", "Easy",
            "Count total digits in a given number N by repeatedly extracting the last digit (n / 10).",
            """
            // Java Code - Count Digits
            public int countDigits(int n) {
                int count = 0;
                int temp = n;
                while (temp > 0) {
                    int lastDigit = temp % 10;
                    count++;
                    temp = temp / 10;
                }
                return count;
            }
            """,
            null, null, null, createArrayState(new int[]{7, 4, 2, 9, 5}), null, null, null,
            new ComplexityDetail("O(log10 N)", "Number of iterations equals number of digits = log10(N)", "Dividing by 10 in each loop iteration reduces length by 1 digit.",
                                "O(1)", "Uses scalar integer variables count and temp.", "No additional memory allocated.", "O(1)", "O(1)"),
            "Array"
        ));

        // 2. Reverse a Number
        problems.put("reverse-number", new ProblemDetail(
            "reverse-number", "Reverse a Number", "Learn the Basics - Maths", "Learn the Basics", "Easy",
            "Reverse digits of an integer N. E.g., 12345 becomes 54321.",
            """
            // Java Code - Reverse Number
            public int reverseNumber(int n) {
                int rev = 0;
                int temp = n;
                while (temp > 0) {
                    int lastDigit = temp % 10;
                    rev = (rev * 10) + lastDigit;
                    temp = temp / 10;
                }
                return rev;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 3, 4, 5}), null, null, null,
            new ComplexityDetail("O(log10 N)", "Iterates through each digit of N.", "Dividing by 10 processes one digit per step.",
                                "O(1)", "Uses scalar variables rev and temp.", "No auxiliary memory used.", "O(1)", "O(1)"),
            "Array"
        ));

        // 3. Palindrome Number
        problems.put("palindrome-number", new ProblemDetail(
            "palindrome-number", "Palindrome Number Check", "Learn the Basics - Maths", "Learn the Basics", "Easy",
            "Check whether a number reads the same backward as forward (e.g., 12321 is Palindrome).",
            """
            // Java Code - Palindrome Number
            public boolean isPalindrome(int n) {
                if (n < 0) return false;
                int original = n;
                int rev = 0;
                while (n > 0) {
                    int digit = n % 10;
                    rev = (rev * 10) + digit;
                    n = n / 10;
                }
                return original == rev;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 3, 2, 1}), null, null, null,
            new ComplexityDetail("O(log10 N)", "Iterates through digits of N.", "Processing digits of N.",
                                "O(1)", "Auxiliary scalar space.", "No array stack allocated.", "O(1)", "O(1)"),
            "Array"
        ));

        // 4. GCD of Two Numbers
        problems.put("gcd-two-numbers", new ProblemDetail(
            "gcd-two-numbers", "GCD / HCF of Two Numbers", "Learn the Basics - Maths", "Learn the Basics", "Easy",
            "Find Greatest Common Divisor using Euclidean Algorithm: GCD(a, b) = GCD(b, a % b).",
            """
            // Java Code - Euclidean Algorithm for GCD
            public int findGCD(int a, int b) {
                while (a > 0 && b > 0) {
                    if (a > b) a = a % b;
                    else b = b % a;
                }
                if (a == 0) return b;
                return a;
            }
            """,
            null, null, null, createArrayState(new int[]{52, 12}), null, null, null,
            new ComplexityDetail("O(log(min(a, b)))", "Euclidean modulo reduces inputs logarithmically.", "Modulo operation drastically shrinks numbers in logarithmic steps.",
                                "O(1)", "Constant iterative space.", "No extra memory.", "O(1)", "O(1)"),
            "Array"
        ));

        // 5. Armstrong Number
        problems.put("armstrong-check", new ProblemDetail(
            "armstrong-check", "Check Armstrong Number", "Learn the Basics - Maths", "Learn the Basics", "Easy",
            "An Armstrong number equals the sum of its own digits raised to the power of total digits (153 = 1^3 + 5^3 + 3^3).",
            """
            // Java Code - Armstrong Number
            public boolean isArmstrong(int n) {
                int original = n;
                int sum = 0;
                int digits = String.valueOf(n).length();
                while (n > 0) {
                    int digit = n % 10;
                    sum += Math.pow(digit, digits);
                    n = n / 10;
                }
                return sum == original;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 5, 3}), null, null, null,
            new ComplexityDetail("O(log10 N)", "Processes each digit of N.", "Digit extraction loop.",
                                "O(1)", "Scalar space for sum and original.", "No auxiliary structures.", "O(1)", "O(1)"),
            "Array"
        ));

        // 6. Print All Divisors
        problems.put("print-divisors", new ProblemDetail(
            "print-divisors", "Print All Divisors of N", "Learn the Basics - Maths", "Learn the Basics", "Easy",
            "Find all divisors of N up to sqrt(N). For every i where N % i == 0, both i and N/i are divisors.",
            """
            // Java Code - Print Divisors
            public List<Integer> getDivisors(int n) {
                List<Integer> divisors = new ArrayList<>();
                for (int i = 1; i * i <= n; i++) {
                    if (n % i == 0) {
                        divisors.add(i);
                        if ((n / i) != i) {
                            divisors.add(n / i);
                        }
                    }
                }
                Collections.sort(divisors);
                return divisors;
            }
            """,
            null, null, null, createArrayState(new int[]{36}), null, null, null,
            new ComplexityDetail("O(sqrt(N))", "Loops up to sqrt(N) instead of N.", "Divisors come in pairs (i, n/i).",
                                "O(d)", "Stores d divisors in list.", "Output list size d.", "O(1)", "O(d)"),
            "Array"
        ));

        // 7. Check for Prime Number
        problems.put("check-prime", new ProblemDetail(
            "check-prime", "Check for Prime Number", "Learn the Basics - Maths", "Learn the Basics", "Easy",
            "A prime number has exactly 2 distinct divisors (1 and itself). Check factors up to sqrt(N).",
            """
            // Java Code - Check Prime
            public boolean isPrime(int n) {
                if (n <= 1) return false;
                int count = 0;
                for (int i = 1; i * i <= n; i++) {
                    if (n % i == 0) {
                        count++;
                        if ((n / i) != i) count++;
                    }
                }
                return count == 2;
            }
            """,
            null, null, null, createArrayState(new int[]{29}), null, null, null,
            new ComplexityDetail("O(sqrt(N))", "Loops from 1 to sqrt(N).", "Checking divisor pairs up to square root of N.",
                                "O(1)", "Uses single counter variable.", "No auxiliary memory.", "O(1)", "O(1)"),
            "Array"
        ));
    }

    private List<ArrayElement> createArrayState(int[] arr) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            list.add(new ArrayElement(i, arr[i], "default"));
        }
        return list;
    }

    private List<ExecutionStep> generateCountDigitsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 74295;
        int temp = n;
        int count = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(stepNum++, 2, "Initialize count = 0, temp = " + n, List.of(), Map.of(), List.of(), Map.of("count", "0", "temp", String.valueOf(temp)), "Array", null, createArrayState(new int[]{7, 4, 2, 9, 5}), null, null));

        while (temp > 0) {
            int lastDigit = temp % 10;
            count++;
            temp = temp / 10;
            steps.add(new ExecutionStep(stepNum++, 5, String.format("Extracted last digit = %d. Increment count = %d. Remaining temp = %d", lastDigit, count, temp), List.of(), Map.of(), List.of(), Map.of("extracted", String.valueOf(lastDigit), "count", String.valueOf(count), "temp", String.valueOf(temp)), "Array", null, createArrayState(new int[]{7, 4, 2, 9, 5}), null, null));
        }

        steps.add(new ExecutionStep(stepNum++, 9, String.format("Complete! Total digits in %d = %d", n, count), List.of(), Map.of(), List.of(), Map.of("Total Digits", String.valueOf(count)), "Array", null, createArrayState(new int[]{7, 4, 2, 9, 5}), null, null));
        return steps;
    }

    private List<ExecutionStep> generateReverseNumberSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 12345;
        int temp = n;
        int rev = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(stepNum++, 2, "Initialize rev = 0, temp = " + n, List.of(), Map.of(), List.of(), Map.of("rev", "0", "temp", String.valueOf(temp)), "Array", null, createArrayState(new int[]{1, 2, 3, 4, 5}), null, null));

        while (temp > 0) {
            int lastDigit = temp % 10;
            rev = (rev * 10) + lastDigit;
            temp = temp / 10;
            steps.add(new ExecutionStep(stepNum++, 6, String.format("Extract digit %d -> New rev = (rev * 10) + %d = %d. Remaining temp = %d", lastDigit, lastDigit, rev, temp), List.of(), Map.of(), List.of(), Map.of("digit", String.valueOf(lastDigit), "rev", String.valueOf(rev), "temp", String.valueOf(temp)), "Array", null, createArrayState(new int[]{1, 2, 3, 4, 5}), null, null));
        }

        steps.add(new ExecutionStep(stepNum++, 10, String.format("Reversal Complete! Original: %d -> Reversed: %d", n, rev), List.of(), Map.of(), List.of(), Map.of("Reversed Result", String.valueOf(rev)), "Array", null, createArrayState(new int[]{1, 2, 3, 4, 5}), null, null));
        return steps;
    }

    private List<ExecutionStep> generatePalindromeNumberSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 12321;
        int temp = n;
        int rev = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(stepNum++, 3, "Original N = " + n + ". Initialize rev = 0.", List.of(), Map.of(), List.of(), Map.of("original", String.valueOf(n), "rev", "0"), "Array", null, createArrayState(new int[]{1, 2, 3, 2, 1}), null, null));

        while (temp > 0) {
            int digit = temp % 10;
            rev = (rev * 10) + digit;
            temp /= 10;
            steps.add(new ExecutionStep(stepNum++, 7, String.format("Extract %d -> Update rev = %d, remaining = %d", digit, rev, temp), List.of(), Map.of(), List.of(), Map.of("digit", String.valueOf(digit), "rev", String.valueOf(rev), "temp", String.valueOf(temp)), "Array", null, createArrayState(new int[]{1, 2, 3, 2, 1}), null, null));
        }

        boolean isPalin = (n == rev);
        steps.add(new ExecutionStep(stepNum++, 10, String.format("Check: original (%d) == reversed (%d) -> %b (%s Palindrome!)", n, rev, isPalin, isPalin ? "IS" : "NOT"), List.of(), Map.of(), List.of(), Map.of("Result", isPalin ? "PALINDROME" : "NOT PALINDROME"), "Array", null, createArrayState(new int[]{1, 2, 3, 2, 1}), null, null));
        return steps;
    }

    private List<ExecutionStep> generateGcdSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int a = 52, b = 12;
        int stepNum = 1;

        steps.add(new ExecutionStep(stepNum++, 2, String.format("Find GCD of a = %d, b = %d via Euclidean Algorithm", a, b), List.of(), Map.of(), List.of(), Map.of("a", String.valueOf(a), "b", String.valueOf(b)), "Array", null, createArrayState(new int[]{a, b}), null, null));

        while (a > 0 && b > 0) {
            if (a > b) {
                int oldA = a;
                a = a % b;
                steps.add(new ExecutionStep(stepNum++, 4, String.format("a (%d) > b (%d) -> a = %d %% %d = %d", oldA, b, oldA, b, a), List.of(), Map.of(), List.of(), Map.of("a", String.valueOf(a), "b", String.valueOf(b)), "Array", null, createArrayState(new int[]{a, b}), null, null));
            } else {
                int oldB = b;
                b = b % a;
                steps.add(new ExecutionStep(stepNum++, 5, String.format("b (%d) >= a (%d) -> b = %d %% %d = %d", oldB, a, oldB, a, b), List.of(), Map.of(), List.of(), Map.of("a", String.valueOf(a), "b", String.valueOf(b)), "Array", null, createArrayState(new int[]{a, b}), null, null));
            }
        }

        int gcd = (a == 0) ? b : a;
        steps.add(new ExecutionStep(stepNum++, 8, String.format("Euclidean Algorithm Complete! GCD(52, 12) = %d", gcd), List.of(), Map.of(), List.of(), Map.of("GCD", String.valueOf(gcd)), "Array", null, createArrayState(new int[]{gcd}), null, null));
        return steps;
    }

    private List<ExecutionStep> generateArmstrongSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 153;
        int temp = n;
        int sum = 0;
        int numDigits = 3;
        int stepNum = 1;

        steps.add(new ExecutionStep(stepNum++, 2, String.format("Check if N = %d is Armstrong. Total Digits = %d", n, numDigits), List.of(), Map.of(), List.of(), Map.of("N", String.valueOf(n), "sum", "0"), "Array", null, createArrayState(new int[]{1, 5, 3}), null, null));

        while (temp > 0) {
            int digit = temp % 10;
            int powVal = (int) Math.pow(digit, numDigits);
            sum += powVal;
            temp /= 10;
            steps.add(new ExecutionStep(stepNum++, 7, String.format("Digit %d -> %d^3 = %d. Running Sum = %d", digit, digit, powVal, sum), List.of(), Map.of(), List.of(), Map.of("digit", String.valueOf(digit), "pow", String.valueOf(powVal), "sum", String.valueOf(sum)), "Array", null, createArrayState(new int[]{1, 5, 3}), null, null));
        }

        boolean isArmstrong = (sum == n);
        steps.add(new ExecutionStep(stepNum++, 10, String.format("Result: Sum of cubed digits (%d) == N (%d) -> %s!", sum, n, isArmstrong ? "ARMSTRONG NUMBER" : "NOT ARMSTRONG"), List.of(), Map.of(), List.of(), Map.of("Result", isArmstrong ? "ARMSTRONG" : "NOT ARMSTRONG"), "Array", null, createArrayState(new int[]{1, 5, 3}), null, null));
        return steps;
    }

    private List<ExecutionStep> generatePrintDivisorsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 36;
        List<Integer> divisors = new ArrayList<>();
        int stepNum = 1;

        steps.add(new ExecutionStep(stepNum++, 2, String.format("Find all divisors of N = %d up to sqrt(%d) = 6", n, n), List.of(), Map.of(), List.of(), Map.of("N", "36"), "Array", null, createArrayState(new int[]{36}), null, null));

        for (int i = 1; i * i <= n; i++) {
            if (n % i == 0) {
                divisors.add(i);
                if (n / i != i) divisors.add(n / i);
                steps.add(new ExecutionStep(stepNum++, 5, String.format("i = %d divides 36! Found divisors: %d and %d. Current divisors = %s", i, i, n/i, divisors), List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "pair", String.valueOf(n/i), "divisors", divisors.toString()), "Array", null, createArrayState(new int[]{36}), null, null));
            }
        }

        Collections.sort(divisors);
        steps.add(new ExecutionStep(stepNum++, 10, String.format("Divisor Extraction Complete! All Divisors of 36: %s", divisors), List.of(), Map.of(), List.of(), Map.of("Divisors", divisors.toString()), "Array", null, createArrayState(new int[]{36}), null, null));
        return steps;
    }

    private List<ExecutionStep> generateCheckPrimeSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 29;
        int count = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(stepNum++, 3, "Check if N = 29 is Prime by counting factors up to sqrt(29) = 5", List.of(), Map.of(), List.of(), Map.of("N", "29", "count", "0"), "Array", null, createArrayState(new int[]{29}), null, null));

        for (int i = 1; i * i <= n; i++) {
            if (n % i == 0) {
                count++;
                if (n / i != i) count++;
                steps.add(new ExecutionStep(stepNum++, 6, String.format("i = %d divides 29! Increment factor count to %d", i, count), List.of(), Map.of(), List.of(), Map.of("factor", String.valueOf(i), "count", String.valueOf(count)), "Array", null, createArrayState(new int[]{29}), null, null));
            }
        }

        boolean isPrime = (count == 2);
        steps.add(new ExecutionStep(stepNum++, 11, String.format("Factor count = %d (Exactly 2 factors: 1 and 29) -> 29 is a PRIME NUMBER!", count), List.of(), Map.of(), List.of(), Map.of("Result", "PRIME"), "Array", null, createArrayState(new int[]{29}), null, null));
        return steps;
    }
}
