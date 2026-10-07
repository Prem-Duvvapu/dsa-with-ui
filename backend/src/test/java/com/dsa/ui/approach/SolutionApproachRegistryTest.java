package com.dsa.ui.approach;

import com.dsa.ui.catalog.*;
import com.dsa.ui.model.*;
import com.dsa.ui.tracer.*;
import com.dsa.ui.tracer.impl.ClimbingStairsTracer;
import com.dsa.ui.controller.ProblemsController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SolutionApproachRegistryTest {
    private final ClimbingStairsTracer canonical = new ClimbingStairsTracer();
    private final TracerRegistry tracers = new TracerRegistry(List.of(canonical));
    private ProblemCatalog catalog;
    private ProblemDetail problem;

    @BeforeEach void catalogue() {
        problem = new ProblemDetail();
        problem.setId(canonical.id());
        problem.setDsType(DsType.DP_TABLE);
        problem.setComplexity(new ComplexityDetail("O(N)", "One per stair", "DP", "O(N)", "Table", "DP", "O(N)", "DP table"));
        catalog = new ProblemCatalog(List.of(new ProblemProvider() {
            public List<ProblemDetail> getAllProblems() { return List.of(problem); }
            public ProblemDetail getProblemById(String id) { return problem; }
        }), tracers);
    }

    private SolutionApproach definition(String id, boolean isDefault, AlgorithmTracer tracer) {
        return new SolutionApproach(canonical.id(), id, id, "A real registered executable", isDefault,
                ApproachComplexity.from(problem.getComplexity()), tracer);
    }
    private SolutionApproachRegistry registry(SolutionApproach... definitions) {
        return new SolutionApproachRegistry(tracers, catalog, List.of((r, c) -> List.of(definitions)));
    }

    @Test void canonicalAdapterDoesNotInventAnAlgorithmClassification() {
        var registry = new SolutionApproachRegistry(tracers, catalog, List.of());
        assertSame(canonical, registry.resolve(canonical.id(), null).tracer());
        assertEquals("Current solution", registry.resolve(canonical.id(), "canonical").label());
        assertThrows(UnavailableApproachException.class, () -> registry.resolve(canonical.id(), "memoization"));
    }

    @Test void duplicatePairsAreRejectedAcrossProviders() {
        var defaultDefinition = definition("tabulation", true, canonical);
        assertThrows(IllegalStateException.class, () -> new SolutionApproachRegistry(tracers, catalog,
                List.of((r, c) -> List.of(defaultDefinition), (r, c) -> List.of(defaultDefinition))));
    }
    @Test void missingDefaultIsRejected() {
        assertThrows(IllegalStateException.class, () -> registry(definition("memoization", false, new Fixture())));
    }
    @Test void multipleDefaultsAreRejected() {
        assertThrows(IllegalStateException.class, () -> registry(definition("one", true, canonical), definition("two", true, canonical)));
    }
    @Test void defaultCannotChangeTheExistingExecutable() {
        assertThrows(IllegalStateException.class, () -> registry(definition("new-default", true, new Fixture())));
    }
    @Test void orphanProblemIsRejected() {
        assertThrows(IllegalStateException.class, () -> registry(new SolutionApproach("made-up", "memoization", "Memoization",
                "No such problem", false, null, new Fixture())));
    }
    @Test void mismatchedExecutableIdIsRejected() {
        assertThrows(IllegalStateException.class, () -> registry(definition("memoization", false, new Fixture() {
            @Override public String id() { return "two-sum"; }
        })));
    }
    @Test void missingRendererIsRejected() {
        assertThrows(IllegalStateException.class, () -> registry(definition("memoization", false, new Fixture() {
            @Override public DsType dsType() { return null; }
        })));
    }
    @ParameterizedTest @ValueSource(strings = {"", "   ", "CamelCase", "a/b", "with space"})
    void invalidIdentityIsRejected(String id) {
        assertThrows(IllegalStateException.class, () -> registry(definition(id, true, canonical)));
    }
    @Test void missingSourceIsRejected() {
        assertThrows(IllegalStateException.class, () -> registry(definition("memoization", false, new Fixture() {
            @Override public String annotatedCode() { return " "; }
        })));
    }
    @Test void missingFieldCeilingIsRejected() {
        assertThrows(IllegalStateException.class, () -> registry(definition("memoization", false, new Fixture() {
            @Override public InputSpec inputSpec() { return InputSpec.of(InputField.of("n", FieldType.INT).defaultValue(5).build()); }
        })));
    }
    @Test void elementValueLimitIsNotAnArrayLengthCeiling() {
        var arrayCanonical = new Fixture() {
            @Override public InputSpec inputSpec() {
                return InputSpec.of(InputField.of("n", FieldType.INT_ARRAY).length(1, 8).values(0, 10)
                        .defaultValue(List.of(1, 2)).build());
            }
            @Override public Map<String, Object> alternateInput() { return Map.of("n", List.of(3)); }
        };
        var unsafe = new Fixture() {
            @Override public InputSpec inputSpec() {
                return InputSpec.of(InputField.of("n", FieldType.INT_ARRAY).values(0, 10)
                        .defaultValue(List.of(1, 2)).build());
            }
            @Override public Map<String, Object> alternateInput() { return Map.of("n", List.of(3)); }
        };
        assertThrows(IllegalStateException.class, () -> new SolutionApproachRegistry(
                new TracerRegistry(List.of(arrayCanonical)), catalog,
                List.of((r, c) -> List.of(definition("default", true, arrayCanonical), definition("unsafe", false, unsafe)))));
    }
    @Test void invalidDefaultsAreRejected() {
        assertThrows(InputValidationException.class, () -> registry(definition("memoization", false, new Fixture() {
            @Override public InputSpec inputSpec() { return spec("n", 12); }
        })));
    }
    @Test void invalidAlternateIsRejected() {
        assertThrows(InputValidationException.class, () -> registry(definition("memoization", false, new Fixture() {
            @Override public Map<String, Object> alternateInput() { return Map.of("n", 1000); }
        })));
    }
    @Test void differentInputFieldsAreRejected() {
        assertThrows(IllegalStateException.class, () -> registry(definition("memoization", false, new Fixture() {
            @Override public InputSpec inputSpec() { return spec("stairs", 5); }
        })));
    }
    @Test void relaxedGlobalBudgetsAreRejected() {
        assertThrows(IllegalStateException.class, () -> registry(definition("memoization", false, new Fixture() {
            @Override public InputSpec inputSpec() { return super.inputSpec().withMaxSteps(Integer.MAX_VALUE); }
        })));
        assertThrows(IllegalStateException.class, () -> registry(definition("memoization", false, new Fixture() {
            @Override public InputSpec inputSpec() { return super.inputSpec().withMaxBytes(Long.MAX_VALUE); }
        })));
    }
    @Test void metadataIsFrozenAndAlternativesDoNotEnterCanonicalRegistry() {
        var registry = registry(definition("tabulation", true, canonical), definition("memoization", false, new Fixture()));
        problem.getComplexity().setTimeComplexity("mutated");
        assertEquals("O(N)", registry.resolve(canonical.id(), "memoization").complexity().timeComplexity());
        assertEquals(1, tracers.size());
        assertSame(canonical, tracers.find(canonical.id()).orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> registry.available(canonical.id()).clear());
        assertThrows(UnsupportedOperationException.class, () -> registry.resolve(canonical.id(), null).summaryView().clear());
    }

    @Test void controllerDispatchesAndCachesBySelectedExecutableNotJustProblemId() throws Exception {
        var alternative = new Fixture();
        var registry = registry(definition("tabulation", true, canonical), definition("fixture", false, alternative));
        var controller = new ProblemsController(catalog, tracers, new TraceRunner(),
                new StatementCatalog(new ObjectMapper()), registry);
        for (String encoding : List.of("full", "delta")) {
            var original = controller.executeDefaults(canonical.id(), encoding, null);
            var selected = controller.executeDefaults(canonical.id(), encoding, "fixture");
            assertEquals("tabulation", original.getApproachId());
            assertEquals("fixture", selected.getApproachId());
            assertEquals(DsType.RECURSION_TREE, selected.getDsType());
            assertNotEquals(original.getCode(), selected.getCode());
            assertNotEquals(original.getStepCount(), selected.getStepCount());
            assertSame(selected, controller.executeDefaults(canonical.id(), encoding.toUpperCase(), "fixture"));
            assertSame(original, controller.executeDefaults(canonical.id(), encoding, "tabulation"));
        }
        assertEquals(8, controller.inputSpec(canonical.id(), "fixture").field("n").getConstraints().get("max"));
        assertEquals(DsType.RECURSION_TREE, controller.detail(canonical.id(), "fixture").get("dsType"));
        assertEquals(1, controller.execute(canonical.id(), Map.of("n", 7), "full", "fixture").getStepCount());
        assertThrows(InputValidationException.class, () -> controller.execute(canonical.id(), Map.of("n", 9), "full", "fixture"));
        assertThrows(UnavailableApproachException.class, () -> controller.executeDefaults(canonical.id(), "full", "missing"));
    }
    private static InputSpec spec(String name, int value) {
        return InputSpec.of(InputField.of(name, FieldType.INT).range(1, 8).defaultValue(value).build());
    }
    private static class Fixture implements AlgorithmTracer {
        public String id() { return "climbing-stairs"; }
        public DsType dsType() { return DsType.RECURSION_TREE; }
        public InputSpec inputSpec() { return spec("n", 5); }
        public Map<String, Object> alternateInput() { return Map.of("n", 3); }
        public String annotatedCode() { return "// @a result\nreturn n;"; }
        public void run(Inputs input, StepEmitter emit) { emit.at("result").say("Fixture result").var("answer", input.getInt("n")).step(); }
    }
}
