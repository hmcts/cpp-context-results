package uk.gov.moj.cpp.results.query.view.service;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.moj.cpp.results.query.view.TestTemplates.templateDefendant;
import static uk.gov.moj.cpp.results.query.view.TestTemplates.templateHearingResultDocument;
import static uk.gov.moj.cpp.results.query.view.TestTemplates.templateHearingResultDocuments;
import static uk.gov.moj.cpp.results.query.view.TestTemplates.templateHearingResultsAdded;
import static uk.gov.moj.cpp.results.query.view.TestTemplates.templateProsecutionCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.json.schemas.core.Defendant;
import uk.gov.justice.json.schemas.core.ProsecutionCase;
import uk.gov.justice.json.schemas.core.SharedHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;
import uk.gov.moj.cpp.results.persist.HearingResultedDocumentRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;
import uk.gov.moj.cpp.results.query.view.response.DefendantView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummaryView;

import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class HearingServiceTest {

    private static final LocalDate FROM_DATE = PAST_LOCAL_DATE.next();
    private static final String[] HEARING_TYPES = {"PTPH", "SENTENCE", "TRIAL"};
    private static final String HEARING_TYPE =
            HEARING_TYPES[new Random().nextInt(HEARING_TYPES.length)];

    @InjectMocks
    private HearingService hearingService;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private HearingResultedDocumentRepository hearingResultedDocumentRepository;

    @Test
    public void shouldSearchAndFilterHearingByDefendantId() {
        final UUID hearingId = UUID.randomUUID();
        final HearingResultedDocument hearingResultedDocument = templateHearingResultDocument();
        PublicHearingResulted publicHearingResulted = uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplate();
        final UUID defendantId = publicHearingResulted.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();
                HearingResultsAdded payload = new HearingResultsAdded(publicHearingResulted.getHearing(), publicHearingResulted.getSharedTime(), publicHearingResulted.getVariants());
        List<ProsecutionCase> prosecutionCases = asList(templateProsecutionCase(), templateProsecutionCase(), templateProsecutionCase());
        payload.getHearing().setProsecutionCases(prosecutionCases);
        for (ProsecutionCase prosecutionCase : prosecutionCases) {
            prosecutionCase.setDefendants(asList(templateDefendant(), templateDefendant(), templateDefendant()));
        }
        long expectedOutputVariantCount = publicHearingResulted.getVariants().stream().filter(v->v.getKey().getDefendantId().equals(defendantId)).count();
        long expectedSharedResultCount =  publicHearingResulted.getHearing().getSharedResultLines().stream().filter(line->line.getDefendantId().equals(defendantId)).count();

        ProsecutionCase targetCase = prosecutionCases.get(1);
        Defendant targetDefendant = targetCase.getDefendants().get(1);
        targetDefendant.setId(defendantId);
        JsonObject payloadJson = Mockito.mock(JsonObject.class);
        when(stringToJsonObjectConverter.convert(hearingResultedDocument.getPayload())).thenReturn(payloadJson);
        when(jsonObjectToObjectConverter.convert(payloadJson, HearingResultsAdded.class)).thenReturn(payload);
        when (hearingResultedDocumentRepository.findBy(hearingId)).thenReturn(hearingResultedDocument);

        //The test call !!!!!!!
        HearingResultsAdded result =hearingService.findHearingDetailsByHearingIdDefendantId(hearingId, defendantId);

        assertThat(result.getHearing().getProsecutionCases().size(), is(1));
        ProsecutionCase resultCase = result.getHearing().getProsecutionCases().get(0);
        assertThat(resultCase.getDefendants().size(), is(1));
        assertThat(resultCase.getId(), is(targetCase.getId()));
        Defendant resultDefendant = resultCase.getDefendants().get(0);
        assertThat(resultDefendant.getId(), is(targetDefendant.getId()));
        assertThat((long) result.getVariants().size(), is(expectedOutputVariantCount));
        assertThat((long) result.getHearing().getSharedResultLines().size(), is(expectedSharedResultCount));

    }

    @Test
    public void shouldFindHearingResultSummariesFromDate() throws Exception {
        final List<HearingResultedDocument> hearingResultedDocuments = templateHearingResultDocuments(2);
        final List<HearingResultsAdded> payloadObjects = new ArrayList<>();
        for (HearingResultedDocument document : hearingResultedDocuments) {
            final HearingResultsAdded payloadObject = templateHearingResultsAdded();
            payloadObjects.add(payloadObject);
            final JsonObject payloadJsonObject = Mockito.mock(JsonObject.class);
            when(stringToJsonObjectConverter.convert(document.getPayload())).thenReturn(payloadJsonObject);
            when(jsonObjectToObjectConverter.convert(payloadJsonObject, HearingResultsAdded.class)).thenReturn(payloadObject);
        }

        when(hearingResultedDocumentRepository.findByFromDate(FROM_DATE)).thenReturn(hearingResultedDocuments);

        final HearingResultSummariesView hearingSummaries = hearingService.findHearingResultSummariesFromDate(FROM_DATE);
        assertThat(hearingSummaries.getResults().size(), is(2));

        checkSummary(hearingSummaries.getResults().get(0), payloadObjects.get(0));

    }

    private void checkSummary(final HearingResultSummaryView summary, final HearingResultsAdded hearingResultsAdded) {
        SharedHearing hearing0 = hearingResultsAdded.getHearing();
        ProsecutionCase prosecutionCase0 = hearing0.getProsecutionCases().get(0);

        assertThat(summary.getHearingId(), is(hearing0.getId()));
        assertThat(summary.getHearingDate(), is(hearing0.getHearingDays().get(0).getSittingDay().toLocalDate()));
        assertThat(summary.getHearingType(), is(hearing0.getType().getDescription()));
        assertThat(summary.getUrns(), is(asList(prosecutionCase0.getProsecutionCaseIdentifier().getCaseURN())));

        Defendant defendant = prosecutionCase0.getDefendants().get(0);
        DefendantView defendantSummary = summary.getDefendant();

        assertThat(defendantSummary.getFirstName(), is(defendant.getPersonDefendant().getPersonDetails().getFirstName()));
        assertThat(defendantSummary.getLastName(), is(defendant.getPersonDefendant().getPersonDetails().getLastName()));
        assertThat(defendantSummary.getPersonId(), is(defendant.getId()));
    }


}
