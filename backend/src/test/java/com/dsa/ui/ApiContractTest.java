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
        // The detail endpoint is unaffected by legacy execute retirement, so any catalogued
        // id proves the round-trip. It used to need a non-retired id, which is now the empty
        // set: every catalogued problem is traced and every legacy execute route refuses.
        String id = getJson(base + "/problems").get(0).get("id").asText();
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
                arguments("/api/graphs/advanced", "graph-intro"),
                arguments("/api/graphs/advanced", "graph-rep-cpp"),
                arguments("/api/graphs/advanced", "graph-rep-java"),
                arguments("/api/graphs/advanced", "connected-components-intro"),
                arguments("/api/graphs/advanced", "bfs-dfs-intro"),
                arguments("/api/graphs/advanced", "dijkstra-pq-theory"),
                arguments("/api/graphs/advanced", "mst-theory"),
                arguments("/api/recursion-backtracking", "subsets-i"),
                arguments("/api/recursion-backtracking", "combination-sum-i"),
                arguments("/api/recursion-backtracking", "rat-in-a-maze"),
                arguments("/api/recursion-backtracking", "m-coloring"),
                arguments("/api/recursion-backtracking", "palindrome-partitioning"),
                arguments("/api/recursion-backtracking", "permutations"),
                arguments("/api/recursion-backtracking", "word-search"),
                arguments("/api/recursion-backtracking", "atoi-recursive"),
                arguments("/api/recursion-backtracking", "pow-x-n-recursive"),
                arguments("/api/recursion-backtracking", "count-good-numbers"),
                arguments("/api/recursion-backtracking", "sort-stack-recursion"),
                arguments("/api/recursion-backtracking", "reverse-stack-recursion"),
                arguments("/api/recursion-backtracking", "generate-binary-strings"),
                arguments("/api/recursion-backtracking", "generate-parentheses"),
                arguments("/api/recursion-backtracking", "power-set"),
                arguments("/api/recursion-backtracking", "subsequences-patterns-theory"),
                arguments("/api/recursion-backtracking", "count-subsequences-sum-k"),
                arguments("/api/recursion-backtracking", "check-subsequence-sum-k"),
                arguments("/api/recursion-backtracking", "combination-sum-2"),
                arguments("/api/recursion-backtracking", "subsets-2"),
                arguments("/api/recursion-backtracking", "combination-sum-3"),
                arguments("/api/recursion-backtracking", "letter-combinations-phone"),
                arguments("/api/recursion-backtracking", "word-break"),
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
                arguments("/api/graphs/advanced", "cheapest-flights-k-stops"),
                arguments("/api/graphs/advanced", "network-delay-time"),
                arguments("/api/graphs/advanced", "number-of-ways-destination"),
                arguments("/api/graphs/advanced", "min-multiplications-reach-end"),
                arguments("/api/graphs/advanced", "floyd-warshall"),
                arguments("/api/graphs/advanced", "city-smallest-neighbors"),
                arguments("/api/graphs/advanced", "word-ladder-2"),
                arguments("/api/graphs/advanced", "accounts-merge"),
                arguments("/api/graphs/advanced", "number-of-islands-2"),
                arguments("/api/graphs/advanced", "making-large-island"),
                arguments("/api/graphs/advanced", "swim-in-rising-water"),
                arguments("/api/graphs/advanced", "tarjan-bridges"),
                arguments("/api/graphs/advanced", "articulation-points"),
                arguments("/api/binarysearch", "search-rotated-sorted"),
                arguments("/api/binarysearch", "koko-eating-bananas"),
                arguments("/api/binarysearch", "split-array-largest-sum"),
                arguments("/api/binarysearch", "median-2-sorted-arrays"),
                arguments("/api/binarysearch", "kth-element-2-sorted-arrays"),
                arguments("/api/binarysearch", "search-insert-position"),
                arguments("/api/binarysearch", "floor-ceil-sorted-array"),
                arguments("/api/binarysearch", "first-last-occurrence"),
                arguments("/api/binarysearch", "count-occurrences"),
                arguments("/api/binarysearch", "search-rotated-sorted-2"),
                arguments("/api/binarysearch", "count-rotations"),
                arguments("/api/binarysearch", "find-peak-element"),
                arguments("/api/binarysearch", "square-root-number"),
                arguments("/api/binarysearch", "nth-root-number"),
                arguments("/api/binarysearch", "min-days-bouquets"),
                arguments("/api/binarysearch", "smallest-divisor"),
                arguments("/api/binarysearch", "ship-packages-d-days"),
                arguments("/api/binarysearch", "kth-missing-positive"),
                arguments("/api/binarysearch", "painters-partition"),
                arguments("/api/binarysearch", "minimize-max-distance-gas-station"),
                arguments("/api/binarysearch", "row-max-ones"),
                arguments("/api/binarysearch", "search-2d-matrix"),
                arguments("/api/binarysearch", "search-2d-matrix-2"),
                arguments("/api/binarysearch", "find-peak-element-2d"),
                arguments("/api/binarysearch", "matrix-median"),
                arguments("/api/linkedlist", "middle-linked-list"),
                arguments("/api/linkedlist", "intro-singly-ll"),
                arguments("/api/linkedlist", "insert-head-ll"),
                arguments("/api/linkedlist", "delete-head-ll"),
                arguments("/api/linkedlist", "length-ll"),
                arguments("/api/linkedlist", "search-ll"),
                arguments("/api/linkedlist", "intro-doubly-ll"),
                arguments("/api/linkedlist", "insert-head-dll"),
                arguments("/api/linkedlist", "delete-head-dll"),
                arguments("/api/linkedlist", "reverse-dll"),
                arguments("/api/linkedlist", "reverse-ll-recursive"),
                arguments("/api/linkedlist", "detect-loop-linked-list"),
                arguments("/api/linkedlist", "length-of-loop-ll"),
                arguments("/api/linkedlist", "palindrome-ll"),
                arguments("/api/linkedlist", "segregate-odd-even-ll"),
                arguments("/api/linkedlist", "remove-nth-from-back"),
                arguments("/api/linkedlist", "delete-middle-node-ll"),
                arguments("/api/linkedlist", "sort-ll"),
                arguments("/api/linkedlist", "sort-012-ll"),
                arguments("/api/linkedlist", "intersection-point-y-ll"),
                arguments("/api/linkedlist", "add-one-to-number-ll"),
                arguments("/api/linkedlist", "add-two-numbers-ll"),
                arguments("/api/linkedlist", "delete-occurrences-key-dll"),
                arguments("/api/linkedlist", "pairs-given-sum-dll"),
                arguments("/api/linkedlist", "remove-duplicates-sorted-dll"),
                arguments("/api/linkedlist", "rotate-ll"),
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
                arguments("/api/stackqueue", "trapping-rainwater"),
                arguments("/api/stackqueue", "largest-rectangle-histogram"),
                arguments("/api/stackqueue", "next-greater-element-2"),
                arguments("/api/stackqueue", "asteroid-collision"),
                arguments("/api/stackqueue", "balanced-parentheses"),
                arguments("/api/stackqueue", "next-greater-element-1"),
                arguments("/api/stackqueue", "stack-array-impl"),
                arguments("/api/stackqueue", "queue-array-impl"),
                arguments("/api/stackqueue", "stack-queue-impl"),
                arguments("/api/stackqueue", "queue-stack-impl"),
                arguments("/api/stackqueue", "stack-ll-impl"),
                arguments("/api/stackqueue", "queue-ll-impl"),
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
                arguments("/api/trees", "tree-intro"),
                arguments("/api/trees", "tree-rep-java"),
                arguments("/api/trees", "iterative-preorder"),
                arguments("/api/trees", "iterative-inorder"),
                arguments("/api/trees", "postorder-2-stacks"),
                arguments("/api/trees", "postorder-1-stack"),
                arguments("/api/trees", "morris-preorder"),
                arguments("/api/trees", "traversals-in-one-pass"),
                arguments("/api/trees", "pre-post-in-one-traversal"),
                arguments("/api/trees", "top-view-bt"),
                arguments("/api/trees", "bottom-view-bt"),
                arguments("/api/trees", "right-left-view-bt"),
                arguments("/api/trees", "boundary-traversal"),
                arguments("/api/trees", "tree-height"),
                arguments("/api/trees", "tree-balanced"),
                arguments("/api/trees", "tree-diameter"),
                arguments("/api/trees", "symmetric-tree"),
                arguments("/api/trees", "identical-trees"),
                arguments("/api/trees", "children-sum-property"),
                arguments("/api/trees", "max-width-bt"),
                arguments("/api/trees", "unique-bt-requirements"),
                arguments("/api/trees", "count-complete-tree-nodes"),
                arguments("/api/trees", "construct-bt-pre-in"),
                arguments("/api/trees", "construct-bt-post-in"),
                arguments("/api/trees", "flatten-bt-to-ll"),
                arguments("/api/trees", "root-to-leaf-path"),
                arguments("/api/trees", "nodes-distance-k"),
                arguments("/api/trees", "bst-intro"),
                arguments("/api/trees", "bst-search"),
                arguments("/api/trees", "bst-min-max"),
                arguments("/api/trees", "bst-floor"),
                arguments("/api/trees", "bst-lca"),
                arguments("/api/trees", "bst-validate"),
                arguments("/api/trees", "bst-kth-smallest"),
                arguments("/api/trees", "bst-inorder-successor"),
                arguments("/api/trees", "two-sum-bst"),
                arguments("/api/trees", "construct-bst-preorder"),
                arguments("/api/trees", "merge-two-bsts"),
                arguments("/api/trees", "largest-bst-in-bt"),
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
                arguments("/api/greedy", "job-sequencing"),
                arguments("/api/greedy", "valid-parentheses-checker"),
                arguments("/api/greedy", "jump-game-2"),
                arguments("/api/greedy", "candy"),
                arguments("/api/greedy", "shortest-job-first"),
                arguments("/api/greedy", "lru-page-replacement"),
                arguments("/api/greedy", "merge-intervals"),
                arguments("/api/greedy", "non-overlapping-intervals"),
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
                arguments("/api/bitmanipulation", "power-set-bitwise"),
                arguments("/api/bitmanipulation", "intro-bits-tricks"),
                arguments("/api/bitmanipulation", "check-ith-bit-set"),
                arguments("/api/bitmanipulation", "check-number-odd"),
                arguments("/api/bitmanipulation", "set-unset-rightmost-bit"),
                arguments("/api/bitmanipulation", "swap-two-numbers"),
                arguments("/api/bitmanipulation", "divide-two-numbers-bitwise"),
                arguments("/api/bitmanipulation", "min-bit-flips"),
                arguments("/api/bitmanipulation", "print-prime-factors"),
                arguments("/api/bitmanipulation", "divisors-of-number"),
                arguments("/api/bitmanipulation", "count-primes-range-sieve"),
                arguments("/api/bitmanipulation", "prime-factorisation-queries"),
                arguments("/api/heaps", "kth-largest-element"),
                arguments("/api/heaps", "kth-smallest-element"),
                arguments("/api/heaps", "task-scheduler"),
                arguments("/api/heaps", "top-k-frequent-elements"),
                arguments("/api/heaps", "hand-of-straights"),
                arguments("/api/heaps", "min-cost-connect-sticks"),
                arguments("/api/heaps", "median-data-stream"),
                arguments("/api/heaps", "merge-k-sorted-lists"),
                arguments("/api/heaps", "heaps-theory"),
                arguments("/api/heaps", "implement-min-heap"),
                arguments("/api/heaps", "check-min-heap"),
                arguments("/api/heaps", "min-to-max-heap"),
                arguments("/api/heaps", "sort-k-sorted-array"),
                arguments("/api/heaps", "replace-rank-array"),
                arguments("/api/heaps", "design-twitter"),
                arguments("/api/heaps", "kth-largest-stream"),
                arguments("/api/heaps", "maximum-sum-combination"),
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

    @ParameterizedTest(name = "{0}/execute/'{'id'}' is retired for every catalogued id")
    @MethodSource("basePaths")
    @DisplayName("Every legacy execute route refuses: no catalogued id may still be served steps")
    void everyLegacyExecuteRouteIsRetired(String base) throws Exception {
        // All 433 catalogued problems are traced on /api/problems, so no legacy route has a
        // legitimate consumer left. A `default:` that still returns steps serves whatever
        // algorithm that branch happens to hold - the exact defect the tracer layer exists
        // to make impossible. This asserts the refusal for every id the controller claims,
        // so a future id cannot quietly fall through.
        JsonNode catalog = getJson(base + "/problems");
        List<String> stillServed = new ArrayList<>();
        for (JsonNode problem : catalog) {
            String id = problem.get("id").asText();
            int code = mockMvc.perform(get(base + "/execute/" + id))
                    .andReturn().getResponse().getStatus();
            if (code == 200) {
                stillServed.add(id);
            }
        }
        assertTrue(stillServed.isEmpty(),
                base + "/execute still returns 200 - and therefore some other algorithm's"
                        + " steps - for: " + stillServed);
    }
}
