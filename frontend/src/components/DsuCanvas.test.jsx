import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import DsuCanvas from './DsuCanvas';

const FULL_STEP = {
  variables: {
    'parent[]': '[0, 1, 1, 3, 4]',
    'rank[]': '[0, 1, 0, 0, 0]',
    'Disjoint Sets': '{1, 2}, {3}, {4}',
    Operation: 'union(1, 2)'
  }
};

describe('DsuCanvas', () => {
  it('renders the DSU carried by the step', () => {
    render(<DsuCanvas step={FULL_STEP} />);
    expect(screen.getByText('union(1, 2)')).toBeInTheDocument();
    expect(screen.getByText('{1, 2}, {3}, {4}')).toBeInTheDocument();
  });

  // The regression guard. This canvas used to default parent[]/rank[] to a hardcoded
  // 7-element DSU, so a renamed variable key drew a plausible, entirely fabricated
  // structure and nothing failed. A missing payload must be visible, not invented.
  it('keeps the two semantic hues for state, not for row labels', () => {
    // Bench spends amber on "happening right now" and green on "finished and proven".
    // This canvas used both as decoration: the "Connected Components" heading and the
    // parent[i] label in probe amber, the rank[i] label in settled green, and every rank
    // CELL painted green unconditionally - so every rank value read as resolved whether
    // or not anything had resolved.
    const { container } = render(
      <DsuCanvas currentStep={{ variables: { 'parent[]': '[0, 0, 2]', 'rank[]': '[1, 0, 0]' } }} />
    );

    // The LABELS specifically - a DSU root is still drawn in probe amber, and should be,
    // because "this element is its own parent" is a state and not decoration.
    for (const text of ['parent[i]', 'rank[i]', 'Connected Components']) {
      const label = screen.getByText(text);
      expect(label.getAttribute('style') ?? '').not.toMatch(/var\(--(probe|settled)\)/);
    }
  });

  it('says so when the step carries no DSU state, instead of inventing one', () => {
    render(<DsuCanvas step={{ variables: {} }} />);
    expect(screen.getByTestId('dsu-state-unavailable')).toBeInTheDocument();
  });

  it('does not invent a DSU when only one of parent[]/rank[] is present', () => {
    render(<DsuCanvas step={{ variables: { 'parent[]': '[0, 1, 2]' } }} />);
    expect(screen.getByTestId('dsu-state-unavailable')).toBeInTheDocument();
  });

  it('renders nothing fabricated for a step with no variables at all', () => {
    render(<DsuCanvas step={{}} />);
    expect(screen.getByTestId('dsu-state-unavailable')).toBeInTheDocument();
    expect(screen.queryByText('Initialize DSU(7)')).not.toBeInTheDocument();
  });
});
