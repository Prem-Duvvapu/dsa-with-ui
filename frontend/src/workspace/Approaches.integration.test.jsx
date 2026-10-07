import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
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
      variables: i === 3 ? { answer: '8' } : { event: i === 2 ? 'cache-hit' : 'enter' }
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
