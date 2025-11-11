package uk.gov.moj.cpp.results.query.api;

import static org.mockito.Mockito.verify;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.query.view.NcesEmailNotificationQueryView;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NcesEmailNotificationQueryApiTest {

    @Mock
    private NcesEmailNotificationQueryView ncesEmailNotificationQueryView;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @InjectMocks
    private NcesEmailNotificationQueryApi ncesEmailNotificationQueryApi;

    @Test
    public void getNcesEmailNotificationDetails() {
        ncesEmailNotificationQueryApi.getNcesEmailNotificationDetails(jsonEnvelope);
        verify(ncesEmailNotificationQueryView).getNcesEmailNotificationDetails(jsonEnvelope);
    }
}
