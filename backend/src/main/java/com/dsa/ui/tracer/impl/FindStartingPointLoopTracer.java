package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ListNode;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Floyd's tortoise and hare, in two phases. Phase 1 only proves a cycle exists: a fast
 * pointer moving 2x speed is guaranteed to lap a slow pointer inside any cycle, however far
 * around it starts. Phase 2 finds *where* the cycle begins, using the fact that the
 * distance from the list's head to the cycle's start equals the distance from the phase-1
 * meeting point to that same start, walked forward at the same speed - so resetting one
 * pointer to the head and advancing both one step at a time lands them together exactly at
 * the start.
 */
@Component
public class FindStartingPointLoopTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "find-starting-point-loop";
    }

    @Override
    public DsType dsType() {
        return DsType.LINKED_LIST;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("values", FieldType.LINKED_LIST)
                        .label("List values")
                        .help("Node values from head to tail.")
                        .length(1, 15).values(-999, 999)
                        .defaultValue(List.of(3, 2, 0, -4))
                        .build(),
                InputField.of("loopPos", FieldType.INT)
                        .label("Loop position")
                        .help("0-indexed node the tail connects back to, or -1 for no loop. "
                                + "Must be a valid index for the array above - not checked.")
                        .range(-1, 14)
                        .defaultValue(1)
                        .build());
    }

    /** No loop at all: the fast pointer runs off the end instead of ever meeting the slow one. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("values", List.of(1, 2, 3), "loopPos", -1);
    }

    @Override
    public String annotatedCode() {
        return """
               public ListNode detectCycle(ListNode head) {
                   // @a init
                   ListNode slow = head, fast = head;
                   while (fast != null && fast.next != null) {
                       // @a advanceSlowFast
                       slow = slow.next;
                       fast = fast.next.next;
                       if (slow == fast) {
                           // @a metInLoop
                           break;
                       }
                   }
                   if (fast == null || fast.next == null) {
                       // @a noLoop
                       return null;
                   }
                   // @a resetToHead
                   slow = head;
                   while (slow != fast) {
                       // @a advanceBoth
                       slow = slow.next;
                       fast = fast.next;
                   }
                   // @a foundStart
                   return slow;
               }""";
    }

    /** -1 past the last index means "no next" (list end); any other value wraps into the loop. */
    private Integer next(int i, int n, int loopPos) {
        if (i + 1 < n) {
            return i + 1;
        }
        return loopPos == -1 ? null : loopPos;
    }

    private List<ListNode> render(int[] values, int loopPos, Integer slow, Integer fast) {
        List<ListNode> nodes = new ArrayList<>(values.length);
        for (int i = 0; i < values.length; i++) {
            String state;
            if (slow != null && i == slow && fast != null && i == fast) {
                state = "curr";
            } else if (fast != null && i == fast) {
                state = "fast";
            } else if (slow != null && i == slow) {
                state = "slow";
            } else {
                state = "default";
            }
            Integer nextId = next(i, values.length, loopPos);
            nodes.add(new ListNode(i, String.valueOf(values[i]), nextId, null, state));
        }
        return nodes;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] values = in.getLinkedList("values");
        int loopPos = in.getInt("loopPos");
        int n = values.length;

        emit.at("init")
                .say("slow = head, fast = head. Neither pointer has moved yet.")
                .var("slow", values[0]).var("fast", values[0])
                .list(render(values, loopPos, 0, 0)).step();

        Integer slow = 0, fast = 0;
        boolean met = false;
        while (true) {
            Integer fastNext1 = next(fast, n, loopPos);
            if (fastNext1 == null) {
                break;
            }
            Integer fastNext2 = next(fastNext1, n, loopPos);
            if (fastNext2 == null) {
                break;
            }
            slow = next(slow, n, loopPos);
            fast = fastNext2;
            emit.at("advanceSlowFast")
                    .say("slow moves to %d, fast moves to %d (two steps).", values[slow], values[fast])
                    .var("slow", values[slow]).var("fast", values[fast])
                    .list(render(values, loopPos, slow, fast)).step();
            if (slow.equals(fast)) {
                met = true;
                emit.at("metInLoop")
                        .say("slow and fast are on the same node - a cycle exists. Its exact "
                                + "start is still unknown; only phase 2 finds that.")
                        .list(render(values, loopPos, slow, fast)).step();
                break;
            }
        }

        if (!met) {
            emit.at("noLoop")
                    .say("fast reached the end of the list without ever meeting slow - there is no cycle.")
                    .var("answer", "null")
                    .list(render(values, loopPos, slow, null)).step();
            return;
        }

        slow = 0;
        emit.at("resetToHead")
                .say("Reset slow to the head. fast stays at the meeting point (%d). Advance "
                        + "both one step at a time from here.", values[fast])
                .var("slow", values[slow]).var("fast", values[fast])
                .list(render(values, loopPos, slow, fast)).step();

        while (!slow.equals(fast)) {
            slow = next(slow, n, loopPos);
            fast = next(fast, n, loopPos);
            emit.at("advanceBoth")
                    .say("slow moves to %d, fast moves to %d.", values[slow], values[fast])
                    .var("slow", values[slow]).var("fast", values[fast])
                    .list(render(values, loopPos, slow, fast)).step();
        }

        emit.at("foundStart")
                .say("slow and fast met at node %d (index %d) - this is where the loop begins.",
                        values[slow], slow)
                .var("answer", values[slow])
                .list(render(values, loopPos, slow, slow)).step();
    }
}
