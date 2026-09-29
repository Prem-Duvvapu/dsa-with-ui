import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import { MemoryRouter, Routes, Route, useLocation, useNavigate } from 'react-router-dom';
import useLatestSearchParams from './useLatestSearchParams';

let location;
let navigateRef;
function Harness() {
  const [params, update] = useLatestSearchParams();
  const navigate = useNavigate();
  navigateRef = navigate;
  location = useLocation();
  return (
    <div>
      <output data-testid="q">{params.get('q') ?? ''}|{params.get('view') ?? ''}</output>
      <button onClick={() => { update((p) => p.set('q', 'tree')); update((p) => p.set('view', 'code')); }}>two writes</button>
      <button onClick={() => navigate('/?q=elsewhere')}>external</button>
      <button onClick={() => { update((p) => p.set('q', 'b')); update((p) => p.set('q', 'a')); }}>round trip</button>
    </div>
  );
}

const mount = (path = '/') => render(
  <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <Routes><Route path="/" element={<Harness />} /></Routes>
  </MemoryRouter>
);

describe('useLatestSearchParams', () => {
  it('shows a write at once and merges a second write in the same tick onto it', () => {
    mount('/?step=3');
    fireEvent.click(screen.getByText('two writes'));
    expect(screen.getByTestId('q')).toHaveTextContent('tree|code');
    const params = new URLSearchParams(location.search);
    expect([params.get('q'), params.get('view'), params.get('step')]).toEqual(['tree', 'code', '3']);
  });

  it('adopts a navigation it did not make', () => {
    mount('/?q=mine');
    expect(screen.getByTestId('q')).toHaveTextContent('mine|');
    fireEvent.click(screen.getByText('external'));
    expect(screen.getByTestId('q')).toHaveTextContent('elsewhere|');
  });

  it('does not treat its own delivered writes as external, however many land', () => {
    mount('/');
    for (let i = 0; i < 5; i += 1) fireEvent.click(screen.getByText('round trip'));
    expect(screen.getByTestId('q')).toHaveTextContent('a|');
    expect(location.search).toBe('?q=a');
  });

  it('adopts an external navigation even when it matches an earlier write of its own', () => {
    // The review's sequence: at ?q=a, write q=b then q=a in one tick, then navigate
    // elsewhere to ?q=b. String equality mistook that navigation for the stale own write.
    mount('/?q=a');
    fireEvent.click(screen.getByText('round trip'));
    expect(screen.getByTestId('q')).toHaveTextContent('a|');
    act(() => navigateRef('/?q=b'));
    expect(new URLSearchParams(location.search).get('q')).toBe('b');
    expect(screen.getByTestId('q')).toHaveTextContent('b|');
  });
});
