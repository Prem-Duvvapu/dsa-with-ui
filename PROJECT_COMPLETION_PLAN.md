# DSA Visualizer Project Completion Plan

## Purpose

This document is the execution handoff for completing the project from the state of `main` after PRs #122, #126, #124, and #125. It is intended to be actionable by another coding agent without relying on prior chat context.

The primary objective is to replace every remaining catalogue-only or legacy demonstration route with a real, caller-driven tracer while preserving API contracts, visualization fidelity, and regression coverage. Each algorithm topic must be completed in exactly one topic PR, in ascending order of remaining problem count.

## Authoritative baseline

Baseline commit: `8335f598ba072248ee25ed52e46a3a19ee8fb463`

The values below were read from a live `GET /api/problems/stats` and `GET /api/problems` run against that commit on 2026-09-08.

| Metric | Current | Completion target |
| --- | ---: | ---: |
| Unique catalogued problems | 433 | 433, unless an intentional catalogue change is documented |
| Registered real tracers | 307 | 433 |
| Untraced problems | 126 | 0 |
| Duplicate catalogue IDs | 7 | 0 |
| Orphaned tracer IDs | 0 | 0 |

### Topic status

| Delivery order | Topic | Traced | Total | Remaining | Status |
| ---: | --- | ---: | ---: | ---: | --- |
| — | Advanced Graphs | 53 | 53 | 0 | Complete |
| — | Arrays | 40 | 40 | 0 | Complete |
| — | Binary Trees | 38 | 38 | 0 | Complete |
| — | BST | 16 | 16 | 0 | Complete |
| — | Graph BFS/DFS | 7 | 7 | 0 | Complete |
| — | Greedy Algorithms | 14 | 14 | 0 | Complete |
| — | Heaps & PriorityQueue | 17 | 17 | 0 | Complete |
| — | Learn the Basics | 14 | 14 | 0 | Complete |
| — | Sliding Window | 12 | 12 | 0 | Complete |
| — | Sorting Algorithms | 5 | 5 | 0 | Complete |
| — | Stack & Queue | 30 | 30 | 0 | Complete |
| — | Tries & Prefixes | 2 | 2 | 0 | Complete |
| — | Bit Manipulation | 18 | 18 | 0 | Complete |
| 2 | Strings | 4 | 24 | 20 | Next |
| 3 | Binary Search | 12 | 32 | 20 | Pending |
| 4 | Recursion & Backtracking | 4 | 25 | 21 | Pending |
| 5 | Linked List | 5 | 31 | 26 | Pending |
| 6 | Dynamic Programming | 28 | 55 | 27 | Pending |

Strings precedes Binary Search for the 20-item tie. Do not reorder topics unless the backlog is re-audited and the user explicitly changes the rule.

## Recently completed work

- Advanced Graph foundations were completed in PR #122.
- Greedy Algorithms reached 14/14 real tracers in PR #126. PR #123 was automatically closed when its stacked base branch was deleted and was superseded by #126.
- Frontend routing tests and interval visualization reliability were fixed in PR #124.
- Heaps & PriorityQueue reached 17/17 real tracers in PR #125.
- The current backend suite passed 4,538 tests before the final Heap merge. The Heap-focused contract run passed 2,856 checks, and golden regeneration passed 308 tests. GitHub CI passed backend, frontend/build, and security checks on every merged PR.
- Bit Manipulation reached 18/18 real tracers in Topic PR 1: all 12 remaining IDs (`power-set-bitwise`, `intro-bits-tricks`, `check-ith-bit-set`, `check-number-odd`, `set-unset-rightmost-bit`, `swap-two-numbers`, `divide-two-numbers-bitwise`, `min-bit-flips`, `print-prime-factors`, `divisors-of-number`, `count-primes-range-sieve`, `prime-factorisation-queries`) now have real `AlgorithmTracer`s. The full backend suite (4,832 tests) and frontend suite (220 tests) plus `vite build` passed. `ProblemsApiTest`'s canonical untraced example moved from `intro-bits-tricks` to `bracket-reversals` (Strings, still untraced).
- Current catalogue integrity is 433 unique IDs, 319 tracer-backed IDs, no orphan tracers, and seven recorded duplicate providers.

## Non-negotiable delivery rules

1. Start every topic branch from freshly pulled `main`; do not stack topic PRs.
2. Deliver exactly one PR per topic. Medium-sized logical commits inside that PR are encouraged.
3. Complete and merge the current topic before starting the next topic.
4. Keep unrelated refactors out of topic PRs. Use the post-topic cleanup PRs described later.
5. Never delete a PR base branch while another PR is based on it. Prefer unstacked PRs to eliminate this risk.
6. Preserve user changes and existing golden fixtures. Do not bulk-normalize line endings.
7. A topic is not complete merely because an endpoint returns steps. Every listed ID must meet the real-tracer contract below.

## Real-tracer definition of done

Every new tracer must:

- Be a Spring-discovered `AlgorithmTracer` with a unique, catalogue-matching `id()`.
- Declare the truthful `DsType` and emit a compatible payload on every step.
- Declare an `InputSpec` with useful defaults, constraints, labels, and help text.
- Use caller input rather than fixed fixture data.
- Produce meaningfully different traces for valid alternate input.
- Validate malformed, missing, oversized, and semantically invalid input through the shared validation path.
- Trace the actual named algorithm; it must not forward to a generic or unrelated legacy generator.
- Emit stable, monotonically numbered steps with valid source-line anchors.
- Provide enough state to teach the algorithm: active indices/nodes, decisions, intermediate state, variables, and a clear final result.
- Remain inside `maxSteps` and `maxBytes` for all allowed inputs.
- Have a reviewed golden fixture generated from its default input.
- Pass registry, metadata, payload, encoding, endpoint, and alternate-input contracts.

For theory/introduction lessons, use a small executable demonstration that exposes the concept through input. A static slideshow is not a substitute for a caller-driven tracer.

## Standard changes required in every topic PR

Apply this checklist once for the entire topic, not as separate PRs:

- Add all missing tracer classes under `backend/src/main/java/com/dsa/ui/tracer/impl/`.
- Prefer shared helpers when at least three tracers need the same non-trivial operation, but keep helper APIs narrow.
- Update the topic service metadata so every catalogue `dsType` equals its tracer `dsType()`.
- Retire each migrated ID in the topic service's legacy `generateSteps` switch with `LegacyTraceRetiredException`.
- Remove obsolete legacy generators only when no remaining ID calls them. Never leave a misleading default fallback serving unrelated steps.
- Update the topic service test's retired-ID set and route assertions.
- Update `ApiContractTest` retired IDs and endpoint route cases.
- Move `ProblemsApiTest`'s canonical untraced example to the next topic in this document. In the final topic PR, replace that assertion with an all-traced assertion.
- Add exactly one new golden JSON file per new tracer under `backend/src/test/resources/golden/`.
- Update README, HANDOFF, and PROJECT_CONTEXT counts in the same PR. Counts must come from the live stats endpoint, not manual arithmetic alone.
- Update frontend rendering or tests inside the topic PR if a new truthful `DsType` or payload shape requires it.
- Include the full ID list, before/after count, test evidence, and known limitations in the PR description.

## Topic PR 1 — Bit Manipulation, 12 remaining

Branch suggestion: `feat/bit-manipulation-trace-completion`

Expected result after merge: 319/433 traced, 114 untraced.

| ID | Problem | Recommended input and trace focus |
| --- | --- | --- |
| `power-set-bitwise` | Power Set Bit Manipulation | `nums`; enumerate masks, inspect each bit, accumulate every subset. |
| `intro-bits-tricks` | Introduction to Bits and Tricks | `n`, optional second operand and shift; demonstrate AND, OR, XOR, NOT, left/right shift with aligned binary state. |
| `check-ith-bit-set` | Check if i-th Bit is Set or Not | `n`, `i`; construct mask and show AND result. Validate the bit index. |
| `check-number-odd` | Check if Number is Odd or Not | `n`; inspect the least-significant bit. |
| `set-unset-rightmost-bit` | Set / Unset Rightmost Unset Bit | `n`, explicit operation; show mask formation and before/after binary state. Clarify the catalogue title's set/unset ambiguity. |
| `swap-two-numbers` | Swap Two Numbers Using XOR | `a`, `b`; show all three XOR mutations and preserve signed integer behavior. |
| `divide-two-numbers-bitwise` | Divide Two Numbers Without `*` or `/` | `dividend`, `divisor`; trace sign handling and descending shifts. Reject zero divisor and handle integer overflow deliberately. |
| `min-bit-flips` | Minimum Bit Flips to Convert Number | `start`, `goal`; show XOR and set-bit removal/counting. |
| `print-prime-factors` | Print Prime Factors of a Number | `n`; trace trial divisors and repeated division. Define behavior for `n < 2`. |
| `divisors-of-number` | Print All Divisors of a Number | `n`; trace factor pairs up to square root and return sorted divisors. |
| `count-primes-range-sieve` | Count Primes with Sieve of Eratosthenes | `n`; show composite marking and final count. Constrain `n` to a visualization-safe maximum. |
| `prime-factorisation-queries` | Prime Factorisation of a Number | query values or a single `n`; build/use an SPF array and trace repeated lookup. Make the input model match the final title/description. |

Implementation notes:

- Use `DsType.BITS` for scalar binary tracks and `DsType.ARRAY` for subsets, factors, divisors, and sieve state.
- Reuse bit formatting and unsigned-width calculation so negative inputs do not generate misleading tracks.
- Retire every legacy case above; the current legacy methods are fixed-data demonstrations and are not acceptable fallbacks.
- Confirm that `single-number-1`, `check-power-of-2`, `count-set-bits`, `xor-numbers-in-range`, `single-number-3`, and `pow-x-n-math` remain green.

## Topic PR 2 — Strings, 20 remaining

Branch suggestion: `feat/string-trace-completion`

Expected result after merge: 339/433 traced, 94 untraced.

| ID | Problem | Recommended input and trace focus |
| --- | --- | --- |
| `bracket-reversals` | Minimum Bracket Reversals | brace string; cancellation/stack and reversal count; reject odd length or non-brace input. |
| `count-and-say` | Count and Say | `n`; show run grouping and each generated term. |
| `string-hashing-theory` | String Hashing Theory | text, base, modulus; trace rolling prefix hash construction and collision caveat. |
| `rabin-karp-algo` | Rabin–Karp Algorithm | text and pattern; initial hashes, rolling updates, candidate verification, match indices. |
| `count-palindromic-subsequences` | Count Palindromic Subsequences | string; interval DP transitions with a matrix payload and clearly stated counting semantics. |
| `valid-anagram` | Valid Anagram | `s`, `t`; frequency delta construction and final comparison. |
| `remove-outermost-parentheses` | Remove Outermost Parentheses | valid parenthesis string; depth changes and appended characters. |
| `reverse-words-string` | Reverse Words in a String | string; token normalization and reverse traversal. |
| `largest-odd-number-string` | Largest Odd Number in String | digit string; scan from right and select the longest valid prefix. |
| `longest-common-prefix` | Longest Common Prefix | string list; compare columns or shrink the candidate prefix. |
| `isomorphic-strings` | Isomorphic Strings | `s`, `t`; two-way mapping and first conflict. |
| `rotate-string` | Rotate String | `s`, `goal`; length check and rotation/match search. |
| `sort-characters-frequency` | Sort Characters by Frequency | string; frequency table and priority/bucket extraction. |
| `max-nesting-depth-parentheses` | Maximum Nesting Depth | string; depth increment/decrement and maximum update. |
| `roman-to-integer` | Roman to Integer | Roman string; additive/subtractive decisions and validation policy. |
| `string-to-integer-atoi` | String to Integer (`atoi`) | raw string; whitespace, sign, digits, and clamping states. |
| `count-substrings-k-distinct` | Count Substrings with K Distinct Characters | string and `k`; trace `atMost(k) - atMost(k-1)` sliding windows. |
| `longest-palindromic-substring` | Longest Palindromic Substring | string; center expansion or DP, with current best boundaries. |
| `sum-beauty-all-substrings` | Sum of Beauty of All Substrings | string; extend each start, update frequencies, add max-minus-min-positive frequency. |
| `reverse-every-word` | Reverse Every Word in String | string; preserve a documented whitespace policy while reversing each token. |

Implementation notes:

- Introduce or use a truthful string visualization payload rather than labelling character state as an integer array when avoidable.
- Use `DsType.MATRIX` for interval-DP state and an existing character/string-compatible type for scans.
- `longest-common-prefix` is currently a duplicate catalogue ID; implement against the canonical record returned by `/api/problems/{id}` and resolve provider duplication in the later catalogue-integrity PR.
- Preserve the four existing string tracers and add alternate-input cases for Unicode policy, empty strings, whitespace, and malformed constrained alphabets.

## Topic PR 3 — Binary Search, 20 remaining

Branch suggestion: `feat/binary-search-trace-completion`

Expected result after merge: 359/433 traced, 74 untraced.

| ID | Problem | Recommended input and trace focus |
| --- | --- | --- |
| `search-insert-position` | Search Insert Position | sorted `nums`, `target`; lower-bound invariant. |
| `floor-ceil-sorted-array` | Floor and Ceil in Sorted Array | sorted `nums`, `target`; track both candidates. |
| `first-last-occurrence` | First and Last Occurrence | sorted `nums`, `target`; two bound searches. |
| `count-occurrences` | Count Occurrences in Sorted Array | sorted `nums`, `target`; derive count from bounds. |
| `search-rotated-sorted-2` | Search in Rotated Sorted Array II | rotated `nums`, `target`; duplicate-boundary shrink and sorted-half choice. |
| `count-rotations` | Find How Many Times Array Is Rotated | rotated sorted `nums`; minimum-index search. |
| `find-peak-element` | Find Peak Element | `nums`; slope-based half selection. |
| `square-root-number` | Integer Square Root | non-negative `n`; answer-space search with overflow-safe comparison. |
| `nth-root-number` | Find Nth Root | value and root degree; bounded multiplication and exact/not-found result. |
| `min-days-bouquets` | Minimum Days for M Bouquets | bloom days, `m`, `k`; feasibility scan and answer-space search. |
| `smallest-divisor` | Smallest Divisor Given Threshold | positive `nums`, threshold; ceiling-sum feasibility. |
| `ship-packages-d-days` | Capacity to Ship Packages Within D Days | weights, days; capacity feasibility. |
| `kth-missing-positive` | Kth Missing Positive Number | increasing positive array, `k`; missing-count invariant. |
| `painters-partition` | Painter's Partition Problem | boards and painters; maximum-load feasibility. |
| `minimize-max-distance-gas-station` | Minimize Maximum Gas-Station Distance | sorted positions and `k`; numeric binary search with precision/iteration policy. |
| `row-max-ones` | Row with Maximum 1s | sorted binary matrix; per-row lower bound and best row. |
| `search-2d-matrix` | Search in a 2D Matrix | globally ordered matrix and target; flattened binary search. |
| `search-2d-matrix-2` | Search in 2D Matrix II | row/column sorted matrix and target; staircase elimination. |
| `find-peak-element-2d` | Find Peak Element II | matrix; column maximum and horizontal comparisons. |
| `matrix-median` | Matrix Median | row-wise sorted matrix; value-space search using upper bounds. |

Implementation notes:

- Share overflow-safe midpoint, ceiling division, and feasibility helpers where this improves correctness without hiding trace decisions.
- Validate sortedness/rotation assumptions explicitly or document that validation rejects invalid inputs.
- Emit matrices for 2D problems and arrays for 1D/answer-space problems; always expose low, mid, high, predicate result, and current answer.
- Test duplicates, empty/one-element inputs, impossible allocation, and numeric precision boundaries.

## Topic PR 4 — Recursion & Backtracking, 21 remaining

Branch suggestion: `feat/recursion-backtracking-trace-completion`

Expected result after merge: 380/433 traced, 53 untraced.

| ID | Problem | Core trace |
| --- | --- | --- |
| `rat-in-a-maze` | Rat in a Maze | matrix path exploration, visited cells, backtracking, all valid paths. |
| `m-coloring` | M-Coloring Problem | graph assignments, safety checks, undo decisions. |
| `palindrome-partitioning` | Palindrome Partitioning | substring choice, palindrome check, partition stack, emitted solutions. |
| `permutations` | Permutations of Array / String | choose/swap, recurse, undo, result accumulation. |
| `word-search` | Word Search in 2D Board | matrix position, word index, visited state, four-direction backtracking. |
| `atoi-recursive` | Recursive `atoi()` | whitespace/sign/digit recursion and overflow clamp. |
| `pow-x-n-recursive` | Recursive `pow(x,n)` | exponent halving, returned partial powers, negative exponent handling. |
| `count-good-numbers` | Count Good Numbers | modular exponentiation recursion and parity-position counts. |
| `sort-stack-recursion` | Sort a Stack Using Recursion | pop recursion and sorted insertion unwind. |
| `reverse-stack-recursion` | Reverse a Stack Using Recursion | pop recursion and bottom insertion unwind. |
| `generate-binary-strings` | Binary Strings Without Consecutive 1s | prefix choices and pruning. |
| `generate-parentheses` | Generate Parentheses | open/close counts, prefix stack, pruning. |
| `power-set` | Power Set | include/exclude recursion tree and subset emission. |
| `subsequences-patterns-theory` | Subsequence Patterns | caller-provided array; demonstrate include/exclude, count, and early-exit patterns. |
| `count-subsequences-sum-k` | Count Subsequences with Sum K | index, current sum, include/exclude, returned counts. |
| `check-subsequence-sum-k` | Check Subsequence Sum K Exists | include/exclude and short-circuit success. |
| `combination-sum-2` | Combination Sum II | sorted candidates, duplicate skip, bounded reuse, remaining target. |
| `subsets-2` | Subsets II | sorted values, duplicate skip, subset emission. |
| `combination-sum-3` | Combination Sum III | candidate range, remaining slots/sum, pruning. |
| `letter-combinations-phone` | Phone Letter Combinations | digit index, keypad choices, built string. |
| `word-break` | Word Break | index/memo state, dictionary matches, chosen segmentation. |

Implementation notes:

- Build a consistent recursion-frame representation so enter, choose, recurse, return, and undo are visible rather than represented as arbitrary array mutations.
- Use strict visualization limits for combinatorial inputs. Small defaults and explicit maximum lengths are required.
- Backtracking tracers must show state restoration. A trace that only lists final answers is insufficient.
- Use `Matrix`/`Graph` for maze, board, or coloring state and `Stack` only when it truthfully represents the active recursion/choice stack.
- Test zero-solution, one-solution, multiple-solution, duplicate-value, and pruning cases.

## Topic PR 5 — Linked List, 26 remaining

Branch suggestion: `feat/linked-list-trace-completion`

Expected result after merge: 406/433 traced, 27 untraced.

| ID | Problem |
| --- | --- |
| `middle-linked-list` | Middle of Linked List with Tortoise–Hare |
| `intro-singly-ll` | Introduction to Singly Linked List |
| `insert-head-ll` | Insert at Head of Linked List |
| `delete-head-ll` | Delete Head of Linked List |
| `length-ll` | Length of Linked List |
| `search-ll` | Search in Linked List |
| `intro-doubly-ll` | Introduction to Doubly Linked List |
| `insert-head-dll` | Insert Before Head in Doubly Linked List |
| `delete-head-dll` | Delete Head of Doubly Linked List |
| `reverse-dll` | Reverse a Doubly Linked List |
| `reverse-ll-recursive` | Reverse Linked List Recursively |
| `detect-loop-linked-list` | Detect a Loop in Linked List |
| `length-of-loop-ll` | Length of a Linked-List Loop |
| `palindrome-ll` | Check Whether a Linked List Is a Palindrome |
| `segregate-odd-even-ll` | Segregate Odd and Even Positions |
| `remove-nth-from-back` | Remove Nth Node from the End |
| `delete-middle-node-ll` | Delete the Middle Node |
| `sort-ll` | Sort Linked List with Merge Sort |
| `sort-012-ll` | Sort a 0/1/2 Linked List |
| `intersection-point-y-ll` | Find Intersection of Y-Shaped Lists |
| `add-one-to-number-ll` | Add One to a Number Represented by a List |
| `add-two-numbers-ll` | Add Two Numbers in Linked Lists |
| `delete-occurrences-key-dll` | Delete All Key Occurrences in a DLL |
| `pairs-given-sum-dll` | Find Pairs with Given Sum in a DLL |
| `remove-duplicates-sorted-dll` | Remove Duplicates from a Sorted DLL |
| `rotate-ll` | Rotate a Linked List |

Implementation notes:

- First add/reuse a tested internal linked-list trace builder that assigns stable node IDs and emits forward/backward links without fabricating identity from duplicate values.
- Use array input for ordinary list construction, then visualize `DsType.LINKED_LIST` throughout execution.
- Cyclic-list inputs need `values` plus a validated zero-based `cycleIndex` (`-1` for no cycle). Never traverse cyclic input without a node/step bound.
- Intersection input needs two prefixes plus an explicitly shared tail, not two value-equal but identity-distinct arrays.
- Doubly linked-list traces must keep `next` and `prev` consistent at every emitted step.
- Pointer algorithms should expose named pointers such as `slow`, `fast`, `prev`, `current`, `next`, and dummy nodes.
- Test empty lists, singleton lists, duplicate values, invalid positions, no-cycle/no-intersection cases, carry propagation, and all-node deletion.

## Topic PR 6 — Dynamic Programming, 27 remaining

Branch suggestion: `feat/dynamic-programming-trace-completion`

Expected result after merge: 433/433 traced, 0 untraced.

| ID | Problem | DP family/state |
| --- | --- | --- |
| `longest-common-subsequence` | Longest Common Subsequence | 2D prefix DP. |
| `partition-set-min-abs-diff` | Partition Set Minimum Absolute Difference | subset-sum reachability. |
| `assign-cookies-dp` | Assign Cookies DP | define whether this remains DP or should be reconciled with the canonical greedy lesson; trace the documented recurrence. |
| `target-sum-dp` | Target Sum | sign assignment transformed to subset count. |
| `rod-cutting-problem` | Rod Cutting | unbounded length/value DP. |
| `print-longest-common-subsequence` | Print LCS | LCS table plus reconstruction path. |
| `longest-common-substring` | Longest Common Substring | 2D suffix-length DP and maximum cell. |
| `longest-palindromic-subsequence` | Longest Palindromic Subsequence | LCS with reverse or interval DP. |
| `min-insertions-palindrome` | Minimum Insertions for Palindrome | length minus LPS. |
| `min-insertions-deletions-a-b` | Convert A to B | LCS-derived deletion/insertion counts. |
| `shortest-common-supersequence` | Shortest Common Supersequence | LCS table plus reconstruction. |
| `distinct-subsequences` | Distinct Subsequences | source/target prefix-count DP. |
| `best-time-stock-1` | Stock I | running minimum and best single transaction. |
| `best-time-stock-2` | Stock II | day/buy state or equivalent transition. |
| `best-time-stock-3` | Stock III | day/buy/transaction-cap state. |
| `best-time-stock-4` | Stock IV | caller-provided `k` transaction-cap state. |
| `stock-cooldown` | Stock with Cooldown | buy/sell with day skip. |
| `stock-transaction-fee` | Stock with Transaction Fee | buy/sell with fee transition. |
| `longest-string-chain` | Longest String Chain | sorted words and predecessor DP. |
| `longest-bitonic-subsequence` | Longest Bitonic Subsequence | forward LIS plus reverse LDS. |
| `number-of-lis` | Number of Longest Increasing Subsequences | length and count arrays. |
| `largest-divisible-subset` | Largest Divisible Subset | sorted predecessor DP and reconstruction. |
| `mcm-cost-eval` | Matrix-Chain Cost Evaluation | interval split costs. |
| `evaluate-boolean-expression` | Evaluate Expression to True | interval true/false count DP. |
| `palindrome-partitioning-2` | Palindrome Partitioning II | palindrome preprocessing plus minimum-cut DP. |
| `partition-array-max-sum` | Partition Array for Maximum Sum | prefix partition DP bounded by `k`. |
| `matrix-chain-multiplication-theory` | MCM Theory and Partition Pattern | caller-driven dimensions and interval/split demonstration. |

Implementation notes:

- Prefer a matrix payload for 2D and interval DP. Use arrays for rolling/1D state only when the trace still explains the recurrence.
- Every DP tracer must identify the state meaning, base cases, transition candidates, chosen value, table fill order, and final extraction/reconstruction.
- Keep defaults small enough that each cell update is legible. Set strict dimension/string-length constraints.
- Use numeric types that cannot silently overflow within allowed constraints, especially counting problems and matrix-chain costs.
- Add top-down stack frames only when the selected implementation is memoized recursion; do not show recursion for a tabulation implementation.
- In this final topic PR, change catalogue/API contracts from “known untraced route” assertions to `untraced == 0` and `traced == catalogued`.

## Post-topic PR A — Catalogue integrity and legacy removal

Create this PR only after all six topic PRs are merged.

Resolve the seven duplicate provider registrations reported by `/api/problems/stats`:

| Duplicate ID | Extra provider currently reported | Required decision |
| --- | --- | --- |
| `number-of-islands` | `GraphBfsDfsService` | Keep one canonical metadata owner and remove the duplicate registration without breaking the canonical route. |
| `longest-common-prefix` | `TrieService` | Keep the Strings implementation/catalogue record unless an explicit product decision chooses Trie ownership. |
| `longest-substring-without-repeating` | `StringService` | Keep one canonical metadata owner and preserve the existing tracer ID. |
| `dfs-traversal` | `GraphBfsDfsService` | Keep one canonical graph record. |
| `merge-intervals` | `GreedyService` | Keep one canonical record and route. |
| `flood-fill` | `GraphBfsDfsService` | Keep one canonical graph record. |
| `surrounded-regions` | `GraphBfsDfsService` | Keep one canonical graph record. |

Then:

- Delete unused legacy generator methods, dead imports, and unreachable switch branches across all services.
- Replace every unrelated `default` fallback with an explicit not-found/unsupported error.
- Add a contract that fails if `duplicateIds` or `orphanedTracerIds` is non-empty.
- Add a contract that every catalogue ID maps to exactly one real tracer and every tracer maps to a catalogue record.
- Confirm the unique catalogue remains 433 unless the PR explicitly documents and tests an intentional product change.
- Confirm legacy category endpoints either delegate to the canonical tracer or return the deliberate retired response; none may serve fixture-only steps.

## Post-topic PR B — Frontend visualization and end-to-end completion

- Audit every backend `DsType` against the frontend renderer selection and add a contract/table test for the mapping.
- Add a dedicated PriorityQueue/Heap renderer if the existing array view cannot show heap parent/child relationships and active heap boundaries clearly.
- Ensure Bits, String/character, LinkedList, Matrix, Graph, Stack, Queue, Interval, Tree, and DP table states render their active/visited/final markers.
- Test empty, loading, validation-error, network-error, retry, zero-step, and oversized-response states.
- Add browser-level end-to-end coverage for catalogue load, category filter, search, deep link, input editing, execution, play/pause, seek, speed, restart, and error recovery.
- Verify keyboard operation, focus visibility, labels, color contrast, reduced-motion behavior, and screen-reader text for visualization state.
- Verify responsive layout at phone, tablet, laptop, and wide desktop widths.
- Ensure route navigation does not leak timers, stale requests, or state from the previous problem.
- Keep the frontend test run free of unexpected React `act`, router-future, key, and console errors.

## Post-topic PR C — Repository, test, and build hygiene

- Add or refine `.gitattributes` so golden JSON and source files do not churn between LF and CRLF on Windows/WSL.
- Document that focused and golden-regeneration Maven runs should use `-Djacoco.skip=true` on OneDrive-mounted workspaces; the final CI run must still exercise normal coverage configuration.
- Configure local coverage output outside the OneDrive tree if practical, because JaCoCo HTML writes can dominate otherwise completed test runs.
- Make generated build/coverage artifacts consistently ignored and confirm `git status` stays clean after the full verification suite.
- Add a deterministic script or test that prints the catalogue/tracer/category inventory used by documentation, avoiding future hand-counted status drift.
- Review dependency versions and address security findings through focused upgrades with regression tests.
- Ensure CI caching does not cache generated golden outputs or stale compiled classes in a way that can hide failures.

## Post-topic PR D — Release readiness and documentation

- Update README, HANDOFF, PROJECT_CONTEXT, API examples, and setup instructions to the final live stats.
- Document supported Java, Maven, Node, and npm versions and provide copy/paste development commands.
- Document frontend API-base configuration, backend port/profile configuration, CORS policy, and production environment variables.
- Add/verify a lightweight health endpoint and production smoke check.
- Verify a clean-clone backend build, frontend build, and startup on Linux CI and Windows/WSL.
- Define production packaging/deployment steps, static asset/API routing, and rollback instructions.
- Record known visualization limits and input constraints so users understand why large inputs are rejected.
- Add a release checklist and tag only after all acceptance criteria below pass.

## Verification commands for every topic PR

Run from `backend/` unless stated otherwise. Replace `<TopicServiceTest>` with the relevant service test class.

```bash
mvn test -Djacoco.skip=true -Dtest=TracerContractTest,DsTypePayloadContractTest,CatalogTracerMetadataTest,<TopicServiceTest>,ApiContractTest,ProblemsApiTest
mvn test -Djacoco.skip=true -Dtest=GoldenTraceTest -Dgolden.regenerate=true
mvn test
```

After golden regeneration:

1. Preserve only the new or deliberately changed fixtures.
2. Revert accidental line-ending-only changes to existing fixtures.
3. Inspect every new fixture's final variables/result and several intermediate decision steps.
4. Run the golden test once more without regeneration.

Run from `frontend/`:

```bash
npm test -- --run
npm run build
```

Live smoke audit after starting the backend:

```bash
curl -s http://localhost:8080/api/problems/stats
curl -s http://localhost:8080/api/problems
```

Also execute at least one default and one alternate-input request for every new ID, including `?encoding=delta` where supported. Decode delta output in tests and assert it round-trips to the original steps.

## PR review checklist

- The PR is based on current `main` and contains only one topic or one named post-topic scope.
- Every promised ID has a registered tracer, a truthful catalogue `dsType`, an `InputSpec`, and a golden fixture.
- Default and alternate inputs yield valid, different, algorithm-correct traces.
- Invalid input yields the shared structured validation response.
- No new duplicate or orphan IDs exist.
- No unrelated legacy fallback can answer for a migrated ID.
- Step numbers, line anchors, payload type, markers, variables, and final result are coherent.
- Backend full tests, frontend tests/build, and all GitHub required checks are green.
- Live stats exactly match the expected count for that stage.
- Documentation counts and next-topic pointers match the live API.
- PR description contains test evidence and a rollback/review note for any risky shared helper change.

## Final project acceptance criteria

The implementation phase is complete only when all of the following are true on `main`:

- `GET /api/problems/stats` reports `catalogued: 433`, `traced: 433`, and `untraced: 0`, unless an intentional catalogue-count change has been reviewed and documented.
- `duplicateIds` is empty and `orphanedTracerIds` is empty.
- All 433 catalogue IDs execute a real caller-driven tracer; no ID receives unrelated default or fixture-only steps.
- Every tracer has validated input metadata, alternate-input coverage, correct visualization payloads, and a reviewed golden fixture.
- Delta encoding round-trips all tracer output.
- Backend full tests, frontend full tests, production frontend build, security checks, and clean-clone smoke tests pass.
- Frontend critical journeys pass in browser-level tests and remain usable with keyboard, responsive layouts, and reduced motion.
- The worktree is clean after tests and builds.
- Documentation and deployment instructions match the released application.

## Safe continuation procedure

For the next agent:

1. Read this document, `HANDOFF.md`, `PROJECT_CONTEXT.md`, and the tracer contracts/tests before editing.
2. Fetch and switch to `main`, then run `git pull --ff-only origin main`.
3. Confirm the live stats still equal the baseline. If they differ, regenerate the per-topic inventory before implementation.
4. Begin only Topic PR 1 (Bit Manipulation). Do not start Strings in the same branch or PR.
5. Use medium logical commits: shared support, tracer batches, contract/golden updates, then documentation/counts.
6. Push the topic branch, open one PR, wait for every required check, review live stats, and merge it.
7. Repeat in the exact topic order above.
8. Complete the four post-topic PRs, then apply the final acceptance checklist.

