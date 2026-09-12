import { afterEach, beforeEach } from 'vitest';

// View preferences persist to localStorage, which is shared across every test in a file.
// Without this, a test that opens the input editor leaves it open for the next one, and
// suites start passing or failing depending on their order. Isolating storage keeps each
// test describing the app's real first-load state.
beforeEach(() => {
  window.localStorage.clear();
  // Almost every test describes a RETURNING visitor - the app doing its job, not being
  // introduced. The first-run guide would otherwise cover the UI in all of them. Tests
  // that are about onboarding remove this key themselves to get a genuine first load.
  window.localStorage.setItem('dsa-ui:seenWelcome', 'true');
});

afterEach(() => {
  window.localStorage.clear();
});
