package com.dsa.ui.service;

import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SortingService {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public SortingService() {
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
            case "selection-sort": return generateSelectionSortSteps();
            case "bubble-sort": return generateBubbleSortSteps();
            case "insertion-sort": return generateInsertionSortSteps();
            case "merge-sort": return generateMergeSortSteps();
            case "quick-sort": return generateQuickSortSteps();
            default: return generateSelectionSortSteps();
        }
    }

    private void initProblems() {
        // 1. Selection Sort
        problems.put("selection-sort", new ProblemDetail(
            "selection-sort", "Selection Sort", "Sorting - Basics", "Sorting Algorithms", "Easy",
            "Selection Sort repeatedly scans the unsorted subarray to find the minimum element, then swaps it with the first unsorted element to extend the sorted prefix.",
            """
            // Java Selection Sort (Striver A2Z Sheet)
            public void selectionSort(int arr[], int n) {
                for (int i = 0; i < n - 1; i++) {
                    int mini = i;
                    for (int j = i + 1; j < n; j++) {
                        if (arr[j] < arr[mini]) {
                            mini = j;
                        }
                    }
                    // Swap arr[i] and arr[mini]
                    int temp = arr[i];
                    arr[i] = arr[mini];
                    arr[mini] = temp;
                }
            }
            """,
            null, null, null, createDefaultArray(), null, null, null,
            new ComplexityDetail(
                "O(N^2)",
                "Time Complexity: Outer loop runs N-1 times. For pass i, inner loop makes (N - i - 1) comparisons. Total comparisons = (N-1) + (N-2) + ... + 1 = N*(N-1)/2 = O(N^2).",
                "Why O(N^2) in Best, Average, and Worst Cases? Selection Sort ALWAYS scans all remaining unsorted elements to guarantee finding the absolute minimum, regardless of initial array ordering.",
                "O(1)",
                "Space Complexity: In-place sorting requiring O(1) auxiliary space.",
                "Why O(1)? Swaps elements directly in the input array using a single temporary variable `temp`.",
                "Auxiliary Space: O(1)",
                "Array Space: In-place O(1)"
            ),
            "Array"
        ));

        // 2. Bubble Sort
        problems.put("bubble-sort", new ProblemDetail(
            "bubble-sort", "Bubble Sort", "Sorting - Basics", "Sorting Algorithms", "Easy",
            "Repeatedly swap adjacent elements if they are in wrong order, bubbling maximum element to end.",
            """
            // Java Bubble Sort with Optimization (Striver A2Z Sheet)
            public void bubbleSort(int arr[], int n) {
                for (int i = n - 1; i >= 0; i--) {
                    boolean didSwap = false;
                    for (int j = 0; j <= i - 1; j++) {
                        if (arr[j] > arr[j + 1]) {
                            int temp = arr[j];
                            arr[j] = arr[j + 1];
                            arr[j + 1] = temp;
                            didSwap = true;
                        }
                    }
                    if (!didSwap) break; // Optimization: Best case O(N) for sorted array!
                }
            }
            """,
            null, null, null, createDefaultArray(), null, null, null,
            new ComplexityDetail(
                "O(N^2)",
                "Time Complexity: Worst & Average case O(N^2). Best case O(N) when array is already sorted.",
                "Why O(N) Best Case? Flag `didSwap` checks if any swap occurred; if none, breaks loop early.",
                "O(1)",
                "Space Complexity: In-place sorting algorithm.",
                "Why O(1)? Uses only single temp variable for swapping.",
                "Auxiliary Space: O(1)",
                "Array Space: O(N)"
            ),
            "Array"
        ));

        // 3. Insertion Sort
        problems.put("insertion-sort", new ProblemDetail(
            "insertion-sort", "Insertion Sort", "Sorting - Basics", "Sorting Algorithms", "Easy",
            "Build the sorted array one element at a time by inserting current element into correct position.",
            """
            // Java Insertion Sort (Striver A2Z Sheet)
            public void insertionSort(int arr[], int n) {
                for (int i = 0; i < n; i++) {
                    int j = i;
                    while (j > 0 && arr[j - 1] > arr[j]) {
                        int temp = arr[j];
                        arr[j] = arr[j - 1];
                        arr[j - 1] = temp;
                        j--;
                    }
                }
            }
            """,
            null, null, null, createDefaultArray(), null, null, null,
            new ComplexityDetail(
                "O(N^2)",
                "Time Complexity: Worst & Average O(N^2). Best case O(N) for already sorted array.",
                "Why Insertion Sort is adaptive? Inner while loop stops immediately when element finds its sorted position.",
                "O(1)",
                "Space Complexity: In-place O(1) space.",
                "Why O(1)? No auxiliary data structures needed.",
                "Auxiliary Space: O(1)",
                "Array Space: O(N)"
            ),
            "Array"
        ));

        // 4. Merge Sort
        problems.put("merge-sort", new ProblemDetail(
            "merge-sort", "Merge Sort (Divide & Conquer)", "Sorting - Divide & Conquer", "Sorting Algorithms", "Medium",
            "Divide array into two halves, recursively sort both halves, and merge sorted halves.",
            """
            // Java Merge Sort (Striver A2Z Sheet)
            public void mergeSort(int[] arr, int l, int r) {
                if (l >= r) return;
                int mid = (l + r) / 2;
                mergeSort(arr, l, mid);
                mergeSort(arr, mid + 1, r);
                merge(arr, l, mid, r);
            }

            private void merge(int[] arr, int low, int mid, int high) {
                ArrayList<Integer> temp = new ArrayList<>();
                int left = low, right = mid + 1;
                while (left <= mid && right <= high) {
                    if (arr[left] <= arr[right]) temp.add(arr[left++]);
                    else temp.add(arr[right++]);
                }
                while (left <= mid) temp.add(arr[left++]);
                while (right <= high) temp.add(arr[right++]);

                for (int i = low; i <= high; i++) {
                    arr[i] = temp.get(i - low);
                }
            }
            """,
            null, null, null, createDefaultArray(), null, null, null,
            new ComplexityDetail(
                "O(N log N)",
                "Time Complexity: Array is divided log N times (tree height log N). At each level, merging N elements takes O(N) time. Total = O(N log N) in all cases.",
                "Why O(N log N) is optimal comparison sort bound? Recurrence T(N) = 2T(N/2) + O(N) solves to O(N log N) by Master Theorem.",
                "O(N)",
                "Space Complexity: O(N) auxiliary temporary array space for merging + O(log N) recursion call stack depth.",
                "Why O(N)? Temporary array `temp` holds elements during two-way merge.",
                "Auxiliary Space: O(N) (Temp Merge Array)",
                "Call Stack Space: O(log N)"
            ),
            "Array"
        ));

        // 5. Quick Sort
        problems.put("quick-sort", new ProblemDetail(
            "quick-sort", "Quick Sort (Partitioning)", "Sorting - Divide & Conquer", "Sorting Algorithms", "Medium",
            "Pick a pivot element and partition array such that elements smaller than pivot are on left and larger on right.",
            """
            // Java Quick Sort (Striver A2Z Sheet)
            public void quickSort(int[] arr, int low, int high) {
                if (low < high) {
                    int pIndex = partition(arr, low, high);
                    quickSort(arr, low, pIndex - 1);
                    quickSort(arr, pIndex + 1, high);
                }
            }

            private int partition(int[] arr, int low, int high) {
                int pivot = arr[low];
                int i = low, j = high;

                while (i < j) {
                    while (arr[i] <= pivot && i <= high - 1) i++;
                    while (arr[j] > pivot && j >= low + 1) j--;
                    if (i < j) {
                        int temp = arr[i]; arr[i] = arr[j]; arr[j] = temp;
                    }
                }
                int temp = arr[low]; arr[low] = arr[j]; arr[j] = temp;
                return j;
            }
            """,
            null, null, null, createDefaultArray(), null, null, null,
            new ComplexityDetail(
                "O(N log N)",
                "Time Complexity: Average & Best case O(N log N). Worst case O(N^2) when pivot picked is smallest/largest (e.g. already sorted array).",
                "Why Quick Sort is preferred in practice? Cache friendly in-place partitioning with tiny constant factors.",
                "O(log N)",
                "Space Complexity: Auxiliary call stack depth O(log N) for balanced partitions.",
                "Why O(1) extra array memory? Performs swaps directly in-place.",
                "Auxiliary Space: O(log N) (Recursion Call Stack)",
                "Array Space: In-place O(1)"
            ),
            "Array"
        ));
    }

    // Dynamic Step Generators for Selection Sort
    private List<ExecutionStep> generateSelectionSortSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = new int[]{13, 46, 24, 52, 20, 9};
        int n = arr.length;
        int stepNum = 1;

        // Step 1: Initial State
        steps.add(new ExecutionStep(
            stepNum++, 43,
            "Input Array: [13, 46, 24, 52, 20, 9] (Length N = 6). Target: Sort in ascending order using Selection Sort.",
            List.of(), Map.of(), List.of(), Map.of("N", "6", "Algorithm", "Selection Sort"),
            "Array", null, createDetailedArrayState(arr, -1, -1, -1, 0), null, null
        ));

        for (int i = 0; i < n - 1; i++) {
            int mini = i;

            // Step: Pass start
            steps.add(new ExecutionStep(
                stepNum++, 45,
                String.format("Pass %d (i = %d): Set initial mini = %d (val = %d). Unsorted region is indices [%d..%d].", i + 1, i, mini, arr[mini], i, n - 1),
                List.of(), Map.of(), List.of(), Map.of("Pass", String.valueOf(i + 1), "i", String.valueOf(i), "mini", String.valueOf(mini), "arr[mini]", String.valueOf(arr[mini])),
                "Array", null, createDetailedArrayState(arr, i, mini, -1, i), null, null
            ));

            for (int j = i + 1; j < n; j++) {
                boolean isSmaller = arr[j] < arr[mini];
                if (isSmaller) {
                    int prevMini = mini;
                    mini = j;
                    steps.add(new ExecutionStep(
                        stepNum++, 48,
                        String.format("Compare arr[j=%d] (%d) with arr[mini=%d] (%d): %d < %d is TRUE! Update mini = %d.", j, arr[j], prevMini, arr[prevMini], arr[j], arr[prevMini], mini),
                        List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "j", String.valueOf(j), "prevMini", String.valueOf(prevMini), "newMini", String.valueOf(mini), "arr[mini]", String.valueOf(arr[mini])),
                        "Array", null, createDetailedArrayState(arr, i, mini, j, i), null, null
                    ));
                } else {
                    steps.add(new ExecutionStep(
                        stepNum++, 47,
                        String.format("Compare arr[j=%d] (%d) with arr[mini=%d] (%d): %d >= %d. mini remains %d (val %d).", j, arr[j], mini, arr[mini], arr[j], arr[mini], mini, arr[mini]),
                        List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "j", String.valueOf(j), "mini", String.valueOf(mini), "arr[mini]", String.valueOf(arr[mini])),
                        "Array", null, createDetailedArrayState(arr, i, mini, j, i), null, null
                    ));
                }
            }

            // Swap step
            if (mini != i) {
                int temp = arr[i];
                arr[i] = arr[mini];
                arr[mini] = temp;

                steps.add(new ExecutionStep(
                    stepNum++, 53,
                    String.format("Pass %d Complete: Swap arr[i=%d] (%d) with minimum arr[mini=%d] (%d). Element %d is placed in sorted position!", i + 1, i, temp, mini, arr[i], arr[i]),
                    List.of(), Map.of(), List.of(), Map.of("Swapped", String.format("arr[%d] (%d) <-> arr[%d] (%d)", i, temp, mini, arr[i]), "Sorted Prefix Length", String.valueOf(i + 1)),
                    "Array", null, createDetailedArrayState(arr, -1, i, mini, i + 1), null, null
                ));
            } else {
                steps.add(new ExecutionStep(
                    stepNum++, 52,
                    String.format("Pass %d Complete: arr[i=%d] (%d) is already the minimum. No swap needed. Element %d is in sorted position!", i + 1, i, arr[i], arr[i]),
                    List.of(), Map.of(), List.of(), Map.of("Sorted Prefix Length", String.valueOf(i + 1)),
                    "Array", null, createDetailedArrayState(arr, -1, i, -1, i + 1), null, null
                ));
            }
        }

        // Final step
        steps.add(new ExecutionStep(
            stepNum++, 56,
            "Selection Sort Complete! Input: [13, 46, 24, 52, 20, 9] -> Final Sorted Output: [9, 13, 20, 24, 46, 52].",
            List.of(), Map.of(), List.of(), Map.of("Status", "Sorted", "Output", "[9, 13, 20, 24, 46, 52]"),
            "Array", null, createDetailedArrayState(arr, -1, -1, -1, n), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateBubbleSortSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] vals = new int[]{13, 46, 24, 52, 20, 9};

        steps.add(new ExecutionStep(1, 4, "Initial array: [13, 46, 24, 52, 20, 9]", List.of(), Map.of(), List.of(), Map.of("Pass", "1"), "Array", null, createDefaultArray(), null, null));
        steps.add(new ExecutionStep(2, 7, "Compare adjacent 46 and 24 -> 46 > 24, swap!", List.of(), Map.of(), List.of(), Map.of("swap", "46 <-> 24"), "Array", null, createDetailedArrayState(new int[]{13, 24, 46, 52, 20, 9}, -1, 1, 2, 0), null, null));
        steps.add(new ExecutionStep(3, 7, "Compare adjacent 52 and 20 -> 52 > 20, swap!", List.of(), Map.of(), List.of(), Map.of("swap", "52 <-> 20"), "Array", null, createDetailedArrayState(new int[]{13, 24, 46, 20, 52, 9}, -1, 3, 4, 0), null, null));
        steps.add(new ExecutionStep(4, 7, "Bubble largest element 52 to last index!", List.of(), Map.of(), List.of(), Map.of("bubbled", "52"), "Array", null, createDetailedArrayState(new int[]{13, 24, 46, 20, 9, 52}, -1, 4, 5, 1), null, null));
        steps.add(new ExecutionStep(5, 12, "Bubble Sort Completed! Final Sorted Array: [9, 13, 20, 24, 46, 52]", List.of(), Map.of(), List.of(), Map.of("Status", "Sorted"), "Array", null, createDetailedArrayState(new int[]{9, 13, 20, 24, 46, 52}, -1, -1, -1, 6), null, null));

        return steps;
    }

    private List<ExecutionStep> generateInsertionSortSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] vals = new int[]{13, 46, 24, 52, 20, 9};

        steps.add(new ExecutionStep(1, 4, "Initial array: [13, 46, 24, 52, 20, 9]", List.of(), Map.of(), List.of(), Map.of("i", "0"), "Array", null, createDefaultArray(), null, null));
        steps.add(new ExecutionStep(2, 6, "Insert 24 into sorted part [13, 46] -> Shift 46 right, insert 24", List.of(), Map.of(), List.of(), Map.of("inserted", "24"), "Array", null, createDetailedArrayState(new int[]{13, 24, 46, 52, 20, 9}, -1, 1, 2, 0), null, null));
        steps.add(new ExecutionStep(3, 6, "Insert 20 into sorted part -> Shift 52, 46, 24 right, insert 20", List.of(), Map.of(), List.of(), Map.of("inserted", "20"), "Array", null, createDetailedArrayState(new int[]{13, 20, 24, 46, 52, 9}, -1, 1, 4, 0), null, null));
        steps.add(new ExecutionStep(4, 4, "Insertion Sort Completed! Final Sorted Array: [9, 13, 20, 24, 46, 52]", List.of(), Map.of(), List.of(), Map.of("Status", "Sorted"), "Array", null, createDetailedArrayState(new int[]{9, 13, 20, 24, 46, 52}, -1, -1, -1, 6), null, null));

        return steps;
    }

    private List<ExecutionStep> generateMergeSortSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        steps.add(new ExecutionStep(1, 4, "Divide array [13, 46, 24, 52, 20, 9] into left [13, 46, 24] and right [52, 20, 9]", List.of("mergeSort(0, 5)"), Map.of(), List.of(), Map.of("mid", "2"), "Array", null, createDefaultArray(), null, null));
        steps.add(new ExecutionStep(2, 10, "Merge sorted left [13, 24, 46] and sorted right [9, 20, 52]", List.of("merge(0, 2, 5)"), Map.of(), List.of(), Map.of("merge", "2-way"), "Array", null, createDetailedArrayState(new int[]{9, 13, 20, 24, 46, 52}, -1, 0, 5, 0), null, null));
        steps.add(new ExecutionStep(3, 5, "Merge Sort Completed! Final Array: [9, 13, 20, 24, 46, 52]", List.of(), Map.of(), List.of(), Map.of("Status", "Sorted"), "Array", null, createDetailedArrayState(new int[]{9, 13, 20, 24, 46, 52}, -1, -1, -1, 6), null, null));

        return steps;
    }

    private List<ExecutionStep> generateQuickSortSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        steps.add(new ExecutionStep(1, 4, "Quick Sort: Select pivot = arr[0] = 13", List.of("quickSort(0, 5)"), Map.of(), List.of(), Map.of("pivot", "13"), "Array", null, createDetailedArrayState(new int[]{13, 46, 24, 52, 20, 9}, 0, 0, -1, 0), null, null));
        steps.add(new ExecutionStep(2, 11, "Partitioning: Elements <= 13 on left, > 13 on right -> Pivot 13 placed at correct index 1", List.of("partition"), Map.of(), List.of(), Map.of("pivot_index", "1"), "Array", null, createDetailedArrayState(new int[]{9, 13, 24, 52, 20, 46}, 1, 1, -1, 0), null, null));
        steps.add(new ExecutionStep(3, 5, "Recursively partition left [9] and right [24, 52, 20, 46] -> Array Sorted!", List.of(), Map.of(), List.of(), Map.of("Status", "Sorted"), "Array", null, createDetailedArrayState(new int[]{9, 13, 20, 24, 46, 52}, -1, -1, -1, 6), null, null));

        return steps;
    }

    // Helper builders
    private List<ArrayElement> createDefaultArray() {
        int[] vals = new int[]{13, 46, 24, 52, 20, 9};
        return createDetailedArrayState(vals, -1, -1, -1, 0);
    }

    private List<ArrayElement> createDetailedArrayState(int[] vals, int iIndex, int miniIndex, int jIndex, int sortedUpTo) {
        List<ArrayElement> list = new ArrayList<>();
        for (int idx = 0; idx < vals.length; idx++) {
            String state = "default";
            if (idx < sortedUpTo) {
                state = "sorted";
            } else if (idx == miniIndex || idx == iIndex) {
                state = "pivot"; // Highlight mini or current pass start i
            } else if (idx == jIndex) {
                state = "comparing"; // Highlight comparing j element
            }
            list.add(new ArrayElement(idx, vals[idx], state));
        }
        return list;
    }
}
