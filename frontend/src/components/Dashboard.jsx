import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, RefreshCw, Compass } from 'lucide-react';
import useProgress from '../hooks/useProgress';
import useLastVisited from '../hooks/useLastVisited';
import styles from './Dashboard.module.css';

const BROWSE_FALLBACK_ID = 'two-sum';

/**
 * The landing page at `/`. Previously `/` was an immediate redirect straight into
 * two-sum, so every visitor - first time or the hundredth - saw the same problem with no
 * sense of where they'd left off. This is the one thing that fixes that: it reads
 * useLastVisited's read-only side (never writing it - only App, on an actual problem
 * route, does that) and offers a continue link if there is one, or a plain start link
 * if there is not.
 *
 * Kept deliberately thin: it fetches the catalogue only for a title and a count, and
 * hands off to the existing problem page - fully featured sidebar, search, category grid
 * - for everything past that first click, rather than rebuilding any of it here.
 */
export default function Dashboard() {
  const navigate = useNavigate();
  const [problems, setProblems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const { watchedCount } = useProgress();
  const lastVisitedId = useLastVisited();

  const fetchCatalog = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch('/api/problems');
      if (!response.ok) throw new Error(`Catalogue fetch failed: ${response.status}`);
      const data = await response.json();
      setProblems(Array.isArray(data) ? data : []);
    } catch (err) {
      console.warn('Backend connection failed:', err);
      setError('Could not reach the backend. Try again once it is running.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchCatalog(); }, [fetchCatalog]);

  const continueProblem = problems.find((p) => p.id === lastVisitedId) || null;
  const startId = problems[0]?.id || BROWSE_FALLBACK_ID;
  const browseId = continueProblem?.id || startId;

  return (
    <div className={styles.page}>
      <div className={`glass-panel ${styles.card}`}>
        <p className={styles.eyebrow}>DSA Visualizer</p>
        <h1 className={styles.title}>
          {continueProblem ? 'Welcome back' : 'Watch every algorithm actually run'}
        </h1>

        {loading && <p className={styles.status}>Loading the catalogue…</p>}

        {error && (
          <div className={styles.errorBox} role="alert">
            <p>{error}</p>
            <button type="button" onClick={fetchCatalog} className="btn btn-outline">
              <RefreshCw size={13} /> Retry
            </button>
          </div>
        )}

        {!loading && !error && (
          <>
            {problems.length > 0 && (
              <p className={styles.progressLine}>
                {watchedCount} of {problems.length} problems watched
              </p>
            )}

            {continueProblem ? (
              <div className={styles.continueBlock}>
                <p className={styles.continueLabel}>Continue where you left off</p>
                <p className={styles.continueTitle}>{continueProblem.title}</p>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => navigate(`/problem/${continueProblem.id}`)}
                >
                  Continue <ArrowRight size={14} />
                </button>
              </div>
            ) : (
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => navigate(`/problem/${startId}`)}
              >
                Start with the first problem <ArrowRight size={14} />
              </button>
            )}

            <button
              type="button"
              className={`btn btn-outline ${styles.browseBtn}`}
              onClick={() => navigate(`/problem/${browseId}`)}
            >
              <Compass size={14} /> Browse all problems
            </button>
          </>
        )}
      </div>
    </div>
  );
}
