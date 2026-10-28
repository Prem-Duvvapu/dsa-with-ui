import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import SearchSpaceCanvas from './SearchSpaceCanvas';
import styles from './SearchSpaceCanvas.module.css';

const continuous = (low, high, extra = {}, arrayState = []) => ({
  variables: { searchDomain: 'continuous', low: String(low), high: String(high), ...extra },
  arrayState,
});
const show = (steps, index = steps.length - 1) => render(
  <SearchSpaceCanvas steps={steps} currentStepIndex={index} currentStep={steps[index]} />,
);

describe('SearchSpaceCanvas continuous intervals', () => {
  it('measures a continuous interval by its width rather than an inclusive integer count', () => {
    show([continuous(0, 1000)]);
    const summary = screen.getByTestId('search-range');
    expect(summary).toHaveTextContent('continuous [0, 1000]');
    expect(summary).toHaveTextContent('width 1000 of 1000');
    expect(summary).not.toHaveTextContent('left');
    expect(screen.getByTestId('search-live')).toHaveStyle({ left: '0%', width: '100%' });
  });

  it('positions the remaining interval and midpoint against the actual original width', () => {
    const { container } = show([continuous(0, 1000), continuous(250, 500, { mid: '375' })]);
    expect(screen.getByTestId('search-range')).toHaveTextContent('width 250 of 1000');
    expect(screen.getByTestId('search-live')).toHaveStyle({ left: '25%', width: '25%' });
    expect(container.querySelector(`.${styles.rulerMid}`)).toHaveStyle({ left: '37.5%' });
  });

  it('honors continuous metadata even when the bounds and cells look like an index search', () => {
    const input = [4, 8].map((value, index) => ({ index, value, state: 'target' }));
    show([continuous(0, 1, {}, input)]);
    expect(screen.getByTestId('search-range')).toHaveTextContent('continuous [0, 1]');
    expect(screen.getByTestId('search-range')).toHaveTextContent('width 1 of 1');
    expect(screen.queryByTestId('search-cells')).not.toBeInTheDocument();
    expect(screen.getByTestId('search-input-row')).toHaveTextContent('checked against');
  });

  it('retains the real tiny final interval instead of reporting approximately one candidate', () => {
    const low = 333.3333333330302;
    const high = 333.3333333339397;
    show([continuous(0, 1000), continuous(low, high)]);
    const summary = screen.getByTestId('search-range');
    expect(summary).toHaveTextContent(`[${low}, ${high}]`);
    expect(summary).toHaveTextContent(`width ${high - low} of 1000`);
    expect(summary).not.toHaveTextContent('left');
    const width = Number.parseFloat(screen.getByTestId('search-live').style.width);
    expect(width).toBeGreaterThan(0);
    expect(width).toBeCloseTo(((high - low) / 1000) * 100, 16);
  });

  it('carries the complete range and its domain through commentary while showing current input reads', () => {
    const input = [12, 4, 6].map((value, index) => ({ index, value, state: 'target' }));
    const first = continuous(0, 1000, {}, input);
    const scanning = { variables: { needed: '4' }, arrayState: input.map((cell, index) => ({
      ...cell, state: index === 2 ? 'current' : 'target',
    })) };
    show([first, continuous(250, 500, { mid: '375' }, input), scanning]);
    expect(screen.getByTestId('search-range')).toHaveTextContent('continuous [250, 500]');
    expect(screen.getByTestId('search-range')).toHaveTextContent('width 250 of 1000');
    expect(screen.getByTestId('search-mid')).toHaveTextContent('probing 375');
    const cells = screen.getByTestId('search-input-row').querySelectorAll('[data-state]');
    expect([...cells].map(cell => cell.getAttribute('data-state'))).toEqual(['target', 'target', 'current']);
  });

  it('does not combine a later partial bound or domain with the previous complete snapshot', () => {
    show([continuous(0, 1000), {
      variables: { high: '500', searchDomain: 'discrete', needed: '4' },
    }]);
    expect(screen.getByTestId('search-range')).toHaveTextContent('continuous [0, 1000]');
    expect(screen.getByTestId('search-range')).toHaveTextContent('width 1000 of 1000');
  });

  it('uses the paired final bounds and clears the old probe when the completed snapshot has no mid', () => {
    const low = 0.024999999999636202;
    const high = 0.025000000000545697;
    show([continuous(0, 1), continuous(0.024, 0.026, { mid: '0.025' }),
      continuous(low, high, { answer: '0.02500' })]);
    expect(screen.getByTestId('search-range')).toHaveTextContent(`continuous [${low}, ${high}]`);
    expect(screen.getByTestId('search-range')).toHaveTextContent(`width ${high - low} of 1`);
    expect(screen.queryByTestId('search-mid')).not.toBeInTheDocument();
    expect(screen.getByTestId('search-result')).toHaveTextContent('Result: 0.025');
  });

  it.each([false, true])('represents a valid zero-width interval without a fabricated candidate or invalid ruler value (initial=%s)', initial => {
    const point = continuous(2, 2, { answer: '2.00000' });
    const { container } = show(initial ? [point] : [continuous(0, 10), point]);
    expect(screen.getByTestId('search-range')).toHaveTextContent(`continuous [2, 2] · width 0 of ${initial ? 0 : 10}`);
    expect(screen.getByTestId('search-range')).not.toHaveTextContent('left');
    expect(screen.getByTestId('search-result')).toHaveTextContent('Result: 2');
    expect(container.innerHTML).not.toMatch(/NaN|Infinity/);
  });

  it.each(['logarithmic', '', null])('does not invent discrete semantics for an explicit unsupported domain %j', searchDomain => {
    show([{ variables: { low: '0', high: '1', searchDomain } }]);
    expect(screen.getByRole('status')).toHaveTextContent(/unsupported search domain/i);
    expect(screen.queryByTestId('search-range')).not.toBeInTheDocument();
    expect(screen.queryByTestId('search-live')).not.toBeInTheDocument();
  });

  it('does not disguise an inverted continuous interval as a valid zero-width point', () => {
    show([continuous(9, 3)]);
    expect(screen.getByRole('status')).toHaveTextContent(/invalid continuous search interval/i);
    expect(screen.queryByTestId('search-range')).not.toBeInTheDocument();
    expect(screen.queryByTestId('search-live')).not.toBeInTheDocument();
  });

  it('keeps legacy integer semantics when no domain marker is supplied', () => {
    show([{ variables: { low: '4', high: '9' } }]);
    expect(screen.getByTestId('search-range')).toHaveTextContent('answers [4, 9] · 6 left of 6');
  });

  it('does not create a range merely because a continuous domain was declared', () => {
    show([{ variables: { searchDomain: 'continuous', needed: '4' } }]);
    expect(screen.getByRole('status')).toHaveTextContent('No search range');
    expect(screen.queryByTestId('search-range')).not.toBeInTheDocument();
  });
});
