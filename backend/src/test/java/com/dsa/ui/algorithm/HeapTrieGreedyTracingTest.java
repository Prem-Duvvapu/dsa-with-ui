package com.dsa.ui.algorithm;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.service.GreedyService;
import com.dsa.ui.service.HeapService;
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
        List<ExecutionStep> steps = trieService.generateSteps("implement-trie");
        assertNotNull(steps);
        assertTrue(steps.size() >= 6, "Implement Trie should have >=6 steps, actual: " + steps.size());
    }

    @Test
    void testNMeetingsTracing() {
        List<ExecutionStep> steps = greedyService.generateSteps("n-meetings-in-one-room");
        assertNotNull(steps);
        assertTrue(steps.size() >= 5, "N Meetings in One Room should have >=5 steps, actual: " + steps.size());
    }
}
