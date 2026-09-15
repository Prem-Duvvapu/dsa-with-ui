package com.dsa.ui;

import com.dsa.ui.catalog.ProblemProvider;
import com.dsa.ui.model.ProblemDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * One contract for the eighteen catalogue providers.
 *
 * <p>These classes used to be the legacy trace layer: eighteen controllers over eighteen
 * services, each with a {@code switch (problemId)} returning {@code List<ExecutionStep>}.
 * That layer is gone - every problem is served by {@code tracer/} through
 * {@code /api/problems}. What survives is the half that was never legacy: each service
 * still owns the {@link ProblemDetail} metadata for its topic, which
 * {@link com.dsa.ui.catalog.ProblemCatalog} merges into one id-keyed view.
 *
 * <p>This replaces eighteen near-identical {@code *ServiceTest} classes. They were
 * copy-pasted, and each carried a hand-maintained list of "retired" ids that had to be
 * updated by hand whenever a tracer landed - drift in those lists is what let stragglers
 * keep falling through {@code default:} and serving another algorithm's animation.
 * Parameterizing over the providers Spring actually registers means a new provider is
 * covered the moment it exists.
 */
@SpringBootTest
class ProblemProviderContractTest {

    @Autowired
    private List<ProblemProvider> providers;

    static Stream<String> providerNames() {
        return Stream.of(
                "AdvancedGraphService", "ArrayService", "BasicMathService", "BasicRecursionService",
                "BinarySearchService", "BitManipulationService", "DpService", "GraphBfsDfsService",
                "GreedyService", "HeapService", "LinkedListService", "RecursionBacktrackingService",
                "SlidingWindowService", "SortingService", "StackQueueService", "StringService",
                "TreeService", "TrieService");
    }

    private ProblemProvider byName(String simpleName) {
        return providers.stream()
                .filter(p -> p.getClass().getSimpleName().equals(simpleName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(simpleName + " is not a registered ProblemProvider"));
    }

    @ParameterizedTest(name = "{0} is registered")
    @MethodSource("providerNames")
    @DisplayName("Every expected provider is on the context")
    void providerIsRegistered(String name) {
        assertNotNull(byName(name));
    }

    @ParameterizedTest(name = "{0} serves a well-formed catalogue")
    @MethodSource("providerNames")
    @DisplayName("Each provider's problems carry the fields the catalogue merge relies on")
    void catalogueIsWellFormed(String name) {
        List<ProblemDetail> problems = byName(name).getAllProblems();
        assertFalse(problems.isEmpty(), name + " serves an empty catalogue");

        Set<String> seen = new HashSet<>();
        for (ProblemDetail p : problems) {
            assertNotNull(p.getId(), name + " has an entry with no id");
            assertFalse(p.getId().isBlank(), name + " has an entry with a blank id");
            assertTrue(seen.add(p.getId()), name + " lists '" + p.getId() + "' twice");
            assertFalse(p.getTitle() == null || p.getTitle().isBlank(),
                    name + " entry '" + p.getId() + "' has no title");
            assertFalse(p.getCategory() == null || p.getCategory().isBlank(),
                    name + " entry '" + p.getId() + "' has no category");
            assertNotNull(p.getDsType(), name + " entry '" + p.getId() + "' has no dsType");
        }
    }

    @ParameterizedTest(name = "{0} round-trips every id it lists")
    @MethodSource("providerNames")
    @DisplayName("getProblemById returns the same problem getAllProblems listed")
    void everyListedIdRoundTrips(String name) {
        ProblemProvider provider = byName(name);
        List<String> broken = new ArrayList<>();
        for (ProblemDetail p : provider.getAllProblems()) {
            ProblemDetail fetched = provider.getProblemById(p.getId());
            if (fetched == null || !p.getId().equals(fetched.getId())) {
                broken.add(p.getId());
            }
        }
        assertTrue(broken.isEmpty(), name + " lists ids it cannot fetch back: " + broken);
    }

    @ParameterizedTest(name = "{0} returns null for an unknown id")
    @MethodSource("providerNames")
    @DisplayName("An unknown id is null, never a substitute problem")
    void unknownIdIsNull(String name) {
        assertNull(byName(name).getProblemById("definitely-not-a-real-problem-id"),
                name + " returned something for an id it does not have");
    }
}
