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

    // Granular Insertion Sort Step Generator
    private List<ExecutionStep> generateInsertionSortSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = new int[]{13, 46, 24, 52, 20, 9};
        int n = arr.length;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 43,
            "Input Array: [13, 46, 24, 52, 20, 9] (N = 6). Target: Insert elements one by one into sorted prefix.",
            List.of(), Map.of(), List.of(), Map.of("N", "6"),
            "Array", null, createDetailedArrayState(arr, -1, -1, -1, 1), null, null
        ));

        for (int i = 0; i < n; i++) {
            int j = i;

            steps.add(new ExecutionStep(
                stepNum++, 44,
                String.format("Pass %d (i = %d): Pick element arr[%d] (%d) to insert into sorted prefix [0..%d].", i + 1, i, i, arr[i], Math.max(0, i - 1)),
                List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "val", String.valueOf(arr[i])),
                "Array", null, createDetailedArrayState(arr, i, -1, -1, i), null, null
            ));

            while (j > 0 && arr[j - 1] > arr[j]) {
                int temp = arr[j];
                arr[j] = arr[j - 1];
                arr[j - 1] = temp;

                steps.add(new ExecutionStep(
                    stepNum++, 47,
                    String.format("Compare arr[j-1=%d] (%d) > arr[j=%d] (%d): TRUE! Shift %d right and move %d left. Array: %s.", j - 1, arr[j], j, temp, arr[j], temp, Arrays.toString(arr)),
                    List.of(), Map.of(), List.of(), Map.of("j", String.valueOf(j), "shift", String.format("%d <-> %d", arr[j], temp)),
                    "Array", null, createDetailedArrayState(arr, j - 1, j, -1, i), null, null
                ));
                j--;
            }

            steps.add(new ExecutionStep(
                stepNum++, 50,
                String.format("Pass %d Complete: Element %d inserted at its correct position index %d. Sorted prefix length: %d.", i + 1, arr[j], j, i + 1),
                List.of(), Map.of(), List.of(), Map.of("Inserted At", String.valueOf(j), "Sorted Prefix", String.valueOf(i + 1)),
                "Array", null, createDetailedArrayState(arr, -1, -1, -1, i + 1), null, null
            ));
        }

        steps.add(new ExecutionStep(
            stepNum++, 52,
            "Insertion Sort Complete! Final Sorted Output: [9, 13, 20, 24, 46, 52].",
            List.of(), Map.of(), List.of(), Map.of("Status", "Sorted", "Output", "[9, 13, 20, 24, 46, 52]"),
            "Array", null, createDetailedArrayState(arr, -1, -1, -1, n), null, null
        ));

        return steps;
    }

    // Comprehensive Granular Merge Sort Step Generator with Recursion Call Tree!
    private List<ExecutionStep> generateMergeSortSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = new int[]{13, 46, 24, 52, 20, 9};
        Map<Integer, String> nodeStates = new HashMap<>();
        int stepNum = 1;

        // 1. Initial State
        nodeStates.put(1, "calling");
        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Input Array: [13, 46, 24, 52, 20, 9] (N = 6). Call Root mergeSort(l=0, r=5). Recursion Tree Level 0.",
            List.of("mergeSort(0, 5)"), new HashMap<>(nodeStates), List.of(), Map.of("Root Call", "ms(0,5)", "Call Stack Depth", "1"),
            "Stack", null, createDetailedArrayState(arr, -1, -1, -1, 0), null, null
        ));

        // 2. Divide Left Half ms(0, 2)
        nodeStates.put(2, "calling");
        steps.add(new ExecutionStep(
            stepNum++, 6,
            "Divide Left: Call mergeSort(0, 2) for subarray [13, 46, 24]. Call Stack Depth = 2.",
            List.of("mergeSort(0, 5)", "mergeSort(0, 2)"), new HashMap<>(nodeStates), List.of(), Map.of("Subarray", "[13, 46, 24]", "mid", "1"),
            "Stack", null, createDetailedArrayState(arr, 0, 2, -1, 0), null, null
        ));

        // 3. Divide Left Half ms(0, 1)
        nodeStates.put(4, "calling");
        steps.add(new ExecutionStep(
            stepNum++, 6,
            "Divide Left: Call mergeSort(0, 1) for subarray [13, 46]. Call Stack Depth = 3.",
            List.of("mergeSort(0, 5)", "mergeSort(0, 2)", "mergeSort(0, 1)"), new HashMap<>(nodeStates), List.of(), Map.of("Subarray", "[13, 46]", "mid", "0"),
            "Stack", null, createDetailedArrayState(arr, 0, 1, -1, 0), null, null
        ));

        // 4. Base Case ms(0, 0)
        nodeStates.put(8, "visited");
        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Base Case: mergeSort(0, 0) for single element [13] (l >= r). Single element is already sorted!",
            List.of("mergeSort(0, 5)", "mergeSort(0, 2)", "mergeSort(0, 1)", "mergeSort(0, 0)"), new HashMap<>(nodeStates), List.of(), Map.of("Base Case", "ms(0,0)", "val", "13"),
            "Stack", null, createDetailedArrayState(arr, 0, -1, -1, 0), null, null
        ));

        // 5. Base Case ms(1, 1)
        nodeStates.put(9, "visited");
        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Base Case: mergeSort(1, 1) for single element [46] (l >= r). Single element is already sorted!",
            List.of("mergeSort(0, 5)", "mergeSort(0, 2)", "mergeSort(0, 1)", "mergeSort(1, 1)"), new HashMap<>(nodeStates), List.of(), Map.of("Base Case", "ms(1,1)", "val", "46"),
            "Stack", null, createDetailedArrayState(arr, 1, -1, -1, 0), null, null
        ));

        // 6. Merge ms(0, 1)
        nodeStates.put(4, "merging");
        steps.add(new ExecutionStep(
            stepNum++, 11,
            "Merge Phase merge(0, 0, 1): Compare 2 sorted halves [13] and [46] -> Combine to [13, 46]. Node ms(0,1) merged!",
            List.of("merge(0, 0, 1)"), new HashMap<>(nodeStates), List.of(), Map.of("Merged Subarray", "[13, 46]"),
            "Stack", null, createDetailedArrayState(arr, 0, 1, -1, 0), null, null
        ));
        nodeStates.put(4, "visited");

        // 7. Base Case ms(2, 2)
        nodeStates.put(5, "visited");
        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Base Case: mergeSort(2, 2) for single element [24]. Single element is already sorted!",
            List.of("mergeSort(0, 5)", "mergeSort(0, 2)", "mergeSort(2, 2)"), new HashMap<>(nodeStates), List.of(), Map.of("Base Case", "ms(2,2)", "val", "24"),
            "Stack", null, createDetailedArrayState(arr, 2, -1, -1, 0), null, null
        ));

        // 8. Merge ms(0, 2)
        int[] arrLeftMerged = new int[]{13, 24, 46, 52, 20, 9};
        nodeStates.put(2, "merging");
        steps.add(new ExecutionStep(
            stepNum++, 11,
            "Merge Phase merge(0, 1, 2): Compare left sorted [13, 46] and right sorted [24] -> Combine to [13, 24, 46]. Node ms(0,2) merged!",
            List.of("merge(0, 1, 2)"), new HashMap<>(nodeStates), List.of(), Map.of("Merged Subarray", "[13, 24, 46]"),
            "Stack", null, createDetailedArrayState(arrLeftMerged, 0, 2, -1, 0), null, null
        ));
        nodeStates.put(2, "visited");

        // 9. Divide Right Half ms(3, 5)
        nodeStates.put(3, "calling");
        steps.add(new ExecutionStep(
            stepNum++, 6,
            "Divide Right: Call mergeSort(3, 5) for subarray [52, 20, 9]. Call Stack Depth = 2.",
            List.of("mergeSort(0, 5)", "mergeSort(3, 5)"), new HashMap<>(nodeStates), List.of(), Map.of("Subarray", "[52, 20, 9]", "mid", "4"),
            "Stack", null, createDetailedArrayState(arrLeftMerged, 3, 5, -1, 0), null, null
        ));

        // 10. Divide Left Half of Right ms(3, 4)
        nodeStates.put(6, "calling");
        steps.add(new ExecutionStep(
            stepNum++, 6,
            "Divide Left: Call mergeSort(3, 4) for subarray [52, 20]. Call Stack Depth = 3.",
            List.of("mergeSort(0, 5)", "mergeSort(3, 5)", "mergeSort(3, 4)"), new HashMap<>(nodeStates), List.of(), Map.of("Subarray", "[52, 20]", "mid", "3"),
            "Stack", null, createDetailedArrayState(arrLeftMerged, 3, 4, -1, 0), null, null
        ));

        // 11. Base Case ms(3, 3)
        nodeStates.put(10, "visited");
        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Base Case: mergeSort(3, 3) for single element [52]. Single element is already sorted!",
            List.of("mergeSort(0, 5)", "mergeSort(3, 5)", "mergeSort(3, 4)", "mergeSort(3, 3)"), new HashMap<>(nodeStates), List.of(), Map.of("Base Case", "ms(3,3)", "val", "52"),
            "Stack", null, createDetailedArrayState(arrLeftMerged, 3, -1, -1, 0), null, null
        ));

        // 12. Base Case ms(4, 4)
        nodeStates.put(11, "visited");
        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Base Case: mergeSort(4, 4) for single element [20]. Single element is already sorted!",
            List.of("mergeSort(0, 5)", "mergeSort(3, 5)", "mergeSort(3, 4)", "mergeSort(4, 4)"), new HashMap<>(nodeStates), List.of(), Map.of("Base Case", "ms(4,4)", "val", "20"),
            "Stack", null, createDetailedArrayState(arrLeftMerged, 4, -1, -1, 0), null, null
        ));

        // 13. Merge ms(3, 4)
        int[] arrSubRight = new int[]{13, 24, 46, 20, 52, 9};
        nodeStates.put(6, "merging");
        steps.add(new ExecutionStep(
            stepNum++, 11,
            "Merge Phase merge(3, 3, 4): Compare [52] and [20] -> Combine to [20, 52]. Node ms(3,4) merged!",
            List.of("merge(3, 3, 4)"), new HashMap<>(nodeStates), List.of(), Map.of("Merged Subarray", "[20, 52]"),
            "Stack", null, createDetailedArrayState(arrSubRight, 3, 4, -1, 0), null, null
        ));
        nodeStates.put(6, "visited");

        // 14. Base Case ms(5, 5)
        nodeStates.put(7, "visited");
        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Base Case: mergeSort(5, 5) for single element [9]. Single element is already sorted!",
            List.of("mergeSort(0, 5)", "mergeSort(3, 5)", "mergeSort(5, 5)"), new HashMap<>(nodeStates), List.of(), Map.of("Base Case", "ms(5,5)", "val", "9"),
            "Stack", null, createDetailedArrayState(arrSubRight, 5, -1, -1, 0), null, null
        ));

        // 15. Merge ms(3, 5)
        int[] arrRightMerged = new int[]{13, 24, 46, 9, 20, 52};
        nodeStates.put(3, "merging");
        steps.add(new ExecutionStep(
            stepNum++, 11,
            "Merge Phase merge(3, 4, 5): Compare left sorted [20, 52] and right sorted [9] -> Combine to [9, 20, 52]. Node ms(3,5) merged!",
            List.of("merge(3, 4, 5)"), new HashMap<>(nodeStates), List.of(), Map.of("Merged Subarray", "[9, 20, 52]"),
            "Stack", null, createDetailedArrayState(arrRightMerged, 3, 5, -1, 0), null, null
        ));
        nodeStates.put(3, "visited");

        // 16. Final Root Merge ms(0, 5)
        int[] finalSorted = new int[]{9, 13, 20, 24, 46, 52};
        nodeStates.put(1, "merging");
        steps.add(new ExecutionStep(
            stepNum++, 11,
            "FINAL ROOT MERGE merge(0, 2, 5): Compare left sorted [13, 24, 46] and right sorted [9, 20, 52] using 2 pointers. Combine to fully sorted array [9, 13, 20, 24, 46, 52]!",
            List.of("merge(0, 2, 5)"), new HashMap<>(nodeStates), List.of(), Map.of("Final Merged Array", "[9, 13, 20, 24, 46, 52]"),
            "Stack", null, createDetailedArrayState(finalSorted, 0, 5, -1, 6), null, null
        ));
        nodeStates.put(1, "visited");

        // 17. Final Completion
        steps.add(new ExecutionStep(
            stepNum++, 16,
            "Merge Sort Complete! Recursion Tree fully executed & merged. Output: [9, 13, 20, 24, 46, 52].",
            List.of(), new HashMap<>(nodeStates), List.of(), Map.of("Status", "Sorted", "Output", "[9, 13, 20, 24, 46, 52]"),
            "Stack", null, createDetailedArrayState(finalSorted, -1, -1, -1, 6), null, null
        ));

        return steps;
    }

    // Granular Quick Sort Step Generator
    private List<ExecutionStep> generateQuickSortSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] arr = new int[]{13, 46, 24, 52, 20, 9};
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Input Array: [13, 46, 24, 52, 20, 9]. Quick Sort selects a pivot element to partition array in-place.",
            List.of("quickSort(0, 5)"), Map.of(), List.of(), Map.of("low", "0", "high", "5"),
            "Array", null, createDetailedArrayState(arr, -1, -1, -1, 0), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 10,
            "Partition Pass 1: Select pivot = arr[low=0] (13). Pointer i=0 scans right for elements > 13, pointer j=5 scans left for elements <= 13.",
            List.of("partition(0, 5)"), Map.of(), List.of(), Map.of("pivot", "13", "i", "0", "j", "5"),
            "Array", null, createDetailedArrayState(arr, 0, 0, 5, 0), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 18,
            "Swap Pivot: Place pivot 13 at its correct sorted partition index j=1. Array: [9, 13, 24, 52, 20, 46].",
            List.of("partition complete"), Map.of(), List.of(), Map.of("Pivot Index", "1", "Pivot Val", "13"),
            "Array", null, createDetailedArrayState(new int[]{9, 13, 24, 52, 20, 46}, 1, 1, -1, 0), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 5,
            "Recursively partition Left sub-array [9] and Right sub-array [24, 52, 20, 46]...",
            List.of("quickSort(2, 5)"), Map.of(), List.of(), Map.of("Sub-Array", "[24, 52, 20, 46]"),
            "Array", null, createDetailedArrayState(new int[]{9, 13, 20, 24, 46, 52}, -1, -1, -1, 6), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 7,
            "Quick Sort Complete! Final Sorted Output: [9, 13, 20, 24, 46, 52].",
            List.of(), Map.of(), List.of(), Map.of("Status", "Sorted", "Output", "[9, 13, 20, 24, 46, 52]"),
            "Array", null, createDetailedArrayState(new int[]{9, 13, 20, 24, 46, 52}, -1, -1, -1, 6), null, null
        ));

        return steps;
    }

    // Helper tree nodes for Merge Sort Recursion Tree
    private List<TreeNode> createMergeSortTreeNodes() {
        return List.of(
            new TreeNode(1, "ms(0,5)", 190, 35, 2, 3, "unvisited"),
            new TreeNode(2, "ms(0,2)", 100, 90, 4, 5, "unvisited"),
            new TreeNode(3, "ms(3,5)", 280, 90, 6, 7, "unvisited"),
            new TreeNode(4, "ms(0,1)", 60, 145, 8, 9, "unvisited"),
            new TreeNode(5, "ms(2,2)", 140, 145, null, null, "unvisited"),
            new TreeNode(6, "ms(3,4)", 240, 145, 10, 11, "unvisited"),
            new TreeNode(7, "ms(5,5)", 320, 145, null, null, "unvisited"),
            new TreeNode(8, "ms(0,0)", 35, 200, null, null, "unvisited"),
            new TreeNode(9, "ms(1,1)", 85, 200, null, null, "unvisited"),
            new TreeNode(10, "ms(3,3)", 215, 200, null, null, "unvisited"),
            new TreeNode(11, "ms(4,4)", 265, 200, null, null, "unvisited")
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
