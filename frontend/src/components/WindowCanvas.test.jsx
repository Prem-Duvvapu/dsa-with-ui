import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import WindowCanvas from './WindowCanvas';

const cells = (labels, insideFrom, insideTo) =>
  labels.map((label, i) => ({
    index: i,
    value: i,
    label,
    state: i >= insideFrom && i <= insideTo ? 'active' : 'default'
  }));

describe('WindowCanvas', () => {
  it('derives the window from the cells rather than from variable names', () => {
    // The twelve tracers disagree about naming - left/right, start/end, or just i - but
    // they all mark the cells inside the window with a non-default state. That is the one
    // signal every one of them carries.
    render(<WindowCanvas currentStep={{ arrayState: cells(['1', '2', '3', '2', '2'], 1, 3) }} />);
    expect(screen.getByTestId('window-bounds').textContent).toContain('window [1, 3]');
    expect(screen.getByTestId('window-bounds').textContent).toContain('3 wide');
  });

  it('marks which cells are inside and leaves the rest on screen', () => {
    // Dimmed, not removed: seeing what the window has moved past is how you understand
    // that it moved at all.
    const { container } = render(
      <WindowCanvas currentStep={{ arrayState: cells(['a', 'b', 'c', 'd'], 1, 2) }} />
    );
    expect(container.querySelectorAll('[data-inside="true"]')).toHaveLength(2);
    expect(screen.getByText('a')).toBeInTheDocument();
    expect(screen.getByText('d')).toBeInTheDocument();
  });

  it('renders characters as readily as numbers', () => {
    // Half of these problems are string windows. One canvas serves both because the cells
    // carry their own labels.
    render(<WindowCanvas currentStep={{ arrayState: cells(['a', 'b', 'c'], 0, 1) }} />);
    expect(screen.getByText('a')).toBeInTheDocument();
    expect(screen.getByText('b')).toBeInTheDocument();
  });

  it('holds the window through a commentary step that carries no cells', () => {
    // Tracers interleave "here is the window" steps with steps that carry only variables.
    // Dropping the frame on those would make the window flicker out every other step.
    const steps = [
      { arrayState: cells(['1', '2', '3'], 0, 1) },
      { variables: { windowLen: '2', maxLen: '2' } }
    ];
    render(<WindowCanvas steps={steps} currentStepIndex={1} currentStep={steps[1]} />);
    expect(screen.getByTestId('window-frame')).toBeInTheDocument();
    expect(screen.getByTestId('window-bounds').textContent).toContain('window [0, 1]');
  });

  it('shows the aggregates but not the bounds, which are already drawn', () => {
    render(
      <WindowCanvas
        currentStep={{
          arrayState: cells(['1', '2', '3'], 0, 1),
          variables: { left: '0', right: '1', maxLen: '2' }
        }}
      />
    );
    expect(screen.getByText(/maxLen/)).toBeInTheDocument();
    expect(screen.queryByText(/^left/)).not.toBeInTheDocument();
  });

  it('says so when a step carries no window at all', () => {
    render(<WindowCanvas currentStep={{ variables: {} }} />);
    expect(screen.getByRole('status').textContent).toContain('No window data');
  });
});
