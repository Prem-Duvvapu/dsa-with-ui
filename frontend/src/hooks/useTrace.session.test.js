import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import useTrace from './useTrace';

/**
 * The run-identity contract (IMPLEMENTATION_HANDOFF.md §6): a run's steps, the input it
 * ran on, its anchors and its truncation flag are one committed unit. A failed rerun keeps
 * the last valid unit on screen and says so; it never blanks it, and it never pairs new
 * steps with old metadata.
 */

function ok(body) {
  return { ok: true, status: 200, json: () => Promise.resolve(body) };
}

function traceFor(label, resolvedInput, extra = {}) {
  return {
    encoding: 'delta',
    truncated: false,
    resolvedInput,
    anchors: { start: 1 },
    steps: [
      { stepNumber: 1, activeLine: 1, keyframe: true, dsType: 'Array', variables: {},
        description: `${label} one`, arrayState: [{ index: 0, value: 1, state: 'default' }] },
      { stepNumber: 2, activeLine: 1, dsType: 'Array', description: `${label} two` }
    ],
    ...extra
  };
}

const DEFAULTS = { nums: [2, 7], target: 9 };

function stub(handler) {
  vi.stubGlobal('fetch', vi.fn((url, opts = {}) => {
    if (url === '/api/problems/two-sum') return Promise.resolve(ok({ id: 'two-sum', title: 'Two Sum' }));
    if (url === '/api/problems/two-sum/execute' && opts.method !== 'POST') {
      return Promise.resolve(ok(traceFor('default', DEFAULTS)));
    }
    return handler(url, opts);
  }));
}

async function loaded() {
  const hook = renderHook(() => useTrace('two-sum', null));
  await waitFor(() => expect(hook.result.current.loading).toBe(false));
  expect(hook.result.current.steps[0].description).toBe('default one');
  return hook;
}

beforeEach(() => stub(() => Promise.resolve(ok(null))));
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

describe('useTrace run identity', () => {
  it('commits the input a custom run echoes together with its steps', async () => {
    stub((url, opts) => Promise.resolve(ok(traceFor('custom', JSON.parse(opts.body)))));
    const { result } = await loaded();
    expect(result.current.resolvedInput).toEqual(DEFAULTS);

    let outcome;
    await act(async () => { outcome = await result.current.runInput({ nums: [1, 2], target: 3 }); });

    expect(outcome).toMatchObject({ ok: true });
    expect(result.current.steps[0].description).toBe('custom one');
    // The defect: steps were replaced but the echo still described the default run.
    expect(result.current.resolvedInput).toEqual({ nums: [1, 2], target: 3 });
    expect(result.current.run.submittedInput).toEqual({ nums: [1, 2], target: 3 });
  });

  it('falls back to the submitted values when a success carries no echo', async () => {
    stub(() => Promise.resolve(ok(traceFor('custom', undefined))));
    const { result } = await loaded();
    await act(async () => { await result.current.runInput({ nums: [4], target: 4 }); });
    expect(result.current.resolvedInput).toEqual({ nums: [4], target: 4 });
  });

  it('snapshots the submitted input so later edits cannot rewrite what ran', async () => {
    stub(() => Promise.resolve(ok(traceFor('custom', undefined))));
    const { result } = await loaded();
    const draft = { nums: [1, 2], target: 3 };
    await act(async () => { await result.current.runInput(draft); });
    draft.nums.push(99);
    draft.target = 100;
    expect(result.current.run.submittedInput).toEqual({ nums: [1, 2], target: 3 });
  });

  it.each([
    ['network', () => Promise.reject(new Error('offline')), 'fetch'],
    ['malformed', () => Promise.resolve(ok({ unexpected: true })), 'malformed'],
    ['empty', () => Promise.resolve(ok([])), 'empty'],
    ['server error', () => Promise.resolve({ ok: false, status: 500, json: () => Promise.resolve(null) }), 'fetch'],
    ['rate limit', () => Promise.resolve({ ok: false, status: 429, json: () => Promise.resolve(null) }), 'rate-limited']
  ])('keeps the last valid run and labels a %s rerun failure', async (_name, respond, kind) => {
    stub(() => respond());
    const { result } = await loaded();

    let outcome;
    await act(async () => { outcome = await result.current.runInput({ nums: [1], target: 1 }); });

    expect(outcome).toMatchObject({ ok: false, kind });
    expect(result.current.steps[0].description).toBe('default one');
    expect(result.current.resolvedInput).toEqual(DEFAULTS);
    expect(result.current.rerunFailure).toMatchObject({ kind, input: { nums: [1], target: 1 } });
    expect(result.current.pending).toBe(false);
  });

  it('returns field errors as an invalid outcome and keeps the run', async () => {
    stub(() => Promise.resolve({
      ok: false, status: 400,
      json: () => Promise.resolve({ fieldErrors: { target: 'Must be at most 100.' } })
    }));
    const { result } = await loaded();

    let outcome;
    await act(async () => { outcome = await result.current.runInput({ nums: [1], target: 999 }); });

    expect(outcome).toMatchObject({ ok: false, kind: 'invalid', fieldErrors: { target: 'Must be at most 100.' } });
    expect(result.current.fieldErrors).toEqual({ target: 'Must be at most 100.' });
    expect(result.current.rerunFailure).toBeNull();
    expect(result.current.steps[0].description).toBe('default one');
  });

  it('clears a labelled failure once a later run succeeds', async () => {
    let fail = true;
    stub(() => (fail ? Promise.reject(new Error('down')) : Promise.resolve(ok(traceFor('custom', { nums: [3] })))));
    const { result } = await loaded();
    await act(async () => { await result.current.runInput({ nums: [3] }); });
    expect(result.current.rerunFailure).not.toBeNull();

    fail = false;
    await act(async () => { await result.current.runInput({ nums: [3] }); });
    expect(result.current.rerunFailure).toBeNull();
    expect(result.current.steps[0].description).toBe('custom one');
  });

  it('ignores an older run whose body finishes decoding after a newer run started', async () => {
    let releaseSlowBody;
    stub((url, opts) => {
      const body = JSON.parse(opts.body);
      if (body.target === 1) {
        // Headers arrive at once; the body decodes late. The version guard has to run
        // again after decoding, not only after the headers.
        return Promise.resolve({
          ok: true, status: 200,
          json: () => new Promise((resolve) => { releaseSlowBody = () => resolve(traceFor('older', body)); })
        });
      }
      return Promise.resolve(ok(traceFor('newer', body)));
    });
    const { result } = await loaded();

    let older;
    await act(async () => { older = result.current.runInput({ nums: [1], target: 1 }); });
    await waitFor(() => expect(releaseSlowBody).toBeTypeOf('function'));
    await act(async () => { await result.current.runInput({ nums: [2], target: 2 }); });
    expect(result.current.steps[0].description).toBe('newer one');

    await act(async () => { releaseSlowBody(); await older; });
    expect(result.current.steps[0].description).toBe('newer one');
    expect(result.current.resolvedInput).toEqual({ nums: [2], target: 2 });
    await expect(older).resolves.toMatchObject({ ok: false, kind: 'superseded' });
  });

  it('pauses playback when a run is submitted', async () => {
    let release;
    stub(() => new Promise((resolve) => { release = () => resolve(ok(traceFor('custom', { nums: [5] }))); }));
    const { result } = await loaded();
    act(() => result.current.play());
    expect(result.current.isPlaying).toBe(true);

    let pending;
    act(() => { pending = result.current.runInput({ nums: [5] }); });
    expect(result.current.isPlaying).toBe(false);
    expect(result.current.pending).toBe(true);
    // The old run stays drawable while the new one is in flight.
    expect(result.current.loading).toBe(false);
    expect(result.current.steps[0].description).toBe('default one');
    await act(async () => { release(); await pending; });
    expect(result.current.pending).toBe(false);
  });

  it('never hands a newly requested problem the previous problem\'s run, even for one render', async () => {
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url === '/api/problems/b/execute') return new Promise(() => {});
      if (url.endsWith('/execute')) return Promise.resolve(ok(traceFor('a', { n: 1 })));
      return Promise.resolve(ok({ id: url.split('/').pop() }));
    }));
    const seen = [];
    const { result, rerender } = renderHook(({ id }) => {
      const value = useTrace(id, null);
      seen.push({ id, first: value.steps[0]?.description ?? null, echo: value.resolvedInput, loading: value.loading });
      return value;
    }, { initialProps: { id: 'a' } });
    await waitFor(() => expect(result.current.steps).toHaveLength(2));

    rerender({ id: 'b' });
    const forB = seen.filter((r) => r.id === 'b');
    expect(forB.every((r) => r.first === null && r.echo === null)).toBe(true);
    expect(forB.every((r) => r.loading)).toBe(true);
  });

  it('abandons an in-flight run when it unmounts, so its continuation reports superseded', async () => {
    let release;
    stub(() => new Promise((resolve) => { release = () => resolve(ok(traceFor('late', { n: 3 }))); }));
    const { result, unmount } = await loaded();
    let pending;
    act(() => { pending = result.current.runInput({ n: 3 }); });
    const signal = fetch.mock.calls.at(-1)[1].signal;
    unmount();
    expect(signal.aborted).toBe(true);
    release();
    await expect(pending).resolves.toMatchObject({ ok: false, kind: 'superseded' });
  });

  it('pauses when the tab is hidden and does not resume by itself', async () => {
    const { result } = await loaded();
    act(() => result.current.play());
    expect(result.current.isPlaying).toBe(true);

    const hidden = vi.spyOn(document, 'hidden', 'get').mockReturnValue(true);
    act(() => { document.dispatchEvent(new Event('visibilitychange')); });
    expect(result.current.isPlaying).toBe(false);

    hidden.mockReturnValue(false);
    act(() => { document.dispatchEvent(new Event('visibilitychange')); });
    expect(result.current.isPlaying).toBe(false);
  });

  it('reports an unknown id as not found rather than as a network failure', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: false, status: 404, json: () => Promise.resolve(null) })));
    const { result } = renderHook(() => useTrace('no-such-problem', null));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe('notfound');
    expect(result.current.steps).toEqual([]);
  });

  it('never pairs a new problem with the previous problem\'s detail, even for one render', async () => {
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url.endsWith('/execute')) return Promise.resolve(ok(traceFor(url, {})));
      if (url === '/api/problems/b') return new Promise(() => {});
      return Promise.resolve(ok({ id: url.split('/').pop(), title: 'A' }));
    }));
    const seen = [];
    const { result, rerender } = renderHook(({ id }) => {
      const value = useTrace(id, null);
      seen.push({ id, detailId: value.detail?.id ?? null });
      return value;
    }, { initialProps: { id: 'a' } });
    await waitFor(() => expect(result.current.detail?.id).toBe('a'));

    rerender({ id: 'b' });
    // Every render that asked for "b" must not have been handed a's metadata - the first
    // render after a switch happens before any effect can clear it.
    expect(seen.filter((r) => r.id === 'b' && r.detailId === 'a')).toEqual([]);
  });
});
