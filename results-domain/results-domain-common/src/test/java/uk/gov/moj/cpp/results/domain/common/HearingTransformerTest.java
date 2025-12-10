package uk.gov.moj.cpp.results.domain.common;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.external.ApiDefendant;
import uk.gov.justice.core.courts.external.ApiHearing;
import uk.gov.justice.core.courts.external.ApiJudicialResult;
import uk.gov.justice.core.courts.external.ApiOffence;
import uk.gov.justice.core.courts.external.BreachType;
import uk.gov.justice.core.courts.external.JurisdictionType;
import uk.gov.justice.core.courts.external.LinkType;
import uk.gov.justice.core.courts.external.OffenceActiveOrder;
import uk.gov.justice.core.courts.external.SummonsTemplateType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.HearingTransformer;
import uk.gov.moj.cpp.domains.JudicialRoleTypeEnum;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class HearingTransformerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingHelperTest.class.getName());

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private final HearingTransformer hearingTransformer = new HearingTransformer();

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldTransformHearingWithOutVehicleCode() {
        final JsonObject hearingJson = getHearingJson("hearing.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        final ApiHearing apiHearing = hearingTransformer.hearing(hearing).build();
        final ApiDefendant apiDefendant = apiHearing.getProsecutionCases().get(0).getDefendants().get(0);

        assertThat(apiHearing.getDefendantJudicialResults().size(), is(1));
        assertThat(apiDefendant.getJudicialResults().size(), is(2));
        assertThat(apiDefendant.getOffences().get(0).getJudicialResults().size(), is(2));
        assertThat(apiHearing.getCourtApplications().get(0).getJudicialResults().size(), is(2));
        assertThat(apiHearing.getCourtCentre().getCode(), is("1234"));
        final ApiOffence apiOffence = apiDefendant.getOffences().get(0);
        assertThat(apiOffence.getOffenceFacts().getVehicleCode(), nullValue());
        assertThat(apiOffence.getListingNumber(), is(2));
        final ApiJudicialResult apiJudicialResult = apiOffence.getJudicialResults().get(0);
        assertThat(apiJudicialResult.getNextHearing().getHearingLanguage().name(), is(HearingLanguage.ENGLISH.name()));
        assertThat(apiHearing.getJurisdictionType().name(), is(JurisdictionType.MAGISTRATES.name()));
    }

    @Test
    public void shouldTransformHearingWithCourtApplications() {
        final JsonObject hearingJson = getHearingJson("hearingWithCourtApplications.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        final ApiHearing apiHearing = hearingTransformer.hearing(hearing).build();
        final ApiDefendant apiDefendant = apiHearing.getProsecutionCases().get(0).getDefendants().get(0);

        assertThat(apiHearing.getDefendantJudicialResults().size(), is(1));
        assertThat(apiDefendant.getJudicialResults().size(), is(2));
        assertThat(apiDefendant.getOffences().get(0).getJudicialResults().size(), is(2));
        assertThat(apiHearing.getCourtApplications().get(0).getJudicialResults().size(), is(2));
        assertThat(apiHearing.getCourtCentre().getCode(), is("1234"));
        final ApiOffence apiOffence = apiDefendant.getOffences().get(0);
        assertThat(apiOffence.getOffenceFacts().getVehicleCode(), nullValue());
        final ApiJudicialResult apiJudicialResult = apiOffence.getJudicialResults().get(0);
        assertThat(apiJudicialResult.getNextHearing().getHearingLanguage().name(), is(HearingLanguage.ENGLISH.name()));
        assertThat(apiHearing.getJurisdictionType().name(), is(JurisdictionType.MAGISTRATES.name()));

        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getPersonDetails().getFirstName(), is("Fred"));
        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getPersonDetails().getLastName(), is("Smith"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getPersonDetails().getFirstName(), is("John"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getPersonDetails().getLastName(), is("Beard"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getAssociationStartDate().toString(), is("2019-09-12"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getAssociationEndDate().toString(), is("2019-12-12"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getFundingType().toString(), is("REPRESENTATION_ORDER"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getIsAssociatedByLAA(), is(true));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getLaaContractNumber(), is("LAA44569"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getName(), is("Test"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getIncorporationNumber(), is("cegH7rIgdX"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getRegisteredCharityNumber(), is("TestCharity"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getAddress(), notNullValue());
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getContact(), notNullValue());
        
        assertThat(apiHearing.getCourtApplications().get(0).getType().getId().toString(), is("c10e3b71-6a6d-45ef-9b62-34df4d54972b"));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getCategoryCode(), is("App category code"));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getLinkType().name(), is(LinkType.STANDALONE.name()));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getJurisdiction().name(), is(JurisdictionType.MAGISTRATES.name()));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getSummonsTemplateType().name(), is(SummonsTemplateType.GENERIC_APPLICATION.name()));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getBreachType().name(), is(BreachType.GENERIC_BREACH.name()));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getAppealFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getApplicantAppellantFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getPleaApplicableFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getCommrOfOathFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getCourtOfAppealFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getCourtExtractAvlFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getProsecutorThirdPartyFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getSpiOutApplicableFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getOffenceActiveOrder().name(), is(OffenceActiveOrder.OFFENCE.name()));

        assertThat(apiHearing.getCourtApplications().get(0).getApplicationReceivedDate(), is(LocalDate.of(2020,1,20)));
        assertThat(apiHearing.getCourtApplications().get(0).getApplicationStatus().toString(), is("DRAFT"));
    }

    @Test
    public void shouldTransformHearingWithCourtApplicationsWithMissingContactDetailsForDefenceOrganisation() {
        final JsonObject hearingJson = getHearingJson("hearingWithCourtApplicationsWithMissingContactDetailsForDefenceOrganisation.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        final ApiHearing apiHearing = hearingTransformer.hearing(hearing).build();
        final ApiDefendant apiDefendant = apiHearing.getProsecutionCases().get(0).getDefendants().get(0);

        assertThat(apiHearing.getDefendantJudicialResults().size(), is(1));
        assertThat(apiDefendant.getJudicialResults().size(), is(2));
        assertThat(apiDefendant.getOffences().get(0).getJudicialResults().size(), is(2));
        assertThat(apiHearing.getCourtApplications().get(0).getJudicialResults().size(), is(2));
        assertThat(apiHearing.getCourtCentre().getCode(), is("1234"));
        final ApiOffence apiOffence = apiDefendant.getOffences().get(0);
        assertThat(apiOffence.getOffenceFacts().getVehicleCode(), nullValue());
        final ApiJudicialResult apiJudicialResult = apiOffence.getJudicialResults().get(0);
        assertThat(apiJudicialResult.getNextHearing().getHearingLanguage().name(), is(HearingLanguage.ENGLISH.name()));
        assertThat(apiHearing.getJurisdictionType().name(), is(JurisdictionType.MAGISTRATES.name()));

        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getPersonDetails().getFirstName(), is("Fred"));
        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getPersonDetails().getLastName(), is("Smith"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getPersonDetails().getFirstName(), is("John"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getPersonDetails().getLastName(), is("Beard"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getAssociationStartDate().toString(), is("2019-09-12"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getAssociationEndDate().toString(), is("2019-12-12"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getFundingType().toString(), is("REPRESENTATION_ORDER"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getIsAssociatedByLAA(), is(true));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getLaaContractNumber(), is("LAA44569"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getName(), is("Test"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getIncorporationNumber(), is("cegH7rIgdX"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getRegisteredCharityNumber(), is("TestCharity"));
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getAddress(), nullValue());
        assertThat(apiHearing.getCourtApplications().get(0).getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getContact(), nullValue());

        assertThat(apiHearing.getCourtApplications().get(0).getType().getId().toString(), is("c10e3b71-6a6d-45ef-9b62-34df4d54972b"));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getCategoryCode(), is("App category code"));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getLinkType().name(), is(LinkType.STANDALONE.name()));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getJurisdiction().name(), is(JurisdictionType.MAGISTRATES.name()));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getSummonsTemplateType().name(), is(SummonsTemplateType.GENERIC_APPLICATION.name()));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getBreachType().name(), is(BreachType.GENERIC_BREACH.name()));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getAppealFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getApplicantAppellantFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getPleaApplicableFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getCommrOfOathFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getCourtOfAppealFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getCourtExtractAvlFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getProsecutorThirdPartyFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getSpiOutApplicableFlag(), is(false));
        assertThat(apiHearing.getCourtApplications().get(0).getType().getOffenceActiveOrder().name(), is(OffenceActiveOrder.OFFENCE.name()));

        assertThat(apiHearing.getCourtApplications().get(0).getApplicationReceivedDate(), is(LocalDate.of(2020,1,20)));
        assertThat(apiHearing.getCourtApplications().get(0).getApplicationStatus().toString(), is("DRAFT"));
    }

    @Test
    public void shouldTransformHearingWithVehicleCode() {
        final JsonObject hearingJson = getHearingJson("hearingWithVehicleCode.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        final ApiHearing apiHearing = hearingTransformer.hearing(hearing).build();
        final ApiDefendant apiDefendant = apiHearing.getProsecutionCases().get(0).getDefendants().get(0);

        assertThat(apiHearing.getDefendantJudicialResults(), hasSize(1));
        assertThat(apiDefendant.getJudicialResults(), hasSize(2));
        assertThat(apiDefendant.getOffences().get(0).getJudicialResults(), hasSize(2));
        assertThat(apiHearing.getCourtApplications().get(0).getJudicialResults(), hasSize(2));
        assertThat(apiHearing.getCourtCentre().getCode(), is("1234"));
        final ApiOffence apiOffence = apiDefendant.getOffences().get(0);
        assertThat(apiOffence.getOffenceFacts().getVehicleCode().toString(), is("PASSENGER_CARRYING_VEHICLE"));
        final ApiJudicialResult apiJudicialResult = apiOffence.getJudicialResults().get(0);
        assertThat(apiJudicialResult.getNextHearing().getHearingLanguage(), nullValue());
        assertThat(apiHearing.getJurisdictionType(), nullValue());
    }

    @Test
    public void shouldTransformHearingWithNSP() {
        final JsonObject hearingJson = getHearingJson("hearingWithNSP.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        final ApiHearing apiHearing = hearingTransformer.hearing(hearing).build();

        assertThat(apiHearing.getDefendantJudicialResults().size(), is(1));
        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getProsecutingAuthority().getFirstName(),is("FN"));
        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getProsecutingAuthority().getMiddleName(),is("MN"));
        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getProsecutingAuthority().getLastName(),is("LN"));
        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getProsecutingAuthority().getName(),isEmptyOrNullString());
        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getProsecutingAuthority().getProsecutorCategory(),is("PC"));
        assertThat(apiHearing.getProsecutionCases().get(0).getProsecutionCaseIdentifier().getProsecutorCategory(),is("PCIPC"));

        assertThat(apiHearing.getCourtCentre().getCode(), is("1234"));
        assertThat(apiHearing.getJurisdictionType().name(), is(JurisdictionType.MAGISTRATES.name()));
    }

    @Test
    public void shouldTransformHearingWithAllAttributes() {
        final JsonObject hearingJson = getHearingJson("hearingWithAllAttributes.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        final ApiHearing apiHearing = hearingTransformer.hearing(hearing).build();

        assertThat(apiHearing.getDefendantJudicialResults().size(), is(1));
        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getProsecutingAuthority().getFirstName(),is("FN"));
        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getProsecutingAuthority().getMiddleName(),is("MN"));
        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getProsecutingAuthority().getLastName(),is("LN"));
        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getProsecutingAuthority().getName(),isEmptyOrNullString());
        assertThat(apiHearing.getCourtApplications().get(0).getApplicant().getProsecutingAuthority().getProsecutorCategory(),is("PC"));
        assertThat(apiHearing.getCourtApplications().get(0).getLaaApplnReference().getApplicationReference(),is("LAA-SIT-001"));
        assertThat(apiHearing.getCourtApplications().get(0).getLaaApplnReference().getStatusCode(),is("FM"));
        assertThat(apiHearing.getCourtApplications().get(0).getLaaApplnReference().getStatusId(), is(UUID.fromString("4218b955-7bf8-3972-a37b-b0f921f5e5e4")));
        assertThat(apiHearing.getProsecutionCases().get(0).getProsecutionCaseIdentifier().getProsecutorCategory(),is("PCIPC"));
        assertThat(JudicialRoleTypeEnum.valueFor(apiHearing.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()),is(Optional.of(JudicialRoleTypeEnum.CIRCUIT_JUDGE)));
        assertThat(JudicialRoleTypeEnum.valueFor(apiHearing.getJudiciary().get(1).getJudicialRoleType().getJudiciaryType()),is(Optional.of(JudicialRoleTypeEnum.DISTRICT_JUDGE)));
        assertThat(JudicialRoleTypeEnum.valueFor(apiHearing.getJudiciary().get(2).getJudicialRoleType().getJudiciaryType()),is(Optional.of(JudicialRoleTypeEnum.RECORDER)));
        assertThat(JudicialRoleTypeEnum.valueFor(apiHearing.getJudiciary().get(3).getJudicialRoleType().getJudiciaryType()),is(Optional.of(JudicialRoleTypeEnum.MAGISTRATE)));
        assertThat(JudicialRoleTypeEnum.valueFor(apiHearing.getJudiciary().get(4).getJudicialRoleType().getJudiciaryType()),is(Optional.empty()));

        assertThat(apiHearing.getCourtCentre().getCode(), is("1234"));
        assertThat(apiHearing.getJurisdictionType().name(), is(JurisdictionType.MAGISTRATES.name()));
    }
    @Test
    public void shouldTransformYouthCourtDetails() {
        final JsonObject hearingJson = getHearingJson("hearing.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        final ApiHearing apiHearing = hearingTransformer.hearing(hearing).build();
    }

    private static JsonObject getHearingJson(final String resourceName) {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(resourceName),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            LOGGER.info("error {}", e.getMessage());
        }

        return new StringToJsonObjectConverter().convert(response);
    }
}
