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
import static uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest.informantRegisterDocumentRequest;
import static uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterHearingVenue.informantRegisterHearingVenue;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.results.persist.InformantRegisterRepository;
import uk.gov.moj.cpp.results.persist.entity.InformantRegisterEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    private JsonEnvelope createPayload(final String ouCode, final String startDate, final String endDate) {
        final MetadataBuilder metadataBuilder = metadataBuilder().withId(randomUUID()).withName("results.prosecutor-results");
        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("ouCode", ouCode)
                .add("startDate", startDate);
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
}