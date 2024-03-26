package uk.gov.moj.cpp.results.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNull;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.gov.justice.core.courts.*;
import uk.gov.justice.core.courts.external.ApiAddress;
import uk.gov.justice.core.courts.external.ApiCourtCentre;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.AmendmentType;
import uk.gov.moj.cpp.results.domain.event.JudicialResultDetails;
import uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions;
import uk.gov.moj.cpp.results.it.utils.Queries;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummaryView;
import uk.gov.moj.cpp.results.test.TestTemplates;
import uk.gov.moj.cpp.results.test.matchers.BeanMatcher;

import javax.jms.JMSException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.InputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.LocalDate.now;
import static java.time.LocalDate.of;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createReader;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.*;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsWithPoliceNotificationRequested;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.stub.NotificationServiceStub.verifyEmailNotificationIsRaised;
import static uk.gov.moj.cpp.results.it.stub.ProgressionStub.stubGetProgressionCaseExistsByUrn;
import static uk.gov.moj.cpp.results.it.stub.ProgressionStub.stubGetProgressionProsecutionCases;
import static uk.gov.moj.cpp.results.it.utils.EventGridStub.stubEventGridEndpoint;
import static uk.gov.moj.cpp.results.it.utils.HttpClientUtil.sendGeneratePoliceResultsForADefendantCommand;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.*;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.stubNotificationNotifyEndPoint;
import static uk.gov.moj.cpp.results.test.TestTemplates.*;
import static uk.gov.moj.cpp.results.test.matchers.BeanMatcher.isBean;

@SuppressWarnings({"unchecked", "serial", "squid:S2925", "squid:S1607"})
public class HearingResultedIT {

    private static final String TEMPLATE_PAYLOAD_MAG = "json/public.events.hearing.hearing-resulted.json";
    private static final String TEMPLATE_PAYLOAD_RESHARE_MAG = "json/public.events.hearing.hearing-results-reshared.json";
    private static final String TEMPLATE_PAYLOAD_CROWN = "json/public.events.hearing.hearing-resulted-crown.json";
    private static final String TEMPLATE_PAYLOAD_RESHARE_CROWN = "json/public.events.hearing.hearing-results-reshared-crown.json";
    private static final String SESSION_ID = "sessionId";
    private static final String CASE_ID = "caseId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_ID_VALUE = "cccc1111-1e20-4c21-916a-81a6c90239e5";
    private static final String DEFENDANT_ID_VALUE = "dddd1111-1e20-4c21-916a-81a6c90239e5";
    private static final String EMAIL = "email@email.com";
    private static final String MAG_EMAIL = "mag@email.com";
    private static final String FINDING_VALUE_DEFAULT = "G";
    private static final String FINDING_VALUE_NOT_GUILTY = "N";
    private static final String OU_CODE = "0300000";
    private static final String PROSECUTION_AUTHORITY = "bdc190e7-c939-37ca-be4b-9f615d6ef40e";

    private static final String HEARING_RESULT_APPLICATION_ONLY_JURISDICTION_CROWN = "json/public.hearing-resulted_with_crown_judicialresults_in_application_only.json";
    private static final String HEARING_RESULT_RESHARE_APPLICATION_ONLY_JURISDICTION_CROWN = "json/public.hearing-result_reshare_crown_judicialresults_in_application_only.json";
    private static final String HEARING_RESULT_APPLICATION_ONLY_JURISDICTION_MAGS = "json/public.hearing-resulted_with_mags_judicialresults_in_application_only.json";

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @BeforeClass
    public static void setUpClass() {
        setupUserAsPrisonAdminGroup(getUserId());
        stubEventGridEndpoint();
        stubCountryNationalities();
        stubGetOrgainsationUnit();
        stubJudicialResults();
        stubBailStatuses();
        stubModeOfTrialReasons();
        stubPoliceFlag(OU_CODE, PROSECUTION_AUTHORITY);
        stubNotificationNotifyEndPoint();

    }

    @After
    public void teardown() throws JMSException {
        closeMessageConsumers();
    }

    @Before
    public void setUp() {
        stubSpiOutFlag(true, true);

        createMessageConsumers();
    }

    @Test
    public void testCCForSpiOut() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();

        Optional<String> response = verifyInPublicTopic();
        assertThat(resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOrderIndex(), is(new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).get("offenceSequenceNumber")));
    }

    @Test
    public void testCCForSpiOutWithIndicatedPlea() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsV2TemplateForIndicatedPlea(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();

        Optional<String> response = verifyInPublicTopic();
        assertThat(resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOrderIndex(), is(new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).get("offenceSequenceNumber")));
        assertThat(resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getIndicatedPlea(), is(notNullValue()));
        assertThat(resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getIndicatedPlea().getIndicatedPleaValue(), is(IndicatedPleaValue.INDICATED_GUILTY));
    }

    private void setOuCodeAndProsecutorAuthority(final PublicHearingResulted resultsMessage) {
        if (null != resultsMessage.getHearing().getProsecutionCases() && !resultsMessage.getHearing().getProsecutionCases().isEmpty()) {
            int size = resultsMessage.getHearing().getProsecutionCases().size();
            for (int i = 0; i < size; i++) {
                final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(i))
                        .withOriginatingOrganisation(OU_CODE)
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(i).getProsecutionCaseIdentifier())
                                .withProsecutionAuthorityCode(PROSECUTION_AUTHORITY)
                                .withProsecutionAuthorityOUCode(OU_CODE).build())
                        .build();

                resultsMessage.getHearing().getProsecutionCases().set(i, prosecutionCase);

            }
        }
    }

    @Test
    public void testCCForSpiOutForHearingDay() throws JMSException {
        final LocalDate hearingDay = LocalDate.now();
        final PublicHearingResulted resultsMessage = basicShareResultsV2TemplateWithHearingDay(MAGISTRATES, hearingDay);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGeneratedForDay(hearingDay);

        Optional<String> response = verifyInPublicTopic();
        assertThat(resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOrderIndex(), is(new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).get("offenceSequenceNumber")));
    }

    @Test
    public void testCCForSpiOutWithOffenceDateCode() throws JMSException {
        final Integer offenceDateCode = 4;
        final PublicHearingResulted resultsMessage = basicShareResultsV2WithMagistratesAlongWithOffenceDateCodeTemplate(offenceDateCode);
        setOuCodeAndProsecutorAuthority(resultsMessage);
        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();

        Optional<String> response = verifyInPublicTopic();
        assertThat(resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOrderIndex(), is(new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).get("offenceSequenceNumber")));
        assertThat(resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceDateCode(), is(new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).get("offenceDateCode")));
    }

    @Test
    public void testCCForSpiOutDefaultOffenceDateCode() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);
        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();

        Optional<String> response = verifyInPublicTopic();
        assertThat(resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOrderIndex(), is(new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).get("offenceSequenceNumber")));
        assertThat(1, is(new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).get("offenceDateCode")));
    }


    @Test
    public void whenVerdictNotPresentAndConvictedDatePresentThenPoliceResultGeneratedHasFindingWithDefaultValue() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);
        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEventsWithPoliceResultGenerated();

        verifyInPublicPoliceResultGeneratedMessage(FINDING_VALUE_DEFAULT);
    }

    @Test
    public void whenVerdictPresentThenPoliceResultGeneratedHasFindingWithVerdictCode() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsV2WithVerdictTemplate(MAGISTRATES, true, false);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEventsWithPoliceResultGenerated();

        verifyInPublicPoliceResultGeneratedMessage(FINDING_VALUE_NOT_GUILTY);
    }


    @Test
    public void testCCForEmailNotificationFailWhenEmailIsEmpty() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(JurisdictionType.CROWN);
        setOuCodeAndProsecutorAuthority(resultsMessage);
        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceNotificationFailed();
    }

    @Test
    public void testCCForEmailNotificationSuccess() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(JurisdictionType.CROWN);
        stubSpiOutFlag(true, true, EMAIL);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceNotificationRequested(EMAIL);
    }

    @Test
    public void testCCForSpiOutFalse() throws JMSException {
        stubSpiOutFlag(false, true);
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated(false);
        verifyNotInPublicTopic();
    }

    @Test
    public void testCCForRejectedEvent() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsV2TemplateWithoutResult(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);
        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsForRejected();
    }

    @Test
    public void shouldGeneratePoliceResultForFirstShareAndSendEmailWhenAmendedForMagistrates() throws JMSException {
        final UUID hearingId = randomUUID();
        System.out.println(hearingId);
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_MAG, hearingId);
        stubSpiOutFlag(true, true, MAG_EMAIL);
        hearingResultsHaveBeenSharedV2(payload);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        final LocalDate startDate = of(2019, 5, 25);

        getSummariesByDate(startDate);

        verifyPrivateEventsWithPoliceResultGenerated();
        verifyInPublicTopic();

        final JsonObject amendedPayload = getPayload(TEMPLATE_PAYLOAD_RESHARE_MAG, hearingId);
        hearingResultsHaveBeenSharedV2(amendedPayload);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        getSummariesByDate(startDate);
        verifyPrivateEventsForAmendment(MAG_EMAIL, JudicialResultDetails.judicialResultDetails()
                .withId(fromString("ea5eb48e-387c-4921-8f9d-d779d731fc38"))
                .withJudicialResultTypeId(fromString("1726363f-e216-48a3-b0aa-567fd269f0cb"))
                .withResultTitle("Imprisonment")
                .withAmendmentType(AmendmentType.ADDED)
                .build());
    }

    @Test
    public void shouldSendEmailForFirstShareAndAmendmentsForCrownCourt() throws JMSException {
        final UUID hearingId = randomUUID();
        System.out.println(hearingId);
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD_CROWN, hearingId);
        stubSpiOutFlag(true, true, EMAIL);
        hearingResultsHaveBeenSharedV2(payload);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        final LocalDate startDate = of(2019, 5, 25);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceNotificationRequested(EMAIL);

        final JsonObject amendedPayload = getPayload(TEMPLATE_PAYLOAD_RESHARE_CROWN, hearingId);
        hearingResultsHaveBeenSharedV2(amendedPayload);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        getSummariesByDate(startDate);
        verifyPrivateEventsForAmendment(EMAIL, JudicialResultDetails.judicialResultDetails()
                .withId(fromString("ea5eb48e-387c-4921-8f9d-d779d731fc38"))
                .withJudicialResultTypeId(fromString("1726363f-e216-48a3-b0aa-567fd269f0cb"))
                .withResultTitle("Imprisonment")
                .withAmendmentType(AmendmentType.ADDED)
                .build());
    }

    @Test
    public void testGeneratePoliceResultsForDefendantCC() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEventsWithPoliceResultGenerated();
        verifyInPublicTopic();

        final JsonObject payload = createObjectBuilder()
                .add(SESSION_ID, resultsMessage.getHearing().getId().toString())
                .add(CASE_ID, CASE_ID_VALUE)
                .add(DEFENDANT_ID, DEFENDANT_ID_VALUE)
                .build();

        sendGeneratePoliceResultsForADefendantCommand(payload);

        verifyPrivateEventsForPoliceGenerateResultsForDefendant();
        verifyInPublicTopic();
    }

    @Test
    public void testGeneratePoliceResultsForDefendantCCWhenSpiOutFalse() throws JMSException {
        stubSpiOutFlag(false, true);
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEventsWithPoliceResultGenerated(false);
        verifyNotInPublicTopic();

        final JsonObject payload = createObjectBuilder()
                .add(SESSION_ID, resultsMessage.getHearing().getId().toString())
                .add(CASE_ID, CASE_ID_VALUE)
                .add(DEFENDANT_ID, DEFENDANT_ID_VALUE)
                .build();

        sendGeneratePoliceResultsForADefendantCommand(payload);

        verifyPrivateEventsForPoliceGenerateResultsForDefendant(true);
        verifyInPublicTopic();
    }

    @Test
    public void journeyHearingToResults() {
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);
        final Hearing hearingIn = resultsMessage.getHearing();
        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();

        //share results
        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());


        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        //search summaries
        HearingResultSummariesView summaries;

        final Function<HearingResultSummariesView, List<HearingResultSummaryView>> resultsFilter =
                summs -> summs.getResults().stream().filter(sum -> sum.getHearingId().equals(hearingIn.getId()))
                        .collect(Collectors.toList());


        final long defendantCount = hearingIn.getProsecutionCases().stream().mapToLong(c -> c.getDefendants().size()).sum();

        final BeanMatcher<HearingResultSummariesView> summaryCheck = isBean(HearingResultSummariesView.class)
                .withValue(summs -> resultsFilter.apply(summs).size(),
                        (int) hearingIn.getProsecutionCases().stream().mapToLong(pc -> pc.getDefendants().size()).sum())
                .with(summs -> resultsFilter.apply(summs),
                        hasItem(isBean(HearingResultSummaryView.class)
                                .withValue(HearingResultSummaryView::getHearingId, hearingIn.getId())
                                .withValue(HearingResultSummaryView::getHearingType, hearingIn.getType().getDescription())
                        )
                );

        final LocalDate searchStartDate = startDate;
        Queries.pollForMatch(15, 500, () -> getSummariesByDate(searchStartDate), summaryCheck);

        final LocalDate earlierDate = hearingIn.getHearingDays().stream().map(hd -> hd.getSittingDay().toLocalDate()).min(Comparator.naturalOrder()).orElse(null).minusDays(1);
        final LocalDate laterDate = hearingIn.getHearingDays().stream().map(hd -> hd.getSittingDay().toLocalDate()).max(Comparator.naturalOrder()).orElse(null).plusDays(1);

        //check that date filters work
        summaries = getSummariesByDate(earlierDate);
        assertThat(resultsFilter.apply(summaries).size(), is((int) defendantCount));
        summaries = getSummariesByDate(laterDate);
        assertThat(resultsFilter.apply(summaries).size(), is(0));

        //matcher to check details results
        final Matcher<HearingResultsAdded> matcher = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, hearingIn.getId())
                        .withValue(Hearing::getJurisdictionType, hearingIn.getJurisdictionType())
                        .withValue(Hearing::getCourtApplications, resultsMessage.getHearing().getCourtApplications())
                        .withValue(Hearing::getType, hearingIn.getType())
                );
        // check the details from query
        getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, matcher);

        Matcher<HearingResultsAdded> matcherStatus = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getSharedTime, is(resultsMessage.getSharedTime()))
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, resultsMessage.getHearing().getId())
                        .withValue(Hearing::getCourtApplications, resultsMessage.getHearing().getCourtApplications())
                        .withValue(hearing -> hearing.getProsecutionCases().get(0).getDefendants().get(0).getPersonDefendant().getPersonDetails().getTitle(), "Baroness")
                );

        // get the details and check
        getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, matcherStatus);
    }

    @Test
    public void outOfOrderJourney() throws Exception {
        final PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);
        final Hearing hearingIn = resultsMessage.getHearing();
        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();


        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        hearingResultsHaveBeenSharedV2(resultsMessage);
        verifyPrivateEventsWithPoliceResultGenerated();

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);
        //search summaries
        HearingResultSummariesView summaries;

        final Function<HearingResultSummariesView, List<HearingResultSummaryView>> resultsFilter =
                summs -> summs.getResults().stream().filter(sum -> sum.getHearingId().equals(hearingIn.getId()))
                        .collect(Collectors.toList());

        final BeanMatcher<HearingResultSummariesView> summaryCheck = isBean(HearingResultSummariesView.class)
                .withValue(summs -> resultsFilter.apply(summs).size(),
                        (int) hearingIn.getProsecutionCases().stream().mapToLong(pc -> pc.getDefendants().size()).sum())
                .with(resultsFilter::apply,
                        hasItem(isBean(HearingResultSummaryView.class)
                                .withValue(HearingResultSummaryView::getHearingId, hearingIn.getId())
                                .withValue(HearingResultSummaryView::getHearingType, hearingIn.getType().getDescription())
                        )
                );
        final LocalDate searchStartDate = startDate;
        Queries.pollForMatch(15, 500, () -> getSummariesByDate(searchStartDate), summaryCheck);

        summaries = getSummariesByDate(startDate);
        assertThat(summaries, summaryCheck);

        //matcher to check details results
        final Matcher<HearingResultsAdded> matcher = isBean(HearingResultsAdded.class)
                .with(HearingResultsAdded::getHearing, isBean(Hearing.class)
                        .withValue(Hearing::getId, hearingIn.getId())
                        .withValue(Hearing::getJurisdictionType, hearingIn.getJurisdictionType())
                        .withValue(Hearing::getType, hearingIn.getType())
                );
        // check the details from query
        getHearingDetails(resultsMessage.getHearing().getId(), defendantId0, matcher);
    }

    @Test
    public void getHearingDetails_shouldReturnBadRequestForResultsSummaryWithoutFromDate() {
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        thenReturnsBadRequestForResultsSummaryWithoutFromDate();
    }

    @Test
    public void testJourneyHearingToDisplayAllDetailsInResults() {
        PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);
        final Hearing hearingIn = resultsMessage.getHearing();


        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        ApiCourtCentre expectedCourtCentre =
                ApiCourtCentre.apiCourtCentre()
                        .withAddress(ApiAddress.apiAddress()
                                .withAddress1(hearingIn.getCourtCentre().getAddress().getAddress1())
                                .withAddress2(hearingIn.getCourtCentre().getAddress().getAddress2())
                                .withAddress3(hearingIn.getCourtCentre().getAddress().getAddress3())
                                .withAddress4(hearingIn.getCourtCentre().getAddress().getAddress4())
                                .withAddress5(hearingIn.getCourtCentre().getAddress().getAddress5())
                                .withPostcode(hearingIn.getCourtCentre().getAddress().getPostcode()).build())
                        .withId(hearingIn.getCourtCentre().getId())
                        .withName(hearingIn.getCourtCentre().getName())
                        .withRoomId(hearingIn.getCourtCentre().getRoomId())
                        .withRoomName(hearingIn.getCourtCentre().getRoomName())
                        .withWelshName(hearingIn.getCourtCentre().getWelshName())
                        .withWelshRoomName(hearingIn.getCourtCentre().getWelshRoomName()).build();

        final Matcher[] matcher1 = {
                withJsonPath("$.hearing.id", equalTo(hearingIn.getId().toString())),
                withJsonPath("$.hearing", Matchers.notNullValue()),
                withJsonPath("$.hearing.courtCentre.name", equalTo(expectedCourtCentre.getName())),
                withJsonPath("$.hearing.courtCentre.id", equalTo(expectedCourtCentre.getId().toString())),
                withJsonPath("$.hearing.courtCentre.roomId", equalTo(expectedCourtCentre.getRoomId().toString())),
                withJsonPath("$.hearing.courtCentre.roomName", equalTo(expectedCourtCentre.getRoomName())),
                withJsonPath("$.hearing.courtCentre.welshName", equalTo(expectedCourtCentre.getWelshName())),
                withJsonPath("$.hearing.courtCentre.welshRoomName", equalTo(expectedCourtCentre.getWelshRoomName())),
                withJsonPath("$.hearing.courtCentre.address.address1", equalTo(expectedCourtCentre.getAddress().getAddress1())),
                withJsonPath("$.hearing.courtCentre.address.address2", equalTo(expectedCourtCentre.getAddress().getAddress2())),
                withJsonPath("$.hearing.courtCentre.address.postcode", equalTo(expectedCourtCentre.getAddress().getPostcode())),
                withJsonPath("$.sharedTime", notNullValue())
        };
        ResultsStepDefinitions.getHearingDetailsForHearingId(resultsMessage.getHearing().getId(), matcher1);

    }

    @Test
    public void testJourneyHearingToDisplayAllDetailsInResultsWithHearingDate() {
        PublicHearingResulted resultsMessage = basicShareResultsV2TemplateWithHearingDay(MAGISTRATES, LocalDate.of(2018, 5, 2));

        final Hearing hearingIn = resultsMessage.getHearing();

        hearingResultsHaveBeenSharedV2(resultsMessage);

        resultsMessage = basicShareResultsV2TemplateWithHearingDay(MAGISTRATES, LocalDate.of(2018, 5, 3));
        resultsMessage.setHearing(Hearing.hearing().withValuesFrom(resultsMessage.getHearing())
                .withId(hearingIn.getId()).build());

        hearingResultsHaveBeenSharedV2(resultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        ApiCourtCentre expectedCourtCentre =
                ApiCourtCentre.apiCourtCentre()
                        .withAddress(ApiAddress.apiAddress()
                                .withAddress1(hearingIn.getCourtCentre().getAddress().getAddress1())
                                .withAddress2(hearingIn.getCourtCentre().getAddress().getAddress2())
                                .withAddress3(hearingIn.getCourtCentre().getAddress().getAddress3())
                                .withAddress4(hearingIn.getCourtCentre().getAddress().getAddress4())
                                .withAddress5(hearingIn.getCourtCentre().getAddress().getAddress5())
                                .withPostcode(hearingIn.getCourtCentre().getAddress().getPostcode()).build())
                        .withId(hearingIn.getCourtCentre().getId())
                        .withName(hearingIn.getCourtCentre().getName())
                        .withRoomId(hearingIn.getCourtCentre().getRoomId())
                        .withRoomName(hearingIn.getCourtCentre().getRoomName())
                        .withWelshName(hearingIn.getCourtCentre().getWelshName())
                        .withWelshRoomName(hearingIn.getCourtCentre().getWelshRoomName()).build();

        final Matcher[] matcher1 = {
                withJsonPath("$.hearing.id", equalTo(hearingIn.getId().toString())),
                withJsonPath("$.hearing", Matchers.notNullValue()),
                withJsonPath("$.hearing.courtCentre.name", equalTo(expectedCourtCentre.getName())),
                withJsonPath("$.hearing.courtCentre.id", equalTo(expectedCourtCentre.getId().toString())),
                withJsonPath("$.hearing.courtCentre.roomId", equalTo(expectedCourtCentre.getRoomId().toString())),
                withJsonPath("$.hearing.courtCentre.roomName", equalTo(expectedCourtCentre.getRoomName())),
                withJsonPath("$.hearing.courtCentre.welshName", equalTo(expectedCourtCentre.getWelshName())),
                withJsonPath("$.hearing.courtCentre.welshRoomName", equalTo(expectedCourtCentre.getWelshRoomName())),
                withJsonPath("$.hearing.courtCentre.address.address1", equalTo(expectedCourtCentre.getAddress().getAddress1())),
                withJsonPath("$.hearing.courtCentre.address.address2", equalTo(expectedCourtCentre.getAddress().getAddress2())),
                withJsonPath("$.hearing.courtCentre.address.postcode", equalTo(expectedCourtCentre.getAddress().getPostcode())),
                withJsonPath("$.sharedTime", notNullValue()),
                withJsonPath("$.hearing.hearingDays[3].sittingDay", is("2018-05-02T00:00:00.000Z"))
        };
        ResultsStepDefinitions.getHearingDetailsForHearingIdAndHearingDate(resultsMessage.getHearing().getId(), LocalDate.of(2018, 5, 2), matcher1);
    }

    @Test
    public void shouldDisplayAllInternalDetailsInHearingResults() {
        PublicHearingResulted resultsMessage = basicShareResultsV2Template(MAGISTRATES);
        setOuCodeAndProsecutorAuthority(resultsMessage);
        final Hearing hearingIn = resultsMessage.getHearing();

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        CourtCentre expectedCourtCentre =
                CourtCentre.courtCentre()
                        .withAddress(Address.address()
                                .withAddress1(hearingIn.getCourtCentre().getAddress().getAddress1())
                                .withAddress2(hearingIn.getCourtCentre().getAddress().getAddress2())
                                .withAddress3(hearingIn.getCourtCentre().getAddress().getAddress3())
                                .withAddress4(hearingIn.getCourtCentre().getAddress().getAddress4())
                                .withAddress5(hearingIn.getCourtCentre().getAddress().getAddress5())
                                .withPostcode(hearingIn.getCourtCentre().getAddress().getPostcode()).build())
                        .withId(hearingIn.getCourtCentre().getId())
                        .withName(hearingIn.getCourtCentre().getName())
                        .withRoomId(hearingIn.getCourtCentre().getRoomId())
                        .withRoomName(hearingIn.getCourtCentre().getRoomName())
                        .withWelshName(hearingIn.getCourtCentre().getWelshName())
                        .withWelshRoomName(hearingIn.getCourtCentre().getWelshRoomName()).build();

        final Matcher[] matcher = {
                withJsonPath("$.hearing.id", equalTo(hearingIn.getId().toString())),
                withJsonPath("$.hearing", Matchers.notNullValue()),
                withJsonPath("$.hearing.courtCentre.name", equalTo(expectedCourtCentre.getName())),
                withJsonPath("$.hearing.courtCentre.id", equalTo(expectedCourtCentre.getId().toString())),
                withJsonPath("$.hearing.courtCentre.roomId", equalTo(expectedCourtCentre.getRoomId().toString())),
                withJsonPath("$.hearing.courtCentre.roomName", equalTo(expectedCourtCentre.getRoomName())),
                withJsonPath("$.hearing.courtCentre.welshName", equalTo(expectedCourtCentre.getWelshName())),
                withJsonPath("$.hearing.courtCentre.welshRoomName", equalTo(expectedCourtCentre.getWelshRoomName())),
                withJsonPath("$.hearing.courtCentre.address.address1", equalTo(expectedCourtCentre.getAddress().getAddress1())),
                withJsonPath("$.hearing.courtCentre.address.address2", equalTo(expectedCourtCentre.getAddress().getAddress2())),
                withJsonPath("$.hearing.courtCentre.address.postcode", equalTo(expectedCourtCentre.getAddress().getPostcode())),
                withJsonPath("$.sharedTime", notNullValue())
        };
        getInternalHearingDetailsForHearingId(resultsMessage.getHearing().getId(), matcher);
    }

    @Test
    public void shouldGenerateAllTheRequiredEventsWhenResultsAreCreatedByIssuingTheCommandByAnAdmin() throws JMSException {
        whenResultsAreCreatedBySystemAdmin();

        verifyPrivateEventsWithPoliceResultGenerated();
    }

    private static JsonObject getPayload(final String path, final UUID hearingId) {
        String request = null;
        try {
            final InputStream inputStream = HearingResultedIT.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, IsNull.notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset()).replace("HEARING_ID", hearingId.toString());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = createReader(new StringReader(request));
        return reader.readObject();
    }

    @Test
    public void shouldBeSentToSpiOutForApplicationWithNoJudicialResultsV2() throws JMSException {

        final PublicHearingResulted resultsMessage = PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithApplication(randomUUID(), MAGISTRATES))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        resultsMessage.setIsReshare(Optional.of(false));
        resultsMessage.setHearingDay(Optional.of(LocalDate.of(2018, 5, 2)));

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(0))
                .withOriginatingOrganisation(null)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier())
                        .withProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE).build())
                .withOriginatingOrganisation(null)
                .build();
        final CourtApplicationCase courtApplicationCase = courtApplicationCase().
                withValuesFrom(resultsMessage.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier())
                        .withProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE).build()).build();

        resultsMessage.getHearing().getProsecutionCases().set(0, prosecutionCase);
        resultsMessage.getHearing().getCourtApplications().get(0).getCourtApplicationCases().set(0, courtApplicationCase);

        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);

        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();

        Optional<String> response = verifyInPublicTopic();

        final JSONObject firstOffenceJson = new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(1);
        assertThat(firstOffenceJson.get("offenceSequenceNumber"), is(65));
        assertThat(new JSONObject(response.get()).getJSONObject("defendant").get("prosecutorReference"), is("0800PP0100000000001H"));
        assertThat(firstOffenceJson.get("finding"), is("N"));
        assertThat(firstOffenceJson.getJSONObject("plea").get("pleaValue"), is("NOT_GUILTY"));
    }

    @Test
    public void shouldBeSentToSpiOutForApplicationWithJudicialResultsV2() throws JMSException {

        final PublicHearingResulted resultsMessage = PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithCustomApplication(randomUUID(), MAGISTRATES,
                        asList(CourtApplication.courtApplication()
                                .withId(fromString("f8254db1-1683-483e-afb3-b87fde5a0a26"))
                                .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                                .withApplicationReference("OFFENCE_CODE_REFERENCE")
                                .withType(TestTemplates.courtApplicationTypeTemplates())
                                .withApplicant(TestTemplates.courtApplicationPartyTemplates())
                                .withApplicationStatus(ApplicationStatus.DRAFT)
                                .withSubject(TestTemplates.courtApplicationPartyTemplates())
                                .withCourtApplicationCases(asList(TestTemplates.createCourtApplicationCaseWithOffences()))
                                .withApplicationParticulars("bail application")
                                .withJudicialResults(buildJudicialResultList())
                                .withAllegationOrComplaintStartDate(LocalDate.now())
                                .withPlea(uk.gov.justice.core.courts.Plea.plea().withOffenceId(UUID.randomUUID()).withPleaDate(LocalDate.now()).withPleaValue("NOT_GUILTY").build())
                                .withVerdict(Verdict.verdict()
                                        .withVerdictType(VerdictType.verdictType()
                                                .withId(fromString("3f0d69d0-2fda-3472-8d4c-a6248f661825"))
                                                .withCategory(STRING.next())
                                                .withCategoryType(STRING.next())
                                                .withCjsVerdictCode("N")
                                                .build())
                                        .withOriginatingHearingId(randomUUID())
                                        .withOffenceId(UUID.randomUUID())
                                        .withVerdictDate(now())
                                        .build())
                                .build())))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(0))
                .withOriginatingOrganisation(null)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier())
                        .withProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE).build())
                .withOriginatingOrganisation(null)
                .build();
        final CourtApplicationCase courtApplicationCase = courtApplicationCase().
                withValuesFrom(resultsMessage.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withValuesFrom(resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier())
                        .withProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE).build()).build();

        resultsMessage.getHearing().getProsecutionCases().set(0, prosecutionCase);
        resultsMessage.getHearing().getCourtApplications().get(0).getCourtApplicationCases().set(0, courtApplicationCase);


        resultsMessage.setIsReshare(Optional.of(false));
        resultsMessage.setHearingDay(Optional.of(LocalDate.of(2018, 5, 2)));
        setOuCodeAndProsecutorAuthority(resultsMessage);
        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();

        Optional<String> response = verifyInPublicTopic();
        final JSONObject firstOffenceJson = new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(1);
        assertThat(firstOffenceJson.get("offenceSequenceNumber"), is(0));
        assertThat(new JSONObject(response.get()).getJSONObject("defendant").get("prosecutorReference"), is("0800PP0100000000001H"));
        assertThat(firstOffenceJson.get("finding"), is("N"));
        assertThat(firstOffenceJson.getJSONObject("plea").get("pleaValue"), is("NOT_GUILTY"));
    }

    @Test
    public void shouldSentSpiOutForApplicationWithCourtOrderOnlyV2() throws JMSException {
        final UUID caseId = randomUUID();
        final String caseUrn = "32DN1212262";
        final JsonObject payload = getPayload("json/public.events.hearing.hearing-resulted-court-order.json", randomUUID());
        final PublicHearingResulted publicHearingResulted = jsonToObjectConverter.convert(payload, PublicHearingResulted.class);

        stubGetProgressionProsecutionCases(caseId);
        stubGetProgressionCaseExistsByUrn(caseUrn, caseId);
        hearingResultsHaveBeenSharedV2(publicHearingResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = publicHearingResulted.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();

        Optional<String> response = verifyInPublicTopic();
        JSONObject jsonObject = new JSONObject(response.get());
        if(!jsonObject.getString("caseId").equalsIgnoreCase("4d7fd02d-2297-4249-a7c6-d1d7bd567d58")) {
            response = verifyInPublicTopic();
            jsonObject = new JSONObject(response.get());
        }
        assertThat(jsonObject.getString("caseId"), is("4d7fd02d-2297-4249-a7c6-d1d7bd567d58"));
        assertThat(jsonObject.getJSONObject("defendant").getJSONArray("offences").length(), is(2));
        assertThat(jsonObject.getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).getString("id"), is("b729153d-50e3-4ce4-811c-f16799043d4f"));
    }

    @Test
    public void testCCForEmailNotificationSuccess_WhenApplicationIsResultedOnly() throws JMSException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final String caseUrn = "31DI1504926";
        JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
        final PublicHearingResulted resultsMessage = jsonObjectToObjectConverter.convert(getPayload(HEARING_RESULT_APPLICATION_ONLY_JURISDICTION_CROWN,hearingId), PublicHearingResulted.class);

        stubGetProgressionProsecutionCases(caseId);
        stubGetProgressionCaseExistsByUrn(caseUrn, caseId);
        stubSpiOutFlag(true, true, EMAIL);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceNotificationRequested(EMAIL);
    }

    @Test
    public void testCCForEmailNotificationSuccess_WhenApplicationIsResultedAndResharedOnly() throws JMSException {
        final UUID hearingId = randomUUID();
        final UUID caseId = fromString("cfb28f37-5159-4297-ab6a-653a63627d0b");
        final String caseUrn = "31DI1504926";
        JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
        final PublicHearingResulted resultsMessage = jsonObjectToObjectConverter.convert(getPayload(HEARING_RESULT_APPLICATION_ONLY_JURISDICTION_CROWN, hearingId), PublicHearingResulted.class);

        stubGetProgressionProsecutionCases(caseId);
        stubGetProgressionCaseExistsByUrn(caseUrn, caseId);
        stubSpiOutFlag(true, true, EMAIL);
        setOuCodeAndProsecutorAuthority(resultsMessage);

        hearingResultsHaveBeenSharedV2(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceNotificationRequested(EMAIL);
        verifyEmailNotificationIsRaised(Arrays.asList("TestArmand TestKrajcik"));


        final PublicHearingResulted resultsMessage1 = jsonObjectToObjectConverter.convert(getPayload(HEARING_RESULT_RESHARE_APPLICATION_ONLY_JURISDICTION_CROWN, hearingId), PublicHearingResulted.class);
        hearingResultsHaveBeenSharedV2(resultsMessage1);
        verifyEmailNotificationIsRaised(Arrays.asList("TestArmand1 TestKrajcik1"));
    }

    @Test
    public void shouldSentSpiOutForResultApplicationOnlyForMags() throws JMSException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final String caseUrn = "31DI1504926";
        JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(getPayload(HEARING_RESULT_APPLICATION_ONLY_JURISDICTION_MAGS,hearingId), PublicHearingResulted.class);

        stubGetProgressionProsecutionCases(caseId);
        stubGetProgressionCaseExistsByUrn(caseUrn, caseId);
        hearingResultsHaveBeenSharedV2(publicHearingResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = publicHearingResulted.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();

        Optional<String> response = verifyInPublicTopic();
        final JSONObject jsonObject = new JSONObject(response.get());
        assertThat(jsonObject.getString("caseId"), is(caseId.toString()));
    }
}