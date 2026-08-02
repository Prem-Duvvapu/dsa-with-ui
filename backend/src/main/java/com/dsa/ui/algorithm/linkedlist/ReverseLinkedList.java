package com.dsa.ui.algorithm.linkedlist;

import com.dsa.ui.model.ListNode;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Problem: Reverse Linked List (LeetCode 206)
 *
 * Reverse a singly linked list in-place using 3 pointers (prev, curr, next).
 *
 * Time Complexity:  O(N) - Single pass through N nodes.
 * Space Complexity: O(1) - Modifies pointers in-place.
 */
public class ReverseLinkedList {

    public static class Node {
        public int val;
        public Node next;

        public Node(int val) {
            this.val = val;
        }
    }

    public Node solve(Node head, TraceRecorder recorder) {
        recorder.record(new TraceEvent(
            "start", 15,
            "Reverse Linked List: Initialize prev = null, curr = head. Start iterative 3-pointer reversal.",
            Map.of("prev", "null", "curr", head != null ? String.valueOf(head.val) : "null"),
            "LinkedList", createListSnapshot(head, null, head)
        ));

        Node prev = null;
        Node curr = head;

        while (curr != null) {
            Node next = curr.next;

            recorder.record(new TraceEvent(
                "pointer_save", 20,
                String.format("Save next = curr.next (%s). Prepare to break link (%d -> %s) and point backward to prev (%s).",
                    next != null ? String.valueOf(next.val) : "null", curr.val, next != null ? String.valueOf(next.val) : "null", prev != null ? String.valueOf(prev.val) : "null"),
                Map.of("curr", String.valueOf(curr.val), "next", next != null ? String.valueOf(next.val) : "null", "prev", prev != null ? String.valueOf(prev.val) : "null"),
                "LinkedList", createListSnapshot(head, prev, curr)
            ));

            // Reverse link
            curr.next = prev;

            recorder.record(new TraceEvent(
                "pointer_reverse", 23,
                String.format("Reversed Link! Set node %d.next = %s.", curr.val, prev != null ? String.valueOf(prev.val) : "null"),
                Map.of("node", String.valueOf(curr.val), "new_next", prev != null ? String.valueOf(prev.val) : "null"),
                "LinkedList", createListSnapshot(head, prev, curr)
            ));

            // Move pointers forward
            prev = curr;
            curr = next;

            recorder.record(new TraceEvent(
                "pointer_advance", 27,
                String.format("Advance Pointers: prev = %d, curr = %s.", prev.val, curr != null ? String.valueOf(curr.val) : "null"),
                Map.of("prev", String.valueOf(prev.val), "curr", curr != null ? String.valueOf(curr.val) : "null"),
                "LinkedList", createListSnapshot(prev, prev, curr)
            ));
        }

        recorder.record(new TraceEvent(
            "complete", 30,
            String.format("Reverse Linked List Complete! New Head is node %s.", prev != null ? String.valueOf(prev.val) : "null"),
            Map.of("New Head", prev != null ? String.valueOf(prev.val) : "null"),
            "LinkedList", createListSnapshot(prev, prev, null)
        ));

        return prev;
    }

    private List<ListNode> createListSnapshot(Node head, Node prev, Node curr) {
        List<ListNode> list = new ArrayList<>();
        Node temp = head;
        int id = 1;
        while (temp != null) {
            String state = "default";
            if (temp == curr) state = "active";
            else if (temp == prev) state = "visited";

            Integer nextId = temp.next != null ? id + 1 : null;
            Integer prevId = id > 1 ? id - 1 : null;
            list.add(new ListNode(id, String.valueOf(temp.val), nextId, prevId, state));
            id++;
            temp = temp.next;
        }
        return list;
    }
}
