package uk.gov.moj.cpp.results.event.service;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested.ncesEmailNotificationRequested;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;

import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationNotifyServiceTest {

    private static final String OU_CODE = "GFL123";
    private static final String URN = "urn123";
    private static final String EMAIL_ADDRESS = "test@hmcts.net";
    private static final String POLICE_TEMPLATE_ID = "781b970d-a13e-4440-97c3-ecf22a4540d5";
    private static final String FIELD_SEND_TO_ADDRESS = "sendToAddress";
    private static final String FIELD_TEMPLATE_ID = "templateId";
    private static final String NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE = "notificationnotify.send-email-notification";
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();
    @Mock
    private Sender sender;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private FileStorer fileStorer;
    @Mock
    private SystemDocGenerator systemDocGenerator;
    @InjectMocks
    private NotificationNotifyService notificationNotifyService;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldSendEmailNotification() {

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("results.event.police-notification-requested"),
                createObjectBuilder()
                        .add("ouCode", OU_CODE)
                        .add("urn", URN)
                        .build());

        final JsonObject jsonObject = createObjectBuilder()
                .add(FIELD_TEMPLATE_ID, POLICE_TEMPLATE_ID)
                .add(FIELD_SEND_TO_ADDRESS, EMAIL_ADDRESS)
                .build();

        notificationNotifyService.sendEmailNotification(event, jsonObject);

        final ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor = forClass((Class) Envelope.class);

        verify(sender).sendAsAdmin(envelopeCaptor.capture());

        final Metadata metadata = envelopeCaptor.getValue().metadata();

        assertThat(metadata.name(), is(NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE));

        final JsonObject payload = envelopeCaptor.getValue().payload();

        with(payload.toString())
                .assertThat(FIELD_TEMPLATE_ID, is(POLICE_TEMPLATE_ID))
                .assertThat(FIELD_SEND_TO_ADDRESS, is(EMAIL_ADDRESS));
    }

    @Test
    public void shouldSendData() {
        final EmailNotification emailNotification = EmailNotification.emailNotification()
                .withNotificationId(randomUUID())
                .withTemplateId(randomUUID())
                .withSendToAddress("sendToAddress")
                .withSubject("my subject")
                .withFileId(randomUUID())
                .withMaterialUrl(randomUUID().toString())
                .build();

        notificationNotifyService.sendNcesEmail(emailNotification, envelope());

        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope emailCommand = envelopeCaptor.getValue();
        assertThat(emailCommand.metadata().name(), Matchers.is("notificationnotify.send-email-notification"));
        final JsonObject payload = emailCommand.payloadAsJsonObject();
        assertThat(payload.size(), Matchers.is(5));
        assertThat(payload.getString("notificationId"), equalTo(emailNotification.getNotificationId().toString()));
        assertThat(payload.getString("templateId"), equalTo(emailNotification.getTemplateId().toString()));
        assertThat(payload.getString("sendToAddress"), equalTo(emailNotification.getSendToAddress()));
        assertThat(payload.getJsonObject("personalisation").getString("subject"), equalTo("my subject"));
    }

    private JsonEnvelope envelope() {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("origin envelope"),
                createObjectBuilder()
        );
    }

    @Test
    public void shouldGenerateNotification() throws FileServiceException {
        final NcesEmailNotificationRequested ncesEmailNotificationRequested = ncesEmailNotificationRequested()
                .withCaseReferences("ref")
                .withDefendantName("name")
                .withGobAccountNumber("accNo")
                .withListedDate("15032021")
                .withDivisionCode("divCode")
                .withMasterDefendantId(randomUUID())
                .build();

        final JsonObject obj = createObjectBuilder()
                .add("caseReferenceNumber", "ref")
                .add("defendantName", "name")
                .build();
        when(objectToJsonObjectConverter.convert(any())).thenReturn(obj);
        when(fileStorer.store(any(), any())).thenReturn(randomUUID());
        doNothing().when(systemDocGenerator).generateDocument(any(), any());
        assertNotNull(notificationNotifyService.generateNotification(ncesEmailNotificationRequested, envelope()));
        verify(objectToJsonObjectConverter).convert(any());
        verify(fileStorer).store(any(), any());
        verify(systemDocGenerator).generateDocument(any(), any());
    }
}