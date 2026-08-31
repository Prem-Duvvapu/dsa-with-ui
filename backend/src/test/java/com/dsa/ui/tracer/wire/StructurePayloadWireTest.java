package com.dsa.ui.tracer.wire;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.TrieNodeModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructurePayloadWireTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void queueCallStackAndTrieHaveIndependentCarryEmptyAndNullSemantics() throws Exception {
        TrieNodeModel root = new TrieNodeModel(
                0, "root", false, 100, 20,
                new LinkedHashMap<>(Map.of("a", 1)), "known");
        TrieNodeModel equalRebuiltRoot = new TrieNodeModel(
                0, "root", false, 100, 20,
                new LinkedHashMap<>(Map.of("a", 1)), "known");
        List<ExecutionStep> original = List.of(
                step(1, List.of("front", "back"), List.of("solve(3)"), List.of(root)),
                step(2, List.of("front", "back"), List.of("solve(3)"),
                        List.of(equalRebuiltRoot)),
                step(3, List.of(), List.of("solve(3)"), List.of(equalRebuiltRoot)),
                step(4, List.of(), List.of(), List.of()),
                step(5, null, null, null));

        List<DeltaStep> encoded = TraceEncoder.encode(original);

        assertTrue(Boolean.TRUE.equals(encoded.get(0).getKeyframe()));
        assertEquals(List.of("front", "back"), encoded.get(0).getQueueOrStackState());
        assertEquals(List.of("solve(3)"), encoded.get(0).getCallStack());
        assertEquals(List.of(root), encoded.get(0).getTrieState());
        assertNull(encoded.get(1).getQueueOrStackState(), "unchanged queue should be carried");
        assertNull(encoded.get(1).getCallStack(), "unchanged call stack should be carried");
        assertNull(encoded.get(1).getTrieState(),
                "separately constructed equal trie nodes should be carried");
        assertEquals(List.of(), encoded.get(2).getQueueOrStackState(),
                "empty must be sent so stale queue contents are cleared");
        assertNull(encoded.get(2).getCallStack(),
                "clearing the queue must not resend or clear the call stack");
        assertNull(encoded.get(2).getTrieState());
        assertNull(encoded.get(3).getQueueOrStackState(),
                "the already-empty queue should carry independently");
        assertEquals(List.of(), encoded.get(3).getCallStack(),
                "empty must be sent so stale call frames are cleared");
        assertEquals(List.of(), encoded.get(3).getTrieState(),
                "empty must be sent so stale trie nodes are cleared");
        assertTrue(Boolean.TRUE.equals(encoded.get(4).getKeyframe()),
                "a value returning to null needs a self-contained keyframe");
        assertNull(encoded.get(4).getQueueOrStackState());
        assertNull(encoded.get(4).getCallStack());
        assertNull(encoded.get(4).getTrieState());

        List<ExecutionStep> decoded = decode(encoded);
        for (int i = 0; i < original.size(); i++) {
            assertEquals(json.writeValueAsString(original.get(i)),
                    json.writeValueAsString(decoded.get(i)),
                    "step " + (i + 1) + " did not survive reference decoding");
        }
    }

    @Test
    void arrayLabelIsOptionalOnJsonAndParticipatesInDeltaEquality() throws Exception {
        ArrayElement oldShape = new ArrayElement(0, 65, "known");
        ArrayElement labelledA = new ArrayElement(0, 65, "known", "A");
        ArrayElement labelledB = new ArrayElement(0, 65, "known", "B");
        ExecutionStep first = step(1, List.of(), List.of(), null);
        first.setArrayState(List.of(labelledA));
        ExecutionStep second = step(2, List.of(), List.of(), null);
        second.setArrayState(List.of(labelledB));
        ExecutionStep third = step(3, List.of(), List.of(), null);
        third.setArrayState(List.of(new ArrayElement(0, 65, "known", "B")));

        List<DeltaStep> encoded = TraceEncoder.encode(List.of(first, second, third));

        assertFalse(json.writeValueAsString(oldShape).contains("label"),
                "the old constructor must retain its old wire shape");
        assertTrue(json.writeValueAsString(labelledA).contains("\"label\":\"A\""));
        assertNotNull(encoded.get(1).getArrayState(),
                "changing only the label must still transmit the array state");
        assertNull(encoded.get(2).getArrayState(),
                "an equal label should allow the array state to be carried");
    }

    private static ExecutionStep step(int number, List<String> queueOrStackState,
                                      List<String> callStack,
                                      List<TrieNodeModel> trieState) {
        ExecutionStep step = new ExecutionStep();
        step.setStepNumber(number);
        step.setQueueOrStackState(queueOrStackState);
        step.setCallStack(callStack);
        step.setTrieState(trieState);
        return step;
    }

    /** Literal reference decoder matching TraceEncoderTest and the browser decoder. */
    private static List<ExecutionStep> decode(List<DeltaStep> deltas) {
        List<ExecutionStep> decoded = new ArrayList<>(deltas.size());
        ExecutionStep carried = null;
        for (DeltaStep delta : deltas) {
            ExecutionStep previous = Boolean.TRUE.equals(delta.getKeyframe()) ? null : carried;
            ExecutionStep step = new ExecutionStep();
            step.setStepNumber(delta.getStepNumber());
            step.setActiveLine(delta.getActiveLine());
            step.setDescription(delta.getDescription());
            step.setQueueOrStackState(pick(delta.getQueueOrStackState(),
                    previous == null ? null : previous.getQueueOrStackState()));
            step.setCallStack(pick(delta.getCallStack(),
                    previous == null ? null : previous.getCallStack()));
            step.setTrieState(pick(delta.getTrieState(),
                    previous == null ? null : previous.getTrieState()));
            decoded.add(step);
            carried = step;
        }
        return decoded;
    }

    private static <T> T pick(T sent, T carried) {
        return sent != null ? sent : carried;
    }
}
