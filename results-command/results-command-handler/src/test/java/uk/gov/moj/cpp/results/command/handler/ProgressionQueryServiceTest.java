package uk.gov.moj.cpp.results.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.results.command.service.ProgressionQueryService;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ProgressionQueryServiceTest {

    @Mock
    private Requester requester;

    @InjectMocks
    private ProgressionQueryService progressionQueryService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Class> classArgumentCaptor;

    @Test
    public void shouldRequestForMemberCasesByGroupId() {
        final String groupId = randomUUID().toString();

        progressionQueryService.getGroupMemberCases(envelope()
                        .with(metadataWithRandomUUIDAndName())
                        .build(),
                groupId);

        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture(), classArgumentCaptor.capture());

        assertThat(classArgumentCaptor.getValue().getName(), is(JsonObject.class.getName()));
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.query.group-member-cases"));
        assertThat(envelopeArgumentCaptor.getValue().payload(), is(payloadIsJson(withJsonPath("$.groupId", is(groupId)))));
        verifyNoMoreInteractions(requester);
    }
}