import React, { useEffect, useId, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ArrowRight, Check, Search, SlidersHorizontal, Star, X } from 'lucide-react';
import { useCatalog } from '../catalog/CatalogProvider';
import useProgress from '../hooks/useProgress';
import useLastVisited from '../hooks/useLastVisited';
import useStreak from '../hooks/useStreak';
import useLatestSearchParams from '../hooks/useLatestSearchParams';
import { useProblemSearch } from '../search/useProblemSearch';
import { searchProblems, matchRanges } from '../search/scoreProblem';
import { pickDailyProblem } from '../search/dailyProblem';
import LearningHeader from './LearningHeader';
import layout from './LearningLayout.module.css';
import styles from './AlgorithmLibrary.module.css';

/**
 * The front door: every catalogued problem, searchable, filterable, in the backend's own
 * order. It never executes anything - opening a result is a link, and the workspace runs it.
 *
 * Layout decisions (REFERENCE_DESIGN.md D6/D7): one heading and one sentence, then the
 * search, so the first result is on screen at 1366x768 and the search is reachable without
 * scrolling on a phone. A returning learner's Continue link sits in the progress line
 * rather than only inside the closed "Your learning" disclosure. On a phone the filters
 * fold into a "Filters (N)" disclosure holding the very same controls; active filters are
 * always visible as individually removable chips.
 *
 * URL parameters (`q`, `category`, `difficulty`, `status`, `runnable`, `limit`) are the
 * library's state, so Back returns to the exact search, filters and loaded batch; the
 * scroll position is kept per history entry in sessionStorage and restored once the rows
 * exist. Invalid values normalise to "no filter" rather than an empty list.
 */

const DIFFICULTIES = ['Easy', 'Medium', 'Hard'];
const STATUSES = [
  { value: 'starred', label: 'Starred' },
  { value: 'watched', label: 'Watched' },
  { value: 'unwatched', label: 'Not watched' }
];
const BATCH = 50;
const FILTER_KEYS = ['category', 'difficulty', 'status', 'runnable'];

function MatchedTitle({ title, query }) {
  let cursor = 0;
  const parts = [];
  matchRanges(query, title).forEach(([start, end]) => { parts.push(title.slice(cursor, start), <mark key={start}>{title.slice(start, end)}</mark>); cursor = end; });
  parts.push(title.slice(cursor));
  return <>{parts}</>;
}

function scrollKey(location) {
  return `dsa:library-scroll:${location.key}`;
}

export default function AlgorithmLibrary() {
  const { problems, loading, error, retry, source } = useCatalog();
  const { progress, toggleStar } = useProgress();
  const lastVisitedId = useLastVisited();
  const [params, update] = useLatestSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const searchRef = useRef(null);
  const loadMoreRef = useRef(null);
  const listRef = useRef(null);
  const focusAfterLoad = useRef(null);
  const restored = useRef(false);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const filtersId = useId();
  const today = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const { current: streakDays } = useStreak(today);
  const { recents, commitRecent } = useProblemSearch();

  const query = params.get('q') || '';
  const categories = useMemo(() => [...new Set(problems.map(p => p.category).filter(Boolean))].sort(), [problems]);
  const category = categories.includes(params.get('category')) ? params.get('category') : '';
  const difficulty = DIFFICULTIES.includes(params.get('difficulty')) ? params.get('difficulty') : '';
  const status = STATUSES.some(s => s.value === params.get('status')) ? params.get('status') : '';
  const runnable = params.get('runnable') === 'true';
  const limit = Math.min(10000, Math.max(BATCH, Number(params.get('limit')) || BATCH));

  const results = useMemo(() => (query.trim() ? searchProblems(query, problems) : problems).filter(p =>
    (!category || p.category === category) && (!difficulty || p.difficulty === difficulty) && (!runnable || p.traced === true)
    && (!status || (status === 'starred' ? progress[p.id]?.starred : status === 'watched' ? progress[p.id]?.watched : !progress[p.id]?.watched))),
  [query, problems, category, difficulty, runnable, status, progress]);
  const visible = results.slice(0, limit);
  const continueProblem = problems.find(p => p.id === lastVisitedId);
  const daily = useMemo(() => pickDailyProblem(problems, today), [problems, today]);
  const review = problems.find(p => progress[p.id]?.starred);
  const watchedCount = problems.filter(p => progress[p.id]?.watched).length;

  const chips = [
    category && { key: 'category', label: `Category: ${category}` },
    difficulty && { key: 'difficulty', label: `Difficulty: ${difficulty}` },
    status && { key: 'status', label: `Progress: ${STATUSES.find(s => s.value === status).label}` },
    runnable && { key: 'runnable', label: 'Runnable only' }
  ].filter(Boolean);

  useEffect(() => { document.title = 'Algorithm library · DSA Visualizer'; }, []);
  useEffect(() => {
    const onKey = event => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); searchRef.current?.focus(); }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);
  // Back to this history entry: restore where the learner was, once the rows exist. A fresh
  // visit to `/` has nothing saved under its key and stays at the top.
  useEffect(() => {
    if (loading || restored.current) return;
    restored.current = true;
    let top = 0;
    try { top = Number(sessionStorage.getItem(scrollKey(location))) || 0; } catch { /* optional */ }
    if (top) requestAnimationFrame(() => window.scrollTo(0, top));
  }, [loading, location]);
  // After "Load more" removes itself (everything is loaded), focus the first new row rather
  // than dropping to <body>. While more remain, focus simply stays on the button.
  useEffect(() => {
    const index = focusAfterLoad.current;
    if (index === null) return;
    focusAfterLoad.current = null;
    if (document.activeElement === loadMoreRef.current) return;
    listRef.current?.querySelectorAll('a')[index]?.focus();
  }, [limit]);

  function filter(key, value) {
    update(next => {
      if (value) next.set(key, value); else next.delete(key);
      if (key !== 'limit') next.delete('limit');
    });
  }
  function clearFilters() {
    update(next => { FILTER_KEYS.forEach(key => next.delete(key)); next.delete('limit'); });
  }
  function clearEverything() {
    update(next => [...next.keys()].forEach(key => next.delete(key)));
  }
  function loadMore() {
    focusAfterLoad.current = visible.length;
    filter('limit', String(limit + BATCH));
  }
  function remember() {
    if (query.trim()) commitRecent(query);
    try { sessionStorage.setItem(scrollKey(location), String(window.scrollY)); } catch { /* optional */ }
  }
  function open(id) { remember(); navigate(`/problem/${id}`); }

  const catalogueAlert = error && (
    <div className={layout.alert} role="alert">
      <p>
        {error}
        {source === 'sample' && ' These entries are for browsing only; their traces need the backend.'}
      </p>
      <button type="button" onClick={retry} className={layout.button}>Retry loading the catalogue</button>
    </div>
  );

  return <div className={layout.page}>
    <a className={layout.skip} href="#library-search">Skip to search</a>
    <LearningHeader />
    <main className={layout.width}>
      <div className={styles.intro}>
        <h1>Algorithm library</h1>
        <p className={styles.lead}>Find a problem, run the real Java on your own input, and watch every step.</p>
      </div>

      {loading && <p role="status" className={styles.status}>Loading the catalogue…</p>}
      {catalogueAlert}

      {!loading && problems.length > 0 && <>
        <div className={styles.progress}>
          <p>{watchedCount} of {problems.length} watched{streakDays > 0 && <> · <span>{streakDays}-day streak</span></>}</p>
          {continueProblem && <Link className={styles.continue} to={`/problem/${continueProblem.id}`} onClick={remember}>
            Continue: <strong>{continueProblem.title}</strong> <ArrowRight size={15} aria-hidden="true" />
          </Link>}
        </div>
        <details className={styles.learning}><summary>Your learning <span>Continue, today’s pick, and saved problems</span></summary><section className={styles.resume} aria-label="Your learning">
          <div><p className={layout.eyebrow}>{continueProblem ? 'Continue where you left off' : 'A place to begin'}</p><h2>{continueProblem?.title || 'Start with one small example'}</h2><button type="button" className={styles.textButton} onClick={() => open(continueProblem?.id || problems[0]?.id)}>{continueProblem ? 'Continue' : 'Start with the first problem'} <ArrowRight size={16} /></button></div>
          {daily && <div><p className={layout.eyebrow}>Today’s pick</p><h2>{daily.title}</h2><button type="button" className={styles.textButton} onClick={() => open(daily.id)}>Try today’s problem <ArrowRight size={16} /></button></div>}
          <div><p className={layout.eyebrow}>{review ? 'Your review queue' : 'Your collection'}</p><h2>{review ? 'Keep a good question close.' : 'Save what sparks a question.'}</h2>{review ? <button type="button" className={styles.textButton} onClick={() => open(review.id)}>Review {review.title} <ArrowRight size={16} /></button> : <p className={layout.muted}>Star any algorithm below to come back to it.</p>}</div>
        </section></details>
      </>}

      <section id="algorithm-library" className={styles.library} aria-label="Algorithm library">
        <div className={styles.search}>
          <Search size={20} aria-hidden="true" />
          <input id="library-search" ref={searchRef} aria-label="Search algorithms" placeholder="Try binary search, sliding window, trees…" value={query} onChange={event => filter('q', event.target.value)} />
          {query
            ? <button type="button" className={styles.clearSearch} onClick={() => { filter('q', ''); searchRef.current?.focus(); }} aria-label="Clear search"><X size={18} /></button>
            : <kbd>⌘ / Ctrl K</kbd>}
        </div>

        <div className={styles.toolbar}>
          <button type="button" className={styles.filtersToggle} aria-expanded={filtersOpen} aria-controls={filtersId} onClick={() => setFiltersOpen(open => !open)} data-audit="filters-toggle">
            <SlidersHorizontal size={16} aria-hidden="true" /> Filters{chips.length > 0 ? ` (${chips.length})` : ''}
          </button>
          <p className={styles.resultCount} role="status">{loading ? 'Loading algorithms…' : `${results.length} ${results.length === 1 ? 'algorithm' : 'algorithms'}`}</p>
        </div>

        <div id={filtersId} className={styles.filters} data-open={filtersOpen}>
          <label>Category<select value={category} onChange={event => filter('category', event.target.value)}><option value="">All categories</option>{categories.map(name => <option key={name}>{name}</option>)}</select></label>
          <label>Difficulty<select value={difficulty} onChange={event => filter('difficulty', event.target.value)}><option value="">All levels</option>{DIFFICULTIES.map(name => <option key={name}>{name}</option>)}</select></label>
          <label>Progress<select value={status} onChange={event => filter('status', event.target.value)}><option value="">All problems</option>{STATUSES.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}</select></label>
          <label className={styles.checkbox}><input type="checkbox" checked={runnable} onChange={event => filter('runnable', event.target.checked ? 'true' : '')} />Runnable only</label>
          {chips.length > 0 && <button type="button" className={styles.textButton} onClick={clearFilters}>Clear filters</button>}
        </div>

        {chips.length > 0 && <ul className={styles.chips} aria-label="Active filters">
          {chips.map(chip => <li key={chip.key}><button type="button" onClick={() => filter(chip.key, '')} aria-label={`Remove filter ${chip.label}`}>{chip.label}<X size={14} aria-hidden="true" /></button></li>)}
        </ul>}

        {!query && recents.length > 0 && <div className={styles.recents}><span>Recent searches</span>{recents.map(recent => <button type="button" key={recent} onClick={() => filter('q', recent)}>{recent}</button>)}</div>}

        <h2 className="sr-only">Results</h2>
        {!loading && <ul ref={listRef} className={styles.results}>{visible.map((problem, index) => <li key={problem.id} data-audit="result-row">
          <span className={styles.number} aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
          <Link to={`/problem/${problem.id}`} onClick={remember} className={styles.problemLink}><strong><MatchedTitle title={problem.title} query={query} /></strong><span>{problem.category} · {problem.dsType}{problem.traced === false ? ' · Not yet traced' : ''}</span></Link>
          <span className={styles.difficulty}>{problem.difficulty || 'Unrated'}</span>
          <span className={styles.watched}>{progress[problem.id]?.watched && <Check size={16} aria-label="Watched" />}</span>
          <button type="button" className={styles.star} aria-pressed={Boolean(progress[problem.id]?.starred)} aria-label={`${progress[problem.id]?.starred ? 'Unstar' : 'Star'} ${problem.title}`} onClick={() => toggleStar(problem.id)}><Star size={18} fill={progress[problem.id]?.starred ? 'currentColor' : 'none'} /></button>
        </li>)}</ul>}

        {!loading && results.length === 0 && <div className={styles.empty}>
          <h3>{problems.length ? 'No algorithms match these filters.' : 'The catalogue is empty.'}</h3>
          {problems.length > 0 && <>
            <p>Try another search, or clear the search and filters to see every algorithm.</p>
            <button className={layout.button} type="button" onClick={clearEverything}>Clear search and filters</button>
          </>}
        </div>}

        {!loading && results.length > 0 && <div className={styles.more}>
          <span role="status">Showing {visible.length} of {results.length}</span>
          {visible.length < results.length && <button ref={loadMoreRef} type="button" className={layout.button} onClick={loadMore}>Load more algorithms <ArrowRight size={16} /></button>}
        </div>}
      </section>
      <footer className={styles.footer}>Understand the change. Follow the reasoning. Make it your own.</footer>
    </main>
  </div>;
}
