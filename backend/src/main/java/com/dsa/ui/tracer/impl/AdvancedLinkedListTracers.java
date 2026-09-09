package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.InputSpec;
import com.dsa.ui.tracer.Inputs;
import com.dsa.ui.tracer.StepEmitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
class SortLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "sort-ll"; }
    public InputSpec inputSpec() { return InputSpec.of(listField("values", "Unsorted list", List.of(4, 2, 1, 3))); }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(5, 1, 4, 2, 3)); }
    public String annotatedCode() {
        return """
               public ListNode mergeSort(ListNode head) {
                   // @a inspect
                   if (head == null || head.next == null) return head;
                   ListNode middle = split(head);
                   // @a split
                   ListNode left = mergeSort(head);
                   ListNode right = mergeSort(middle);
                   // @a merge
                   return merge(left, right);
               }
               // @a done
               // the merged chain is sorted
               """;
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "unsorted list", all);
        TraceListNode head = mergeSort(nodes[0], all, emit);
        emit.at("done").say("Merge sort returns the identities in nondecreasing value order.")
                .var("result", values(head, all.size())).list(snapshot(all, states(head, "result"))).step();
    }
    private TraceListNode mergeSort(TraceListNode head, List<TraceListNode> all, StepEmitter emit) {
        if (head.next == null) return head;
        emit.push("sort(#" + head.id + ")");
        TraceListNode slow = head, fast = head.next;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        TraceListNode right = slow.next;
        slow.next = null;
        emit.at("split").say("Cut after identity #%d into two smaller chains.", slow.id)
                .list(snapshot(all, states(slow, "prev", right, "next"))).step();
        TraceListNode leftSorted = mergeSort(head, all, emit);
        TraceListNode rightSorted = mergeSort(right, all, emit);
        TraceListNode merged = merge(leftSorted, rightSorted, all, emit);
        emit.pop();
        return merged;
    }
    private TraceListNode merge(TraceListNode left, TraceListNode right, List<TraceListNode> all, StepEmitter emit) {
        TraceListNode dummy = new TraceListNode(-1, 0), tail = dummy;
        while (left != null && right != null) {
            TraceListNode chosen;
            if (left.value <= right.value) {
                chosen = left; left = left.next;
            } else {
                chosen = right; right = right.next;
            }
            tail.next = chosen;
            tail = chosen;
            tail.next = null;
            emit.at("merge").say("Append identity #%d (value %d), the smaller current head.", chosen.id, chosen.value)
                    .list(snapshot(all, states(chosen, "current"))).step();
        }
        tail.next = left != null ? left : right;
        emit.at("merge").say("Append the already-sorted remainder.")
                .list(snapshot(all, states(tail, "prev", tail.next, "next"))).step();
        return dummy.next;
    }
}

@Component
class Sort012LinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "sort-012-ll"; }
    public InputSpec inputSpec() {
        return InputSpec.of(listField("values", "0/1/2 list", List.of(1, 2, 0, 1, 2, 0), 0, 2, false));
    }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(2, 1, 0, 2)); }
    public String annotatedCode() {
        return """
               public ListNode sort012(ListNode head) {
                   int[] count = new int[3];
                   for (ListNode p = head; p != null; p = p.next) {
                       // @a inspect
                       count[p.val]++;
                       // @a count
                   }
                   for (ListNode p = head; p != null; p = p.next) {
                       // @a write
                       p.val = nextValue(count);
                   }
                   // @a done
                   return head;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "0/1/2 list", all);
        int[] counts = new int[3];
        for (TraceListNode node : nodes) {
            counts[node.value]++;
            emit.at("count").say("Count value %d: totals are [0=%d, 1=%d, 2=%d].", node.value, counts[0], counts[1], counts[2])
                    .list(snapshot(all, states(node, "current"))).step();
        }
        int value = 0;
        for (TraceListNode node : nodes) {
            while (counts[value] == 0) value++;
            node.value = value;
            counts[value]--;
            emit.at("write").say("Write %d into identity #%d and consume one count.", value, node.id)
                    .list(snapshot(all, states(node, "current"))).step();
        }
        emit.at("done").say("The list is sorted using three counters and unchanged links.")
                .var("result", values(nodes[0], nodes.length)).list(snapshot(all)).step();
    }
}

@Component
class IntersectionPointYLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "intersection-point-y-ll"; }
    public InputSpec inputSpec() {
        return InputSpec.of(
                listField("prefixA", "First unique prefix", List.of(4, 1)),
                listField("prefixB", "Second unique prefix", List.of(5, 6, 1)),
                listField("sharedTail", "Shared tail", List.of(8, 4, 5)));
    }
    public Map<String, Object> alternateInput() {
        return Map.of("prefixA", List.of(2), "prefixB", List.of(3, 7), "sharedTail", List.of(9, 10));
    }
    public String annotatedCode() {
        return """
               public ListNode intersection(ListNode a, ListNode b) {
                   // @a inspect
                   ListNode p = a, q = b;
                   while (p != q) {
                       p = p == null ? b : p.next;
                       q = q == null ? a : q.next;
                       // @a advance
                   }
                   // @a done
                   return p;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        int[] aValues = in.getLinkedList("prefixA");
        int[] bValues = in.getLinkedList("prefixB");
        int[] sharedValues = in.getLinkedList("sharedTail");
        TraceListNode[] a = singly(aValues, 0);
        TraceListNode[] b = singly(bValues, a.length);
        TraceListNode[] shared = singly(sharedValues, a.length + b.length);
        a[a.length - 1].next = shared[0];
        b[b.length - 1].next = shared[0];
        List<TraceListNode> all = all(a, b, shared);
        traceInput(emit, "inspect", "Y-shaped structure", all);
        TraceListNode p = a[0], q = b[0];
        int guard = 0;
        while (p != q && guard++ <= all.size() * 2) {
            p = p == null ? b[0] : p.next;
            q = q == null ? a[0] : q.next;
            emit.at("advance").say("Advance both pointers, switching heads after null: p=%s, q=%s.", value(p), value(q))
                    .list(snapshot(all, states(p, "slow", q, "fast"))).step();
        }
        emit.at("done").say("Both pointers identify the same object #%d, not merely an equal value.", p.id)
                .var("intersectionId", p.id).var("result", p.value)
                .list(snapshot(all, states(p, "result"))).step();
    }
}

@Component
class AddOneLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "add-one-to-number-ll"; }
    public InputSpec inputSpec() {
        return InputSpec.of(listField("digits", "Most-significant digit first", List.of(1, 2, 9), 0, 9, false));
    }
    public Map<String, Object> alternateInput() { return Map.of("digits", List.of(9, 9)); }
    public String annotatedCode() {
        return """
               public ListNode addOne(ListNode head) {
                   // @a inspect
                   head = reverse(head);
                   // @a reverse
                   int carry = 1;
                   for (ListNode p = head; p != null && carry > 0; p = p.next) {
                       int sum = p.val + carry;
                       p.val = sum % 10;
                       carry = sum / 10;
                       // @a add
                   }
                   if (carry > 0) {
                       // @a grow
                       append(head, new ListNode(carry));
                   }
                   head = reverse(head);
                   // @a done
                   return head;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("digits"));
        List<TraceListNode> all = new ArrayList<>(all(nodes));
        traceInput(emit, "inspect", "digit list", all);
        TraceListNode reversed = reverse(nodes[0], all, emit);
        int carry = 1;
        TraceListNode current = reversed, tail = null;
        while (current != null && carry > 0) {
            int sum = current.value + carry;
            current.value = sum % 10;
            carry = sum / 10;
            tail = current;
            emit.at("add").say("Add the carry at identity #%d: digit=%d, next carry=%d.", current.id, current.value, carry)
                    .var("carry", carry).list(snapshot(all, states(current, "current"))).step();
            current = current.next;
        }
        if (carry > 0) {
            TraceListNode extra = new TraceListNode(all.size(), carry);
            tail.next = extra;
            all.add(extra);
            emit.at("grow").say("Every digit overflowed, so append a new carry identity #%d.", extra.id)
                    .list(snapshot(all, states(extra, "active"))).step();
        }
        TraceListNode result = reverse(reversed, all, emit);
        emit.at("done").say("Restore most-significant-first order after adding one.")
                .var("result", values(result, all.size())).list(snapshot(all, states(result, "result"))).step();
    }
    private TraceListNode reverse(TraceListNode head, List<TraceListNode> all, StepEmitter emit) {
        TraceListNode prev = null, current = head;
        while (current != null) {
            TraceListNode next = current.next;
            current.next = prev;
            prev = current;
            emit.at("reverse").say("Reverse the link at digit identity #%d.", current.id)
                    .list(snapshot(all, states(current, "current"))).step();
            current = next;
        }
        return prev;
    }
}

@Component
class AddTwoNumbersLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "add-two-numbers-ll"; }
    public InputSpec inputSpec() {
        return InputSpec.of(
                listField("a", "First digits (least significant first)", List.of(2, 4, 3), 0, 9, false),
                listField("b", "Second digits (least significant first)", List.of(5, 6, 4), 0, 9, false));
    }
    public Map<String, Object> alternateInput() { return Map.of("a", List.of(9, 9), "b", List.of(1)); }
    public String annotatedCode() {
        return """
               public ListNode addTwoNumbers(ListNode a, ListNode b) {
                   // @a inspect
                   ListNode dummy = new ListNode(0), tail = dummy;
                   int carry = 0;
                   while (a != null || b != null || carry != 0) {
                       int sum = value(a) + value(b) + carry;
                       // @a add
                       tail.next = new ListNode(sum % 10);
                       tail = tail.next;
                       carry = sum / 10;
                   }
                   // @a done
                   return dummy.next;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] a = singly(in.getLinkedList("a"), 0);
        TraceListNode[] b = singly(in.getLinkedList("b"), a.length);
        List<TraceListNode> all = new ArrayList<>(all(a, b));
        traceInput(emit, "inspect", "input number", all);
        TraceListNode pa = a[0], pb = b[0], resultHead = null, tail = null;
        int carry = 0, nextId = all.size();
        while (pa != null || pb != null || carry != 0) {
            int av = pa == null ? 0 : pa.value;
            int bv = pb == null ? 0 : pb.value;
            int sum = av + bv + carry;
            TraceListNode digit = new TraceListNode(nextId++, sum % 10);
            if (resultHead == null) resultHead = digit;
            else tail.next = digit;
            tail = digit;
            carry = sum / 10;
            all.add(digit);
            emit.at("add").say("%d + %d with carry creates digit %d and next carry %d.", av, bv, digit.value, carry)
                    .var("carry", carry).list(snapshot(all, states(pa, "slow", pb, "fast", digit, "active"))).step();
            if (pa != null) pa = pa.next;
            if (pb != null) pb = pb.next;
        }
        emit.at("done").say("The result digits remain least-significant first, matching the input convention.")
                .var("result", values(resultHead, all.size())).list(snapshot(all, states(resultHead, "result"))).step();
    }
}
