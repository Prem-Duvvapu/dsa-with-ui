package com.dsa.ui.tracer;

import com.dsa.ui.approach.SolutionApproachRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Direct scalar tables and the two exact coin call trees share declared work bounds. */
@SpringBootTest
@AutoConfigureMockMvc
class CoinSubsetWorkLimitContractTest {
    private static final AtomicInteger CLIENTS = new AtomicInteger();
    private final ObjectMapper json = new ObjectMapper();
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;

    record Contract(String id, String form) {
        boolean coin() { return id.equals("minimum-coins-dp") || id.equals("coin-change-2"); }
        String array() { return coin() ? "coins" : "nums"; }
        String scalar() { return coin() ? "amount" : id.equals("subset-sum-equal-target") ? "target" : "k"; }
        int limit() { return form.equals("canonical") ? 225 : form.equals("memoization") ? 80 : 220; }
        String errorField() { return form.equals("canonical") ? scalar() : array(); }
        public String toString() { return id + "/" + form; }
    }
    static Stream<Contract> contracts() {
        var result = new ArrayList<Contract>();
        for (String id : List.of("minimum-coins-dp", "coin-change-2", "subset-sum-equal-target", "count-subsets-with-sum-k")) {
            for (String form : id.contains("coin") ? List.of("canonical", "memoization", "recursion") : List.of("canonical", "memoization")) {
                result.add(new Contract(id, form));
            }
        }
        return result.stream();
    }

    @ParameterizedTest(name = "served contract: {0}") @MethodSource("contracts")
    void actualMetadataServesTheExistingBoundAtAllClientSeams(Contract c) throws Exception {
        var tracer = tracer(c);
        assertEquals(List.of(rule(c)), tracer.inputSpec().getWorkLimits());
        var detail = json.readTree(request(get(path(c)).param("approach", c.form()), 200));
        var spec = json.readTree(request(get(path(c) + "/input-spec").param("approach", c.form()), 200));
        assertEquals(json.valueToTree(List.of(rule(c))), spec.path("workLimits"));
        assertEquals(spec, detail.path("inputSpec"));
        JsonNode nested = null;
        for (var approach : detail.path("approaches")) if (approach.path("id").asText().equals(c.form())) nested = approach;
        assertNotNull(nested);
        assertEquals(spec, nested.path("inputSpec"));
        if (c.form().equals("canonical")) {
            var summaries = json.readTree(request(get("/api/problems"), 200));
            JsonNode summary = null;
            for (var entry : summaries) if (entry.path("id").asText().equals(c.id())) summary = entry;
            assertNotNull(summary);
            assertEquals(spec, summary.path("inputSpec"));
        }
    }

    @ParameterizedTest(name = "shared validation: {0}") @MethodSource("contracts")
    void normalizedInputValidationRefusesOversizedShapesBeforeExecution(Contract c) {
        var supplied = over(c);
        var before = new LinkedHashMap<>(supplied);
        var failure = assertThrows(InputValidationException.class,
                () -> InputValidator.validate(tracer(c).inputSpec(), supplied));
        assertTrue(failure.getFieldErrors().containsKey(c.errorField()));
        assertEquals(before, supplied);
        if (c.form().equals("recursion")) assertEquals(221, calls(values(c, supplied), values(c, supplied).size(), target(c, supplied)));
        else assertTrue((long) (values(c, supplied).size() + 1) * (target(c, supplied) + 1) > c.limit());
    }

    @ParameterizedTest(name = "HTTP boundary: {0}") @MethodSource("contracts")
    void originalMaximaCompleteAndOneOverReturnsFieldErrorsWithoutStepsInEitherEncoding(Contract c) throws Exception {
        for (String encoding : List.of("full", "delta")) {
            var refused = json.readTree(request(post(path(c) + "/execute").param("approach", c.form()).param("encoding", encoding)
                    .contentType("application/json").content(json.writeValueAsString(over(c))), 400));
            assertEquals("invalid_input", refused.path("error").asText());
            assertTrue(refused.path("fieldErrors").path(c.errorField()).isTextual());
            assertFalse(refused.has("steps"));
            assertFalse(refused.has("resolvedInput"));
            byte[] bytes = request(post(path(c) + "/execute").param("approach", c.form()).param("encoding", encoding)
                    .contentType("application/json").content(json.writeValueAsString(maximum(c))), 200);
            assertTrue(bytes.length < InputSpec.DEFAULT_MAX_BYTES, c + " " + encoding + " bytes=" + bytes.length);
            var body = json.readTree(bytes);
            assertFalse(body.path("truncated").asBoolean());
            assertEquals(json.valueToTree(maximum(c)), body.path("resolvedInput"));
            assertEquals(InputSpec.DEFAULT_MAX_STEPS, body.path("maxSteps").asInt());
            assertEquals(InputSpec.DEFAULT_MAX_BYTES, body.path("maxBytes").asLong());
            var done = body.path("steps").get(body.path("stepCount").asInt() - 1);
            assertEquals(oracle(c, maximum(c)), done.path("variables").path("answer").asText());
            if (c.form().equals("recursion")) assertEquals("220", done.path("variables").path("calls").asText());
        }
    }

    @ParameterizedTest(name = "unchanged domains/results: {0}") @MethodSource("contracts")
    void originalDomainsDefaultsAlternatesAndHonestTinyBudgetsRemainIntact(Contract c) {
        var tracer = tracer(c);
        var spec = tracer.inputSpec();
        assertEquals(InputSpec.DEFAULT_MAX_STEPS, spec.getMaxSteps());
        assertEquals(InputSpec.DEFAULT_MAX_BYTES, spec.getMaxBytes());
        assertEquals(2, spec.getFields().size());
        var expectedArray = new HashMap<String, Object>(Map.of("minLength", 1, "maxLength", c.coin() ? 8 : 10,
                "minValue", c.id().equals("subset-sum-equal-target") ? 0 : 1,
                "maxValue", c.coin() ? 25 : c.id().equals("subset-sum-equal-target") ? 20 : 15));
        if (c.id().equals("coin-change-2")) expectedArray.put("requireDistinct", true);
        assertEquals(expectedArray, spec.field(c.array()).getConstraints());
        assertEquals(Map.of("min", c.coin() ? 1 : 0, "max", c.id().equals("count-subsets-with-sum-k") ? 60 : 40),
                spec.field(c.scalar()).getConstraints());
        for (var input : List.of(InputValidator.defaults(spec).asMap(), tracer.alternateInput(), maximum(c))) assertAnswer(c, input);
        if (c.form().equals("recursion")) {
            assertAnswer(c, input(c, List.of(1), 40));
            assertAnswer(c, input(c, IntStream.rangeClosed(18, 25).boxed().toList(), 40));
        }
        AlgorithmTracer tiny = new AlgorithmTracer() {
            public String id() { return tracer.id(); }
            public com.dsa.ui.model.DsType dsType() { return tracer.dsType(); }
            public InputSpec inputSpec() { return spec.withMaxSteps(3); }
            public Map<String, Object> alternateInput() { return tracer.alternateInput(); }
            public String annotatedCode() { return tracer.annotatedCode(); }
            public void run(Inputs in, StepEmitter emit) { tracer.run(in, emit); }
        };
        var trace = runner.runDefaults(tiny);
        assertTrue(trace.isTruncated());
        assertEquals(3, trace.getStepCount());
        for (var step : trace.getSteps()) assertNotEquals(trace.getAnchors().get("done").intValue(), step.getActiveLine());
    }

    @Test void subsetZeroRulesAndDerivedTargetShortcutsRemainTheirOwnContracts() {
        for (String form : List.of("canonical", "memoization", "recursion")) {
            var feasible = new Contract("subset-sum-equal-target", form);
            assertAnswer(feasible, input(feasible, List.of(0, 0, 1), 1));
            assertAnswer(feasible, input(feasible, List.of(0, 0), 0));
            var count = new Contract("count-subsets-with-sum-k", form);
            assertThrows(InputValidationException.class,
                    () -> runner.run(tracer(count), input(count, List.of(0, 1), 1)));
            if (form.equals("recursion")) {
                assertTrue(tracer(feasible).inputSpec().getWorkLimits().isEmpty());
                assertTrue(tracer(count).inputSpec().getWorkLimits().isEmpty());
            }
        }
        for (String id : List.of("partition-equal-subset-sum", "count-partitions-given-diff")) {
            for (var approach : approaches.available(id)) assertTrue(approach.tracer().inputSpec().getWorkLimits().isEmpty(), id);
        }
        var odd = approaches.resolve("partition-equal-subset-sum", "memoization").tracer();
        var oddInput = new ArrayList<>(Collections.nCopies(9, 20)); oddInput.add(19);
        var trace = runner.run(odd, Map.of("nums", oddInput));
        assertFalse(trace.isTruncated()); assertEquals(1, trace.getStepCount());
        assertEquals("false", trace.getSteps().get(0).getVariables().get("answer"));
        assertEquals("0", trace.getSteps().get(0).getVariables().get("calls"));
        var difference = approaches.resolve("count-partitions-given-diff", "memoization").tracer();
        var impossible = runner.run(difference, Map.of("nums", Collections.nCopies(10, 15), "d", 59));
        assertFalse(impossible.isTruncated()); assertEquals(1, impossible.getStepCount());
        assertEquals("0", impossible.getSteps().get(0).getVariables().get("answer"));
    }

    private AlgorithmTracer tracer(Contract c) { return approaches.resolve(c.id(), c.form()).tracer(); }
    private static String path(Contract c) { return "/api/problems/" + c.id(); }
    private static InputWorkLimit rule(Contract c) {
        String message = c.form().equals("recursion")
                ? "This input needs more than 220 recursive calls. Use fewer denominations, a smaller amount or larger coins, or select Memoization."
                : c.form().equals("memoization") ? c.coin()
                ? "Memo tracing needs (n+1) × (amount+1) ≤ 80 cells; use fewer denominations or a smaller amount."
                : c.id().equals("subset-sum-equal-target")
                ? "Memo tracing needs (n+1) × (target+1) ≤ 80 cells; use fewer/smaller values or a smaller target."
                : "Memo tracing needs (n+1) × (k+1) ≤ 80 cells; use fewer numbers or a smaller k."
                : c.coin() ? "Use a smaller amount or fewer coins."
                : c.id().equals("subset-sum-equal-target") ? "Use a smaller target or fewer numbers." : "Use a smaller k or fewer numbers.";
        return new InputWorkLimit(c.form().equals("recursion") ? InputWorkLimit.Kind.SKIP_TAKE_CALLS : InputWorkLimit.Kind.CAPACITY_TABLE_CELLS,
                c.array(), c.scalar(), c.limit(), c.form().equals("recursion"), c.errorField(), message);
    }
    private static Map<String, Object> maximum(Contract c) {
        if (c.form().equals("recursion")) return input(c, List.of(23,6,7,15,3,11), 23);
        return input(c, IntStream.rangeClosed(1, c.form().equals("canonical") ? 8 : 7).boxed().toList(), c.form().equals("canonical") ? 24 : 9);
    }
    private static Map<String, Object> over(Contract c) {
        if (c.form().equals("recursion")) return input(c, List.of(7,23,25,6,5,20), 31);
        return input(c, IntStream.rangeClosed(1, 8).boxed().toList(), c.form().equals("canonical") ? 25 : 8);
    }
    private void assertAnswer(Contract c, Map<String, Object> supplied) {
        var trace = runner.run(tracer(c), supplied);
        assertFalse(trace.isTruncated(), c + " " + supplied);
        assertEquals(supplied, trace.getResolvedInput());
        assertEquals(oracle(c, supplied), trace.getSteps().get(trace.getStepCount() - 1).getVariables().get("answer"));
    }
    private static String oracle(Contract c, Map<String, Object> supplied) {
        var values = values(c, supplied); int target = target(c, supplied);
        if (c.id().equals("minimum-coins-dp")) {
            var reached = new boolean[target + 1]; var queue = new ArrayDeque<Integer>(); queue.add(0); reached[0] = true;
            for (int depth = 0; !queue.isEmpty(); depth++) for (int left = queue.size(); left > 0; left--) {
                int sum = queue.remove(); if (sum == target) return String.valueOf(depth);
                for (int coin : values) if (sum + coin <= target && !reached[sum + coin]) { reached[sum + coin] = true; queue.add(sum + coin); }
            }
            return "-1";
        }
        if (c.coin()) return String.valueOf(quantities(values, 0, target));
        int count = 0;
        for (int mask = 0; mask < (1 << values.size()); mask++) {
            int sum = 0; for (int i = 0; i < values.size(); i++) if ((mask & (1 << i)) != 0) sum += values.get(i);
            if (sum == target) count++;
        }
        return c.id().equals("subset-sum-equal-target") ? String.valueOf(count > 0) : String.valueOf(count);
    }
    private static int quantities(List<Integer> coins, int index, int remaining) {
        if (index == coins.size()) return remaining == 0 ? 1 : 0;
        int count = 0; for (int taken = 0; taken * coins.get(index) <= remaining; taken++) count += quantities(coins, index + 1, remaining - taken * coins.get(index));
        return count;
    }
    private static long calls(List<Integer> coins, int n, int remaining) {
        if (n == 0 || remaining == 0) return 1;
        return 1 + calls(coins, n - 1, remaining) + (coins.get(n - 1) <= remaining ? calls(coins, n, remaining - coins.get(n - 1)) : 0);
    }
    private static Map<String, Object> input(Contract c, List<Integer> values, int target) { return Map.of(c.array(), values, c.scalar(), target); }
    private static List<Integer> values(Contract c, Map<String, Object> input) { return ((List<?>) input.get(c.array())).stream().map(v -> ((Number) v).intValue()).toList(); }
    private static int target(Contract c, Map<String, Object> input) { return ((Number) input.get(c.scalar())).intValue(); }
    private byte[] request(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String client = "coin-subset-work-" + CLIENTS.incrementAndGet();
        var result = http.perform(request.with(r -> { r.setRemoteAddr(client); return r; })).andReturn();
        assertEquals(client, result.getRequest().getRemoteAddr());
        assertEquals(expectedStatus, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return result.getResponse().getContentAsByteArray();
    }
}
