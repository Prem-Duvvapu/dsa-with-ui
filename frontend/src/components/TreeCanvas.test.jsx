import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import '@testing-library/jest-dom';
import TreeCanvas from './TreeCanvas';

describe('TreeCanvas', () => {
  it('renders the trace topology instead of the catalogue default topology', () => {
    const problem = {
      defaultTreeNodes: [
        { id: 1, val: 'default-root', x: 180, y: 40, leftId: 2, rightId: 3, state: 'unvisited' },
        { id: 2, val: 'default-left', x: 100, y: 120, leftId: null, rightId: null, state: 'unvisited' },
        { id: 3, val: 'default-right', x: 260, y: 120, leftId: null, rightId: null, state: 'unvisited' }
      ]
    };
    const currentStep = {
      treeNodes: [
        { id: 40, val: 'trace-root', x: 180, y: 55, leftId: null, rightId: 90, state: 'visiting' },
        { id: 90, val: 'trace-right', x: 600, y: 145, leftId: null, rightId: null, state: 'visited' }
      ]
    };

    render(<TreeCanvas problem={problem} currentStep={currentStep} />);

    const traceRoot = screen.getByText('trace-root');
    const traceRootCircle = traceRoot.parentElement.querySelector('circle');
    expect(traceRoot).toBeInTheDocument();
    expect(traceRootCircle).toHaveAttribute('r', '21');
    expect(traceRootCircle).toHaveAttribute('fill', 'var(--state-current)');
    expect(traceRoot.closest('svg')).toHaveAttribute('viewBox', '0 0 624 300');
    expect(screen.getByText('trace-right')).toBeInTheDocument();
    expect(screen.queryByText('default-root')).not.toBeInTheDocument();
    expect(screen.queryByText('default-left')).not.toBeInTheDocument();
    expect(screen.queryByText('default-right')).not.toBeInTheDocument();
  });

  describe('a step that does not restate the tree', () => {
    const emitted = [{ id: 1, val: '9', x: 100, y: 40, leftId: 2, rightId: null },
                     { id: 2, val: '8', x: 60, y: 100, leftId: null, rightId: null }];
    const problem = { defaultTreeNodes: [{ id: 1, val: '1', x: 100, y: 40 }] };

    it('keeps the tree the run emitted, not the catalogue default', () => {
      // Measured across Binary Trees and BST: 259 steps in 48 of 54 problems carried no
      // treeNodes and fell back to problem.defaultTreeNodes. Run a tree of [9,8,7,6] and
      // half the steps drew 1,2,3,4,5 - the same defect as IntervalCanvas (RCA-025).
      const steps = [{ treeNodes: emitted }, { description: 'height of the left subtree is 2' }];
      render(<TreeCanvas problem={problem} steps={steps} currentStepIndex={1} currentStep={steps[1]} />);
      expect(screen.getByText('9')).toBeInTheDocument();
      expect(screen.queryByText('1')).not.toBeInTheDocument();
    });

    it('still shows the catalogue default before any step has emitted a tree', () => {
      // Honest here: with no run yet, the default IS what a run would use.
      render(<TreeCanvas problem={problem} steps={[{}]} currentStepIndex={0} currentStep={{}} />);
      expect(screen.getByText('1')).toBeInTheDocument();
    });
  });
});
