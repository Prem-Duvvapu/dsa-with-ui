package com.dsa.ui.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Every statement file under {@code resources/statements/}, keyed by problem id. A malformed
 * or misnamed file fails application startup, the same way TracerRegistry treats a broken
 * tracer: a statement that silently failed to load would look like a problem with none.
 */
@Component
public class StatementCatalog {

    private final Map<String, ProblemStatement> statements;

    public StatementCatalog(ObjectMapper mapper) throws IOException {
        Map<String, ProblemStatement> loaded = new LinkedHashMap<>();
        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath*:statements/*.json");
        for (Resource file : files) {
            try (InputStream in = file.getInputStream()) {
                ProblemStatement statement = mapper.readValue(in, ProblemStatement.class);
                String expected = file.getFilename() == null ? "" : file.getFilename().replaceFirst("\\.json$", "");
                if (statement.id() == null || !statement.id().equals(expected)) {
                    throw new IllegalStateException("Statement file " + file.getFilename()
                            + " declares id '" + statement.id() + "'; the file must be named after its id");
                }
                if (loaded.put(statement.id(), statement) != null) {
                    throw new IllegalStateException("Duplicate statement for " + statement.id());
                }
            }
        }
        this.statements = Collections.unmodifiableMap(loaded);
    }

    public Optional<ProblemStatement> find(String id) {
        return Optional.ofNullable(statements.get(id));
    }

    public Map<String, ProblemStatement> all() {
        return statements;
    }
}
