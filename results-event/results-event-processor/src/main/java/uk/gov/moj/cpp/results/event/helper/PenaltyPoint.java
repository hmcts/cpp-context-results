package uk.gov.moj.cpp.results.event.helper;

import uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt;

import java.math.BigDecimal;

public class PenaltyPoint {

    private static final String PENALTY_POINT = "PENPT";

    public BigDecimal getPenaltyPointFromResults(final Prompt promptDefinition, final uk.gov.justice.sjp.results.Prompts prompt){

        return  promptDefinition.getId().equals(prompt.getId()) && PENALTY_POINT.equalsIgnoreCase(promptDefinition.getReference())  ? new BigDecimal(prompt.getValue()) : null ;
    }
}
