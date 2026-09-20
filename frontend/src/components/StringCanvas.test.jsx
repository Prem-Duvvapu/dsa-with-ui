import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import StringCanvas from './StringCanvas';

const chars = (letters, states = []) =>
  letters.split('').map((ch, i) => ({
    index: i,
    value: ch.codePointAt(0),
    label: ch,
    state: states[i] || 'default'
  }));

describe('StringCanvas', () => {
  it('draws the letter, not the code point', () => {
    // ArrayCanvas, the placeholder this replaced, showed the code point as the headline
    // number and the letter as a small caption. The letter is the structure here; the
    // code point is an implementation detail no learner asked to see.
    render(<StringCanvas currentStep={{ arrayState: chars('abc') }} />);
    expect(screen.getByText('a')).toBeInTheDocument();
    expect(screen.getByText('b')).toBeInTheDocument();
    expect(screen.getByText('c')).toBeInTheDocument();
    expect(screen.queryByText('97')).not.toBeInTheDocument();
  });

  it('marks the current and target positions distinctly from the rest', () => {
    // "kayak" repeats both 'k' and 'a', so the two tracked positions are distinguished
    // by which cell they sit in, not by which letter they are.
    render(
      <StringCanvas currentStep={{ arrayState: chars('kayak', ['current', 'default', 'default', 'default', 'target']) }} />
    );
    const ks = screen.getAllByText('k');
    expect(ks[0].closest('[data-string-index]')).toHaveAttribute('data-state', 'current');
    expect(ks[1].closest('[data-string-index]')).toHaveAttribute('data-state', 'target');
  });

  it('renders the index under every character', () => {
    render(<StringCanvas currentStep={{ arrayState: chars('go') }} />);
    expect(screen.getByText('0')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('holds the track through a commentary step that carries no cells', () => {
    // Same carry-forward rule as every other structure canvas: a tracer restates
    // arrayState only on the steps that change it.
    const steps = [
      { arrayState: chars('go') },
      { variables: { i: '1' } }
    ];
    render(<StringCanvas steps={steps} currentStepIndex={1} currentStep={steps[1]} />);
    expect(screen.getByText('g')).toBeInTheDocument();
    expect(screen.getByText('o')).toBeInTheDocument();
  });

  it('says so for an empty string rather than rendering nothing', () => {
    render(<StringCanvas currentStep={{ arrayState: [] }} />);
    expect(screen.getByRole('status').textContent).toContain('Empty string');
  });

  it('renders a highlighted space as a visible cell, not a blank one', () => {
    render(<StringCanvas currentStep={{ arrayState: chars('a b', ['default', 'current', 'default']) }} />);
    const spaceCell = screen.getByText('1').closest('[data-string-index]');
    expect(spaceCell).toHaveAttribute('data-state', 'current');
  });
});
