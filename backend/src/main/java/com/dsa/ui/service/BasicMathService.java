package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BasicMathService implements ProblemProvider {

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
            // These ids have real tracers (tracer/impl). Refuse rather than let default:
            // serve another algorithm's steps under this id. The default: stays until
            // PROMPT D; no other ids remain in this service.
            case "count-digits":
            case "reverse-number":
            case "palindrome-number":
            case "gcd-two-numbers":
            case "armstrong-check":
            case "print-divisors":
            case "check-prime":
                throw new LegacyTraceRetiredException(problemId);
            default: throw new LegacyTraceRetiredException(problemId);
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

}
