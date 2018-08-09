package uk.gov.moj.cpp.results.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.results.shareResults.ShareResultsMessage;
import uk.gov.moj.cpp.results.persist.entity.Defendant;
import uk.gov.moj.cpp.results.persist.entity.Hearing;
import uk.gov.moj.cpp.results.persist.entity.HearingResult;
import uk.gov.moj.cpp.results.persist.entity.VariantDirectory;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultsConverterTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private static ShareResultsMessage shareResultsMessage;

    @BeforeClass
    public static void init() {
        shareResultsMessage = TestTemplates.basicShareResultsTemplate();
    }

    @Before
    public void setUp() {
        initMocks(this);
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void should_convert_the_current_payload_with_defendants_to_defendant_entities() {

        final JsonObject payload = envelopeFrom(metadataOf(UUID.randomUUID(), "results.add-hearing-result"), objectToJsonObjectConverter.convert(shareResultsMessage)).payloadAsJsonObject();

        final Map<String, Object> hearingResultsMap = HearingResultsConverter.withJsonObject(payload).convert();

        assertThat(hearingResultsMap, is(notNullValue()));

        final Set<Defendant> expectedPersons = (Set<Defendant>) hearingResultsMap.get(HearingResultsConverter.DEFENDANTS);

        final Set<uk.gov.moj.cpp.domains.results.shareResults.Person> givenPersons = shareResultsMessage.getHearing().getDefendants().stream().map(d -> d.getPerson()).distinct().collect(Collectors.toSet());

        assertThat(expectedPersons.size(), is(givenPersons.size()));

        expectedPersons.forEach(expectedPerson -> {
            final uk.gov.moj.cpp.domains.results.shareResults.Person givenPerson = givenPersons.stream()
                    .filter(p -> p.getId().equals(expectedPerson.getId()))
                    .findFirst()
                    .get();
            assertThat(givenPerson, is(notNullValue()));
            assertThat(expectedPerson.getId(), is(givenPerson.getId()));
            assertThat(expectedPerson.getHearingId(), is(hearingResultsMap.get("hearingId")));
            assertThat(expectedPerson.getFirstName(), is(givenPerson.getFirstName()));
            assertThat(expectedPerson.getLastName(), is(givenPerson.getLastName()));
            assertThat(expectedPerson.getDateOfBirth(), is(givenPerson.getDateOfBirth()));
            assertThat(expectedPerson.getAddress1(), is(givenPerson.getAddress().getAddress1()));
            assertThat(expectedPerson.getAddress2(), is(givenPerson.getAddress().getAddress2()));
            assertThat(expectedPerson.getAddress3(), is(givenPerson.getAddress().getAddress3()));
            assertThat(expectedPerson.getAddress4(), is(givenPerson.getAddress().getAddress4()));
            assertThat(expectedPerson.getPostCode(), is(givenPerson.getAddress().getPostCode()));
        });
    }

    @Test
    public void should_convert_the_current_payload_with_hearing_to_hearing_entities() {

        final JsonObject payload = envelopeFrom(metadataOf(UUID.randomUUID(), "results.add-hearing-result"), objectToJsonObjectConverter.convert(shareResultsMessage)).payloadAsJsonObject();

        final Map<String, Object> hearingResultsMap = HearingResultsConverter.withJsonObject(payload).convert();

        assertThat(hearingResultsMap, is(notNullValue()));

        final Set<Hearing> expectedHearings = (Set<Hearing>) hearingResultsMap.get(HearingResultsConverter.HEARINGS);

        assertThat(expectedHearings.stream().map(Hearing::getId).distinct().count(), is(Stream.of(shareResultsMessage).map(ShareResultsMessage::getHearing).distinct().count()));
        assertThat(expectedHearings.stream().map(Hearing::getPersonId).distinct().count(), is(Stream.of(shareResultsMessage).flatMap(s -> s.getHearing().getDefendants().stream()).map(d -> d.getPerson().getId()).distinct().count()));

        final Hearing expectedHearing = expectedHearings.stream().findFirst().get();

        assertThat(expectedHearing.getId(), is(shareResultsMessage.getHearing().getId()));
        assertThat(expectedHearing.getHearingType(), is(shareResultsMessage.getHearing().getHearingType()));
        assertThat(expectedHearing.getStartDate(), is(shareResultsMessage.getHearing().getStartDateTime().toLocalDate()));
    }

    @Test
    public void should_convert_the_current_payload_with_result_lines_to_result_line_entities() {

        final JsonObject payload = envelopeFrom(metadataOf(UUID.randomUUID(), "results.add-hearing-result"), objectToJsonObjectConverter.convert(shareResultsMessage)).payloadAsJsonObject();

        final Map<String, Object> hearingResultsMap = HearingResultsConverter.withJsonObject(payload).convert();

        assertThat(hearingResultsMap, is(notNullValue()));

        final List<HearingResult> expectedResultLines =  (List<HearingResult>) hearingResultsMap.get(HearingResultsConverter.HEARING_RESULTS);

        assertThat(expectedResultLines.size(), is(shareResultsMessage.getHearing().getSharedResultLines().size()));

        expectedResultLines.forEach(expectedResultLine -> {
            final uk.gov.moj.cpp.domains.results.shareResults.SharedResultLine givenSharedResultLine = shareResultsMessage.getHearing().getSharedResultLines().stream()
                    .filter(p -> p.getId().equals(expectedResultLine.getId()))
                    .findFirst()
                    .get();
            assertThat(givenSharedResultLine, is(notNullValue()));
            assertThat(expectedResultLine.getId(), is(givenSharedResultLine.getId()));
            assertThat(expectedResultLine.getCaseId(), is(givenSharedResultLine.getCaseId()));
            assertThat(expectedResultLine.getOffenceId(), is(givenSharedResultLine.getOffenceId()));
            assertThat(expectedResultLine.getResultLevel().name(), is(givenSharedResultLine.getLevel()));
            assertThat(expectedResultLine.getResultLabel(), is(givenSharedResultLine.getLabel()));
            assertThat(expectedResultLine.getResultPrompts().size(), is(givenSharedResultLine.getPrompts().size()));
            assertThat(expectedResultLine.getConvictionDate(), notNullValue());
            assertThat(expectedResultLine.getVerdictDate(), notNullValue());
            assertThat(expectedResultLine.getVerdictCategory(), notNullValue());
            assertThat(expectedResultLine.getVerdictDescription(), notNullValue());
            assertThat(expectedResultLine.getOrderedDate(), notNullValue());
            assertThat(expectedResultLine.getClerkOfTheCourtId(), is(givenSharedResultLine.getCourtClerk().getId()));
            assertThat(expectedResultLine.getClerkOfTheCourtFirstName(), is(givenSharedResultLine.getCourtClerk().getFirstName()));
            assertThat(expectedResultLine.getClerkOfTheCourtLastName(), is(givenSharedResultLine.getCourtClerk().getLastName()));
            assertThat(expectedResultLine.getLastSharedDateTime().toString(), is(ZonedDateTimes.toString(givenSharedResultLine.getLastSharedDateTime())));
            expectedResultLine.getResultPrompts().forEach(expectedPrompt -> {
                final uk.gov.moj.cpp.domains.results.shareResults.Prompt givenPrompt = givenSharedResultLine.getPrompts().stream()
                        .filter(p -> p.getId().equals(expectedPrompt.getId()))
                        .findFirst()
                        .get();
                assertThat(expectedPrompt.getId(), is(givenPrompt.getId()));
                assertThat(expectedPrompt.getLabel(), is(givenPrompt.getLabel()));
                assertThat(expectedPrompt.getValue(), is(givenPrompt.getValue()));
            });
        });
    }

    @Test
    public void should_convert_the_current_payload_with_variant_to_variant_entities() {

        final JsonObject payload = envelopeFrom(metadataOf(UUID.randomUUID(), "results.add-hearing-result"), objectToJsonObjectConverter.convert(shareResultsMessage)).payloadAsJsonObject();

        final Map<String, Object> hearingResultsMap = HearingResultsConverter.withJsonObject(payload).convert();

        assertThat(hearingResultsMap, is(notNullValue()));

        final List<VariantDirectory> expectedVariantDirectories = (List<VariantDirectory>) hearingResultsMap.get(HearingResultsConverter.VARIANTS);

        assertThat(expectedVariantDirectories.stream().map(VariantDirectory::getMaterialId).distinct().count(), is(Stream.of(shareResultsMessage).map(ShareResultsMessage::getVariants).distinct().count()));

        final VariantDirectory expectedVariantDirectory = expectedVariantDirectories.stream().findFirst().get();

        assertThat(expectedVariantDirectory.getStatus(), is("BUILDING"));
        assertThat(expectedVariantDirectory.getTemplateName(), is(shareResultsMessage.getVariants().get(0).getTemplateName()));
        assertThat(expectedVariantDirectory.getDescription(), is(shareResultsMessage.getVariants().get(0).getDescription()));
        assertThat(expectedVariantDirectory.getMaterialId(), is(shareResultsMessage.getVariants().get(0).getMaterialId()));
        assertThat(expectedVariantDirectory.getNowsTypeId(), is(shareResultsMessage.getVariants().get(0).getKey().getNowsTypeId()));
        assertThat(expectedVariantDirectory.getDefendantId(), is(shareResultsMessage.getVariants().get(0).getKey().getDefendantId()));
        assertThat(expectedVariantDirectory.getHearingId(), is(shareResultsMessage.getVariants().get(0).getKey().getHearingId()));
    }
}
