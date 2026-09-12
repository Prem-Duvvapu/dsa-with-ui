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
            // These fifteen have real tracers (tracer/impl). Refuse rather than let
            // default: serve another array problem's steps under these ids. The
            // default: stays until PROMPT D; other ids here still rely on it.
            case "largest-element":
            case "max-consecutive-ones":
            case "move-zeros-end":
            case "find-missing-number":
            case "stock-buy-sell":
            case "second-largest-element":
            case "check-sorted-ii":
            case "remove-duplicates-sorted":
            case "left-rotate-one":
            case "linear-search":
            case "left-rotate-k":
            case "single-number":
            case "majority-element":
            case "leaders-in-array":
            case "longest-subarray-sum-k-positives":
            // count-inversions and reverse-pairs have real tracers (tracer/impl). Refuse
            // rather than let default: serve largest-element's steps under these ids.
            case "count-inversions":
            case "reverse-pairs":
            // sort-0-1-2 and next-permutation have real tracers (tracer/impl). Refuse
            // rather than let default: serve largest-element's steps under these ids.
            case "sort-0-1-2":
            case "next-permutation":
            // repeating-missing-number and merge-two-sorted-arrays have real tracers
            // (tracer/impl) now. Their generators were hardcoded narrations - a real
            // algorithm always run on one baked-in array, ignoring caller input - and
            // are gone; refusing loudly beats serving the same canned trace forever.
            case "repeating-missing-number":
            case "merge-two-sorted-arrays":
            // three-sum and four-sum have real tracers (tracer/impl) now. Same
            // hardcoded-narration pattern as the pair above - gone rather than kept
            // as dead weight that could accidentally get called again.
            case "three-sum":
            case "four-sum":
                throw new LegacyTraceRetiredException(problemId);
            // All 15 remaining array problems now have real tracers (tracer/impl).
            // two-sum and kadane-algo already had tracers — now explicitly retired too.
            case "union-sorted-arrays":
            case "longest-subarray-sum-k":
            case "two-sum":
            case "kadane-algo":
            case "print-max-subarray":
            case "rearrange-by-sign":
            case "longest-consecutive-sequence":
            case "set-matrix-zeroes":
            case "rotate-matrix-90":
            case "spiral-matrix":
            case "count-subarrays-given-sum":
            case "pascals-triangle":
            case "majority-element-ii":
            case "largest-subarray-sum-0":
            case "count-subarrays-xor-k":
            case "merge-intervals":
            case "max-product-subarray":
                throw new LegacyTraceRetiredException(problemId);
            default:
                throw new LegacyTraceRetiredException(problemId);
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
            "Array"
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

    // ==================== 14 EASY ARRAY STEP GENERATORS ====================

}
