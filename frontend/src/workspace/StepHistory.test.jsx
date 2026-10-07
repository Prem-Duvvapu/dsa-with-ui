import React from 'react';
import { render, screen, fireEvent, within } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it, vi } from 'vitest';
import StepHistory, { HISTORY_PAGE_SIZE } from './StepHistory';

const run = (n) => Array.from({ length: n }, (_, i) => ({ stepNumber: i + 1, description: `narration ${i + 1}` }));
const entries = () => within(screen.getByRole('list')).getAllByRole('button');

describe('StepHistory (P6a)', () => {
  it('lists one bounded page with its real range, never the whole run', () => {
    render(<StepHistory steps={run(5000)} current={0} onSeek={() => {}} />);
    expect(entries()).toHaveLength(HISTORY_PAGE_SIZE);
    expect(screen.getByText(/^Steps 1–50 of 5000/)).toBeInTheDocument();
    expect(entries()[0]).toHaveTextContent('Step 1narration 1');
  });

  it('pages without seeking', () => {
    const onSeek = vi.fn();
    render(<StepHistory steps={run(120)} current={0} onSeek={onSeek} />);
    fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
    expect(screen.getByText(/^Steps 51–100 of 120/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
    expect(screen.getByText(/^Steps 101–120 of 120/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled();
    expect(onSeek).not.toHaveBeenCalled();
  });

  it('reaches the final step of a near-limit run and seeks to it', () => {
    const onSeek = vi.fn();
    render(<StepHistory steps={run(4999)} current={0} onSeek={onSeek} />);
    for (let i = 0; i < 99; i++) fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
    const last = entries().at(-1);
    expect(last).toHaveTextContent('Step 4999narration 4999');
    expect(entries()).toHaveLength(49);
    fireEvent.click(last);
    expect(onSeek).toHaveBeenCalledWith(4998);
  });

  it('marks the current step, and says where it is when it is on another page', () => {
    const { rerender } = render(<StepHistory steps={run(120)} current={2} onSeek={() => {}} />);
    expect(entries()[2]).toHaveAttribute('aria-current', 'step');
    fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
    expect(screen.getByText(/the current step, 3, is on page 1/)).toBeInTheDocument();
    rerender(<StepHistory steps={run(120)} current={110} onSeek={() => {}} />);
    fireEvent.click(screen.getByRole('button', { name: 'Jump to current step' }));
    expect(screen.getByText(/^Steps 101–120 of 120/)).toBeInTheDocument();
    expect(entries()[10]).toHaveAttribute('aria-current', 'step');
  });

  it('opens on the page holding the current step', () => {
    render(<StepHistory steps={run(300)} current={260} onSeek={() => {}} />);
    expect(screen.getByText(/^Steps 251–300 of 300/)).toBeInTheDocument();
  });

  it('says so when there is no run', () => {
    render(<StepHistory steps={[]} current={0} onSeek={() => {}} />);
    expect(screen.getByText('There are no steps to list: no run is loaded.')).toBeInTheDocument();
  });

  it('announces page changes without announcing every off-page playback tick', () => {
    const steps = run(120);
    const onSeek = vi.fn();
    const { rerender } = render(<StepHistory steps={steps} current={0} onSeek={onSeek} />);
    rerender(<StepHistory steps={steps} current={100} onSeek={onSeek} />);
    expect(document.querySelector('[aria-live="polite"]')).toHaveTextContent(/^Steps 1–50 of 120$/);
    expect(screen.getByText(/the current step, 101, is on page 3/)).toBeInTheDocument();
    rerender(<StepHistory steps={steps} current={101} onSeek={onSeek} />);
    expect(document.querySelector('[aria-live="polite"]')).toHaveTextContent(/^Steps 1–50 of 120$/);
    expect(screen.getByText(/the current step, 102, is on page 3/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
    expect(document.querySelector('[aria-live="polite"]')).toHaveTextContent(/^Steps 51–100 of 120$/);
    expect(onSeek).not.toHaveBeenCalled();
  });
});
