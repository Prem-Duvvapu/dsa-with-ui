import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import '@testing-library/jest-dom';
import { MemoryRouter, useParams } from 'react-router-dom';
import AppRouter from './AppRouter.jsx';

// Routing is isolated here; catalogue lifecycle is covered by library integration tests.
vi.mock('./catalog/CatalogProvider', () => ({ CatalogProvider: ({ children }) => children }));

vi.mock('./App.jsx', () => ({
  default: () => {
    const { id } = useParams();
    return <div data-testid="app-route-target">Active Problem: {id}</div>;
  }
}));

vi.mock('./components/Dashboard.jsx', () => ({
  default: () => <div data-testid="dashboard-route-target">Dashboard</div>
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

  it('renders the dashboard at the root path /', () => {
    render(
      <MemoryRouter initialEntries={['/']} future={routerFuture}>
        <AppRouter />
      </MemoryRouter>
    );

    expect(screen.getByTestId('dashboard-route-target')).toBeInTheDocument();
  });

  it('redirects unmatched wildcard route * to / (the dashboard)', () => {
    render(
      <MemoryRouter initialEntries={['/some/unknown/deep/path']} future={routerFuture}>
        <AppRouter />
      </MemoryRouter>
    );

    expect(screen.getByTestId('dashboard-route-target')).toBeInTheDocument();
  });
});
