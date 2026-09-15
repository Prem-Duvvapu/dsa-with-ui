/**
 * A stable, no-server "problem of the day": the same pick for everyone on the same
 * calendar date, without persisting anything or asking the backend to remember a
 * schedule. `dateString` is the caller's, deliberately - this file never calls `new
 * Date()` itself, so it stays trivial to test against any date and immune to timezone
 * skew between "today" as the caller sees it and as this function would have computed it.
 */
function hashString(value) {
  let hash = 0;
  for (let i = 0; i < value.length; i += 1) {
    hash = (hash * 31 + value.charCodeAt(i)) | 0;
  }
  return Math.abs(hash);
}

export function pickDailyProblem(problems, dateString) {
  if (!Array.isArray(problems) || problems.length === 0) return null;
  const index = hashString(dateString) % problems.length;
  return problems[index];
}
