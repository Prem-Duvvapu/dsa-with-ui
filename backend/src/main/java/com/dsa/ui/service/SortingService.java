package com.dsa.ui.service;

import com.dsa.ui.algorithm.sorting.MergeSort;
import com.dsa.ui.algorithm.sorting.QuickSort;
import com.dsa.ui.model.*;
import com.dsa.ui.trace.ListTraceRecorder;
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
            "Repeatedly swap adjacent elements if they are in wrong order, bubbling maximum element to end of array in each pass.",
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
            "Build the sorted array one element at a time by inserting current element into correct position in sorted prefix.",
            """
            // Java Insertion Sort (Shifting with Key)
            public void insertionSort(int arr[]) {
                int n = arr.length;
                for (int i = 1; i < n; i++) {
                    int key = arr[i];
                    int j = i - 1;
                    
                    while (j >= 0 && arr[j] > key) {
                        arr[j + 1] = arr[j];
                        j--;
                    }
                    arr[j + 1] = key;
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
            "Merge Sort divides array into two halves recursively until base case (1 element), then merges sorted halves using 2 pointers.",
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
            null, null, createMergeSortTreeNodes(), createDefaultArray(), null, null, null,
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
            "Stack"
        ));

        // 5. Quick Sort
        problems.put("quick-sort", new ProblemDetail(
            "quick-sort", "Quick Sort (Partitioning)", "Sorting - Divide & Conquer", "Sorting Algorithms", "Medium",
            "Pick a pivot element and partition array such that elements smaller than pivot are on left and larger on right.",
            """
            // Java Quick Sort (Lomuto Partitioning with Pivot = arr[low])
            public void quickSort(int[] arr, int low, int high) {
                if (low < high) {
                    int pIndex = partition(arr, low, high);
                    quickSort(arr, low, pIndex - 1);
                    quickSort(arr, pIndex + 1, high);
                }
            }

            private int partition(int[] arr, int low, int high) {
                int pivot = arr[low];
                int i = low;

                for (int j = low + 1; j <= high; j++) {
                    if (arr[j] < pivot) {
                        i++;
                        swap(arr, i, j);
                    }
                }
                swap(arr, low, i);
                return i;
            }

            private void swap(int[] arr, int i, int j) {
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
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

    // Granular Selection Sort Step Generator
    private List<ExecutionStep> generateSelectionSortSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = new int[]{13, 46, 24, 52, 20, 9};
        int n = arr.length;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 43,
            "Input Array: [13, 46, 24, 52, 20, 9] (Length N = 6). Target: Sort in ascending order using Selection Sort.",
            List.of(), Map.of(), List.of(), Map.of("N", "6", "Algorithm", "Selection Sort"),
            "Array", null, createDetailedArrayState(arr, -1, -1, -1, 0), null, null
        ));

        for (int i = 0; i < n - 1; i++) {
            int mini = i;

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

        steps.add(new ExecutionStep(
            stepNum++, 56,
            "Selection Sort Complete! Input: [13, 46, 24, 52, 20, 9] -> Final Sorted Output: [9, 13, 20, 24, 46, 52].",
            List.of(), Map.of(), List.of(), Map.of("Status", "Sorted", "Output", "[9, 13, 20, 24, 46, 52]"),
            "Array", null, createDetailedArrayState(arr, -1, -1, -1, n), null, null
        ));

        return steps;
    }

    // Granular Bubble Sort Step Generator
    private List<ExecutionStep> generateBubbleSortSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = new int[]{13, 46, 24, 52, 20, 9};
        int n = arr.length;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 43,
            "Input Array: [13, 46, 24, 52, 20, 9] (N = 6). Target: Bubbling largest element to end in each pass.",
            List.of(), Map.of(), List.of(), Map.of("N", "6"),
            "Array", null, createDetailedArrayState(arr, -1, -1, -1, 0), null, null
        ));

        for (int i = n - 1; i >= 0; i--) {
            boolean didSwap = false;

            steps.add(new ExecutionStep(
                stepNum++, 44,
                String.format("Pass %d (i = %d): Bubbling largest element in unsorted range [0..%d] to index %d.", n - i, i, i, i),
                List.of(), Map.of(), List.of(), Map.of("Pass", String.valueOf(n - i), "i", String.valueOf(i)),
                "Array", null, createDetailedArrayState(arr, -1, -1, -1, n - 1 - i), null, null
            ));

            for (int j = 0; j <= i - 1; j++) {
                boolean needsSwap = arr[j] > arr[j + 1];
                if (needsSwap) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    didSwap = true;

                    steps.add(new ExecutionStep(
                        stepNum++, 48,
                        String.format("Compare arr[j=%d] (%d) > arr[j+1=%d] (%d): TRUE! Swap arr[%d] and arr[%d]. Array: %s.", j, temp, j + 1, arr[j], j, j + 1, Arrays.toString(arr)),
                        List.of(), Map.of(), List.of(), Map.of("j", String.valueOf(j), "swap", String.format("%d <-> %d", temp, arr[j])),
                        "Array", null, createDetailedArrayState(arr, j, j + 1, -1, n - 1 - i), null, null
                    ));
                } else {
                    steps.add(new ExecutionStep(
                        stepNum++, 47,
                        String.format("Compare arr[j=%d] (%d) > arr[j+1=%d] (%d): FALSE. Order is correct, no swap.", j, arr[j], j + 1, arr[j + 1]),
                        List.of(), Map.of(), List.of(), Map.of("j", String.valueOf(j), "arr[j]", String.valueOf(arr[j]), "arr[j+1]", String.valueOf(arr[j + 1])),
                        "Array", null, createDetailedArrayState(arr, j, j + 1, -1, n - 1 - i), null, null
                    ));
                }
            }

            if (!didSwap) {
                steps.add(new ExecutionStep(
                    stepNum++, 52,
                    String.format("Pass %d Optimization Check: No swaps occurred in entire pass! Array is already fully sorted. Breaking loop early!", n - i),
                    List.of(), Map.of(), List.of(), Map.of("didSwap", "false", "Status", "Sorted Early"),
                    "Array", null, createDetailedArrayState(arr, -1, -1, -1, n), null, null
                ));
                break;
            } else {
                steps.add(new ExecutionStep(
                    stepNum++, 51,
                    String.format("Pass %d Complete: Element %d bubbled to its final sorted position at index %d.", n - i, arr[i], i),
                    List.of(), Map.of(), List.of(), Map.of("Bubbled Element", String.valueOf(arr[i]), "Sorted Position", String.valueOf(i)),
                    "Array", null, createDetailedArrayState(arr, -1, -1, -1, n - i), null, null
                ));
            }
        }

        steps.add(new ExecutionStep(
            stepNum++, 53,
            "Bubble Sort Complete! Final Sorted Output: [9, 13, 20, 24, 46, 52].",
            List.of(), Map.of(), List.of(), Map.of("Status", "Sorted", "Output", "[9, 13, 20, 24, 46, 52]"),
            "Array", null, createDetailedArrayState(arr, -1, -1, -1, n), null, null
        ));

        return steps;
    }

    // Granular Insertion Sort Step Generator (Key-based Shifting)
    private List<ExecutionStep> generateInsertionSortSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = new int[]{13, 46, 24, 52, 20, 9};
        int n = arr.length;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 1,
            "Input Array: [13, 46, 24, 52, 20, 9] (N = 6). Target: Insert elements one by one into sorted prefix using key-based shifting.",
            List.of(), Map.of(), List.of(), Map.of("N", "6"),
            "Array", null, createDetailedArrayState(arr, -1, -1, -1, 1), null, null
        ));

        for (int i = 1; i < n; i++) {
            int key = arr[i];
            int j = i - 1;

            steps.add(new ExecutionStep(
                stepNum++, 4,
                String.format("Pass %d (i = %d): Set key = arr[%d] (%d). Compare with sorted prefix elements at indices [0..%d].", i, i, i, key, i - 1),
                List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "key", String.valueOf(key)),
                "Array", null, createDetailedArrayState(arr, i, -1, j, i), null, null
            ));

            while (j >= 0 && arr[j] > key) {
                steps.add(new ExecutionStep(
                    stepNum++, 8,
                    String.format("Compare arr[j=%d] (%d) > key (%d): TRUE! Shift arr[%d] (%d) right to arr[%d].", j, arr[j], key, j, arr[j], j + 1),
                    List.of(), Map.of(), List.of(), Map.of("j", String.valueOf(j), "arr[j]", String.valueOf(arr[j]), "key", String.valueOf(key), "shiftedTo", String.valueOf(j + 1)),
                    "Array", null, createDetailedArrayState(arr, j, -1, j + 1, i), null, null
                ));

                arr[j + 1] = arr[j];
                j--;
            }

            arr[j + 1] = key;

            steps.add(new ExecutionStep(
                stepNum++, 11,
                String.format("Insert key (%d) at index %d (arr[j+1]). Sorted prefix length is now %d.", key, j + 1, i + 1),
                List.of(), Map.of(), List.of(), Map.of("key", String.valueOf(key), "insertedAt", String.valueOf(j + 1), "Sorted Prefix", String.valueOf(i + 1)),
                "Array", null, createDetailedArrayState(arr, -1, j + 1, -1, i + 1), null, null
            ));
        }

        steps.add(new ExecutionStep(
            stepNum++, 13,
            "Insertion Sort Complete! Final Sorted Output: [9, 13, 20, 24, 46, 52].",
            List.of(), Map.of(), List.of(), Map.of("Status", "Sorted", "Output", "[9, 13, 20, 24, 46, 52]"),
            "Array", null, createDetailedArrayState(arr, -1, -1, -1, n), null, null
        ));

        return steps;
    }

    // Full Execution Trace Merge Sort Generator using TraceRecorder
    private List<ExecutionStep> generateMergeSortSteps() {
        int[] arr = new int[]{13, 46, 24, 52, 20, 9};
        ListTraceRecorder recorder = new ListTraceRecorder();
        new MergeSort().solve(arr, recorder);
        return recorder.toExecutionSteps();
    }

    // Full Execution Trace Quick Sort Generator using TraceRecorder
    private List<ExecutionStep> generateQuickSortSteps() {
        int[] arr = new int[]{13, 46, 24, 52, 20, 9};
        ListTraceRecorder recorder = new ListTraceRecorder();
        new QuickSort().solve(arr, recorder);
        return recorder.toExecutionSteps();
    }

    // Helper tree nodes for Merge Sort Recursion Tree
    private List<TreeNode> createMergeSortTreeNodes() {
        return List.of(
            new TreeNode(1, "[0..5]:13,46,24,52,20,9", 190, 35, 2, 3, "unvisited"),
            new TreeNode(2, "[0..2]:13,46,24", 100, 90, 4, 5, "unvisited"),
            new TreeNode(3, "[3..5]:52,20,9", 280, 90, 6, 7, "unvisited"),
            new TreeNode(4, "[0..1]:13,46", 60, 145, 8, 9, "unvisited"),
            new TreeNode(5, "[2]:24", 140, 145, null, null, "unvisited"),
            new TreeNode(6, "[3..4]:52,20", 240, 145, 10, 11, "unvisited"),
            new TreeNode(7, "[5]:9", 320, 145, null, null, "unvisited"),
            new TreeNode(8, "[0]:13", 35, 200, null, null, "unvisited"),
            new TreeNode(9, "[1]:46", 85, 200, null, null, "unvisited"),
            new TreeNode(10, "[3]:52", 215, 200, null, null, "unvisited"),
            new TreeNode(11, "[4]:20", 265, 200, null, null, "unvisited")
        );
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
                state = "pivot";
            } else if (idx == jIndex) {
                state = "comparing";
            }
            list.add(new ArrayElement(idx, vals[idx], state));
        }
        return list;
    }
}
