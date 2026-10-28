package com.dsa.ui.tracer.impl;

import com.dsa.ui.approach.SolutionApproachRegistry;
import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.catalog.StatementCatalog;
import com.dsa.ui.model.DsType;
import com.dsa.ui.model.ExecutionStep;
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
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Independent Cartesian products distinguish complete output from a plausible count. */
@SpringBootTest
@AutoConfigureMockMvc
class PhoneCombinationsContractTest {
    private static final String ID = "letter-combinations-phone";
    private static final String ENDPOINT = "/api/problems/" + ID;
    private static final Map<Character, String> LETTERS = Map.of(
            '2', "abc", '3', "def", '4', "ghi", '5', "jkl",
            '6', "mno", '7', "pqrs", '8', "tuv", '9', "wxyz");
    private static final AtomicInteger CLIENTS = new AtomicInteger();
    private final ObjectMapper json = new ObjectMapper();

    @Autowired LetterCombinationsPhoneTracer tracer;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @Autowired ProblemCatalog catalog;
    @Autowired SolutionApproachRegistry approaches;
    @Autowired StatementCatalog statements;
    @TempDir Path temporary;

    @Test
    void actualListsAndCaptureOrderMatchTheCartesianProductForDefaultsAlternateAndSeededInputs() {
        var probes = new ArrayList<>(List.of("23", "2", "7", "9", "79", "98", "23456", "77777"));
        probes.addAll(randomDigits(80, 71001));
        for (String digits : probes) checkTrace(digits);
        assertEquals(List.of("ad", "ae", "af", "bd", "be", "bf", "cd", "ce", "cf"), oracle("23"));
        assertEquals(oracle("23").toString(), last(runner.runDefaults(tracer)).getVariables().get("results"));
        assertEquals(oracle("2").toString(), last(runner.run(tracer, tracer.alternateInput())).getVariables().get("results"));
    }

    @Test
    void branchShapesAndSafeSixDigitInputsRemainCompleteWithoutSilentlyReducingTheLengthCap() {
        assertEquals(22, checkTrace("23").getStepCount());
        assertEquals(2389, checkTrace("77777").getStepCount());
        assertEquals(1822, checkTrace("222222").getStepCount());
        assertEquals(2308, checkTrace("222227").getStepCount());
        assertEquals(972, oracle("222227").size());
        assertEquals(972, oracle("722222").size(), "Same leaf count has a different intermediate-tree cost");
    }

    @Test
    void exactDisplayedJavaCompilesWithOnlyJdkImportsAndReturnsActualListsBeyondTheVisualizationBudget() throws Exception {
        String source = AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
        // Supply ordinary JDK imports and, for a method listing, an outer class only.
        // In particular, never inject KEYPAD or a missing recursion helper.
        String complete = source.contains("class Solution") ? source
                : "public class Solution {\n" + source + "\n}";
        complete = "import java.util.*;\n" + complete;
        Path directory = Files.createTempDirectory(temporary, "phone-source");
        Path file = directory.resolve("Solution.java");
        Files.writeString(file, complete);
        var errors = new ByteArrayOutputStream();
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, errors,
                "--release", "17", "-classpath", directory.toString(), "-d", directory.toString(), file.toString()), errors::toString);
        try (var loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            var type = loader.loadClass("Solution");
            var solve = type.getMethod("letterCombinations", String.class);
            Object instance = type.getConstructor().newInstance();
            var probes = new ArrayList<>(List.of("23", "2", "79", "77777", "222222", "222227", "722222", "777777"));
            probes.addAll(randomDigits(160, 71002));
            probes.addAll(sixDigitShapes());
            for (String digits : probes) {
                Object actual = solve.invoke(instance, digits);
                assertInstanceOf(List.class, actual, digits);
                assertEquals(oracle(digits), actual, digits);
                assertEquals(oracle(digits), solve.invoke(instance, digits), "Repeated invocation must not retain results: " + digits);
            }
        }
    }

    @Test
    void sixDigitPreflightUsesTheActualTreeCostAndRefusesBeforeEmittingAnyStep() throws Exception {
        var constructor = StepEmitter.class.getDeclaredConstructor(AnnotatedCode.class, int.class, long.class, DsType.class);
        constructor.setAccessible(true);
        int accepted = 0;
        for (String digits : sixDigitShapes()) {
            Inputs in = InputValidator.validate(tracer.inputSpec(), input(digits));
            var emitter = constructor.newInstance(AnnotatedCode.parse(tracer.annotatedCode()),
                    InputSpec.DEFAULT_MAX_STEPS, InputSpec.DEFAULT_MAX_BYTES, tracer.dsType());
            if (safeShape(digits)) {
                tracer.run(in, emitter);
                assertEquals(expectedSteps(digits), emitter.count(), digits);
                accepted++;
            } else {
                var failure = assertThrows(InputValidationException.class, () -> tracer.run(in, emitter), digits);
                assertTrue(failure.getFieldErrors().containsKey("digits"), digits);
                assertEquals(0, emitter.count(), "Refusal must precede the first partial trace: " + digits);
            }
        }
        assertEquals(2, accepted);
    }

    @Test
    void originalFieldDomainAndNewGeneratorHintsAreServedWithoutShrinkingTheLegalLength() throws Exception {
        var spec = tracer.inputSpec();
        var field = spec.field("digits");
        assertEquals(DsType.RECURSION_TREE, tracer.dsType());
        assertEquals(FieldType.STRING, field.getType());
        assertEquals(1, spec.getFields().size());
        assertEquals(Map.of("minLength", 1, "maxLength", 6, "pattern", "[2-9]+",
                "patternHint", "Digits 2-9 only.", "alphabet", "23456789", "randomMaxLength", 5), field.getConstraints());
        assertEquals(input("23"), InputValidator.defaults(spec).asMap());
        assertEquals(input("2"), tracer.alternateInput());
        assertEquals(InputSpec.DEFAULT_MAX_STEPS, spec.getMaxSteps());
        assertEquals(InputSpec.DEFAULT_MAX_BYTES, spec.getMaxBytes());
        JsonNode detail = json.readTree(request(get(ENDPOINT), 200));
        assertEquals(json.valueToTree(field.getConstraints()), detail.path("inputSpec").path("fields").get(0).path("constraints"));
        for (Object invalid : List.of("", "1", "0", "20", "2a", " 23", "23 ", "2222222", "２３", 23, List.of("2"))) {
            assertRefused(Map.of("digits", invalid));
        }
        var unknown = new HashMap<String, Object>(input("23"));
        unknown.put("invented", true);
        assertThrows(InputValidationException.class, () -> runner.run(tracer, unknown));
        JsonNode body = json.readTree(request(post(ENDPOINT + "/execute").contentType("application/json")
                .content(json.writeValueAsString(unknown)), 400));
        assertFalse(body.has("steps"));
    }

    @Test
    void bothHttpEncodingsReturnCompleteActualListsForAcceptedInputsAndFieldErrorsForUnsafeShapes() throws Exception {
        for (String digits : sixDigitShapes()) {
            for (String encoding : List.of("full", "delta")) {
                JsonNode body = json.readTree(request(post(ENDPOINT + "/execute").param("encoding", encoding)
                        .contentType("application/json").content(json.writeValueAsString(input(digits))), safeShape(digits) ? 200 : 400));
                if (safeShape(digits)) {
                    assertFalse(body.path("truncated").asBoolean());
                    assertEquals(expectedSteps(digits), body.path("stepCount").asInt());
                    assertEquals(oracle(digits).toString(), body.path("steps").get(body.path("stepCount").asInt() - 1)
                            .path("variables").path("results").asText());
                    assertEquals(json.valueToTree(input(digits)), body.path("resolvedInput"));
                } else {
                    assertEquals("invalid_input", body.path("error").asText());
                    assertTrue(body.path("fieldErrors").path("digits").isTextual());
                    assertFalse(body.has("steps"), digits);
                }
            }
        }
    }

    @Test
    void largestAcceptedShapesStayWithinExistingCapsAndFullAndDeltaDecodeIdentically() throws Exception {
        for (String digits : List.of("77777", "222227")) {
            byte[] fullBytes = request(post(ENDPOINT + "/execute").param("encoding", "full")
                    .contentType("application/json").content(json.writeValueAsString(input(digits))), 200);
            byte[] deltaBytes = request(post(ENDPOINT + "/execute").param("encoding", "delta")
                    .contentType("application/json").content(json.writeValueAsString(input(digits))), 200);
            assertTrue(fullBytes.length < InputSpec.DEFAULT_MAX_BYTES, digits + " full bytes=" + fullBytes.length);
            assertTrue(deltaBytes.length < InputSpec.DEFAULT_MAX_BYTES, digits + " delta bytes=" + deltaBytes.length);
            JsonNode full = json.readTree(fullBytes), delta = json.readTree(deltaBytes);
            for (JsonNode body : List.of(full, delta)) {
                assertEquals(InputSpec.DEFAULT_MAX_STEPS, body.path("maxSteps").asInt());
                assertEquals(InputSpec.DEFAULT_MAX_BYTES, body.path("maxBytes").asLong());
                assertFalse(body.path("truncated").asBoolean());
                assertEquals(expectedSteps(digits), body.path("stepCount").asInt());
            }
            assertEquals(full.path("steps"), decode(delta.path("steps"), full.path("steps")));
            var done = full.path("steps").get(expectedSteps(digits) - 1);
            assertEquals(oracle(digits).toString(), done.path("variables").path("results").asText());
            assertEquals(oracle(digits).size(), done.path("variables").path("combinations").asInt());
            assertEquals(json.createArrayNode(), done.path("callStack"));
        }
    }

    @Test
    void ownerComplexityAndSourceAgreeAcrossCatalogDetailApproachesDefaultAndCustomExecution() throws Exception {
        JsonNode detail = json.readTree(request(get(ENDPOINT), 200));
        JsonNode complexity = detail.path("complexity");
        assertEquals("O(N * P)", complexity.path("timeComplexity").asText());
        assertEquals("O(N)", complexity.path("spaceComplexity").asText());
        String time = complexity.path("timeExplanation").asText();
        assertTrue(time.contains("N"));
        assertTrue(time.contains("P"));
        String space = complexity.path("spaceExplanation").asText().toLowerCase(Locale.ROOT);
        assertTrue(space.contains("output"));
        assertTrue(space.replace(" ", "").contains("o(n*p)"), space);
        assertEquals(complexity, json.valueToTree(catalog.find(ID).orElseThrow().getProblem().getComplexity()));
        assertSame(tracer, approaches.resolve(ID, null).tracer());
        assertEquals(complexity, json.valueToTree(approaches.resolve(ID, null).complexity()));
        assertEquals(complexity, detail.path("approaches").get(0).path("complexity"));
        var code = AnnotatedCode.parse(tracer.annotatedCode());
        assertEquals(code.getDisplayCode(), detail.path("javaCode").asText());
        for (String encoding : List.of("full", "delta")) for (boolean defaults : List.of(true, false)) {
            var request = defaults ? get(ENDPOINT + "/execute").param("encoding", encoding)
                    : post(ENDPOINT + "/execute").param("encoding", encoding).contentType("application/json")
                    .content(json.writeValueAsString(input("79")));
            JsonNode body = json.readTree(request(request, 200));
            assertEquals(complexity, body.path("complexity"));
            assertEquals(detail.path("javaCode"), body.path("code"));
            assertEquals(json.valueToTree(code.getAnchors()), body.path("anchors"));
            assertEquals("canonical", body.path("approachId").asText());
            assertEquals(ID, body.path("problemId").asText());
            assertEquals("RecursionTree", body.path("dsType").asText());
            assertFalse(body.path("truncated").asBoolean());
            var done = body.path("steps").get(body.path("stepCount").asInt() - 1);
            assertEquals(oracle(defaults ? "23" : "79").toString(), done.path("variables").path("results").asText());
        }
    }

    @Test
    void authoredAndServedExamplesGiveActualListsInsteadOfOnlyTheirCounts() throws Exception {
        var statement = statements.find(ID).orElseThrow();
        assertFalse(statement.examples().isEmpty());
        for (var example : statement.examples()) {
            assertEquals("results", example.answerVariable());
            String digits = (String) example.input().get("digits");
            assertEquals(oracle(digits).toString(), example.output());
            assertEquals(example.output(), last(checkTrace(digits)).getVariables().get("results"));
        }
        JsonNode detail = json.readTree(request(get(ENDPOINT), 200));
        assertEquals(statement.examples().size(), detail.path("examples").size());
        for (int i = 0; i < statement.examples().size(); i++) {
            assertEquals(statement.examples().get(i).output(), detail.path("examples").get(i).path("output").asText());
        }
    }

    @Test
    void aTinyEmitterBudgetTruncatesWithoutPublishingResultsOrACompletedAnchor() {
        AlgorithmTracer limited = new AlgorithmTracer() {
            public String id() { return tracer.id(); }
            public DsType dsType() { return tracer.dsType(); }
            public InputSpec inputSpec() {
                return InputSpec.of(tracer.inputSpec().getFields().toArray(InputField[]::new)).withMaxSteps(3);
            }
            public Map<String, Object> alternateInput() { return tracer.alternateInput(); }
            public String annotatedCode() { return tracer.annotatedCode(); }
            public void run(Inputs in, StepEmitter emit) { tracer.run(in, emit); }
        };
        var trace = runner.runDefaults(limited);
        assertTrue(trace.isTruncated());
        assertEquals(3, trace.getStepCount());
        int done = AnnotatedCode.parse(tracer.annotatedCode()).resolve("done");
        for (var step : trace.getSteps()) {
            assertFalse(step.getVariables().containsKey("results"));
            assertFalse(step.getVariables().containsKey("combinations"));
            assertNotEquals(done, step.getActiveLine());
        }
    }

    private ExecutionTrace checkTrace(String digits) {
        var trace = runner.run(tracer, input(digits));
        assertFalse(trace.isTruncated(), digits);
        assertEquals(input(digits), trace.getResolvedInput());
        assertEquals(expectedSteps(digits), trace.getStepCount(), digits);
        var code = AnnotatedCode.parse(tracer.annotatedCode());
        List<String> expected = oracle(digits), captures = new ArrayList<>();
        for (var step : trace.getSteps()) {
            var vars = step.getVariables();
            assertNotNull(step.getCallStack());
            if (step.getActiveLine() == code.resolve("capture")) {
                String actual = vars.get("combination");
                assertTrue(captures.size() < expected.size(), digits);
                assertEquals(expected.get(captures.size()), actual, digits);
                captures.add(actual);
                assertEquals(String.valueOf(captures.size()), vars.get("count"));
                assertEquals(actual, chars(step));
                assertEquals(digits.length() + 1, step.getCallStack().size());
            } else if (step.getActiveLine() == code.resolve("choose")) {
                String prefix = chars(step);
                assertFalse(prefix.isEmpty());
                assertTrue(prefix.length() <= digits.length());
                for (int i = 0; i < prefix.length(); i++) {
                    assertTrue(LETTERS.get(digits.charAt(i)).indexOf(prefix.charAt(i)) >= 0, digits + " prefix " + prefix);
                }
                assertEquals(String.valueOf(digits.charAt(prefix.length() - 1)), vars.get("digit"));
                assertEquals(prefix.substring(prefix.length() - 1), vars.get("letter"));
                assertEquals(prefix.length(), step.getCallStack().size());
            } else {
                assertEquals(code.resolve("done"), step.getActiveLine());
                assertSame(last(trace), step);
                assertTrue(step.getCallStack().isEmpty());
            }
            if (step != last(trace)) assertFalse(vars.containsKey("results"));
        }
        assertEquals(expected, captures, digits);
        var done = last(trace);
        assertEquals(code.resolve("done"), done.getActiveLine());
        assertEquals(String.valueOf(expected.size()), done.getVariables().get("combinations"));
        assertEquals(expected.toString(), done.getVariables().get("results"));
        assertTrue(done.getCallStack().isEmpty());
        return trace;
    }

    private static String chars(ExecutionStep step) {
        assertNotNull(step.getArrayState());
        return step.getArrayState().stream().map(element -> {
            assertNotNull(element.getLabel());
            return element.getLabel();
        }).collect(Collectors.joining());
    }

    /** Iterative Cartesian expansion, independent of the recursive displayed algorithm. */
    private static List<String> oracle(String digits) {
        List<String> prefixes = List.of("");
        for (char digit : digits.toCharArray()) {
            var next = new ArrayList<String>();
            for (String prefix : prefixes) for (char letter : LETTERS.get(digit).toCharArray()) next.add(prefix + letter);
            prefixes = next;
        }
        return prefixes;
    }
    private static int expectedSteps(String digits) {
        int width = 1, edges = 0;
        for (char digit : digits.toCharArray()) { width *= LETTERS.get(digit).length(); edges += width; }
        return edges + width + 1;
    }
    private static List<String> sixDigitShapes() {
        var result = new ArrayList<String>();
        for (int mask = 0; mask < 64; mask++) {
            var digits = new StringBuilder();
            for (int i = 0; i < 6; i++) digits.append((mask & (1 << i)) == 0 ? '2' : '7');
            result.add(digits.toString());
        }
        return result;
    }
    private static boolean safeShape(String digits) { return digits.equals("222222") || digits.equals("222227"); }
    private static List<String> randomDigits(int count, long seed) {
        var random = new Random(seed);
        var result = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            var digits = new StringBuilder();
            int length = 1 + random.nextInt(5);
            for (int j = 0; j < length; j++) digits.append((char) ('2' + random.nextInt(8)));
            result.add(digits.toString());
        }
        return result;
    }
    private static Map<String, Object> input(String digits) { return Map.of("digits", digits); }
    private static ExecutionStep last(ExecutionTrace trace) { return trace.getSteps().get(trace.getStepCount() - 1); }
    private byte[] request(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String client = "phone-contract-" + CLIENTS.incrementAndGet();
        var result = http.perform(request.with(r -> { r.setRemoteAddr(client); return r; })).andReturn();
        assertEquals(client, result.getRequest().getRemoteAddr());
        assertEquals(expectedStatus, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return result.getResponse().getContentAsByteArray();
    }
    private void assertRefused(Map<String, Object> input) throws Exception {
        var failure = assertThrows(InputValidationException.class, () -> runner.run(tracer, input));
        assertTrue(failure.getFieldErrors().containsKey("digits"));
        for (String encoding : List.of("full", "delta")) {
            JsonNode body = json.readTree(request(post(ENDPOINT + "/execute").param("encoding", encoding)
                    .contentType("application/json").content(json.writeValueAsString(input)), 400));
            assertEquals("invalid_input", body.path("error").asText());
            assertTrue(body.path("fieldErrors").path("digits").isTextual());
            assertFalse(body.has("steps"));
        }
    }
    private JsonNode decode(JsonNode deltas, JsonNode full) {
        var result = json.createArrayNode();
        ObjectNode snapshot = json.createObjectNode();
        var fields = new HashSet<String>();
        for (var step : full) step.fieldNames().forEachRemaining(fields::add);
        for (var delta : deltas) {
            if (delta.path("keyframe").asBoolean()) {
                snapshot = json.createObjectNode();
                for (String field : fields) snapshot.set(field, NullNode.instance);
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
}
