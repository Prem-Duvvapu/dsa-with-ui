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

    // Dynamic Step Generators
    private List<ExecutionStep> generateBs1dSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 3, 5, 7, 9, 11, 13};
        int target = 9;
        int low = 0, high = nums.length - 1;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Binary Search: Target = 9. Search range: low = 0, high = 6.",
            List.of(), Map.of(), List.of(), Map.of("low", "0", "high", "6", "target", "9"),
            "Array", null, createArrayState(nums, low, high, -1), null, null
        ));

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                steps.add(new ExecutionStep(
                    stepNum++, 6,
                    String.format("Mid Check: mid = %d (val %d). nums[mid] == target (9 == 9)! TARGET FOUND AT INDEX %d!", mid, nums[mid], mid),
                    List.of(), Map.of(), List.of(), Map.of("mid", String.valueOf(mid), "Found Index", String.valueOf(mid)),
                    "Array", null, createArrayState(nums, -1, -1, mid), null, null
                ));
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Binary Search Complete! Return index %d.", mid),
                    List.of(), Map.of(), List.of(), Map.of("Result Index", String.valueOf(mid)),
                    "Array", null, createArrayState(nums, -1, -1, mid), null, null
                ));
                return steps;
            } else if (nums[mid] < target) {
                steps.add(new ExecutionStep(
                    stepNum++, 7,
                    String.format("Mid Check: mid = %d (val %d). nums[mid] (%d) < target (%d). Discard left half! Move low = mid + 1 = %d.", mid, nums[mid], nums[mid], target, mid + 1),
                    List.of(), Map.of(), List.of(), Map.of("mid", String.valueOf(mid), "low", String.valueOf(mid + 1), "high", String.valueOf(high)),
                    "Array", null, createArrayState(nums, mid + 1, high, mid), null, null
                ));
                low = mid + 1;
            } else {
                steps.add(new ExecutionStep(
                    stepNum++, 8,
                    String.format("Mid Check: mid = %d (val %d). nums[mid] (%d) > target (%d). Discard right half! Move high = mid - 1 = %d.", mid, nums[mid], nums[mid], target, mid - 1),
                    List.of(), Map.of(), List.of(), Map.of("mid", String.valueOf(mid), "low", String.valueOf(low), "high", String.valueOf(mid - 1)),
                    "Array", null, createArrayState(nums, low, mid - 1, mid), null, null
                ));
                high = mid - 1;
            }
        }
        return steps;
    }

    private List<ExecutionStep> generateSearchRotatedSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{4, 5, 6, 7, 0, 1, 2};
        int target = 0;
        int low = 0, high = nums.length - 1;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Rotated Array: [4, 5, 6, 7, 0, 1, 2], Target = 0. Range: low = 0, high = 6.",
            List.of(), Map.of(), List.of(), Map.of("low", "0", "high", "6", "target", "0"),
            "Array", null, createArrayState(nums, low, high, -1), null, null
        ));

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                steps.add(new ExecutionStep(
                    stepNum++, 6,
                    String.format("mid = %d (val %d). nums[mid] == target (0 == 0)! TARGET FOUND AT INDEX %d!", mid, nums[mid], mid),
                    List.of(), Map.of(), List.of(), Map.of("mid", String.valueOf(mid), "Found Index", String.valueOf(mid)),
                    "Array", null, createArrayState(nums, -1, -1, mid), null, null
                ));
                return steps;
            }

            if (nums[low] <= nums[mid]) { // Left half sorted
                if (nums[low] <= target && target < nums[mid]) {
                    steps.add(new ExecutionStep(
                        stepNum++, 9,
                        String.format("mid = %d (val %d). Left half [4..7] is sorted and target 0 is inside range [%d..%d]. Move high = mid - 1 = %d.", mid, nums[mid], nums[low], nums[mid], mid - 1),
                        List.of(), Map.of(), List.of(), Map.of("left_sorted", "true", "high", String.valueOf(mid - 1)),
                        "Array", null, createArrayState(nums, low, mid - 1, mid), null, null
                    ));
                    high = mid - 1;
                } else {
                    steps.add(new ExecutionStep(
                        stepNum++, 10,
                        String.format("mid = %d (val %d). Left half [4..7] is sorted, but target 0 is NOT inside range [%d..%d]. Move low = mid + 1 = %d.", mid, nums[mid], nums[low], nums[mid], mid + 1),
                        List.of(), Map.of(), List.of(), Map.of("left_sorted", "true", "low", String.valueOf(mid + 1)),
                        "Array", null, createArrayState(nums, mid + 1, high, mid), null, null
                    ));
                    low = mid + 1;
                }
            } else { // Right half sorted
                if (nums[mid] < target && target <= nums[high]) {
                    steps.add(new ExecutionStep(
                        stepNum++, 12,
                        String.format("mid = %d (val %d). Right half is sorted and target is inside range. Move low = mid + 1.", mid, nums[mid]),
                        List.of(), Map.of(), List.of(), Map.of("right_sorted", "true", "low", String.valueOf(mid + 1)),
                        "Array", null, createArrayState(nums, mid + 1, high, mid), null, null
                    ));
                    low = mid + 1;
                } else {
                    steps.add(new ExecutionStep(
                        stepNum++, 13,
                        String.format("mid = %d (val %d). Right half is sorted, target NOT inside range. Move high = mid - 1.", mid, nums[mid]),
                        List.of(), Map.of(), List.of(), Map.of("right_sorted", "true", "high", String.valueOf(mid - 1)),
                        "Array", null, createArrayState(nums, low, mid - 1, mid), null, null
                    ));
                    high = mid - 1;
                }
            }
        }
        return steps;
    }

    private List<ExecutionStep> generateFindPeakSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 2, 1, 3, 5, 6, 4};
        int low = 1, high = nums.length - 2;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 8,
            "Find Peak Element: Array = [1, 2, 1, 3, 5, 6, 4]. Range: low = 1, high = 5.",
            List.of(), Map.of(), List.of(), Map.of("low", "1", "high", "5"),
            "Array", null, createArrayState(nums, low, high, -1), null, null
        ));

        while (low <= high) {
            int mid = low + (high - low) / 2;
            boolean isPeak = nums[mid] > nums[mid - 1] && nums[mid] > nums[mid + 1];

            if (isPeak) {
                steps.add(new ExecutionStep(
                    stepNum++, 11,
                    String.format("mid = %d (val %d): nums[%d] (%d) > nums[%d] (%d) AND nums[%d] > nums[%d] (%d). PEAK FOUND AT INDEX %d!", mid, nums[mid], mid, nums[mid], mid - 1, nums[mid - 1], mid, mid + 1, nums[mid + 1], mid),
                    List.of(), Map.of(), List.of(), Map.of("Peak Index", String.valueOf(mid), "Peak Value", String.valueOf(nums[mid])),
                    "Array", null, createArrayState(nums, -1, -1, mid), null, null
                ));
                return steps;
            } else if (nums[mid] > nums[mid - 1]) {
                steps.add(new ExecutionStep(
                    stepNum++, 12,
                    String.format("mid = %d (val %d): Ascending slope (nums[%d] > nums[%d]). Peak MUST exist to the right! Move low = mid + 1 = %d.", mid, nums[mid], mid, mid - 1, mid + 1),
                    List.of(), Map.of(), List.of(), Map.of("slope", "ascending", "low", String.valueOf(mid + 1)),
                    "Array", null, createArrayState(nums, mid + 1, high, mid), null, null
                ));
                low = mid + 1;
            } else {
                steps.add(new ExecutionStep(
                    stepNum++, 13,
                    String.format("mid = %d (val %d): Descending slope (nums[%d] <= nums[%d]). Peak MUST exist to the left! Move high = mid - 1 = %d.", mid, nums[mid], mid, mid - 1, mid - 1),
                    List.of(), Map.of(), List.of(), Map.of("slope", "descending", "high", String.valueOf(mid - 1)),
                    "Array", null, createArrayState(nums, low, mid - 1, mid), null, null
                ));
                high = mid - 1;
            }
        }
        return steps;
    }

    private List<ExecutionStep> generateKokoSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] piles = new int[]{3, 6, 7, 11};
        int h = 8;
        int low = 1, high = 11, ans = 11;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Koko Eating Bananas: Piles = [3, 6, 7, 11], Target Hours h = 8. Binary search speed range k in [1, 11].",
            List.of(), Map.of(), List.of(), Map.of("low_k", "1", "high_k", "11", "h", "8"),
            "Array", null, createArrayState(piles, -1, -1, -1), null, null
        ));

        while (low <= high) {
            int mid = low + (high - low) / 2;
            long hours = calculateHours(piles, mid);

            if (hours <= h) {
                ans = mid;
                steps.add(new ExecutionStep(
                    stepNum++, 9,
                    String.format("Speed k = %d: Koko finishes in %d hours <= 8. Speed %d WORKS! Save ans = %d, try smaller speed high = mid - 1 = %d.", mid, hours, mid, ans, mid - 1),
                    List.of(), Map.of(), List.of(), Map.of("k", String.valueOf(mid), "hours", String.valueOf(hours), "ans", String.valueOf(ans)),
                    "Array", null, createArrayState(piles, -1, -1, -1), null, null
                ));
                high = mid - 1;
            } else {
                steps.add(new ExecutionStep(
                    stepNum++, 11,
                    String.format("Speed k = %d: Koko needs %d hours > 8 (Too slow!). Speed %d FAILS! Increase speed low = mid + 1 = %d.", mid, hours, mid, mid + 1),
                    List.of(), Map.of(), List.of(), Map.of("k", String.valueOf(mid), "hours", String.valueOf(hours), "status", "too slow"),
                    "Array", null, createArrayState(piles, -1, -1, -1), null, null
                ));
                low = mid + 1;
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 14,
            String.format("Koko Eating Bananas Complete! Minimum Eating Speed k = %d.", ans),
            List.of(), Map.of(), List.of(), Map.of("Min Speed k", String.valueOf(ans)),
            "Array", null, createArrayState(piles, -1, -1, -1), null, null
        ));

        return steps;
    }

    private long calculateHours(int[] piles, int speed) {
        long h = 0;
        for (int p : piles) h += (p + speed - 1) / speed;
        return h;
    }

    // Helper builders
    private List<ArrayElement> createSortedArray() { return createArrayState(new int[]{1, 3, 5, 7, 9, 11, 13}, 0, 6, -1); }
    private List<ArrayElement> createRotatedArray() { return createArrayState(new int[]{4, 5, 6, 7, 0, 1, 2}, 0, 6, -1); }
    private List<ArrayElement> createPeakArray() { return createArrayState(new int[]{1, 2, 1, 3, 5, 6, 4}, 1, 5, -1); }
    private List<ArrayElement> createKokoArray() { return createArrayState(new int[]{3, 6, 7, 11}, -1, -1, -1); }

    private List<ArrayElement> createArrayState(int[] vals, int lowIdx, int highIdx, int midIdx) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            String state = "default";
            if (i == midIdx) state = "pivot";
            else if (i == lowIdx || i == highIdx) state = "comparing";
            list.add(new ArrayElement(i, vals[i], state));
        }
        return list;
    }
}
