package com.dsa.ui.controller;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.BinarySearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/binarysearch")
public class BinarySearchController {

    private final BinarySearchService binarySearchService;

    public BinarySearchController(BinarySearchService binarySearchService) {
        this.binarySearchService = binarySearchService;
    }

    @GetMapping("/problems")
    public ResponseEntity<List<ProblemDetail>> getAllProblems() {
        return ResponseEntity.ok(binarySearchService.getAllProblems());
    }

    @GetMapping("/problems/{id}")
    public ResponseEntity<ProblemDetail> getProblemById(@PathVariable String id) {
        ProblemDetail problem = binarySearchService.getProblemById(id);
        if (problem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(problem);
    }

    @GetMapping("/execute/{id}")
    public ResponseEntity<List<ExecutionStep>> getExecutionSteps(@PathVariable String id) {
        ProblemDetail problem = binarySearchService.getProblemById(id);
        if (problem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(binarySearchService.generateSteps(id));
    }
}
