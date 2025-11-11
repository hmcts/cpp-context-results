package uk.gov.moj.cpp.results.event.service;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcsServiceTest {

    @Mock
    private Sender sender;
    @InjectMocks
    private DcsService dcsService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> argumentCaptor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Test
    void shouldGenerateDcsCaseRecord() {
        final DcsCreateCaseRequest request = DcsCreateCaseRequest.DcsCreateCaseRequestBuilder
                .aDcsCreateCaseRequest()
                .withCaseId(UUID.randomUUID())
                .withCaseUrn(UUID.randomUUID().toString())
                .withProsecutionAuthority("Test")
                .withDefendants(List.of(DcsDefendant.DcsDefendantBuilder.aDcsDefendant()
                                .withId(UUID.randomUUID())
                                .withBailStatus("BailStatus")
                                .withDefendantPerson(DefendantPerson.DefendantPersonBuilder.aDefendantPerson()
                                        .withSurname("surname")
                                        .withForename("forename")
                                        .withMiddleName("middlename")
                                        .withDateOfBirth(LocalDate.now().minusYears(20))
                                        .build())
                                .withDefendantOrganisation(DefendantOrganisation.DefendantOrganisationBuilder.aDefendantOrganisation()
                                        .withName("orgnisation name")
                                        .build())
                                .withInterpreterLanguage("english")
                                .withHearings(List.of(DcsHearing.Builder.newHearing()
                                        .withHearingDate(LocalDate.now())
                                        .build()))
                                .withOffenceDetails(OffenceDetails.Builder.newOffence()
                                        .withAddedOffences(Set.of(DcsOffence.Builder.newOffence()
                                                        .withOffenceCode("123")
                                                        .withOffenceId(UUID.randomUUID())
                                                .build()))
                                        .withRemovedOffences(Set.of(DcsOffence.Builder.newOffence()
                                                .withOffenceCode("124")
                                                .withOffenceId(UUID.randomUUID())
                                                .build()))
                                        .build())
                        .build()))
                .build();
        dcsService.createCase(request, envelope());

        verify(sender).sendAsAdmin(argumentCaptor.capture());
        final Envelope<JsonObject> actual = argumentCaptor.getValue();

        final JsonObject payload = objectToJsonObjectConverter.convert(request);
        assertThat(actual.payload(), equalTo(payload));
        assertThat(actual.metadata().name(), equalTo("stagingdcs.submit-dcs-case-record"));
    }
    @Test
    void shouldGenerateDcsCaseRecordWithOnlyMandatoryValues() {
        final DcsCreateCaseRequest request = DcsCreateCaseRequest.DcsCreateCaseRequestBuilder
                .aDcsCreateCaseRequest()
                .withCaseId(UUID.randomUUID())
                .withProsecutionAuthority("Test")
                .withDefendants(List.of(DcsDefendant.DcsDefendantBuilder.aDcsDefendant()
                                .withId(UUID.randomUUID())
                                .withBailStatus("BailStatus")
                                .withDefendantPerson(DefendantPerson.DefendantPersonBuilder.aDefendantPerson()
                                        .withSurname("surname")
                                        .withForename("forename")
                                        .build())
                                .withHearings(List.of(DcsHearing.Builder.newHearing()
                                        .withCourtCentre("court centre")
                                        .withHearingDate(LocalDate.now())
                                        .build()))
                                .withDefendantOrganisation(DefendantOrganisation.DefendantOrganisationBuilder.aDefendantOrganisation()
                                        .withName("organisation name")
                                        .build())
                                .withOffenceDetails(OffenceDetails.Builder.newOffence()
                                        .withAddedOffences(Set.of(DcsOffence.Builder.newOffence()
                                                        .withOffenceCode("123")
                                                        .withOffenceId(UUID.randomUUID())
                                                .build()))
                                        .withRemovedOffences(Set.of(DcsOffence.Builder.newOffence()
                                                .withOffenceId(UUID.randomUUID())
                                                .build()))
                                        .build())
                        .build()))
                .build();
        dcsService.createCase(request, envelope());

        verify(sender).sendAsAdmin(argumentCaptor.capture());
        final Envelope<JsonObject> actual = argumentCaptor.getValue();

        final JsonObject payload = objectToJsonObjectConverter.convert(request);
        assertThat(actual.payload(), equalTo(payload));
        assertThat(actual.metadata().name(), equalTo("stagingdcs.submit-dcs-case-record"));
    }

    private JsonEnvelope envelope() {
        return envelopeFrom(metadataWithRandomUUID("test envelope"), createObjectBuilder().build());
    }
}