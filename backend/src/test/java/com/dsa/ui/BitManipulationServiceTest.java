package com.dsa.ui;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.BitManipulationService;
import com.dsa.ui.service.LegacyTraceRetiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BitManipulationServiceTest {

    private BitManipulationService service;

    @BeforeEach
    public void setUp() {
        service = new BitManipulationService();
    }

    @Test
    public void testGetAllProblems() {
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
}
