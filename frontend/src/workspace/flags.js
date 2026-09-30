/**
 * The new problem workspace stays behind this switch until its reference slice passes the
 * P4 gate (IMPLEMENTATION_HANDOFF.md §9) - it is not advertised or linked anywhere. Turn it
 * on for a browser with:  localStorage.setItem('dsa-ui:workspace', '"next"')
 */
export function isNextWorkspaceEnabled() {
  try {
    return JSON.parse(window.localStorage.getItem('dsa-ui:workspace')) === 'next';
  } catch {
    return false;
  }
}
