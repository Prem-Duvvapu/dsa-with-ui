package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.GraphBfsDfsService;
import com.dsa.ui.service.LegacyTraceRetiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GraphBfsDfsServiceTest {

    private GraphBfsDfsService service;

    @BeforeEach
    void setUp() {
        service = new GraphBfsDfsService();
    }

    @Test
    @DisplayName("Should load all 11 Striver A2Z Graph BFS & DFS problems")
    void testGetAllProblems() {
        // Every catalogued id in this topic is traced on /api/problems, so the legacy trace
        // is retired for all of them. This asserts the refusal for the whole catalogue rather
        // than a hand-maintained list: a list is what let stragglers keep falling through
        // `default:` and serving another algorithm's animation.
        for (ProblemDetail p : service.getAllProblems()) {
            assertThrows(LegacyTraceRetiredException.class,
                    () -> service.generateSteps(p.getId()),
                    p.getId() + " is traced by the v2 layer and must not fall back");
        }
    }

    @Test
    @DisplayName("flood-fill no longer has a legacy path: its grid trace comes from the tracer layer")
    void testGridProblemSteps() {
        // flood-fill was the last grid problem this service still served. Its FloodFillTracer
        // owns the grid trace now, and DsTypePayloadContractTest asserts that MATRIX tracers
        // populate gridState - so the assertion this test used to make lives there, against
        // the real algorithm rather than a hardcoded legacy narration.
        assertThrows(LegacyTraceRetiredException.class,
                () -> service.generateSteps("flood-fill"));
    }
}
