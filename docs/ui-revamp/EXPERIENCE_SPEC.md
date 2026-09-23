# Experience and interaction specification

Status: proposed design, 2026-09-23. Values below are starting design constraints; the reference slice validates them with real content. Behavior requirements are release gates.

## 1. Experience principles

1. Give the current learning task visual priority.
2. Keep related evidence together: state, narration, active source line, and variables must refer to the same step.
3. Make all features findable, even when they are not continuously visible.
4. Let the page scroll. Use local scrolling only for genuinely oversized structures, source, and long lists.
5. Preserve user work and execution context across presentation changes.
6. Explain what a control will do. Distinguish replay reset from input reset.
7. Communicate status through words and shape as well as color.

## 2. Information architecture

```text
Algorithm library /
  Search + category + difficulty + runnable filter
  Results with explicit continuation
  Problem /problem/:id
    Playground              default view
    Code walkthrough        ?view=code
    Analysis                ?view=analysis
    Quick problem switcher  modal, not a permanent side column
    Execution history      disclosure associated with the current run
```

Problem views are URL-addressable. Split-panel preference and focus mode are local presentation preferences, not new routes. A copied problem URL identifies the problem and view; it does not promise to reconstruct a custom run or unsaved input.

## 3. Library page

### Anatomy

```text
DSA Visualizer                                      Theme
Algorithms, made visible.
One short sentence explaining how to begin.

[ Search by name or concept............................ ]
[ Category v ] [ Difficulty v ] [ Runnable only ] [ Clear ]

Explore a category / current search scope          Result count
-------------------------------------------------------------
Problem name               Category       Difficulty  Open →
Problem name               Category       Difficulty  Open →
...
[ Load more ]                      Showing X of Y algorithms
```

Use a compact hero, not a marketing page. On a normal laptop, search and initial results must be visible without scrolling past an illustration.

Search uses existing ranking and text matching. Category and runnable filtering retain their semantics; difficulty is a planned addition. Search/filter state is represented in query parameters (`q`, `category`, `difficulty`, `runnable`) so Back restores the library context. Invalid parameter values fall back visibly to an unfiltered/default state.

The library uses semantic links for problem destinations, supporting open-in-new-tab. Filters use labeled controls. Show zero results with a clear scope and a Clear filters action. Distinguish zero matching results from a catalogue request failure.

Start with explicit Load more in stable batches, with result counts and accessible announcements. Changing filters resets the visible batch. Returning from a problem restores filters and enough loaded rows to restore the prior scroll position. Do not silently stop at 50 results or require infinite-scroll guessing.

Recent searches remain local and bounded. Record a search when a result is opened, not on each keystroke. No completion scores or fake recommendations are introduced.

## 4. Problem header and navigation

Header: brand link, All algorithms link, Switch problem button, theme control. Problem introduction: category, difficulty, title, optional type metadata, and one brief instruction. Do not invent problem statements that are not in the current content contract.

Keep the header compact enough that the visualization starts within the first laptop screen. Long problem names wrap; they must not force neighboring controls off-screen.

Use three top-level tabs: Playground, Code walkthrough, Analysis. The active tab has a strong visual indication and an accessible selected state. Left/Right/Home/End move focus; Enter/Space activate, so keyboard exploration does not change the view unexpectedly. The tab rail may be sticky, with one coordinated sticky offset. Scroll a newly activated panel into view only for an explicit user tab action, respecting reduced motion.

Do not make the whole page a fixed-height application frame. The browser owns vertical document scrolling.

## 5. Playground

### Wide layout

```text
← All algorithms                                  Switch problem
Arrays / Easy
Two Sum
[ Playground ] [ Code walkthrough ] [ Analysis ]

Experiment & observe                        [Show code] [Focus]
┌─────────────────────────────────┬────────────────────────────┐
│ Visualization                   │ Optional Java source       │
│ Legend + structure              │ Active line follows step   │
│ Relevant companion, if needed  │                            │
├─────────────────────────────────┴────────────────────────────┤
│ Current step explanation                                     │
│ Reset playback  Prev  Play/Pause  Next  Speed  Step/seek       │
└──────────────────────────────────────────────────────────────┘
Input used: [compact summary]                 [Edit input ↓]

Try your own example
Labeled input editor + help + validation
[Run with this input] [Randomize input] [Restore default input]

▸ Execution history                  Inspect or jump to a step
```

The default diagram uses the full workbench width. Input editing is a normal-flow section directly below playback, reachable by a visible Edit input action. That action expands the editor if necessary and moves focus to its heading/first field. The reference slice must test whether this placement is easy to discover. If users repeatedly miss inputs, bring a compact input editor above the stage; do not add a permanently narrow third column.

Give diagrams a useful minimum area rather than the leftover height. Proposed starting minimums: about 360px for simple sequences and 440–520px for spatial structures on desktop. These are minimums, not fixed heights; content may grow. At 1366×768, essential controls should be reachable with modest normal scrolling and without shrinking the diagram to fit all content.

The narration belongs immediately beside the evidence, not in a detached ticker at the edge of the page. Show meaningful descriptions in full, allowing wrapping. At step zero/empty/error states, use accurate status copy.

### Optional code pane

Show code alongside is a persistent, labeled toggle. On sufficiently wide screens, split the workbench between diagram and source. Proposed initial bounds: at least 480px for the diagram and 360px for code, with a keyboard-operable adjustable separator where sufficient space exists. The default split favors the diagram.

When the available width cannot honor those minima, stack the panels or offer the dedicated Code walkthrough view. Never leave two unreadable slivers. The preference may persist, but a narrow viewport overrides its layout safely and explains the change through visible positioning rather than an error.

## 6. Code walkthrough

Use the same run and playback controller. The source has more room than in the optional Playground split. On a wide screen, the visualization and Java source remain together; controls and narration span their shared context.

Show line numbers and active-line styling without relying only on color. Follow the active line while playback/stepping is under user control, but do not continuously override a learner manually scrolling through code. Provide a visible Follow execution toggle or Return to active line action.

Long source lines scroll inside the source panel; they do not widen the page. Allow copying/selecting source. Keyboard playback must not consume editing/selection shortcuts. Provide a collapsed or compact input-used summary and an Edit input action returning to the Playground editor.

On narrow screens, provide mutually exclusive Diagram / Source subviews with shared narration and playback. Keep the selected step and source scroll state when switching. This prevents the learner from scrolling between distant panels after every step.

## 7. Analysis

Lead with the current step and its explanation. Provide a compact shared playback bar and separate, clearly titled sections for Variables, Call stack, Data structure contents, and Complexity. Sections with no relevant state show honest empty copy or are omitted with context; do not render an apparently broken empty dashboard.

Variables use readable name/value rows. Objects and long strings can expand without being silently truncated. Call-stack ordering and current frame are explicit. Queue/stack state retains Front, Back, and Top markers.

Time and space explanations receive reading width and normal body text. Label them as algorithm analysis, not measurements of the animation. Do not infer CPU time, byte allocation, or benchmarks from frame count. Missing metadata says unavailable.

The full diagram is not required to remain visible in Analysis. A Back to visualization action preserves the step; this is how the view gains enough room for explanations. Dedicated companions required to explain the visual transition stay with the diagram in Playground/Code.

## 8. History and navigation

Execution history starts collapsed and has a descriptive summary, step count, and current position. Preserve the existing capture strip for suitable trace types. The strip remains omitted where it duplicates Graph, Tree, or DP rendering under the existing rules.

Add a paginated or windowed textual step list so history remains meaningful for every trace family. Each item shows its ordinal and narration and has an explicit jump action. Paging must make the entire trace reachable; disclose which rows are currently rendered. Expanding history does not reset playback or the current step. Selecting a history entry pauses and seeks.

The quick switcher opens through its button or Ctrl/Cmd+K. Focus enters search, background content becomes inert, Escape closes the dialog, and focus returns to the trigger. Within the search field, clearing a query uses a visible clear action; Escape consistently closes this modal. The library search may retain its existing Escape-to-clear behavior outside a modal.

Only one global shortcut handler is active. No hidden or unmounted search box may intercept the shortcut. Selecting a different problem resets execution to that problem's default run and closes the switcher. Switching to the current problem simply closes it.

## 9. Input and run lifecycle

| User action | Expected result |
| --- | --- |
| Edit a field | Update draft; keep the current run visible and mark that input differs |
| Randomize / restore defaults | Change draft only; mark it as not yet run |
| Run | Pause playback; validate/submit captured draft; indicate pending state |
| Successful run | Replace run atomically; select its first step; show the input snapshot used |
| Validation rejection | Preserve previous run and draft; associate field errors; focus first error |
| Network/server failure | Display explicit failure and retry path; never relabel an old run as the new result |
| Reset playback | Pause and return to first step of the same run; keep input draft |
| Switch view | Pause, preserve run/step/draft, show the chosen presentation |
| Switch problem | Cancel/supersede old requests; initialize new problem inputs and default run |
| Refresh problem URL | Load the default run; custom-run persistence is not promised in this release |

Prefer disabling a second Run while one request is pending. If input editing remains enabled during a request, the captured submitted input and the current draft must stay distinct; a response cannot overwrite newer edits.

Initial load should not show input values from the previous problem. Unknown input types receive an explicit unsupported-input message, not a silent missing field.

## 10. Responsive specification

| Available width | Layout |
| --- | --- |
| 1200px and wider | Optional diagram/source split if minimum panel widths fit; otherwise full-width stage |
| 900–1199px | Full-width stage by default; code split only if measured space is adequate; inputs below |
| 600–899px | One primary column; diagrams have local pan/scroll; Code view uses source/diagram subviews as needed |
| 320–599px | Single column; wrapping controls; compact header; accessible horizontal tab rail if labels cannot fit |

Breakpoints are starting points and use available container width, not device labels. Also test short height, 200% zoom, long titles, long narration, large numeric values, and sparse structures.

Keep one sticky tab rail initially. Playback is adjacent to the active content in normal flow; a sticky mobile playback dock is only added if usability evidence shows a need and it does not cover content or the virtual keyboard.

Diagrams may scroll locally. Provide Fit/Reset view and keyboard alternatives where new pan/zoom is implemented. Never scale labels down indefinitely to avoid scrolling. Page-wide horizontal overflow is a release failure.

## 11. Visual system and accessibility

Use semantic CSS variables for surfaces, text, border, focus, selection, and execution state. Reuse the current state-token semantics and contrast guards. Give typography, spacing, control heights, and panel padding a small documented scale. Avoid ad hoc inline styling in new shell components.

Proposed spacing scale: 4, 8, 12, 16, 24, 32, 48px. Use 24–32px between major sections and tighter spacing within a group. Use subtle borders and modest radii; avoid pervasive shadows, gradients, badges, and animated chrome.

Controls should generally offer 44px touch targets, even when the icon is smaller. Preserve a visible focus ring in both themes. Reduced-motion mode removes automatic transitions and smooth scrolling while retaining explicit step changes.

Screen-reader users need a textual representation of the current structure/state. Manual step changes can announce the current explanation; autoplay should not flood a live region every frame. Theme selection offers System, Light, and Dark, respects changes to system preference when selected, and works without localStorage.

## 12. States that must be designed

Design and verify: catalogue loading, empty catalogue, no search matches, backend unavailable with/without a checked-in sample, detail loading, unknown problem, unsupported renderer, absent input contract, validation failure, execution pending, empty trace, malformed trace, truncated trace, renderer error, final step, and denied preference storage.

Use a clear heading, accurate explanation, and appropriate action. Preserve a valid previous run only with a visible label that it is the previous run. Never mask an execution error with a plausible new animation.
