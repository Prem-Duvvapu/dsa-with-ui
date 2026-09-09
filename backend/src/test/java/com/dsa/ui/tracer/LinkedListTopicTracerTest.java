package com.dsa.ui.tracer;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ListNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LinkedListTopicTracerTest {

    @Autowired private TracerRegistry registry;
    @Autowired private TraceRunner runner;

    Stream<Arguments> expectedDefaultResults() {
        return Stream.of(
                Arguments.of("middle-linked-list", "result", "3"),
                Arguments.of("intro-singly-ll", "length", "4"),
                Arguments.of("insert-head-ll", "result", "[1, 2, 3, 4]"),
                Arguments.of("delete-head-ll", "result", "[2, 3, 4]"),
                Arguments.of("length-ll", "result", "4"),
                Arguments.of("search-ll", "result", "2"),
                Arguments.of("intro-doubly-ll", "head.prev", "null"),
                Arguments.of("insert-head-dll", "result", "[1, 2, 4, 6]"),
                Arguments.of("delete-head-dll", "result", "[2, 3, 4]"),
                Arguments.of("reverse-dll", "result", "[4, 3, 2, 1]"),
                Arguments.of("reverse-ll-recursive", "result", "[4, 3, 2, 1]"),
                Arguments.of("detect-loop-linked-list", "result", "true"),
                Arguments.of("length-of-loop-ll", "result", "4"),
                Arguments.of("palindrome-ll", "result", "true"),
                Arguments.of("segregate-odd-even-ll", "result", "[1, 3, 5, 2, 4, 6]"),
                Arguments.of("remove-nth-from-back", "result", "[1, 2, 3, 5]"),
                Arguments.of("delete-middle-node-ll", "result", "[1, 2, 4, 5]"),
                Arguments.of("sort-ll", "result", "[1, 2, 3, 4]"),
                Arguments.of("sort-012-ll", "result", "[0, 0, 1, 1, 2, 2]"),
                Arguments.of("intersection-point-y-ll", "result", "8"),
                Arguments.of("add-one-to-number-ll", "result", "[1, 3, 0]"),
                Arguments.of("add-two-numbers-ll", "result", "[7, 0, 8]"),
                Arguments.of("delete-occurrences-key-dll", "result", "[1, 3, 4]"),
                Arguments.of("pairs-given-sum-dll", "result", "[[1, 6], [2, 5], [3, 4]]"),
                Arguments.of("remove-duplicates-sorted-dll", "result", "[1, 2, 3, 4]"),
                Arguments.of("rotate-ll", "result", "[4, 5, 1, 2, 3]"));
    }

    @ParameterizedTest(name = "{0} returns its documented default result")
    @MethodSource("expectedDefaultResults")
    void defaultResultsAreAlgorithmicallyCorrect(String id, String variable, String expected) {
        ExecutionTrace trace = runner.runDefaults(registry.find(id).orElseThrow());
        ExecutionStep last = trace.getSteps().get(trace.getSteps().size() - 1);
        assertEquals(expected, last.getVariables().get(variable), id);
    }

    @Test
    void everyDoublyLinkedSnapshotKeepsReciprocalLinks() {
        List<String> ids = List.of(
                "intro-doubly-ll", "insert-head-dll", "delete-head-dll", "reverse-dll",
                "delete-occurrences-key-dll", "pairs-given-sum-dll", "remove-duplicates-sorted-dll");

        for (String id : ids) {
            ExecutionTrace trace = runner.runDefaults(registry.find(id).orElseThrow());
            for (ExecutionStep step : trace.getSteps()) {
                List<ListNode> nodes = step.getListState();
                if (nodes == null) continue;
                Map<Integer, ListNode> byId = nodes.stream()
                        .collect(Collectors.toMap(ListNode::getId, Function.identity()));
                for (ListNode node : nodes) {
                    if (node.getNextId() != null) {
                        assertEquals(node.getId(), byId.get(node.getNextId()).getPrevId(),
                                id + " step " + step.getStepNumber() + " has a one-way next link");
                    }
                    if (node.getPrevId() != null) {
                        assertEquals(node.getId(), byId.get(node.getPrevId()).getNextId(),
                                id + " step " + step.getStepNumber() + " has a one-way prev link");
                    }
                }
            }
        }
    }

    @Test
    void duplicateValuesStillReceiveDistinctStableIdentities() {
        AlgorithmTracer tracer = registry.find("intro-singly-ll").orElseThrow();
        ExecutionTrace trace = runner.run(tracer, Map.of("values", List.of(5, 5, 5)));
        List<ListNode> nodes = trace.getSteps().get(trace.getSteps().size() - 1).getListState();
        assertEquals(3, nodes.stream().map(ListNode::getId).distinct().count());
    }
}
