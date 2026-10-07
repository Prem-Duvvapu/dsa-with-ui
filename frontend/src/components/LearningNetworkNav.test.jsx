import React from 'react';
import '@testing-library/jest-dom';
import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import LearningNetworkNav from './LearningNetworkNav';

describe('Learning network navigation', () => {
  it('offers native same-tab routes to the hub and all four subjects', () => {
    render(<LearningNetworkNav />);
    fireEvent.click(screen.getByText('Learning network', { selector: 'summary span' }));
    const navigation = screen.getByRole('navigation', { name: 'Learning network' });
    const links = within(navigation).getAllByRole('link');
    expect(links.map(link => link.getAttribute('href'))).toEqual([
      'https://learning-hub-with-ui.vercel.app/',
      'https://dsa-with-ui.vercel.app/',
      'https://lld-with-ui.vercel.app/',
      'https://hld-with-ui.vercel.app/',
      'https://cs-fundamentals-with-ui.vercel.app/',
    ]);
    for (const link of links) expect(link.hasAttribute('target')).toBe(false);
    const current = links.filter(link => link.getAttribute('aria-current') === 'location');
    expect(current).toHaveLength(1);
    expect(current[0].textContent.trim()).toBe('DSA');
  });

  it('keeps all subjects in a labelled disclosure when used inside the page header', () => {
    render(<LearningNetworkNav />);
    const summary = screen.getByText('Learning network', { selector: 'summary span' }).closest('summary');
    // jsdom does not apply the native closed-details visibility rule to its descendants;
    // browser evidence checks their accessibility-tree exposure too.
    expect(summary.closest('details')).not.toHaveAttribute('open');
    fireEvent.click(summary);
    const navigation = screen.getByRole('navigation', { name: 'Learning network' });
    expect(within(navigation).getAllByRole('link')).toHaveLength(5);
    within(navigation).getByRole('link', { name: 'HLD' }).focus();
    const event = new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', bubbles: true, cancelable: true });
    expect(within(navigation).getByRole('link', { name: 'HLD' }).dispatchEvent(event)).toBe(false);
    expect(summary).toHaveFocus();
    expect(summary.closest('details')).not.toHaveAttribute('open');
  });
});
