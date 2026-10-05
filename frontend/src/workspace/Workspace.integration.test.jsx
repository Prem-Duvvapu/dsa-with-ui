import { render, screen, fireEvent, waitFor, cleanup, within } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import React from 'react';
import '@testing-library/jest-dom';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { TOUR_STEPS } from '../components/TourGuide';
import ProblemWorkspace from './ProblemWorkspace';

/**
 * The problem page's integration suite, ported from the retired App shell's
 * App.integration.test.jsx (IMPLEMENTATION_HANDOFF.md §11: a moved feature is not a removed
 * feature). Every behaviour that suite guarded is still asserted here, reached through its
 * new location:
 *
 *   sidebar click to select a problem  → the Switch problem dialog
 *   header "N/N runnable" badge        → switcher search over the merged catalogue
 *   code panel / complexity toggles    → the Code walkthrough and Analysis views
 *   mobile drawer                      → the phone Menu holding the same navigation
 *   speed buttons                      → the Speed menu
 */

/** Stands in for the library: shows where "View all" landed. */
function LibraryProbe() {
  const location = useLocation();
  return <p data-testid="library-probe">{location.pathname}{location.search}</p>;
}

function renderApp(path = '/problem/two-sum') {
  return render(
    <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        <Route path="/" element={<LibraryProbe />} />
        <Route path="/problem/:id" element={<ProblemWorkspace />} />
      </Routes>
    </MemoryRouter>
  );
}

function problem(id, title, category, dsType = 'Array') {
  return {
    id, title, category, difficulty: 'Easy', dsType, traced: true,
    javaCode: 'int solve() {\n    return 0;\n}',
    complexity: { timeComplexity: 'O(N)', spaceComplexity: 'O(1)' }
  };
}

const CATALOG = [
  problem('bfs-traversal', 'BFS Traversal', 'Graphs', 'Graph'),
  problem('dijkstra', 'Dijkstra', 'Graphs', 'Graph'),
  problem('tree-preorder', 'Preorder Traversal', 'Binary Trees', 'Tree'),
  problem('merge-sort', 'Merge Sort', 'Sorting Algorithms', 'Array'),
  problem('two-sum', 'Two Sum', 'Arrays', 'Array'),
  problem('valid-anagram', 'Valid Anagram', 'Strings', 'Array'),
  problem('kth-largest', 'Kth Largest', 'Heaps', 'Array'),
  problem('min-stack', 'Min Stack', 'Stack & Queue', 'Stack'),
  problem('jump-game', 'Jump Game', 'Greedy', 'Array'),
  problem('count-digits', 'Count Digits', 'Basic Maths', 'Array'),
  problem('print-1-to-n', 'Print 1 To N', 'Basic Recursion', 'Array')
];

function stepsFor(id) {
  const row = (states) => states.map((state, index) => ({ index, value: index, state }));
  return [
    { stepNumber: 1, activeLine: 1, description: `${id} step one`, variables: {}, dsType: 'Array', arrayState: row(['default', 'default']) },
    { stepNumber: 2, activeLine: 2, description: `${id} step two`, variables: {}, dsType: 'Array', arrayState: row(['comparing', 'default']) }
  ];
}

let calls;
let deferred;
const ok = (body) => ({ ok: true, status: 200, json: () => Promise.resolve(body) });
const notFound = () => ({ ok: false, status: 404, json: () => Promise.resolve(null) });

function respondTo(url) {
  if (url === '/api/problems') return ok(CATALOG);
  const detail = url.match(/^\/api\/problems\/([^/]+)$/);
  if (detail) {
    const found = CATALOG.find((p) => p.id === detail[1]);
    return found ? ok(found) : notFound();
  }
  const exec = url.match(/^\/api\/problems\/([^/]+)\/execute$/);
  if (exec) {
    const found = CATALOG.find((p) => p.id === exec[1]);
    return found ? ok(stepsFor(found.id)) : notFound();
  }
  return notFound();
}

beforeEach(() => {
  calls = [];
  deferred = new Map();
  vi.stubGlobal('fetch', vi.fn((url) => {
    calls.push(url);
    if (deferred.has(url)) return new Promise((resolve) => deferred.set(url, () => resolve(respondTo(url))));
    return Promise.resolve(respondTo(url));
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

const narration = () => document.querySelector('p[aria-live="polite"]');

async function switchTo(title) {
  fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
  const dialog = screen.getByRole('dialog', { name: /command palette/i });
  fireEvent.change(within(dialog).getByRole('combobox'), { target: { value: title } });
  // Matches are highlighted, so the title is split across <mark>s: choose by accessible name.
  fireEvent.click(within(dialog).getAllByRole('option', { name: (name) => name.startsWith(title) })[0]);
}

describe('Workspace catalogue loading', () => {
  it('renders without crashing and shows the product before the network responds', () => {
    deferred.set('/api/problems', null);
    renderApp();
    expect(screen.getByText('DSA Visualizer')).toBeInTheDocument();
  });

  it('fetches the catalogue from the single v2 endpoint', async () => {
    renderApp();
    await waitFor(() => expect(calls).toContain('/api/problems'));
  });

  it('knows the whole merged catalogue, not just the problems a search happens to match', async () => {
    // The retired App suite asserted "11/11 runnable" in the header; the merged total is now
    // stated by the guided tour, which reads the same catalogue length.
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.click(screen.getAllByRole('button', { name: 'Help' })[0]);
    fireEvent.click(screen.getByRole('button', { name: 'Take the guided tour' }));
    expect(await screen.findByText(new RegExp(`^${CATALOG.length} problems, each with a real execution trace`))).toBeInTheDocument();
  });

  it('merges every category into the switcher, including Maths and Basic Recursion', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
    const dialog = screen.getByRole('dialog', { name: /command palette/i });
    fireEvent.change(within(dialog).getByRole('combobox'), { target: { value: 'Count Digits' } });
    expect(within(dialog).getByRole('option', { name: /^Count Digits/ })).toBeInTheDocument();
    fireEvent.change(within(dialog).getByRole('combobox'), { target: { value: 'Print 1' } });
    expect(within(dialog).getByRole('option', { name: /^Print 1 To N/ })).toBeInTheDocument();
  });

  it('de-duplicates problems with repeated ids', async () => {
    vi.stubGlobal('fetch', vi.fn((url) => {
      calls.push(url);
      if (url === '/api/problems') return Promise.resolve(ok([...CATALOG, problem('two-sum', 'Two Sum (Duplicate)', 'Sorting Algorithms')]));
      return Promise.resolve(respondTo(url));
    }));
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
    fireEvent.change(within(screen.getByRole('dialog')).getByRole('combobox'), { target: { value: 'Two Sum' } });
    expect(screen.queryByText('Two Sum (Duplicate)')).not.toBeInTheDocument();
  });
});

describe('Workspace problem selection', () => {
  it('issues exactly one detail and one execute request per selection', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    calls.length = 0;
    await switchTo('Valid Anagram');
    await waitFor(() => expect(calls.filter((u) => u === '/api/problems/valid-anagram/execute')).toHaveLength(1));
    expect(calls.filter((u) => u === '/api/problems/valid-anagram')).toHaveLength(1);
  });

  it('renders the selected problem\'s steps', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    await switchTo('Min Stack');
    await screen.findByText('min-stack step one');
  });

  it('discards a slow response that is superseded by a newer selection', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    deferred.set('/api/problems/kth-largest/execute', null);
    await switchTo('Kth Largest');
    await waitFor(() => expect(deferred.get('/api/problems/kth-largest/execute')).toBeTypeOf('function'));
    await switchTo('Jump Game');
    await screen.findByText('jump-game step one');
    deferred.get('/api/problems/kth-largest/execute')();
    await waitFor(() => expect(screen.getByText('jump-game step one')).toBeInTheDocument());
    expect(screen.queryByText('kth-largest step one')).not.toBeInTheDocument();
  });

  it('closes without resetting the run when the current problem is chosen again', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    const before = calls.length;
    await switchTo('Two Sum');
    expect(screen.queryByRole('dialog', { name: /command palette/i })).not.toBeInTheDocument();
    expect(narration()).toHaveTextContent('two-sum step two');
    expect(calls.length).toBe(before);
  });
});

describe('Workspace execution capture', () => {
  async function openValidAnagram() {
    renderApp('/problem/valid-anagram');
    await waitFor(() => expect(screen.getByLabelText('Execution capture')).toBeInTheDocument());
  }

  it('keeps the whole run as a strip in the execution history', async () => {
    await openValidAnagram();
    expect(screen.getAllByRole('button', { name: /^Step \d+ of 2$/ })).toHaveLength(2);
  });

  it('frames the canvas in the shared shell, without a key to marks it does not draw', async () => {
    const { container } = renderApp('/problem/valid-anagram');
    await screen.findByText('valid-anagram step one');
    expect(screen.queryByText('happening now')).not.toBeInTheDocument();
    expect(container.querySelectorAll('.shell-head')).toHaveLength(1);
  });

  it('seeking on the strip moves the visualization, not just the strip', async () => {
    await openValidAnagram();
    fireEvent.click(screen.getByRole('button', { name: 'Step 2 of 2' }));
    await waitFor(() => expect(narration()).toHaveTextContent('valid-anagram step two'));
  });

  it('draws no strip for a trace that carries nothing to draw', async () => {
    vi.stubGlobal('fetch', vi.fn((url) => {
      calls.push(url);
      if (url.includes('/execute')) return Promise.resolve(ok([{ stepNumber: 1, activeLine: 1, description: 'scalar only', variables: { n: 5 } }]));
      return Promise.resolve(respondTo(url));
    }));
    renderApp('/problem/valid-anagram');
    await screen.findByText('scalar only');
    expect(screen.queryByLabelText('Execution capture')).not.toBeInTheDocument();
  });

  it.each([
    ['DpTable', { dpTable: { rowLabels: ['dp'], colLabels: ['0', '1'], cells: [[{ value: '1', state: 'read' }, { value: '2', state: 'probe' }]] } }, 'fill LIS table'],
    ['Graph', { nodeStates: { 0: 'queued' }, graphNodes: [{ id: 0, label: '0', x: 10, y: 10, state: 'queued' }], graphEdges: [], queueOrStackState: ['0'] }, 'seed the queue'],
    ['Tree', { treeNodes: [{ id: 0, val: 1, x: 0, y: 0, state: 'visiting' }] }, 'visit the root']
  ])('gives %s traces the full stage instead of an execution capture', async (dsType, payload, description) => {
    const entry = problem('hero', 'Hero', 'Test', dsType);
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/problems') return Promise.resolve(ok([entry]));
      if (url === '/api/problems/hero') return Promise.resolve(ok(entry));
      if (url === '/api/problems/hero/execute') {
        // The legacy arrayState keeps the test honest: the strip COULD render it.
        return Promise.resolve(ok([{ stepNumber: 1, activeLine: 1, description, variables: {}, dsType, arrayState: [{ index: 0, value: 1, state: 'current' }], ...payload }]));
      }
      return Promise.resolve(notFound());
    }));
    const { container } = renderApp('/problem/hero');
    await screen.findByText(description);
    expect(screen.queryByLabelText('Execution capture')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Play' })).toBeInTheDocument();
    // The hero itself is on the stage, framed once (restored from the retired App suite,
    // which asserted the DP table and the single shell explicitly). The five-state key
    // appears only on the DP table, the one canvas that draws those marks.
    if (dsType === 'DpTable') expect(screen.getByRole('table', { name: 'Dynamic programming table' })).toBeInTheDocument();
    expect(screen.queryAllByText('happening now')).toHaveLength(dsType === 'DpTable' ? 1 : 0);
    expect(container.querySelectorAll('.shell-head')).toHaveLength(1);
  });

  it('shows an explicit empty state for an unknown dsType instead of an array', async () => {
    const entry = problem('unknown-shape', 'Unknown Shape', 'Test', 'Mystery');
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/problems') return Promise.resolve(ok([entry]));
      if (url === '/api/problems/unknown-shape') return Promise.resolve(ok(entry));
      if (url === '/api/problems/unknown-shape/execute') {
        return Promise.resolve(ok([{ stepNumber: 1, activeLine: 1, description: 'unknown shape step', variables: {}, dsType: 'Mystery', arrayState: [{ index: 0, value: 99, state: 'current' }] }]));
      }
      return Promise.resolve(notFound());
    }));
    renderApp('/problem/unknown-shape');
    await screen.findByText('No visualization for Mystery');
  });
});

describe('Workspace trace error surface', () => {
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
    renderApp('/problem/broken-trace');
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent(message));
    expect(screen.getByRole('slider', { name: 'Seek to step' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Play' })).toBeDisabled();
    expect(screen.getByText('No trace steps available.')).toBeInTheDocument();
  });
});

describe('Workspace per-problem detail merge', () => {
  it('shows the detail endpoint\'s code instead of placeholder code', async () => {
    const summaryOnly = CATALOG.map(({ javaCode, complexity, ...summary }) => summary);
    vi.stubGlobal('fetch', vi.fn((url) => {
      calls.push(url);
      if (url === '/api/problems') return Promise.resolve(ok(summaryOnly));
      if (url === '/api/problems/merge-sort') {
        return Promise.resolve(ok({ ...summaryOnly.find((p) => p.id === 'merge-sort'), javaCode: '// MERGE_SORT_DISTINCTIVE_MARKER\nint solve() { return 42; }' }));
      }
      return Promise.resolve(respondTo(url));
    }));
    renderApp('/problem/merge-sort?view=code');
    await screen.findByText('// MERGE_SORT_DISTINCTIVE_MARKER');
  });
});

describe('Workspace input editor', () => {
  const TWO_SUM = {
    id: 'two-sum', title: 'Two Sum', category: 'Arrays', difficulty: 'Easy', dsType: 'Array', traced: true,
    javaCode: 'int solve() {\n    return 0;\n}', complexity: { timeComplexity: 'O(N)', spaceComplexity: 'O(1)' },
    inputSpec: { fields: [
      { name: 'nums', label: 'Array', type: 'INT_ARRAY', defaultValue: [2, 7, 11, 15], help: '', constraints: { minLength: 2, maxLength: 10, minValue: -100, maxValue: 100 } },
      { name: 'target', label: 'Target sum', type: 'INT', defaultValue: 9, help: '', constraints: { min: -100, max: 100 } }
    ], maxSteps: 5000, maxBytes: 2000000 }
  };
  function stepsForInput(nums, target) {
    return {
      encoding: 'delta', truncated: false, resolvedInput: { nums, target },
      anchors: { init: 2, complement: 4, found: 6, remember: 8, none: 10 },
      steps: [{ stepNumber: 1, activeLine: 1, keyframe: true, dsType: 'Array', variables: {},
        description: `custom run nums=${JSON.stringify(nums)} target=${target}`,
        arrayState: nums.map((v, i) => ({ index: i, value: v, state: 'default' })) }]
    };
  }
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn((url, opts) => {
      calls.push(url);
      if (url === '/api/problems') return Promise.resolve(ok([TWO_SUM]));
      if (url === '/api/problems/two-sum') return Promise.resolve(ok(TWO_SUM));
      if (url === '/api/problems/two-sum/execute' && opts?.method === 'POST') {
        const body = JSON.parse(opts.body);
        if (typeof body.target !== 'number' || body.target > 100) {
          return Promise.resolve({ ok: false, status: 400, json: () => Promise.resolve({ error: 'invalid_input', fieldErrors: { target: 'Must be at most 100.' } }) });
        }
        return Promise.resolve(ok(stepsForInput(body.nums, body.target)));
      }
      if (url === '/api/problems/two-sum/execute') return Promise.resolve(ok(stepsForInput([2, 7, 11, 15], 9)));
      return Promise.resolve(notFound());
    }));
  });
  const opened = () => screen.findByText('custom run nums=[2,7,11,15] target=9');

  it('keeps setup out of the stage: the editor follows playback, complexity lives in Analysis', async () => {
    renderApp();
    await opened();
    const stage = document.querySelector('[data-audit="stage"]');
    const editor = screen.getByRole('region', { name: 'Try your own input' });
    expect(stage.compareDocumentPosition(editor) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(screen.queryByRole('region', { name: 'Algorithm complexity' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: 'Analysis' }));
    expect(screen.getByRole('region', { name: 'Algorithm complexity' })).toBeInTheDocument();
  });

  it('still says which input the animation is running on', async () => {
    renderApp();
    await opened();
    expect(screen.getByTestId('input-summary')).toHaveTextContent(/target\s*9/);
  });

  it('says which branches the current input never took', async () => {
    renderApp('/problem/two-sum?view=code');
    await opened();
    expect(screen.getByTestId('unreached-note').textContent).toMatch(/branch(es)? not taken/);
  });

  it('gives the canvas state a text alternative', async () => {
    renderApp();
    await opened();
    expect(screen.getByTestId('step-state-summary').textContent).toContain('Array of 4: 2, 7, 11, 15.');
  });

  it('runs a custom input through POST /execute', async () => {
    renderApp();
    await opened();
    fireEvent.change(screen.getByLabelText('Target sum'), { target: { value: '13' } });
    fireEvent.click(screen.getByRole('button', { name: 'Run input' }));
    await screen.findByText('custom run nums=[2,7,11,15] target=13');
  });

  it('shows a rejected field error inline without blanking the current animation', async () => {
    renderApp();
    await opened();
    fireEvent.change(screen.getByLabelText('Target sum'), { target: { value: '999' } });
    fireEvent.click(screen.getByRole('button', { name: 'Run input' }));
    await waitFor(() => expect(screen.getAllByText(/Must be at most 100\./).length).toBeGreaterThan(0));
    expect(screen.getByText('custom run nums=[2,7,11,15] target=9')).toBeInTheDocument();
  });

  it('Restore defaults repopulates the form with the spec defaults', async () => {
    renderApp();
    await opened();
    fireEvent.change(screen.getByLabelText('Target sum'), { target: { value: '77' } });
    expect(screen.getByLabelText('Target sum').value).toBe('77');
    fireEvent.click(screen.getByRole('button', { name: 'Restore defaults' }));
    expect(screen.getByLabelText('Target sum').value).toBe('9');
  });

  it('anchors every tour step to an element the page actually renders', async () => {
    // Steps point at data-tour attributes so that a redesign has to break them deliberately
    // - and this is what makes "deliberately" mean "a red test".
    renderApp();
    await opened();
    const missing = TOUR_STEPS.map((s) => s.target).filter((t) => document.querySelector(`[data-tour="${t}"]`) === null);
    expect(missing, 'tour steps pointing at anchors no longer in the DOM').toEqual([]);
  });
});

describe('Workspace guidance and keyboard', () => {
  it('leaves Focus before touring, so every step points at something on screen', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.click(screen.getByRole('button', { name: 'Focus' }));
    fireEvent.keyDown(window, { key: '?', code: 'Slash' });
    fireEvent.click(screen.getByRole('button', { name: 'Take the guided tour' }));
    expect(await screen.findByTestId('tour-guide')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Exit focus' })).not.toBeInTheDocument();
    expect(screen.getByRole('dialog', { name: TOUR_STEPS[0].title })).toBeInTheDocument();
  });

  it('offers the tour on a phone too', async () => {
    const original = window.innerWidth;
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 390 });
    try {
      renderApp();
      await screen.findByText('two-sum step one');
      fireEvent.click(screen.getAllByRole('button', { name: 'Help' })[0]);
      expect(screen.getByRole('button', { name: 'Take the guided tour' })).toBeInTheDocument();
    } finally {
      Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: original });
    }
  });

  it('opens the tour from Help and walks through it', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.click(screen.getAllByRole('button', { name: 'Help' })[0]);
    fireEvent.click(screen.getByRole('button', { name: 'Take the guided tour' }));
    expect(await screen.findByTestId('tour-guide')).toBeInTheDocument();
    const tour = within(screen.getByRole('dialog', { name: TOUR_STEPS[0].title }));
    fireEvent.click(tour.getByRole('button', { name: 'Next' }));
    expect(screen.getByText(TOUR_STEPS[1].title)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Skip' }));
    await waitFor(() => expect(screen.queryByTestId('tour-guide')).not.toBeInTheDocument());
  });

  it('greets a genuine first visitor, once', async () => {
    window.localStorage.removeItem('dsa-ui:seenWelcome');
    renderApp();
    expect(await screen.findByTestId('welcome-guide')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Start exploring/i }));
    await waitFor(() => expect(screen.queryByTestId('welcome-guide')).not.toBeInTheDocument());
    cleanup();
    renderApp();
    await waitFor(() => expect(calls).toContain('/api/problems'));
    expect(screen.queryByTestId('welcome-guide')).not.toBeInTheDocument();
  });

  it('is reachable again from the shortcut panel after being dismissed', async () => {
    renderApp();
    await waitFor(() => expect(calls).toContain('/api/problems'));
    fireEvent.keyDown(window, { key: '?' });
    fireEvent.click(await screen.findByRole('button', { name: /Show the introduction again/i }));
    expect(await screen.findByTestId('welcome-guide')).toBeInTheDocument();
  });

  it('opens the shortcut list with ? and closes it with Escape', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.keyDown(window, { key: '?' });
    expect(await screen.findByRole('dialog', { name: 'Keyboard shortcuts' })).toBeInTheDocument();
    fireEvent.keyDown(window, { code: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Keyboard shortcuts' })).not.toBeInTheDocument());
  });

  it('keeps stepping with J and L after a control button has been clicked', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    const restart = screen.getByRole('button', { name: 'Restart' });
    restart.focus();
    fireEvent.keyDown(window, { code: 'KeyL' });
    await waitFor(() => expect(narration()).toHaveTextContent('two-sum step two'));
    fireEvent.keyDown(window, { code: 'KeyJ' });
    await waitFor(() => expect(narration()).toHaveTextContent('two-sum step one'));
  });

  it('remembers the playback speed across a reload', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.change(screen.getByRole('combobox', { name: 'Speed' }), { target: { value: '250' } });
    expect(window.localStorage.getItem('dsa-ui:speed')).toBe('250');
    cleanup();
    renderApp();
    await screen.findByText('two-sum step one');
    expect(screen.getByRole('combobox', { name: 'Speed' })).toHaveValue('250');
  });
});

describe('Workspace catalogue error surface', () => {
  it('shows a visible error and a Retry when the catalogue fetch fails', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('network down'))));
    renderApp();
    await waitFor(() => expect(screen.getAllByRole('alert').some((a) => /could not reach the backend/i.test(a.textContent))).toBe(true));
    expect(screen.getByText('DSA Visualizer')).toBeInTheDocument();
  });

  it('Retry clears the notice once the catalogue loads', async () => {
    let shouldFail = true;
    vi.stubGlobal('fetch', vi.fn((url) => (shouldFail ? Promise.reject(new Error('down')) : Promise.resolve(respondTo(url)))));
    renderApp();
    await screen.findByRole('button', { name: 'Retry loading the catalogue' });
    shouldFail = false;
    fireEvent.click(screen.getByRole('button', { name: 'Retry loading the catalogue' }));
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Retry loading the catalogue' })).not.toBeInTheDocument());
  });
});

describe('Workspace on a phone', () => {
  const ORIGINAL_WIDTH = window.innerWidth;
  beforeEach(() => { Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 390 }); });
  afterEach(() => { Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: ORIGINAL_WIDTH }); });

  it('starts with the statement collapsed and the stage on the page', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    expect(document.querySelector('details[open]')).toBeNull();
    expect(document.querySelector('[data-audit="stage"]')).toBeInTheDocument();
  });

  it('keeps every problem reachable from the Menu, and Escape closes the switcher even while typing', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    const menu = screen.getByText('Menu').closest('details');
    fireEvent.click(within(menu).getByText('Menu'));
    fireEvent.click(within(menu).getByRole('button', { name: /Switch problem/ }));
    const dialog = screen.getByRole('dialog', { name: /command palette/i });
    within(dialog).getByRole('combobox').focus();
    fireEvent.keyDown(window, { code: 'Escape' });
    expect(screen.queryByRole('dialog', { name: /command palette/i })).not.toBeInTheDocument();
    expect(within(menu).getByRole('link', { name: 'All algorithms' })).toHaveAttribute('href', '/');
  });

  it('still offers the code, the analysis and the editor on a phone', async () => {
    const spec = { fields: [{ name: 'n', label: 'Count', type: 'INT', defaultValue: 1 }] };
    vi.stubGlobal('fetch', vi.fn((url) => {
      calls.push(url);
      if (url === '/api/problems') return Promise.resolve(ok(CATALOG.map((p) => (p.id === 'two-sum' ? { ...p, inputSpec: spec } : p))));
      if (url === '/api/problems/two-sum') return Promise.resolve(ok({ ...CATALOG.find((p) => p.id === 'two-sum'), inputSpec: spec }));
      return Promise.resolve(respondTo(url));
    }));
    renderApp();
    await screen.findByText('two-sum step one');
    expect(screen.getByRole('region', { name: 'Try your own input' })).toBeInTheDocument();
    expect(screen.getByLabelText('Count')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: 'Code walkthrough' }));
    // The line is split into syntax-coloured spans, so assert the source's text, not one node.
    expect(screen.getByRole('region', { name: 'Java source' })).toHaveTextContent('int solve() {');
    fireEvent.click(screen.getByRole('tab', { name: 'Analysis' }));
    expect(screen.getByRole('region', { name: 'Algorithm complexity' })).toHaveTextContent('O(N)');
  });
});

describe('Workspace execution history (P6a)', () => {
  it('lists the steps of a graph run - a family with no capture strip - and seeks from the list', async () => {
    const graphStep = (n, text) => ({
      stepNumber: n, activeLine: n, description: text, variables: {}, dsType: 'Graph',
      graphNodes: [{ id: 0, label: '0', x: 10, y: 10, state: 'queued' }], graphEdges: [], nodeStates: { 0: 'queued' }
    });
    vi.stubGlobal('fetch', vi.fn((url) => {
      calls.push(url);
      if (url === '/api/problems/bfs-traversal/execute') {
        return Promise.resolve(ok([graphStep(1, 'bfs-traversal step one'), graphStep(2, 'bfs-traversal step two')]));
      }
      return Promise.resolve(respondTo(url));
    }));
    renderApp('/problem/bfs-traversal');
    await screen.findByText('bfs-traversal step one');
    const before = calls.filter((u) => u.endsWith('/execute')).length;
    const history = screen.getByText('Execution history').closest('details');
    fireEvent.click(within(history).getByText('Execution history'));
    expect(await within(history).findByText(/^Steps 1–\d+ of \d+/)).toBeInTheDocument();
    expect(within(history).queryByLabelText(/Execution capture/)).toBeNull();
    const second = within(history).getByRole('button', { name: /^Step 2/ });
    fireEvent.click(second);
    expect(narration()).toHaveTextContent('bfs-traversal step two');
    expect(second).toHaveAttribute('aria-current', 'step');
    expect(calls.filter((u) => u.endsWith('/execute'))).toHaveLength(before);
  });
});

describe('Workspace end of the run (P6b)', () => {
  const executes = () => calls.filter((u) => u.endsWith('/execute'));
  function withCatalog(mapProblem, mapExecute = () => null) {
    vi.stubGlobal('fetch', vi.fn((url, init) => {
      calls.push(url);
      if (url === '/api/problems') return Promise.resolve(ok(CATALOG.map(mapProblem)));
      const detail = url.match(/^\/api\/problems\/([^/]+)$/);
      if (detail) {
        const found = CATALOG.map(mapProblem).find((p) => p.id === detail[1]);
        return Promise.resolve(found ? ok(found) : notFound());
      }
      const custom = mapExecute(url, init);
      return Promise.resolve(custom ?? respondTo(url));
    }));
  }

  it('offers Replay and the next problem only at the end of a complete run', async () => {
    withCatalog((p) => (['two-sum', 'valid-anagram'].includes(p.id) ? { ...p, striverSheetSection: 'Warm-up' } : p));
    renderApp();
    await screen.findByText('two-sum step one');
    expect(screen.queryByRole('group', { name: 'End of the run' })).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    const end = screen.getByRole('group', { name: 'End of the run' });
    expect(within(end).getByRole('button', { name: /^Next: Valid Anagram/ })).toBeInTheDocument();
    fireEvent.click(within(end).getByRole('button', { name: 'Replay' }));
    expect(narration()).toHaveTextContent('two-sum step one');
    expect(screen.getByRole('button', { name: 'Pause' })).toBeInTheDocument();
    expect(screen.queryByRole('group', { name: 'End of the run' })).toBeNull();
  });

  it('points to the library at the end of the last problem in a section', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    const end = screen.getByRole('group', { name: 'End of the run' });
    expect(within(end).getByRole('link', { name: 'Browse all algorithms' })).toHaveAttribute('href', '/');
  });

  it('never calls a truncated run finished', async () => {
    withCatalog((p) => p, (url) => (url === '/api/problems/two-sum/execute'
      ? ok({ encoding: 'full', truncated: true, steps: stepsFor('two-sum') })
      : null));
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(narration()).toHaveTextContent('two-sum step two');
    expect(screen.queryByRole('group', { name: 'End of the run' })).toBeNull();
    expect(screen.getByText(/hit the step budget/)).toBeInTheDocument();
  });

  it('runs the declared other case from the end of the run', async () => {
    const spec = { fields: [{ name: 'n', label: 'Count', type: 'INT', defaultValue: 1 }] };
    const bodies = [];
    withCatalog((p) => (p.id === 'two-sum' ? { ...p, inputSpec: spec, alternateInput: { n: 9 } } : p), (url, init) => {
      if (url === '/api/problems/two-sum/execute') bodies.push(init?.body ? JSON.parse(init.body) : null);
      return null;
    });
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    fireEvent.click(within(screen.getByRole('group', { name: 'End of the run' })).getByRole('button', { name: 'Run the other case' }));
    await waitFor(() => expect(bodies.at(-1)).toEqual({ n: 9 }));
    expect(screen.getByLabelText('Count')).toHaveValue(9);
  });
});

describe('Workspace Focus mode (P5b)', () => {
  const spec = { fields: [{ name: 'n', label: 'Count', type: 'INT', defaultValue: 1 }] };
  function withEditor() {
    vi.stubGlobal('fetch', vi.fn((url) => {
      calls.push(url);
      if (url === '/api/problems') return Promise.resolve(ok(CATALOG.map((p) => (p.id === 'two-sum' ? { ...p, inputSpec: spec } : p))));
      if (url === '/api/problems/two-sum') return Promise.resolve(ok({ ...CATALOG.find((p) => p.id === 'two-sum'), inputSpec: spec }));
      return Promise.resolve(respondTo(url));
    }));
  }
  const executes = () => calls.filter((u) => u.endsWith('/execute')).length;

  it('shows only the stage, and keeps the run, the step and an unsaved draft', async () => {
    withEditor();
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    fireEvent.change(screen.getByLabelText('Count'), { target: { value: '7' } });
    expect(screen.getByText('Changes not run')).toBeVisible();
    const before = executes();

    fireEvent.click(screen.getByRole('button', { name: 'Focus' }));
    expect(screen.getByRole('button', { name: 'Exit focus' })).toHaveFocus();
    expect(screen.getByRole('heading', { level: 2, name: /Two Sum/ })).toBeVisible();
    expect(narration()).toHaveTextContent('two-sum step two');
    expect(screen.getByRole('button', { name: 'Play' })).toBeVisible();
    // Everything optional is hidden - but still mounted, so nothing is lost.
    expect(screen.getByLabelText('Count')).not.toBeVisible();
    expect(screen.queryByRole('tab', { name: 'Code walkthrough' })).toBeNull();
    expect(screen.queryByRole('heading', { level: 1 })).toBeNull();

    fireEvent.click(screen.getByRole('button', { name: 'Exit focus' }));
    expect(screen.getByRole('button', { name: 'Focus' })).toHaveFocus();
    expect(screen.getByLabelText('Count')).toHaveValue(7);
    expect(screen.getByText('Changes not run')).toBeVisible();
    expect(narration()).toHaveTextContent('two-sum step two');
    expect(executes()).toBe(before);
  });

  it('pauses when entering and when leaving', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.click(screen.getByRole('button', { name: 'Play' }));
    expect(screen.getByRole('button', { name: 'Pause' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Focus' }));
    expect(screen.getByRole('button', { name: 'Play' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Play' }));
    fireEvent.click(screen.getByRole('button', { name: 'Exit focus' }));
    expect(screen.getByRole('button', { name: 'Play' })).toBeInTheDocument();
  });

  it('leaves on Escape only once a dialog above it has closed', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.click(screen.getByRole('button', { name: 'Focus' }));
    fireEvent.keyDown(window, { key: '?', code: 'Slash' });
    expect(screen.getByRole('dialog', { name: 'Keyboard shortcuts' })).toBeInTheDocument();
    fireEvent.keyDown(window, { code: 'Escape', key: 'Escape' });
    expect(screen.queryByRole('dialog', { name: 'Keyboard shortcuts' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Exit focus' })).toBeInTheDocument();
    fireEvent.keyDown(window, { code: 'Escape', key: 'Escape' });
    expect(screen.queryByRole('button', { name: 'Exit focus' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Focus' })).toHaveFocus();
  });

  it('ends when another problem is chosen', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    fireEvent.click(screen.getByRole('button', { name: 'Focus' }));
    await switchTo('Valid Anagram');
    await screen.findByText('valid-anagram step one');
    expect(screen.queryByRole('button', { name: 'Exit focus' })).not.toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Code walkthrough' })).toBeVisible();
  });
});

describe('Workspace switcher', () => {
  it('is closed until Cmd/Ctrl+K opens it, with the query focused', async () => {
    renderApp();
    await waitFor(() => expect(calls).toContain('/api/problems'));
    expect(screen.queryByRole('dialog', { name: /command palette/i })).not.toBeInTheDocument();
    fireEvent.keyDown(window, { key: 'k', metaKey: true });
    const dialog = screen.getByRole('dialog', { name: /command palette/i });
    expect(within(dialog).getByRole('combobox')).toHaveFocus();
  });

  it('closes on Escape and on the backdrop', async () => {
    renderApp();
    await waitFor(() => expect(calls).toContain('/api/problems'));
    fireEvent.keyDown(window, { key: 'k', metaKey: true });
    fireEvent.keyDown(window, { code: 'Escape' });
    expect(screen.queryByRole('dialog', { name: /command palette/i })).not.toBeInTheDocument();
    fireEvent.keyDown(window, { key: 'k', metaKey: true });
    fireEvent.click(screen.getByTestId('command-palette-backdrop'));
    expect(screen.queryByRole('dialog', { name: /command palette/i })).not.toBeInTheDocument();
  });

  it('jumps straight to a problem chosen from the results, and closes', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    await switchTo('Dijkstra');
    await waitFor(() => expect(screen.queryByRole('dialog', { name: /command palette/i })).not.toBeInTheDocument());
    await screen.findByRole('heading', { level: 1, name: 'Dijkstra' });
  });

  it('makes the page inert while open, and returns focus to the button that opened it', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    const trigger = screen.getAllByRole('button', { name: /Switch problem/ })[0];
    trigger.focus();
    fireEvent.click(trigger);
    const dialog = screen.getByRole('dialog', { name: /command palette/i });
    expect(document.getElementById('workspace-view').closest('[inert]')).not.toBeNull();
    expect(dialog.closest('[inert]')).toBeNull();
    fireEvent.keyDown(within(dialog).getByRole('combobox'), { key: 'Escape' });
    expect(screen.queryByRole('dialog', { name: /command palette/i })).not.toBeInTheDocument();
    expect(document.querySelector('[inert]')).toBeNull();
    expect(trigger).toHaveFocus();
  });

  it('carries the query to the library with View all, without running anything', async () => {
    renderApp();
    await screen.findByText('two-sum step one');
    const before = calls.length;
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
    const dialog = screen.getByRole('dialog', { name: /command palette/i });
    fireEvent.change(within(dialog).getByRole('combobox'), { target: { value: 'sum' } });
    fireEvent.click(within(dialog).getByRole('option', { name: /in the library/ }));
    expect(await screen.findByTestId('library-probe')).toHaveTextContent('/?q=sum');
    expect(document.querySelector('[inert]')).toBeNull();
    expect(calls.slice(before).filter((u) => u.endsWith('/execute'))).toHaveLength(0);
  });

  it('offers quick actions on an empty query, including toggling the theme', async () => {
    renderApp();
    await waitFor(() => expect(calls).toContain('/api/problems'));
    expect(document.documentElement.getAttribute('data-theme')).toBeNull();
    fireEvent.keyDown(window, { key: 'k', metaKey: true });
    fireEvent.click(within(screen.getByRole('dialog', { name: /command palette/i })).getByRole('option', { name: /toggle theme/i }));
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(screen.queryByRole('dialog', { name: /command palette/i })).not.toBeInTheDocument();
  });
});

describe('Workspace comparison panel', () => {
  const TWO_SUM_ALT = { ...problem('two-sum', 'Two Sum', 'Arrays'), alternateInput: { nums: [2, 3, 1], target: 5 } };
  function tinyTrace(n) {
    return { encoding: 'full', resolvedInput: { n }, steps: Array.from({ length: n }, (_, i) => ({ stepNumber: i + 1, activeLine: 1, description: `compare step ${i + 1}`, arrayState: [{ index: 0, value: i, state: 'default' }] })) };
  }
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn((url, opts) => {
      calls.push(url);
      if (url === '/api/problems') return Promise.resolve(ok([TWO_SUM_ALT]));
      if (url === '/api/problems/two-sum') return Promise.resolve(ok(TWO_SUM_ALT));
      if (url === '/api/problems/two-sum/execute' && opts?.method === 'POST') {
        return Promise.resolve(ok(tinyTrace(Object.keys(JSON.parse(opts.body)).length === 0 ? 2 : 5)));
      }
      if (url === '/api/problems/two-sum/execute') return Promise.resolve(ok(tinyTrace(2)));
      return Promise.resolve(notFound());
    }));
  });

  it('offers a compare toggle when the problem has an alternate input, and fetches nothing until opened', async () => {
    renderApp();
    expect(await screen.findByRole('button', { name: /compare other case/i })).toBeInTheDocument();
    expect(calls.filter((u) => u === '/api/problems/two-sum/execute')).toHaveLength(1);
  });

  it('shows both runs stacked, each with its own step count, once opened; hides on a second click', async () => {
    renderApp();
    fireEvent.click(await screen.findByRole('button', { name: /compare other case/i }));
    expect(await screen.findByRole('heading', { name: /^Default input/ })).toHaveTextContent('2 steps');
    expect(screen.getByRole('heading', { name: /^Other case/ })).toHaveTextContent('5 steps');
    fireEvent.click(screen.getByRole('button', { name: /hide comparison/i }));
    expect(screen.queryByRole('heading', { name: /^Default input/ })).not.toBeInTheDocument();
  });
});
