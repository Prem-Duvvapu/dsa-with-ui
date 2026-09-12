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
