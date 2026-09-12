import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import HeapCanvas from './HeapCanvas';

describe('HeapCanvas', () => {
  const arrayStep = {
    arrayState: [
      { value: 1, state: 'current' }, { value: 3, state: 'default' },
      { value: 8, state: 'default' }, { value: 5, state: 'default' }
    ]
  };

  it('shows the heap as a tree AND as the array it is stored in', () => {
    // The two views are the same structure. A canvas showing one hides the idea.
    render(<HeapCanvas currentStep={arrayStep} />);
    expect(screen.getByTestId('heap-canvas')).toBeInTheDocument();
    expect(screen.getByTestId('heap-array').children).toHaveLength(4);
    expect(screen.getByRole('img', { name: /Heap of 4 elements/ })).toBeInTheDocument();
  });

  it('spells out the arithmetic that joins the two views', () => {
    render(<HeapCanvas currentStep={arrayStep} />);
    expect(screen.getByText(/2i\+1/)).toBeInTheDocument();
    expect(screen.getByText(/\(i-1\)\/2/)).toBeInTheDocument();
  });

  it('derives the tree from an array-only trace', () => {
    // Eleven of these problems use a heap as a tool and emit only the array. The tree is
    // pure index arithmetic, so it costs no tracer change to show it.
    const { container } = render(<HeapCanvas currentStep={arrayStep} />);
    // Four nodes, and three edges: 0->1, 0->2, 1->3.
    expect(container.querySelectorAll('circle')).toHaveLength(4);
    expect(container.querySelectorAll('line')).toHaveLength(3);
  });

  it('derives the array from a tree-only trace, in level order', () => {
    // The five heap-mechanics problems emit treeNodes and no array at all.
    render(<HeapCanvas currentStep={{
      treeNodes: [
        { id: 0, val: '2', leftId: 1, rightId: 2, state: 'default' },
        { id: 1, val: '4', leftId: null, rightId: null, state: 'current' },
        { id: 2, val: '9', leftId: null, rightId: null, state: 'default' }
      ]
    }} />);
    const slots = [...screen.getByTestId('heap-array').children].map((el) => el.textContent);
    expect(slots).toEqual(['20', '41', '92']);   // value followed by its index
  });

  it('names each slot with its parent, since that is the relationship', () => {
    render(<HeapCanvas currentStep={arrayStep} />);
    expect(screen.getByTitle('index 0 (root)')).toBeInTheDocument();
    expect(screen.getByTitle('index 3, parent 1')).toBeInTheDocument();
  });

  it('says so when a step carries no heap at all', () => {
    render(<HeapCanvas currentStep={{ variables: {} }} />);
    expect(screen.getByRole('status').textContent).toContain('No heap contents');
  });
});
