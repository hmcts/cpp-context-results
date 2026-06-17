package uk.gov.moj.cpp.results.event;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest.informantRegisterDocumentRequest;
import static uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterHearingVenue.informantRegisterHearingVenue;
import static uk.gov.justice.results.courts.InformantRegisterNotified.informantRegisterNotified;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.GENERATED;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.NOTIFIED;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.RECORDED;

import uk.gov.justice.core.courts.InformantRegisterRecorded;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.results.courts.InformantRegisterGenerated;
import uk.gov.justice.results.courts.InformantRegisterNotified;
import uk.gov.justice.results.courts.InformantRegisterNotifiedV2;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.results.courts.InformantRegisterGeneratedV2;
import uk.gov.justice.results.courts.InformantRegisterRecordedV2;
import uk.gov.moj.cpp.results.persist.InformantRegisterRepository;
import uk.gov.moj.cpp.results.persist.entity.InformantRegisterEntity;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class InformantRegisterEventListenerTest {

    @Mock
    private InformantRegisterRepository informantRegisterRepository;

    @InjectMocks
    private InformantRegisterEventListener informantRegisterEventListener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSaveInformantRegisterRequested() {
        final UUID prosecutionAuthId = randomUUID();
        final String ouCode = randomAlphanumeric(10);
        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = informantRegisterDocumentRequest()
                .withProsecutionAuthorityId(prosecutionAuthId)
                .withProsecutionAuthorityOuCode(ouCode)
                .withRegisterDate(ZonedDateTime.now())
                .withHearingVenue(informantRegisterHearingVenue().build())
                .build();

        final InformantRegisterRecorded informantRegisterRecorded = new InformantRegisterRecorded(
                informantRegisterDocumentRequest,
                informantRegisterDocumentRequest.getProsecutionAuthorityId());

        informantRegisterEventListener.saveInformantRegister(envelopeFrom(metadataWithRandomUUID("results.event.informant-register-recorded"),
                objectToJsonObjectConverter.convert(informantRegisterRecorded)));

        final ArgumentCaptor<InformantRegisterEntity> informantRegisterRequestEntity = forClass(InformantRegisterEntity.class);
        verify(this.informantRegisterRepository).save(informantRegisterRequestEntity.capture());
        final InformantRegisterEntity savedInformantRegisterEntity = informantRegisterRequestEntity.getValue();
        final JsonObject jsonPayload = createReader(new StringReader(savedInformantRegisterEntity.getPayload())).readObject();
        final InformantRegisterDocumentRequest informantRegisterRequestSaved = jsonObjectToObjectConverter.convert(jsonPayload, InformantRegisterDocumentRequest.class);

        assertThat(savedInformantRegisterEntity.getProsecutionAuthorityId(), is(prosecutionAuthId));
        assertThat(informantRegisterRequestSaved.getProsecutionAuthorityId(), is(prosecutionAuthId));
        assertThat(informantRegisterRequestSaved.getProsecutionAuthorityOuCode(), is(ouCode));
        assertThat(savedInformantRegisterEntity.getStatus(), is(RECORDED));
    }

    @Test
    public void saveInformantRegisterV2_shouldSaveEntityFromLocalDocumentRequest() {
        final UUID prosecutionAuthId = randomUUID();
        final String ouCode = randomAlphanumeric(10);
        final ZonedDateTime registerDate = ZonedDateTime.parse("2026-04-13T09:00:00Z");

        final uk.gov.justice.results.courts.informantRegisterDocument.InformantRegisterDocumentRequest localDocumentRequest =
                uk.gov.justice.results.courts.informantRegisterDocument.InformantRegisterDocumentRequest.informantRegisterDocumentRequest()
                        .withProsecutionAuthorityId(prosecutionAuthId)
                        .withProsecutionAuthorityOuCode(ouCode)
                        .withRegisterDate(registerDate)
                        .build();

        final InformantRegisterRecordedV2 event = InformantRegisterRecordedV2.informantRegisterRecordedV2()
                .withProsecutionAuthorityId(prosecutionAuthId)
                .withInformantRegister(localDocumentRequest)
                .build();

        informantRegisterEventListener.saveInformantRegisterV2(envelopeFrom(
                metadataWithRandomUUID("results.event.informant-register-recorded-v2"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<InformantRegisterEntity> captor = forClass(InformantRegisterEntity.class);
        verify(informantRegisterRepository).save(captor.capture());
        final InformantRegisterEntity saved = captor.getValue();

        assertThat(saved.getProsecutionAuthorityId(), is(prosecutionAuthId));
        assertThat(saved.getProsecutionAuthorityOuCode(), is(ouCode));
        assertThat(saved.getStatus(), is(RECORDED));
        assertThat(saved.getPayload(), notNullValue());
    }

    @Test
    public void shouldSaveInformantRegisterGenerated() {
        final UUID prosecutionAuthId = randomUUID();
        // Use a fixed instant (in UTC) so the expected registerDate matches how the listener
        // derives it: the framework's JSON converter normalises ZonedDateTime to UTC, and the
        // listener calls getRegisterDate().toLocalDate(). Using LocalDate.now()/ZonedDateTime.now()
        // here makes the test flaky around the midnight boundary in non-UTC zones (e.g. BST).
        final ZonedDateTime registerDateTime = ZonedDateTime.parse("2026-04-13T09:00:00Z");
        final LocalDate registerDate = registerDateTime.toLocalDate();
        final UUID fileId = randomUUID();
        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = informantRegisterDocumentRequest()
                .withProsecutionAuthorityId(prosecutionAuthId)
                .withRegisterDate(registerDateTime)
                .withHearingVenue(informantRegisterHearingVenue().build())
                .build();

        final InformantRegisterGenerated informantRegisterGenerated = new InformantRegisterGenerated(
                singletonList(informantRegisterDocumentRequest),
                fileId, false);

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthId);
        informantRegisterEntity.setStatus(RECORDED);
        when(informantRegisterRepository.findByProsecutionAuthorityIdAndRegisterDateForStatusRecorded(prosecutionAuthId, registerDate)).thenReturn(singletonList(informantRegisterEntity));

        informantRegisterEventListener.generateInformantRegister(envelopeFrom(metadataWithRandomUUID("results.event.informant-register-generated"),
                objectToJsonObjectConverter.convert(informantRegisterGenerated)));

        assertThat(informantRegisterEntity.getProcessedOn().toString(), is(notNullValue()));
        assertThat(informantRegisterEntity.getStatus(), is(GENERATED));
    }

    @Test
    public void generateInformantRegisterV2_shouldSetEntityStatusToGenerated() {
        final UUID prosecutionAuthId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.parse("2026-04-13T09:00:00Z");

        final uk.gov.justice.results.courts.informantRegisterDocument.InformantRegisterDocumentRequest localDocumentRequest =
                uk.gov.justice.results.courts.informantRegisterDocument.InformantRegisterDocumentRequest.informantRegisterDocumentRequest()
                        .withProsecutionAuthorityId(prosecutionAuthId)
                        .withRegisterDate(registerDate)
                        .build();

        final InformantRegisterGeneratedV2 event = InformantRegisterGeneratedV2.informantRegisterGeneratedV2()
                .withInformantRegisterDocumentRequests(singletonList(localDocumentRequest))
                .withSystemGenerated(false)
                .build();

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthId);
        informantRegisterEntity.setStatus(RECORDED);
        when(informantRegisterRepository.findByProsecutionAuthorityIdAndRegisterDateForStatusRecorded(
                prosecutionAuthId, registerDate.toLocalDate())).thenReturn(singletonList(informantRegisterEntity));

        informantRegisterEventListener.generateInformantRegisterV2(envelopeFrom(
                metadataWithRandomUUID("results.event.informant-register-generated-v2"),
                objectToJsonObjectConverter.convert(event)));

        assertThat(informantRegisterEntity.getStatus(), is(GENERATED));
        assertThat(informantRegisterEntity.getProcessedOn(), notNullValue());
    }

    @Test
    public void saveInformantRegister_withPreMigrationFlatVerdictCode_shouldNotThrow() {
        final UUID prosecutionAuthId = randomUUID();
        final JsonObject offenceJson = createObjectBuilder()
                .add("offenceCode", "PS90010")
                .add("orderIndex", 1)
                .add("offenceTitle", "Theft")
                .add("pleaValue", "NOT_GUILTY")
                .add("verdictCode", "G")
                .build();
        final JsonObject informantRegisterDocumentRequestJson = createObjectBuilder()
                .add("prosecutionAuthorityId", prosecutionAuthId.toString())
                .add("registerDate", "2026-04-13T09:00:00Z")
                .add("hearingDate", "2026-04-13T09:00:00Z")
                .add("hearingId", randomUUID().toString())
                .add("prosecutionAuthorityCode", "TFL")
                .add("fileName", "test.csv")
                .add("hearingVenue", createObjectBuilder()
                        .add("courtHouse", "Crown Court")
                        .add("ljaName", "LJA")
                        .add("courtSessions", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("courtRoom", "Room 1")
                                        .add("hearingStartTime", "2026-04-13T09:00:00Z")
                                        .add("defendants", createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("name", "John Smith")
                                                        .add("address1", "1 High St")
                                                        .add("firstName", "John")
                                                        .add("lastName", "Smith")
                                                        .add("prosecutionCasesOrApplications", createArrayBuilder()
                                                                .add(createObjectBuilder()
                                                                        .add("caseOrApplicationReference", "TFL123")
                                                                        .add("offences", createArrayBuilder().add(offenceJson)))))))))
                .build();
        final JsonObject payload = createObjectBuilder()
                .add("informantRegister", informantRegisterDocumentRequestJson)
                .add("prosecutionAuthorityId", prosecutionAuthId.toString())
                .build();

        final ArgumentCaptor<InformantRegisterEntity> captor = forClass(InformantRegisterEntity.class);

        informantRegisterEventListener.saveInformantRegister(envelopeFrom(
                metadataWithRandomUUID("results.event.informant-register-recorded"), payload));

        verify(informantRegisterRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus(), is(RECORDED));
        assertThat(captor.getValue().getProsecutionAuthorityId(), is(prosecutionAuthId));
    }

    @Test
    public void generateInformantRegister_withPreMigrationFlatVerdictCode_shouldNotThrow() {
        final UUID prosecutionAuthId = randomUUID();
        final LocalDate registerDate = LocalDate.parse("2026-04-13");
        final JsonObject offenceJson = createObjectBuilder()
                .add("offenceCode", "PS90010")
                .add("orderIndex", 1)
                .add("offenceTitle", "Theft")
                .add("pleaValue", "NOT_GUILTY")
                .add("verdictCode", "G")
                .build();
        final JsonObject documentRequestJson = createObjectBuilder()
                .add("prosecutionAuthorityId", prosecutionAuthId.toString())
                .add("registerDate", "2026-04-13T09:00:00Z")
                .add("hearingDate", "2026-04-13T09:00:00Z")
                .add("hearingId", randomUUID().toString())
                .add("prosecutionAuthorityCode", "TFL")
                .add("fileName", "test.csv")
                .add("hearingVenue", createObjectBuilder()
                        .add("courtHouse", "Crown Court")
                        .add("ljaName", "LJA")
                        .add("courtSessions", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("courtRoom", "Room 1")
                                        .add("hearingStartTime", "2026-04-13T09:00:00Z")
                                        .add("defendants", createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("name", "John Smith")
                                                        .add("address1", "1 High St")
                                                        .add("firstName", "John")
                                                        .add("lastName", "Smith")
                                                        .add("prosecutionCasesOrApplications", createArrayBuilder()
                                                                .add(createObjectBuilder()
                                                                        .add("caseOrApplicationReference", "TFL123")
                                                                        .add("offences", createArrayBuilder().add(offenceJson)))))))))
                .build();
        final JsonObject payload = createObjectBuilder()
                .add("informantRegisterDocumentRequests", createArrayBuilder().add(documentRequestJson))
                .add("fileId", randomUUID().toString())
                .add("systemGenerated", false)
                .build();
        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthId);
        informantRegisterEntity.setStatus(RECORDED);
        when(informantRegisterRepository.findByProsecutionAuthorityIdAndRegisterDateForStatusRecorded(prosecutionAuthId, registerDate)).thenReturn(singletonList(informantRegisterEntity));

        informantRegisterEventListener.generateInformantRegister(envelopeFrom(
                metadataWithRandomUUID("results.event.informant-register-generated"), payload));

        assertThat(informantRegisterEntity.getStatus(), is(GENERATED));
    }

    @Test
    public void shouldNotifyInformantRegister() {
        final UUID prosecutionAuthId = randomUUID();
        final UUID fileId = randomUUID();

        final InformantRegisterNotified informantRegisterNotified = informantRegisterNotified()
                .withFileId(fileId)
                .withProsecutionAuthorityId(prosecutionAuthId)
                .build();

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();

        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthId);
        informantRegisterEntity.setStatus(GENERATED);

        when(informantRegisterRepository.findByProsecutionAuthorityIdAndStatusGenerated(prosecutionAuthId)).thenReturn(Lists.newArrayList(informantRegisterEntity));

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("results.event.informant-register-notified"),
                objectToJsonObjectConverter.convert(informantRegisterNotified)
        );
        informantRegisterEventListener.notifyInformantRegister(jsonEnvelope);
        assertThat(informantRegisterEntity.getStatus(), is(NOTIFIED));
        assertThat(informantRegisterEntity.getProcessedOn(), is(notNullValue()));
    }

    @Test
    public void shouldNotifyInformantRegisterV2() {
        final UUID prosecutionAuthId = randomUUID();
        final UUID fileId = randomUUID();
        final LocalDate registerDate = LocalDate.parse("2024-10-24");

        final InformantRegisterNotifiedV2 informantRegisterNotified = InformantRegisterNotifiedV2.informantRegisterNotifiedV2()
                .withFileId(fileId)
                .withProsecutionAuthorityId(prosecutionAuthId)
                .withRegisterDate(registerDate)
                .build();

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthId);
        informantRegisterEntity.setRegisterDate(registerDate);
        informantRegisterEntity.setStatus(GENERATED);

        when(informantRegisterRepository.findByProsecutionAuthorityIdAndRegisterDateAndStatusGenerated(prosecutionAuthId, registerDate)).thenReturn(Lists.newArrayList(informantRegisterEntity));

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("results.event.informant-register-notified-v2"),
                objectToJsonObjectConverter.convert(informantRegisterNotified)
        );
        informantRegisterEventListener.notifyInformantRegisterV2(jsonEnvelope);
        assertThat(informantRegisterEntity.getStatus(), is(NOTIFIED));
        assertThat(informantRegisterEntity.getProcessedOn(), is(notNullValue()));
    }
}
