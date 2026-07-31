package com.dsa.ui.service;

import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HeapService {

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
            default: return generateKthLargestSteps();
        }
    }

    private void initProblems() {
        // 1. Kth Largest Element in an Array
        problems.put("kth-largest-element", new ProblemDetail(
            "kth-largest-element", "Kth Largest Element in an Array", "Heaps - Min-Heap", "Heaps & PriorityQueue", "Medium",
            "Find the Kth largest element in an unsorted array using a Min-Heap PriorityQueue of size K.",
            """
            // Java Kth Largest via Min-Heap (LeetCode 215)
            public int findKthLargest(int[] nums, int k) {
                PriorityQueue<Integer> pq = new PriorityQueue<>(); // Min-Heap
                for (int num : nums) {
                    pq.add(num);
                    if (pq.size() > k) {
                        pq.poll(); // Evict smallest element!
                    }
                }
                return pq.peek(); // Top of Min-Heap is Kth largest!
            }
            """,
            null, null, null, createArrayState(new int[]{3, 2, 1, 5, 6, 4}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N log K)",
                "Time Complexity: Single pass iteration over N elements. Each add/poll in PriorityQueue of max size K takes O(log K) time.",
                "Why Min-Heap of size K? Maintaining exactly K largest elements in Min-Heap ensures top element `pq.peek()` is the Kth largest element.",
                "O(K)",
                "Space Complexity: PriorityQueue auxiliary memory bounded by K.",
                "Why O(K)? Never stores more than K elements simultaneously.",
                "Auxiliary Space: O(K) (Min-Heap)",
                "Return Value: O(1)"
            ),
            "PriorityQueue"
        ));

        // 2. Merge K Sorted Lists
        problems.put("merge-k-sorted-lists", new ProblemDetail(
            "merge-k-sorted-lists", "Merge K Sorted Lists", "Heaps - Hard", "Heaps & PriorityQueue", "Hard",
            "Merge K sorted linked lists into one sorted linked list using Min-Heap PriorityQueue.",
            """
            // Java Merge K Sorted Lists via Min-Heap (LeetCode 23)
            public ListNode mergeKLists(ListNode[] lists) {
                if (lists == null || lists.length == 0) return null;
                PriorityQueue<ListNode> pq = new PriorityQueue<>((a, b) -> a.val - b.val);

                for (ListNode head : lists) {
                    if (head != null) pq.add(head);
                }

                ListNode dummy = new ListNode(-1);
                ListNode temp = dummy;

                while (!pq.isEmpty()) {
                    ListNode minNode = pq.poll();
                    temp.next = minNode;
                    temp = temp.next;

                    if (minNode.next != null) pq.add(minNode.next);
                }
                return dummy.next;
            }
            """,
            null, null, null, null, createDefaultList(), null, null,
            new ComplexityDetail(
                "O(N log K)",
                "Time Complexity: Total N nodes across K lists. PriorityQueue insertion and extraction take O(log K) per node.",
                "Why Min-Heap for K lists? Comparing heads of K lists takes O(log K) time per node extraction instead of O(K) linear scanning.",
                "O(K)",
                "Space Complexity: PriorityQueue memory holding at most K node references simultaneously.",
                "Why O(K)? Stores at most one head pointer from each of the K lists.",
                "Auxiliary Space: O(K) (Min-Heap)",
                "Merged List: Reused in-place O(1)"
            ),
            "PriorityQueue"
        ));
    }

    private List<ExecutionStep> generateKthLargestSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] nums = new int[]{3, 2, 1, 5, 6, 4};
        int k = 2;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Kth Largest Element: Array = [3, 2, 1, 5, 6, 4], K = 2. Initialize Min-Heap of max size K.",
            List.of(), Map.of(), List.of(), Map.of("K", "2", "heap", "[]"),
            "Array", null, createArrayState(nums, -1, -1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 7,
            "Process elements 3, 2, 1, 5: Add to Min-Heap. Size > 2 -> Evict smallest elements (1, 2). Heap contents: [3, 5].",
            List.of(), Map.of(), List.of(), Map.of("heap", "[3, 5]"),
            "Array", null, createArrayState(nums, 3, -1), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 7,
            "Process elements 6, 4: Add to Min-Heap. Evict smallest (3, 4). Heap contents: [5, 6].",
            List.of(), Map.of(), List.of(), Map.of("heap", "[5, 6]"),
            "Array", null, createArrayState(nums, 4, 5), null, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 10,
            "Kth Largest Complete! Top of Min-Heap `pq.peek()` = 5. The 2nd Largest Element in Array is 5!",
            List.of(), Map.of(), List.of(), Map.of("2nd Largest", "5"),
            "Array", null, createArrayState(nums, 3, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateMergeKListsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        List<ListNode> list = createDefaultList();
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Merge K Sorted Lists: K = 3 lists. Add heads [1, 2, 3] to Min-Heap.",
            List.of(), Map.of(), List.of(), Map.of("pq_size", "3", "heads", "[1, 2, 3]"),
            "LinkedList", null, null, list, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 14,
            "Extract minNode = 1 from Min-Heap. Append to merged list. Add minNode.next (val 4) to Min-Heap.",
            List.of(), Map.of(), List.of(), Map.of("extracted", "1", "pq", "[2, 3, 4]"),
            "LinkedList", null, null, list, null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 19,
            "Merge K Sorted Lists Complete! Output Merged List: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> NULL.",
            List.of(), Map.of(), List.of(), Map.of("Merged Output", "1->2->3->4->5->6"),
            "LinkedList", null, null, list, null
        ));

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
