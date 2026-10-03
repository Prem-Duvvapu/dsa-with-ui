import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import DsuCanvas from './DsuCanvas';

// The traced DSU is the owner's: union by SIZE, elements numbered from 0, no padding slot.
const FULL_STEP = {
  variables: {
    'parent[]': '[0, 1, 1, 3]',
    'size[]': '[1, 2, 1, 1]',
    'Disjoint Sets': '{0}, {1, 2}, {3}',
    Operation: 'unionBySize(1, 2)'
  }
};

describe('DsuCanvas', () => {
  it('renders the DSU carried by the step', () => {
    render(<DsuCanvas step={FULL_STEP} />);
    expect(screen.getByText('unionBySize(1, 2)')).toBeInTheDocument();
    expect(screen.getByText('{0}, {1, 2}, {3}')).toBeInTheDocument();
  });

  it('draws every element from 0, exactly as many as the arrays hold, with a size[] row', () => {
    // It used to skip index 0 as padding and pad out to 7 columns, which drew elements the
    // DSU does not have and hid element 0, which it does.
    render(<DsuCanvas step={FULL_STEP} />);
    const header = screen.getByText('Element i').parentElement;
    expect([...header.children].slice(1).map((c) => c.textContent)).toEqual(['0', '1', '2', '3']);
    const sizeRow = screen.getByText('size[i]').parentElement;
    expect([...sizeRow.children].slice(1).map((c) => c.textContent)).toEqual(['1', '2', '1', '1']);
    expect(screen.queryByText('rank[i]')).not.toBeInTheDocument();
  });

  // The regression guard. This canvas used to default parent[]/rank[] to a hardcoded
  // 7-element DSU, so a renamed variable key drew a plausible, entirely fabricated
  // structure and nothing failed. A missing payload must be visible, not invented.
  it('keeps the two semantic hues for state, not for row labels', () => {
    // Bench spends amber on "happening right now" and green on "finished and proven".
    // This canvas used both as decoration: the "Connected Components" heading and the
    // parent[i] label in probe amber, the rank[i] (now size[i]) label in settled green, and
    // every such CELL painted green unconditionally - so every value read as resolved
    // whether or not anything had resolved.
    const { container } = render(
      <DsuCanvas currentStep={{ variables: { 'parent[]': '[0, 0, 2]', 'size[]': '[2, 1, 1]' } }} />
    );

    // The LABELS specifically - a DSU root is still drawn in probe amber, and should be,
    // because "this element is its own parent" is a state and not decoration.
    for (const text of ['parent[i]', 'size[i]', 'Connected Components']) {
      const label = screen.getByText(text);
      expect(label.getAttribute('style') ?? '').not.toMatch(/var\(--(probe|settled)\)/);
    }
  });

  it('says so when the step carries no DSU state, instead of inventing one', () => {
    render(<DsuCanvas step={{ variables: {} }} />);
    expect(screen.getByTestId('dsu-state-unavailable')).toBeInTheDocument();
  });

  it('does not invent a DSU when only one of parent[]/size[] is present', () => {
    render(<DsuCanvas step={{ variables: { 'parent[]': '[0, 1, 2]' } }} />);
    expect(screen.getByTestId('dsu-state-unavailable')).toBeInTheDocument();
  });

  it('renders nothing fabricated for a step with no variables at all', () => {
    render(<DsuCanvas step={{}} />);
    expect(screen.getByTestId('dsu-state-unavailable')).toBeInTheDocument();
    expect(screen.queryByText('Initialize DSU(7)')).not.toBeInTheDocument();
  });
});
