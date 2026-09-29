import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import { MemoryRouter, Routes, Route, useLocation, useNavigate } from 'react-router-dom';
import useLatestSearchParams from './useLatestSearchParams';

let location;
function Harness() {
  const [params, update] = useLatestSearchParams();
  const navigate = useNavigate();
  location = useLocation();
  return (
    <div>
      <output data-testid="q">{params.get('q') ?? ''}|{params.get('view') ?? ''}</output>
      <button onClick={() => { update((p) => p.set('q', 'tree')); update((p) => p.set('view', 'code')); }}>two writes</button>
      <button onClick={() => navigate('/?q=elsewhere')}>external</button>
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

  it('does not treat its own delivered write as external', () => {
    mount('/');
    fireEvent.click(screen.getByText('two writes'));
    fireEvent.click(screen.getByText('two writes'));
    expect(screen.getByTestId('q')).toHaveTextContent('tree|code');
    expect(location.search).toBe('?q=tree&view=code');
  });
});
