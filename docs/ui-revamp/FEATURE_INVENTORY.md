# Feature inventory and preservation contract

Status: source-inspected baseline, 2026-09-23. Browser verification is pending. Every row below is an acceptance obligation, not a statement that the redesign already implements it.

## 1. Source evidence

| Area | Current source |
| --- | --- |
| Application orchestration and fixed layout | [App.jsx](../../frontend/src/App.jsx), [App.module.css](../../frontend/src/App.module.css) |
| Routes | [AppRouter.jsx](../../frontend/src/AppRouter.jsx) |
| Search and categories | [Sidebar.jsx](../../frontend/src/components/Sidebar.jsx), [SearchBox.jsx](../../frontend/src/components/SearchBox.jsx), [search directory](../../frontend/src/search) |
| Execution lifecycle | [useTrace.js](../../frontend/src/hooks/useTrace.js) |
| Editable inputs | [InputPanel.jsx](../../frontend/src/components/InputPanel.jsx), [input directory](../../frontend/src/input) |
| Renderer dispatch | [registry.js](../../frontend/src/canvas/registry.js) |
| Companion selection | [companions.js](../../frontend/src/canvas/companions.js) |
| Controls and history | [Controls.jsx](../../frontend/src/components/Controls.jsx), [CaptureStrip.jsx](../../frontend/src/components/CaptureStrip.jsx) |
| Code and inspection | [CodeViewer.jsx](../../frontend/src/components/CodeViewer.jsx), [MemoryComplexityCard.jsx](../../frontend/src/components/MemoryComplexityCard.jsx) |
| Tokens and canvas styles | [index.css](../../frontend/src/index.css), [designTokens.test.js](../../frontend/src/designTokens.test.js) |
| Regression and build gates | [App.integration.test.jsx](../../frontend/src/App.integration.test.jsx), [CI workflow](../../.github/workflows/ci.yml) |

The README reports 433 catalogued and traced problems. This planning pass did not query the running API to revalidate those counts. Use the live stats endpoint during the baseline audit; derive UI counts from actual catalogue data.

## 2. Feature destination matrix

| ID | Existing capability | Redesign destination | Required proof |
| --- | --- | --- | --- |
| F01 | Catalogue, category normalization, title/difficulty/type metadata | Library and quick switcher | Every returned unique ID is reachable; counts match the current filter scope |
| F02 | Ranked search and matched-text highlighting | Shared search logic, library results, switcher | Existing scoring fixtures remain valid; keyboard and pointer select the same result |
| F03 | Category selection and runnable-only filter | Library filter bar; switcher scope controls | Combined filters work; clearing a category does not silently clear the query |
| F04 | Recent searches, bounded local storage, denied-storage handling | Search surface | Existing entries remain usable; storage failure does not break search |
| F05 | Ctrl/Cmd+K and arrow/Enter search navigation | Global switcher command; library search | One handler owns the shortcut; hidden inputs never receive focus |
| F06 | Direct `/problem/:id` navigation | Same route, optional view query | Existing links open the intended problem; refresh works through the hosting setup |
| F07 | Catalogue loading, retry, duplicate-ID protection | Library and workspace status | Slow/error responses do not erase valid context; duplicates do not create duplicate entries |
| F08 | Default execution and detail fetch | Shared problem session | Exactly one logical selection initiates the intended fetch pair; superseded results cannot replace the current problem |
| F09 | Custom Run action | Playground input editor | Submitted values determine the Java trace; run identity and visible input summary agree |
| F10 | Randomize and restore defaults | Input editor | Bounds and semantics remain respected; neither action silently executes a run |
| F11 | Field help, bounds, server validation | Input editor | Errors attach to their fields and remain readable; rejected input preserves the previous valid run |
| F12 | Play/pause, previous/next, reset, seek, speed | Shared playback bar in each view | Every view drives the same selected step; disabled states and zero-step behavior are correct |
| F13 | Space, arrows, R keyboard playback | Workspace keyboard scope | Typing, editing, tabs, dialogs, and code selection do not accidentally control playback |
| F14 | Active-step narration and progress | Beside stage/playback; compact context in Analysis | Text, highlighted source, selected history item, and structure agree on one step |
| F15 | Primary canvas and state legend | Visualization stage | Every registry mapping still renders; no unknown type silently becomes an array |
| F16 | Auxiliary queue/grid views | Stable companion section | Panels remain present through empty steps if the run uses them; missing grid snapshots retain defined prior state |
| F17 | Execution capture and click-to-seek | History disclosure | Capture remains available where meaningful; selection updates the actual run |
| F18 | Java source and active-line highlighting | Code walkthrough and optional split | Source remains the backend's displayed code; highlight remains anchor-derived |
| F19 | Variables, call stack, stack/queue contents | Analysis and contextual companion panels | Objects, nulls, empty containers, and current/front/back/top markers stay interpretable |
| F20 | Time/space metadata and explanations | Analysis | Missing values remain unavailable; no invented Big-O or simulated benchmark claims |
| F21 | Error boundary and explicit unavailable/malformed/empty trace states | Stage and recovery actions | A renderer failure is contained; changing problem can recover |
| F22 | Trace truncation warning | Persistent run status | Incomplete execution is never described as a completed result |
| F23 | Checked-in offline examples when network execution fails | Explicitly labeled sample mode | Only verified sample inputs are described as sample results; a failed custom run is not presented as a successful sample run |
| F24 | Sidebar/bottom-panel collapse intent | Quick switcher, focused views, Focus visualization | All formerly hidden capabilities are still discoverable; ordinary use no longer requires layout repair |
| F25 | Theme tokens and reduced-motion behavior | Shared theme controls and semantic canvas tokens | Both themes and system mode preserve state meanings and readable contrast |

Track these IDs in implementation handoffs and the release ledger. A changed location is acceptable; lost behavior is not.

## 3. Input editor coverage

`InputPanel` currently dispatches seven field-type names to five editor forms. Preserve both editing and serialization semantics.

| Contract type | Editor behavior to retain | Stress case |
| --- | --- | --- |
| INT | Numeric bounds and editable value | Minimum/maximum, temporary empty text, invalid integer |
| STRING | Text, maximum length, help | Long operation sequence, whitespace, validation failure |
| INT_ARRAY | Add/remove/update numeric elements | Length bounds, negative/duplicate values |
| LINKED_LIST | Numeric sequence through array editor | Single node and maximum supported sequence |
| BINARY_TREE | Nullable array slots | Sparse tree; null must not silently become zero |
| INT_GRID | Row/column editing | Rectangular dimensions, bounded overflow, invalid row |
| GRAPH | Node/edge editing | Weighted edge, isolated node, invalid endpoint |

The redesign must handle an input specification arriving after the page mounts. Initialize defaults once the matching specification is available, without overwriting an existing user draft during an unrelated rerender.

## 4. Renderer coverage

The inspected registry contains **17 dsType keys mapped to 12 distinct renderer components/variants**. That is a source-level count; it differs from older prose in the README. Recheck the registry when implementation begins.

| dsType keys | Current renderer | Layout requirements |
| --- | --- | --- |
| Array, Window, SearchSpace, String, Bits, PriorityQueue | ArrayCanvas | Indices, values, active ranges/pointers, and labels remain legible; scrolling is local for wide sequences |
| Matrix | GridCanvas | Preserve board/grid semantics and axes; companion queue must not compress cells into illegibility |
| DpTable | DpTableCanvas | Row/column labels, active cell, predecessors, recurrence information, and slice context remain visible |
| Tree | TreeCanvas | Large and skewed trees need bounded navigation; preserve edge/node relationships |
| Graph | GraphCanvas | Labels, direction/weights, and companion queue; verify disconnected and dense cases |
| LinkedList | LinkedListCanvas | Next/prev, cycle, child, and random-pointer relationships; long values and long lists |
| Stack | StackCanvas | Top marker, operation direction, empty state; optional grid companion |
| Queue | QueueHeroCanvas | Front/back markers, FIFO order, empty state |
| Trie | TrieCanvas | Shared-prefix branches and terminal-word markers |
| RecursionTree | RecursionTreeCanvas | Branch growth, current frame, completed/backtracked state |
| Dsu | DsuCanvas | Group/parent relationships and changed connection |
| Interval | IntervalCanvas | Aligned interval bounds, overlap and result states |

Current companions are queue views for Graph/Matrix and a grid view for Stack when the trace supplies one. Presence is derived from the full run, not each frame, so stepping does not make panels flicker in and out. Retain that behavior.

Do not derive semantics from problem-title matching. The registry, trace fields, and input contracts remain authoritative.

## 5. Audit findings and boundaries

- The current application root redirects to Two Sum. A library landing page is new behavior.
- The search hook exposes only the first 50 results as `visible`. Full-library access requires a deliberate continuation mechanism; copying the sidebar into a larger page is insufficient.
- Catalogue fetching in `App` depends on the active problem through its callback. Investigate repeated catalogue requests when selecting problems; preserve stale-response protections during extraction.
- Input values are local to `InputPanel`. Conditional remounting across views could erase edits. State ownership must change or mounting must be preserved deliberately.
- Existing CSS supports theme variants; a complete user-facing theme preference controller is proposed, not established by these token definitions.
- The frontend contains explicit, labeled offline fallback behavior, despite broader “no fallback” wording about backend tracer dispatch in the README. Keep those two behaviors distinct.
- Existing tests often expect the always-visible sidebar. Update those journeys to open the new switcher or library; keep their request-order and selection assertions.
- Historical visualization plans include future dedicated renderers and other fidelity work. This redesign must not claim those features are implemented by moving existing components.

## 6. Baseline audit output

R0-01 records current routes, catalogue counts, representative IDs, known failures, and screenshots at 390×844, 768×1024, 1366×768, and 1440×900. Add a 320px width and 200% zoom check. Record theme, browser, viewport, commit, and whether the backend or a fixture supplied the trace.

R0-02 maps every F-ID to its new surface, work item, and verification. All rows begin **pending redesign verification**. A test or screenshot reference, not a checkmark without evidence, closes an obligation.
