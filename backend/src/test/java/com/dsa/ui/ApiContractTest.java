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
            "two-sum", "kadane-algo",
            "union-sorted-arrays", "longest-subarray-sum-k",
            "print-max-subarray", "rearrange-by-sign", "longest-consecutive-sequence",
            "set-matrix-zeroes", "rotate-matrix-90", "spiral-matrix",
            "count-subarrays-given-sum", "pascals-triangle", "majority-element-ii",
            "largest-subarray-sum-0", "count-subarrays-xor-k", "merge-intervals", "max-product-subarray",
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
            "flattening-ll", "clone-ll-random-pointer",
            "tree-burn-time", "vertical-order-traversal",
            "morris-inorder", "correct-bst-swap",
            "bst-insert", "bst-delete", "bst-floor-ceil",
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
            "word-ladder-1", "alien-dictionary",
            "lru-cache", "ninja-and-his-friends",
            "implement-trie", "word-break-trie",
            "bfs-traversal", "dfs-traversal", "number-of-provinces", "rotting-oranges",
            "undirected-cycle-bfs", "undirected-cycle-dfs", "directed-cycle-dfs",
            "distance-nearest-1",
            "jump-game-1", "assign-cookies", "fractional-knapsack",
            "lemonade-change", "minimum-platforms", "insert-interval",
            "selection-sort", "bubble-sort", "insertion-sort", "merge-sort", "quick-sort",
            "single-number-1", "check-power-of-2", "count-set-bits",
            "xor-numbers-in-range", "single-number-3", "pow-x-n-math",
            "kth-largest-element", "kth-smallest-element", "task-scheduler", "top-k-frequent-elements",
            "print-1-to-n", "print-n-to-1", "sum-first-n", "factorial-number",
            "reverse-array-recursion", "palindrome-string-recursion", "fibonacci-recursion",
            "count-digits", "reverse-number", "palindrome-number", "gcd-two-numbers",
            "armstrong-check", "print-divisors", "check-prime",
            "hand-of-straights", "min-cost-connect-sticks", "median-data-stream", "merge-k-sorted-lists",
            "fruit-into-baskets", "longest-repeating-character-replacement",
            "minimum-window-substring", "subarrays-k-different-integers",
            "longest-substring-without-repeating", "max-consecutive-ones-3",
            "binary-subarrays-with-sum", "count-nice-subarrays",
            "number-substrings-all-three-chars", "maximum-points-cards",
            "longest-substring-k-distinct", "minimum-window-subsequence",
            "kahn-algo-bfs", "cycle-directed-bfs", "disjoint-set-dsu",
            "cycle-undirected-bfs", "cycle-undirected-dfs", "bipartite-graph-dfs", "cycle-directed-dfs",
            "topo-sort-dfs", "course-schedule-1", "course-schedule-2", "find-eventual-safe-states",
            "shortest-path-undirected", "shortest-path-dag", "shortest-path-binary-maze", "path-min-effort",
            "prims-mst", "kruskals-mst", "network-connected-ops", "most-stones-removed",
            "num-provinces", "connected-matrix", "rotten-oranges", "flood-fill",
            "nearest-cell-1", "surrounded-regions", "number-of-enclaves");

    /**
     * Returns empty when every catalogued id for this base is retired - Sorting is the
     * first controller to fully migrate, so {@code /api/sorting} has no legacy id left to
     * exercise. That is a real, permanent state for a fully-migrated controller, not a bug.
     */
    private java.util.Optional<String> firstProblemId(String base) throws Exception {
        JsonNode catalog = getJson(base + "/problems");
        for (JsonNode problem : catalog) {
            String id = problem.get("id").asText();
            if (!RETIRED_IDS.contains(id)) {
                return java.util.Optional.of(id);   // a retired id answers 410, so it cannot prove the execute path
            }
        }
        return java.util.Optional.empty();
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
        java.util.Optional<String> maybeId = firstProblemId(base);
        org.junit.jupiter.api.Assumptions.assumeTrue(maybeId.isPresent(),
                base + " has no non-retired problem left - every catalogued id is fully migrated");
        String id = maybeId.get();
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
        java.util.Optional<String> maybeId = firstProblemId(base);
        org.junit.jupiter.api.Assumptions.assumeTrue(maybeId.isPresent(),
                base + " has no non-retired problem left - every catalogued id is fully migrated");
        String id = maybeId.get();
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
                arguments("/api/graphs/advanced", "kahn-algo-bfs"),
                arguments("/api/graphs/advanced", "cycle-directed-bfs"),
                arguments("/api/graphs/advanced", "disjoint-set-dsu"),
                arguments("/api/graphs/advanced", "cycle-undirected-bfs"),
                arguments("/api/graphs/advanced", "cycle-undirected-dfs"),
                arguments("/api/graphs/advanced", "bipartite-graph-dfs"),
                arguments("/api/graphs/advanced", "cycle-directed-dfs"),
                arguments("/api/graphs/advanced", "topo-sort-dfs"),
                arguments("/api/graphs/advanced", "course-schedule-1"),
                arguments("/api/graphs/advanced", "course-schedule-2"),
                arguments("/api/graphs/advanced", "find-eventual-safe-states"),
                arguments("/api/graphs/advanced", "shortest-path-undirected"),
                arguments("/api/graphs/advanced", "shortest-path-dag"),
                arguments("/api/graphs/advanced", "shortest-path-binary-maze"),
                arguments("/api/graphs/advanced", "path-min-effort"),
                arguments("/api/graphs/advanced", "prims-mst"),
                arguments("/api/graphs/advanced", "kruskals-mst"),
                arguments("/api/graphs/advanced", "network-connected-ops"),
                arguments("/api/graphs/advanced", "most-stones-removed"),
                arguments("/api/graphs/advanced", "num-provinces"),
                arguments("/api/graphs/advanced", "connected-matrix"),
                arguments("/api/graphs/advanced", "rotten-oranges"),
                arguments("/api/graphs/advanced", "flood-fill"),
                arguments("/api/graphs/advanced", "nearest-cell-1"),
                arguments("/api/graphs/advanced", "surrounded-regions"),
                arguments("/api/graphs/advanced", "number-of-enclaves"),
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
                arguments("/api/linkedlist", "flattening-ll"),
                arguments("/api/linkedlist", "clone-ll-random-pointer"),
                arguments("/api/trees", "tree-burn-time"),
                arguments("/api/trees", "vertical-order-traversal"),
                arguments("/api/trees", "morris-inorder"),
                arguments("/api/trees", "correct-bst-swap"),
                arguments("/api/trees", "bst-insert"),
                arguments("/api/trees", "bst-delete"),
                arguments("/api/trees", "bst-floor-ceil"),
                arguments("/api/dp", "minimum-coins-dp"),
                arguments("/api/dp", "coin-change-2"),
                arguments("/api/dp", "edit-distance"),
                arguments("/api/dp", "wildcard-matching"),
                arguments("/api/graphs/advanced", "word-ladder-1"),
                arguments("/api/graphs/advanced", "alien-dictionary"),
                arguments("/api/stackqueue", "lru-cache"),
                arguments("/api/dp", "ninja-and-his-friends"),
                arguments("/api/tries", "implement-trie"),
                arguments("/api/tries", "word-break-trie"),
                arguments("/api/graphs/bfs-dfs", "bfs-traversal"),
                arguments("/api/graphs/bfs-dfs", "dfs-traversal"),
                arguments("/api/graphs/bfs-dfs", "number-of-provinces"),
                arguments("/api/graphs/bfs-dfs", "rotting-oranges"),
                arguments("/api/graphs/bfs-dfs", "undirected-cycle-bfs"),
                arguments("/api/graphs/bfs-dfs", "undirected-cycle-dfs"),
                arguments("/api/graphs/bfs-dfs", "directed-cycle-dfs"),
                arguments("/api/graphs/bfs-dfs", "distance-nearest-1"),
                arguments("/api/greedy", "jump-game-1"),
                arguments("/api/greedy", "assign-cookies"),
                arguments("/api/greedy", "fractional-knapsack"),
                arguments("/api/greedy", "lemonade-change"),
                arguments("/api/greedy", "minimum-platforms"),
                arguments("/api/greedy", "insert-interval"),
                arguments("/api/sorting", "selection-sort"),
                arguments("/api/sorting", "bubble-sort"),
                arguments("/api/sorting", "insertion-sort"),
                arguments("/api/sorting", "merge-sort"),
                arguments("/api/sorting", "quick-sort"),
                arguments("/api/bitmanipulation", "single-number-1"),
                arguments("/api/bitmanipulation", "check-power-of-2"),
                arguments("/api/bitmanipulation", "count-set-bits"),
                arguments("/api/bitmanipulation", "xor-numbers-in-range"),
                arguments("/api/bitmanipulation", "single-number-3"),
                arguments("/api/bitmanipulation", "pow-x-n-math"),
                arguments("/api/heaps", "kth-largest-element"),
                arguments("/api/heaps", "kth-smallest-element"),
                arguments("/api/heaps", "task-scheduler"),
                arguments("/api/heaps", "top-k-frequent-elements"),
                arguments("/api/heaps", "hand-of-straights"),
                arguments("/api/heaps", "min-cost-connect-sticks"),
                arguments("/api/heaps", "median-data-stream"),
                arguments("/api/heaps", "merge-k-sorted-lists"),
                arguments("/api/slidingwindow", "fruit-into-baskets"),
                arguments("/api/slidingwindow", "longest-repeating-character-replacement"),
                arguments("/api/slidingwindow", "minimum-window-substring"),
                arguments("/api/slidingwindow", "subarrays-k-different-integers"),
                arguments("/api/basic-recursion", "print-1-to-n"),
                arguments("/api/basic-recursion", "print-n-to-1"),
                arguments("/api/basic-recursion", "sum-first-n"),
                arguments("/api/basic-recursion", "factorial-number"),
                arguments("/api/basic-recursion", "reverse-array-recursion"),
                arguments("/api/basic-recursion", "palindrome-string-recursion"),
                arguments("/api/basic-recursion", "fibonacci-recursion"),
                arguments("/api/maths", "count-digits"),
                arguments("/api/maths", "reverse-number"),
                arguments("/api/maths", "palindrome-number"),
                arguments("/api/maths", "gcd-two-numbers"),
                arguments("/api/maths", "armstrong-check"),
                arguments("/api/maths", "print-divisors"),
                arguments("/api/maths", "check-prime"));
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
