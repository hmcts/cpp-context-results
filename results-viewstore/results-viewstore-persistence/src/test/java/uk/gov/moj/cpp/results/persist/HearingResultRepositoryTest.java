package uk.gov.moj.cpp.results.persist;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.domains.results.result.ResultLevel;
import uk.gov.moj.cpp.results.persist.entity.CourtClerk;
import uk.gov.moj.cpp.results.persist.entity.HearingResult;
import uk.gov.moj.cpp.results.persist.entity.ResultPrompt;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;

@SuppressWarnings("CdiInjectionPointsInspection")
@RunWith(CdiTestRunner.class)
public class HearingResultRepositoryTest extends BaseTransactionalTest {

    @Inject
    private HearingResultRepository hearingResultRepository;

    private static final UUID HEARING_RESULT_ID = randomUUID();
    private static final String FIRST_CLERK_NAME = "Braven";
    private static final String SECOND_CLERK_NAME = "Amon";
    private static final HearingResult HEARING_RESULT = HearingResult.builder()
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
            .withClerkOfTheCourtFirstName(FIRST_CLERK_NAME)
            .withClerkOfTheCourtLastName(STRING.next())
            .withStartDate(PAST_LOCAL_DATE.next())
            .withEndDate(PAST_LOCAL_DATE.next())
            .withResultPrompts(singletonList(
                    ResultPrompt.builder()
                            .withId(randomUUID())
                            .withLabel(STRING.next())
                            .withValue(STRING.next())
                            .withHearingResultId(HEARING_RESULT_ID)
                            .build()

            ))
            .build();

    @Test
    public void shouldRetrieveAHearingResult() {
        givenHearingResultsDoesNotExist(HEARING_RESULT_ID);
        givenAHearingResult(HEARING_RESULT);

        final HearingResult actualHearingResult = hearingResultRepository.findBy(HEARING_RESULT_ID);

        assertThat(actualHearingResult.getId(), is(HEARING_RESULT.getId()));
        assertThat(actualHearingResult.getCaseId(), is(HEARING_RESULT.getCaseId()));
        assertThat(actualHearingResult.getOffenceId(), is(HEARING_RESULT.getOffenceId()));
        assertThat(actualHearingResult.getPleaValue(), is(HEARING_RESULT.getPleaValue()));
        assertThat(actualHearingResult.getPleaDate(), is(HEARING_RESULT.getPleaDate()));
        assertThat(actualHearingResult.getOffenceTitle(), is(HEARING_RESULT.getOffenceTitle()));
        assertThat(actualHearingResult.getUrn(), is(HEARING_RESULT.getUrn()));
        assertThat(actualHearingResult.getResultLabel(), is(HEARING_RESULT.getResultLabel()));
        assertThat(actualHearingResult.getResultLevel(), is(HEARING_RESULT.getResultLevel()));
        assertThat(actualHearingResult.getResultPrompts(), hasSize(1));
        assertThat(actualHearingResult.getResultPrompts().get(0).getHearingResultId(), is(HEARING_RESULT.getId()));
        assertThat(actualHearingResult.getResultPrompts().get(0).getLabel(), is(HEARING_RESULT.getResultPrompts().get(0).getLabel()));
        assertThat(actualHearingResult.getResultPrompts().get(0).getValue(), is(HEARING_RESULT.getResultPrompts().get(0).getValue()));
    }

    @Test
    public void shouldRemoveAHearingResult() {
        givenAHearingResult(HEARING_RESULT);

        final HearingResult hearingResult = hearingResultRepository.findBy(HEARING_RESULT_ID);
        assertThat(hearingResult, is(notNullValue()));

        hearingResultRepository.remove(hearingResult);
        assertThat(hearingResultRepository.findBy(HEARING_RESULT_ID), is(nullValue()));
    }

    @Test
    public void shouldNotFindAHearingResultByHearingIdAndPersonId() {
        final List<HearingResult> hearingResults = hearingResultRepository
                .findByHearingIdAndPersonId(HEARING_RESULT.getHearingId(), HEARING_RESULT.getPersonId());

        assertThat(hearingResults, hasSize(0));
    }

    @Test
    public void shouldFindAHearingResultByHearingIdAndPersonId() {
        givenAHearingResult(HEARING_RESULT);

        final List<HearingResult> hearingResults = hearingResultRepository
                .findByHearingIdAndPersonId(HEARING_RESULT.getHearingId(), HEARING_RESULT.getPersonId());

        assertThat(hearingResults, hasSize(1));
    }

    @Test
    public void shouldFindACourtClerk() {
        givenAHearingResult(HEARING_RESULT);

        List<CourtClerk> courtClerks = hearingResultRepository
                .findCourtClerksForHearingIdAndPersonId(HEARING_RESULT.getHearingId(), HEARING_RESULT.getPersonId());

        assertThat(courtClerks, hasSize(1));
    }

    @Test
    public void shouldFindMultipleCourtClerks() {

        givenAHearingResult(HEARING_RESULT);

        final UUID hearingResultId = randomUUID();
        givenAHearingResult(HearingResult.of(HEARING_RESULT)
                        .withId(hearingResultId)
                        .withClerkOfTheCourtId(randomUUID())
                        .withClerkOfTheCourtFirstName(SECOND_CLERK_NAME)
                        .withClerkOfTheCourtLastName(STRING.next())
                        .withResultPrompts(singletonList(
                                ResultPrompt.builder()
                                        .withId(randomUUID())
                                        .withLabel(STRING.next())
                                        .withValue(STRING.next())
                                        .withHearingResultId(hearingResultId)
                                        .build()))
                        .build());

        List<CourtClerk> courtClerks = hearingResultRepository
                .findCourtClerksForHearingIdAndPersonId(HEARING_RESULT.getHearingId(), HEARING_RESULT.getPersonId());

        assertThat(courtClerks, hasSize(2));
        assertThat(courtClerks.get(0).getClerkOfTheCourtFirstName(), is(SECOND_CLERK_NAME));
        assertThat(courtClerks.get(1).getClerkOfTheCourtFirstName(), is(FIRST_CLERK_NAME));
    }

    @Test
    public void shouldFindMultipleCourtClerksThatAreTheSameClerk() {
        givenAHearingResult(HEARING_RESULT);

        final UUID newHearingResultId = randomUUID();

        givenAHearingResult(HearingResult.of(HEARING_RESULT).withId(newHearingResultId)
                .withResultPrompts(
                        singletonList(
                                ResultPrompt.builder()
                                        .withId(randomUUID())
                                        .withHearingResultId(newHearingResultId)
                                        .withLabel(STRING.next())
                                        .withValue(STRING.next())
                                        .build()))
                .build());

        List<CourtClerk> courtClerks = hearingResultRepository
                .findCourtClerksForHearingIdAndPersonId(HEARING_RESULT.getHearingId(), HEARING_RESULT.getPersonId());

        assertThat(courtClerks, hasSize(1));
        assertThat(courtClerks.get(0).getClerkOfTheCourtFirstName(), is(FIRST_CLERK_NAME));
    }

    private void givenAHearingResult(final HearingResult hearingResult) {
        hearingResultRepository.save(hearingResult);
    }

    private void givenHearingResultsDoesNotExist(final UUID... hearingResultIds) {
        stream(hearingResultIds).forEach(hearingResultId ->
                assertThat(hearingResultRepository.findBy(hearingResultId), is(nullValue()))
        );
    }
}