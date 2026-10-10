package com.dsa.ui.approach;

import com.dsa.ui.tracer.*;
import com.dsa.ui.model.DsType;
import com.dsa.ui.catalog.StatementCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RequestedDpApproachesTest {
    @Autowired SolutionApproachRegistry approaches;
    @Autowired TracerRegistry canonical;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    @Autowired StatementCatalog statements;
    @TempDir Path temporary;
    static final List<String> PROBLEMS = List.of("longest-increasing-subsequence", "stock-transaction-fee",
            "min-insertions-palindrome", "count-partitions-given-diff", "ninja-and-his-friends");

    @Test void requestedProblemsAdvertiseRealRecursionMemoizationAndTabulationWithoutChangingDefaults() {
        for (String problem : PROBLEMS) {
            var options = approaches.available(problem);
            assertTrue(options.stream().anyMatch(a -> a.id().equals("recursion")), problem + " missing recursion");
            assertTrue(options.stream().anyMatch(a -> a.id().equals("memoization")), problem + " missing memoization");
            assertTrue(options.stream().anyMatch(a -> a.label().startsWith("Tabulation")), problem + " missing tabulation");
            assertSame(canonical.find(problem).orElseThrow(), approaches.resolve(problem, null).tracer());
            assertEquals("canonical", approaches.resolve(problem, null).id());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"longest-increasing-subsequence", "stock-transaction-fee", "min-insertions-palindrome", "count-partitions-given-diff", "ninja-and-his-friends"})
    void everyOfferedFormReturnsIndependentAnswersAndCompleteStacks(String problem) {
        for (var option : approaches.available(problem)) for (var input : cases(problem)) {
            var trace = runner.run(option.tracer(), input);
            assertFalse(trace.isTruncated(), problem + "/" + option.id() + " " + input);
            assertEquals(String.valueOf(oracle(problem, input)), last(trace).get("answer"), problem + "/" + option.id() + " " + input);
            assertTrue(trace.getSteps().get(trace.getStepCount() - 1).getCallStack().isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"longest-increasing-subsequence", "stock-transaction-fee", "min-insertions-palindrome", "count-partitions-given-diff", "ninja-and-his-friends"})
    void authoredExamplesAreCorrectAndOversizedRecursiveExamplesAreRefused(String problem) {
        for (var example : statements.find(problem).orElseThrow().examples()) {
            assertEquals(example.output(), String.valueOf(oracle(problem, example.input())));
            for (var option : approaches.available(problem)) {
                try {
                    InputValidator.validate(option.tracer().inputSpec(), example.input());
                } catch (InputValidationException outsideVisualizer) {
                    assertFalse(outsideVisualizer.getFieldErrors().isEmpty());
                    assertThrows(InputValidationException.class, () -> runner.run(option.tracer(), example.input()));
                    continue;
                }
                var trace = runner.run(option.tracer(), example.input());
                assertFalse(trace.isTruncated());
                assertEquals(example.output(), last(trace).get("answer"), option.id());
                assertEquals(example.input(), trace.getResolvedInput(), "Never shrink a statement example to fit recursion");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"longest-increasing-subsequence", "stock-transaction-fee", "min-insertions-palindrome", "count-partitions-given-diff", "ninja-and-his-friends"})
    void exactDisplayedSourcesCompileInIsolationAndMatchIndependentOracles(String problem) throws Exception {
        for (var option : approaches.available(problem)) {
            String code = AnnotatedCode.parse(option.tracer().annotatedCode()).getDisplayCode();
            // Only canonical LIS/Ninja are method snippets. No imports or helper stubs are injected.
            String unit = code.startsWith("public class Solution") ? code : "public class Solution {\n" + code + "\n}";
            Path directory = Files.createDirectory(temporary.resolve(option.id()));
            Path source = directory.resolve("Solution.java");
            Files.writeString(source, unit);
            var errors = new ByteArrayOutputStream();
            assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, errors, "--release", "17", "-classpath",
                    directory.toString(), "-d", directory.toString(), source.toString()), () -> option.id() + ": " + errors);
            try (var loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                var solution = loader.loadClass("Solution");
                var method = Arrays.stream(solution.getMethods()).filter(m -> m.getDeclaringClass() == solution).findFirst().orElseThrow();
                for (var input : cases(problem)) {
                    Object[] arguments = switch (problem) {
                        case "longest-increasing-subsequence" -> new Object[]{array(input, "nums")};
                        case "stock-transaction-fee" -> new Object[]{array(input, "prices"), input.get("fee")};
                        case "min-insertions-palindrome" -> new Object[]{input.get("text")};
                        case "count-partitions-given-diff" -> new Object[]{array(input, "nums"), input.get("d")};
                        case "ninja-and-his-friends" -> new Object[]{grid(input)};
                        default -> throw new AssertionError(problem);
                    };
                    assertEquals(oracle(problem, input), ((Number) method.invoke(solution.getConstructor().newInstance(), arguments)).intValue(),
                            problem + "/" + option.id() + " " + input);
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"longest-increasing-subsequence", "stock-transaction-fee", "min-insertions-palindrome", "count-partitions-given-diff", "ninja-and-his-friends"})
    void maximumRecursiveInputsFitBudgetsAndCanvasWithoutClamping(String problem) throws Exception {
        for (String form : List.of("recursion", "memoization")) {
            var tracer = approaches.resolve(problem, form).tracer();
            var input = maximum(problem, tracer);
            var trace = runner.run(tracer, input);
            assertFalse(trace.isTruncated(), problem + "/" + form);
            assertEquals(input, trace.getResolvedInput());
            assertEquals(String.valueOf(oracle(problem, input)), last(trace).get("answer"));
            assertTrue(Integer.parseInt(last(trace).get("calls")) <= 220, "Every actual call must fit the recursion renderer");
            assertTrue(new ObjectMapper().writeValueAsBytes(trace).length < 2_000_000);
            Map<String, Object> invalid = new HashMap<>(input);
            switch (problem) {
                case "longest-increasing-subsequence", "count-partitions-given-diff" -> invalid.put("nums", Collections.nCopies(array(input, "nums").length + 1, 1));
                case "stock-transaction-fee" -> invalid.put("prices", Collections.nCopies(array(input, "prices").length + 1, 1));
                case "min-insertions-palindrome" -> invalid.put("text", input.get("text") + "z");
                case "ninja-and-his-friends" -> invalid.put("grid", Collections.nCopies(grid(input).length + 1, List.of(1)));
                default -> throw new AssertionError(problem);
            }
            assertThrows(InputValidationException.class, () -> runner.run(tracer, invalid));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"longest-increasing-subsequence", "stock-transaction-fee", "min-insertions-palindrome", "count-partitions-given-diff", "ninja-and-his-friends"})
    void memoCachesCompleteKeysAndZeroWithoutExpandingHits(String problem) {
        var tracer = approaches.resolve(problem, "memoization").tracer();
        var trace = runner.run(tracer, memoCase(problem));
        assertFalse(trace.isTruncated());
        Set<String> evaluated = new HashSet<>(), callIds = new HashSet<>();
        int hits = 0;
        var steps = trace.getSteps();
        assertNotNull(steps.get(0).getDpTable());
        assertTrue(steps.get(0).getDpTable().cells().stream().flatMap(List::stream).allMatch(c -> c.value().equals("·") && c.state().equals("void")));
        for (int i = 0; i < steps.size(); i++) {
            var step = steps.get(i);
            var v = step.getVariables();
            if (step.getDpTable() != null && step.getDpTable().cells().stream().flatMap(List::stream)
                    .anyMatch(cell -> "probe".equals(cell.state())))
                assertTrue(Set.of("base", "store").contains(v.getOrDefault("event", "")),
                        "The stored-this-step glyph must not claim another write on enter, return or done");
            if ("enter".equals(v.get("event"))) assertTrue(callIds.add(v.get("callId")));
            if (Set.of("base", "store").contains(v.getOrDefault("event", ""))) assertTrue(evaluated.add(v.get("state")), "Memo state evaluated twice");
            if ("cache-hit".equals(v.get("event"))) {
                hits++;
                var returned = steps.get(i + 1);
                assertEquals("return", returned.getVariables().get("event"));
                assertEquals(v.get("callId"), returned.getVariables().get("callId"));
                assertEquals(step.getCallStack().size() - 1, returned.getCallStack().size());
            }
        }
        assertTrue(hits > 0, problem + " must demonstrate real memo reuse");
        assertEquals(String.valueOf(hits), last(trace).get("cacheHits"));
        assertEquals(String.valueOf(evaluated.size()), last(trace).get("stateEvaluations"));
        assertEquals(String.valueOf(evaluated.size()), last(trace).get("computedStates"));
        if (!problem.equals("longest-increasing-subsequence") && !problem.equals("count-partitions-given-diff")) {
            if (!problem.equals("min-insertions-palindrome")) assertEquals("0", last(trace).get("answer"));
            assertTrue(steps.stream().anyMatch(s -> "cache-hit".equals(s.getVariables().get("event")) && "0".equals(s.getVariables().get("value"))));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"longest-increasing-subsequence", "stock-transaction-fee", "min-insertions-palindrome", "count-partitions-given-diff", "ninja-and-his-friends"})
    void recursionHasDistinctCallsAndBudgetStopsBeforeAnyCompletionClaim(String problem) {
        var actual = approaches.resolve(problem, "recursion").tracer();
        var trace = runner.runDefaults(actual);
        assertTrue(trace.getSteps().stream().allMatch(s -> s.getDpTable() == null));
        Set<String> ids = new HashSet<>();
        for (var step : trace.getSteps()) if ("enter".equals(step.getVariables().get("event")))
            assertTrue(ids.add(step.getVariables().get("callId")));
        assertEquals(String.valueOf(ids.size()), last(trace).get("calls"));
        assertEquals("0", last(trace).get("cacheHits"));
        for (String form : List.of("recursion", "memoization")) {
            var owner = approaches.resolve(problem, form).tracer();
            AlgorithmTracer capped = new AlgorithmTracer() {
                public String id() { return owner.id(); }
                public DsType dsType() { return owner.dsType(); }
                public String annotatedCode() { return owner.annotatedCode(); }
                public InputSpec inputSpec() { return owner.inputSpec().withMaxSteps(3); }
                public Map<String, Object> alternateInput() { return owner.alternateInput(); }
                public void run(Inputs input, StepEmitter emitter) { owner.run(input, emitter); }
            };
            var partial = runner.runDefaults(capped);
            assertTrue(partial.isTruncated());
            assertEquals(3, partial.getStepCount());
            assertFalse(partial.getSteps().stream().anyMatch(s -> s.getVariables().containsKey("answer")));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"longest-increasing-subsequence", "stock-transaction-fee", "min-insertions-palindrome", "count-partitions-given-diff", "ninja-and-his-friends"})
    void everyDeclaredAnchorAndTeachingEventIsReachable(String problem) {
        for (var option : approaches.available(problem)) {
            Set<Integer> lines = new HashSet<>();
            Set<String> events = new HashSet<>();
            for (var input : cases(problem)) for (var step : runner.run(option.tracer(), input).getSteps()) {
                lines.add(step.getActiveLine());
                events.add(step.getVariables().getOrDefault("event", ""));
            }
            assertEquals(new HashSet<>(AnnotatedCode.parse(option.tracer().annotatedCode()).getAnchors().values()), lines, option.id());
            assertTrue(events.containsAll(option.teaching().eventNotes().keySet()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"longest-increasing-subsequence", "stock-transaction-fee", "min-insertions-palindrome", "count-partitions-given-diff", "ninja-and-his-friends"})
    void apiServesEachApproachWithCorrectSourceTypeInputAndEncoding(String problem) throws Exception {
        var json = new ObjectMapper();
        for (var option : approaches.available(problem)) {
            var detail = json.readTree(http.perform(get("/api/problems/" + problem).param("approach", option.id()))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertEquals(option.id(), detail.path("approachId").asText());
            assertEquals(option.tracer().dsType().wireValue(), detail.path("dsType").asText());
            assertTrue(detail.path("teaching").path("recurrence").isTextual());
            for (String encoding : List.of("full", "delta")) {
                var body = json.readTree(http.perform(get("/api/problems/" + problem + "/execute").param("approach", option.id())
                                .param("encoding", encoding).with(r -> { r.setRemoteAddr(problem + option.id() + encoding); return r; }))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
                assertEquals(problem, body.path("problemId").asText());
                assertEquals(option.id(), body.path("approachId").asText());
                assertEquals(detail.path("javaCode"), body.path("code"));
                assertEquals(detail.path("dsType"), body.path("dsType"));
                assertEquals(detail.path("complexity"), body.path("complexity"));
                assertFalse(body.path("truncated").asBoolean());
            }
            http.perform(post("/api/problems/" + problem + "/execute").param("approach", option.id())
                    .contentType("application/json").content("{\"unknown\":1}")).andExpect(status().isBadRequest());
        }
        http.perform(get("/api/problems/" + problem + "/execute").param("approach", "fake")).andExpect(status().isBadRequest());
    }

    @Test void jointMemoBudgetAndExistingPositiveOnlyPartitionContractAreExplicitRefusals() {
        var tracer = approaches.resolve("count-partitions-given-diff", "memoization").tracer();
        var error = assertThrows(InputValidationException.class, () -> runner.run(tracer, Map.of("nums", Collections.nCopies(10, 15), "d", 0)));
        assertTrue(error.getFieldErrors().get("nums").contains("100"));
        for (var option : approaches.available(tracer.id()))
            assertThrows(InputValidationException.class, () -> runner.run(option.tracer(), Map.of("nums", List.of(0, 1), "d", 1)));
        for (var option : approaches.available("ninja-and-his-friends")) {
            assertThrows(InputValidationException.class, () -> runner.run(option.tracer(), Map.of("grid", List.of(List.of(1, 2), List.of(1)))));
            assertThrows(InputValidationException.class, () -> runner.run(option.tracer(), Map.of("grid", List.of(List.of(-1)))));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"min-insertions-palindrome", "ninja-and-his-friends"})
    void newBottomUpTablesFinishAtTheirAdvertisedMaximumAndRefuseBeyondIt(String problem) {
        var tracer = approaches.resolve(problem, "tabulation").tracer();
        var input = maximum(problem, tracer);
        var trace = runner.run(tracer, input);
        assertFalse(trace.isTruncated());
        assertEquals(String.valueOf(oracle(problem, input)), last(trace).get("answer"));
        Map<String, Object> oversized = new HashMap<>(input);
        if (problem.equals("min-insertions-palindrome")) oversized.put("text", input.get("text") + "z");
        else oversized.put("grid", Collections.nCopies(grid(input).length + 1, List.of(100)));
        assertThrows(InputValidationException.class, () -> runner.run(tracer, oversized));
    }

    private static Map<String, String> last(ExecutionTrace trace) { return trace.getSteps().get(trace.getStepCount() - 1).getVariables(); }
    private static int[] array(Map<String, Object> input, String key) { return ((List<?>) input.get(key)).stream().mapToInt(x -> ((Number) x).intValue()).toArray(); }
    private static int[][] grid(Map<String, Object> input) { return ((List<?>) input.get("grid")).stream().map(r -> ((List<?>) r).stream().mapToInt(x -> ((Number) x).intValue()).toArray()).toArray(int[][]::new); }

    private static Map<String, Object> memoCase(String problem) {
        return switch (problem) {
            case "longest-increasing-subsequence" -> Map.of("nums", List.of(1, 2, 3, 4, 5));
            case "stock-transaction-fee" -> Map.of("prices", Collections.nCopies(6, 0), "fee", 0);
            case "min-insertions-palindrome" -> Map.of("text", "abcde");
            case "count-partitions-given-diff" -> Map.of("nums", List.of(1, 1, 1, 1, 1, 1), "d", 0);
            case "ninja-and-his-friends" -> Map.of("grid", Collections.nCopies(4, Collections.nCopies(3, 0)));
            default -> throw new AssertionError(problem);
        };
    }
    private static Map<String, Object> maximum(String problem, AlgorithmTracer tracer) {
        var field = tracer.inputSpec().getFields().get(0);
        int n = ((Number) field.getConstraints().getOrDefault("maxLength", field.getConstraints().getOrDefault("maxRows", 1))).intValue();
        return switch (problem) {
            case "longest-increasing-subsequence" -> Map.of("nums", java.util.stream.IntStream.range(0, n).boxed().toList());
            case "stock-transaction-fee" -> Map.of("prices", java.util.stream.IntStream.range(0, n).map(i -> i % 2 == 0 ? 0 : 100).boxed().toList(), "fee", 0);
            case "min-insertions-palindrome" -> Map.of("text", "abcdefghijkl".substring(0, n));
            case "count-partitions-given-diff" -> Map.of("nums", Collections.nCopies(n, 1), "d", 0);
            case "ninja-and-his-friends" -> Map.of("grid", Collections.nCopies(n, Collections.nCopies(((Number) field.getConstraints().get("maxCols")).intValue(), 100)));
            default -> throw new AssertionError(problem);
        };
    }
    private static List<Map<String, Object>> cases(String problem) {
        var random = new Random(816);
        List<Map<String, Object>> cases = new ArrayList<>();
        cases.add(memoCase(problem));
        for (int sample = 0; sample < 50; sample++) {
            int length = 1 + random.nextInt(5);
            List<Integer> values = java.util.stream.IntStream.range(0, length).map(i -> random.nextInt(4)).boxed().toList();
            cases.add(switch (problem) {
                case "longest-increasing-subsequence" -> Map.of("nums", values.stream().map(v -> v - 2).toList());
                case "stock-transaction-fee" -> Map.of("prices", values, "fee", random.nextInt(4));
                case "min-insertions-palindrome" -> Map.of("text", values.stream().map(v -> String.valueOf((char) ('a' + v))).collect(java.util.stream.Collectors.joining()));
                case "count-partitions-given-diff" -> Map.of("nums", values.stream().map(v -> v + 1).toList(), "d", random.nextInt(12));
                case "ninja-and-his-friends" -> {
                    int rows = 1 + random.nextInt(3), cols = 1 + random.nextInt(3);
                    List<List<Integer>> grid = new ArrayList<>();
                    for (int r = 0; r < rows; r++) grid.add(java.util.stream.IntStream.range(0, cols).map(i -> random.nextInt(4)).boxed().toList());
                    yield Map.of("grid", grid);
                }
                default -> throw new AssertionError(problem);
            });
        }
        return cases;
    }
    // References enumerate mathematical choices, not the recursive state/cache implementations.
    private static int oracle(String problem, Map<String, Object> input) {
        return switch (problem) {
            case "longest-increasing-subsequence" -> {
                int[] values = array(input, "nums"); int best = 0;
                for (int mask = 0; mask < 1 << values.length; mask++) {
                    int previous = Integer.MIN_VALUE, count = 0; boolean increasing = true;
                    for (int i = 0; i < values.length; i++) if ((mask & (1 << i)) != 0) {
                        if (values[i] <= previous) increasing = false;
                        previous = values[i]; count++;
                    }
                    if (increasing) best = Math.max(best, count);
                }
                yield best;
            }
            case "stock-transaction-fee" -> trades(array(input, "prices"), ((Number) input.get("fee")).intValue(), 0);
            case "min-insertions-palindrome" -> {
                String text = (String) input.get("text"); int longest = 0;
                for (int mask = 0; mask < 1 << text.length(); mask++) {
                    var chosen = new StringBuilder();
                    for (int i = 0; i < text.length(); i++) if ((mask & (1 << i)) != 0) chosen.append(text.charAt(i));
                    if (chosen.toString().equals(new StringBuilder(chosen).reverse().toString())) longest = Math.max(longest, chosen.length());
                }
                yield text.length() - longest;
            }
            case "count-partitions-given-diff" -> {
                int[] values = array(input, "nums"); int total = Arrays.stream(values).sum(), count = 0;
                for (int mask = 0; mask < 1 << values.length; mask++) {
                    int s1 = 0;
                    for (int i = 0; i < values.length; i++) if ((mask & (1 << i)) != 0) s1 += values[i];
                    if (2 * s1 - total == ((Number) input.get("d")).intValue()) count++;
                }
                yield count;
            }
            case "ninja-and-his-friends" -> {
                int[][] grid = grid(input); var first = paths(grid.length, grid[0].length, 0); var second = paths(grid.length, grid[0].length, grid[0].length - 1);
                int best = 0;
                for (int[] a : first) for (int[] b : second) {
                    int sum = 0;
                    for (int r = 0; r < grid.length; r++) sum += grid[r][a[r]] + (a[r] == b[r] ? 0 : grid[r][b[r]]);
                    best = Math.max(best, sum);
                }
                yield best;
            }
            default -> throw new AssertionError(problem);
        };
    }
    private static int trades(int[] prices, int fee, int after) {
        int best = 0;
        for (int buy = after; buy < prices.length; buy++) for (int sell = buy + 1; sell < prices.length; sell++)
            best = Math.max(best, prices[sell] - prices[buy] - fee + trades(prices, fee, sell + 1));
        return best;
    }
    private static List<int[]> paths(int rows, int cols, int start) {
        List<int[]> current = new ArrayList<>(); current.add(new int[]{start});
        for (int r = 1; r < rows; r++) {
            List<int[]> next = new ArrayList<>();
            for (int[] path : current) for (int col = Math.max(0, path[r - 1] - 1); col <= Math.min(cols - 1, path[r - 1] + 1); col++) {
                int[] longer = Arrays.copyOf(path, r + 1); longer[r] = col; next.add(longer);
            }
            current = next;
        }
        return current;
    }
}
