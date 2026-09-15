import { renderHook, act } from '@testing-library/react';
import { describe, expect, it, beforeEach } from 'vitest';
import useInputPresets from './useInputPresets';

const KEY = 'dsa-ui:presets';

describe('useInputPresets', () => {
  beforeEach(() => window.localStorage.clear());

  it('saves a preset and lists it back for the same problem', () => {
    const { result } = renderHook(() => useInputPresets('two-sum'));
    act(() => result.current.savePreset('Tricky negatives', { nums: [-3, -1, 5], target: 2 }));

    expect(result.current.presets).toHaveLength(1);
    expect(result.current.presets[0]).toMatchObject({
      name: 'Tricky negatives',
      values: { nums: [-3, -1, 5], target: 2 }
    });
  });

  it('keeps presets scoped to their own problem', () => {
    const twoSum = renderHook(() => useInputPresets('two-sum'));
    act(() => twoSum.result.current.savePreset('mine', { nums: [1] }));

    const kadane = renderHook(() => useInputPresets('kadane-algo'));
    expect(kadane.result.current.presets).toHaveLength(0);
  });

  it('survives a reload - a fresh hook instance reads what an earlier one wrote', () => {
    const first = renderHook(() => useInputPresets('two-sum'));
    act(() => first.result.current.savePreset('mine', { nums: [7] }));

    const second = renderHook(() => useInputPresets('two-sum'));
    expect(second.result.current.presets).toHaveLength(1);
    expect(second.result.current.presets[0].name).toBe('mine');
  });

  it('removes a preset by id, and only that one', () => {
    const { result } = renderHook(() => useInputPresets('two-sum'));
    act(() => result.current.savePreset('a', { nums: [1] }));
    act(() => result.current.savePreset('b', { nums: [2] }));
    const [first, second] = result.current.presets;

    act(() => result.current.removePreset(first.id));

    expect(result.current.presets).toHaveLength(1);
    expect(result.current.presets[0].id).toBe(second.id);
  });

  it('refuses a blank name rather than saving an unlabelled preset', () => {
    const { result } = renderHook(() => useInputPresets('two-sum'));
    act(() => result.current.savePreset('   ', { nums: [1] }));
    expect(result.current.presets).toHaveLength(0);
  });

  it('caps presets per problem, dropping the oldest rather than growing forever', () => {
    const { result } = renderHook(() => useInputPresets('two-sum'));
    for (let i = 0; i < 10; i++) {
      act(() => result.current.savePreset(`preset ${i}`, { nums: [i] }));
    }
    expect(result.current.presets.length).toBeLessThanOrEqual(8);
    // The most recent save must survive the cap; the earliest is what gets evicted.
    expect(result.current.presets.at(-1).name).toBe('preset 9');
    expect(result.current.presets.some((p) => p.name === 'preset 0')).toBe(false);
  });

  it('discards a corrupted or foreign stored shape instead of half-applying it', () => {
    window.localStorage.setItem(KEY, '{"two-sum": "not-an-array", "kadane-algo": [{"bad": true}]}');
    const { result } = renderHook(() => useInputPresets('two-sum'));
    expect(result.current.presets).toEqual([]);

    window.localStorage.setItem(KEY, 'not json');
    const { result: after } = renderHook(() => useInputPresets('two-sum'));
    expect(after.current.presets).toEqual([]);
  });

  it('does not throw and returns an empty list for a problem with nothing saved', () => {
    const { result } = renderHook(() => useInputPresets('never-saved'));
    expect(result.current.presets).toEqual([]);
  });
});
