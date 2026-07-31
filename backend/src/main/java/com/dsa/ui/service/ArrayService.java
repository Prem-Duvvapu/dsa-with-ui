package com.dsa.ui.service;

import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ArrayService {

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
            case "two-sum": return generateTwoSumSteps();
            case "sort-0-1-2": return generateSort012Steps();
            case "majority-element": return generateMajorityElementSteps();
            case "kadane-algo": return generateKadaneSteps();
            case "stock-buy-sell": return generateStockSteps();
            default: return generateTwoSumSteps();
        }
    }

    private void initProblems() {
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

        // 2. Sort 0s, 1s, 2s (Dutch National Flag)
        problems.put("sort-0-1-2", new ProblemDetail(
            "sort-0-1-2", "Sort an Array of 0s, 1s and 2s", "Arrays - Medium", "Arrays", "Medium",
            "Sort an array of 0s, 1s, and 2s in single pass O(N) time without using built-in sort function.",
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

        // 3. Majority Element (Moore's Voting Algorithm)
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

        // 5. Best Time to Buy and Sell Stock
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
    }

    // Step Generators
    private List<ExecutionStep> generateTwoSumSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{2, 7, 11, 15};
        steps.add(new ExecutionStep(1, 4, "Array: [2, 7, 11, 15], Target = 9. Start HashMap scan...", List.of(), Map.of(), List.of(), Map.of("target", "9"), "Array", null, createArrayState(nums, -1, -1), null, null));
        steps.add(new ExecutionStep(2, 5, "i = 0 (val 2): complement = 9 - 2 = 7. HashMap does not contain 7. Add map(2 -> 0)", List.of(), Map.of(), List.of(), Map.of("map", "{2:0}"), "Array", null, createArrayState(nums, 0, -1), null, null));
        steps.add(new ExecutionStep(3, 6, "i = 1 (val 7): complement = 9 - 7 = 2. HashMap CONTAINS 2 at index 0! MATCH FOUND!", List.of(), Map.of(), List.of(), Map.of("Match", "[0, 1]"), "Array", null, createArrayState(nums, 0, 1), null, null));
        steps.add(new ExecutionStep(4, 7, "Two Sum Result: Indices [0, 1]", List.of(), Map.of(), List.of(), Map.of("Result", "[0, 1]"), "Array", null, createArrayState(nums, 0, 1), null, null));
        return steps;
    }

    private List<ExecutionStep> generateSort012Steps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{2, 0, 2, 1, 1, 0};
        steps.add(new ExecutionStep(1, 4, "Initial array: [2, 0, 2, 1, 1, 0]. Pointers: low=0, mid=0, high=5", List.of(), Map.of(), List.of(), Map.of("mid", "0"), "Array", null, createArrayState(nums, 0, 5), null, null));
        steps.add(new ExecutionStep(2, 10, "nums[mid=0] == 2: Swap nums[0] and nums[5]. Decrement high=4. Array: [0, 0, 2, 1, 1, 2]", List.of(), Map.of(), List.of(), Map.of("high", "4"), "Array", null, createArrayState(new int[]{0, 0, 2, 1, 1, 2}, 0, 4), null, null));
        steps.add(new ExecutionStep(3, 6, "nums[mid=0] == 0: Swap nums[0] and nums[0]. Increment low=1, mid=1", List.of(), Map.of(), List.of(), Map.of("low", "1", "mid", "1"), "Array", null, createArrayState(new int[]{0, 0, 2, 1, 1, 2}, 1, 4), null, null));
        steps.add(new ExecutionStep(4, 15, "Dutch National Flag algorithm finished. Sorted Array: [0, 0, 1, 1, 2, 2]", List.of(), Map.of(), List.of(), Map.of("Status", "Sorted"), "Array", null, createArrayState(new int[]{0, 0, 1, 1, 2, 2}, -1, -1), null, null));
        return steps;
    }

    private List<ExecutionStep> generateMajorityElementSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{2, 2, 1, 1, 1, 2, 2};
        steps.add(new ExecutionStep(1, 4, "Moore's Voting: Initialize count = 0, candidate el = 0", List.of(), Map.of(), List.of(), Map.of("count", "0"), "Array", null, createArrayState(nums, -1, -1), null, null));
        steps.add(new ExecutionStep(2, 6, "i = 0 (val 2): count was 0 -> set candidate el = 2, count = 1", List.of(), Map.of(), List.of(), Map.of("candidate", "2", "count", "1"), "Array", null, createArrayState(nums, 0, -1), null, null));
        steps.add(new ExecutionStep(3, 8, "i = 1 (val 2): nums[1] == candidate (2) -> count = 2", List.of(), Map.of(), List.of(), Map.of("candidate", "2", "count", "2"), "Array", null, createArrayState(nums, 1, -1), null, null));
        steps.add(new ExecutionStep(4, 13, "Array scan finished. Majority Element (> N/2) = 2", List.of(), Map.of(), List.of(), Map.of("Majority Element", "2"), "Array", null, createArrayState(nums, -1, -1), null, null));
        return steps;
    }

    private List<ExecutionStep> generateKadaneSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4};
        steps.add(new ExecutionStep(1, 4, "Kadane's Algorithm: Initialize maxi = -INF, sum = 0", List.of(), Map.of(), List.of(), Map.of("maxi", "-INF"), "Array", null, createArrayState(nums, -1, -1), null, null));
        steps.add(new ExecutionStep(2, 6, "Process subarray [4, -1, 2, 1] -> sum = 6. Update maxi = 6", List.of(), Map.of(), List.of(), Map.of("sum", "6", "maxi", "6"), "Array", null, createArrayState(nums, 3, 6), null, null));
        steps.add(new ExecutionStep(3, 10, "Kadane's Complete! Maximum Subarray Sum = 6 (Subarray [4, -1, 2, 1])", List.of(), Map.of(), List.of(), Map.of("Max Subarray Sum", "6"), "Array", null, createArrayState(nums, 3, 6), null, null));
        return steps;
    }

    private List<ExecutionStep> generateStockSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] prices = new int[]{7, 1, 5, 3, 6, 4};
        steps.add(new ExecutionStep(1, 4, "Prices: [7, 1, 5, 3, 6, 4]. Initialize minPrice = INF, maxProfit = 0", List.of(), Map.of(), List.of(), Map.of("minPrice", "INF"), "Array", null, createArrayState(prices, -1, -1), null, null));
        steps.add(new ExecutionStep(2, 5, "Day 2: price = 1 -> minPrice updated to 1", List.of(), Map.of(), List.of(), Map.of("minPrice", "1"), "Array", null, createArrayState(prices, 1, -1), null, null));
        steps.add(new ExecutionStep(3, 7, "Day 5: price = 6 -> profit = 6 - 1 = 5. Update maxProfit = 5", List.of(), Map.of(), List.of(), Map.of("buy", "1", "sell", "6", "maxProfit", "5"), "Array", null, createArrayState(prices, 1, 4), null, null));
        steps.add(new ExecutionStep(4, 9, "Max Profit = 5 (Buy at 1, Sell at 6)", List.of(), Map.of(), List.of(), Map.of("Max Profit", "5"), "Array", null, createArrayState(prices, 1, 4), null, null));
        return steps;
    }

    // Helper builders
    private List<ArrayElement> createTwoSumArray() {
        return createArrayState(new int[]{2, 7, 11, 15}, -1, -1);
    }
    private List<ArrayElement> createSort012Array() {
        return createArrayState(new int[]{2, 0, 2, 1, 1, 0}, -1, -1);
    }
    private List<ArrayElement> createMajorityArray() {
        return createArrayState(new int[]{2, 2, 1, 1, 1, 2, 2}, -1, -1);
    }
    private List<ArrayElement> createKadaneArray() {
        return createArrayState(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}, -1, -1);
    }
    private List<ArrayElement> createStockArray() {
        return createArrayState(new int[]{7, 1, 5, 3, 6, 4}, -1, -1);
    }

    private List<ArrayElement> createArrayState(int[] vals, int activeIdx1, int activeIdx2) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            String state = (i == activeIdx1 || i == activeIdx2) ? "active" : "default";
            list.add(new ArrayElement(i, vals[i], state));
        }
        return list;
    }
}
