/**
 * The problems either side of this one in its authored curriculum section, in the order
 * the backend serves the catalogue (see SectionNav for why that order is the curriculum).
 */
export function curriculumNeighbours(problems, problemId) {
  const active = problems.find((p) => p.id === problemId);
  const section = active?.striverSheetSection;
  if (!section) return null;
  const ordered = problems.filter((p) => p.striverSheetSection === section);
  const index = ordered.findIndex((p) => p.id === problemId);
  if (ordered.length <= 1 || index < 0) return null;
  return {
    section,
    position: index + 1,
    total: ordered.length,
    previous: index > 0 ? ordered[index - 1] : null,
    next: index < ordered.length - 1 ? ordered[index + 1] : null
  };
}
