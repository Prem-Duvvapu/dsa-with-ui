import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom';
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, useLocation } from 'react-router-dom';
import AppRouter from '../AppRouter';
import { encodeInput, decodeInput } from '../hooks/useShareableView';
import fixture from '../test/frogApproaches.json';

const problem = fixture.problem;
let location, posts;
const ok = body => ({ ok: true, status: 200, json: async () => structuredClone(body) });
beforeEach(() => {
  posts = [];
  localStorage.setItem('dsa-ui:seenWelcome', 'true');
  Element.prototype.scrollIntoView = vi.fn();
  vi.stubGlobal('fetch', vi.fn((url, options = {}) => {
    if (url === '/api/problems') return Promise.resolve(ok([problem]));
    const parsed = new URL(url, 'https://test.invalid');
    const id = parsed.searchParams.get('approach') ?? 'canonical';
    const run = fixture.runs[id];
    const definition = problem.approaches.find(a => a.id === id);
    if (!run || !definition) throw Error(`Unexpected approach fixture ${id}`);
    if (!parsed.pathname.endsWith('/execute')) return Promise.resolve(ok({ ...problem,
      approachId: id, javaCode: run.code, dsType: run.dsType, complexity: run.complexity,
      inputSpec: definition.inputSpec, alternateInput: definition.alternateInput, teaching: definition.teaching }));
    if (options.method === 'POST') {
      const submitted = JSON.parse(options.body);
      const heights = submitted.heights ?? run.resolvedInput.heights;
      posts.push({ approach: id, input: submitted });
      const max = definition.inputSpec.fields[0].constraints.maxLength;
      if (heights.length > max) return Promise.resolve({ ok: false, status: 400,
        json: async () => ({ fieldErrors: { heights: `Length must be at most ${max}.` } }) });
      if (JSON.stringify(heights) !== JSON.stringify(run.resolvedInput.heights))
        throw Error('Generate a genuine API fixture for any additional successful input');
    }
    return Promise.resolve(ok(run));
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });
function mount(path = '/problem/frog-jump') {
  function Probe() { location = useLocation(); return null; }
  render(<MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <AppRouter /><Probe />
  </MemoryRouter>);
}
async function ready() {
  const selector = await screen.findByRole('combobox', { name: 'Solution approach' });
  await waitFor(() => expect(selector).toBeEnabled());
  return selector;
}

describe('Frog Jump array session with genuine API fixtures', () => {
  it('keeps existing canonical links while presenting the truthful tabulation label', async () => {
    mount('/problem/frog-jump?approach=canonical');
    const selector = await ready();
    expect(selector).toHaveValue('canonical');
    expect(within(selector).getAllByRole('option').map(o => o.textContent)).toEqual(['Recursion', 'Memoization', 'Tabulation']);
    expect(screen.getByRole('status', { name: 'Solution approach' })).toHaveTextContent('Showing: Tabulation');
    expect(screen.queryByText(/solution.*unavailable/i)).not.toBeInTheDocument();
    // An explicit approach link restores that executable with its defaults. It is
    // not a no-request link, and must not silently select a different approach.
    await waitFor(() => expect(posts).toEqual([{ approach: 'canonical', input: {} }]));
    expect(new URLSearchParams(location.search).get('input')).toBeNull();
  });

  it('keeps an eleven-element draft and old run/link when recursion is refused', async () => {
    mount();
    const selector = await ready();
    for (let i = 0; i < 7; i++) fireEvent.click(screen.getByRole('button', { name: 'Add', exact: true }));
    expect(screen.getAllByRole('spinbutton')).toHaveLength(11);
    const before = location.search;
    fireEvent.change(selector, { target: { value: 'recursion' } });
    expect(screen.getAllByRole('spinbutton')).toHaveLength(11);
    expect(screen.getByRole('button', { name: 'Add', exact: true })).toBeDisabled();
    expect(screen.getByRole('status', { name: 'Solution approach' })).toHaveTextContent('Selected: Recursion; showing: Tabulation');
    fireEvent.click(screen.getByRole('button', { name: 'Run input', exact: true }));
    const error = await screen.findByText('This input could not run');
    expect(error.closest('[role="alert"]')).toHaveFocus();
    expect(screen.getAllByRole('spinbutton')).toHaveLength(11);
    expect(posts[0].input.heights).toHaveLength(11);
    expect(location.search).toBe(before);
    expect(screen.getByRole('heading', { name: 'DP table', exact: true })).toBeInTheDocument();
  });

  it('restores a shared memoized array before seeking and showing its complete source', async () => {
    const input = fixture.runs.memoization.resolvedInput;
    mount(`/problem/frog-jump?approach=memoization&input=${encodeInput(input)}&step=3&view=code`);
    const selector = await ready();
    await waitFor(() => expect(screen.getByRole('slider', { name: 'Seek to step' })).toHaveValue('2'));
    expect(selector).toHaveValue('memoization');
    expect(posts).toEqual([{ approach: 'memoization', input }]);
    expect(screen.getByRole('tabpanel')).toHaveTextContent('Integer[] memo');
    expect(decodeInput(new URLSearchParams(location.search).get('input'))).toEqual(input);
    expect(screen.getByRole('status', { name: 'Solution approach' })).toHaveTextContent('Showing: Memoization');
  });

  it('compares the committed array despite edited chips, with real counters and independent steps', async () => {
    mount();
    const selector = await ready();
    fireEvent.change(selector, { target: { value: 'memoization' } });
    fireEvent.click(screen.getByRole('button', { name: 'Run memoization', exact: true }));
    await waitFor(() => expect(screen.getByRole('status', { name: 'Solution approach' })).toHaveTextContent('Showing: Memoization'));
    fireEvent.change(screen.getByRole('slider', { name: 'Seek to step' }), { target: { value: '2' } });
    await waitFor(() => expect(new URLSearchParams(location.search).get('step')).toBe('3'));
    const before = location.search;
    fireEvent.change(screen.getByLabelText('Position 1 value'), { target: { value: '5' } });
    fireEvent.click(screen.getByRole('button', { name: 'Compare approaches', exact: true }));
    const panel = screen.getByRole('region', { name: 'Approach comparison', exact: true });
    expect(posts).toHaveLength(1);
    expect(within(panel).getByText(/Recursion: Stair heights length 2–10, values 0–999/)).toBeVisible();
    fireEvent.click(within(panel).getByRole('button', { name: 'Run comparison', exact: true }));
    await within(panel).findByText('Both complete runs recorded the same answer.');
    expect(posts.slice(1).map(p => p.input)).toEqual([fixture.runs.memoization.resolvedInput, fixture.runs.memoization.resolvedInput]);
    const memo = within(panel).getByRole('region', { name: 'Memoization comparison' });
    expect(within(memo).getByText('Function calls').nextElementSibling).toHaveTextContent('6');
    expect(within(memo).getByText('Cache hits').nextElementSibling).toHaveTextContent('2');
    fireEvent.change(within(panel).getByRole('slider', { name: 'Recursion: step' }), { target: { value: '4' } });
    expect(within(panel).getByRole('slider', { name: 'Memoization: step' })).toHaveValue('1');
    expect(screen.getByLabelText('Position 1 value')).toHaveValue(5);
    expect(screen.getByRole('slider', { name: 'Seek to step' })).toHaveValue('2');
    expect(location.search).toBe(before);
  });
});
