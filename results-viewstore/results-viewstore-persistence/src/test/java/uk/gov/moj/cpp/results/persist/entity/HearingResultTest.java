package uk.gov.moj.cpp.results.persist.entity;

import org.junit.Test;
import uk.gov.moj.cpp.domains.results.result.ResultLevel;

import java.util.UUID;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;
import static uk.gov.moj.cpp.results.persist.entity.HearingResult.builder;

public class HearingResultTest {

    private static final UUID HEARING_RESULT_ID = randomUUID();
    private static final HearingResult HEARING_RESULT = builder()
            .withId(HEARING_RESULT_ID)
            .withUrn(STRING.next())
            .withOffenceId(randomUUID())
            .withOffenceTitle(STRING.next())
            .withCaseId(randomUUID())
            .withHearingId(randomUUID())
            .withPersonId(randomUUID())
            .withPleaValue(STRING.next())
            .withPleaDate(PAST_LOCAL_DATE.next())
            .withResultLevel(randomEnum(ResultLevel.class).next())
            .withResultLabel(STRING.next())
            .withCourt(STRING.next())
            .withCourtRoom(STRING.next())
            .withClerkOfTheCourtId(randomUUID())
            .withClerkOfTheCourtFirstName(STRING.next())
            .withClerkOfTheCourtLastName(STRING.next())
            .withStartDate(PAST_LOCAL_DATE.next())
            .withEndDate(PAST_LOCAL_DATE.next())
            .withResultPrompts(asList(
                    ResultPrompt.builder()
                            .withId(randomUUID())
                            .withLabel(STRING.next())
                            .withValue(STRING.next())
                            .withHearingResultId(HEARING_RESULT_ID)
                            .build(),
                    ResultPrompt.builder()
                            .withId(randomUUID())
                            .withLabel(STRING.next())
                            .withValue(STRING.next())
                            .withHearingResultId(HEARING_RESULT_ID)
                            .build()

            ))
            .build();

    @Test
    public void shouldHaveANoArgsConstructor() {
        assertThat(HearingResult.class, hasValidBeanConstructor());
    }

    @Test
    public void shouldCreateNewObjectWithSameValuesIfBuilderDoesNotOverwriteAnyFields() {

        final HearingResult actualHearingResult = HearingResult.of(HEARING_RESULT).build();

        assertThat(actualHearingResult, is((HEARING_RESULT)));

        assertThat(actualHearingResult.getId(), is(HEARING_RESULT.getId()));
        assertThat(actualHearingResult.getCaseId(), is(HEARING_RESULT.getCaseId()));
        assertThat(actualHearingResult.getOffenceId(), is(HEARING_RESULT.getOffenceId()));
        assertThat(actualHearingResult.getHearingId(), is(HEARING_RESULT.getHearingId()));
        assertThat(actualHearingResult.getPersonId(), is(HEARING_RESULT.getPersonId()));
        assertThat(actualHearingResult.getPleaValue(), is(HEARING_RESULT.getPleaValue()));
        assertThat(actualHearingResult.getUrn(), is(HEARING_RESULT.getUrn()));
        assertThat(actualHearingResult.getOffenceTitle(), is(HEARING_RESULT.getOffenceTitle()));
        assertThat(actualHearingResult.getResultLevel(), is(HEARING_RESULT.getResultLevel()));
        assertThat(actualHearingResult.getResultLabel(), is(HEARING_RESULT.getResultLabel()));

        assertThat(actualHearingResult.getResultPrompts().size(), is(HEARING_RESULT.getResultPrompts().size()));

        assertThat(actualHearingResult.getResultPrompts().get(0).getLabel(), is(HEARING_RESULT.getResultPrompts().get(0).getLabel()));
        assertThat(actualHearingResult.getResultPrompts().get(0).getValue(), is(HEARING_RESULT.getResultPrompts().get(0).getValue()));
        assertThat(actualHearingResult.getResultPrompts().get(1).getLabel(), is(HEARING_RESULT.getResultPrompts().get(1).getLabel()));
        assertThat(actualHearingResult.getResultPrompts().get(1).getValue(), is(HEARING_RESULT.getResultPrompts().get(1).getValue()));
    }

    @Test
    public void shouldBeAbleToOverwriteFieldsFromBuilder() {

        final HearingResult data = builder()
                .withId(HEARING_RESULT_ID)
                .withUrn(STRING.next())
                .withOffenceId(randomUUID())
                .withOffenceTitle(STRING.next())
                .withCaseId(randomUUID())
                .withHearingId(randomUUID())
                .withPersonId(randomUUID())
                .withPleaValue(STRING.next())
                .withPleaDate(PAST_LOCAL_DATE.next())
                .withResultLevel(randomEnum(ResultLevel.class).next())
                .withResultLabel(STRING.next())
                .withCourt(STRING.next())
                .withCourtRoom(STRING.next())
                .withClerkOfTheCourtId(randomUUID())
                .withClerkOfTheCourtFirstName(STRING.next())
                .withClerkOfTheCourtLastName(STRING.next())
                .withStartDate(PAST_LOCAL_DATE.next())
                .withEndDate(PAST_LOCAL_DATE.next())
                .withResultPrompts(asList(
                        ResultPrompt.builder()
                                .withId(randomUUID())
                                .withLabel(STRING.next())
                                .withValue(STRING.next())
                                .withHearingResultId(HEARING_RESULT_ID)
                                .build(),
                        ResultPrompt.builder()
                                .withId(randomUUID())
                                .withLabel(STRING.next())
                                .withValue(STRING.next())
                                .withHearingResultId(HEARING_RESULT_ID)
                                .build()

                ))
                .build();

        final HearingResult actualHearingResult = HearingResult.of(HEARING_RESULT)
                .withId(data.getId())
                .withUrn(data.getUrn())
                .withOffenceId(data.getOffenceId())
                .withOffenceTitle(data.getOffenceTitle())
                .withCaseId(data.getCaseId())
                .withHearingId(data.getHearingId())
                .withPersonId(data.getPersonId())
                .withPleaValue(data.getPleaValue())
                .withPleaDate(data.getPleaDate())
                .withResultLevel(data.getResultLevel())
                .withResultLabel(data.getResultLabel())
                .withCourt(data.getCourt())
                .withCourtRoom(data.getCourtRoom())
                .withClerkOfTheCourtId(data.getClerkOfTheCourtId())
                .withClerkOfTheCourtFirstName(data.getClerkOfTheCourtFirstName())
                .withClerkOfTheCourtLastName(data.getClerkOfTheCourtLastName())
                .withStartDate(data.getStartDate())
                .withEndDate(data.getEndDate())
                .withResultPrompts(asList(
                        ResultPrompt.builder()
                                .withId(data.getResultPrompts().get(0).getId())
                                .withLabel(data.getResultPrompts().get(0).getLabel())
                                .withValue(data.getResultPrompts().get(0).getValue())
                                .withHearingResultId(data.getId())
                                .build(),
                        ResultPrompt.builder()
                                .withId(data.getResultPrompts().get(1).getId())
                                .withLabel(data.getResultPrompts().get(1).getLabel())
                                .withValue(data.getResultPrompts().get(1).getValue())
                                .withHearingResultId(data.getId())
                                .build()))
                .build();

        assertThat(actualHearingResult, is(HEARING_RESULT));

        assertThat(actualHearingResult.getId(), is(data.getId()));
        assertThat(actualHearingResult.getCaseId(), is(data.getCaseId()));
        assertThat(actualHearingResult.getOffenceId(), is(data.getOffenceId()));
        assertThat(actualHearingResult.getHearingId(), is(data.getHearingId()));
        assertThat(actualHearingResult.getPersonId(), is(data.getPersonId()));
        assertThat(actualHearingResult.getUrn(), is(data.getUrn()));
        assertThat(actualHearingResult.getPleaValue(), is(data.getPleaValue()));
        assertThat(actualHearingResult.getOffenceTitle(), is(data.getOffenceTitle()));

        assertThat(actualHearingResult.getResultLevel(), is(data.getResultLevel()));
        assertThat(actualHearingResult.getResultLabel(), is(data.getResultLabel()));

        assertThat(actualHearingResult.getResultPrompts().size(), is(data.getResultPrompts().size()));

        assertThat(actualHearingResult.getResultPrompts().get(0).getLabel(), is(data.getResultPrompts().get(0).getLabel()));
        assertThat(actualHearingResult.getResultPrompts().get(0).getValue(), is(data.getResultPrompts().get(0).getValue()));
        assertThat(actualHearingResult.getResultPrompts().get(1).getLabel(), is(data.getResultPrompts().get(1).getLabel()));
        assertThat(actualHearingResult.getResultPrompts().get(1).getValue(), is(data.getResultPrompts().get(1).getValue()));
    }
}