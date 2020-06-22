package uk.gov.moj.cpp.results.event.service;

import static com.jayway.jsonassert.JsonAssert.with;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import javax.json.JsonObject;

import org.apache.activemq.artemis.utils.Env;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
@RunWith(MockitoJUnitRunner.class)
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
    @InjectMocks
    private NotificationNotifyService notificationNotifyService;

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

        final ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor = forClass((Class)Envelope.class);

        verify(sender).sendAsAdmin(envelopeCaptor.capture());

        final Metadata metadata = envelopeCaptor.getValue().metadata();

        assertThat(metadata.name(), is(NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE));

        final JsonObject payload = envelopeCaptor.getValue().payload();

        with(payload.toString())
                .assertThat(FIELD_TEMPLATE_ID, is(POLICE_TEMPLATE_ID))
                .assertThat(FIELD_SEND_TO_ADDRESS, is(EMAIL_ADDRESS))
        ;
    }
}