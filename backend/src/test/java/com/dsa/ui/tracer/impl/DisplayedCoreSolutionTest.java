package com.dsa.ui.tracer.impl;

import com.dsa.ui.catalog.StatementCatalog;
import com.dsa.ui.tracer.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.web.servlet.MockMvc;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Executes the exact standalone source delivered to learners, not tracer-side helpers. */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DisplayedCoreSolutionTest {
    @Autowired TracerRegistry registry;
    @Autowired TraceRunner runner;
    @Autowired StatementCatalog statements;
    @Autowired MockMvc http;
    @TempDir Path temporary;
    private final ObjectMapper json = new ObjectMapper();

    Stream<CompleteSourceTracer> family() {
        return registry.all().stream().filter(CompleteSourceTracer.class::isInstance)
                .map(CompleteSourceTracer.class::cast);
    }

    @Test
    void eachCompleteResourceHasExactlyOneRegisteredOwner() throws Exception {
        Set<String> resources = Arrays.stream(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:solutions/core/*.java"))
                .map(r -> r.getFilename().replaceFirst("\\.java$", "")).collect(Collectors.toSet());
        List<CompleteSourceTracer> tracers = family().toList();
        assertFalse(tracers.isEmpty());
        assertEquals(tracers.stream().map(AlgorithmTracer::id).collect(Collectors.toSet()), resources);
        for (var tracer : tracers) {
            assertEquals(SolutionSource.read(tracer.sourceResourcePath()), tracer.annotatedCode());
        }
    }

    @ParameterizedTest(name = "standalone source executes: {0}")
    @MethodSource("family")
    void compilesExactSourceAndMatchesDefaultsAlternateAndPublishedExamples(CompleteSourceTracer tracer)
            throws Exception {
        try (var solution = compile(tracer)) {
            var examples = statements.find(tracer.id()).orElseThrow().examples();
            assertFalse(examples.isEmpty(), "Independent published expected answers are required");
            assertEquals(1, examples.stream().map(e -> e.answerVariable()).distinct().count());
            String answerVariable = examples.get(0).answerVariable();
            assertAgrees(tracer, solution, Map.of(), answerVariable, null);
            assertAgrees(tracer, solution, tracer.alternateInput(), answerVariable, null);
            for (var example : examples) {
                assertAgrees(tracer, solution, example.input(), example.answerVariable(), example.output());
            }
        }
    }

    @ParameterizedTest(name = "HTTP exact source and anchors: {0}")
    @MethodSource("family")
    void detailAndFullAndDeltaExecuteServeTheOwnedSource(CompleteSourceTracer tracer) throws Exception {
        var code = AnnotatedCode.parse(tracer.annotatedCode());
        var detail = http.perform(get("/api/problems/{id}", tracer.id())).andExpect(status().isOk()).andReturn();
        assertEquals(code.getDisplayCode(), json.readTree(detail.getResponse().getContentAsByteArray())
                .path("javaCode").asText());
        for (String encoding : List.of("full", "delta")) {
            var response = http.perform(get("/api/problems/{id}/execute", tracer.id()).param("encoding", encoding)
                    .with(r -> { r.setRemoteAddr("core-source-" + tracer.id()); return r; }))
                    .andExpect(status().isOk()).andReturn();
            var body = json.readTree(response.getResponse().getContentAsByteArray());
            assertEquals(code.getDisplayCode(), body.path("code").asText());
            assertEquals(json.valueToTree(code.getAnchors()), body.path("anchors"));
            assertEquals(encoding, body.path("encoding").asText());
            assertFalse(body.path("truncated").asBoolean());
            assertFalse(body.path("code").asText().contains("// @a"));
            for (var step : body.path("steps")) {
                int line = step.path("activeLine").asInt();
                assertTrue(line > 0 && line <= code.lineCount());
                assertTrue(code.getAnchors().containsValue(line));
            }
        }
    }

    @Test
    void lisReconstructionKeepsTheTracersStrictUpdateAndFirstEndpointTieRules() throws Exception {
        var tracer = owner(PrintLisTracer.class);
        try (var solution = compile(tracer)) {
            assertAgrees(tracer, solution, Map.of("nums", List.of(7)), "lis", "[7]");
            assertAgrees(tracer, solution, Map.of("nums", List.of(2, 2, 2)), "lis", "[2]");
            assertAgrees(tracer, solution, Map.of("nums", List.of(3, 1, 4, 1, 5)), "lis", "[3, 4, 5]");
            assertAgrees(tracer, solution, Map.of("nums", List.of(-3, -1, -2, 0)), "lis", "[-3, -1, 0]");
            assertAgrees(tracer, solution, Map.of("nums", List.of(4, 3, 2, 1)), "lis", "[4]");
        }
    }

    @Test
    void parenthesesCoverAllCloserTypesAndBothFailureModes() throws Exception {
        var tracer = owner(BalancedParenthesesTracer.class);
        try (var solution = compile(tracer)) {
            for (String input : List.of("()", "[]", "{}", "{[()]}", "()[]{}")) {
                assertAgrees(tracer, solution, Map.of("expression", input), "answer", "true");
            }
            for (String input : List.of(")", "]", "}", "(", "[", "{", "(]", "[}", "{)", "([)]")) {
                assertAgrees(tracer, solution, Map.of("expression", input), "answer", "false");
            }
        }
    }

    @Test
    void asteroidSurvivorsStayLeftToRightAcrossCollisionBranchesAndBoundaryValues() throws Exception {
        var tracer = owner(AsteroidCollisionTracer.class);
        try (var solution = compile(tracer)) {
            assertAgrees(tracer, solution, Map.of("asteroids", List.of(-2, -1, 1, 2)), "answer", "[-2, -1, 1, 2]");
            assertAgrees(tracer, solution, Map.of("asteroids", List.of(2, 3, -4)), "answer", "[-4]");
            assertAgrees(tracer, solution, Map.of("asteroids", List.of(4, 3, -2)), "answer", "[4, 3]");
            assertAgrees(tracer, solution, Map.of("asteroids", List.of(100, -100)), "answer", "[]");
            assertAgrees(tracer, solution, Map.of("asteroids", List.of(-100)), "answer", "[-100]");
        }
    }

    @Test
    void nextPermutationMatchesAnIndependentEnumeratedOrderIncludingDuplicates() throws Exception {
        var tracer = owner(NextPermutationTracer.class);
        try (var solution = compile(tracer)) {
            List<List<Integer>> orders = List.of(List.of(1, 1, 2), List.of(1, 2, 1), List.of(2, 1, 1));
            for (int i = 0; i < orders.size(); i++) {
                assertAgrees(tracer, solution, Map.of("nums", orders.get(i)), "result",
                        orders.get((i + 1) % orders.size()).toString());
            }
            assertAgrees(tracer, solution, Map.of("nums", List.of(1)), "result", "[1]");
            assertAgrees(tracer, solution, Map.of("nums", List.of(2, 2)), "result", "[2, 2]");
            assertAgrees(tracer, solution, Map.of("nums", List.of(-2, -1, -3)), "result", "[-1, -3, -2]");
        }
    }

    @Test
    void quickSortMatchesIndependentJdkOrderingOnRepeatedNegativeAndExtremeValues() throws Exception {
        var tracer = owner(QuickSortTracer.class);
        try (var solution = compile(tracer)) {
            var random = new Random(541);
            for (int count = 2; count <= 20; count++) {
                var input = random.ints(count, -4, 5).boxed().toList();
                assertAgrees(tracer, solution, Map.of("nums", input), "result",
                        input.stream().sorted().toList().toString());
            }
            assertAgrees(tracer, solution, Map.of("nums", List.of(999, -999)), "result", "[-999, 999]");
            assertAgrees(tracer, solution, Map.of("nums", List.of(2, 2, 2)), "result", "[2, 2, 2]");
        }
    }

    @Test
    void reverseEveryWordPreservesEverySpaceAgainstIndependentTokenReversal() throws Exception {
        var tracer = owner(ReverseEveryWordTracer.class);
        try (var solution = compile(tracer)) {
            assertEquals(String.class, solution.entry().getReturnType());
            for (String input : List.of(" ", " ".repeat(60), " a  BC'  d ", "'", "B",
                    "a".repeat(60), " aB'".repeat(15))) {
                assertWordReversal(tracer, solution, input);
            }
            // Empty split tokens preserve leading/trailing/repeated spaces independently
            // of the tracer's character-array scan and in-place swap implementation.
            char[] alphabet = {'a', 'B', '\'', ' '};
            for (int length = 1; length <= 5; length++) {
                int cases = 1 << (2 * length);
                for (int encoded = 0; encoded < cases; encoded++) {
                    int remainder = encoded;
                    var input = new StringBuilder();
                    for (int index = 0; index < length; index++) {
                        input.append(alphabet[remainder & 3]);
                        remainder >>= 2;
                    }
                    assertWordReversal(tracer, solution, input.toString());
                }
            }
        }
    }

    private void assertWordReversal(AlgorithmTracer tracer, Compiled solution, String input) throws Exception {
        String expected = Arrays.stream(input.split(" ", -1))
                .map(word -> new StringBuilder(word).reverse().toString()).collect(Collectors.joining(" "));
        var trace = assertAgrees(tracer, solution, Map.of("s", input), "answer", expected);
        String answer = trace.getSteps().get(trace.getStepCount() - 1).getVariables().get("answer");
        assertEquals(input.length(), answer.length());
        for (int index = 0; index < input.length(); index++) {
            assertEquals(input.charAt(index) == ' ', answer.charAt(index) == ' ', "Space index " + index);
        }
    }

    @Test
    void infixPostfixMatchesAnIndependentExpressionTreeIncludingAssociativity() throws Exception {
        assertInfixSources(owner(InfixToPostfixTracer.class), false);
    }

    @Test
    void infixPrefixMatchesAnIndependentExpressionTreeIncludingAssociativity() throws Exception {
        assertInfixSources(owner(InfixToPrefixTracer.class), true);
    }

    private void assertInfixSources(AlgorithmTracer tracer, boolean prefix) throws Exception {
        try (var solution = compile(tracer)) {
            assertEquals(String.class, solution.entry().getReturnType());
            var inputs = new LinkedHashSet<>(List.of("A", "(A)", "(((A)))", "A^B^C", "A-B-C", "A/B/C",
                    "A+B*C^D^E", "(A+B)*(C-D)", "A^(B^C)", "(A^B)^C", "A-(B-C)", "A/(B/C)",
                    "A" + "+A".repeat(19), "(" + "A" + "^B".repeat(18) + ")"));
            // Exhaustive short strings exercise both grammar and all operator kinds. The
            // oracle is recursive descent -> syntax tree traversal, not shunting yard.
            char[] alphabet = {'A', '+', '-', '*', '/', '^', '(', ')'};
            for (int length = 1; length <= 4; length++) {
                int count = 1 << (3 * length);
                for (int encoded = 0; encoded < count; encoded++) {
                    int remaining = encoded;
                    var input = new StringBuilder();
                    for (int i = 0; i < length; i++) { input.append(alphabet[remaining & 7]); remaining >>= 3; }
                    inputs.add(input.toString());
                }
            }
            var random = new Random(55057);
            for (int index = 0; index < 250; index++) inputs.add(randomExpression(random, 3));
            int validCases = 0, invalidCases = 0;
            for (String input : inputs) {
                ExpressionNode tree;
                try { tree = new ExpressionParser(input).parse(); }
                catch (IllegalArgumentException invalid) {
                    invalidCases++;
                    assertThrows(InputValidationException.class,
                            () -> InputValidator.validate(tracer.inputSpec(), Map.of("expression", input)), input);
                    var failure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                            () -> solution.entry().invoke(solution.instance(), input), input);
                    assertInstanceOf(IllegalArgumentException.class, failure.getCause(), input);
                    continue;
                }
                validCases++;
                assertAgrees(tracer, solution, Map.of("expression", input), "answer", tree.render(prefix));
            }
            System.out.printf("Infix oracle %s: %d valid / %d invalid unique expressions%n",
                    tracer.id(), validCases, invalidCases);
            for (String invalid : List.of("", "AB", "A++B", "(A+B", "A+B)", "()", "A(B)",
                    "a+b", "A B", "A& B")) {
                var failure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                        () -> solution.entry().invoke(solution.instance(), invalid), invalid);
                assertInstanceOf(IllegalArgumentException.class, failure.getCause());
            }
            var nullFailure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> solution.entry().invoke(solution.instance(), new Object[]{null}));
            assertInstanceOf(IllegalArgumentException.class, nullFailure.getCause());
        }
    }

    private String randomExpression(Random random, int depth) {
        if (depth == 0 || random.nextInt(4) == 0) return String.valueOf((char) ('A' + random.nextInt(26)));
        String value = randomExpression(random, depth - 1) + "+-*/^".charAt(random.nextInt(5))
                + randomExpression(random, depth - 1);
        return random.nextBoolean() ? "(" + value + ")" : value;
    }

    private record ExpressionNode(char token, ExpressionNode left, ExpressionNode right) {
        String render(boolean prefix) {
            if (left == null) return String.valueOf(token);
            return prefix ? token + left.render(true) + right.render(true)
                    : left.render(false) + right.render(false) + token;
        }
    }

    /** Conventional grammar: sums -> products -> right-associative powers -> grouped operands. */
    private static final class ExpressionParser {
        private final String text;
        private int cursor;
        ExpressionParser(String text) { this.text = text; }
        ExpressionNode parse() {
            ExpressionNode result = sum();
            if (cursor != text.length()) throw new IllegalArgumentException();
            return result;
        }
        private ExpressionNode sum() {
            ExpressionNode node = product();
            while (cursor < text.length() && "+-".indexOf(text.charAt(cursor)) >= 0) {
                char op = text.charAt(cursor++);
                node = new ExpressionNode(op, node, product());
            }
            return node;
        }
        private ExpressionNode product() {
            ExpressionNode node = power();
            while (cursor < text.length() && "*/".indexOf(text.charAt(cursor)) >= 0) {
                char op = text.charAt(cursor++);
                node = new ExpressionNode(op, node, power());
            }
            return node;
        }
        private ExpressionNode power() {
            ExpressionNode node = atom();
            if (cursor < text.length() && text.charAt(cursor) == '^') {
                cursor++;
                node = new ExpressionNode('^', node, power());
            }
            return node;
        }
        private ExpressionNode atom() {
            if (cursor == text.length()) throw new IllegalArgumentException();
            char token = text.charAt(cursor++);
            if (token >= 'A' && token <= 'Z') return new ExpressionNode(token, null, null);
            if (token != '(') throw new IllegalArgumentException();
            ExpressionNode node = sum();
            if (cursor == text.length() || text.charAt(cursor++) != ')') throw new IllegalArgumentException();
            return node;
        }
    }

    @Test
    void rabinKarpMatchesNaiveSearchAndIndependentWindowHashesAcrossCollisionsAndBounds() throws Exception {
        var tracer = owner(RabinKarpTracer.class);
        try (var solution = compile(tracer)) {
            assertEquals("java.util.List<java.lang.Integer>", solution.entry().getGenericReturnType().getTypeName());
            String source = AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode();
            assertTrue(source.contains("private static final int BASE = 256;"));
            assertTrue(source.contains("private static final int MOD = 101;"));
            assertEquals(90, polynomialHash("ca"));
            assertEquals(90, polynomialHash("ah"));
            for (var pair : List.of(List.of("ca", "ah"), List.of("caah", "ah"),
                    List.of("a", "ab"), List.of("a", "a"), List.of("a", "b"),
                    List.of("aaaaa", "aa"), List.of("zzabra", "abra"),
                    List.of("abracadabra", "abra"), List.of("z".repeat(30), "z".repeat(12)))) {
                assertRabinKarp(tracer, solution, pair.get(0), pair.get(1));
            }
            List<String> texts = binaryWords(5), patterns = binaryWords(3);
            for (String text : texts) for (String pattern : patterns) {
                assertRabinKarp(tracer, solution, text, pattern);
            }
            var random = new Random(256101);
            for (int index = 0; index < 80; index++) {
                String text = random.ints(30, 'a', 'z' + 1)
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
                String pattern = index % 2 == 0 ? text.substring(18)
                        : random.ints(12, 'a', 'z' + 1)
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
                assertRabinKarp(tracer, solution, text, pattern);
            }
        }
    }

    private List<String> binaryWords(int maximumLength) {
        var words = new ArrayList<String>();
        for (int length = 1; length <= maximumLength; length++) {
            for (int encoded = 0; encoded < (1 << length); encoded++) {
                var word = new StringBuilder();
                for (int index = 0; index < length; index++) word.append((encoded & (1 << index)) == 0 ? 'a' : 'b');
                words.add(word.toString());
            }
        }
        return words;
    }

    private void assertRabinKarp(AlgorithmTracer tracer, Compiled solution, String text, String pattern) throws Exception {
        Method sourceHash = solution.instance().getClass().getDeclaredMethod("hash", String.class);
        sourceHash.setAccessible(true);
        assertEquals(polynomialHash(pattern), ((Number) sourceHash.invoke(solution.instance(), pattern)).longValue(),
                "The compiled helper, not just an unused BASE/MOD declaration, must implement the displayed hash");
        var expected = new ArrayList<Integer>();
        for (int start = 0; start + pattern.length() <= text.length(); start++) {
            if (text.regionMatches(start, pattern, 0, pattern.length())) expected.add(start);
        }
        var trace = assertAgrees(tracer, solution, Map.of("text", text, "pattern", pattern), "answer", expected.toString());
        var observed = trace.getSteps().stream().filter(step -> step.getVariables().containsKey("start")).toList();
        assertEquals(Math.max(0, text.length() - pattern.length() + 1), observed.size());
        for (int index = 0; index < observed.size(); index++) {
            var variables = observed.get(index).getVariables();
            String window = text.substring(index, index + pattern.length());
            assertEquals(String.valueOf(index), variables.get("start"));
            assertEquals(String.valueOf(polynomialHash(pattern)), variables.get("patternHash"));
            assertEquals(String.valueOf(polynomialHash(window)), variables.get("windowHash"));
            assertEquals(polynomialHash(window), ((Number) sourceHash.invoke(solution.instance(), window)).longValue());
            int current = index;
            assertEquals(expected.stream().filter(match -> match <= current).toList().toString(), variables.get("matches"));
        }
        if (text.equals("ca") && pattern.equals("ah")) {
            assertTrue(trace.getSteps().get(0).getDescription().contains("Hash collision rejected"));
        }
    }

    private long polynomialHash(String value) {
        // Recompute a polynomial independently, not by reusing the rolling recurrence.
        long result = 0;
        for (int index = 0; index < value.length(); index++) {
            long weight = 1;
            for (int exponent = 0; exponent < value.length() - index - 1; exponent++) weight = weight * 256 % 101;
            result = (result + value.charAt(index) * weight) % 101;
        }
        return result;
    }

    @Test
    void bookAllocationMatchesExhaustiveContiguousPartitionsIncludingImpossibleStudents() throws Exception {
        var tracer = owner(BookAllocationTracer.class);
        try (var solution = compile(tracer)) {
            assertAgrees(tracer, solution, Map.of("pages", List.of(1000), "m", 1), "answer", "1000");
            assertAgrees(tracer, solution, Map.of("pages", List.of(1), "m", 12), "answer", "-1");
            partitionCases(tracer, solution, "pages", "m", 12, 1000, true);
        }
    }

    @Test
    void paintersMatchExhaustivePartitionsAndCanLeaveExtraPaintersIdle() throws Exception {
        var tracer = owner(PaintersPartitionTracer.class);
        try (var solution = compile(tracer)) {
            assertAgrees(tracer, solution, Map.of("boards", List.of(1000), "painters", 20), "answer", "1000");
            partitionCases(tracer, solution, "boards", "painters", 20, 1000, false);
        }
    }

    @Test
    void shippingMatchesExhaustivePartitionsWithoutReorderingPackages() throws Exception {
        var tracer = owner(ShipPackagesDDaysTracer.class);
        try (var solution = compile(tracer)) {
            assertAgrees(tracer, solution, Map.of("weights", List.of(500), "days", 25), "answer", "500");
            partitionCases(tracer, solution, "weights", "days", 25, 500, false);
        }
    }

    @Test
    void bouquetsMatchIndependentIntervalSelectionAndRespectAdjacencyAndNonReuse() throws Exception {
        var tracer = owner(MinDaysBouquetsTracer.class);
        try (var solution = compile(tracer)) {
            assertAgrees(tracer, solution, Map.of("bloomDay", List.of(1000), "m", 1, "k", 1), "answer", "1000");
            assertAgrees(tracer, solution, Map.of("bloomDay", List.of(1), "m", 30, "k", 30), "answer", "-1");
            assertAgrees(tracer, solution, Map.of("bloomDay", List.of(1, 10, 1, 10), "m", 2, "k", 2), "answer", "10");
            assertAgrees(tracer, solution, Map.of("bloomDay", List.of(1, 1, 1, 10), "m", 2, "k", 2), "answer", "10");
            assertAgrees(tracer, solution, Map.of("bloomDay", Collections.nCopies(30, 1000), "m", 10, "k", 3), "answer", "1000");
            var random = new Random(1482);
            for (int count = 1; count <= 8; count++) {
                var days = random.ints(count, 1, 11).boxed().toList();
                for (int k = 1; k <= count + 1; k++) {
                    int m = 2;
                    int expected = -1;
                    for (int day : days.stream().distinct().sorted().toList()) {
                        // Dynamic interval selection, independent of the tracer's greedy run counter.
                        int[] best = new int[count + 1];
                        for (int end = 1; end <= count; end++) {
                            best[end] = best[end - 1];
                            if (end >= k && Collections.max(days.subList(end - k, end)) <= day) {
                                best[end] = Math.max(best[end], best[end - k] + 1);
                            }
                        }
                        if (best[count] >= m) { expected = day; break; }
                    }
                    assertAgrees(tracer, solution, Map.of("bloomDay", days, "m", m, "k", k),
                            "answer", String.valueOf(expected));
                }
            }
        }
    }

    private void partitionCases(AlgorithmTracer tracer, Compiled solution, String arrayField,
                                String groupsField, int maxLength, int maxValue, boolean requireEveryGroup)
            throws Exception {
        var random = new Random(1011);
        for (int count = 1; count <= 7; count++) {
            var values = random.ints(count, 1, 21).boxed().toList();
            for (int groups = 1; groups <= count + 1; groups++) {
                int expected = Integer.MAX_VALUE;
                for (int cuts = 0; cuts < (1 << (count - 1)); cuts++) {
                    int used = Integer.bitCount(cuts) + 1;
                    if (requireEveryGroup ? used != groups : used > groups) continue;
                    int sum = 0, largest = 0;
                    for (int i = 0; i < count; i++) {
                        sum += values.get(i);
                        if (i == count - 1 || (cuts & (1 << i)) != 0) {
                            largest = Math.max(largest, sum);
                            sum = 0;
                        }
                    }
                    expected = Math.min(expected, largest);
                }
                assertAgrees(tracer, solution, Map.of(arrayField, values, groupsField, groups), "answer",
                        String.valueOf(expected == Integer.MAX_VALUE ? -1 : expected));
            }
        }
        var ceiling = Collections.nCopies(maxLength, maxValue);
        assertAgrees(tracer, solution, Map.of(arrayField, ceiling, groupsField, 1), "answer",
                String.valueOf(maxLength * maxValue));
        assertAgrees(tracer, solution, Map.of(arrayField, ceiling, groupsField, maxLength), "answer",
                String.valueOf(maxValue));
    }

    @Test
    void smallestDivisorMatchesExhaustiveSmallInputsAndAnIndependentLinearOracle() throws Exception {
        var tracer = owner(SmallestDivisorTracer.class);
        try (var solution = compile(tracer)) {
            for (int length = 1; length <= 3; length++) {
                int combinations = (int) Math.pow(4, length);
                for (int encoded = 0; encoded < combinations; encoded++) {
                    List<Integer> nums = new ArrayList<>();
                    int remaining = encoded;
                    for (int i = 0; i < length; i++) {
                        nums.add(remaining % 4 + 1);
                        remaining /= 4;
                    }
                    for (int threshold = length; threshold <= nums.stream().mapToInt(Integer::intValue).sum(); threshold++) {
                        int expected = 1;
                        while (true) {
                            int divisor = expected;
                            long sum = nums.stream().mapToLong(x -> (x.longValue() + divisor - 1) / divisor).sum();
                            if (sum <= threshold) break;
                            expected++;
                        }
                        assertAgrees(tracer, solution, Map.of("nums", nums, "threshold", threshold), "answer",
                                String.valueOf(expected));
                    }
                }
            }
            assertAgrees(tracer, solution, Map.of("nums", Collections.nCopies(30, 1_000_000), "threshold", 30),
                    "answer", "1000000");
            assertAgrees(tracer, solution, Map.of("nums", List.of(1_000_000), "threshold", 10_000_000),
                    "answer", "1");
        }
    }

    @Test
    void matrixMedianMatchesIndependentFlattenedOrderingIncludingEvenAndDuplicateGrids() throws Exception {
        var tracer = owner(MatrixMedianTracer.class);
        try (var solution = compile(tracer)) {
            assertAgrees(tracer, solution, Map.of("matrix", List.of(List.of(1, 9))), "answer", "1");
            assertAgrees(tracer, solution, Map.of("matrix", List.of(List.of(0, 0), List.of(999, 999))), "answer", "0");
            var random = new Random(2709);
            for (int rows = 1; rows <= 8; rows++) {
                for (int columns = 1; columns <= 8; columns++) {
                    List<List<Integer>> matrix = new ArrayList<>();
                    for (int row = 0; row < rows; row++) {
                        matrix.add(random.ints(columns, 0, 1000).boxed().sorted().toList());
                    }
                    var ordered = matrix.stream().flatMap(List::stream).sorted().toList();
                    int expected = ordered.get((ordered.size() - 1) / 2);
                    assertAgrees(tracer, solution, Map.of("matrix", matrix), "answer", String.valueOf(expected));
                }
            }
        }
    }

    @Test
    void standaloneSourcesRejectTheSameImpossibleThresholdAndMalformedMatrixContracts() throws Exception {
        try (var solution = compile(owner(SmallestDivisorTracer.class))) {
            for (Object[] arguments : List.of(new Object[]{new int[]{1, 2}, 1},
                    new Object[]{new int[0], 1}, new Object[]{new int[]{0}, 1})) {
                var failure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                        () -> solution.entry().invoke(solution.instance(), arguments));
                assertInstanceOf(IllegalArgumentException.class, failure.getCause());
            }
        }
        try (var solution = compile(owner(MatrixMedianTracer.class))) {
            for (int[][] matrix : List.<int[][]>of(new int[0][], new int[][]{{}},
                    new int[][]{{1, 2}, {3}}, new int[][]{{9, 1, 2}})) {
                var failure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                        () -> solution.entry().invoke(solution.instance(), (Object) matrix));
                assertInstanceOf(IllegalArgumentException.class, failure.getCause());
            }
        }
    }

    @Test
    void phaseAnchorsPointAtActualOperationsRatherThanMissingHelpers() {
        checkLine(owner(PrintLisTracer.class), "backlink", "reversed.add(nums[cursor]);");
        checkLine(owner(PrintLisTracer.class), "done", "return answer;");
        checkLine(owner(BalancedParenthesesTracer.class), "mismatch", "return false;");
        checkLine(owner(AsteroidCollisionTracer.class), "done", "return toArray(stack);");
        checkLine(owner(NextPermutationTracer.class), "done", "return nums;");
        checkLine(owner(QuickSortTracer.class), "done", "return nums;");
        checkLine(owner(QuickSortTracer.class), "base", "return;");
        checkLine(owner(QuickSortTracer.class), "placePivot", "swap(nums, low, i);");
        checkLine(owner(ReverseEveryWordTracer.class), "reverse", "reverse(chars, start, end - 1);");
        checkLine(owner(ReverseEveryWordTracer.class), "done", "return new String(chars);");
        checkLine(owner(RabinKarpTracer.class), "window", "for (int start = 0; start + pattern.length() <= text.length(); start++) {");
        checkLine(owner(RabinKarpTracer.class), "done", "return matches;");
        checkLine(owner(InfixToPostfixTracer.class), "done", "return result.toString();");
        checkLine(owner(InfixToPrefixTracer.class), "init", "String reversed = reverseAndSwap(expression);");
        checkLine(owner(InfixToPrefixTracer.class), "done", "return result.reverse().toString();");
        for (var tracer : List.of(owner(InfixToPostfixTracer.class), owner(InfixToPrefixTracer.class))) {
            checkLine(tracer, "operand", "result.append(c);");
            checkLine(tracer, "openParen", "stack.push(c);");
            checkLine(tracer, "closeParen", "result.append(stack.pop());");
            checkLine(tracer, "higherPrec", "result.append(stack.pop());");
            checkLine(tracer, "pushOp", "stack.push(c);");
            checkLine(tracer, "flush", "result.append(stack.pop());");
        }
        checkLine(owner(BookAllocationTracer.class), "impossible", "if (m > pages.length) return -1;");
        checkLine(owner(BookAllocationTracer.class), "newStudent", "students++;");
        checkLine(owner(BookAllocationTracer.class), "addToCurrent", "pageSum += p;");
        checkLine(owner(PaintersPartitionTracer.class), "newPainter", "painters++;");
        checkLine(owner(PaintersPartitionTracer.class), "addToCurrent", "time += b;");
        checkLine(owner(ShipPackagesDDaysTracer.class), "newDay", "days++;");
        checkLine(owner(ShipPackagesDDaysTracer.class), "addToDay", "load += w;");
        checkLine(owner(MinDaysBouquetsTracer.class), "impossible", "return -1;");
        checkLine(owner(MinDaysBouquetsTracer.class), "init", "int low = min(bloomDay), high = max(bloomDay), ans = -1;");
        checkLine(owner(MinDaysBouquetsTracer.class), "extendRun", "run++;");
        checkLine(owner(MinDaysBouquetsTracer.class), "breakRun", "run = 0;");
        for (var tracer : family().filter(t -> t.dsType() == com.dsa.ui.model.DsType.SEARCH_SPACE).toList()) {
            if (SmallestDivisorTracer.class.isInstance(tracer)) {
                checkLine(tracer, "init", "int low = 1, high = max(nums), ans = high;");
                checkLine(tracer, "tally", "sum += ((long) x + mid - 1) / mid;");
            } else if (!(tracer instanceof MinDaysBouquetsTracer)) {
                checkLine(tracer, "init", "while (low <= high) {");
            }
            checkLine(tracer, "mid", "int mid = low + (high - low) / 2;");
            checkLine(tracer, "feasible", "ans = mid;");
            checkLine(tracer, "infeasible", "low = mid + 1;");
            checkLine(tracer, "done", "return ans;");
        }
        checkLine(owner(MatrixMedianTracer.class), "init", "int low = minOfFirstColumn(matrix), high = maxOfLastColumn(matrix);");
        checkLine(owner(MatrixMedianTracer.class), "mid", "int mid = low + (high - low) / 2;");
        checkLine(owner(MatrixMedianTracer.class), "tooFew", "low = mid + 1;");
        checkLine(owner(MatrixMedianTracer.class), "enough", "high = mid;");
        checkLine(owner(MatrixMedianTracer.class), "done", "return low;");
    }

    private void checkLine(AlgorithmTracer tracer, String anchor, String expected) {
        var code = AnnotatedCode.parse(tracer.annotatedCode());
        assertEquals(expected, code.getDisplayCode().lines().toList().get(code.resolve(anchor) - 1).trim());
    }

    private <T extends AlgorithmTracer> T owner(Class<T> type) {
        var matches = registry.all().stream().filter(type::isInstance).map(type::cast).toList();
        assertEquals(1, matches.size());
        return matches.get(0);
    }

    private ExecutionTrace assertAgrees(AlgorithmTracer tracer, Compiled solution, Map<String, Object> input,
                              String answerVariable, String expected) throws Exception {
        var trace = runner.run(tracer, input);
        assertFalse(trace.isTruncated(), tracer.id() + " " + input);
        assertEquals(AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode(), trace.getCode());
        Object[] arguments = Arrays.stream(solution.entry().getParameters()).map(p ->
                json.convertValue(trace.getResolvedInput().get(p.getName()), p.getType())).toArray();
        Object answer = solution.entry().invoke(solution.instance(), arguments);
        if (tracer instanceof RabinKarpTracer) {
            assertInstanceOf(List.class, answer);
            for (Object position : (List<?>) answer) assertInstanceOf(Integer.class, position);
        }
        String rendered = answer instanceof int[] array ? Arrays.toString(array) : String.valueOf(answer);
        var finalStep = trace.getSteps().get(trace.getStepCount() - 1);
        assertEquals(finalStep.getVariables().get(answerVariable), rendered, tracer.id() + " " + input);
        if (expected != null) assertEquals(expected, rendered, "Independent expected answer: " + tracer.id() + " " + input);
        // Both array transformations are in-place algorithms, not copy-and-sort substitutions.
        if (tracer instanceof NextPermutationTracer || tracer instanceof QuickSortTracer) {
            assertSame(arguments[0], answer);
        }
        return trace;
    }

    private Compiled compile(AlgorithmTracer tracer) throws Exception {
        Path directory = Files.createTempDirectory(temporary, tracer.id());
        Path file = directory.resolve("Solution.java");
        Files.writeString(file, AnnotatedCode.parse(tracer.annotatedCode()).getDisplayCode());
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A Java 17 JDK is required");
        var diagnostics = new ByteArrayOutputStream();
        assertEquals(0, compiler.run(null, null, diagnostics, "--release", "17", "-proc:none", "-parameters",
                "-classpath", directory.toString(), "-d", directory.toString(), file.toString()),
                () -> tracer.id() + " incomplete displayed source:\n" + diagnostics);
        var loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
        try {
            Class<?> type = loader.loadClass("Solution");
            var entries = Arrays.stream(type.getDeclaredMethods()).filter(m ->
                    Modifier.isPublic(m.getModifiers()) && m.getName().equals("solve")).toList();
            assertEquals(1, entries.size(), "One typed public solve entry");
            Method entry = entries.get(0);
            assertNotEquals(Object.class, entry.getReturnType());
            assertNotEquals(void.class, entry.getReturnType());
            assertEquals(tracer.inputSpec().getFields().stream().map(InputField::getName).toList(),
                    Arrays.stream(entry.getParameters()).map(java.lang.reflect.Parameter::getName).toList());
            assertEquals(tracer.inputSpec().getFields().stream().map(field -> switch (field.getType()) {
                case INT -> int.class;
                case INT_ARRAY -> int[].class;
                case INT_GRID -> int[][].class;
                case STRING -> String.class;
                default -> throw new AssertionError("Add an authoritative parameter binding for " + field.getType());
            }).toList(), Arrays.stream(entry.getParameterTypes()).toList());
            return new Compiled(loader, type.getConstructor().newInstance(), entry);
        } catch (Throwable failure) {
            loader.close();
            throw failure;
        }
    }

    private record Compiled(URLClassLoader loader, Object instance, Method entry) implements AutoCloseable {
        @Override public void close() throws Exception { loader.close(); }
    }
}
