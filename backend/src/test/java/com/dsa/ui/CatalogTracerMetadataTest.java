package com.dsa.ui;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.DpService;
import com.dsa.ui.tracer.AlgorithmTracer;
import com.dsa.ui.tracer.TracerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class CatalogTracerMetadataTest {

    @Autowired
    private ProblemCatalog catalogue;

    @Autowired
    private TracerRegistry tracers;

    @Autowired
    private DpService dpCatalogue;

    @Test
    void catalogueDsTypesMatchEveryRegisteredTracer() {
        List<String> mismatches = new ArrayList<>();

        for (AlgorithmTracer tracer : tracers.all()) {
            var entry = catalogue.find(tracer.id());
            if (entry.isEmpty()) {
                mismatches.add(tracer.id() + ": missing from catalogue");
                continue;
            }

            var catalogueType = entry.orElseThrow().getProblem().getDsType();
            if (catalogueType != tracer.dsType()) {
                mismatches.add(tracer.id() + ": catalogue=" + catalogueType
                        + ", tracer=" + tracer.dsType());
            }
        }

        assertEquals(List.of(), mismatches,
                "catalogue dsType must route every traced problem to its emitted structure");
    }

    @ParameterizedTest(name = "{0} catalogue complexity matches its tracer implementation")
    @MethodSource("tracedDpComplexities")
    void tracedDpComplexityMatchesImplementation(String id,
                                                  String expectedTime,
                                                  String expectedSpace) {
        ProblemDetail problem = dpCatalogue.getProblemById(id);

        assertNotNull(problem);
        assertEquals(expectedTime, problem.getComplexity().getTimeComplexity());
        assertEquals(expectedSpace, problem.getComplexity().getSpaceComplexity());
    }

    static Stream<Arguments> tracedDpComplexities() {
        return Stream.of(
                Arguments.of("climbing-stairs", "O(N)", "O(N)"),
                Arguments.of("frog-jump", "O(N)", "O(N)"),
                Arguments.of("frog-jump-k-distance", "O(N * K)", "O(N)"),
                Arguments.of("max-sum-non-adjacent", "O(N)", "O(N)"),
                Arguments.of("house-robber-2", "O(N)", "O(N)"),
                Arguments.of("grid-unique-paths", "O(M * N)", "O(M * N)"),
                Arguments.of("unique-paths-2", "O(M * N)", "O(M * N)"),
                Arguments.of("minimum-falling-path-sum", "O(N^2)", "O(N^2)"),
                Arguments.of("triangle-min-path-sum", "O(N^2)", "O(N^2)"),
                Arguments.of("longest-increasing-subsequence", "O(N^2)", "O(N^2)"),
                Arguments.of("lis-binary-search", "O(N log N)", "O(N)"),
                Arguments.of("print-lis", "O(N^2)", "O(N)"));
    }
}
