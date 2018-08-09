package uk.gov.moj.cpp.results.query.view.service;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;
import static uk.gov.moj.cpp.results.persist.entity.HearingResult.builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.moj.cpp.domains.results.result.ResultLevel;
import uk.gov.moj.cpp.results.persist.HearingRepository;
import uk.gov.moj.cpp.results.persist.HearingResultRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResult;
import uk.gov.moj.cpp.results.persist.entity.HearingResultSummary;
import uk.gov.moj.cpp.results.persist.entity.ResultPrompt;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;

@RunWith(MockitoJUnitRunner.class)
public class HearingServiceTest {

    private static final LocalDate FROM_DATE = PAST_LOCAL_DATE.next();
    private static final UUID PERSON_ID_1 = randomUUID();
    private static final UUID PERSON_ID_2 = randomUUID();
    private static final UUID HEARING_ID_1 = randomUUID();
    private static final UUID HEARING_ID_2 = randomUUID();
    private static final String[] HEARING_TYPES = {"PTPH", "SENTENCE", "TRIAL"};
    private static final String HEARING_TYPE =
                    HEARING_TYPES[new Random().nextInt(HEARING_TYPES.length)];
    private static final LocalDate HEARING_DATE = PAST_LOCAL_DATE.next();
    private static final String FIRST_NAME = STRING.next();
    private static final String LAST_NAME = STRING.next();

    private static final UUID HEARING_RESULT_ID = randomUUID();
    private static final String CASE_URN_1_1 = STRING.next();
    private static final String CASE_URN_1_2 = STRING.next();
    private static final String CASE_URN_2_1 = STRING.next();
    private static final String CASE_URN_2_2 = STRING.next();

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
                            .build()

            ))
            .build();

    @InjectMocks
    private HearingService hearingService;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingResultRepository hearingResultRepository;

    @Test
    public void shouldFindHearingResultSummariesFromDate() throws Exception {
        when(hearingRepository.findHearingResultSummariesByFromDate(FROM_DATE)).thenReturn(hearingResultSummaries());

        when(hearingResultRepository.findByHearingIdAndPersonId(HEARING_ID_1, PERSON_ID_1)).thenReturn(asList(
                HearingResult.of(HEARING_RESULT).withUrn(CASE_URN_1_1).withPersonId(PERSON_ID_1).withHearingId(HEARING_ID_1).build(),
                HearingResult.of(HEARING_RESULT).withUrn(CASE_URN_1_2).withPersonId(PERSON_ID_1).withHearingId(HEARING_ID_1).build()

        ));

        when(hearingResultRepository.findByHearingIdAndPersonId(HEARING_ID_2, PERSON_ID_2)).thenReturn(asList(
                HearingResult.of(HEARING_RESULT).withUrn(CASE_URN_2_1).withPersonId(PERSON_ID_1).withHearingId(HEARING_ID_2).build(),
                HearingResult.of(HEARING_RESULT).withUrn(CASE_URN_2_2).withPersonId(PERSON_ID_1).withHearingId(HEARING_ID_2).build()

        ));

        final HearingResultSummariesView hearingSummaries = hearingService.findHearingResultSummariesFromDate(FROM_DATE);

        assertThat(hearingSummaries.getResults(), hasSize(2));
        assertThat(hearingSummaries.getResults().get(0).getDefendant().getFirstName(), is(FIRST_NAME));
        assertThat(hearingSummaries.getResults().get(0).getDefendant().getLastName(), is(LAST_NAME));
        assertThat(hearingSummaries.getResults().get(0).getDefendant().getPersonId(), is(PERSON_ID_1));
        assertThat(hearingSummaries.getResults().get(0).getHearingDate(), is(HEARING_DATE));
        assertThat(hearingSummaries.getResults().get(0).getHearingId(), is(HEARING_ID_1));
        assertThat(hearingSummaries.getResults().get(0).getHearingType(), is(HEARING_TYPE));

        assertThat(hearingSummaries.getResults().get(0).getUrns(), hasSize(2));
        assertThat(hearingSummaries.getResults().get(0).getUrns().get(0), is(CASE_URN_1_1));
        assertThat(hearingSummaries.getResults().get(0).getUrns().get(1), is(CASE_URN_1_2));

        assertThat(hearingSummaries.getResults().get(1).getUrns(), hasSize(2));
        assertThat(hearingSummaries.getResults().get(1).getUrns().get(0), is(CASE_URN_2_1));
        assertThat(hearingSummaries.getResults().get(1).getUrns().get(1), is(CASE_URN_2_2));
    }

    private List<HearingResultSummary> hearingResultSummaries() {
        return Stream.of(
                new HearingResultSummary(HEARING_ID_1, PERSON_ID_1, HEARING_TYPE, HEARING_DATE, FIRST_NAME, LAST_NAME),
                new HearingResultSummary(HEARING_ID_2, PERSON_ID_2, HEARING_TYPE, HEARING_DATE, FIRST_NAME, LAST_NAME))
            .collect(Collectors.toList());

    }
}
