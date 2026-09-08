import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import '@testing-library/jest-dom';
import { MemoryRouter, useParams } from 'react-router-dom';
import AppRouter from './AppRouter.jsx';

vi.mock('./App.jsx', () => ({
  default: () => {
    const { id } = useParams();
    return <div data-testid="app-route-target">Active Problem: {id}</div>;
  }
}));

describe('AppRouter', () => {
  const routerFuture = { v7_startTransition: true, v7_relativeSplatPath: true };

  it('renders problem view when navigating to /problem/:id', () => {
    render(
      <MemoryRouter initialEntries={['/problem/kadane-algo']} future={routerFuture}>
        <AppRouter />
      </MemoryRouter>
    );

    const target = screen.getByTestId('app-route-target');
    expect(target).toHaveTextContent('Active Problem: kadane-algo');
  });

  it('redirects root path / to /problem/two-sum', () => {
    render(
      <MemoryRouter initialEntries={['/']} future={routerFuture}>
        <AppRouter />
      </MemoryRouter>
    );

    const target = screen.getByTestId('app-route-target');
    expect(target).toHaveTextContent('Active Problem: two-sum');
  });

  it('redirects unmatched wildcard route * to / then to /problem/two-sum', () => {
    render(
      <MemoryRouter initialEntries={['/some/unknown/deep/path']} future={routerFuture}>
        <AppRouter />
      </MemoryRouter>
    );

    const target = screen.getByTestId('app-route-target');
    expect(target).toHaveTextContent('Active Problem: two-sum');
  });
});
