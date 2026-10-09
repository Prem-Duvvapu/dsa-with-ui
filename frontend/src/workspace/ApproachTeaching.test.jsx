import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import ApproachTeaching from './ApproachTeaching';

const approach = { label: 'Memoization', teaching: { state: 'State', baseCases: 'Bases',
  recurrence: 'Recurrence', evaluationOrder: 'Order', cacheKey: 'k',
  eventNotes: { hit: 'Actual cache hit' }, anchorNotes: { done: 'Actual completion' } } };

describe('audited approach teaching', () => {
  it.each([
    { ...approach.teaching, state: { text: 'Not a text field' } },
    { ...approach.teaching, baseCases: ' ' },
    { ...approach.teaching, recurrence: null },
    { ...approach.teaching, cacheKey: 7 },
    { ...approach.teaching, eventNotes: [] },
    { ...approach.teaching, anchorNotes: { done: { text: 'Not a note' } } }
  ])('hides malformed optional metadata without breaking the execution %j', teaching => {
    const { container } = render(<ApproachTeaching approach={{ ...approach, teaching }} />);
    expect(container).toBeEmptyDOMElement();
  });
  it.each(['__proto__', 'toString'])('does not render inherited event metadata for %s', event => {
    const { container } = render(<ApproachTeaching approach={approach}
      step={{ variables: { event }, activeLine: 9 }} anchors={{ done: 9 }} />);
    expect(container.querySelector('[aria-live]')).toBeNull();
    expect(screen.queryByText('Actual completion')).not.toBeInTheDocument();
  });
  it('does not use inherited anchor notes', () => {
    const { container } = render(<ApproachTeaching approach={approach}
      step={{ activeLine: 9 }} anchors={{ toString: 9 }} />);
    expect(container.querySelector('[aria-live]')).toBeNull();
  });
  it('shows the actual event note, not a simultaneous anchor note', () => {
    render(<ApproachTeaching approach={approach} step={{ variables: { event: 'hit' }, activeLine: 9 }} anchors={{ done: 9 }} />);
    expect(screen.getByText('Actual cache hit')).toHaveAttribute('aria-live', 'polite');
    expect(screen.queryByText('Actual completion')).not.toBeInTheDocument();
  });
  it('uses an actual anchor when the tracer has no event field', () => {
    render(<ApproachTeaching approach={approach} step={{ activeLine: 9 }} anchors={{ done: 9 }} />);
    expect(screen.getByText('Actual completion')).toHaveAttribute('aria-live', 'polite');
  });
  it('finds an authored note even when an unannotated alias shares its source line', () => {
    render(<ApproachTeaching approach={approach} step={{ activeLine: 9 }} anchors={{ alias: 9, done: 9 }} />);
    expect(screen.getByText('Actual completion')).toHaveAttribute('aria-live', 'polite');
  });
  it('does not guess between conflicting notes on the same source line', () => {
    const ambiguous = { ...approach, teaching: { ...approach.teaching,
      anchorNotes: { init: 'Actual initialization', done: 'Actual completion' } } };
    const { container } = render(<ApproachTeaching approach={ambiguous}
      step={{ activeLine: 9 }} anchors={{ init: 9, done: 9 }} />);
    expect(container.querySelector('[aria-live]')).toBeNull();
  });
  it('allows identical audited notes on aliased source anchors', () => {
    const aliased = { ...approach, teaching: { ...approach.teaching,
      anchorNotes: { done: 'Actual completion', alias: 'Actual completion' } } };
    render(<ApproachTeaching approach={aliased} step={{ activeLine: 9 }} anchors={{ alias: 9, done: 9 }} />);
    expect(screen.getByText('Actual completion')).toHaveAttribute('aria-live', 'polite');
  });
  it('renders nothing when audited metadata is absent', () => {
    const { container } = render(<ApproachTeaching approach={{ label: 'Current solution' }} />);
    expect(container).toBeEmptyDOMElement();
  });
});
