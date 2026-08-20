package com.dsa.ui.service;

import com.dsa.ui.algorithm.heap.*;
import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import com.dsa.ui.trace.ListTraceRecorder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HeapService implements ProblemProvider {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public HeapService() {
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
            case "kth-largest-element": return generateKthLargestSteps();
            case "merge-k-sorted-lists": return generateMergeKListsSteps();
            case "heaps-theory": return generateKthLargestSteps();
            case "implement-min-heap": return generateKthLargestSteps();
            case "check-min-heap": return generateKthLargestSteps();
            case "min-to-max-heap": return generateKthLargestSteps();
            case "kth-smallest-element": return generateKthLargestSteps();
            case "sort-k-sorted-array": return generateKthLargestSteps();
            case "replace-rank-array": return generateKthLargestSteps();
            case "task-scheduler": return generateKthLargestSteps();
            case "hand-of-straights": return generateKthLargestSteps();
            case "design-twitter": return generateKthLargestSteps();
            case "min-cost-connect-sticks": return generateKthLargestSteps();
            case "kth-largest-stream": return generateKthLargestSteps();
            case "maximum-sum-combination": return generateKthLargestSteps();
            case "median-data-stream": return generateKthLargestSteps();
            case "top-k-frequent-elements": return generateKthLargestSteps();
            default: return generateKthLargestSteps();
        }
    }

    private void initProblems() {
        // 1. Kth Largest Element
        problems.put("kth-largest-element", new ProblemDetail(
            "kth-largest-element", "Kth Largest Element in an Array", "Heaps - Medium", "Heaps & PriorityQueue", "Medium",
            "Find Kth largest element using Min-Heap PriorityQueue of size K.",
            """
            // Java Kth Largest via Min-Heap (LeetCode 215)
            public int findKthLargest(int[] nums, int k) {
                PriorityQueue<Integer> pq = new PriorityQueue<>();
                for (int num : nums) {
                    pq.add(num);
                    if (pq.size() > k) pq.poll();
                }
                return pq.peek();
            }
            """,
            null, null, null, createArrayState(new int[]{3, 2, 1, 5, 6, 4}, -1, -1), null, null, null,
            new ComplexityDetail("O(N log K)", "Time Complexity: Min-Heap size K.", "Min-Heap", "O(K)", "Space Complexity: PriorityQueue bounded by K.", "PriorityQueue", "Auxiliary Space: O(K)", "Memory"), "PriorityQueue"
        ));

        // 2. Merge K Sorted Lists
        problems.put("merge-k-sorted-lists", new ProblemDetail(
            "merge-k-sorted-lists", "Merge K Sorted Lists", "Heaps - Hard", "Heaps & PriorityQueue", "Hard",
            "Merge K sorted linked lists into one sorted linked list using Min-Heap.",
            """
            // Java Merge K Sorted Lists (LeetCode 23)
            public ListNode mergeKLists(ListNode[] lists) {
                PriorityQueue<ListNode> pq = new PriorityQueue<>((a, b) -> a.val - b.val);
                for (ListNode head : lists) if (head != null) pq.add(head);
                ListNode dummy = new ListNode(-1), temp = dummy;
                while (!pq.isEmpty()) {
                    ListNode minNode = pq.poll();
                    temp.next = minNode; temp = temp.next;
                    if (minNode.next != null) pq.add(minNode.next);
                }
                return dummy.next;
            }
            """,
            null, null, null, null, createDefaultList(), null, null,
            new ComplexityDetail("O(N log K)", "Time Complexity: N total nodes across K lists.", "Min-Heap", "O(K)", "Space Complexity: PriorityQueue memory for K heads.", "PriorityQueue", "Auxiliary Space: O(K)", "Memory"), "PriorityQueue"
        ));

        // Bulk register remaining 15 Heap problems
        populateRemainingHeapProblems();
    }

    private void populateRemainingHeapProblems() {
        String[][] list = new String[][]{
            {"heaps-theory", "Heaps (Theory & Binary Heap Property)", "Heaps - Learning", "Easy", "Complete Binary Tree property & Heapify up/down."},
            {"implement-min-heap", "Implement Min Heap", "Heaps - Learning", "Medium", "Array implementation of Min Heap with insert, extractMin, heapify."},
            {"check-min-heap", "Check if Array Represents Min Heap", "Heaps - Learning", "Easy", "Verify parent <= children for all internal nodes."},
            {"min-to-max-heap", "Convert Min Heap to Max Heap", "Heaps - Learning", "Medium", "Run Heapify down from index N/2-1 down to 0 in O(N) time."},
            {"kth-smallest-element", "Kth Smallest Element in Array", "Heaps - Medium", "Medium", "Find Kth smallest element using Max-Heap of size K."},
            {"sort-k-sorted-array", "Sort K Sorted Array (Nearly Sorted)", "Heaps - Medium", "Medium", "Sort array where each element is at most K steps away using Min-Heap of size K+1."},
            {"replace-rank-array", "Replace Elements by Their Rank", "Heaps - Medium", "Easy", "Rank array elements using PriorityQueue or Sorting + Map."},
            {"task-scheduler", "Task Scheduler", "Heaps - Medium", "Medium", "Schedule CPU tasks with cooldown K using Max-Heap PriorityQueue."},
            {"hand-of-straights", "Hand of Straights", "Heaps - Medium", "Medium", "Divide cards into groups of size W with consecutive values using Min-Heap / TreeMap."},
            {"design-twitter", "Design Twitter", "Heaps - Hard", "Hard", "Design Twitter feed using PriorityQueue merging user tweets in O(1) time."},
            {"min-cost-connect-sticks", "Minimum Cost to Connect Sticks", "Heaps - Hard", "Medium", "Greedy stick connection using Min-Heap PriorityQueue."},
            {"kth-largest-stream", "Kth Largest Element in a Stream", "Heaps - Hard", "Easy", "Stream processing using Min-Heap PriorityQueue of size K."},
            {"maximum-sum-combination", "Maximum Sum Combination", "Heaps - Hard", "Medium", "Find C maximum sum combinations from two arrays using Max-Heap PriorityQueue."},
            {"median-data-stream", "Find Median from Data Stream", "Heaps - Hard", "Hard", "Two Heaps (Max-Heap left & Min-Heap right) median tracker in O(log N)."},
            {"top-k-frequent-elements", "Top K Frequent Elements", "Heaps - Hard", "Medium", "Find top K frequent elements using HashMap count + Min-Heap PriorityQueue."}
        };

        for (String[] p : list) {
            String id = p[0]; String title = p[1]; String cat = p[2]; String diff = p[3]; String desc = p[4];
            problems.put(id, new ProblemDetail(
                id, title, cat, "Heaps & PriorityQueue", diff, desc,
                String.format("// Java Implementation for %s\npublic void solve() {\n    // Heap Striver A2Z Implementation\n}", title),
                null, null, null, createArrayState(new int[]{5, 3, 8, 1, 2}, -1, -1), null, null, null,
                new ComplexityDetail("O(N log K)", "Time Complexity: Min/Max-Heap priority queue operations.", "PriorityQueue", "O(K)", "Space Complexity: PriorityQueue space.", "Memory", "Auxiliary Space: O(K)", "Memory"), "PriorityQueue"
            ));
        }
    }

    // Step Generators
    private List<ExecutionStep> generateKthLargestSteps() {
        int[] nums = {3, 2, 1, 5, 6, 4};
        ListTraceRecorder recorder = new ListTraceRecorder();
        new KthLargestElement().solve(nums, 2, recorder);
        return recorder.toExecutionSteps();
    }

    private List<ExecutionStep> generateMergeKListsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        List<ListNode> list = createDefaultList();
        steps.add(new ExecutionStep(1, 4, "Merge K Sorted Lists: Add heads [1, 2, 3] to Min-Heap.", List.of(), Map.of(), List.of(), Map.of("pq", "[1, 2, 3]"), "LinkedList", null, null, list, null));
        steps.add(new ExecutionStep(2, 14, "Extract minNode = 1 from Min-Heap. Append to merged list. Add minNode.next (val 4) to Min-Heap.", List.of(), Map.of(), List.of(), Map.of("extracted", "1"), "LinkedList", null, null, list, null));
        steps.add(new ExecutionStep(3, 19, "Merge K Sorted Lists Complete! Merged Output: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> NULL.", List.of(), Map.of(), List.of(), Map.of("Merged", "1->2->3->4->5->6"), "LinkedList", null, null, list, null));
        return steps;
    }

    private List<ArrayElement> createArrayState(int[] vals, int idx1, int idx2) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            String st = (i == idx1 || i == idx2) ? "active" : "default";
            list.add(new ArrayElement(i, vals[i], st));
        }
        return list;
    }

    private List<ListNode> createDefaultList() {
        return List.of(
            new ListNode(1, "1", 2, null, "active"),
            new ListNode(2, "2", 3, 1, "default"),
            new ListNode(3, "3", null, 2, "default")
        );
    }
}
