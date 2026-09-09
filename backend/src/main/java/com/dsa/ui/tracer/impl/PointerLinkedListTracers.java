package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.InputSpec;
import com.dsa.ui.tracer.InputValidationException;
import com.dsa.ui.tracer.Inputs;
import com.dsa.ui.tracer.StepEmitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
class ReverseLinkedListRecursiveTracer extends LinkedListTopicTracer {
    public String id() { return "reverse-ll-recursive"; }
    public InputSpec inputSpec() { return InputSpec.of(listField("values", "List values", List.of(1, 2, 3, 4))); }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(7, 8, 9)); }
    public String annotatedCode() {
        return """
               public ListNode reverse(ListNode head) {
                   // @a inspect
                   if (head == null || head.next == null) {
                       // @a base
                       return head;
                   }
                   // @a recurse
                   ListNode newHead = reverse(head.next);
                   // @a rewire
                   head.next.next = head;
                   head.next = null;
                   return newHead;
               }
               // @a done
               // return the tail as the new head
               """;
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "list", all);
        TraceListNode head = reverse(nodes[0], all, emit);
        emit.at("done").say("Unwinding is complete; identity #%d is the new head.", head.id)
                .var("result", values(head, all.size())).list(snapshot(all, states(head, "result"))).step();
    }
    private TraceListNode reverse(TraceListNode head, List<TraceListNode> all, StepEmitter emit) {
        emit.push("reverse(#" + head.id + ")");
        if (head.next == null) {
            emit.at("base").say("Identity #%d is the tail, so it becomes the new head.", head.id)
                    .var("head", head.value).list(snapshot(all, states(head, "active"))).step();
            emit.pop();
            return head;
        }
        emit.at("recurse").say("Recurse from identity #%d before changing identity #%d's link.", head.next.id, head.id)
                .var("head", head.value).var("next", head.next.value)
                .list(snapshot(all, states(head, "current", head.next, "next"))).step();
        TraceListNode oldNext = head.next;
        TraceListNode newHead = reverse(oldNext, all, emit);
        oldNext.next = head;
        head.next = null;
        emit.at("rewire").say("On unwind, point #%d back to #%d and clear #%d.next.", oldNext.id, head.id, head.id)
                .var("returnHead", newHead.value)
                .list(snapshot(all, states(oldNext, "prev", head, "current"))).step();
        emit.pop();
        return newHead;
    }
}

@Component
class DetectLoopLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "detect-loop-linked-list"; }
    public InputSpec inputSpec() {
        return InputSpec.of(
                listField("values", "List values", List.of(1, 2, 3, 4, 5)),
                intField("cycleIndex", "Cycle entry index (-1 for none)", -1, 15, 1));
    }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(8, 9, 10, 11), "cycleIndex", -1); }
    public String annotatedCode() {
        return """
               public boolean hasCycle(ListNode head) {
                   // @a inspect
                   ListNode slow = head, fast = head;
                   while (fast != null && fast.next != null) {
                       slow = slow.next;
                       fast = fast.next.next;
                       // @a advance
                       if (slow == fast) break;
                   }
                   // @a done
                   return slow == fast;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        int cycleIndex = validCycleIndex(in.getInt("cycleIndex"), nodes.length);
        if (cycleIndex >= 0) nodes[nodes.length - 1].next = nodes[cycleIndex];
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "possibly cyclic list", all);
        TraceListNode slow = nodes[0], fast = nodes[0];
        boolean found = false;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            emit.at("advance").say("Slow moves to #%d; fast moves to %s.", slow.id, fast == null ? "null" : "#" + fast.id)
                    .var("slow", slow.value).var("fast", value(fast))
                    .list(snapshot(all, states(slow, "slow", fast, "fast"))).step();
            if (slow == fast) { found = true; break; }
        }
        emit.at("done").say(found ? "The pointers meet at identity #%d, proving a cycle." : "Fast reaches null, so no cycle exists.",
                        found ? slow.id : -1)
                .var("result", found).list(snapshot(all, found ? states(slow, "result") : Map.of())).step();
    }
    private int validCycleIndex(int index, int length) {
        if (index >= length) throw new InputValidationException(Map.of("cycleIndex", "Must be -1 or a valid zero-based list index."));
        return index;
    }
}

@Component
class LengthOfLoopLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "length-of-loop-ll"; }
    public InputSpec inputSpec() {
        return InputSpec.of(
                listField("values", "List values", List.of(1, 2, 3, 4, 5, 6)),
                intField("cycleIndex", "Cycle entry index", 0, 15, 2));
    }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(4, 5, 6, 7), "cycleIndex", 0); }
    public String annotatedCode() {
        return """
               public int loopLength(ListNode head) {
                   // @a inspect
                   ListNode slow = head, fast = head;
                   do {
                       slow = slow.next;
                       fast = fast.next.next;
                       // @a meet
                   } while (slow != fast);
                   int length = 1;
                   for (ListNode p = slow.next; p != slow; p = p.next) {
                       length++;
                       // @a count
                   }
                   // @a done
                   return length;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        int cycleIndex = in.getInt("cycleIndex");
        if (cycleIndex >= nodes.length) throw new InputValidationException(Map.of("cycleIndex", "Must identify a node in the list."));
        nodes[nodes.length - 1].next = nodes[cycleIndex];
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "cyclic list", all);
        TraceListNode slow = nodes[0], fast = nodes[0];
        do {
            slow = slow.next;
            fast = fast.next.next;
            emit.at("meet").say("Advance Floyd pointers: slow #%d, fast #%d.", slow.id, fast.id)
                    .var("slow", slow.value).var("fast", fast.value)
                    .list(snapshot(all, states(slow, "slow", fast, "fast"))).step();
        } while (slow != fast);
        int length = 1;
        TraceListNode cursor = slow.next;
        while (cursor != slow) {
            length++;
            emit.at("count").say("Walk once around the cycle: counted %d nodes so far.", length)
                    .var("length", length).list(snapshot(all, states(slow, "slow", cursor, "current"))).step();
            cursor = cursor.next;
        }
        emit.at("done").say("Returning to identity #%d closes a loop of length %d.", slow.id, length)
                .var("result", length).list(snapshot(all, states(slow, "result"))).step();
    }
}

@Component
class PalindromeLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "palindrome-ll"; }
    public InputSpec inputSpec() { return InputSpec.of(listField("values", "List values", List.of(1, 2, 3, 2, 1))); }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(1, 2, 4, 3)); }
    public String annotatedCode() {
        return """
               public boolean isPalindrome(ListNode head) {
                   // @a inspect
                   ListNode slow = head, fast = head;
                   while (fast.next != null && fast.next.next != null) {
                       slow = slow.next;
                       fast = fast.next.next;
                       // @a middle
                   }
                   // @a reverse
                   ListNode second = reverse(slow.next);
                   ListNode left = head, right = second;
                   boolean equal = true;
                   while (right != null) {
                       equal &= left.val == right.val;
                       // @a compare
                       left = left.next;
                       right = right.next;
                   }
                   // @a restore
                   slow.next = reverse(second);
                   // @a done
                   return equal;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "palindrome candidate", all);
        TraceListNode slow = nodes[0], fast = nodes[0];
        while (fast.next != null && fast.next.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            emit.at("middle").say("Move slow to #%d and fast to #%d while locating the halfway point.", slow.id, fast.id)
                    .list(snapshot(all, states(slow, "slow", fast, "fast"))).step();
        }
        TraceListNode second = reverse(slow.next, all, emit);
        slow.next = second;
        TraceListNode left = nodes[0], right = second;
        boolean equal = true;
        while (right != null) {
            boolean same = left.value == right.value;
            equal &= same;
            emit.at("compare").say("Compare %d from the front with %d from the reversed half: %s.", left.value, right.value, same)
                    .var("equalSoFar", equal).list(snapshot(all, states(left, "current", right, "target"))).step();
            left = left.next;
            right = right.next;
        }
        slow.next = restore(second);
        emit.at("restore").say("Reverse the second half again so the caller's list is unchanged.")
                .list(snapshot(all)).step();
        emit.at("done").say(equal ? "All mirrored values match." : "At least one mirrored pair differs.")
                .var("result", equal).list(snapshot(all)).step();
    }
    private TraceListNode reverse(TraceListNode head, List<TraceListNode> all, StepEmitter emit) {
        TraceListNode prev = null, current = head;
        while (current != null) {
            TraceListNode next = current.next;
            current.next = prev;
            prev = current;
            emit.at("reverse").say("Reverse identity #%d inside the second half.", current.id)
                    .list(snapshot(all, states(current, "current"))).step();
            current = next;
        }
        return prev;
    }
    private TraceListNode restore(TraceListNode head) {
        TraceListNode prev = null, current = head;
        while (current != null) {
            TraceListNode next = current.next;
            current.next = prev;
            prev = current;
            current = next;
        }
        return prev;
    }
}

@Component
class SegregateOddEvenLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "segregate-odd-even-ll"; }
    public InputSpec inputSpec() { return InputSpec.of(listField("values", "List values", List.of(1, 2, 3, 4, 5, 6))); }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(9, 8, 7, 6)); }
    public String annotatedCode() {
        return """
               public ListNode oddEvenList(ListNode head) {
                   // @a inspect
                   ListNode odd = head, even = head.next, evenHead = even;
                   while (even != null && even.next != null) {
                       // @a rewire
                       odd.next = even.next;
                       odd = odd.next;
                       even.next = odd.next;
                       even = even.next;
                   }
                   odd.next = evenHead;
                   // @a join
                   // @a done
                   return head;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "positioned list", all);
        TraceListNode odd = nodes[0], even = nodes[0].next, evenHead = even;
        if (evenHead == null) {
            emit.at("rewire").say("There is no even-position node to move.")
                    .list(snapshot(all, states(odd, "current"))).step();
            emit.at("join").say("The odd chain is already the complete singleton list.")
                    .list(snapshot(all, states(odd, "result"))).step();
            emit.at("done").say("The singleton order is unchanged.")
                    .var("result", values(odd, 1)).list(snapshot(all, states(odd, "result"))).step();
            return;
        }
        while (even != null && even.next != null) {
            odd.next = even.next;
            odd = odd.next;
            even.next = odd.next;
            even = even.next;
            emit.at("rewire").say("Append the next odd-position node #%d; advance even to %s.", odd.id,
                            even == null ? "null" : "#" + even.id)
                    .list(snapshot(all, states(odd, "current", even, "next"))).step();
        }
        odd.next = evenHead;
        emit.at("join").say("Join the odd-position tail to the saved even-position head #%d.", evenHead.id)
                .list(snapshot(all, states(odd, "prev", evenHead, "next"))).step();
        emit.at("done").say("Odd positions now precede even positions without changing node identities.")
                .var("result", values(nodes[0], all.size())).list(snapshot(all)).step();
    }
}

@Component
class RemoveNthFromBackLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "remove-nth-from-back"; }
    public InputSpec inputSpec() {
        return InputSpec.of(
                listField("values", "List values", List.of(1, 2, 3, 4, 5)),
                intField("n", "Position from end", 1, 16, 2));
    }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(7, 8, 9), "n", 1); }
    public String annotatedCode() {
        return """
               public ListNode removeNthFromEnd(ListNode head, int n) {
                   // @a inspect
                   ListNode dummy = new ListNode(0, head);
                   ListNode fast = dummy, slow = dummy;
                   for (int i = 0; i < n; i++) {
                       fast = fast.next;
                       // @a gap
                   }
                   while (fast.next != null) {
                       fast = fast.next;
                       slow = slow.next;
                       // @a advance
                   }
                   // @a remove
                   slow.next = slow.next.next;
                   // @a done
                   return dummy.next;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        int n = in.getInt("n");
        if (n > nodes.length) throw new InputValidationException(Map.of("n", "Must not exceed the list length."));
        List<TraceListNode> all = new ArrayList<>(all(nodes));
        traceInput(emit, "inspect", "list", all);
        TraceListNode dummy = new TraceListNode(nodes.length, 0);
        dummy.next = nodes[0];
        all.add(dummy);
        TraceListNode fast = dummy, slow = dummy;
        for (int i = 0; i < n; i++) {
            fast = fast.next;
            emit.at("gap").say("Move fast %d of %d steps to create the gap.", i + 1, n)
                    .var("fast", fast.value).list(snapshot(all, states(fast, "fast", slow, "slow"))).step();
        }
        while (fast.next != null) {
            fast = fast.next;
            slow = slow.next;
            emit.at("advance").say("Advance both pointers; the fixed gap remains %d.", n)
                    .list(snapshot(all, states(fast, "fast", slow, "slow"))).step();
        }
        TraceListNode removed = slow.next;
        slow.next = removed.next;
        removed.next = null;
        emit.at("remove").say("Fast is at the tail, so remove identity #%d immediately after slow.", removed.id)
                .var("removed", removed.value).list(snapshot(all, states(slow, "slow", removed, "removed"))).step();
        emit.at("done").say("The list after removal is %s.", values(dummy.next, nodes.length))
                .var("result", values(dummy.next, nodes.length)).list(snapshot(all)).step();
    }
}

@Component
class DeleteMiddleLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "delete-middle-node-ll"; }
    public InputSpec inputSpec() { return InputSpec.of(listField("values", "List values", List.of(1, 2, 3, 4, 5))); }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(7, 8, 9, 10)); }
    public String annotatedCode() {
        return """
               public ListNode deleteMiddle(ListNode head) {
                   // @a inspect
                   ListNode slow = head, fast = head, previous = null;
                   while (fast != null && fast.next != null) {
                       previous = slow;
                       slow = slow.next;
                       fast = fast.next.next;
                       // @a advance
                   }
                   // @a remove
                   previous.next = slow.next;
                   // @a done
                   return head;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "list", all);
        if (nodes.length == 1) {
            emit.at("advance").say("A singleton's head is also its middle.")
                    .list(snapshot(all, states(nodes[0], "current"))).step();
            emit.at("remove").say("Remove the only node.")
                    .list(snapshot(all, states(nodes[0], "removed"))).step();
            emit.at("done").say("The resulting list is empty.").var("result", List.of()).list(snapshot(all, states(nodes[0], "removed"))).step();
            return;
        }
        TraceListNode slow = nodes[0], fast = nodes[0], previous = null;
        while (fast != null && fast.next != null) {
            previous = slow;
            slow = slow.next;
            fast = fast.next.next;
            emit.at("advance").say("Slow moves to #%d while fast moves to %s.", slow.id, fast == null ? "null" : "#" + fast.id)
                    .list(snapshot(all, states(previous, "prev", slow, "slow", fast, "fast"))).step();
        }
        previous.next = slow.next;
        slow.next = null;
        emit.at("remove").say("Bypass middle identity #%d after the fast pointer reaches the end.", slow.id)
                .var("removed", slow.value).list(snapshot(all, states(previous, "prev", slow, "removed"))).step();
        emit.at("done").say("The remaining list is %s.", values(nodes[0], nodes.length))
                .var("result", values(nodes[0], nodes.length)).list(snapshot(all)).step();
    }
}
