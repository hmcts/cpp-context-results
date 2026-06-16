package uk.gov.moj.cpp.results.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterCaseOrApplication.informantRegisterCaseOrApplication;
import static uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterDefendant.informantRegisterDefendant;
import static uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterDocumentRequest.informantRegisterDocumentRequest;
import static uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterHearing.informantRegisterHearing;
import static uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterHearingVenue.informantRegisterHearingVenue;
import static uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterOffence.informantRegisterOffence;
import static uk.gov.moj.cpp.results.domain.informant.model.Verdict.verdict;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterCaseOrApplication;
import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterDefendant;
import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterDocumentRequest;
import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterHearing;
import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterHearingVenue;
import uk.gov.moj.cpp.results.domain.informant.model.InformantRegisterOffence;
import uk.gov.moj.cpp.results.domain.informant.model.Verdict;
import uk.gov.moj.cpp.results.persist.InformantRegisterRepository;
import uk.gov.moj.cpp.results.persist.entity.InformantRegisterEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutorResultsQueryViewTest {

    @Mock
    private InformantRegisterRepository informantRegisterRepository;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private ProsecutorResultsQueryView prosecutorResultsQueryView;

    private String ouCode;
    private String prosecutionAuthorityCode;
    private UUID prosecutionAuthorityId;
    private String prosecutionAuthorityName;

    @BeforeEach
    public void setUp() {
        ouCode = randomAlphanumeric(7);
        prosecutionAuthorityCode = randomAlphanumeric(10);
        prosecutionAuthorityId = randomUUID();
        prosecutionAuthorityName = randomAlphanumeric(25);
    }

    @Test
    public void getProsecutorResults_onlyStartDateSupplied() {
        final String ouCode = randomAlphanumeric(7);
        final LocalDate startDate = now();

        final List<InformantRegisterEntity> results = getResults();
        when(informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(ouCode, startDate, startDate)).thenReturn(results);

        final JsonEnvelope prosecutorResults = prosecutorResultsQueryView.getProsecutorResults(createPayload(ouCode, startDate.toString(), null));

        verify(informantRegisterRepository).findByProsecutionAuthorityOuCodeAndRegisterDateRange(ouCode, startDate, startDate);
        assertThat(prosecutorResults.metadata().name(), is("results.prosecutor-results"));
        assertThat(prosecutorResults.payloadAsJsonObject().getJsonArray("hearingVenues"), hasSize(1));
        assertThat(prosecutorResults.payloadAsJsonObject().getString("startDate"), is(startDate.toString()));
        assertThat(prosecutorResults.payloadAsJsonObject().getString("endDate", null), nullValue());
        assertThat(prosecutorResults.payloadAsJsonObject().getString("prosecutionAuthorityCode"), is(prosecutionAuthorityCode));
        assertThat(prosecutorResults.payloadAsJsonObject().getString("prosecutionAuthorityId"), is(prosecutionAuthorityId.toString()));
        assertThat(prosecutorResults.payloadAsJsonObject().getString("prosecutionAuthorityName"), is(prosecutionAuthorityName));
    }

    @Test
    public void getProsecutorResults_DateRangeSupplied() {
        final String ouCode = randomAlphanumeric(7);
        final LocalDate startDate = now();
        final LocalDate endDate = now();

        final List<InformantRegisterEntity> results = getResults();
        when(informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(ouCode, startDate, endDate)).thenReturn(results);

        final JsonEnvelope prosecutorResults = prosecutorResultsQueryView.getProsecutorResults(createPayload(ouCode, startDate.toString(), endDate.toString()));

        verify(informantRegisterRepository).findByProsecutionAuthorityOuCodeAndRegisterDateRange(ouCode, startDate, endDate);
        assertThat(prosecutorResults.metadata().name(), is("results.prosecutor-results"));
        assertThat(prosecutorResults.payloadAsJsonObject().getJsonArray("hearingVenues"), hasSize(1));
        assertThat(prosecutorResults.payloadAsJsonObject().getString("startDate"), is(startDate.toString()));
        assertThat(prosecutorResults.payloadAsJsonObject().getString("endDate"), is(endDate.toString()));
        assertThat(prosecutorResults.payloadAsJsonObject().getString("prosecutionAuthorityCode"), is(prosecutionAuthorityCode));
        assertThat(prosecutorResults.payloadAsJsonObject().getString("prosecutionAuthorityId"), is(prosecutionAuthorityId.toString()));
        assertThat(prosecutorResults.payloadAsJsonObject().getString("prosecutionAuthorityName"), is(prosecutionAuthorityName));
    }

    @Test
    public void getProsecutorResults_NoResultsAvailable() {
        final String ouCode = randomAlphanumeric(7);
        final LocalDate startDate = now();
        final LocalDate endDate = now();

        when(informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(ouCode, startDate, endDate)).thenReturn(newArrayList());

        final JsonEnvelope prosecutorResults = prosecutorResultsQueryView.getProsecutorResults(createPayload(ouCode, startDate.toString(), endDate.toString()));

        verify(informantRegisterRepository).findByProsecutionAuthorityOuCodeAndRegisterDateRange(ouCode, startDate, endDate);
        assertThat(prosecutorResults.metadata().name(), is("results.prosecutor-results"));
        assertThat(prosecutorResults.payloadAsJsonObject().getJsonArray("hearingVenues"), hasSize(0));
        assertThat(prosecutorResults.payloadAsJsonObject().getString("startDate"), is(startDate.toString()));
        assertThat(prosecutorResults.payloadAsJsonObject().getString("endDate"), is(endDate.toString()));
        assertThat(prosecutorResults.payloadAsJsonObject().getString("prosecutionAuthorityCode", null), nullValue());
        assertThat(prosecutorResults.payloadAsJsonObject().getString("prosecutionAuthorityId", null), nullValue());
        assertThat(prosecutorResults.payloadAsJsonObject().getString("prosecutionAuthorityName", null), nullValue());
    }

    @Test
    public void getProsecutorResults_whenRequiredParamsMissing_shouldReturnNull() {
        final JsonEnvelope prosecutorResults = prosecutorResultsQueryView.getProsecutorResults(createPayload(null, null, null));
        assertThat(prosecutorResults, nullValue());
    }

    @Test
    public void getProsecutorResults_whenOffenceHasVerdict_shouldIncludeVerdictInResponse() {
        final Verdict verdictObj = verdict().withVerdictCode("G").withVerdictDate("2026-04-13").withVerdictType("FOUND_GUILTY").build();
        final List<InformantRegisterEntity> results = getResultsWithVerdict(verdictObj);
        when(informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(ouCode, now(), now())).thenReturn(results);

        final JsonEnvelope prosecutorResults = prosecutorResultsQueryView.getProsecutorResults(createPayload(ouCode, now().toString(), now().toString()));

        final JsonObject offence = getFirstOffence(prosecutorResults);
        assertThat(offence.getJsonObject("verdict").getString("verdictCode"), is("G"));
        assertThat(offence.getJsonObject("verdict").getString("verdictDate"), is("2026-04-13"));
        assertThat(offence.getJsonObject("verdict").getString("verdictType"), is("FOUND_GUILTY"));
    }

    @Test
    public void getProsecutorResults_whenOffenceHasNoVerdict_shouldOmitVerdictField() {
        final List<InformantRegisterEntity> results = getResultsWithVerdict(null);
        when(informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(ouCode, now(), now())).thenReturn(results);

        final JsonEnvelope prosecutorResults = prosecutorResultsQueryView.getProsecutorResults(createPayload(ouCode, now().toString(), now().toString()));

        final JsonObject offence = getFirstOffence(prosecutorResults);
        assertThat(offence.containsKey("verdict"), is(false));
    }

    @Test
    public void getProsecutorResults_whenOffenceVerdictIsNull_shouldOmitVerdictField() {
        final List<InformantRegisterEntity> results = getResultsWithVerdict(null);
        when(informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(ouCode, now(), now())).thenReturn(results);

        final JsonEnvelope prosecutorResults = prosecutorResultsQueryView.getProsecutorResults(createPayload(ouCode, now().toString(), now().toString()));

        final JsonObject offence = getFirstOffence(prosecutorResults);
        assertThat(offence.get("verdict"), nullValue());
    }

    private JsonEnvelope createPayload(final String ouCode, final String startDate, final String endDate) {
        final MetadataBuilder metadataBuilder = metadataBuilder().withId(randomUUID()).withName("results.prosecutor-results");
        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        if (nonNull(ouCode)) {
            payloadBuilder.add("ouCode", ouCode);
        }
        if (nonNull(startDate)) {
            payloadBuilder.add("startDate", startDate);
        }
        if (nonNull(endDate)) {
            payloadBuilder.add("endDate", endDate);
        }
        return envelopeFrom(metadataBuilder, payloadBuilder);
    }

    private List<InformantRegisterEntity> getResults() {
        final InformantRegisterEntity entity = new InformantRegisterEntity();
        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = informantRegisterDocumentRequest()
                .withHearingVenue(informantRegisterHearingVenue().build())
                .withProsecutionAuthorityOuCode(ouCode)
                .withProsecutionAuthorityId(prosecutionAuthorityId)
                .withProsecutionAuthorityName(prosecutionAuthorityName)
                .withProsecutionAuthorityCode(prosecutionAuthorityCode)
                .build();
        entity.setPayload(objectToJsonObjectConverter.convert(informantRegisterDocumentRequest).toString());
        return singletonList(entity);
    }

    private List<InformantRegisterEntity> getResultsWithVerdict(final Verdict verdictObj) {
        final InformantRegisterEntity entity = new InformantRegisterEntity();
        final InformantRegisterOffence offence = informantRegisterOffence()
                .withOrderIndex(1)
                .withOffenceCode("OFF001")
                .withOffenceTitle("Theft")
                .withVerdict(verdictObj)
                .build();
        final InformantRegisterCaseOrApplication caseOrApplication = informantRegisterCaseOrApplication()
                .withCaseOrApplicationReference("CASE001")
                .withOffences(singletonList(offence))
                .build();
        final InformantRegisterDefendant defendant = informantRegisterDefendant()
                .withName("John Smith")
                .withAddress1("1 Main St")
                .withProsecutionCasesOrApplications(singletonList(caseOrApplication))
                .build();
        final InformantRegisterHearing hearing = informantRegisterHearing()
                .withCourtRoom("Court 1")
                .withHearingStartTime("2026-04-13")
                .withDefendants(singletonList(defendant))
                .build();
        final InformantRegisterHearingVenue hearingVenue = informantRegisterHearingVenue()
                .withCourtHouse("Crown Court")
                .withCourtSessions(singletonList(hearing))
                .build();
        final InformantRegisterDocumentRequest documentRequest = informantRegisterDocumentRequest()
                .withHearingVenue(hearingVenue)
                .withProsecutionAuthorityOuCode(ouCode)
                .withProsecutionAuthorityId(prosecutionAuthorityId)
                .withProsecutionAuthorityName(prosecutionAuthorityName)
                .withProsecutionAuthorityCode(prosecutionAuthorityCode)
                .build();
        entity.setPayload(objectToJsonObjectConverter.convert(documentRequest).toString());
        return singletonList(entity);
    }

    private JsonObject getFirstOffence(final JsonEnvelope prosecutorResults) {
        return prosecutorResults.payloadAsJsonObject()
                .getJsonArray("hearingVenues").get(0).asJsonObject()
                .getJsonArray("courtSessions").get(0).asJsonObject()
                .getJsonArray("defendants").get(0).asJsonObject()
                .getJsonArray("prosecutionCasesOrApplications").get(0).asJsonObject()
                .getJsonArray("offences").get(0).asJsonObject();
    }
}
