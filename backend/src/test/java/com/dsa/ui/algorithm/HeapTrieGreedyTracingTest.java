package com.dsa.ui.algorithm;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.service.GreedyService;
import com.dsa.ui.service.HeapService;
import com.dsa.ui.service.LegacyTraceRetiredException;
import com.dsa.ui.service.TrieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HeapTrieGreedyTracingTest {

    private HeapService heapService;
    private TrieService trieService;
    private GreedyService greedyService;

    @BeforeEach
    void setUp() {
        heapService = new HeapService();
        trieService = new TrieService();
        greedyService = new GreedyService();
    }

    @Test
    void testKthLargestTracing() {
        List<ExecutionStep> steps = heapService.generateSteps("kth-largest-element");
        assertNotNull(steps);
        assertTrue(steps.size() >= 8, "Kth Largest Element should have >=8 steps, actual: " + steps.size());
    }

    @Test
    void testImplementTrieTracing() {
        // implement-trie has a real tracer now (ImplementTrieTracer) — the legacy path
        // retires it rather than serving substitute steps. Content is pinned by its
        // golden file.
        assertThrows(LegacyTraceRetiredException.class,
                () -> trieService.generateSteps("implement-trie"));
    }

    @Test
    void testNMeetingsTracing() {
        // n-meetings-in-one-room has a real tracer now — the legacy path retires it
        // rather than serving substitute steps. Content is pinned by its golden file.
        assertThrows(LegacyTraceRetiredException.class,
                () -> greedyService.generateSteps("n-meetings-in-one-room"));
    }
}
