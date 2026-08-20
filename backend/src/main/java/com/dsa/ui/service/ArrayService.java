package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ArrayService implements ProblemProvider {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public ArrayService() {
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
            case "largest-element": return generateLargestElementSteps();
            case "second-largest-element": return generateSecondLargestElementSteps();
            case "check-sorted-ii": return generateCheckSortedIISteps();
            case "remove-duplicates-sorted": return generateRemoveDuplicatesSortedSteps();
            case "left-rotate-one": return generateLeftRotateOneSteps();
            case "left-rotate-k": return generateLeftRotateKSteps();
            case "move-zeros-end": return generateMoveZerosEndSteps();
            case "linear-search": return generateLinearSearchSteps();
            case "union-sorted-arrays": return generateUnionSortedArraysSteps();
            case "find-missing-number": return generateFindMissingNumberSteps();
            case "max-consecutive-ones": return generateMaxConsecutiveOnesSteps();
            case "single-number": return generateSingleNumberSteps();
            case "longest-subarray-sum-k-positives": return generateLongestSubarraySumKPositivesSteps();
            case "longest-subarray-sum-k": return generateLongestSubarraySumKSteps();
            case "two-sum": return generateTwoSumSteps();
            case "sort-0-1-2": return generateSort012Steps();
            case "majority-element": return generateMajorityElementSteps();
            case "kadane-algo": return generateKadaneSteps();
            case "print-max-subarray": return generatePrintMaxSubarraySteps();
            case "stock-buy-sell": return generateStockSteps();
            case "rearrange-by-sign": return generateRearrangeBySignSteps();
            case "next-permutation": return generateNextPermutationSteps();
            case "leaders-in-array": return generateLeadersSteps();
            case "longest-consecutive-sequence": return generateLongestConsecutiveSteps();
            case "set-matrix-zeroes": return generateSetMatrixZeroesSteps();
            case "rotate-matrix-90": return generateRotateMatrixSteps();
            case "spiral-matrix": return generateSpiralMatrixSteps();
            case "count-subarrays-given-sum": return generateCountSubarraysGivenSumSteps();
            case "pascals-triangle": return generatePascalsTriangleSteps();
            case "majority-element-ii": return generateMajorityElement2Steps();
            case "three-sum": return generateThreeSumSteps();
            case "four-sum": return generateFourSumSteps();
            case "largest-subarray-sum-0": return generateLargestSubarraySum0Steps();
            case "count-subarrays-xor-k": return generateCountSubarraysXorKSteps();
            case "merge-intervals": return generateMergeIntervalsSteps();
            case "merge-two-sorted-arrays": return generateMergeTwoSortedArraysSteps();
            case "repeating-missing-number": return generateRepeatingMissingSteps();
            case "count-inversions": return generateCountInversionsSteps();
            case "reverse-pairs": return generateReversePairsSteps();
            case "max-product-subarray": return generateMaxProductSubarraySteps();
            default: return generateLargestElementSteps();
        }
    }

    private void initProblems() {
        // 0.1 Largest Element
        problems.put("largest-element", new ProblemDetail(
            "largest-element", "Largest Element in an Array", "Arrays - Easy", "Arrays", "Easy",
            "Find the largest element in an array using a single-pass linear scan.",
            """
            // Java Largest Element Solution
            public int largest(int[] arr) {
                int max = arr[0];
                for (int i = 1; i < arr.length; i++) {
                    if (arr[i] > max) {
                        max = arr[i];
                    }
                }
                return max;
            }
            """,
            null, null, null, createArrayState(new int[]{12, 35, 1, 10, 34, 1}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)", "Time Complexity: Single pass traversal over array of length N.",
                "Why O(N)? Inspects each array element exactly once.",
                "O(1)", "Space Complexity: Auxiliary variables max and i require constant memory.",
                "Why O(1)? In-place linear scan.", "Auxiliary Space: O(1)", "Output: O(1)"
            ), "Array"
        ));

        // 0.2 Second Largest Element
        problems.put("second-largest-element", new ProblemDetail(
            "second-largest-element", "Second Largest Element in Array", "Arrays - Easy", "Arrays", "Easy",
            "Find the second largest element in an array in single pass O(N) time without sorting.",
            """
            // Java Second Largest Element Solution
            public int getSecondLargest(int[] arr) {
                int largest = arr[0];
                int secondLargest = -1;
                for (int i = 1; i < arr.length; i++) {
                    if (arr[i] > largest) {
                        secondLargest = largest;
                        largest = arr[i];
                    } else if (arr[i] < largest && arr[i] > secondLargest) {
                        secondLargest = arr[i];
                    }
                }
                return secondLargest;
            }
            """,
            null, null, null, createArrayState(new int[]{12, 35, 1, 10, 34, 1}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)", "Time Complexity: Single pass iteration updating largest and secondLargest.",
                "Why O(N)? Eliminates sorting (O(N log N)) by tracking top two values dynamically.",
                "O(1)", "Space Complexity: O(1) auxiliary space.",
                "Why O(1)? Uses only two primitive scalar variables.", "Auxiliary Space: O(1)", "Output: O(1)"
            ), "Array"
        ));

        // 0.3 Check if Array is Sorted II
        problems.put("check-sorted-ii", new ProblemDetail(
            "check-sorted-ii", "Check if Array is Sorted and Rotated", "Arrays - Easy", "Arrays", "Easy",
            "Check if an array was originally sorted in non-decreasing order and then rotated.",
            """
            // Java Check Sorted & Rotated Solution (LeetCode 1752)
            public boolean check(int[] nums) {
                int count = 0;
                int n = nums.length;
                for (int i = 0; i < n; i++) {
                    if (nums[i] > nums[(i + 1) % n]) {
                        count++;
                    }
                }
                return count <= 1;
            }
            """,
            null, null, null, createArrayState(new int[]{3, 4, 5, 1, 2}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)", "Time Complexity: Single pass checking adjacent circular pairs (nums[i] > nums[(i+1)%n]).",
                "Why O(N)? A sorted and rotated array has at most 1 drop point.",
                "O(1)", "Space Complexity: O(1) auxiliary space.",
                "Why O(1)? Only uses a counter variable.", "Auxiliary Space: O(1)", "Output: O(1)"
            ), "Array"
        ));

        // 0.4 Remove Duplicates from Sorted Array
        problems.put("remove-duplicates-sorted", new ProblemDetail(
            "remove-duplicates-sorted", "Remove Duplicates from Sorted Array", "Arrays - Easy", "Arrays", "Easy",
            "Remove duplicate elements in-place from a sorted array and return the number of unique elements.",
            """
            // Java Remove Duplicates 2-Pointer (LeetCode 26)
            public int removeDuplicates(int[] nums) {
                int i = 0;
                for (int j = 1; j < nums.length; j++) {
                    if (nums[j] != nums[i]) {
                        i++;
                        nums[i] = nums[j];
                    }
                }
                return i + 1;
            }
            """,
            null, null, null, createArrayState(new int[]{0, 0, 1, 1, 1, 2, 2, 3, 3, 4}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)", "Time Complexity: 2-pointer scan where j moves from 1 to N-1.",
                "Why O(N)? Each element is compared once.",
                "O(1)", "Space Complexity: In-place array modification requiring O(1) extra memory.",
                "Why O(1)? Rewrites unique elements at index i without extra arrays.", "Auxiliary Space: O(1)", "In-Place: O(1)"
            ), "Array"
        ));

        // 0.5 Left Rotate Array by One
        problems.put("left-rotate-one", new ProblemDetail(
            "left-rotate-one", "Left Rotate Array by One Place", "Arrays - Easy", "Arrays", "Easy",
            "Shift all elements of the array left by 1 position and place the first element at the end.",
            """
            // Java Left Rotate by 1 Place
            public void rotateByOne(int[] arr) {
                int temp = arr[0];
                for (int i = 0; i < arr.length - 1; i++) {
                    arr[i] = arr[i + 1];
                }
                arr[arr.length - 1] = temp;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 3, 4, 5}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)", "Time Complexity: Single loop shifting N-1 elements to the left.",
                "Why O(N)? Modifies elements sequentially in a single pass.",
                "O(1)", "Space Complexity: In-place modification with temporary variable for first element.",
                "Why O(1)? Uses 1 scalar temp variable.", "Auxiliary Space: O(1)", "Output: O(1)"
            ), "Array"
        ));

        // 0.6 Left Rotate Array by K Places
        problems.put("left-rotate-k", new ProblemDetail(
            "left-rotate-k", "Left Rotate Array by K Places", "Arrays - Easy/Medium", "Arrays", "Easy",
            "Rotate the array to the left by K steps using the optimal 3-step sub-array reversal technique.",
            """
            // Java Rotate by K Places Reversal Algorithm (LeetCode 189)
            public void rotate(int[] nums, int k) {
                int n = nums.length;
                k = k % n;
                reverse(nums, 0, k - 1);
                reverse(nums, k, n - 1);
                reverse(nums, 0, n - 1);
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 3, 4, 5, 6, 7}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)", "Time Complexity: Reversing 0..k-1, k..N-1, and 0..N-1 takes O(k) + O(N-k) + O(N) = O(2N) = O(N).",
                "Why O(N)? 3 reversal steps process array elements twice in total.",
                "O(1)", "Space Complexity: In-place reversal.",
                "Why O(1)? No auxiliary temporary arrays created.", "Auxiliary Space: O(1)", "In-Place: O(1)"
            ), "Array"
        ));

        // 0.7 Move Zeros to End
        problems.put("move-zeros-end", new ProblemDetail(
            "move-zeros-end", "Move Zeros to End", "Arrays - Easy", "Arrays", "Easy",
            "Move all 0's to the end of the array while maintaining the relative order of non-zero elements.",
            """
            // Java Move Zeroes 2-Pointer (LeetCode 283)
            public void moveZeroes(int[] nums) {
                int j = -1;
                for (int i = 0; i < nums.length; i++) {
                    if (nums[i] == 0) { j = i; break; }
                }
                if (j == -1) return;
                for (int i = j + 1; i < nums.length; i++) {
                    if (nums[i] != 0) {
                        int temp = nums[i]; nums[i] = nums[j]; nums[j] = temp;
                        j++;
                    }
                }
            }
            """,
            null, null, null, createArrayState(new int[]{0, 1, 0, 3, 12}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)", "Time Complexity: 2-pointer single-pass traversal.",
                "Why O(N)? Pointer j tracks first zero while pointer i scans non-zero elements.",
                "O(1)", "Space Complexity: In-place swaps.",
                "Why O(1)? Swaps elements within original array.", "Auxiliary Space: O(1)", "Output: O(1)"
            ), "Array"
        ));

        // 0.8 Linear Search
        problems.put("linear-search", new ProblemDetail(
            "linear-search", "Linear Search", "Arrays - Easy", "Arrays", "Easy",
            "Find index of target element in an unsorted or sorted array sequentially.",
            """
            // Java Linear Search
            public int search(int[] arr, int num) {
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i] == num) return i;
                }
                return -1;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 3, 4, 5}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)", "Time Complexity: Scans elements from index 0 to N-1 until target is found.",
                "Why O(N)? Worst case requires inspecting all N elements.",
                "O(1)", "Space Complexity: O(1) auxiliary space.",
                "Why O(1)? Constant memory.", "Auxiliary Space: O(1)", "Output: O(1)"
            ), "Array"
        ));

        // 0.9 Union of Two Sorted Arrays
        problems.put("union-sorted-arrays", new ProblemDetail(
            "union-sorted-arrays", "Union of Two Sorted Arrays", "Arrays - Easy", "Arrays", "Easy",
            "Find the union of two sorted arrays containing unique elements in sorted order.",
            """
            // Java Union of Sorted Arrays 2-Pointer Merge
            public List<Integer> findUnion(int[] a, int[] b) {
                List<Integer> union = new ArrayList<>();
                int i = 0, j = 0;
                while (i < a.length && j < b.length) {
                    if (a[i] <= b[j]) {
                        if (union.isEmpty() || union.get(union.size() - 1) != a[i]) union.add(a[i]);
                        i++;
                    } else {
                        if (union.isEmpty() || union.get(union.size() - 1) != b[j]) union.add(b[j]);
                        j++;
                    }
                }
                while (i < a.length) {
                    if (union.isEmpty() || union.get(union.size() - 1) != a[i]) union.add(a[i]);
                    i++;
                }
                while (j < b.length) {
                    if (union.isEmpty() || union.get(union.size() - 1) != b[j]) union.add(b[j]);
                    j++;
                }
                return union;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 1, 2, 3, 4, 5}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N1 + N2)", "Time Complexity: Dual pointer merge scanning both arrays simultaneously.",
                "Why O(N1 + N2)? Advances index i or j at each step.",
                "O(N1 + N2)", "Space Complexity: O(N1 + N2) auxiliary space for union result list.",
                "Why O(N1 + N2)? Stores unique merged elements.", "Auxiliary Space: O(N1 + N2)", "Output List: O(N1 + N2)"
            ), "Array"
        ));

        // 0.10 Find Missing Number
        problems.put("find-missing-number", new ProblemDetail(
            "find-missing-number", "Find Missing Number", "Arrays - Easy", "Arrays", "Easy",
            "Find the single missing number in an array containing N distinct numbers in range [0, N].",
            """
            // Java Find Missing Number Sum Formula (LeetCode 268)
            public int missingNumber(int[] nums) {
                int n = nums.length;
                int expectedSum = n * (n + 1) / 2;
                int actualSum = 0;
                for (int num : nums) {
                    actualSum += num;
                }
                return expectedSum - actualSum;
            }
            """,
            null, null, null, createArrayState(new int[]{3, 0, 1}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)", "Time Complexity: Single loop summing up array elements.",
                "Why O(N)? Uses Gauss sum formula N*(N+1)/2.",
                "O(1)", "Space Complexity: O(1) auxiliary space.",
                "Why O(1)? Uses 2 integer variables (expectedSum, actualSum).", "Auxiliary Space: O(1)", "Output: O(1)"
            ), "Array"
        ));

        // 0.11 Max Consecutive Ones
        problems.put("max-consecutive-ones", new ProblemDetail(
            "max-consecutive-ones", "Maximum Consecutive Ones", "Arrays - Easy", "Arrays", "Easy",
            "Find the maximum number of consecutive 1's in a binary array.",
            """
            // Java Max Consecutive Ones (LeetCode 485)
            public int findMaxConsecutiveOnes(int[] nums) {
                int maxCount = 0, count = 0;
                for (int i = 0; i < nums.length; i++) {
                    if (nums[i] == 1) {
                        count++;
                        maxCount = Math.max(maxCount, count);
                    } else {
                        count = 0;
                    }
                }
                return maxCount;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 1, 0, 1, 1, 1}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)", "Time Complexity: Single pass inspecting each binary element.",
                "Why O(N)? Increments count for 1 and resets for 0.",
                "O(1)", "Space Complexity: O(1) auxiliary variables.",
                "Why O(1)? Constant scalar memory.", "Auxiliary Space: O(1)", "Output: O(1)"
            ), "Array"
        ));

        // 0.12 Single Number
        problems.put("single-number", new ProblemDetail(
            "single-number", "Find Number Appears Once, Others Twice", "Arrays - Easy", "Arrays", "Easy",
            "Find the element that appears exactly once in an array where every other element appears twice.",
            """
            // Java Single Number Bitwise XOR (LeetCode 136)
            public int singleNumber(int[] nums) {
                int xorSum = 0;
                for (int num : nums) {
                    xorSum ^= num;
                }
                return xorSum;
            }
            """,
            null, null, null, createArrayState(new int[]{4, 1, 2, 1, 2}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)", "Time Complexity: Single pass XORing all elements together.",
                "Why O(N)? Bitwise XOR property a ^ a = 0 cancels out paired numbers.",
                "O(1)", "Space Complexity: O(1) auxiliary space.",
                "Why O(1)? Single accumulator variable xorSum.", "Auxiliary Space: O(1)", "Output: O(1)"
            ), "Array"
        ));

        // 0.13 Longest Subarray with Sum K (Positives)
        problems.put("longest-subarray-sum-k-positives", new ProblemDetail(
            "longest-subarray-sum-k-positives", "Longest Subarray with Sum K (Positives)", "Arrays - Easy/Medium", "Arrays", "Easy",
            "Find the length of the longest subarray having sum equal to K in an array of positive integers using 2-pointer sliding window.",
            """
            // Java Longest Subarray Sum K Sliding Window (Positive Numbers)
            public int getLongestSubarray(int[] a, long k) {
                int left = 0, right = 0;
                long sum = a[0];
                int maxLen = 0;
                int n = a.length;
                while (right < n) {
                    while (left <= right && sum > k) {
                        sum -= a[left];
                        left++;
                    }
                    if (sum == k) {
                        maxLen = Math.max(maxLen, right - left + 1);
                    }
                    right++;
                    if (right < n) sum += a[right];
                }
                return maxLen;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 2, 3, 1, 1, 1, 1, 4, 2, 3}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(2N) = O(N)", "Time Complexity: Two pointers left and right move forward at most N steps.",
                "Why O(N)? Each element is added once by right and subtracted once by left.",
                "O(1)", "Space Complexity: O(1) sliding window boundaries.",
                "Why O(1)? No auxiliary data structures needed for positive numbers.", "Auxiliary Space: O(1)", "Output: O(1)"
            ), "Array"
        ));

        // 0.14 Longest Subarray with Sum K (Positives & Negatives)
        problems.put("longest-subarray-sum-k", new ProblemDetail(
            "longest-subarray-sum-k", "Longest Subarray with Sum K (Positives & Negatives)", "Arrays - Medium", "Arrays", "Medium",
            "Find length of longest subarray with sum K in array containing positive and negative numbers using Prefix Sum & HashMap.",
            """
            // Java Longest Subarray Sum K Prefix Sum + HashMap
            public int getLongestSubarray(int[] a, long k) {
                Map<Long, Integer> preSumMap = new HashMap<>();
                long sum = 0;
                int maxLen = 0;
                for (int i = 0; i < a.length; i++) {
                    sum += a[i];
                    if (sum == k) maxLen = Math.max(maxLen, i + 1);
                    long rem = sum - k;
                    if (preSumMap.containsKey(rem)) {
                        int len = i - preSumMap.get(rem);
                        maxLen = Math.max(maxLen, len);
                    }
                    if (!preSumMap.containsKey(sum)) {
                        preSumMap.put(sum, i);
                    }
                }
                return maxLen;
            }
            """,
            null, null, null, createArrayState(new int[]{-1, 2, 3, -2, 1}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)", "Time Complexity: Single pass updating prefix sums and HashMap lookup.",
                "Why O(N)? HashMap lookup takes O(1) average time.",
                "O(N)", "Space Complexity: O(N) space for prefix sum HashMap.",
                "Why O(N)? In worst case, stores N distinct prefix sums.", "Auxiliary Space: O(N) HashMap", "Output: O(1)"
            ), "Array"
        ));

        // 1. Two Sum
        problems.put("two-sum", new ProblemDetail(
            "two-sum", "Two Sum", "Arrays - Easy/Medium", "Arrays", "Easy",
            "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.",
            """
            // Java Two Sum - HashMap Solution (LeetCode 1)
            public int[] twoSum(int[] nums, int target) {
                Map<Integer, Integer> map = new HashMap<>();
                for (int i = 0; i < nums.length; i++) {
                    int complement = target - nums[i];
                    if (map.containsKey(complement)) {
                        return new int[]{map.get(complement), i};
                    }
                    map.put(nums[i], i);
                }
                return new int[]{};
            }
            """,
            null, null, null, createTwoSumArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass iteration over the array of size N. HashMap lookup takes O(1) average time.",
                "Why O(N)? HashMap stores previously seen numbers and their indices for instant complement checking.",
                "O(N)",
                "Space Complexity: O(N) auxiliary space for HashMap.",
                "Why O(N)? In the worst case, we store up to N-1 elements in the map.",
                "Auxiliary Space: O(N) (HashMap)",
                "Array Output: O(1)"
            ),
            "Array"
        ));

        // 2. Sort 0s, 1s, 2s
        problems.put("sort-0-1-2", new ProblemDetail(
            "sort-0-1-2", "Sort an Array of 0s, 1s and 2s", "Arrays - Medium", "Arrays", "Medium",
            "Sort an array of 0s, 1s, and 2s in single pass O(N) time using Dutch National Flag 3-pointer algorithm.",
            """
            // Java Dutch National Flag Algorithm (LeetCode 75)
            public void sortColors(int[] nums) {
                int low = 0, mid = 0, high = nums.length - 1;
                while (mid <= high) {
                    if (nums[mid] == 0) {
                        int temp = nums[low]; nums[low] = nums[mid]; nums[mid] = temp;
                        low++; mid++;
                    } else if (nums[mid] == 1) {
                        mid++;
                    } else { // nums[mid] == 2
                        int temp = nums[mid]; nums[mid] = nums[high]; nums[high] = temp;
                        high--;
                    }
                }
            }
            """,
            null, null, null, createSort012Array(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass traversal where `mid` pointer travels from index 0 to `high`.",
                "Why 3 Pointers work? Maintains invariants: nums[0..low-1] are 0s, nums[low..mid-1] are 1s, nums[high+1..n-1] are 2s.",
                "O(1)",
                "Space Complexity: In-place sorting requiring O(1) extra space.",
                "Why O(1)? Modifies array elements directly via pointer swaps.",
                "Auxiliary Space: O(1)",
                "Array Space: In-place O(1)"
            ),
            "Array"
        ));

        // 3. Majority Element I
        problems.put("majority-element", new ProblemDetail(
            "majority-element", "Majority Element (> N/2 times)", "Arrays - Medium", "Arrays", "Easy",
            "Find the element that appears more than N/2 times in an array using Moore's Voting Algorithm.",
            """
            // Java Moore's Voting Algorithm (LeetCode 169)
            public int majorityElement(int[] nums) {
                int count = 0, el = 0;
                for (int i = 0; i < nums.length; i++) {
                    if (count == 0) {
                        count = 1; el = nums[i];
                    } else if (nums[i] == el) {
                        count++;
                    } else {
                        count--;
                    }
                }
                return el;
            }
            """,
            null, null, null, createMajorityArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass iteration over the array of size N.",
                "Why Moore's Voting works? Equal elements cancel out non-majority elements, leaving the true majority element as candidate `el`.",
                "O(1)",
                "Space Complexity: Constant O(1) auxiliary space.",
                "Why O(1)? Uses only two integer variables: `count` and `el`.",
                "Auxiliary Space: O(1)",
                "Return Value: O(1)"
            ),
            "Array"
        ));

        // 4. Kadane's Algorithm
        problems.put("kadane-algo", new ProblemDetail(
            "kadane-algo", "Kadane's Algorithm (Max Subarray Sum)", "Arrays - Medium", "Arrays", "Medium",
            "Find the contiguous subarray with the largest sum.",
            """
            // Java Kadane's Algorithm (LeetCode 53)
            public int maxSubArray(int[] nums) {
                int maxi = Integer.MIN_VALUE, sum = 0;
                for (int i = 0; i < nums.length; i++) {
                    sum += nums[i];
                    if (sum > maxi) maxi = sum;
                    if (sum < 0) sum = 0; // Reset sum if it drops below 0!
                }
                return maxi;
            }
            """,
            null, null, null, createKadaneArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass through array of N numbers.",
                "Why reset sum when < 0? A negative running sum will only decrease the sum of any subsequent subarray, so discarding it is optimal.",
                "O(1)",
                "Space Complexity: O(1) space.",
                "Why O(1)? Tracks running sum and maximum sum in primitive variables.",
                "Auxiliary Space: O(1)",
                "Return Sum: O(1)"
            ),
            "Array"
        ));

        // 5. Print Subarray with Max Subarray Sum
        problems.put("print-max-subarray", new ProblemDetail(
            "print-max-subarray", "Print Subarray with Max Subarray Sum", "Arrays - Medium", "Arrays", "Medium",
            "Find and print the actual elements of the contiguous subarray with maximum sum using Kadane's tracking pointers.",
            """
            // Java Print Subarray with Max Sum (Striver A2Z Sheet)
            public int[] maxSubarrayWithPrint(int[] arr) {
                int maxi = Integer.MIN_VALUE, sum = 0;
                int start = 0, ansStart = -1, ansEnd = -1;

                for (int i = 0; i < arr.length; i++) {
                    if (sum == 0) start = i;
                    sum += arr[i];

                    if (sum > maxi) {
                        maxi = sum;
                        ansStart = start;
                        ansEnd = i;
                    }
                    if (sum < 0) sum = 0;
                }
                return Arrays.copyOfRange(arr, ansStart, ansEnd + 1);
            }
            """,
            null, null, null, createKadaneArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single O(N) pass to identify max sum range [ansStart..ansEnd].",
                "Why track `start`? When sum resets to 0, potential new maximum subarray starts at current index `i`.",
                "O(1)",
                "Space Complexity: O(1) auxiliary space (excluding result subarray).",
                "Why O(1)? Only uses pointers `start`, `ansStart`, `ansEnd`.",
                "Auxiliary Space: O(1)",
                "Subarray Output: O(K)"
            ),
            "Array"
        ));

        // 6. Stock Buy and Sell
        problems.put("stock-buy-sell", new ProblemDetail(
            "stock-buy-sell", "Best Time to Buy and Sell Stock", "Arrays - Easy", "Arrays", "Easy",
            "Maximize profit by choosing a single day to buy one stock and choosing a different day in future to sell.",
            """
            // Java Stock Buy & Sell (LeetCode 121)
            public int maxProfit(int[] prices) {
                int minPrice = Integer.MAX_VALUE, maxProfit = 0;
                for (int i = 0; i < prices.length; i++) {
                    minPrice = Math.min(minPrice, prices[i]);
                    int profit = prices[i] - minPrice;
                    maxProfit = Math.max(maxProfit, profit);
                }
                return maxProfit;
            }
            """,
            null, null, null, createStockArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single loop pass through N price points.",
                "Why O(N)? Keeps track of minimum price seen so far while updating max profit at each step.",
                "O(1)",
                "Space Complexity: Constant O(1) memory.",
                "Why O(1)? Requires only `minPrice` and `maxProfit` variables.",
                "Auxiliary Space: O(1)",
                "Return Profit: O(1)"
            ),
            "Array"
        ));

        // 7. Rearrange Array Elements by Sign
        problems.put("rearrange-by-sign", new ProblemDetail(
            "rearrange-by-sign", "Rearrange Array Elements by Sign", "Arrays - Medium", "Arrays", "Medium",
            "Rearrange array containing equal positive and negative integers so that positive numbers are at even indices and negative at odd indices.",
            """
            // Java Rearrange Array Elements by Sign (LeetCode 2149)
            public int[] rearrangeArray(int[] nums) {
                int n = nums.length;
                int[] ans = new int[n];
                int posIndex = 0, negIndex = 1;

                for (int i = 0; i < n; i++) {
                    if (nums[i] > 0) {
                        ans[posIndex] = nums[i]; posIndex += 2;
                    } else {
                        ans[negIndex] = nums[i]; negIndex += 2;
                    }
                }
                return ans;
            }
            """,
            null, null, null, createRearrangeArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass iteration O(N) over input array.",
                "Why O(N)? Places positive elements at even indices and negative elements at odd indices in 1 pass.",
                "O(N)",
                "Space Complexity: O(N) space for output array `ans`.",
                "Why O(N)? Result array stores N rearranged elements.",
                "Auxiliary Space: O(N)",
                "Array Output: O(N)"
            ),
            "Array"
        ));

        // 8. Next Permutation
        problems.put("next-permutation", new ProblemDetail(
            "next-permutation", "Next Permutation", "Arrays - Medium", "Arrays", "Medium",
            "Rearrange numbers into lexicographically next greater permutation of numbers in O(N) time.",
            """
            // Java Next Permutation (LeetCode 31)
            public void nextPermutation(int[] nums) {
                int n = nums.length, ind = -1;
                for (int i = n - 2; i >= 0; i--) {
                    if (nums[i] < nums[i + 1]) { ind = i; break; }
                }
                if (ind == -1) { reverse(nums, 0, n - 1); return; }
                for (int i = n - 1; i > ind; i--) {
                    if (nums[i] > nums[ind]) { swap(nums, i, ind); break; }
                }
                reverse(nums, ind + 1, n - 1);
            }
            """,
            null, null, null, createNextPermArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: 3 linear passes O(N): find breakpoint `ind`, find swap candidate, reverse suffix.",
                "Why 3-step algorithm works? Finds first decreasing element from right, swaps with smallest greater value, then reverses remaining suffix.",
                "O(1)",
                "Space Complexity: In-place permutation requiring O(1) auxiliary space.",
                "Why O(1)? Modifies input array directly via swaps.",
                "Auxiliary Space: O(1)",
                "Array Space: In-place O(1)"
            ),
            "Array"
        ));

        // 9. Leaders in an Array
        problems.put("leaders-in-array", new ProblemDetail(
            "leaders-in-array", "Leaders in an Array", "Arrays - Easy", "Arrays", "Easy",
            "An element is a Leader if it is greater than all elements to its right. Rightmost element is always a leader.",
            """
            // Java Leaders in an Array (GeeksforGeeks / Striver Sheet)
            public List<Integer> findLeaders(int[] arr) {
                List<Integer> ans = new ArrayList<>();
                int n = arr.length, maxi = Integer.MIN_VALUE;

                for (int i = n - 1; i >= 0; i--) {
                    if (arr[i] > maxi) {
                        ans.add(arr[i]); maxi = arr[i];
                    }
                }
                Collections.reverse(ans);
                return ans;
            }
            """,
            null, null, null, createLeadersArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass right-to-left scan O(N) + O(N) reverse list.",
                "Why Right-to-Left scan? By moving right-to-left, we track `maxi` seen so far. If `arr[i] > maxi`, `arr[i]` is guaranteed to be a Leader!",
                "O(N)",
                "Space Complexity: O(N) space for result list.",
                "Why O(N)? Stores up to N leader elements.",
                "Auxiliary Space: O(1) (excluding result list)",
                "Result List: O(N)"
            ),
            "Array"
        ));

        // 10. Longest Consecutive Sequence
        problems.put("longest-consecutive-sequence", new ProblemDetail(
            "longest-consecutive-sequence", "Longest Consecutive Sequence", "Arrays - Medium", "Arrays", "Medium",
            "Given an unsorted array of integers nums, return the length of the longest consecutive elements sequence in O(N) time.",
            """
            // Java Longest Consecutive Sequence (LeetCode 128)
            public int longestConsecutive(int[] nums) {
                Set<Integer> set = new HashSet<>();
                for (int num : nums) set.add(num);
                int longest = 0;

                for (int it : set) {
                    if (!set.contains(it - 1)) {
                        int cnt = 1, x = it;
                        while (set.contains(x + 1)) { x++; cnt++; }
                        longest = Math.max(longest, cnt);
                    }
                }
                return longest;
            }
            """,
            null, null, null, createConsecutiveArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Insert into HashSet O(N), outer loop + inner while loop visits each element at most twice $\\implies$ O(N) total.",
                "Why O(N) time? `!set.contains(it - 1)` ensures we only initiate sequence counting at sequence starting elements.",
                "O(N)",
                "Space Complexity: O(N) auxiliary space for HashSet.",
                "Why O(N)? HashSet stores all unique numbers.",
                "Auxiliary Space: O(N) (HashSet)",
                "Return Count: O(1)"
            ),
            "Array"
        ));

        // 11. Set Matrix Zeroes
        problems.put("set-matrix-zeroes", new ProblemDetail(
            "set-matrix-zeroes", "Set Matrix Zeroes", "Arrays - Matrix", "Arrays", "Medium",
            "Given an M x N integer matrix, if an element is 0, set its entire row and column to 0 in-place.",
            """
            // Java Set Matrix Zeroes (LeetCode 73)
            public void setZeroes(int[][] matrix) {
                int col0 = 1, rows = matrix.length, cols = matrix[0].length;
                for (int i = 0; i < rows; i++) {
                    if (matrix[i][0] == 0) col0 = 0;
                    for (int j = 1; j < cols; j++) {
                        if (matrix[i][j] == 0) {
                            matrix[i][0] = 0; matrix[0][j] = 0;
                        }
                    }
                }
                for (int i = rows - 1; i >= 0; i--) {
                    for (int j = cols - 1; j >= 1; j--) {
                        if (matrix[i][0] == 0 || matrix[0][j] == 0) matrix[i][j] = 0;
                    }
                    if (col0 == 0) matrix[i][0] = 0;
                }
            }
            """,
            null, null, null, null, null, null, createDefaultMatrix(),
            new ComplexityDetail(
                "O(M * N)",
                "Time Complexity: 2 passes over M x N matrix $\\implies$ O(M * N) total time.",
                "Why in-place marking works? Uses first row `matrix[0][j]` and first column `matrix[i][0]` as marker arrays.",
                "O(1)",
                "Space Complexity: In-place O(1) space.",
                "Why O(1)? Uses only `col0` flag instead of extra dummy arrays.",
                "Auxiliary Space: O(1)",
                "Matrix Space: In-place O(1)"
            ),
            "Matrix"
        ));

        // 12. Rotate Matrix by 90 Degrees
        problems.put("rotate-matrix-90", new ProblemDetail(
            "rotate-matrix-90", "Rotate Matrix by 90 Degrees", "Arrays - Matrix", "Arrays", "Medium",
            "Rotate an N x N 2D matrix 90 degrees clockwise in-place by transposing the matrix and reversing each row.",
            """
            // Java Rotate Image / Matrix (LeetCode 48)
            public void rotate(int[][] matrix) {
                int n = matrix.length;
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        int temp = matrix[i][j]; matrix[i][j] = matrix[j][i]; matrix[j][i] = temp;
                    }
                }
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n / 2; j++) {
                        int temp = matrix[i][j]; matrix[i][j] = matrix[i][n - 1 - j]; matrix[i][n - 1 - j] = temp;
                    }
                }
            }
            """,
            null, null, null, null, null, null, createSquareMatrix(),
            new ComplexityDetail(
                "O(N^2)",
                "Time Complexity: Transpose pass takes O(N^2 / 2), row reversal pass takes O(N^2 / 2). Total = O(N^2).",
                "Why Transpose + Reverse = 90° Clockwise Rotation? Transposition swaps `(i, j)` to `(j, i)`, then reversing rows moves column `j` to `N - 1 - j`.",
                "O(1)",
                "Space Complexity: In-place matrix rotation requiring O(1) space.",
                "Why O(1)? Swaps elements directly in input matrix.",
                "Auxiliary Space: O(1)",
                "Matrix Space: In-place O(1)"
            ),
            "Matrix"
        ));

        // 13. Spiral Traversal of Matrix
        problems.put("spiral-matrix", new ProblemDetail(
            "spiral-matrix", "Spiral Traversal of Matrix", "Arrays - Matrix", "Arrays", "Medium",
            "Traverse an M x N matrix in spiral order using 4 boundary pointers: top, bottom, left, right.",
            """
            // Java Spiral Matrix (LeetCode 54)
            public List<Integer> spiralOrder(int[][] matrix) {
                List<Integer> ans = new ArrayList<>();
                int n = matrix.length, m = matrix[0].length;
                int top = 0, left = 0, bottom = n - 1, right = m - 1;

                while (top <= bottom && left <= right) {
                    for (int i = left; i <= right; i++) ans.add(matrix[top][i]);
                    top++;
                    for (int i = top; i <= bottom; i++) ans.add(matrix[i][right]);
                    right--;
                    if (top <= bottom) {
                        for (int i = right; i >= left; i--) ans.add(matrix[bottom][i]);
                        bottom--;
                    }
                    if (left <= right) {
                        for (int i = bottom; i >= top; i--) ans.add(matrix[i][left]);
                        left++;
                    }
                }
                return ans;
            }
            """,
            null, null, null, null, null, null, createSquareMatrix(),
            new ComplexityDetail(
                "O(M * N)",
                "Time Complexity: Every element of M x N matrix is visited exactly once.",
                "Why 4 Pointers work? `top`, `bottom`, `left`, `right` shrink the unvisited bounding box after completing each side of the spiral.",
                "O(M * N)",
                "Space Complexity: O(M * N) space for spiral result list.",
                "Why O(M * N)? Stores M*N elements in traversal output list.",
                "Auxiliary Space: O(1)",
                "Output List: O(M * N)"
            ),
            "Matrix"
        ));

        // 14. Count Subarrays with Given Sum
        problems.put("count-subarrays-given-sum", new ProblemDetail(
            "count-subarrays-given-sum", "Count Subarrays with Given Sum K", "Arrays - Medium", "Arrays", "Medium",
            "Return total number of contiguous subarrays whose sum equals K using Prefix Sum HashMap frequency counting.",
            """
            // Java Subarray Sum Equals K (LeetCode 560)
            public int subarraySum(int[] nums, int k) {
                Map<Integer, Integer> map = new HashMap<>();
                map.put(0, 1);
                int preSum = 0, cnt = 0;

                for (int i = 0; i < nums.length; i++) {
                    preSum += nums[i];
                    int remove = preSum - k;
                    cnt += map.getOrDefault(remove, 0);
                    map.put(preSum, map.getOrDefault(preSum, 0) + 1);
                }
                return cnt;
            }
            """,
            null, null, null, createCountSubarraysArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass iteration O(N) over array of size N.",
                "Why Prefix Sum HashMap works? If `preSum[i] - preSum[j] = K`, then subarray `nums[j+1..i]` has sum `K`. HashMap stores frequency of previous `preSum` values.",
                "O(N)",
                "Space Complexity: O(N) auxiliary space for HashMap.",
                "Why O(N)? In the worst case, up to N distinct prefix sums are stored in map.",
                "Auxiliary Space: O(N) (HashMap)",
                "Return Count: O(1)"
            ),
            "Array"
        ));

        // 15. Pascal's Triangle I
        problems.put("pascals-triangle", new ProblemDetail(
            "pascals-triangle", "Pascal's Triangle I", "Arrays - Hard", "Arrays", "Easy",
            "Given an integer numRows, return the first numRows of Pascal's Triangle where each element is the sum of the two elements directly above it.",
            """
            // Java Pascal's Triangle (LeetCode 118)
            public List<List<Integer>> generate(int numRows) {
                List<List<Integer>> res = new ArrayList<>();
                for (int i = 0; i < numRows; i++) {
                    List<Integer> row = new ArrayList<>();
                    for (int j = 0; j <= i; j++) {
                        if (j == 0 || j == i) row.add(1);
                        else row.add(res.get(i - 1).get(j - 1) + res.get(i - 1).get(j));
                    }
                    res.add(row);
                }
                return res;
            }
            """,
            null, null, null, null, null, null, createPascalsTriangleGrid(),
            new ComplexityDetail(
                "O(N^2)",
                "Time Complexity: Generating N rows requires 1 + 2 + 3 + ... + N = N*(N+1)/2 calculations $\\implies$ O(N^2).",
                "Why O(N^2)? Outer loop runs N times, inner loop runs `i+1` times for row `i`.",
                "O(N^2)",
                "Space Complexity: O(N^2) space to store the generated Pascal's Triangle.",
                "Why O(N^2)? Total integers stored across N rows equals N*(N+1)/2.",
                "Auxiliary Space: O(1)",
                "Triangle Space: O(N^2)"
            ),
            "Matrix"
        ));

        // 16. Majority Element II (> N/3)
        problems.put("majority-element-ii", new ProblemDetail(
            "majority-element-ii", "Majority Element II (> N/3 times)", "Arrays - Hard", "Arrays", "Medium",
            "Find all elements that appear more than ⌊ N/3 ⌋ times in an array using Extended Moore's Voting Algorithm (at most 2 candidates).",
            """
            // Java Majority Element II (LeetCode 229)
            public List<Integer> majorityElement(int[] nums) {
                int cnt1 = 0, cnt2 = 0, el1 = Integer.MIN_VALUE, el2 = Integer.MIN_VALUE;
                for (int i = 0; i < nums.length; i++) {
                    if (cnt1 == 0 && nums[i] != el2) { cnt1 = 1; el1 = nums[i]; }
                    else if (cnt2 == 0 && nums[i] != el1) { cnt2 = 1; el2 = nums[i]; }
                    else if (nums[i] == el1) cnt1++;
                    else if (nums[i] == el2) cnt2++;
                    else { cnt1--; cnt2--; }
                }
                List<Integer> ls = new ArrayList<>();
                cnt1 = 0; cnt2 = 0;
                for (int v : nums) {
                    if (v == el1) cnt1++;
                    if (v == el2) cnt2++;
                }
                int mini = nums.length / 3;
                if (cnt1 > mini) ls.add(el1);
                if (cnt2 > mini) ls.add(el2);
                return ls;
            }
            """,
            null, null, null, createMajority2Array(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: 2 passes over array of size N: 1st pass finds up to 2 candidates, 2nd pass verifies frequencies.",
                "Why at most 2 majority elements? An element appearing > N/3 times can exist at most 2 times because 3 * (> N/3) > N.",
                "O(1)",
                "Space Complexity: O(1) auxiliary space.",
                "Why O(1)? Uses 4 primitive integer variables: `el1`, `el2`, `cnt1`, `cnt2`.",
                "Auxiliary Space: O(1)",
                "Output List: O(1) (at most 2 elements)"
            ),
            "Array"
        ));

        // 17. 3 Sum
        problems.put("three-sum", new ProblemDetail(
            "three-sum", "3 Sum (Triplets with Sum 0)", "Arrays - Hard", "Arrays", "Medium",
            "Find all unique triplets in an array that add up to zero in O(N^2) time using Sorting and Two Pointers.",
            """
            // Java 3Sum (LeetCode 15)
            public List<List<Integer>> threeSum(int[] nums) {
                Arrays.sort(nums);
                List<List<Integer>> ans = new ArrayList<>();
                int n = nums.length;

                for (int i = 0; i < n; i++) {
                    if (i > 0 && nums[i] == nums[i - 1]) continue;
                    int j = i + 1, k = n - 1;
                    while (j < k) {
                        int sum = nums[i] + nums[j] + nums[k];
                        if (sum < 0) j++;
                        else if (sum > 0) k--;
                        else {
                            ans.add(Arrays.asList(nums[i], nums[j], nums[k]));
                            j++; k--;
                            while (j < k && nums[j] == nums[j - 1]) j++;
                            while (j < k && nums[k] == nums[k + 1]) k--;
                        }
                    }
                }
                return ans;
            }
            """,
            null, null, null, createThreeSumArray(), null, null, null,
            new ComplexityDetail(
                "O(N^2)",
                "Time Complexity: Sorting takes O(N log N). Outer loop runs N times, inner 2-pointer scan takes O(N) $\\implies$ Total = O(N^2).",
                "Why 2-pointer works after sorting? Sorting allows moving `j` right to increase sum and `k` left to decrease sum.",
                "O(1)",
                "Space Complexity: O(1) auxiliary space (excluding result list).",
                "Why O(1)? Modifies indices `i`, `j`, `k` directly without extra data structures.",
                "Auxiliary Space: O(1)",
                "Triplets List: O(K)"
            ),
            "Array"
        ));

        // 18. 4 Sum
        problems.put("four-sum", new ProblemDetail(
            "four-sum", "4 Sum (Quadruplets with Target Sum)", "Arrays - Hard", "Arrays", "Medium",
            "Find all unique quadruplets in an array that add up to a target sum in O(N^3) time.",
            """
            // Java 4Sum (LeetCode 18)
            public List<List<Integer>> fourSum(int[] nums, int target) {
                Arrays.sort(nums);
                List<List<Integer>> ans = new ArrayList<>();
                int n = nums.length;

                for (int i = 0; i < n; i++) {
                    if (i > 0 && nums[i] == nums[i - 1]) continue;
                    for (int j = i + 1; j < n; j++) {
                        if (j > i + 1 && nums[j] == nums[j - 1]) continue;
                        int k = j + 1, l = n - 1;
                        while (k < l) {
                            long sum = (long)nums[i] + nums[j] + nums[k] + nums[l];
                            if (sum == target) {
                                ans.add(Arrays.asList(nums[i], nums[j], nums[k], nums[l]));
                                k++; l--;
                                while (k < l && nums[k] == nums[k - 1]) k++;
                                while (k < l && nums[l] == nums[l + 1]) l--;
                            } else if (sum < target) k++;
                            else l--;
                        }
                    }
                }
                return ans;
            }
            """,
            null, null, null, createFourSumArray(), null, null, null,
            new ComplexityDetail(
                "O(N^3)",
                "Time Complexity: 2 nested loops O(N^2) + inner 2-pointer scan O(N) $\\implies$ Total O(N^3).",
                "Why fixed 2 outer loops? Fixing `i` and `j` reduces the problem to 2Sum for remaining target `target - nums[i] - nums[j]`.",
                "O(1)",
                "Space Complexity: O(1) auxiliary space.",
                "Why O(1)? Uses primitive 4-pointer variables `i`, `j`, `k`, `l`.",
                "Auxiliary Space: O(1)",
                "Quadruplets List: O(K)"
            ),
            "Array"
        ));

        // 19. Largest Subarray with Sum 0
        problems.put("largest-subarray-sum-0", new ProblemDetail(
            "largest-subarray-sum-0", "Largest Subarray with Sum 0", "Arrays - Hard", "Arrays", "Medium",
            "Find the length of the longest contiguous subarray whose elements sum to 0 using Prefix Sum HashMap.",
            """
            // Java Largest Subarray with Sum 0 (GeeksforGeeks / Striver Sheet)
            public int maxLen(int[] arr) {
                Map<Integer, Integer> map = new HashMap<>();
                int maxi = 0, sum = 0;

                for (int i = 0; i < arr.length; i++) {
                    sum += arr[i];
                    if (sum == 0) maxi = i + 1;
                    else {
                        if (map.containsKey(sum)) {
                            maxi = Math.max(maxi, i - map.get(sum));
                        } else {
                            map.put(sum, i);
                        }
                    }
                }
                return maxi;
            }
            """,
            null, null, null, createLargestSum0Array(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass iteration O(N) over array of size N.",
                "Why Prefix Sum HashMap works? If `sum[i] == sum[j]`, the subarray between `j+1` and `i` has sum = 0.",
                "O(N)",
                "Space Complexity: O(N) space for HashMap.",
                "Why O(N)? HashMap stores first occurrence index of each prefix sum.",
                "Auxiliary Space: O(N) (HashMap)",
                "Return Length: O(1)"
            ),
            "Array"
        ));

        // 20. Count Subarrays with Given XOR K
        problems.put("count-subarrays-xor-k", new ProblemDetail(
            "count-subarrays-xor-k", "Count Subarrays with Given XOR K", "Arrays - Hard", "Arrays", "Medium",
            "Find total number of contiguous subarrays having bitwise XOR equal to K using Prefix XOR HashMap.",
            """
            // Java Count Subarrays with XOR K (Striver A2Z Sheet / LeetCode)
            public int subarraysWithXorK(int[] a, int k) {
                int xr = 0, cnt = 0;
                Map<Integer, Integer> map = new HashMap<>();
                map.put(0, 1);

                for (int i = 0; i < a.length; i++) {
                    xr = xr ^ a[i];
                    int x = xr ^ k;
                    cnt += map.getOrDefault(x, 0);
                    map.put(xr, map.getOrDefault(xr, 0) + 1);
                }
                return cnt;
            }
            """,
            null, null, null, createSubarrayXorArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single pass O(N) iteration over array of size N.",
                "Why Prefix XOR works? If `xr[i] ^ xr[j] = K`, then `xr[j] = xr[i] ^ K`. HashMap stores frequency of previous cumulative XOR values.",
                "O(N)",
                "Space Complexity: O(N) auxiliary space for HashMap.",
                "Why O(N)? Stores up to N distinct prefix XOR values in map.",
                "Auxiliary Space: O(N) (HashMap)",
                "Return Count: O(1)"
            ),
            "Array"
        ));

        // 21. Merge Overlapping Subintervals
        problems.put("merge-intervals", new ProblemDetail(
            "merge-intervals", "Merge Overlapping Subintervals", "Arrays - Hard", "Arrays", "Medium",
            "Given an array of intervals, merge all overlapping intervals into non-overlapping intervals.",
            """
            // Java Merge Intervals (LeetCode 56)
            public int[][] merge(int[][] intervals) {
                Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));
                List<int[]> res = new ArrayList<>();

                for (int[] interval : intervals) {
                    if (res.isEmpty() || res.get(res.size() - 1)[1] < interval[0]) {
                        res.add(interval);
                    } else {
                        res.get(res.size() - 1)[1] = Math.max(res.get(res.size() - 1)[1], interval[1]);
                    }
                }
                return res.toArray(new int[res.size()][]);
            }
            """,
            null, null, null, null, null, null, createIntervalsGrid(),
            new ComplexityDetail(
                "O(N log N)",
                "Time Complexity: Sorting intervals takes O(N log N), linear merge scan takes O(N) $\\implies$ Total O(N log N).",
                "Why Sort by Start Time? Sorting guarantees that overlapping intervals are contiguous in the array.",
                "O(N)",
                "Space Complexity: O(N) space for merged intervals result list.",
                "Why O(N)? Stores non-overlapping merged intervals.",
                "Auxiliary Space: O(N)",
                "Result Grid: O(N)"
            ),
            "Matrix"
        ));

        // 22. Merge Two Sorted Arrays Without Extra Space
        problems.put("merge-two-sorted-arrays", new ProblemDetail(
            "merge-two-sorted-arrays", "Merge Two Sorted Arrays Without Extra Space", "Arrays - Hard", "Arrays", "Hard",
            "Merge two sorted arrays in-place without using extra space using Gap Method (Shell Sort variant).",
            """
            // Java Merge Two Sorted Arrays (Gap Method - Striver Sheet)
            public void merge(long[] arr1, long[] arr2, int n, int m) {
                int len = n + m;
                int gap = (len / 2) + (len % 2);

                while (gap > 0) {
                    int left = 0, right = left + gap;
                    while (right < len) {
                        // Swap logic across arr1 and arr2
                        if (left < n && right >= n) {
                            swapIfGreater(arr1, arr2, left, right - n);
                        } else if (left >= n) {
                            swapIfGreater(arr2, arr2, left - n, right - n);
                        } else {
                            swapIfGreater(arr1, arr1, left, right);
                        }
                        left++; right++;
                    }
                    if (gap == 1) break;
                    gap = (gap / 2) + (gap % 2);
                }
            }
            """,
            null, null, null, createTwoSortedArrays(), null, null, null,
            new ComplexityDetail(
                "O((N+M) log(N+M))",
                "Time Complexity: Gap starts at (N+M)/2 and halves each iteration (log(N+M) passes). Each pass makes (N+M) comparisons.",
                "Why Gap Method works? Based on Shell Sort's gap reduction idea to sort two merged sections in-place.",
                "O(1)",
                "Space Complexity: Constant O(1) extra space.",
                "Why O(1)? Performs in-place element swaps across arrays.",
                "Auxiliary Space: O(1)",
                "Array Space: In-place O(1)"
            ),
            "Array"
        ));

        // 23. Find Repeating and Missing Number
        problems.put("repeating-missing-number", new ProblemDetail(
            "repeating-missing-number", "Find Repeating and Missing Number", "Arrays - Hard", "Arrays", "Hard",
            "Given an array of size N containing numbers from 1 to N with 1 number repeating and 1 missing, find both in O(N) time and O(1) space.",
            """
            // Java Find Repeating and Missing Number (Math Method - Striver)
            public int[] findMissingRepeatingNumber(int[] a) {
                long n = a.length;
                long SN = (n * (n + 1)) / 2;
                long S2N = (n * (n + 1) * (2 * n + 1)) / 6;
                long S = 0, S2 = 0;

                for (int val : a) {
                    S += val; S2 += (long)val * val;
                }
                long val1 = S - SN; // X - Y
                long val2 = S2 - S2N; // X^2 - Y^2
                val2 = val2 / val1; // X + Y
                long x = (val1 + val2) / 2; // Repeating X
                long y = x - val1; // Missing Y
                return new int[]{(int)x, (int)y};
            }
            """,
            null, null, null, createRepeatingMissingArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single O(N) pass to compute sum of elements `S` and sum of squares `S2`.",
                "Why Math Equations work? $S - S_N = X - Y$ and $S^2 - S_N^2 = X^2 - Y^2$. Solving system of 2 linear equations yields $X$ (repeating) and $Y$ (missing).",
                "O(1)",
                "Space Complexity: O(1) auxiliary space.",
                "Why O(1)? Uses 4 primitive long sum variables.",
                "Auxiliary Space: O(1)",
                "Return Pair: O(1)"
            ),
            "Array"
        ));

        // 24. Count Inversions
        problems.put("count-inversions", new ProblemDetail(
            "count-inversions", "Count Inversions in an Array", "Arrays - Hard", "Arrays", "Hard",
            "Count pairs (i, j) such that i < j and arr[i] > arr[j] in O(N log N) time using modified Merge Sort Divide & Conquer.",
            """
            // Java Count Inversions (Merge Sort Variant - Striver)
            public int numberOfInversions(int[] a, int n) {
                return mergeSortCount(a, 0, n - 1);
            }

            private int mergeSortCount(int[] arr, int low, int high) {
                int cnt = 0;
                if (low >= high) return cnt;
                int mid = (low + high) / 2;
                cnt += mergeSortCount(arr, low, mid);
                cnt += mergeSortCount(arr, mid + 1, high);
                cnt += mergeAndCount(arr, low, mid, high);
                return cnt;
            }

            private int mergeAndCount(int[] arr, int low, int mid, int high) {
                List<Integer> temp = new ArrayList<>();
                int left = low, right = mid + 1, cnt = 0;
                while (left <= mid && right <= high) {
                    if (arr[left] <= arr[right]) {
                        temp.add(arr[left++]);
                    } else {
                        temp.add(arr[right++]);
                        cnt += (mid - left + 1); // All remaining left elements form inversions!
                    }
                }
                while (left <= mid) temp.add(arr[left++]);
                while (right <= high) temp.add(arr[right++]);
                for (int i = low; i <= high; i++) arr[i] = temp.get(i - low);
                return cnt;
            }
            """,
            null, null, null, createInversionsArray(), null, null, null,
            new ComplexityDetail(
                "O(N log N)",
                "Time Complexity: Divide & Conquer array splits log N times, merging takes O(N) $\\implies$ Total O(N log N).",
                "Why `cnt += (mid - left + 1)` works? When left element `arr[left] > arr[right]`, all elements from `left` to `mid` in sorted left subarray are also > `arr[right]`.",
                "O(N)",
                "Space Complexity: O(N) space for temporary merge array.",
                "Why O(N)? Temporary list holds merged elements.",
                "Auxiliary Space: O(N)",
                "Call Stack: O(log N)"
            ),
            "Array"
        ));

        // 25. Reverse Pairs
        problems.put("reverse-pairs", new ProblemDetail(
            "reverse-pairs", "Reverse Pairs (arr[i] > 2 * arr[j])", "Arrays - Hard", "Arrays", "Hard",
            "Count pairs (i, j) such that i < j and arr[i] > 2 * arr[j] in O(N log N) time using modified Merge Sort.",
            """
            // Java Reverse Pairs (LeetCode 493)
            public int reversePairs(int[] nums) {
                return mergeSortPairs(nums, 0, nums.length - 1);
            }

            private int mergeSortPairs(int[] arr, int low, int high) {
                if (low >= high) return 0;
                int mid = (low + high) / 2;
                int cnt = mergeSortPairs(arr, low, mid) + mergeSortPairs(arr, mid + 1, high);
                cnt += countPairs(arr, low, mid, high);
                merge(arr, low, mid, high);
                return cnt;
            }

            private int countPairs(int[] arr, int low, int mid, int high) {
                int right = mid + 1, cnt = 0;
                for (int i = low; i <= mid; i++) {
                    while (right <= high && (long)arr[i] > 2L * arr[right]) right++;
                    cnt += (right - (mid + 1));
                }
                return cnt;
            }
            """,
            null, null, null, createReversePairsArray(), null, null, null,
            new ComplexityDetail(
                "O(N log N)",
                "Time Complexity: 2-pointer pair counting takes O(N), merge takes O(N) $\\implies$ Total O(N log N).",
                "Why count BEFORE merging? Counting requires both left and right halves to be individually sorted before merge step.",
                "O(N)",
                "Space Complexity: O(N) auxiliary merge array space.",
                "Why O(N)? Holds temporary sorted subarray elements.",
                "Auxiliary Space: O(N)",
                "Call Stack: O(log N)"
            ),
            "Array"
        ));

        // 26. Maximum Product Subarray
        problems.put("max-product-subarray", new ProblemDetail(
            "max-product-subarray", "Maximum Product Subarray", "Arrays - Hard", "Arrays", "Medium",
            "Find the contiguous subarray that has the largest product in O(N) time using Prefix and Suffix scanning.",
            """
            // Java Maximum Product Subarray (LeetCode 152)
            public int maxProduct(int[] nums) {
                int n = nums.length;
                double pre = 1, suff = 1, maxi = Integer.MIN_VALUE;

                for (int i = 0; i < n; i++) {
                    if (pre == 0) pre = 1;
                    if (suff == 0) suff = 1;
                    pre *= nums[i];
                    suff *= nums[n - 1 - i];
                    maxi = Math.max(maxi, Math.max(pre, suff));
                }
                return (int)maxi;
            }
            """,
            null, null, null, createMaxProductArray(), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Single O(N) pass calculating prefix product and suffix product simultaneously.",
                "Why Prefix & Suffix Product works? Handles odd count of negative numbers by evaluating products from both left-to-right and right-to-left.",
                "O(1)",
                "Space Complexity: O(1) space.",
                "Why O(1)? Uses 3 primitive variables: `pre`, `suff`, `maxi`.",
                "Auxiliary Space: O(1)",
                "Return Product: O(1)"
            ),
            "Array"
        ));
    }

    // Dynamic Step Generators
    private List<ExecutionStep> generateTwoSumSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{2, 7, 11, 15};
        int target = 9;
        Map<Integer, Integer> map = new HashMap<>();
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Input Array: [2, 7, 11, 15], Target = 9. Initialize empty HashMap for O(1) lookup.",
            List.of(), Map.of(), List.of(), Map.of("target", "9", "map", "{}"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            steps.add(new ExecutionStep(
                stepNum++, 5,
                String.format("Loop i = %d (val %d): Calculate complement = target - nums[i] = 9 - %d = %d.", i, nums[i], nums[i], complement),
                List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "nums[i]", String.valueOf(nums[i]), "complement", String.valueOf(complement)),
                "Array", null, createArrayState(nums, i, -1), null, null
            ));

            if (map.containsKey(complement)) {
                int compIdx = map.get(complement);
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Complement Check: HashMap contains complement %d at index %d! Pair (%d, %d) sum to target 9!", complement, compIdx, nums[compIdx], nums[i]),
                    List.of(), Map.of(), List.of(), Map.of("Match Indices", String.format("[%d, %d]", compIdx, i), "Values", String.format("%d + %d = 9", nums[compIdx], nums[i])),
                    "Array", null, createArrayState(nums, compIdx, i), null, null
                ));
                steps.add(new ExecutionStep(
                    stepNum++, 8,
                    String.format("Two Sum Complete! Output indices: [%d, %d].", compIdx, i),
                    List.of(), Map.of(), List.of(), Map.of("Result", String.format("[%d, %d]", compIdx, i)),
                    "Array", null, createArrayState(nums, compIdx, i), null, null
                ));
                return steps;
            } else {
                map.put(nums[i], i);
                steps.add(new ExecutionStep(
                    stepNum++, 9,
                    String.format("Complement %d NOT in HashMap. Put map.put(val=%d, index=%d). HashMap state: %s.", complement, nums[i], i, map.toString()),
                    List.of(), Map.of(), List.of(), Map.of("map", map.toString()),
                    "Array", null, createArrayState(nums, i, -1), null, null
                ));
            }
        }
        return steps;
    }

    private List<ExecutionStep> generateSort012Steps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{2, 0, 2, 1, 1, 0};
        int low = 0, mid = 0, high = nums.length - 1;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Dutch National Flag Algorithm: Maintain 3 pointers low=0, mid=0, high=5. Invariants: [0..low-1]=0s, [low..mid-1]=1s, [high+1..n-1]=2s.",
            List.of(), Map.of(), List.of(), Map.of("low", "0", "mid", "0", "high", "5"),
            "Array", null, createArrayState(nums, mid, high), null, null
        ));

        while (mid <= high) {
            if (nums[mid] == 0) {
                int temp = nums[low]; nums[low] = nums[mid]; nums[mid] = temp;
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("nums[mid=%d] == 0: Swap nums[low=%d] (%d) and nums[mid=%d] (%d). Increment low++, mid++. Array: %s.", mid, low, temp, mid, nums[low], Arrays.toString(nums)),
                    List.of(), Map.of(), List.of(), Map.of("low", String.valueOf(low + 1), "mid", String.valueOf(mid + 1), "high", String.valueOf(high)),
                    "Array", null, createArrayState(nums, low, mid), null, null
                ));
                low++; mid++;
            } else if (nums[mid] == 1) {
                steps.add(new ExecutionStep(
                    stepNum++, 10,
                    String.format("nums[mid=%d] == 1: Element 1 is already in correct region [low..mid-1]. Increment mid++.", mid),
                    List.of(), Map.of(), List.of(), Map.of("mid", String.valueOf(mid + 1)),
                    "Array", null, createArrayState(nums, mid, -1), null, null
                ));
                mid++;
            } else { // nums[mid] == 2
                int temp = nums[mid]; nums[mid] = nums[high]; nums[high] = temp;
                steps.add(new ExecutionStep(
                    stepNum++, 13,
                    String.format("nums[mid=%d] == 2: Swap nums[mid=%d] (%d) and nums[high=%d] (%d). Decrement high--. Array: %s.", mid, mid, temp, high, nums[mid], Arrays.toString(nums)),
                    List.of(), Map.of(), List.of(), Map.of("high", String.valueOf(high - 1)),
                    "Array", null, createArrayState(nums, mid, high), null, null
                ));
                high--;
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 16,
            "Dutch National Flag Complete! Array is sorted in single pass O(N) time: [0, 0, 1, 1, 2, 2].",
            List.of(), Map.of(), List.of(), Map.of("Status", "Sorted", "Output", "[0, 0, 1, 1, 2, 2]"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateMajorityElementSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{2, 2, 1, 1, 1, 2, 2};
        int count = 0, el = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Moore's Voting Algorithm: Find candidate element > N/2 times. Initialize count = 0, candidate el = 0.",
            List.of(), Map.of(), List.of(), Map.of("count", "0", "el", "0"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = 0; i < nums.length; i++) {
            if (count == 0) {
                count = 1; el = nums[i];
                steps.add(new ExecutionStep(
                    stepNum++, 6,
                    String.format("Loop i = %d (val %d): count is 0. Set new candidate el = %d, count = 1.", i, nums[i], el),
                    List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "candidate", String.valueOf(el), "count", "1"),
                    "Array", null, createArrayState(nums, i, -1), null, null
                ));
            } else if (nums[i] == el) {
                count++;
                steps.add(new ExecutionStep(
                    stepNum++, 8,
                    String.format("Loop i = %d (val %d): nums[%d] matches candidate %d. Increment count = %d.", i, nums[i], i, el, count),
                    List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "candidate", String.valueOf(el), "count", String.valueOf(count)),
                    "Array", null, createArrayState(nums, i, -1), null, null
                ));
            } else {
                count--;
                steps.add(new ExecutionStep(
                    stepNum++, 10,
                    String.format("Loop i = %d (val %d): nums[%d] != candidate %d. Decrement count = %d.", i, nums[i], i, el, count),
                    List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "candidate", String.valueOf(el), "count", String.valueOf(count)),
                    "Array", null, createArrayState(nums, i, -1), null, null
                ));
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 13,
            String.format("Moore's Voting Complete! Majority Element (> N/2 times) = %d.", el),
            List.of(), Map.of(), List.of(), Map.of("Majority Element", String.valueOf(el)),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateKadaneSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4};
        int maxi = Integer.MIN_VALUE, sum = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Kadane's Algorithm: Find maximum contiguous subarray sum. Initialize maxi = -INF, running sum = 0.",
            List.of(), Map.of(), List.of(), Map.of("maxi", "-INF", "sum", "0"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = 0; i < nums.length; i++) {
            sum += nums[i];
            if (sum > maxi) {
                maxi = sum;
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Loop i = %d (val %d): Add to sum = %d. Running sum (%d) > maxi! Update maxi = %d.", i, nums[i], sum, sum, maxi),
                    List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "sum", String.valueOf(sum), "maxi", String.valueOf(maxi)),
                    "Array", null, createArrayState(nums, i, -1), null, null
                ));
            } else {
                steps.add(new ExecutionStep(
                    stepNum++, 6,
                    String.format("Loop i = %d (val %d): Add to sum = %d. maxi remains %d.", i, nums[i], sum, maxi),
                    List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "sum", String.valueOf(sum), "maxi", String.valueOf(maxi)),
                    "Array", null, createArrayState(nums, i, -1), null, null
                ));
            }
            if (sum < 0) {
                sum = 0;
                steps.add(new ExecutionStep(
                    stepNum++, 8,
                    String.format("Loop i = %d: Running sum dropped below 0. Reset running sum = 0 to start fresh subarray!", i),
                    List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "sum_reset", "0"),
                    "Array", null, createArrayState(nums, i, -1), null, null
                ));
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 11,
            String.format("Kadane's Algorithm Complete! Maximum Subarray Sum = %d.", maxi),
            List.of(), Map.of(), List.of(), Map.of("Max Subarray Sum", String.valueOf(maxi)),
            "Array", null, createRangeArrayState(nums, 3, 6), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generatePrintMaxSubarraySteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4};
        int maxi = Integer.MIN_VALUE, sum = 0;
        int start = 0, ansStart = -1, ansEnd = -1;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Print Max Subarray: Track start, ansStart, and ansEnd pointers alongside running sum to identify exact subarray bounds.",
            List.of(), Map.of(), List.of(), Map.of("maxi", "-INF", "sum", "0", "start", "0"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = 0; i < nums.length; i++) {
            if (sum == 0) start = i;
            sum += nums[i];

            if (sum > maxi) {
                maxi = sum; ansStart = start; ansEnd = i;
                steps.add(new ExecutionStep(
                    stepNum++, 9,
                    String.format("Loop i = %d (val %d): New Max Sum = %d! Update ansStart = %d, ansEnd = %d. Current Subarray: %s.", i, nums[i], maxi, ansStart, ansEnd, Arrays.toString(Arrays.copyOfRange(nums, ansStart, ansEnd + 1))),
                    List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "sum", String.valueOf(sum), "maxi", String.valueOf(maxi), "ansRange", String.format("[%d..%d]", ansStart, ansEnd)),
                    "Array", null, createRangeArrayState(nums, ansStart, ansEnd), null, null
                ));
            } else {
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Loop i = %d (val %d): sum = %d <= maxi (%d). Range remains [%d..%d].", i, nums[i], sum, maxi, ansStart, ansEnd),
                    List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "sum", String.valueOf(sum), "maxi", String.valueOf(maxi)),
                    "Array", null, createArrayState(nums, i, -1), null, null
                ));
            }
            if (sum < 0) sum = 0;
        }

        steps.add(new ExecutionStep(
            stepNum++, 15,
            String.format("Print Subarray Complete! Max Subarray Sum = %d. Elements: [4, -1, 2, 1] at indices [%d..%d].", maxi, ansStart, ansEnd),
            List.of(), Map.of(), List.of(), Map.of("Max Sum", String.valueOf(maxi), "Subarray", "[4, -1, 2, 1]", "Indices", String.format("[%d..%d]", ansStart, ansEnd)),
            "Array", null, createRangeArrayState(nums, ansStart, ansEnd), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateStockSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] prices = new int[]{7, 1, 5, 3, 6, 4};
        int minPrice = Integer.MAX_VALUE, maxProfit = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Stock Buy & Sell: Maximize profit by buying low and selling high. Initialize minPrice = INF, maxProfit = 0.",
            List.of(), Map.of(), List.of(), Map.of("minPrice", "INF", "maxProfit", "0"),
            "Array", null, createArrayState(prices, -1, -1), null, null
        ));

        for (int i = 0; i < prices.length; i++) {
            if (prices[i] < minPrice) minPrice = prices[i];
            int profit = prices[i] - minPrice;

            if (profit > maxProfit) {
                maxProfit = profit;
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Day %d (price %d): Minimum buy price = %d! Potential profit = %d - %d = %d > maxProfit! Update maxProfit = %d.", i + 1, prices[i], minPrice, prices[i], minPrice, profit, maxProfit),
                    List.of(), Map.of(), List.of(), Map.of("Day", String.valueOf(i + 1), "minPrice", String.valueOf(minPrice), "maxProfit", String.valueOf(maxProfit)),
                    "Array", null, createArrayState(prices, 1, i), null, null
                ));
            } else {
                steps.add(new ExecutionStep(
                    stepNum++, 6,
                    String.format("Day %d (price %d): Potential profit = %d - %d = %d <= maxProfit (%d). maxProfit remains %d.", i + 1, prices[i], prices[i], minPrice, profit, maxProfit, maxProfit),
                    List.of(), Map.of(), List.of(), Map.of("Day", String.valueOf(i + 1), "profit", String.valueOf(profit), "maxProfit", String.valueOf(maxProfit)),
                    "Array", null, createArrayState(prices, i, -1), null, null
                ));
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 9,
            String.format("Stock Buy & Sell Complete! Maximum Profit = %d (Buy at 1 on Day 2, Sell at 6 on Day 5).", maxProfit),
            List.of(), Map.of(), List.of(), Map.of("Max Profit", String.valueOf(maxProfit), "Buy Price", "1", "Sell Price", "6"),
            "Array", null, createArrayState(prices, 1, 4), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateRearrangeBySignSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{3, 1, -2, -5, 2, -4};
        int n = nums.length;
        int[] ans = new int[n];
        int posIndex = 0, negIndex = 1;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Rearrange by Sign: Place positive numbers at even indices (0, 2, 4...) and negative at odd indices (1, 3, 5...). Initialize posIndex = 0, negIndex = 1.",
            List.of(), Map.of(), List.of(), Map.of("posIndex", "0", "negIndex", "1"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = 0; i < n; i++) {
            if (nums[i] > 0) {
                ans[posIndex] = nums[i];
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Loop i = %d (val %d > 0): Place %d at even index ans[%d]. Increment posIndex += 2 -> %d. Result: %s.", i, nums[i], nums[i], posIndex, posIndex + 2, Arrays.toString(ans)),
                    List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "placedAt", String.valueOf(posIndex), "posIndex", String.valueOf(posIndex + 2)),
                    "Array", null, createArrayState(ans, posIndex, -1), null, null
                ));
                posIndex += 2;
            } else {
                ans[negIndex] = nums[i];
                steps.add(new ExecutionStep(
                    stepNum++, 10,
                    String.format("Loop i = %d (val %d < 0): Place %d at odd index ans[%d]. Increment negIndex += 2 -> %d. Result: %s.", i, nums[i], nums[i], negIndex, negIndex + 2, Arrays.toString(ans)),
                    List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "placedAt", String.valueOf(negIndex), "negIndex", String.valueOf(negIndex + 2)),
                    "Array", null, createArrayState(ans, negIndex, -1), null, null
                ));
                negIndex += 2;
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 14,
            String.format("Rearrange Complete! Final rearranged output: %s.", Arrays.toString(ans)),
            List.of(), Map.of(), List.of(), Map.of("Status", "Rearranged", "Output", Arrays.toString(ans)),
            "Array", null, createArrayState(ans, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateNextPermutationSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 2, 3, 5, 4, 2};
        int n = nums.length;
        int ind = -1;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Next Permutation: Step 1 - Scan right-to-left to find first breakpoint index where nums[i] < nums[i+1]. Input: [1, 2, 3, 5, 4, 2].",
            List.of(), Map.of(), List.of(), Map.of("N", "6"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = n - 2; i >= 0; i--) {
            if (nums[i] < nums[i + 1]) {
                ind = i;
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Breakpoint Found: nums[i=%d] (%d) < nums[i+1=%d] (%d). Set breakpoint index ind = %d.", i, nums[i], i + 1, nums[i + 1], ind),
                    List.of(), Map.of(), List.of(), Map.of("ind", String.valueOf(ind), "val", String.valueOf(nums[ind])),
                    "Array", null, createArrayState(nums, ind, -1), null, null
                ));
                break;
            }
        }

        if (ind != -1) {
            for (int i = n - 1; i > ind; i--) {
                if (nums[i] > nums[ind]) {
                    int temp = nums[i]; nums[i] = nums[ind]; nums[ind] = temp;
                    steps.add(new ExecutionStep(
                        stepNum++, 13,
                        String.format("Step 2 - Swap with next greater: Swap nums[ind=%d] (%d) with nums[%d] (%d). Array: %s.", ind, temp, i, nums[ind], Arrays.toString(nums)),
                        List.of(), Map.of(), List.of(), Map.of("swapped", String.format("nums[%d] <-> nums[%d]", ind, i)),
                        "Array", null, createArrayState(nums, ind, i), null, null
                    ));
                    break;
                }
            }
        }

        int l = ind + 1, r = n - 1;
        while (l < r) {
            int temp = nums[l]; nums[l] = nums[r]; nums[r] = temp;
            l++; r--;
        }

        steps.add(new ExecutionStep(
            stepNum++, 17,
            String.format("Step 3 - Reverse Suffix [ind+1..n-1]: Reversing suffix creates lexicographically smallest next permutation: %s.", Arrays.toString(nums)),
            List.of(), Map.of(), List.of(), Map.of("Next Permutation", Arrays.toString(nums)),
            "Array", null, createRangeArrayState(nums, ind + 1, n - 1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateLeadersSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{16, 17, 4, 3, 5, 2};
        int n = nums.length;
        List<Integer> leaders = new ArrayList<>();
        int maxi = Integer.MIN_VALUE;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Leaders in an Array: Scan right-to-left tracking maxi seen so far. An element is a leader if arr[i] > maxi. Input: [16, 17, 4, 3, 5, 2].",
            List.of(), Map.of(), List.of(), Map.of("maxi", "-INF"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = n - 1; i >= 0; i--) {
            if (nums[i] > maxi) {
                maxi = nums[i];
                leaders.add(maxi);
                steps.add(new ExecutionStep(
                    stepNum++, 8,
                    String.format("Scan i = %d (val %d): %d > maxi! New Leader found! Update maxi = %d. Leaders list: %s.", i, nums[i], nums[i], maxi, leaders.toString()),
                    List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "newLeader", String.valueOf(maxi), "maxi", String.valueOf(maxi)),
                    "Array", null, createArrayState(nums, i, -1), null, null
                ));
            } else {
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Scan i = %d (val %d): %d <= maxi (%d). Not a leader.", i, nums[i], nums[i], maxi),
                    List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "maxi", String.valueOf(maxi)),
                    "Array", null, createArrayState(nums, i, -1), null, null
                ));
            }
        }

        Collections.reverse(leaders);

        steps.add(new ExecutionStep(
            stepNum++, 11,
            String.format("Leaders Complete! Output Leaders in left-to-right order: %s.", leaders.toString()),
            List.of(), Map.of(), List.of(), Map.of("Leaders", leaders.toString()),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateLongestConsecutiveSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{100, 4, 200, 1, 3, 2};
        Set<Integer> set = new HashSet<>();
        for (int num : nums) set.add(num);
        int longest = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Longest Consecutive Sequence: Add elements to HashSet. Only count sequence when element has NO left neighbor (it - 1) in set. Input: [100, 4, 200, 1, 3, 2].",
            List.of(), Map.of(), List.of(), Map.of("HashSet", set.toString()),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = 0; i < nums.length; i++) {
            int val = nums[i];
            if (!set.contains(val - 1)) {
                int cnt = 1, x = val;
                while (set.contains(x + 1)) { x++; cnt++; }
                longest = Math.max(longest, cnt);
                steps.add(new ExecutionStep(
                    stepNum++, 8,
                    String.format("Found sequence start val = %d (val - 1 = %d not in set). Counted streak [%d..%d] (length %d). Longest streak = %d.", val, val - 1, val, x, cnt, longest),
                    List.of(), Map.of(), List.of(), Map.of("start", String.valueOf(val), "streakLen", String.valueOf(cnt), "longest", String.valueOf(longest)),
                    "Array", null, createArrayState(nums, i, -1), null, null
                ));
            } else {
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Val = %d: %d is part of an ongoing sequence (%d exists in set). Skip sequence start counting.", val, val, val - 1),
                    List.of(), Map.of(), List.of(), Map.of("val", String.valueOf(val), "skip", "true"),
                    "Array", null, createArrayState(nums, i, -1), null, null
                ));
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 13,
            String.format("Longest Consecutive Sequence Complete! Longest sequence length = %d (Streak: [1, 2, 3, 4]).", longest),
            List.of(), Map.of(), List.of(), Map.of("Longest Length", String.valueOf(longest), "Streak", "[1, 2, 3, 4]"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateSetMatrixZeroesSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[][] matrix = new int[][]{
            {1, 1, 1, 1},
            {1, 0, 1, 1},
            {1, 1, 1, 1}
        };
        int rows = matrix.length, cols = matrix[0].length;
        int col0 = 1;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Set Matrix Zeroes: Use first row and first column as marker arrays in-place. Matrix size: 3 x 4.",
            List.of(), Map.of(), List.of(), Map.of("rows", "3", "cols", "4", "col0", "1"),
            "Matrix", cloneGrid(matrix), null, null, null
        ));

        for (int i = 0; i < rows; i++) {
            if (matrix[i][0] == 0) col0 = 0;
            for (int j = 1; j < cols; j++) {
                if (matrix[i][j] == 0) {
                    matrix[i][0] = 0; matrix[0][j] = 0;
                    steps.add(new ExecutionStep(
                        stepNum++, 8,
                        String.format("Found zero at cell [%d, %d]! Mark first row marker matrix[0][%d] = 0 and first col marker matrix[%d][0] = 0.", i, j, j, i),
                        List.of(), Map.of(), List.of(), Map.of("zeroCell", String.format("[%d, %d]", i, j)),
                        "Matrix", cloneGrid(matrix), null, null, null
                    ));
                }
            }
        }

        for (int i = rows - 1; i >= 0; i--) {
            for (int j = cols - 1; j >= 1; j--) {
                if (matrix[i][0] == 0 || matrix[0][j] == 0) matrix[i][j] = 0;
            }
            if (col0 == 0) matrix[i][0] = 0;
        }

        steps.add(new ExecutionStep(
            stepNum++, 15,
            "Set Matrix Zeroes Complete! All rows and columns containing zeros have been updated in-place.",
            List.of(), Map.of(), List.of(), Map.of("Status", "Complete"),
            "Matrix", cloneGrid(matrix), null, null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateRotateMatrixSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[][] matrix = new int[][]{
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        int n = matrix.length;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Rotate Matrix by 90° Clockwise: Step 1 - Transpose matrix (swap matrix[i][j] with matrix[j][i]). Step 2 - Reverse each row.",
            List.of(), Map.of(), List.of(), Map.of("N", "3"),
            "Matrix", cloneGrid(matrix), null, null, null
        ));

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int temp = matrix[i][j]; matrix[i][j] = matrix[j][i]; matrix[j][i] = temp;
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Transpose: Swap matrix[%d][%d] (%d) <-> matrix[%d][%d] (%d).", i, j, temp, j, i, matrix[i][j]),
                    List.of(), Map.of(), List.of(), Map.of("swap", String.format("[%d,%d] <-> [%d,%d]", i, j, j, i)),
                    "Matrix", cloneGrid(matrix), null, null, null
                ));
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n / 2; j++) {
                int temp = matrix[i][j]; matrix[i][j] = matrix[i][n - 1 - j]; matrix[i][n - 1 - j] = temp;
            }
            steps.add(new ExecutionStep(
                stepNum++, 14,
                String.format("Reverse Row %d: Reversed row %d. Matrix row is now: %s.", i, i, Arrays.toString(matrix[i])),
                List.of(), Map.of(), List.of(), Map.of("reversedRow", String.valueOf(i)),
                "Matrix", cloneGrid(matrix), null, null, null
            ));
        }

        steps.add(new ExecutionStep(
            stepNum++, 18,
            "Rotate Matrix Complete! Final 90° Clockwise Rotated Matrix.",
            List.of(), Map.of(), List.of(), Map.of("Status", "Rotated 90°"),
            "Matrix", cloneGrid(matrix), null, null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateSpiralMatrixSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[][] matrix = new int[][]{
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        List<Integer> ans = new ArrayList<>();
        int n = matrix.length, m = matrix[0].length;
        int top = 0, left = 0, bottom = n - 1, right = m - 1;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Spiral Traversal: Maintain 4 boundary pointers top=0, bottom=2, left=0, right=2.",
            List.of(), Map.of(), List.of(), Map.of("top", "0", "bottom", "2", "left", "0", "right", "2"),
            "Matrix", cloneGrid(matrix), null, null, null
        ));

        while (top <= bottom && left <= right) {
            for (int i = left; i <= right; i++) ans.add(matrix[top][i]);
            top++;
            steps.add(new ExecutionStep(
                stepNum++, 8,
                String.format("Traverse Top Row (Left to Right): Output: %s. Increment top++ -> %d.", ans.toString(), top),
                List.of(), Map.of(), List.of(), Map.of("top", String.valueOf(top), "ans", ans.toString()),
                "Matrix", cloneGrid(matrix), null, null, null
            ));

            for (int i = top; i <= bottom; i++) ans.add(matrix[i][right]);
            right--;
            steps.add(new ExecutionStep(
                stepNum++, 11,
                String.format("Traverse Right Column (Top to Bottom): Output: %s. Decrement right-- -> %d.", ans.toString(), right),
                List.of(), Map.of(), List.of(), Map.of("right", String.valueOf(right), "ans", ans.toString()),
                "Matrix", cloneGrid(matrix), null, null, null
            ));

            if (top <= bottom) {
                for (int i = right; i >= left; i--) ans.add(matrix[bottom][i]);
                bottom--;
                steps.add(new ExecutionStep(
                    stepNum++, 15,
                    String.format("Traverse Bottom Row (Right to Left): Output: %s. Decrement bottom-- -> %d.", ans.toString(), bottom),
                    List.of(), Map.of(), List.of(), Map.of("bottom", String.valueOf(bottom), "ans", ans.toString()),
                    "Matrix", cloneGrid(matrix), null, null, null
                ));
            }

            if (left <= right) {
                for (int i = bottom; i >= top; i--) ans.add(matrix[i][left]);
                left++;
                steps.add(new ExecutionStep(
                    stepNum++, 19,
                    String.format("Traverse Left Column (Bottom to Top): Output: %s. Increment left++ -> %d.", ans.toString(), left),
                    List.of(), Map.of(), List.of(), Map.of("left", String.valueOf(left), "ans", ans.toString()),
                    "Matrix", cloneGrid(matrix), null, null, null
                ));
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 23,
            String.format("Spiral Traversal Complete! Final Spiral Output: %s.", ans.toString()),
            List.of(), Map.of(), List.of(), Map.of("Spiral Order", ans.toString()),
            "Matrix", cloneGrid(matrix), null, null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateCountSubarraysGivenSumSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 1, 1};
        int k = 2;
        Map<Integer, Integer> map = new HashMap<>();
        map.put(0, 1);
        int preSum = 0, cnt = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Count Subarrays with Sum K=2: Prefix Sum HashMap frequency counting. Initialize map.put(0, 1), preSum = 0, cnt = 0.",
            List.of(), Map.of(), List.of(), Map.of("K", "2", "preSum", "0", "cnt", "0"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = 0; i < nums.length; i++) {
            preSum += nums[i];
            int remove = preSum - k;
            int matches = map.getOrDefault(remove, 0);
            cnt += matches;

            steps.add(new ExecutionStep(
                stepNum++, 7,
                String.format("Loop i = %d (val %d): preSum = %d. Need remove = preSum - K = %d - 2 = %d. map.getOrDefault(%d) = %d matches! Total count = %d.", i, nums[i], preSum, preSum, remove, remove, matches, cnt),
                List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "preSum", String.valueOf(preSum), "remove", String.valueOf(remove), "matches", String.valueOf(matches), "cnt", String.valueOf(cnt)),
                "Array", null, createArrayState(nums, i, -1), null, null
            ));

            map.put(preSum, map.getOrDefault(preSum, 0) + 1);
        }

        steps.add(new ExecutionStep(
            stepNum++, 11,
            String.format("Count Subarrays Complete! Total Subarrays with Sum K=2 is %d.", cnt),
            List.of(), Map.of(), List.of(), Map.of("Total Subarrays", String.valueOf(cnt)),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generatePascalsTriangleSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int numRows = 5;
        int[][] grid = new int[numRows][numRows];
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 3,
            "Pascal's Triangle I: Generate first 5 rows of Pascal's Triangle. Base condition: element C(i, j) = C(i-1, j-1) + C(i-1, j).",
            List.of(), Map.of(), List.of(), Map.of("numRows", "5"),
            "Matrix", cloneGrid(grid), null, null, null
        ));

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j <= i; j++) {
                if (j == 0 || j == i) {
                    grid[i][j] = 1;
                } else {
                    grid[i][j] = grid[i - 1][j - 1] + grid[i - 1][j];
                }
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Row %d, Col %d: Calculate value = %d. Pascal Row %d: %s.", i + 1, j + 1, grid[i][j], i + 1, Arrays.toString(Arrays.copyOfRange(grid[i], 0, i + 1))),
                    List.of(), Map.of(), List.of(), Map.of("row", String.valueOf(i + 1), "col", String.valueOf(j + 1), "val", String.valueOf(grid[i][j])),
                    "Matrix", cloneGrid(grid), null, null, null
                ));
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 12,
            "Pascal's Triangle Complete! All 5 rows generated successfully.",
            List.of(), Map.of(), List.of(), Map.of("Status", "Generated 5 Rows"),
            "Matrix", cloneGrid(grid), null, null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateMajorityElement2Steps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 1, 1, 3, 3, 2, 2, 2};
        int cnt1 = 0, cnt2 = 0, el1 = Integer.MIN_VALUE, el2 = Integer.MIN_VALUE;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Majority Element II (> N/3): Extended Moore's Voting algorithm with 2 candidates (el1, el2) and counts (cnt1, cnt2). Input: [1, 1, 1, 3, 3, 2, 2, 2].",
            List.of(), Map.of(), List.of(), Map.of("cnt1", "0", "cnt2", "0"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = 0; i < nums.length; i++) {
            int val = nums[i];
            if (cnt1 == 0 && val != el2) {
                cnt1 = 1; el1 = val;
            } else if (cnt2 == 0 && val != el1) {
                cnt2 = 1; el2 = val;
            } else if (val == el1) cnt1++;
            else if (val == el2) cnt2++;
            else { cnt1--; cnt2--; }

            steps.add(new ExecutionStep(
                stepNum++, 8,
                String.format("Loop i = %d (val %d): Candidates -> el1 = %d (cnt1 = %d), el2 = %d (cnt2 = %d).", i, val, el1, cnt1, el2, cnt2),
                List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "el1", String.valueOf(el1), "cnt1", String.valueOf(cnt1), "el2", String.valueOf(el2), "cnt2", String.valueOf(cnt2)),
                "Array", null, createArrayState(nums, i, -1), null, null
            ));
        }

        List<Integer> ls = new ArrayList<>();
        if (el1 != Integer.MIN_VALUE) ls.add(el1);
        if (el2 != Integer.MIN_VALUE) ls.add(el2);

        steps.add(new ExecutionStep(
            stepNum++, 18,
            String.format("Majority Element II Complete! Elements appearing > N/3 times: %s.", ls.toString()),
            List.of(), Map.of(), List.of(), Map.of("Majority Elements", ls.toString()),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateThreeSumSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{-1, 0, 1, 2, -1, -4};
        Arrays.sort(nums); // [-4, -1, -1, 0, 1, 2]
        int n = nums.length;
        List<List<Integer>> ans = new ArrayList<>();
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "3 Sum: Step 1 - Sort input array -> [-4, -1, -1, 0, 1, 2]. Use outer loop i and 2 pointers j=i+1, k=n-1 to find triplets summing to 0.",
            List.of(), Map.of(), List.of(), Map.of("Sorted Array", Arrays.toString(nums)),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = 0; i < n; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) continue;
            int j = i + 1, k = n - 1;

            while (j < k) {
                int sum = nums[i] + nums[j] + nums[k];
                if (sum == 0) {
                    ans.add(Arrays.asList(nums[i], nums[j], nums[k]));
                    steps.add(new ExecutionStep(
                        stepNum++, 13,
                        String.format("Found 3Sum Triplet! nums[%d]=%d + nums[%d]=%d + nums[%d]=%d = 0! Triplets: %s.", i, nums[i], j, nums[j], k, nums[k], ans.toString()),
                        List.of(), Map.of(), List.of(), Map.of("Triplet", String.format("[%d, %d, %d]", nums[i], nums[j], nums[k])),
                        "Array", null, createArrayState(nums, j, k), null, null
                    ));
                    j++; k--;
                    while (j < k && nums[j] == nums[j - 1]) j++;
                    while (j < k && nums[k] == nums[k + 1]) k--;
                } else if (sum < 0) {
                    steps.add(new ExecutionStep(
                        stepNum++, 11,
                        String.format("3Sum scan: nums[%d]=%d + nums[%d]=%d + nums[%d]=%d = %d < 0. Move j++.", i, nums[i], j, nums[j], k, nums[k], sum),
                        List.of(), Map.of(), List.of(), Map.of("sum", String.valueOf(sum), "j", String.valueOf(j + 1)),
                        "Array", null, createArrayState(nums, j, k), null, null
                    ));
                    j++;
                } else {
                    steps.add(new ExecutionStep(
                        stepNum++, 12,
                        String.format("3Sum scan: nums[%d]=%d + nums[%d]=%d + nums[%d]=%d = %d > 0. Move k--.", i, nums[i], j, nums[j], k, nums[k], sum),
                        List.of(), Map.of(), List.of(), Map.of("sum", String.valueOf(sum), "k", String.valueOf(k - 1)),
                        "Array", null, createArrayState(nums, j, k), null, null
                    ));
                    k--;
                }
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 19,
            String.format("3 Sum Complete! Found unique triplets: %s.", ans.toString()),
            List.of(), Map.of(), List.of(), Map.of("Triplets", ans.toString()),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateFourSumSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 0, -1, 0, -2, 2};
        int target = 0;
        Arrays.sort(nums); // [-2, -1, 0, 0, 1, 2]
        int n = nums.length;
        List<List<Integer>> ans = new ArrayList<>();
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "4 Sum: Sort input array -> [-2, -1, 0, 0, 1, 2]. Use 2 nested loops (i, j) and 2 pointers (k, l) to find quadruplets summing to target 0.",
            List.of(), Map.of(), List.of(), Map.of("Sorted Array", Arrays.toString(nums)),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = 0; i < n; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) continue;
            for (int j = i + 1; j < n; j++) {
                if (j > i + 1 && nums[j] == nums[j - 1]) continue;
                int k = j + 1, l = n - 1;

                while (k < l) {
                    long sum = (long)nums[i] + nums[j] + nums[k] + nums[l];
                    if (sum == target) {
                        ans.add(Arrays.asList(nums[i], nums[j], nums[k], nums[l]));
                        steps.add(new ExecutionStep(
                            stepNum++, 14,
                            String.format("Found 4Sum Quadruplet! (%d, %d, %d, %d) sum = 0! Quadruplets: %s.", nums[i], nums[j], nums[k], nums[l], ans.toString()),
                            List.of(), Map.of(), List.of(), Map.of("Quadruplet", String.format("[%d, %d, %d, %d]", nums[i], nums[j], nums[k], nums[l])),
                            "Array", null, createArrayState(nums, k, l), null, null
                        ));
                        k++; l--;
                        while (k < l && nums[k] == nums[k - 1]) k++;
                        while (k < l && nums[l] == nums[l + 1]) l--;
                    } else if (sum < target) k++;
                    else l--;
                }
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 22,
            String.format("4 Sum Complete! Found unique quadruplets: %s.", ans.toString()),
            List.of(), Map.of(), List.of(), Map.of("Quadruplets", ans.toString()),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateLargestSubarraySum0Steps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = new int[]{15, -2, 2, -8, 1, 7, 10, 23};
        Map<Integer, Integer> map = new HashMap<>();
        int maxi = 0, sum = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Largest Subarray with Sum 0: Track cumulative prefix sum. If prefix sum repeats at index i and j, subarray arr[j+1..i] has sum 0! Input: [15, -2, 2, -8, 1, 7, 10, 23].",
            List.of(), Map.of(), List.of(), Map.of("maxi", "0", "sum", "0"),
            "Array", null, createArrayState(arr, -1, -1), null, null
        ));

        for (int i = 0; i < arr.length; i++) {
            sum += arr[i];
            if (sum == 0) {
                maxi = i + 1;
            } else {
                if (map.containsKey(sum)) {
                    int prevIdx = map.get(sum);
                    int len = i - prevIdx;
                    maxi = Math.max(maxi, len);
                    steps.add(new ExecutionStep(
                        stepNum++, 10,
                        String.format("Loop i = %d (val %d): Prefix sum = %d repeated! Found sum 0 subarray between indices [%d..%d] of length %d. Max length = %d.", i, arr[i], sum, prevIdx + 1, i, len, maxi),
                        List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "sum", String.valueOf(sum), "prevIdx", String.valueOf(prevIdx), "maxi", String.valueOf(maxi)),
                        "Array", null, createRangeArrayState(arr, prevIdx + 1, i), null, null
                    ));
                } else {
                    map.put(sum, i);
                    steps.add(new ExecutionStep(
                        stepNum++, 12,
                        String.format("Loop i = %d (val %d): Prefix sum = %d seen first time. Store map.put(sum=%d, index=%d).", i, arr[i], sum, sum, i),
                        List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "sum", String.valueOf(sum)),
                        "Array", null, createArrayState(arr, i, -1), null, null
                    ));
                }
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 16,
            String.format("Largest Subarray with Sum 0 Complete! Max Subarray Length = %d (Subarray: [-2, 2, -8, 1, 7] at indices [1..5]).", maxi),
            List.of(), Map.of(), List.of(), Map.of("Max Length", String.valueOf(maxi), "Subarray", "[-2, 2, -8, 1, 7]"),
            "Array", null, createRangeArrayState(arr, 1, 5), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateCountSubarraysXorKSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] a = new int[]{4, 2, 2, 6, 4};
        int k = 6;
        int xr = 0, cnt = 0;
        Map<Integer, Integer> map = new HashMap<>();
        map.put(0, 1);
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Count Subarrays with XOR K=6: Cumulative Prefix XOR frequency counting. Initialize map.put(0, 1), xr = 0, cnt = 0. Input: [4, 2, 2, 6, 4].",
            List.of(), Map.of(), List.of(), Map.of("K", "6", "xr", "0", "cnt", "0"),
            "Array", null, createArrayState(a, -1, -1), null, null
        ));

        for (int i = 0; i < a.length; i++) {
            xr = xr ^ a[i];
            int x = xr ^ k;
            int matches = map.getOrDefault(x, 0);
            cnt += matches;
            map.put(xr, map.getOrDefault(xr, 0) + 1);

            steps.add(new ExecutionStep(
                stepNum++, 9,
                String.format("Loop i = %d (val %d): Cumulative XOR xr = %d. Need x = xr ^ K = %d ^ 6 = %d. Found %d matching prefix XORs! Total count = %d.", i, a[i], xr, xr, x, matches, cnt),
                List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "xr", String.valueOf(xr), "x", String.valueOf(x), "matches", String.valueOf(matches), "totalCnt", String.valueOf(cnt)),
                "Array", null, createArrayState(a, i, -1), null, null
            ));
        }

        steps.add(new ExecutionStep(
            stepNum++, 13,
            String.format("Count Subarrays with XOR K=6 Complete! Total Subarrays = %d.", cnt),
            List.of(), Map.of(), List.of(), Map.of("Total Subarrays", String.valueOf(cnt)),
            "Array", null, createArrayState(a, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateMergeIntervalsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[][] intervals = new int[][]{{1, 3}, {2, 6}, {8, 10}, {15, 18}};
        List<int[]> res = new ArrayList<>();
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Merge Overlapping Subintervals: Step 1 - Sort intervals by start time. Input: [[1,3], [2,6], [8,10], [15,18]].",
            List.of(), Map.of(), List.of(), Map.of("N", "4"),
            "Matrix", cloneGrid(intervals), null, null, null
        ));

        for (int[] interval : intervals) {
            if (res.isEmpty() || res.get(res.size() - 1)[1] < interval[0]) {
                res.add(interval);
                steps.add(new ExecutionStep(
                    stepNum++, 8,
                    String.format("Interval [%d, %d]: No overlap with previous interval. Add new interval [%d, %d] to merged result.", interval[0], interval[1], interval[0], interval[1]),
                    List.of(), Map.of(), List.of(), Map.of("Merged Result", formatIntervals(res)),
                    "Matrix", cloneGrid(intervals), null, null, null
                ));
            } else {
                int oldEnd = res.get(res.size() - 1)[1];
                res.get(res.size() - 1)[1] = Math.max(oldEnd, interval[1]);
                steps.add(new ExecutionStep(
                    stepNum++, 11,
                    String.format("Interval [%d, %d] Overlaps! Merge with previous interval: update end to max(%d, %d) = %d. Merged: %s.", interval[0], interval[1], oldEnd, interval[1], res.get(res.size() - 1)[1], formatIntervals(res)),
                    List.of(), Map.of(), List.of(), Map.of("Overlap", "TRUE", "Merged Result", formatIntervals(res)),
                    "Matrix", cloneGrid(intervals), null, null, null
                ));
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 15,
            String.format("Merge Overlapping Intervals Complete! Merged non-overlapping intervals: %s.", formatIntervals(res)),
            List.of(), Map.of(), List.of(), Map.of("Merged Intervals", formatIntervals(res)),
            "Matrix", cloneGrid(intervals), null, null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateMergeTwoSortedArraysSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = new int[]{1, 3, 5, 7, 0, 2, 6, 8};
        int n = 4, m = 4, len = n + m;
        int gap = (len / 2) + (len % 2);
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Merge Two Sorted Arrays Without Extra Space (Gap Method): arr1 = [1, 3, 5, 7], arr2 = [0, 2, 6, 8]. Initialize Gap = ceil((N+M)/2) = 4.",
            List.of(), Map.of(), List.of(), Map.of("gap", "4"),
            "Array", null, createArrayState(arr, -1, -1), null, null
        ));

        while (gap > 0) {
            int left = 0, right = left + gap;
            while (right < len) {
                if (arr[left] > arr[right]) {
                    int temp = arr[left]; arr[left] = arr[right]; arr[right] = temp;
                    steps.add(new ExecutionStep(
                        stepNum++, 12,
                        String.format("Gap = %d: Compare arr[%d] (%d) > arr[%d] (%d): Swap! Array: %s.", gap, left, temp, right, arr[left], Arrays.toString(arr)),
                        List.of(), Map.of(), List.of(), Map.of("gap", String.valueOf(gap), "swap", String.format("%d <-> %d", temp, arr[left])),
                        "Array", null, createArrayState(arr, left, right), null, null
                    ));
                }
                left++; right++;
            }
            if (gap == 1) break;
            gap = (gap / 2) + (gap % 2);
        }

        steps.add(new ExecutionStep(
            stepNum++, 18,
            String.format("Merge Two Sorted Arrays Complete! In-place merged array: %s.", Arrays.toString(arr)),
            List.of(), Map.of(), List.of(), Map.of("Merged Output", Arrays.toString(arr)),
            "Array", null, createArrayState(arr, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateRepeatingMissingSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] a = new int[]{3, 1, 2, 5, 3};
        long n = a.length;
        long SN = (n * (n + 1)) / 2; // 15
        long S2N = (n * (n + 1) * (2 * n + 1)) / 6; // 55
        long S = 0, S2 = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Find Repeating & Missing Number: Use math sum equations S - SN = X - Y and S^2 - S2N = X^2 - Y^2. Expected sum SN = 15, expected sum of squares S2N = 55.",
            List.of(), Map.of(), List.of(), Map.of("SN", "15", "S2N", "55"),
            "Array", null, createArrayState(a, -1, -1), null, null
        ));

        for (int val : a) {
            S += val; S2 += (long)val * val;
        }

        long val1 = S - SN; // -1
        long val2 = S2 - S2N; // -6
        val2 = val2 / val1; // 6
        long x = (val1 + val2) / 2; // 3
        long y = x - val1; // 4

        steps.add(new ExecutionStep(
            stepNum++, 10,
            String.format("Calculate Sums: Array sum S = %d, sum of squares S2 = %d. Equation 1 (X - Y) = %d. Equation 2 (X + Y) = %d.", S, S2, val1, val2),
            List.of(), Map.of(), List.of(), Map.of("S", String.valueOf(S), "S2", String.valueOf(S2), "X-Y", String.valueOf(val1), "X+Y", String.valueOf(val2)),
            "Array", null, createArrayState(a, -1, -1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 15,
            String.format("Repeating & Missing Complete! Repeating Number X = %d, Missing Number Y = %d.", x, y),
            List.of(), Map.of(), List.of(), Map.of("Repeating (X)", String.valueOf(x), "Missing (Y)", String.valueOf(y)),
            "Array", null, createArrayState(a, 0, 4), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateCountInversionsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] a = new int[]{5, 3, 2, 4, 1};
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Count Inversions: Use Divide & Conquer Merge Sort. An inversion is a pair (i, j) where i < j and arr[i] > arr[j]. Input: [5, 3, 2, 4, 1].",
            List.of(), Map.of(), List.of(), Map.of("N", "5"),
            "Array", null, createArrayState(a, -1, -1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 10,
            "Merge Phase 1: Left [5] vs Right [3] -> Inversion (5, 3). Inversion count += 1.",
            List.of("mergeAndCount(0, 1)"), Map.of(), List.of(), Map.of("Inversions", "1"),
            "Array", null, createArrayState(new int[]{3, 5, 2, 4, 1}, 0, 1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 15,
            "Merge Phase 2: Sorted Left [3, 5] vs Right [2] -> Inversions (3, 2) and (5, 2). Inversion count += 2.",
            List.of("mergeAndCount(0, 2)"), Map.of(), List.of(), Map.of("Inversions", "3"),
            "Array", null, createArrayState(new int[]{2, 3, 5, 4, 1}, 0, 2), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 20,
            "Merge Phase 3: Final Merge of Left [2, 3, 5] and Right [1, 4] -> Total Inversions count = 8.",
            List.of("mergeAndCount(0, 4)"), Map.of(), List.of(), Map.of("Total Inversions", "8"),
            "Array", null, createArrayState(new int[]{1, 2, 3, 4, 5}, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateReversePairsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 3, 2, 3, 1};
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Reverse Pairs: Count pairs (i, j) where i < j and arr[i] > 2 * arr[j] using Merge Sort Divide & Conquer. Input: [1, 3, 2, 3, 1].",
            List.of(), Map.of(), List.of(), Map.of("N", "5"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 10,
            "Pair Count 1: Check arr[1]=3 > 2 * arr[4]=1 (3 > 2) -> Valid Reverse Pair (3, 1)!",
            List.of("countPairs(0, 4)"), Map.of(), List.of(), Map.of("Reverse Pairs", "1"),
            "Array", null, createArrayState(nums, 1, 4), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 12,
            "Pair Count 2: Check arr[3]=3 > 2 * arr[4]=1 (3 > 2) -> Valid Reverse Pair (3, 1)!",
            List.of("countPairs(0, 4)"), Map.of(), List.of(), Map.of("Reverse Pairs", "2"),
            "Array", null, createArrayState(nums, 3, 4), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 16,
            "Reverse Pairs Complete! Total Reverse Pairs (arr[i] > 2 * arr[j]) = 2.",
            List.of(), Map.of(), List.of(), Map.of("Total Reverse Pairs", "2"),
            "Array", null, createArrayState(new int[]{1, 1, 2, 3, 3}, -1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateMaxProductSubarraySteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{2, 3, -2, 4};
        int n = nums.length;
        double pre = 1, suff = 1, maxi = Integer.MIN_VALUE;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Maximum Product Subarray: Calculate prefix product (left-to-right) and suffix product (right-to-left) simultaneously. Input: [2, 3, -2, 4].",
            List.of(), Map.of(), List.of(), Map.of("pre", "1", "suff", "1", "maxi", "-INF"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        for (int i = 0; i < n; i++) {
            if (pre == 0) pre = 1;
            if (suff == 0) suff = 1;
            pre *= nums[i];
            suff *= nums[n - 1 - i];
            maxi = Math.max(maxi, Math.max(pre, suff));

            steps.add(new ExecutionStep(
                stepNum++, 9,
                String.format("Step i = %d: Left val %d (prefix prod = %.0f), Right val %d (suffix prod = %.0f). Max product so far = %.0f.", i, nums[i], pre, nums[n - 1 - i], suff, maxi),
                List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "pre", String.valueOf(pre), "suff", String.valueOf(suff), "maxi", String.valueOf(maxi)),
                "Array", null, createArrayState(nums, i, n - 1 - i), null, null
            ));
        }

        steps.add(new ExecutionStep(
            stepNum++, 13,
            String.format("Max Product Subarray Complete! Maximum Product = %.0f (Subarray: [2, 3]).", maxi),
            List.of(), Map.of(), List.of(), Map.of("Max Product", String.valueOf((int)maxi), "Subarray", "[2, 3]"),
            "Array", null, createRangeArrayState(nums, 0, 1), null, null
        ));

        return steps;
    }

    // Helper builders
    private List<ArrayElement> createTwoSumArray() { return createArrayState(new int[]{2, 7, 11, 15}, -1, -1); }
    private List<ArrayElement> createSort012Array() { return createArrayState(new int[]{2, 0, 2, 1, 1, 0}, -1, -1); }
    private List<ArrayElement> createMajorityArray() { return createArrayState(new int[]{2, 2, 1, 1, 1, 2, 2}, -1, -1); }
    private List<ArrayElement> createKadaneArray() { return createArrayState(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}, -1, -1); }
    private List<ArrayElement> createStockArray() { return createArrayState(new int[]{7, 1, 5, 3, 6, 4}, -1, -1); }
    private List<ArrayElement> createRearrangeArray() { return createArrayState(new int[]{3, 1, -2, -5, 2, -4}, -1, -1); }
    private List<ArrayElement> createNextPermArray() { return createArrayState(new int[]{1, 2, 3, 5, 4, 2}, -1, -1); }
    private List<ArrayElement> createLeadersArray() { return createArrayState(new int[]{16, 17, 4, 3, 5, 2}, -1, -1); }
    private List<ArrayElement> createConsecutiveArray() { return createArrayState(new int[]{100, 4, 200, 1, 3, 2}, -1, -1); }
    private List<ArrayElement> createCountSubarraysArray() { return createArrayState(new int[]{1, 1, 1}, -1, -1); }
    private List<ArrayElement> createMajority2Array() { return createArrayState(new int[]{1, 1, 1, 3, 3, 2, 2, 2}, -1, -1); }
    private List<ArrayElement> createThreeSumArray() { return createArrayState(new int[]{-1, 0, 1, 2, -1, -4}, -1, -1); }
    private List<ArrayElement> createFourSumArray() { return createArrayState(new int[]{1, 0, -1, 0, -2, 2}, -1, -1); }
    private List<ArrayElement> createLargestSum0Array() { return createArrayState(new int[]{15, -2, 2, -8, 1, 7, 10, 23}, -1, -1); }
    private List<ArrayElement> createSubarrayXorArray() { return createArrayState(new int[]{4, 2, 2, 6, 4}, -1, -1); }
    private List<ArrayElement> createTwoSortedArrays() { return createArrayState(new int[]{1, 3, 5, 7, 0, 2, 6, 8}, -1, -1); }
    private List<ArrayElement> createRepeatingMissingArray() { return createArrayState(new int[]{3, 1, 2, 5, 3}, -1, -1); }
    private List<ArrayElement> createInversionsArray() { return createArrayState(new int[]{5, 3, 2, 4, 1}, -1, -1); }
    private List<ArrayElement> createReversePairsArray() { return createArrayState(new int[]{1, 3, 2, 3, 1}, -1, -1); }
    private List<ArrayElement> createMaxProductArray() { return createArrayState(new int[]{2, 3, -2, 4}, -1, -1); }

    private int[][] createDefaultMatrix() {
        return new int[][]{
            {1, 1, 1, 1},
            {1, 0, 1, 1},
            {1, 1, 1, 1}
        };
    }

    private int[][] createSquareMatrix() {
        return new int[][]{
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
    }

    private int[][] createPascalsTriangleGrid() {
        return new int[][]{
            {1, 0, 0, 0, 0},
            {1, 1, 0, 0, 0},
            {1, 2, 1, 0, 0},
            {1, 3, 3, 1, 0},
            {1, 4, 6, 4, 1}
        };
    }

    private int[][] createIntervalsGrid() {
        return new int[][]{
            {1, 3},
            {2, 6},
            {8, 10},
            {15, 18}
        };
    }

    private String formatIntervals(List<int[]> res) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < res.size(); i++) {
            sb.append("[").append(res.get(i)[0]).append(",").append(res.get(i)[1]).append("]");
            if (i < res.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private int[][] cloneGrid(int[][] grid) {
        int[][] clone = new int[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            clone[i] = grid[i].clone();
        }
        return clone;
    }

    private List<ArrayElement> createArrayState(int[] vals, int activeIdx1, int activeIdx2) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            String state = (i == activeIdx1 || i == activeIdx2) ? "active" : "default";
            list.add(new ArrayElement(i, vals[i], state));
        }
        return list;
    }

    private List<ArrayElement> createRangeArrayState(int[] vals, int start, int end) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            String state = (i >= start && i <= end) ? "sorted" : "default";
            list.add(new ArrayElement(i, vals[i], state));
        }
        return list;
    }

    private ExecutionStep createStep(int stepNum, int line, String desc, List<ArrayElement> arrayState, Map<String, String> vars) {
        return new ExecutionStep(
            stepNum, line, desc,
            List.of(), Map.of(), List.of(), vars,
            "Array", null, arrayState, null, null
        );
    }

    private List<ArrayElement> createSearchArrayState(int[] arr, int currentIdx, int maxIdx) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            String state = "default";
            if (i == maxIdx) {
                state = "pivot";
            }
            if (i == currentIdx && currentIdx != maxIdx) {
                state = "comparing";
            } else if (i != maxIdx && currentIdx != -1 && i < currentIdx) {
                state = "visited";
            }
            list.add(new ArrayElement(i, arr[i], state));
        }
        return list;
    }

    private List<ArrayElement> createSecondLargestArrayState(int[] arr, int currentIdx, int largestIdx, int secondLargestIdx) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            String state = "default";
            if (i == largestIdx) {
                state = "pivot"; // Purple for Largest
            } else if (i == secondLargestIdx) {
                state = "sorted"; // Green for Second Largest
            }
            if (i == currentIdx && currentIdx != largestIdx && currentIdx != secondLargestIdx) {
                state = "comparing"; // Orange for Current (i)
            } else if (currentIdx != -1 && i < currentIdx && i != largestIdx && i != secondLargestIdx) {
                state = "visited"; // Slate for Visited
            }
            list.add(new ArrayElement(i, arr[i], state));
        }
        return list;
    }

    // ==================== 14 EASY ARRAY STEP GENERATORS ====================

    private List<ExecutionStep> generateLargestElementSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = {12, 35, 1, 10, 34, 1};
        int stepNum = 1;
        int maxIdx = 0;
        int max = arr[0];

        steps.add(createStep(stepNum++, 3, "Initialize max = arr[0] = " + arr[0] + " at index 0.", createSearchArrayState(arr, 0, maxIdx), Map.of("max", String.valueOf(arr[0]), "maxIndex", "0", "i", "0")));

        for (int i = 1; i < arr.length; i++) {
            steps.add(createStep(stepNum++, 4, "Comparing arr[" + i + "] (" + arr[i] + ") with current max (" + max + " at index " + maxIdx + ")", createSearchArrayState(arr, i, maxIdx), Map.of("max", String.valueOf(max), "maxIndex", String.valueOf(maxIdx), "i", String.valueOf(i), "arr[i]", String.valueOf(arr[i]))));
            if (arr[i] > max) {
                max = arr[i];
                maxIdx = i;
                steps.add(createStep(stepNum++, 5, "Found larger element! Update max = " + max + " at index " + maxIdx, createSearchArrayState(arr, i, maxIdx), Map.of("max", String.valueOf(max), "maxIndex", String.valueOf(maxIdx), "i", String.valueOf(i))));
            }
        }
        steps.add(createStep(stepNum++, 8, "Completed linear scan. Largest element in array is " + max + " at index " + maxIdx, createSearchArrayState(arr, -1, maxIdx), Map.of("max", String.valueOf(max), "maxIndex", String.valueOf(maxIdx))));
        return steps;
    }

    private List<ExecutionStep> generateSecondLargestElementSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = {12, 35, 1, 10, 34, 1};
        int largestIdx = 0;
        int secondLargestIdx = -1;
        int largest = arr[0];
        int secondLargest = -1;
        int stepNum = 1;

        steps.add(createStep(stepNum++, 3, "Initialize largest = arr[0] = " + largest + " (idx 0), secondLargest = -1", createSecondLargestArrayState(arr, 0, largestIdx, secondLargestIdx), Map.of("largest", String.valueOf(largest), "largestIdx", "0", "secondLargest", "-1", "secondLargestIdx", "-1", "i", "0")));

        for (int i = 1; i < arr.length; i++) {
            steps.add(createStep(stepNum++, 5, "Inspect arr[" + i + "] = " + arr[i] + ". Compare with largest (" + largest + ") & secondLargest (" + (secondLargest == -1 ? "NONE" : secondLargest) + ").", createSecondLargestArrayState(arr, i, largestIdx, secondLargestIdx), Map.of("largest", String.valueOf(largest), "largestIdx", String.valueOf(largestIdx), "secondLargest", String.valueOf(secondLargest), "secondLargestIdx", String.valueOf(secondLargestIdx), "i", String.valueOf(i), "arr[i]", String.valueOf(arr[i]))));
            if (arr[i] > largest) {
                secondLargest = largest;
                secondLargestIdx = largestIdx;
                largest = arr[i];
                largestIdx = i;
                steps.add(createStep(stepNum++, 7, "arr[" + i + "] (" + arr[i] + ") > largest! Shift largest to secondLargest (" + secondLargest + " at idx " + secondLargestIdx + "), new largest = " + largest + " at idx " + largestIdx, createSecondLargestArrayState(arr, i, largestIdx, secondLargestIdx), Map.of("largest", String.valueOf(largest), "largestIdx", String.valueOf(largestIdx), "secondLargest", String.valueOf(secondLargest), "secondLargestIdx", String.valueOf(secondLargestIdx), "i", String.valueOf(i))));
            } else if (arr[i] < largest && arr[i] > secondLargest) {
                secondLargest = arr[i];
                secondLargestIdx = i;
                steps.add(createStep(stepNum++, 9, "arr[" + i + "] (" + arr[i] + ") is between secondLargest & largest! New secondLargest = " + secondLargest + " at idx " + secondLargestIdx, createSecondLargestArrayState(arr, i, largestIdx, secondLargestIdx), Map.of("largest", String.valueOf(largest), "largestIdx", String.valueOf(largestIdx), "secondLargest", String.valueOf(secondLargest), "secondLargestIdx", String.valueOf(secondLargestIdx), "i", String.valueOf(i))));
            }
        }
        steps.add(createStep(stepNum++, 12, "Completed scan! Final Largest = " + largest + " (idx " + largestIdx + "), Second Largest = " + secondLargest + " (idx " + secondLargestIdx + ")", createSecondLargestArrayState(arr, -1, largestIdx, secondLargestIdx), Map.of("largest", String.valueOf(largest), "largestIdx", String.valueOf(largestIdx), "secondLargest", String.valueOf(secondLargest), "secondLargestIdx", String.valueOf(secondLargestIdx))));
        return steps;
    }

    private List<ExecutionStep> generateCheckSortedIISteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = {3, 4, 5, 1, 2};
        int n = nums.length;
        int count = 0;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Initialize drop count = 0 for array length N = " + n, createArrayState(nums, -1, -1), Map.of("count", "0", "n", String.valueOf(n))));

        for (int i = 0; i < n; i++) {
            int nextIdx = (i + 1) % n;
            steps.add(createStep(stepNum++, 5, "Comparing circular pair nums[" + i + "] (" + nums[i] + ") > nums[" + nextIdx + "] (" + nums[nextIdx] + ")", createArrayState(nums, i, nextIdx), Map.of("i", String.valueOf(i), "nextIdx", String.valueOf(nextIdx), "nums[i]", String.valueOf(nums[i]), "nums[nextIdx]", String.valueOf(nums[nextIdx]), "count", String.valueOf(count))));
            if (nums[i] > nums[nextIdx]) {
                count++;
                steps.add(createStep(stepNum++, 6, "Drop detected! Incremented count = " + count, createArrayState(nums, i, nextIdx), Map.of("count", String.valueOf(count), "i", String.valueOf(i))));
            }
        }
        boolean isSortedRotated = count <= 1;
        steps.add(createStep(stepNum++, 9, "Total drop count = " + count + " (<= 1). Result: " + isSortedRotated, createArrayState(nums, -1, -1), Map.of("count", String.valueOf(count), "isSortedRotated", String.valueOf(isSortedRotated))));
        return steps;
    }

    private List<ExecutionStep> generateRemoveDuplicatesSortedSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = {0, 0, 1, 1, 1, 2, 2, 3, 3, 4};
        int i = 0;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Initialize unique end pointer i = 0 at nums[0] = " + nums[0], createArrayState(nums, 0, -1), Map.of("i", "0", "j", "1")));

        for (int j = 1; j < nums.length; j++) {
            steps.add(createStep(stepNum++, 5, "Comparing explorer nums[j=" + j + "] (" + nums[j] + ") with unique nums[i=" + i + "] (" + nums[i] + ")", createArrayState(nums, i, j), Map.of("i", String.valueOf(i), "j", String.valueOf(j), "nums[i]", String.valueOf(nums[i]), "nums[j]", String.valueOf(nums[j]))));
            if (nums[j] != nums[i]) {
                i++;
                nums[i] = nums[j];
                steps.add(createStep(stepNum++, 7, "New unique element " + nums[j] + " found! Placed at nums[i=" + i + "]", createArrayState(nums, i, j), Map.of("i", String.valueOf(i), "j", String.valueOf(j), "placedValue", String.valueOf(nums[i]))));
            }
        }
        int uniqueCount = i + 1;
        steps.add(createStep(stepNum++, 10, "Deduplication complete. Number of unique elements = " + uniqueCount, createArrayState(nums, 0, i), Map.of("uniqueCount", String.valueOf(uniqueCount))));
        return steps;
    }

    private List<ExecutionStep> generateLeftRotateOneSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = {1, 2, 3, 4, 5};
        int temp = arr[0];
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Store first element temp = arr[0] = " + temp, createArrayState(arr, 0, -1), Map.of("temp", String.valueOf(temp))));

        for (int i = 0; i < arr.length - 1; i++) {
            arr[i] = arr[i + 1];
            steps.add(createStep(stepNum++, 5, "Shift element left: arr[" + i + "] = arr[" + (i + 1) + "] (" + arr[i] + ")", createArrayState(arr, i, i + 1), Map.of("i", String.valueOf(i), "temp", String.valueOf(temp))));
        }
        arr[arr.length - 1] = temp;
        steps.add(createStep(stepNum++, 7, "Place saved temp (" + temp + ") at last index arr[" + (arr.length - 1) + "]", createArrayState(arr, arr.length - 1, -1), Map.of("temp", String.valueOf(temp))));
        return steps;
    }

    private List<ExecutionStep> generateLeftRotateKSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = {1, 2, 3, 4, 5, 6, 7};
        int n = nums.length;
        int k = 3 % n;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 4, "Normalize k = 3 % " + n + " = " + k, createArrayState(nums, -1, -1), Map.of("k", String.valueOf(k), "n", String.valueOf(n))));

        // Step 1: Reverse 0..k-1
        reverseArrayRange(nums, 0, k - 1);
        steps.add(createStep(stepNum++, 5, "Step 1: Reverse first K elements (indices 0.." + (k - 1) + ")", createRangeArrayState(nums, 0, k - 1), Map.of("k", String.valueOf(k))));

        // Step 2: Reverse k..n-1
        reverseArrayRange(nums, k, n - 1);
        steps.add(createStep(stepNum++, 6, "Step 2: Reverse remaining elements (indices " + k + ".." + (n - 1) + ")", createRangeArrayState(nums, k, n - 1), Map.of("k", String.valueOf(k))));

        // Step 3: Reverse 0..n-1
        reverseArrayRange(nums, 0, n - 1);
        steps.add(createStep(stepNum++, 7, "Step 3: Reverse entire array (indices 0.." + (n - 1) + ")", createRangeArrayState(nums, 0, n - 1), Map.of("k", String.valueOf(k))));
        return steps;
    }

    private void reverseArrayRange(int[] arr, int start, int end) {
        while (start < end) {
            int temp = arr[start];
            arr[start] = arr[end];
            arr[end] = temp;
            start++;
            end--;
        }
    }

    private List<ExecutionStep> generateMoveZerosEndSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = {0, 1, 0, 3, 12};
        int j = -1;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Find first zero index j", createArrayState(nums, -1, -1), Map.of("j", "-1")));

        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == 0) {
                j = i;
                break;
            }
        }
        steps.add(createStep(stepNum++, 5, "First zero found at index j = " + j, createArrayState(nums, j, -1), Map.of("j", String.valueOf(j))));

        for (int i = j + 1; i < nums.length; i++) {
            steps.add(createStep(stepNum++, 7, "Inspecting nums[i=" + i + "] = " + nums[i], createArrayState(nums, j, i), Map.of("j", String.valueOf(j), "i", String.valueOf(i), "nums[i]", String.valueOf(nums[i]))));
            if (nums[i] != 0) {
                int temp = nums[i]; nums[i] = nums[j]; nums[j] = temp;
                steps.add(createStep(stepNum++, 9, "Swapped non-zero nums[" + i + "] with zero at nums[" + j + "]", createArrayState(nums, j, i), Map.of("j", String.valueOf(j), "i", String.valueOf(i))));
                j++;
            }
        }
        steps.add(createStep(stepNum++, 12, "Moved all zeroes to the end of the array successfully!", createArrayState(nums, -1, -1), Map.of("j", String.valueOf(j))));
        return steps;
    }

    private List<ArrayElement> createLinearSearchArrayState(int[] arr, int currentIdx, int foundIdx) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            String state = "default";
            if (i == foundIdx) {
                state = "sorted"; // Emerald Green for Target Found!
            } else if (i == currentIdx) {
                state = "comparing"; // Amber Orange for Current (i)
            } else if (currentIdx != -1 && i < currentIdx) {
                state = "visited"; // Slate Muted for Visited
            }
            list.add(new ArrayElement(i, arr[i], state));
        }
        return list;
    }

    private List<ExecutionStep> generateLinearSearchSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = {1, 2, 3, 4, 5};
        int num = 4;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 2, "Start linear search for target X = " + num + " in array [1, 2, 3, 4, 5].", createLinearSearchArrayState(arr, -1, -1), Map.of("target", String.valueOf(num))));

        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == num) {
                steps.add(createStep(stepNum++, 4, "Comparing arr[" + i + "] (" + arr[i] + ") == target (" + num + ") -> Target element " + num + " MATCH FOUND at index " + i + "!", createLinearSearchArrayState(arr, i, i), Map.of("foundIndex", String.valueOf(i), "target", String.valueOf(num), "i", String.valueOf(i))));
                return steps;
            } else {
                steps.add(createStep(stepNum++, 4, "Comparing arr[" + i + "] (" + arr[i] + ") != target (" + num + "). Continue search...", createLinearSearchArrayState(arr, i, -1), Map.of("i", String.valueOf(i), "arr[i]", String.valueOf(arr[i]), "target", String.valueOf(num))));
            }
        }
        steps.add(createStep(stepNum++, 6, "Target element " + num + " not found in array. Return -1.", createLinearSearchArrayState(arr, -1, -1), Map.of("foundIndex", "-1")));
        return steps;
    }

    private List<ExecutionStep> generateUnionSortedArraysSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] a = {1, 1, 2, 3, 4, 5};
        int[] b = {2, 3, 4, 4, 5, 6};
        List<Integer> union = new ArrayList<>();
        int i = 0, j = 0;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 4, "Initialize pointers i = 0 (arr A) and j = 0 (arr B)", createArrayState(a, 0, -1), Map.of("i", "0", "j", "0", "union", union.toString())));

        while (i < a.length && j < b.length) {
            steps.add(createStep(stepNum++, 6, "Comparing a[i=" + i + "] (" + a[i] + ") and b[j=" + j + "] (" + b[j] + ")", createArrayState(a, i, -1), Map.of("i", String.valueOf(i), "j", String.valueOf(j), "a[i]", String.valueOf(a[i]), "b[j]", String.valueOf(b[j]), "union", union.toString())));
            if (a[i] <= b[j]) {
                if (union.isEmpty() || union.get(union.size() - 1) != a[i]) {
                    union.add(a[i]);
                    steps.add(createStep(stepNum++, 8, "Added unique element " + a[i] + " from arr A to union", createArrayState(a, i, -1), Map.of("union", union.toString(), "i", String.valueOf(i))));
                }
                i++;
            } else {
                if (union.isEmpty() || union.get(union.size() - 1) != b[j]) {
                    union.add(b[j]);
                    steps.add(createStep(stepNum++, 11, "Added unique element " + b[j] + " from arr B to union", createArrayState(b, j, -1), Map.of("union", union.toString(), "j", String.valueOf(j))));
                }
                j++;
            }
        }
        while (i < a.length) {
            if (union.isEmpty() || union.get(union.size() - 1) != a[i]) union.add(a[i]);
            i++;
        }
        while (j < b.length) {
            if (union.isEmpty() || union.get(union.size() - 1) != b[j]) union.add(b[j]);
            j++;
        }
        steps.add(createStep(stepNum++, 20, "Union merge complete! Resulting Union = " + union, createArrayState(a, -1, -1), Map.of("finalUnion", union.toString())));
        return steps;
    }

    private List<ExecutionStep> generateFindMissingNumberSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = {3, 0, 1};
        int n = nums.length;
        int expectedSum = n * (n + 1) / 2;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 4, "N = " + n + ". Expected sum for [0.." + n + "] = " + n + "*(" + (n + 1) + ")/2 = " + expectedSum, createArrayState(nums, -1, -1), Map.of("N", String.valueOf(n), "expectedSum", String.valueOf(expectedSum))));

        int actualSum = 0;
        for (int i = 0; i < nums.length; i++) {
            actualSum += nums[i];
            steps.add(createStep(stepNum++, 7, "Add nums[" + i + "] (" + nums[i] + ") to actualSum -> " + actualSum, createArrayState(nums, i, -1), Map.of("actualSum", String.valueOf(actualSum), "i", String.valueOf(i))));
        }
        int missing = expectedSum - actualSum;
        steps.add(createStep(stepNum++, 9, "Missing Number = expectedSum (" + expectedSum + ") - actualSum (" + actualSum + ") = " + missing, createArrayState(nums, -1, -1), Map.of("missing", String.valueOf(missing))));
        return steps;
    }

    private List<ExecutionStep> generateMaxConsecutiveOnesSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = {1, 1, 0, 1, 1, 1};
        int maxCount = 0, count = 0;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Initialize count = 0, maxCount = 0", createArrayState(nums, -1, -1), Map.of("count", "0", "maxCount", "0")));

        for (int i = 0; i < nums.length; i++) {
            steps.add(createStep(stepNum++, 5, "Inspecting nums[" + i + "] = " + nums[i], createArrayState(nums, i, -1), Map.of("i", String.valueOf(i), "val", String.valueOf(nums[i]), "count", String.valueOf(count), "maxCount", String.valueOf(maxCount))));
            if (nums[i] == 1) {
                count++;
                maxCount = Math.max(maxCount, count);
                steps.add(createStep(stepNum++, 7, "Element is 1! Incremented count = " + count + ", maxCount = " + maxCount, createArrayState(nums, i, -1), Map.of("count", String.valueOf(count), "maxCount", String.valueOf(maxCount))));
            } else {
                count = 0;
                steps.add(createStep(stepNum++, 9, "Element is 0! Reset current streak count = 0", createArrayState(nums, i, -1), Map.of("count", "0", "maxCount", String.valueOf(maxCount))));
            }
        }
        steps.add(createStep(stepNum++, 12, "Scan complete! Maximum consecutive ones = " + maxCount, createArrayState(nums, -1, -1), Map.of("maxConsecutiveOnes", String.valueOf(maxCount))));
        return steps;
    }

    private List<ExecutionStep> generateSingleNumberSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = {4, 1, 2, 1, 2};
        int xorSum = 0;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 3, "Initialize xorSum = 0", createArrayState(nums, -1, -1), Map.of("xorSum", "0")));

        for (int i = 0; i < nums.length; i++) {
            int prevXor = xorSum;
            xorSum ^= nums[i];
            steps.add(createStep(stepNum++, 5, "xorSum = " + prevXor + " ^ nums[" + i + "] (" + nums[i] + ") = " + xorSum, createArrayState(nums, i, -1), Map.of("i", String.valueOf(i), "num", String.valueOf(nums[i]), "xorSum", String.valueOf(xorSum))));
        }
        steps.add(createStep(stepNum++, 7, "Bitwise XOR cancellation complete. Single number that appears once = " + xorSum, createArrayState(nums, -1, -1), Map.of("singleNumber", String.valueOf(xorSum))));
        return steps;
    }

    private List<ExecutionStep> generateLongestSubarraySumKPositivesSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] a = {1, 2, 3, 1, 1, 1, 1, 4, 2, 3};
        long k = 3;
        int left = 0, right = 0;
        long sum = a[0];
        int maxLen = 0;
        int n = a.length;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 4, "Initialize sliding window left = 0, right = 0, sum = " + sum + ", K = " + k, createRangeArrayState(a, left, right), Map.of("left", "0", "right", "0", "sum", String.valueOf(sum), "maxLen", "0", "K", String.valueOf(k))));

        while (right < n) {
            while (left <= right && sum > k) {
                sum -= a[left];
                left++;
                steps.add(createStep(stepNum++, 8, "Window sum (" + sum + ") > K (" + k + ")! Shrink window left -> " + left, createRangeArrayState(a, left, right), Map.of("left", String.valueOf(left), "right", String.valueOf(right), "sum", String.valueOf(sum), "maxLen", String.valueOf(maxLen))));
            }
            if (sum == k) {
                maxLen = Math.max(maxLen, right - left + 1);
                steps.add(createStep(stepNum++, 11, "Window sum == K (" + k + ")! Subarray [" + left + ".." + right + "] length = " + (right - left + 1) + ", maxLen = " + maxLen, createRangeArrayState(a, left, right), Map.of("left", String.valueOf(left), "right", String.valueOf(right), "sum", String.valueOf(sum), "maxLen", String.valueOf(maxLen))));
            }
            right++;
            if (right < n) {
                sum += a[right];
                steps.add(createStep(stepNum++, 14, "Expand window right -> " + right + ", updated sum = " + sum, createRangeArrayState(a, left, right), Map.of("left", String.valueOf(left), "right", String.valueOf(right), "sum", String.valueOf(sum), "maxLen", String.valueOf(maxLen))));
            }
        }
        steps.add(createStep(stepNum++, 17, "Sliding window scan complete! Longest subarray length with sum K=" + k + " is " + maxLen, createArrayState(a, -1, -1), Map.of("maxLen", String.valueOf(maxLen))));
        return steps;
    }

    private List<ExecutionStep> generateLongestSubarraySumKSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] a = {-1, 2, 3, -2, 1};
        long k = 3;
        Map<Long, Integer> preSumMap = new HashMap<>();
        long sum = 0;
        int maxLen = 0;
        int stepNum = 1;
        steps.add(createStep(stepNum++, 4, "Initialize prefix sum = 0, maxLen = 0, preSumMap", createArrayState(a, -1, -1), Map.of("sum", "0", "maxLen", "0", "K", String.valueOf(k))));

        for (int i = 0; i < a.length; i++) {
            sum += a[i];
            steps.add(createStep(stepNum++, 7, "Add a[" + i + "] (" + a[i] + ") -> prefix sum = " + sum, createArrayState(a, i, -1), Map.of("i", String.valueOf(i), "val", String.valueOf(a[i]), "sum", String.valueOf(sum), "maxLen", String.valueOf(maxLen))));
            if (sum == k) {
                maxLen = Math.max(maxLen, i + 1);
                steps.add(createStep(stepNum++, 8, "Prefix sum == K (" + k + ")! Subarray [0.." + i + "] length = " + (i + 1) + ", maxLen = " + maxLen, createRangeArrayState(a, 0, i), Map.of("maxLen", String.valueOf(maxLen))));
            }
            long rem = sum - k;
            if (preSumMap.containsKey(rem)) {
                int len = i - preSumMap.get(rem);
                maxLen = Math.max(maxLen, len);
                steps.add(createStep(stepNum++, 11, "Found remainder rem = " + rem + " in map at index " + preSumMap.get(rem) + "! Subarray length = " + len + ", maxLen = " + maxLen, createRangeArrayState(a, preSumMap.get(rem) + 1, i), Map.of("rem", String.valueOf(rem), "len", String.valueOf(len), "maxLen", String.valueOf(maxLen))));
            }
            if (!preSumMap.containsKey(sum)) {
                preSumMap.put(sum, i);
            }
        }
        steps.add(createStep(stepNum++, 17, "Prefix sum scan complete! Longest subarray length with sum K=" + k + " is " + maxLen, createArrayState(a, -1, -1), Map.of("maxLen", String.valueOf(maxLen))));
        return steps;
    }
}
