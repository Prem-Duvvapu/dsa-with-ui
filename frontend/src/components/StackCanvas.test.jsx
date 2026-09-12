import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import StackCanvas from './StackCanvas';

describe('StackCanvas', () => {
  it('shows an explicit empty state when the step carries no stack', () => {
    render(<StackCanvas step={{ queueOrStackState: [] }} />);
    expect(screen.getByText('empty')).toBeInTheDocument();
  });

  it('renders every entry and tags only the top of the stack', () => {
    render(<StackCanvas step={{ queueOrStackState: ['4', '2', '9'] }} title="Stack" />);

    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('9')).toBeInTheDocument();
    expect(screen.getByText('top')).toBeInTheDocument();
    expect(screen.getAllByText('top')).toHaveLength(1);
  });

  it('fills bottom-up: rests the earliest push on the floor and stacks later ones above it', () => {
    // min-stack's own payload after push 3, push 5, push 1 — index 0 is the top, so '3'
    // is the block that must sit on the bottom edge and stay there.
    const { container } = render(<StackCanvas step={{ queueOrStackState: ['1', '5', '3'] }} />);

    const drawn = [...container.querySelectorAll('[data-stack-index]')];
    const well = drawn[0].parentElement;

    // A column lays children out top-of-screen first, so array order already puts the top
    // of the stack highest. What made it read as a list was the anchor: pinned to the
    // ceiling, the pile grew DOWNWARD on every push.
    expect(well).toHaveStyle({ flexDirection: 'column', justifyContent: 'flex-end' });
    expect(drawn.map((el) => el.getAttribute('data-stack-index'))).toEqual(['0', '1', '2']);
    expect(drawn.map((el) => el.textContent)).toEqual(['top1', '5', '3']);
    // The floor block is last in the DOM, i.e. lowest on screen; the badged top is highest.
    expect(drawn[drawn.length - 1].textContent).toBe('3');
    expect(drawn[0].textContent).toContain('top');
  });

  it('centres the empty state instead of anchoring it to the floor', () => {
    const { container } = render(<StackCanvas step={{ queueOrStackState: [] }} />);
    expect(screen.getByText('empty').parentElement).toHaveStyle({ justifyContent: 'center' });
  });

  it('reads currentStep when step is not supplied', () => {
    render(<StackCanvas currentStep={{ queueOrStackState: ['7'] }} />);
    expect(screen.getByText('7')).toBeInTheDocument();
  });

  it('does not throw when both step and currentStep are missing', () => {
    render(<StackCanvas />);
    expect(screen.getByText('empty')).toBeInTheDocument();
  });

  describe('a step that does not restate the stack', () => {
    it('keeps showing what is there, rather than claiming it is empty', () => {
      // stock-span-problem narrates a span between every push, and those steps carry no
      // payload. Rendering them as empty made the stack blink empty on every other step -
      // 60 such steps across 21 of the 24 Stack & Queue problems.
      const steps = [{ queueOrStackState: ['0'] }, { description: 'span for day 1 = 1' }];
      render(<StackCanvas steps={steps} currentStepIndex={1} currentStep={steps[1]} />);
      expect(screen.getByText('0')).toBeInTheDocument();
      expect(screen.queryByText('empty')).not.toBeInTheDocument();
    });

    it('still shows empty when the trace actually says empty', () => {
      // An explicit [] is a real value and must not be confused with silence.
      const steps = [{ queueOrStackState: ['0'] }, { queueOrStackState: [] }];
      render(<StackCanvas steps={steps} currentStepIndex={1} currentStep={steps[1]} />);
      expect(screen.getByText('empty')).toBeInTheDocument();
    });
  });
});
