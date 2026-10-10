import React from 'react';
import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import '@testing-library/jest-dom';
import StateInspector from './StateInspector';

describe('live Analysis structure guards', () => {
  it('marks a queue singleton as both front and back, without duplicating the item', () => {
    const step = { queueOrStackState: ['7'] };
    render(<StateInspector step={step} steps={[step]} dsType="Queue" />);
    const queue = screen.getByRole('region', { name: 'Queue contents' });
    expect(within(queue).getAllByRole('listitem')).toHaveLength(1);
    expect(within(queue).getByText('Front · Back')).toBeInTheDocument();
    expect(within(queue).getByText('7')).toBeInTheDocument();
  });

  it('retains run-level structures when they are empty at the selected step', () => {
    const step = { callStack: [], queueOrStackState: [] };
    render(<StateInspector step={step} steps={[step, { callStack: ['solve(2)'], queueOrStackState: ['7'] }]}
      dsType="Graph" />);
    for (const name of ['Call stack', 'Container contents']) {
      expect(within(screen.getByRole('region', { name })).getByText('Empty at this step.')).toBeInTheDocument();
    }
    expect(screen.queryByText('solve(2)')).not.toBeInTheDocument();
    expect(screen.queryByText('7')).not.toBeInTheDocument();
    expect(screen.queryByText('Front')).not.toBeInTheDocument();
    expect(screen.queryByText('Top')).not.toBeInTheDocument();
  });

  it('does not invent unused structures or missing complexity', () => {
    const step = { variables: { value: null }, callStack: [], queueOrStackState: [] };
    render(<StateInspector step={step} steps={[step]} dsType="Array" problem={{}} />);
    expect(screen.getByText('null')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Call stack' })).not.toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Container contents' })).not.toBeInTheDocument();
    expect(screen.getAllByText('unavailable')).toHaveLength(2);
    expect(screen.queryByText('O(1)')).not.toBeInTheDocument();
  });
});
