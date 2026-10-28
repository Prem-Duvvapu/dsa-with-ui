package com.dsa.ui.tracer;

import com.dsa.ui.approach.SolutionApproachRegistry;
import com.dsa.ui.model.DsType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Work-shape safety is declared, served and validated independently of execution. */
@SpringBootTest
@AutoConfigureMockMvc
class WorkLimitContractTest {
    private static final AtomicInteger CLIENTS = new AtomicInteger();
    private final ObjectMapper json = new ObjectMapper();
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;

    @Test
    void allSixActualSpecsServeTheirExistingWorkLimitsThroughEveryMetadataSeam() throws Exception {
        var summaries = json.readTree(request(get("/api/problems"), 200));
        for (var owner : owners()) {
            JsonNode expected = expectedRule(owner);
            assertEquals(expected, json.valueToTree(owner.tracer().inputSpec()).path("workLimits"), owner.toString());
            var detail = json.readTree(request(get(path(owner)).param("approach", owner.form()), 200));
            var spec = json.readTree(request(get(path(owner) + "/input-spec").param("approach", owner.form()), 200));
            assertEquals(expected, spec.path("workLimits"));
            assertEquals(spec, detail.path("inputSpec"));
            JsonNode nested = null;
            for (var entry : detail.path("approaches")) if (entry.path("id").asText().equals(owner.form())) nested = entry;
            assertNotNull(nested);
            assertEquals(spec, nested.path("inputSpec"));
            if (owner.form().equals("canonical")) {
                JsonNode summary = null;
                for (var entry : summaries) if (entry.path("id").asText().equals(owner.id())) summary = entry;
                assertNotNull(summary);
                assertEquals(expected, summary.path("inputSpec").path("workLimits"));
            }
        }
        var fractional = json.readTree(request(get("/api/problems/fractional-knapsack/input-spec"), 200));
        assertFalse(fractional.has("workLimits"), "This package does not extend fractional Knapsack");
    }

    @Test
    void actualSharedValidatorRefusesExistingTableAndTreeOverflowsWithoutCallingTheTracer() {
        for (var owner : owners()) {
            var supplied = over(owner);
            var before = new LinkedHashMap<>(supplied);
            var failure = assertThrows(InputValidationException.class,
                    () -> InputValidator.validate(owner.tracer().inputSpec(), supplied), owner.toString());
            assertTrue(failure.getFieldErrors().containsKey(errorField(owner)), owner.toString());
            assertEquals(before, supplied);
            if (owner.form().equals("recursion")) assertEquals(221, exactCalls(weights(supplied), capacity(supplied), reuse(owner)));
        }
    }

    @Test
    void workMetadataIsImmutableOptionalAndRetainedInEitherBuilderOrderAndBudgetCopies() {
        InputSpec plain = synthetic();
        assertFalse(json.valueToTree(plain).has("workLimits"));
        assertEquals(List.of(), rules(plain));
        InputWorkLimit rule = rule("CAPACITY_TABLE_CELLS", "weights", "capacity", 80, false, "weights", "Use a smaller input.");
        InputSpec spec = add(plain.withEqualLengthGroup("weights", "values"), rule);
        assertEquals(List.of(rule), rules(spec));
        assertEquals(List.of(), rules(plain));
        assertThrows(UnsupportedOperationException.class, () -> rules(spec).clear());
        assertEquals(rules(spec), rules(spec.withMaxSteps(3)));
        assertEquals(rules(spec), rules(spec.withMaxBytes(99)));
        assertEquals(spec.getEqualLengthGroups(), spec.withMaxSteps(3).getEqualLengthGroups());
        assertEquals(json.valueToTree(spec), json.valueToTree(add(plain, rule).withEqualLengthGroup("weights", "values")));
    }

    @Test
    void malformedOrAmbiguousWorkDeclarationsFailBeforeTheyCanBeServed() {
        InputSpec plain = synthetic();
        assertThrows(IllegalArgumentException.class, () -> add(plain, null));
        assertThrows(IllegalArgumentException.class, () -> rule(null, "weights", "capacity", 80, false, "weights", "Too large."));
        for (String field : List.of("missing", "values")) {
            assertThrows(IllegalArgumentException.class, () -> add(plain,
                    rule("CAPACITY_TABLE_CELLS", "weights", field, 80, false, "weights", "Too large.")));
        }
        assertThrows(IllegalArgumentException.class, () -> add(plain,
                rule("CAPACITY_TABLE_CELLS", "capacity", "capacity", 80, false, "weights", "Too large.")));
        assertThrows(IllegalArgumentException.class, () -> add(plain,
                rule("CAPACITY_TABLE_CELLS", "weights", "capacity", 80, true, "weights", "Too large.")));
        for (int limit : List.of(0, -1)) assertThrows(IllegalArgumentException.class, () -> rule(
                "SKIP_TAKE_CALLS", "weights", "capacity", limit, true, "weights", "Too large."));
        assertThrows(IllegalArgumentException.class, () -> add(plain,
                rule("SKIP_TAKE_CALLS", "weights", "capacity", 220, true, "missing", "Too large.")));
        assertThrows(IllegalArgumentException.class, () -> rule("SKIP_TAKE_CALLS", "weights", "capacity", 220, true, "weights", ""));
        InputWorkLimit table = rule("CAPACITY_TABLE_CELLS", "weights", "capacity", 80, false, "weights", "Too large.");
        assertThrows(IllegalArgumentException.class, () -> add(add(plain, table), table));
        InputSpec ambiguous = InputSpec.of(array("weights", 1), array("weights", 1), array("values", 1), scalar());
        assertThrows(IllegalArgumentException.class, () -> add(ambiguous, table));
        for (Object bad : List.of("30", 1.5, 4294967296L, -1)) {
            InputSpec invalid = InputSpec.of(array("weights", 1), array("values", 1),
                    InputField.of("capacity", FieldType.INT).range(1, 30).constraint("max", bad).defaultValue(5).build());
            assertThrows(IllegalArgumentException.class, () -> add(invalid, table));
        }
        for (int weightMinimum : List.of(0, -1)) {
            InputSpec invalid = InputSpec.of(array("weights", weightMinimum), array("values", 1), scalar());
            assertThrows(IllegalArgumentException.class, () -> add(invalid,
                    rule("SKIP_TAKE_CALLS", "weights", "capacity", 220, true, "weights", "Too large.")));
        }
        InputSpec negativeCapacity = InputSpec.of(array("weights", 1), array("values", 1),
                InputField.of("capacity", FieldType.INT).range(-1, 30).defaultValue(5).build());
        assertThrows(IllegalArgumentException.class, () -> add(negativeCapacity, table));
        InputSpec integralMetadata = InputSpec.of(array("weights", 1), array("values", 1),
                InputField.of("capacity", FieldType.INT).range(1, 30).constraint("max", 30.0).defaultValue(5).build());
        assertDoesNotThrow(() -> add(integralMetadata, table));
        InputSpec missingMinimum = InputSpec.of(InputField.of("weights", FieldType.INT_ARRAY)
                .constraint("maxLength", 8).values(1, 20).defaultValue(List.of(1)).build(), scalar());
        assertThrows(IllegalArgumentException.class, () -> add(missingMinimum, table));
    }

    @Test
    void boundedCallCounterAgreesWithAnIndependentUncachedTreeIncludingBaseCasesAndOrder() {
        for (boolean reuse : List.of(false, true)) {
            InputWorkLimit rule = rule("SKIP_TAKE_CALLS", "weights", "capacity", 220, reuse, "weights", "Too large.");
            for (int n = 0; n <= 4; n++) {
                int vectors = (int) Math.pow(3, n);
                for (int code = 0; code < vectors; code++) {
                    int remaining = code;
                    var weights = new ArrayList<Integer>();
                    for (int i = 0; i < n; i++, remaining /= 3) weights.add(1 + remaining % 3);
                    long previous = 0;
                    for (int c = 0; c <= 8; c++) {
                        long expected = Math.min(221, exactCalls(weights, c, reuse));
                        long actual = measure(rule, weights, c);
                        assertEquals(expected, actual, weights + " C=" + c + " reuse=" + reuse);
                        assertTrue(actual >= previous);
                        previous = actual;
                    }
                }
            }
        }
        InputWorkLimit reusable = rule("SKIP_TAKE_CALLS", "weights", "capacity", 220, true, "weights", "Too large.");
        assertEquals(216, measure(reusable, List.of(1, 3), 23));
        assertEquals(221, measure(reusable, List.of(3, 1), 23), "Never sort submitted weights before counting");
        for (var owner : owners()) if (owner.form().equals("recursion")) {
            assertEquals(220, measure(ruleFor(owner), weights(maximum(owner)), capacity(maximum(owner))));
            assertEquals(221, measure(ruleFor(owner), weights(over(owner)), capacity(over(owner))));
        }
    }

    @Test
    void arithmeticDoesNotOverflowOrAllocateInProportionToAnArbitraryCapacity() {
        InputWorkLimit calls = rule("SKIP_TAKE_CALLS", "weights", "capacity", 220, true, "weights", "Too large.");
        var tracked = new AbstractList<Integer>() {
            private int reads;
            public int size() { return 8; }
            public Integer get(int index) {
                assertTrue(++reads <= 220, "The counter must stop after the first limit+1 visits");
                return 1;
            }
        };
        assertEquals(221, measure(calls, tracked, Integer.MAX_VALUE));
        assertEquals(3, measure(calls, List.of(Integer.MAX_VALUE), Integer.MAX_VALUE));
        assertEquals(1, measure(calls, List.of(), Integer.MAX_VALUE));
        InputWorkLimit table = rule("CAPACITY_TABLE_CELLS", "weights", "capacity", 225, false, "capacity", "Too large.");
        var enormousLength = new AbstractList<Integer>() {
            public int size() { return Integer.MAX_VALUE; }
            public Integer get(int index) { throw new AssertionError("Table shape needs length only"); }
        };
        assertEquals(4_611_686_018_427_387_904L, measure(table, enormousLength, Integer.MAX_VALUE));
        for (String kind : List.of("CAPACITY_TABLE_CELLS", "SKIP_TAKE_CALLS")) {
            InputSpec zero = InputSpec.of(
                    InputField.of("weights", FieldType.INT_ARRAY).length(0, 0).values(1, 20).defaultValue(List.of()).build(),
                    InputField.of("capacity", FieldType.INT).range(0, 0).defaultValue(0).build());
            InputWorkLimit base = rule(kind, "weights", "capacity", 1, false, "weights", "Too large.");
            assertEquals(1, measure(base, List.of(), 0));
            assertEquals(0, InputValidator.defaults(add(zero, base)).getInt("capacity"));
        }
    }

    @Test
    void actualHttpLimitsKeepOriginalKeysAndNoPartialTraceInEitherEncoding() throws Exception {
        for (var owner : owners()) for (String encoding : List.of("full", "delta")) {
            JsonNode refused = json.readTree(request(post(path(owner) + "/execute").param("approach", owner.form())
                    .param("encoding", encoding).contentType("application/json").content(json.writeValueAsString(over(owner))), 400));
            assertEquals("invalid_input", refused.path("error").asText());
            assertTrue(refused.path("fieldErrors").path(errorField(owner)).isTextual());
            assertFalse(refused.has("steps"));
            assertFalse(refused.has("resolvedInput"));
            JsonNode accepted = json.readTree(request(post(path(owner) + "/execute").param("approach", owner.form())
                    .param("encoding", encoding).contentType("application/json").content(json.writeValueAsString(maximum(owner))), 200));
            assertFalse(accepted.path("truncated").asBoolean());
            assertEquals(json.valueToTree(maximum(owner)), accepted.path("resolvedInput"));
            assertEquals(InputSpec.DEFAULT_MAX_STEPS, accepted.path("maxSteps").asInt());
            assertEquals(InputSpec.DEFAULT_MAX_BYTES, accepted.path("maxBytes").asLong());
            var done = accepted.path("steps").get(accepted.path("stepCount").asInt() - 1);
            assertEquals(oracle(owner, maximum(owner)), done.path("variables").path("answer").asInt());
            if (owner.form().equals("recursion")) assertEquals(220, done.path("variables").path("calls").asInt());
        }
    }

    @Test
    void cheapFullDomainTreesRemainAcceptedAndFieldPairAndEmitterBudgetsRemainIndependent() {
        for (var owner : owners()) {
            var cheap = input(Collections.nCopies(8, 20), Collections.nCopies(8, 100), 30);
            if (owner.form().equals("recursion")) {
                assertDoesNotThrow(() -> InputValidator.validate(owner.tracer().inputSpec(), cheap));
                var trace = runner.run(owner.tracer(), cheap);
                assertFalse(trace.isTruncated());
                assertEquals("100", trace.getSteps().get(trace.getStepCount() - 1).getVariables().get("answer"));
                assertEquals(reuse(owner) ? "53" : "45", trace.getSteps().get(trace.getStepCount() - 1).getVariables().get("calls"));
            } else assertThrows(InputValidationException.class, () -> InputValidator.validate(owner.tracer().inputSpec(), cheap));
            var mismatch = input(List.of(1, 2), List.of(1), 30);
            assertTrue(assertThrows(InputValidationException.class,
                    () -> InputValidator.validate(owner.tracer().inputSpec(), mismatch)).getFieldErrors().containsKey("values"));
            AlgorithmTracer tiny = new AlgorithmTracer() {
                public String id() { return owner.id(); }
                public DsType dsType() { return owner.tracer().dsType(); }
                public InputSpec inputSpec() { return owner.tracer().inputSpec().withMaxSteps(3); }
                public Map<String, Object> alternateInput() { return owner.tracer().alternateInput(); }
                public String annotatedCode() { return owner.tracer().annotatedCode(); }
                public void run(Inputs in, StepEmitter emit) { owner.tracer().run(in, emit); }
            };
            var trace = runner.runDefaults(tiny);
            assertTrue(trace.isTruncated());
            assertEquals(3, trace.getStepCount());
            for (var step : trace.getSteps()) assertNotEquals(trace.getAnchors().get("done").intValue(), step.getActiveLine());
        }
    }

    private record Owner(String id, String form, AlgorithmTracer tracer) {}
    private List<Owner> owners() {
        var result = new ArrayList<Owner>();
        for (String id : List.of("knapsack-01", "unbounded-knapsack")) for (var form : List.of("canonical", "memoization", "recursion")) {
            result.add(new Owner(id, form, approaches.resolve(id, form).tracer()));
        }
        return result;
    }
    private static String path(Owner owner) { return "/api/problems/" + owner.id(); }
    private static boolean reuse(Owner owner) { return owner.id().equals("unbounded-knapsack"); }
    private static String errorField(Owner owner) { return owner.form().equals("canonical") ? "capacity" : "weights"; }
    private static int limit(Owner owner) { return owner.form().equals("canonical") ? 225 : owner.form().equals("memoization") ? 80 : 220; }
    private JsonNode expectedRule(Owner owner) {
        return json.valueToTree(List.of(Map.of("kind", owner.form().equals("recursion") ? "SKIP_TAKE_CALLS" : "CAPACITY_TABLE_CELLS",
                "arrayField", "weights", "scalarField", "capacity", "limit", limit(owner), "reuse", owner.form().equals("recursion") && reuse(owner),
                "errorField", errorField(owner), "message", expectedMessage(owner))));
    }
    private static String expectedMessage(Owner owner) {
        return owner.form().equals("canonical") ? "Use a smaller capacity or fewer items."
                : owner.form().equals("memoization") ? "Memo tracing needs (n+1) × (capacity+1) ≤ 80 cells; use fewer paired items or a smaller capacity."
                : "This input needs more than 220 recursive calls. Use fewer paired items, a smaller capacity or larger weights, or select Memoization.";
    }
    private static InputWorkLimit ruleFor(Owner owner) { return rule(owner.form().equals("recursion") ? "SKIP_TAKE_CALLS" : "CAPACITY_TABLE_CELLS",
            "weights", "capacity", limit(owner), owner.form().equals("recursion") && reuse(owner), errorField(owner), expectedMessage(owner)); }
    private static Map<String, Object> maximum(Owner owner) {
        if (owner.form().equals("canonical")) return input(Collections.nCopies(8, 20), Collections.nCopies(8, 100), 24);
        if (owner.form().equals("memoization")) return input(Collections.nCopies(7, 1), Collections.nCopies(7, 100), 9);
        return reuse(owner) ? input(List.of(17,2,4,15), Collections.nCopies(4, 100), 26)
                : input(List.of(3,1,15,5,3,19,7,17), Collections.nCopies(8, 100), 29);
    }
    private static Map<String, Object> over(Owner owner) {
        if (owner.form().equals("canonical")) return input(Collections.nCopies(8, 1), Collections.nCopies(8, 1), 25);
        if (owner.form().equals("memoization")) return input(Collections.nCopies(8, 1), Collections.nCopies(8, 1), 8);
        return reuse(owner) ? input(List.of(3,6,11,13,1,5), Collections.nCopies(6, 1), 12)
                : input(List.of(17,10,5,5,11,17,12,2), Collections.nCopies(8, 1), 30);
    }
    private static long exactCalls(List<Integer> weights, int capacity, boolean reuse) {
        return calls(weights, weights.size(), capacity, reuse);
    }
    private static long calls(List<Integer> weights, int n, int capacity, boolean reuse) {
        if (n == 0 || capacity == 0) return 1;
        return 1 + calls(weights, n - 1, capacity, reuse)
                + (weights.get(n - 1) <= capacity ? calls(weights, reuse ? n : n - 1, capacity - weights.get(n - 1), reuse) : 0);
    }
    private static int oracle(Owner owner, Map<String, Object> input) {
        return quantities(weights(input), ((List<?>) input.get("values")).stream().mapToInt(v -> ((Number) v).intValue()).toArray(), 0, capacity(input), reuse(owner));
    }
    private static int quantities(List<Integer> weights, int[] values, int index, int capacity, boolean reuse) {
        if (index == weights.size()) return 0;
        int best = 0, max = Math.min(capacity / weights.get(index), reuse ? Integer.MAX_VALUE : 1);
        for (int taken = 0; taken <= max; taken++) best = Math.max(best, taken * values[index]
                + quantities(weights, values, index + 1, capacity - taken * weights.get(index), reuse));
        return best;
    }
    private static List<Integer> weights(Map<String, Object> input) { return ((List<?>) input.get("weights")).stream().map(v -> ((Number) v).intValue()).toList(); }
    private static int capacity(Map<String, Object> input) { return ((Number) input.get("capacity")).intValue(); }
    private static Map<String, Object> input(List<Integer> weights, List<Integer> values, int capacity) { return Map.of("weights", weights, "values", values, "capacity", capacity); }
    private static InputField array(String name, int minimum) { return InputField.of(name, FieldType.INT_ARRAY).length(1, 8).values(minimum, 20).defaultValue(List.of(1)).build(); }
    private static InputField scalar() { return InputField.of("capacity", FieldType.INT).range(1, 30).defaultValue(5).build(); }
    private static InputSpec synthetic() { return InputSpec.of(array("weights", 1), array("values", 1), scalar()); }
    private static InputWorkLimit rule(String kind, String array, String scalar, int limit, boolean reuse, String error, String message) {
        return new InputWorkLimit(kind == null ? null : InputWorkLimit.Kind.valueOf(kind), array, scalar, limit, reuse, error, message);
    }
    private static InputSpec add(InputSpec spec, InputWorkLimit rule) { return spec.withWorkLimit(rule); }
    private static List<InputWorkLimit> rules(InputSpec spec) { return spec.getWorkLimits(); }
    private static long measure(InputWorkLimit rule, List<? extends Number> array, int scalar) { return rule.measure(array, scalar); }
    private byte[] request(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String client = "work-limit-contract-" + CLIENTS.incrementAndGet();
        var result = http.perform(request.with(r -> { r.setRemoteAddr(client); return r; })).andReturn();
        assertEquals(client, result.getRequest().getRemoteAddr());
        assertEquals(expectedStatus, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return result.getResponse().getContentAsByteArray();
    }
}
