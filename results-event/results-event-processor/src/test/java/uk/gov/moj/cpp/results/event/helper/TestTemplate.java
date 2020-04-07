package uk.gov.moj.cpp.results.event.helper;

import static com.google.common.collect.ImmutableList.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.AllocationDecision.allocationDecision;
import static uk.gov.justice.sjp.results.BasePersonDetail.basePersonDetail;
import static uk.gov.justice.sjp.results.BaseResult.baseResult;
import static uk.gov.justice.sjp.results.CorporateDefendant.corporateDefendant;
import static uk.gov.justice.sjp.results.Prompts.prompts;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.sjp.results.BasePersonDetail;
import uk.gov.justice.sjp.results.BaseResult;
import uk.gov.justice.sjp.results.CaseDefendant;
import uk.gov.justice.sjp.results.CorporateDefendant;
import uk.gov.justice.sjp.results.Prompts;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TestTemplate {

    public static final UUID SESSION_ID = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6741d");
    public static final ZonedDateTime DATE_AND_TIME_OF_SESSION = ZonedDateTime.of(LocalDate.of(2019, 5, 2), LocalTime.of(12, 3, 10), ZoneId.systemDefault());

    private static final UUID RESULT_ID = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6743d");
    private static final UUID PROMPT_ID = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6741d");
    private static final UUID PROMPT_ID_1 = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6742d");
    private static final UUID ID = fromString("e4003b92-419b-4e47-b3f9-89a4bbd6743d");
    private static final String PRESENT_AT_HEARING = "F";

    private TestTemplate() {

    }

    public static List<BaseResult> getBaseResults() {
        final BaseResult baseResult = getBaseResult();
        final List<BaseResult> baseResultList = new ArrayList<>();
        baseResultList.add(baseResult);
        return baseResultList;
    }

    public static BaseResult getBaseResult() {
        return baseResult().withId(ID)
                .withPrompts(buildListOfPrompt())
                .build();
    }

    public static List<BaseResult> getBaseResultsWithNoPrompts() {
        final BaseResult baseResult = getBaseResult();
        baseResult.setPrompts(null);
        final List<BaseResult> baseResultList = new ArrayList<>();
        baseResultList.add(baseResult);
        return baseResultList;
    }


    public static List<Prompts> buildListOfPrompt() {
        final List<Prompts> promptList = new ArrayList<>();
        promptList.add(buildPrompts(PROMPT_ID));
        promptList.add(buildPrompts(PROMPT_ID_1));
        return promptList;
    }

    public static Prompts buildPrompts(final UUID promptId) {
        return prompts().withId(promptId).withValue("10.00").build();
    }

    public static Prompts buildPromptsWithPromptIdAndValue(final UUID promptId, final String promptValue) {
        return prompts().withId(promptId).withValue(promptValue).build();
    }


    public static BasePersonDetail buildBasePersonDetails() {
        return basePersonDetail().withTelephoneNumberMobile("9999999999")
                .withTelephoneNumberMobile("8888888888")
                .withTelephoneNumberBusiness("7777777777")
                .withEmailAddress1("primaryemail@gmail.com")
                .withEmailAddress2("secondaryemail@gmail.com")
                .build();
    }

    public static ResultDefinition buildResultDefinition() {
        final ResultDefinition resultDefinition = new ResultDefinition();
        resultDefinition.setId(RESULT_ID);
        resultDefinition.setAdjournment("Y");
        resultDefinition.setLabel("label");
        resultDefinition.setShortCode("shortCode");
        resultDefinition.setLevel("level");
        resultDefinition.setRank(1);
        resultDefinition.setStartDate(new Date());
        resultDefinition.setEndDate(new Date());
        resultDefinition.setWelshLabel("welshLabel");
        resultDefinition.setIsAvailableForCourtExtract(true);
        resultDefinition.setFinancial("Y");
        resultDefinition.setCategory("A");
        resultDefinition.setCjsCode("cjsCode");
        resultDefinition.setConvicted("Y");
        resultDefinition.setVersion("1.0");
        resultDefinition.setUserGroups(of("1", "2"));
        resultDefinition.setPrompts(of(buildPrompt(PROMPT_ID), buildPrompt(PROMPT_ID_1)));
        resultDefinition.setTerminatesOffenceProceedings(false);
        resultDefinition.setLifeDuration(false);
        resultDefinition.setPublishedAsAPrompt(false);
        resultDefinition.setExcludedFromResults(false);
        resultDefinition.setAlwaysPublished(false);
        resultDefinition.setUrgent(false);
        resultDefinition.setD20(false);
        return resultDefinition;
    }

    public static Prompt buildPrompt(final UUID promptId) {
        final Prompt prompt = new Prompt();
        prompt.setDuration("duration");
        prompt.setLabel("label");
        prompt.setWelshLabel("welshLabel");
        prompt.setMandatory(false);
        prompt.setType("type");
        prompt.setSequence(2);
        prompt.setFixedListId(randomUUID());
        prompt.setReference("AOF");
        prompt.setId(promptId);
        prompt.setDurationSequence(1);
        return prompt;
    }



    public static CaseDefendant getCaseDefendant() {
        return CaseDefendant.caseDefendant()
                .withCorporateDefendant(getBuildCorporateDefendant())
                .withParentGuardianDetails(getBuildParentGuardianDetails())
                .build();
    }

    private static CorporateDefendant getBuildCorporateDefendant() {
        return corporateDefendant()
                .withAddress(buildAddress())
                .withPresentAtHearing(PRESENT_AT_HEARING)
                .withPncIdentifier("PNC123456")
                .withOrganisationName("OrganisationName").build();
    }

    private static Address buildAddress() {
        return address()
                .withAddress1("Fitzalan Place")
                .withAddress2("Cardiff")
                .withAddress3("addressline3")
                .withAddress4("address4")
                .withAddress5("address5")
                .withPostcode("CF24 0RZ")
                .build();
    }

    private static BasePersonDetail getBuildParentGuardianDetails() {
        return basePersonDetail()
                .withAddress(buildAddress())
                .withBirthDate(ZonedDateTime.of(LocalDate.of(2019, 2, 2), LocalTime.of(12, 3, 10), ZoneId.systemDefault()))
                .withEmailAddress1("parentguardianmemail1@random.random")
                .withEmailAddress2("parentguardianemail2@random.random")
                .withFirstName("ParentGuardianFirstName")
                .withGender(uk.gov.justice.sjp.results.Gender.MALE)
                .withLastName("ParentGuardianLastName")
                .withPersonTitle("Ms")
                .withTelephoneNumberBusiness("6666666666")
                .withTelephoneNumberHome("77777777777")
                .withTelephoneNumberMobile("8888888888")
                .build();
    }

    public static AllocationDecision buildAllocationDecision() {
        return allocationDecision().withOriginatingHearingId(randomUUID())
                .withAllocationDecisionDate(LocalDate.of(2018,3,14))
                .withAllocationDecisionDate(LocalDate.of(2018,12,12))
                .withMotReasonDescription("motDescription")
                .withMotReasonCode("10")
                .withMotReasonId(randomUUID())
                .withSequenceNumber(10)
                .withOffenceId(randomUUID())
                .build();
    }



    public static ResultDefinition buildResultDefinitionForPrimaryDuration(boolean lifeDuration) {
        final ResultDefinition resultDefinition = new ResultDefinition();
        resultDefinition.setId(RESULT_ID);
        resultDefinition.setAdjournment("Y");
        resultDefinition.setLabel("label");
        resultDefinition.setShortCode("shortCode");
        resultDefinition.setLevel("level");
        resultDefinition.setRank(1);
        resultDefinition.setStartDate(new Date());
        resultDefinition.setEndDate(new Date());
        resultDefinition.setWelshLabel("welshLabel");
        resultDefinition.setIsAvailableForCourtExtract(true);
        resultDefinition.setFinancial("Y");
        resultDefinition.setCategory("A");
        resultDefinition.setCjsCode("cjsCode");
        resultDefinition.setConvicted("Y");
        resultDefinition.setVersion("1.0");
        resultDefinition.setUserGroups(of("1", "2"));
        resultDefinition.setPrompts(of(buildPrompt(PROMPT_ID), buildPrompt(PROMPT_ID_1)));
        resultDefinition.setTerminatesOffenceProceedings(false);
        resultDefinition.setLifeDuration(lifeDuration);
        resultDefinition.setPublishedAsAPrompt(false);
        resultDefinition.setExcludedFromResults(false);
        resultDefinition.setAlwaysPublished(false);
        resultDefinition.setUrgent(false);
        resultDefinition.setD20(false);
        return resultDefinition;
    }

    public static Prompt buildPromptWithReference(final UUID promptId, final String reference) {
        final Prompt prompt = new Prompt();
        prompt.setDuration("duration");
        prompt.setLabel("label");
        prompt.setReference(reference);
        prompt.setWelshLabel("welshLabel");
        prompt.setMandatory(false);
        prompt.setType("type");
        prompt.setSequence(2);
        prompt.setDurationSequence(1);
        prompt.setFixedListId(randomUUID());
        prompt.setId(promptId);
        return prompt;
    }


    public static ResultDefinition buildResultDefinitionWithId(final UUID id, final String promptReference) {
        final ResultDefinition resultDefinition = new ResultDefinition();
        resultDefinition.setId(id);
        resultDefinition.setIsAvailableForCourtExtract(true);
        resultDefinition.setFinancial("financial");
        resultDefinition.setCategory("A");
        resultDefinition.setCjsCode("cjsCode");
        resultDefinition.setConvicted("Y");
        resultDefinition.setVersion("1.0");
        resultDefinition.setUserGroups(of("1", "2"));
        resultDefinition.setPrompts(of(buildPrompt(PROMPT_ID), buildPromptWithReference(id, promptReference)));
        resultDefinition.setTerminatesOffenceProceedings(false);
        return resultDefinition;
    }

    public static ResultDefinition buildResultDefinitionWithEmptyPrompts() {
        final ResultDefinition resultDefinition = new ResultDefinition();
        resultDefinition.setId(RESULT_ID);
        resultDefinition.setAdjournment("Y");
        resultDefinition.setLabel("label");
        resultDefinition.setShortCode("shortCode");
        resultDefinition.setLevel("level");
        resultDefinition.setRank(1);
        resultDefinition.setPrompts(of(buildPrompt(PROMPT_ID), buildPrompt(PROMPT_ID_1)));
        resultDefinition.setTerminatesOffenceProceedings(false);
        resultDefinition.setLifeDuration(false);
        resultDefinition.setPublishedAsAPrompt(false);
        resultDefinition.setExcludedFromResults(false);
        resultDefinition.setAlwaysPublished(false);
        resultDefinition.setUrgent(false);
        resultDefinition.setD20(false);
        return resultDefinition;
    }



}
