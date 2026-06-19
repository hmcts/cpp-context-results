package uk.gov.moj.cpp.results.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.RECORDED;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.InformantRegisterRepository;
import uk.gov.moj.cpp.results.persist.entity.InformantRegisterEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InformantRegisterDocumentRequestQueryViewTest {

    @Mock
    private InformantRegisterRepository informantRegisterRepository;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @InjectMocks
    private InformantRegisterDocumentRequestQueryView informantRegisterDocumentRequestQueryView;

    @Test
    public void shouldGetInformantRegisterRequestsByStatus() {
        final UUID prosecutionAuthorityId = randomUUID();
        final LocalDate registerDate = now();

        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(randomUUID())
                        .withName("results.query.informant-register-document-request").build(),
                createObjectBuilder().add("requestStatus", RECORDED.name()).build());

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthorityId);
        informantRegisterEntity.setRegisterDate(registerDate);
        informantRegisterEntity.setStatus(RECORDED);

        final JsonObject transformedJsonEntity = createObjectBuilder().add("prosecutionAuthorityId", prosecutionAuthorityId.toString()).build();
        when(objectToJsonObjectConverter.convert(informantRegisterEntity)).thenReturn(transformedJsonEntity);
        when(informantRegisterRepository.findByStatusRecorded()).thenReturn(Collections.singletonList(informantRegisterEntity));

        final JsonEnvelope informantRegisterRequests = informantRegisterDocumentRequestQueryView.getInformantRegisterRequests(envelope);
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests").size(), is(1));
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0).getString("prosecutionAuthorityId"), is(prosecutionAuthorityId.toString()));
    }


    @Test
    public void shouldGetInformantRegisterByMaterial() {
        final UUID fileId = randomUUID();
        final UUID prosecutionAuthorityId = randomUUID();

        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(randomUUID())
                        .withName("results.query.informant-register-document-by-material").build(),
                createObjectBuilder().add("fileId", fileId.toString()).build());

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthorityId);
        informantRegisterEntity.setFileId(fileId);
        informantRegisterEntity.setRegisterDate(now());

        final JsonObject transformedJsonEntity = createObjectBuilder().add("prosecutionAuthorityId", prosecutionAuthorityId.toString()).build();
        when(objectToJsonObjectConverter.convert(informantRegisterEntity)).thenReturn(transformedJsonEntity);
        when(informantRegisterRepository.findByFileId(fileId)).thenReturn(newArrayList(informantRegisterEntity));

        final JsonEnvelope informantRegisterRequests = informantRegisterDocumentRequestQueryView.getInformantRegistersByMaterial(envelope);
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests").size(), is(1));
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0).getString("prosecutionAuthorityId"), is(prosecutionAuthorityId.toString()));
    }

    @Test
    public void shouldGetInformantRegisterByDate() {
        final LocalDate registerDate = now();
        final String prosecutionAuthorityCode = "TFL";

        final JsonObject payload = createObjectBuilder().add("registerDate", registerDate.toString())
                .add("prosecutionAuthorityCode", prosecutionAuthorityCode)
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(randomUUID())
                        .withName("results.query.informant-register-document-by-request-date").build(),payload);

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        final UUID prosecutionAuthorityId = randomUUID();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthorityId);
        informantRegisterEntity.setRegisterDate(registerDate);
        informantRegisterEntity.setProsecutionAuthorityCode(prosecutionAuthorityCode);

        final JsonObject transformedJsonEntity = createObjectBuilder()
                .add("prosecutionAuthorityId", prosecutionAuthorityId.toString())
                .add("registerDate", registerDate.toString())
                .build();
        when(objectToJsonObjectConverter.convert(informantRegisterEntity)).thenReturn(transformedJsonEntity);
        when(informantRegisterRepository.findByRegisterDateAndProsecutionAuthorityCode(registerDate, prosecutionAuthorityCode)).thenReturn(newArrayList(informantRegisterEntity));

        final JsonEnvelope informantRegisterRequests = informantRegisterDocumentRequestQueryView.getInformantRegistersByRequestDate(envelope);
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests").size(), is(1));
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0).getString("prosecutionAuthorityId"), is(prosecutionAuthorityId.toString()));
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0).getString("registerDate"), is(registerDate.toString()));
    }

    @Test
    public void shouldGetInformantRegisterByDateWhenProsecutionAuthorityIsEmpty() {
        final LocalDate registerDate = now();
        final String prosecutionAuthorityCode = "TFL";

        final JsonObject payload = createObjectBuilder().add("registerDate", registerDate.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("results.query.informant-register-document-by-request-date").build(),payload);

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        final UUID prosecutionAuthorityId = randomUUID();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthorityId);
        informantRegisterEntity.setRegisterDate(registerDate);
        informantRegisterEntity.setProsecutionAuthorityCode(prosecutionAuthorityCode);

        final JsonObject transformedJsonEntity = createObjectBuilder()
                .add("prosecutionAuthorityId", prosecutionAuthorityId.toString())
                .add("registerDate", registerDate.toString())
                .build();
        when(objectToJsonObjectConverter.convert(informantRegisterEntity)).thenReturn(transformedJsonEntity);
        when(informantRegisterRepository.findByRegisterDate(registerDate)).thenReturn(newArrayList(informantRegisterEntity));


        final JsonEnvelope informantRegisterRequests = informantRegisterDocumentRequestQueryView.getInformantRegistersByRequestDate(envelope);
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests").size(), is(1));
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0).getString("prosecutionAuthorityId"), is(prosecutionAuthorityId.toString()));
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0).getString("registerDate"), is(registerDate.toString()));
    }

    @Test
    public void shouldConvertOffenceVerdictCodeToVerdictObjectWhenOnlyVerdictCodeIsPresent() {
        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        final JsonObject converted = createObjectBuilder()
                .add("payload", payloadWithOffence(createObjectBuilder().add("verdictCode", "G")))
                .build();

        final JsonEnvelope envelope = recordedRequestEnvelope();
        when(objectToJsonObjectConverter.convert(informantRegisterEntity)).thenReturn(converted);
        when(informantRegisterRepository.findByStatusRecorded()).thenReturn(Collections.singletonList(informantRegisterEntity));

        final JsonObject offence = firstOffence(informantRegisterDocumentRequestQueryView.getInformantRegisterRequests(envelope));

        assertThat(offence.containsKey("verdictCode"), is(false));
        assertThat(offence.getJsonObject("verdict").getString("verdictCode"), is("G"));
        assertThat(offence.getJsonObject("verdict").isNull("verdictType"), is(true));
        assertThat(offence.getJsonObject("verdict").isNull("verdictDate"), is(true));
    }

    @Test
    public void shouldKeepVerdictObjectAsIsWhenAlreadyPresent() {
        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        final JsonObject verdictObject = createObjectBuilder()
                .add("verdictCode", "G")
                .add("verdictType", "FOUND_GUILTY")
                .add("verdictDate", "2026-04-13")
                .build();
        final JsonObject converted = createObjectBuilder()
                .add("payload", payloadWithOffence(createObjectBuilder().add("verdict", verdictObject)))
                .build();

        final JsonEnvelope envelope = recordedRequestEnvelope();
        when(objectToJsonObjectConverter.convert(informantRegisterEntity)).thenReturn(converted);
        when(informantRegisterRepository.findByStatusRecorded()).thenReturn(Collections.singletonList(informantRegisterEntity));

        final JsonObject offence = firstOffence(informantRegisterDocumentRequestQueryView.getInformantRegisterRequests(envelope));

        assertThat(offence.containsKey("verdictCode"), is(false));
        assertThat(offence.getJsonObject("verdict").getString("verdictCode"), is("G"));
        assertThat(offence.getJsonObject("verdict").getString("verdictType"), is("FOUND_GUILTY"));
        assertThat(offence.getJsonObject("verdict").getString("verdictDate"), is("2026-04-13"));
    }

    @Test
    public void shouldConvertOffenceVerdictCodeToVerdictObjectForByDateQueryWhenOnlyVerdictCodeIsPresent() {
        final LocalDate registerDate = now();
        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        final JsonObject converted = createObjectBuilder()
                .add("payload", payloadWithOffence(createObjectBuilder().add("verdictCode", "G")))
                .build();

        final JsonObject payload = createObjectBuilder().add("registerDate", registerDate.toString()).build();
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("results.query.informant-register-document-by-request-date").build(), payload);

        when(objectToJsonObjectConverter.convert(informantRegisterEntity)).thenReturn(converted);
        when(informantRegisterRepository.findByRegisterDate(registerDate)).thenReturn(newArrayList(informantRegisterEntity));

        final JsonObject offence = firstOffence(informantRegisterDocumentRequestQueryView.getInformantRegistersByRequestDate(envelope));

        assertThat(offence.containsKey("verdictCode"), is(false));
        assertThat(offence.getJsonObject("verdict").getString("verdictCode"), is("G"));
        assertThat(offence.getJsonObject("verdict").isNull("verdictType"), is(true));
        assertThat(offence.getJsonObject("verdict").isNull("verdictDate"), is(true));
    }

    private JsonEnvelope recordedRequestEnvelope() {
        return envelopeFrom(metadataBuilder().withId(randomUUID())
                        .withName("results.query.informant-register-document-request").build(),
                createObjectBuilder().add("requestStatus", RECORDED.name()).build());
    }

    private String payloadWithOffence(final javax.json.JsonObjectBuilder offenceBuilder) {
        final JsonObject offence = offenceBuilder
                .add("offenceCode", "PS90010")
                .add("orderIndex", 1)
                .add("offenceTitle", "An offence")
                .build();
        final JsonObject caseOrApplication = createObjectBuilder()
                .add("caseOrApplicationReference", "TFL4359536")
                .add("offences", createArrayBuilder().add(offence).build())
                .build();
        final JsonObject defendant = createObjectBuilder()
                .add("name", "Fred Smith")
                .add("address1", "Flat 1")
                .add("prosecutionCasesOrApplications", createArrayBuilder().add(caseOrApplication).build())
                .build();
        final JsonObject hearing = createObjectBuilder()
                .add("courtRoom", "Room name")
                .add("hearingStartTime", "2020-03-12")
                .add("defendants", createArrayBuilder().add(defendant).build())
                .build();
        final JsonObject hearingVenue = createObjectBuilder()
                .add("courtHouse", "Lavender Hill Magistrates' Court")
                .add("courtSessions", createArrayBuilder().add(hearing).build())
                .build();
        return createObjectBuilder()
                .add("fileName", "InformantRegister_TFL_2020-04-12.csv")
                .add("hearingVenue", hearingVenue)
                .build()
                .toString();
    }

    private JsonObject firstOffence(final JsonEnvelope informantRegisterRequests) {
        final String payload = informantRegisterRequests.payloadAsJsonObject()
                .getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0)
                .getString("payload");
        return stringToJsonObjectConverter.convert(payload)
                .getJsonObject("hearingVenue")
                .getJsonArray("courtSessions").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("prosecutionCasesOrApplications").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0);
    }
}
