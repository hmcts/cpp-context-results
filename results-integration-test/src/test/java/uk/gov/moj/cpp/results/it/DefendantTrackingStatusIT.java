package uk.gov.moj.cpp.results.it;

import static com.google.common.collect.ImmutableList.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.closeMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.createMessageConsumers;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.hearingResultsHaveBeenShared;
import static uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions.setLoggedInUserAsCourtAdmin;
import static uk.gov.moj.cpp.results.it.steps.data.factory.HearingResultDataFactory.getUserId;
import static uk.gov.moj.cpp.results.it.utils.EventGridStub.stubEventGridEndpoint;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubGetOrgainsationUnit;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubJudicialResults;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubModeOfTrialReasons;
import static uk.gov.moj.cpp.results.it.utils.ReferenceDataServiceStub.stubSpiOutFlag;
import static uk.gov.moj.cpp.results.it.utils.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.setupUserAsPrisonAdminGroup;
import static uk.gov.moj.cpp.results.test.TestTemplates.RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_ACTIVATE;
import static uk.gov.moj.cpp.results.test.TestTemplates.RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_DEACTIVATE;
import static uk.gov.moj.cpp.results.test.TestTemplates.RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_OFF;
import static uk.gov.moj.cpp.results.test.TestTemplates.RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_ON;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplate;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.it.steps.ResultsStepDefinitions;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

import javax.jms.JMSException;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S2699")
public class DefendantTrackingStatusIT {

    private static final String DEFENDANT_ID_VALUE = "dddd1111-1e20-4c21-916a-81a6c90239e5";
    private static final String DEFENDANT_ID_VALUE2 = "dddd2222-1e20-4c21-916a-81a6c90239e5";
    private static final String DEFENDANT_ID_VALUE3 = "dddd3333-1e20-4c21-916a-81a6c90239e5";
    private static final String DEFENDANT_ID_VALUE4 = "dddd4444-1e20-4c21-916a-81a6c90239e5";

    private final LocalDate LAST_MODIFIED_TIME_TODAY_LOCALDATE = LocalDate.now();
    private final LocalDate LAST_MODIFIED_TIME_LAST_WEEK_LOCALDATE = LocalDate.now().minusDays(7);

    private final String LAST_MODIFIED_TIME_TODAY = LAST_MODIFIED_TIME_TODAY_LOCALDATE.atStartOfDay(ZoneOffset.UTC).toString();
    private final String LAST_MODIFIED_TIME_LAST_WEEK = LAST_MODIFIED_TIME_LAST_WEEK_LOCALDATE.atStartOfDay(ZoneOffset.UTC).toString();

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @BeforeAll
    public static void setUpClass() {
        setupUserAsPrisonAdminGroup(getUserId());
        stubEventGridEndpoint();
        stubCountryNationalities();
        stubGetOrgainsationUnit();
        stubJudicialResults();
        stubModeOfTrialReasons();
    }

    @AfterEach
    public void teardown() throws JMSException {
        closeMessageConsumers();
    }

    @BeforeEach
    public void setUp() {
        cleanViewStoreTables();
        stubSpiOutFlag(true, true);
        createMessageConsumers();
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldUpdateDefendantTrackingStatusWhenElectronicMonitoringResultsSharedInNextHearing() {

        final PublicHearingResulted template = basicShareResultsTemplate(MAGISTRATES);

        final Hearing hearing = template.getHearing();
        final Defendant defendant1 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE)).findFirst().get();
        final Offence offence1 = defendant1.getOffences().get(0);
        final Defendant defendant2 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE2)).findFirst().get();
        final Offence offence2 = defendant2.getOffences().get(0);

        // update template with ELMON result code before sharing results
        final PublicHearingResulted firstHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_ACTIVATE, LAST_MODIFIED_TIME_LAST_WEEK_LOCALDATE, Optional.of(offence1), Optional.of(offence2), empty());

        setLoggedInUserAsCourtAdmin(getUserId());

        hearingResultsHaveBeenShared(firstHearing);
        final Matcher[] matcher = {
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE, DEFENDANT_ID_VALUE2)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*].offenceId", hasItem(offence2.getId().toString())),
                withJsonPath("$.defendants[*].trackingStatus[*].emStatus", hasItem(true)),
                withJsonPath("$.defendants[*].trackingStatus[*].woaStatus", hasItem(false)),
                withJsonPath("$.defendants[*].trackingStatus[*].emLastModifiedTime", hasItem(LAST_MODIFIED_TIME_LAST_WEEK)),
        };

        StringJoiner defendantIds = new StringJoiner(",");
        defendantIds.add(DEFENDANT_ID_VALUE)
                .add(DEFENDANT_ID_VALUE2)
                .add(DEFENDANT_ID_VALUE3)
                .add(DEFENDANT_ID_VALUE4);

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcher);

        // create a second hearing with all 4 defendants, def 1 and def 2 having offences from previous hearing, def 3 and def 4 having new offences
        final PublicHearingResulted secondHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_DEACTIVATE, LAST_MODIFIED_TIME_TODAY_LOCALDATE, Optional.of(offence1), Optional.of(offence2), empty());

        hearingResultsHaveBeenShared(secondHearing);

        final Matcher[] matcherAfterDeactivation = {
                withJsonPath("$.defendants", hasSize(0))
        };

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcherAfterDeactivation);
    }

    @Test
    public void shouldUpdateDefendantTrackingStatusWhenWoAResultsSharedInNextHearing() {

        final PublicHearingResulted template = basicShareResultsTemplate(MAGISTRATES);

        final Hearing hearing = template.getHearing();
        final Defendant defendant1 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE)).findFirst().get();
        final Offence offence1 = defendant1.getOffences().get(0);
        final Defendant defendant2 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE2)).findFirst().get();
        final Offence offence2 = defendant2.getOffences().get(0);

        // update template with "Warrants of arrest" result code before sharing results
        final PublicHearingResulted firstHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_ON, LAST_MODIFIED_TIME_LAST_WEEK_LOCALDATE, Optional.of(offence1), Optional.of(offence2), empty());

        setLoggedInUserAsCourtAdmin(getUserId());

        hearingResultsHaveBeenShared(firstHearing);
        final Matcher[] matcher = {
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE, DEFENDANT_ID_VALUE2)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*].offenceId", hasItem(offence2.getId().toString())),
                withJsonPath("$.defendants[*].trackingStatus[*].woaStatus", hasItem(true)),
                withJsonPath("$.defendants[*].trackingStatus[*].emStatus", hasItem(false)),
                withJsonPath("$.defendants[*].trackingStatus[*].woaLastModifiedTime", hasItem(LAST_MODIFIED_TIME_LAST_WEEK)),
        };

        StringJoiner defendantIds = new StringJoiner(",");
        defendantIds.add(DEFENDANT_ID_VALUE)
                .add(DEFENDANT_ID_VALUE2)
                .add(DEFENDANT_ID_VALUE3)
                .add(DEFENDANT_ID_VALUE4);

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcher);

        // create a second hearing with all 4 defendants, def 1 and def 2 having offences from previous hearing, def 3 and def 4 having new offences
        final PublicHearingResulted secondHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_OFF, LAST_MODIFIED_TIME_TODAY_LOCALDATE, Optional.of(offence1), Optional.of(offence2), empty());

        hearingResultsHaveBeenShared(secondHearing);

        final Matcher[] matcherAfterDeactivation = {
                withJsonPath("$.defendants", hasSize(0))
        };

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcherAfterDeactivation);
    }

    @Test
    public void shouldUpdateDefendantTrackingStatusWhenElectronicMonitoringResultsReSharedAtALaterDate() {

        final PublicHearingResulted template = basicShareResultsTemplate(MAGISTRATES);

        final Hearing hearing = template.getHearing();
        final Defendant defendant1 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE)).findFirst().get();
        final Offence offence1 = defendant1.getOffences().get(0);
        final Defendant defendant2 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE2)).findFirst().get();
        final Offence offence2 = defendant2.getOffences().get(0);

        // update template with ELMON result code before sharing results
        final PublicHearingResulted firstHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_ACTIVATE, LAST_MODIFIED_TIME_LAST_WEEK_LOCALDATE, Optional.of(offence1), Optional.of(offence2), empty());

        setLoggedInUserAsCourtAdmin(getUserId());

        hearingResultsHaveBeenShared(firstHearing);
        final Matcher[] matcher = {
                withJsonPath("$.defendants", hasSize(2)),
                withJsonPath("$.defendants[*].defendantId", not(hasItems(DEFENDANT_ID_VALUE3, DEFENDANT_ID_VALUE4))),
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE, DEFENDANT_ID_VALUE2)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*].offenceId", hasItem(offence2.getId().toString())),
                withJsonPath("$.defendants[*].trackingStatus[*].emStatus", hasItem(true)),
                withJsonPath("$.defendants[*].trackingStatus[*].woaStatus", hasItem(false)),
                withJsonPath("$.defendants[*].trackingStatus[*].emLastModifiedTime", hasItem(LAST_MODIFIED_TIME_LAST_WEEK)),
        };

        StringJoiner defendantIds = new StringJoiner(",");
        defendantIds.add(DEFENDANT_ID_VALUE)
                .add(DEFENDANT_ID_VALUE2)
                .add(DEFENDANT_ID_VALUE3)
                .add(DEFENDANT_ID_VALUE4);

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcher);

        // update the same hearing for def 1 and def 2 for a re-share
        final PublicHearingResulted firstHearingReshare = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_DEACTIVATE, LAST_MODIFIED_TIME_TODAY_LOCALDATE, Optional.of(offence1), Optional.of(offence2), Optional.of(hearing.getId()));

        hearingResultsHaveBeenShared(firstHearingReshare);

        final Matcher[] matcherAfterDeactivation = {
                withJsonPath("$.defendants", hasSize(0))
        };

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcherAfterDeactivation);
    }

    @Test
    public void shouldUpdateDefendantTrackingStatusWhenWoAResultsReSharedAtALaterDate() {

        final PublicHearingResulted template = basicShareResultsTemplate(MAGISTRATES);

        final Hearing hearing = template.getHearing();
        final Defendant defendant1 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE)).findFirst().get();
        final Offence offence1 = defendant1.getOffences().get(0);
        final Defendant defendant2 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE2)).findFirst().get();
        final Offence offence2 = defendant2.getOffences().get(0);

        // update template with "Warrants of arrest" result code before sharing results
        final PublicHearingResulted firstHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_ON, LAST_MODIFIED_TIME_LAST_WEEK_LOCALDATE, Optional.of(offence1), Optional.of(offence2), empty());

        setLoggedInUserAsCourtAdmin(getUserId());

        hearingResultsHaveBeenShared(firstHearing);
        final Matcher[] matcher = {
                withJsonPath("$.defendants", hasSize(2)),
                withJsonPath("$.defendants[*].defendantId", not(hasItems(DEFENDANT_ID_VALUE3, DEFENDANT_ID_VALUE4))),
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE, DEFENDANT_ID_VALUE2)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*].offenceId", hasItem(offence2.getId().toString())),
                withJsonPath("$.defendants[*].trackingStatus[*].woaStatus", hasItem(true)),
                withJsonPath("$.defendants[*].trackingStatus[*].emStatus", hasItem(false)),
                withJsonPath("$.defendants[*].trackingStatus[*].woaLastModifiedTime", hasItem(LAST_MODIFIED_TIME_LAST_WEEK)),
        };

        StringJoiner defendantIds = new StringJoiner(",");
        defendantIds.add(DEFENDANT_ID_VALUE)
                .add(DEFENDANT_ID_VALUE2)
                .add(DEFENDANT_ID_VALUE3)
                .add(DEFENDANT_ID_VALUE4);

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcher);

        // update the same hearing for def 1 and def 2 for a re-share
        final PublicHearingResulted secondHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_OFF, LAST_MODIFIED_TIME_TODAY_LOCALDATE, Optional.of(offence1), Optional.of(offence2), Optional.of(hearing.getId()));

        hearingResultsHaveBeenShared(secondHearing);

        final Matcher[] matcherAfterDeactivation = {
                withJsonPath("$.defendants", hasSize(0))
        };

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcherAfterDeactivation);
    }

    @Test
    public void shouldNotUpdateDefendantTrackingStatusWhenElectronicMonitoringResultsReSharedAfterANewHearing() {

        final PublicHearingResulted template = basicShareResultsTemplate(MAGISTRATES);

        final Hearing hearing = template.getHearing();
        final Defendant defendant1 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE)).findFirst().get();
        final Offence offence1 = defendant1.getOffences().get(0);
        final Defendant defendant2 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE2)).findFirst().get();
        final Offence offence2 = defendant2.getOffences().get(0);

        // update template with ELMON result code before sharing results
        final PublicHearingResulted firstHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_ACTIVATE, LAST_MODIFIED_TIME_LAST_WEEK_LOCALDATE, Optional.of(offence1), Optional.of(offence2), empty());

        setLoggedInUserAsCourtAdmin(getUserId());

        hearingResultsHaveBeenShared(firstHearing);
        final Matcher[] matcher = {
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE, DEFENDANT_ID_VALUE2)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[*].trackingStatus[*].emStatus", hasItem(true)),
                withJsonPath("$.defendants[*].trackingStatus[*].woaStatus", hasItem(false)),
                withJsonPath("$.defendants[*].trackingStatus[*].emLastModifiedTime", hasItem(LAST_MODIFIED_TIME_LAST_WEEK)),
        };

        StringJoiner defendantIds = new StringJoiner(",");
        defendantIds.add(DEFENDANT_ID_VALUE)
                .add(DEFENDANT_ID_VALUE2)
                .add(DEFENDANT_ID_VALUE3)
                .add(DEFENDANT_ID_VALUE4);

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcher);

        // create a second hearing with all 4 defendants, def 1 and def 2 having offences from previous hearing, def 3 and def 4 having new offences, EM status EXTENDED
        final PublicHearingResulted newHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_ACTIVATE, LAST_MODIFIED_TIME_TODAY_LOCALDATE, Optional.of(offence1), Optional.of(offence2), empty());

        hearingResultsHaveBeenShared(newHearing);

        final Matcher[] matcherAfterAmend = {
                withJsonPath("$.defendants", hasSize(2)),
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE, DEFENDANT_ID_VALUE2)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*]", hasSize(1)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*]", hasSize(1)),

                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*].offenceId", hasItem(offence2.getId().toString())),
                withJsonPath("$.defendants[*].trackingStatus[*].emStatus", hasItem(true)),
                withJsonPath("$.defendants[*].trackingStatus[*].woaStatus", hasItem(false)),
                withJsonPath("$.defendants[*].trackingStatus[*].emLastModifiedTime", hasItem(LAST_MODIFIED_TIME_TODAY)),

        };

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcherAfterAmend);

        // amend and re-share first hearing on a later date, but it will be ignored because we have a newer order date from the previous hearing
        final PublicHearingResulted amendedHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_DEACTIVATE, LAST_MODIFIED_TIME_LAST_WEEK_LOCALDATE, Optional.of(offence1), Optional.of(offence2), Optional.of(hearing.getId()));

        hearingResultsHaveBeenShared(amendedHearing);

        final Matcher[] matcherAfterDeactivation = {
                withJsonPath("$.defendants", hasSize(2)),
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE, DEFENDANT_ID_VALUE2)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*]", hasSize(1)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*]", hasSize(1)),

                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*].offenceId", hasItem(offence2.getId().toString())),
                withJsonPath("$.defendants[*].trackingStatus[*].emStatus", hasItem(true)),
                withJsonPath("$.defendants[*].trackingStatus[*].emLastModifiedTime", hasItem(LAST_MODIFIED_TIME_TODAY)),

        };

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcherAfterDeactivation);
    }

    @Test
    public void shouldNotUpdateDefendantTrackingStatusWhenWoAResultsReSharedAfterANewHearing() {

        final PublicHearingResulted template = basicShareResultsTemplate(MAGISTRATES);

        final Hearing hearing = template.getHearing();
        final Defendant defendant1 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE)).findFirst().get();
        final Offence offence1 = defendant1.getOffences().get(0);
        final Defendant defendant2 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE2)).findFirst().get();
        final Offence offence2 = defendant2.getOffences().get(0);

        // update template with "Warrants of arrest" result code before sharing results
        final PublicHearingResulted firstHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_ON, LAST_MODIFIED_TIME_LAST_WEEK_LOCALDATE, Optional.of(offence1), Optional.of(offence2), empty());

        setLoggedInUserAsCourtAdmin(getUserId());

        hearingResultsHaveBeenShared(firstHearing);
        final Matcher[] matcher = {
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE, DEFENDANT_ID_VALUE2)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[*].trackingStatus[*].woaStatus", hasItem(true)),
                withJsonPath("$.defendants[*].trackingStatus[*].emStatus", hasItem(false)),
                withJsonPath("$.defendants[*].trackingStatus[*].woaLastModifiedTime", hasItem(LAST_MODIFIED_TIME_LAST_WEEK)),
        };

        StringJoiner defendantIds = new StringJoiner(",");
        defendantIds.add(DEFENDANT_ID_VALUE)
                .add(DEFENDANT_ID_VALUE2)
                .add(DEFENDANT_ID_VALUE3)
                .add(DEFENDANT_ID_VALUE4);

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcher);

        // create a second hearing with all 4 defendants, def 1 and def 2 having offences from previous hearing, def 3 and def 4 having new offences, Warrant of Arrest status EXTENDED
        final PublicHearingResulted newHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_ON, LAST_MODIFIED_TIME_TODAY_LOCALDATE, Optional.of(offence1), Optional.of(offence2), empty());

        hearingResultsHaveBeenShared(newHearing);

        final Matcher[] matcherAfterAmend = {
                withJsonPath("$.defendants", hasSize(2)),
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE, DEFENDANT_ID_VALUE2)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*]", hasSize(1)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*]", hasSize(1)),

                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*].offenceId", hasItem(offence2.getId().toString())),
                withJsonPath("$.defendants[*].trackingStatus[*].woaStatus", hasItem(true)),
                withJsonPath("$.defendants[*].trackingStatus[*].emStatus", hasItem(false)),
                withJsonPath("$.defendants[*].trackingStatus[*].woaLastModifiedTime", hasItem(LAST_MODIFIED_TIME_TODAY)),

        };

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcherAfterAmend);

        // amend and re-share first hearing on a later date, but it will be ignored because we have a newer order date from the previous hearing
        final PublicHearingResulted amendedHearing = updateHearingTemplateWithResultDefinitionGroup(template, RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_OFF, LAST_MODIFIED_TIME_LAST_WEEK_LOCALDATE, Optional.of(offence1), Optional.of(offence2), Optional.of(hearing.getId()));

        hearingResultsHaveBeenShared(amendedHearing);

        final Matcher[] matcherAfterDeactivation = {
                withJsonPath("$.defendants", hasSize(2)),
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE, DEFENDANT_ID_VALUE2)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*]", hasSize(1)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*]", hasSize(1)),

                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*].offenceId", hasItem(offence2.getId().toString())),
                withJsonPath("$.defendants[*].trackingStatus[*].woaStatus", hasItem(true)),
                withJsonPath("$.defendants[*].trackingStatus[*].emStatus", hasItem(false)),
                withJsonPath("$.defendants[*].trackingStatus[*].woaLastModifiedTime", hasItem(LAST_MODIFIED_TIME_TODAY)),

        };

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcherAfterDeactivation);
    }

    @Test
    public void shouldUpdateDefendantTrackingStatusWhenBothElectronicMonitoringAndWoAResultsSharedInNextHearing() {

        final PublicHearingResulted template = basicShareResultsTemplate(MAGISTRATES);

        final Hearing hearing = template.getHearing();
        final Defendant defendant1 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE)).findFirst().get();
        final Offence offence1 = defendant1.getOffences().get(0);
        final Defendant defendant2 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE2)).findFirst().get();
        final Offence offence2 = defendant2.getOffences().get(0);

        // update template with "Warrants of arrest" result code for defendant1 and "ELMON" for defendant2 before sharing results
        final PublicHearingResulted firstHearing = updateHearingTemplateWithDifferentResultDefinitionGroupsForMultipleDefendants(template, RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_ON, RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_ACTIVATE, LAST_MODIFIED_TIME_LAST_WEEK_LOCALDATE, Optional.of(offence1), Optional.of(offence2));

        setLoggedInUserAsCourtAdmin(getUserId());

        hearingResultsHaveBeenShared(firstHearing);
        final Matcher[] matcher = {
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE, DEFENDANT_ID_VALUE2)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*].offenceId", hasItem(offence2.getId().toString())),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].woaStatus", hasItem(true)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].woaLastModifiedTime", hasItem(LAST_MODIFIED_TIME_LAST_WEEK)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*].emStatus", hasItem(true)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE2 + "')].trackingStatus[*].emLastModifiedTime", hasItem(LAST_MODIFIED_TIME_LAST_WEEK)),
        };

        StringJoiner defendantIds = new StringJoiner(",");
        defendantIds.add(DEFENDANT_ID_VALUE)
                .add(DEFENDANT_ID_VALUE2)
                .add(DEFENDANT_ID_VALUE3)
                .add(DEFENDANT_ID_VALUE4);

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcher);

        // create a second hearing with all 4 defendants, def 1 and def 2 having offences from previous hearing, def 3 and def 4 having new offences
        final PublicHearingResulted secondHearing = updateHearingTemplateWithDifferentResultDefinitionGroupsForMultipleDefendants(template, RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_OFF, RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_DEACTIVATE, LAST_MODIFIED_TIME_TODAY_LOCALDATE, Optional.of(offence1), Optional.of(offence2));

        hearingResultsHaveBeenShared(secondHearing);

        final Matcher[] matcherAfterDeactivation = {
                withJsonPath("$.defendants", hasSize(0))
        };

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcherAfterDeactivation);
    }

    @Test
    public void shouldUpdateDefendantTrackingStatusWhenBothElectronicMonitoringAndWoAResultsSharedForSameDefendant() {

        final PublicHearingResulted template = basicShareResultsTemplate(MAGISTRATES);

        final Hearing hearing = template.getHearing();
        final Defendant defendant1 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE)).findFirst().get();
        final Offence offence1 = defendant1.getOffences().get(0);
        final Defendant defendant2 = hearing.getProsecutionCases().get(0).getDefendants().stream().filter(d -> d.getId().toString().equals(DEFENDANT_ID_VALUE2)).findFirst().get();
        final Offence offence2 = defendant2.getOffences().get(0);

        // update template with both "Warrants of arrest" and "ELMON" result codes for defendant1
        final PublicHearingResulted firstHearing = updateHearingTemplateWithDifferentResultDefinitionGroupsForMultipleDefendants(template, RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_ON + "," + RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_ACTIVATE, "", LAST_MODIFIED_TIME_LAST_WEEK_LOCALDATE, Optional.of(offence1), Optional.of(offence2));

        setLoggedInUserAsCourtAdmin(getUserId());

        hearingResultsHaveBeenShared(firstHearing);
        final Matcher[] matcher = {
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE)),
                withJsonPath("$.defendants[*].defendantId", not(hasItems(DEFENDANT_ID_VALUE2))),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].woaStatus", hasItem(true)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].woaLastModifiedTime", hasItem(LAST_MODIFIED_TIME_LAST_WEEK)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].emStatus", hasItem(true)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].emLastModifiedTime", hasItem(LAST_MODIFIED_TIME_LAST_WEEK)),
        };

        StringJoiner defendantIds = new StringJoiner(",");
        defendantIds.add(DEFENDANT_ID_VALUE)
                .add(DEFENDANT_ID_VALUE2)
                .add(DEFENDANT_ID_VALUE3)
                .add(DEFENDANT_ID_VALUE4);

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcher);

        // create a second hearing with defendant1's WoA and ELMON extended, def2, def3 and def4 remains the same
        final PublicHearingResulted secondHearing = updateHearingTemplateWithDifferentResultDefinitionGroupsForMultipleDefendants(template, RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_ON + "," + RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_ACTIVATE, "", LAST_MODIFIED_TIME_TODAY_LOCALDATE, Optional.of(offence1), Optional.of(offence2));

        hearingResultsHaveBeenShared(secondHearing);

        final Matcher[] matcherForExtended = {
                withJsonPath("$.defendants[*].defendantId", hasItems(DEFENDANT_ID_VALUE)),
                withJsonPath("$.defendants[*].defendantId", not(hasItems(DEFENDANT_ID_VALUE2))),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].offenceId", hasItem(offence1.getId().toString())),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].woaStatus", hasItem(true)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].woaLastModifiedTime", hasItem(LAST_MODIFIED_TIME_TODAY)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].emStatus", hasItem(true)),
                withJsonPath("$.defendants[?(@.defendantId=='" + DEFENDANT_ID_VALUE + "')].trackingStatus[*].emLastModifiedTime", hasItem(LAST_MODIFIED_TIME_TODAY)),
        };

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcherForExtended);

        // create a third hearing with defendant1's WoA and ELMON ended, def2, def3 and def4 remains the same
        final PublicHearingResulted thirdHearing = updateHearingTemplateWithDifferentResultDefinitionGroupsForMultipleDefendants(template, RESULT_DEFINITION_GROUP_WARRANT_OF_ARREST_OFF + "," + RESULT_DEFINITION_GROUP_ELECTRONIC_MONITORING_DEACTIVATE, "", LAST_MODIFIED_TIME_TODAY_LOCALDATE, Optional.of(offence1), Optional.of(offence2));

        hearingResultsHaveBeenShared(thirdHearing);
        final Matcher[] matcherForWoaAndELMONEnded = {
                withJsonPath("$.defendants[*]", hasSize(0))
        };

        ResultsStepDefinitions.getDefendantTrackingStatus(defendantIds.toString(), matcherForWoaAndELMONEnded);

    }

    /**
     * createPublicHearingResulted
     *
     * @param resultDefinitionGroup - comma separated result definition group values to determine the tracking status (e.g. ELMON, ELMONEND, Warrants of arrest, WOAEXTEND)
     * @param orderDate             - result share date
     * @param offence               - optional offence. if provided, defendant's offence id will be overwritten with the given one, meaning this offence has been resulted before in a hearing.
     * @param offence2              - optional offence2. if provided, second defendant's offence id will be overwritten with the given one, meaning this offence has been resulted before in a hearing.
     * @param resharedHearingId     - optional hearingId. if provided, hearing id will be overwritten so the returned template can be used as a reshared hearing.
     */
    private PublicHearingResulted updateHearingTemplateWithResultDefinitionGroup(final PublicHearingResulted hearingTemplate, final String resultDefinitionGroup, final LocalDate orderDate, final Optional<Offence> offence, final Optional<Offence> offence2, final Optional<UUID> resharedHearingId) {

        Defendant defendant1 = Defendant.defendant().build();
        Defendant defendant2 = Defendant.defendant().build();

        if (offence.isPresent()) {
            final Offence o1 = hearingTemplate.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);

            final JudicialResult judicialResult = JudicialResult.judicialResult().withValuesFrom(o1.getJudicialResults().get(0))
                    .withResultDefinitionGroup(resultDefinitionGroup)
                    .withOrderedDate(orderDate)
                    .withOffenceId(offence.get().getId())
                    .withJudicialResultId(randomUUID())
                    .build();

            defendant1 = Defendant.defendant().withValuesFrom(hearingTemplate.getHearing().getProsecutionCases().get(0).getDefendants().get(0)).withOffences(Arrays.asList(
                    Offence.offence().withValuesFrom(o1).withId(offence.get().getId())
                            .withJudicialResults(of(judicialResult))
                            .build()
            )).build();

        }
        if (offence2.isPresent()) {
            final Offence o2 = hearingTemplate.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(0);

            final JudicialResult judicialResult = JudicialResult.judicialResult().withValuesFrom(o2.getJudicialResults().get(0))
                    .withResultDefinitionGroup(resultDefinitionGroup)
                    .withOrderedDate(orderDate)
                    .withOffenceId(offence2.get().getId())
                    .withJudicialResultId(randomUUID())
                    .build();

            defendant2 = Defendant.defendant().withValuesFrom(hearingTemplate.getHearing().getProsecutionCases().get(0).getDefendants().get(1)).withOffences(Arrays.asList(
                    Offence.offence().withValuesFrom(o2).withId(offence2.get().getId())
                            .withJudicialResults(of(judicialResult))
                            .build()
            )).build();

        }
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(hearingTemplate.getHearing().getProsecutionCases().get(0))
                .withDefendants(Arrays.asList(defendant1, defendant2)).build();

        hearingTemplate.setHearing(Hearing.hearing().withValuesFrom(hearingTemplate.getHearing()).
                withProsecutionCases(of(prosecutionCase)).build());

        if (resharedHearingId.isPresent()) {
            hearingTemplate.setHearing(Hearing.hearing().withValuesFrom(hearingTemplate.getHearing())
                    .withId(resharedHearingId.get()).build());
        }

        return hearingTemplate;
    }

    /**
     * createPublicHearingResulted
     *
     * @param resultDefinitionGroup1 - comma separated result definition group values to determine the tracking status (e.g. ELMON, ELMONEND, Warrants of arrest, WOAEXTEND) of defendant 1
     * @param resultDefinitionGroup2 - comma separated result definition group values to determine the tracking status (e.g. ELMON, ELMONEND, Warrants of arrest, WOAEXTEND) of defendant 2
     * @param orderDate              - result share date
     * @param offence                - optional offence. if provided, defendant's offence id will be overwritten with the given one, meaning this offence has been resulted before in a hearing.
     * @param offence2               - optional offence2. if provided, second defendant's offence id will be overwritten with the given one, meaning this offence has been resulted before in a hearing.
     */
    private PublicHearingResulted updateHearingTemplateWithDifferentResultDefinitionGroupsForMultipleDefendants(final PublicHearingResulted hearingTemplate, final String resultDefinitionGroup1, final String resultDefinitionGroup2, final LocalDate orderDate, final Optional<Offence> offence, final Optional<Offence> offence2) {

        Defendant defendant1 = Defendant.defendant().build();
        Defendant defendant2 = Defendant.defendant().build();

        if (offence.isPresent()) {
            final Offence o = hearingTemplate.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);

            final JudicialResult judicialResult = JudicialResult.judicialResult().withValuesFrom(o.getJudicialResults().get(0))
                    .withResultDefinitionGroup(resultDefinitionGroup1)
                    .withOrderedDate(orderDate)
                    .withOffenceId(offence.get().getId())
                    .withJudicialResultId(randomUUID())
                    .build();
            defendant1 = Defendant.defendant().withValuesFrom(hearingTemplate.getHearing().getProsecutionCases().get(0).getDefendants().get(0)).withOffences(Arrays.asList(
                    Offence.offence().withValuesFrom(o).withId(offence.get().getId())
                            .withJudicialResults(of(judicialResult))
                            .build()
            )).build();
        }
        if (offence2.isPresent()) {
            final Offence o2 = hearingTemplate.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(0);

            final JsonObject jr = objectToJsonObjectConverter.convert(o2.getJudicialResults().get(0));
            final JudicialResult deepCopyJudicialResult = JudicialResult.judicialResult().withValuesFrom(jsonToObjectConverter.convert(jr, JudicialResult.class))
                    .withResultDefinitionGroup(resultDefinitionGroup2)
                    .withOrderedDate(orderDate)
                    .withOffenceId(offence2.get().getId())
                    .withJudicialResultId(randomUUID())
                    .build();
            defendant2 = Defendant.defendant().withValuesFrom(hearingTemplate.getHearing().getProsecutionCases().get(0).getDefendants().get(1)).withOffences(Arrays.asList(
                    Offence.offence().withValuesFrom(o2).withId(offence2.get().getId())
                            .withJudicialResults(of(deepCopyJudicialResult))
                            .build()
            )).build();

        }

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(hearingTemplate.getHearing().getProsecutionCases().get(0))
                .withDefendants(Arrays.asList(defendant1, defendant2)).build();

        hearingTemplate.setHearing(Hearing.hearing().withValuesFrom(hearingTemplate.getHearing()).
                withProsecutionCases(of(prosecutionCase)).build());

        return hearingTemplate;
    }
}
