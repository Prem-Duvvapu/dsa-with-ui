import { useCallback } from 'react';
import usePersistentState from './usePersistentState';

const DEFAULT_RECORD = { lastDate: null, current: 0, longest: 0 };

const isValidRecord = (v) => v && typeof v === 'object'
  && (v.lastDate === null || typeof v.lastDate === 'string')
  && Number.isInteger(v.current) && Number.isInteger(v.longest);

/** Whole calendar days between two 'YYYY-MM-DD' strings, both read as UTC midnight so
 *  neither side's local timezone can shift which day a visit lands on. */
function daysBetween(from, to) {
  const MS_PER_DAY = 86400000;
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / MS_PER_DAY);
}

/**
 * A visit streak with no server and no account: `today` is supplied by the caller
 * (App.jsx passes a real date string), so this file never calls `new Date()` itself and
 * stays trivial to test against any sequence of days.
 */
export default function useStreak(today) {
  const [record, setRecord] = usePersistentState('streak', DEFAULT_RECORD, isValidRecord);

  const recordVisit = useCallback(() => {
    if (!today) return;
    setRecord((prev) => {
      if (prev.lastDate === today) return prev;
      const gap = prev.lastDate ? daysBetween(prev.lastDate, today) : null;
      const current = gap === 1 ? prev.current + 1 : 1;
      return { lastDate: today, current, longest: Math.max(prev.longest, current) };
    });
  }, [today, setRecord]);

  return { current: record.current, longest: record.longest, recordVisit };
}
