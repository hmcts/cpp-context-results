package uk.gov.moj.cpp.results.event.helper;


import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt.prompt;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class PenaltyPointTest {

    private UUID promptId = randomUUID();

    final  uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt getPromptRefDataWithReferenceValue=
            prompt()
                    .setId(promptId)
                    .setLabel("promptRefData1")
                    .setReference("PENPT")
                    .setUserGroups(Arrays.asList("usergroup0", "usergroup1"));


    final uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt promptRefDataWithoutReferenceValue =
            prompt()
                    .setId(promptId)
                    .setLabel("promptRefData2")
                    .setUserGroups(Arrays.asList("usergroup0", "usergroup1"));


    final  uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt promptRefData=
            prompt()
                    .setId(randomUUID())
                    .setLabel("promptReferenceData0")
                    .setReference("PENPT")
                    .setUserGroups(Arrays.asList("usergroup0", "usergroup1"));


    final uk.gov.justice.sjp.results.Prompts sjpPrompt = new uk.gov.justice.sjp.results.Prompts(promptId, "10.00");


    @Test
    public void givenA_PromptReference_of_PENPT_Then_PenaltyPoints_should_be_Set() {
        final PenaltyPoint penaltyPoint = new PenaltyPoint();
        final BigDecimal actualPenaltyPoint = penaltyPoint.getPenaltyPointFromResults(getPromptRefDataWithReferenceValue, sjpPrompt);

        assertThat(actualPenaltyPoint, is(new BigDecimal("10.00")));

    }

    @Test
    public void shouldReturnEmptyPromptReferenceDataForPenaltyPoint(){
        final PenaltyPoint penaltyPoint = new PenaltyPoint();
        final BigDecimal actualPenaltyPoint = penaltyPoint.getPenaltyPointFromResults(promptRefDataWithoutReferenceValue, sjpPrompt);
        assertNull(actualPenaltyPoint);

    }

    @Test
    public void shouldReturnEmptyPromptReferenceDataFor_PenaltyPoint_WhenPromptIds_Do_Not_Match(){
        final PenaltyPoint penaltyPoint = new PenaltyPoint();
        final BigDecimal actualPenaltyPoint = penaltyPoint.getPenaltyPointFromResults(promptRefData, sjpPrompt);
        assertNull(actualPenaltyPoint);

    }

}
