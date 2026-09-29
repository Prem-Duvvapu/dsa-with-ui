import { useCallback, useLayoutEffect, useReducer, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';

/**
 * Search parameters that are always the latest thing this component wrote.
 *
 * The router runs navigation inside a React transition (the `v7_startTransition` future
 * flag), so after `setSearchParams` the router's own `params` keep describing the OLD URL
 * for a render or more. Two things broke on that: a text input bound to `?q` lost
 * keystrokes (typing "binary search tree" quickly left `q=e`), and two writes in the same
 * tick raced, the second erasing the first.
 *
 * This keeps its own `latest` copy, advanced synchronously on every write and re-rendered
 * at once. The router's URL is only consulted once COMMITTED (a layout effect): under
 * concurrent rendering an interrupted transition render sees the new URL and the urgent
 * keystroke render after it sees the old one again, so reading it during render mistook
 * our own lag for someone else's navigation. A committed URL is either one of our pending
 * writes (ignored, however stale) or an external navigation - Back, a link - which
 * replaces `latest`. Writes are `replace`, so none of them adds a history entry.
 */
export default function useLatestSearchParams() {
  const [routerParams, setRouterParams] = useSearchParams();
  const latest = useRef(routerParams);
  const pending = useRef([]);
  const committed = useRef(routerParams.toString());
  const [, rerender] = useReducer((n) => n + 1, 0);
  const setRef = useRef(setRouterParams);
  setRef.current = setRouterParams;

  useLayoutEffect(() => {
    const reported = routerParams.toString();
    if (reported === committed.current) return;
    committed.current = reported;
    const ours = pending.current.indexOf(reported);
    if (ours >= 0) {
      pending.current = pending.current.slice(ours + 1);
      return;
    }
    pending.current = [];
    if (reported !== latest.current.toString()) {
      latest.current = routerParams;
      rerender();
    }
  }, [routerParams]);

  const update = useCallback((mutate) => {
    const next = new URLSearchParams(latest.current);
    mutate(next);
    const text = next.toString();
    if (text === latest.current.toString()) return;
    latest.current = next;
    pending.current.push(text);
    rerender();
    setRef.current(next, { replace: true });
  }, []);

  return [latest.current, update];
}
