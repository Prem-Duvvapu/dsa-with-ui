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
});
