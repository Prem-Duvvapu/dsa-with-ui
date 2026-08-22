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
 */

/** The catalogue endpoints App is expected to request on mount (18 legacy + 1 tracer enrichment). */
const EXPECTED_CATALOG_URLS = [
  '/api/graphs/bfs-dfs/problems',
  '/api/graphs/advanced/problems',
  '/api/trees/problems',
  '/api/recursion-backtracking/problems',
  '/api/sorting/problems',
  '/api/arrays/problems',
  '/api/linkedlist/problems',
  '/api/binarysearch/problems',
  '/api/dp/problems',
  '/api/tries/problems',
  '/api/greedy/problems',
  '/api/strings/problems',
  '/api/bitmanipulation/problems',
  '/api/heaps/problems',
  '/api/stackqueue/problems',
  '/api/slidingwindow/problems',
  '/api/maths/problems',
  '/api/basic-recursion/problems',
  '/api/problems'
];

/** Paths the backend has never served. Requesting them silently loses whole categories. */
const PATHS_THAT_DO_NOT_EXIST = [
  '/api/math/basic/problems',
  '/api/recursion/basic/problems'
];

function problem(id, title, category) {
  return {
    id,
    title,
    category,
    difficulty: 'Easy',
    dsType: 'Array',
    javaCode: 'int solve() {\n    return 0;\n}',
    complexity: { timeComplexity: 'O(N)', spaceComplexity: 'O(1)' }
  };
}

/** One problem per endpoint, so the merged catalogue has a known size of 18. */
const CATALOG = {
  '/api/graphs/bfs-dfs': problem('bfs-traversal', 'BFS Traversal', 'Graph BFS/DFS'),
  '/api/graphs/advanced': problem('dijkstra', 'Dijkstra', 'Advanced Graphs'),
  '/api/trees': problem('tree-preorder', 'Preorder Traversal', 'Binary Trees'),
  '/api/recursion-backtracking': problem('n-queens', 'N Queens', 'Recursion'),
  '/api/sorting': problem('merge-sort', 'Merge Sort', 'Sorting Algorithms'),
  '/api/arrays': problem('two-sum', 'Two Sum', 'Arrays'),
  '/api/linkedlist': problem('reverse-linked-list', 'Reverse Linked List', 'Linked List'),
  '/api/binarysearch': problem('bs-1d', 'Binary Search 1D', 'Binary Search'),
  '/api/dp': problem('climbing-stairs', 'Climbing Stairs', 'Dynamic Programming'),
  '/api/tries': problem('implement-trie', 'Implement Trie', 'Tries'),
  '/api/greedy': problem('jump-game', 'Jump Game', 'Greedy'),
  '/api/strings': problem('valid-anagram', 'Valid Anagram', 'Strings'),
  '/api/bitmanipulation': problem('single-number', 'Single Number', 'Bit Manipulation'),
  '/api/heaps': problem('kth-largest', 'Kth Largest', 'Heaps'),
  '/api/stackqueue': problem('min-stack', 'Min Stack', 'Stack & Queue'),
  '/api/slidingwindow': problem('longest-substring', 'Longest Substring', 'Sliding Window'),
  '/api/maths': problem('count-digits', 'Count Digits', 'Basic Maths'),
  '/api/basic-recursion': problem('print-1-to-n', 'Print 1 To N', 'Basic Recursion')
};

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
  return { ok: true, json: () => Promise.resolve(body) };
}

function respondTo(url) {
  for (const [base, prob] of Object.entries(CATALOG)) {
    if (url === `${base}/problems`) return ok([prob]);
    if (url === `${base}/problems/${prob.id}`) return ok(prob);
    if (url === `${base}/execute/${prob.id}`) return ok(stepsFor(prob.id));
  }
  return { ok: false, json: () => Promise.resolve(null) };
}

beforeEach(() => {
  calls = [];
  deferred = new Map();
  vi.stubGlobal('fetch', vi.fn((url) => {
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

const catalogUrls = () => calls.filter((u) => u.endsWith('/problems'));

describe('App catalogue loading', () => {
  it('renders without crashing and shows a problem before the network responds', () => {
    render(<App />);
    // The cold-start fallback: something is on screen immediately, not a black page.
    expect(screen.getByText('DSA Visualizer')).toBeInTheDocument();
  });

  it('requests every catalogue endpoint the backend actually serves', async () => {
    render(<App />);
    await waitFor(() => expect(catalogUrls().length).toBe(EXPECTED_CATALOG_URLS.length));
    expect(catalogUrls().sort()).toEqual([...EXPECTED_CATALOG_URLS].sort());
  });

  it('never requests the two paths the backend does not serve', async () => {
    render(<App />);
    await waitFor(() => expect(catalogUrls().length).toBeGreaterThan(0));
    for (const dead of PATHS_THAT_DO_NOT_EXIST) {
      expect(calls).not.toContain(dead);
    }
  });

  it('merges every category into the catalogue, including Maths and Basic Recursion', async () => {
    render(<App />);
    // Header prints the merged count; 18 endpoints x 1 problem each.
    await waitFor(() => expect(screen.getByText('18 algorithms')).toBeInTheDocument());
    expect(screen.getByText('Count Digits')).toBeInTheDocument();
    expect(screen.getByText('Print 1 To N')).toBeInTheDocument();
  });

  it('de-duplicates problems with repeated ids across endpoints', async () => {
    const duplicateProb = problem('two-sum', 'Two Sum (Duplicate)', 'Sorting Algorithms');
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/sorting/problems') return Promise.resolve(ok([duplicateProb]));
      return Promise.resolve(respondTo(url));
    }));

    render(<App />);
    // When sorting returns duplicate two-sum, 18 endpoints yield 17 unique algorithms
    await waitFor(() => expect(screen.getByText('17 algorithms')).toBeInTheDocument());
  });
});

describe('App problem selection', () => {
  it('issues exactly one detail and one execute request per selection', async () => {
    render(<App />);
    await waitFor(() => expect(screen.getByText('Valid Anagram')).toBeInTheDocument());

    calls.length = 0;
    fireEvent.click(screen.getByText('Valid Anagram'));

    await waitFor(() =>
      expect(calls.filter((u) => u === '/api/strings/execute/valid-anagram')).toHaveLength(1)
    );
    // Previously handleSelectProblem fetched directly AND set the id that retriggers
    // the effect, producing two identical request pairs per click.
    expect(calls.filter((u) => u === '/api/strings/problems/valid-anagram')).toHaveLength(1);
    expect(calls.filter((u) => u === '/api/strings/execute/valid-anagram')).toHaveLength(1);
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
    deferred.set('/api/heaps/execute/kth-largest', null);

    fireEvent.click(screen.getByText('Kth Largest'));
    await waitFor(() => expect(deferred.get('/api/heaps/execute/kth-largest')).toBeTypeOf('function'));

    // A newer selection resolves immediately.
    fireEvent.click(screen.getByText('Jump Game'));
    await waitFor(() => expect(screen.getByText('jump-game step one')).toBeInTheDocument());

    // Now let the stale Heaps response land. It must not overwrite Jump Game.
    deferred.get('/api/heaps/execute/kth-largest')();

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
      if (url.includes('/execute/')) {
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
