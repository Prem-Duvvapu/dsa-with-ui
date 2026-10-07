package com.dsa.ui.approach;

import com.dsa.ui.tracer.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClimbingStairsApproachesTest {
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @TempDir Path temporary;
    private final String problem = "climbing-stairs";

    @Test void allThreeAdvertisedFormsAreRealExecutables() {
        assertEquals(Set.of("recursion", "memoization", "tabulation"), approaches.available(problem).stream()
                .map(SolutionApproach::id).collect(java.util.stream.Collectors.toSet()));
        assertEquals("tabulation", approaches.resolve(problem, null).id());
    }

    @Test void allFormsAgreeWithAnIndependentCombinatorialOracle() {
        for (String id : List.of("recursion", "memoization", "tabulation")) {
            var approach = approaches.resolve(problem, id);
            int max = ((Number) approach.tracer().inputSpec().field("n").getConstraints().get("max")).intValue();
            for (int n = 1; n <= max; n++) {
                var trace = runner.run(approach.tracer(), Map.of("n", n));
                assertFalse(trace.isTruncated(), id + " boundary " + n);
                assertEquals(String.valueOf(oracle(n)), last(trace).get("answer"), id + " n=" + n);
                assertTrue(trace.getSteps().get(trace.getStepCount() - 1).getCallStack().isEmpty());
            }
            assertThrows(InputValidationException.class, () -> runner.run(approach.tracer(), Map.of("n", max + 1)));
        }
    }

    @Test void memoizationReusesStatesAndDoesNotInventChildrenOnHits() {
        var memo = runner.run(approaches.resolve(problem, "memoization").tracer(), Map.of("n", 8));
        var naive = runner.run(approaches.resolve(problem, "recursion").tracer(), Map.of("n", 8));
        assertTrue(Integer.parseInt(last(memo).get("calls")) < Integer.parseInt(last(naive).get("calls")));
        assertTrue(Integer.parseInt(last(memo).get("cacheHits")) > 0);
        assertEquals("9", last(memo).get("computedStates"));
        var steps = memo.getSteps();
        boolean nonBaseHit = false;
        Set<String> stored = new HashSet<>();
        for (int i = 0; i < steps.size(); i++) {
            var step = steps.get(i);
            var variables = step.getVariables();
            if ("store".equals(variables.get("event")) || "base".equals(variables.get("event"))) {
                assertTrue(stored.add(variables.get("state")), "Each memo state is computed once");
            }
            if ("cache-hit".equals(variables.get("event"))) {
                assertEquals("return", steps.get(i + 1).getVariables().get("event"));
                assertEquals(variables.get("callId"), steps.get(i + 1).getVariables().get("callId"));
                assertTrue(steps.get(i + 1).getCallStack().size() < step.getCallStack().size());
                if (Integer.parseInt(variables.get("state")) > 1) nonBaseHit = true;
            }
        }
        assertTrue(nonBaseHit, "Cache reuse must not consist only of trivial base cases");
        assertTrue(memo.getSteps().get(0).getDpTable().cells().get(0).stream().allMatch(c -> c.state().equals("void")));
    }

    @Test void recursionHasDistinctCallIdentitiesAndNoMemoCache() {
        var trace = runner.runDefaults(approaches.resolve(problem, "recursion").tracer());
        Set<String> calls = new HashSet<>();
        Map<String, Integer> frequency = new HashMap<>();
        for (var step : trace.getSteps()) {
            assertNull(step.getDpTable());
            if ("enter".equals(step.getVariables().get("event"))) {
                assertTrue(calls.add(step.getVariables().get("callId")));
                frequency.merge(step.getVariables().get("state"), 1, Integer::sum);
            }
        }
        assertTrue(frequency.values().stream().anyMatch(count -> count > 1));
        assertEquals(String.valueOf(calls.size()), last(trace).get("calls"));
        assertEquals("0", last(trace).get("cacheHits"));
    }

    @Test void budgetStopsActualRecursiveExpansionWithoutClaimingAnAnswer() {
        for (String id : List.of("recursion", "memoization")) {
            var actual = approaches.resolve(problem, id).tracer();
            AlgorithmTracer capped = new AlgorithmTracer() {
                public String id() { return actual.id(); }
                public com.dsa.ui.model.DsType dsType() { return actual.dsType(); }
                public String annotatedCode() { return actual.annotatedCode(); }
                public InputSpec inputSpec() { return actual.inputSpec().withMaxSteps(3); }
                public Map<String, Object> alternateInput() { return actual.alternateInput(); }
                public void run(Inputs input, StepEmitter emitter) { actual.run(input, emitter); }
            };
            var trace = runner.run(capped, Map.of("n", 10));
            assertTrue(trace.isTruncated());
            assertEquals(3, trace.getStepCount());
            assertFalse(trace.getSteps().stream().anyMatch(step -> step.getVariables().containsKey("answer")));
            assertTrue(Integer.parseInt(last(trace).get("calls")) <= 2,
                    "Emitting before descent must stop expansion, not just stop recording a completed tree");
        }
    }

    @Test void displayedClassesCompileIndependentlyAndSolveTheAdvertisedBounds() throws Exception {
        for (var approach : approaches.available(problem)) {
            String code = AnnotatedCode.parse(approach.tracer().annotatedCode()).getDisplayCode();
            Path directory = Files.createDirectory(temporary.resolve(approach.id()));
            Path source = directory.resolve("Solution.java");
            Files.writeString(source, code);
            var compiler = ToolProvider.getSystemJavaCompiler();
            assertNotNull(compiler);
            var errors = new ByteArrayOutputStream();
            assertEquals(0, compiler.run(null, null, errors, "--release", "17", "-classpath", directory.toString(),
                    "-d", directory.toString(), source.toString()), () -> approach.id() + ": " + errors);
            try (var loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                var solution = loader.loadClass("Solution");
                var method = solution.getMethod("climbStairs", int.class);
                int max = ((Number) approach.tracer().inputSpec().field("n").getConstraints().get("max")).intValue();
                for (int n = 1; n <= max; n++) assertEquals(oracle(n), ((Number) method.invoke(solution.getConstructor().newInstance(), n)).longValue());
            }
        }
    }

    @Test void fullAndDeltaServeTheSameSelectedSourceAndIdentity() throws Exception {
        var json = new ObjectMapper();
        for (String id : List.of("recursion", "memoization", "tabulation")) {
            var detail = http.perform(get("/api/problems/climbing-stairs").param("approach", id))
                    .andExpect(status().isOk()).andReturn();
            var source = json.readTree(detail.getResponse().getContentAsByteArray()).path("javaCode");
            for (String encoding : List.of("full", "delta")) {
                var response = http.perform(get("/api/problems/climbing-stairs/execute").param("approach", id)
                        .param("encoding", encoding).with(r -> { r.setRemoteAddr("pilot-" + id); return r; }))
                        .andExpect(status().isOk()).andReturn();
                var body = json.readTree(response.getResponse().getContentAsByteArray());
                assertEquals(id, body.path("approachId").asText());
                assertEquals(source, body.path("code"));
                assertFalse(body.path("truncated").asBoolean());
            }
        }
    }

    @Test void defaultAndAlternateReachAllDeclaredAnchors() {
        for (var approach : approaches.available(problem)) {
            Set<Integer> visited = new HashSet<>();
            for (var input : List.of(Map.<String, Object>of(), approach.tracer().alternateInput())) {
                var trace = runner.run(approach.tracer(), input);
                trace.getSteps().forEach(step -> visited.add(step.getActiveLine()));
            }
            assertEquals(new HashSet<>(AnnotatedCode.parse(approach.tracer().annotatedCode()).getAnchors().values()), visited);
        }
    }

    private static Map<String, String> last(ExecutionTrace trace) { return trace.getSteps().get(trace.getStepCount() - 1).getVariables(); }
    // Choose k two-step moves and interleave them with n-2k one-step moves.
    private static long oracle(int n) {
        long total = 0;
        for (int k = 0; k <= n / 2; k++) {
            long combinations = 1;
            for (int i = 1; i <= k; i++) combinations = combinations * (n - k - i + 1) / i;
            total += combinations;
        }
        return total;
    }
}
