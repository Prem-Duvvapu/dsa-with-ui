import React from 'react';
import { act, render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, useLocation } from 'react-router-dom';
import AppRouter from '../AppRouter';
import { decodeInput } from '../hooks/useShareableView';

const definitions = ['recursion', 'memoization', 'tabulation'].map(id => ({ id,
  label: id[0].toUpperCase() + id.slice(1), summary: `Purpose of ${id}.`, isDefault: id === 'tabulation',
  dsType: id === 'tabulation' ? 'DpTable' : 'RecursionTree',
  inputSpec: { fields: [{ name: 'n', label: 'Stairs', type: 'INT', defaultValue: 5,
    constraints: { min: 1, max: id === 'recursion' ? 10 : 30 } }] },
  alternateInput: { n: id === 'tabulation' ? 12 : 6 },
  complexity: { timeComplexity: id === 'recursion' ? 'O(2^N)' : 'O(N)', spaceComplexity: 'O(N)' }
}));
definitions.forEach(a => { a.teaching = { state: 'ways(k): ways to reach stair k', baseCases: 'ways(0) = ways(1) = 1',
  recurrence: 'ways(k) = ways(k-1) + ways(k-2)', evaluationOrder: 'Read both smaller states first.',
  cacheKey: a.id === 'memoization' ? 'k' : null,
  eventNotes: a.id === 'memoization' ? { 'cache-hit': 'Return the recorded memo value without expanding children.' } : {},
  anchorNotes: a.id === 'tabulation' ? { init: 'Allocate unknown cells before computing them.' } : {} }; });
const problem = { id: 'climbing-stairs', title: 'Climbing Stairs', category: 'Dynamic Programming', difficulty: 'Easy',
  traced: true, defaultApproachId: 'tabulation', approaches: definitions, ...definitions[2],
  id: 'climbing-stairs', approachId: 'tabulation', javaCode: 'public class TabulatedSolution {}' };
problem.defaultArray = [{ index: 0, value: 99, state: 'default' }];
problem.defaultTreeNodes = [{ id: 123, val: 'not a recursive call', x: 10, y: 10 }];
const code = id => `public class ${id === 'recursion' ? 'Recursive' : id === 'memoization' ? 'Memoized' : 'Tabulated'}Solution {}`;
function run(approach, n = 5) {
  const definition = definitions.find(a => a.id === approach);
  const memo = approach === 'memoization';
  const table = state => ({ rowLabels: ['memo'], colLabels: ['0', '1'], cells: [[
    { value: state === 'void' ? '·' : '1', state }, { value: '·', state: 'void' }
  ]] });
  return { problemId: problem.id, approachId: approach, approachLabel: definition.label,
    code: code(approach), complexity: definition.complexity, dsType: definition.dsType,
    anchors: { init: 1 }, resolvedInput: { n }, encoding: 'full', truncated: false,
    steps: Array.from({ length: 4 }, (_, i) => ({ stepNumber: i + 1, activeLine: 1,
      description: `${approach} ran n=${n} step ${i + 1}`, dsType: definition.dsType,
      callStack: approach === 'tabulation' || i === 0 || i === 3 ? [] : ['ways(5) #1'],
      dpTable: memo ? table(i === 0 ? 'void' : i === 2 ? 'read' : 'known') : approach === 'tabulation' ? table('known') : null,
      variables: i === 3 ? { answer: '8' } : approach === 'tabulation' ? {} : { event: i === 2 ? 'cache-hit' : 'enter' }
    })) };
}
const ok = body => ({ ok: true, status: 200, json: async () => body });
let posts, location;
beforeEach(() => {
  posts = [];
  Element.prototype.scrollIntoView = vi.fn();
  vi.stubGlobal('fetch', vi.fn((url, options = {}) => {
    if (url === '/api/problems') return Promise.resolve(ok([problem]));
    const parsed = new URL(url, 'https://test.invalid');
    if (!parsed.pathname.endsWith('/execute')) return Promise.resolve(ok(problem));
    const approach = parsed.searchParams.get('approach') ?? 'tabulation';
    if (options.method === 'POST') {
      const input = JSON.parse(options.body);
      posts.push({ approach, input });
      if (approach === 'recursion' && input.n > 10) return Promise.resolve({ ok: false, status: 400,
        json: async () => ({ fieldErrors: { n: 'Must be at most 10.' } }) });
      return Promise.resolve(ok(run(approach, input.n ?? 5)));
    }
    return Promise.resolve(ok(run(approach)));
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });
function mount(path = '/problem/climbing-stairs') {
  function Probe() { location = useLocation(); return null; }
  return render(<MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <AppRouter /><Probe />
  </MemoryRouter>);
}
async function loaded() {
  mount();
  await screen.findByText('tabulation ran n=5 step 1');
  return screen.findByRole('combobox', { name: 'Solution approach' });
}
async function selectAndRun(id) {
  fireEvent.change(screen.getByRole('combobox', { name: 'Solution approach' }), { target: { value: id } });
  fireEvent.click(screen.getByRole('button', { name: `Run ${id}` }));
  await screen.findByText(`${id} ran n=5 step 1`);
}

describe('one approach selector above one committed workspace', () => {
  it.each([null, { ...definitions[2].teaching, recurrence: null },
    { ...definitions[2].teaching, state: { text: 'Malformed state' } }])(
    'does not break the trace or append a blank row for unavailable teaching %j', async teaching => {
    const original = fetch.getMockImplementation();
    const single = { ...problem, approaches: [{ ...definitions[2], teaching }] };
    fetch.mockImplementation((url, options) => url === '/api/problems' ? Promise.resolve(ok([single]))
      : url === '/api/problems/climbing-stairs' ? Promise.resolve(ok(single)) : original(url, options));
    mount();
    await screen.findByText('tabulation ran n=5 step 1');
    const card = screen.getByRole('heading', { name: 'DP table', exact: true }).closest('section');
    expect(card.lastElementChild.textContent.trim()).not.toBe('');
    expect(screen.queryByText('How the shown approach works')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Compare approaches', exact: true })).not.toBeInTheDocument();
  });

  it('waits for a committed executable before offering input comparison requests', async () => {
    const original = fetch.getMockImplementation();
    let release;
    fetch.mockImplementation((url, options = {}) => new URL(url, 'https://test.invalid').pathname.endsWith('/execute')
      && options.method !== 'POST' ? new Promise(resolve => { release = () => resolve(ok(run('tabulation'))); })
      : original(url, options));
    mount();
    const toggle = await screen.findByRole('button', { name: 'Compare other case', exact: true });
    expect(toggle).toBeDisabled();
    fireEvent.click(toggle);
    expect(posts).toHaveLength(0);
    await act(async () => release());
    await screen.findByText('tabulation ran n=5 step 1');
    expect(toggle).toBeEnabled();
    fireEvent.click(toggle);
    await screen.findByRole('heading', { name: /^Other case/ });
    expect(posts.map(post => post.approach)).toEqual(['tabulation', 'tabulation']);
  });
  it('discloses the shown recurrence and changes event teaching only from recorded events', async () => {
    await loaded();
    fireEvent.click(screen.getByText('How the shown approach works'));
    expect(screen.getByText('ways(k) = ways(k-1) + ways(k-2)')).toBeVisible();
    expect(screen.getByText('Allocate unknown cells before computing them.')).toBeVisible();
    fireEvent.change(screen.getByRole('combobox', { name: 'Solution approach' }), { target: { value: 'memoization' } });
    expect(screen.getByText('Allocate unknown cells before computing them.')).toBeVisible();
    await selectAndRun('memoization');
    fireEvent.click(screen.getByRole('button', { name: 'Next', exact: true }));
    fireEvent.click(screen.getByRole('button', { name: 'Next', exact: true }));
    expect(screen.getByText('Return the recorded memo value without expanding children.')).toBeVisible();
    expect(posts).toHaveLength(1);
  });

  it('opens comparison without fetching; explicit comparison uses shown input, not edited draft or candidate', async () => {
    await loaded();
    fireEvent.click(screen.getByRole('button', { name: 'Next', exact: true }));
    fireEvent.change(screen.getByLabelText('Stairs'), { target: { value: '8' } });
    fireEvent.change(screen.getByRole('combobox', { name: 'Solution approach' }), { target: { value: 'memoization' } });
    const toggle = screen.getByRole('button', { name: 'Compare approaches', exact: true });
    const controlled = document.getElementById(toggle.getAttribute('aria-controls'));
    expect(controlled).not.toBeNull();
    expect(controlled).not.toBeVisible();
    fireEvent.click(toggle);
    expect(posts).toHaveLength(0);
    const panel = screen.getByRole('region', { name: 'Approach comparison' });
    expect(within(panel).getByText(/Same recorded input/)).toBeVisible();
    fireEvent.click(within(panel).getByRole('button', { name: 'Run comparison' }));
    await within(panel).findByText('Both complete runs recorded the same answer.');
    expect(posts).toEqual([{ approach: 'tabulation', input: { n: 5 } }, { approach: 'recursion', input: { n: 5 } }]);
    expect(screen.getByLabelText('Stairs')).toHaveValue(8);
    expect(screen.getByText('tabulation ran n=5 step 2')).toBeInTheDocument();
    expect(new URLSearchParams(location.search).get('approach')).toBeNull();
  });

  it('keeps independent comparison positions without seeking or marking the main run complete', async () => {
    await loaded();
    fireEvent.click(screen.getByRole('button', { name: 'Compare approaches', exact: true }));
    fireEvent.click(screen.getByRole('button', { name: 'Run comparison' }));
    await screen.findByText('Both complete runs recorded the same answer.');
    const panel = screen.getByRole('region', { name: 'Approach comparison' });
    fireEvent.change(within(panel).getByRole('slider', { name: 'Recursion: step' }), { target: { value: '4' } });
    expect(within(panel).getByRole('slider', { name: 'Tabulation: step' })).toHaveValue('1');
    expect(screen.getByRole('slider', { name: 'Seek to step' })).toHaveValue('0');
    expect(JSON.parse(localStorage.getItem('dsa-ui:progress') ?? '{}')['climbing-stairs']?.watched).not.toBe(true);
  });

  it('labels safe limits, refuses an oversized side and preserves the main run', async () => {
    await loaded();
    fireEvent.change(screen.getByLabelText('Stairs'), { target: { value: '20' } });
    fireEvent.click(screen.getByRole('button', { name: 'Run input', exact: true }));
    await screen.findByText('tabulation ran n=20 step 1');
    await waitFor(() => expect(decodeInput(new URLSearchParams(location.search).get('input'))).toEqual({ n: 20 }));
    const before = location.search;
    fireEvent.click(screen.getByRole('button', { name: 'Compare approaches', exact: true }));
    const panel = screen.getByRole('region', { name: 'Approach comparison' });
    expect(within(panel).getByText(/Recursion: Stairs 1–10/)).toBeVisible();
    fireEvent.click(within(panel).getByRole('button', { name: 'Run comparison' }));
    await within(panel).findByText(/Must be at most 10/);
    expect(within(panel).queryByText('Both complete runs recorded the same answer.')).not.toBeInTheDocument();
    expect(screen.getAllByText('tabulation ran n=20 step 1').some(node => !panel.contains(node))).toBe(true);
    expect(screen.getByLabelText('Stairs')).toHaveValue(20);
    expect(location.search).toBe(before);
  });

  it('keeps comparison requests and positions across view changes and owns playback keys locally', async () => {
    await loaded();
    fireEvent.click(screen.getByRole('button', { name: 'Compare approaches', exact: true }));
    fireEvent.click(screen.getByRole('button', { name: 'Run comparison' }));
    await screen.findByText('Both complete runs recorded the same answer.');
    const panel = screen.getByRole('region', { name: 'Approach comparison' });
    fireEvent.change(within(panel).getByRole('slider', { name: 'Recursion: step' }), { target: { value: '2' } });
    const button = within(panel).getByRole('button', { name: 'Run comparison' });
    button.focus();
    expect(fireEvent.keyDown(button, { key: 'ArrowRight', code: 'ArrowRight' })).toBe(true);
    expect(screen.getByRole('slider', { name: 'Seek to step' })).toHaveValue('0');
    fireEvent.click(screen.getByRole('tab', { name: 'Analysis' }));
    expect(within(panel).getByRole('slider', { name: 'Recursion: step' })).toHaveValue('2');
    expect(posts).toHaveLength(2);
    expect(screen.queryByTestId('recursion-tree-stage')).not.toBeInTheDocument();
  });

  it('leaves the global Ctrl+K switcher shortcut available inside comparison controls', async () => {
    await loaded();
    fireEvent.click(screen.getByRole('button', { name: 'Compare approaches', exact: true }));
    const button = screen.getByRole('button', { name: 'Run comparison' });
    button.focus();
    fireEvent.keyDown(button, { key: 'k', code: 'KeyK', ctrlKey: true });
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(posts).toHaveLength(0);
  });

  it('changing a comparison pair clears old labelled results and waits for another explicit request', async () => {
    await loaded();
    fireEvent.click(screen.getByRole('button', { name: 'Compare approaches', exact: true }));
    fireEvent.click(screen.getByRole('button', { name: 'Run comparison' }));
    await screen.findByText('Both complete runs recorded the same answer.');
    const panel = screen.getByRole('region', { name: 'Approach comparison' });
    fireEvent.change(within(panel).getByRole('combobox', { name: 'Approach 2', exact: true }), { target: { value: 'memoization' } });
    expect(within(panel).queryByRole('region', { name: 'Recursion comparison' })).not.toBeInTheDocument();
    expect(posts).toHaveLength(2);
    fireEvent.click(within(panel).getByRole('button', { name: 'Run comparison' }));
    await within(panel).findByRole('region', { name: 'Memoization comparison' });
    expect(posts.at(-1)).toEqual({ approach: 'memoization', input: { n: 5 } });
  });

  it('keeps the comparison submitter focusable while busy without allowing another request', async () => {
    await loaded();
    const bodies = [];
    fetch.mockImplementation((url) => {
      const approach = new URL(url, 'https://test.invalid').searchParams.get('approach');
      return Promise.resolve({ ok: true, status: 200, json: () => new Promise(resolve =>
        bodies.push(() => resolve(run(approach)))) });
    });
    fireEvent.click(screen.getByRole('button', { name: 'Compare approaches', exact: true }));
    const panel = screen.getByRole('region', { name: 'Approach comparison' });
    const button = within(panel).getByRole('button', { name: 'Run comparison' });
    button.focus();
    fireEvent.click(button);
    await waitFor(() => expect(bodies).toHaveLength(2));
    expect(button).toHaveAttribute('aria-disabled', 'true');
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(button).not.toBeDisabled();
    expect(within(panel).getByLabelText('Approach 1')).toBeDisabled();
    const calls = fetch.mock.calls.length;
    fireEvent.click(button);
    expect(fetch).toHaveBeenCalledTimes(calls);
    await act(async () => bodies.forEach(release => release()));
    expect(button).toHaveAttribute('aria-disabled', 'false');
    expect(button).toHaveAttribute('aria-busy', 'false');
    expect(button).toHaveFocus();
  });

  it.each(['close', 'new main run'])('retires pending comparison bodies after %s', async action => {
    await loaded();
    const original = fetch.getMockImplementation();
    const bodies = [];
    const signals = [];
    fetch.mockImplementation((url, options = {}) => {
      if (options.method !== 'POST' || signals.length >= 2) return original(url, options);
      const approach = new URL(url, 'https://test.invalid').searchParams.get('approach');
      signals.push(options.signal);
      return Promise.resolve({ ok: true, status: 200, json: () => new Promise(resolve =>
        bodies.push(() => resolve(run(approach)))) });
    });
    fireEvent.click(screen.getByRole('button', { name: 'Compare approaches', exact: true }));
    fireEvent.click(screen.getByRole('button', { name: 'Run comparison' }));
    await waitFor(() => expect(bodies).toHaveLength(2));
    if (action === 'close') {
      fireEvent.click(screen.getByRole('button', { name: 'Hide approach comparison', exact: true }));
      fireEvent.click(screen.getByRole('button', { name: 'Compare approaches', exact: true }));
    } else {
      fireEvent.change(screen.getByLabelText('Stairs'), { target: { value: '8' } });
      fireEvent.click(screen.getByRole('button', { name: 'Run input', exact: true }));
      await screen.findByText('tabulation ran n=8 step 1');
      await waitFor(() => expect(decodeInput(new URLSearchParams(location.search).get('input'))).toEqual({ n: 8 }));
    }
    expect(signals.every(signal => signal.aborted)).toBe(true);
    const panel = screen.getByRole('region', { name: 'Approach comparison' });
    expect(within(panel).getByRole('button', { name: 'Run comparison' })).toBeEnabled();
    expect(within(panel).queryByText('Both complete runs recorded the same answer.')).not.toBeInTheDocument();
    if (action === 'new main run') {
      fireEvent.click(within(panel).getByRole('button', { name: 'Run comparison' }));
      await within(panel).findByText('Both complete runs recorded the same answer.');
    }
    const link = location.search;
    await act(async () => bodies.forEach(release => release()));
    expect(location.search).toBe(link);
    expect(screen.getByRole('slider', { name: 'Seek to step' })).toHaveValue('0');
    if (action === 'close') {
      expect(within(panel).queryByText('Both complete runs recorded the same answer.')).not.toBeInTheDocument();
    } else {
      expect(within(panel).getByRole('region', { name: 'Recursion comparison' })).toHaveTextContent('recursion ran n=8 step 1');
      expect(within(panel).queryByText('recursion ran n=5 step 1')).not.toBeInTheDocument();
    }
  });

  it('never calls a truncated comparison complete or uses its last-frame answer', async () => {
    await loaded();
    fetch.mockImplementation((url, options = {}) => {
      const approach = new URL(url, 'https://test.invalid').searchParams.get('approach');
      return Promise.resolve(ok({ ...run(approach), truncated: true }));
    });
    fireEvent.click(screen.getByRole('button', { name: 'Compare approaches', exact: true }));
    fireEvent.click(screen.getByRole('button', { name: 'Run comparison' }));
    const panel = screen.getByRole('region', { name: 'Approach comparison' });
    await waitFor(() => expect(within(panel).getAllByText('Incomplete: this trace was cut short.')).toHaveLength(2));
    expect(within(panel).queryByText('Both complete runs recorded the same answer.')).not.toBeInTheDocument();
    expect(within(panel).getAllByText('Unavailable for this incomplete or uninstrumented run')).toHaveLength(2);
    expect(screen.getByRole('slider', { name: 'Seek to step' })).toHaveValue('0');
  });
  it('lets the recursive region scroll with arrows without also seeking playback', async () => {
    await loaded();
    await selectAndRun('recursion');
    const region = screen.getByRole('region', { name: 'Recursive calls' });
    region.focus();
    expect(fireEvent.keyDown(region, { key: 'ArrowRight', code: 'ArrowRight' })).toBe(true);
    expect(screen.getByText('recursion ran n=5 step 1')).toBeInTheDocument();
  });
  it('prepares an approach without running it, and labels selected versus shown in every view', async () => {
    const select = await loaded();
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    fireEvent.change(select, { target: { value: 'recursion' } });
    expect(posts).toHaveLength(0);
    expect(screen.getByRole('status', { name: 'Solution approach' })).toHaveTextContent('Selected: Recursion; showing: Tabulation');
    fireEvent.click(screen.getByRole('tab', { name: 'Code walkthrough' }));
    expect(screen.getByRole('region', { name: 'Java source' })).toHaveTextContent('TabulatedSolution');
    expect(screen.getByRole('combobox', { name: 'Solution approach' })).toHaveValue('recursion');
    expect(screen.getByText('tabulation ran n=5 step 2')).toBeInTheDocument();
    expect(posts).toHaveLength(0);
  });

  it('runs explicitly, focuses the stage, and commits source and Analysis complexity together', async () => {
    await loaded();
    await selectAndRun('recursion');
    expect(document.activeElement).toBe(screen.getByRole('heading', { name: 'Recursion tree' }));
    expect(screen.getByRole('status', { name: 'Solution approach' })).toHaveTextContent('Showing: Recursion');
    fireEvent.click(screen.getByRole('tab', { name: 'Code walkthrough' }));
    expect(screen.getByRole('region', { name: 'Java source' })).toHaveTextContent('RecursiveSolution');
    fireEvent.click(screen.getByRole('tab', { name: 'Analysis' }));
    expect(within(screen.getByRole('region', { name: 'Algorithm complexity' })).getByText('O(2^N)')).toBeInTheDocument();
    expect(posts).toEqual([{ approach: 'recursion', input: { n: 5 } }]);
    await waitFor(() => expect(new URLSearchParams(location.search).get('approach')).toBe('recursion'));
    expect(decodeInput(new URLSearchParams(location.search).get('input'))).toEqual({ n: 5 });
  });

  it('keeps a too-large draft and prior run/link, and focuses the field-error summary', async () => {
    const select = await loaded();
    fireEvent.change(screen.getByLabelText('Stairs'), { target: { value: '20' } });
    fireEvent.change(select, { target: { value: 'recursion' } });
    expect(screen.getByLabelText('Stairs')).toHaveValue(20);
    expect(screen.getByLabelText('Stairs')).toHaveAttribute('max', '10');
    const before = location.search;
    fireEvent.click(screen.getByRole('button', { name: 'Run recursion' }));
    const summary = await screen.findByRole('alert', { name: 'This input could not run' });
    expect(document.activeElement).toBe(summary);
    expect(screen.getByText('tabulation ran n=5 step 1')).toBeInTheDocument();
    expect(location.search).toBe(before);
  });

  it('compares inputs using the displayed approach and its own alternate, not the new candidate', async () => {
    await loaded();
    await selectAndRun('memoization');
    fireEvent.change(screen.getByRole('combobox', { name: 'Solution approach' }), { target: { value: 'recursion' } });
    fireEvent.click(screen.getByRole('button', { name: 'Compare other case' }));
    await waitFor(() => expect(posts).toHaveLength(3));
    expect(posts.slice(1)).toEqual([{ approach: 'memoization', input: {} }, { approach: 'memoization', input: { n: 6 } }]);
  });

  it('shows an actual memo table with unknown entries in Playground and Analysis', async () => {
    await loaded();
    await selectAndRun('memoization');
    const disclosure = screen.getByText('Memo table');
    fireEvent.click(disclosure);
    expect(screen.getAllByRole('cell').some(cell => cell.getAttribute('aria-label')?.includes('void'))).toBe(true);
    expect(screen.getByTestId('step-state-summary')).toHaveTextContent(/unknown/i);
    expect(screen.queryByText('Live Array State:')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: 'Analysis' }));
    expect(screen.getByText('Memo table')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Memo table'));
    expect(screen.getAllByRole('cell').some(cell => cell.getAttribute('aria-label')?.includes('void'))).toBe(true);
  });

  it('preserves follow/scroll for preparation and view changes, but resets for a new executed source', async () => {
    await loaded();
    fireEvent.click(screen.getByRole('tab', { name: 'Code walkthrough' }));
    const source = screen.getByRole('region', { name: 'Java source' });
    fireEvent.wheel(source);
    source.scrollTop = 200;
    fireEvent.change(screen.getByRole('combobox', { name: 'Solution approach' }), { target: { value: 'recursion' } });
    expect(screen.getByText(/Not following/)).toBeInTheDocument();
    expect(source.scrollTop).toBe(200);
    fireEvent.click(screen.getByRole('tab', { name: 'Playground' }));
    fireEvent.click(screen.getByRole('button', { name: 'Run recursion' }));
    await screen.findByText('recursion ran n=5 step 1');
    fireEvent.click(screen.getByRole('tab', { name: 'Code walkthrough' }));
    expect(screen.getByText('Following execution')).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Java source' }).scrollTop).toBe(0);
  });
});
