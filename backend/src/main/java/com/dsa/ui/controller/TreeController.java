package com.dsa.ui.controller;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.TreeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trees")
public class TreeController {

    private final TreeService treeService;

    public TreeController(TreeService treeService) {
        this.treeService = treeService;
    }

    @GetMapping("/problems")
    public ResponseEntity<List<ProblemDetail>> getAllProblems() {
        return ResponseEntity.ok(treeService.getAllProblems());
    }

    @GetMapping("/problems/{id}")
    public ResponseEntity<ProblemDetail> getProblemById(@PathVariable String id) {
        ProblemDetail problem = treeService.getProblemById(id);
        if (problem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(problem);
    }

    @GetMapping("/execute/{id}")
    public ResponseEntity<List<ExecutionStep>> getExecutionSteps(@PathVariable String id) {
        ProblemDetail problem = treeService.getProblemById(id);
        if (problem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(treeService.generateSteps(id));
    }
}
