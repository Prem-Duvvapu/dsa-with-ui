package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SortingService implements ProblemProvider {

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
            // All five have real tracers now (tracer/impl). Refuse rather than let
            // default: serve another sorting algorithm's steps under these ids.
            case "selection-sort":
            case "bubble-sort":
            case "insertion-sort":
            case "merge-sort":
            case "quick-sort":
                throw new LegacyTraceRetiredException(problemId);
            default: throw new LegacyTraceRetiredException(problemId);
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
            // The tracer emits DsType.ARRAY (the merge itself is shown as in-place array
            // swaps/writes, matching the other four sorts) - this catalogue entry previously
            // said "Stack", which CatalogTracerMetadataTest now catches as a mismatch.
            "Array"
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
