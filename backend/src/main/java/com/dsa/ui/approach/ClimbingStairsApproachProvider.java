package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.TracerRegistry;
import org.springframework.stereotype.Component;
import java.util.List;

/** D1 audits the existing default; D2 will add real alternative executables. */
@Component
public final class ClimbingStairsApproachProvider implements SolutionApproachProvider {
    @Override public List<SolutionApproach> approaches(TracerRegistry canonical, ProblemCatalog catalog) {
        String id = "climbing-stairs";
        return List.of(new SolutionApproach(id, "tabulation", "Tabulation",
                "Fill each stair's count from the two earlier stairs.", true,
                ApproachComplexity.from(catalog.find(id).orElseThrow().getProblem().getComplexity()),
                canonical.find(id).orElseThrow()));
    }
}
