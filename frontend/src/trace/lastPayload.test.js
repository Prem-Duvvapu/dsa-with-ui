import { describe, expect, it } from 'vitest';
import { lastPayload } from './lastPayload';

describe('lastPayload', () => {
  it('uses the current step when it states the field', () => {
    const steps = [{ s: ['a'] }, { s: ['b'] }];
    expect(lastPayload(steps, 1, 's', steps[1])).toEqual(['b']);
  });

  it('looks back when the current step is silent', () => {
    // The defect this exists for: a narration step between two pushes must not make the
    // stack blink empty.
    const steps = [{ s: ['a'] }, { note: 'span = 1' }];
    expect(lastPayload(steps, 1, 's', steps[1])).toEqual(['a']);
  });

  it('treats an explicit empty array as a real value, not as silence', () => {
    // "The stack is empty" and "this step did not mention the stack" are different facts
    // and must render differently.
    const steps = [{ s: ['a'] }, { s: [] }];
    expect(lastPayload(steps, 1, 's', steps[1])).toEqual([]);
  });

  it('skips over several silent steps', () => {
    const steps = [{ s: ['a', 'b'] }, {}, {}, {}];
    expect(lastPayload(steps, 3, 's', steps[3])).toEqual(['a', 'b']);
  });

  it('never looks past the step being shown', () => {
    const steps = [{}, { s: ['later'] }];
    expect(lastPayload(steps, 0, 's', steps[0])).toBeNull();
  });

  it('returns null when nothing has ever stated the field', () => {
    expect(lastPayload([{}, {}], 1, 's', {})).toBeNull();
    expect(lastPayload(null, 0, 's', undefined)).toBeNull();
  });
});
