package com.dsa.ui.model;

public class ComplexityDetail {
    private String timeComplexity;
    private String timeExplanation;
    private String timeWhy;

    private String spaceComplexity;
    private String spaceExplanation;
    private String spaceWhy;

    private String auxiliarySpace;
    private String dataStructureSpace;

    public ComplexityDetail() {}

    public ComplexityDetail(String timeComplexity, String timeExplanation, String timeWhy,
                            String spaceComplexity, String spaceExplanation, String spaceWhy,
                            String auxiliarySpace, String dataStructureSpace) {
        this.timeComplexity = timeComplexity;
        this.timeExplanation = timeExplanation;
        this.timeWhy = timeWhy;
        this.spaceComplexity = spaceComplexity;
        this.spaceExplanation = spaceExplanation;
        this.spaceWhy = spaceWhy;
        this.auxiliarySpace = auxiliarySpace;
        this.dataStructureSpace = dataStructureSpace;
    }

    public String getTimeComplexity() { return timeComplexity; }
    public void setTimeComplexity(String timeComplexity) { this.timeComplexity = timeComplexity; }

    public String getTimeExplanation() { return timeExplanation; }
    public void setTimeExplanation(String timeExplanation) { this.timeExplanation = timeExplanation; }

    public String getTimeWhy() { return timeWhy; }
    public void setTimeWhy(String timeWhy) { this.timeWhy = timeWhy; }

    public String getSpaceComplexity() { return spaceComplexity; }
    public void setSpaceComplexity(String spaceComplexity) { this.spaceComplexity = spaceComplexity; }

    public String getSpaceExplanation() { return spaceExplanation; }
    public void setSpaceExplanation(String spaceExplanation) { this.spaceExplanation = spaceExplanation; }

    public String getSpaceWhy() { return spaceWhy; }
    public void setSpaceWhy(String spaceWhy) { this.spaceWhy = spaceWhy; }

    public String getAuxiliarySpace() { return auxiliarySpace; }
    public void setAuxiliarySpace(String auxiliarySpace) { this.auxiliarySpace = auxiliarySpace; }

    public String getDataStructureSpace() { return dataStructureSpace; }
    public void setDataStructureSpace(String dataStructureSpace) { this.dataStructureSpace = dataStructureSpace; }
}
