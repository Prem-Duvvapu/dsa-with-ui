import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { MemoryRouter, useLocation } from 'react-router-dom';
import AppRouter from '../AppRouter';
import { CANVAS_BY_DSTYPE } from '../canvas/registry';
import { STAGE_BY_DSTYPE } from './stageFamily';

/**
 * The P3 workspace (docs/ui-revamp/playground-concept.png), exercised through the real
 * router with the development flag on. The contract under test is the handoff's §6: one
 * session above the views, so presentation changes never touch the run, step or draft.
 */

let location;
function Probe() { location = useLocation(); return null; }

function renderAt(path) {
  return render(
    <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AppRouter />
      <Probe />
    </MemoryRouter>
  );
}

const ok = (body) => ({ ok: true, status: 200, json: () => Promise.resolve(body) });
const spec = { fields: [{ name: 'n', label: 'Count', type: 'INT', defaultValue: 3, constraints: { min: 0, max: 50 } }] };
const ARRAY = {
  id: 'alpha', title: 'Alpha Search', category: 'Arrays', difficulty: 'Medium', dsType: 'Array', traced: true,
  striverSheetSection: 'Arrays - Easy', javaCode: 'int a() {\n  return 0;\n}', inputSpec: spec,
  description: 'Find the answer quickly. Then explain it.', constraints: ['1 <= n <= 10^5'],
  complexity: { timeComplexity: 'O(N)', spaceComplexity: 'O(1)' }
};
const GRAPH = { ...ARRAY, id: 'beta', title: 'Beta Graph', dsType: 'Graph', description: 'Walk the graph.' };

function trace(id, n, count = 6, extra = {}, dsType = 'Array') {
  return {
    encoding: 'delta', truncated: false, resolvedInput: { n }, anchors: { start: 1 }, ...extra,
    steps: Array.from({ length: count }, (_, i) => ({
      stepNumber: i + 1, activeLine: 1, keyframe: i === 0, dsType, variables: { i },
      description: `${id} n=${n} step ${i + 1}`, arrayState: [{ index: 0, value: i, state: 'default' }]
    }))
  };
}

let executes;
beforeEach(() => {
  executes = [];
  vi.stubGlobal('fetch', vi.fn((url, opts = {}) => {
    if (url === '/api/problems') return Promise.resolve(ok([ARRAY, GRAPH]));
    const detail = url.match(/^\/api\/problems\/([^/]+)$/);
    if (detail) {
      const found = [ARRAY, GRAPH].find((p) => p.id === detail[1]);
      return Promise.resolve(found ? ok(found) : { ok: false, status: 404, json: () => Promise.resolve(null) });
    }
    const exec = url.match(/^\/api\/problems\/([^/]+)\/execute$/);
    const found = [ARRAY, GRAPH].find((p) => p.id === exec?.[1]);
    if (!found) return Promise.resolve({ ok: false, status: 404, json: () => Promise.resolve(null) });
    executes.push(opts.method || 'GET');
    if (opts.method === 'POST') {
      const { n } = JSON.parse(opts.body);
      if (n > 50) return Promise.resolve({ ok: false, status: 400, json: () => Promise.resolve({ fieldErrors: { n: 'Must be at most 50.' } }) });
      return Promise.resolve(ok(trace(found.id, n)));
    }
    return Promise.resolve(ok(trace(found.id, 3, 6, {}, found.dsType)));
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

const narration = () => screen.getByText(/step \d+$/, { selector: 'p[aria-live="polite"]' });

async function openAlpha(path = '/problem/alpha') {
  renderAt(path);
  await screen.findByText('alpha n=3 step 1');
}

describe('Workspace as the problem page', () => {
  it('is the problem page, with no switch to turn it on', async () => {
    renderAt('/problem/alpha');
    await screen.findByText('alpha n=3 step 1');
    expect(screen.getByRole('tablist', { name: 'Learning views' })).toBeInTheDocument();
  });

  it('shows the concept\'s context: breadcrumb, title, summary, difficulty, statement and curriculum', async () => {
    await openAlpha();
    expect(screen.getByRole('heading', { level: 1, name: 'Alpha Search' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Arrays' })).toHaveAttribute('href', '/?category=Arrays');
    expect(screen.getByText('Find the answer quickly.')).toBeInTheDocument();
    expect(screen.getByText('Medium')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Problem & examples'));
    expect(screen.getByText('1 <= n <= 10^5')).toBeInTheDocument();
    expect(screen.getByText(/not this visualizer's input limits/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Next: Beta Graph' }));
    await screen.findByText('beta n=3 step 1');
    expect(location.pathname).toBe('/problem/beta');
  });

  it('titles the page with the problem and the view', async () => {
    await openAlpha();
    expect(document.title).toBe('Alpha Search · Playground · DSA Visualizer');
    fireEvent.click(screen.getByRole('tab', { name: 'Analysis' }));
    await waitFor(() => expect(document.title).toBe('Alpha Search · Analysis · DSA Visualizer'));
  });

  it('says an unknown id does not exist and runs nothing', async () => {
    renderAt('/problem/nope');
    expect(await screen.findByRole('heading', { name: /No algorithm called “nope”/ })).toBeInTheDocument();
    expect(executes).toEqual([]);
  });
});

describe('Page footer', () => {
  it('ends the problem page, and the not-found page, with the author credit', async () => {
    await openAlpha();
    expect(screen.getByRole('contentinfo')).toHaveTextContent('developed by Prem Duvvapu');
  });

  it('keeps the credit on the not-found page too', async () => {
    renderAt('/problem/nope');
    await screen.findByRole('heading', { name: /No algorithm called/ });
    expect(screen.getByRole('contentinfo')).toHaveTextContent('developed by Prem Duvvapu');
  });
});

describe('Full problem statement', () => {
  it('shows the written statement, worked examples, constraints and the original problem links', async () => {
    const withStatement = {
      ...ARRAY,
      statement: ['There are n cities. Some are connected.', 'Return the number of provinces.'],
      examples: [{ input: { isConnected: [[1, 1], [1, 1]] }, output: '1', explanation: 'Both cities are connected.' }],
      constraints: ['1 ≤ n ≤ 200'],
      sources: [{ label: 'LeetCode 547', url: 'https://leetcode.com/problems/number-of-provinces/' }, { label: 'Unsafe', url: 'javascript:alert(1)' }]
    };
    vi.stubGlobal('fetch', vi.fn((url, opts = {}) => {
      if (url === '/api/problems') return Promise.resolve(ok([withStatement]));
      if (url === '/api/problems/alpha') return Promise.resolve(ok(withStatement));
      return Promise.resolve(ok(trace('alpha', 3)));
    }));
    renderAt('/problem/alpha');
    await screen.findByText('alpha n=3 step 1');
    expect(screen.getByText('There are n cities.')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Problem & examples'));
    expect(screen.getByText('Return the number of provinces.')).toBeInTheDocument();
    const example = screen.getByText('Example 1').closest('figure');
    expect(example).toHaveTextContent('Input: isConnected = [[1,1],[1,1]]');
    expect(example).toHaveTextContent('Output: 1');
    expect(example).toHaveTextContent('Explanation: Both cities are connected.');
    expect(screen.getByText('1 ≤ n ≤ 200')).toBeInTheDocument();
    const link = screen.getByRole('link', { name: 'LeetCode 547' });
    expect(link).toHaveAttribute('href', 'https://leetcode.com/problems/number-of-provinces/');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    expect(screen.queryByRole('link', { name: 'Unsafe' })).not.toBeInTheDocument();
  });
});

describe('Workspace: one session above every view', () => {
  it('keeps the run, the step and the draft across views, with no extra execution', async () => {
    await openAlpha();
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(narration()).toHaveTextContent('alpha n=3 step 3');
    fireEvent.change(screen.getByLabelText('Count'), { target: { value: '7' } });
    const before = executes.length;

    for (const tab of ['Code walkthrough', 'Analysis', 'Playground']) {
      fireEvent.click(screen.getByRole('tab', { name: tab }));
      expect(screen.getByRole('tab', { name: tab })).toHaveAttribute('aria-selected', 'true');
      expect(narration()).toHaveTextContent('alpha n=3 step 3');
    }
    expect(screen.getByLabelText('Count')).toHaveValue(7);
    expect(screen.getByText('Changes not run')).toBeInTheDocument();
    expect(executes.length).toBe(before);
  });

  it('carries the view in the link and restores it', async () => {
    await openAlpha('/problem/alpha?view=code&step=2');
    expect(screen.getByRole('tab', { name: 'Code walkthrough' })).toHaveAttribute('aria-selected', 'true');
    await waitFor(() => expect(narration()).toHaveTextContent('alpha n=3 step 2'));
    fireEvent.click(screen.getByRole('tab', { name: 'Playground' }));
    await waitFor(() => expect(new URLSearchParams(location.search).get('view')).toBeNull());
    expect(new URLSearchParams(location.search).get('step')).toBe('2');
  });

  it('moves between tabs with the arrow keys without stepping the trace or switching view', async () => {
    await openAlpha();
    const playground = screen.getByRole('tab', { name: 'Playground' });
    playground.focus();
    fireEvent.keyDown(playground, { key: 'ArrowRight', code: 'ArrowRight' });
    expect(document.activeElement).toBe(screen.getByRole('tab', { name: 'Code walkthrough' }));
    expect(playground).toHaveAttribute('aria-selected', 'true');
    expect(narration()).toHaveTextContent('alpha n=3 step 1');
    fireEvent.keyDown(document.activeElement, { key: 'End', code: 'End' });
    expect(document.activeElement).toBe(screen.getByRole('tab', { name: 'Analysis' }));
    expect(narration()).toHaveTextContent('alpha n=3 step 1');
  });

  it('keeps a main landmark and ties every tab to the one labelled tab panel (review S6)', async () => {
    await openAlpha();
    expect(screen.getByRole('main')).toBeInTheDocument();
    const panel = screen.getByRole('tabpanel');
    expect(panel).toHaveAttribute('aria-labelledby', 'workspace-tab-playground');
    for (const tab of screen.getAllByRole('tab')) {
      expect(document.getElementById(tab.getAttribute('aria-controls'))).toBe(panel);
    }
  });

  it('moves the roving tab stop with focus, not with selection (review S6)', async () => {
    await openAlpha();
    const playground = screen.getByRole('tab', { name: 'Playground' });
    playground.focus();
    fireEvent.keyDown(playground, { key: 'ArrowRight', code: 'ArrowRight' });
    const code = screen.getByRole('tab', { name: 'Code walkthrough' });
    expect(document.activeElement).toBe(code);
    expect(code).toHaveAttribute('tabindex', '0');
    expect(playground).toHaveAttribute('tabindex', '-1');
    expect(playground).toHaveAttribute('aria-selected', 'true');
  });
});

describe('Workspace: Playground', () => {
  it('sends Edit input to the editor heading and Show code to the Code view', async () => {
    await openAlpha();
    const scroll = vi.fn();
    Element.prototype.scrollIntoView = scroll;
    fireEvent.click(screen.getByRole('button', { name: 'Edit input' }));
    expect(document.activeElement).toBe(screen.getByRole('heading', { name: 'Try your own input' }));
    expect(scroll).toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Show code' }));
    expect(screen.getByRole('tab', { name: 'Code walkthrough' })).toHaveAttribute('aria-selected', 'true');
  });

  it('returns attention to the result after a successful run, and keeps it in the editor after a rejected one', async () => {
    await openAlpha();
    fireEvent.change(screen.getByLabelText('Count'), { target: { value: '9' } });
    fireEvent.click(screen.getByRole('button', { name: 'Run input' }));
    await screen.findByText('alpha n=9 step 1');
    expect(document.activeElement).toBe(screen.getByRole('heading', { level: 2, name: 'Array' }));

    fireEvent.change(screen.getByLabelText('Count'), { target: { value: '99' } });
    fireEvent.click(screen.getByRole('button', { name: 'Run input' }));
    const summary = await screen.findByRole('alert', { name: /could not run/i });
    expect(document.activeElement).toBe(summary);
    expect(screen.getByText('alpha n=9 step 1')).toBeInTheDocument();
  });

  it('names every editor action by its visible text', async () => {
    await openAlpha();
    const editor = screen.getByRole('region', { name: 'Try your own input' });
    for (const name of ['Run input', 'Randomize', 'Restore defaults', 'Save input']) {
      expect(within(editor).getByRole('button', { name: new RegExp(name) })).toBeInTheDocument();
    }
  });

  it('gives spatial structures the taller stage', async () => {
    renderAt('/problem/beta');
    await screen.findByText('beta n=3 step 1');
    expect(document.querySelector('[data-audit="stage"]')).toHaveAttribute('data-family', 'spatial');
  });

  it('has a stage size and label for every renderer the registry routes', () => {
    const missing = Object.keys(CANVAS_BY_DSTYPE).filter((key) => !STAGE_BY_DSTYPE[key]);
    expect(missing).toEqual([]);
  });

  it('marks a complete run watched only at its last step', async () => {
    await openAlpha();
    for (let i = 0; i < 4; i += 1) fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(JSON.parse(window.localStorage.getItem('dsa-ui:progress') || '{}').alpha?.watched).not.toBe(true);
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(JSON.parse(window.localStorage.getItem('dsa-ui:progress')).alpha.watched).toBe(true));
    expect(screen.getByText('Watched')).toBeInTheDocument();
  });
});
