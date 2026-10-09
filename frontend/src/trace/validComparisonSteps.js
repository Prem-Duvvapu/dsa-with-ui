/** ExecutionStep debug values are Map<String, String>; never stringify malformed answers
 * or counters into apparent agreement. Missing/null variables remain uninstrumented.
 */
export function validComparisonSteps(steps) {
  return steps.length > 0 && steps.every((step, index) => step?.stepNumber === index + 1
    && typeof step.description === 'string' && step.description.trim()
    && (step.variables == null || typeof step.variables === 'object'
      && !Array.isArray(step.variables)
      && Object.values(step.variables).every(value => value == null || typeof value === 'string')));
}
