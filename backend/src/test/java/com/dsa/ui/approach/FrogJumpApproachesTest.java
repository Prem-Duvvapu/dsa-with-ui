package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FrogJumpApproachesTest {
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TracerRegistry canonical;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @TempDir Path temporary;
    private static final String PROBLEM = "frog-jump";
    // Keep the old public id so existing ?approach=canonical links still work.
    private static final List<String> FORMS = List.of("recursion", "memoization", "canonical");

    @Test void threeGenuineFormsPreserveTheCanonicalExecutableAndOldLinks() throws Exception {
        assertEquals(new HashSet<>(FORMS), approaches.available(PROBLEM).stream()
                .map(SolutionApproach::id).collect(java.util.stream.Collectors.toSet()));
        var current = approaches.resolve(PROBLEM, null);
        assertEquals("canonical", current.id());
        assertEquals("Tabulation", current.label());
        assertSame(canonical.find(PROBLEM).orElseThrow(), current.tracer());
        for (String id : FORMS) assertEquals(id.equals("canonical") ? DsType.DP_TABLE : DsType.RECURSION_TREE,
                approaches.resolve(PROBLEM, id).tracer().dsType());
        http.perform(get("/api/problems/frog-jump").param("approach", "canonical"))
                .andExpect(status().isOk());
    }

    @Test void allFormsMatchIndependentlyEnumeratedLegalJumpPaths() {
        for (String id : FORMS) {
            var tracer = approaches.resolve(PROBLEM, id).tracer();
            for (int length = 2; length <= 5; length++) {
                int count = (int) Math.pow(3, length);
                for (int encoded = 0; encoded < count; encoded++) {
                    List<Integer> heights = new ArrayList<>();
                    int value = encoded;
                    for (int i = 0; i < length; i++, value /= 3) heights.add(new int[]{0, 1, 3}[value % 3]);
                    assertAnswer(tracer, heights);
                }
            }
            for (var heights : List.of(List.of(5, 5), List.of(0, 999), List.of(10, 50, 40, 30),
                    List.of(30, 10, 60, 10, 20), List.of(0, 999, 0, 999, 0))) assertAnswer(tracer, heights);
        }
    }

    @Test void declaredLengthAndValueBoundsFinishOrRefuseWithoutShrinking() {
        for (String id : FORMS) {
            var tracer = approaches.resolve(PROBLEM, id).tracer();
            int maximum = maxLength(tracer);
            assertEquals(id.equals("recursion") ? 10 : 20, maximum);
            for (int length = 2; length <= maximum; length++) assertAnswer(tracer, zigzag(length));
            for (var invalid : List.of(List.of(5), zigzag(maximum + 1), List.of(-1, 0), List.of(0, 1000))) {
                assertThrows(InputValidationException.class, () -> runner.run(tracer, Map.of("heights", invalid)));
            }
            assertThrows(InputValidationException.class, () -> runner.run(tracer, Map.of("heights", List.of(5, 5), "n", 2)));
        }
    }

    @Test void zeroIsAComputedMemoValueAndHitsDoNotExpandChildren() {
        var trace = runner.run(approaches.resolve(PROBLEM, "memoization").tracer(),
                Map.of("heights", Collections.nCopies(7, 5)));
        assertEquals("0", last(trace).get("answer"));
        assertEquals("12", last(trace).get("calls"));
        assertEquals("5", last(trace).get("cacheHits"));
        assertEquals("7", last(trace).get("computedStates"));
        assertEquals("7", last(trace).get("stateEvaluations"));
        var steps = trace.getSteps();
        assertTrue(steps.get(0).getDpTable().cells().get(0).stream().allMatch(c -> c.state().equals("void")));
        Set<String> computed = new HashSet<>();
        int hits = 0;
        for (int i = 0; i < steps.size(); i++) {
            var step = steps.get(i);
            var variables = step.getVariables();
            if (Set.of("base", "store").contains(variables.getOrDefault("event", "")))
                assertTrue(computed.add(variables.get("state")), "A memo state must be evaluated once");
            if ("cache-hit".equals(variables.get("event"))) {
                hits++;
                assertEquals("0", variables.get("value"));
                assertEquals("return", steps.get(i + 1).getVariables().get("event"));
                assertEquals(variables.get("callId"), steps.get(i + 1).getVariables().get("callId"));
                assertEquals(step.getCallStack().size() - 1, steps.get(i + 1).getCallStack().size());
                int state = Integer.parseInt(variables.get("state"));
                assertEquals("0", step.getDpTable().cells().get(0).get(state).value());
                assertEquals("read", step.getDpTable().cells().get(0).get(state).state());
            }
        }
        assertEquals(5, hits);
    }

    @Test void recursiveVisitsAreDistinctAndDoNotInventMemoData() {
        var trace = runner.runDefaults(approaches.resolve(PROBLEM, "recursion").tracer());
        Set<String> calls = new HashSet<>();
        Map<String, Integer> visits = new HashMap<>();
        for (var step : trace.getSteps()) {
            assertNull(step.getDpTable());
            if ("enter".equals(step.getVariables().get("event"))) {
                assertTrue(calls.add(step.getVariables().get("callId")));
                visits.merge(step.getVariables().get("state"), 1, Integer::sum);
                assertTrue(step.getCallStack().get(step.getCallStack().size() - 1).startsWith("energy("));
            }
        }
        assertTrue(visits.values().stream().anyMatch(n -> n > 1));
        assertEquals(String.valueOf(calls.size()), last(trace).get("calls"));
        assertEquals("0", last(trace).get("cacheHits"));
    }

    @Test void aStepBudgetStopsRealExpansionBeforeAnAnswerIsClaimed() {
        for (String id : List.of("recursion", "memoization")) {
            var actual = approaches.resolve(PROBLEM, id).tracer();
            AlgorithmTracer capped = new AlgorithmTracer() {
                public String id() { return actual.id(); }
                public DsType dsType() { return actual.dsType(); }
                public String annotatedCode() { return actual.annotatedCode(); }
                public InputSpec inputSpec() { return actual.inputSpec().withMaxSteps(3); }
                public Map<String, Object> alternateInput() { return actual.alternateInput(); }
                public void run(Inputs input, StepEmitter emitter) { actual.run(input, emitter); }
            };
            var trace = runner.run(capped, Map.of("heights", zigzag(10)));
            assertTrue(trace.isTruncated());
            assertEquals(3, trace.getStepCount());
            assertFalse(trace.getSteps().stream().anyMatch(s -> s.getVariables().containsKey("answer")));
            assertTrue(Integer.parseInt(last(trace).get("calls")) <= 2);
        }
    }

    @Test void completeDisplayedJavaCompilesAndReturnsIndependentAnswers() throws Exception {
        for (String id : FORMS) {
            var tracer = approaches.resolve(PROBLEM, id).tracer();
            String code = AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
            // The existing canonical source is a complete method; retain it unchanged.
            String unit = code.startsWith("public class Solution") ? code : "public class Solution {\n" + code + "\n}";
            Path directory = Files.createDirectory(temporary.resolve(id));
            Path source = directory.resolve("Solution.java");
            Files.writeString(source, unit);
            var errors = new ByteArrayOutputStream();
            var compiler = ToolProvider.getSystemJavaCompiler();
            assertNotNull(compiler);
            assertEquals(0, compiler.run(null, null, errors, "--release", "17", "-classpath", directory.toString(),
                    "-d", directory.toString(), source.toString()), () -> id + ": " + errors);
            try (var loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                var solution = loader.loadClass("Solution");
                var method = solution.getMethod("frogJump", int.class, int[].class);
                for (int length = 2; length <= maxLength(tracer); length++) {
                    var heights = zigzag(length);
                    int[] values = heights.stream().mapToInt(Integer::intValue).toArray();
                    assertEquals(oracle(heights), method.invoke(solution.getConstructor().newInstance(), length, values));
                }
            }
        }
    }

    @Test void selectedApiServesTeachingSourceIdentityAndStrictRefusal() throws Exception {
        var json = new ObjectMapper();
        for (String id : FORMS) {
            var approach = approaches.resolve(PROBLEM, id);
            var detail = json.readTree(http.perform(get("/api/problems/frog-jump").param("approach", id))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertEquals("energy(i): minimum total energy to reach stair i", detail.path("teaching").path("state").asText());
            for (String encoding : List.of("full", "delta")) {
                var body = json.readTree(http.perform(get("/api/problems/frog-jump/execute").param("approach", id)
                        .param("encoding", encoding).with(r -> { r.setRemoteAddr("frog-" + id); return r; }))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
                assertEquals(PROBLEM, body.path("problemId").asText());
                assertEquals(id, body.path("approachId").asText());
                assertEquals(detail.path("javaCode"), body.path("code"));
                assertEquals(detail.path("dsType"), body.path("dsType"));
                assertFalse(body.path("truncated").asBoolean());
            }
            var input = json.writeValueAsString(Map.of("heights", zigzag(maxLength(approach.tracer()) + 1)));
            http.perform(post("/api/problems/frog-jump/execute").param("approach", id)
                    .contentType("application/json").content(input)).andExpect(status().isBadRequest());
        }
        http.perform(get("/api/problems/frog-jump/execute").param("approach", "unknown")).andExpect(status().isBadRequest());
    }

    @Test void everySourceAnchorAndAuthoredEventIsActuallyEmitted() {
        for (String id : FORMS) {
            var approach = approaches.resolve(PROBLEM, id);
            Set<Integer> lines = new HashSet<>();
            Set<String> events = new HashSet<>();
            for (var input : List.of(Map.<String, Object>of(), approach.tracer().alternateInput()))
                for (var step : runner.run(approach.tracer(), input).getSteps()) {
                    lines.add(step.getActiveLine());
                    if (step.getVariables().containsKey("event")) events.add(step.getVariables().get("event"));
                }
            var source = AnnotatedCode.parse(approach.tracer().annotatedCode());
            assertEquals(new HashSet<>(source.getAnchors().values()), lines);
            assertTrue(source.getAnchors().keySet().containsAll(approach.teaching().anchorNotes().keySet()));
            assertTrue(events.containsAll(approach.teaching().eventNotes().keySet()));
        }
    }

    private void assertAnswer(AlgorithmTracer tracer, List<Integer> heights) {
        var trace = runner.run(tracer, Map.of("heights", heights));
        assertFalse(trace.isTruncated(), tracer.id() + " " + heights);
        assertEquals(String.valueOf(oracle(heights)), last(trace).get("answer"), heights.toString());
        assertTrue(trace.getSteps().get(trace.getStepCount() - 1).getCallStack().isEmpty());
    }
    private static int maxLength(AlgorithmTracer tracer) {
        return ((Number) tracer.inputSpec().field("heights").getConstraints().get("maxLength")).intValue();
    }
    private static List<Integer> zigzag(int length) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < length; i++) result.add(i % 2 == 0 ? 0 : 999);
        return result;
    }
    private static Map<String, String> last(ExecutionTrace trace) {
        return trace.getSteps().get(trace.getStepCount() - 1).getVariables();
    }
    // Enumerate forward paths, unlike the backward-state recurrences under test.
    private static int oracle(List<Integer> heights) { return pathCost(heights, 0, 0); }
    private static int pathCost(List<Integer> heights, int at, int spent) {
        if (at == heights.size() - 1) return spent;
        int best = Integer.MAX_VALUE;
        for (int distance = 1; distance <= 2 && at + distance < heights.size(); distance++) {
            int next = at + distance;
            best = Math.min(best, pathCost(heights, next, spent + Math.abs(heights.get(next) - heights.get(at))));
        }
        return best;
    }
}
