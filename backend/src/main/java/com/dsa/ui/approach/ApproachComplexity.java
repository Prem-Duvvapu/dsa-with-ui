package com.dsa.ui.approach;

import com.dsa.ui.model.ComplexityDetail;

/** Immutable complexity snapshot; selection must never mutate catalogue metadata. */
public record ApproachComplexity(String timeComplexity, String timeExplanation, String timeWhy,
                                 String spaceComplexity, String spaceExplanation, String spaceWhy,
                                 String auxiliarySpace, String dataStructureSpace) {
    public static ApproachComplexity from(ComplexityDetail value) {
        return value == null ? null : new ApproachComplexity(value.getTimeComplexity(),
                value.getTimeExplanation(), value.getTimeWhy(), value.getSpaceComplexity(),
                value.getSpaceExplanation(), value.getSpaceWhy(), value.getAuxiliarySpace(),
                value.getDataStructureSpace());
    }
}
