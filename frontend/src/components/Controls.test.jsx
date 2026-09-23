import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import Controls from './Controls';

const baseProps = {
  isPlaying: false,
  currentStepIndex: 3,
  totalSteps: 10,
  speed: 1000,
  onPlayPause: () => {},
  onStepNext: () => {},
  onStepPrev: () => {},
  onStepSelect: () => {},
  onReset: () => {},
  onSpeedChange: () => {}
};

/**
 * useShareableView already mirrors the current step into ?step= with `replace`, for every
 * run - default or custom input alike - so window.location.href at any moment already IS
 * the shareable URL. Nothing here needs to reconstruct one; it only needs to copy what the
 * address bar already carries.
 */
describe('Controls: copy link', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/problem/kadane-algo?step=4');
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('copies the current address, not a reconstructed one', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText } });

    render(<Controls {...baseProps} />);
    fireEvent.click(screen.getByRole('button', { name: /copy link/i }));

    await waitFor(() => expect(writeText).toHaveBeenCalledWith(
      'http://localhost:3000/problem/kadane-algo?step=4'
    ));
  });

  it('confirms the copy out loud, not just visually', async () => {
    // A silent clipboard write is not obvious it happened. The confirmation is a real
    // status announcement, not merely changed pixels the button's own label swallows.
    Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });

    render(<Controls {...baseProps} />);
    fireEvent.click(screen.getByRole('button', { name: /copy link/i }));

    expect(await screen.findByRole('status')).toHaveTextContent(/copied/i);
  });

  it('reverts to the copy prompt after the confirmation fades', async () => {
    vi.useFakeTimers();
    Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });

    render(<Controls {...baseProps} />);
    fireEvent.click(screen.getByRole('button', { name: /copy link/i }));
    await vi.waitFor(() => expect(screen.getByRole('status')).toHaveTextContent(/copied/i));

    vi.advanceTimersByTime(3000);
    expect(screen.getByRole('button', { name: /copy link/i })).toBeInTheDocument();
  });

  it('says so, rather than nothing, when the clipboard write fails', async () => {
    // Never fail silently - the same rule this codebase applies to a tracer or a canvas
    // applies to a browser API that can also just be absent (insecure context, an older
    // browser, a denied permission).
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockRejectedValue(new Error('denied')) }
    });

    render(<Controls {...baseProps} />);
    fireEvent.click(screen.getByRole('button', { name: /copy link/i }));

    expect(await screen.findByRole('status')).toHaveTextContent(/could not copy|couldn.t copy/i);
  });
});
