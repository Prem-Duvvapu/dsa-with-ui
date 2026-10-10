import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import AppRouter from '../AppRouter';
import { encodeInput } from '../hooks/useShareableView';
import fixture from '../test/frogApproaches.json';

// Preserve Controls.test's clipboard guards on the UI a learner actually uses.
// Successful trace fixtures are genuine API responses; no invented custom run is served.
let view;
beforeEach(() => {
  Element.prototype.scrollIntoView = vi.fn();
  vi.stubGlobal('fetch', vi.fn((url, options = {}) => {
    if (url === '/api/problems') return Promise.resolve(ok([fixture.problem]));
    const parsed = new URL(url, window.location.origin);
    const id = parsed.searchParams.get('approach') ?? 'canonical';
    const run = fixture.runs[id];
    if (!run) throw Error(`Unrecorded executable ${id}`);
    if (parsed.pathname.endsWith('/execute')) {
      if (options.method === 'POST') expect(JSON.parse(options.body)).toEqual(run.resolvedInput);
      return Promise.resolve(ok(run));
    }
    const definition = fixture.problem.approaches.find(option => option.id === id);
    return Promise.resolve(ok({ ...fixture.problem, approachId: id, javaCode: run.code,
      dsType: run.dsType, complexity: run.complexity, inputSpec: definition.inputSpec,
      alternateInput: definition.alternateInput, teaching: definition.teaching }));
  }));
});
afterEach(() => {
  view?.unmount();
  view = null;
  vi.restoreAllMocks();
  vi.useRealTimers();
  vi.unstubAllGlobals();
  window.history.replaceState(null, '', '/');
});
const ok = body => ({ ok: true, status: 200, json: async () => structuredClone(body) });
async function ready(initialView = 'playground') {
  window.history.replaceState(null, '', `/problem/frog-jump?${new URLSearchParams({
    approach: 'memoization', input: encodeInput(fixture.runs.memoization.resolvedInput), step: '3', view: initialView
  })}`);
  view = render(<BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}><AppRouter /></BrowserRouter>);
  await waitFor(() => expect(screen.getByRole('slider', { name: 'Seek to step' })).toHaveValue('2'));
}

describe('workspace sharing (retired Controls guards)', () => {
  it('copies the actual restored address with input, approach and the newly selected step', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal('navigator', { ...navigator, clipboard: { writeText } });
    await ready();
    fireEvent.change(screen.getByRole('slider', { name: 'Seek to step' }), { target: { value: '4' } });
    await waitFor(() => expect(new URLSearchParams(window.location.search).get('step')).toBe('5'));
    const calls = fetch.mock.calls.length;
    fireEvent.click(screen.getByRole('button', { name: 'Copy link' }));
    await waitFor(() => expect(writeText).toHaveBeenCalledWith(window.location.href));
    const shared = new URL(writeText.mock.calls[0][0]).searchParams;
    expect(shared.get('approach')).toBe('memoization');
    expect(shared.get('input')).toBe(encodeInput(fixture.runs.memoization.resolvedInput));
    expect(shared.get('step')).toBe('5');
    expect(fetch).toHaveBeenCalledTimes(calls);
  });

  it('announces copy success and restores the copy affordance after its existing timeout', async () => {
    vi.stubGlobal('navigator', { ...navigator, clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });
    await ready();
    vi.useFakeTimers();
    await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Copy link' })); });
    expect(screen.getByRole('button', { name: 'Link copied' })).toBeInTheDocument();
    expect(screen.getByText('Link to this run and step copied.')).toHaveAttribute('role', 'status');
    act(() => vi.advanceTimersByTime(2500));
    expect(screen.getByRole('button', { name: 'Copy link' })).toBeInTheDocument();
    expect(screen.queryByText('Link to this run and step copied.')).not.toBeInTheDocument();
  });

  it.each(['denied', 'absent'])('announces an actionable failure when the clipboard is %s', async failure => {
    vi.stubGlobal('navigator', { ...navigator, clipboard: failure === 'absent' ? undefined
      : { writeText: vi.fn().mockRejectedValue(Error('denied')) } });
    await ready();
    fireEvent.click(screen.getByRole('button', { name: 'Copy link' }));
    const status = await screen.findByText('Could not copy the link. Copy the address bar instead.');
    expect(status).toHaveAttribute('role', 'status');
    expect(screen.getByRole('button', { name: 'Copy failed' })).toBeInTheDocument();
  });

  it('clears the copy timer when the workspace unmounts', async () => {
    vi.stubGlobal('navigator', { ...navigator, clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });
    await ready();
    const schedule = vi.spyOn(globalThis, 'setTimeout');
    await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Copy link' })); });
    const copyTimerIndex = schedule.mock.calls.findIndex(([, delay]) => delay === 2500);
    expect(copyTimerIndex).toBeGreaterThanOrEqual(0);
    const copyTimer = schedule.mock.results[copyTimerIndex].value;
    const clear = vi.spyOn(globalThis, 'clearTimeout');
    view.unmount(); view = null;
    expect(clear).toHaveBeenCalledWith(copyTimer);
  });

  it.each(['code', 'analysis'])('copies the current %s view through the switcher quick action without executing again', async initialView => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal('navigator', { ...navigator, clipboard: { writeText } });
    await ready(initialView);
    const calls = fetch.mock.calls.length;
    const address = window.location.href;
    fireEvent.keyDown(window, { key: 'k', code: 'KeyK', ctrlKey: true });
    fireEvent.click(await screen.findByRole('option', { name: 'Copy link to this step' }));
    await waitFor(() => expect(writeText).toHaveBeenCalledWith(address));
    expect(new URL(writeText.mock.calls[0][0]).searchParams.get('view')).toBe(initialView);
    expect(fetch).toHaveBeenCalledTimes(calls);
    expect(screen.getByRole('option', { name: 'Link copied' })).toBeInTheDocument();
  });
});
