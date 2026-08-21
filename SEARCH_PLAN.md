# Search Overhaul — Plan & Implementation Prompt

> ⚠️ **TEMPORARY FILE.** This is a work order for one task. Delete it once the search
> work is merged and reviewed.

**Audience:** paste this entire file to the implementing model. Part 1 is context — why
each requirement exists. Part 2 is the specification. Part 3 is the acceptance checklist.

---

# Part 1 — What is wrong today

All of this is verified against `frontend/src/components/Sidebar.jsx` at the current HEAD.
Line numbers refer to that file.

### 1. "Recent Searches" is fabricated

```js
{['Two Sum', 'Binary Tree Traversal', 'HashMap Lookup', 'HashMap Traversal'].map(...)}   // line 191
```

Four hardcoded strings. They never update, are never persisted, and have nothing to do
with anything the user has searched. Worse — **three of the four match zero problems in
the catalogue.** `Binary Tree Traversal`, `HashMap Lookup` and `HashMap Traversal` are not
titles of anything. Clicking them produces an empty result list. The feature is not
merely unimplemented; it actively sends users into dead ends.

### 2. Search reads a field that does not exist

```js
(p.subcategory && p.subcategory.toLowerCase().includes(q))   // line 65
{prob.subcategory || prob.category}                          // line 338
```

`subcategory` does not exist anywhere in the backend — `grep -rn "subcategory" backend/src`
returns nothing. The real field on `ProblemDetail` is `striverSheetSection`, which has
exactly one call site in the entire codebase: its own setter. It is never populated, so it
is always `null`. Both lines above are dead code, and the row subtitle always falls through
to `category`.

### 3. No ranking of any kind

`Array.prototype.filter` preserves catalogue order. Typing `two sum` returns every problem
whose title contains that substring **in registration order**, so the exact match *Two Sum*
can appear below weaker ones. With 433 problems this is the single largest usability
failure: the right answer is present but not findable.

### 4. Multi-word queries fail out of order

One `includes()` against the whole query string. `bfs graph` matches nothing, even though
*BFS Traversal of Graph* exists. `sum two` matches nothing. Users type words in the order
they think of them, not the order the title happens to use.

### 5. The category fallback is either/or, not ranked

```js
if (titleMatches.length > 0) return titleMatches;   // line 68
return problems.filter(p => category or difficulty match);
```

If a single title matches, category matches are never shown at all. Searching `tree`
surfaces titles containing "tree" and hides the entire Binary Trees category.

### 6. Zero keyboard support

Result rows are `<div onClick>` with no `role` and no `tabIndex` (line 315). **The entire
catalogue is unreachable without a mouse.** The search input has no ArrowDown into results,
no Enter to open the top hit, no Escape to clear.

### 7. The list silently escapes its own filter, then mislabels the result

`filteredProblems` (lines 88–100) drops the active category whenever that category has no
matches, and falls back to searching everything. The header still reads
`{activeCategory} Problems ({count})` (line 306), so the UI says "Binary Trees Problems (37)"
while displaying results from every category. Silent scope changes are worse than empty
results.

### 8. The placeholder promises something that does not exist

`"Search Algorithms, Problems, or Code..."` — code is never searched, and `javaCode` is not
even present in the list payload.

### 9. Nothing indicates which problems actually run

**8 of 433 problems have real execution traces.** The other 425 are catalogued but return
501 from the v2 API. Search offers no way to find the runnable ones, so the overwhelmingly
likely outcome of any search is a problem that cannot be run.

---

## Decisions already taken (do not revisit)

| Decision | Choice |
|---|---|
| Surface | Upgrade the sidebar search **in place** into an ARIA combobox. No modal overlay. |
| Matching | A **hand-rolled, unit-tested scoring module**. No new dependencies — not fuse.js, not lodash. |
| Runnable | Enrich the catalogue with the `traced` flag; badge it and offer a "Runnable only" filter. |

---

# Part 2 — Specification

## 2.0 Scope fence — read this first

**Do not:**
- touch anything under `backend/`
- modify the execute path: `fetchProblemDetailsAndSteps`, `_endpoint`, or the
  category→endpoint ladder in `App.jsx:252-270`
- add any npm dependency
- restyle parts of the app outside the sidebar, or convert anything to CSS Modules
- add `react-router` or change routing
- rename or move existing components other than as specified below

**Do** keep every existing test passing. `cd frontend && npx vitest run` must stay green.

---

## 2.1 New file: `frontend/src/search/scoreProblem.js`

A pure module. No React, no imports, no side effects.

```js
export function scoreProblem(query, problem) → number   // 0 means "no match"
export function searchProblems(query, problems) → Problem[]   // ranked, filtered
export function matchRanges(query, text) → Array<[start, end]>  // for highlighting
```

### Normalization

```js
normalize(s) = (s ?? '').toLowerCase().trim()
tokenize(q)  = normalize(q).split(/\s+/).filter(Boolean)
words(field) = normalize(field).split(/[\s\-_/&,()]+/).filter(Boolean)
```

### Field weights

| Field | Weight |
|---|---|
| `title` | 1.0 |
| `id` | 0.6 |
| `category` | 0.35 |
| `dsType` | 0.3 |
| `difficulty` | 0.25 |

### Match kinds — for one token against one field, the best kind wins

| Kind | Value | Condition |
|---|---|---|
| exact | 1.00 | `normalize(field) === token` |
| prefix | 0.85 | `normalize(field).startsWith(token)` |
| wordPrefix | 0.75 | any word in `words(field)` starts with `token` |
| acronym | 0.70 | token length ≥ 2 and token equals the initials of `words(field)` — `bfs` matches "Breadth First Search" |
| contains | 0.50 | `normalize(field).includes(token)` |
| subsequence | 0.30 | token length ≥ 3 and every char of token appears in `normalize(field)` in order |
| none | 0 | otherwise |

### Combining

```
tokenScore(token) = max over all fields of (fieldWeight × bestMatchKind)

AND semantics: if ANY token has tokenScore === 0, the whole problem scores 0.
   → "bfs graph" requires both "bfs" and "graph" to hit something.

score = Σ tokenScore(token) for all tokens
      + 0.50 if normalize(title) === normalize(query)        // exact title bonus
      + 0.25 if normalize(title).startsWith(normalize(query)) // whole-query prefix bonus

Empty query → scoreProblem returns 0, and searchProblems returns the input array
              unchanged and unsorted (browse mode; do NOT reorder the catalogue).
```

### Sorting — must be fully deterministic

```
score DESC, then traced DESC (true first), then title ASC (localeCompare)
```

`traced` is a **tie-break only**, never a score bonus. A runnable weak match must not
outrank a non-runnable strong one.

### `matchRanges(query, text)`

Returns non-overlapping `[start, end)` index pairs of every token occurrence in `text`,
case-insensitive, sorted ascending and merged where they overlap. Used for highlighting.
For an empty query, return `[]`.

---

## 2.2 New file: `frontend/src/search/useProblemSearch.js`

A hook owning all search state.

```js
useProblemSearch({ problems, activeCategory, normalizeCategory })
  → {
      query, setQuery,
      runnableOnly, setRunnableOnly,
      results,          // ranked, already filtered by category + runnableOnly
      visible,          // results.slice(0, 50)
      totalMatches,     // results.length
      globalMatches,    // matches ignoring activeCategory — for the "search all" affordance
      activeIndex, setActiveIndex,
      recents, commitRecent, removeRecent,
      isSearching       // query.trim().length > 0
    }
```

**Filter order matters:** score → filter by `runnableOnly` → filter by `activeCategory` →
sort. Compute `globalMatches` from the same ranked list *before* the category filter.

**No debouncing.** Scoring 433 items is well under a frame. The cost is DOM, which the
50-item cap handles. Do not add `setTimeout`, and do not add a debounce dependency.

**`activeIndex` resets to `-1`** whenever `query`, `runnableOnly`, or `activeCategory`
changes. `-1` means "nothing highlighted".

**Recents** — `localStorage` key `dsa:recentSearches`:
- Append only on `commitRecent(q)`, which the UI calls **when a result is actually opened**,
  never on keystroke.
- Store the trimmed query, max 5, most-recent first, de-duplicated case-insensitively.
- Wrap every `localStorage` call in `try/catch` — it throws in private-mode Safari and in
  some embedded webviews. A storage failure must degrade to an empty list, never crash.
- If the list is empty, the UI hides the section entirely.

---

## 2.3 `frontend/src/components/SearchBox.jsx` — the combobox

### ARIA — required exactly

```jsx
<input
  role="combobox"
  aria-expanded={isSearching}
  aria-controls="problem-results"
  aria-activedescendant={activeIndex >= 0 ? `problem-opt-${visible[activeIndex].id}` : undefined}
  aria-label="Search algorithms"
  aria-describedby="search-hint"
/>
```

The results container is `<div role="listbox" id="problem-results">`; each row is
`<div role="option" id={`problem-opt-${prob.id}`} aria-selected={i === activeIndex}>`.

Rows stay `role="option"`, **not** `<button>`. Focus remains on the input at all times and
`aria-activedescendant` conveys the highlight — this is what makes the list keyboard-reachable
without fighting the app's global key handler.

### Keyboard contract

| Key | Behaviour |
|---|---|
| `ArrowDown` | activeIndex + 1, wrapping from last → 0. From `-1` goes to `0`. |
| `ArrowUp` | activeIndex − 1, wrapping from 0 → last. From `-1` goes to last. |
| `Enter` | Open the active result. If `activeIndex === -1`, open the **first** result. No-op when there are no results. |
| `Escape` | If the query is non-empty: clear it and keep focus. If already empty: blur the input. |
| `Home` / `End` | First / last visible result. |
| `Tab` | Native behaviour. Do not trap focus. |

Every one of these calls `e.preventDefault()` **only** when it acts.

The active row must be scrolled into view with
`el.scrollIntoView({ block: 'nearest' })` — never `behavior: 'smooth'`, which lags behind
held arrow keys.

### ⌘K / Ctrl+K

A `window` keydown listener: on `(e.metaKey || e.ctrlKey) && e.key === 'k'`, call
`preventDefault()`, focus the input and select its contents. Register in a `useEffect` and
**remove it in the cleanup**.

Show a `⌘K` hint chip inside the input's right edge, hidden while the input has focus or a
query. Render `⌘K` on Apple platforms and `Ctrl K` elsewhere —
`navigator.platform`/`userAgent` test computed **once** at module scope.

### Fix the global handler collision — `App.jsx:163`

```js
// Currently:
if (['INPUT', 'TEXTAREA'].includes(document.activeElement?.tagName)) return;
// Change to:
const tag = document.activeElement?.tagName;
if (['INPUT', 'TEXTAREA', 'SELECT', 'BUTTON'].includes(tag)
    || document.activeElement?.isContentEditable) return;
```

Without this, pressing `r` while a category tile has focus resets the animation, and
`Space` toggles playback instead of activating the focused button.

---

## 2.4 Result rows

Each row shows:

- **Title** with matched substrings wrapped in `<mark>` (use `matchRanges`)
- **Subtitle**: `category · difficulty` — **remove the `subcategory` reference entirely**,
  it is always undefined
- **A ⚡ badge when `traced === true`**, with `title="Runnable — executes on your input"`.
  The badge must carry a text label or `aria-label`; never encode it by colour alone.

Highlighting renders as `<mark>` elements. Do not use `dangerouslySetInnerHTML`.

---

## 2.5 States — all four must be distinct

| Condition | What renders |
|---|---|
| `problems.length === 0` and not loading | "Could not reach the backend." + a Retry button. **Not** an empty search result. |
| Query empty | Full browse list for the active category. Header: `All problems · N` or `{Category} · N`. |
| Query with matches | Header: `N results`. If `N > 50`: `Showing 50 of N — keep typing to narrow`. |
| Query, 0 matches in scope | `No algorithm matches "{query}".` Plus, when `globalMatches > 0` and a category is active, a button: `Search all {globalMatches} matches` which clears the category **without** clearing the query. When `runnableOnly` is on and it is the cause, a second button: `Include problems that aren't runnable yet`. |

**The list must never silently escape its own filter.** Removing the category filter is an
explicit user action via the button above, never automatic.

### Screen-reader announcement

```jsx
<div role="status" aria-live="polite" className="sr-only">
  {isSearching ? `${totalMatches} results for ${query}` : ''}
</div>
```

Add an `.sr-only` class to `index.css` if one does not exist.

---

## 2.6 `App.jsx` — enrich with `traced`

Add **one** entry to the existing `Promise.allSettled` batch in `fetchAllProblems`:

```js
fetch('/api/problems').then(r => (r.ok ? r.json() : []))
```

Build `Map<id, traced>` from it and merge `traced` onto each combined problem by id.

**Constraints:**
- The 18 legacy fetches and `_endpoint` remain exactly as they are. This call is additive.
- If it fails or returns `[]`, every problem gets `traced: undefined`, and the
  "Runnable only" toggle **does not render at all**. A filter that would return nothing
  must not be offered.
- Do not `await` it separately — it goes in the same `allSettled` array so it cannot delay
  or fail the catalogue load.

---

## 2.6b De-duplicate the combined catalogue

`fetchAllProblems` concatenates all 18 legacy catalogues, which contain **440 registrations
but only 433 unique ids** — seven ids are claimed by two services each (`dfs-traversal`,
`flood-fill`, `longest-common-prefix`, `longest-substring-without-repeating`,
`merge-intervals`, `number-of-islands`, `surrounded-regions`).

Every result row is keyed `key={prob.id}`, so today those seven produce **duplicate React
keys**, which is undefined behaviour during reconciliation and makes the header count wrong.

When building `combined`, skip an item whose id has already been added (first wins, matching
the backend's `ProblemCatalog` precedence). Add a test asserting that a fixture containing a
repeated id yields one entry.

---

## 2.7 Copy fixes

- Placeholder → `Search 433 algorithms…` (derive the number from `problems.length`, do not
  hardcode it). Drop the false "or Code" claim.
- Panel heading `Search & Explore` stays.
- The category tile labelled `Arrays & Math` is wrong — `normalizeCategory` routes anything
  containing "Math" to **Bit Manipulation** (`Sidebar.jsx:49`), so no Math problem ever
  lands there. Relabel the tile to `Arrays`.

---

## 2.8 Styling

Use the existing custom properties in `index.css`. Do not introduce raw hex values for
anything that has a token.

`designTokens.test.js` will fail the build on any `var(--x)` or `className` that
`index.css` does not define — **and it currently only scans `src/components/*.jsx` plus
`App.jsx`.** Update `componentSources()` in that file to walk `src/**/*.jsx` recursively so
the new `src/search/` files are covered too. This is required, not optional.

`<mark>` needs an explicit style — the browser default is black-on-yellow and unreadable on
this dark surface. Use a violet tint background with `color: inherit`.

---

# Part 3 — Tests (required, not optional)

Write these alongside the code. A test that cannot fail is worse than no test.

### `src/search/scoreProblem.test.js`

1. Exact title beats prefix beats word-prefix beats contains beats subsequence — assert the
   **ordering of returned scores**, not fixed magic numbers.
2. `two sum` ranks the problem titled *Two Sum* first out of a fixture containing
   *Two Sum*, *Two Sum II*, *3Sum*, *Two Pointers*.
3. `bfs graph` matches *BFS Traversal of Graph* — proves out-of-order multi-token matching.
4. `bfs` matches *Breadth First Search* via acronym.
5. AND semantics: `kadane zzzz` returns 0.
6. Case and surrounding whitespace are irrelevant: `'  KADANE '` === `'kadane'`.
7. Empty query → `searchProblems` returns the array **unchanged and in original order**.
8. Unknown/missing fields (`dsType: undefined`) do not throw.
9. Ties break by `traced` then title, and `searchProblems` is a pure function — calling it
   twice on the same input gives an identically ordered array.
10. `matchRanges` merges overlapping ranges and returns `[]` for an empty query.

### `src/search/useProblemSearch.test.js`

11. `runnableOnly` filters to `traced === true`.
12. `activeIndex` resets to `-1` when the query changes.
13. `commitRecent` writes to `localStorage`, caps at 5, and de-duplicates case-insensitively.
14. Recents survive a hook remount.
15. A throwing `localStorage` (mock `getItem` to throw) degrades to `[]` without crashing.

### `src/components/SearchBox.test.jsx` (React Testing Library)

16. Typing filters the rendered list.
17. `ArrowDown` then `Enter` calls `onSelectProblem` with the **top-ranked** id.
18. `Enter` with no prior arrow key opens the first result.
19. `Escape` clears a non-empty query and leaves focus on the input.
20. ArrowDown wraps from the last option to the first.
21. `aria-activedescendant` matches the highlighted row's `id`.
22. Zero results renders the empty state, and the "Search all N matches" button clears the
    category while preserving the query.
23. The ⚡ badge appears only on `traced` problems.
24. The "Runnable only" toggle is absent when no problem carries a `traced` field.

### Verification discipline

For each of tests 2, 3, 17 and 22: **temporarily revert the relevant fix and confirm the
test fails**, then restore. A test that passes against the broken code proves nothing. State
in your final summary which tests you verified this way and what failure message each
produced.

---

# Part 4 — Acceptance checklist

Run and report actual output. Do not claim a step passed without pasting its result.

```bash
cd frontend && npx vitest run     # all tests green, new ones included
cd frontend && npx vite build     # clean build
```

- [ ] No new entry in `package.json` dependencies
- [ ] `grep -rn "subcategory" frontend/src` returns nothing
- [ ] `grep -rn "HashMap Lookup" frontend/src` returns nothing
- [ ] Catalogue is fully navigable by keyboard alone: Tab to search, ⌘K, arrows, Enter
- [ ] Backend stopped → sidebar shows the connection error state, not "no results"
- [ ] All four states in §2.5 reachable by hand
- [ ] `backend/` is untouched: `git diff --stat backend/` is empty
- [ ] No duplicate-key warning in the browser console with the real backend running

**Report honestly.** If something is left undone or a test is failing, say so explicitly
with the output. A partial implementation that is accurately described is far more useful
than a complete-sounding summary that does not survive review.
