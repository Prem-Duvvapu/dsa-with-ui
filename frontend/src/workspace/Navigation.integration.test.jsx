import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import ProblemWorkspace from './ProblemWorkspace';
import { encodeInput, decodeInput } from '../hooks/useShareableView';

/**
 * Same-page external navigation (docs/ui-revamp/INDEPENDENT_REVIEW_DB8683B.md B1, B2, S1).
 * A navigation to the same problem is a new session identity: whatever was in flight for the
 * old URL must not land, the step must follow the new URL, and a link that cannot be honoured
 * must describe - and share - the run actually left on screen.
 */

let location;
let navigateTo;
function Probe() {
  location = useLocation();
  navigateTo = useNavigate();
  return null;
}
const renderAt = (path) => render(
  <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <Routes><Route path="/problem/:id" element={<ProblemWorkspace />} /></Routes>
    <Probe />
  </MemoryRouter>
);
const params = () => new URLSearchParams(location.search);
const ok = (body) => ({ ok: true, status: 200, json: () => Promise.resolve(body) });
const ALPHA = {
  id: 'alpha', title: 'Alpha', category: 'Test', difficulty: 'Easy', dsType: 'Array', traced: true, javaCode: 'void a() {}',
  inputSpec: { fields: [{ name: 'n', label: 'Count', type: 'INT', defaultValue: 1, constraints: { min: 0, max: 50 } }] }
};
const trace = (n) => ({
  encoding: 'delta', resolvedInput: { n }, anchors: {},
  steps: Array.from({ length: 5 }, (_, i) => ({ stepNumber: i + 1, activeLine: 1, keyframe: i === 0, dsType: 'Array',
    variables: {}, description: `n=${n} step ${i + 1}`, arrayState: [{ index: 0, value: i, state: 'default' }] }))
});

let held;
let hold;
beforeEach(() => {
  held = [];
  hold = false;
  Element.prototype.scrollIntoView = vi.fn();
  vi.stubGlobal('fetch', vi.fn((url, opts = {}) => {
    if (url === '/api/problems') return Promise.resolve(ok([ALPHA]));
    if (url === '/api/problems/alpha') return Promise.resolve(ok(ALPHA));
    if (opts.method === 'POST') {
      const { n } = JSON.parse(opts.body);
      const respond = () => (n > 50
        ? { ok: false, status: 400, json: () => Promise.resolve({ fieldErrors: { n: 'Must be at most 50.' } }) }
        : ok(trace(n)));
      if (hold) return new Promise((resolve) => held.push({ n, release: () => resolve(respond()) }));
      return Promise.resolve(respond());
    }
    return Promise.resolve(ok(trace(1)));
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

const narration = () => document.querySelector('p[aria-live="polite"]');
const link = (input, step) => `/problem/alpha?${new URLSearchParams({
  ...(input !== undefined ? { input: typeof input === 'string' ? input : encodeInput(input) } : {}),
  ...(step ? { step: String(step) } : {})
})}`;
const settle = () => act(async () => { await new Promise((r) => setTimeout(r, 30)); });

describe('Same-page navigation retires what the old URL started (B1)', () => {
  it('does not let a held link run replace the default the new URL asks for', async () => {
    hold = true;
    renderAt(link({ n: 7 }));
    await waitFor(() => expect(held).toHaveLength(1));

    act(() => navigateTo('/problem/alpha'));
    await settle();
    await act(async () => { held[0].release(); await new Promise((r) => setTimeout(r, 30)); });

    expect(narration()).toHaveTextContent('n=1 step 1');
    expect(params().get('input')).toBeNull();
  });

  it('does not let a held submission land on, or share over, a newer navigation', async () => {
    renderAt('/problem/alpha');
    await screen.findByText('n=1 step 1', { selector: 'p' });
    hold = true;
    fireEvent.change(screen.getByLabelText('Count'), { target: { value: '9' } });
    fireEvent.click(screen.getByRole('button', { name: 'Run input' }));
    await waitFor(() => expect(held).toHaveLength(1));

    act(() => navigateTo('/problem/alpha?step=2'));
    await settle();
    await act(async () => { held[0].release(); await new Promise((r) => setTimeout(r, 30)); });

    expect(narration()).toHaveTextContent('n=1 step 2');
    expect(params().get('input')).toBeNull();
    expect(params().get('step')).toBe('2');
  });
});

describe('A same-page link that cannot be honoured describes the run left on screen (B2)', () => {
  it.each([
    ['rejected', { n: 99 }],
    ['unreadable', 'not-valid-json']
  ])('keeps a %s link from claiming defaults while a custom run is shown', async (_kind, input) => {
    renderAt(link({ n: 7 }));
    await waitFor(() => expect(narration()).toHaveTextContent('n=7 step 1'));

    act(() => navigateTo(link(input)));
    const notice = await screen.findByRole('status', { name: 'Shared link' });

    expect(narration()).toHaveTextContent('n=7');
    expect(notice).not.toHaveTextContent(/showing the default input/i);
    expect(notice).toHaveTextContent(/previous run/i);
    // The link now identifies the run that is actually displayed.
    await waitFor(() => expect(decodeInput(params().get('input'))).toEqual({ n: 7 }));
  });

  it('still says "default input" when the default really is what is shown', async () => {
    renderAt('/problem/alpha');
    await waitFor(() => expect(narration()).toHaveTextContent('n=1 step 1'));
    act(() => navigateTo(link({ n: 99 })));
    const notice = await screen.findByRole('status', { name: 'Shared link' });
    expect(notice).toHaveTextContent(/showing the default input/i);
    await waitFor(() => expect(params().get('input')).toBeNull());
  });
});

describe('Every same-page navigation applies its own step (S1)', () => {
  async function atStepThree() {
    renderAt('/problem/alpha');
    await waitFor(() => expect(narration()).toHaveTextContent('n=1 step 1'));
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(narration()).toHaveTextContent('n=1 step 3');
  }

  it('returns to step 1 for a same-page link with no step', async () => {
    await atStepThree();
    act(() => navigateTo('/problem/alpha'));
    await waitFor(() => expect(narration()).toHaveTextContent('n=1 step 1'));
  });

  it('actually shows step 1 when it says an out-of-range step fell back to step 1', async () => {
    await atStepThree();
    act(() => navigateTo('/problem/alpha?step=400'));
    const notice = await screen.findByRole('status', { name: 'Shared link' });
    expect(notice).toHaveTextContent(/Showing step 1/);
    expect(narration()).toHaveTextContent('n=1 step 1');
  });

  it('moves to a valid requested step on the same page', async () => {
    await atStepThree();
    act(() => navigateTo('/problem/alpha?step=5'));
    await waitFor(() => expect(narration()).toHaveTextContent('n=1 step 5'));
  });
});
