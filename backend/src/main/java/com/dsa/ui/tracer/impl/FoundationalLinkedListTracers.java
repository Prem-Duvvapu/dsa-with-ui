package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.InputSpec;
import com.dsa.ui.tracer.Inputs;
import com.dsa.ui.tracer.StepEmitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
class MiddleLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "middle-linked-list"; }
    public InputSpec inputSpec() { return InputSpec.of(listField("values", "List values", List.of(1, 2, 3, 4, 5))); }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(8, 9, 10, 11)); }
    public String annotatedCode() {
        return """
               public ListNode middleNode(ListNode head) {
                   // @a inspect
                   ListNode slow = head, fast = head;
                   // @a init
                   while (fast != null && fast.next != null) {
                       slow = slow.next;
                       fast = fast.next.next;
                       // @a advance
                   }
                   // @a done
                   return slow;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "list", all);
        TraceListNode slow = nodes[0], fast = nodes[0];
        emit.at("init").say("Start slow and fast at the head.")
                .var("slow", slow.value).var("fast", fast.value)
                .list(snapshot(all, states(slow, "slow"))).step();
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            emit.at("advance").say("Move slow once to %d and fast twice to %s.", slow.value, value(fast))
                    .var("slow", slow.value).var("fast", value(fast))
                    .list(snapshot(all, states(slow, "slow", fast, "fast"))).step();
        }
        emit.at("done").say("Fast reached the end; slow identifies middle node #%d with value %d.", slow.id, slow.value)
                .var("middleIndex", slow.id).var("result", slow.value)
                .list(snapshot(all, states(slow, "result"))).step();
    }
}

@Component
class IntroSinglyLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "intro-singly-ll"; }
    public InputSpec inputSpec() { return InputSpec.of(listField("values", "Node values", List.of(4, 8, 15, 16))); }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(2, 7, 1)); }
    public String annotatedCode() {
        return """
               public ListNode build(int[] values) {
                   ListNode head = null, tail = null;
                   for (int value : values) {
                       // @a inspect
                       ListNode node = new ListNode(value);
                       if (head == null) head = node;
                       else tail.next = node;
                       tail = node;
                   }
                   // @a done
                   return head;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "singly linked list", all);
        emit.at("done").say("The head reaches %d nodes by following next links; the tail points to null.", nodes.length)
                .var("head", nodes[0].value).var("tail", nodes[nodes.length - 1].value)
                .var("length", nodes.length).list(snapshot(all)).step();
    }
}

@Component
class InsertHeadLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "insert-head-ll"; }
    public InputSpec inputSpec() {
        return InputSpec.of(
                listField("values", "Existing list", List.of(2, 3, 4)),
                intField("value", "New head value", -999, 999, 1));
    }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(8, 9), "value", 7); }
    public String annotatedCode() {
        return """
               public ListNode insertHead(ListNode head, int value) {
                   // @a inspect
                   ListNode oldHead = head;
                   // @a insert
                   ListNode node = new ListNode(value);
                   node.next = oldHead;
                   // @a done
                   return node;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = new ArrayList<>(all(nodes));
        traceInput(emit, "inspect", "existing list", all);
        TraceListNode inserted = new TraceListNode(nodes.length, in.getInt("value"));
        inserted.next = nodes[0];
        all.add(inserted);
        emit.at("insert").say("Create identity #%d and point it at the former head #%d.", inserted.id, nodes[0].id)
                .var("newHead", inserted.value).var("next", nodes[0].value)
                .list(snapshot(all, states(inserted, "active", nodes[0], "next"))).step();
        emit.at("done").say("Insertion is complete without traversing or copying the existing nodes.")
                .var("result", values(inserted, all.size())).list(snapshot(all, states(inserted, "result"))).step();
    }
}

@Component
class DeleteHeadLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "delete-head-ll"; }
    public InputSpec inputSpec() { return InputSpec.of(listField("values", "Existing list", List.of(1, 2, 3, 4))); }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(9, 8)); }
    public String annotatedCode() {
        return """
               public ListNode deleteHead(ListNode head) {
                   // @a inspect
                   ListNode oldHead = head;
                   // @a delete
                   head = head.next;
                   oldHead.next = null;
                   // @a done
                   return head;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "existing list", all);
        TraceListNode removed = nodes[0];
        TraceListNode head = removed.next;
        removed.next = null;
        emit.at("delete").say("Advance head to %s and detach former head #%d.",
                        head == null ? "null" : "#" + head.id, removed.id)
                .var("removed", removed.value).var("head", value(head))
                .list(snapshot(all, states(removed, "removed", head, "active"))).step();
        emit.at("done").say(head == null ? "Deleting the singleton leaves an empty list."
                        : "The remaining list starts at %d.", head == null ? 0 : head.value)
                .var("result", values(head, all.size())).list(snapshot(all, states(head, "result"))).step();
    }
}

@Component
class LengthLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "length-ll"; }
    public InputSpec inputSpec() { return InputSpec.of(listField("values", "List values", List.of(3, 1, 4, 1))); }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(5, 9)); }
    public String annotatedCode() {
        return """
               public int length(ListNode head) {
                   // @a inspect
                   int count = 0;
                   while (head != null) {
                       count++;
                       head = head.next;
                       // @a count
                   }
                   // @a done
                   return count;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "list", all);
        int count = 0;
        for (TraceListNode current : nodes) {
            count++;
            emit.at("count").say("Visit identity #%d and increase the length to %d.", current.id, count)
                    .var("count", count).list(snapshot(all, states(current, "current"))).step();
        }
        emit.at("done").say("The null link after the tail ends the traversal at length %d.", count)
                .var("result", count).list(snapshot(all)).step();
    }
}

@Component
class SearchLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "search-ll"; }
    public InputSpec inputSpec() {
        return InputSpec.of(
                listField("values", "List values", List.of(10, 20, 30, 40)),
                intField("target", "Target", -999, 999, 30));
    }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(7, 8, 9), "target", 6); }
    public String annotatedCode() {
        return """
               public int search(ListNode head, int target) {
                   // @a inspect
                   int index = 0;
                   while (head != null) {
                       // @a compare
                       if (head.val == target) break;
                       head = head.next;
                       index++;
                   }
                   // @a done
                   return head == null ? -1 : index;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = singly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        int target = in.getInt("target");
        traceInput(emit, "inspect", "search list", all);
        int answer = -1;
        for (int i = 0; i < nodes.length; i++) {
            TraceListNode current = nodes[i];
            emit.at("compare").say("Compare index %d value %d with target %d.", i, current.value, target)
                    .var("index", i).var("target", target)
                    .list(snapshot(all, states(current, "current"))).step();
            if (current.value == target) {
                answer = i;
                break;
            }
        }
        emit.at("done").say(answer >= 0 ? "Target found at index %d." : "Target is absent after reaching null.", answer)
                .var("result", answer).list(snapshot(all, answer >= 0 ? states(nodes[answer], "result") : Map.of())).step();
    }
}

@Component
class IntroDoublyLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "intro-doubly-ll"; }
    public InputSpec inputSpec() { return InputSpec.of(listField("values", "Node values", List.of(5, 10, 15))); }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(1, 4, 9, 16)); }
    public String annotatedCode() {
        return """
               public ListNode buildDoubly(int[] values) {
                   ListNode head = null, previous = null;
                   for (int value : values) {
                       // @a inspect
                       ListNode node = new ListNode(value);
                       node.prev = previous;
                       if (previous != null) previous.next = node;
                       else head = node;
                       previous = node;
                   }
                   // @a done
                   return head;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = doubly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "doubly linked list", all);
        emit.at("done").say("Every adjacent pair has matching next and prev links.")
                .var("head.prev", "null").var("tail.next", "null")
                .list(snapshot(all)).step();
    }
}

@Component
class InsertHeadDoublyLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "insert-head-dll"; }
    public InputSpec inputSpec() {
        return InputSpec.of(
                listField("values", "Existing DLL", List.of(2, 4, 6)),
                intField("value", "New head value", -999, 999, 1));
    }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(10, 20), "value", 5); }
    public String annotatedCode() {
        return """
               public ListNode insertBeforeHead(ListNode head, int value) {
                   // @a inspect
                   ListNode oldHead = head;
                   // @a insert
                   ListNode node = new ListNode(value);
                   node.next = oldHead;
                   oldHead.prev = node;
                   // @a done
                   return node;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = doubly(in.getLinkedList("values"));
        List<TraceListNode> all = new ArrayList<>(all(nodes));
        traceInput(emit, "inspect", "existing DLL", all);
        TraceListNode inserted = new TraceListNode(nodes.length, in.getInt("value"));
        inserted.next = nodes[0];
        nodes[0].prev = inserted;
        all.add(inserted);
        emit.at("insert").say("Link new head #%d forward to #%d and link the old head back.", inserted.id, nodes[0].id)
                .var("newHead", inserted.value)
                .list(snapshot(all, states(inserted, "active", nodes[0], "next"))).step();
        emit.at("done").say("Both directions are consistent and the new head has no prev link.")
                .var("result", values(inserted, all.size())).list(snapshot(all, states(inserted, "result"))).step();
    }
}

@Component
class DeleteHeadDoublyLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "delete-head-dll"; }
    public InputSpec inputSpec() { return InputSpec.of(listField("values", "Existing DLL", List.of(1, 2, 3, 4))); }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(7, 8)); }
    public String annotatedCode() {
        return """
               public ListNode deleteHead(ListNode head) {
                   // @a inspect
                   ListNode oldHead = head;
                   // @a delete
                   head = head.next;
                   head.prev = null;
                   oldHead.next = null;
                   // @a done
                   return head;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = doubly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "existing DLL", all);
        TraceListNode removed = nodes[0];
        TraceListNode head = removed.next;
        removed.next = null;
        if (head != null) head.prev = null;
        emit.at("delete").say("Detach #%d in both directions; %s becomes the head.", removed.id,
                        head == null ? "null" : "#" + head.id)
                .var("removed", removed.value).var("head", value(head))
                .list(snapshot(all, states(removed, "removed", head, "active"))).step();
        emit.at("done").say(head == null ? "Deleting the singleton leaves an empty DLL."
                        : "The new head's prev link is null.")
                .var("result", values(head, all.size())).list(snapshot(all, states(head, "result"))).step();
    }
}

@Component
class ReverseDoublyLinkedListTracer extends LinkedListTopicTracer {
    public String id() { return "reverse-dll"; }
    public InputSpec inputSpec() { return InputSpec.of(listField("values", "DLL values", List.of(1, 2, 3, 4))); }
    public Map<String, Object> alternateInput() { return Map.of("values", List.of(5, 6, 7)); }
    public String annotatedCode() {
        return """
               public ListNode reverse(ListNode head) {
                   // @a inspect
                   ListNode current = head, newHead = null;
                   while (current != null) {
                       ListNode oldNext = current.next;
                       // @a swap
                       current.next = current.prev;
                       current.prev = oldNext;
                       newHead = current;
                       current = oldNext;
                   }
                   // @a done
                   return newHead;
               }""";
    }
    public void run(Inputs in, StepEmitter emit) {
        TraceListNode[] nodes = doubly(in.getLinkedList("values"));
        List<TraceListNode> all = all(nodes);
        traceInput(emit, "inspect", "DLL", all);
        TraceListNode current = nodes[0], newHead = null;
        while (current != null) {
            TraceListNode oldNext = current.next;
            current.next = current.prev;
            current.prev = oldNext;
            newHead = current;
            emit.at("swap").say("Swap next and prev on identity #%d; continue through its old next link.", current.id)
                    .var("current", current.value).var("newHead", newHead.value)
                    .list(snapshot(all, states(current, "current"))).step();
            current = oldNext;
        }
        emit.at("done").say("Every pair of links is reversed; former tail #%d is the new head.", newHead.id)
                .var("result", values(newHead, all.size())).list(snapshot(all, states(newHead, "result"))).step();
    }
}
