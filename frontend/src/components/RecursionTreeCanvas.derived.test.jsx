import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import RecursionTreeCanvas from './RecursionTreeCanvas';

const frame = (...f) => ({ callStack: f });

describe('RecursionTreeCanvas with a derived tree', () => {
  it('draws the tree a backtracking run explored, from its call stacks', () => {
    // None of the 25 Recursion & Backtracking tracers emits treeNodes; every one emits
    // callStack. The canvas rebuilds the tree rather than requiring 25 tracer changes.
    const steps = [frame('s(0)'), frame('s(0)', 's(1)'), frame('s(0)'), frame('s(0)', 's(2)')];
    render(<RecursionTreeCanvas steps={steps} currentStepIndex={3} currentStep={steps[3]} />);
    const tree = screen.getByTestId('derived-recursion-tree');
    expect(tree).toBeInTheDocument();
    // Root plus two siblings: the branch tried, abandoned, and the next one taken.
    expect(tree.querySelectorAll('g[data-state]')).toHaveLength(3);
  });

  it('marks where the algorithm is now, and what it has already left', () => {
    const steps = [frame('a'), frame('a', 'b'), frame('a')];
    const { container } = render(
      <RecursionTreeCanvas steps={steps} currentStepIndex={2} currentStep={steps[2]} />
    );
    expect(container.querySelectorAll('g[data-state="done"]')).toHaveLength(1);
    expect(container.querySelectorAll('g[data-state="current"]')).toHaveLength(1);
  });

  it('grows only as far as the step being shown', () => {
    const steps = [frame('a'), frame('a', 'b'), frame('a'), frame('a', 'c')];
    const { container, rerender } = render(
      <RecursionTreeCanvas steps={steps} currentStepIndex={1} currentStep={steps[1]} />
    );
    expect(container.querySelectorAll('g[data-state]')).toHaveLength(2);
    rerender(<RecursionTreeCanvas steps={steps} currentStepIndex={3} currentStep={steps[3]} />);
    expect(container.querySelectorAll('g[data-state]')).toHaveLength(3);
  });

  it('leaves a tracer that emits its own tree alone', () => {
    // The four Learn the Basics problems build real treeNodes. Those keep their own layout.
    const step = {
      treeNodes: [{ id: 1, label: 'fib(3)', x: 100, y: 40 }],
      nodeStates: { 1: 'visited' }
    };
    render(<RecursionTreeCanvas currentStep={step} steps={[step]} currentStepIndex={0} />);
    expect(screen.queryByTestId('derived-recursion-tree')).not.toBeInTheDocument();
  });

  it('writes the arguments, not the function name that every node shares', () => {
    // The bug this replaces: truncating from the front kept `backtrack(` - identical on
    // every node - and cut the arguments, which are the only distinguishing part. Every
    // node in the permutations tree read the same.
    const steps = [frame('backtrack(idx=0)'), frame('backtrack(idx=0)', 'backtrack(idx=1)')];
    render(<RecursionTreeCanvas steps={steps} currentStepIndex={1} currentStep={steps[1]} />);
    expect(screen.getByText('idx=0')).toBeInTheDocument();
    expect(screen.getByText('idx=1')).toBeInTheDocument();
    expect(screen.queryByText(/^backtrack\($/)).not.toBeInTheDocument();
  });

  it('keeps the whole frame available on hover', () => {
    const steps = [frame('find(idx=0, target=7)')];
    const { container } = render(
      <RecursionTreeCanvas steps={steps} currentStepIndex={0} currentStep={steps[0]} />
    );
    expect(container.querySelector('title').textContent).toBe('find(idx=0, target=7)');
  });

  it('falls back to the whole frame when there are no arguments to show', () => {
    const steps = [frame('solve')];
    const { container } = render(
      <RecursionTreeCanvas steps={steps} currentStepIndex={0} currentStep={steps[0]} />
    );
    // The <title> carries it too, so assert on the drawn text specifically.
    expect(container.querySelector('text').textContent).toContain('solve');
  });
});
