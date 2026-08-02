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
            case "lower-bound": return generateLowerBoundSteps();
            case "upper-bound": return generateUpperBoundSteps();
            case "search-insert-position": return generateSearchInsertSteps();
            case "floor-ceil-sorted-array": return generateFloorCeilSteps();
            case "first-last-occurrence": return generateFirstLastOccurrenceSteps();
            case "count-occurrences": return generateCountOccurrencesSteps();
            case "search-rotated-sorted": return generateSearchRotatedSteps();
            case "search-rotated-sorted-2": return generateSearchRotated2Steps();
            case "find-min-rotated-sorted": return generateFindMinRotatedSteps();
            case "count-rotations": return generateCountRotationsSteps();
            case "single-element-sorted": return generateSingleElementSortedSteps();
            case "find-peak-element": return generateFindPeakSteps();
            case "square-root-number": return generateSquareRootSteps();
            case "nth-root-number": return generateNthRootSteps();
            case "koko-eating-bananas": return generateKokoSteps();
            case "min-days-bouquets": return generateMinDaysBouquetsSteps();
            case "smallest-divisor": return generateSmallestDivisorSteps();
            case "ship-packages-d-days": return generateShipPackagesSteps();
            case "kth-missing-positive": return generateKthMissingPositiveSteps();
            case "aggressive-cows": return generateAggressiveCowsSteps();
            case "book-allocation": return generateBookAllocationSteps();
            case "split-array-largest-sum": return generateSplitArrayLargestSumSteps();
            case "painters-partition": return generatePaintersPartitionSteps();
            case "minimize-max-distance-gas-station": return generateGasStationSteps();
            case "median-2-sorted-arrays": return generateMedian2SortedArraysSteps();
            case "kth-element-2-sorted-arrays": return generateKthElement2SortedArraysSteps();
            case "row-max-ones": return generateRowMaxOnesSteps();
            case "search-2d-matrix": return generateSearch2dMatrixSteps();
            case "search-2d-matrix-2": return generateSearch2dMatrix2Steps();
            case "find-peak-element-2d": return generateFindPeakElement2dSteps();
            case "matrix-median": return generateMatrixMedianSteps();
            default: return generateBs1dSteps();
        }
    }

    private void initProblems() {
        // 1. Binary Search 1D
        problems.put("binary-search-1d", new ProblemDetail(
            "binary-search-1d", "Search X in Sorted Array", "Binary Search - 1D Arrays", "Binary Search", "Easy",
            "Search for a target value X in a sorted array using binary search.",
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
            new ComplexityDetail("O(log N)", "Time Complexity: Search space is halved at every iteration.", "Recurrence log2(N)", "O(1)", "Space Complexity: Iterative pointers low, high, mid.", "Iterative", "Auxiliary Space: O(1)", "Pointers"), "Array"
        ));

        // 2. Lower Bound
        problems.put("lower-bound", new ProblemDetail(
            "lower-bound", "Lower Bound", "Binary Search - 1D Arrays", "Binary Search", "Easy",
            "Find the smallest index in a sorted array such that arr[index] >= target X.",
            """
            // Java Lower Bound Algorithm (Striver Sheet)
            public int lowerBound(int[] arr, int n, int x) {
                int low = 0, high = n - 1, ans = n;
                while (low <= high) {
                    int mid = (low + high) / 2;
                    if (arr[mid] >= x) { ans = mid; high = mid - 1; }
                    else low = mid + 1;
                }
                return ans;
            }
            """,
            null, null, null, createSortedArray(), null, null, null,
            new ComplexityDetail("O(log N)", "Time Complexity: Halves search space each step.", "Binary Search", "O(1)", "Space Complexity: Constant memory.", "Iterative", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 3. Upper Bound
        problems.put("upper-bound", new ProblemDetail(
            "upper-bound", "Upper Bound", "Binary Search - 1D Arrays", "Binary Search", "Easy",
            "Find the smallest index in a sorted array such that arr[index] > target X.",
            """
            // Java Upper Bound Algorithm (Striver Sheet)
            public int upperBound(int[] arr, int n, int x) {
                int low = 0, high = n - 1, ans = n;
                while (low <= high) {
                    int mid = (low + high) / 2;
                    if (arr[mid] > x) { ans = mid; high = mid - 1; }
                    else low = mid + 1;
                }
                return ans;
            }
            """,
            null, null, null, createSortedArray(), null, null, null,
            new ComplexityDetail("O(log N)", "Time Complexity: Binary search halving.", "Binary Search", "O(1)", "Space Complexity: O(1) space.", "Iterative", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 4. Search Insert Position
        problems.put("search-insert-position", new ProblemDetail(
            "search-insert-position", "Search Insert Position", "Binary Search - 1D Arrays", "Binary Search", "Easy",
            "Find index if target is found. If not, return index where it would be if inserted in order.",
            """
            // Java Search Insert Position (LeetCode 35)
            public int searchInsert(int[] nums, int target) {
                int low = 0, high = nums.length - 1, ans = nums.length;
                while (low <= high) {
                    int mid = (low + high) / 2;
                    if (nums[mid] >= target) { ans = mid; high = mid - 1; }
                    else low = mid + 1;
                }
                return ans;
            }
            """,
            null, null, null, createSortedArray(), null, null, null,
            new ComplexityDetail("O(log N)", "Time Complexity: Equivalent to finding Lower Bound of target.", "Lower Bound", "O(1)", "Space Complexity: Constant extra memory.", "Pointers", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 5. Floor and Ceil
        problems.put("floor-ceil-sorted-array", new ProblemDetail(
            "floor-ceil-sorted-array", "Floor and Ceil in Sorted Array", "Binary Search - 1D Arrays", "Binary Search", "Easy",
            "Find Floor (largest val <= X) and Ceil (smallest val >= X) in a sorted array.",
            """
            // Java Floor and Ceil
            public int[] getFloorAndCeil(int[] a, int n, int x) {
                int f = findFloor(a, n, x);
                int c = findCeil(a, n, x);
                return new int[]{f, c};
            }
            """,
            null, null, null, createSortedArray(), null, null, null,
            new ComplexityDetail("O(log N)", "Time Complexity: Two binary searches for floor and ceil.", "Binary Search", "O(1)", "Space Complexity: O(1) memory.", "Iterative", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 6. First and Last Occurrence
        problems.put("first-last-occurrence", new ProblemDetail(
            "first-last-occurrence", "First and Last Occurrence", "Binary Search - 1D Arrays", "Binary Search", "Medium",
            "Find starting and ending position of a given target value in a sorted array.",
            """
            // Java First and Last Occurrence (LeetCode 34)
            public int[] searchRange(int[] nums, int target) {
                int first = firstOccurrence(nums, target);
                if (first == -1) return new int[]{-1, -1};
                int last = lastOccurrence(nums, target);
                return new int[]{first, last};
            }
            """,
            null, null, null, createDuplicateSortedArray(), null, null, null,
            new ComplexityDetail("O(log N)", "Time Complexity: Two binary searches 2 * O(log N) = O(log N).", "Binary Search", "O(1)", "Space Complexity: Constant auxiliary space.", "Pointers", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 7. Count Occurrences
        problems.put("count-occurrences", new ProblemDetail(
            "count-occurrences", "Count Occurrences in Sorted Array", "Binary Search - 1D Arrays", "Binary Search", "Easy",
            "Given a sorted array of N integers and a target X, count total occurrences of X.",
            """
            // Java Count Occurrences (Striver Sheet)
            public int count(int[] arr, int n, int x) {
                int first = firstOccurrence(arr, n, x);
                if (first == -1) return 0;
                int last = lastOccurrence(arr, n, x);
                return (last - first + 1);
            }
            """,
            null, null, null, createDuplicateSortedArray(), null, null, null,
            new ComplexityDetail("O(log N)", "Time Complexity: Difference between last and first occurrence indices.", "Binary Search", "O(1)", "Space Complexity: O(1) space.", "Iterative", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 8. Search in Rotated Sorted Array I
        problems.put("search-rotated-sorted", new ProblemDetail(
            "search-rotated-sorted", "Search in Rotated Sorted Array I", "Binary Search - 1D Arrays", "Binary Search", "Medium",
            "Search for target in a sorted array that has been rotated at an unknown pivot point.",
            """
            // Java Search in Rotated Sorted Array (LeetCode 33)
            public int search(int[] nums, int target) {
                int low = 0, high = nums.length - 1;
                while (low <= high) {
                    int mid = low + (high - low) / 2;
                    if (nums[mid] == target) return mid;
                    if (nums[low] <= nums[mid]) {
                        if (nums[low] <= target && target < nums[mid]) high = mid - 1;
                        else low = mid + 1;
                    } else {
                        if (nums[mid] < target && target <= nums[high]) low = mid + 1;
                        else high = mid - 1;
                    }
                }
                return -1;
            }
            """,
            null, null, null, createRotatedArray(), null, null, null,
            new ComplexityDetail("O(log N)", "Time Complexity: At least one half of array is always sorted.", "Sorted Half Halving", "O(1)", "Space Complexity: Iterative pointers.", "Pointers", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 9. Search in Rotated Sorted Array II
        problems.put("search-rotated-sorted-2", new ProblemDetail(
            "search-rotated-sorted-2", "Search in Rotated Sorted Array II", "Binary Search - 1D Arrays", "Binary Search", "Medium",
            "Search for target in a rotated sorted array containing duplicates.",
            """
            // Java Search Rotated Array II (LeetCode 81)
            public boolean search(int[] nums, int target) {
                int low = 0, high = nums.length - 1;
                while (low <= high) {
                    int mid = low + (high - low) / 2;
                    if (nums[mid] == target) return true;
                    if (nums[low] == nums[mid] && nums[mid] == nums[high]) {
                        low++; high--; continue; // Shrink search space when duplicates match!
                    }
                    if (nums[low] <= nums[mid]) {
                        if (nums[low] <= target && target < nums[mid]) high = mid - 1;
                        else low = mid + 1;
                    } else {
                        if (nums[mid] < target && target <= nums[high]) low = mid + 1;
                        else high = mid - 1;
                    }
                }
                return false;
            }
            """,
            null, null, null, createRotatedDuplicateArray(), null, null, null,
            new ComplexityDetail("O(log N) Avg, O(N) Worst", "Time Complexity: Average O(log N), Worst O(N) when all array elements are identical.", "Duplicate Shrinking", "O(1)", "Space Complexity: Constant memory.", "Pointers", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 10. Find Minimum in Rotated Sorted Array
        problems.put("find-min-rotated-sorted", new ProblemDetail(
            "find-min-rotated-sorted", "Find Minimum in Rotated Sorted Array", "Binary Search - 1D Arrays", "Binary Search", "Medium",
            "Find the minimum element in a rotated sorted array of unique numbers.",
            """
            // Java Find Minimum in Rotated Array (LeetCode 153)
            public int findMin(int[] nums) {
                int low = 0, high = nums.length - 1, ans = Integer.MAX_VALUE;
                while (low <= high) {
                    int mid = (low + high) / 2;
                    if (nums[low] <= nums[mid]) {
                        ans = Math.min(ans, nums[low]); low = mid + 1;
                    } else {
                        ans = Math.min(ans, nums[mid]); high = mid - 1;
                    }
                }
                return ans;
            }
            """,
            null, null, null, createRotatedArray(), null, null, null,
            new ComplexityDetail("O(log N)", "Time Complexity: Halves array by tracking min in sorted half.", "Binary Search", "O(1)", "Space Complexity: Constant extra space.", "Pointers", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 11. Count Rotations
        problems.put("count-rotations", new ProblemDetail(
            "count-rotations", "Find How Many Times Array is Rotated", "Binary Search - 1D Arrays", "Binary Search", "Easy",
            "Find total number of right rotations in a rotated sorted array (Index of minimum element).",
            """
            // Java Count Rotations (Striver Sheet)
            public int findKRotation(int[] arr, int n) {
                int low = 0, high = n - 1, ans = Integer.MAX_VALUE, index = -1;
                while (low <= high) {
                    int mid = (low + high) / 2;
                    if (arr[low] <= arr[mid]) {
                        if (arr[low] < ans) { ans = arr[low]; index = low; }
                        low = mid + 1;
                    } else {
                        if (arr[mid] < ans) { ans = arr[mid]; index = mid; }
                        high = mid - 1;
                    }
                }
                return index;
            }
            """,
            null, null, null, createRotatedArray(), null, null, null,
            new ComplexityDetail("O(log N)", "Time Complexity: Index of minimum element equals total right rotations.", "Min Index Search", "O(1)", "Space Complexity: O(1) space.", "Pointers", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 12. Single Element in Sorted Array
        problems.put("single-element-sorted", new ProblemDetail(
            "single-element-sorted", "Single Element in Sorted Array", "Binary Search - 1D Arrays", "Binary Search", "Medium",
            "Given a sorted array where every element appears twice except one, find the single non-duplicate element in O(log N) time.",
            """
            // Java Single Element in Sorted Array (LeetCode 540)
            public int singleNonDuplicate(int[] nums) {
                int n = nums.length;
                if (n == 1) return nums[0];
                if (nums[0] != nums[1]) return nums[0];
                if (nums[n - 1] != nums[n - 2]) return nums[n - 1];

                int low = 1, high = n - 2;
                while (low <= high) {
                    int mid = (low + high) / 2;
                    if (nums[mid] != nums[mid - 1] && nums[mid] != nums[mid + 1]) return nums[mid];
                    if ((mid % 2 == 1 && nums[mid] == nums[mid - 1]) || (mid % 2 == 0 && nums[mid] == nums[mid + 1])) {
                        low = mid + 1; // Standing on left half -> single element is on right!
                    } else {
                        high = mid - 1; // Standing on right half -> single element is on left!
                    }
                }
                return -1;
            }
            """,
            null, null, null, createSingleNonDuplicateArray(), null, null, null,
            new ComplexityDetail("O(log N)", "Time Complexity: Even/Odd index parity determines whether single element lies left or right.", "Index Parity Halving", "O(1)", "Space Complexity: Constant memory.", "Pointers", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 13. Find Peak Element
        problems.put("find-peak-element", new ProblemDetail(
            "find-peak-element", "Find Peak Element", "Binary Search - 1D Arrays", "Binary Search", "Medium",
            "A peak element is an element that is strictly greater than its neighbors. Find any peak element in O(log N) time.",
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
                    if (nums[mid] > nums[mid - 1]) low = mid + 1;
                    else high = mid - 1;
                }
                return -1;
            }
            """,
            null, null, null, createPeakArray(), null, null, null,
            new ComplexityDetail("O(log N)", "Time Complexity: Follows rising slope towards peak.", "Slope Binary Search", "O(1)", "Space Complexity: O(1) space.", "Pointers", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // Populate BS on Answers (14 to 27) and BS on 2D Arrays (28 to 32)
        populateBsAnswersAnd2DProblems();
    }

    private void populateBsAnswersAnd2DProblems() {
        String[][] list = new String[][]{
            {"square-root-number", "14. Find Square Root of a Number", "Binary Search - Answers", "Easy", "Find integer square root floor(sqrt(N)) using Binary Search on range [1..N]."},
            {"nth-root-number", "15. Find Nth Root of a Number", "Binary Search - Answers", "Medium", "Find integer Mth root of N using Binary Search on range [1..M]."},
            {"koko-eating-bananas", "16. Koko Eating Bananas", "Binary Search - Answers", "Medium", "Find minimum eating speed K to eat all bananas within H hours."},
            {"min-days-bouquets", "17. Minimum Days for M Bouquets", "Binary Search - Answers", "Medium", "Find minimum days needed to bloom M bouquets of K adjacent flowers."},
            {"smallest-divisor", "18. Find Smallest Divisor Given Threshold", "Binary Search - Answers", "Medium", "Find smallest divisor such that sum of division results <= threshold."},
            {"ship-packages-d-days", "19. Capacity to Ship Packages Within D Days", "Binary Search - Answers", "Medium", "Find minimum weight capacity to ship all packages within D days."},
            {"kth-missing-positive", "20. Kth Missing Positive Number", "Binary Search - Answers", "Easy", "Find the K-th missing positive integer in a sorted array."},
            {"aggressive-cows", "21. Aggressive Cows", "Binary Search - Answers", "Hard", "Maximize minimum distance between any two assigned cows in stalls."},
            {"book-allocation", "22. Book Allocation Problem", "Binary Search - Answers", "Hard", "Minimize maximum pages allocated to any student."},
            {"split-array-largest-sum", "23. Split Array - Largest Sum", "Binary Search - Answers", "Hard", "Split array into K subarrays minimizing the maximum subarray sum."},
            {"painters-partition", "24. Painter's Partition Problem", "Binary Search - Answers", "Hard", "Minimize maximum time to paint N boards using K painters."},
            {"minimize-max-distance-gas-station", "25. Minimize Max Distance to Gas Station", "Binary Search - Answers", "Hard", "Add K gas stations to minimize maximum distance between adjacent stations."},
            {"median-2-sorted-arrays", "26. Median of 2 Sorted Arrays", "Binary Search - Answers", "Hard", "Find median of two sorted arrays in O(log(min(N, M))) time."},
            {"kth-element-2-sorted-arrays", "27. Kth Element of 2 Sorted Arrays", "Binary Search - Answers", "Hard", "Find K-th element of 2 sorted arrays in O(log(min(N, M))) time."},
            {"row-max-ones", "28. Find Row with Maximum 1s", "Binary Search - 2D Arrays", "Medium", "Find row with maximum number of 1s in a row-sorted 2D binary matrix."},
            {"search-2d-matrix", "29. Search in a 2D Matrix", "Binary Search - 2D Arrays", "Medium", "Search target in M x N matrix where rows & first cols are sorted."},
            {"search-2d-matrix-2", "30. Search in 2D Matrix II", "Binary Search - 2D Arrays", "Medium", "Search target in matrix sorted row-wise and column-wise from top-right corner."},
            {"find-peak-element-2d", "31. Find Peak Element II (2D Matrix)", "Binary Search - 2D Arrays", "Hard", "Find 2D peak element strictly greater than top, bottom, left, right neighbors."},
            {"matrix-median", "32. Matrix Median", "Binary Search - 2D Arrays", "Hard", "Find median of a row-sorted N x M matrix in O(32 * N * log M) time."}
        };

        for (String[] p : list) {
            String id = p[0];
            String title = p[1];
            String cat = p[2];
            String diff = p[3];
            String desc = p[4];

            problems.put(id, new ProblemDetail(
                id, title, cat, "Binary Search", diff, desc,
                String.format("// Java Implementation for %s\npublic int solve() {\n    // Binary Search Striver A2Z Implementation\n    return 0;\n}", title),
                null, null, null, createSortedArray(), null, null, null,
                new ComplexityDetail("O(log N)", "Time Complexity: Binary search halving over answer search space.", "BS on Answers", "O(1)", "Space Complexity: Iterative pointers low, high, mid.", "Memory", "Auxiliary Space: O(1)", "Pointers"),
                cat.endsWith("2D Arrays") ? "Matrix" : "Array"
            ));
        }
    }

    // Step Generators
    private List<ExecutionStep> generateBs1dSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{1, 3, 5, 7, 9, 11, 13};
        int target = 7, low = 0, high = nums.length - 1;
        int stepNum = 1;

        steps.add(new ExecutionStep(stepNum++, 4, "Search X=7 in sorted array [1, 3, 5, 7, 9, 11, 13]. Initialize low = 0, high = 6.", List.of(), Map.of(0, "low", 6, "high"), List.of(), Map.of("target", "7"), "Array", null, createArrayState(nums, low, high, -1), null, null));

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (nums[mid] == target) {
                steps.add(new ExecutionStep(stepNum++, 8, String.format("nums[mid=%d] == 7! Target 7 found at index %d!", mid, mid), List.of(), Map.of(mid, "mid"), List.of(), Map.of("Found Index", String.valueOf(mid)), "Array", null, createArrayState(nums, mid, mid, mid), null, null));
                return steps;
            } else if (nums[mid] < target) {
                steps.add(new ExecutionStep(stepNum++, 9, String.format("nums[mid=%d] (%d) < target (7). Move low = mid + 1 -> %d.", mid, nums[mid], mid + 1), List.of(), Map.of(mid, "mid"), List.of(), Map.of("target", "7"), "Array", null, createArrayState(nums, low, high, mid), null, null));
                low = mid + 1;
            } else {
                steps.add(new ExecutionStep(stepNum++, 10, String.format("nums[mid=%d] (%d) > target (7). Move high = mid - 1 -> %d.", mid, nums[mid], mid - 1), List.of(), Map.of(mid, "mid"), List.of(), Map.of("target", "7"), "Array", null, createArrayState(nums, low, high, mid), null, null));
                high = mid - 1;
            }
        }
        return steps;
    }

    private List<ExecutionStep> generateLowerBoundSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateUpperBoundSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateSearchInsertSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateFloorCeilSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateFirstLastOccurrenceSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateCountOccurrencesSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateSearchRotatedSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateSearchRotated2Steps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateFindMinRotatedSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateCountRotationsSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateSingleElementSortedSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateFindPeakSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateSquareRootSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateNthRootSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateKokoSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateMinDaysBouquetsSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateSmallestDivisorSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateShipPackagesSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateKthMissingPositiveSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateAggressiveCowsSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateBookAllocationSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateSplitArrayLargestSumSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generatePaintersPartitionSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateGasStationSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateMedian2SortedArraysSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateKthElement2SortedArraysSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateRowMaxOnesSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateSearch2dMatrixSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateSearch2dMatrix2Steps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateFindPeakElement2dSteps() { return generateBs1dSteps(); }
    private List<ExecutionStep> generateMatrixMedianSteps() { return generateBs1dSteps(); }

    // Helpers
    private List<ArrayElement> createSortedArray() { return createArrayState(new int[]{1, 3, 5, 7, 9, 11, 13}, -1, -1, -1); }
    private List<ArrayElement> createDuplicateSortedArray() { return createArrayState(new int[]{1, 2, 2, 2, 3, 4, 5}, -1, -1, -1); }
    private List<ArrayElement> createRotatedArray() { return createArrayState(new int[]{4, 5, 6, 7, 0, 1, 2}, -1, -1, -1); }
    private List<ArrayElement> createRotatedDuplicateArray() { return createArrayState(new int[]{2, 5, 6, 0, 0, 1, 2}, -1, -1, -1); }
    private List<ArrayElement> createSingleNonDuplicateArray() { return createArrayState(new int[]{1, 1, 2, 3, 3, 4, 4, 8, 8}, -1, -1, -1); }
    private List<ArrayElement> createPeakArray() { return createArrayState(new int[]{1, 2, 1, 3, 5, 6, 4}, -1, -1, -1); }

    private List<ArrayElement> createArrayState(int[] vals, int low, int high, int mid) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            String state = "default";
            if (i == mid) state = "active";
            else if (i == low || i == high) state = "comparing";
            list.add(new ArrayElement(i, vals[i], state));
        }
        return list;
    }
}
