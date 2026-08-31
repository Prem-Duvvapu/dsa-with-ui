import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import GraphCanvas from './GraphCanvas';

const problem = {
  defaultGraphNodes: [
    { id: 90, label: 'default-a', x: 20, y: 30 },
    { id: 91, label: 'default-b', x: 80, y: 30 }
  ],
  defaultGraphEdges: [
    { from: 90, to: 91, directed: true, weight: 99 }
  ]
};

describe('GraphCanvas', () => {
  it('renders trace topology instead of stale problem defaults and keeps trace highlighting', () => {
    const currentStep = {
      graphNodes: [
        { id: 1, label: 'trace-a', x: 40, y: 50 },
        { id: 2, label: 'trace-b', x: 140, y: 50 },
        { id: 3, label: 'trace-c', x: 240, y: 120 }
      ],
      graphEdges: [
        { from: 1, to: 2, directed: true, weight: 7 },
        { from: 2, to: 3, directed: false }
      ],
      nodeStates: { 1: 'visiting', 2: 'done' },
      activeEdges: ['1-2']
    };

    const { container } = render(
      <GraphCanvas problem={problem} currentStep={currentStep} />
    );

    expect(screen.getByText('trace-a')).toBeTruthy();
    expect(screen.getByText('trace-b')).toBeTruthy();
    expect(screen.getByText('trace-c')).toBeTruthy();
    expect(screen.queryByText('default-a')).toBeNull();
    expect(screen.queryByText('default-b')).toBeNull();
    expect(screen.getByText('7')).toBeTruthy();
    expect(screen.queryByText('99')).toBeNull();

    const edges = container.querySelectorAll('svg line');
    expect(edges).toHaveLength(2);
    expect(edges[0].getAttribute('x1')).toBe('40');
    expect(edges[0].getAttribute('x2')).toBe('140');
    expect(edges[0].getAttribute('stroke')).toBe('var(--probe)');
    expect(edges[0].getAttribute('marker-end')).toBe('url(#arrowhead-active)');

    const visitingNode = screen.getByText('trace-a').closest('g').querySelector('circle');
    const doneNode = screen.getByText('trace-b').closest('g').querySelector('circle');
    expect(visitingNode.getAttribute('fill')).toBe('var(--probe)');
    expect(doneNode.getAttribute('fill')).toBe('var(--settled)');
  });

  it('keeps an explicitly edgeless trace graph instead of mixing in default edges', () => {
    const { container } = render(
      <GraphCanvas
        problem={problem}
        currentStep={{
          graphNodes: [{ id: 1, label: 'only-trace-node', x: 100, y: 100 }],
          graphEdges: []
        }}
      />
    );

    expect(screen.getByText('only-trace-node')).toBeTruthy();
    expect(container.querySelectorAll('svg line')).toHaveLength(0);
  });

  it('renders a zero edge weight instead of treating it as absent', () => {
    render(
      <GraphCanvas
        problem={{}}
        currentStep={{
          graphNodes: [
            { id: 1, label: 'source', x: 40, y: 50 },
            { id: 2, label: 'target', x: 140, y: 50 }
          ],
          graphEdges: [{ from: 1, to: 2, directed: false, weight: 0 }]
        }}
      />
    );

    expect(screen.getByText('0', { selector: 'text' })).toBeTruthy();
  });

  it('falls back for absent or empty step topology and handles no topology safely', () => {
    const { rerender } = render(
      <GraphCanvas problem={problem} currentStep={{ graphNodes: [], graphEdges: [] }} />
    );
    expect(screen.getByText('default-a')).toBeTruthy();
    expect(screen.getByText('default-b')).toBeTruthy();

    rerender(<GraphCanvas problem={{}} currentStep={{}} />);
    expect(screen.getByText('No graph data available')).toBeTruthy();
  });
});
