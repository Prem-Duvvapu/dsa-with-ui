package com.dsa.ui.catalog;

import com.dsa.ui.model.ProblemDetail;

import java.util.List;

/**
 * A source of catalogue metadata.
 *
 * <p>The eighteen domain services already exposed exactly these two methods; declaring
 * the interface lets {@link ProblemCatalog} collect them as one list instead of naming
 * each service, and gives Phase 4 a seam to move problems between services without
 * touching the API layer.
 */
public interface ProblemProvider {

    List<ProblemDetail> getAllProblems();

    ProblemDetail getProblemById(String id);
}
