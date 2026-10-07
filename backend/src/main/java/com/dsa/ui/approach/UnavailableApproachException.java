package com.dsa.ui.approach;

import java.util.List;

public final class UnavailableApproachException extends RuntimeException {
    private final String problemId;
    private final String approachId;
    private final List<String> available;

    public UnavailableApproachException(String problemId, String approachId, List<String> available) {
        super("Solution approach '" + approachId + "' is unavailable for '" + problemId + "'.");
        this.problemId = problemId;
        this.approachId = approachId;
        this.available = List.copyOf(available);
    }
    public String getProblemId() { return problemId; }
    public String getApproachId() { return approachId; }
    public List<String> getAvailable() { return available; }
}
