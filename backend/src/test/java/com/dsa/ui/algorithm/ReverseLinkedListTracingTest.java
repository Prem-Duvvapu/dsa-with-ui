package com.dsa.ui.algorithm;

import com.dsa.ui.algorithm.linkedlist.ReverseLinkedList;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.service.LinkedListService;
import com.dsa.ui.trace.ListTraceRecorder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReverseLinkedListTracingTest {

    @Test
    void testReverseLinkedListFullTrace() {
        ReverseLinkedList.Node head = new ReverseLinkedList.Node(1);
        ReverseLinkedList.Node n2 = new ReverseLinkedList.Node(2);
        ReverseLinkedList.Node n3 = new ReverseLinkedList.Node(3);
        ReverseLinkedList.Node n4 = new ReverseLinkedList.Node(4);
        head.next = n2;
        n2.next = n3;
        n3.next = n4;

        ListTraceRecorder recorder = new ListTraceRecorder();
        ReverseLinkedList solver = new ReverseLinkedList();
        ReverseLinkedList.Node newHead = solver.solve(head, recorder);

        assertEquals(4, newHead.val, "Reversed head should be node 4");

        List<ExecutionStep> steps = recorder.toExecutionSteps();
        assertTrue(steps.size() >= 13, "Reverse linked list should record full pointer steps (>=13), actual: " + steps.size());
    }

    @Test
    void testLinkedListServiceReverseSteps() {
        LinkedListService service = new LinkedListService();
        List<ExecutionStep> steps = service.generateSteps("reverse-linked-list");
        assertNotNull(steps);
        assertTrue(steps.size() >= 3);
    }
}
