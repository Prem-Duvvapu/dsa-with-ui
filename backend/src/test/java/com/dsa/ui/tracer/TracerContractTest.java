package com.dsa.ui.tracer;

import com.dsa.ui.catalog.ProblemCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The contract every tracer must satisfy, enforced across the whole registry.
 *
 * <p>The previous suite could not tell a real implementation from a stub: its only
 * per-problem assertion was {@code !steps.isEmpty()}, which the old {@code default:}
 * branch guaranteed for every id including typos. 303 of 440 problems returned another
 * algorithm's animation and all 90 tests passed.
 *
 * <p>The two tests that close that hole are {@link #traceRespondsToItsInput} and
 * {@link #noTwoTracersProduceIdenticalTraces}.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TracerContractTest {

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    @Autowired
    private ProblemCatalog catalog;

    /**
     * Every tracer the application registers, in a stable order.
     *
     * <p>This used to mirror a hardcoded map, kept honest by a test asserting the two
     * matched. That works for 8 tracers and collapses at 433: every new tracer would edit
     * one central file. The registry is the source of truth now, and the second input each
     * test needs comes from {@link AlgorithmTracer#alternateInput()} — so adding a tracer
     * touches only its own file.
     *
     * <p>Non-static, which @MethodSource permits only under {@code Lifecycle.PER_CLASS}.
     * That is why this class declares it: the alternative is a static context holder,
     * which is more machinery for the same result.
     */
    Stream<String> tracerIds() {
        return registry.tracedIds().stream().sorted();
    }

    @ParameterizedTest(name = "{0} runs on its declared defaults")
    @MethodSource("tracerIds")
    @DisplayName("Defaults are valid and produce a trace")
    void runsOnDefaults(String id) {
        ExecutionTrace trace = runner.runDefaults(registry.find(id).orElseThrow());
        assertFalse(trace.getSteps().isEmpty(), id + " produced no steps on its own defaults");
        assertFalse(trace.isTruncated(), id + " cannot even finish its default input");
    }

    @ParameterizedTest(name = "{0} steps are well formed")
    @MethodSource("tracerIds")
    @DisplayName("Steps are sequential, described, and point at a real line")
    void stepsAreWellFormed(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        ExecutionTrace trace = runner.runDefaults(tracer);
        int lineCount = AnnotatedCode.parse(tracer.annotatedCode()).lineCount();

        List<ExecutionStep> steps = trace.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            ExecutionStep s = steps.get(i);
            assertEquals(i + 1, s.getStepNumber(), id + " step numbering is not sequential");
            assertNotNull(s.getDescription(), id + " step " + (i + 1) + " has no description");
            assertFalse(s.getDescription().isBlank(), id + " step " + (i + 1) + " has a blank description");
            assertTrue(s.getActiveLine() >= 1 && s.getActiveLine() <= lineCount,
                    id + " step " + (i + 1) + " highlights line " + s.getActiveLine()
                            + ", outside its " + lineCount + "-line source");
        }
    }

    @ParameterizedTest(name = "{0} declares no unused anchors")
    @MethodSource("tracerIds")
    @DisplayName("Anchors resolve, and none are dead")
    void anchorsAreAllReachable(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        AnnotatedCode code = AnnotatedCode.parse(tracer.annotatedCode());
        assertFalse(code.getAnchors().isEmpty(), id + " declares no anchors at all");

        // Every anchor must be reachable from some input, not merely parse. Collecting the
        // lines and asserting the list was non-empty is what this test used to do, and it
        // was vacuous in exactly the way !steps.isEmpty() was: a tracer could declare ten
        // anchors, emit one, and pass.
        Set<Integer> usedLines = new HashSet<>();
        for (Map<String, Object> input : List.of(Map.<String, Object>of(), alternate(id))) {
            runner.run(tracer, input).getSteps().forEach(s -> usedLines.add(s.getActiveLine()));
        }
        assertFalse(usedLines.isEmpty(), id + " emitted nothing");

        List<String> dead = code.getAnchors().entrySet().stream()
                .filter(e -> !usedLines.contains(e.getValue()))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        assertTrue(dead.isEmpty(), id + " declares " + code.getAnchors().size()
                + " anchors but never highlights " + dead + " across its default or its"
                + " alternate input — either emit them, or delete the marker");
    }

    /**
     * The core test. A canned narration ignores its input and so produces the same
     * trace for materially different data.
     */
    @ParameterizedTest(name = "{0} actually reads its input")
    @MethodSource("tracerIds")
    @DisplayName("A different input produces a different trace")
    void traceRespondsToItsInput(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        String onDefaults = fingerprint(runner.runDefaults(tracer));
        String onAlternate = fingerprint(runner.run(tracer, alternate(id)));

        assertNotEquals(onDefaults, onAlternate,
                id + " produced an identical trace for two different inputs, so it is not "
                        + "executing the algorithm — it is replaying a fixed narration");
    }

    @Test
    @DisplayName("No two problems share a trace")
    void noTwoTracersProduceIdenticalTraces() {
        Map<String, String> byFingerprint = new LinkedHashMap<>();
        List<String> collisions = new ArrayList<>();

        for (AlgorithmTracer tracer : registry.all()) {
            String print = fingerprint(runner.runDefaults(tracer));
            String owner = byFingerprint.putIfAbsent(print, tracer.id());
            if (owner != null) {
                collisions.add(tracer.id() + " is identical to " + owner);
            }
        }
        assertEquals(List.of(), collisions,
                "These problems render the same animation as another problem");
    }

    @Test
    @DisplayName("The byte budget stops a trace whose steps are individually huge")
    void byteBudgetTruncates() {
        // 400 array elements per step. Well inside the step budget, nowhere near inside a
        // response a browser can use — which is the case maxSteps alone cannot see.
        int[] wide = new int[400];
        AlgorithmTracer heavy = new AlgorithmTracer() {
            @Override public String id() { return "test-only-heavy"; }
            @Override public InputSpec inputSpec() {
                return InputSpec.of().withMaxSteps(10_000).withMaxBytes(50_000);
            }
            @Override public Map<String, Object> alternateInput() { return Map.of(); }
            @Override public String annotatedCode() { return "// @a fat\nwhile (true) {}"; }
            @Override public void run(Inputs in, StepEmitter emit) {
                for (int i = 0; i < 10_000; i++) {
                    emit.at("fat").say("step %d", i).array(wide, i % wide.length).step();
                }
            }
        };

        ExecutionTrace trace = runner.runDefaults(heavy);

        assertTrue(trace.isTruncated(), "an over-budget trace must report itself truncated");
        assertTrue(trace.getStepCount() < 10_000,
                "the byte ceiling must bite long before the 10,000-step ceiling");
        assertNotNull(trace.getTruncationReason(), "a truncated trace must say why");
        assertTrue(trace.getTruncationReason().contains("KB response limit"),
                "the reason must name the byte ceiling, not the step ceiling: "
                        + trace.getTruncationReason());

        long collected = trace.getSteps().stream().mapToLong(StepEmitter::estimateBytes).sum();
        assertTrue(collected <= 50_000,
                "the collected trace must be within budget, not merely stop after exceeding it;"
                        + " it weighs " + collected);
    }

    /**
     * The byte ceiling is enforced against an estimate, because serialising every step to
     * measure it would cost more than generating it. That makes the estimate load-bearing:
     * if it drifts low, the ceiling silently stops enforcing anything.
     */
    @ParameterizedTest(name = "{0} byte estimate tracks its real payload")
    @MethodSource("tracerIds")
    @DisplayName("The byte estimate stays close to the serialised size")
    void byteEstimateTracksActualPayload(String id) throws Exception {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        ExecutionTrace trace = runner.runDefaults(tracer);

        long estimated = trace.getSteps().stream().mapToLong(StepEmitter::estimateBytes).sum();
        long actual = new ObjectMapper().writeValueAsBytes(trace.getSteps()).length;
        double ratio = (double) estimated / actual;

        assertTrue(ratio >= 0.9 && ratio <= 2.5, String.format(
                "%s estimates %d bytes for a payload that serialises to %d (ratio %.2f)."
                        + " The budget is only as good as this estimate — recalibrate the"
                        + " per-element constants in StepEmitter.estimateBytes.",
                id, estimated, actual, ratio));
    }

    @Test
    @DisplayName("The step budget stops a runaway trace instead of exhausting the server")
    void stepBudgetTruncates() {
        AlgorithmTracer greedy = new AlgorithmTracer() {
            @Override public String id() { return "test-only-runaway"; }
            @Override public InputSpec inputSpec() { return InputSpec.of().withMaxSteps(25); }
            @Override public Map<String, Object> alternateInput() { return Map.of(); }
            @Override public String annotatedCode() { return "// @a spin\nwhile (true) {}"; }
            @Override public void run(Inputs in, StepEmitter emit) {
                for (int i = 0; i < 1_000_000; i++) {
                    emit.at("spin").say("step %d", i).step();
                }
            }
        };

        ExecutionTrace trace = runner.runDefaults(greedy);
        assertTrue(trace.isTruncated(), "an over-budget trace must report itself truncated");
        assertEquals(25, trace.getSteps().size(), "the budget is a hard cap");
        assertTrue(trace.getTruncationReason().contains("25-step limit"),
                "the reason must name the step ceiling: " + trace.getTruncationReason());
    }

    @Test
    @DisplayName("Every tracer has a catalogue entry, so it is reachable in the UI")
    void noOrphanedTracers() {
        assertEquals(List.of(), catalog.getOrphanedTracerIds(),
                "These tracers work but no catalogue entry lists them, so nobody can reach them");
    }

    @Test
    @DisplayName("The catalogue reports traced coverage honestly")
    void catalogReportsCoverage() {
        assertEquals(registry.size(), catalog.tracedCount(),
                "every registered tracer should be marked traced in the catalogue");
        assertTrue(catalog.size() > catalog.tracedCount(),
                "coverage is not yet complete; this test flips when the migration finishes");
    }

    private Map<String, Object> alternate(String id) {
        Map<String, Object> input = registry.find(id).orElseThrow().alternateInput();
        assertNotNull(input, id + " returned a null alternate input");
        return input;
    }


    /**
     * {@link #traceRespondsToItsInput} compares a trace on the defaults against a trace on
     * the alternate input. If the two inputs are the same, it compares a trace with itself
     * and passes while proving nothing — so the alternate must actually differ.
     */
    @ParameterizedTest(name = "{0} alternate input differs from its defaults")
    @MethodSource("tracerIds")
    @DisplayName("The alternate input is not a copy of the defaults")
    void alternateInputDiffersFromDefaults(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();

        Map<String, Object> defaults = new LinkedHashMap<>();
        Set<String> declared = new LinkedHashSet<>();
        for (InputField field : tracer.inputSpec().getFields()) {
            defaults.put(field.getName(), field.getDefaultValue());
            declared.add(field.getName());
        }

        Map<String, Object> alternate = tracer.alternateInput();
        assertFalse(alternate.isEmpty(), id + " declares an empty alternate input");
        assertTrue(declared.containsAll(alternate.keySet()),
                id + " alternate input sets fields its spec does not declare: "
                        + alternate.keySet().stream().filter(k -> !declared.contains(k)).toList());

        // Compare the EFFECTIVE input, so an alternate that names a subset of the fields
        // and repeats their default values is still caught.
        Map<String, Object> effective = new LinkedHashMap<>(defaults);
        effective.putAll(alternate);
        assertNotEquals(defaults, effective,
                id + " alternate input resolves to the spec defaults, so traceRespondsToItsInput "
                        + "compares a trace against itself and cannot detect a canned narration");
    }


    /** Field kinds whose size can be scaled without inventing a new problem. */
    private static final Set<FieldType> GROWABLE =
            EnumSet.of(FieldType.INT_ARRAY, FieldType.INT_GRID, FieldType.LINKED_LIST, FieldType.BINARY_TREE);

    /**
     * A tracer that reads its input does more work when given more of it.
     *
     * <p>This catches a narration that varies its wording with the data but emits a fixed
     * number of steps — something {@link #traceRespondsToItsInput} cannot see, because two
     * different-but-equal-length narrations already have different fingerprints.
     *
     * <p>Tracers with nothing scalable are skipped rather than passed. Surefire reports the
     * skip and its reason; a silently green case would read as coverage that does not exist.
     */
    @ParameterizedTest(name = "{0} does more work on a larger input")
    @MethodSource("tracerIds")
    @DisplayName("Step count grows with input size")
    void stepCountGrowsWithInput(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();

        List<InputField> growables = tracer.inputSpec().getFields().stream()
                .filter(f -> GROWABLE.contains(f.getType()))
                .toList();
        assumeTrue(!growables.isEmpty(), id + " declares no INT_ARRAY / INT_GRID / LINKED_LIST /"
                + " BINARY_TREE field, so there is nothing to scale");

        // Scale EVERY growable field, not just the first: a tracer whose inputs travel in
        // pairs (meeting starts and ends, matrix and vector) rejects one-sided growth,
        // and rejecting valid-looking input here reads as a broken tracer.
        Map<String, Object> larger = new LinkedHashMap<>();
        for (InputField field : tracer.inputSpec().getFields()) {
            larger.put(field.getName(), field.getDefaultValue());
        }
        for (InputField field : growables) {
            larger.put(field.getName(), scaleUp(field));
        }

        int onDefaults = runner.runDefaults(tracer).getSteps().size();
        int onLarger = runner.run(tracer, larger).getSteps().size();

        assertTrue(onLarger > onDefaults, id + " emitted " + onLarger + " steps for larger "
                + growables.stream().map(InputField::getName).toList()
                + " and " + onDefaults + " for its defaults — the step count"
                + " does not depend on how much input there is");
    }

    /**
     * A bigger version of this field's default, still satisfying its own constraints.
     *
     * <p>Extra values go at the FRONT for lists. Appending would leave an early-exiting
     * algorithm finishing in the same number of steps — two-sum finds 2 + 7 at indices 0
     * and 1 however much you bolt on the end.
     */
    private Object scaleUp(InputField field) {
        Object base = field.getDefaultValue();
        return switch (field.getType()) {
            case INT_ARRAY, LINKED_LIST -> growList(field, asIntList(base));
            case INT_GRID -> growGrid(field, (List<?>) base);
            case BINARY_TREE -> growTree(field, ((List<?>) base).size());
            default -> throw new IllegalStateException("not growable: " + field.getType());
        };
    }

    private List<Integer> growList(InputField field, List<Integer> base) {
        int cap = field.intConstraint("maxLength") != null ? field.intConstraint("maxLength") : base.size() * 2;
        int target = Math.min(Math.max(base.size() * 2, base.size() + 1), cap);
        int extra = target - base.size();
        assertTrue(extra > 0, field.getName() + " cannot be grown within its own maxLength");

        Integer minValue = field.intConstraint("minValue");
        Integer maxValue = field.intConstraint("maxValue");
        boolean sorted = field.flag("requireSorted");
        boolean distinct = field.flag("requireDistinct");

        List<Integer> grown = new ArrayList<>();
        if (sorted) {
            // Keep it sorted and distinct by extending below the first element, or above
            // the last if there is no room underneath.
            int first = base.isEmpty() ? 0 : base.get(0);
            if (minValue == null || first - extra >= minValue) {
                for (int k = extra; k >= 1; k--) {
                    grown.add(first - k);
                }
                grown.addAll(base);
            } else {
                grown.addAll(base);
                int last = base.get(base.size() - 1);
                for (int k = 1; k <= extra; k++) {
                    grown.add(last + k);
                }
            }
        } else if (distinct) {
            // Distinct but unordered (a rotated array, say): extending below the first
            // element would collide with values that already sit later in it. Walk the
            // declared range from the bottom and take only values not already present.
            int lo = minValue != null ? minValue : -999;
            int hi = maxValue != null ? maxValue : 999;
            java.util.Set<Integer> used = new java.util.HashSet<>(base);
            for (int v = lo; v <= hi && grown.size() < extra; v++) {
                if (!used.contains(v)) {
                    grown.add(v);
                    used.add(v);
                }
            }
            assertTrue(grown.size() == extra,
                    field.getName() + " cannot grow within its declared value range");
            grown.addAll(base);
        } else {
            int filler = maxValue != null ? maxValue : 999;
            for (int k = 0; k < extra; k++) {
                grown.add(filler);
            }
            grown.addAll(base);
        }
        return grown;
    }

    private List<?> growGrid(InputField field, List<?> base) {
        Integer maxRows = field.intConstraint("maxRows");
        int target = maxRows != null ? Math.min(base.size() * 2, maxRows) : base.size() * 2;
        assertTrue(target > base.size(), field.getName() + " cannot be grown within its own maxRows");

        List<Object> grown = new ArrayList<>(base);
        for (int r = 0; grown.size() < target; r++) {
            grown.add(base.get(r % base.size()));
        }
        return grown;
    }

    private List<Integer> growTree(InputField field, int baseSize) {
        Integer cap = field.intConstraint("maxLength");
        int target = Math.min(Math.max(baseSize * 2, baseSize + 1), cap != null ? cap : baseSize * 2);
        assertTrue(target > baseSize, field.getName() + " cannot be grown within its own maxLength");

        // A complete level-order tree, so no value lands under an absent parent.
        Integer maxValue = field.intConstraint("maxValue");
        List<Integer> grown = new ArrayList<>();
        for (int i = 1; i <= target; i++) {
            grown.add(maxValue != null ? Math.min(i, maxValue) : i);
        }
        return grown;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> asIntList(Object value) {
        return (List<Integer>) value;
    }

    /** Everything a viewer would perceive: the narration and the highlighted lines. */
    private String fingerprint(ExecutionTrace trace) {
        StringBuilder sb = new StringBuilder();
        for (ExecutionStep s : trace.getSteps()) {
            sb.append(s.getActiveLine()).append('|')
              .append(s.getDescription()).append('|')
              .append(s.getVariables()).append('\n');
        }
        return sb.toString();
    }
}
