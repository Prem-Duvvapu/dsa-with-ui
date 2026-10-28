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
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Owner metadata is checked where it reaches clients, alongside independent answers. */
@SpringBootTest
@AutoConfigureMockMvc
class CountSquaresMetadataTest {
    private static final String ID = "count-square-submatrices";
    private static final String ENDPOINT = "/api/problems/" + ID;
    private static final AtomicInteger CLIENTS = new AtomicInteger();
    private final ObjectMapper json = new ObjectMapper();
    @Autowired CountSquareSubmatricesTracer tracer;
    @Autowired TraceRunner runner;
    @Autowired ProblemCatalog catalog;
    @Autowired SolutionApproachRegistry approaches;
    @Autowired StatementCatalog statements;
    @Autowired MockMvc http;
    @TempDir Path temporary;

    @Test
    void catalogDetailAndCanonicalApproachDescribeTheActualInPlaceAlgorithm() throws Exception {
        JsonNode detail = json.readTree(request(get(ENDPOINT), 200));
        JsonNode complexity = detail.path("complexity");
        assertOwnerComplexity(complexity);
        assertEquals(complexity, json.valueToTree(catalog.find(ID).orElseThrow().getProblem().getComplexity()));
        assertSame(tracer, approaches.resolve(ID, null).tracer());
        assertEquals(complexity, json.valueToTree(approaches.resolve(ID, null).complexity()));
        assertEquals(1, detail.path("approaches").size(), "This fix does not add DP variants");
        assertEquals("canonical", detail.path("approaches").get(0).path("id").asText());
        assertEquals(complexity, detail.path("approaches").get(0).path("complexity"));
    }

    @Test
    void defaultCustomAndMaximumExecutionsServeTheOwnerMetadataAndIdenticalFullAndDeltaSteps() throws Exception {
        JsonNode detail = json.readTree(request(get(ENDPOINT), 200));
        var code = AnnotatedCode.parse(tracer.annotatedCode());
        for (var probe : probes()) {
            var full = json.readTree(request(execute(probe, "full"), 200));
            var delta = json.readTree(request(execute(probe, "delta"), 200));
            for (JsonNode body : List.of(full, delta)) {
                assertOwnerComplexity(body.path("complexity"));
                assertEquals(detail.path("complexity"), body.path("complexity"));
                assertEquals(detail.path("javaCode"), body.path("code"));
                assertEquals(json.valueToTree(code.getAnchors()), body.path("anchors"));
                assertEquals(ID, body.path("problemId").asText());
                assertEquals("canonical", body.path("approachId").asText());
                assertEquals("Matrix", body.path("dsType").asText());
                assertFalse(body.path("truncated").asBoolean());
                assertEquals(probe.steps(), body.path("stepCount").asInt());
                assertEquals(json.valueToTree(probe.input()), body.path("resolvedInput"));
                assertEquals(InputSpec.DEFAULT_MAX_STEPS, body.path("maxSteps").asInt());
                assertEquals(InputSpec.DEFAULT_MAX_BYTES, body.path("maxBytes").asLong());
            }
            assertEquals(full.path("steps"), decode(delta.path("steps"), full.path("steps")));
            JsonNode done = full.path("steps").get(probe.steps() - 1);
            assertEquals(String.valueOf(probe.answer()), done.path("variables").path("total").asText());
            assertEquals(code.resolve("done"), done.path("activeLine").asInt());
            assertEquals(probe.answer(), countByEnumeration(grid(probe.input())));
        }
    }

    @Test
    void fieldBoundsDefaultsAlternateAndFieldSpecificRefusalsRemainUnchanged() throws Exception {
        var spec = tracer.inputSpec();
        var field = spec.field("grid");
        assertEquals(1, spec.getFields().size());
        assertEquals(FieldType.INT_GRID, field.getType());
        assertEquals(DsType.MATRIX, tracer.dsType());
        assertEquals(Map.of("maxRows", 8, "maxCols", 8, "minValue", 0, "maxValue", 1), field.getConstraints());
        assertEquals(defaultInput(), InputValidator.defaults(spec).asMap());
        assertEquals(input(new int[][]{{1,0,1},{0,1,0},{1,0,1}}), tracer.alternateInput());
        assertEquals(InputSpec.DEFAULT_MAX_STEPS, spec.getMaxSteps());
        assertEquals(InputSpec.DEFAULT_MAX_BYTES, spec.getMaxBytes());
        JsonNode detail = json.readTree(request(get(ENDPOINT), 200));
        assertEquals(json.readTree(json.writeValueAsBytes(spec)), detail.path("inputSpec"));
        List<Object> invalid = List.of(List.of(), List.of(List.of()),
                Collections.nCopies(9, List.of(1)), List.of(Collections.nCopies(9, 1)),
                List.of(List.of(1), List.of(1,1)), List.of(List.of(-1)), List.of(List.of(2)));
        for (Object grid : invalid) {
            Map<String, Object> bad = Map.of("grid", grid);
            assertTrue(assertThrows(InputValidationException.class, () -> runner.run(tracer, bad))
                    .getFieldErrors().containsKey("grid"));
            for (String encoding : List.of("full", "delta")) {
                JsonNode body = json.readTree(request(post(ENDPOINT + "/execute").param("encoding", encoding)
                        .contentType("application/json").content(json.writeValueAsString(bad)), 400));
                assertEquals("invalid_input", body.path("error").asText());
                assertTrue(body.path("fieldErrors").path("grid").isTextual());
                assertFalse(body.has("steps"));
            }
        }
    }

    @Test
    void displayedJavaStillCompilesAndComputesTheAuthoredAndMaximumAnswers() throws Exception {
        var code = AnnotatedCode.parse(tracer.annotatedCode());
        assertEquals(Set.of("init", "compute", "count", "done"), code.getAnchors().keySet());
        String display = code.getDisplayCode();
        String source = display.contains("class Solution") ? display : "public class Solution {\n" + display + "\n}";
        Path directory = Files.createTempDirectory(temporary, "count-squares-source");
        Path file = directory.resolve("Solution.java");
        Files.writeString(file, source);
        var errors = new ByteArrayOutputStream();
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, errors,
                "--release", "17", "-classpath", directory.toString(), "-d", directory.toString(), file.toString()), errors::toString);
        try (var loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            var type = loader.loadClass("Solution");
            var solve = type.getMethod("countSquares", int[][].class);
            Object solution = type.getConstructor().newInstance();
            for (var probe : probes()) {
                int[][] grid = grid(probe.input());
                assertEquals(probe.answer(), countByEnumeration(grid));
                assertEquals(probe.answer(), solve.invoke(solution, (Object) grid));
                assertEquals(code.getDisplayCode(), runner.run(tracer, probe.input()).getCode());
            }
        }
    }

    @Test
    void authoredExamplesStillMatchIndependentEnumerationAndTheirServedAnswers() throws Exception {
        var statement = statements.find(ID).orElseThrow();
        assertEquals(2, statement.examples().size());
        JsonNode detail = json.readTree(request(get(ENDPOINT), 200));
        assertEquals(statement.examples().size(), detail.path("examples").size());
        for (int i = 0; i < statement.examples().size(); i++) {
            var example = statement.examples().get(i);
            assertEquals("total", example.answerVariable());
            int expected = countByEnumeration(grid(example.input()));
            assertEquals(String.valueOf(expected), example.output());
            JsonNode served = detail.path("examples").get(i);
            assertEquals(json.valueToTree(example.input()), served.path("input"));
            assertEquals(example.output(), served.path("output").asText());
            assertEquals(example.explanation(), served.path("explanation").asText());
            var trace = runner.run(tracer, example.input());
            assertFalse(trace.isTruncated());
            assertEquals(example.output(), trace.getSteps().get(trace.getStepCount() - 1).getVariables().get("total"));
        }
    }

    private void assertOwnerComplexity(JsonNode complexity) {
        assertEquals("O(R * C)", complexity.path("timeComplexity").asText());
        String time = complexity.path("timeExplanation").asText().toLowerCase(Locale.ROOT);
        assertTrue(time.contains("row"), time);
        assertTrue(time.contains("col"), time);
        assertEquals("O(1)", complexity.path("spaceComplexity").asText());
        String space = complexity.path("spaceExplanation").asText().toLowerCase(Locale.ROOT);
        String compact = space.replace(" ", "");
        assertTrue(space.contains("input") && space.contains("conversion"), space);
        assertTrue(compact.contains("o(r*c)"), space);
        assertTrue(space.contains("output") && compact.contains("o(1)"), space);
        assertTrue(space.contains("animation") && space.contains("excluded"), space);
        assertTrue(complexity.path("auxiliarySpace").asText().contains("O(1)"));
    }
    private MockHttpServletRequestBuilder execute(Probe probe, String encoding) throws Exception {
        return probe.defaults() ? get(ENDPOINT + "/execute").param("encoding", encoding)
                : post(ENDPOINT + "/execute").param("encoding", encoding).contentType("application/json")
                .content(json.writeValueAsString(probe.input()));
    }
    private byte[] request(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String client = "count-squares-metadata-" + CLIENTS.incrementAndGet();
        var result = http.perform(request.with(r -> { r.setRemoteAddr(client); return r; })).andReturn();
        assertEquals(client, result.getRequest().getRemoteAddr());
        assertEquals(expectedStatus, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return result.getResponse().getContentAsByteArray();
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
    private record Probe(Map<String, Object> input, int answer, int steps, boolean defaults) {}
    private static List<Probe> probes() {
        int[][] maximum = new int[8][8];
        for (var row : maximum) Arrays.fill(row, 1);
        return List.of(new Probe(defaultInput(), 15, 18, true),
                new Probe(input(new int[][]{{1,1},{1,1}}), 5, 7, false),
                new Probe(input(maximum), 204, 115, false));
    }
    private static Map<String, Object> defaultInput() {
        return input(new int[][]{{0,1,1,1},{1,1,1,1},{0,1,1,1}});
    }
    private static Map<String, Object> input(int[][] grid) {
        return Map.of("grid", Arrays.stream(grid).map(row -> Arrays.stream(row).boxed().toList()).toList());
    }
    private static int[][] grid(Map<String, Object> input) {
        return ((List<?>) input.get("grid")).stream().map(row -> ((List<?>) row).stream()
                .mapToInt(value -> ((Number) value).intValue()).toArray()).toArray(int[][]::new);
    }
    /** Directly inspects every top-left/side square, independently of the DP recurrence. */
    private static int countByEnumeration(int[][] grid) {
        int total = 0;
        for (int r = 0; r < grid.length; r++) for (int c = 0; c < grid[0].length; c++) {
            for (int side = 1; r + side <= grid.length && c + side <= grid[0].length; side++) {
                boolean all = true;
                for (int i = r; i < r + side; i++) for (int j = c; j < c + side; j++) all &= grid[i][j] == 1;
                if (all) total++;
            }
        }
        return total;
    }
}
