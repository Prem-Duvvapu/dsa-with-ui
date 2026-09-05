import React from 'react';
import { render } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import LinkedListCanvas from './LinkedListCanvas';

describe('LinkedListCanvas', () => {
  it('draws the inline next arrow between every pair, exactly as before, when nextId always matches the adjacent box (every pre-existing tracer)', () => {
    const listState = [
      { id: 0, val: '1', nextId: 1, prevId: null, state: 'curr' },
      { id: 1, val: '2', nextId: 2, prevId: null, state: 'default' },
      { id: 2, val: '3', nextId: null, prevId: null, state: 'default' },
    ];
    const { container } = render(<LinkedListCanvas step={{ listState }} />);

    // Two adjacent pairs, so two inline arrows and no suppressed-arrow gaps.
    expect(container.querySelectorAll('.lucide-arrow-right')).toHaveLength(2);
    expect(container.querySelectorAll('path[stroke="#a855f7"]')).toHaveLength(0);
    expect(container.querySelectorAll('path[stroke="#f97316"]')).toHaveLength(0);
  });

  it('suppresses the inline next arrow between an adjacent pair whose nextId does not actually match', () => {
    const listState = [
      { id: 0, val: '1', nextId: 5, prevId: null, state: 'default' }, // true next is node 5, not the adjacent box
      { id: 1, val: '2', nextId: null, prevId: null, state: 'default' },
    ];
    const { container } = render(<LinkedListCanvas step={{ listState }} />);

    expect(container.querySelectorAll('.lucide-arrow-right')).toHaveLength(0);
  });

  it('renders a dashed childId edge without disturbing the next-arrow rendering', () => {
    const listState = [
      { id: 0, val: '1', nextId: 1, prevId: null, childId: 2, state: 'curr' },
      { id: 1, val: '2', nextId: null, prevId: null, state: 'default' },
      { id: 2, val: '9', nextId: null, prevId: null, state: 'default' },
    ];
    const { container, getByText } = render(<LinkedListCanvas step={{ listState }} />);

    expect(container.querySelectorAll('.lucide-arrow-right')).toHaveLength(1);
    expect(container.querySelectorAll('path[stroke="#a855f7"]')).toHaveLength(1);
    expect(getByText('child')).toBeInTheDocument();
  });

  it('renders a distinctly styled randomId edge alongside a childId edge', () => {
    const listState = [
      { id: 0, val: '7', nextId: 1, prevId: null, randomId: null, state: 'default' },
      { id: 1, val: '13', nextId: 2, prevId: null, randomId: 0, state: 'curr' },
      { id: 2, val: '11', nextId: null, prevId: null, randomId: 1, state: 'default' },
    ];
    const { container, getByText } = render(<LinkedListCanvas step={{ listState }} />);

    expect(container.querySelectorAll('path[stroke="#f97316"]')).toHaveLength(2);
    expect(container.querySelectorAll('path[stroke="#a855f7"]')).toHaveLength(0);
    expect(getByText('random')).toBeInTheDocument();
  });

  it('falls back to the problem default list and does not throw with no step at all', () => {
    const { container } = render(<LinkedListCanvas problem={{ defaultList: [] }} />);
    expect(container.querySelectorAll('.lucide-arrow-right')).toHaveLength(0);
  });
});
