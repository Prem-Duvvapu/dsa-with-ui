import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Crosshair } from 'lucide-react';
import CodeViewer from '../components/CodeViewer';
import styles from './ProblemWorkspace.module.css';

const MANUAL_KEYS = new Set(['ArrowUp', 'ArrowDown', 'PageUp', 'PageDown', 'Home', 'End', ' ']);

/**
 * The Java source, following execution until the learner takes over.
 *
 * Following is explicit: while on, the active line is kept in view as steps change. Any
 * manual scroll - wheel, touch, or scrolling keys inside the pane - suspends it, and
 * "Return to active line" restores it. Programmatic scrolling never re-enables anything
 * and never moves focus or selection. The scroll position is handed back to the session
 * (`scrollMemory`) on unmount so leaving and re-entering the view keeps the reader's place.
 */
export default function SourcePane({ problem, currentStep, anchors, steps, scrollMemory }) {
  const sourceRef = useRef(null);
  const [following, setFollowing] = useState(scrollMemory.current.following ?? true);
  const activeLine = Number.isInteger(currentStep?.activeLine) ? currentStep.activeLine : null;
  const followingRef = useRef(following);
  followingRef.current = following;

  // Restore where the reader was, once, before the first paint.
  useLayoutEffect(() => {
    const node = sourceRef.current;
    if (node && Number.isFinite(scrollMemory.current.top)) node.scrollTop = scrollMemory.current.top;
    return () => {
      scrollMemory.current = { top: node?.scrollTop ?? 0, following: followingRef.current };
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!following || activeLine === null) return;
    const row = sourceRef.current?.querySelector('[data-active-line="true"]');
    row?.scrollIntoView?.({ block: 'nearest', inline: 'nearest' });
  }, [following, activeLine, currentStep]);

  useEffect(() => {
    const node = sourceRef.current;
    if (!node) return undefined;
    const suspend = () => setFollowing(false);
    const onKey = (event) => { if (MANUAL_KEYS.has(event.key)) suspend(); };
    node.addEventListener('wheel', suspend, { passive: true });
    node.addEventListener('touchmove', suspend, { passive: true });
    node.addEventListener('keydown', onKey);
    return () => {
      node.removeEventListener('wheel', suspend);
      node.removeEventListener('touchmove', suspend);
      node.removeEventListener('keydown', onKey);
    };
  }, []);

  return (
    <div className={styles.sourcePane} data-tour="code-panel">
      <div className={styles.followBar}>
        <span className={styles.meta} role="status">
          {following ? 'Following execution' : 'Not following — you scrolled the source'}
        </span>
        {!following && (
          <button type="button" className={styles.control} onClick={() => setFollowing(true)}>
            <Crosshair size={16} aria-hidden="true" /> Return to active line
          </button>
        )}
      </div>
      <CodeViewer problem={problem} currentStep={currentStep} anchors={anchors} steps={steps} comfortable sourceRef={sourceRef} title="Source · Java" />
    </div>
  );
}
