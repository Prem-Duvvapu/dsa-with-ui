import { useCallback, useEffect, useLayoutEffect, useReducer, useRef, useState } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';

/**
 * Search parameters that are always the latest thing this component wrote, plus a counter
 * of the navigations it did NOT write.
 *
 * The router runs navigation inside a React transition (the `v7_startTransition` future
 * flag), so after `setSearchParams` the router's own `params` keep describing the OLD URL
 * for a render or more. A text input bound to `?q` lost keystrokes that way, and two writes
 * in one tick raced. So this keeps its own `latest` copy, advanced synchronously on every
 * write and re-rendered at once.
 *
 * Telling our own writes from someone else's navigation is done by IDENTITY, not by
 * comparing query strings: each write carries a unique token in the location state, and a
 * committed location bearing one of our pending tokens is ours. String equality failed
 * both ways - an external navigation to a value we had once written was mistaken for a
 * stale write of ours, and a navigation back to the value already committed was not seen
 * at all. Only COMMITTED locations are consulted (a layout effect): under concurrent
 * rendering an interrupted transition render and the urgent render after it disagree about
 * the URL, so reading it during render mistook our own lag for a navigation.
 *
 * `navigation` increments on every external navigation - Back/Forward, a link, a different
 * problem - so callers can restore state from a URL someone else produced, and only then.
 * Writes are `replace`, so none of them adds a history entry. After unmount, writes are
 * ignored: a page that has been left no longer owns the URL.
 */

const WRITE_KEY = 'dsaUrlWrite';
const MAX_PENDING = 64;
let instances = 0;

export default function useLatestSearchParams() {
  const [routerParams, setRouterParams] = useSearchParams();
  const location = useLocation();
  const instance = useRef(null);
  if (instance.current === null) instance.current = `w${++instances}`;
  const sequence = useRef(0);
  const latest = useRef(routerParams);
  const pending = useRef([]);
  const committedKey = useRef(location.key);
  const [navigation, setNavigation] = useState(0);
  const [, rerender] = useReducer((n) => n + 1, 0);
  const mounted = useRef(true);
  const setRef = useRef(setRouterParams);
  setRef.current = setRouterParams;
  const stateRef = useRef(location.state);
  stateRef.current = location.state;

  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);

  useLayoutEffect(() => {
    if (location.key === committedKey.current) return;
    committedKey.current = location.key;
    const token = location.state?.[WRITE_KEY];
    const ours = token ? pending.current.indexOf(token) : -1;
    if (ours >= 0) {
      // Our own write landed. Anything written before it is obsolete.
      pending.current = pending.current.slice(ours + 1);
      return;
    }
    pending.current = [];
    latest.current = routerParams;
    setNavigation((n) => n + 1);
  }, [location.key, location.state, routerParams]);

  const update = useCallback((mutate) => {
    if (!mounted.current) return;
    const next = new URLSearchParams(latest.current);
    mutate(next);
    if (next.toString() === latest.current.toString()) return;
    const token = `${instance.current}:${++sequence.current}`;
    latest.current = next;
    pending.current = [...pending.current, token].slice(-MAX_PENDING);
    rerender();
    setRef.current(next, { replace: true, state: { ...(stateRef.current ?? {}), [WRITE_KEY]: token } });
  }, []);

  return [latest.current, update, navigation];
}
