package uk.gov.moj.cpp.results.event.helper;

import static com.google.common.collect.Lists.newArrayList;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.LocalDate.now;
import static java.time.ZoneId.of;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AssociatedIndividual;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCivilApplication;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Individual;
import uk.gov.justice.core.courts.IndividualDefendant;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.helper.results.CommonMethods;
import uk.gov.moj.cpp.results.event.service.ProgressionService;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.io.InputStream;
import java.io.StringReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;


@ExtendWith(MockitoExtension.class)
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

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private ReferenceCache referenceCache;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private ProgressionService progressionService;

    @InjectMocks
    private CasesConverter casesConverter;

    @BeforeEach
    void setUpBeforeEachTest() {
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    public static Optional<JsonObject> getCountryNationality() {
        return Optional.of(createObjectBuilder()
                .add("isoCode", COUNTRY_ISO_CODE)
                .add("id", NATIONALITY_ID.toString())
                .build());
    }

    @Test
    void testConverter2() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());
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
    void convertApplicationWithNoOffences() {
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
    void testConverter() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);
        final Hearing hearing = shareResultsMessage.getHearing();
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
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
    void testConverterWhenPoliceProsecutor() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());
        when(referenceDataService.getPoliceFlag(anyString(), anyString())).thenReturn(true);
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);
        final Hearing hearing = shareResultsMessage.getHearing();
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
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
    void testConverter_MissingProsecutionCases() {

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);
        final Hearing hearing = shareResultsMessage.getHearing();
        shareResultsMessage.setHearing(hearing().withValuesFrom(hearing).withProsecutionCases(null).build());

        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList, hasSize(0));
    }

    @Test
    void courtApplicationWithJudicialResultsAndNoCourtOrderJudicialResults() {
        final UUID hearingId = randomUUID();
        final JsonObject payload = getPayload("public.hearing-resulted-court-order-with-no-judicial-results.json", hearingId);
        final PublicHearingResulted publicHearingResulted = jsonToObjectConverter.convert(payload, PublicHearingResulted.class);

        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final Hearing hearing = publicHearingResulted.getHearing();
        final UUID caseId = randomUUID();
        when(progressionService.getProsecutionCaseDetails(any())).thenReturn(getProsecutionCase("32DN1212262", caseId));
        when(progressionService.caseExistsByCaseUrn("32DN1212262")).thenReturn(Optional.of(JsonObjects.createObjectBuilder()
                .add("caseId", caseId.toString())
                .build()));
        final List<CaseDetails> caseDetailsList = casesConverter.convert(publicHearingResulted);
        assertThat(caseDetailsList.size(), is(1));
    }

    @Test
    void courtApplicationWithJudicialResultsAndNoCourtOrderJudicialResultsHasSameCaseURNAsApplication() {
        final UUID hearingId = randomUUID();
        final JsonObject payload = getPayload("public.hearing-resulted-court-order-with-no-judicial-results-cloned-offence.json", hearingId);
        final PublicHearingResulted publicHearingResulted = jsonToObjectConverter.convert(payload, PublicHearingResulted.class);

        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final Hearing hearing = publicHearingResulted.getHearing();
        final List<CaseDetails> caseDetailsList = casesConverter.convert(publicHearingResulted);
        assertThat(caseDetailsList.size(), is(1));
        final CourtOrder courtOrder = publicHearingResulted.getHearing().getCourtApplications().get(0).getCourtOrder();
        final Optional<CourtOrderOffence> courtOrderOffence = courtOrder.getCourtOrderOffences().stream().filter(orderOffence -> orderOffence.getProsecutionCaseId().equals(caseDetailsList.get(0).getCaseId())).findFirst();
        assertThat(courtOrderOffence.isPresent(), is(false));
        assertThat(caseDetailsList.get(0).getUrn(), is(publicHearingResulted.getHearing().getCourtApplications().get(0).getApplicationReference()));
    }

    @Test
    void courtApplicationWithJustJudicialResultsAndNoCaseJudicialResults() {
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
    void courtApplicationWithJudicialResultsAndCourtOrderJudicialResultsHasSameCaseURNAsApplication() {
        final UUID hearingId = randomUUID();
        final JsonObject payload = getPayload("public.hearing-resulted-court-order.json", hearingId);
        final PublicHearingResulted publicHearingResulted = jsonToObjectConverter.convert(payload, PublicHearingResulted.class);

        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final Hearing hearing = publicHearingResulted.getHearing();
        final List<CaseDetails> caseDetailsList = casesConverter.convert(publicHearingResulted);
        assertThat(caseDetailsList.size(), is(1));
        final CourtOrder courtOrder = publicHearingResulted.getHearing().getCourtApplications().get(0).getCourtOrder();
        final Optional<CourtOrderOffence> courtOrderOffence = courtOrder.getCourtOrderOffences().stream().filter(orderOffence -> orderOffence.getProsecutionCaseId().equals(caseDetailsList.get(0).getCaseId())).findFirst();
        assertThat(courtOrderOffence.isPresent(), is(true));
        assertThat(caseDetailsList.get(0).getUrn(), is(publicHearingResulted.getHearing().getCourtApplications().get(0).getApplicationReference()));
    }

    @Test
    void testConverter_WhenProsecutionCaseIsCivilTrue() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());
        when(referenceDataService.getPoliceFlag(anyString(), anyString())).thenReturn(false);

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);
        final Hearing hearing = shareResultsMessage.getHearing();
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
        final List<ProsecutionCase> civilCases = prosecutionCases.stream()
                .map(p -> ProsecutionCase.prosecutionCase().withValuesFrom(p).withIsCivil(true).build())
                .collect(java.util.stream.Collectors.toList());
        shareResultsMessage.setHearing(uk.gov.justice.core.courts.Hearing.hearing().withValuesFrom(hearing).withProsecutionCases(civilCases).build());

        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList, hasSize(2));
        for (final CaseDetails caseDetails : caseDetailsList) {
            assertThat(caseDetails.getIsCivil(), is(true));
        }
    }

    @Test
    void courtApplicationWithCourtCivilApplicationTrue() {
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
                .withCourtCivilApplication(CourtCivilApplication.courtCivilApplication().withIsCivil(true).build())
                .withApplicationParticulars("bail application")
                .withAllegationOrComplaintStartDate(now())
                .withJudicialResults(TestTemplates.buildJudicialResultList())
                .build());
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES)
                .setHearing(basicShareHearingTemplateWithCustomApplication(hearingId, JurisdictionType.MAGISTRATES, courtApplications))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList.size(), is(1));
    }

    @Test
    void courtApplicationWithCourtCivilApplicationIsCivilNull() {
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
                .withCourtCivilApplication(CourtCivilApplication.courtCivilApplication().withIsCivil(null).build())
                .withApplicationParticulars("bail application")
                .withAllegationOrComplaintStartDate(now())
                .withJudicialResults(TestTemplates.buildJudicialResultList())
                .build());
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES)
                .setHearing(basicShareHearingTemplateWithCustomApplication(hearingId, JurisdictionType.MAGISTRATES, courtApplications))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList.size(), is(1));
    }

    @Test
    void courtOrderOffenceWithNullJudicialResultsIsSkipped() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final Offence offenceWithNullJudicialResults = Offence.offence()
                .withId(randomUUID())
                .withOffenceCode("offenceCode")
                .withOffenceTitle("title")
                .withWording("wording")
                .withStartDate(now())
                .withEndDate(now())
                .withJudicialResults(null)
                .build();
        final CourtOrderOffence courtOrderOffence = CourtOrderOffence.courtOrderOffence()
                .withOffence(offenceWithNullJudicialResults)
                .withProsecutionCaseId(caseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("AVSPF")
                        .withCaseURN("32DN1212262")
                        .build())
                .build();
        final CourtOrder courtOrder = CourtOrder.courtOrder()
                .withId(randomUUID())
                .withCourtOrderOffences(singletonList(courtOrderOffence))
                .build();

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(randomUUID())
                .withType(courtApplicationTypeTemplates())
                .withApplicationReference("APPREF")
                .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                .withApplicant(courtApplicationPartyTemplates())
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withSubject(courtApplicationPartyTemplates())
                .withCourtOrder(courtOrder)
                .build();

        final PublicHearingResulted shareResultsMessage = publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithCustomApplication(hearingId, JurisdictionType.MAGISTRATES, singletonList(courtApplication)))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList, hasSize(1));
    }

    @Test
    void courtOrderWithDefendantCaseAndProsecutingAuthority() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final UUID hearingId = randomUUID();
        final UUID defendantCaseId = randomUUID();
        final String defendantCaseReference = "URNFROMDEFCASE";

        final DefendantCase defendantCase = DefendantCase.defendantCase()
                .withCaseId(defendantCaseId)
                .withCaseReference(defendantCaseReference)
                .withDefendantId(DEFAULT_DEFENDANT_ID1)
                .build();

        final MasterDefendant masterDefendant = MasterDefendant.masterDefendant()
                .withMasterDefendantId(DEFAULT_DEFENDANT_ID1)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(uk.gov.justice.core.courts.Person.person()
                                .withFirstName("John")
                                .withLastName("Smith")
                                .withNationalityId(NATIONALITY_ID)
                                .build())
                        .withArrestSummonsNumber("ASN1234")
                        .build())
                .withDefendantCase(singletonList(defendantCase))
                .build();

        final CourtApplicationParty subject = CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withMasterDefendant(masterDefendant)
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();

        final ProsecutingAuthority prosecutingAuthority = new ProsecutingAuthority(
                null, null, null, null, null, null, null, null, "PROSAUTHCODE", randomUUID(), null, null, null, null);

        final CourtApplicationParty applicant = CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withMasterDefendant(masterDefendant)
                .withProsecutingAuthority(prosecutingAuthority)
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();

        final Offence offenceWithJudicialResults = Offence.offence()
                .withId(randomUUID())
                .withOffenceCode("offenceCode")
                .withJudicialResults(TestTemplates.buildJudicialResultList())
                .build();

        final CourtOrderOffence courtOrderOffence = CourtOrderOffence.courtOrderOffence()
                .withOffence(offenceWithJudicialResults)
                .withProsecutionCaseId(randomUUID())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("FALLBACKCODE")
                        .withCaseURN("32DN1212262")
                        .build())
                .build();

        final CourtOrder courtOrder = CourtOrder.courtOrder()
                .withId(randomUUID())
                .withCourtOrderOffences(singletonList(courtOrderOffence))
                .build();

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(randomUUID())
                .withType(courtApplicationTypeTemplates())
                .withApplicationReference("APPREF")
                .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                .withApplicant(applicant)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withSubject(subject)
                .withCourtOrder(courtOrder)
                .withCourtCivilApplication(CourtCivilApplication.courtCivilApplication().withIsCivil(true).build())
                .build();

        final PublicHearingResulted shareResultsMessage = publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithCustomApplication(hearingId, JurisdictionType.MAGISTRATES, singletonList(courtApplication)))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);

        final Optional<CaseDetails> matchedDetail = caseDetailsList.stream()
                .filter(c -> defendantCaseId.equals(c.getCaseId())).findFirst();
        assertThat(matchedDetail.isPresent(), is(true));
        assertThat(matchedDetail.get().getUrn(), is(defendantCaseReference));
        assertThat(matchedDetail.get().getProsecutionAuthorityCode(), is("PROSAUTHCODE"));
    }

    @Test
    void courtOrderWithDefendantCaseAndNullProsecutingAuthority() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final UUID hearingId = randomUUID();
        final UUID defendantCaseId = randomUUID();

        final DefendantCase defendantCase = DefendantCase.defendantCase()
                .withCaseId(defendantCaseId)
                .withCaseReference("URN_FROM_DEFCASE")
                .withDefendantId(DEFAULT_DEFENDANT_ID1)
                .build();

        final MasterDefendant masterDefendant = MasterDefendant.masterDefendant()
                .withMasterDefendantId(DEFAULT_DEFENDANT_ID1)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(uk.gov.justice.core.courts.Person.person()
                                .withFirstName("Jane")
                                .withLastName("Doe")
                                .withNationalityId(NATIONALITY_ID)
                                .build())
                        .withArrestSummonsNumber("ASN9999")
                        .build())
                .withDefendantCase(singletonList(defendantCase))
                .build();

        final CourtApplicationParty subject = CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withMasterDefendant(masterDefendant)
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();

        final CourtApplicationParty applicant = CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withMasterDefendant(masterDefendant)
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();

        final Offence offence = Offence.offence()
                .withId(randomUUID())
                .withOffenceCode("offenceCode")
                .withJudicialResults(TestTemplates.buildJudicialResultList())
                .build();

        final CourtOrderOffence courtOrderOffence = CourtOrderOffence.courtOrderOffence()
                .withOffence(offence)
                .withProsecutionCaseId(randomUUID())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("FALLBACKCODE")
                        .withCaseURN("32DN1212262")
                        .build())
                .build();

        final CourtOrder courtOrder = CourtOrder.courtOrder()
                .withId(randomUUID())
                .withCourtOrderOffences(singletonList(courtOrderOffence))
                .build();

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(randomUUID())
                .withType(courtApplicationTypeTemplates())
                .withApplicationReference("APPREF")
                .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                .withApplicant(applicant)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withSubject(subject)
                .withCourtOrder(courtOrder)
                .build();

        final PublicHearingResulted shareResultsMessage = publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithCustomApplication(hearingId, JurisdictionType.MAGISTRATES, singletonList(courtApplication)))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);

        final Optional<CaseDetails> matchedDetail = caseDetailsList.stream()
                .filter(c -> defendantCaseId.equals(c.getCaseId())).findFirst();
        assertThat(matchedDetail.isPresent(), is(true));
        assertThat(matchedDetail.get().getProsecutionAuthorityCode(), is("FALLBACKCODE"));
    }

    @Test
    void courtOrderWithEmptyDefendantCaseList() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final UUID hearingId = randomUUID();

        final MasterDefendant masterDefendant = MasterDefendant.masterDefendant()
                .withMasterDefendantId(DEFAULT_DEFENDANT_ID1)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(uk.gov.justice.core.courts.Person.person()
                                .withFirstName("Empty")
                                .withLastName("Case")
                                .withNationalityId(NATIONALITY_ID)
                                .build())
                        .withArrestSummonsNumber("ASN0001")
                        .build())
                .withDefendantCase(new java.util.ArrayList<>())
                .build();

        final CourtApplicationParty party = CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withMasterDefendant(masterDefendant)
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();

        final UUID offenceProsecutionCaseId = randomUUID();
        final Offence offence = Offence.offence()
                .withId(randomUUID())
                .withOffenceCode("offenceCode")
                .withJudicialResults(TestTemplates.buildJudicialResultList())
                .build();

        final CourtOrderOffence courtOrderOffence = CourtOrderOffence.courtOrderOffence()
                .withOffence(offence)
                .withProsecutionCaseId(offenceProsecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("FALLBACKCODE")
                        .withCaseURN("32DN1212262")
                        .build())
                .build();

        final CourtOrder courtOrder = CourtOrder.courtOrder()
                .withId(randomUUID())
                .withCourtOrderOffences(singletonList(courtOrderOffence))
                .build();

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(randomUUID())
                .withType(courtApplicationTypeTemplates())
                .withApplicationReference("APPREF")
                .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                .withApplicant(party)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withSubject(party)
                .withCourtOrder(courtOrder)
                .build();

        final PublicHearingResulted shareResultsMessage = publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithCustomApplication(hearingId, JurisdictionType.MAGISTRATES, singletonList(courtApplication)))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);

        final Optional<CaseDetails> matchedDetail = caseDetailsList.stream()
                .filter(c -> offenceProsecutionCaseId.equals(c.getCaseId())).findFirst();
        assertThat(matchedDetail.isPresent(), is(true));
    }

    @Test
    void courtOrderWithNullMasterDefendantOnSubject() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final UUID hearingId = randomUUID();

        final CourtApplicationParty subjectWithNullMasterDefendant = CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();

        final UUID offenceProsecutionCaseId = randomUUID();
        final Offence offence = Offence.offence()
                .withId(randomUUID())
                .withOffenceCode("offenceCode")
                .withJudicialResults(TestTemplates.buildJudicialResultList())
                .build();

        final CourtOrderOffence courtOrderOffence = CourtOrderOffence.courtOrderOffence()
                .withOffence(offence)
                .withProsecutionCaseId(offenceProsecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("AVSPF")
                        .withCaseURN("32DN1212262")
                        .build())
                .build();

        final CourtOrder courtOrder = CourtOrder.courtOrder()
                .withId(randomUUID())
                .withCourtOrderOffences(singletonList(courtOrderOffence))
                .build();

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(randomUUID())
                .withType(courtApplicationTypeTemplates())
                .withApplicationReference("APPREF")
                .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                .withApplicant(courtApplicationPartyTemplates())
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withSubject(subjectWithNullMasterDefendant)
                .withCourtOrder(courtOrder)
                .build();

        final PublicHearingResulted shareResultsMessage = publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithCustomApplication(hearingId, JurisdictionType.MAGISTRATES, singletonList(courtApplication)))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);

        final Optional<CaseDetails> matchedDetail = caseDetailsList.stream()
                .filter(c -> offenceProsecutionCaseId.equals(c.getCaseId())).findFirst();
        assertThat(matchedDetail.isPresent(), is(true));
        assertThat(matchedDetail.get().getProsecutionAuthorityCode(), is("AVSPF"));
    }

    @Test
    void mergeCaseDetailsAddsNewDefendantWhenSameCaseHasDifferentDefendants() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final UUID hearingId = randomUUID();
        final UUID sharedCaseId = fromString("cccc1111-1e20-4c21-916a-81a6c90239e5");

        final MasterDefendant alternateMasterDefendant = MasterDefendant.masterDefendant()
                .withMasterDefendantId(fromString("dddd9999-1e20-4c21-916a-81a6c90239e5"))
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(uk.gov.justice.core.courts.Person.person()
                                .withFirstName("Alt")
                                .withLastName("Defendant")
                                .withNationalityId(NATIONALITY_ID)
                                .build())
                        .withArrestSummonsNumber("ASN_ALT")
                        .build())
                .build();

        final CourtApplicationParty altParty = CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withMasterDefendant(alternateMasterDefendant)
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();

        final CourtApplicationCase courtApplicationCase = CourtApplicationCase.courtApplicationCase()
                .withCaseStatus("ACTIVE")
                .withIsSJP(false)
                .withProsecutionCaseId(sharedCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("CODE1")
                        .withProsecutionAuthorityReference("REF1")
                        .build())
                .withOffences(singletonList(Offence.offence()
                        .withId(randomUUID())
                        .withOffenceCode("offenceCode")
                        .withJudicialResults(TestTemplates.buildJudicialResultList())
                        .build()))
                .build();

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(fromString("f8254db1-1683-483e-afb3-b87fde5a0a26"))
                .withType(courtApplicationTypeTemplates())
                .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                .withApplicant(altParty)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withSubject(altParty)
                .withCourtApplicationCases(singletonList(courtApplicationCase))
                .withApplicationParticulars("bail application")
                .withAllegationOrComplaintStartDate(now())
                .build();

        final PublicHearingResulted shareResultsMessage = publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithCustomApplication(hearingId, JurisdictionType.MAGISTRATES, singletonList(courtApplication)))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList, hasSize(1));
        final CaseDetails mergedCase = caseDetailsList.get(0);
        assertThat(mergedCase.getCaseId(), is(sharedCaseId));
        final boolean containsAltDefendant = mergedCase.getDefendants().stream()
                .anyMatch(d -> d.getDefendantId().equals(alternateMasterDefendant.getMasterDefendantId()));
        assertThat(containsAltDefendant, is(true));
    }

    @Test
    void convertStandaloneApplicationWithNoProsecutionCaseDetailsShouldReturnEmptyCaseDetailsList() {
        when(progressionService.caseExistsByCaseUrn(anyString()))
                .thenReturn(Optional.of(JsonObjects.createObjectBuilder()
                        .add("caseId", randomUUID().toString())
                        .build()));
        when(progressionService.getProsecutionCaseDetails(any()))
                .thenReturn(JsonObjects.createObjectBuilder().build());

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(randomUUID())
                .withType(courtApplicationTypeTemplates())
                .withApplicationReference("APPREF")
                .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                .withApplicant(courtApplicationPartyTemplates())
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withSubject(courtApplicationPartyTemplates())
                .withCourtApplicationCases(null)
                .withJudicialResults(TestTemplates.buildJudicialResultList())
                .build();

        final PublicHearingResulted shareResultsMessage = publicHearingResulted()
                .setHearing(
                        hearing()
                                .withProsecutionCases(null)
                                .withCourtApplications(singletonList(courtApplication))
                                .build()
                )
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);

        assertThat(caseDetailsList, hasSize(0));
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

    private JsonObject getProsecutionCase(final String caseUrn, final UUID caseId) {
        ProsecutionCase prosecutionCase =  ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withCaseStatus("ACTIVE")
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN(caseUrn)
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityReference("CITYPF")
                        .build())
                .build();
        return JsonObjects.createObjectBuilder().add("prosecutionCase",objectToJsonObjectConverter.convert(prosecutionCase)).build();
    }

}
