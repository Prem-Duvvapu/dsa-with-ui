import React from 'react';
import { render, screen, act, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import useShareableView, { encodeInput, decodeInput, parseSharedInput } from './useShareableView';

function Harness({ problemId, stepIndex, totalSteps, onRestore }) {
  const { shareInput } = useShareableView({ problemId, stepIndex, totalSteps, onRestore });
  return <button onClick={() => shareInput({ nums: [1, 2] })}>share</button>;
}

const at = (url, props) => render(
  <MemoryRouter initialEntries={[url]}>
    <Routes><Route path="/problem/:id" element={<Harness {...props} />} /></Routes>
  </MemoryRouter>
);

describe('useShareableView', () => {
  it('round-trips an input map through the URL', () => {
    const values = { word1: 'horse', nums: [3, 1, 2] };
    expect(decodeInput(encodeInput(values))).toEqual(values);
  });

  it('tells an unreadable input apart from an absent one', () => {
    expect(parseSharedInput(null)).toEqual({ status: 'absent' });
    expect(parseSharedInput('')).toEqual({ status: 'absent' });
    expect(parseSharedInput('not-valid-json')).toEqual({ status: 'invalid' });
    expect(parseSharedInput(encodeInput([1]))).toEqual({ status: 'invalid' });
    expect(parseSharedInput(encodeInput({ n: 1 }))).toEqual({ status: 'valid', value: { n: 1 } });
  });

  it('refuses anything that is not an input map, rather than half-restoring it', () => {
    expect(decodeInput(encodeInput([1, 2, 3]))).toBeNull();
    expect(decodeInput('not base64 at all !!')).toBeNull();
    expect(decodeInput(null)).toBeNull();
  });

  it('recovers the step and the input a link carried', () => {
    const onRestore = vi.fn();
    const encoded = encodeInput({ nums: [9] });
    at(`/problem/kadane-algo?step=5&input=${encoded}`, {
      problemId: 'kadane-algo', stepIndex: 0, totalSteps: 0, onRestore
    });
    // 1-based in the URL, 0-based in the player: step=5 is index 4.
    expect(onRestore).toHaveBeenCalledWith({ step: 4, input: { status: 'valid', value: { nums: [9] } }, approachId: null, samePage: false });
  });

  it('restores once per problem, not on every render', () => {
    const onRestore = vi.fn();
    const { rerender } = at('/problem/kadane-algo?step=3', {
      problemId: 'kadane-algo', stepIndex: 0, totalSteps: 0, onRestore
    });
    rerender(
      <MemoryRouter initialEntries={['/problem/kadane-algo?step=3']}>
        <Routes><Route path="/problem/:id" element={
          <Harness problemId="kadane-algo" stepIndex={2} totalSteps={9} onRestore={onRestore} />
        } /></Routes>
      </MemoryRouter>
    );
    expect(onRestore).toHaveBeenCalledTimes(1);
  });

  it('ignores a step that is not a positive integer', () => {
    const onRestore = vi.fn();
    at('/problem/kadane-algo?step=abc', {
      problemId: 'kadane-algo', stepIndex: 0, totalSteps: 0, onRestore
    });
    expect(onRestore).toHaveBeenCalledWith({ step: null, input: { status: 'absent' }, approachId: null, samePage: false });
  });

  it('merges two writes made in the same tick instead of dropping the first', () => {
    // A view change and an input share in one handler used to race: each started from the
    // params captured at render time, so the second write erased the first.
    let location;
    function Writer() {
      const { shareInput, setView } = useShareableView({ problemId: 'p', stepIndex: 0, totalSteps: 0 });
      location = useLocation();
      return <button onClick={() => { setView('code'); shareInput({ nums: [3] }); }}>both</button>;
    }
    render(
      <MemoryRouter initialEntries={['/problem/p?step=4']}>
        <Routes><Route path="/problem/:id" element={<Writer />} /></Routes>
      </MemoryRouter>
    );
    fireEvent.click(screen.getByText('both'));
    const params = new URLSearchParams(location.search);
    expect(params.get('view')).toBe('code');
    expect(decodeInput(params.get('input'))).toEqual({ nums: [3] });
    expect(params.get('step')).toBe('4');
  });

  it('refuses to put an input that is too long for a link into the URL', () => {
    let location;
    let result;
    function Writer() {
      const { shareInput } = useShareableView({ problemId: 'p', stepIndex: 0, totalSteps: 0 });
      location = useLocation();
      return <button onClick={() => { result = shareInput({ s: 'x'.repeat(5000) }); }}>long</button>;
    }
    render(
      <MemoryRouter initialEntries={['/problem/p']}>
        <Routes><Route path="/problem/:id" element={<Writer />} /></Routes>
      </MemoryRouter>
    );
    fireEvent.click(screen.getByText('long'));
    expect(result).toBe(false);
    expect(new URLSearchParams(location.search).get('input')).toBeNull();
  });

  it('does not mirror the step while a link is still being restored', () => {
    let location;
    function Mirror({ mirror }) {
      useShareableView({ problemId: 'p', stepIndex: 0, totalSteps: 9, mirror, onRestore: () => {} });
      location = useLocation();
      return null;
    }
    const tree = (mirror) => (
      <MemoryRouter initialEntries={['/problem/p?step=7']}>
        <Routes><Route path="/problem/:id" element={<Mirror mirror={mirror} />} /></Routes>
      </MemoryRouter>
    );
    render(tree(false));
    expect(new URLSearchParams(location.search).get('step')).toBe('7');
  });
});
