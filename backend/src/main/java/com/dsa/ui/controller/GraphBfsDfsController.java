package com.dsa.ui.controller;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.GraphBfsDfsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graphs/bfs-dfs")
public class GraphBfsDfsController {

    private final GraphBfsDfsService service;

    public GraphBfsDfsController(GraphBfsDfsService service) {
        this.service = service;
    }

    @GetMapping("/problems")
    public ResponseEntity<List<ProblemDetail>> getAllProblems() {
        return ResponseEntity.ok(service.getAllProblems());
    }

    @GetMapping("/problems/{id}")
    public ResponseEntity<ProblemDetail> getProblemById(@PathVariable String id) {
        ProblemDetail problem = service.getProblemById(id);
        if (problem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(problem);
    }

    @GetMapping("/execute/{id}")
    public ResponseEntity<List<ExecutionStep>> getExecutionSteps(@PathVariable String id) {
        ProblemDetail problem = service.getProblemById(id);
        if (problem == null) {
            return ResponseEntity.notFound().build();
        }
        List<ExecutionStep> steps = service.generateSteps(id);
        return ResponseEntity.ok(steps);
    }
}
