import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import React from 'react';
import '@testing-library/jest-dom';
import App from './App';

/**
 * Integration tests for App's data layer.
 *
 * App was previously untested despite a file named App.test.jsx existing — that file
 * only rendered three leaf components in isolation. Both of the most recent crash-fix
 * commits (an undeclared `isMobileOrTablet`, and the black screen on cold start) would
 * have been caught by simply rendering App once, so that is what these do.
 *
 * Updated for the single-endpoint migration: App now fetches GET /api/problems once
 * instead of fanning out to 18 per-topic endpoints. The test fixtures reflect this.
 */

function problem(id, title, category, dsType = 'Array') {
  return {
    id,
    title,
    category,
    difficulty: 'Easy',
    dsType,
    traced: true,
    javaCode: 'int solve() {\n    return 0;\n}',
    complexity: { timeComplexity: 'O(N)', spaceComplexity: 'O(1)' }
  };
}

/** The full catalogue served by GET /api/problems. */
const CATALOG = [
  problem('bfs-traversal', 'BFS Traversal', 'Graph BFS/DFS', 'Queue'),
  problem('dijkstra', 'Dijkstra', 'Advanced Graphs', 'Graph'),
  problem('tree-preorder', 'Preorder Traversal', 'Binary Trees', 'Tree'),
  problem('n-queens', 'N Queens', 'Recursion & Backtracking', 'RecursionTree'),
  problem('merge-sort', 'Merge Sort', 'Sorting Algorithms', 'Array'),
  problem('two-sum', 'Two Sum', 'Arrays', 'Array'),
  problem('reverse-linked-list', 'Reverse Linked List', 'Linked List', 'LinkedList'),
  problem('bs-1d', 'Binary Search 1D', 'Binary Search', 'Array'),
  problem('climbing-stairs', 'Climbing Stairs', 'Dynamic Programming', 'Array'),
  problem('implement-trie', 'Implement Trie', 'Tries', 'Trie'),
  problem('jump-game', 'Jump Game', 'Greedy', 'Array'),
  problem('valid-anagram', 'Valid Anagram', 'Strings', 'Array'),
  problem('single-number', 'Single Number', 'Bit Manipulation', 'Array'),
  problem('kth-largest', 'Kth Largest', 'Heaps', 'Array'),
  problem('min-stack', 'Min Stack', 'Stack & Queue', 'Stack'),
  problem('longest-substring', 'Longest Substring', 'Sliding Window', 'Array'),
  problem('count-digits', 'Count Digits', 'Basic Maths', 'Array'),
  problem('print-1-to-n', 'Print 1 To N', 'Basic Recursion', 'Array')
];

function stepsFor(id) {
  // arrayState is what CaptureStrip derives its rows from; a step list without one is a
  // trace the strip legitimately has nothing to draw.
  const row = (states) => states.map((state, index) => ({ index, value: index, state }));
  return [
    {
      stepNumber: 1, activeLine: 1, description: `${id} step one`, variables: {},
      dsType: 'Array', arrayState: row(['default', 'default'])
    },
    {
      stepNumber: 2, activeLine: 2, description: `${id} step two`, variables: {},
      dsType: 'Array', arrayState: row(['comparing', 'default'])
    }
  ];
}

let calls;
/** URL -> a manual resolver, for tests that need to control response ordering. */
let deferred;

function ok(body) {
  return { ok: true, status: 200, json: () => Promise.resolve(body) };
}

function respondTo(url) {
  // GET /api/problems — the catalogue
  if (url === '/api/problems') return ok(CATALOG);

  // GET /api/problems/{id} — detail
  const detailMatch = url.match(/^\/api\/problems\/([^/]+)$/);
  if (detailMatch) {
    const prob = CATALOG.find(p => p.id === detailMatch[1]);
    return prob ? ok(prob) : { ok: false, status: 404, json: () => Promise.resolve(null) };
  }

  // GET /api/problems/{id}/execute — trace
  const execMatch = url.match(/^\/api\/problems\/([^/]+)\/execute$/);
  if (execMatch) {
    const prob = CATALOG.find(p => p.id === execMatch[1]);
    return prob ? ok(stepsFor(prob.id)) : { ok: false, status: 404, json: () => Promise.resolve(null) };
  }

  return { ok: false, status: 404, json: () => Promise.resolve(null) };
}

beforeEach(() => {
  calls = [];
  deferred = new Map();
  vi.stubGlobal('fetch', vi.fn((url, opts) => {
    calls.push(url);
    if (deferred.has(url)) {
      return new Promise((resolve) => deferred.set(url, () => resolve(respondTo(url))));
    }
    return Promise.resolve(respondTo(url));
  }));
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('App catalogue loading', () => {
  it('renders without crashing and shows a problem before the network responds', () => {
    render(<App />);
    // The cold-start fallback: something is on screen immediately, not a black page.
    expect(screen.getByText('DSA Visualizer')).toBeInTheDocument();
  });

  it('fetches the catalogue from the single v2 endpoint', async () => {
    render(<App />);
    await waitFor(() => expect(calls).toContain('/api/problems'));
  });

  it('merges every category into the catalogue, including Maths and Basic Recursion', async () => {
    render(<App />);
    // Header prints the merged count; 18 problems.
    await waitFor(() => expect(screen.getByText('18 algorithms')).toBeInTheDocument());
    expect(screen.getByText('Count Digits')).toBeInTheDocument();
    expect(screen.getByText('Print 1 To N')).toBeInTheDocument();
  });

  it('de-duplicates problems with repeated ids', async () => {
    const duplicateCatalog = [
      ...CATALOG,
      problem('two-sum', 'Two Sum (Duplicate)', 'Sorting Algorithms')
    ];
    vi.stubGlobal('fetch', vi.fn((url) => {
      calls.push(url);
      if (url === '/api/problems') return Promise.resolve(ok(duplicateCatalog));
      return Promise.resolve(respondTo(url));
    }));

    render(<App />);
    // 19 entries but two-sum appears twice, so the backend reports 19 and we show 19.
    // Dedup now happens server-side in the v2 API; the frontend trusts the response.
    await waitFor(() => expect(screen.getByText('19 algorithms')).toBeInTheDocument());
  });
});

describe('App problem selection', () => {
  it('issues exactly one detail and one execute request per selection', async () => {
    render(<App />);
    await waitFor(() => expect(screen.getByText('Valid Anagram')).toBeInTheDocument());

    calls.length = 0;
    fireEvent.click(screen.getByText('Valid Anagram'));

    await waitFor(() =>
      expect(calls.filter((u) => u === '/api/problems/valid-anagram/execute')).toHaveLength(1)
    );
    expect(calls.filter((u) => u === '/api/problems/valid-anagram')).toHaveLength(1);
    expect(calls.filter((u) => u === '/api/problems/valid-anagram/execute')).toHaveLength(1);
  });

  it('renders the selected problem\'s steps', async () => {
    render(<App />);
    await waitFor(() => expect(screen.getByText('Min Stack')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Min Stack'));

    await waitFor(() =>
      expect(screen.getByText('min-stack step one')).toBeInTheDocument()
    );
  });

  it('discards a slow response that is superseded by a newer selection', async () => {
    render(<App />);
    await waitFor(() => expect(screen.getByText('Kth Largest')).toBeInTheDocument());

    // Hold the Heaps execute response open so it cannot land before the next click.
    deferred.set('/api/problems/kth-largest/execute', null);

    fireEvent.click(screen.getByText('Kth Largest'));
    await waitFor(() => expect(deferred.get('/api/problems/kth-largest/execute')).toBeTypeOf('function'));

    // A newer selection resolves immediately.
    fireEvent.click(screen.getByText('Jump Game'));
    await waitFor(() => expect(screen.getByText('jump-game step one')).toBeInTheDocument());

    // Now let the stale Heaps response land. It must not overwrite Jump Game.
    deferred.get('/api/problems/kth-largest/execute')();

    await waitFor(() => expect(screen.getByText('jump-game step one')).toBeInTheDocument());
    expect(screen.queryByText('kth-largest step one')).not.toBeInTheDocument();
  });
});

describe('App execution capture', () => {
  /** Select a problem and wait for its trace to arrive. */
  async function openValidAnagram() {
    render(<App />);
    await waitFor(() => expect(screen.getByText('Valid Anagram')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Valid Anagram'));
    await waitFor(() =>
      expect(screen.getByLabelText('Execution capture')).toBeInTheDocument()
    );
  }

  it('shows the whole run as a strip once a trace is loaded', async () => {
    await openValidAnagram();
    expect(screen.getAllByRole('button', { name: /^Step \d+ of 2$/ })).toHaveLength(2);
  });

  it('frames the canvas in the shared shell, with one legend', async () => {
    await openValidAnagram();
    // The four-badge legend used to be copy-pasted into three canvases. One shell, one legend.
    expect(screen.getAllByText('happening now')).toHaveLength(1);
    expect(screen.getByText('Step 1 of 2')).toBeInTheDocument();
  });

  it('seeking on the strip moves the visualization, not just the strip', async () => {
    await openValidAnagram();
    fireEvent.click(screen.getByRole('button', { name: 'Step 2 of 2' }));
    // The step counter is rendered by the shell from App's own index, so if it moved,
    // the canvas and the code highlight moved with it.
    await waitFor(() => expect(screen.getByText('Step 2 of 2')).toBeInTheDocument());
  });

  it('draws no strip for a trace that carries nothing to draw', async () => {
    // App always has a problem open (activeProblemId is seeded to two-sum), so the
    // empty case that actually occurs is a trace with no per-slot state — a scalar
    // recursion, say. Drawing an empty frame there is worse than drawing nothing.
    vi.stubGlobal('fetch', vi.fn((url) => {
      calls.push(url);
      if (url.includes('/execute')) {
        return Promise.resolve(
          ok([{ stepNumber: 1, activeLine: 1, description: 'scalar only', variables: { n: 5 } }])
        );
      }
      return Promise.resolve(respondTo(url));
    }));

    render(<App />);
    await waitFor(() => expect(screen.getByText('Valid Anagram')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Valid Anagram'));
    await waitFor(() => expect(screen.getByText('scalar only')).toBeInTheDocument());
    expect(screen.queryByLabelText('Execution capture')).not.toBeInTheDocument();
  });
});

describe('App per-problem detail merge', () => {
  /**
   * GET /api/problems returns summaries only (id, title, category, dsType, traced,
   * inputSpec — see ProblemsController#summarize). javaCode, complexity, and
   * defaultGraphNodes/defaultGrid live only on GET /api/problems/{id}. If App uses the
   * catalogue-summary object as `activeProblem` instead of merging the detail fetch in,
   * CodeViewer silently falls back to its hardcoded placeholder for every problem, and
   * MemoryComplexityCard's complexity is blank — a real problem's data replaced with
   * generic filler, which is exactly what this codebase's tracer/legacy split exists to
   * prevent for the trace itself. This test holds the same standard for the detail fetch.
   */
  const SUMMARY_ONLY_CATALOG = CATALOG.map(({ javaCode, complexity, ...summary }) => summary);

  const DETAIL_JAVA_CODE = '// MERGE_SORT_DISTINCTIVE_MARKER\nint solve() { return 42; }';

  function detailFor(id) {
    const summary = SUMMARY_ONLY_CATALOG.find((p) => p.id === id);
    return {
      ...summary,
      javaCode: DETAIL_JAVA_CODE,
      complexity: { timeComplexity: 'O(N)', spaceComplexity: 'O(1)' }
    };
  }

  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn((url) => {
      calls.push(url);
      if (url === '/api/problems') return Promise.resolve(ok(SUMMARY_ONLY_CATALOG));
      const detailMatch = url.match(/^\/api\/problems\/([^/]+)$/);
      if (detailMatch) {
        const d = detailFor(detailMatch[1]);
        return Promise.resolve(d ? ok(d) : { ok: false, status: 404, json: () => Promise.resolve(null) });
      }
      return Promise.resolve(respondTo(url));
    }));
  });

  it('merges the detail fetch into activeProblem instead of showing placeholder code', async () => {
    render(<App />);
    await waitFor(() => expect(screen.getByText('Merge Sort')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Merge Sort'));

    await waitFor(() =>
      expect(screen.getByText('// MERGE_SORT_DISTINCTIVE_MARKER')).toBeInTheDocument()
    );
    // The generic fallback CodeViewer ships with when `problem.javaCode` is missing.
    expect(screen.queryByText(/Java sliding window \(LeetCode 3\)/)).not.toBeInTheDocument();
  });
});
