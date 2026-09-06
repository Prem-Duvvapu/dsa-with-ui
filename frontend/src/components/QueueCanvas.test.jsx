import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import QueueCanvas, { QueueHeroCanvas } from './QueueCanvas';
import { CANVAS_BY_DSTYPE } from '../canvas/registry';

const CSS = readFileSync(
  resolve(dirname(fileURLToPath(import.meta.url)), '../index.css'),
  'utf8'
);

/** The declaration block for one class selector in index.css. */
function ruleFor(selector) {
  const start = CSS.indexOf(`${selector} {`);
  expect(start, `${selector} is not defined in index.css`).toBeGreaterThan(-1);
  return CSS.slice(start, CSS.indexOf('\n}', start));
}

describe('QueueCanvas', () => {
  it('shows an explicit empty state when the step carries no queue', () => {
    render(<QueueCanvas step={{ queueOrStackState: [] }} />);
    expect(screen.getByText('empty')).toBeInTheDocument();
  });

  it('renders every entry and tags only the front of the queue', () => {
    render(<QueueCanvas step={{ queueOrStackState: ['1:4', '3:6'] }} title="Priority queue" />);

    expect(screen.getByLabelText('Priority queue')).toBeInTheDocument();
    expect(screen.getByText('1:4')).toBeInTheDocument();
    expect(screen.getByText('3:6')).toBeInTheDocument();
    expect(screen.getByText('front')).toBeInTheDocument();
    expect(screen.getAllByText('front')).toHaveLength(1);
  });

  it('does not throw when the step itself is missing', () => {
    render(<QueueCanvas step={undefined} />);
    expect(screen.getByText('empty')).toBeInTheDocument();
  });

  it('reads currentStep when only that prop is supplied', () => {
    // App.jsx's hero call site spreads { currentStep, step: currentStep }; companions.js
    // passes `step` alone. Both have to land on the same element.
    render(<QueueCanvas currentStep={{ queueOrStackState: ['7'] }} />);
    expect(screen.getByText('7')).toBeInTheDocument();
  });

  it('lays the queue out front-first, left to right', () => {
    const { container } = render(<QueueCanvas step={{ queueOrStackState: ['a', 'b', 'c'] }} />);

    const cells = [...container.querySelectorAll('[data-queue-index]')];
    expect(cells.map((el) => el.getAttribute('data-queue-index'))).toEqual(['0', '1', '2']);
    expect(cells.map((el) => el.textContent)).toEqual(['fronta', 'b', 'c']);
    // jsdom applies no stylesheet, so the row direction is asserted against index.css
    // itself — a column here is a stack's shape, not a queue's.
    expect(ruleFor('.queue-row')).toMatch(/flex-direction:\s*row;/);
    expect(ruleFor('.queue-row')).not.toMatch(/flex-direction:\s*column;/);
  });

  it('gives the hero variant its own widened pane, leaving the companion narrow', () => {
    const { container: hero } = render(<QueueHeroCanvas step={{ queueOrStackState: ['a'] }} />);
    expect(hero.querySelector('.companion-pane').className).toContain('queue-pane-hero');

    const { container: companion } = render(<QueueCanvas step={{ queueOrStackState: ['a'] }} />);
    expect(companion.querySelector('.companion-pane').className).not.toContain('queue-pane-hero');

    expect(ruleFor('.companion-pane')).toMatch(/flex:\s*0 0 200px;/);
    expect(ruleFor('.queue-pane-hero')).toMatch(/flex:\s*1;/);
  });

  it('routes DsType.QUEUE to the hero variant, not the companion one', () => {
    expect(CANVAS_BY_DSTYPE.Queue).toBe(QueueHeroCanvas);
    expect(CANVAS_BY_DSTYPE.Queue).not.toBe(QueueCanvas);
  });
});
