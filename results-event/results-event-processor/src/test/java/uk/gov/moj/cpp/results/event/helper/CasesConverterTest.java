package uk.gov.moj.cpp.results.event.helper;

import static com.google.common.collect.Lists.newArrayList;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.LocalDate.now;
import static java.time.ZoneId.of;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createReader;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted.publicHearingResulted;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareHearingTemplateWithApplication;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareHearingTemplateWithCustomApplication;
import static uk.gov.moj.cpp.results.test.TestTemplates.courtApplicationPartyTemplates;
import static uk.gov.moj.cpp.results.test.TestTemplates.courtApplicationTypeTemplates;
import static uk.gov.moj.cpp.results.test.TestTemplates.createCourtApplicationCaseWithoutOffences;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AssociatedIndividual;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Individual;
import uk.gov.justice.core.courts.IndividualDefendant;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.helper.results.CommonMethods;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.io.InputStream;
import java.io.StringReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CasesConverterTest {

    private static final UUID DEFAULT_DEFENDANT_ID1 = fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID2 = fromString("dddd2222-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID3 = fromString("dddd3333-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID4 = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");
    private static final UUID NATIONALITY_ID = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");

    private static final String NON_POLICE_URN_DEFAULT_VALUE = "00NP0000008";
    private static final String POLICE_URN_DEFAULT_VALUE = "00PP0000008";
    private static final String NON_POLICE_ASN_DEFAULT_VALUE = "0800NP0100000000001H";


    private static final String COUNTRY_ISO_CODE = "UK";

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Mock
    private ReferenceCache referenceCache;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private CasesConverter casesConverter;

    @Before
    public void setUpBeforeEachTest() {
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    public static Optional<JsonObject> getCountryNationality() {
        return Optional.of(createObjectBuilder()
                .add("isoCode", COUNTRY_ISO_CODE)
                .add("id", NATIONALITY_ID.toString())
                .build());
    }

    @Test
    public void testConverter2()  {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());
        when(referenceDataService.getSpiOutFlag(any())).thenReturn(true);
        when(referenceDataService.getPoliceFlag(anyString(), anyString())).thenReturn(false);

        final UUID hearingId = randomUUID();
        final PublicHearingResulted shareResultsMessage = publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithApplication(hearingId, JurisdictionType.MAGISTRATES))
                .setSharedTime(ZonedDateTime.now(of("UTC")));
        final Hearing hearing = shareResultsMessage.getHearing();
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);

        assertThat(caseDetailsList.size(), is(1));

        final CaseDetails caseDetails = caseDetailsList.get(0);
        final Optional<ProsecutionCase> prosecutionCaseOptional = prosecutionCases.stream().filter(p -> p.getId().equals(caseDetails.getCaseId())).findFirst();
        assertThat(prosecutionCaseOptional.isPresent(), is(true));
        final ProsecutionCase prosecutionCase = prosecutionCaseOptional.get();
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        if (isNotEmpty(prosecutionCaseIdentifier.getCaseURN())) {
            assertThat(caseDetails.getUrn(), is(prosecutionCaseIdentifier.getCaseURN()));
        } else if (isNotEmpty(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
            assertThat(caseDetails.getUrn(), is(NON_POLICE_URN_DEFAULT_VALUE));
        } else {
            assertThat(caseDetails.getUrn(), is("00PP0000008"));
        }
        assertThat(caseDetails.getDefendants(), hasSize(2));
        Optional<CaseDefendant> caseDefendant = caseDetails.getDefendants().stream().filter(x -> x.getDefendantId().equals(hearing.getProsecutionCases().get(0).getDefendants().get(0).getMasterDefendantId())).findFirst();
        assertThat(caseDefendant.isPresent(), is(true));
        assertThat(caseDefendant.get().getOffences().size(), is(2));
        caseDefendant = caseDetails.getDefendants().stream().filter(x -> x.getDefendantId().equals(hearing.getProsecutionCases().get(0).getDefendants().get(1).getMasterDefendantId())).findFirst();
        assertThat(caseDefendant.isPresent(), is(true));
        assertThat(caseDefendant.get().getOffences().size(), is(1));
    }

    @Test
    public void convertApplicationWithNoOffences(){
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final UUID hearingId = randomUUID();
        final List<CourtApplication> courtApplications = singletonList(CourtApplication.courtApplication()
                .withId(fromString("f8254db1-1683-483e-afb3-b87fde5a0a26"))
                .withType(courtApplicationTypeTemplates())
                .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                .withApplicant(courtApplicationPartyTemplates())
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withSubject(courtApplicationPartyTemplates())
                .withCourtApplicationCases(singletonList(createCourtApplicationCaseWithoutOffences()))
                .withApplicationParticulars("bail application")
                .withAllegationOrComplaintStartDate(now())
                .build());
        final PublicHearingResulted shareResultsMessage = publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithCustomApplication(hearingId, JurisdictionType.MAGISTRATES, courtApplications))
                .setSharedTime(ZonedDateTime.now(of("UTC")));
        final Hearing hearing = shareResultsMessage.getHearing();
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
        when(referenceDataService.getSpiOutFlag(any())).thenReturn(true);
        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList.size(), is(1));
        for (final CaseDetails caseDetails : caseDetailsList) {
            final Optional<ProsecutionCase> prosecutionCaseOptional = prosecutionCases.stream().filter(p -> p.getId().equals(caseDetails.getCaseId())).findFirst();
            assertThat(prosecutionCaseOptional.isPresent(), is(true));
            final ProsecutionCase prosecutionCase = prosecutionCaseOptional.get();
            final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
            if (isNotEmpty(prosecutionCaseIdentifier.getCaseURN())) {
                assertThat(caseDetails.getUrn(), is(prosecutionCaseIdentifier.getCaseURN()));
            } else if (isNotEmpty(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
                assertThat(caseDetails.getUrn(), is(NON_POLICE_URN_DEFAULT_VALUE));
            } else {
                assertThat(caseDetails.getUrn(), is("00PP0000008"));
            }
        }
        assertThat(caseDetailsList.get(0).getDefendants(), hasSize(2));
        assertDefendants(hearing.getProsecutionCases().get(0).getDefendants(), caseDetailsList.get(0).getDefendants(), hearing);
    }

    @Test
    public void testConverter() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);
        final Hearing hearing = shareResultsMessage.getHearing();
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
        when(referenceDataService.getSpiOutFlag(any())).thenReturn(true);
        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList, hasSize(2));
        assertThat(caseDetailsList, hasSize(prosecutionCases.size()));
        for (final CaseDetails caseDetails : caseDetailsList) {
            final Optional<ProsecutionCase> prosecutionCaseOptional = prosecutionCases.stream().filter(p -> p.getId().equals(caseDetails.getCaseId())).findFirst();
            assertThat(prosecutionCaseOptional.isPresent(), is(true));
            final ProsecutionCase prosecutionCase = prosecutionCaseOptional.get();
            final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
            final boolean isUrnValid = CommonMethods.checkURNValidity(prosecutionCaseIdentifier.getCaseURN());
            if (isNotEmpty(prosecutionCaseIdentifier.getCaseURN()) &&  isUrnValid) {
                assertThat(caseDetails.getUrn(), is(prosecutionCaseIdentifier.getCaseURN()));
            } else if (isNotEmpty(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
                assertThat(caseDetails.getUrn(), is(NON_POLICE_URN_DEFAULT_VALUE));
            } else {
                assertThat(caseDetails.getUrn(), is("00NP0000008"));
            }
            final List<Defendant> defendantsFromRequest = prosecutionCase.getDefendants();
            final List<CaseDefendant> caseDetailsDefendants = caseDetails.getDefendants();
            assertThat(caseDetailsDefendants, hasSize(2));
            assertThat(caseDetailsDefendants, hasSize(defendantsFromRequest.size()));
            assertDefendants(defendantsFromRequest, caseDetailsDefendants, hearing);
        }
    }

    @Test
    public void testConverterWhenPoliceProsecutor() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());
        when(referenceDataService.getPoliceFlag(anyString(), anyString())).thenReturn(true);
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);
        final Hearing hearing = shareResultsMessage.getHearing();
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
        when(referenceDataService.getSpiOutFlag(any())).thenReturn(true);
        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList, hasSize(2));
        assertThat(caseDetailsList, hasSize(prosecutionCases.size()));
        for (final CaseDetails caseDetails : caseDetailsList) {
            final Optional<ProsecutionCase> prosecutionCaseOptional = prosecutionCases.stream().filter(p -> p.getId().equals(caseDetails.getCaseId())).findFirst();
            assertThat(prosecutionCaseOptional.isPresent(), is(true));
            final ProsecutionCase prosecutionCase = prosecutionCaseOptional.get();
            final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
            final boolean isUrnValid = CommonMethods.checkURNValidity(prosecutionCaseIdentifier.getCaseURN());
            if (isNotEmpty(prosecutionCaseIdentifier.getCaseURN()) ) {
                assertThat(caseDetails.getUrn(), is(prosecutionCaseIdentifier.getCaseURN()));
            } else if (isNotEmpty(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
                assertThat(caseDetails.getUrn(), is(POLICE_URN_DEFAULT_VALUE));
            } else {
                assertThat(caseDetails.getUrn(), is(POLICE_URN_DEFAULT_VALUE));
            }
            final List<Defendant> defendantsFromRequest = prosecutionCase.getDefendants();
            final List<CaseDefendant> caseDetailsDefendants = caseDetails.getDefendants();
            assertThat(caseDetailsDefendants, hasSize(2));
            assertThat(caseDetailsDefendants, hasSize(defendantsFromRequest.size()));

        }
    }

    @Test
    public void testConverter_MissingProsecutionCases() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);
        final Hearing hearing = shareResultsMessage.getHearing();
        shareResultsMessage.setHearing(hearing().withValuesFrom(hearing).withProsecutionCases(null).build());

        when(referenceDataService.getSpiOutFlag(any())).thenReturn(true);
        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList, hasSize(0));
    }

    @Test
    public void courtApplicationWithJudicialResultsAndNoCourtOrderJudicialResults()  {
        final UUID hearingId = randomUUID();
        final JsonObject payload = getPayload("public.hearing-resulted-court-order-with-no-judicial-results.json", hearingId);
        final PublicHearingResulted publicHearingResulted = jsonToObjectConverter.convert(payload, PublicHearingResulted.class);

        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final Hearing hearing = publicHearingResulted.getHearing();
        when(referenceDataService.getSpiOutFlag(any())).thenReturn(true);
        final List<CaseDetails> caseDetailsList = casesConverter.convert(publicHearingResulted);
        assertThat(caseDetailsList.size(), is(0));
    }

    @Test
    public void courtApplicationWithJudicialResultsAndNoCourtOrderJudicialResultsHasSameCaseURNAsApplication() {
        final UUID hearingId = randomUUID();
        final JsonObject payload = getPayload("public.hearing-resulted-court-order-with-no-judicial-results-cloned-offence.json", hearingId);
        final PublicHearingResulted publicHearingResulted = jsonToObjectConverter.convert(payload, PublicHearingResulted.class);

        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final Hearing hearing = publicHearingResulted.getHearing();
        when(referenceDataService.getSpiOutFlag(any())).thenReturn(true);
        final List<CaseDetails> caseDetailsList = casesConverter.convert(publicHearingResulted);
        assertThat(caseDetailsList.size(), is(1));
        final CourtOrder courtOrder = publicHearingResulted.getHearing().getCourtApplications().get(0).getCourtOrder();
        final Optional<CourtOrderOffence> courtOrderOffence = courtOrder.getCourtOrderOffences().stream().filter(orderOffence -> orderOffence.getProsecutionCaseId().equals(caseDetailsList.get(0).getCaseId())).findFirst();
        assertThat(courtOrderOffence.isPresent(), is(false));
        assertThat(caseDetailsList.get(0).getUrn(), is(publicHearingResulted.getHearing().getCourtApplications().get(0).getApplicationReference()));
    }

    @Test
    public void courtApplicationWithJustJudicialResultsAndNoCaseJudicialResults()  {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final UUID hearingId = randomUUID();
        final List<CourtApplication> courtApplications = singletonList(CourtApplication.courtApplication()
                .withId(fromString("f8254db1-1683-483e-afb3-b87fde5a0a26"))
                .withType(courtApplicationTypeTemplates())
                .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                .withApplicant(courtApplicationPartyTemplates())
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withSubject(courtApplicationPartyTemplates())
                .withCourtApplicationCases(singletonList(createCourtApplicationCaseWithoutOffences()))
                .withApplicationParticulars("bail application")
                .withAllegationOrComplaintStartDate(now())
                .withJudicialResults(TestTemplates.buildJudicialResultList())
                .build());
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES)
                .setHearing(basicShareHearingTemplateWithCustomApplication(hearingId, JurisdictionType.MAGISTRATES, courtApplications))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        final Hearing hearing = shareResultsMessage.getHearing();
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
        when(referenceDataService.getSpiOutFlag(any())).thenReturn(true);
        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList.size(), is(1));
        final CaseDetails caseDetails = caseDetailsList.get(0);
        final Optional<ProsecutionCase> prosecutionCaseOptional = prosecutionCases.stream().filter(p -> p.getId().equals(caseDetails.getCaseId())).findFirst();
        assertThat(prosecutionCaseOptional.isPresent(), is(true));
        final ProsecutionCase prosecutionCase = prosecutionCaseOptional.get();
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        if (isNotEmpty(prosecutionCaseIdentifier.getCaseURN())) {
            assertThat(caseDetails.getUrn(), is(prosecutionCaseIdentifier.getCaseURN()));
        } else if (isNotEmpty(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
            assertThat(caseDetails.getUrn(), is(NON_POLICE_URN_DEFAULT_VALUE));
        } else {
            assertThat(caseDetails.getUrn(), is(NON_POLICE_URN_DEFAULT_VALUE));
        }
        assertThat(caseDetails.getDefendants(), hasSize(2));
        assertDefendantsWithJudicialResultsAndNoCaseJudicialResults(caseDetailsList.get(0).getDefendants(), hearing);
    }

    @Test
    public void courtApplicationWithJudicialResultsAndCourtOrderJudicialResultsHasSameCaseURNAsApplication() {
        final UUID hearingId = randomUUID();
        final JsonObject payload = getPayload("public.hearing-resulted-court-order.json", hearingId);
        final PublicHearingResulted publicHearingResulted = jsonToObjectConverter.convert(payload, PublicHearingResulted.class);

        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final Hearing hearing = publicHearingResulted.getHearing();
        when(referenceDataService.getSpiOutFlag(any())).thenReturn(true);
        final List<CaseDetails> caseDetailsList = casesConverter.convert(publicHearingResulted);
        assertThat(caseDetailsList.size(), is(1));
        final CourtOrder courtOrder = publicHearingResulted.getHearing().getCourtApplications().get(0).getCourtOrder();
        final Optional<CourtOrderOffence> courtOrderOffence = courtOrder.getCourtOrderOffences().stream().filter(orderOffence -> orderOffence.getProsecutionCaseId().equals(caseDetailsList.get(0).getCaseId())).findFirst();
        assertThat(courtOrderOffence.isPresent(), is(true));
        assertThat(caseDetailsList.get(0).getUrn(), is(publicHearingResulted.getHearing().getCourtApplications().get(0).getApplicationReference()));
    }

    private void assertDefendantsWithJudicialResultsAndNoCaseJudicialResults(final List<CaseDefendant> caseDetailsDefendants, final Hearing hearing) {
        assertThat(caseDetailsDefendants.size(), is(2));
        Optional<CaseDefendant> caseDefendant = caseDetailsDefendants.stream().filter(x -> x.getDefendantId().equals(hearing.getProsecutionCases().get(0).getDefendants().get(0).getMasterDefendantId())).findFirst();
        assertThat(caseDefendant.isPresent(), is(true));
        assertThat(caseDefendant.get().getOffences().size(), is(2));
        caseDefendant = caseDetailsDefendants.stream().filter(x -> x.getDefendantId().equals(hearing.getProsecutionCases().get(0).getDefendants().get(1).getMasterDefendantId())).findFirst();
        assertThat(caseDefendant.isPresent(), is(true));
        assertThat(caseDefendant.get().getOffences().size(), is(1));

    }

    private void assertDefendants(final List<Defendant> defendantsFromRequest, final List<CaseDefendant> caseDetailsDefendants, final Hearing hearing) {
        for (final CaseDefendant caseDetailsDefendant : caseDetailsDefendants) {
            final Optional<Defendant> defendantOptional = defendantsFromRequest.stream().filter(d -> d.getId().equals(caseDetailsDefendant.getDefendantId())).findFirst();
            assertThat(defendantOptional.isPresent(), is(true));
            final Defendant defendantFromRequest = defendantOptional.get();
            if (isNotEmpty(defendantFromRequest.getProsecutionAuthorityReference())) {
                assertThat(caseDetailsDefendant.getProsecutorReference(), is(defendantFromRequest.getProsecutionAuthorityReference()));
            } else {
                assertThat(caseDetailsDefendant.getProsecutorReference(), is(NON_POLICE_ASN_DEFAULT_VALUE));
            }
            assertThat(caseDetailsDefendant.getPncId(), is(defendantFromRequest.getPncId()));
            assertThat(caseDetailsDefendant.getCorporateDefendant(), is(defendantFromRequest.getDefenceOrganisation()));
            if (null != defendantFromRequest.getAssociatedPersons()) {
                defendantFromRequest.getAssociatedPersons().forEach(a -> {
                    final Optional<AssociatedIndividual> associatedIndividualOptional = caseDetailsDefendant.getAssociatedPerson().stream().filter(a1 -> a1.getPerson().getLastName().equalsIgnoreCase(a.getPerson().getLastName())).findFirst();
                    assertThat(associatedIndividualOptional.isPresent(), is(true));
                    final AssociatedIndividual associatedIndividual = associatedIndividualOptional.get();
                    assertThat(associatedIndividual.getRole(), is("parentGuardian"));
                    assertPerson(associatedIndividual.getPerson(), a.getPerson());
                });
            }
            if (null != hearing.getDefendantAttendance()) {
                assertAttendanceDays(caseDetailsDefendant.getAttendanceDays(), hearing.getDefendantAttendance(), caseDetailsDefendant.getDefendantId());
            }
            assertPresentAtHearing(caseDetailsDefendant);
            assertDefendantPerson(caseDetailsDefendant.getIndividualDefendant(), defendantFromRequest.getPersonDefendant());
            assertOffences(caseDetailsDefendant.getOffences(), defendantFromRequest.getOffences());
        }
    }

    private void assertDefendants(final CourtOrderOffence courtOrderOffence, final CourtApplicationParty courtApplicationParty, final List<CaseDefendant> caseDetailsDefendants, final Hearing hearing, final boolean courtApplicationWithJudicialResults) {
        for (final CaseDefendant caseDetailsDefendant : caseDetailsDefendants) {
            final MasterDefendant masterDefendant = courtApplicationParty.getMasterDefendant();
            assertThat(caseDetailsDefendant.getProsecutorReference(), is(masterDefendant.getPersonDefendant().getArrestSummonsNumber()));
            assertThat(caseDetailsDefendant.getPncId(), is(masterDefendant.getPncId()));
            assertThat(caseDetailsDefendant.getDefendantId(), is(masterDefendant.getMasterDefendantId()));
            if (null != hearing.getDefendantAttendance()) {
                assertAttendanceDays(caseDetailsDefendant.getAttendanceDays(), hearing.getDefendantAttendance(), caseDetailsDefendant.getDefendantId());
            }
            assertPresentAtHearing(caseDetailsDefendant);
            assertDefendantPerson(caseDetailsDefendant.getIndividualDefendant(), masterDefendant.getPersonDefendant());
            final Optional<OffenceDetails> courtApplicationOffence = caseDetailsDefendant.getOffences().stream().filter(offenceDetails -> offenceDetails.getId().equals(hearing.getCourtApplications().get(0).getId())).findFirst();
            if (courtApplicationWithJudicialResults) {
                assertThat(courtApplicationOffence.isPresent(), is(true));
            } else {
                assertOffences(caseDetailsDefendant.getOffences(), newArrayList(courtOrderOffence.getOffence()));
            }
        }
    }

    private void assertPresentAtHearing(final CaseDefendant caseDetailsDefendant) {
        if (DEFAULT_DEFENDANT_ID1.equals(caseDetailsDefendant.getDefendantId()) || DEFAULT_DEFENDANT_ID4.equals(caseDetailsDefendant.getDefendantId())) {
            assertThat(caseDetailsDefendant.getIndividualDefendant().getPresentAtHearing(), is("Y"));
        }
        if (DEFAULT_DEFENDANT_ID2.equals(caseDetailsDefendant.getDefendantId())) {
            assertThat(caseDetailsDefendant.getIndividualDefendant().getPresentAtHearing(), is("N"));
        }
        if (DEFAULT_DEFENDANT_ID3.equals(caseDetailsDefendant.getDefendantId())) {
            assertThat(caseDetailsDefendant.getIndividualDefendant().getPresentAtHearing(), is("A"));
        }
    }

    private void assertOffences(final List<OffenceDetails> offences, final List<Offence> defendantFromRequestOffences) {

        for (final OffenceDetails offence : offences) {
            final Optional<Offence> offenceOptional = defendantFromRequestOffences.stream().filter(o -> o.getId().equals(offence.getId())).findFirst();
            assertThat(offenceOptional.isPresent(), is(true));
            final Offence offenceFromRequest = offenceOptional.get();
            assertThat(offence.getArrestDate(), is(offenceFromRequest.getArrestDate()));
            assertThat(offence.getChargeDate(), is(offenceFromRequest.getChargeDate()));
            assertThat(offence.getConvictingCourt(), is(nullValue()));
            assertThat(offence.getConvictionDate(), is(offenceFromRequest.getConvictionDate()));
            assertThat(offence.getEndDate(), is(offenceFromRequest.getEndDate()));
            assertThat(offence.getFinalDisposal(), is("Y"));
            assertThat(offence.getModeOfTrial(), is(offenceFromRequest.getModeOfTrial()));
            assertThat(offence.getOffenceCode(), is(offenceFromRequest.getOffenceCode()));
            assertThat(offence.getOffenceFacts(), is(offenceFromRequest.getOffenceFacts()));
            assertThat(offence.getOffenceSequenceNumber(), is(offenceFromRequest.getOrderIndex()));
            assertThat(offence.getStartDate(), is(offenceFromRequest.getStartDate()));
            assertThat(offence.getWording(), is(offenceFromRequest.getWording()));
        }
    }

    private void assertDefendantPerson(final IndividualDefendant individualDefendant, final PersonDefendant defendantFromRequest) {
        assertThat(individualDefendant.getReasonForBailConditionsOrCustody(), is(defendantFromRequest.getBailReasons()));
        assertThat(individualDefendant.getBailStatus(), is(defendantFromRequest.getBailStatus()));
        assertThat(individualDefendant.getBailConditions(), is(defendantFromRequest.getBailConditions()));
        assertPerson(individualDefendant.getPerson(), defendantFromRequest.getPersonDetails());
    }

    private void assertAttendanceDays(final List<AttendanceDay> attendanceDays, final List<DefendantAttendance> defendantAttendance, final UUID defendantId) {
        final Optional<List<AttendanceDay>> attendanceDaysFromRequest = defendantAttendance.stream().filter(a -> a.getDefendantId().equals(defendantId)).findFirst().map(a -> a.getAttendanceDays());
        assertThat(attendanceDaysFromRequest.isPresent(), is(true));
        final List<AttendanceDay> attendanceDaysListFromRequest = attendanceDaysFromRequest.get();
        assertThat(attendanceDaysListFromRequest, hasSize(1));
        assertThat(attendanceDays, hasSize(attendanceDaysListFromRequest.size()));
        final AttendanceDay attendanceDay = attendanceDays.get(0);
        final AttendanceDay attendanceDayFromRequest = attendanceDaysListFromRequest.get(0);
        assertThat(attendanceDay.getAttendanceType(), is(attendanceDayFromRequest.getAttendanceType()));
        assertThat(attendanceDay.getDay(), is(attendanceDayFromRequest.getDay()));

    }

    private void assertPerson(final Individual associatedPerson, final Person person) {
        assertThat(associatedPerson.getFirstName(), is(person.getFirstName()));
        assertThat(associatedPerson.getAddress(), is(person.getAddress()));
        assertThat(associatedPerson.getLastName(), is(person.getLastName()));
        assertThat(associatedPerson.getContact(), is(person.getContact()));
        assertThat(associatedPerson.getDateOfBirth(), is(person.getDateOfBirth()));
        assertThat(associatedPerson.getGender(), is(person.getGender()));
        assertThat(associatedPerson.getMiddleName(), is(person.getMiddleName()));
        assertThat(associatedPerson.getNationality(), is("UK"));
        assertThat(associatedPerson.getTitle(), is(person.getTitle()));
    }

    private static JsonObject getPayload(final String path, final UUID hearingId) {
        String request = null;
        try {
            final InputStream inputStream = CasesConverterTest.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, IsNull.notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset()).replace("HEARING_ID", hearingId.toString());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = createReader(new StringReader(request));
        return reader.readObject();
    }

}
