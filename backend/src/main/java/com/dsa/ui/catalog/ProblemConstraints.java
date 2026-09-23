package com.dsa.ui.catalog;

import java.util.List;
import java.util.Map;

/**
 * The ORIGINAL problems' constraints, as their source (LeetCode / GFG / the Striver sheet)
 * states them.
 *
 * <p><strong>These are not the visualiser's input limits.</strong> {@code InputSpec}'s field
 * constraints cap an array at 40 elements so one caller cannot exhaust the step budget; the
 * real problem allows 10^5. Both are true, they differ by orders of magnitude, and conflating
 * them would either lie about what this tool accepts or make the real problem look trivial.
 * They are stored apart and labelled apart in the UI.
 *
 * <p><strong>Absent means absent.</strong> An id with no entry here renders no constraints
 * section at all. That follows the rule the catalogue already applies to missing complexity
 * analysis: show it as unavailable rather than fill it with something plausible. A bound
 * invented from memory is indistinguishable from a real one on screen and strictly worse
 * than silence, so ids are added here only when the constraint is actually known.
 *
 * <p>Populating an id is a one-line edit in this file. {@code ProblemCatalog} applies the map
 * at startup, so nothing else has to change.
 */
public final class ProblemConstraints {

    private ProblemConstraints() {}

    private static final Map<String, List<String>> BY_ID = Map.ofEntries(
            // --- Arrays -------------------------------------------------------------
            Map.entry("two-sum", List.of(
                    "2 <= nums.length <= 10^4",
                    "-10^9 <= nums[i] <= 10^9",
                    "-10^9 <= target <= 10^9",
                    "Exactly one valid answer exists.")),
            Map.entry("three-sum", List.of(
                    "3 <= nums.length <= 3000",
                    "-10^5 <= nums[i] <= 10^5")),
            Map.entry("four-sum", List.of(
                    "1 <= nums.length <= 200",
                    "-10^9 <= nums[i] <= 10^9",
                    "-10^9 <= target <= 10^9")),
            Map.entry("kadane-algo", List.of(
                    "1 <= nums.length <= 10^5",
                    "-10^4 <= nums[i] <= 10^4")),
            Map.entry("max-product-subarray", List.of(
                    "1 <= nums.length <= 2 * 10^4",
                    "-10 <= nums[i] <= 10",
                    "Every prefix/suffix product fits in a 32-bit integer.")),
            Map.entry("majority-element", List.of(
                    "1 <= nums.length <= 5 * 10^4",
                    "-10^9 <= nums[i] <= 10^9",
                    "The majority element always exists.")),
            Map.entry("majority-element-ii", List.of(
                    "1 <= nums.length <= 5 * 10^4",
                    "-10^9 <= nums[i] <= 10^9")),
            Map.entry("next-permutation", List.of(
                    "1 <= nums.length <= 100",
                    "0 <= nums[i] <= 100")),
            Map.entry("sort-0-1-2", List.of(
                    "1 <= nums.length <= 300",
                    "nums[i] is 0, 1 or 2.")),
            Map.entry("stock-buy-sell", List.of(
                    "1 <= prices.length <= 10^5",
                    "0 <= prices[i] <= 10^4")),
            Map.entry("longest-consecutive-sequence", List.of(
                    "0 <= nums.length <= 10^5",
                    "-10^9 <= nums[i] <= 10^9")),
            Map.entry("merge-intervals", List.of(
                    "1 <= intervals.length <= 10^4",
                    "intervals[i].length == 2",
                    "0 <= start_i <= end_i <= 10^4")),
            Map.entry("set-matrix-zeroes", List.of(
                    "1 <= m, n <= 200",
                    "-2^31 <= matrix[i][j] <= 2^31 - 1")),
            Map.entry("rotate-matrix-90", List.of(
                    "n == matrix.length == matrix[i].length",
                    "1 <= n <= 20",
                    "-1000 <= matrix[i][j] <= 1000")),
            Map.entry("spiral-matrix", List.of(
                    "1 <= m, n <= 10",
                    "-100 <= matrix[i][j] <= 100")),
            Map.entry("pascals-triangle", List.of("1 <= numRows <= 30")),

            // --- Binary Search ------------------------------------------------------
            Map.entry("binary-search-1d", List.of(
                    "1 <= nums.length <= 10^4",
                    "-10^4 <= nums[i], target <= 10^4",
                    "nums is sorted in ascending order and all values are distinct.")),
            Map.entry("search-insert-position", List.of(
                    "1 <= nums.length <= 10^4",
                    "-10^4 <= nums[i], target <= 10^4",
                    "nums contains distinct values sorted in ascending order.")),
            Map.entry("search-rotated-sorted", List.of(
                    "1 <= nums.length <= 5000",
                    "-10^4 <= nums[i], target <= 10^4",
                    "All values of nums are unique.")),
            Map.entry("search-rotated-sorted-2", List.of(
                    "1 <= nums.length <= 5000",
                    "-10^4 <= nums[i], target <= 10^4",
                    "nums may contain duplicates.")),
            Map.entry("find-min-rotated-sorted", List.of(
                    "1 <= nums.length <= 5000",
                    "-5000 <= nums[i] <= 5000",
                    "All integers are unique.")),
            Map.entry("single-element-sorted", List.of(
                    "1 <= nums.length <= 10^5",
                    "0 <= nums[i] <= 10^5",
                    "Every element appears twice except one.")),
            Map.entry("find-peak-element", List.of(
                    "1 <= nums.length <= 1000",
                    "-2^31 <= nums[i] <= 2^31 - 1",
                    "nums[i] != nums[i + 1] for all valid i.")),
            Map.entry("koko-eating-bananas", List.of(
                    "1 <= piles.length <= 10^4",
                    "piles.length <= h <= 10^9",
                    "1 <= piles[i] <= 10^9")),
            Map.entry("median-2-sorted-arrays", List.of(
                    "0 <= m, n <= 1000",
                    "1 <= m + n <= 2000",
                    "-10^6 <= nums1[i], nums2[i] <= 10^6")),
            Map.entry("search-2d-matrix", List.of(
                    "1 <= m, n <= 100",
                    "-10^4 <= matrix[i][j], target <= 10^4",
                    "Each row is sorted, and the first value of a row exceeds the last of the previous row.")),
            Map.entry("search-2d-matrix-2", List.of(
                    "1 <= m, n <= 300",
                    "-10^9 <= matrix[i][j], target <= 10^9",
                    "Rows and columns are each sorted in ascending order.")),

            // --- Strings ------------------------------------------------------------
            Map.entry("valid-anagram", List.of(
                    "1 <= s.length, t.length <= 5 * 10^4",
                    "s and t consist of lowercase English letters.")),
            Map.entry("longest-common-prefix", List.of(
                    "1 <= strs.length <= 200",
                    "0 <= strs[i].length <= 200",
                    "strs[i] consists of lowercase English letters.")),
            Map.entry("longest-palindromic-substring", List.of(
                    "1 <= s.length <= 1000",
                    "s consists of digits and English letters.")),
            Map.entry("roman-to-integer", List.of(
                    "1 <= s.length <= 15",
                    "s contains only the characters I, V, X, L, C, D, M.",
                    "s is a valid Roman numeral in the range [1, 3999].")),
            Map.entry("string-to-integer-atoi", List.of(
                    "0 <= s.length <= 200",
                    "s consists of letters, digits, spaces, '+', '-' and '.'.")),

            // --- Sliding Window -----------------------------------------------------
            Map.entry("longest-substring-without-repeating", List.of(
                    "0 <= s.length <= 5 * 10^4",
                    "s consists of English letters, digits, symbols and spaces.")),
            Map.entry("max-consecutive-ones", List.of(
                    "1 <= nums.length <= 10^5",
                    "nums[i] is either 0 or 1.")),

            // --- Linked List --------------------------------------------------------
            Map.entry("reverse-linked-list", List.of(
                    "The list holds between 0 and 5000 nodes.",
                    "-5000 <= Node.val <= 5000")),
            Map.entry("middle-linked-list", List.of(
                    "The list holds between 1 and 100 nodes.",
                    "1 <= Node.val <= 100")),
            Map.entry("detect-loop-linked-list", List.of(
                    "The list holds between 0 and 10^4 nodes.",
                    "-10^5 <= Node.val <= 10^5")),
            Map.entry("add-two-numbers-ll", List.of(
                    "Each list holds between 1 and 100 nodes.",
                    "0 <= Node.val <= 9",
                    "The number has no leading zeros, except the number 0 itself.")),

            // --- Stack & Queue ------------------------------------------------------
            Map.entry("balanced-parentheses", List.of(
                    "1 <= s.length <= 10^4",
                    "s consists only of the characters ()[]{}.")),
            Map.entry("next-greater-element-1", List.of(
                    "1 <= nums1.length <= nums2.length <= 1000",
                    "0 <= nums1[i], nums2[i] <= 10^4",
                    "nums1 is a subset of nums2, and all integers are unique.")),
            Map.entry("largest-rectangle-histogram", List.of(
                    "1 <= heights.length <= 10^5",
                    "0 <= heights[i] <= 10^4")),
            Map.entry("trapping-rainwater", List.of(
                    "1 <= height.length <= 2 * 10^4",
                    "0 <= height[i] <= 10^5")),
            Map.entry("sliding-window-maximum", List.of(
                    "1 <= nums.length <= 10^5",
                    "-10^4 <= nums[i] <= 10^4",
                    "1 <= k <= nums.length")),
            Map.entry("lru-cache", List.of(
                    "1 <= capacity <= 3000",
                    "0 <= key <= 10^4",
                    "0 <= value <= 10^5",
                    "At most 2 * 10^5 calls to get and put.")),

            // --- Binary Trees / BST -------------------------------------------------
            Map.entry("tree-preorder", List.of(
                    "The tree holds between 0 and 100 nodes.",
                    "-100 <= Node.val <= 100")),
            Map.entry("tree-inorder", List.of(
                    "The tree holds between 0 and 100 nodes.",
                    "-100 <= Node.val <= 100")),
            Map.entry("tree-postorder", List.of(
                    "The tree holds between 0 and 100 nodes.",
                    "-100 <= Node.val <= 100")),
            Map.entry("tree-level-order", List.of(
                    "The tree holds between 0 and 2000 nodes.",
                    "-1000 <= Node.val <= 1000")),
            Map.entry("tree-height", List.of(
                    "The tree holds between 0 and 10^4 nodes.",
                    "-100 <= Node.val <= 100")),
            Map.entry("tree-diameter", List.of(
                    "The tree holds between 1 and 10^4 nodes.",
                    "-100 <= Node.val <= 100")),
            Map.entry("tree-balanced", List.of(
                    "The tree holds between 0 and 5000 nodes.",
                    "-10^4 <= Node.val <= 10^4")),
            Map.entry("bst-validate", List.of(
                    "The tree holds between 1 and 10^4 nodes.",
                    "-2^31 <= Node.val <= 2^31 - 1")),
            Map.entry("bst-search", List.of(
                    "The tree holds between 1 and 5000 nodes.",
                    "1 <= Node.val <= 10^7",
                    "1 <= val <= 10^7")),
            Map.entry("bst-kth-smallest", List.of(
                    "The tree holds n nodes, 1 <= k <= n <= 10^4",
                    "0 <= Node.val <= 10^4")),

            // --- Graphs -------------------------------------------------------------
            Map.entry("number-of-islands", List.of(
                    "1 <= m, n <= 300",
                    "grid[i][j] is '0' (water) or '1' (land).")),
            Map.entry("rotting-oranges", List.of(
                    "1 <= m, n <= 10",
                    "grid[i][j] is 0 (empty), 1 (fresh) or 2 (rotten).")),
            Map.entry("flood-fill", List.of(
                    "1 <= m, n <= 50",
                    "0 <= image[i][j], color < 2^16")),
            Map.entry("surrounded-regions", List.of(
                    "1 <= m, n <= 200",
                    "board[i][j] is 'X' or 'O'.")),
            Map.entry("course-schedule-1", List.of(
                    "1 <= numCourses <= 2000",
                    "0 <= prerequisites.length <= 5000",
                    "All prerequisite pairs are distinct.")),
            Map.entry("word-ladder-1", List.of(
                    "1 <= beginWord.length <= 10",
                    "endWord.length == beginWord.length",
                    "1 <= wordList.length <= 5000",
                    "All words are lowercase English letters and distinct.")),

            // --- Dynamic Programming ------------------------------------------------
            Map.entry("climbing-stairs", List.of("1 <= n <= 45")),
            Map.entry("house-robber-2", List.of(
                    "1 <= nums.length <= 100",
                    "0 <= nums[i] <= 1000")),
            Map.entry("grid-unique-paths", List.of(
                    "1 <= m, n <= 100",
                    "The answer is guaranteed to fit in a 32-bit integer.")),
            Map.entry("longest-common-subsequence", List.of(
                    "1 <= text1.length, text2.length <= 1000",
                    "Both strings consist of lowercase English characters.")),
            Map.entry("longest-increasing-subsequence", List.of(
                    "1 <= nums.length <= 2500",
                    "-10^4 <= nums[i] <= 10^4")),
            Map.entry("edit-distance", List.of(
                    "0 <= word1.length, word2.length <= 500",
                    "Both strings consist of lowercase English letters.")),
            Map.entry("coin-change-2", List.of(
                    "1 <= coins.length <= 300",
                    "1 <= coins[i] <= 5000",
                    "0 <= amount <= 5000",
                    "All coin values are distinct.")),

            // --- Heaps / Greedy -----------------------------------------------------
            Map.entry("kth-largest-element", List.of(
                    "1 <= k <= nums.length <= 10^5",
                    "-10^4 <= nums[i] <= 10^4")),
            Map.entry("top-k-frequent-elements", List.of(
                    "1 <= nums.length <= 10^5",
                    "-10^4 <= nums[i] <= 10^4",
                    "k is in the range [1, the number of distinct elements].")),
            Map.entry("merge-k-sorted-lists", List.of(
                    "0 <= k <= 10^4",
                    "0 <= lists[i].length <= 500",
                    "-10^4 <= lists[i][j] <= 10^4")),
            Map.entry("lemonade-change", List.of(
                    "1 <= bills.length <= 10^5",
                    "bills[i] is 5, 10 or 20.")),
            Map.entry("jump-game-1", List.of(
                    "1 <= nums.length <= 10^4",
                    "0 <= nums[i] <= 10^5")),

            // --- Recursion & Backtracking -------------------------------------------
            Map.entry("n-queens", List.of("1 <= n <= 9")),
            Map.entry("sudoku-solver", List.of(
                    "board.length == board[i].length == 9",
                    "board[i][j] is a digit 1-9 or '.'.",
                    "Exactly one solution is guaranteed.")),
            Map.entry("subsets-i", List.of(
                    "1 <= nums.length <= 10",
                    "-10 <= nums[i] <= 10",
                    "All numbers are unique.")),
            Map.entry("permutations", List.of(
                    "1 <= nums.length <= 6",
                    "-10 <= nums[i] <= 10",
                    "All integers are distinct.")),
            Map.entry("combination-sum-i", List.of(
                    "1 <= candidates.length <= 30",
                    "2 <= candidates[i] <= 40",
                    "All candidates are distinct.",
                    "1 <= target <= 40")),
            Map.entry("word-search", List.of(
                    "1 <= m, n <= 6",
                    "1 <= word.length <= 15",
                    "board and word consist of English letters.")),

            // --- Tries --------------------------------------------------------------
            Map.entry("implement-trie", List.of(
                    "1 <= word.length, prefix.length <= 2000",
                    "word and prefix consist only of lowercase English letters.",
                    "At most 3 * 10^4 calls to insert, search and startsWith."))
    );

    /** The source-stated constraints for {@code id}, or an empty list when none are recorded. */
    public static List<String> forId(String id) {
        return BY_ID.getOrDefault(id, List.of());
    }

    /** How many ids currently have recorded constraints. */
    public static int recordedCount() {
        return BY_ID.size();
    }
}
