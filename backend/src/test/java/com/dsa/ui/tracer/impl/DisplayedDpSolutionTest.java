package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import com.dsa.ui.catalog.StatementCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Compiles and executes the exact source the learner receives, without app helpers. */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DisplayedDpSolutionTest {
    @Autowired TracerRegistry registry;
    @Autowired TraceRunner runner;
    @Autowired StatementCatalog statements;
    @Autowired MockMvc http;
    @TempDir Path temporary;
    private final ObjectMapper json = new ObjectMapper();

    Stream<AlgorithmTracer> repairedFamily() {
        return registry.all().stream().filter(t -> t instanceof RemainingDpTracer);
    }

    @Test
    void completeSourcesHaveExactlyOneOwningTracer() throws Exception {
        Set<String> files = Arrays.stream(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:solutions/dp/*.java"))
                .map(r -> r.getFilename().replaceFirst("\\.java$", "")).collect(Collectors.toSet());
        assertFalse(files.isEmpty());
        assertEquals(repairedFamily().map(AlgorithmTracer::id).collect(Collectors.toSet()), files);
    }

    @ParameterizedTest(name = "HTTP serves matching source: {0}")
    @MethodSource("repairedFamily")
    void detailAndBothTraceEncodingsServeTheCompleteSource(AlgorithmTracer tracer) throws Exception {
        String source = AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
        var detail = http.perform(get("/api/problems/{id}", tracer.id())).andExpect(status().isOk()).andReturn();
        assertEquals(source, json.readTree(detail.getResponse().getContentAsByteArray()).path("javaCode").asText());
        for (String encoding : List.of("delta", "full")) {
            var response = http.perform(get("/api/problems/{id}/execute", tracer.id()).param("encoding", encoding)
                    .with(request -> { request.setRemoteAddr("source-contract-" + tracer.id()); return request; }))
                    .andExpect(status().isOk()).andReturn();
            var body = json.readTree(response.getResponse().getContentAsByteArray());
            assertEquals(source, body.path("code").asText());
            assertEquals(encoding, body.path("encoding").asText());
            assertEquals(json.valueToTree(AnnotatedCode.parse(tracer.annotatedCode()).getAnchors()), body.path("anchors"));
            assertFalse(body.path("code").asText().contains("// @a"));
        }
    }

    @Test
    void noRegisteredTracerServesTheKnownPlaceholderSketch() {
        List<String> broken = registry.all().stream().filter(t -> {
            String code = t.annotatedCode();
            return Stream.of("initialiseBaseCases(", "evaluateTransitionCandidates(",
                    "extractAnswer(", "reconstructChosenSolution(").anyMatch(code::contains);
        }).map(AlgorithmTracer::id).toList();
        assertEquals(List.of(), broken, "Learners must receive complete source, not a sketch");
    }

    @Test
    void distinctExecutionPhasesHighlightTheirOwnStatements() {
        ExecutionTrace stock = runner.runDefaults(registry.find("stock-transaction-fee").orElseThrow());
        var stockLines = stock.getCode().lines().toList();
        for (var step : stock.getSteps().subList(1, stock.getStepCount() - 1)) {
            String line = stockLines.get(step.getActiveLine() - 1);
            if ("buy".equals(step.getVariables().get("state"))) {
                assertTrue(line.contains("dp[day][0] ="), line);
            } else {
                assertTrue(line.contains("dp[day][1] =") && line.contains("- fee"), line);
            }
        }
        ExecutionTrace bitonic = runner.runDefaults(registry.find("longest-bitonic-subsequence").orElseThrow());
        for (var step : bitonic.getSteps()) {
            String line = bitonic.getCode().lines().toList().get(step.getActiveLine() - 1);
            if (step.getDescription().startsWith("Forward pair")) assertTrue(line.contains("increasing[i] ="), line);
            if (step.getDescription().startsWith("Reverse pair")) assertTrue(line.contains("decreasing[i] ="), line);
        }
        ExecutionTrace palindrome = runner.runDefaults(registry.find("palindrome-partitioning-2").orElseThrow());
        for (var step : palindrome.getSteps()) {
            String line = palindrome.getCode().lines().toList().get(step.getActiveLine() - 1);
            if (step.getDescription().startsWith("Substring")) assertTrue(line.contains("palindrome[i][j] ="), line);
            if (step.getDescription().startsWith("Minimum cuts for prefix")) assertEquals("cuts[end] = best;", line.trim());
        }
    }

    @ParameterizedTest(name = "displayed source compiles and runs: {0}")
    @MethodSource("repairedFamily")
    void displayedSourceCompilesAndAgreesWithExecution(AlgorithmTracer tracer) throws Exception {
        String source = AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
        Path directory = Files.createDirectory(temporary.resolve(tracer.id()));
        Path file = directory.resolve("Solution.java");
        Files.writeString(file, source, StandardCharsets.UTF_8);
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Run this contract with JDK 17, not a JRE");
        var diagnostics = new ByteArrayOutputStream();
        // An empty classpath proves the displayed code needs no tracer/application classes.
        assertEquals(0, compiler.run(null, null, diagnostics, "--release", "17", "-parameters",
                "-classpath", directory.toString(), "-d", directory.toString(), file.toString()),
                () -> tracer.id() + " displayed source is incomplete:\n" + diagnostics);
        try (var loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Class<?> solution = loader.loadClass("Solution");
            List<Method> entries = Arrays.stream(solution.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()) && m.getName().equals("solve")).toList();
            assertEquals(1, entries.size(), "One typed public solve entry; helper methods stay private");
            Method entry = entries.get(0);
            assertNotEquals(Object.class, entry.getReturnType(), "Declare the actual result type");
            assertEquals(tracer.inputSpec().getFields().stream().map(InputField::getName).collect(Collectors.toSet()),
                    Arrays.stream(entry.getParameters()).map(java.lang.reflect.Parameter::getName).collect(Collectors.toSet()));
            List<Map<String, Object>> inputs = new ArrayList<>(List.of(Map.of(), tracer.alternateInput()));
            var examples = statements.find(tracer.id()).orElseThrow().examples();
            examples.forEach(example -> inputs.add(example.input()));
            for (Map<String, Object> input : inputs) {
                ExecutionTrace trace = runner.run(tracer, input);
                assertFalse(trace.isTruncated(), tracer.id() + " input " + input);
                assertEquals(source, trace.getCode());
                Object[] args = Arrays.stream(entry.getParameters()).map(p -> {
                    assertTrue(trace.getResolvedInput().containsKey(p.getName()),
                            "Source parameter must name an input field: " + p.getName());
                    return json.convertValue(trace.getResolvedInput().get(p.getName()), p.getType());
                }).toArray();
                Object result = entry.invoke(solution.getConstructor().newInstance(), args);
                Map<String, String> variables = trace.getSteps().get(trace.getStepCount() - 1).getVariables();
                if (result instanceof Collection<?> subset) {
                    assertEquals(variables.get("subset"), subset.toString(), tracer.id() + " reconstructed subset");
                    assertEquals(variables.get("answer"), String.valueOf(subset.size()));
                } else {
                    assertEquals(variables.get("answer"), String.valueOf(result), tracer.id() + " input " + input);
                }
                for (var example : examples) {
                    if (example.input().equals(input)) {
                        assertEquals(example.output(), String.valueOf(result),
                                tracer.id() + " must independently agree with the published example");
                    }
                }
            }
        }
    }
}
