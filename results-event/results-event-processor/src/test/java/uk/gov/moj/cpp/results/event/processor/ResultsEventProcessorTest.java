package uk.gov.moj.cpp.results.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.shareResults.DefenceAdvocate;
import uk.gov.moj.cpp.domains.results.shareResults.ShareResultsMessage;
import uk.gov.moj.cpp.results.test.TestTemplates;

@RunWith(DataProviderRunner.class)
public class ResultsEventProcessorTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private Sender sender;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private ResultsEventProcessor resultsEventProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Before
    public void setUp() {
        initMocks(this);
        setField(jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void hearingResulted_shouldForwardAsIsAsPrivateEvent() {

        final ShareResultsMessage shareResultsMessage = TestTemplates.basicShareResultsTemplate();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(shareResultsMessage));

        resultsEventProcessor.hearingResulted(envelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());

        assertThat(
                envelopeArgumentCaptor.getValue(), jsonEnvelope(
                        metadata().withName("results.add-hearing-result"),
                        payloadIsJson(allOf(
                                withJsonPath("$.sharedTime", is(ZonedDateTimes.toString(shareResultsMessage.getSharedTime()))),
                                withJsonPath("$.hearing.id", is(shareResultsMessage.getHearing().getId().toString())),
                                withJsonPath("$.hearing.hearingType", is(shareResultsMessage.getHearing().getHearingType())),
                                withJsonPath("$.hearing.startDateTime", is(ZonedDateTimes.toString(shareResultsMessage.getHearing().getStartDateTime()))),
                                withJsonPath("$.hearing.courtCentre.courtCentreId", is(shareResultsMessage.getHearing().getCourtCentre().getCourtCentreId().toString())),
                                withJsonPath("$.hearing.courtCentre.courtCentreName", is(shareResultsMessage.getHearing().getCourtCentre().getCourtCentreName())),
                                withJsonPath("$.hearing.courtCentre.courtRoomId", is(shareResultsMessage.getHearing().getCourtCentre().getCourtRoomId().toString())),
                                withJsonPath("$.hearing.courtCentre.courtRoomName", is(shareResultsMessage.getHearing().getCourtCentre().getCourtRoomName())),

                                withJsonPath("$.hearing.attendees[0].personId", is(shareResultsMessage.getHearing().getAttendees().get(0).getPersonId().toString())),
                                withJsonPath("$.hearing.attendees[0].firstName", is(shareResultsMessage.getHearing().getAttendees().get(0).getFirstName())),
                                withJsonPath("$.hearing.attendees[0].lastName", is(shareResultsMessage.getHearing().getAttendees().get(0).getLastName())),
                                withJsonPath("$.hearing.attendees[0].type", is("JUDGE")),
                                withJsonPath("$.hearing.attendees[0].title", is(shareResultsMessage.getHearing().getAttendees().get(0).getTitle())),

                                withJsonPath("$.hearing.attendees[1].personId", is(shareResultsMessage.getHearing().getAttendees().get(1).getPersonId().toString())),
                                withJsonPath("$.hearing.attendees[1].firstName", is(shareResultsMessage.getHearing().getAttendees().get(1).getFirstName())),
                                withJsonPath("$.hearing.attendees[1].lastName", is(shareResultsMessage.getHearing().getAttendees().get(1).getLastName())),
                                withJsonPath("$.hearing.attendees[1].type", is("COURTCLERK")),
                                withJsonPath("$.hearing.attendees[1].title", is(shareResultsMessage.getHearing().getAttendees().get(1).getTitle())),

                                withJsonPath("$.hearing.attendees[2].firstName", is(shareResultsMessage.getHearing().getAttendees().get(2).getFirstName())),
                                withJsonPath("$.hearing.attendees[2].lastName", is(shareResultsMessage.getHearing().getAttendees().get(2).getLastName())),
                                withJsonPath("$.hearing.attendees[2].type", is("DEFENCEADVOCATE")),
                                withJsonPath("$.hearing.attendees[2].title", is(shareResultsMessage.getHearing().getAttendees().get(2).getTitle())),
                                withJsonPath("$.hearing.attendees[2].defendantIds[0]", is(((DefenceAdvocate)shareResultsMessage.getHearing().getAttendees().get(2)).getDefendantIds().get(0).toString())),
                                withJsonPath("$.hearing.attendees[2].status", is(((DefenceAdvocate)shareResultsMessage.getHearing().getAttendees().get(2)).getStatus())),

                                withJsonPath("$.hearing.defendants[0].id", is(shareResultsMessage.getHearing().getDefendants().get(0).getId().toString())),
                                withJsonPath("$.hearing.defendants[0].person.id", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getId().toString())),
                                withJsonPath("$.hearing.defendants[0].person.firstName", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getFirstName())),
                                withJsonPath("$.hearing.defendants[0].person.lastName", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getLastName())),
                                withJsonPath("$.hearing.defendants[0].person.dateOfBirth", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getDateOfBirth().toString())),
                                withJsonPath("$.hearing.defendants[0].person.address.address1", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getAddress().getAddress1())),
                                withJsonPath("$.hearing.defendants[0].person.address.address2", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getAddress().getAddress2())),
                                withJsonPath("$.hearing.defendants[0].person.address.address3", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getAddress().getAddress3())),
                                withJsonPath("$.hearing.defendants[0].person.address.address4", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getAddress().getAddress4())),
                                withJsonPath("$.hearing.defendants[0].person.address.postCode", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getAddress().getPostCode())),
                                withJsonPath("$.hearing.defendants[0].person.nationality", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getNationality())),
                                withJsonPath("$.hearing.defendants[0].person.homeTelephone", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getHomeTelephone())),
                                withJsonPath("$.hearing.defendants[0].person.workTelephone", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getWorkTelephone())),
                                withJsonPath("$.hearing.defendants[0].person.mobile", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getMobile())),
                                withJsonPath("$.hearing.defendants[0].person.fax", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getFax())),
                                withJsonPath("$.hearing.defendants[0].person.email", is(shareResultsMessage.getHearing().getDefendants().get(0).getPerson().getEmail())),

                                withJsonPath("$.hearing.defendants[0].defenceOrganisation", is(shareResultsMessage.getHearing().getDefendants().get(0).getDefenceOrganisation())),
                                withJsonPath("$.hearing.defendants[0].interpreter.name", is(shareResultsMessage.getHearing().getDefendants().get(0).getInterpreter().getName())),
                                withJsonPath("$.hearing.defendants[0].interpreter.language", is(shareResultsMessage.getHearing().getDefendants().get(0).getInterpreter().getLanguage())),

                                withJsonPath("$.hearing.defendants[0].cases[0].id", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getId().toString())),
                                withJsonPath("$.hearing.defendants[0].cases[0].urn", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getUrn())),
                                withJsonPath("$.hearing.defendants[0].cases[0].bailStatus", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getBailStatus())),
                                withJsonPath("$.hearing.defendants[0].cases[0].custodyTimeLimitDate", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getCustodyTimeLimitDate().toString())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].id", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getId().toString())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].code", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getCode())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].convictionDate", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getConvictionDate().toString())),

                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].plea.id", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getPlea().getId().toString())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].plea.date", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getPlea().getDate().toString())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].plea.value", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getPlea().getValue())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].plea.enteredHearingId", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getPlea().getEnteredHearingId().toString())),

                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].verdict.verdictDescription", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getVerdict().getVerdictDescription())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].verdict.verdictCategory", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getVerdict().getVerdictCategory())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].verdict.numberOfSplitJurors", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getVerdict().getNumberOfSplitJurors())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].verdict.verdictDate", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getVerdict().getVerdictDate().toString())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].verdict.numberOfJurors", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getVerdict().getNumberOfJurors())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].verdict.unanimous", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getVerdict().isUnanimous())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].verdict.enteredHearingId", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getVerdict().getEnteredHearingId().toString())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].wording", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getWording())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].startDate", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getStartDate().toString())),
                                withJsonPath("$.hearing.defendants[0].cases[0].offences[0].endDate", is(shareResultsMessage.getHearing().getDefendants().get(0).getCases().get(0).getOffences().get(0).getEndDate().toString())),

                                withJsonPath("$.hearing.sharedResultLines[0].id", is(shareResultsMessage.getHearing().getSharedResultLines().get(0).getId().toString())),
                                withJsonPath("$.hearing.sharedResultLines[0].caseId", is(shareResultsMessage.getHearing().getSharedResultLines().get(0).getCaseId().toString())),
                                withJsonPath("$.hearing.sharedResultLines[0].defendantId", is(shareResultsMessage.getHearing().getSharedResultLines().get(0).getDefendantId().toString())),
                                withJsonPath("$.hearing.sharedResultLines[0].offenceId", is(shareResultsMessage.getHearing().getSharedResultLines().get(0).getOffenceId().toString())),
                                withJsonPath("$.hearing.sharedResultLines[0].level", is("OFFENCE")),
                                withJsonPath("$.hearing.sharedResultLines[0].label", is(shareResultsMessage.getHearing().getSharedResultLines().get(0).getLabel())),
                                withJsonPath("$.hearing.sharedResultLines[0].prompts[0].label", is(shareResultsMessage.getHearing().getSharedResultLines().get(0).getPrompts().get(0).getLabel())),
                                withJsonPath("$.hearing.sharedResultLines[0].prompts[0].value", is(shareResultsMessage.getHearing().getSharedResultLines().get(0).getPrompts().get(0).getValue())),

                                withJsonPath("$.hearing.sharedResultLines[1].id", is(shareResultsMessage.getHearing().getSharedResultLines().get(1).getId().toString())),
                                withJsonPath("$.hearing.sharedResultLines[1].caseId", is(shareResultsMessage.getHearing().getSharedResultLines().get(1).getCaseId().toString())),
                                withJsonPath("$.hearing.sharedResultLines[1].defendantId", is(shareResultsMessage.getHearing().getSharedResultLines().get(1).getDefendantId().toString())),
                                withJsonPath("$.hearing.sharedResultLines[1].offenceId", is(shareResultsMessage.getHearing().getSharedResultLines().get(1).getOffenceId().toString())),
                                withJsonPath("$.hearing.sharedResultLines[1].level", is("DEFENDANT")),
                                withJsonPath("$.hearing.sharedResultLines[1].label", is(shareResultsMessage.getHearing().getSharedResultLines().get(1).getLabel())),
                                withJsonPath("$.hearing.sharedResultLines[1].prompts[0].label", is(shareResultsMessage.getHearing().getSharedResultLines().get(1).getPrompts().get(0).getLabel())),
                                withJsonPath("$.hearing.sharedResultLines[1].prompts[0].value", is(shareResultsMessage.getHearing().getSharedResultLines().get(1).getPrompts().get(0).getValue())),

                                withJsonPath("$.hearing.sharedResultLines[2].id", is(shareResultsMessage.getHearing().getSharedResultLines().get(2).getId().toString())),
                                withJsonPath("$.hearing.sharedResultLines[2].caseId", is(shareResultsMessage.getHearing().getSharedResultLines().get(2).getCaseId().toString())),
                                withJsonPath("$.hearing.sharedResultLines[2].defendantId", is(shareResultsMessage.getHearing().getSharedResultLines().get(2).getDefendantId().toString())),
                                withJsonPath("$.hearing.sharedResultLines[2].offenceId", is(shareResultsMessage.getHearing().getSharedResultLines().get(2).getOffenceId().toString())),
                                withJsonPath("$.hearing.sharedResultLines[2].level", is("CASE")),
                                withJsonPath("$.hearing.sharedResultLines[2].label", is(shareResultsMessage.getHearing().getSharedResultLines().get(2).getLabel())),
                                withJsonPath("$.hearing.sharedResultLines[2].prompts[0].label", is(shareResultsMessage.getHearing().getSharedResultLines().get(2).getPrompts().get(0).getLabel())),
                                withJsonPath("$.hearing.sharedResultLines[2].prompts[0].value", is(shareResultsMessage.getHearing().getSharedResultLines().get(2).getPrompts().get(0).getValue())))
                        )
                )
        );
    }
}