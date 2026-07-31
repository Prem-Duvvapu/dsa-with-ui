package com.dsa.ui.service;

import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BinarySearchService {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public BinarySearchService() {
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
            case "binary-search-1d": return generateBs1dSteps();
            case "search-rotated-sorted": return generateSearchRotatedSteps();
            case "find-peak-element": return generateFindPeakSteps();
            case "koko-eating-bananas": return generateKokoSteps();
            default: return generateBs1dSteps();
        }
    }

    private void initProblems() {
        // 1. Binary Search 1D
        problems.put("binary-search-1d", new ProblemDetail(
            "binary-search-1d", "Binary Search on 1D Array", "Binary Search - 1D Arrays", "Binary Search", "Easy",
            "Search for a target value in a sorted array using binary search.",
            """
            // Java Binary Search (LeetCode 704)
            public int search(int[] nums, int target) {
                int low = 0, high = nums.length - 1;
                while (low <= high) {
                    int mid = low + (high - low) / 2;
                    if (nums[mid] == target) return mid;
                    else if (nums[mid] < target) low = mid + 1;
                    else high = mid - 1;
                }
                return -1;
            }
            """,
            null, null, null, createSortedArray(), null, null, null,
            new ComplexityDetail(
                "O(log N)",
                "Time Complexity: Search space is halved at every iteration.",
                "Why O(log N)? Recurrence T(N) = T(N/2) + O(1) yields log2(N) steps.",
                "O(1)",
                "Space Complexity: Iterative approach takes O(1) space.",
                "Why O(1)? Uses `low`, `high`, `mid` pointers.",
                "Auxiliary Space: O(1)",
                "Return Index: O(1)"
            ),
            "Array"
        ));

        // 2. Search in Rotated Sorted Array
        problems.put("search-rotated-sorted", new ProblemDetail(
            "search-rotated-sorted", "Search in Rotated Sorted Array", "Binary Search - 1D Arrays", "Binary Search", "Medium",
            "Search for target in a sorted array that has been rotated at an unknown pivot point.",
            """
            // Java Search in Rotated Sorted Array (LeetCode 33)
            public int search(int[] nums, int target) {
                int low = 0, high = nums.length - 1;
                while (low <= high) {
                    int mid = low + (high - low) / 2;
                    if (nums[mid] == target) return mid;

                    // Left half is sorted
                    if (nums[low] <= nums[mid]) {
                        if (nums[low] <= target && target < nums[mid]) high = mid - 1;
                        else low = mid + 1;
                    } else { // Right half is sorted
                        if (nums[mid] < target && target <= nums[high]) low = mid + 1;
                        else high = mid - 1;
                    }
                }
                return -1;
            }
            """,
            null, null, null, createRotatedArray(), null, null, null,
            new ComplexityDetail(
                "O(log N)",
                "Time Complexity: At least one half of the rotated array (left or right) is ALWAYS sorted! Eliminates half of array per step.",
                "Why O(log N)? Identifies which half is sorted, checks if target lies within that sorted range, and discards the other half.",
                "O(1)",
                "Space Complexity: O(1) space.",
                "Why O(1)? Uses only index variables.",
                "Auxiliary Space: O(1)",
                "Return Index: O(1)"
            ),
            "Array"
        ));

        // 3. Find Peak Element
        problems.put("find-peak-element", new ProblemDetail(
            "find-peak-element", "Find Peak Element", "Binary Search - 1D Arrays", "Binary Search", "Medium",
            "A peak element is an element that is strictly greater than its neighbors. Find index of any peak.",
            """
            // Java Find Peak Element (LeetCode 162)
            public int findPeakElement(int[] nums) {
                int n = nums.length;
                if (n == 1) return 0;
                if (nums[0] > nums[1]) return 0;
                if (nums[n - 1] > nums[n - 2]) return n - 1;

                int low = 1, high = n - 2;
                while (low <= high) {
                    int mid = low + (high - low) / 2;
                    if (nums[mid] > nums[mid - 1] && nums[mid] > nums[mid + 1]) return mid;
                    else if (nums[mid] > nums[mid - 1]) low = mid + 1; // On ascending slope -> peak is on right
                    else high = mid - 1; // On descending slope -> peak is on left
                }
                return -1;
            }
            """,
            null, null, null, createPeakArray(), null, null, null,
            new ComplexityDetail(
                "O(log N)",
                "Time Complexity: Binary search on slope direction.",
                "Why slope indicates peak? If nums[mid] > nums[mid-1], an ascending slope guarantees at least one peak exists to the right.",
                "O(1)",
                "Space Complexity: O(1) space.",
                "Why O(1)? Iterative pointer search.",
                "Auxiliary Space: O(1)",
                "Return Index: O(1)"
            ),
            "Array"
        ));

        // 4. Koko Eating Bananas
        problems.put("koko-eating-bananas", new ProblemDetail(
            "koko-eating-bananas", "Koko Eating Bananas (BS on Answer)", "Binary Search - On Answers", "Binary Search", "Medium",
            "Find the minimum integer eating speed k such that Koko can eat all bananas within h hours.",
            """
            // Java Koko Eating Bananas (LeetCode 875)
            public int minEatingSpeed(int[] piles, int h) {
                int low = 1, high = getMax(piles);
                int ans = high;

                while (low <= high) {
                    int mid = low + (high - low) / 2;
                    long totalHours = calculateHours(piles, mid);
                    if (totalHours <= h) {
                        ans = mid;
                        high = mid - 1; // Try smaller speed
                    } else {
                        low = mid + 1; // Speed too slow, increase speed!
                    }
                }
                return ans;
            }
            """,
            null, null, null, createKokoArray(), null, null, null,
            new ComplexityDetail(
                "O(N log (max(A)))",
                "Time Complexity: Binary search range `[1, max(piles)]` takes log(max(A)) steps. Hour calculation takes O(N) per step.",
                "Why Binary Search on Answer works? The function `calculateHours(speed)` is monotonically decreasing as `speed` increases.",
                "O(1)",
                "Space Complexity: O(1) space.",
                "Why O(1)? Only search range variables used.",
                "Auxiliary Space: O(1)",
                "Return Speed: O(1)"
            ),
            "Array"
        ));
    }

    // Step Generators
    private List<ExecutionStep> generateBs1dSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 3, 5, 7, 9, 11, 13};
        steps.add(new ExecutionStep(1, 4, "Sorted Array: [1, 3, 5, 7, 9, 11, 13]. Target = 9. Pointers: low=0, high=6", List.of(), Map.of(), List.of(), Map.of("low", "0", "high", "6"), "Array", null, createArrayState(nums, 0, 6), null, null));
        steps.add(new ExecutionStep(2, 5, "mid = 0 + (6-0)/2 = 3 (val 7). nums[3] (7) < target (9). Move low = mid + 1 = 4", List.of(), Map.of(), List.of(), Map.of("mid", "3", "nums[mid]", "7"), "Array", null, createArrayState(nums, 3, -1), null, null));
        steps.add(new ExecutionStep(3, 6, "low=4, high=6 -> mid = 4 + (6-4)/2 = 5 (val 11). nums[5] (11) > target (9). Move high = mid - 1 = 4", List.of(), Map.of(), List.of(), Map.of("mid", "5", "nums[mid]", "11"), "Array", null, createArrayState(nums, 5, -1), null, null));
        steps.add(new ExecutionStep(4, 6, "low=4, high=4 -> mid = 4 (val 9). nums[4] == target (9). FOUND TARGET AT INDEX 4!", List.of(), Map.of(), List.of(), Map.of("Found Index", "4"), "Array", null, createArrayState(nums, 4, -1), null, null));
        return steps;
    }

    private List<ExecutionStep> generateSearchRotatedSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{4, 5, 6, 7, 0, 1, 2};
        steps.add(new ExecutionStep(1, 4, "Rotated Array: [4, 5, 6, 7, 0, 1, 2]. Target = 0. Pointers: low=0, high=6", List.of(), Map.of(), List.of(), Map.of("target", "0"), "Array", null, createArrayState(nums, 0, 6), null, null));
        steps.add(new ExecutionStep(2, 8, "mid = 3 (val 7). Left half [4, 5, 6, 7] is sorted. Target 0 is NOT in left range [4..7]. Move low = mid + 1 = 4", List.of(), Map.of(), List.of(), Map.of("mid", "3", "left_sorted", "true"), "Array", null, createArrayState(nums, 3, -1), null, null));
        steps.add(new ExecutionStep(3, 6, "low=4, high=6 -> mid = 4 (val 0). nums[4] == target (0). TARGET FOUND AT INDEX 4!", List.of(), Map.of(), List.of(), Map.of("Found Index", "4"), "Array", null, createArrayState(nums, 4, -1), null, null));
        return steps;
    }

    private List<ExecutionStep> generateFindPeakSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 2, 1, 3, 5, 6, 4};
        steps.add(new ExecutionStep(1, 8, "Array: [1, 2, 1, 3, 5, 6, 4]. Pointers: low=1, high=5", List.of(), Map.of(), List.of(), Map.of("low", "1", "high", "5"), "Array", null, createArrayState(nums, 1, 5), null, null));
        steps.add(new ExecutionStep(2, 11, "mid = 3 (val 3). nums[3] (3) > nums[2] (1) -> Ascending slope! Move low = mid + 1 = 4", List.of(), Map.of(), List.of(), Map.of("mid", "3", "slope", "ascending"), "Array", null, createArrayState(nums, 3, -1), null, null));
        steps.add(new ExecutionStep(3, 11, "low=4, high=5 -> mid = 5 (val 6). nums[5] (6) > 5 and nums[5] (6) > 4. PEAK FOUND AT INDEX 5!", List.of(), Map.of(), List.of(), Map.of("Peak Index", "5", "Peak Val", "6"), "Array", null, createArrayState(nums, 5, -1), null, null));
        return steps;
    }

    private List<ExecutionStep> generateKokoSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] piles = new int[]{3, 6, 7, 11};
        steps.add(new ExecutionStep(1, 4, "Bananas: [3, 6, 7, 11], Max hours h = 8. Search speed range k in [1, 11]", List.of(), Map.of(), List.of(), Map.of("low_k", "1", "high_k", "11"), "Array", null, createArrayState(piles, -1, -1), null, null));
        steps.add(new ExecutionStep(2, 8, "Test speed k = 6: hours = ceil(3/6) + ceil(6/6) + ceil(7/6) + ceil(11/6) = 1+1+2+2 = 6 <= 8. Speed 6 WORKS! Try smaller speed...", List.of(), Map.of(), List.of(), Map.of("speed_k", "6", "hours", "6"), "Array", null, createArrayState(piles, -1, -1), null, null));
        steps.add(new ExecutionStep(3, 8, "Test speed k = 4: hours = 1+2+2+3 = 8 <= 8. Speed 4 WORKS! Try speed 3...", List.of(), Map.of(), List.of(), Map.of("speed_k", "4", "hours", "8"), "Array", null, createArrayState(piles, -1, -1), null, null));
        steps.add(new ExecutionStep(4, 12, "Test speed k = 3: hours = 1+2+3+4 = 10 > 8 (Too slow!). Minimum Eating Speed = 4", List.of(), Map.of(), List.of(), Map.of("Min Speed k", "4"), "Array", null, createArrayState(piles, -1, -1), null, null));
        return steps;
    }

    // Helper builders
    private List<ArrayElement> createSortedArray() {
        return createArrayState(new int[]{1, 3, 5, 7, 9, 11, 13}, -1, -1);
    }
    private List<ArrayElement> createRotatedArray() {
        return createArrayState(new int[]{4, 5, 6, 7, 0, 1, 2}, -1, -1);
    }
    private List<ArrayElement> createPeakArray() {
        return createArrayState(new int[]{1, 2, 1, 3, 5, 6, 4}, -1, -1);
    }
    private List<ArrayElement> createKokoArray() {
        return createArrayState(new int[]{3, 6, 7, 11}, -1, -1);
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
