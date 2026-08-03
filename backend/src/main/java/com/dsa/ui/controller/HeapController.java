package com.dsa.ui.controller;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.HeapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/heaps")
@CrossOrigin(origins = "*")
public class HeapController {

    private final HeapService service;

    @Autowired
    public HeapController(HeapService service) {
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
    public ResponseEntity<List<ExecutionStep>> executeProblem(@PathVariable String id) {
        List<ExecutionStep> steps = service.generateSteps(id);
        return ResponseEntity.ok(steps);
    }
}
