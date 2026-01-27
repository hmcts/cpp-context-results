package uk.gov.moj.cpp.results.query.view.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserGoupsServiceTest {

    @InjectMocks
    private UserGroupsService service;

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Test
    public void testFindUserGroupsByUserIdReturnsUserGroups() {
        List<String> userGroupNames = Arrays.asList("Listing Office", "Court Clerk", "Prison Admin");
        final JsonEnvelope originalEnvelope = setupMocksAndStubData(userGroupNames);

        final List<String> userGroups = service.findUserGroupsByUserId(originalEnvelope);
        assertThat(userGroups, hasItems("Listing Office", "Court Clerk", "Prison Admin"));
    }

    private JsonEnvelope setupMocksAndStubData(List<String> userGroupNames) {
        final JsonArrayBuilder groupsArray = createArrayBuilder();
        Arrays.stream(userGroupNames.toArray()).forEach(userGroup ->
                groupsArray
                        .add(createObjectBuilder()
                                .add("groupId", UUID.randomUUID().toString())
                                .add("groupName", userGroup.toString()))
        );

        final JsonObject groupsPayload = createObjectBuilder().add("groups", groupsArray).build();

        final UUID userId = UUID.randomUUID();
        final JsonEnvelope originalEnvelope = envelopeFrom(
                metadataWithRandomUUID("usersgroups.get-groups-by-user").withUserId(userId.toString()),
                createObjectBuilder().add("userId", userId.toString()).build()
        );

        final JsonEnvelope userAndGroupsResponse = enveloper.withMetadataFrom(originalEnvelope, "usersgroups.get-groups-by-user").apply(groupsPayload);
        when(requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(originalEnvelope).withName("usersgroups.get-groups-by-user"),
                payloadIsJson(withJsonPath("$.userId", is(userId.toString())))
        )))).thenReturn(userAndGroupsResponse);

        return originalEnvelope;
    }
}