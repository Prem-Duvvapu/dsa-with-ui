import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import ProblemWorkspace from './ProblemWorkspace';
import { encodeInput } from '../hooks/useShareableView';

/**
 * Session and URL lifecycle across navigation (independent review of 3db06a5, findings
 * 1-3, 6, 7). Each case is a sequence in which the screen, the draft, the executed request
 * or the URL stopped describing the same run - or a page kept acting after it was left.
 */

let latestLocation;
let navigateTo;
function Probe() {
  latestLocation = useLocation();
  navigateTo = useNavigate();
  return null;
}

function renderApp(path) {
  return render(
    <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        <Route path="/problem/:id" element={<ProblemWorkspace />} />
        <Route path="/" element={<p>Library home</p>} />
      </Routes>
      <Probe />
    </MemoryRouter>
  );
}

const params = () => new URLSearchParams(latestLocation.search);
const ok = (body) => ({ ok: true, status: 200, json: () => Promise.resolve(body) });

const spec = (defaultValue) => ({ fields: [{ name: 'n', label: 'Count', type: 'INT', defaultValue, constraints: { min: 0, max: 50 } }] });
const A = { id: 'alpha', title: 'Alpha', category: 'Test', difficulty: 'Easy', dsType: 'Array', traced: true, striverSheetSection: 'Section', javaCode: 'void a() {}', inputSpec: spec(1) };
const B = { id: 'beta', title: 'Beta', category: 'Test', difficulty: 'Easy', dsType: 'Array', traced: true, striverSheetSection: 'Section', javaCode: 'void b() {}', inputSpec: spec(2) };

function trace(id, input, count = 5) {
  return {
    encoding: 'delta', truncated: false, resolvedInput: input, anchors: {},
    steps: Array.from({ length: count }, (_, i) => ({
      stepNumber: i + 1, activeLine: 1, keyframe: i === 0, dsType: 'Array', variables: {},
      description: `${id} ran n=${input.n} step ${i + 1}`,
      arrayState: [{ index: 0, value: i, state: 'default' }]
    }))
  };
}

let posts;
let holdPosts;
let released;
let failDefault;
beforeEach(() => {
  posts = [];
  holdPosts = false;
  released = [];
  failDefault = false;
  vi.stubGlobal('fetch', vi.fn((url, opts = {}) => {
    if (url === '/api/problems') return Promise.resolve(ok([A, B]));
    const detail = url.match(/^\/api\/problems\/([^/]+)$/);
    if (detail) return Promise.resolve(ok([A, B].find((p) => p.id === detail[1])));
    const exec = url.match(/^\/api\/problems\/([^/]+)\/execute$/);
    const problem = [A, B].find((p) => p.id === exec?.[1]);
    if (!problem) return Promise.resolve({ ok: false, status: 404, json: () => Promise.resolve(null) });
    if (opts.method === 'POST') {
      const body = JSON.parse(opts.body);
      posts.push({ id: problem.id, body });
      const respond = () => (body.n > 50
        ? { ok: false, status: 400, json: () => Promise.resolve({ fieldErrors: { n: 'Must be at most 50.' } }) }
        : ok(trace(problem.id, body)));
      if (holdPosts) return new Promise((resolve) => released.push(() => resolve(respond())));
      return Promise.resolve(respond());
    }
    if (failDefault) return Promise.reject(new Error('offline'));
    return Promise.resolve(ok(trace(problem.id, { n: problem.inputSpec.fields[0].defaultValue })));
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

const link = (id, input, step) => `/problem/${id}?${new URLSearchParams({
  ...(input ? { input: encodeInput(input) } : {}), ...(step ? { step: String(step) } : {})
})}`;

describe('App lifecycle: navigating away from a restored link (review #1)', () => {
  it('never runs the previous problem\'s shared input on the next problem', async () => {
    renderApp(link('alpha', { n: 7 }, 3));
    await screen.findByText('alpha ran n=7 step 3');

    fireEvent.click(screen.getByRole('button', { name: /Next: Beta/ }));
    await screen.findByText('beta ran n=2 step 1');

    expect(posts.filter((p) => p.id === 'beta')).toEqual([]);
    expect(latestLocation.pathname).toBe('/problem/beta');
    // The URL write is a router transition; under a loaded full-suite run it has landed after
    // the default 1s (twice, RCA-053 pattern). The assertion is unchanged - only the wait.
    await waitFor(() => expect(params().get('step')).toBeNull(), { timeout: 4000 });
    expect(params().get('input')).toBeNull();
  });
});

describe('App lifecycle: leaving while a run is pending (review #2)', () => {
  it('does not navigate back when the abandoned run finishes', async () => {
    renderApp('/problem/alpha');
    await screen.findByText('alpha ran n=1 step 1');
    fireEvent.click(screen.getByRole('button', { name: 'Edit input' }));
    fireEvent.change(screen.getByLabelText('Count'), { target: { value: '9' } });
    holdPosts = true;
    fireEvent.click(screen.getByRole('button', { name: 'Run input' }));
    await waitFor(() => expect(released).toHaveLength(1));

    act(() => navigateTo('/'));
    await screen.findByText('Library home');
    await act(async () => { released[0](); await new Promise((r) => setTimeout(r, 20)); });

    expect(latestLocation.pathname).toBe('/');
    expect(latestLocation.search).toBe('');
  });
});

describe('App lifecycle: editing while a link restores (review #3)', () => {
  it('keeps edits made while the shared input is still running', async () => {
    holdPosts = true;
    renderApp(link('alpha', { n: 7 }));
    await waitFor(() => expect(released).toHaveLength(1));
    fireEvent.click(screen.getByRole('button', { name: 'Edit input' }));
    // The link's values are in the editor at once, before the run answers.
    expect(screen.getByLabelText('Count')).toHaveValue(7);
    fireEvent.change(screen.getByLabelText('Count'), { target: { value: '9' } });

    await act(async () => { released[0](); await new Promise((r) => setTimeout(r, 20)); });
    await screen.findByText('alpha ran n=7 step 1');
    expect(screen.getByLabelText('Count')).toHaveValue(9);
  });

  it('keeps edits made while a rejected shared input is running', async () => {
    holdPosts = true;
    renderApp(link('alpha', { n: 99 }));
    await waitFor(() => expect(released).toHaveLength(1));
    fireEvent.click(screen.getByRole('button', { name: 'Edit input' }));
    fireEvent.change(screen.getByLabelText('Count'), { target: { value: '4' } });

    await act(async () => { released[0](); await new Promise((r) => setTimeout(r, 20)); });
    await screen.findByRole('status', { name: 'Shared link' });
    expect(screen.getByLabelText('Count')).toHaveValue(4);
  });
});

describe('App lifecycle: a new link to the same problem (review #6)', () => {
  it('runs the new link\'s input and step instead of keeping the old run', async () => {
    renderApp('/problem/alpha');
    await screen.findByText('alpha ran n=1 step 1');

    act(() => navigateTo(link('alpha', { n: 6 }, 2)));
    await screen.findByText('alpha ran n=6 step 2');
    await waitFor(() => expect(params().get('step')).toBe('2'));
  });

  it('returns to the default run when a same-problem link carries no input', async () => {
    renderApp(link('alpha', { n: 6 }));
    await screen.findByText('alpha ran n=6 step 1');

    act(() => navigateTo('/problem/alpha?step=2'));
    await screen.findByText('alpha ran n=1 step 2');
  });
});

describe('App lifecycle: links that cannot be honoured (review #7)', () => {
  it('explains an unreadable input instead of silently running the default at its step', async () => {
    renderApp('/problem/alpha?input=not-valid-json&step=3');
    const notice = await screen.findByRole('status', { name: 'Shared link' });
    expect(notice).toHaveTextContent(/input in this link could not be read/i);
    expect(notice).toHaveTextContent(/step 3 belonged to that run/i);
    expect(screen.getByText('alpha ran n=1 step 1')).toBeInTheDocument();
    await waitFor(() => expect(params().get('input')).toBeNull());
    expect(params().get('step')).toBeNull();
  });

  it('says so when a step-only link has no run to restore into', async () => {
    failDefault = true;
    renderApp('/problem/alpha?step=3');
    const notice = await screen.findByRole('status', { name: 'Shared link' });
    expect(notice).toHaveTextContent(/step 3.*no run/i);
  });
});
