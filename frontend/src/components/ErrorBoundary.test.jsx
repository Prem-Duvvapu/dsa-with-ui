import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, afterEach } from 'vitest';
import '@testing-library/jest-dom';
import ErrorBoundary from './ErrorBoundary';

function Bomb({ armed }) {
  if (armed) throw new Error('canvas exploded');
  return <div>fine</div>;
}

describe('ErrorBoundary', () => {
  afterEach(() => vi.restoreAllMocks());

  it('renders children normally when nothing throws', () => {
    render(<ErrorBoundary><Bomb armed={false} /></ErrorBoundary>);
    expect(screen.getByText('fine')).toBeInTheDocument();
  });

  it('catches a thrown error and shows a fallback instead of blanking the tree', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    render(<ErrorBoundary><Bomb armed /></ErrorBoundary>);
    expect(screen.getByText('This visualization hit an error.')).toBeInTheDocument();
    expect(screen.getByText('canvas exploded')).toBeInTheDocument();
  });

  it('"Try again" clears the error and re-renders children', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    const { rerender } = render(<ErrorBoundary><Bomb armed /></ErrorBoundary>);
    expect(screen.getByText('This visualization hit an error.')).toBeInTheDocument();

    // Fix the underlying condition, then let the boundary retry.
    rerender(<ErrorBoundary><Bomb armed={false} /></ErrorBoundary>);
    fireEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(screen.getByText('fine')).toBeInTheDocument();
  });

  it('a resetKey change after a crash clears the error automatically', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    const { rerender } = render(
      <ErrorBoundary resetKey="problem-a"><Bomb armed /></ErrorBoundary>
    );
    expect(screen.getByText('This visualization hit an error.')).toBeInTheDocument();

    rerender(<ErrorBoundary resetKey="problem-b"><Bomb armed={false} /></ErrorBoundary>);
    expect(screen.getByText('fine')).toBeInTheDocument();
  });

  it('does not reset when resetKey is unchanged, even if children would now succeed', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    const { rerender } = render(
      <ErrorBoundary resetKey="problem-a"><Bomb armed /></ErrorBoundary>
    );
    rerender(<ErrorBoundary resetKey="problem-a"><Bomb armed={false} /></ErrorBoundary>);
    // Still showing the fallback — the boundary itself doesn't re-render its children
    // once it has caught, by React's own design; only a key/state change does.
    expect(screen.getByText('This visualization hit an error.')).toBeInTheDocument();
  });
});
