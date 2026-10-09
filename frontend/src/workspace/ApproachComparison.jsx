import React, { useId, useState } from 'react';
import useApproachComparison from '../hooks/useApproachComparison';
import InputSummary from '../components/InputSummary';
import styles from './ApproachComparison.module.css';

const OWNED_KEYS = new Set(['ArrowLeft', 'ArrowRight', 'Home', 'End', ' ', 'j', 'k', 'l', 'J', 'K', 'L', ',', '.', 'r', 'R', '[', ']']);
const COUNTERS = [['calls', 'Function calls'], ['stateEvaluations', 'State evaluations'], ['computedStates', 'Distinct computed states'], ['cacheHits', 'Cache hits']];
function limits(option) {
  return (option.inputSpec?.fields ?? []).map(field => {
    const c = field.constraints ?? {};
    const bounds = [];
    if (c.min != null && c.max != null) bounds.push(`${c.min}–${c.max}`);
    if (c.minLength != null && c.maxLength != null) bounds.push(`length ${c.minLength}–${c.maxLength}`);
    if (c.minValue != null && c.maxValue != null) bounds.push(`values ${c.minValue}–${c.maxValue}`);
    return `${field.label ?? field.name} ${bounds.length ? bounds.join(', ') : 'see input constraints'}`;
  }).join('; ');
}
function Side({ option, result }) {
  const [position, setPosition] = useState(0);
  const run = result?.run;
  const index = Math.min(position, Math.max(0, (run?.steps.length ?? 0) - 1));
  const final = run?.steps.at(-1)?.variables ?? {};
  const completeAnswer = run && !run.truncated && final.answer != null;
  return <section className={styles.side} aria-label={`${option.label} comparison`}>
    <h3>{option.label}</h3>
    {result?.error && <p role="alert">Could not run: {result.error}</p>}
    {run && <>
      <p>{run.truncated ? 'Incomplete: this trace was cut short.' : 'Trace returned without truncation.'}</p>
      <dl className={styles.metrics}>
        <dt>Recorded answer</dt><dd>{completeAnswer ? String(final.answer) : 'Unavailable for this incomplete or uninstrumented run'}</dd>
        {COUNTERS.filter(([key]) => final[key] != null).map(([key, label]) => <React.Fragment key={key}>
          <dt>{label}</dt><dd>{String(final[key])}</dd>
        </React.Fragment>)}
        <dt>Trace events</dt><dd>{run.steps.length}</dd>
        {run.responseJsonBytes != null && <><dt>Response JSON (UTF-8)</dt><dd>{run.responseJsonBytes} bytes</dd></>}
        <dt>Theoretical time</dt><dd>{run.complexity?.timeComplexity ?? 'Unavailable'}</dd>
        <dt>Theoretical space</dt><dd>{run.complexity?.spaceComplexity ?? 'Unavailable'}</dd>
      </dl>
      {!COUNTERS.some(([key]) => final[key] != null) && <p>Algorithm counters are not instrumented for this run.</p>}
      <label className={styles.position}>{option.label}: step {index + 1} of {run.steps.length}
        <input type="range" min="1" max={run.steps.length} value={index + 1}
          aria-label={`${option.label}: step`} aria-valuetext={`Step ${index + 1} of ${run.steps.length}`}
          onChange={e => setPosition(Number(e.target.value) - 1)} />
      </label>
      <p>{run.steps[index].description}</p>
    </>}
  </section>;
}

/** At most two text summaries, no additional primary canvases or ordinal synchronization. */
export default function ApproachComparison({ problemId, run, approaches, onPrepare }) {
  const id = useId();
  const [pair, setPair] = useState([run.approachId, approaches.find(a => a.id !== run.approachId)?.id]);
  const { pending, results, compare, clear } = useApproachComparison(problemId);
  const options = pair.map(key => approaches.find(a => a.id === key));
  if (options.some(a => !a)) return null;
  const change = (index, value) => { clear(); setPair(current => current.map((key, i) => i === index ? value : key)); };
  const answers = results?.map(r => r.run && !r.run.truncated ? r.run.steps.at(-1)?.variables?.answer : null);
  const comparable = answers?.every(a => a != null);
  return <div className={styles.wrap} onKeyDown={e => {
    if (!e.ctrlKey && !e.metaKey && !e.altKey && OWNED_KEYS.has(e.key)) e.stopPropagation();
  }}>
    <p>Same recorded input for both approaches; your draft and the main run are unchanged.</p>
    <InputSummary resolvedInput={run.resolvedInput} label="Comparison input" comfortable />
    <div className={styles.setup}>
      {options.map((option, index) => <div key={index} className={styles.field}>
        <label htmlFor={`${id}-${index}`}>Approach {index + 1}</label>
        <select id={`${id}-${index}`} value={option.id} disabled={pending} onChange={e => change(index, e.target.value)}>
          {approaches.map(a => <option key={a.id} value={a.id} disabled={a.id === pair[1 - index]}>{a.label}</option>)}
        </select>
      </div>)}
      <button className={styles.control} type="button" aria-disabled={pending} aria-busy={pending}
        onClick={() => { if (!pending) { onPrepare(); compare(pair, run.resolvedInput); } }}>
        {pending ? 'Comparing…' : 'Run comparison'}
      </button>
    </div>
    {options.map(option => <p key={option.id} className={styles.scope}>{option.label}: {limits(option)} (visualizer limits). No input is silently shortened.</p>)}
    <p className={styles.scope}>Read each run independently. Step numbers are not matching moments. Trace events and JSON size are not runtime measurements; JSON size is before transport compression. Absent counters are not zero.</p>
    {pending && <p role="status">Running both approaches on the recorded input…</p>}
    {comparable && <p role="status">{String(answers[0]) === String(answers[1]) ? 'Both complete runs recorded the same answer.' : 'The recorded answers differ; inspect the runs.'}</p>}
    {results && <div className={styles.sides}>{options.map((option, i) => <Side key={`${option.id}:${results[i].run ? 'run' : 'error'}`} option={option} result={results[i]} />)}</div>}
  </div>;
}
