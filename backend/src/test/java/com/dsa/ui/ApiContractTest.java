package com.dsa.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One contract, enforced across every controller.
 *
 * <p>The 18 controllers were written in three copy-pasted variants that had silently
 * diverged: eight of them dropped the 404 guard on {@code /execute/{id}}, so an unknown
 * id returned HTTP 200 with some other algorithm's steps (whatever the service's
 * {@code default:} branch happened to return). Testing one controller by hand is what
 * let that happen, so this test is parameterized over all of them.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    /** Every base path the application exposes. Keep in sync with the @RequestMapping values. */
    static Stream<String> basePaths() {
        return Stream.of(
                "/api/arrays",
                "/api/graphs/bfs-dfs",
                "/api/graphs/advanced",
                "/api/trees",
                "/api/tries",
                "/api/sorting",
                "/api/binarysearch",
                "/api/dp",
                "/api/greedy",
                "/api/heaps",
                "/api/linkedlist",
                "/api/recursion-backtracking",
                "/api/slidingwindow",
                "/api/stackqueue",
                "/api/strings",
                "/api/bitmanipulation",
                "/api/maths",
                "/api/basic-recursion"
        );
    }

    private JsonNode getJson(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        return MAPPER.readTree(result.getResponse().getContentAsByteArray());
    }

    private String firstProblemId(String base) throws Exception {
        return getJson(base + "/problems").get(0).get("id").asText();
    }

    @ParameterizedTest(name = "{0}/problems returns a non-empty catalog")
    @MethodSource("basePaths")
    @DisplayName("Every controller serves a non-empty catalog with the required fields")
    void catalogIsNonEmptyAndWellFormed(String base) throws Exception {
        JsonNode catalog = getJson(base + "/problems");

        assertTrue(catalog.isArray(), base + " must return a JSON array");
        assertTrue(catalog.size() > 0, base + " returned an empty catalog");

        for (JsonNode problem : catalog) {
            String id = problem.path("id").asText(null);
            assertNotNull(id, base + " has a catalog entry with no id");
            assertFalse(id.isBlank(), base + " has a catalog entry with a blank id");
            assertFalse(problem.path("title").asText("").isBlank(),
                    base + " entry '" + id + "' has no title");
            assertFalse(problem.path("category").asText("").isBlank(),
                    base + " entry '" + id + "' has no category");
        }
    }

    @ParameterizedTest(name = "{0}/problems/'{'id'}' round-trips")
    @MethodSource("basePaths")
    @DisplayName("A valid id returns that same problem")
    void detailReturnsTheRequestedProblem(String base) throws Exception {
        String id = firstProblemId(base);
        JsonNode problem = getJson(base + "/problems/" + id);
        assertEquals(id, problem.get("id").asText(),
                base + "/problems/" + id + " returned a different problem");
    }

    @ParameterizedTest(name = "{0}/problems/'{'unknown'}' is 404")
    @MethodSource("basePaths")
    @DisplayName("An unknown id is 404 on the detail endpoint")
    void detailRejectsUnknownId(String base) throws Exception {
        mockMvc.perform(get(base + "/problems/definitely-not-a-real-problem-id"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "{0}/execute/'{'id'}' returns a well-formed trace")
    @MethodSource("basePaths")
    @DisplayName("Execution steps are non-empty and sequentially numbered from 1")
    void executeReturnsWellFormedSteps(String base) throws Exception {
        String id = firstProblemId(base);
        JsonNode steps = getJson(base + "/execute/" + id);

        assertTrue(steps.isArray(), base + "/execute/" + id + " must return a JSON array");
        assertTrue(steps.size() > 0, base + "/execute/" + id + " returned no steps");

        for (int i = 0; i < steps.size(); i++) {
            JsonNode step = steps.get(i);
            assertEquals(i + 1, step.path("stepNumber").asInt(),
                    base + "/execute/" + id + " step " + i + " is not sequentially numbered");
            assertFalse(step.path("description").asText("").isBlank(),
                    base + "/execute/" + id + " step " + (i + 1) + " has no description");
        }
    }

    /**
     * The regression guard. Before this, eight controllers answered 200 here and streamed
     * back an unrelated algorithm's animation.
     */
    @ParameterizedTest(name = "{0}/execute/'{'unknown'}' is 404, never another problem's steps")
    @MethodSource("basePaths")
    @DisplayName("An unknown id is 404 on execute — never a fallback trace")
    void executeRejectsUnknownIdInsteadOfFallingBack(String base) throws Exception {
        mockMvc.perform(get(base + "/execute/definitely-not-a-real-problem-id"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "{0} ids are unique")
    @MethodSource("basePaths")
    @DisplayName("A controller never lists the same problem id twice")
    void catalogIdsAreUniqueWithinAController(String base) throws Exception {
        JsonNode catalog = getJson(base + "/problems");
        // Top-level ids only — nested GraphNode/TreeNode/ListNode payloads also carry an "id".
        List<String> ids = new ArrayList<>();
        catalog.forEach(problem -> ids.add(problem.path("id").asText()));
        assertEquals(ids.size(), ids.stream().distinct().count(),
                base + " lists duplicate problem ids");
    }
}
