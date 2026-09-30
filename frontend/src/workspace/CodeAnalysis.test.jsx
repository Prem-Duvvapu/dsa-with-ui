import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import AppRouter from '../AppRouter';

/**
 * P4a: the Code walkthrough (measured split, subviews, follow execution, kept scroll) and
 * the Analysis reading columns - all views of the one session, none of them re-running it.
 */

const ok = (body) => ({ ok: true, status: 200, json: () => Promise.resolve(body) });
const LONG = Array.from({ length: 40 }, (_, i) => i);
const PROBLEM = {
  id: 'fib', title: 'Fibonacci', category: 'Recursion', difficulty: 'Easy', dsType: 'Stack', traced: true,
  javaCode: Array.from({ length: 30 }, (_, i) => `line ${i + 1};`).join('\n'),
  inputSpec: { fields: [{ name: 'n', label: 'N', type: 'INT', defaultValue: 4 }] },
  complexity: { timeComplexity: 'O(2^N)', timeExplanation: 'Two calls per frame.' }
};
const STEPS = [
  { stepNumber: 1, activeLine: 2, keyframe: true, dsType: 'Stack', description: 'call fib(4)',
    variables: { n: 4, memo: null, label: '' , seen: LONG }, callStack: ['fib(4)'], queueOrStackState: [4] },
  { stepNumber: 2, activeLine: 20, dsType: 'Stack', description: 'call fib(3)',
    variables: { n: 3 }, callStack: ['fib(4)', 'fib(3)'], queueOrStackState: [4, 3] },
  { stepNumber: 3, activeLine: 25, dsType: 'Stack', description: 'return 2', variables: { n: 3 }, callStack: ['fib(4)'], queueOrStackState: [4] }
];

let executes;
let width;
beforeEach(() => {
  executes = 0;
  width = 1200;
  window.localStorage.setItem('dsa-ui:workspace', JSON.stringify('next'));
  vi.spyOn(Element.prototype, 'getBoundingClientRect').mockImplementation(function rect() {
    const w = this.className?.includes?.('codeArea') ? width : 0;
    return { width: w, height: 0, top: 0, left: 0, right: w, bottom: 0, x: 0, y: 0 };
  });
  Element.prototype.scrollIntoView = vi.fn();
  vi.stubGlobal('fetch', vi.fn((url) => {
    if (url === '/api/problems') return Promise.resolve(ok([PROBLEM]));
    if (url === '/api/problems/fib') return Promise.resolve(ok(PROBLEM));
    executes += 1;
    return Promise.resolve(ok({ encoding: 'delta', resolvedInput: { n: 4 }, anchors: { a: 2, b: 20, c: 25, d: 28 }, steps: STEPS }));
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

const renderAt = (path) => render(
  <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <AppRouter />
  </MemoryRouter>
);
const narration = () => document.querySelector('p[aria-live="polite"]');

async function openCode() {
  renderAt('/problem/fib?view=code');
  await waitFor(() => expect(narration()).toHaveTextContent('call fib(4)'));
}

describe('Code walkthrough', () => {
  it('shows diagram and source side by side when the measured container fits both', async () => {
    await openCode();
    expect(screen.getByRole('separator', { name: 'Resize diagram and source' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Java source' })).toBeInTheDocument();
    expect(document.querySelector('[data-audit="stage"]')).toBeInTheDocument();
  });

  it('resizes from the keyboard within its bounds, persists, and resets', async () => {
    await openCode();
    const separator = screen.getByRole('separator');
    expect(separator).toHaveAttribute('aria-valuenow', '60');
    fireEvent.keyDown(separator, { key: 'ArrowRight', code: 'ArrowRight' });
    expect(separator).toHaveAttribute('aria-valuenow', '65');
    fireEvent.keyDown(separator, { key: 'End', code: 'End' });
    fireEvent.keyDown(separator, { key: 'ArrowRight', code: 'ArrowRight' });
    expect(separator).toHaveAttribute('aria-valuenow', '70');
    expect(JSON.parse(window.localStorage.getItem('dsa-ui:codeSplit'))).toEqual({ v: 1, ratio: 0.7 });
    fireEvent.keyDown(separator, { key: 'Home', code: 'Home' });
    expect(separator).toHaveAttribute('aria-valuenow', '45');
    fireEvent.keyDown(separator, { key: 'Enter', code: 'Enter' });
    expect(separator).toHaveAttribute('aria-valuenow', '60');
    // The separator's arrows resize; they do not also step the trace.
    expect(narration()).toHaveTextContent('call fib(4)');
  });

  it('clamps an out-of-range stored ratio and ignores an unknown version', async () => {
    window.localStorage.setItem('dsa-ui:codeSplit', JSON.stringify({ v: 1, ratio: 5 }));
    await openCode();
    expect(screen.getByRole('separator')).toHaveAttribute('aria-valuenow', '70');
  });

  it('falls back to a default for a preference written by a future version', async () => {
    window.localStorage.setItem('dsa-ui:codeSplit', JSON.stringify({ v: 2, panes: [1, 2] }));
    await openCode();
    expect(screen.getByRole('separator')).toHaveAttribute('aria-valuenow', '60');
  });

  it('uses Diagram / Source subviews when the container is too narrow, on the same step', async () => {
    width = 700;
    await openCode();
    expect(screen.queryByRole('separator')).not.toBeInTheDocument();
    const group = screen.getByRole('group', { name: 'Show in Code walkthrough' });
    expect(within(group).getByRole('button', { name: 'Source' })).toHaveAttribute('aria-pressed', 'true');
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    fireEvent.click(within(group).getByRole('button', { name: 'Diagram' }));
    expect(document.querySelector('[data-audit="stage"]')).toBeInTheDocument();
    expect(narration()).toHaveTextContent('call fib(3)');
    expect(executes).toBe(1);
  });

  it('follows execution until the reader scrolls, and returns on request', async () => {
    await openCode();
    expect(screen.getByText('Following execution')).toBeInTheDocument();
    fireEvent.wheel(screen.getByRole('region', { name: 'Java source' }));
    expect(screen.getByText(/Not following/)).toBeInTheDocument();
    const calls = Element.prototype.scrollIntoView.mock.calls.length;
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(Element.prototype.scrollIntoView.mock.calls.length).toBe(calls);
    fireEvent.click(screen.getByRole('button', { name: /Return to active line/ }));
    expect(screen.getByText('Following execution')).toBeInTheDocument();
    expect(Element.prototype.scrollIntoView.mock.calls.length).toBeGreaterThan(calls);
  });

  it('keeps the reader\'s place in the source across view changes', async () => {
    await openCode();
    const source = screen.getByRole('region', { name: 'Java source' });
    fireEvent.wheel(source);
    source.scrollTop = 240;
    fireEvent.click(screen.getByRole('tab', { name: 'Analysis' }));
    fireEvent.click(screen.getByRole('tab', { name: 'Code walkthrough' }));
    expect(screen.getByRole('region', { name: 'Java source' }).scrollTop).toBe(240);
    expect(screen.getByText(/Not following/)).toBeInTheDocument();
    expect(executes).toBe(1);
  });
});

describe('Analysis', () => {
  it('shows variables in full, including null and empty values', async () => {
    renderAt('/problem/fib?view=analysis');
    const vars = await screen.findByRole('region', { name: 'Variables' });
    expect(within(vars).getByText('null')).toBeInTheDocument();
    expect(within(vars).getByText('"" (empty string)')).toBeInTheDocument();
    expect(within(vars).getByText('show all')).toBeInTheDocument();
    expect(vars.querySelector('pre').textContent).toContain('39');
  });

  it('labels frame order, the current frame and the stack top from the trace', async () => {
    renderAt('/problem/fib?view=analysis&step=2');
    const stack = await screen.findByRole('region', { name: 'Call stack' });
    await waitFor(() => expect(within(stack).getAllByRole('listitem')).toHaveLength(2));
    expect(within(stack).getByText(/Outermost call first/)).toBeInTheDocument();
    expect(within(stack).getAllByRole('listitem')[1]).toHaveTextContent('fib(3)Current frame');
    const contents = screen.getByRole('region', { name: 'Stack contents' });
    expect(within(contents).getAllByRole('listitem')[1]).toHaveTextContent('3Top');
  });

  it('shows the backend\'s complexity and says unavailable for what it lacks', async () => {
    renderAt('/problem/fib?view=analysis');
    const cost = await screen.findByRole('region', { name: 'Algorithm complexity' });
    expect(within(cost).getByText('O(2^N)')).toBeInTheDocument();
    expect(within(cost).getByText('Two calls per frame.')).toBeInTheDocument();
    expect(within(cost).getByText('unavailable')).toBeInTheDocument();
    expect(within(cost).getByText(/No space explanation is recorded/)).toBeInTheDocument();
  });

  it('omits a call stack the run never has, and reads a graph run\'s container as its queue', async () => {
    const GRAPH = { ...PROBLEM, id: 'bfs', dsType: 'Graph' };
    const graphSteps = [
      { stepNumber: 1, activeLine: 1, keyframe: true, dsType: 'Graph', description: 'seed', variables: { node: 0 }, callStack: [], queueOrStackState: [0] },
      { stepNumber: 2, activeLine: 1, dsType: 'Graph', description: 'visit', variables: { node: 0 }, queueOrStackState: [1, 2] }
    ];
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/problems') return Promise.resolve(ok([GRAPH]));
      if (url === '/api/problems/bfs') return Promise.resolve(ok(GRAPH));
      return Promise.resolve(ok({ encoding: 'delta', resolvedInput: {}, anchors: {}, steps: graphSteps }));
    }));
    renderAt('/problem/bfs?view=analysis&step=2');
    const queue = await screen.findByRole('region', { name: 'Queue contents' });
    await waitFor(() => expect(within(queue).getAllByRole('listitem')).toHaveLength(2));
    expect(within(queue).getAllByRole('listitem')[0]).toHaveTextContent('1Front');
    expect(within(queue).getAllByRole('listitem')[1]).toHaveTextContent('2Back');
    expect(screen.queryByRole('region', { name: 'Call stack' })).not.toBeInTheDocument();
  });
});
