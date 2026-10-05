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

  it('skips a target that is rendered but hidden - Focus mode, a closed phone Menu', () => {
    const host = withTargets(['canvas']);
    const hidden = document.createElement('div');
    hidden.hidden = true;
    hidden.appendChild(Object.assign(document.createElement('div'), {}));
    hidden.firstChild.setAttribute('data-tour', 'input-editor');
    const menu = document.createElement('details');
    menu.innerHTML = '<summary>Menu</summary><button data-tour="switcher">Switch problem</button>';
    host.append(hidden, menu);
    render(
      <TourGuide
        open
        onClose={() => {}}
        steps={[
          { target: 'switcher', title: 'In the closed menu', body: 'x' },
          { target: 'input-editor', title: 'Hidden by Focus', body: 'y' },
          { target: 'canvas', title: 'On screen', body: 'z' }
        ]}
      />
    );
    expect(screen.getByText('1 of 1')).toBeInTheDocument();
    expect(screen.getByText('On screen')).toBeInTheDocument();
  });

  it('scrolls each target into view before spotlighting it', () => {
    const host = withTargets(['canvas', 'input-editor']);
    const scrolled = [];
    host.querySelectorAll('[data-tour]').forEach((el) => {
      el.scrollIntoView = (options) => scrolled.push([el.getAttribute('data-tour'), options.block]);
    });
    render(
      <TourGuide
        open
        onClose={() => {}}
        steps={[
          { target: 'canvas', title: 'Top', body: 'one' },
          { target: 'input-editor', title: 'Below the fold', body: 'two' }
        ]}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(scrolled).toEqual([['canvas', 'center'], ['input-editor', 'center']]);
  });

  it('makes the page inert while it is up, and gives focus back when it ends', () => {
    withTargets(['canvas']);
    const opener = document.createElement('button');
    opener.textContent = 'Help';
    document.body.appendChild(opener);
    opener.focus();
    const one = [{ target: 'canvas', title: 'Only', body: 'x' }];
    const { rerender } = render(<TourGuide open onClose={() => {}} steps={one} />);
    expect(opener).toHaveAttribute('inert');
    expect(screen.getByRole('button', { name: 'Done' })).toHaveFocus();
    rerender(<TourGuide open={false} onClose={() => {}} steps={one} />);
    expect(document.querySelector('[inert]')).toBeNull();
    expect(opener).toHaveFocus();
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
