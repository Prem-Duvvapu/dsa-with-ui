package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.InputSpec;
import com.dsa.ui.tracer.Inputs;
import com.dsa.ui.tracer.StepEmitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
class DeleteOccurrencesKeyDoublyLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "delete-occurrences-key-dll"; }
    public InputSpec inputSpec() {
        return InputSpec.of(
                listField("values", "DLL values", List.of(1, 2, 3, 2, 4, 2)),
                intField("key", "Key to delete", -999, 999, 2));
    }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(5, 5, 1, 5), "key", 5); }
    public String annotatedCode() {
        return """
               public ListNode deleteAll(ListNode head, int key) {
                   // @a inspect
                   ListNode current = head;
                   while (current != null) {
                       ListNode next = current.next;
                       if (current.val == key) {
                           // @a delete
                           unlink(current);
                           if (current == head) head = next;
                       }
                       current = next;
                   }
                   // @a done
                   return head;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = doubly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        int key = in.getInt("key");
        traceInput(emit, "inspect", "DLL", all);
        TraceListNode head = nodes[0];
        int removedCount = 0;
        for (TraceListNode current : nodes) {
            if (current.value != key) continue;
            TraceListNode next = current.next;
            if (current.prev != null) current.prev.next = current.next;
            else head = current.next;
            if (current.next != null) current.next.prev = current.prev;
            current.next = null;
            current.prev = null;
            removedCount++;
            emit.at("delete").say("Unlink matching identity #%d and reconnect both neighboring directions.", current.id)
                    .var("removedCount", removedCount).list(snapshot(all, states(current, "removed", next, "next"))).step();
        }
        emit.at("done").say("Deleted %d occurrence%s of %d.", removedCount, removedCount == 1 ? "" : "s", key)
                .var("result", head == null ? List.of() : values(head, all.size()))
                .list(snapshot(all, states(head, "result"))).step();
    }
}

@Component
class PairsGivenSumDoublyLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "pairs-given-sum-dll"; }
    public InputSpec inputSpec() {
        return InputSpec.of(
                listField("values", "Sorted DLL", List.of(1, 2, 3, 4, 5, 6), -999, 999, true),
                intField("target", "Target sum", -1998, 1998, 7));
    }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(1, 2, 4, 8), "target", 10); }
    public String annotatedCode() {
        return """
               public List<int[]> pairs(ListNode head, int target) {
                   // @a inspect
                   ListNode left = head, right = tail(head);
                   List<int[]> result = new ArrayList<>();
                   while (left != right && left.prev != right) {
                       int sum = left.val + right.val;
                       // @a compare
                       if (sum == target) {
                           result.add(new int[]{left.val, right.val});
                           left = left.next;
                           right = right.prev;
                       } else if (sum < target) left = left.next;
                       else right = right.prev;
                   }
                   // @a done
                   return result;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = doubly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        int target = in.getInt("target");
        traceInput(emit, "inspect", "sorted DLL", all);
        int left = 0, right = nodes.length - 1;
        List<String> pairs = new ArrayList<>();
        while (left < right) {
            int sum = nodes[left].value + nodes[right].value;
            emit.at("compare").say("Compare %d + %d = %d with target %d.", nodes[left].value, nodes[right].value, sum, target)
                    .var("sum", sum).var("pairs", pairs)
                    .list(snapshot(all, states(nodes[left], "slow", nodes[right], "fast"))).step();
            if (sum == target) {
                pairs.add("[" + nodes[left].value + ", " + nodes[right].value + "]");
                left++; right--;
            } else if (sum < target) left++;
            else right--;
        }
        emit.at("done").say("The inward scan found %d pair%s.", pairs.size(), pairs.size() == 1 ? "" : "s")
                .var("result", pairs).list(snapshot(all)).step();
    }
}

@Component
class RemoveDuplicatesSortedDoublyLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "remove-duplicates-sorted-dll"; }
    public InputSpec inputSpec() {
        return InputSpec.of(listField("values", "Sorted DLL", List.of(1, 1, 2, 3, 3, 4), -999, 999, true));
    }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(2, 2, 2, 5, 5)); }
    public String annotatedCode() {
        return """
               public ListNode deduplicate(ListNode head) {
                   // @a inspect
                   ListNode current = head;
                   while (current != null && current.next != null) {
                       // @a compare
                       if (current.val == current.next.val) {
                           // @a remove
                           unlink(current.next);
                       } else current = current.next;
                   }
                   // @a done
                   return head;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = doubly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "sorted DLL", all);
        TraceListNode current = nodes[0];
        int removed = 0;
        while (current != null && current.next != null) {
            TraceListNode next = current.next;
            boolean duplicate = current.value == next.value;
            emit.at("compare").say("Compare adjacent identities #%d and #%d: duplicate=%s.", current.id, next.id, duplicate)
                    .list(snapshot(all, states(current, "current", next, "next"))).step();
            if (duplicate) {
                current.next = next.next;
                if (next.next != null) next.next.prev = current;
                next.next = null;
                next.prev = null;
                removed++;
                emit.at("remove").say("Unlink duplicate identity #%d and repair both directions.", next.id)
                        .var("removed", removed).list(snapshot(all, states(current, "current", next, "removed"))).step();
            } else {
                current = next;
            }
        }
        emit.at("done").say("Removed %d duplicate node%s.", removed, removed == 1 ? "" : "s")
                .var("result", values(nodes[0], all.size())).list(snapshot(all)).step();
    }
}

@Component
class RotateLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "rotate-ll"; }
    public InputSpec inputSpec() {
        return InputSpec.of(
                listField("values", "List values", List.of(1, 2, 3, 4, 5)),
                intField("k", "Right rotations", 0, 100, 2));
    }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(10, 20, 30, 40), "k", 1); }
    public String annotatedCode() {
        return """
               public ListNode rotateRight(ListNode head, int k) {
                   // @a inspect
                   int length = 1;
                   ListNode tail = head;
                   while (tail.next != null) {
                       tail = tail.next;
                       length++;
                       // @a length
                   }
                   k %= length;
                   tail.next = head;
                   // @a cycle
                   ListNode newTail = head;
                   for (int i = 1; i < length - k; i++) newTail = newTail.next;
                   ListNode newHead = newTail.next;
                   // @a cut
                   newTail.next = null;
                   // @a done
                   return newHead;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "list", all);
        int length = 1;
        TraceListNode tail = nodes[0];
        while (tail.next != null) {
            tail = tail.next;
            length++;
            emit.at("length").say("Advance to identity #%d; measured length is %d.", tail.id, length)
                    .var("length", length).list(snapshot(all, states(tail, "current"))).step();
        }
        int k = in.getInt("k") % length;
        tail.next = nodes[0];
        emit.at("cycle").say("Temporarily join tail #%d to head #%d; normalized k=%d.", tail.id, nodes[0].id, k)
                .var("k", k).list(snapshot(all, states(tail, "prev", nodes[0], "next"))).step();
        TraceListNode newTail = nodes[0];
        for (int i = 1; i < length - k; i++) newTail = newTail.next;
        TraceListNode newHead = newTail.next;
        newTail.next = null;
        emit.at("cut").say("Cut after identity #%d; identity #%d becomes the new head.", newTail.id, newHead.id)
                .list(snapshot(all, states(newTail, "prev", newHead, "result"))).step();
        emit.at("done").say("Right rotation by %d produces %s.", k, values(newHead, length))
                .var("result", values(newHead, length)).list(snapshot(all, states(newHead, "result"))).step();
    }
}
