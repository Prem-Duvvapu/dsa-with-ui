import React from 'react';
import { render, screen, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import useShareableView, { encodeInput, decodeInput } from './useShareableView';

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
    expect(onRestore).toHaveBeenCalledWith({ step: 4, input: { nums: [9] } });
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
    expect(onRestore).toHaveBeenCalledWith({ step: null, input: null });
  });
});
