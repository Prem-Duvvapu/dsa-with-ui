import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import GridCanvas, { GridCompanion } from './GridCanvas';

describe('GridCanvas', () => {
  it('shows a placeholder when there is no grid data', () => {
    render(<GridCanvas step={{}} />);
    expect(screen.getByText('No grid data available')).toBeInTheDocument();
  });

  it('renders every cell of the current step\'s grid', () => {
    render(<GridCanvas step={{ gridState: [[1, 0], [0, 1]] }} />);
    expect(screen.getAllByText('1')).toHaveLength(2);
    expect(screen.getAllByText('0')).toHaveLength(2);
  });

  it('falls back to the problem\'s default grid when the step carries none', () => {
    render(<GridCanvas step={{}} problem={{ defaultGrid: [[2]] }} />);
    expect(screen.getByText('2')).toBeInTheDocument();
  });
});

describe('GridCompanion', () => {
  it('shows an explicit empty state when the step carries no grid', () => {
    render(<GridCompanion step={{}} />);
    expect(screen.getByText('empty')).toBeInTheDocument();
  });

  it('renders inside a companion pane, titled and every cell present', () => {
    render(<GridCompanion step={{ gridState: [[1, 0], [1, 1]] }} title="Grid" />);

    expect(screen.getByLabelText('Grid')).toBeInTheDocument();
    expect(screen.getAllByText('1')).toHaveLength(3);
    expect(screen.getAllByText('0')).toHaveLength(1);
  });

  it('does not print per-cell coordinates the way the hero variant does', () => {
    // The companion pane is a fraction of the hero's width - the "(r,c)" subtitle that
    // fits under a 55px hero cell would overflow a 22px companion one.
    const { container: hero } = render(<GridCanvas step={{ gridState: [[1]] }} />);
    const { container: companion } = render(<GridCompanion step={{ gridState: [[1]] }} />);

    expect(hero.textContent).toContain('(0,0)');
    expect(companion.textContent).not.toContain('(0,0)');
  });
});
