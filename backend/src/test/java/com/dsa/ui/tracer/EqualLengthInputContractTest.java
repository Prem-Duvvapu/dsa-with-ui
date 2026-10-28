package com.dsa.ui.tracer;

import com.dsa.ui.approach.SolutionApproachRegistry;
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

/** Pairs are a map-level input invariant, not a repair applied by a tracer or editor. */
@SpringBootTest
@AutoConfigureMockMvc
class EqualLengthInputContractTest {
    private static final List<String> OWNERS = List.of("knapsack-01", "unbounded-knapsack", "fractional-knapsack");
    private static final AtomicInteger CLIENTS = new AtomicInteger();
    private final ObjectMapper json = new ObjectMapper();
    @Autowired TracerRegistry registry;
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;

    @Test
    void actualOwnerSpecsDeclarePairingWithoutChangingTheOriginalFieldDomains() {
        for (var owner : owners()) {
            InputSpec spec = owner.tracer().inputSpec();
            boolean fractional = owner.id().equals("fractional-knapsack");
            assertEquals(json.valueToTree(List.of(owner.group())), wire(spec).path("equalLengthGroups"), owner.toString());
            assertEquals(Map.of("minLength", 1, "maxLength", fractional ? 20 : 8,
                    "minValue", 1, "maxValue", fractional ? 100 : 20), spec.field("weights").getConstraints());
            assertEquals(Map.of("minLength", 1, "maxLength", fractional ? 20 : 8,
                    "minValue", 1, "maxValue", fractional ? 500 : 100), spec.field("values").getConstraints());
            assertEquals(Map.of("min", 1, "max", fractional ? 1000 : 30), spec.field("capacity").getConstraints());
            assertEquals(3, spec.getFields().size());
            assertEquals(InputSpec.DEFAULT_MAX_STEPS, spec.getMaxSteps());
            assertEquals(InputSpec.DEFAULT_MAX_BYTES, spec.getMaxBytes());
            var defaults = fractional ? input(List.of(10,20,30), List.of(60,100,120), 50)
                    : owner.id().equals("knapsack-01") ? input(List.of(1,2,4,5), List.of(5,4,8,6), 5)
                    : input(List.of(2,4,6), List.of(5,11,13), 10);
            assertEquals(defaults, InputValidator.defaults(spec).asMap());
            if (owner.form().equals("canonical")) assertSame(registry.find(owner.id()).orElseThrow(), owner.tracer());
        }
    }

    @Test
    void relationListsAreDefensivelyCopiedImmutableAndRetainedByBudgetCopies() {
        InputSpec plain = synthetic();
        assertFalse(wire(plain).has("equalLengthGroups"), "Empty metadata must not rewrite every existing spec");
        assertEquals(List.of(), groups(plain));
        String[] caller = {"a", "b"};
        InputSpec paired = pair(plain, caller);
        caller[0] = "c";
        assertEquals(List.of(List.of("a", "b")), groups(paired));
        assertEquals(List.of(), groups(plain), "Fluent builders leave the original spec unchanged");
        var snapshot = groups(paired);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(List.of("c", "d")));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.get(0).set(0, "c"));
        assertEquals(snapshot, groups(paired.withMaxSteps(3)));
        assertEquals(snapshot, groups(paired.withMaxBytes(99)));
        assertEquals(List.of(List.of("a", "b"), List.of("c", "d")), groups(pair(paired, "c", "d")));
    }

    @Test
    void malformedDeclarationsAndImpossibleLengthIntersectionsFailAtConstruction() {
        InputSpec plain = synthetic();
        assertThrows(IllegalArgumentException.class, () -> pair(plain));
        assertThrows(IllegalArgumentException.class, () -> pair(plain, "a"));
        assertThrows(IllegalArgumentException.class, () -> pair(plain, "a", "a"));
        assertThrows(IllegalArgumentException.class, () -> pair(plain, "a", "missing"));
        assertThrows(IllegalArgumentException.class, () -> pair(plain, "a", "scalar"));
        assertThrows(IllegalArgumentException.class, () -> pair(plain, "a", ""));
        assertThrows(IllegalArgumentException.class, () -> pair(plain, "a", null));
        assertThrows(IllegalArgumentException.class, () -> pair(plain, (String[]) null));
        assertThrows(IllegalArgumentException.class, () -> pair(pair(plain, "a", "b"), "b", "c"));
        InputSpec impossible = InputSpec.of(array("a", 1, 2), array("b", 3, 4));
        assertThrows(IllegalArgumentException.class, () -> pair(impossible, "a", "b"));
        InputSpec ambiguous = InputSpec.of(array("a", 0, 3), array("a", 0, 3), array("b", 0, 3));
        assertThrows(IllegalArgumentException.class, () -> pair(ambiguous, "a", "b"));
    }

    @Test
    void normalizedListsAndDefaultsAreComparedWithoutChangingTheCallerMap() {
        var supplied = new LinkedHashMap<String, Object>();
        supplied.put("a", List.of(3L, 1L));
        supplied.put("b", List.of(8L, 2L));
        var before = new LinkedHashMap<>(supplied);
        InputSpec spec = pair(InputSpec.of(array("a", 0, 4), array("b", 0, 4)), "a", "b");
        Inputs normalized = InputValidator.validate(spec, supplied);
        assertEquals(List.of(3, 1), normalized.asMap().get("a"));
        assertEquals(List.of(8, 2), normalized.asMap().get("b"));
        assertEquals(before, supplied);
        assertEquals(Map.of("a", List.of(1), "b", List.of(1)), InputValidator.defaults(spec).asMap());
        var mismatch = assertThrows(InputValidationException.class,
                () -> InputValidator.validate(spec, Map.of("a", List.of(1, 2))));
        assertTrue(mismatch.getFieldErrors().containsKey("b"), "An omitted b is its real one-item default, never resized");
        InputSpec empty = pair(InputSpec.of(array("a", 0, 0), array("b", 0, 0)), "a", "b");
        assertEquals(Map.of("a", List.of(), "b", List.of()),
                InputValidator.validate(empty, Map.of("a", List.of(), "b", List.of())).asMap());
        InputSpec unbounded = pair(InputSpec.of(InputField.of("a", FieldType.INT_ARRAY).build(),
                InputField.of("b", FieldType.INT_ARRAY).length(0, 0).build()), "a", "b");
        assertDoesNotThrow(() -> InputValidator.validate(unbounded, Map.of("a", List.of(), "b", List.of())));
    }

    @Test
    void rawLengthMetadataCannotBeNarrowedOrIgnoredBeforeDeclaringAGroup() {
        for (String key : List.of("minLength", "maxLength")) {
            for (Object bad : List.of("3", 1.5, 4294967296L, Double.NaN, Double.POSITIVE_INFINITY,
                    new java.math.BigDecimal("3.00000000000000000001"))) {
                InputSpec malformed = InputSpec.of(InputField.of("a", FieldType.INT_ARRAY)
                        .constraint(key, bad).build(), array("b", 0, 8));
                assertThrows(IllegalArgumentException.class, () -> pair(malformed, "a", "b"),
                        key + "=" + bad + " must not change meaning between Java and JSON");
            }
        }
        for (Object whole : List.of(3L, 3.0)) {
            InputSpec paired = pair(InputSpec.of(InputField.of("a", FieldType.INT_ARRAY)
                    .constraint("maxLength", whole).build(), array("b", 0, 8)), "a", "b");
            assertDoesNotThrow(() -> InputValidator.validate(paired,
                    Map.of("a", List.of(1,2,3), "b", List.of(1,2,3))));
            assertThrows(InputValidationException.class, () -> InputValidator.validate(paired,
                    Map.of("a", List.of(1,2,3,4), "b", List.of(1,2,3,4))));
        }
    }

    @Test
    void fieldErrorsUnknownKeysAndOutOfRangeNumbersRemainAuthoritative() {
        InputSpec spec = pair(InputSpec.of(array("a", 0, 4), array("b", 0, 4)), "a", "b");
        for (Object bad : List.of("1,2", 2, List.of(4294967296L), List.of(-1))) {
            var failure = assertThrows(InputValidationException.class,
                    () -> InputValidator.validate(spec, Map.of("a", bad, "b", List.of(1))));
            assertTrue(failure.getFieldErrors().containsKey("a"));
        }
        var nullValue = new HashMap<String, Object>();
        nullValue.put("a", null); nullValue.put("b", List.of(1));
        assertTrue(assertThrows(InputValidationException.class, () -> InputValidator.validate(spec, nullValue))
                .getFieldErrors().containsKey("a"));
        assertTrue(assertThrows(InputValidationException.class,
                () -> InputValidator.validate(spec, Map.of("a", List.of(1), "b", List.of(1), "invented", 3)))
                .getFieldErrors().containsKey("invented"));
    }

    @Test
    void actualOwnerSharedValidationRefusesBothMismatchDirectionsBeforeTheTracerRuns() {
        for (var owner : owners()) for (boolean reverse : List.of(false, true)) {
            var input = new LinkedHashMap<>(InputValidator.defaults(owner.tracer().inputSpec()).asMap());
            input.put(owner.group().get(0), reverse ? List.of(1) : List.of(1, 2));
            input.put(owner.group().get(1), reverse ? List.of(1, 2) : List.of(1));
            var before = new LinkedHashMap<>(input);
            // This specifically exercises the trust boundary, not an old run()-local guard.
            var error = assertThrows(InputValidationException.class,
                    () -> InputValidator.validate(owner.tracer().inputSpec(), input), owner.toString());
            assertTrue(error.getFieldErrors().containsKey(owner.group().get(1)), owner.toString());
            assertEquals(before, input, "Reject instead of truncating or padding the draft");
        }
    }

    @Test
    void catalogDetailInputSpecAndNestedApproachesServeTheActualRelation() throws Exception {
        JsonNode summaries = json.readTree(request(get("/api/problems"), 200));
        for (String id : OWNERS) {
            JsonNode summary = null;
            for (JsonNode candidate : summaries) if (candidate.path("id").asText().equals(id)) summary = candidate;
            assertNotNull(summary, id);
            assertEquals(json.valueToTree(List.of(group(id))), summary.path("inputSpec").path("equalLengthGroups"));
        }
        for (var owner : owners()) {
            var detail = json.readTree(request(get("/api/problems/" + owner.id()).param("approach", owner.form()), 200));
            var spec = json.readTree(request(get("/api/problems/" + owner.id() + "/input-spec")
                    .param("approach", owner.form()), 200));
            assertEquals(json.valueToTree(List.of(owner.group())), spec.path("equalLengthGroups"));
            assertEquals(spec, detail.path("inputSpec"));
            JsonNode nested = null;
            for (JsonNode candidate : detail.path("approaches")) if (candidate.path("id").asText().equals(owner.form())) nested = candidate;
            assertNotNull(nested);
            assertEquals(spec, nested.path("inputSpec"));
        }
    }

    @Test
    void actualHttpRefusalsRemainFieldSpecificAndContainNoPartialStepsInEitherEncoding() throws Exception {
        for (var owner : owners()) for (boolean reverse : List.of(false, true)) {
            var input = new LinkedHashMap<>(InputValidator.defaults(owner.tracer().inputSpec()).asMap());
            input.put(owner.group().get(0), reverse ? List.of(1) : List.of(1, 2));
            input.put(owner.group().get(1), reverse ? List.of(1, 2) : List.of(1));
            for (String encoding : List.of("full", "delta")) {
                JsonNode body = json.readTree(request(post("/api/problems/" + owner.id() + "/execute")
                        .param("approach", owner.form()).param("encoding", encoding).contentType("application/json")
                        .content(json.writeValueAsString(input)), 400));
                assertEquals("invalid_input", body.path("error").asText());
                assertTrue(body.path("fieldErrors").path(owner.group().get(1)).isTextual());
                assertFalse(body.has("steps"));
                assertFalse(body.has("resolvedInput"));
            }
        }
    }

    @Test
    void validPairsKeepTheirIndexAssociationAndExactSubmittedOrderInBothEncodings() throws Exception {
        for (var owner : owners()) for (boolean reverse : List.of(false, true)) {
            boolean fractional = owner.id().equals("fractional-knapsack");
            var input = Map.<String, Object>of("weights", reverse ? List.of(2, 3) : List.of(3, 2),
                    "values", reverse ? List.of(4, 100) : List.of(100, 4), "capacity", fractional ? 3 : 2);
            String expected = fractional ? "100" : "4";
            String answer = fractional ? "totalValue" : "answer";
            Inputs checked = InputValidator.validate(owner.tracer().inputSpec(), input);
            assertEquals(input, checked.asMap());
            var trace = runner.run(owner.tracer(), input);
            assertFalse(trace.isTruncated());
            assertEquals(expected, trace.getSteps().get(trace.getStepCount() - 1).getVariables().get(answer));
            for (String encoding : List.of("full", "delta")) {
                JsonNode body = json.readTree(request(post("/api/problems/" + owner.id() + "/execute")
                        .param("approach", owner.form()).param("encoding", encoding).contentType("application/json")
                        .content(json.writeValueAsString(input)), 200));
                assertFalse(body.path("truncated").asBoolean());
                assertEquals(json.valueToTree(input), body.path("resolvedInput"));
                assertEquals(trace.getStepCount(), body.path("stepCount").asInt());
                assertEquals(expected, body.path("steps").get(trace.getStepCount() - 1).path("variables").path(answer).asText());
            }
        }
    }

    @Test
    void pairingDoesNotRelaxExistingTableTreeOrScalarBounds() {
        for (String id : List.of("knapsack-01", "unbounded-knapsack")) {
            var canonical = approaches.resolve(id, "canonical").tracer();
            var memo = approaches.resolve(id, "memoization").tracer();
            var recursion = approaches.resolve(id, "recursion").tracer();
            var heavy = input(Collections.nCopies(8, 20), Collections.nCopies(8, 100), 30);
            assertDoesNotThrow(() -> InputValidator.validate(canonical.inputSpec(), heavy));
            assertThrows(InputValidationException.class, () -> runner.run(canonical, heavy));
            assertFalse(runner.run(recursion, heavy).isTruncated(), "Cheap full-domain trees remain legal");
            var memoOver = input(Collections.nCopies(8, 1), Collections.nCopies(8, 1), 8);
            assertThrows(InputValidationException.class, () -> runner.run(memo, memoOver));
            var treeOver = input(Collections.nCopies(7, 1), Collections.nCopies(7, 1), 7);
            assertThrows(InputValidationException.class, () -> runner.run(recursion, treeOver));
            assertThrows(InputValidationException.class,
                    () -> InputValidator.validate(canonical.inputSpec(), input(List.of(0), List.of(1), 1)));
            assertThrows(InputValidationException.class,
                    () -> InputValidator.validate(canonical.inputSpec(), input(List.of(1), List.of(1), 31)));
        }
    }

    private record Owner(String id, String form, AlgorithmTracer tracer, List<String> group) {}
    private List<Owner> owners() {
        var result = new ArrayList<Owner>();
        for (String id : OWNERS) for (var approach : approaches.available(id)) {
            result.add(new Owner(id, approach.id(), approach.tracer(), group(id)));
        }
        assertEquals(7, result.size());
        return result;
    }
    private static List<String> group(String id) {
        return id.equals("fractional-knapsack") ? List.of("values", "weights") : List.of("weights", "values");
    }
    private JsonNode wire(InputSpec spec) { return json.valueToTree(spec); }
    private static InputField array(String name, int min, int max) {
        return InputField.of(name, FieldType.INT_ARRAY).length(min, max).values(0, 20)
                .defaultValue(max == 0 ? List.of() : List.of(1)).build();
    }
    private static InputSpec synthetic() {
        return InputSpec.of(array("a", 0, 4), array("b", 0, 4), array("c", 0, 4), array("d", 0, 4),
                InputField.of("scalar", FieldType.INT).range(0, 4).defaultValue(1).build());
    }
    private static InputSpec pair(InputSpec spec, String... fields) {
        return spec.withEqualLengthGroup(fields);
    }
    private static List<List<String>> groups(InputSpec spec) {
        return spec.getEqualLengthGroups();
    }
    private byte[] request(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String client = "equal-length-contract-" + CLIENTS.incrementAndGet();
        var result = http.perform(request.with(r -> { r.setRemoteAddr(client); return r; })).andReturn();
        assertEquals(client, result.getRequest().getRemoteAddr());
        assertEquals(expectedStatus, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return result.getResponse().getContentAsByteArray();
    }
    private static Map<String, Object> input(List<Integer> weights, List<Integer> values, int capacity) {
        return Map.of("weights", weights, "values", values, "capacity", capacity);
    }
}
