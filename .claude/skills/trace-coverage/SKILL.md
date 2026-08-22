---
name: trace-coverage
description: >
  Report real animation coverage in the dsa-with-ui repo and decide what to migrate next.
  Use for "how many problems actually work", "what's left to trace", "where are we on the
  migration", "which category should I do next", progress updates, or before editing any
  coverage claim in README.md / HANDOFF.md. Reads the numbers from the code and the running
  API rather than from any document, because every document here is a snapshot that goes
  stale the moment a tracer lands.
---

# Trace coverage

## The distinction that matters

- **Catalogued** — a `ProblemDetail` exists, so the problem appears in the UI.
- **Traced** — an `AlgorithmTracer` exists, so it actually animates its own algorithm.

433 catalogued, 8 traced at the last check. The gap is the project. `traced` is an honesty
flag, not a feature flag: an untraced problem returns **501**, and the UI is meant to say
"not yet traced" rather than animate something else.

**Never quote a coverage number from a document.** `README.md` has historically carried
four different catalogue sizes, none matching the source, and `HANDOFF.md` says of its own
numbers "if those no longer match `GET /api/problems/stats`, treat this file with
suspicion." Run the commands.

---

## Authoritative: the API

```bash
cd backend && mvn spring-boot:run          # separate shell
curl -s http://localhost:8923/api/problems/stats
```

```json
{ "catalogued": 433, "traced": 8, "untraced": 425,
  "duplicateIds": { "flood-fill": ["GraphBfsDfsService"], … 7 total },
  "orphanedTracerIds": [] }
```

`orphanedTracerIds` must stay empty — a tracer with no catalogue entry works but nothing
lists it, so nobody can reach it. `TracerContractTest.noOrphanedTracers` enforces this.

### Per category

```bash
curl -s http://localhost:8923/api/problems | python3 -c "
import json,sys,collections
cat=collections.Counter(); tr=collections.Counter()
for p in json.load(sys.stdin):
    cat[p['category']]+=1
    if p['traced']: tr[p['category']]+=1
for c,n in cat.most_common(): print(f'{tr[c]:>3}/{n:<4} {c}')"
```

Last run:

```
  0/55   Dynamic Programming        0/24   Strings
  1/53   Advanced Graphs            0/18   Bit Manipulation
  2/40   Arrays                     0/17   Heaps & PriorityQueue
  2/38   Binary Trees               0/16   BST
  1/32   Binary Search              0/14   Learn the Basics
  1/31   Linked List                0/14   Greedy Algorithms
  0/30   Stack & Queue              0/12   Sliding Window
  0/25   Recursion & Backtracking   1/7    Graph BFS/DFS
                                    0/5    Sorting Algorithms
                                    0/2    Tries & Prefixes
```

These are **tracer** counts and are lower than the per-category tables in `HANDOFF.md`,
which count *distinct legacy animations*. Two different measurements — say which one you
are quoting.

---

## Offline: no server needed

```bash
# Which problems have a tracer (8 today).
grep -A2 'public String id()' backend/src/main/java/com/dsa/ui/tracer/impl/*.java \
  | grep -oP 'return "\K[^"]+' | sort
```

```bash
# Where the stubs are: one-line delegate generators, by what they delegate to.
grep -rhoP 'private List<ExecutionStep> \w+\(\) \{ return \K\w+' \
     backend/src/main/java/com/dsa/ui/service/ | sort | uniq -c | sort -rn
#      60 generateGraphIntroSteps        (AdvancedGraphService — a 2-step placeholder
#                                         standing in for Dijkstra, Bellman-Ford,
#                                         Floyd-Warshall, Prim, Kruskal, Tarjan, KMP…)
#      31 generateBs1dSteps              (BinarySearchService — a fixed search over
#                                         {1,3,5,7,9,11,13})
#      29 generateReverseSteps           (LinkedListService — a 3-step reverse narration)
#       2 generateClimbingStairsSteps    (DpService)
```

122 delegates in four clusters. **That number must only ever go down.** Each one is a
problem currently playing another algorithm's animation. `dsa-review` re-runs this as a
regression check.

```bash
# Legacy `default:` fallbacks: 18, one per service. Deleted per-service in HANDOFF PROMPT D.
grep -rc "default:" backend/src/main/java/com/dsa/ui/service/ | grep -v ":0" | wc -l
```

Note `grep -c 'problems.put(' backend/src/main/java/com/dsa/ui/service/*.java` **undercounts
badly** (124 vs 433): most entries are bulk-registered inside `for` loops over `String[][]`
arrays. Do not use it as a catalogue count.

---

## Choosing what to migrate next

HANDOFF PROMPT C's order is by leverage — biggest stub cluster first:

1. **Binary Trees & BST** (53 to write) — `tracer/impl/BinaryTreeLayout` already exists
2. **Binary Search** (31) — one search space, many predicates; highly templatable
3. **Advanced Graphs** (60)
4. **Dynamic Programming** (53)
5. **Linked List** (29)
6. **Stack & Queue** (25)
7. **Heaps** (15), **Greedy** (12), **Sliding Window** (9)
8. Deepen **Bit Manipulation** (16) and **Strings** (10)

### Free wins first

Eight classes under `backend/src/main/java/com/dsa/ui/algorithm/` are fully implemented,
already unit-tested, and referenced by **nothing** in `main/`. They emit via the older
`trace/TraceRecorder` + `TraceEvent` path and only need porting to `AlgorithmTracer`:

```bash
ls backend/src/main/java/com/dsa/ui/algorithm/*/
```

`tree/TreePreorderTraversal`, `TreeInorderTraversal`, `TreePostorderTraversal`,
`TreeLevelOrderTraversal`, `graph/DijkstraShortestPath`,
`binarysearch/RotatedSortedArraySearch`, `linkedlist/ReverseLinkedList`,
`greedy/NMeetingsInOneRoom`.

`TreePreorderTracer` and `TreeInorderTracer` already exist as new tracers — reconcile
rather than duplicate, and delete whichever implementation loses.

---

## Reporting the number

- Give **traced / catalogued**, never "problems supported".
- State whether a category count is tracers or legacy distinct animations.
- If you changed the count, `stats.traced` must be **higher than the previous commit and
  never lower**, and the `README.md` coverage table updates in the same commit.
- Moving the pinned `433` in `ProblemsApiTest` or the pinned `7` duplicates is a deliberate
  act that belongs in the commit message. See `dsa-review` §5.
- Say what is *not* done. A category with one tracer out of 53 is 1/53, not "in progress".
