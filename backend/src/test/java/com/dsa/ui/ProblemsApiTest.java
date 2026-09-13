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
        assertEquals(431, all.size(), "catalogue size changed");
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

        assertEquals(catalogued, traced, "every catalogue entry should now have a tracer");
        assertEquals(0, stats.get("untraced").asInt());
        assertTrue(stats.get("orphanedTracerIds").isEmpty(),
                "a tracer with no catalogue entry is unreachable: " + stats.get("orphanedTracerIds"));

        // Cross-service id collisions are surfaced rather than hidden; resolving them
        // means moving problems between services, which is Phase 4 work.
        // Zero, and it must stay zero. Seven ids were registered twice and four more problems
        // were registered under word-order variants of the same name; all eleven came from
        // AdvancedGraphService and GraphBfsDfsService cataloguing the same problems. Both
        // now live in one "Graphs" topic, which removes the cause rather than the symptom.
        assertEquals(0, stats.get("duplicateIds").size(),
                "a problem is registered twice again: " + stats.get("duplicateIds"));
    }

    @Test
    @DisplayName("Completed catalogue entries expose input specs")
    void inputSpecPresentOnlyWhenTraced() throws Exception {
        assertFalse(getJson("/api/problems/two-sum").path("inputSpec").isMissingNode());
        assertTrue(getJson("/api/problems/two-sum").path("traced").asBoolean());

        JsonNode completed = getJson("/api/problems/longest-common-subsequence");
        assertTrue(completed.path("traced").asBoolean());
        assertFalse(completed.path("inputSpec").isMissingNode());
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
    @DisplayName("Detail exposes the tracer's alternate input, so the UI can offer it")
    void detailCarriesAlternateInput() throws Exception {
        // Every tracer must declare a materially different second input - the contract makes
        // it abstract so none can skip it, and alternateInputDiffersFromDefaults rejects one
        // pasted from the spec defaults. All 431 of them existed only for the test suite:
        // nothing served it, so the only input a visitor could reach was the default, and
        // every branch that only the alternate reaches was permanently unvisitable. The
        // code panel marks those branches as not taken; this is what lets someone go take
        // them.
        JsonNode detail = getJson("/api/problems/next-permutation");
        JsonNode alternate = detail.get("alternateInput");

        assertNotNull(alternate, "a traced problem must expose its alternate input");
        assertTrue(alternate.has("nums"), "alternate input carries the tracer's own field names");

        // And it must run, which is the whole point of offering it.
        mockMvc.perform(post("/api/problems/next-permutation/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(alternate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps", not(empty())));
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
    @DisplayName("An unknown problem is 404 after tracer coverage is complete")
    void missingVersusNotYetTraced() throws Exception {
        mockMvc.perform(get("/api/problems/no-such-problem-at-all/execute"))
                .andExpect(status().isNotFound());
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
    @DisplayName("The legacy per-topic routes are gone, not merely retired")
    void legacyRoutesNoLongerExist() throws Exception {
        // The eighteen legacy controllers are deleted. They answered 410 for a while after
        // every id was traced; now the routes themselves do not exist, so Spring 404s. This
        // asserts the deletion rather than leaving the migration's last step untested - if a
        // legacy controller were ever reintroduced, this is what would catch it.
        mockMvc.perform(get("/api/arrays/problems")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/dp/execute/longest-common-subsequence"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/graphs/bfs-dfs/execute/number-of-islands"))
                .andExpect(status().isNotFound());
    }
}
