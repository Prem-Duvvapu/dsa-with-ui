import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describe, expect, it, vi } from 'vitest';
import WelcomeGuide from './WelcomeGuide';

describe('WelcomeGuide', () => {
  it('names the three regions a newcomer has to find', () => {
    render(<WelcomeGuide open onDismiss={() => {}} onShowShortcuts={() => {}} />);
    expect(screen.getByRole('dialog', { name: /Welcome to the DSA Visualizer/i })).toBeInTheDocument();
    expect(screen.getByText('Pick a problem')).toBeInTheDocument();
    expect(screen.getByText('Watch it run')).toBeInTheDocument();
    expect(screen.getByText('Follow the code')).toBeInTheDocument();
  });

  it('describes the workspace as it is: the switcher and the views, not the retired sidebar', () => {
    render(<WelcomeGuide open onDismiss={() => {}} onShowShortcuts={() => {}} totalProblems={431} />);
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveTextContent('Switch problem (Ctrl/⌘ K, or /) searches all 431 problems');
    expect(dialog).toHaveTextContent('Code walkthrough puts the Java beside the diagram');
    expect(dialog).not.toHaveTextContent(/list on the left|panel on the right/);
  });

  it('makes the page behind it inert until dismissed', () => {
    const page = document.createElement('main');
    document.body.appendChild(page);
    const { rerender } = render(<WelcomeGuide open onDismiss={() => {}} onShowShortcuts={() => {}} />);
    expect(page).toHaveAttribute('inert');
    rerender(<WelcomeGuide open={false} onDismiss={() => {}} onShowShortcuts={() => {}} />);
    expect(page).not.toHaveAttribute('inert');
    page.remove();
  });

  it('renders nothing when closed', () => {
    const { container } = render(<WelcomeGuide open={false} onDismiss={() => {}} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('dismisses from the primary action', () => {
    const onDismiss = vi.fn();
    render(<WelcomeGuide open onDismiss={onDismiss} onShowShortcuts={() => {}} />);
    fireEvent.click(screen.getByRole('button', { name: /Start exploring/i }));
    expect(onDismiss).toHaveBeenCalled();
  });

  it('hands off to the shortcut list', () => {
    const onShowShortcuts = vi.fn();
    render(<WelcomeGuide open onDismiss={() => {}} onShowShortcuts={onShowShortcuts} />);
    fireEvent.click(screen.getByRole('button', { name: /See all shortcuts/i }));
    expect(onShowShortcuts).toHaveBeenCalled();
  });

  it('focuses its primary action so the keyboard lands inside the dialog', () => {
    render(<WelcomeGuide open onDismiss={() => {}} onShowShortcuts={() => {}} />);
    expect(document.activeElement).toBe(screen.getByRole('button', { name: /Start exploring/i }));
  });
});
