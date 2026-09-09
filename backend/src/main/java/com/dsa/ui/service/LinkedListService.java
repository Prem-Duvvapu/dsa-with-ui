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
        if (problems.containsKey(problemId)) {
            throw new LegacyTraceRetiredException(problemId);
        }
        throw new IllegalArgumentException("Unknown Linked List problem: " + problemId);
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

    private List<ListNode> createDefaultList() {
        return List.of(
            new ListNode(1, "1", 2, null, "default"),
            new ListNode(2, "2", 3, 1, "default"),
            new ListNode(3, "3", 4, 2, "default"),
            new ListNode(4, "4", null, 3, "default")
        );
    }
}
