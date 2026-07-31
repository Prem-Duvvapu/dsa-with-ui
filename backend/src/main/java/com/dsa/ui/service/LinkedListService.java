package com.dsa.ui.service;

import com.dsa.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LinkedListService {

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
            case "reverse-linked-list": return generateReverseSteps();
            case "middle-linked-list": return generateMiddleSteps();
            case "detect-loop-linked-list": return generateDetectLoopSteps();
            case "delete-node-linked-list": return generateDeleteNodeSteps();
            case "merge-two-sorted-lists": return generateMergeListsSteps();
            default: return generateReverseSteps();
        }
    }

    private void initProblems() {
        // 1. Reverse Linked List
        problems.put("reverse-linked-list", new ProblemDetail(
            "reverse-linked-list", "Reverse Linked List", "Linked List - Easy", "Linked List", "Easy",
            "Reverse a singly linked list using 3 pointers (prev, curr, next).",
            """
            // Java Reverse Linked List (LeetCode 206)
            public ListNode reverseList(ListNode head) {
                ListNode prev = null;
                ListNode curr = head;

                while (curr != null) {
                    ListNode next = curr.next;
                    curr.next = prev;
                    prev = curr;
                    curr = next;
                }
                return prev;
            }
            """,
            null, null, null, null, createDefaultList(), null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Iterates through all N nodes once to reverse pointer connections.",
                "Why O(N)? Moves `curr` pointer forward by one node at each iteration.",
                "O(1)",
                "Space Complexity: O(1) auxiliary memory.",
                "Why O(1)? Uses 3 reference pointers `prev`, `curr`, `next` without creating new nodes.",
                "Auxiliary Space: O(1)",
                "Linked List Output: Reused in-place O(1)"
            ),
            "LinkedList"
        ));

        // 2. Middle of Linked List
        problems.put("middle-linked-list", new ProblemDetail(
            "middle-linked-list", "Middle of Linked List (Fast & Slow)", "Linked List - Easy", "Linked List", "Easy",
            "Find middle node using Tortoise & Hare (Slow & Fast Pointers) algorithm in single pass.",
            """
            // Java Middle of Linked List (LeetCode 876)
            public ListNode middleNode(ListNode head) {
                ListNode slow = head;
                ListNode fast = head;

                while (fast != null && fast.next != null) {
                    slow = slow.next;
                    fast = fast.next.next;
                }
                return slow;
            }
            """,
            null, null, null, null, createDefaultList(), null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Fast pointer advances 2 nodes per step, traversing N nodes in N/2 iterations = O(N).",
                "Why Fast & Slow works? When fast pointer reaches the end of the list, slow pointer is guaranteed to be at the exact middle node.",
                "O(1)",
                "Space Complexity: O(1) auxiliary space.",
                "Why O(1)? Only two pointers `slow` and `fast` used.",
                "Auxiliary Space: O(1)",
                "Return Node: O(1)"
            ),
            "LinkedList"
        ));

        // 3. Detect Loop in Linked List
        problems.put("detect-loop-linked-list", new ProblemDetail(
            "detect-loop-linked-list", "Detect Loop in Linked List (Floyd's Cycle)", "Linked List - Medium", "Linked List", "Easy",
            "Detect if a linked list contains a cycle using Floyd's Cycle Detection algorithm.",
            """
            // Java Floyd's Cycle Detection (LeetCode 141)
            public boolean hasCycle(ListNode head) {
                ListNode slow = head;
                ListNode fast = head;

                while (fast != null && fast.next != null) {
                    slow = slow.next;
                    fast = fast.next.next;
                    if (slow == fast) {
                        return true; // Cycle detected!
                    }
                }
                return false;
            }
            """,
            null, null, null, null, createCyclicList(), null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: In a cyclic list of size N, `fast` catches up to `slow` within at most N loop steps.",
                "Why fast catches slow? Relative speed between fast and slow is 1 node per iteration, shrinking distance by 1 each step until collision.",
                "O(1)",
                "Space Complexity: O(1) auxiliary space.",
                "Why O(1)? Only `slow` and `fast` pointers.",
                "Auxiliary Space: O(1)",
                "Return Boolean: O(1)"
            ),
            "LinkedList"
        ));

        // 4. Delete Node in Linked List (O(1))
        problems.put("delete-node-linked-list", new ProblemDetail(
            "delete-node-linked-list", "Delete Node in a Linked List (O(1))", "Linked List - Easy", "Linked List", "Easy",
            "Delete a node (except tail) in a singly linked list given only access to that node.",
            """
            // Java O(1) Delete Node (LeetCode 237)
            public void deleteNode(ListNode node) {
                node.val = node.next.val;
                node.next = node.next.next;
            }
            """,
            null, null, null, null, createDefaultList(), null, null,
            new ComplexityDetail(
                "O(1)",
                "Time Complexity: Constant O(1) time complexity.",
                "Why O(1)? Copies next node's value into current node and bypasses next node.",
                "O(1)",
                "Space Complexity: O(1) extra space.",
                "Why O(1)? No allocations.",
                "Auxiliary Space: O(1)",
                "Return Space: Void O(1)"
            ),
            "LinkedList"
        ));

        // 5. Merge Two Sorted Lists
        problems.put("merge-two-sorted-lists", new ProblemDetail(
            "merge-two-sorted-lists", "Merge Two Sorted Linked Lists", "Linked List - Easy", "Linked List", "Easy",
            "Merge two sorted linked lists into a single sorted list by splicing nodes.",
            """
            // Java Merge Two Sorted Lists (LeetCode 21)
            public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
                ListNode dummy = new ListNode(-1);
                ListNode temp = dummy;

                while (list1 != null && list2 != null) {
                    if (list1.val <= list2.val) {
                        temp.next = list1; list1 = list1.next;
                    } else {
                        temp.next = list2; list2 = list2.next;
                    }
                    temp = temp.next;
                }
                if (list1 != null) temp.next = list1;
                else temp.next = list2;

                return dummy.next;
            }
            """,
            null, null, null, null, createDefaultList(), null, null,
            new ComplexityDetail(
                "O(N + M)",
                "Time Complexity: Iterates through list1 (size N) and list2 (size M).",
                "Why O(N+M)? Compares heads of list1 and list2 and links smaller node in each step.",
                "O(1)",
                "Space Complexity: Splices existing nodes in-place requiring O(1) auxiliary space.",
                "Why O(1)? Uses dummy node reference without duplicating list nodes.",
                "Auxiliary Space: O(1)",
                "Merged List: Reused in-place"
            ),
            "LinkedList"
        ));
    }

    // Dynamic Step Generators
    private List<ExecutionStep> generateReverseSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        List<ListNode> list = createDefaultList();
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Initial Linked List: 1 -> 2 -> 3 -> 4 -> NULL. Pointers: prev = NULL, curr = node 1.",
            List.of(), Map.of(), List.of(), Map.of("prev", "null", "curr", "1"),
            "LinkedList", null, null, updateListState(list, 1, "curr"), null
        ));

        for (int currVal = 1; currVal <= 4; currVal++) {
            String nextVal = (currVal < 4) ? String.valueOf(currVal + 1) : "null";
            steps.add(new ExecutionStep(
                stepNum++, 8,
                String.format("Step %d: Save next = node %s. Reverse link: node %d points to prev. Advance prev = node %d, curr = node %s.", currVal, nextVal, currVal, currVal, nextVal),
                List.of(), Map.of(), List.of(), Map.of("prev", String.valueOf(currVal), "curr", nextVal),
                "LinkedList", null, null, updateListState(list, currVal, "curr"), null
            ));
        }

        steps.add(new ExecutionStep(
            stepNum++, 13,
            "Reverse Linked List Complete! Return prev as new head. Output: 4 -> 3 -> 2 -> 1 -> NULL.",
            List.of(), Map.of(), List.of(), Map.of("New Head", "4", "Result", "4->3->2->1"),
            "LinkedList", null, null, updateListState(list, 4, "visited"), null
        ));

        return steps;
    }

    private List<ExecutionStep> generateMiddleSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        List<ListNode> list = createDefaultList();
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Initialize Tortoise & Hare pointers: slow = node 1, fast = node 1.",
            List.of(), Map.of(), List.of(), Map.of("slow", "1", "fast", "1"),
            "LinkedList", null, null, updateListState(list, 1, "slow"), null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 8,
            "Step 1: Move slow = slow.next (node 2), fast = fast.next.next (node 3).",
            List.of(), Map.of(), List.of(), Map.of("slow", "2", "fast", "3"),
            "LinkedList", null, null, updateListState(list, 2, "slow"), null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 8,
            "Step 2: Move slow = slow.next (node 3), fast = fast.next.next (null).",
            List.of(), Map.of(), List.of(), Map.of("slow", "3", "fast", "null"),
            "LinkedList", null, null, updateListState(list, 3, "active"), null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 11,
            "Fast pointer reached end! Middle Node of Linked List is node 3.",
            List.of(), Map.of(), List.of(), Map.of("Middle Node", "3"),
            "LinkedList", null, null, updateListState(list, 3, "active"), null
        ));

        return steps;
    }

    private List<ExecutionStep> generateDetectLoopSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        List<ListNode> list = createCyclicList();
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Floyd's Cycle Detection: Initialize slow = node 1, fast = node 1 in a cyclic linked list.",
            List.of(), Map.of(), List.of(), Map.of("slow", "1", "fast", "1"),
            "LinkedList", null, null, updateListState(list, 1, "slow"), null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 8,
            "Iteration 1: Move slow = node 2, fast = node 3.",
            List.of(), Map.of(), List.of(), Map.of("slow", "2", "fast", "3"),
            "LinkedList", null, null, updateListState(list, 2, "slow"), null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 10,
            "Iteration 2: Fast loops back! Move slow = node 3, fast = node 3. Collision: slow == fast! CYCLE DETECTED!",
            List.of(), Map.of(), List.of(), Map.of("Collision Node", "3", "Cycle Detected", "TRUE"),
            "LinkedList", null, null, updateListState(list, 3, "active"), null
        ));

        return steps;
    }

    private List<ExecutionStep> generateDeleteNodeSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        List<ListNode> list = createDefaultList();
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 3,
            "Delete Node 2 in O(1): Copy value of node 3 into node 2 (node.val = node.next.val).",
            List.of(), Map.of(), List.of(), Map.of("copied_val", "3"),
            "LinkedList", null, null, updateListState(list, 2, "active"), null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Bypass next node: Set node.next = node.next.next. Node 2 deleted in O(1) time!",
            List.of(), Map.of(), List.of(), Map.of("Status", "Deleted"),
            "LinkedList", null, null, updateListState(list, 2, "visited"), null
        ));

        return steps;
    }

    private List<ExecutionStep> generateMergeListsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        List<ListNode> list = createDefaultList();
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 5,
            "Merge Sorted Lists: List 1 [1, 3] and List 2 [2, 4]. Compare heads of both lists.",
            List.of(), Map.of(), List.of(), Map.of("l1", "1", "l2", "2"),
            "LinkedList", null, null, updateListState(list, 1, "active"), null
        ));

        steps.add(new ExecutionStep(
            stepNum++, 12,
            "Merge Complete! Spliced Merged List: 1 -> 2 -> 3 -> 4 -> NULL.",
            List.of(), Map.of(), List.of(), Map.of("Output List", "1->2->3->4"),
            "LinkedList", null, null, updateListState(list, 1, "visited"), null
        ));

        return steps;
    }

    // Helper builders
    private List<ListNode> createDefaultList() {
        return List.of(
            new ListNode(1, "1", 2, null, "default"),
            new ListNode(2, "2", 3, 1, "default"),
            new ListNode(3, "3", 4, 2, "default"),
            new ListNode(4, "4", null, 3, "default")
        );
    }

    private List<ListNode> createCyclicList() {
        return List.of(
            new ListNode(1, "1", 2, null, "default"),
            new ListNode(2, "2", 3, 1, "default"),
            new ListNode(3, "3", 1, 2, "default")
        );
    }

    private List<ListNode> updateListState(List<ListNode> list, int activeId, String state) {
        List<ListNode> newList = new ArrayList<>();
        for (ListNode node : list) {
            String st = (node.getId() == activeId) ? state : "default";
            newList.add(new ListNode(node.getId(), node.getVal(), node.getNextId(), node.getPrevId(), st));
        }
        return newList;
    }
}
