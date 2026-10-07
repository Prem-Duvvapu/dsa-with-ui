import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Crosshair } from 'lucide-react';
import CodeViewer from '../components/CodeViewer';
import styles from './ProblemWorkspace.module.css';

const MANUAL_KEYS = new Set(['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'PageUp', 'PageDown', 'Home', 'End', ' ']);

/**
 * The Java source, following execution until the learner takes over.
 *
 * Following is explicit: while on, the active line is kept in view as steps change. Any
 * manual scroll - wheel, touch, or scrolling keys inside the pane - suspends it, and
 * "Return to active line" restores it. Programmatic scrolling never re-enables anything
 * and never moves focus or selection. The scroll position is handed back to the session
 * (`scrollMemory`, both axes, tagged with the problem) on unmount so leaving and re-entering
 * the view keeps the reader's place.
 */
export default function SourcePane({ problemId, sourceKey = problemId, problem, currentStep, anchors, steps, scrollMemory }) {
  const sourceRef = useRef(null);
  // The memory belongs to one problem. The workspace keys this pane by problem too, so a new
  // problem always starts following, at the top (INDEPENDENT_REVIEW_DB8683B.md S4).
  const remembered = (scrollMemory.current?.sourceKey ?? scrollMemory.current?.problemId) === sourceKey ? scrollMemory.current : null;
  const [following, setFollowing] = useState(remembered?.following ?? true);
  const activeLine = Number.isInteger(currentStep?.activeLine) ? currentStep.activeLine : null;
  const followingRef = useRef(following);
  followingRef.current = following;

  // Restore where the reader was - both axes - before the first paint; hand it back on unmount.
  useLayoutEffect(() => {
    const node = sourceRef.current;
    if (node && remembered) {
      node.scrollTop = remembered.top ?? 0;
      node.scrollLeft = remembered.left ?? 0;
    }
    return () => {
      scrollMemory.current = {
        problemId,
        sourceKey,
        top: node?.scrollTop ?? 0,
        left: node?.scrollLeft ?? 0,
        following: followingRef.current
      };
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!following || activeLine === null) return;
    const row = sourceRef.current?.querySelector('[data-active-line="true"]');
    row?.scrollIntoView?.({ block: 'nearest', inline: 'nearest' });
  }, [following, activeLine, currentStep]);

  // The pane owns its scrolling keys: they scroll the source natively and stop there, so End
  // or Space inside the code never also moves playback (review S2). They also end following.
  useEffect(() => {
    const node = sourceRef.current;
    if (!node) return undefined;
    const suspend = () => setFollowing(false);
    const onKey = (event) => {
      if (!MANUAL_KEYS.has(event.key) || event.target !== node) return;
      event.stopPropagation();
      suspend();
    };
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
