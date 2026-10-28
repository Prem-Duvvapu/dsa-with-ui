import React from 'react';
import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import RecursionTreeCanvas from './RecursionTreeCanvas';
import styles from './RecursionTreeCanvas.module.css';

function shown(arrayState) {
  const step = { callStack: ['backtrack(idx=0)'], arrayState };
  const { container } = render(<RecursionTreeCanvas steps={[step]} currentStep={step} currentStepIndex={0} />);
  return [...container.querySelectorAll(`.${styles.stripValue}`)].map(element => element.textContent);
}

describe('recursive path character labels', () => {
  it('shows actual keypad letters instead of their numeric code points', () => {
    expect(shown([{ index: 0, value: 97, label: 'a' }, { index: 1, value: 100, label: 'd' }])).toEqual(['a', 'd']);
  });
  it('preserves supplied Unicode character labels', () => {
    expect(shown([{ index: 0, value: 119070, label: '𝄞' }])).toEqual(['𝄞']);
  });
  it('keeps genuine numeric values, including zero, when there is no label', () => {
    expect(shown([{ index: 0, value: 0, state: 'visited' }, { index: 1, value: -7, state: 'sorted' }])).toEqual(['0', '-7']);
  });
});
