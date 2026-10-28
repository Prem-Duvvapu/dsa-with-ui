package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.junit.jupiter.api.Test;
import java.util.Locale;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PhoneTraceBudgetTest {
    @Test
    void shapeCostExactlyMatchesRealEmitterAccountingAcrossEveryShortBranchPattern() {
        for (int length = 1; length <= 5; length++) for (int mask = 0; mask < (1 << length); mask++) {
            var digits = new StringBuilder();
            for (int index = 0; index < length; index++) digits.append((mask & (1 << index)) == 0 ? '2' : '7');
            assertMeasuredCost(digits.toString());
        }
        for (String digits : new String[]{"222222", "222227", "99999", "23456", "98765", "27938"}) assertMeasuredCost(digits);
    }

    @Test
    void exactBudgetIsAcceptedAndOneLessStepOrByteIsRefused() {
        var cost = PhoneTraceBudget.estimate("23");
        int steps = Math.toIntExact(cost.steps());
        var exact = InputSpec.of().withMaxSteps(steps).withMaxBytes(cost.bytes());
        assertDoesNotThrow(() -> PhoneTraceBudget.requireFits("23", exact));
        assertTrue(assertThrows(InputValidationException.class,
                () -> PhoneTraceBudget.requireFits("23", exact.withMaxSteps(steps - 1))).getFieldErrors().containsKey("digits"));
        assertTrue(assertThrows(InputValidationException.class,
                () -> PhoneTraceBudget.requireFits("23", exact.withMaxBytes(cost.bytes() - 1))).getFieldErrors().containsKey("digits"));
    }

    @Test
    void captionsAndCostDoNotDriftWithServerLocale() {
        var locale = Locale.getDefault();
        try { Locale.setDefault(Locale.forLanguageTag("ar")); assertMeasuredCost("77777"); }
        finally { Locale.setDefault(locale); }
    }

    private static void assertMeasuredCost(String digits) {
        var tracer = new LetterCombinationsPhoneTracer();
        var trace = new TraceRunner().run(tracer, Map.of("digits", digits));
        assertFalse(trace.isTruncated(), digits);
        var estimated = PhoneTraceBudget.estimate(digits);
        assertEquals(trace.getStepCount(), estimated.steps(), digits);
        assertEquals(trace.getSteps().stream().mapToLong(StepEmitter::estimateBytes).sum(), estimated.bytes(), digits);
        assertTrue(estimated.bytes() <= tracer.inputSpec().getMaxBytes(), digits);
    }
}
