import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom';
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

const OTHER = { ...PROBLEM, id: 'fib-two', title: 'Fibonacci Two' };

let executes;
let width;
beforeEach(() => {
  executes = 0;
  width = 1200;
  vi.spyOn(Element.prototype, 'getBoundingClientRect').mockImplementation(function rect() {
    const w = this.className?.includes?.('codeArea') ? width : 0;
    return { width: w, height: 0, top: 0, left: 0, right: w, bottom: 0, x: 0, y: 0 };
  });
  Element.prototype.scrollIntoView = vi.fn();
  vi.stubGlobal('fetch', vi.fn((url) => {
    if (url === '/api/problems') return Promise.resolve(ok([PROBLEM, OTHER]));
    if (url === '/api/problems/fib') return Promise.resolve(ok(PROBLEM));
    if (url === '/api/problems/fib-two') return Promise.resolve(ok(OTHER));
    executes += 1;
    return Promise.resolve(ok({ encoding: 'delta', resolvedInput: { n: 4 }, anchors: { a: 2, b: 20, c: 25, d: 28 }, steps: STEPS }));
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

let navigateTo;
let location;
function Nav() { navigateTo = useNavigate(); location = useLocation(); return null; }
const renderAt = (path) => render(
  <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <AppRouter />
    <Nav />
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
    // Wide enough that the 45-70% preference bounds are all reachable; at narrower widths
    // the pane minimums cap them (see the S5 geometry test below).
    width = 1500;
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
    width = 1500;
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

describe('Keyboard ownership (review S2)', () => {
  it.each(['ArrowLeft', 'ArrowRight'])('lets source %s scroll without seeking or changing the URL (re-audit R1)', async (key) => {
    await openCode();
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(new URLSearchParams(location.search).get('step')).toBe('2'));
    const before = location.search;
    const source = screen.getByRole('region', { name: 'Java source' });
    source.focus();
    // dispatchEvent returns false when a listener cancels the native scrolling action.
    expect(fireEvent.keyDown(source, { key, code: key })).toBe(true);
    expect(narration()).toHaveTextContent('call fib(3)');
    expect(location.search).toBe(before);
    expect(screen.getByText(/Not following/)).toBeInTheDocument();
    expect(document.activeElement).toBe(source);
    expect(executes).toBe(1);
    expect(screen.getByRole('button', { name: 'Play' })).toBeInTheDocument();
  });

  it('still steps with horizontal arrows outside the source', async () => {
    await openCode();
    fireEvent.keyDown(window, { key: 'ArrowRight', code: 'ArrowRight' });
    expect(narration()).toHaveTextContent('call fib(3)');
    fireEvent.keyDown(window, { key: 'ArrowLeft', code: 'ArrowLeft' });
    expect(narration()).toHaveTextContent('call fib(4)');
  });

  it('lets the focused source scroll with End without jumping playback to the last step', async () => {
    await openCode();
    const source = screen.getByRole('region', { name: 'Java source' });
    source.focus();
    fireEvent.keyDown(source, { key: 'End', code: 'End' });
    fireEvent.keyDown(source, { key: ' ', code: 'Space' });
    expect(narration()).toHaveTextContent('call fib(4)');
    expect(screen.getByRole('button', { name: 'Play' })).toBeInTheDocument();
    expect(screen.getByText(/Not following/)).toBeInTheDocument();
  });

  it('keeps player shortcuts from acting behind an open dialog', async () => {
    await openCode();
    fireEvent.keyDown(window, { key: '?' });
    expect(await screen.findByRole('dialog', { name: 'Keyboard shortcuts' })).toBeInTheDocument();
    for (const code of ['KeyL', 'End', 'ArrowRight', 'Period']) fireEvent.keyDown(window, { code, key: code });
    expect(narration()).toHaveTextContent('call fib(4)');
    fireEvent.keyDown(window, { code: 'Escape', key: 'Escape' });
    fireEvent.keyDown(window, { code: 'KeyL', key: 'l' });
    await waitFor(() => expect(narration()).toHaveTextContent('call fib(3)'));
  });

  it('keeps Space on the separator from toggling playback', async () => {
    await openCode();
    const separator = screen.getByRole('separator');
    separator.focus();
    fireEvent.keyDown(separator, { key: ' ', code: 'Space' });
    expect(screen.getByRole('button', { name: 'Play' })).toBeInTheDocument();
  });

  it('ignores a modifier chord a focused control already handled', async () => {
    await openCode();
    const source = screen.getByRole('region', { name: 'Java source' });
    source.addEventListener('keydown', (e) => e.preventDefault(), { once: true });
    fireEvent.keyDown(source, { key: 'k', ctrlKey: true });
    expect(screen.queryByRole('dialog', { name: /command palette/i })).not.toBeInTheDocument();
  });
});

describe('Presentation continuity (review S3, S4)', () => {
  it('pauses when the Code subview changes, as a view change does', async () => {
    width = 700;
    await openCode();
    fireEvent.click(screen.getByRole('button', { name: 'Play' }));
    expect(screen.getByRole('button', { name: 'Pause' })).toBeInTheDocument();
    fireEvent.click(within(screen.getByRole('group', { name: 'Show in Code walkthrough' })).getByRole('button', { name: 'Diagram' }));
    expect(screen.getByRole('button', { name: 'Play' })).toBeInTheDocument();
  });

  it('keeps the horizontal source position across view changes too', async () => {
    await openCode();
    const source = screen.getByRole('region', { name: 'Java source' });
    source.scrollLeft = 100;
    source.scrollTop = 40;
    fireEvent.click(screen.getByRole('tab', { name: 'Analysis' }));
    fireEvent.click(screen.getByRole('tab', { name: 'Code walkthrough' }));
    const again = screen.getByRole('region', { name: 'Java source' });
    expect(again.scrollLeft).toBe(100);
    expect(again.scrollTop).toBe(40);
  });

  it('starts a different problem\'s source fresh, following, at the top', async () => {
    await openCode();
    const source = screen.getByRole('region', { name: 'Java source' });
    fireEvent.wheel(source);
    source.scrollTop = 200;
    expect(screen.getByText(/Not following/)).toBeInTheDocument();

    const { act } = await import('@testing-library/react');
    act(() => navigateTo('/problem/fib-two?view=code'));
    await screen.findByRole('heading', { level: 1, name: 'Fibonacci Two' });
    await waitFor(() => expect(screen.getByText('Following execution')).toBeInTheDocument());
    expect(screen.getByRole('region', { name: 'Java source' }).scrollTop).toBe(0);
  });
});

describe('Separator lifecycle and geometry (review S5)', () => {
  it('leaves no pointer listeners behind when Code view unmounts mid-drag', async () => {
    const added = [];
    const removed = [];
    const add = window.addEventListener.bind(window);
    const remove = window.removeEventListener.bind(window);
    vi.spyOn(window, 'addEventListener').mockImplementation((type, fn, o) => { if (type.startsWith('pointer')) added.push(fn); return add(type, fn, o); });
    vi.spyOn(window, 'removeEventListener').mockImplementation((type, fn, o) => { if (type.startsWith('pointer')) removed.push(fn); return remove(type, fn, o); });
    await openCode();
    fireEvent.pointerDown(screen.getByRole('separator'), { clientX: 600, pointerId: 1 });
    fireEvent.click(screen.getByRole('tab', { name: 'Analysis' }));
    expect(added.filter((fn) => !removed.includes(fn))).toEqual([]);
  });

  it('stops resizing when the pointer is cancelled', async () => {
    // jsdom has no PointerEvent: supply coordinates and identity using MouseEvent fields.
    vi.stubGlobal('PointerEvent', MouseEvent);
    await openCode();
    const separator = screen.getByRole('separator');
    fireEvent.pointerDown(separator, { clientX: 600, pointerId: 1 });
    fireEvent.pointerMove(separator, { clientX: 780, pointerId: 1 });
    expect(separator).toHaveAttribute('aria-valuenow', '65');
    fireEvent.pointerCancel(separator, { pointerId: 1 });
    fireEvent.pointerMove(separator, { clientX: 200, pointerId: 1 });
    expect(separator).toHaveAttribute('aria-valuenow', '65');
  });

  it('announces the split the pane minimums actually allow', async () => {
    // At a 1000px container the 360px source minimum caps the diagram at 976-360 = 616px,
    // i.e. 63% - not the 70% preference the old separator announced.
    width = 1000;
    await openCode();
    const separator = screen.getByRole('separator');
    fireEvent.keyDown(separator, { key: 'End', code: 'End' });
    expect(separator).toHaveAttribute('aria-valuenow', '63');
    expect(separator).toHaveAttribute('aria-valuemax', '63');
    expect(separator).toHaveAttribute('aria-valuetext', 'Diagram 63%, source 37%');
    fireEvent.keyDown(separator, { key: 'Home', code: 'Home' });
    expect(separator).toHaveAttribute('aria-valuenow', '49');
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

  it('omits a call stack the run never has, and claims no queue or stack order for a Graph or Grid container', async () => {
    // flood-fill is a Matrix hero whose container is a STACK (FloodFillTracer .stack()); the
    // payload does not distinguish it from BFS's queue, so neither may be called either.
    const GRID = { ...PROBLEM, id: 'flood', dsType: 'Matrix' };
    const gridSteps = [
      { stepNumber: 1, activeLine: 1, keyframe: true, dsType: 'Matrix', description: 'push start', variables: { r: 0 }, callStack: [], queueOrStackState: ['(0,0)'] },
      { stepNumber: 2, activeLine: 1, dsType: 'Matrix', description: 'push neighbours', variables: { r: 0 }, queueOrStackState: ['(0,1)', '(1,0)'] }
    ];
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/problems') return Promise.resolve(ok([GRID]));
      if (url === '/api/problems/flood') return Promise.resolve(ok(GRID));
      return Promise.resolve(ok({ encoding: 'delta', resolvedInput: {}, anchors: {}, steps: gridSteps }));
    }));
    renderAt('/problem/flood?view=analysis&step=2');
    const container = await screen.findByRole('region', { name: 'Container contents' });
    await waitFor(() => expect(within(container).getAllByRole('listitem')).toHaveLength(2));
    expect(within(container).getByText(/in the order the trace lists them/i)).toBeInTheDocument();
    expect(within(container).queryByText(/Front|Back|Top/)).not.toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Queue contents' })).not.toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Call stack' })).not.toBeInTheDocument();
  });

  it('keeps Top for a Stack hero and Front/Back for a Queue hero, whose dsType says so', async () => {
    for (const [dsType, name, first, last] of [['Stack', 'Stack contents', null, 'Top'], ['Queue', 'Queue contents', 'Front', 'Back']]) {
      const entry = { ...PROBLEM, id: `hero-${dsType}`, dsType };
      const heroSteps = [{ stepNumber: 1, activeLine: 1, keyframe: true, dsType, description: 'hold', variables: {}, queueOrStackState: ['x', 'y'] }];
      vi.stubGlobal('fetch', vi.fn((url) => {
        if (url === '/api/problems') return Promise.resolve(ok([entry]));
        if (url === `/api/problems/${entry.id}`) return Promise.resolve(ok(entry));
        return Promise.resolve(ok({ encoding: 'delta', resolvedInput: {}, anchors: {}, steps: heroSteps }));
      }));
      const { unmount } = renderAt(`/problem/${entry.id}?view=analysis`);
      const region = await screen.findByRole('region', { name });
      const items = within(region).getAllByRole('listitem');
      if (first) expect(items[0]).toHaveTextContent(first);
      expect(items[1]).toHaveTextContent(last);
      unmount();
    }
  });
});
