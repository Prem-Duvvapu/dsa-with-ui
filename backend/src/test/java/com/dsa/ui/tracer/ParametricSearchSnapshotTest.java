package com.dsa.ui.tracer;

import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.tracer.impl.SmallestDivisorTracer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Complete-source minimum-feasible searches state one coherent snapshot per step.
 * Bounds must not be assembled from unrelated steps, and a scan's input highlight
 * must not disappear behind the canvas's last paired-bound snapshot.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParametricSearchSnapshotTest {
    @Autowired TracerRegistry registry;
    @Autowired TraceRunner runner;

    Stream<CompleteSourceTracer> family() {
        return registry.all().stream().filter(CompleteSourceTracer.class::isInstance)
                .map(CompleteSourceTracer.class::cast).filter(t -> t.dsType() == DsType.SEARCH_SPACE);
    }

    @ParameterizedTest(name = "{0}: default snapshots and both update branches")
    @MethodSource("family")
    void defaultsKeepActualPairedBoundsThroughBothBranches(CompleteSourceTracer tracer) {
        var phases = check(tracer, runner.runDefaults(tracer));
        assertTrue(phases.contains("feasible"), tracer.id());
        assertTrue(phases.contains("infeasible"), tracer.id());
    }

    @ParameterizedTest(name = "{0}: alternate snapshots or explicit impossibility")
    @MethodSource("family")
    void alternateInputsKeepSnapshotsOrStateImpossibilityWithoutInventedBounds(CompleteSourceTracer tracer) {
        check(tracer, runner.run(tracer, tracer.alternateInput()));
    }

    @ParameterizedTest(name = "{0}: declared size/value/scalar boundaries")
    @MethodSource("family")
    void boundariesKeepTheCurrentProbeAndExhaustedTerminalInterval(CompleteSourceTracer tracer) {
        var spec = tracer.inputSpec();
        var array = spec.getFields().stream().filter(f -> f.getType() == FieldType.INT_ARRAY)
                .findFirst().orElseThrow();
        for (int length : List.of(array.intConstraint("minLength"), array.intConstraint("maxLength"))) {
            for (int value : List.of(array.intConstraint("minValue"), array.intConstraint("maxValue"))) {
                var input = new LinkedHashMap<String, Object>();
                input.put(array.getName(), Collections.nCopies(length, value));
                for (var field : spec.getFields()) {
                    if (field.getType() == FieldType.INT) {
                        int minimum = field.intConstraint("min");
                        // Positive ceiling terms cannot total less than the array length.
                        if (SmallestDivisorTracer.class.isInstance(tracer)) minimum = Math.max(minimum, length);
                        input.put(field.getName(), minimum);
                    }
                }
                check(tracer, runner.run(tracer, input));
            }
        }
        var upper = new LinkedHashMap<String, Object>();
        upper.put(array.getName(), Collections.nCopies(array.intConstraint("maxLength"), array.intConstraint("maxValue")));
        for (var field : spec.getFields()) {
            if (field.getType() == FieldType.INT) upper.put(field.getName(), field.intConstraint("max"));
        }
        check(tracer, runner.run(tracer, upper));
    }

    @ParameterizedTest(name = "{0}: a truncated scan is not terminal")
    @MethodSource("family")
    void budgetLimitedTailDoesNotClaimAnAnswerOrTerminalStep(CompleteSourceTracer tracer) {
        AlgorithmTracer limited = new AlgorithmTracer() {
            @Override public String id() { return tracer.id(); }
            @Override public DsType dsType() { return tracer.dsType(); }
            @Override public InputSpec inputSpec() { return tracer.inputSpec().withMaxSteps(3); }
            @Override public Map<String, Object> alternateInput() { return tracer.alternateInput(); }
            @Override public String annotatedCode() { return tracer.annotatedCode(); }
            @Override public void run(Inputs input, StepEmitter emit) { tracer.run(input, emit); }
        };
        var trace = runner.runDefaults(limited);
        assertTrue(trace.isTruncated());
        assertEquals(3, trace.getStepCount());
        var code = AnnotatedCode.parse(tracer.annotatedCode());
        for (var step : trace.getSteps()) {
            assertNotEquals(code.resolve("done"), step.getActiveLine());
            assertFalse(step.getVariables().containsKey("answer"));
        }
        var tail = trace.getSteps().get(2);
        assertTrue(number(tail, "low") <= number(tail, "high"));
        assertTrue(tail.getVariables().containsKey("mid"), "A truncated scan retains its actual probe");
    }

    private Set<String> check(CompleteSourceTracer tracer, ExecutionTrace trace) {
        assertFalse(trace.isTruncated(), tracer.id());
        assertFalse(trace.getSteps().isEmpty(), tracer.id());
        var code = AnnotatedCode.parse(tracer.annotatedCode());
        var anchors = code.getAnchors();
        var first = trace.getSteps().get(0);
        if (anchors.containsKey("impossible") && first.getActiveLine() == code.resolve("impossible")) {
            assertEquals(1, trace.getStepCount());
            assertEquals("-1", first.getVariables().get("answer"));
            for (String key : List.of("low", "high", "mid")) assertFalse(first.getVariables().containsKey(key));
            return Set.of("impossible");
        }
        assertEquals(code.resolve("init"), first.getActiveLine(), tracer.id());
        int low = number(first, "low"), high = number(first, "high");
        assertTrue(low <= high, tracer.id());
        assertFalse(first.getVariables().containsKey("mid"));
        Integer probe = null;
        Set<String> phases = new HashSet<>();
        for (int i = 1; i < trace.getStepCount(); i++) {
            var step = trace.getSteps().get(i);
            int statedLow = number(step, "low"), statedHigh = number(step, "high");
            if (step.getActiveLine() == code.resolve("mid")) {
                assertNull(probe, "A new probe follows the previous bound update");
                assertEquals(low, statedLow);
                assertEquals(high, statedHigh);
                probe = low + (high - low) / 2;
                assertEquals(probe.intValue(), number(step, "mid"));
            } else if (step.getActiveLine() == code.resolve("feasible")) {
                assertNotNull(probe);
                assertEquals(low, statedLow);
                assertEquals(probe - 1, statedHigh);
                assertEquals(String.valueOf(probe), step.getVariables().get("ans"));
                assertFalse(step.getVariables().containsKey("mid"));
                high = statedHigh;
                probe = null;
                phases.add("feasible");
            } else if (step.getActiveLine() == code.resolve("infeasible")) {
                assertNotNull(probe);
                assertEquals(probe + 1, statedLow);
                assertEquals(high, statedHigh);
                assertFalse(step.getVariables().containsKey("mid"));
                low = statedLow;
                probe = null;
                phases.add("infeasible");
            } else if (step.getActiveLine() == code.resolve("done")) {
                assertNull(probe);
                assertEquals(trace.getStepCount() - 1, i);
                assertEquals(low, statedLow);
                assertEquals(high, statedHigh);
                assertEquals(high + 1, low, "An integer search ends with an exhausted interval");
                assertEquals(String.valueOf(low), step.getVariables().get("answer"));
                assertFalse(step.getVariables().containsKey("mid"));
                phases.add("done");
            } else {
                assertNotNull(probe, "A feasibility scan belongs to the current probe");
                assertEquals(low, statedLow);
                assertEquals(high, statedHigh);
                assertEquals(probe.intValue(), number(step, "mid"));
            }
        }
        assertTrue(phases.contains("done"), tracer.id());
        return phases;
    }

    private int number(ExecutionStep step, String name) {
        String raw = step.getVariables().get(name);
        assertNotNull(raw, () -> "Step " + step.getStepNumber() + " has no paired " + name + " snapshot");
        return Integer.parseInt(raw);
    }
}
