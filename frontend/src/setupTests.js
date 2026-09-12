import { afterEach, beforeEach } from 'vitest';

// View preferences persist to localStorage, which is shared across every test in a file.
// Without this, a test that opens the input editor leaves it open for the next one, and
// suites start passing or failing depending on their order. Isolating storage keeps each
// test describing the app's real first-load state.
beforeEach(() => {
  window.localStorage.clear();
});

afterEach(() => {
  window.localStorage.clear();
});
