import React, { StrictMode } from 'react';
import { renderHook, act, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import useProblemSession from './useProblemSession';
import { decodeInput, encodeInput } from './useShareableView';

const spec = (max) => ({ fields: [{ name: 'n', label: 'Stairs', type: 'INT', defaultValue: 5, constraints: { min: 1, max } }] });
const approaches = ['recursion', 'memoization', 'tabulation'].map(id => ({ id,
  label: id[0].toUpperCase() + id.slice(1), summary: `${id} purpose`, isDefault: id === 'tabulation',
  dsType: id === 'tabulation' ? 'DpTable' : 'RecursionTree', inputSpec: spec(id === 'recursion' ? 10 : 30),
  alternateInput: { n: id === 'tabulation' ? 12 : 6 },
  complexity: { timeComplexity: id === 'recursion' ? 'O(2^N)' : 'O(N)', spaceComplexity: 'O(N)' }
}));
const entry = id => ({ id, title: id, dsType: 'DpTable', inputSpec: spec(30), javaCode: 'tabulation source',
  complexity: approaches[2].complexity, approachId: 'tabulation', defaultApproachId: 'tabulation',
  approaches: recursionDefault === 5 ? approaches : approaches.map(option => option.id !== 'recursion' ? option : {
    ...option, inputSpec: { ...option.inputSpec, fields: option.inputSpec.fields.map(field => ({ ...field, defaultValue: recursionDefault })) }
  }), alternateInput: { n: 12 }, defaultArray: [{ value: 99 }], defaultTreeNodes: [{ id: 123 }] });
const response = body => ({ ok: true, status: 200, json: async () => body });
function trace(id, approach, n) {
  const definition = approaches.find(a => a.id === approach);
  return { problemId: id, approachId: approach, approachLabel: definition.label,
    dsType: definition.dsType, complexity: definition.complexity, code: `${approach} source`,
    anchors: { start: 1 }, encoding: 'full', resolvedInput: { n }, truncated: false,
    steps: Array.from({ length: 5 }, (_, i) => ({ stepNumber: i + 1, activeLine: 1,
      dsType: definition.dsType, variables: {}, callStack: [],
      description: `${id} ${approach} n=${n} step ${i + 1}` })) };
}

let posts, held, holdBodies, failDefault, reject, mismatch, mutateResponse, detailHeld, releaseDetail, location, navigate, recursionDefault;
beforeEach(() => {
  posts = []; held = []; holdBodies = false; failDefault = false; reject = null; mismatch = null;
  detailHeld = false; releaseDetail = null;
  mutateResponse = null;
  recursionDefault = 5;
  vi.stubGlobal('fetch', vi.fn((url, options = {}) => {
    const parsed = new URL(url, 'https://test.invalid');
    const id = parsed.pathname.split('/')[3];
    if (!parsed.pathname.endsWith('/execute')) {
      if (detailHeld) return new Promise(resolve => { releaseDetail = () => resolve(response(entry(id))); });
      return Promise.resolve(response(entry(id)));
    }
    const approach = parsed.searchParams.get('approach') ?? 'tabulation';
    if (options.method !== 'POST') return Promise.resolve(failDefault
      ? { ok: false, status: 500, json: async () => null } : response(trace(id, 'tabulation', 5)));
    const input = JSON.parse(options.body);
    posts.push({ id, approach, input, signal: options.signal });
    const definition = approaches.find(a => a.id === approach);
    if (!definition) return Promise.resolve({ ok: false, status: 400,
      json: async () => ({ error: 'unavailable_approach', message: 'No such approach', availableApproaches: approaches.map(a => a.id) }) });
    if (reject || input.n > definition.inputSpec.fields[0].constraints.max) return Promise.resolve({ ok: false,
      status: reject ?? 400, json: async () => ({ fieldErrors: { n: 'Too large for this approach.' } }) });
    const body = trace(mismatch?.problemId ?? id, mismatch?.approachId ?? approach, input.n ?? 5);
    mutateResponse?.(body);
    return Promise.resolve({ ok: true, status: 200, json: () => holdBodies
      ? new Promise(resolve => held.push(() => resolve(body))) : Promise.resolve(body) });
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

function mount(path = '/problem/climbing-stairs', strict = false) {
  function Probe() { location = useLocation(); navigate = useNavigate(); return null; }
  const wrapper = ({ children }) => {
    const content = <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes><Route path="/problem/:id" element={<>{children}<Probe /></>} /></Routes>
    </MemoryRouter>;
    return strict ? <StrictMode>{content}</StrictMode> : content;
  };
  return renderHook(() => {
    const { id } = useParams();
    return useProblemSession({ problemId: id, catalogEntry: entry(id) });
  }, { wrapper });
}
async function loaded() {
  const hook = mount();
  await waitFor(() => expect(hook.result.current.settled).toBe(true));
  await waitFor(() => expect(hook.result.current.approaches).toHaveLength(3));
  return hook;
}
const params = () => new URLSearchParams(location.search);
const link = (approach, input, step = 3) => `/problem/climbing-stairs?${new URLSearchParams({ approach,
  ...(input ? { input: encodeInput(input) } : {}), step: String(step), view: 'code' })}`;

describe('approach identity belongs to the committed run, not the candidate', () => {
  it('preserves an untouched draft when the new approach has different defaults', async () => {
    recursionDefault = 2;
    const { result } = await loaded();
    expect(result.current.draft.isEdited).toBe(false);
    expect(result.current.draft.values).toEqual({ n: 5 });
    const before = location.search;
    act(() => result.current.selectApproach('recursion'));
    expect(result.current.inputSpec.fields[0].defaultValue).toBe(2);
    expect(result.current.draft.values).toEqual({ n: 5 });
    expect(result.current.run.resolvedInput).toEqual({ n: 5 });
    expect(posts).toHaveLength(0);
    expect(location.search).toBe(before);
    await act(async () => result.current.submit(result.current.draft.values));
    expect(posts[0].input).toEqual({ n: 5 });
  });
  it('selects without executing, changing the draft, source, complexity or URL', async () => {
    const { result } = await loaded();
    act(() => { result.current.draft.replace({ n: 20 }); result.current.play(); });
    const before = location.search;
    act(() => result.current.selectApproach('recursion'));
    expect(posts).toHaveLength(0);
    expect(result.current.isPlaying).toBe(false);
    expect(result.current.selectedApproach.id).toBe('recursion');
    expect(result.current.draft.values).toEqual({ n: 20 });
    expect(result.current.inputSpec.fields[0].constraints.max).toBe(10);
    expect(result.current.problem.javaCode).toBe('tabulation source');
    expect(result.current.problem.complexity.timeComplexity).toBe('O(N)');
    expect(location.search).toBe(before);
  });

  it('commits selected source/type/complexity/anchors/input and shares its identity together', async () => {
    const { result } = await loaded();
    act(() => { result.current.selectApproach('recursion'); result.current.seek(3); result.current.setView('analysis'); });
    await act(async () => result.current.submit({ n: 7 }));
    expect(posts[0]).toMatchObject({ approach: 'recursion', input: { n: 7 } });
    expect(result.current.run).toMatchObject({ approachId: 'recursion', code: 'recursion source',
      dsType: 'RecursionTree', resolvedInput: { n: 7 }, anchors: { start: 1 } });
    expect(result.current.problem).toMatchObject({ javaCode: 'recursion source', dsType: 'RecursionTree',
      complexity: { timeComplexity: 'O(2^N)' }, alternateInput: { n: 6 } });
    expect(result.current.currentStepIndex).toBe(0);
    await waitFor(() => expect(params().get('approach')).toBe('recursion'));
    expect(decodeInput(params().get('input'))).toEqual({ n: 7 });
    expect(params().get('view')).toBe('analysis');
    expect(params().get('step')).toBeNull();
    // Presentation changes do not execute another approach.
    act(() => { result.current.setView('code'); result.current.seek(2); });
    expect(posts).toHaveLength(1);
  });

  it('does not borrow canonical default structures for a different executable', async () => {
    const { result } = await loaded();
    expect(result.current.problem.defaultArray).toEqual([{ value: 99 }]);
    await act(async () => result.current.submit({ n: 7 }, 'recursion'));
    expect(result.current.problem.defaultArray).toBeNull();
    expect(result.current.problem.defaultTreeNodes).toBeNull();
  });

  it('refuses tighter bounds without silently clamping the draft or changing a valid link', async () => {
    const { result } = await loaded();
    await act(async () => result.current.submit({ n: 20 }));
    act(() => { result.current.draft.replace({ n: 20 }); result.current.selectApproach('recursion'); result.current.seek(2); });
    await waitFor(() => expect(params().get('step')).toBe('3'));
    const before = location.search;
    await act(async () => result.current.submit({ n: 20 }));
    expect(result.current.fieldErrors.n).toBe('Too large for this approach.');
    expect(result.current.draft.values.n).toBe(20);
    expect(result.current.run.approachId).toBe('tabulation');
    expect(result.current.currentStepIndex).toBe(2);
    expect(location.search).toBe(before);
  });

  it('retries the failed approach and input even when the candidate has changed', async () => {
    const { result } = await loaded();
    act(() => result.current.selectApproach('recursion'));
    reject = 500;
    await act(async () => result.current.submit({ n: 6 }));
    act(() => result.current.selectApproach('memoization'));
    reject = null;
    await act(async () => result.current.retry());
    expect(posts.map(p => p.approach)).toEqual(['recursion', 'recursion']);
    expect(result.current.run.approachId).toBe('recursion');
    expect(result.current.selectedApproach.id).toBe('memoization');
  });

  it.each([{ approachId: 'memoization' }, { problemId: 'other' }])('rejects a substituted execute identity %j', async wrong => {
    const { result } = await loaded();
    act(() => result.current.selectApproach('recursion'));
    mismatch = wrong;
    let outcome;
    await act(async () => { outcome = await result.current.submit({ n: 6 }); });
    expect(outcome).toMatchObject({ ok: false, kind: 'malformed' });
    expect(result.current.run.approachId).toBe('tabulation');
    expect(params().get('approach')).toBeNull();
  });

  it.each(['resolvedInput', 'truncated'])('rejects a selected response missing %s rather than inventing it', async field => {
    const { result } = await loaded();
    mutateResponse = body => { delete body[field]; };
    let outcome;
    await act(async () => { outcome = await result.current.submit({ n: 6 }, 'recursion'); });
    expect(outcome).toMatchObject({ ok: false, kind: 'malformed' });
    expect(result.current.run.approachId).toBe('tabulation');
    expect(params().get('approach')).toBeNull();
  });

  it('does not fall back to canonical complexity when the selected API omits it', async () => {
    const { result } = await loaded();
    mutateResponse = body => { delete body.complexity; };
    await act(async () => result.current.submit({ n: 6 }, 'recursion'));
    expect(result.current.run.approachId).toBe('recursion');
    expect(result.current.problem.complexity).toBeNull();
  });

  it('does not pair late canonical detail with an already committed recursive source', async () => {
    detailHeld = true;
    const { result } = mount();
    await waitFor(() => expect(result.current.settled).toBe(true));
    await act(async () => result.current.submit({ n: 6 }, 'recursion'));
    await act(async () => releaseDetail());
    expect(result.current.problem.javaCode).toBe('recursion source');
    expect(result.current.problem.complexity.timeComplexity).toBe('O(2^N)');
  });

  it('ignores a slow decoded response after a newer approach run commits', async () => {
    const { result } = await loaded();
    holdBodies = true;
    let first;
    act(() => { first = result.current.submit({ n: 6 }, 'recursion'); });
    await waitFor(() => expect(held).toHaveLength(1));
    holdBodies = false;
    await act(async () => result.current.submit({ n: 7 }, 'memoization'));
    await act(async () => { held[0](); await first; });
    expect(result.current.run).toMatchObject({ approachId: 'memoization', code: 'memoization source', resolvedInput: { n: 7 } });
    expect(params().get('approach')).toBe('memoization');
  });
});

describe('approach-aware link restoration', () => {
  it.each([false, true])('restores approach, input and then step (StrictMode=%s)', async strict => {
    const { result } = mount(link('memoization', { n: 7 }), strict);
    await waitFor(() => expect(result.current.currentStep?.description).toBe('climbing-stairs memoization n=7 step 3'));
    expect(result.current.selectedApproach.id).toBe('memoization');
    expect(result.current.draft.values).toEqual({ n: 7 });
    expect(result.current.problem.javaCode).toBe('memoization source');
    expect(result.current.restoring).toBe(false);
    expect(params().get('approach')).toBe('memoization');
    expect(params().get('view')).toBe('code');
  });

  it('restores an approach with its defaults even when the canonical default load fails', async () => {
    failDefault = true;
    const { result } = mount(link('recursion'));
    await waitFor(() => expect(result.current.currentStep?.description).toBe('climbing-stairs recursion n=5 step 3'));
    expect(posts[0]).toMatchObject({ approach: 'recursion', input: {} });
    expect(result.current.restoring).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('refuses an unavailable approach and drops its step instead of using canonical steps', async () => {
    const { result } = mount(link('not-real', { n: 7 }));
    await waitFor(() => expect(result.current.linkNotices.some(n => n.kind === 'approach')).toBe(true));
    expect(result.current.run.approachId).toBe('tabulation');
    expect(result.current.currentStepIndex).toBe(0);
    expect(result.current.restoring).toBe(false);
    await waitFor(() => expect(params().get('approach')).toBeNull());
    expect(params().get('step')).toBeNull();
    expect(params().get('input')).toBeNull();
  });

  it('returns to canonical defaults for an approach-less same-page navigation and restores Back', async () => {
    const { result } = mount(link('recursion', { n: 7 }));
    await waitFor(() => expect(result.current.currentStep?.description).toContain('recursion n=7 step 3'));
    act(() => navigate('/problem/climbing-stairs?step=2'));
    await waitFor(() => expect(result.current.currentStep?.description).toBe('climbing-stairs tabulation n=5 step 2'));
    act(() => navigate(-1));
    await waitFor(() => expect(result.current.currentStep?.description).toBe('climbing-stairs recursion n=7 step 3'));
  });

  it('keeps edits made while an approach link body decodes', async () => {
    holdBodies = true;
    const { result } = mount(link('memoization', { n: 7 }));
    await waitFor(() => expect(held).toHaveLength(1));
    expect(result.current.draft.values.n).toBe(7);
    act(() => result.current.draft.replace({ n: 9 }));
    await act(async () => held[0]());
    await waitFor(() => expect(result.current.restoring).toBe(false));
    expect(result.current.run.resolvedInput.n).toBe(7);
    expect(result.current.draft.values.n).toBe(9);
    expect(result.current.draftChanged).toBe(true);
  });

  it('retires an approach run when navigation changes problem before body decode', async () => {
    const { result } = await loaded();
    holdBodies = true;
    let first;
    act(() => { first = result.current.submit({ n: 6 }, 'recursion'); });
    await waitFor(() => expect(held).toHaveLength(1));
    act(() => navigate('/problem/another'));
    await waitFor(() => expect(result.current.run.problemId).toBe('another'));
    await act(async () => { held[0](); await first; });
    expect(result.current.run.problemId).toBe('another');
    expect(params().get('approach')).toBeNull();
    expect(params().get('input')).toBeNull();
  });
});
