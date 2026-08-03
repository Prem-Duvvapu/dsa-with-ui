package com.dsa.ui.controller;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.DpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dp")
public class DpController {

    private final DpService dpService;

    public DpController(DpService dpService) {
        this.dpService = dpService;
    }

    @GetMapping("/problems")
    public ResponseEntity<List<ProblemDetail>> getAllProblems() {
        return ResponseEntity.ok(dpService.getAllProblems());
    }

    @GetMapping("/problems/{id}")
    public ResponseEntity<ProblemDetail> getProblemById(@PathVariable String id) {
        ProblemDetail problem = dpService.getProblemById(id);
        if (problem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(problem);
    }

    @GetMapping("/execute/{id}")
    public ResponseEntity<List<ExecutionStep>> getExecutionSteps(@PathVariable String id) {
        ProblemDetail problem = dpService.getProblemById(id);
        if (problem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dpService.generateSteps(id));
    }
}
