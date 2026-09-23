import React, { useEffect, useMemo, useRef } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowRight, Check, Search, Star } from 'lucide-react';
import { useCatalog } from '../catalog/CatalogProvider';
import useProgress from '../hooks/useProgress';
import useLastVisited from '../hooks/useLastVisited';
import useStreak from '../hooks/useStreak';
import { useProblemSearch } from '../search/useProblemSearch';
import { searchProblems, matchRanges } from '../search/scoreProblem';
import { pickDailyProblem } from '../search/dailyProblem';
import LearningHeader from './LearningHeader';
import layout from './LearningLayout.module.css';
import styles from './AlgorithmLibrary.module.css';

function MatchedTitle({ title, query }) {
  let cursor = 0;
  const parts = [];
  matchRanges(query, title).forEach(([start, end]) => { parts.push(title.slice(cursor, start), <mark key={start}>{title.slice(start, end)}</mark>); cursor = end; });
  parts.push(title.slice(cursor));
  return <>{parts}</>;
}

export default function AlgorithmLibrary() {
  const { problems, loading, error, retry } = useCatalog();
  const { progress, toggleStar } = useProgress();
  const lastVisitedId = useLastVisited();
  const [params, setParams] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const searchRef = useRef(null);
  const restored = useRef(false);
  const today = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const { current: streakDays } = useStreak(today);
  const { recents, commitRecent } = useProblemSearch();
  const query = params.get('q') || '';
  const categories = useMemo(() => [...new Set(problems.map(p => p.category).filter(Boolean))].sort(), [problems]);
  const category = categories.includes(params.get('category')) ? params.get('category') : '';
  const difficulty = ['Easy', 'Medium', 'Hard'].includes(params.get('difficulty')) ? params.get('difficulty') : '';
  const status = ['starred', 'watched', 'unwatched'].includes(params.get('status')) ? params.get('status') : '';
  const runnable = params.get('runnable') === 'true';
  const limit = Math.min(10000, Math.max(50, Number(params.get('limit')) || 50));
  const results = useMemo(() => (query.trim() ? searchProblems(query, problems) : problems).filter(p =>
    (!category || p.category === category) && (!difficulty || p.difficulty === difficulty) && (!runnable || p.traced === true)
    && (!status || (status === 'starred' ? progress[p.id]?.starred : status === 'watched' ? progress[p.id]?.watched : !progress[p.id]?.watched))),
  [query, problems, category, difficulty, runnable, status, progress]);
  const visible = results.slice(0, limit);
  const continueProblem = problems.find(p => p.id === lastVisitedId);
  const daily = useMemo(() => pickDailyProblem(problems, today), [problems, today]);
  const review = problems.find(p => progress[p.id]?.starred);
  const watchedCount = problems.filter(p => progress[p.id]?.watched).length;

  useEffect(() => { document.title = 'Algorithm library · DSA Visualizer'; }, []);
  useEffect(() => {
    const onKey = event => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); searchRef.current?.focus(); }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);
  useEffect(() => {
    if (loading || restored.current) return;
    restored.current = true;
    let top = 0;
    try { top = Number(sessionStorage.getItem(`dsa:library-scroll:${location.key}`)) || 0; } catch { /* optional */ }
    if (top) requestAnimationFrame(() => window.scrollTo(0, top));
  }, [loading, location.key]);

  function filter(key, value) {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value); else next.delete(key);
    if (key !== 'limit') next.delete('limit');
    setParams(next, { replace: true });
  }
  function remember() {
    if (query.trim()) commitRecent(query);
    try { sessionStorage.setItem(`dsa:library-scroll:${location.key}`, String(window.scrollY)); } catch { /* optional */ }
  }
  function open(id) { remember(); navigate(`/problem/${id}`); }

  return <div className={layout.page}>
    <a className={layout.skip} href="#algorithm-library">Skip to algorithms</a>
    <LearningHeader><span className={styles.headerNote}>A little curiosity. One step at a time.</span></LearningHeader>
    <main className={layout.width}>
      <section className={styles.intro}>
        <div><p className={layout.eyebrow}>THE ALGORITHM LIBRARY</p><h1>Build intuition. <span>Watch it happen.</span></h1><p className={styles.lead}>Explore the structures. Follow the Java. Change an input and see exactly why the result changes.</p><button className={`${layout.button} ${layout.primary}`} type="button" onClick={() => { document.getElementById('algorithm-library')?.scrollIntoView({ block: 'start' }); searchRef.current?.focus({ preventScroll: true }); }}>Browse all problems <ArrowRight size={17} /></button></div>
        <div className={styles.idea} aria-label="A learning path: input, execution, understanding"><span>01 / INPUT</span><div className={styles.cells}>{[2, 7, 11, 15].map(value => <b key={value}>{value}</b>)}</div><div className={styles.ideaLine} /><span>02 / FOLLOW THE EXECUTION</span><p>One change. A clearer mental model.</p><span>03 / EXPLAIN THE RESULT</span></div>
      </section>
      {loading && <p role="status" className={styles.status}>Loading the catalogue…</p>}
      {error && <div className={layout.alert} role="alert"><p>{error}</p><button type="button" onClick={retry} className={layout.button}>Retry</button></div>}
      {!loading && problems.length > 0 && <>
        <div className={styles.progress}><p>{watchedCount} of {problems.length} problems watched</p>{streakDays > 0 && <span>{streakDays}-day streak</span>}<span>Explore at your own pace</span></div>
        <details className={styles.learning}><summary>Your learning <span>Continue, today’s pick, and saved problems</span></summary><section className={styles.resume} aria-label="Your learning">
          <div><p className={layout.eyebrow}>{continueProblem ? 'Continue where you left off' : 'A place to begin'}</p><h2>{continueProblem?.title || 'Start with one small example'}</h2><button type="button" className={styles.textButton} onClick={() => open(continueProblem?.id || problems[0]?.id || 'two-sum')}>{continueProblem ? 'Continue' : 'Start with the first problem'} <ArrowRight size={16} /></button></div>
          {daily && <div><p className={layout.eyebrow}>Today’s pick</p><h2>{daily.title}</h2><button type="button" className={styles.textButton} onClick={() => open(daily.id)}>Try today’s problem <ArrowRight size={16} /></button></div>}
          <div><p className={layout.eyebrow}>{review ? 'Your review queue' : 'Your collection'}</p><h2>{review ? 'Keep a good question close.' : 'Save what sparks a question.'}</h2>{review ? <button type="button" className={styles.textButton} onClick={() => open(review.id)}>Review {review.title} <ArrowRight size={16} /></button> : <p className={layout.muted}>Star any algorithm below to come back to it.</p>}</div>
        </section></details>
      </>}
      <section id="algorithm-library" className={styles.library} aria-label="Algorithm library">
        <div className={styles.libraryHeading}><h2>All algorithms</h2><p className={layout.muted}>Search a name, a technique, or a data structure.</p></div>
        <div className={styles.search}><Search size={21} /><input ref={searchRef} aria-label="Search algorithms" placeholder="Try binary search, sliding window, trees…" value={query} onChange={event => filter('q', event.target.value)} /><kbd>⌘ / Ctrl K</kbd></div>
        <div className={styles.filters}>
          <label>Category<select value={category} onChange={event => filter('category', event.target.value)}><option value="">All categories</option>{categories.map(name => <option key={name}>{name}</option>)}</select></label>
          <label>Difficulty<select value={difficulty} onChange={event => filter('difficulty', event.target.value)}><option value="">All levels</option>{['Easy', 'Medium', 'Hard'].map(name => <option key={name}>{name}</option>)}</select></label>
          <label>Progress<select value={status} onChange={event => filter('status', event.target.value)}><option value="">All problems</option><option value="starred">Starred</option><option value="watched">Watched</option><option value="unwatched">Not watched</option></select></label>
          <label className={styles.checkbox}><input type="checkbox" checked={runnable} onChange={event => filter('runnable', event.target.checked ? 'true' : '')} />Runnable only</label>
          {(query || category || difficulty || status || runnable) && <button type="button" className={styles.textButton} onClick={() => setParams({}, { replace: true })}>Clear filters</button>}
        </div>
        {!query && recents.length > 0 && <div className={styles.recents}><span>Recent searches</span>{recents.map(recent => <button type="button" key={recent} onClick={() => filter('q', recent)}>{recent}</button>)}</div>}
        <p className={styles.resultCount} role="status">{loading ? 'Loading algorithms…' : `${results.length} ${results.length === 1 ? 'algorithm' : 'algorithms'}${category ? ` in ${category}` : ''}`}</p>
        {!loading && <ul className={styles.results}>{visible.map((problem, index) => <li key={problem.id}>
          <span className={styles.number}>{String(index + 1).padStart(2, '0')}</span>
          <Link to={`/problem/${problem.id}`} onClick={remember} className={styles.problemLink}><strong><MatchedTitle title={problem.title} query={query} /></strong><span>{problem.category} · {problem.dsType}{problem.traced === false ? ' · Not yet traced' : ''}</span></Link>
          <span className={styles.difficulty}>{problem.difficulty || 'Explore'}</span><span className={styles.watched}>{progress[problem.id]?.watched && <Check size={16} aria-label="Watched" />}</span>
          <button type="button" className={styles.star} aria-pressed={Boolean(progress[problem.id]?.starred)} aria-label={`${progress[problem.id]?.starred ? 'Unstar' : 'Star'} ${problem.title}`} onClick={() => toggleStar(problem.id)}><Star size={18} fill={progress[problem.id]?.starred ? 'currentColor' : 'none'} /></button>
        </li>)}</ul>}
        {!loading && results.length === 0 && <div className={styles.empty}><h3>{problems.length ? 'No algorithms match these filters.' : 'The catalogue is empty.'}</h3><p>Try another search or clear the filters to explore the library.</p><button className={layout.button} type="button" onClick={() => setParams({}, { replace: true })}>Clear filters</button></div>}
        {!loading && results.length > 0 && <div className={styles.more}><span>Showing {visible.length} of {results.length}</span>{visible.length < results.length && <button type="button" className={layout.button} onClick={() => filter('limit', String(limit + 50))}>Load more algorithms <ArrowRight size={16} /></button>}</div>}
      </section>
      <footer className={styles.footer}>Understand the change. Follow the reasoning. Make it your own.</footer>
    </main>
  </div>;
}
