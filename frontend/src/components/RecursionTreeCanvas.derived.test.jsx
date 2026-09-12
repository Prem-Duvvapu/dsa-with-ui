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
});
