package com.dsa.ui.tracer.impl;

import com.dsa.ui.catalog.StatementCatalog;
import com.dsa.ui.tracer.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ParametricInputContractTest {
    @Autowired SmallestDivisorTracer divisor;
    @Autowired MatrixMedianTracer median;
    @Autowired TraceRunner runner;
    @Autowired StatementCatalog statements;
    @Autowired MockMvc http;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void impossibleThresholdIsRejectedBeforeReturningATrace() {
        var error = assertThrows(InputValidationException.class,
                () -> runner.run(divisor, Map.of("nums", List.of(1, 2), "threshold", 1)));
        assertTrue(error.getFieldErrors().containsKey("threshold"));
    }

    @Test
    void impossibleThresholdReturnsAFieldSpecificHttp400() throws Exception {
        http.perform(post("/api/problems/{id}/execute", divisor.id()).contentType("application/json")
                        .content("{\"nums\":[1,2],\"threshold\":1}")
                        .with(r -> { r.setRemoteAddr("divisor-validation"); return r; }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_input"))
                .andExpect(jsonPath("$.fieldErrors.threshold", containsString("number of elements")))
                .andExpect(jsonPath("$.steps").doesNotExist());
    }

    @Test
    void thresholdAtLengthIsValidAndOneBelowIsNotEvenAtTheArrayCap() {
        var nums = java.util.Collections.nCopies(30, 1_000_000);
        var trace = runner.run(divisor, Map.of("nums", nums, "threshold", 30));
        assertEquals("1000000", trace.getSteps().get(trace.getStepCount() - 1).getVariables().get("answer"));
        assertThrows(InputValidationException.class,
                () -> runner.run(divisor, Map.of("nums", nums, "threshold", 29)));
    }

    @Test
    void unsortedRowsAreRejectedRatherThanSilentlyReordered() {
        var input = List.of(List.of(9, 1, 2));
        var error = assertThrows(InputValidationException.class,
                () -> runner.run(median, Map.of("matrix", input)));
        assertTrue(error.getFieldErrors().containsKey("matrix"));
        assertEquals(List.of(List.of(9, 1, 2)), input);
    }

    @Test
    void unsortedRowsReturnAFieldSpecificHttp400() throws Exception {
        http.perform(post("/api/problems/{id}/execute", median.id()).contentType("application/json")
                        .content("{\"matrix\":[[1,3,5],[2,9,6]]}")
                        .with(r -> { r.setRemoteAddr("median-validation"); return r; }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_input"))
                .andExpect(jsonPath("$.fieldErrors.matrix", containsString("sorted")))
                .andExpect(jsonPath("$.steps").doesNotExist());
    }

    @Test
    void nonemptyRectangularGridValidationStillApplies() throws Exception {
        for (var matrix : List.of(List.of(), List.of(List.of()), List.of(List.of(1, 2), List.of(3)))) {
            http.perform(post("/api/problems/{id}/execute", median.id()).contentType("application/json")
                            .content(json.writeValueAsString(Map.of("matrix", matrix)))
                            .with(r -> { r.setRemoteAddr("median-grid-validation"); return r; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.matrix").exists());
        }
    }

    @Test
    void evenGridLowerMedianExtensionIsPreservedAndExplainedInServedMetadata() throws Exception {
        var trace = runner.runDefaults(median);
        assertEquals("18", trace.getSteps().get(trace.getStepCount() - 1).getVariables().get("answer"));
        var statement = statements.find(median.id()).orElseThrow();
        assertTrue(String.join(" ", statement.statement()).contains("Visualizer extension"));
        assertTrue(String.join(" ", statement.statement()).contains("lower"));
        var response = http.perform(get("/api/problems/{id}", median.id())).andExpect(status().isOk()).andReturn();
        var detail = json.readTree(response.getResponse().getContentAsByteArray());
        var fields = detail.path("inputSpec").path("fields");
        assertEquals(1, fields.size());
        assertEquals("matrix", fields.get(0).path("name").asText());
        assertTrue(fields.get(0).path("help").asText().contains("lower median"));
        assertTrue(fields.get(0).path("help").asText().contains("even"));
        assertTrue(detail.path("statement").toString().contains("Visualizer extension"));
    }

    @Test
    void everyDivisorStepCarriesItsActualBoundsAndOnlyProbesCarryMid() {
        for (var input : List.of(Map.<String, Object>of(), divisor.alternateInput(),
                Map.<String, Object>of("nums", List.of(1), "threshold", 1))) {
            var trace = runner.run(divisor, input);
            var code = AnnotatedCode.parse(divisor.annotatedCode());
            int low = 1;
            int high = ((List<?>) trace.getResolvedInput().get("nums")).stream()
                    .mapToInt(x -> ((Number) x).intValue()).max().orElseThrow();
            int mid = -1;
            for (var step : trace.getSteps()) {
                var variables = step.getVariables();
                String anchor = code.getAnchors().entrySet().stream()
                        .filter(e -> e.getValue() == step.getActiveLine()).map(Map.Entry::getKey).findFirst().orElseThrow();
                if (anchor.equals("mid")) mid = low + (high - low) / 2;
                if (anchor.equals("feasible")) high = mid - 1;
                if (anchor.equals("infeasible")) low = mid + 1;
                assertEquals(String.valueOf(low), variables.get("low"), anchor + " low");
                assertEquals(String.valueOf(high), variables.get("high"), anchor + " high");
                if (anchor.equals("mid") || anchor.equals("tally")) {
                    assertEquals(String.valueOf(mid), variables.get("mid"), anchor + " mid");
                    assertTrue(mid >= low && mid <= high);
                } else {
                    assertFalse(variables.containsKey("mid"), anchor + " must not retain a retired probe");
                }
            }
            assertTrue(low > high, "Terminal bounds must show an exhausted search interval");
        }
    }
}
