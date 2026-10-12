import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import useInputDraft from './useInputDraft';

const spec = (nums = [2, 7], target = 9) => ({ fields: [
  { name: 'nums', type: 'INT_ARRAY', defaultValue: nums },
  { name: 'target', type: 'INT', defaultValue: target }
] });

describe('partial drafts and authoritative late defaults', () => {
  it('fills only omitted fields when the matching input spec arrives after a partial link', () => {
    const { result, rerender } = renderHook(({ inputSpec }) => useInputDraft('two-sum', inputSpec),
      { initialProps: { inputSpec: null } });
    act(() => result.current.replace({ target: 999 }));
    rerender({ inputSpec: spec() });
    expect(result.current.values).toEqual({ nums: [2, 7], target: 999 });
    expect(result.current.isEdited).toBe(true);
  });

  it('preserves explicit empty and null fields instead of treating them as omitted', () => {
    const { result, rerender } = renderHook(({ inputSpec }) => useInputDraft('two-sum', inputSpec),
      { initialProps: { inputSpec: null } });
    act(() => result.current.replace({ nums: [], target: null }));
    rerender({ inputSpec: spec() });
    expect(result.current.values).toEqual({ nums: [], target: null });
    act(() => result.current.replace({ target: '' }));
    expect(result.current.values).toEqual({ nums: [2, 7], target: '' });
  });

  it('freezes all visible defaults, not just the partial edited keys, before changing approaches', () => {
    const { result, rerender } = renderHook(({ inputSpec }) => useInputDraft('two-sum', inputSpec),
      { initialProps: { inputSpec: spec() } });
    act(() => result.current.replace({ target: 999 }));
    act(() => result.current.preserve());
    rerender({ inputSpec: spec([1, 2, 3], 0) });
    expect(result.current.values).toEqual({ nums: [2, 7], target: 999 });
  });

  it('retains queued edits when a same-tick approach preparation snapshots defaults', () => {
    const { result, rerender } = renderHook(({ inputSpec }) => useInputDraft('two-sum', inputSpec),
      { initialProps: { inputSpec: spec() } });
    act(() => { result.current.replace({ target: 13 }); result.current.preserve(); });
    rerender({ inputSpec: spec([1, 2, 3], 0) });
    expect(result.current.values).toEqual({ nums: [2, 7], target: 13 });
  });

  it('uses only the new problem’s fields and defaults after a problem switch', () => {
    const { result, rerender } = renderHook(({ id, inputSpec }) => useInputDraft(id, inputSpec),
      { initialProps: { id: 'two-sum', inputSpec: spec() } });
    act(() => result.current.replace({ target: 999 }));
    rerender({ id: 'reverse-word', inputSpec: { fields: [{ name: 'word', type: 'STRING', defaultValue: 'abc' }] } });
    expect(result.current.values).toEqual({ word: 'abc' });
    expect(result.current.isEdited).toBe(false);
  });
});
