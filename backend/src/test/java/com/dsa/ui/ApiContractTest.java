package com.dsa.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
import static org.junit.jupiter.params.provider.Arguments.arguments;
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

    /** Ids whose legacy trace is retired because a real tracer serves them on /api/problems. */
    private static final List<String> RETIRED_IDS = List.of(
            "tree-preorder", "tree-inorder", "tree-postorder", "tree-level-order",
            "search-rotated-sorted", "n-meetings-in-one-room",
            "climbing-stairs", "frog-jump", "frog-jump-k-distance",
            "max-sum-non-adjacent", "house-robber-2",
            "grid-unique-paths", "unique-paths-2",
            "minimum-falling-path-sum", "triangle-min-path-sum", "ninjas-training",
            "longest-increasing-subsequence", "print-lis", "lis-binary-search",
            "max-rectangle-area-all-ones", "count-square-submatrices",
            "subset-sum-equal-target", "partition-equal-subset-sum",
            "count-subsets-with-sum-k", "count-partitions-given-diff",
            "largest-element", "max-consecutive-ones", "move-zeros-end",
            "find-missing-number", "stock-buy-sell",
            "second-largest-element", "check-sorted-ii", "remove-duplicates-sorted",
            "left-rotate-one", "linear-search",
            "left-rotate-k", "single-number", "majority-element",
            "leaders-in-array", "longest-subarray-sum-k-positives",
            "minimum-coins-dp", "coin-change-2",
            "lower-bound", "upper-bound",
            "count-inversions", "reverse-pairs",
            "sort-0-1-2", "next-permutation",
            "aggressive-cows", "book-allocation",
            "find-min-rotated-sorted", "single-element-sorted",
            "koko-eating-bananas", "split-array-largest-sum",
            "median-2-sorted-arrays", "kth-element-2-sorted-arrays",
            "sliding-window-maximum", "min-stack", "sum-subarray-minimums",
            "reverse-linked-list", "find-starting-point-loop", "reverse-ll-group-k",
            "tree-burn-time", "vertical-order-traversal",
            "morris-inorder", "correct-bst-swap",
            "trapping-rainwater", "largest-rectangle-histogram",
            "next-greater-element-2", "asteroid-collision",
            "matrix-chain-multiplication", "burst-balloons",
            "knapsack-01", "unbounded-knapsack",
            "tree-max-path-sum", "serialize-deserialize-bt",
            "zigzag-traversal", "tree-lca",
            "n-queens", "sudoku-solver",
            "subsets-i", "combination-sum-i",
            "z-function-algo", "kmp-lps-algo",
            "shortest-palindrome", "longest-happy-prefix",
            "repeating-missing-number", "merge-two-sorted-arrays",
            "three-sum", "four-sum",
            "bellman-ford", "kosaraju-scc",
            "edit-distance", "wildcard-matching",
            "lru-cache", "ninja-and-his-friends");

    private String firstProblemId(String base) throws Exception {
        JsonNode catalog = getJson(base + "/problems");
        for (JsonNode problem : catalog) {
            String id = problem.get("id").asText();
            if (!RETIRED_IDS.contains(id)) {
                return id;   // a retired id answers 410, so it cannot prove the execute path
            }
        }
        throw new IllegalStateException(base + " has no non-retired problem to exercise");
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

    /** Ids whose delegate case was deleted after a real tracer landed in tracer/impl. */
    static Stream<Arguments> retiredTraces() {
        return Stream.of(
                arguments("/api/trees", "tree-preorder"),
                arguments("/api/trees", "tree-inorder"),
                arguments("/api/trees", "tree-postorder"),
                arguments("/api/trees", "tree-level-order"),
                arguments("/api/trees", "tree-max-path-sum"),
                arguments("/api/trees", "serialize-deserialize-bt"),
                arguments("/api/trees", "zigzag-traversal"),
                arguments("/api/trees", "tree-lca"),
                arguments("/api/recursion-backtracking", "n-queens"),
                arguments("/api/recursion-backtracking", "sudoku-solver"),
                arguments("/api/graphs/advanced", "z-function-algo"),
                arguments("/api/graphs/advanced", "kmp-lps-algo"),
                arguments("/api/recursion-backtracking", "subsets-i"),
                arguments("/api/recursion-backtracking", "combination-sum-i"),
                arguments("/api/graphs/advanced", "shortest-palindrome"),
                arguments("/api/graphs/advanced", "longest-happy-prefix"),
                arguments("/api/graphs/advanced", "bellman-ford"),
                arguments("/api/graphs/advanced", "kosaraju-scc"),
                arguments("/api/binarysearch", "search-rotated-sorted"),
                arguments("/api/binarysearch", "koko-eating-bananas"),
                arguments("/api/binarysearch", "split-array-largest-sum"),
                arguments("/api/binarysearch", "median-2-sorted-arrays"),
                arguments("/api/binarysearch", "kth-element-2-sorted-arrays"),
                arguments("/api/greedy", "n-meetings-in-one-room"),
                arguments("/api/dp", "climbing-stairs"),
                arguments("/api/dp", "frog-jump"),
                arguments("/api/dp", "frog-jump-k-distance"),
                arguments("/api/dp", "max-sum-non-adjacent"),
                arguments("/api/dp", "house-robber-2"),
                arguments("/api/dp", "grid-unique-paths"),
                arguments("/api/dp", "unique-paths-2"),
                arguments("/api/dp", "minimum-falling-path-sum"),
                arguments("/api/dp", "triangle-min-path-sum"),
                arguments("/api/dp", "ninjas-training"),
                arguments("/api/dp", "longest-increasing-subsequence"),
                arguments("/api/dp", "print-lis"),
                arguments("/api/dp", "lis-binary-search"),
                arguments("/api/dp", "max-rectangle-area-all-ones"),
                arguments("/api/dp", "count-square-submatrices"),
                arguments("/api/dp", "subset-sum-equal-target"),
                arguments("/api/dp", "partition-equal-subset-sum"),
                arguments("/api/dp", "count-subsets-with-sum-k"),
                arguments("/api/dp", "count-partitions-given-diff"),
                arguments("/api/arrays", "largest-element"),
                arguments("/api/arrays", "max-consecutive-ones"),
                arguments("/api/arrays", "move-zeros-end"),
                arguments("/api/arrays", "find-missing-number"),
                arguments("/api/arrays", "stock-buy-sell"),
                arguments("/api/arrays", "second-largest-element"),
                arguments("/api/arrays", "check-sorted-ii"),
                arguments("/api/arrays", "remove-duplicates-sorted"),
                arguments("/api/arrays", "left-rotate-one"),
                arguments("/api/arrays", "linear-search"),
                arguments("/api/arrays", "left-rotate-k"),
                arguments("/api/arrays", "single-number"),
                arguments("/api/arrays", "majority-element"),
                arguments("/api/arrays", "leaders-in-array"),
                arguments("/api/arrays", "longest-subarray-sum-k-positives"),
                arguments("/api/arrays", "repeating-missing-number"),
                arguments("/api/arrays", "merge-two-sorted-arrays"),
                arguments("/api/arrays", "three-sum"),
                arguments("/api/arrays", "four-sum"),
                arguments("/api/stackqueue", "sliding-window-maximum"),
                arguments("/api/stackqueue", "min-stack"),
                arguments("/api/stackqueue", "sum-subarray-minimums"),
                arguments("/api/linkedlist", "reverse-linked-list"),
                arguments("/api/linkedlist", "find-starting-point-loop"),
                arguments("/api/linkedlist", "reverse-ll-group-k"),
                arguments("/api/trees", "tree-burn-time"),
                arguments("/api/trees", "vertical-order-traversal"),
                arguments("/api/trees", "morris-inorder"),
                arguments("/api/trees", "correct-bst-swap"),
                arguments("/api/dp", "minimum-coins-dp"),
                arguments("/api/dp", "coin-change-2"),
                arguments("/api/dp", "edit-distance"),
                arguments("/api/dp", "wildcard-matching"),
                arguments("/api/stackqueue", "lru-cache"),
                arguments("/api/dp", "ninja-and-his-friends"));
    }

    /**
     * The second regression guard. Deleting a migrated id's delegate case makes its
     * switch fall into {@code default:}, which serves whatever generator that service
     * still has — another algorithm's animation under this id's name. A retired trace
     * must be GONE, not substituted.
     */
    @ParameterizedTest(name = "{0}/execute/{1} retired → 410, never a substitute trace")
    @MethodSource("retiredTraces")
    @DisplayName("A migrated id refuses the legacy execute path instead of falling back")
    void retiredIdRefusesTheLegacyExecutePath(String base, String id) throws Exception {
        mockMvc.perform(get(base + "/execute/" + id))
                .andExpect(status().isGone());
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
