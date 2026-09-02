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
  problem('bfs-traversal', 'BFS Traversal', 'Graph BFS/DFS', 'Graph'),
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
    // The backend contract already de-duplicates, but a defensive client guard keeps a
    // malformed response from creating duplicate React keys or ambiguous selection.
    await waitFor(() => expect(screen.getByText('18 algorithms')).toBeInTheDocument());
    expect(screen.queryByText('Two Sum (Duplicate)')).not.toBeInTheDocument();
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

  it('gives DP-table traces the full stage instead of rendering an execution capture', async () => {
    const lis = problem(
      'longest-increasing-subsequence',
      'Longest Increasing Subsequence',
      'Dynamic Programming',
      'DpTable'
    );
    const dpTable = {
      rowLabels: ['dp'],
      colLabels: ['0', '1'],
      cells: [[
        { value: '1', state: 'read' },
        { value: '2', state: 'probe' }
      ]]
    };

    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/problems') return Promise.resolve(ok([lis]));
      if (url === '/api/problems/longest-increasing-subsequence') {
        return Promise.resolve(ok(lis));
      }
      if (url === '/api/problems/longest-increasing-subsequence/execute') {
        return Promise.resolve(ok([{
          stepNumber: 1,
          activeLine: 1,
          description: 'fill LIS table',
          variables: {},
          dsType: 'DpTable',
          dpTable,
          // This legacy payload keeps the test honest: CaptureStrip could render it.
          arrayState: [{ index: 0, value: 1, state: 'current' }]
        }]));
      }
      return Promise.resolve({ ok: false, status: 404, json: () => Promise.resolve(null) });
    }));

    const { container } = render(<App />);

    await waitFor(() =>
      expect(screen.getByRole('table', { name: 'Dynamic programming table' })).toBeInTheDocument()
    );
    expect(screen.queryByLabelText('Execution capture')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Play' })).toBeInTheDocument();
    expect(screen.getAllByText('happening now')).toHaveLength(1);
    expect(container.querySelectorAll('.shell-head')).toHaveLength(1);
  });

  it('shows an explicit empty state for an unknown dsType instead of an array', async () => {
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/problems') {
        return Promise.resolve(ok([problem('unknown-shape', 'Unknown Shape', 'Test', 'Mystery')]));
      }
      if (url === '/api/problems/unknown-shape') {
        return Promise.resolve(ok(problem('unknown-shape', 'Unknown Shape', 'Test', 'Mystery')));
      }
      if (url === '/api/problems/unknown-shape/execute') {
        return Promise.resolve(ok([{
          stepNumber: 1, activeLine: 1, description: 'unknown shape step',
          variables: {}, dsType: 'Mystery',
          arrayState: [{ index: 0, value: 99, state: 'current' }]
        }]));
      }
      return Promise.resolve({ ok: false, status: 404, json: () => Promise.resolve(null) });
    }));

    render(<App />);

    await waitFor(() =>
      expect(screen.getByText('No visualization for Mystery')).toBeInTheDocument()
    );
    expect(screen.queryByText('Array & bar visualizer')).not.toBeInTheDocument();
  });
});

describe('App trace error surface', () => {
  const broken = problem('broken-trace', 'Broken Trace', 'Test', 'Array');

  it.each([
    ['fetch', () => Promise.reject(new Error('network down')), /could not load this trace/i],
    ['empty', () => Promise.resolve(ok([])), /returned an empty trace/i],
    ['malformed', () => Promise.resolve(ok({ unexpected: true })), /returned a malformed trace/i]
  ])('shows an explicit %s state with inert playback', async (_kind, executeResponse, message) => {
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/problems') return Promise.resolve(ok([broken]));
      if (url === '/api/problems/broken-trace') return Promise.resolve(ok(broken));
      if (url === '/api/problems/broken-trace/execute') return executeResponse();
      return Promise.resolve(respondTo(url));
    }));

    render(<App />);

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent(message));
    expect(screen.getByLabelText('Playback position')).toHaveTextContent('Step 0 of 0');
    expect(screen.getByRole('button', { name: 'Play' })).toBeDisabled();
    expect(screen.getByText('No trace steps available.')).toBeInTheDocument();
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

describe('App input panel', () => {
  const TWO_SUM_WITH_SPEC = {
    id: 'two-sum', title: 'Two Sum', category: 'Arrays', difficulty: 'Easy', dsType: 'Array',
    traced: true, javaCode: 'int solve() {\n    return 0;\n}',
    complexity: { timeComplexity: 'O(N)', spaceComplexity: 'O(1)' },
    inputSpec: {
      fields: [
        {
          name: 'nums', label: 'Array', type: 'INT_ARRAY', defaultValue: [2, 7, 11, 15],
          help: '', constraints: { minLength: 2, maxLength: 10, minValue: -100, maxValue: 100 }
        },
        {
          name: 'target', label: 'Target sum', type: 'INT', defaultValue: 9,
          help: '', constraints: { min: -100, max: 100 }
        }
      ],
      maxSteps: 5000, maxBytes: 2000000
    }
  };

  function stepsForInput(nums, target) {
    return {
      encoding: 'delta',
      truncated: false,
      steps: [{
        stepNumber: 1, activeLine: 1, keyframe: true, dsType: 'Array', variables: {},
        description: `custom run nums=${JSON.stringify(nums)} target=${target}`,
        arrayState: nums.map((v, i) => ({ index: i, value: v, state: 'default' }))
      }]
    };
  }

  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn((url, opts) => {
      calls.push(url);
      if (url === '/api/problems') return Promise.resolve(ok([TWO_SUM_WITH_SPEC]));
      if (url === '/api/problems/two-sum') return Promise.resolve(ok(TWO_SUM_WITH_SPEC));
      if (url === '/api/problems/two-sum/execute' && opts?.method === 'POST') {
        const body = JSON.parse(opts.body);
        if (typeof body.target !== 'number' || body.target > 100) {
          return Promise.resolve({
            ok: false, status: 400,
            json: () => Promise.resolve({
              error: 'invalid_input',
              fieldErrors: { target: 'Must be at most 100.' }
            })
          });
        }
        return Promise.resolve(ok(stepsForInput(body.nums, body.target)));
      }
      if (url === '/api/problems/two-sum/execute') {
        return Promise.resolve(ok(stepsForInput([2, 7, 11, 15], 9)));
      }
      return Promise.resolve({ ok: false, status: 404, json: () => Promise.resolve(null) });
    }));
  });

  /** two-sum is the app's default selection, so the panel is already open on mount. */
  async function openTwoSum() {
    render(<App />);
    await waitFor(() =>
      expect(screen.getByText('custom run nums=[2,7,11,15] target=9')).toBeInTheDocument()
    );
  }

  it('renders an editor from inputSpec and runs a custom input through POST /execute', async () => {
    await openTwoSum();

    fireEvent.change(screen.getByLabelText('Target sum'), { target: { value: '13' } });
    fireEvent.click(screen.getByRole('button', { name: 'Run with this input' }));

    await waitFor(() =>
      expect(screen.getByText('custom run nums=[2,7,11,15] target=13')).toBeInTheDocument()
    );
    const postCall = calls.filter((u) => u === '/api/problems/two-sum/execute');
    expect(postCall.length).toBeGreaterThan(0);
  });

  it('shows a rejected field error inline without blanking the current animation', async () => {
    await openTwoSum();

    fireEvent.change(screen.getByLabelText('Target sum'), { target: { value: '999' } });
    fireEvent.click(screen.getByRole('button', { name: 'Run with this input' }));

    await waitFor(() =>
      expect(screen.getByText('Must be at most 100.')).toBeInTheDocument()
    );
    // The last good run is still on screen — a rejected edit doesn't blank the canvas.
    expect(screen.getByText('custom run nums=[2,7,11,15] target=9')).toBeInTheDocument();
  });

  it('Reset repopulates the form with the spec defaults', async () => {
    await openTwoSum();

    fireEvent.change(screen.getByLabelText('Target sum'), { target: { value: '77' } });
    expect(screen.getByLabelText('Target sum').value).toBe('77');

    fireEvent.click(screen.getByRole('button', { name: 'Reset input to default' }));
    expect(screen.getByLabelText('Target sum').value).toBe('9');
  });
});

describe('App catalogue error surface', () => {
  it('shows a visible error and a Retry when the catalogue fetch fails', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('network down'))));

    render(<App />);

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/could not reach the backend/i)
    );
    // The offline fallback is still usable underneath the banner.
    expect(screen.getByText('DSA Visualizer')).toBeInTheDocument();
  });

  it('Retry clears the banner once the catalogue loads', async () => {
    let shouldFail = true;
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (shouldFail) return Promise.reject(new Error('network down'));
      return Promise.resolve(respondTo(url));
    }));

    render(<App />);
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());

    shouldFail = false;
    fireEvent.click(screen.getByRole('button', { name: /retry/i }));

    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
  });
});

describe('App mobile drawer', () => {
  const ORIGINAL_WIDTH = window.innerWidth;

  beforeEach(() => {
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 480 });
  });

  afterEach(() => {
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: ORIGINAL_WIDTH });
  });

  it('starts closed on a narrow viewport', async () => {
    render(<App />);
    await waitFor(() => expect(calls).toContain('/api/problems'));
    // The sidebar's search box is the drawer's own content; absent means closed.
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });

  it('opens with a backdrop that closes it again on click', async () => {
    render(<App />);
    await waitFor(() => expect(calls).toContain('/api/problems'));

    fireEvent.click(screen.getByLabelText(/menu|sidebar|navigation/i));
    await waitFor(() => expect(screen.getByRole('combobox')).toBeInTheDocument());

    // The backdrop is the only aria-hidden element covering the screen at this point.
    const backdrop = document.querySelector('[aria-hidden="true"]');
    expect(backdrop).toBeTruthy();
    fireEvent.click(backdrop);

    await waitFor(() => expect(screen.queryByRole('combobox')).not.toBeInTheDocument());
  });

  it('Escape closes the drawer even while the search box is focused', async () => {
    render(<App />);
    await waitFor(() => expect(calls).toContain('/api/problems'));

    fireEvent.click(screen.getByLabelText(/menu|sidebar|navigation/i));
    await waitFor(() => expect(screen.getByRole('combobox')).toBeInTheDocument());

    screen.getByRole('combobox').focus();
    fireEvent.keyDown(window, { code: 'Escape' });

    await waitFor(() => expect(screen.queryByRole('combobox')).not.toBeInTheDocument());
  });
});
