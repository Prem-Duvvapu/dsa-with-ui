package com.dsa.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The v2 API: one catalogue, and problems that run on caller-supplied input. */
@SpringBootTest
@AutoConfigureMockMvc
class ProblemsApiTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    private JsonNode getJson(String url) throws Exception {
        MvcResult r = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        return MAPPER.readTree(r.getResponse().getContentAsByteArray());
    }

    @Test
    @DisplayName("One request returns the whole catalogue, every entry flagged traced or not")
    void catalogueIsSingleAndFlagged() throws Exception {
        JsonNode all = getJson("/api/problems");
        // 440 registrations across 18 services, 7 of which are ids claimed by two
        // services with different content. Pinned so accidental catalogue loss is caught.
        assertEquals(433, all.size(), "catalogue size changed");
        for (JsonNode entry : all) {
            assertTrue(entry.has("traced"), entry.path("id").asText() + " has no traced flag");
            assertFalse(entry.path("id").asText().isBlank());
        }
    }

    @Test
    @DisplayName("Stats report catalogued and traced separately")
    void statsSeparateCataloguedFromTraced() throws Exception {
        JsonNode stats = getJson("/api/problems/stats");
        int catalogued = stats.get("catalogued").asInt();
        int traced = stats.get("traced").asInt();

        assertTrue(catalogued > traced, "coverage should not yet be complete");
        assertEquals(catalogued - traced, stats.get("untraced").asInt());
        assertTrue(stats.get("orphanedTracerIds").isEmpty(),
                "a tracer with no catalogue entry is unreachable: " + stats.get("orphanedTracerIds"));

        // Cross-service id collisions are surfaced rather than hidden; resolving them
        // means moving problems between services, which is Phase 4 work.
        assertEquals(7, stats.get("duplicateIds").size(),
                "duplicate id count changed: " + stats.get("duplicateIds"));
    }

    @Test
    @DisplayName("A traced problem exposes an input spec; an untraced one does not")
    void inputSpecPresentOnlyWhenTraced() throws Exception {
        assertFalse(getJson("/api/problems/two-sum").path("inputSpec").isMissingNode());
        assertTrue(getJson("/api/problems/two-sum").path("traced").asBoolean());

        JsonNode untraced = getJson("/api/problems/heaps-theory");
        assertFalse(untraced.path("traced").asBoolean());
        assertTrue(untraced.path("inputSpec").isNull() || untraced.path("inputSpec").isMissingNode());
    }

    @Test
    @DisplayName("GET execute runs the declared defaults")
    void executeDefaults() throws Exception {
        mockMvc.perform(get("/api/problems/two-sum/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemId", is("two-sum")))
                .andExpect(jsonPath("$.truncated", is(false)))
                .andExpect(jsonPath("$.steps", hasSize(greaterThan(1))))
                .andExpect(jsonPath("$.resolvedInput.target", is(9)));
    }

    @Test
    @DisplayName("POST execute runs the caller's input, not a fixture")
    void executeWithCallerInput() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/problems/two-sum/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nums\":[1,5,9,14],\"target\":23}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedInput.target", is(23)))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("23"), "the trace should mention the caller's target");
        assertFalse(body.contains("[2, 7, 11, 15]"), "the default fixture must not leak in");
    }

    @Test
    @DisplayName("The code returned carries no anchor markers")
    void codeIsStrippedOfAnchors() throws Exception {
        JsonNode trace = getJson("/api/problems/kadane-algo/execute");
        String code = trace.get("code").asText();
        assertFalse(code.contains("@a"), "anchor markers must never reach the code viewer");
        assertTrue(code.contains("maxSubArray"));

        // Every highlighted line must exist in that code.
        int lines = code.split("\n", -1).length;
        for (JsonNode step : trace.get("steps")) {
            int line = step.get("activeLine").asInt();
            assertTrue(line >= 1 && line <= lines, "step highlights line " + line + " of " + lines);
        }
    }

    @Test
    @DisplayName("Invalid input is 400 with a message per field, not a 500")
    void invalidInputIsPerField() throws Exception {
        mockMvc.perform(post("/api/problems/binary-search-1d/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nums\":[9,3,7],\"target\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("invalid_input")))
                .andExpect(jsonPath("$.fieldErrors.nums", containsString("sorted")));
    }

    @Test
    @DisplayName("An oversized input is refused rather than allowed to exhaust the server")
    void oversizedInputRefused() throws Exception {
        StringBuilder huge = new StringBuilder("[");
        for (int i = 0; i < 500; i++) {
            huge.append(i).append(i == 499 ? "" : ",");
        }
        huge.append("]");

        mockMvc.perform(post("/api/problems/kadane-algo/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nums\":" + huge + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nums", notNullValue()));
    }

    @Test
    @DisplayName("An unknown problem is 404; a catalogued but untraced one is 501")
    void missingVersusNotYetTraced() throws Exception {
        mockMvc.perform(get("/api/problems/no-such-problem-at-all/execute"))
                .andExpect(status().isNotFound());

        // Distinguishing these is the point: the UI can say "not yet traced" honestly
        // rather than animating an unrelated algorithm, which is what used to happen.
        mockMvc.perform(get("/api/problems/heaps-theory/execute"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    @DisplayName("Preorder and inorder no longer share one animation")
    void treeTraversalsDiffer() throws Exception {
        String pre = getJson("/api/problems/tree-preorder/execute").get("steps").toString();
        String in = getJson("/api/problems/tree-inorder/execute").get("steps").toString();
        assertNotEquals(pre, in,
                "these were literally the same three hardcoded steps before the rewrite");
    }

    @Test
    @DisplayName("The input spec is fetchable on its own so a client can build a form first")
    void inputSpecEndpoint() throws Exception {
        mockMvc.perform(get("/api/problems/number-of-islands/input-spec"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields[0].name", is("grid")))
                .andExpect(jsonPath("$.fields[0].type", is("INT_GRID")))
                .andExpect(jsonPath("$.maxSteps", greaterThan(0)));
    }

    @Test
    @DisplayName("Legacy per-topic endpoints still work during the migration")
    void legacyEndpointsUnaffected() throws Exception {
        mockMvc.perform(get("/api/arrays/problems")).andExpect(status().isOk());
        mockMvc.perform(get("/api/arrays/execute/two-sum")).andExpect(status().isOk());
    }
}
