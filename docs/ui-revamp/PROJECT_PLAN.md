/# DSA learning experience redesign

Planning baseline: 2026-09-23. Status: **proposed; implementation paused for planning**.

The user wants the clarity and spaciousness of the sibling `hld-with-ui` project, with every existing DSA feature preserved. This is a redesign of an existing learning product, not a new algorithm implementation project. Nothing in these documents should be read as shipped behavior or completed verification.

## 1. Product goal

Help a learner choose an algorithm, run a meaningful example, follow its state changes, connect them to Java code, and explain its memory and complexity without having to manage a crowded screen.

The learning loop is **choose → experiment → observe → inspect → explain**. A successful redesign makes those activities obvious while keeping the current execution connected across them.

The primary design reference is the implemented HLD module shell: a clear page hierarchy, focused views, natural document scrolling, restrained surfaces, and visible access to details. DSA must additionally support simultaneous code and data inspection, larger algorithm structures, and fast search through hundreds of problems.

“Better than HLD” is a quality target, not a claim. It will mean that these DSA-specific tasks work well on an ordinary laptop and a phone, and that learner walkthroughs reveal fewer obstacles. Screenshots alone cannot establish that outcome.

## 2. Read this plan

| Document | Purpose |
| --- | --- |
| [Feature inventory](FEATURE_INVENTORY.md) | Current evidence, preservation obligations, renderer families, and known gaps |
| [Experience specification](EXPERIENCE_SPEC.md) | Page anatomy, wireframes, interaction rules, visual direction, and responsive behavior |
| [Architecture and migration](ARCHITECTURE.md) | State ownership, component boundaries, routes, compatibility, and rollout |
| [Delivery roadmap](ROADMAP.md) | Ordered work packages, dependencies, estimates, and phase gates |
| [Quality and release review](QUALITY_AND_RELEASE.md) | Behavioral checks, browser matrix, usability tasks, and evidence ledger |

Read this document first, then the inventory and experience specification. Use the architecture and roadmap for implementation. Update the evidence ledger as work is verified.

These documents govern the proposed UI redesign. Existing tracer contracts, golden fixtures, and algorithm-correctness requirements remain applicable. Historical completion plans describe earlier work; their counts and completion claims are not fresh verification of this redesign.

## 3. Current diagnosis

The inspected implementation combines a persistent catalogue sidebar, visualization, companion structures, execution capture, playback controls, narration, Java source, editable input, and memory/complexity in a single viewport.

`App.module.css` fixes the root at `100vh` and hides overflow. The desktop bottom row reserves 340px; the mobile details card reserves 280px. The sidebar has a 320px width in `index.css`. Several ancestors also hide overflow. Those constraints leave the visualization whatever space remains, particularly on short laptop screens.

Collapsing panels is already possible, but the learner must repair the layout manually. Small text, nested scrolling, and multiple equally prominent surfaces add to the problem. The core issue is information organization and space allocation, so changing colors alone cannot meet the goal.

Evidence is from source inspection, not a completed browser audit. Phase 0 records actual screenshots, viewport dimensions, browser behavior, and performance before judging the result.

## 4. Learners and main jobs

| Learner situation | Job | What the interface must support |
| --- | --- | --- |
| First encounter | Understand what moves and why | A readable default example, clear active state, narration, and an obvious Next action |
| Interview preparation | Relate implementation to reasoning | Synchronized Java, variables, and complexity explanations without losing execution position |
| Exploring an edge case | Run and inspect a custom example | Complete input editors, actionable validation, clear separation of draft input and executed input |
| Returning learner | Reach a known problem quickly | Ranked search, keyboard switching, remembered search context, direct problem links |
| Phone or narrow window | Study away from a large monitor | Single-column flow, focused views, accessible controls, bounded diagram overflow |

Do not invent a new onboarding tour, account system, or progress score to serve these jobs. Start with clear pages and working defaults.

## 5. Proposed product surfaces

### Algorithm library

The home route becomes a real library. It provides search, category and difficulty filtering, runnable status, result counts, and access to every catalogue entry. A compact introduction orients newcomers without pushing search below the first screen.

Use category entry cards for exploration and readable rows for large result sets. Avoid hundreds of oversized decorative cards. Keep recent searches and the existing ranking behavior. Add explicit pagination or Load more so results beyond the current 50-item display cap remain reachable.

### Problem workspace

Each problem has a compact header, clear metadata, a quick problem switcher, and three primary views:

1. **Playground:** input, visualization, playback, narration, and relevant companion structures.
2. **Code walkthrough:** source and visualization presented together when width allows, following the same selected step.
3. **Analysis:** current variables, stack/queue/call frames, and time/space explanations, with execution context and stepping available.

The Playground is the default. A learner can optionally show code beside it on a wide screen. Execution history is a discoverable disclosure beneath the main activity; it does not consume permanent stage height.

Views change presentation, not execution identity. Switching views must preserve the current run, step, draft input, validation messages, and playback speed. Explicit view changes pause playback at the selected step so the learner can read without the state moving underneath them.

### Focus view

A Focus visualization action temporarily gives the stage and its controls the available page width. It hides optional neighboring panels, retains narration and exit controls, and restores the previous arrangement on exit. This is an in-page layout mode; browser fullscreen is not required.

### Quick problem switcher

A modal search surface gives fast access to another problem without permanently taking space from the current one. It shares search logic with the library, supports keyboard use, and returns focus correctly when dismissed. Opening it pauses playback. Dismissing it preserves the run; selecting another problem starts that problem's default run.

## 6. First redesign release

Required scope:

- Dedicated library and a shared workspace shell.
- Preservation of every feature in the inventory, including input randomization, reset, keyboard controls, search recents, and companion structures.
- All three focused views, optional code split, focus mode, and inspectable execution history.
- Light, dark, and system theme choices, with execution-state meanings preserved.
- Legible layouts for every registered renderer family, including existing shared-renderer mappings.
- Reliable state transitions, deep links, loading/error/empty/limited states, and browser navigation.
- Responsive, keyboard, zoom, reduced-motion, and screen-reader review.
- Existing automated regression checks and new tests for meaningful new behavior.

The release does not require new algorithms, backend framework upgrades, TypeScript migration, a component-library replacement, accounts, cloud services, gamification, an AI tutor, a code editor/compiler, new practice content, or comparison of multiple runs. Those are separate product decisions.

Existing shared renderers for Window, SearchSpace, String, Bits, and PriorityQueue remain legitimate compatibility obligations. Their replacement with dedicated visualizations is separate work unless a concrete layout defect makes a limited change necessary.

## 7. Visual direction

Use a calm reading surface, confident headings, generous spacing, simple borders, and a clear primary action. HLD's editorial hierarchy is the reference; its exact color palette and decorative header proportions are not requirements.

Proposed defaults are system theme with explicit light/dark choices, a neutral page and surface palette, a restrained navigation accent, and the existing semantic execution colors. Amber continues to indicate the active operation and green a resolved state; decorative chrome must not reuse those meanings indiscriminately.

Body and explanation text should be comfortable at 15–16px; controls generally 14–16px; compact metadata generally at least 12px. Code starts at 14px. These are design targets to verify at the selected font and viewport, not permission to shrink complex diagrams until they fit.

The workspace uses the width it needs, up to a proposed 1440px container; reading blocks remain narrower. A short problem header lets the main activity appear early. Whitespace separates decisions and related content, rather than filling the page with empty decorative regions.

## 8. Delivery strategy

First capture the current experience and freeze the feature checklist. Then build one complete reference slice covering an array, a graph with a queue companion, and a two-dimensional DP table. Exercise real inputs, step changes, both themes, and responsive layouts before expanding the shell across all renderers.

Deliver in ordered, reviewable changes: foundations, library, reference workspace, execution/state reliability, renderer adaptation, and release polish. Keep the existing interface usable until the replacement meets its integration gate. Do not leave a partly routed shell as the default product.

The user has requested planning before further implementation. The immediate deliverable is this document set. Implementation begins only in a subsequent implementation step; a plan review does not itself require publishing, merging, or deployment.

## 9. Effort and uncertainty

Initial estimate: **10–15 focused engineering days** for a thoroughly polished redesign of the existing feature set. The [roadmap](ROADMAP.md) distributes that estimate by phase. This is planning effort, not a calendar promise or a claim about agent wall-clock time.

Expect roughly 20–35 frontend files to be touched initially, with additional renderer and test files if the audit identifies problems. Prefer the smallest set that gives coherent behavior; file count is not a success metric. Most changes should remain in the frontend. Backend changes need a demonstrated contract gap and a separately described acceptance test.

Main uncertainties are renderer sizing assumptions, accessibility of existing input editors, large-trace rendering cost, and the actual behavior at short laptop heights. Re-estimate after the reference slice. If large gaps are found, document the extra work explicitly rather than dropping features or calling a partial result complete.

## 10. Success and completion

The redesign is ready when all inventory rows have a verified destination; a learner can find, run, inspect, and explain representative algorithms; the ordinary laptop layout no longer depends on collapsing a bottom panel; and phone users can perform the same core tasks.

No release claim rests solely on automated tests. Require the browser matrix, learner walkthroughs, error-state checks, and visual review described in [Quality and release review](QUALITY_AND_RELEASE.md). Record unresolved limitations honestly.

The next implementation work item is **R0-01: baseline browser and feature audit**. The first design review artifact is the library plus three representative workspace layouts, not a full application rewrite.
