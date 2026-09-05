package com.dsa.ui.service;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LinkedListService implements ProblemProvider {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public LinkedListService() {
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
            case "intro-singly-ll": return generateIntroSinglySteps();
            case "insert-head-ll": return generateInsertHeadSteps();
            case "delete-head-ll": return generateDeleteHeadSteps();
            case "length-ll": return generateLengthLlSteps();
            case "search-ll": return generateSearchLlSteps();
            case "intro-doubly-ll": return generateIntroDoublySteps();
            case "insert-head-dll": return generateInsertHeadDllSteps();
            case "delete-head-dll": return generateDeleteHeadDllSteps();
            case "reverse-dll": return generateReverseDllSteps();
            case "middle-linked-list": return generateMiddleSteps();
            // reverse-linked-list has a real tracer (tracer/impl) and has for a while -
            // this case was never updated to refuse, so its legacy endpoint has been
            // serving a real trace by coincidence rather than by guard. Fixed now.
            case "reverse-linked-list":
                throw new LegacyTraceRetiredException(problemId);
            case "reverse-ll-recursive": return generateReverseRecursiveSteps();
            case "detect-loop-linked-list": return generateDetectLoopSteps();
            // find-starting-point-loop and reverse-ll-group-k have real tracers
            // (tracer/impl) now. Refuse rather than let default: serve
            // generateReverseSteps()'s unrelated steps under these ids.
            case "find-starting-point-loop":
            case "reverse-ll-group-k":
                throw new LegacyTraceRetiredException(problemId);
            case "length-of-loop-ll": return generateLengthOfLoopSteps();
            case "palindrome-ll": return generatePalindromeLlSteps();
            case "segregate-odd-even-ll": return generateSegregateOddEvenSteps();
            case "remove-nth-from-back": return generateRemoveNthBackSteps();
            case "delete-middle-node-ll": return generateDeleteMiddleNodeSteps();
            case "sort-ll": return generateSortLlSteps();
            case "sort-012-ll": return generateSort012LlSteps();
            case "intersection-point-y-ll": return generateIntersectionPointYSteps();
            case "add-one-to-number-ll": return generateAddOneToNumberSteps();
            case "add-two-numbers-ll": return generateAddTwoNumbersSteps();
            case "delete-occurrences-key-dll": return generateDeleteOccurrencesKeyDllSteps();
            case "pairs-given-sum-dll": return generatePairsGivenSumDllSteps();
            case "remove-duplicates-sorted-dll": return generateRemoveDuplicatesSortedDllSteps();
            case "rotate-ll": return generateRotateLlSteps();
            // flattening-ll and clone-ll-random-pointer have real tracers (tracer/impl)
            // now. Refuse rather than let default: serve generateReverseSteps()'s
            // unrelated steps under these ids.
            case "flattening-ll":
            case "clone-ll-random-pointer":
                throw new LegacyTraceRetiredException(problemId);
            default: return generateReverseSteps();
        }
    }

    private void initProblems() {
        // 1. Reverse Linked List
        problems.put("reverse-linked-list", new ProblemDetail(
            "reverse-linked-list", "Reverse Linked List [Iterative]", "Linked List - Easy", "Linked List", "Easy",
            "Reverse a singly linked list using 3 pointers (prev, curr, next).",
            """
            // Java Reverse Linked List (LeetCode 206)
            public ListNode reverseList(ListNode head) {
                ListNode prev = null, curr = head;
                while (curr != null) {
                    ListNode next = curr.next;
                    curr.next = prev; prev = curr; curr = next;
                }
                return prev;
            }
            """,
            null, null, null, null, createDefaultList(), null, null,
            new ComplexityDetail("O(N)", "Time Complexity: Single pass iteration over N nodes.", "Pointer Reversal", "O(1)", "Space Complexity: O(1) auxiliary space.", "Pointers", "Auxiliary Space: O(1)", "Memory"), "LinkedList"
        ));

        // 2. Middle of Linked List
        problems.put("middle-linked-list", new ProblemDetail(
            "middle-linked-list", "Middle of Linked List [Tortoise-Hare]", "Linked List - Easy", "Linked List", "Easy",
            "Find the middle node of a singly linked list using Fast and Slow pointers.",
            """
            // Java Tortoise-Hare Algorithm (LeetCode 876)
            public ListNode middleNode(ListNode head) {
                ListNode slow = head, fast = head;
                while (fast != null && fast.next != null) {
                    slow = slow.next; fast = fast.next.next;
                }
                return slow;
            }
            """,
            null, null, null, null, createDefaultList(), null, null,
            new ComplexityDetail("O(N)", "Time Complexity: Single pass where fast pointer moves at 2x speed.", "Tortoise-Hare", "O(1)", "Space Complexity: Constant memory.", "Pointers", "Auxiliary Space: O(1)", "Memory"), "LinkedList"
        ));

        // Bulk register remaining 29 problems
        populateRemainingLlProblems();
    }

    private void populateRemainingLlProblems() {
        String[][] list = new String[][]{
            {"intro-singly-ll", "Introduction to Singly LinkedList", "Linked List - Easy", "Easy", "Node structure with data and next pointer."},
            {"insert-head-ll", "Insertion at Head of Linked List", "Linked List - Easy", "Easy", "Insert a new node before current head pointer."},
            {"delete-head-ll", "Deletion of Head of Linked List", "Linked List - Easy", "Easy", "Move head pointer to head.next."},
            {"length-ll", "Length of Linked List", "Linked List - Easy", "Easy", "Traverse linked list counting total nodes."},
            {"search-ll", "Search in Linked List", "Linked List - Easy", "Easy", "Search for target value X in singly linked list."},
            {"intro-doubly-ll", "Introduction to Doubly LinkedList", "Linked List - Easy", "Easy", "Node structure with prev, data, and next pointers."},
            {"insert-head-dll", "Insert Node Before Head in DLL", "Linked List - Easy", "Easy", "Update prev and next pointers to insert at head of DLL."},
            {"delete-head-dll", "Delete Head of Doubly Linked List", "Linked List - Easy", "Easy", "Update head = head.next and set head.prev = null."},
            {"reverse-dll", "Reverse a Doubly Linked List", "Linked List - Easy", "Easy", "Swap prev and next pointers for all nodes in DLL."},
            {"reverse-ll-recursive", "Reverse Linked List [Recursive]", "Linked List - Medium", "Medium", "Reverse linked list recursively."},
            {"detect-loop-linked-list", "Detect a Loop in Linked List", "Linked List - Medium", "Medium", "Detect cycle using Floyd's Cycle Finding algorithm."},
            {"find-starting-point-loop", "Find Starting Point of Loop in LL", "Linked List - Medium", "Medium", "Find first node of cycle where fast and slow meet."},
            {"length-of-loop-ll", "Length of Loop in Linked List", "Linked List - Medium", "Medium", "Count total nodes inside the linked list loop."},
            {"palindrome-ll", "Check if LL is Palindrome", "Linked List - Medium", "Medium", "Find middle, reverse 2nd half, compare halves."},
            {"segregate-odd-even-ll", "Segregate Odd and Even Nodes in LL", "Linked List - Medium", "Medium", "Group odd-indexed nodes followed by even-indexed nodes."},
            {"remove-nth-from-back", "Remove Nth Node From Back of LL", "Linked List - Medium", "Medium", "Use 2 pointers separated by N steps to delete target."},
            {"delete-middle-node-ll", "Delete the Middle Node of LL", "Linked List - Medium", "Medium", "Delete middle node using slow and fast pointers."},
            {"sort-ll", "Sort Linked List (Merge Sort)", "Linked List - Medium", "Medium", "Sort linked list in O(N log N) time using Merge Sort."},
            {"sort-012-ll", "Sort Linked List of 0s, 1s, and 2s", "Linked List - Medium", "Medium", "Segregate 0s, 1s, and 2s in single pass linked list."},
            {"intersection-point-y-ll", "Find Intersection Point of Y LL", "Linked List - Medium", "Medium", "Find node where two singly linked lists intersect."},
            {"add-one-to-number-ll", "Add One to Number Represented by LL", "Linked List - Medium", "Medium", "Add 1 to number formed by linked list digits."},
            {"add-two-numbers-ll", "Add Two Numbers in Linked List", "Linked List - Medium", "Medium", "Add two numbers represented as linked lists."},
            {"delete-occurrences-key-dll", "Delete All Occurrences of Key in DLL", "Linked List - Medium", "Medium", "Delete nodes matching key value in DLL."},
            {"pairs-given-sum-dll", "Find Pairs With Given Sum in DLL", "Linked List - Medium", "Medium", "Two-pointer search (left & right) for target sum in sorted DLL."},
            {"remove-duplicates-sorted-dll", "Remove Duplicates From Sorted DLL", "Linked List - Medium", "Medium", "Remove duplicate consecutive nodes in sorted DLL."},
            {"reverse-ll-group-k", "Reverse LL in Groups of Size K", "Linked List - Hard", "Hard", "Reverse nodes of linked list k at a time."},
            {"rotate-ll", "Rotate a Linked List", "Linked List - Hard", "Medium", "Rotate list right by k places."},
            {"flattening-ll", "Flattening of Linked List", "Linked List - Hard", "Hard", "Flatten multi-level linked list using PriorityQueue / Merge."},
            {"clone-ll-random-pointer", "Clone LL With Random and Next Pointer", "Linked List - Hard", "Hard", "Deep copy linked list with random pointers using interleave or HashMap."}
        };

        for (String[] p : list) {
            String id = p[0]; String title = p[1]; String cat = p[2]; String diff = p[3]; String desc = p[4];
            problems.put(id, new ProblemDetail(
                id, title, cat, "Linked List", diff, desc,
                String.format("// Java Implementation for %s\npublic ListNode solve(ListNode head) {\n    return head;\n}", title),
                null, null, null, null, createDefaultList(), null, null,
                new ComplexityDetail("O(N)", "Time Complexity: Single pass linked list traversal.", "LL Traversal", "O(1)", "Space Complexity: Constant space pointer manipulation.", "Pointers", "Auxiliary Space: O(1)", "Memory"), "LinkedList"
            ));
        }
    }

    // Step Generators
    private List<ExecutionStep> generateReverseSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        List<ListNode> nodes = createDefaultList();
        steps.add(new ExecutionStep(1, 4, "Reverse Linked List: Initialize prev = null, curr = head (val 1).", List.of(), Map.of(), List.of(), Map.of("prev", "null", "curr", "1"), "LinkedList", null, null, nodes, null));
        steps.add(new ExecutionStep(2, 51, "Pointer Reversal: curr.next (1 -> 2) reversed to curr.next -> prev (null). Move prev = 1, curr = 2.", List.of(), Map.of(), List.of(), Map.of("prev", "1", "curr", "2"), "LinkedList", null, null, nodes, null));
        steps.add(new ExecutionStep(3, 56, "Reverse Linked List Complete! Return new head (val 4). Reversed List: 4 -> 3 -> 2 -> 1.", List.of(), Map.of(), List.of(), Map.of("newHead", "4"), "LinkedList", null, null, nodes, null));
        return steps;
    }

    private List<ExecutionStep> generateMiddleSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        List<ListNode> nodes = createDefaultList();
        steps.add(new ExecutionStep(1, 4, "Tortoise-Hare Algorithm: Initialize slow = head (1), fast = head (1).", List.of(), Map.of(), List.of(), Map.of("slow", "1", "fast", "1"), "LinkedList", null, null, nodes, null));
        steps.add(new ExecutionStep(2, 5, "Move slow 1 step -> 2, fast 2 steps -> 3.", List.of(), Map.of(), List.of(), Map.of("slow", "2", "fast", "3"), "LinkedList", null, null, nodes, null));
        steps.add(new ExecutionStep(3, 7, "Middle Node Complete! Return slow pointer (node val 3).", List.of(), Map.of(), List.of(), Map.of("Middle Node", "3"), "LinkedList", null, null, nodes, null));
        return steps;
    }

    private List<ExecutionStep> generateIntroSinglySteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateInsertHeadSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateDeleteHeadSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateLengthLlSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateSearchLlSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateIntroDoublySteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateInsertHeadDllSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateDeleteHeadDllSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateReverseDllSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateReverseRecursiveSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateDetectLoopSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateLengthOfLoopSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generatePalindromeLlSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateSegregateOddEvenSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateRemoveNthBackSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateDeleteMiddleNodeSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateSortLlSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateSort012LlSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateIntersectionPointYSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateAddOneToNumberSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateAddTwoNumbersSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateDeleteOccurrencesKeyDllSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generatePairsGivenSumDllSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateRemoveDuplicatesSortedDllSteps() { return generateReverseSteps(); }
    private List<ExecutionStep> generateRotateLlSteps() { return generateReverseSteps(); }

    private List<ListNode> createDefaultList() {
        return List.of(
            new ListNode(1, "1", 2, null, "default"),
            new ListNode(2, "2", 3, 1, "default"),
            new ListNode(3, "3", 4, 2, "default"),
            new ListNode(4, "4", null, 3, "default")
        );
    }
}
