import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import '@testing-library/jest-dom';
import MemoryComplexityCard from './MemoryComplexityCard';

const PROBLEM = { id: 'two-sum', complexity: { timeComplexity: 'O(N)', spaceComplexity: 'O(1)' } };

describe('MemoryComplexityCard', () => {
  it('desktop: internal Memory/Complexity toggle works with no initialTab prop', () => {
    render(<MemoryComplexityCard currentStep={null} problem={PROBLEM} />);
    expect(screen.queryByText('Time Complexity')).not.toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Memory' })).toHaveAttribute('aria-selected', 'true');

    fireEvent.click(screen.getByText('Complexity'));
    expect(screen.getByText('Time Complexity')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Complexity' })).toHaveAttribute('aria-selected', 'true');
  });

  it('mobile: follows initialTab on the first render', () => {
    render(<MemoryComplexityCard currentStep={null} problem={PROBLEM} initialTab="complexity" />);
    expect(screen.getByText('Time Complexity')).toBeInTheDocument();
  });

  it('mobile: follows initialTab when it changes after mount, not just on first mount', () => {
    // This is the bug: useState(initialTab) only reads the prop once. Clicking the
    // outer app's tab bar from Memory to Complexity re-renders this component with a
    // new initialTab, and it must actually switch — not keep showing Memory.
    const { rerender } = render(
      <MemoryComplexityCard currentStep={null} problem={PROBLEM} initialTab="memory" />
    );
    expect(screen.queryByText('Time Complexity')).not.toBeInTheDocument();

    rerender(<MemoryComplexityCard currentStep={null} problem={PROBLEM} initialTab="complexity" />);
    expect(screen.getByText('Time Complexity')).toBeInTheDocument();
  });

  it('shows recursion frames separately from the algorithm data structure', () => {
    render(
      <MemoryComplexityCard
        problem={{ ...PROBLEM, dsType: 'Stack' }}
        currentStep={{
          dsType: 'Stack',
          variables: { index: 2 },
          callStack: ['solve(0)', 'solve(1)'],
          queueOrStackState: ['4', '7']
        }}
      />
    );

    expect(screen.getByRole('heading', { name: 'Call stack' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Stack contents' })).toBeInTheDocument();
    expect(screen.getByText('solve(1)')).toHaveAccessibleName('Current frame: solve(1)');
    expect(screen.getByText('7')).toHaveAccessibleName('Top: 7');
  });

  it('does not render empty memory sections as active allocations', () => {
    render(
      <MemoryComplexityCard
        problem={PROBLEM}
        currentStep={{ variables: {}, callStack: [], queueOrStackState: [] }}
      />
    );

    expect(screen.getByText('No active memory state')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Call stack' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /contents/ })).not.toBeInTheDocument();
  });

  it('labels a one-item queue as both its front and back', () => {
    render(
      <MemoryComplexityCard
        problem={{ ...PROBLEM, dsType: 'Queue' }}
        currentStep={{ dsType: 'Queue', queueOrStackState: ['only'] }}
      />
    );

    expect(screen.getByRole('heading', { name: 'Queue contents' })).toBeInTheDocument();
    expect(screen.getByText('only')).toHaveAccessibleName('Front / back: only');
  });

  it('does not invent generic complexity when problem detail is unavailable', () => {
    render(<MemoryComplexityCard currentStep={null} problem={{ id: 'missing-detail' }} />);

    fireEvent.click(screen.getByRole('tab', { name: 'Complexity' }));

    expect(screen.queryByText('O(N)')).not.toBeInTheDocument();
    expect(screen.queryByText('O(1)')).not.toBeInTheDocument();
    expect(screen.getByText('Time explanation unavailable.')).toBeInTheDocument();
    expect(screen.getByText('Space explanation unavailable.')).toBeInTheDocument();
  });
});
