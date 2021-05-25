package uk.gov.moj.cpp.results.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.LocalDate.now;
import static java.time.LocalDate.of;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createReader;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.closeMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getHearingDetails;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getInternalHearingDetailsForHearingId;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.getSummariesByDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenShared;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.publicSjpResultedShared;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.thenReturnsBadRequestForResultsSummaryWithoutFromDate;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyInPublicPoliceResultGeneratedMessage;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyInPublicTopic;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyNotInPublicTopic;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsForAmendment;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsForCaseRejectedSjp;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsForPoliceGenerateResultsForDefendant;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsForRejected;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsWithPoliceNotificationRequested;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.verifyPrivateEventsWithPoliceResultGenerated;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenPrisonAdminTriesToViewResultsForThePerson;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.whenResultsAreCreatedBySystemAdmin;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.utils.EventGridStub.stubEventGridEndpoint;
import static uk.gov.moj.cpp.results.it.utils.HttpClientUtil.sendGeneratePoliceResultsForADefendantCommand;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.PROSECUTOR_WITH_SPI_OUT_FALSE;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrgainsationUnit;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubJudicialResults;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubModeOfTrialReasons;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubSpiOutFlag;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicSJPCaseResulted;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareHearingTemplateWithApplication;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareHearingTemplateWithCustomApplication;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplate;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplateWithoutResult;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsWithMagistratesAlongWithOffenceDateCodeTemplate;
import static uk.gov.moj.cpp.results.test.TestTemplates.buildJudicialResultList;
import static uk.gov.moj.cpp.results.test.matchers.BeanMatcher.isBean;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.core.courts.external.ApiAddress;
import uk.gov.justice.core.courts.external.ApiCourtCentre;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.sjp.results.PublicSjpResulted;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions;
import uk.gov.moj.cpp.results.it.utils.Queries;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummariesView;
import uk.gov.moj.cpp.results.query.view.response.HearingResultSummaryView;
import uk.gov.moj.cpp.results.test.TestTemplates;
import uk.gov.moj.cpp.results.test.matchers.BeanMatcher;

import java.io.InputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jms.JMSException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

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

@SuppressWarnings({"unchecked", "serial", "squid:S2925", "squid:S1607"})
public class ResultsIT {

    private static final String TEMPLATE_PAYLOAD = "json/public.hearing-resulted.json";
    private static final String TEMPLATE_PAYLOAD_1 = "json/public.hearing-result-amended.json";
    private static final String SESSION_ID = "sessionId";
    private static final String CASE_ID = "caseId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_ID_VALUE = "cccc1111-1e20-4c21-916a-81a6c90239e5";
    private static final String DEFENDANT_ID_VALUE = "dddd1111-1e20-4c21-916a-81a6c90239e5";
    private static final String EMAIL = "email@email.com";
    private static final String FINDING_VALUE_DEFAULT = "G";
    private static final String FINDING_VALUE_NOT_GUILTY = "N";

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
    }

    @After
    public void teardown() throws JMSException {
        closeMessageConsumers();
    }

    @Before
    public void setUp() {
        stubSpiOutFlag(true, true);
        createMessageConsumers();
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldProcessPublicSjpResulted() throws JMSException {
        final PublicSjpResulted sjpResulted = basicSJPCaseResulted();
        publicSjpResultedShared(sjpResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEventsWithPoliceResultGenerated();
        verifyInPublicTopic();
    }

    @Test
    public void shouldProcessPublicSjpResultedSpiOutFalse() throws JMSException {
        final PublicSjpResulted sjpResulted = basicSJPCaseResulted();
        sjpResulted.getCases().get(0).setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        publicSjpResultedShared(sjpResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEventsWithPoliceResultGenerated(false);
        verifyNotInPublicTopic();
    }

    @Test
    public void testCCForSpiOut() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate(MAGISTRATES);

        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();

        Optional<String> response = verifyInPublicTopic();
        assertThat(resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOrderIndex(), is(new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).get("offenceSequenceNumber")));
    }

    @Test
    public void testCCForSpiOutWithOffenceDateCode() throws JMSException {
        final Integer offenceDateCode = 4;
        final PublicHearingResulted resultsMessage = basicShareResultsWithMagistratesAlongWithOffenceDateCodeTemplate(offenceDateCode);

        hearingResultsHaveBeenShared(resultsMessage);
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
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate(MAGISTRATES);

        hearingResultsHaveBeenShared(resultsMessage);
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
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate(MAGISTRATES);

        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEventsWithPoliceResultGenerated();

        verifyInPublicPoliceResultGeneratedMessage(FINDING_VALUE_DEFAULT);
    }

    @Test
    public void whenVerdictPresentThenPoliceResultGeneratedHasFindingWithVerdictCode() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate(MAGISTRATES, true, false);

        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEventsWithPoliceResultGenerated();

        verifyInPublicPoliceResultGeneratedMessage(FINDING_VALUE_NOT_GUILTY);
    }

    @Test
    public void testCCForEmailNotificationFailWhenEmailIsEmpty() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate(JurisdictionType.CROWN);
        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceNotificationRequested(false);
    }

    @Test
    public void testCCForEmailNotificationSuccess() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate(JurisdictionType.CROWN);
        stubSpiOutFlag(true, true, EMAIL);
        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceNotificationRequested(true);
    }

    @Test
    public void testCCForSpiOutFalse() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate(JurisdictionType.MAGISTRATES);
        resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier().setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        resultsMessage.getHearing().getProsecutionCases().get(1).getProsecutionCaseIdentifier().setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated(false);
        verifyNotInPublicTopic();
    }

    @Test
    public void testCCForRejectedEvent() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplateWithoutResult(MAGISTRATES);

        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = resultsMessage.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsForRejected();
    }

    @Test
    public void testCCForUpdatedEvent() throws JMSException {
        final UUID hearingId = randomUUID();
        final JsonObject payload = getPayload(TEMPLATE_PAYLOAD, hearingId);

        hearingResultsHaveBeenShared(payload);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        final LocalDate startDate = of(2019, 5, 25);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();
        verifyInPublicTopic();

        final JsonObject amendedPayload = getPayload(TEMPLATE_PAYLOAD_1, hearingId);
        hearingResultsHaveBeenShared(amendedPayload);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        getSummariesByDate(startDate);
        verifyPrivateEventsForAmendment();
        verifyInPublicTopic();

    }

    @Test
    public void testSjpForRejectedCaseEvent() throws JMSException {

        final PublicSjpResulted sjpResulted = basicSJPCaseResulted();
        publicSjpResultedShared(sjpResulted);

        verifyPrivateEventsWithPoliceResultGenerated();
        verifyInPublicTopic();

        publicSjpResultedShared(sjpResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        verifyPrivateEventsForCaseRejectedSjp();
    }

    @Test
    public void testSjpForNullDateOfBirth() throws JMSException {

        final PublicSjpResulted sjpResulted = basicSJPCaseResulted();
        sjpResulted.getCases().forEach(caseDetails -> {
            caseDetails.getDefendants().forEach(caseDefendant -> {
                caseDefendant.getParentGuardianDetails().setBirthDate(null);
                caseDefendant.getIndividualDefendant().getBasePersonDetails().setBirthDate(null);
            });
        });

        publicSjpResultedShared(sjpResulted);

        verifyPrivateEventsWithPoliceResultGenerated();
        verifyInPublicTopic();
    }

    @Test
    public void testGeneratePoliceResultsForDefendantCC() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate(MAGISTRATES);

        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEventsWithPoliceResultGenerated();
        verifyInPublicTopic();

        final JsonObject payload = Json.createObjectBuilder()
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
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate(JurisdictionType.MAGISTRATES);
        resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier().setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        resultsMessage.getHearing().getProsecutionCases().get(1).getProsecutionCaseIdentifier().setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        hearingResultsHaveBeenShared(resultsMessage);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEventsWithPoliceResultGenerated(false);
        verifyNotInPublicTopic();

        final JsonObject payload = Json.createObjectBuilder()
                .add(SESSION_ID, resultsMessage.getHearing().getId().toString())
                .add(CASE_ID, CASE_ID_VALUE)
                .add(DEFENDANT_ID, DEFENDANT_ID_VALUE)
                .build();

        sendGeneratePoliceResultsForADefendantCommand(payload);

        verifyPrivateEventsForPoliceGenerateResultsForDefendant(true);
        verifyInPublicTopic();
    }

    @Test
    public void testGeneratePoliceResultsForDefendantSJP() throws JMSException {
        final PublicSjpResulted sjpResulted = basicSJPCaseResulted();

        publicSjpResultedShared(sjpResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEventsWithPoliceResultGenerated();
        verifyInPublicTopic();

        final JsonObject payload = Json.createObjectBuilder()
                .add(SESSION_ID, sjpResulted.getSession().getSessionId().toString())
                .add(CASE_ID, CASE_ID_VALUE)
                .add(DEFENDANT_ID, DEFENDANT_ID_VALUE)
                .build();

        sendGeneratePoliceResultsForADefendantCommand(payload);

        verifyPrivateEventsForPoliceGenerateResultsForDefendant();
        Optional<String> response = verifyInPublicTopic();
        assertThat(sjpResulted.getCases().get(0).getDefendants().get(0).getOffences().get(0).getBaseOffenceDetails().getOffenceSequenceNumber(), is(new JSONObject(response.get()).getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).get("offenceSequenceNumber")));

    }

    @Test
    public void testGeneratePoliceResultsForDefendantSJPWhenSpiOutFalse() throws JMSException {
        final PublicSjpResulted sjpResulted = basicSJPCaseResulted();
        sjpResulted.getCases().get(0).setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        publicSjpResultedShared(sjpResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        verifyPrivateEventsWithPoliceResultGenerated(false);
        verifyNotInPublicTopic();

        final JsonObject payload = Json.createObjectBuilder()
                .add(SESSION_ID, sjpResulted.getSession().getSessionId().toString())
                .add(CASE_ID, CASE_ID_VALUE)
                .add(DEFENDANT_ID, DEFENDANT_ID_VALUE)
                .build();

        sendGeneratePoliceResultsForADefendantCommand(payload);

        verifyPrivateEventsForPoliceGenerateResultsForDefendant(false);
        verifyNotInPublicTopic();
    }

    @Test
    public void journeyHearingToResults() throws JMSException {
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate(JurisdictionType.MAGISTRATES);

        final Hearing hearingIn = resultsMessage.getHearing();
        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();

        //share results
        hearingResultsHaveBeenShared(resultsMessage);
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
        final PublicHearingResulted resultsMessage = basicShareResultsTemplate(MAGISTRATES);

        final Hearing hearingIn = resultsMessage.getHearing();
        final UUID defendantId0 =
                resultsMessage.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId();


        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());
        hearingResultsHaveBeenShared(resultsMessage);
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
        PublicHearingResulted resultsMessage = basicShareResultsTemplate(MAGISTRATES);

        final Hearing hearingIn = resultsMessage.getHearing();

        hearingResultsHaveBeenShared(resultsMessage);
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
    public void shouldDisplayAllInternalDetailsInHearingResults() {
        PublicHearingResulted resultsMessage = basicShareResultsTemplate(MAGISTRATES);

        final Hearing hearingIn = resultsMessage.getHearing();

        hearingResultsHaveBeenShared(resultsMessage);
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
            final InputStream inputStream = ResultsIT.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, IsNull.notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset()).replace("HEARING_ID", hearingId.toString());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = createReader(new StringReader(request));
        return reader.readObject();
    }


    @Test
    public void shouldBeSentToSpiOutForApplicationWithNoJudicialResults() throws JMSException {

        final PublicHearingResulted resultsMessage = PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithApplication(randomUUID(), MAGISTRATES))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier().setProsecutionAuthorityCode("someCode");
        resultsMessage.getHearing().getProsecutionCases().get(0).setOriginatingOrganisation(null);
        resultsMessage.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getProsecutionCaseIdentifier().setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);
        hearingResultsHaveBeenShared(resultsMessage);
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
    public void shouldBeSentToSpiOutForApplicationWithJudicialResults() throws JMSException {

        final PublicHearingResulted resultsMessage = PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithCustomApplication(randomUUID(), MAGISTRATES,
                        singletonList(courtApplication()
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
                                .withAllegationOrComplaintStartDate(now())
                                .withPlea(Plea.plea().withOffenceId(randomUUID()).withPleaDate(now()).withPleaValue("NOT_GUILTY").build())
                                .withVerdict(Verdict.verdict()
                                        .withVerdictType(VerdictType.verdictType()
                                                .withId(fromString("3f0d69d0-2fda-3472-8d4c-a6248f661825"))
                                                .withCategory(STRING.next())
                                                .withCategoryType(STRING.next())
                                                .withCjsVerdictCode("N")
                                                .build())
                                        .withOriginatingHearingId(randomUUID())
                                        .withOffenceId(randomUUID())
                                        .withVerdictDate(now())
                                        .build())
                                .build())))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        resultsMessage.getHearing().getProsecutionCases().get(0).getProsecutionCaseIdentifier().setProsecutionAuthorityCode("someCode");
        resultsMessage.getHearing().getProsecutionCases().get(0).setOriginatingOrganisation(null);
        resultsMessage.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getProsecutionCaseIdentifier().setProsecutionAuthorityCode(PROSECUTOR_WITH_SPI_OUT_FALSE);

        hearingResultsHaveBeenShared(resultsMessage);
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
    public void shouldSentSpiOutForApplicationWithCourtOrderOnly() throws JMSException {
        final JsonObject payload = getPayload("json/public.hearing-resulted-court-order.json", randomUUID());
        final PublicHearingResulted publicHearingResulted = jsonToObjectConverter.convert(payload, PublicHearingResulted.class);
        hearingResultsHaveBeenShared(publicHearingResulted);
        whenPrisonAdminTriesToViewResultsForThePerson(getUserId());

        LocalDate startDate = publicHearingResulted.getHearing().getHearingDays().get(0).getSittingDay().toLocalDate();
        startDate = of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth() - 1);

        getSummariesByDate(startDate);
        verifyPrivateEventsWithPoliceResultGenerated();

        Optional<String> response = verifyInPublicTopic();
        final JSONObject jsonObject = new JSONObject(response.get());
        assertThat(jsonObject.getString("caseId"), is("4d7fd02d-2297-4249-a7c6-d1d7bd567d58"));
        assertThat(jsonObject.getJSONObject("defendant").getJSONArray("offences").length(), is(2));
        assertThat(jsonObject.getJSONObject("defendant").getJSONArray("offences").getJSONObject(0).getString("id"), is("b729153d-50e3-4ce4-811c-f16799043d4f"));
    }
}