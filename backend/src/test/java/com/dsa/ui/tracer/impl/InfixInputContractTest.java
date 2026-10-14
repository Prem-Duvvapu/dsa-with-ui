package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InfixInputContractTest {
    @Autowired InfixToPostfixTracer postfix;
    @Autowired InfixToPrefixTracer prefix;
    @Autowired TraceRunner runner;
    @Autowired MockMvc http;
    private final ObjectMapper json = new ObjectMapper();

    static Stream<Arguments> malformed() {
        return converters().flatMap(id -> Stream.of("(A+B", "A+B)", "A++B", "AB", "()", "A+", "+A", "A(B)",
                "(A)B", ")(", "(A+)", "A+()", "((A)", "A^^B", "A/*B", "A+(B)C")
                .map(expression -> Arguments.of(id, expression)));
    }

    static Stream<String> converters() { return Stream.of("postfix", "prefix"); }

    private AlgorithmTracer converter(String kind) { return kind.equals("postfix") ? postfix : prefix; }

    @ParameterizedTest(name = "{0} rejects malformed infix: {1}")
    @MethodSource("malformed")
    void refusesMalformedExpressionsAtTheTrustBoundaryAndBeforeTraceExecution(String kind, String expression) {
        AlgorithmTracer tracer = converter(kind);
        var input = Map.<String, Object>of("expression", expression);
        var error = assertThrows(InputValidationException.class,
                () -> InputValidator.validate(tracer.inputSpec(), input), tracer.id() + " " + expression);
        assertTrue(error.getFieldErrors().containsKey("expression"));
        assertFalse(error.getFieldErrors().get("expression").isBlank());
        assertThrows(InputValidationException.class, () -> runner.run(tracer, input));
    }

    @ParameterizedTest
    @MethodSource("converters")
    void httpRejectsMalformedGrammarWithAnExpressionErrorAndNoSuccessfulTrace(String kind) throws Exception {
        AlgorithmTracer tracer = converter(kind);
        for (String expression : List.of("(A+B", "A+B)", "A++B", "AB", "()")) {
            http.perform(post("/api/problems/{id}/execute", tracer.id()).contentType("application/json")
                            .content(json.writeValueAsString(Map.of("expression", expression)))
                            .with(r -> { r.setRemoteAddr("infix-validation-" + tracer.id()); return r; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_input"))
                    .andExpect(jsonPath("$.fieldErrors.expression").isString())
                    .andExpect(jsonPath("$.steps").doesNotExist());
        }
    }

    @ParameterizedTest
    @MethodSource("converters")
    void servedSpecsDeclareAndExplainTheGrammarWithoutChangingBoundsOrDefaults(String kind) throws Exception {
        AlgorithmTracer tracer = converter(kind);
        var response = http.perform(get("/api/problems/{id}", tracer.id()))
                .andExpect(status().isOk()).andReturn();
        var field = json.readTree(response.getResponse().getContentAsByteArray())
                .path("inputSpec").path("fields").get(0);
        assertEquals("expression", field.path("name").asText());
        assertTrue(field.path("constraints").path("requireInfixExpression").asBoolean());
        assertEquals(1, field.path("constraints").path("minLength").asInt());
        assertEquals(40, field.path("constraints").path("maxLength").asInt());
        String help = field.path("help").asText();
        assertTrue(help.contains("binary"));
        assertTrue(help.contains("balanced"));
        assertTrue(help.contains("implicit multiplication"));
        assertEquals(tracer instanceof InfixToPostfixTracer ? "A+B*C-D" : "A+B*C",
                field.path("defaultValue").asText());
    }

    @Test
    void typeAlphabetLengthUnknownFieldAndDefaultValidationStillApply() {
        for (AlgorithmTracer tracer : List.of(postfix, prefix)) {
            assertDoesNotThrow(() -> InputValidator.defaults(tracer.inputSpec()));
            assertDoesNotThrow(() -> InputValidator.validate(tracer.inputSpec(), tracer.alternateInput()));
            for (Object expression : List.of("", "A" + "+A".repeat(20), "a+b", "A B", "A&B", 123)) {
                assertThrows(InputValidationException.class, () -> InputValidator.validate(tracer.inputSpec(),
                        Map.of("expression", expression)), String.valueOf(expression));
            }
            var errors = assertThrows(InputValidationException.class, () -> InputValidator.validate(tracer.inputSpec(),
                    Map.of("expression", "AB", "unknown", 1))).getFieldErrors();
            assertTrue(errors.containsKey("expression"));
            assertTrue(errors.containsKey("unknown"));
        }
    }

    @Test
    void prefixEqualExponentPopExplainsReversedAssociativityNotAnImpossibleInequality() {
        int popLine = AnnotatedCode.parse(prefix.annotatedCode()).resolve("higherPrec");
        for (String expression : List.of("A^B^C", "(A+B)*(C-D)^E^F")) {
            var trace = runner.run(prefix, Map.of("expression", expression));
            var equalPops = trace.getSteps().stream().filter(step -> step.getActiveLine() == popLine
                    && "^".equals(step.getVariables().get("popped"))
                    && step.getArrayState().stream().anyMatch(cell -> "current".equals(cell.getState())
                            && "^".equals(cell.getLabel()))).toList();
            assertFalse(equalPops.isEmpty(), "The case must exercise an equal-precedence exponent pop");
            for (var step : equalPops) {
                assertTrue(step.getDescription().contains("Equal '^' precedence"), step.getDescription());
                assertTrue(step.getDescription().contains("right-associative"), step.getDescription());
                assertFalse(step.getDescription().contains("(prec 3) < '^' (prec 3)"));
            }
            for (var step : trace.getSteps()) {
                if (step.getActiveLine() == popLine && step.getArrayState().stream().anyMatch(cell ->
                        "current".equals(cell.getState()) && "*".equals(cell.getLabel()))) {
                    assertFalse(step.getDescription().contains("Equal '^' precedence"), step.getDescription());
                }
            }
        }
    }
}
