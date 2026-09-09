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
        List<ProblemDetail> problems = service.getAllProblems();
        assertNotNull(problems);
        assertEquals(18, problems.size(), "Should load 18 Bit & Math algorithms");
    }

    @Test
    public void testGetProblemById() {
        ProblemDetail problem = service.getProblemById("single-number-1");
        assertNotNull(problem);
    }

    @Test
    public void testGenerateStepsForAllBitMathProblems() {
        Set<String> retired = Set.of("single-number-1", "check-power-of-2", "count-set-bits",
                "xor-numbers-in-range", "single-number-3", "pow-x-n-math",
                "power-set-bitwise", "intro-bits-tricks", "check-ith-bit-set", "check-number-odd",
                "set-unset-rightmost-bit", "swap-two-numbers", "divide-two-numbers-bitwise",
                "min-bit-flips", "print-prime-factors", "divisors-of-number",
                "count-primes-range-sieve", "prime-factorisation-queries");
        List<ProblemDetail> problems = service.getAllProblems();
        for (ProblemDetail p : problems) {
            if (retired.contains(p.getId())) {
                assertThrows(LegacyTraceRetiredException.class,
                        () -> service.generateSteps(p.getId()),
                        p.getId() + " is traced by the v2 layer and must not fall back");
                continue;
            }
            List<ExecutionStep> steps = service.generateSteps(p.getId());
            assertNotNull(steps, "Steps list should not be null for " + p.getId());
            assertFalse(steps.isEmpty(), "Steps list should not be empty for " + p.getId());
        }
    }
}
