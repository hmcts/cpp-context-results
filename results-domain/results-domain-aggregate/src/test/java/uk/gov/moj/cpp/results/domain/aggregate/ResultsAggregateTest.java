package uk.gov.moj.cpp.results.domain.aggregate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationEjected;
import uk.gov.justice.core.courts.HearingCaseEjected;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;

import javax.json.Json;
import javax.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class ResultsAggregateTest {

    @InjectMocks
    private ResultsAggregate resultsAggregate;

    private final PublicHearingResulted input = PublicHearingResulted.publicHearingResulted()
            .setHearing(Hearing.hearing()
                    .withId(UUID.randomUUID())
                    .build())
            .setSharedTime(ZonedDateTime.now());

    @Test
    public void testSaveShareResults_shouldRaiseHearingResultsAddedEvent() {
        final HearingResultsAdded hearingResultsAdded = resultsAggregate.saveHearingResults(input)
                .map(o -> (HearingResultsAdded) o)
                .findFirst()
                .orElse(null);

        assertNotNull(hearingResultsAdded);
        assertEquals(input.getHearing(), hearingResultsAdded.getHearing());
        assertEquals(input.getSharedTime(), hearingResultsAdded.getSharedTime());
    }

    @Test
    public void testEjectCaseOrApplication_whenPayloadContainsCaseId_expectHearingCaseEjectedEvent() {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final String CASE_ID = "caseId";
        final String HEARING_ID = "hearingId";
        input.getHearing().setId(hearingId);
        resultsAggregate.saveHearingResults(input);
        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(CASE_ID, caseId.toString())
                .build();
        final HearingCaseEjected hearingCaseEjected =resultsAggregate.ejectCaseOrApplication(hearingId, payload)
                .map(o->(HearingCaseEjected)o)
                .findFirst().orElse(null);
        assertNotNull(hearingCaseEjected);
        assertEquals(hearingId, hearingCaseEjected.getHearingId());
        assertEquals(caseId, hearingCaseEjected.getCaseId());

    }

    @Test
    public void testEjectCaseOrApplication_whenPayloadContainsCaseId_expectHearingApplicationEjectedEvent() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final String APPLICATION_ID = "applicationId";
        final String HEARING_ID = "hearingId";
        input.getHearing().setId(hearingId);
        resultsAggregate.saveHearingResults(input);
        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(APPLICATION_ID, applicationId.toString())
                .build();
        final HearingApplicationEjected hearingApplicationEjected =resultsAggregate.ejectCaseOrApplication(hearingId, payload)
                .map(o->(HearingApplicationEjected)o)
                .findFirst().orElse(null);
        assertNotNull(hearingApplicationEjected);
        assertEquals(hearingId, hearingApplicationEjected.getHearingId());
        assertEquals(applicationId, hearingApplicationEjected.getApplicationId());

    }

    @Test
    public void testEjectCaseOrApplication_whenHearingNotResultedForHearingInPayload_expecNull() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final String APPLICATION_ID = "applicationId";
        final String HEARING_ID = "hearingId";

        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(APPLICATION_ID, applicationId.toString())
                .build();
        final List<Object> eventStream = resultsAggregate.ejectCaseOrApplication(hearingId, payload).collect(toList());
        assertThat(eventStream.size(), is(0));
    }

}