import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import TourGuide, { TOUR_STEPS } from './TourGuide';

/** Renders the anchors a step needs, so the component can resolve and measure them. */
function withTargets(targets) {
  const host = document.createElement('div');
  targets.forEach((t) => {
    const el = document.createElement('div');
    el.setAttribute('data-tour', t);
    // jsdom reports a zero rect otherwise, which is indistinguishable from "missing".
    el.getBoundingClientRect = () => ({
      top: 100, left: 100, width: 200, height: 80, bottom: 180, right: 300, x: 100, y: 100
    });
    host.appendChild(el);
  });
  document.body.appendChild(host);
  return host;
}

describe('TourGuide', () => {
  beforeEach(() => { document.body.innerHTML = ''; });

  it('renders nothing when closed', () => {
    withTargets(['problem-list']);
    const { container } = render(<TourGuide open={false} onClose={() => {}} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('walks forward through the steps whose targets exist', () => {
    withTargets(['problem-list', 'canvas']);
    render(
      <TourGuide
        open
        onClose={() => {}}
        steps={[
          { target: 'problem-list', title: 'First stop', body: 'one' },
          { target: 'canvas', title: 'Second stop', body: 'two' }
        ]}
      />
    );
    expect(screen.getByText('First stop')).toBeInTheDocument();
    expect(screen.getByText('1 of 2')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByText('Second stop')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Done' })).toBeInTheDocument();
  });

  it('skips a step whose target is not on screen instead of highlighting nothing', () => {
    // A collapsed panel or a desktop-only control simply is not there. Pointing a
    // spotlight at empty space is worse than not mentioning it.
    withTargets(['problem-list']);
    render(
      <TourGuide
        open
        onClose={() => {}}
        steps={[
          { target: 'problem-list', title: 'Present', body: 'one' },
          { target: 'does-not-exist', title: 'Absent', body: 'two' }
        ]}
      />
    );
    expect(screen.getByText('1 of 1')).toBeInTheDocument();
    expect(screen.queryByText('Absent')).not.toBeInTheDocument();
  });

  it('closes on Done, Skip and Escape', () => {
    withTargets(['problem-list']);
    const onClose = vi.fn();
    const one = [{ target: 'problem-list', title: 'Only', body: 'x' }];

    const { unmount } = render(<TourGuide open onClose={onClose} steps={one} />);
    fireEvent.click(screen.getByRole('button', { name: 'Done' }));
    expect(onClose).toHaveBeenCalledTimes(1);
    unmount();

    render(<TourGuide open onClose={onClose} steps={one} />);
    fireEvent.click(screen.getByRole('button', { name: 'Skip' }));
    expect(onClose).toHaveBeenCalledTimes(2);

    fireEvent.keyDown(window, { code: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(3);
  });

  it('owns the arrow keys so the trace cannot step out from under the explanation', () => {
    withTargets(['problem-list', 'canvas']);
    render(
      <TourGuide
        open
        onClose={() => {}}
        steps={[
          { target: 'problem-list', title: 'First stop', body: 'one' },
          { target: 'canvas', title: 'Second stop', body: 'two' }
        ]}
      />
    );
    fireEvent.keyDown(window, { code: 'ArrowRight' });
    expect(screen.getByText('Second stop')).toBeInTheDocument();
    fireEvent.keyDown(window, { code: 'ArrowLeft' });
    expect(screen.getByText('First stop')).toBeInTheDocument();
  });

  it('declares a title and body for every step', () => {
    // Cheap, but it is what catches a half-written step added in a hurry.
    for (const step of TOUR_STEPS) {
      expect(step.target, 'every step needs a target').toBeTruthy();
      expect(step.title?.length, `${step.target} has no title`).toBeGreaterThan(0);
      expect(step.body?.length, `${step.target} has no body`).toBeGreaterThan(20);
    }
  });
});
