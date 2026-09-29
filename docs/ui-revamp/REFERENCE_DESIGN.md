# Reference design — P0 decisions

Date: 2026-09-29. Package: P0. Base: `82f2808`.

This records the P0 design decisions and reference layouts that P2–P4 implement. It
refines [IMPLEMENTATION_HANDOFF.md](IMPLEMENTATION_HANDOFF.md) sections 4–5 and does not
replace them. The layouts are annotated wireframes rather than code prototypes: they
describe structure and sizing, and no fake execution data is involved. Screenshots of the
current product at the same surfaces are in [evidence/p0](evidence/p0).

## What the baseline shows

Measured with Chromium 1.63 against the real backend (see `evidence/p0/measurements.json`):

| Surface | 1366×768 | 390×844 | Gate (handoff §5) |
| --- | --- | --- | --- |
| Library search top | 491px | 549px | ≤360px desktop / ≤320px phone — **fails both** |
| Library first result row | 691–777px (row cut at fold) | below the fold | complete row visible at 1366×768 — **fails** |
| Array stage (Two Sum) | 705×223 at y219 | 362×308 at y223 | stage near y280 with useful height — **height fails** |
| Graph stage (BFS) | 705×359 at y221 | 362×446 at y223 | 440px spatial minimum — **fails on desktop** |
| 2D DP stage (LCS) | 705×355 at y225 | 362×446 at y223 | 440px spatial minimum — **fails on desktop** |
| Page scrolling | fixed 768px shell, no page scroll | fixed 844px shell | natural page scroll — **fails** |
| Horizontal overflow | none | none | none — passes |

The desktop stage gets about 52% of the width: a 240px sidebar and a 370px code column
share the row with it, and the statement, curriculum bar and header sit above. On phones
the canvas is large enough, but code, input and complexity are behind a tab card that
starts closed, and nothing scrolls.

## Visual hierarchy compared with HLD

HLD (`../hld-with-ui`, read-only) does three things well that DSA does not:

1. **One topic, one header.** A `← All modules` link, the eyebrow (`category · level`), a
   title and a single sentence, then one sticky tab rail. DSA stacks an app bar, a
   breadcrumb, a curriculum bar and a statement row before the canvas.
2. **Named activities in a tab rail.** The `role="tablist"` rail, whose views are backed
   by the URL, replaces controls for showing and hiding panels. DSA exposes seven
   show/hide toggles (sidebar, code, input editor, complexity, compare, statement, help).
3. **Paper and surface.** A neutral paper ground, one surface colour for working areas,
   hairline rules and almost no shadow. DSA's workspace still uses `glass-panel` cards
   with shadows and a violet accent, although the library (#145) has already moved to
   the Bench ground/panel/rule tokens.

What HLD does that DSA must **not** copy:

- A 570px hero and 66px module titles. DSA is a tool used for hundreds of problems. The
  title stays at 28–32px and the library intro stays within one line of text plus a search.
- A fixed 335px control column beside the stage. DSA graphs, trees and DP tables need the
  width; input lives below the playback controls instead.
- Five tabs. DSA has three task views, **Playground**, **Code walkthrough** and
  **Analysis**, because those are the three questions a learner asks: what happens, which
  line does it, and what state and complexity result.
- Conditionally mounted page state. HLD's Playground owns its own result, so switching
  tabs loses it. DSA keeps the session above the views (handoff §6).

## Decisions

| # | Decision | Why | Rejected alternative |
| --- | --- | --- | --- |
| D1 | Keep Bench tokens (`--bench-*`, `--probe`, `--settled`). Add semantic aliases only for navigation selection and focus (`--nav-selected-*`, `--focus-ring`) | Execution colours already clear 4.5:1 against both themes (designTokens.test.js); a second palette would fork meaning | Adopting HLD's green/orange palette. Orange would collide with `--probe` ("happening now") |
| D2 | Navigation selection is ink-on-ground (inverted surface), never amber or green | Amber and green are reserved for algorithm state (Bench rule) | Violet accent tab. Violet is `--state-target` on canvases |
| D3 | Workspace width `min(1440px, 100% − 2×gutter)`, gutters 32px desktop, 16px phone. Library keeps 1320px | Handoff §5; more stage width than HLD's 1180px | Full-bleed stage. Line lengths in narration become unreadable |
| D4 | One sticky element: the view rail. Header, title and statement scroll away | Handoff §5; a stacked sticky header plus rail plus dock would eat about 150px of a 768px laptop | Sticky header plus sticky playback. Rejected until a walkthrough proves the need |
| D5 | Stage minimum heights: 360px for sequence families (Array, Bits, String, Window, SearchSpace, Stack, Queue, LinkedList, Interval), 440px for spatial families (Tree, Graph, Matrix, DpTable, Trie, RecursionTree, Dsu, PriorityQueue). Phones use 300px and 340px | Handoff §4.3; derived from the registry key, not the problem title | One universal height. A 5-cell array would float in a 440px box |
| D6 | Library intro is one line: an eyebrow plus a 28px heading and one sentence. The illustration is removed; the "Your learning" disclosure is kept, and a compact **Continue** link sits in the progress line whenever a last-visited problem exists | Meets the ≤360px search gate while keeping F26 discoverable without opening a disclosure | Removing the learning section. That would strand F26 |
| D7 | Phone library: query and a **Filters (N)** disclosure stay visible; active filters appear as removable chips; the same `<select>` controls sit inside the disclosure | Handoff §4.1; one set of controls, not two | A bottom sheet. More code, no benefit at this scale |
| D8 | View rail = manual-activation tabs (arrows move focus; Enter/Space select; roving tabindex) with numbered labels, as in HLD | Handoff §10 accessibility; matches HLD | Automatic activation. Arrow-keying would re-render views on every press |
| D9 | Playback controls keep text labels (`Restart`, `Previous`, `Play`, `Next`) and a labelled speed group. "Reset" is renamed **Restart** so it cannot be confused with **Restore defaults** in the editor | Handoff §4.3: distinct actions need distinct names | Icon-only transport. It fails the discoverability rule in REVIEW.md gate 3 |
| D10 | Code split: available only when the measured container fits a 480px diagram, 360px of source and a 24px gap, i.e. at least 864px of content width. The initial ratio is 60/40, clamped to 45–70%, and persisted as `dsa-ui:codeSplit` `{ v: 1, enabled, ratio }` | Handoff §4.4 | Viewport-width breakpoints. The sidebar/focus state changes the container, not the viewport |

## Reference layouts

### Library, 1366×768

```text
┌ header 64px ─ DSA Visualizer ─────────────────────── [Theme] ┐
│ ALGORITHM LIBRARY                                             │ y≈88
│ Find an algorithm, then watch it run.        (28px, 1 line)   │
│ 12 of 431 watched · 3-day streak · Continue: Two Sum →        │ y≈170
│ ▸ Your learning  (Continue, today's pick, starred)            │
│ [🔍 Search algorithms…                         ⌘/Ctrl K]      │ y≈250 (gate ≤360)
│ Category [▾] Difficulty [▾] Progress [▾] ☐ Runnable  Clear    │
│ [Graphs ×] [Hard ×]                              431 results  │
│ ───────────────────────────────────────────────────────────── │
│ 01  Introduction to Graph            Graphs · Graph   Easy ☆  │ first row fully visible
└───────────────────────────────────────────────────────────────┘
```

### Library, 390×844

```text
header 56px ─ DSA Visualizer ───── [Theme]
ALGORITHM LIBRARY · Find an algorithm…        y≈80
12 of 431 watched · Continue →
[🔍 Search algorithms…              ]          y≈190 (gate ≤320)
[Filters (2)]                  431 results
[Graphs ×] [Hard ×]
───────────────────────────────────────
Introduction to Graph              Easy ☆
Graphs · Graph
```

### Workspace: Playground, 1366×768

```text
← All algorithms                                   [Switch problem ⌘K] [Theme] [Help]
Graphs · Medium · Graph                                                          ☆
BFS Traversal of Graph                                 (28–32px, wraps)
▸ Problem & examples                                ‹ Prev: Cycle…  13 of 19  Next: … ›
┌ sticky rail ─ [01 Playground] [02 Code walkthrough] [03 Analysis] ─────────────────┐ y≈230
Visualization   Graph · Step 4 of 21      [Edit input] [Show code] [Focus]
┌──────────────────────────────── stage ≥440px ───────────────────┬─ Queue ─┐ y≈280
│ legend (one line)                                                │ front→  │
│                 graph (full width minus companion)               │         │
└──────────────────────────────────────────────────────────────────┴─────────┘
Step 4 of 21 — "Visit 2; enqueue its unvisited neighbour 4."      (narration, wraps)
[Restart] [Previous] [Play] [Next]   Speed [0.5× 1× 2× 4×]   ───●──────── seek
Input used: graph {6 vertices, 6 edges} · start 0          Changes not run • [Copy link]
                                   (page scrolls)
Try your own input ─────────────────────────────────────────────────────────────────
Schema-driven fields with help and errors
[Run input] [Other case] [Randomize] [Restore defaults]   Saved inputs · Save current
▸ Execution history (step 4 of 21)                        [Compare other case]
```

### Workspace: Code walkthrough

- Container ≥864px: diagram | separator | source, 60/40, one narration and controls row below.
- Container <864px (phones): `[Diagram] [Source]` sub-toggle above one pane; the narration
  and controls are shared and the step does not move.

```text
[01 Playground] [02 Code walkthrough] [03 Analysis]
Diagram | Source                      (phone subview toggle)
┌ Source · Java ─────────── line 9 · [Follow execution ✓] ┐
│  8   for (int next : adj.get(node)) {                     │
│▶ 9     if (!seen[next]) {                                 │  local scroll only
└────────────────────────────────────────────────────────────┘
Step 4 of 21 — narration
[Restart] [Previous] [Play] [Next]
```

### Workspace: Analysis

```text
Step 4 of 21 — narration              [Previous] [Play] [Next]   ← Back to visualization
Variables                 │ Call stack / Queue contents
node   2                  │ Front → 3, 4 ← Back
visited [0,1,2]  ▸expand  │
Algorithm complexity ─────────────────────────────────────
Time  O(V + E) — explanation (reading width ≤ 72ch)
Space O(V)     — explanation
```

### Open input editor on a phone

```text
Try your own input                     (heading gets focus after "Edit input")
Graph
  Vertices [ 6 ]
  1  [0] → [1]  ✕
  …  [+ Add edge]
Start vertex  [0]
  ⚠ Must be between 0 and 5.           (aria-describedby, linked to summary)
[Run input]  [Other case]
[Randomize]  [Restore defaults]
Saved inputs · [Save current input]
```

## Open decisions

None block P1. Two will be re-checked with real journeys at the P4 gate: whether a
bottom playback dock is ever needed on short landscape phones, and whether Graph/Tree/DP
should gain a textual history list only (the P6 default) while keeping the capture-strip
exclusion.
