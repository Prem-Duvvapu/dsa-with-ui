package com.dsa.ui.controller;

import com.dsa.ui.model.ExecutionStep;
import com.dsa.ui.model.ProblemDetail;
import com.dsa.ui.service.LinkedListService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/linkedlist")
public class LinkedListController {

    private final LinkedListService linkedListService;

    public LinkedListController(LinkedListService linkedListService) {
        this.linkedListService = linkedListService;
    }

    @GetMapping("/problems")
    public ResponseEntity<List<ProblemDetail>> getAllProblems() {
        return ResponseEntity.ok(linkedListService.getAllProblems());
    }

    @GetMapping("/problems/{id}")
    public ResponseEntity<ProblemDetail> getProblemById(@PathVariable String id) {
        ProblemDetail problem = linkedListService.getProblemById(id);
        if (problem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(problem);
    }

    @GetMapping("/execute/{id}")
    public ResponseEntity<List<ExecutionStep>> getExecutionSteps(@PathVariable String id) {
        ProblemDetail problem = linkedListService.getProblemById(id);
        if (problem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(linkedListService.generateSteps(id));
    }
}
