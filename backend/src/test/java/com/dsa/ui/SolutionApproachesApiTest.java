package com.dsa.ui;

import com.dsa.ui.controller.ProblemsController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SolutionApproachesApiTest {
    @Autowired MockMvc http;
    @Autowired ProblemsController controller;
    private final ObjectMapper json = new ObjectMapper();

    @Test void detailNamesTheActualCanonicalApproach() throws Exception {
        JsonNode detail = getJson("/api/problems/climbing-stairs");
        assertEquals("tabulation", detail.path("defaultApproachId").asText());
        assertEquals("tabulation", detail.path("approachId").asText());
        assertEquals(3, detail.path("approaches").size());
        JsonNode approach = java.util.stream.StreamSupport.stream(detail.path("approaches").spliterator(), false)
                .filter(value -> value.path("id").asText().equals("tabulation")).findFirst().orElseThrow();
        assertEquals("Tabulation", approach.path("label").asText());
        assertEquals("DpTable", approach.path("dsType").asText());
        assertEquals(detail.path("inputSpec"), approach.path("inputSpec"));
        assertEquals(detail.path("complexity"), approach.path("complexity"));
    }

    @Test void explicitDefaultAndOldLinksDescribeTheSameExecution() throws Exception {
        for (String encoding : new String[]{"full", "delta"}) {
            JsonNode implicit = getJson("/api/problems/climbing-stairs/execute?encoding=" + encoding);
            JsonNode explicit = getJson("/api/problems/climbing-stairs/execute?approach=tabulation&encoding=" + encoding);
            assertEquals(implicit, explicit);
            assertEquals("tabulation", explicit.path("approachId").asText());
            assertEquals("DpTable", explicit.path("dsType").asText());
            assertEquals(getJson("/api/problems/climbing-stairs").path("javaCode"), explicit.path("code"));
            assertEquals("O(N)", explicit.path("complexity").path("timeComplexity").asText());
        }
    }

    @Test void unknownApproachIsRefusedEverywhereWithoutRunningDefaults() throws Exception {
        for (String suffix : new String[]{"", "/input-spec", "/execute"}) {
            http.perform(get("/api/problems/climbing-stairs" + suffix).param("approach", "not-real"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("unavailable_approach"))
                    .andExpect(jsonPath("$.availableApproaches").value(org.hamcrest.Matchers.hasItem("tabulation")));
        }
        http.perform(post("/api/problems/climbing-stairs/execute").param("approach", "not-real")
                .contentType(MediaType.APPLICATION_JSON).content("{\"n\":5}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("unavailable_approach"));
    }

    @Test void emptyApproachIsAnErrorRatherThanOmission() throws Exception {
        http.perform(get("/api/problems/climbing-stairs/execute").param("approach", ""))
                .andExpect(status().isBadRequest());
    }

    @Test void unknownProblemStillReturns404EvenWithAnApproach() throws Exception {
        http.perform(get("/api/problems/does-not-exist/execute").param("approach", "tabulation"))
                .andExpect(status().isNotFound());
    }

    @Test void selectedSpecStillRejectsInvalidAndUnknownInputFields() throws Exception {
        http.perform(post("/api/problems/climbing-stairs/execute").param("approach", "tabulation")
                .contentType(MediaType.APPLICATION_JSON).content("{\"n\":31}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_input"));
        http.perform(post("/api/problems/climbing-stairs/execute").param("approach", "tabulation")
                .contentType(MediaType.APPLICATION_JSON).content("{\"n\":5,\"madeUp\":1}"))
                .andExpect(status().isBadRequest());
        assertEquals(getJson("/api/problems/climbing-stairs/input-spec"),
                getJson("/api/problems/climbing-stairs/input-spec?approach=tabulation"));
    }

    @Test void encodingAliasesDoNotMultiplyDefaultCacheEntries() throws Exception {
        JsonNode omitted = getJson("/api/problems/climbing-stairs/execute");
        var field = ProblemsController.class.getDeclaredField("defaultTraces");
        field.setAccessible(true);
        int before = ((Map<?, ?>) field.get(controller)).size();
        assertEquals(omitted, getJson("/api/problems/climbing-stairs/execute?encoding=delta"));
        assertEquals(omitted, getJson("/api/problems/climbing-stairs/execute?encoding=DELTA&approach=tabulation"));
        assertEquals(before, ((Map<?, ?>) field.get(controller)).size());
    }

    @Test void unsupportedEncodingIsRefusedBeforeCaching() throws Exception {
        http.perform(get("/api/problems/climbing-stairs/execute").param("encoding", "not-real"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("unsupported_encoding"));
    }

    @Test void customInputsAreExecutedWithoutEnteringTheDefaultCache() throws Exception {
        var field = ProblemsController.class.getDeclaredField("defaultTraces");
        field.setAccessible(true);
        int before = ((Map<?, ?>) field.get(controller)).size();
        var result = http.perform(post("/api/problems/climbing-stairs/execute").param("approach", "tabulation")
                .contentType(MediaType.APPLICATION_JSON).content("{\"n\":7}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsByteArray());
        assertEquals("tabulation", body.path("approachId").asText());
        assertEquals(7, body.path("resolvedInput").path("n").asInt());
        assertEquals(before, ((Map<?, ?>) field.get(controller)).size());
    }

    private JsonNode getJson(String path) throws Exception {
        return json.readTree(http.perform(get(path)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());
    }
}
