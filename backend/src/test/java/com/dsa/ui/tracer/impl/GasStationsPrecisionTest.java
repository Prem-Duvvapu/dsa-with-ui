package com.dsa.ui.tracer.impl;

import com.dsa.ui.approach.SolutionApproachRegistry;
import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.catalog.StatementCatalog;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Precision, displayed source and continuous snapshots have separate executable guards. */
@SpringBootTest
@AutoConfigureMockMvc
class GasStationsPrecisionTest {
    private static final String ID = "minimize-max-distance-gas-station";
    private static final String ENDPOINT = "/api/problems/" + ID;
    private static final int HALVINGS = 40;
    private static final AtomicInteger CLIENTS = new AtomicInteger();
    private final ObjectMapper json = new ObjectMapper();

    @Autowired MinimizeMaxDistanceGasStationTracer tracer;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @Autowired ProblemCatalog catalog;
    @Autowired SolutionApproachRegistry approaches;
    @Autowired StatementCatalog statements;
    @TempDir Path temporary;

    @Test
    void displayedAnswersMatchExactRationalsIncludingTheOriginalCounterexamplesAndRoundingTies() {
        for (var input : examples()) assertAnswer(input);
        assertEquals("0.08", new BigDecimal(answer(run(input(List.of(0, 3), 39))))
                .setScale(2, RoundingMode.HALF_UP).toPlainString());
        assertEquals("0.03", new BigDecimal(answer(run(input(List.of(0, 1), 39))))
                .setScale(2, RoundingMode.HALF_UP).toPlainString());
        assertEquals("0.13", new BigDecimal(answer(run(input(List.of(0, 1), 7))))
                .setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    @Test
    void boundedSmallExhaustiveAndSeededRealDomainTracesAgreeWithAnIndependentOracle() {
        for (int gap = 1; gap <= 40; gap++) for (int k = 1; k <= 5; k++) {
            assertAnswer(input(List.of(0, gap), k));
        }
        for (var input : randomInputs(80, 65040)) assertAnswer(input);
    }

    @Test
    void exactDisplayedJavaCompilesInIsolationAndMatchesAllTwoStationInputsAndSeededMultiGapCases() throws Exception {
        String source = AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
        // A method listing may receive an outer class only, never an invented helper.
        String complete = source.contains("class Solution") ? source : "public class Solution {\n" + source + "\n}";
        Path directory = Files.createTempDirectory(temporary, "gas-source");
        Path file = directory.resolve("Solution.java");
        Files.writeString(file, complete);
        var errors = new ByteArrayOutputStream();
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, errors,
                "--release", "17", "-classpath", directory.toString(), "-d", directory.toString(), file.toString()), errors::toString);
        try (var loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            var type = loader.loadClass("Solution");
            var solve = type.getMethod("minimizeMaxDistance", int[].class, int.class);
            assertEquals(double.class, solve.getReturnType());
            Object instance = type.getConstructor().newInstance();
            var probes = new ArrayList<>(examples());
            probes.addAll(randomInputs(1000, 65041));
            for (var input : probes) checkSource(solve, instance, input);
            for (int gap = 1; gap <= 1000; gap++) for (int k = 1; k <= 50; k++) {
                checkSource(solve, instance, input(List.of(0, gap), k));
            }
        }
    }

    @Test
    void everyEventCarriesTheActualContinuousBracketAndOnlyAnActiveProbeCarriesMid() {
        var visited = new HashSet<String>();
        for (var input : examples()) visited.addAll(checkSnapshots(input));
        assertEquals(Set.of("init", "mid", "gapTally", "feasible", "infeasible", "done"), visited);
    }

    @Test
    void numericSnapshotsAndFiveDecimalAnswerAreIndependentOfTheServerLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            for (var input : List.of(input(List.of(0, 1000), 2), input(List.of(0, 1), 39))) {
                assertAnswer(input);
                checkSnapshots(input);
                assertTrue(answer(run(input)).matches("[0-9]+\\.[0-9]{5}"));
            }
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void actualDetailDefaultCustomAndNestedApproachesServeTheOwnerSpecificComplexity() throws Exception {
        JsonNode detail = json.readTree(request(get(ENDPOINT), 200));
        JsonNode complexity = detail.path("complexity");
        assertEquals("O(N * I)", complexity.path("timeComplexity").asText());
        assertEquals("O(N)", complexity.path("spaceComplexity").asText());
        assertTrue(complexity.path("timeExplanation").asText().contains("40"));
        assertTrue(complexity.path("timeExplanation").asText().toLowerCase(Locale.ROOT).contains("gap"));
        assertTrue(complexity.path("spaceExplanation").asText().toLowerCase(Locale.ROOT).contains("gap"));
        assertEquals(complexity, detail.path("approaches").get(0).path("complexity"));
        assertEquals(complexity, json.valueToTree(catalog.find(ID).orElseThrow().getProblem().getComplexity()));
        assertSame(tracer, approaches.resolve(ID, null).tracer());
        assertEquals(complexity, json.valueToTree(approaches.resolve(ID, null).complexity()));
        for (String encoding : List.of("full", "delta")) {
            for (boolean defaults : List.of(true, false)) {
                var request = defaults ? get(ENDPOINT + "/execute").param("encoding", encoding)
                        : post(ENDPOINT + "/execute").param("encoding", encoding).contentType("application/json")
                        .content(json.writeValueAsString(input(List.of(0, 1000), 2)));
                JsonNode body = json.readTree(request(request, 200));
                assertEquals(complexity, body.path("complexity"));
                assertEquals(detail.path("javaCode"), body.path("code"));
                assertEquals(detail.path("dsType"), body.path("dsType"));
                assertEquals(ID, body.path("problemId").asText());
                assertEquals("canonical", body.path("approachId").asText());
            }
        }
    }

    @Test
    void authoredStatementAndServedExamplesDescribeTheMeasuredApproximation() throws Exception {
        var statement = statements.find(ID).orElseThrow();
        assertEquals("4.00000", statement.examples().get(0).output());
        String prose = String.join(" ", statement.statement());
        assertTrue(prose.contains("40"));
        assertFalse(prose.contains("exact to two decimal places"));
        for (var example : statement.examples()) {
            assertEquals("answer", example.answerVariable());
            assertEquals(oracle(stations(example.input()), k(example.input())).fiveDecimals(), example.output());
            assertEquals(example.output(), answer(run(example.input())));
        }
        JsonNode detail = json.readTree(request(get(ENDPOINT), 200));
        assertEquals("4.00000", detail.path("examples").get(0).path("output").asText());
        assertFalse(detail.path("statement").toString().contains("exact to two decimal places"));
    }

    @Test
    void bothEncodingsAtTheFullDeclaredSizeCompleteWithinTheExistingResponseBudgetAndDecodeEqually() throws Exception {
        for (var input : maxima()) {
            var trace = assertAnswer(input);
            assertEquals(642, trace.getStepCount(), "40 * (mid +14 gap tallies +decision), plus init/done");
            byte[] fullBytes = request(post(ENDPOINT + "/execute").param("encoding", "full").contentType("application/json")
                    .content(json.writeValueAsString(input)), 200);
            byte[] deltaBytes = request(post(ENDPOINT + "/execute").param("encoding", "delta").contentType("application/json")
                    .content(json.writeValueAsString(input)), 200);
            assertTrue(fullBytes.length < InputSpec.DEFAULT_MAX_BYTES, "full bytes=" + fullBytes.length);
            assertTrue(deltaBytes.length < InputSpec.DEFAULT_MAX_BYTES, "delta bytes=" + deltaBytes.length);
            JsonNode full = json.readTree(fullBytes), delta = json.readTree(deltaBytes);
            for (JsonNode body : List.of(full, delta)) {
                assertFalse(body.path("truncated").asBoolean());
                assertEquals(642, body.path("stepCount").asInt());
                assertEquals(json.valueToTree(input), body.path("resolvedInput"));
                assertEquals(InputSpec.DEFAULT_MAX_STEPS, body.path("maxSteps").asInt());
                assertEquals(InputSpec.DEFAULT_MAX_BYTES, body.path("maxBytes").asLong());
            }
            assertEquals(full.path("steps"), decode(delta.path("steps"), full.path("steps")));
            assertEquals(oracle(stations(input), k(input)).fiveDecimals(),
                    full.path("steps").get(641).path("variables").path("answer").asText());
        }
    }

    @Test
    void originalDomainsDefaultsOrderingAndFieldSpecificRefusalsRemainUnchanged() throws Exception {
        var spec = tracer.inputSpec();
        assertEquals(DsType.SEARCH_SPACE, tracer.dsType());
        assertEquals(Map.of("minLength", 2, "maxLength", 15, "minValue", 0, "maxValue", 1000,
                "requireSorted", true, "requireDistinct", true), spec.field("stations").getConstraints());
        assertEquals(Map.of("min", 1, "max", 50), spec.field("k").getConstraints());
        assertEquals(input(List.of(1, 13, 17, 23), 4), InputValidator.defaults(spec).asMap());
        assertEquals(input(List.of(1, 2, 3, 4, 5), 1), tracer.alternateInput());
        assertEquals(InputSpec.DEFAULT_MAX_STEPS, spec.getMaxSteps());
        assertEquals(InputSpec.DEFAULT_MAX_BYTES, spec.getMaxBytes());
        List<List<Integer>> invalidStations = List.of(List.of(), List.of(0), IntStream.rangeClosed(0, 15).boxed().toList(),
                List.of(1, 0), List.of(0, 0), List.of(-1, 1), List.of(0, 1001));
        for (var positions : invalidStations) assertRefused(input(positions, 1), "stations");
        assertRefused(Map.of("stations", List.of(0, 4294967296L), "k", 1), "stations");
        assertRefused(input(List.of(0, 1000), 0), "k");
        assertRefused(input(List.of(0, 1000), 51), "k");
        assertRefused(Map.of("stations", List.of(0, 1000), "k", 4294967297L), "k");
        var unknown = new HashMap<>(tracer.alternateInput());
        unknown.put("invented", 1);
        assertThrows(InputValidationException.class, () -> run(unknown));
    }

    @Test
    void aTinyBudgetCannotPublishACompletedAnswerOrDoneAnchor() {
        AlgorithmTracer limited = new AlgorithmTracer() {
            public String id() { return tracer.id(); }
            public DsType dsType() { return tracer.dsType(); }
            public InputSpec inputSpec() { return tracer.inputSpec().withMaxSteps(3); }
            public Map<String, Object> alternateInput() { return tracer.alternateInput(); }
            public String annotatedCode() { return tracer.annotatedCode(); }
            public void run(Inputs in, StepEmitter emit) { tracer.run(in, emit); }
        };
        var trace = runner.runDefaults(limited);
        assertTrue(trace.isTruncated());
        assertEquals(3, trace.getStepCount());
        int done = AnnotatedCode.parse(tracer.annotatedCode()).resolve("done");
        for (var step : trace.getSteps()) {
            assertFalse(step.getVariables().containsKey("answer"));
            assertNotEquals(done, step.getActiveLine());
        }
    }

    private Set<String> checkSnapshots(Map<String, Object> input) {
        var trace = assertAnswer(input);
        var code = AnnotatedCode.parse(tracer.annotatedCode());
        var anchors = new HashMap<Integer, String>();
        code.getAnchors().forEach((key, line) -> anchors.put(line, key));
        int[] positions = stations(input), gaps = gaps(positions);
        double low = 0, high = Arrays.stream(gaps).max().orElseThrow(), mid = Double.NaN;
        int iterations = 0, gapIndex = 0, needed = 0;
        var visited = new HashSet<String>();
        for (var step : trace.getSteps()) {
            var vars = step.getVariables();
            String phase = anchors.get(step.getActiveLine());
            assertNotNull(phase);
            visited.add(phase);
            assertEquals("continuous", vars.get("searchDomain"), phase);
            if (phase.equals("mid")) {
                mid = (low + high) / 2;
                iterations++;
                gapIndex = 0;
                needed = 0;
            } else if (phase.equals("gapTally")) {
                assertTrue(gapIndex < gaps.length);
                needed += (int) Math.ceil(gaps[gapIndex] / mid) - 1;
                assertEquals(String.valueOf(needed), vars.get("needed"));
                assertEquals("current", step.getArrayState().get(gapIndex).getState());
                gapIndex++;
            } else if (phase.equals("feasible")) {
                assertEquals(gaps.length, gapIndex);
                assertTrue(needed <= k(input));
                high = mid;
            } else if (phase.equals("infeasible")) {
                assertEquals(gaps.length, gapIndex);
                assertTrue(needed > k(input));
                low = mid;
            }
            assertEquals(low, number(vars, "low"), 0.0, phase + " actual low");
            assertEquals(high, number(vars, "high"), 0.0, phase + " actual high");
            assertEquals(high - low, number(vars, "intervalWidth"), 0.0, phase + " actual width");
            if (phase.equals("mid") || phase.equals("gapTally")) assertEquals(mid, number(vars, "mid"), 0.0, phase);
            else assertFalse(vars.containsKey("mid"), phase + " must not retain a retired probe");
            assertTrue(low <= high);
            assertEquals(positions.length - 1, step.getArrayState().size());
            for (int i = 0; i < gaps.length; i++) assertEquals(gaps[i], step.getArrayState().get(i).getValue());
        }
        assertEquals(HALVINGS, iterations);
        assertTrue(high - low < 1e-9);
        assertEquals(code.resolve("done"), trace.getSteps().get(trace.getStepCount() - 1).getActiveLine());
        return visited;
    }

    private ExecutionTrace assertAnswer(Map<String, Object> input) {
        var trace = run(input);
        assertFalse(trace.isTruncated(), input.toString());
        assertEquals(input, trace.getResolvedInput());
        assertEquals(oracle(stations(input), k(input)).fiveDecimals(), answer(trace), input.toString());
        return trace;
    }
    private ExecutionTrace run(Map<String, Object> input) { return runner.run(tracer, input); }
    private static String answer(ExecutionTrace trace) { return trace.getSteps().get(trace.getStepCount() - 1).getVariables().get("answer"); }
    private static double number(Map<String, String> vars, String name) {
        assertNotNull(vars.get(name), "Missing numeric snapshot " + name);
        double value = assertDoesNotThrow(() -> Double.parseDouble(vars.get(name)), name);
        assertTrue(Double.isFinite(value), name);
        return value;
    }
    private byte[] request(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String client = "gas-precision-" + CLIENTS.incrementAndGet();
        var result = http.perform(request.with(r -> { r.setRemoteAddr(client); return r; })).andReturn();
        assertEquals(client, result.getRequest().getRemoteAddr());
        assertEquals(expectedStatus, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return result.getResponse().getContentAsByteArray();
    }
    private void assertRefused(Map<String, Object> input, String field) throws Exception {
        assertTrue(assertThrows(InputValidationException.class, () -> run(input)).getFieldErrors().containsKey(field));
        var body = json.readTree(request(post(ENDPOINT + "/execute").contentType("application/json")
                .content(json.writeValueAsString(input)), 400));
        assertEquals("invalid_input", body.path("error").asText());
        assertTrue(body.path("fieldErrors").path(field).isTextual());
        assertFalse(body.has("steps"));
    }
    private JsonNode decode(JsonNode deltas, JsonNode full) {
        var result = json.createArrayNode();
        ObjectNode snapshot = json.createObjectNode();
        var fields = new HashSet<String>();
        for (var step : full) step.fieldNames().forEachRemaining(fields::add);
        for (var delta : deltas) {
            if (delta.path("keyframe").asBoolean()) {
                snapshot = json.createObjectNode();
                for (String name : fields) snapshot.set(name, NullNode.instance);
            }
            var iterator = delta.fields();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (!entry.getKey().equals("keyframe")) snapshot.set(entry.getKey(), entry.getValue());
            }
            result.add(snapshot.deepCopy());
        }
        return result;
    }
    private static void checkSource(java.lang.reflect.Method solve, Object instance, Map<String, Object> input) throws Exception {
        int[] positions = stations(input), before = positions.clone();
        double actual = (double) solve.invoke(instance, positions, k(input));
        var expected = oracle(before, k(input));
        assertEquals(expected.fiveDecimals(), String.format(Locale.ROOT, "%.5f", actual), input.toString());
        assertEquals(expected.value(), actual, 1e-9, input.toString());
        assertArrayEquals(before, positions, "The solution must preserve submitted station positions");
    }
    private static int[] stations(Map<String, Object> input) { return ((List<?>) input.get("stations")).stream().mapToInt(v -> ((Number) v).intValue()).toArray(); }
    private static int k(Map<String, Object> input) { return ((Number) input.get("k")).intValue(); }
    private static Map<String, Object> input(List<Integer> stations, int k) { return Map.of("stations", stations, "k", k); }
    private static int[] gaps(int[] stations) { return IntStream.range(1, stations.length).map(i -> stations[i] - stations[i - 1]).toArray(); }

    /** Enumerates allocation breakpoints exactly; no binary search or floating feasibility. */
    private static Fraction oracle(int[] stations, int k) {
        int[] gaps = gaps(stations);
        Fraction best = new Fraction(Arrays.stream(gaps).max().orElseThrow(), 1);
        for (int numerator : gaps) for (int denominator = 1; denominator <= k + 1; denominator++) {
            if ((long) numerator * best.denominator >= (long) best.numerator * denominator) continue;
            long extra = 0;
            for (int gap : gaps) extra += ((long) gap * denominator - 1) / numerator;
            if (extra <= k) best = new Fraction(numerator, denominator);
        }
        return best;
    }
    private record Fraction(int numerator, int denominator) {
        double value() { return (double) numerator / denominator; }
        String fiveDecimals() {
            long scaled = numerator * 100_000L;
            long rounded = (2 * scaled + denominator) / (2L * denominator);
            return String.format(Locale.ROOT, "%d.%05d", rounded / 100_000, rounded % 100_000);
        }
    }
    private static List<Map<String, Object>> examples() {
        var inputs = new ArrayList<Map<String, Object>>(List.of(input(List.of(1, 13, 17, 23), 4),
                input(List.of(1, 2, 3, 4, 5), 1), input(List.of(0, 1000), 2), input(List.of(0, 999), 2),
                input(List.of(0, 100, 500, 1000), 5), input(List.of(0, 3), 39),
                input(List.of(0, 1), 39), input(List.of(0, 1), 7), input(List.of(0, 12), 2)));
        inputs.addAll(maxima());
        return inputs;
    }
    private static List<Map<String, Object>> maxima() {
        var longLastGap = new ArrayList<>(IntStream.rangeClosed(0, 13).boxed().toList());
        longLastGap.add(1000);
        return List.of(input(longLastGap, 50), input(IntStream.rangeClosed(0, 14).boxed().toList(), 50));
    }
    private static List<Map<String, Object>> randomInputs(int count, long seed) {
        var result = new ArrayList<Map<String, Object>>();
        var random = new Random(seed);
        for (int i = 0; i < count; i++) {
            int size = 2 + random.nextInt(14);
            var stations = new TreeSet<Integer>();
            while (stations.size() < size) stations.add(random.nextInt(1001));
            result.add(input(new ArrayList<>(stations), 1 + random.nextInt(50)));
        }
        return result;
    }
}
