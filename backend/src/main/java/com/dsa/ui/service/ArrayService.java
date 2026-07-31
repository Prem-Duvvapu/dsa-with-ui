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
                count = 1;
                el = nums[i];
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
            String.format("Kadane's Algorithm Complete! Maximum Subarray Sum = %d (Subarray [4, -1, 2, 1]).", maxi),
            List.of(), Map.of(), List.of(), Map.of("Max Subarray Sum", String.valueOf(maxi)),
            "Array", null, createArrayState(nums, 3, 6), null, null
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
            if (prices[i] < minPrice) {
                minPrice = prices[i];
            }
            int profit = prices[i] - minPrice;
            if (profit > maxProfit) {
                maxProfit = profit;
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Day %d (price %d): New minimum buy price = %d! Potential profit = %d - %d = %d > maxProfit! Update maxProfit = %d.", i + 1, prices[i], minPrice, prices[i], minPrice, profit, maxProfit),
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

    // Helper builders
    private List<ArrayElement> createTwoSumArray() { return createArrayState(new int[]{2, 7, 11, 15}, -1, -1); }
    private List<ArrayElement> createSort012Array() { return createArrayState(new int[]{2, 0, 2, 1, 1, 0}, -1, -1); }
    private List<ArrayElement> createMajorityArray() { return createArrayState(new int[]{2, 2, 1, 1, 1, 2, 2}, -1, -1); }
    private List<ArrayElement> createKadaneArray() { return createArrayState(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}, -1, -1); }
    private List<ArrayElement> createStockArray() { return createArrayState(new int[]{7, 1, 5, 3, 6, 4}, -1, -1); }

    private List<ArrayElement> createArrayState(int[] vals, int activeIdx1, int activeIdx2) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            String state = (i == activeIdx1 || i == activeIdx2) ? "active" : "default";
            list.add(new ArrayElement(i, vals[i], state));
        }
        return list;
    }
}
