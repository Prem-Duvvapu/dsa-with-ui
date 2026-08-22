package com.dsa.ui.tracer;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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
class TracerContractTest {

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    @Autowired
    private ProblemCatalog catalog;

    /**
     * A second, materially different input per tracer. Used to prove the trace is
     * computed rather than canned.
     */
    private static final Map<String, Map<String, Object>> ALTERNATE_INPUT = Map.of(
            "two-sum", Map.of("nums", List.of(3, 2, 4, 8, 1), "target", 20),
            "kadane-algo", Map.of("nums", List.of(5, -1, 5, -20, 3)),
            "binary-search-1d", Map.of("nums", List.of(2, 4, 6, 8, 10, 12, 14, 16), "target", 15),
            "tree-preorder", Map.of("tree", Arrays.asList(10, 20, 30, 40)),
            "tree-inorder", Map.of("tree", Arrays.asList(10, 20, 30, 40)),
            "reverse-linked-list", Map.of("values", List.of(7, 8, 9, 10, 11, 12)),
            "bfs-traversal", Map.of(
                    "graph", Map.of("vertices", 4, "edges", List.of(List.of(0, 1), List.of(1, 2), List.of(2, 3))),
                    "start", 0),
            "number-of-islands", Map.of("grid", List.of(
                    List.of(1, 0, 1),
                    List.of(0, 0, 0),
                    List.of(1, 0, 1)))
    );

    static Stream<String> tracerIds() {
        // Static context cannot see the injected registry, so mirror the ids the pilot
        // registers. registryMatchesThisTestsExpectations() keeps the two in sync.
        return ALTERNATE_INPUT.keySet().stream().sorted();
    }

    @Test
    @DisplayName("Every registered tracer is covered by this test class")
    void registryMatchesThisTestsExpectations() {
        assertEquals(
                registry.tracedIds().stream().sorted().toList(),
                tracerIds().toList(),
                "A tracer was added or removed without updating ALTERNATE_INPUT, so it is untested");
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
    @DisplayName("The step budget stops a runaway trace instead of exhausting the server")
    void stepBudgetTruncates() {
        AlgorithmTracer greedy = new AlgorithmTracer() {
            @Override public String id() { return "test-only-runaway"; }
            @Override public InputSpec inputSpec() { return InputSpec.of().withMaxSteps(25); }
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
        Map<String, Object> input = ALTERNATE_INPUT.get(id);
        assertNotNull(input, "No alternate input registered for " + id);
        return input;
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
