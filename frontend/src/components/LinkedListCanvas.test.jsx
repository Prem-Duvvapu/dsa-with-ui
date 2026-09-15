import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import LinkedListCanvas from './LinkedListCanvas';

// Edge kinds are identified by their stroke. That used to be a literal hex here and in the
// component, which meant the two had to be edited in lockstep; both now name the role token,
// so the colour can change per theme without touching either.
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
    expect(container.querySelectorAll('path[stroke="var(--role-link-child)"]')).toHaveLength(0);
    expect(container.querySelectorAll('path[stroke="var(--role-link-random)"]')).toHaveLength(0);
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
    expect(container.querySelectorAll('path[stroke="var(--role-link-child)"]')).toHaveLength(1);
    expect(getByText('child')).toBeInTheDocument();
  });

  it('renders a distinctly styled randomId edge alongside a childId edge', () => {
    const listState = [
      { id: 0, val: '7', nextId: 1, prevId: null, randomId: null, state: 'default' },
      { id: 1, val: '13', nextId: 2, prevId: null, randomId: 0, state: 'curr' },
      { id: 2, val: '11', nextId: null, prevId: null, randomId: 1, state: 'default' },
    ];
    const { container, getByText } = render(<LinkedListCanvas step={{ listState }} />);

    expect(container.querySelectorAll('path[stroke="var(--role-link-random)"]')).toHaveLength(2);
    expect(container.querySelectorAll('path[stroke="var(--role-link-child)"]')).toHaveLength(0);
    expect(getByText('random')).toBeInTheDocument();
  });

  it('falls back to the problem default list and does not throw with no step at all', () => {
    const { container } = render(<LinkedListCanvas problem={{ defaultList: [] }} />);
    expect(container.querySelectorAll('.lucide-arrow-right')).toHaveLength(0);
  });

  it('keeps what the run emitted when a later step does not restate it', () => {
    // Measured app-wide: 1506 steps across 142 of 232 problems fell through to the
    // catalogue default mid-run, drawing the catalogue's data over the caller's input.
    const steps = [{ listState: [{ id: 1, val: 7, next: null }] }, { description: 'narration only' }];
    render(<LinkedListCanvas problem={{ defaultList: [{ id: 1, val: 99, next: null }] }} steps={steps} currentStepIndex={1} currentStep={steps[1]} />);
    expect(screen.getByText('7')).toBeInTheDocument(); expect(screen.queryByText('99')).not.toBeInTheDocument();
  });

  describe('doubly linked lists', () => {
    // Seven problems emit prevId - intro-doubly-ll, reverse-dll, insert-head-dll and the
    // rest - and the canvas showed only `next`. Reversing a DLL means swapping next AND
    // prev on every node, so half the operation was invisible.
    const dll = [
      { id: 0, val: '1', nextId: 1, prevId: null, state: 'default' },
      { id: 1, val: '2', nextId: 2, prevId: 0, state: 'current' },
      { id: 2, val: '3', nextId: null, prevId: 1, state: 'default' }
    ];

    it('shows each node\'s prev pointer, not only its next', () => {
      render(<LinkedListCanvas currentStep={{ listState: dll }} />);
      expect(screen.getByText('prev -> [0]')).toBeInTheDocument();
      expect(screen.getByText('next -> [1]')).toBeInTheDocument();
    });

    it('names the null ends of the chain', () => {
      render(<LinkedListCanvas currentStep={{ listState: dll }} />);
      expect(screen.getByText('prev -> NULL')).toBeInTheDocument();
      expect(screen.getByText('next -> NULL')).toBeInTheDocument();
    });

    it('leaves a singly linked list unchanged', () => {
      // No prevId anywhere means no prev row - a singly linked list should not grow a
      // column of NULLs it never had.
      const sll = [{ id: 0, val: '1', nextId: 1, state: 'default' },
                   { id: 1, val: '2', nextId: null, state: 'default' }];
      render(<LinkedListCanvas currentStep={{ listState: sll }} />);
      expect(screen.queryByText(/^prev ->/)).not.toBeInTheDocument();
    });
  });
});
