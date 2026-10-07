package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.TracerRegistry;
import java.util.List;

/** Only providers are Spring beans; alternative tracers stay out of TracerRegistry. */
public interface SolutionApproachProvider {
    List<SolutionApproach> approaches(TracerRegistry canonical, ProblemCatalog catalog);
}
