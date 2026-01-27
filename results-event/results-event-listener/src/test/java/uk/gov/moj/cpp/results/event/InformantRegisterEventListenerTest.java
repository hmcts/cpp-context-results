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
    public void shouldSaveInformantRegisterGenerated() {
        final UUID prosecutionAuthId = randomUUID();
        final LocalDate registerDate = LocalDate.now();
        final UUID fileId = randomUUID();
        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = informantRegisterDocumentRequest()
                .withProsecutionAuthorityId(prosecutionAuthId)
                .withRegisterDate(ZonedDateTime.now())
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
