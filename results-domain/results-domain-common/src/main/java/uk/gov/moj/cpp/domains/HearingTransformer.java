package uk.gov.moj.cpp.domains;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.ApplicantCounsel;
import uk.gov.justice.core.courts.ApprovalRequest;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CompanyRepresentative;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationPartyAttendance;
import uk.gov.justice.core.courts.CourtApplicationPartyCounsel;
import uk.gov.justice.core.courts.CourtApplicationPayment;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtIndicatedSentence;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.CrackedIneffectiveTrial;
import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAlias;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingCaseNote;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.Jurors;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.LesserOrAlternativeOffence;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.NextHearingDefendant;
import uk.gov.justice.core.courts.NextHearingOffence;
import uk.gov.justice.core.courts.NextHearingProsecutionCase;
import uk.gov.justice.core.courts.NotifiedPlea;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.PoliceOfficerInCase;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCounsel;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.RespondentCounsel;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.core.courts.external.ApiAddress;
import uk.gov.justice.core.courts.external.ApiAllocationDecision;
import uk.gov.justice.core.courts.external.ApiApplicantCounsel;
import uk.gov.justice.core.courts.external.ApiApprovalRequest;
import uk.gov.justice.core.courts.external.ApiAssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.external.ApiAssociatedPerson;
import uk.gov.justice.core.courts.external.ApiAttendanceDay;
import uk.gov.justice.core.courts.external.ApiBailStatus;
import uk.gov.justice.core.courts.external.ApiCommittingCourt;
import uk.gov.justice.core.courts.external.ApiCompanyRepresentative;
import uk.gov.justice.core.courts.external.ApiContactNumber;
import uk.gov.justice.core.courts.external.ApiCourtApplication;
import uk.gov.justice.core.courts.external.ApiCourtApplicationCase;
import uk.gov.justice.core.courts.external.ApiCourtApplicationParty;
import uk.gov.justice.core.courts.external.ApiCourtApplicationPartyAttendance;
import uk.gov.justice.core.courts.external.ApiCourtApplicationPartyCounsel;
import uk.gov.justice.core.courts.external.ApiCourtApplicationPayment;
import uk.gov.justice.core.courts.external.ApiCourtApplicationType;
import uk.gov.justice.core.courts.external.ApiCourtCentre;
import uk.gov.justice.core.courts.external.ApiCourtIndicatedSentence;
import uk.gov.justice.core.courts.external.ApiCourtOrder;
import uk.gov.justice.core.courts.external.ApiCourtOrderOffence;
import uk.gov.justice.core.courts.external.ApiCrackedIneffectiveTrial;
import uk.gov.justice.core.courts.external.ApiCustodyTimeLimit;
import uk.gov.justice.core.courts.external.ApiDefenceCounsel;
import uk.gov.justice.core.courts.external.ApiDefendant;
import uk.gov.justice.core.courts.external.ApiDefendantAlias;
import uk.gov.justice.core.courts.external.ApiDefendantAttendance;
import uk.gov.justice.core.courts.external.ApiDefendantCase;
import uk.gov.justice.core.courts.external.ApiDefendantJudicialResult;
import uk.gov.justice.core.courts.external.ApiDelegatedPowers;
import uk.gov.justice.core.courts.external.ApiEthnicity;
import uk.gov.justice.core.courts.external.ApiHearing;
import uk.gov.justice.core.courts.external.ApiHearingCaseNote;
import uk.gov.justice.core.courts.external.ApiHearingDay;
import uk.gov.justice.core.courts.external.ApiHearingType;
import uk.gov.justice.core.courts.external.ApiIndicatedPlea;
import uk.gov.justice.core.courts.external.ApiJudicialResult;
import uk.gov.justice.core.courts.external.ApiJudicialResultPrompt;
import uk.gov.justice.core.courts.external.ApiJudicialRole;
import uk.gov.justice.core.courts.external.ApiJudicialRoleType;
import uk.gov.justice.core.courts.external.ApiJurors;
import uk.gov.justice.core.courts.external.ApiLaaReference;
import uk.gov.justice.core.courts.external.ApiLegalEntityDefendant;
import uk.gov.justice.core.courts.external.ApiLesserOrAlternativeOffence;
import uk.gov.justice.core.courts.external.ApiMarker;
import uk.gov.justice.core.courts.external.ApiMasterDefendant;
import uk.gov.justice.core.courts.external.ApiNextHearing;
import uk.gov.justice.core.courts.external.ApiNextHearingDefendant;
import uk.gov.justice.core.courts.external.ApiNextHearingOffence;
import uk.gov.justice.core.courts.external.ApiNextHearingProsecutionCase;
import uk.gov.justice.core.courts.external.ApiNotifiedPlea;
import uk.gov.justice.core.courts.external.ApiOffence;
import uk.gov.justice.core.courts.external.ApiOffenceFacts;
import uk.gov.justice.core.courts.external.ApiOrganisation;
import uk.gov.justice.core.courts.external.ApiPerson;
import uk.gov.justice.core.courts.external.ApiPersonDefendant;
import uk.gov.justice.core.courts.external.ApiPlea;
import uk.gov.justice.core.courts.external.ApiPoliceOfficerInCase;
import uk.gov.justice.core.courts.external.ApiProsecutingAuthority;
import uk.gov.justice.core.courts.external.ApiProsecutionCase;
import uk.gov.justice.core.courts.external.ApiProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.external.ApiProsecutionCounsel;
import uk.gov.justice.core.courts.external.ApiReferralReason;
import uk.gov.justice.core.courts.external.ApiReportingRestriction;
import uk.gov.justice.core.courts.external.ApiRespondentCounsel;
import uk.gov.justice.core.courts.external.ApiSeedingHearing;
import uk.gov.justice.core.courts.external.ApiVerdict;
import uk.gov.justice.core.courts.external.ApiVerdictType;
import uk.gov.justice.core.courts.external.ApprovalType;
import uk.gov.justice.core.courts.external.BreachType;
import uk.gov.justice.core.courts.external.Category;
import uk.gov.justice.core.courts.external.CourtHouseType;
import uk.gov.justice.core.courts.external.DocumentationLanguageNeeds;
import uk.gov.justice.core.courts.external.DriverLicenceCode;
import uk.gov.justice.core.courts.external.Gender;
import uk.gov.justice.core.courts.external.HearingLanguage;
import uk.gov.justice.core.courts.external.IndicatedPleaValue;
import uk.gov.justice.core.courts.external.InitiationCode;
import uk.gov.justice.core.courts.external.Jurisdiction;
import uk.gov.justice.core.courts.external.JurisdictionType;
import uk.gov.justice.core.courts.external.LinkType;
import uk.gov.justice.core.courts.external.NoteType;
import uk.gov.justice.core.courts.external.NotifiedPleaValue;
import uk.gov.justice.core.courts.external.OffenceActiveOrder;
import uk.gov.justice.core.courts.external.Source;
import uk.gov.justice.core.courts.external.SummonsTemplateType;
import uk.gov.justice.core.courts.external.VehicleCode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S3776", "squid:S1067"})
public class HearingTransformer {

    public ApiHearing.Builder hearing(final Hearing hearing) {
        final ApiHearing.Builder result = ApiHearing.apiHearing();

        return result
                .withCourtCentre(hearing.getCourtCentre() == null ? null : courtCentre(hearing.getCourtCentre()).build())
                .withJurisdictionType(hearing.getJurisdictionType() == null ? null : JurisdictionType.valueOf(hearing.getJurisdictionType().name()))
                .withType(hearing.getType() == null ? null : hearingType(hearing.getType()).build())
                .withCrackedIneffectiveTrial(hearing.getCrackedIneffectiveTrial() == null ? null : crackedIneffectiveTrial(hearing.getCrackedIneffectiveTrial()).build())
                .withDefendantHearingYouthMarkers(Collections.emptyList())
                .withHearingLanguage(hearing.getHearingLanguage() == null ? null : HearingLanguage.valueOf(hearing.getHearingLanguage().name()))
                .withHasSharedResults(hearing.getHasSharedResults())
                .withIsBoxHearing(hearing.getIsBoxHearing())
                .withIsEffectiveTrial(hearing.getIsEffectiveTrial())
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withApplicantCounsels(hearing.getApplicantCounsels() == null ? Collections.emptyList() :
                        hearing.getApplicantCounsels().stream().map(ac -> applicantCounsel(ac).build()).collect(Collectors.toList()))
                .withApplicationPartyCounsels(hearing.getApplicationPartyCounsels() == null ? Collections.emptyList() :
                        hearing.getApplicationPartyCounsels().stream()
                                .map(applicationPartyCounsel -> courtApplicationPartyCounsel(applicationPartyCounsel).build()).collect(Collectors.toList()))
                .withCourtApplicationPartyAttendance(hearing.getCourtApplicationPartyAttendance() == null ? Collections.emptyList() :
                        hearing.getCourtApplicationPartyAttendance().stream().map(ap -> courtApplicationPartyAttendance(ap).build()).collect(Collectors.toList()))
                .withCourtApplications(hearing.getCourtApplications() == null ? Collections.emptyList() :
                        hearing.getCourtApplications().stream().map(ca -> courtApplication(ca).build()).collect(Collectors.toList()))
                .withDefenceCounsels(hearing.getDefenceCounsels() == null ? Collections.emptyList() :
                        hearing.getDefenceCounsels().stream().map(dc -> defenceCounsel(dc).build()).collect(Collectors.toList()))
                .withDefendantAttendance(hearing.getDefendantAttendance() == null ? Collections.emptyList() :
                        hearing.getDefendantAttendance().stream().map(da -> defendantAttendance(da).build()).collect(Collectors.toList()))
                .withId(hearing.getId())
                .withDefendantReferralReasons(hearing.getDefendantReferralReasons() == null ? Collections.emptyList() :
                        hearing.getDefendantReferralReasons().stream().map(rr -> referralReason(rr).build()).collect(Collectors.toList()))
                .withJudiciary(hearing.getJudiciary() == null ? Collections.emptyList() :
                        hearing.getJudiciary().stream().map(jr -> judicialRole(jr).build()).collect(Collectors.toList()))
                .withRespondentCounsels(hearing.getRespondentCounsels() == null ? Collections.emptyList() :
                        hearing.getRespondentCounsels().stream().map(rc -> respondentCounsel(rc).build()).collect(Collectors.toList()))
                .withHearingCaseNotes(hearing.getHearingCaseNotes() == null ? Collections.emptyList() :
                        hearing.getHearingCaseNotes().stream().map(hc -> hearingCaseNote(hc).build()).collect(Collectors.toList()))
                .withHearingDays(hearing.getHearingDays() == null ? Collections.emptyList() :
                        hearing.getHearingDays().stream().map(hd -> hearingDay(hd).build()).collect(Collectors.toList()))
                .withProsecutionCases(hearing.getProsecutionCases() == null ? Collections.emptyList() :
                        hearing.getProsecutionCases().stream().map(pc -> prosecutionCase(pc).build()).collect(Collectors.toList()))
                .withDefendantJudicialResults(hearing.getDefendantJudicialResults() == null ?
                        Collections.emptyList() : filterDefendantJudicialResults(hearing.getDefendantJudicialResults()))
                .withProsecutionCounsels(hearing.getProsecutionCounsels() == null ? Collections.emptyList() :
                        hearing.getProsecutionCounsels().stream().map(pc -> prosecutionCounsel(pc).build()).collect(Collectors.toList()))
                .withCompanyRepresentatives(hearing.getCompanyRepresentatives() == null ? Collections.emptyList() :
                        hearing.getCompanyRepresentatives().stream().map(cr -> companyRepresentatives(cr).build()).collect(Collectors.toList()))
                .withIsVacatedTrial(hearing.getIsVacatedTrial())
                .withApprovalsRequested(hearing.getApprovalsRequested() == null ? Collections.emptyList() :
                        hearing.getApprovalsRequested().stream().map(ar -> approvalsRequested(ar).build()).collect(Collectors.toList()))
                .withSeedingHearing(hearing.getSeedingHearing() == null ? null : seedingHearing(hearing.getSeedingHearing()).build());
    }

    private ApiCompanyRepresentative.Builder companyRepresentatives(final CompanyRepresentative companyRepresentative) {
        return ApiCompanyRepresentative.apiCompanyRepresentative().withId(companyRepresentative.getId())
                .withFirstName(companyRepresentative.getFirstName())
                .withLastName(companyRepresentative.getLastName())
                .withPosition(companyRepresentative.getPosition())
                .withAttendanceDays(companyRepresentative.getAttendanceDays())
                .withTitle(companyRepresentative.getTitle())
                .withDefendants(companyRepresentative.getDefendants());
    }

    private ApiApprovalRequest.Builder approvalsRequested(final ApprovalRequest approvalRequest) {
        return ApiApprovalRequest.apiApprovalRequest().withUserId(approvalRequest.getUserId())
                .withHearingId(approvalRequest.getHearingId())
                .withRequestApprovalTime(approvalRequest.getRequestApprovalTime())
                .withApprovalType(approvalRequest.getApprovalType() == null ? null : ApprovalType.valueOf(approvalRequest.getApprovalType().name()));
    }

    private ApiSeedingHearing.Builder seedingHearing(final SeedingHearing seedingHearing) {
        return ApiSeedingHearing.apiSeedingHearing()
                .withSeedingHearingId(seedingHearing.getSeedingHearingId())
                .withSittingDay(seedingHearing.getSittingDay())
                .withJurisdictionType(seedingHearing.getJurisdictionType() == null ? null : JurisdictionType.valueOf(seedingHearing.getJurisdictionType().name()));
    }

    private ApiProsecutionCounsel.Builder prosecutionCounsel(ProsecutionCounsel prosecutionCounsel) {
        return ApiProsecutionCounsel.apiProsecutionCounsel().withAttendanceDays(prosecutionCounsel.getAttendanceDays())
                .withFirstName(prosecutionCounsel.getFirstName())
                .withId(prosecutionCounsel.getId())
                .withLastName(prosecutionCounsel.getLastName())
                .withMiddleName(prosecutionCounsel.getMiddleName())
                .withProsecutionCases(prosecutionCounsel.getProsecutionCases())
                .withStatus(prosecutionCounsel.getStatus())
                .withTitle(prosecutionCounsel.getTitle());
    }

    private ApiProsecutionCase.Builder prosecutionCase(final ProsecutionCase prosecutionCase) {
        final ApiProsecutionCase.Builder prosecutionCaseResult = ApiProsecutionCase.apiProsecutionCase();
        if (prosecutionCase.getPoliceOfficerInCase() != null) {
            prosecutionCaseResult.withPoliceOfficerInCase(policeOfficerInCase(prosecutionCase.getPoliceOfficerInCase()).build());
        }
        if (prosecutionCase.getProsecutionCaseIdentifier() != null) {
            prosecutionCaseResult.withProsecutionCaseIdentifier(prosecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier()).build());
        }
        if (prosecutionCase.getInitiationCode() != null) {
            prosecutionCaseResult.withInitiationCode(InitiationCode.valueOf(prosecutionCase.getInitiationCode().name()));
        }
        if (prosecutionCase.getCaseStatus() != null) {
            prosecutionCaseResult.withCaseStatus(prosecutionCase.getCaseStatus());
        }
        return prosecutionCaseResult.withAppealProceedingsPending(prosecutionCase.getAppealProceedingsPending())
                .withBreachProceedingsPending(prosecutionCase.getBreachProceedingsPending())
                .withCaseMarkers(prosecutionCase.getCaseMarkers() == null ? Collections.emptyList() :
                        prosecutionCase.getCaseMarkers().stream().map(pc -> marker(pc).build()).collect(Collectors.toList()))
                .withDefendants(prosecutionCase.getDefendants() == null ? Collections.emptyList() :
                        prosecutionCase.getDefendants().stream().map(dr -> defendant(dr).build()).collect(Collectors.toList()))
                .withId(prosecutionCase.getId())
                .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                .withSummonsCode(prosecutionCase.getSummonsCode());
    }

    private ApiProsecutionCaseIdentifier.Builder prosecutionCaseIdentifier(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return ApiProsecutionCaseIdentifier.apiProsecutionCaseIdentifier()
                .withCaseURN(prosecutionCaseIdentifier.getCaseURN())
                .withProsecutionAuthorityCode(prosecutionCaseIdentifier.getProsecutionAuthorityCode())
                .withProsecutionAuthorityId(prosecutionCaseIdentifier.getProsecutionAuthorityId())
                .withProsecutionAuthorityName(prosecutionCaseIdentifier.getProsecutionAuthorityName())
                .withProsecutorCategory(prosecutionCaseIdentifier.getProsecutorCategory())
                .withProsecutionAuthorityReference(prosecutionCaseIdentifier.getProsecutionAuthorityReference());
    }

    private ApiPoliceOfficerInCase.Builder policeOfficerInCase(PoliceOfficerInCase policeOfficerInCase) {
        return ApiPoliceOfficerInCase.apiPoliceOfficerInCase().withPersonDetails(person(policeOfficerInCase.getPersonDetails()).build())
                .withPoliceOfficerRank(policeOfficerInCase.getPoliceOfficerRank())
                .withPoliceWorkerLocationCode(policeOfficerInCase.getPoliceWorkerLocationCode())
                .withPoliceWorkerReferenceNumber(policeOfficerInCase.getPoliceWorkerReferenceNumber());
    }

    private ApiMarker.Builder marker(final Marker marker) {
        return ApiMarker.apiMarker()
                .withMarkerTypeid(marker.getMarkerTypeid())
                .withMarkerTypeDescription(marker.getMarkerTypeDescription())
                .withMarkerTypeCode(marker.getMarkerTypeCode());
    }

    private ApiHearingDay.Builder hearingDay(final HearingDay hearingDay) {
        return ApiHearingDay.apiHearingDay().withListedDurationMinutes(hearingDay.getListedDurationMinutes())
                .withListingSequence(hearingDay.getListingSequence())
                .withSittingDay(hearingDay.getSittingDay());
    }

    private ApiHearingCaseNote.Builder hearingCaseNote(final HearingCaseNote hearingCaseNote) {
        final ApiHearingCaseNote.Builder hearingCaseNoteResult = ApiHearingCaseNote.apiHearingCaseNote();
        if (hearingCaseNote.getNoteType() != null) {
            hearingCaseNoteResult.withNoteType(NoteType.valueOf(hearingCaseNote.getNoteType().name()));
        }
        return hearingCaseNoteResult.withId(hearingCaseNote.getId())
                .withNote(hearingCaseNote.getNote())
                .withNoteDateTime(hearingCaseNote.getNoteDateTime())
                .withOriginatingHearingId(hearingCaseNote.getOriginatingHearingId())
                .withCourtClerk(delegatePowers(hearingCaseNote.getCourtClerk()).build())
                .withProsecutionCases(hearingCaseNote.getProsecutionCases());
    }

    private ApiRespondentCounsel.Builder respondentCounsel(final RespondentCounsel respondentCounsel) {
        return ApiRespondentCounsel.apiRespondentCounsel().withAttendanceDays(respondentCounsel.getAttendanceDays())
                .withFirstName(respondentCounsel.getFirstName())
                .withId(respondentCounsel.getId())
                .withLastName(respondentCounsel.getLastName())
                .withMiddleName(respondentCounsel.getMiddleName())
                .withRespondents(respondentCounsel.getRespondents())
                .withStatus(respondentCounsel.getStatus())
                .withTitle(respondentCounsel.getTitle());
    }

    private ApiReferralReason.Builder referralReason(final ReferralReason referralReason) {
        return ApiReferralReason.apiReferralReason().withDefendantId(referralReason.getDefendantId())
                .withDescription(referralReason.getDescription())
                .withId(referralReason.getId());
    }

    private ApiDefendantAttendance.Builder defendantAttendance(final DefendantAttendance defendantAttendance) {
        return ApiDefendantAttendance.apiDefendantAttendance().withAttendanceDays(defendantAttendance.getAttendanceDays() == null ? Collections.emptyList() :
                        defendantAttendance.getAttendanceDays().stream().map(da -> attendanceDays(da).build()).collect(Collectors.toList()))
                .withDefendantId(defendantAttendance.getDefendantId());
    }

    private ApiDefenceCounsel.Builder defenceCounsel(DefenceCounsel defenceCounsel) {
        return ApiDefenceCounsel.apiDefenceCounsel().withAttendanceDays(defenceCounsel.getAttendanceDays())
                .withDefendants(defenceCounsel.getDefendants())
                .withFirstName(defenceCounsel.getFirstName())
                .withId(defenceCounsel.getId())
                .withLastName(defenceCounsel.getLastName())
                .withMiddleName(defenceCounsel.getMiddleName())
                .withStatus(defenceCounsel.getStatus())
                .withTitle(defenceCounsel.getTitle());
    }

    private ApiCrackedIneffectiveTrial.Builder crackedIneffectiveTrial(final CrackedIneffectiveTrial crackedIneffectiveTrial) {
        return ApiCrackedIneffectiveTrial.apiCrackedIneffectiveTrial().withCode(crackedIneffectiveTrial.getCode())
                .withDescription(crackedIneffectiveTrial.getDescription())
                .withId(crackedIneffectiveTrial.getId())
                .withType(crackedIneffectiveTrial.getType());
    }

    private ApiApplicantCounsel.Builder applicantCounsel(final ApplicantCounsel applicantCounsel) {
        return ApiApplicantCounsel.apiApplicantCounsel()
                .withFirstName(applicantCounsel.getFirstName())
                .withLastName(applicantCounsel.getLastName())
                .withId(applicantCounsel.getId())
                .withMiddleName(applicantCounsel.getMiddleName())
                .withStatus(applicantCounsel.getStatus())
                .withTitle(applicantCounsel.getTitle())
                .withApplicants(applicantCounsel.getApplicants())
                .withAttendanceDays(applicantCounsel.getAttendanceDays());
    }

    private ApiCourtApplicationPartyCounsel.Builder courtApplicationPartyCounsel(final CourtApplicationPartyCounsel courtApplicationPartyCounsel) {
        return ApiCourtApplicationPartyCounsel.apiCourtApplicationPartyCounsel()
                .withApplicationId(courtApplicationPartyCounsel.getApplicationId())
                .withApplicationRespondents(courtApplicationPartyCounsel.getApplicationRespondents())
                .withAttendanceDays(courtApplicationPartyCounsel.getAttendanceDays())
                .withFirstName(courtApplicationPartyCounsel.getFirstName())
                .withId(courtApplicationPartyCounsel.getId())
                .withLastName(courtApplicationPartyCounsel.getLastName())
                .withMiddleName(courtApplicationPartyCounsel.getMiddleName())
                .withStatus(courtApplicationPartyCounsel.getStatus())
                .withTitle(courtApplicationPartyCounsel.getTitle())
                .withStatus(courtApplicationPartyCounsel.getStatus());
    }

    private ApiCourtApplicationPartyAttendance.Builder courtApplicationPartyAttendance(final CourtApplicationPartyAttendance applicationPartyAttendance) {
        return ApiCourtApplicationPartyAttendance.apiCourtApplicationPartyAttendance()
                .withAttendanceDays(applicationPartyAttendance.getAttendanceDays() == null ? Collections.emptyList() :
                        applicationPartyAttendance.getAttendanceDays().stream().map(apa -> attendanceDays(apa).build()).collect(Collectors.toList()))
                .withCourtApplicationPartyId(applicationPartyAttendance.getCourtApplicationPartyId());

    }

    private ApiAttendanceDay.Builder attendanceDays(final AttendanceDay attendanceDay) {
        return ApiAttendanceDay.apiAttendanceDay()
                .withDay(attendanceDay.getDay())
                .withAttendanceType(attendanceDay.getAttendanceType() != null ? uk.gov.justice.core.courts.external.AttendanceType.valueOf(attendanceDay.getAttendanceType().name()) : null);
    }

    private ApiCourtCentre.Builder courtCentre(final CourtCentre courtCentre) {
        final ApiCourtCentre.Builder apiCourtCentre = ApiCourtCentre.apiCourtCentre();
        if (courtCentre.getAddress() != null) {
            apiCourtCentre.withAddress(address(courtCentre.getAddress()).build());
        }
        return apiCourtCentre
                .withCode(courtCentre.getCode())
                .withId(courtCentre.getId())
                .withName(courtCentre.getName())
                .withRoomId(courtCentre.getRoomId())
                .withRoomName(courtCentre.getRoomName())
                .withWelshName(courtCentre.getWelshName())
                .withWelshRoomName(courtCentre.getWelshRoomName());
    }

    private ApiAddress.Builder address(final Address address) {
        return ApiAddress.apiAddress().withAddress1(address.getAddress1())
                .withAddress2(address.getAddress2())
                .withAddress3(address.getAddress3())
                .withAddress4(address.getAddress4())
                .withAddress5(address.getAddress5())
                .withPostcode(address.getPostcode());
    }

    private ApiCourtApplication.Builder courtApplication(final CourtApplication courtApplication) {
        final ApiCourtApplication.Builder apiCourtApplication = ApiCourtApplication.apiCourtApplication();
        if (courtApplication.getCourtApplicationPayment() != null) {
            apiCourtApplication.withCourtApplicationPayment(courtApplicationPayment(courtApplication.getCourtApplicationPayment()).build());
        }
        if (courtApplication.getType() != null) {
            apiCourtApplication.withType(courtApplicationType(courtApplication.getType()).build());
        }
        if (courtApplication.getApplicant() != null) {
            apiCourtApplication.withApplicant(courtApplicationParty(courtApplication.getApplicant()).build());
        }
        if (courtApplication.getSubject() != null) {
            apiCourtApplication.withSubject(courtApplicationParty(courtApplication.getSubject()).build());
        }
        if (courtApplication.getApplicationStatus() != null) {
            apiCourtApplication.withApplicationStatus(courtApplication.getApplicationStatus().name());
        }
        if (courtApplication.getPlea() != null) {
            apiCourtApplication.withPlea(applicationPlea(courtApplication.getPlea()).build());
        }
        if (courtApplication.getVerdict() != null) {
            apiCourtApplication.withVerdict(verdict(courtApplication.getVerdict()).build());
        }
        return apiCourtApplication
                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                .withApplicationDecisionSoughtByDate(courtApplication.getApplicationDecisionSoughtByDate())
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationReceivedDate(courtApplication.getApplicationReceivedDate())
                .withId(courtApplication.getId())
                .withJudicialResults(courtApplication.getJudicialResults() == null ?
                        Collections.emptyList() : filterJudicialResults(courtApplication.getJudicialResults()))
                .withOutOfTimeReasons(courtApplication.getOutOfTimeReasons())
                .withParentApplicationId(courtApplication.getParentApplicationId())
                .withRespondents(courtApplication.getRespondents() == null ? Collections.emptyList() : courtApplication.getRespondents().stream().map(res -> courtApplicationParty(res).build()).collect(Collectors.toList()))
                .withConvictionDate(courtApplication.getConvictionDate())
                .withDefendantASN(courtApplication.getDefendantASN())
                .withAllegationOrComplaintEndDate(courtApplication.getAllegationOrComplaintEndDate())
                .withAllegationOrComplaintStartDate(courtApplication.getAllegationOrComplaintStartDate())
                .withCourtOrder(courtApplication.getCourtOrder() == null ? null : courtorder(courtApplication.getCourtOrder()).build())
                .withCourtApplicationCases(courtApplication.getCourtApplicationCases() == null ? Collections.emptyList() :
                        courtApplication.getCourtApplicationCases().stream().map(cac -> courtApplicationCases(cac).build()).collect(Collectors.toList()))
                .withThirdParties(courtApplication.getThirdParties() == null ? Collections.emptyList() :
                        courtApplication.getThirdParties().stream().map(tp -> courtApplicationParty(tp).build()).collect(Collectors.toList()));
    }

    private ApiCourtApplicationCase.Builder courtApplicationCases(final CourtApplicationCase courtApplicationCase) {
        return ApiCourtApplicationCase.apiCourtApplicationCase().withCaseStatus(courtApplicationCase.getCaseStatus())
                .withIsSJP(courtApplicationCase.getIsSJP())
                .withOffences(courtApplicationCase.getOffences() == null ? Collections.emptyList() : courtApplicationCase.getOffences().stream().map(offence -> courtApplicationOffence(offence).build()).collect(Collectors.toList()))
                .withProsecutionCaseId(courtApplicationCase.getProsecutionCaseId())
                .withProsecutionCaseIdentifier(courtApplicationCase.getProsecutionCaseIdentifier() == null ? null : prosecutionCaseIdentifier(courtApplicationCase.getProsecutionCaseIdentifier()).build());
    }

    private ApiOffence.Builder courtApplicationOffence(final Offence courtApplicationOffence) {
        return ApiOffence.apiOffence()
                .withValuesFrom(offence(courtApplicationOffence).build());
    }

    private ApiCourtOrder.Builder courtorder(final CourtOrder courtOrder) {
        return ApiCourtOrder.apiCourtOrder().withId(courtOrder.getId())
                .withCanBeSubjectOfBreachProceedings(courtOrder.getCanBeSubjectOfBreachProceedings())
                .withCanBeSubjectOfVariationProceedings(courtOrder.getCanBeSubjectOfVariationProceedings())
                .withCourtOrderOffences(courtOrder.getCourtOrderOffences() == null ? Collections.emptyList() : courtOrder.getCourtOrderOffences().stream().map(coo -> courtOrderOffence(coo).build()).collect(Collectors.toList()))
                .withDefendantIds(courtOrder.getDefendantIds())
                .withEndDate(courtOrder.getEndDate())
                .withIsSJPOrder(courtOrder.getIsSJPOrder())
                .withJudicialResultTypeId(courtOrder.getJudicialResultTypeId())
                .withLabel(courtOrder.getLabel())
                .withMasterDefendantId(courtOrder.getMasterDefendantId())
                .withOrderDate(courtOrder.getOrderDate())
                .withOrderingCourt(courtOrder.getOrderingCourt() == null ? null : courtCentre(courtOrder.getOrderingCourt()).build())
                .withOrderingHearingId(courtOrder.getOrderingHearingId())
                .withStartDate(courtOrder.getStartDate());
    }

    private ApiCourtOrderOffence.Builder courtOrderOffence(final CourtOrderOffence courtOrderOffence) {
        return ApiCourtOrderOffence.apiCourtOrderOffence().withOffence(courtOrderOffence.getOffence() == null ? null : offence(courtOrderOffence.getOffence()).build())
                .withProsecutionCaseId(courtOrderOffence.getProsecutionCaseId())
                .withProsecutionCaseIdentifier(courtOrderOffence.getProsecutionCaseIdentifier() == null ? null : prosecutionCaseIdentifier(courtOrderOffence.getProsecutionCaseIdentifier()).build());
    }

    private ApiCourtApplicationType.Builder courtApplicationType(final CourtApplicationType courtApplicationType) {
        final ApiCourtApplicationType.Builder apiCourtApplicationType = ApiCourtApplicationType.apiCourtApplicationType();
        if (courtApplicationType.getJurisdiction() != null) {
            apiCourtApplicationType.withJurisdiction(Jurisdiction.valueOf(courtApplicationType.getJurisdiction().name()));
        }
        if (courtApplicationType.getLinkType() != null) {
            apiCourtApplicationType.withLinkType(LinkType.valueOf(courtApplicationType.getLinkType().name()));
        }
        if (courtApplicationType.getSummonsTemplateType() != null) {
            apiCourtApplicationType.withSummonsTemplateType(SummonsTemplateType.valueOf(courtApplicationType.getSummonsTemplateType().name()));
        }
        if (courtApplicationType.getBreachType() != null) {
            apiCourtApplicationType.withBreachType(BreachType.valueOf(courtApplicationType.getBreachType().name()));
        }
        if (courtApplicationType.getOffenceActiveOrder() != null) {
            apiCourtApplicationType.withOffenceActiveOrder(OffenceActiveOrder.valueOf(courtApplicationType.getOffenceActiveOrder().name()));
        }
        return apiCourtApplicationType
                .withCategoryCode(courtApplicationType.getCategoryCode())
                .withCode(courtApplicationType.getCode())
                .withLegislation(courtApplicationType.getLegislation())
                .withLegislationWelsh(courtApplicationType.getLegislationWelsh())
                .withId(courtApplicationType.getId())
                .withAppealFlag(courtApplicationType.getAppealFlag())
                .withCourtOfAppealFlag(courtApplicationType.getCourtOfAppealFlag())
                .withType(courtApplicationType.getType())
                .withTypeWelsh(courtApplicationType.getTypeWelsh())
                .withAppealFlag(courtApplicationType.getAppealFlag())
                .withApplicantAppellantFlag(courtApplicationType.getApplicantAppellantFlag())
                .withPleaApplicableFlag(courtApplicationType.getPleaApplicableFlag())
                .withCommrOfOathFlag(courtApplicationType.getCommrOfOathFlag())
                .withCourtOfAppealFlag(courtApplicationType.getCourtOfAppealFlag())
                .withCourtExtractAvlFlag(courtApplicationType.getCourtExtractAvlFlag())
                .withProsecutorThirdPartyFlag(courtApplicationType.getProsecutorThirdPartyFlag())
                .withSpiOutApplicableFlag(courtApplicationType.getSpiOutApplicableFlag());
    }

    private ApiCourtApplicationPayment.Builder courtApplicationPayment(final CourtApplicationPayment courtApplicationPayment) {
        return ApiCourtApplicationPayment.apiCourtApplicationPayment().withIsFeeExempt(courtApplicationPayment.getIsFeeExempt())
                .withIsFeePaid(courtApplicationPayment.getIsFeePaid())
                .withIsFeeUndertakingAttached(courtApplicationPayment.getIsFeeUndertakingAttached())
                .withPaymentReference(courtApplicationPayment.getPaymentReference());
    }

    private ApiCourtApplicationParty.Builder courtApplicationParty(final CourtApplicationParty courtApplicationParty) {
        final ApiCourtApplicationParty.Builder apiCourtApplicationParty = ApiCourtApplicationParty.apiCourtApplicationParty();
        if (courtApplicationParty.getMasterDefendant() != null) {
            apiCourtApplicationParty.withMasterDefendant(masterDefendant(courtApplicationParty.getMasterDefendant()).build());
        }
        if (courtApplicationParty.getOrganisation() != null) {
            apiCourtApplicationParty.withOrganisation(organisation(courtApplicationParty.getOrganisation()).build());
        }
        if (courtApplicationParty.getPersonDetails() != null) {
            apiCourtApplicationParty.withPersonDetails(person(courtApplicationParty.getPersonDetails()).build());
        }
        if (courtApplicationParty.getProsecutingAuthority() != null) {
            apiCourtApplicationParty.withProsecutingAuthority(prosecutingAuthority(courtApplicationParty.getProsecutingAuthority()).build());
        }
        if (courtApplicationParty.getRepresentationOrganisation() != null) {
            apiCourtApplicationParty.withRepresentationOrganisation(organisation(courtApplicationParty.getOrganisation()).build());
        }
        return apiCourtApplicationParty.withId(courtApplicationParty.getId())
                .withOrganisationPersons(courtApplicationParty.getOrganisationPersons() == null ? Collections.emptyList() :
                        courtApplicationParty.getOrganisationPersons().stream().map(op -> associatedPerson(op).build()).collect(Collectors.toList()))
                .withSynonym(courtApplicationParty.getSynonym());
    }

    private ApiProsecutingAuthority.Builder prosecutingAuthority(final ProsecutingAuthority prosecutingAuthority) {
        final ApiProsecutingAuthority.Builder apiProsecutingAuthority = ApiProsecutingAuthority.apiProsecutingAuthority();
        if (prosecutingAuthority.getAddress() != null) {
            apiProsecutingAuthority.withAddress(address(prosecutingAuthority.getAddress()).build());
        }
        if (prosecutingAuthority.getContact() != null) {
            apiProsecutingAuthority.withContact(contactNumber(prosecutingAuthority.getContact()).build());
        }
        return apiProsecutingAuthority.withAccountCode(prosecutingAuthority.getAccountCode())
                .withName(prosecutingAuthority.getName())
                .withFirstName(prosecutingAuthority.getFirstName())
                .withMiddleName(prosecutingAuthority.getMiddleName())
                .withLastName(prosecutingAuthority.getLastName())
                .withProsecutorCategory(prosecutingAuthority.getProsecutorCategory())
                .withProsecutionAuthorityCode(prosecutingAuthority.getProsecutionAuthorityCode())
                .withProsecutionAuthorityId(prosecutingAuthority.getProsecutionAuthorityId());
    }

    private ApiDefendant.Builder defendant(final Defendant defendant) {
        final ApiDefendant.Builder apiDefendant = ApiDefendant.apiDefendant();
        if (defendant.getLegalEntityDefendant() != null) {
            apiDefendant.withLegalEntityDefendant(legalEntityDefendant(defendant.getLegalEntityDefendant()).build());
        }
        if (defendant.getPersonDefendant() != null) {
            apiDefendant.withPersonDefendant(personDefendant(defendant.getPersonDefendant()).build());
        }
        if (defendant.getAssociatedDefenceOrganisation() != null) {
            apiDefendant.withAssociatedDefenceOrganisation(
                    associatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation()).build()
            );
        }
        return apiDefendant.withAliases(defendant.getAliases() == null ? Collections.emptyList() :
                        defendant.getAliases().stream().map(da -> defendantAlias(da).build()).collect(Collectors.toList()))
                .withCroNumber(defendant.getCroNumber())
                .withAssociatedPersons((defendant.getAssociatedPersons() == null ? Collections.emptyList() :
                        defendant.getAssociatedPersons().stream().map(ap -> associatedPerson(ap).build()).collect(Collectors.toList())))
                .withId(defendant.getId())
                .withJudicialResults(defendant.getDefendantCaseJudicialResults() == null ?
                        Collections.emptyList() : filterJudicialResults(defendant.getDefendantCaseJudicialResults()))
                .withMitigation(defendant.getMitigation())
                .withMitigationWelsh(defendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withOffences(defendant.getOffences() == null ? Collections.emptyList() : defendant.getOffences().stream().map(o -> offence(o).build()).collect(Collectors.toList()))
                .withPncId(defendant.getPncId())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                .withWitnessStatement(defendant.getWitnessStatement())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withLegalAidStatus(defendant.getLegalAidStatus())
                .withIsYouth(defendant.getIsYouth())
                .withProceedingsConcluded(defendant.getProceedingsConcluded());
    }

    private ApiMasterDefendant.Builder masterDefendant(final MasterDefendant defendant) {
        final ApiMasterDefendant.Builder apiMasterDefendant = ApiMasterDefendant.apiMasterDefendant();
        if (defendant.getLegalEntityDefendant() != null) {
            apiMasterDefendant.withLegalEntityDefendant(legalEntityDefendant(defendant.getLegalEntityDefendant()).build());
        }
        if (defendant.getPersonDefendant() != null) {
            apiMasterDefendant.withPersonDefendant(personDefendant(defendant.getPersonDefendant()).build());
        }
        if (defendant.getDefendantCase() != null) {
            apiMasterDefendant.withDefendantCase(
                    defendant.getDefendantCase().stream().map(defendantCase ->
                            ApiDefendantCase.apiDefendantCase()
                                    .withCaseId(defendantCase.getCaseId())
                                    .withDefendantId(defendantCase.getDefendantId())
                                    .withCaseReference(defendantCase.getCaseReference()).build()
                    ).collect(Collectors.toList()));
        }
        return apiMasterDefendant.withMasterDefendantId(defendant.getMasterDefendantId());
    }

    private ApiAssociatedDefenceOrganisation.Builder associatedDefenceOrganisation(
            final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
        final ApiAssociatedDefenceOrganisation.Builder apiAssociatedDefenceOrganisation =
                ApiAssociatedDefenceOrganisation.apiAssociatedDefenceOrganisation();
        if (associatedDefenceOrganisation.getDefenceOrganisation() != null) {
            apiAssociatedDefenceOrganisation.withOrganisation(
                    organisation(associatedDefenceOrganisation.getDefenceOrganisation().getOrganisation()).build());
        }
        if (associatedDefenceOrganisation.getFundingType() != null) {
            final String fundingTypeString = associatedDefenceOrganisation.getFundingType().toString();
            final Optional<uk.gov.justice.core.courts.external.FundingType> fundingTypeOptional
                    = uk.gov.justice.core.courts.external.FundingType.valueFor(fundingTypeString);
            final uk.gov.justice.core.courts.external.FundingType fundingType =
                    fundingTypeOptional.orElse(null);
            apiAssociatedDefenceOrganisation.withFundingType(fundingType);
        }
        return apiAssociatedDefenceOrganisation
                .withApplicationReference(associatedDefenceOrganisation.getApplicationReference())
                .withIsAssociatedByLAA(associatedDefenceOrganisation.getIsAssociatedByLAA())
                .withAssociationEndDate(associatedDefenceOrganisation.getAssociationEndDate())
                .withAssociationStartDate(associatedDefenceOrganisation.getAssociationStartDate());
    }

    private ApiPersonDefendant.Builder personDefendant(final PersonDefendant personDefendant) {
        final ApiPersonDefendant.Builder apiPersonDefendant = ApiPersonDefendant.apiPersonDefendant();
        if (personDefendant.getBailStatus() != null) {
            apiPersonDefendant.withBailStatus(bailStatus(personDefendant.getBailStatus()).build());
        }
        if (personDefendant.getEmployerOrganisation() != null) {
            apiPersonDefendant.withEmployerOrganisation(organisation(personDefendant.getEmployerOrganisation()).build());
        }
        if (personDefendant.getDriverLicenceCode() != null) {
            apiPersonDefendant.withDriverLicenceCode(DriverLicenceCode.valueOf(personDefendant.getDriverLicenceCode().name()));
        }
        if (personDefendant.getPersonDetails() != null) {
            apiPersonDefendant.withPersonDetails(person(personDefendant.getPersonDetails()).build());
        }
        return apiPersonDefendant.withArrestSummonsNumber(personDefendant.getArrestSummonsNumber())
                .withBailConditions(personDefendant.getBailConditions())
                .withCustodyTimeLimit(personDefendant.getCustodyTimeLimit())
                .withDriverLicenseIssue(personDefendant.getDriverLicenseIssue())
                .withDriverNumber(personDefendant.getDriverNumber())
                .withEmployerPayrollReference(personDefendant.getEmployerPayrollReference())
                .withPerceivedBirthYear(personDefendant.getPerceivedBirthYear())
                .withVehicleOperatorLicenceNumber(personDefendant.getVehicleOperatorLicenceNumber());
    }

    private ApiBailStatus.Builder bailStatus(final BailStatus bailStatus) {
        final ApiBailStatus.Builder apiBailStatus = ApiBailStatus.apiBailStatus();
        if (bailStatus.getCustodyTimeLimit() != null) {
            apiBailStatus.withCustodyTimeLimit(custodyTimeLimit(bailStatus.getCustodyTimeLimit()).build());
        }
        return apiBailStatus.withCode(bailStatus.getCode())
                .withDescription(bailStatus.getDescription())
                .withId(bailStatus.getId());
    }

    private ApiOffence.Builder offence(final Offence offence) {
        final ApiOffence.Builder apiOffence = ApiOffence.apiOffence();
        if (offence.getCustodyTimeLimit() != null) {
            apiOffence.withCustodyTimeLimit(custodyTimeLimit(offence.getCustodyTimeLimit()).build());
        }
        if (offence.getIndicatedPlea() != null) {
            apiOffence.withIndicatedPlea(indicatedPlea(offence.getIndicatedPlea()).build());
        }
        if (offence.getNotifiedPlea() != null) {
            apiOffence.withNotifiedPlea(notifiedPlea(offence.getNotifiedPlea()).build());
        }
        if (offence.getOffenceFacts() != null) {
            apiOffence.withOffenceFacts(offenceFacts(offence.getOffenceFacts()).build());
        }
        if (offence.getAllocationDecision() != null) {
            apiOffence.withAllocationDecision(allocationDecision(offence.getAllocationDecision()).build());
        }
        if (offence.getLaaApplnReference() != null) {
            apiOffence.withLaaApplnReference(laaReference(offence.getLaaApplnReference()).build());
        }
        if (offence.getVerdict() != null) {
            apiOffence.withVerdict(verdict(offence.getVerdict()).build());
        }
        if (offence.getPlea() != null) {
            apiOffence.withPlea(applicationPlea(offence.getPlea()).build());
        }

        if(offence.getListingNumber() != null){
            apiOffence.withListingNumber(offence.getListingNumber());
        }

        return apiOffence
                .withAquittalDate(offence.getAquittalDate())
                .withArrestDate(offence.getArrestDate())
                .withChargeDate(offence.getChargeDate())
                .withConvictionDate(offence.getConvictionDate())
                .withCount(offence.getCount())
                .withDateOfInformation(offence.getDateOfInformation())
                .withEndDate(offence.getEndDate())
                .withId(offence.getId())
                .withJudicialResults(offence.getJudicialResults() == null ?
                        Collections.emptyList() : filterJudicialResults(offence.getJudicialResults()))
                .withModeOfTrial(offence.getModeOfTrial())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceDefinitionId(offence.getOffenceDefinitionId())
                .withWordingWelsh(offence.getWordingWelsh())
                .withWording(offence.getWording())
                .withStartDate(offence.getStartDate())
                .withProceedingsConcluded(offence.getProceedingsConcluded())
                .withOrderIndex(offence.getOrderIndex())
                .withOffenceTitleWelsh(offence.getOffenceTitleWelsh())
                .withOffenceTitle(offence.getOffenceTitle())
                .withOffenceLegislationWelsh(offence.getOffenceLegislationWelsh())
                .withOffenceLegislation(offence.getOffenceLegislation())
                .withOffenceDateCode(offence.getOffenceDateCode())
                .withLaidDate(offence.getLaidDate())
                .withIsIntroduceAfterInitialProceedings(offence.getIntroducedAfterInitialProceedings())
                .withDvlaOffenceCode(offence.getDvlaOffenceCode())
                .withVictims(offence.getVictims() == null ? Collections.emptyList() : offence.getVictims().stream().map(o -> person(o).build()).collect(Collectors.toList()))
                .withReportingRestrictions(offence.getReportingRestrictions() == null ? Collections.emptyList() : offence.getReportingRestrictions().stream().map(rr -> reportingRestrictions(rr).build()).collect(Collectors.toList()))
                .withCommittingCourt(offence.getCommittingCourt() == null ? null : committingCourt(offence.getCommittingCourt()).build());
    }

    private ApiCommittingCourt.Builder committingCourt(final CommittingCourt committingCourt) {
        return ApiCommittingCourt.apiCommittingCourt().withCourtCentreId(committingCourt.getCourtCentreId())
                .withCourtHouseCode(committingCourt.getCourtHouseCode())
                .withCourtHouseName(committingCourt.getCourtHouseName())
                .withCourtHouseShortName(committingCourt.getCourtHouseShortName())
                .withCourtHouseType(CourtHouseType.valueOf(committingCourt.getCourtHouseType().name()));
    }

    private ApiReportingRestriction.Builder reportingRestrictions(final ReportingRestriction reportingRestriction) {
        return ApiReportingRestriction.apiReportingRestriction().withId(reportingRestriction.getId())
                .withJudicialResultId(reportingRestriction.getJudicialResultId())
                .withLabel(reportingRestriction.getLabel())
                .withOrderedDate(reportingRestriction.getOrderedDate());
    }

    private ApiNotifiedPlea.Builder notifiedPlea(final NotifiedPlea notifiedPlea) {
        return ApiNotifiedPlea.apiNotifiedPlea().withNotifiedPleaDate(notifiedPlea.getNotifiedPleaDate())
                .withNotifiedPleaValue(NotifiedPleaValue.valueOf(notifiedPlea.getNotifiedPleaValue().name()))
                .withOffenceId(notifiedPlea.getOffenceId());
    }

    private ApiPlea.Builder applicationPlea(final Plea plea) {
        return ApiPlea.apiPlea().withApplicationId(plea.getApplicationId() == null ? null : plea.getApplicationId())
                .withOriginatingHearingId(plea.getOriginatingHearingId())
                .withPleaDate(plea.getPleaDate())
                .withPleaValue(plea.getPleaValue())
                .withDelegatedPowers(plea.getDelegatedPowers() == null ? null : delegatePowers(plea.getDelegatedPowers()).build())
                .withLesserOrAlternativeOffence(plea.getLesserOrAlternativeOffence() == null ? null : lesserOrAlternativeOffence(plea.getLesserOrAlternativeOffence()).build())
                .withOffenceId(plea.getOffenceId());
    }

    private ApiLesserOrAlternativeOffence.Builder lesserOrAlternativeOffence(final LesserOrAlternativeOffence lesserOrAlternativeOffence) {
        final ApiLesserOrAlternativeOffence.Builder apiLesserOrAlternativeOffenceBuilder = ApiLesserOrAlternativeOffence.apiLesserOrAlternativeOffence();
        if (lesserOrAlternativeOffence != null) {
            apiLesserOrAlternativeOffenceBuilder.withOffenceCode(lesserOrAlternativeOffence.getOffenceCode())
                    .withOffenceDefinitionId(lesserOrAlternativeOffence.getOffenceDefinitionId())
                    .withOffenceLegislation(lesserOrAlternativeOffence.getOffenceLegislation())
                    .withOffenceLegislationWelsh(lesserOrAlternativeOffence.getOffenceLegislationWelsh())
                    .withOffenceTitle(lesserOrAlternativeOffence.getOffenceTitle())
                    .withOffenceTitleWelsh(lesserOrAlternativeOffence.getOffenceTitleWelsh());
        }
        return apiLesserOrAlternativeOffenceBuilder;
    }

    private ApiVerdict.Builder verdict(final Verdict verdict) {
        final ApiVerdict.Builder apiVerdictBuilder = ApiVerdict.apiVerdict()
                .withApplicationId(verdict.getApplicationId())
                .withVerdictDate(verdict.getVerdictDate())
                .withVerdictType(verdictType(verdict.getVerdictType()).build())
                .withOriginatingHearingId(verdict.getOriginatingHearingId())
                .withLesserOrAlternativeOffence(lesserOrAlternativeOffence(verdict.getLesserOrAlternativeOffence()).build());
        if (verdict.getJurors() != null) {
            apiVerdictBuilder.withJurors(jurors(verdict.getJurors()).build());
        }
        return apiVerdictBuilder;
    }

    private ApiJurors.Builder jurors(final Jurors applicationJurors) {
        return ApiJurors.apiJurors()
                .withNumberOfJurors(applicationJurors.getNumberOfJurors())
                .withUnanimous(applicationJurors.getUnanimous())
                .withNumberOfSplitJurors(applicationJurors.getNumberOfSplitJurors());
    }

    private ApiVerdictType.Builder verdictType(final VerdictType applicationVerdict) {
        return ApiVerdictType.apiVerdictType()
                .withId(applicationVerdict.getId())
                .withCategory(applicationVerdict.getCategory())
                .withCategoryType(applicationVerdict.getCategoryType())
                .withDescription(applicationVerdict.getDescription())
                .withSequence(applicationVerdict.getSequence())
                .withVerdictCode(applicationVerdict.getVerdictCode())
                .withCjsVerdictCode(applicationVerdict.getCjsVerdictCode());
    }

    private ApiCustodyTimeLimit.Builder custodyTimeLimit(final CustodyTimeLimit custodyTimeLimit) {
        return ApiCustodyTimeLimit.apiCustodyTimeLimit().withDaysSpent(custodyTimeLimit.getDaysSpent())
                .withTimeLimit(custodyTimeLimit.getTimeLimit());
    }

    private ApiAllocationDecision.Builder allocationDecision(final AllocationDecision allocationDecision) {
        final ApiAllocationDecision.Builder apiAllocationDecision = ApiAllocationDecision.apiAllocationDecision();
        if (allocationDecision.getCourtIndicatedSentence() != null) {
            apiAllocationDecision.withCourtIndicatedSentence(courtIndicatedSentence(allocationDecision.getCourtIndicatedSentence()).build());
        }
        return apiAllocationDecision.withAllocationDecisionDate(allocationDecision.getAllocationDecisionDate())
                .withMotReasonCode(allocationDecision.getMotReasonCode())
                .withMotReasonDescription(allocationDecision.getMotReasonDescription())
                .withMotReasonId(allocationDecision.getMotReasonId())
                .withOffenceId(allocationDecision.getOffenceId())
                .withSequenceNumber(allocationDecision.getSequenceNumber())
                .withOriginatingHearingId(allocationDecision.getOriginatingHearingId());
    }

    private ApiCourtIndicatedSentence.Builder courtIndicatedSentence(final CourtIndicatedSentence courtIndicatedSentence) {
        return ApiCourtIndicatedSentence.apiCourtIndicatedSentence().withCourtIndicatedSentenceDescription(courtIndicatedSentence.getCourtIndicatedSentenceDescription())
                .withCourtIndicatedSentenceTypeId(courtIndicatedSentence.getCourtIndicatedSentenceTypeId());
    }

    private ApiIndicatedPlea.Builder indicatedPlea(final IndicatedPlea indicatedPlea) {
        return ApiIndicatedPlea.apiIndicatedPlea().withIndicatedPleaDate(indicatedPlea.getIndicatedPleaDate())
                .withIndicatedPleaValue(IndicatedPleaValue.valueOf(indicatedPlea.getIndicatedPleaValue().name()))
                .withOffenceId(indicatedPlea.getOffenceId())
                .withOriginatingHearingId(indicatedPlea.getOriginatingHearingId())
                .withSource(Source.valueOf(indicatedPlea.getSource().name()));
    }

    private ApiLaaReference.Builder laaReference(final LaaReference laaReference) {
        return ApiLaaReference.apiLaaReference().withApplicationReference(laaReference.getApplicationReference())
                .withEffectiveEndDate(laaReference.getEffectiveEndDate())
                .withEffectiveStartDate(laaReference.getEffectiveStartDate())
                .withStatusCode(laaReference.getStatusCode())
                .withStatusDate(laaReference.getStatusDate())
                .withStatusDescription(laaReference.getStatusDescription())
                .withStatusId(laaReference.getStatusId());
    }

    private ApiOffenceFacts.Builder offenceFacts(final OffenceFacts offenceFacts) {
        final ApiOffenceFacts.Builder builder = ApiOffenceFacts.apiOffenceFacts().withAlcoholReadingAmount(offenceFacts.getAlcoholReadingAmount())
                .withAlcoholReadingMethodCode(offenceFacts.getAlcoholReadingMethodCode())
                .withAlcoholReadingMethodDescription(offenceFacts.getAlcoholReadingMethodDescription())
                .withVehicleRegistration(offenceFacts.getVehicleRegistration());
        if (nonNull(offenceFacts.getVehicleCode())) {
            builder.withVehicleCode(VehicleCode.valueOf(offenceFacts.getVehicleCode().name()));
        }
        return builder;
    }

    private ApiLegalEntityDefendant.Builder legalEntityDefendant(final LegalEntityDefendant legalEntityDefendant) {
        return ApiLegalEntityDefendant.apiLegalEntityDefendant().withOrganisation(organisation(legalEntityDefendant.getOrganisation()).build());
    }

    private ApiOrganisation.Builder organisation(final Organisation organisation) {
        final ApiOrganisation.Builder apiOrganisation = ApiOrganisation.apiOrganisation();
        if (organisation.getContact() != null) {
            apiOrganisation.withContact(contactNumber(organisation.getContact()).build());
        }
        return apiOrganisation.withAddress(address(organisation.getAddress()).build())
                .withIncorporationNumber(organisation.getIncorporationNumber())
                .withName(organisation.getName())
                .withRegisteredCharityNumber(organisation.getRegisteredCharityNumber());
    }

    private ApiJudicialResult.Builder judicialResult(final JudicialResult judicialResult) {
        final ApiJudicialResult.Builder apiJudicialResult = ApiJudicialResult.apiJudicialResult();
        if (judicialResult.getDelegatedPowers() != null) {
            apiJudicialResult.withDelegatedPowers(delegatePowers(judicialResult.getDelegatedPowers()).build());
            apiJudicialResult.withCourtClerk(delegatePowers(judicialResult.getDelegatedPowers()).build());
            apiJudicialResult.withFourEyesApproval(delegatePowers(judicialResult.getDelegatedPowers()).build());
        }
        return apiJudicialResult.withAmendmentDate(judicialResult.getAmendmentDate())
                .withAmendmentReason(judicialResult.getAmendmentReason())
                .withAmendmentReasonId(judicialResult.getAmendmentReasonId())
                .withApprovedDate(judicialResult.getApprovedDate())
                .withCategory(Category.valueOf(judicialResult.getCategory().name()))
                .withCjsCode(judicialResult.getCjsCode())
                .withIsAdjournmentResult(judicialResult.getIsAdjournmentResult())
                .withIsAvailableForCourtExtract(judicialResult.getIsAvailableForCourtExtract())
                .withIsConvictedResult(judicialResult.getIsConvictedResult())
                .withIsDeleted(judicialResult.getIsDeleted())
                .withJudicialResultId(judicialResult.getJudicialResultId())
                .withJudicialResultPrompts(judicialResult.getJudicialResultPrompts() == null ? Collections.emptyList() :
                        judicialResult.getJudicialResultPrompts().stream().map(jr -> judicialResultPrompt(jr).build()).collect(Collectors.toList()))
                .withLabel(judicialResult.getLabel())
                .withLastSharedDateTime(judicialResult.getLastSharedDateTime())
                .withNextHearing(judicialResult.getNextHearing() == null ? null : nextHearing(judicialResult.getNextHearing()).build())
                .withOrderedDate(judicialResult.getOrderedDate())
                .withOrderedHearingId(judicialResult.getOrderedHearingId())
                .withQualifier(judicialResult.getQualifier())
                .withRank(judicialResult.getRank())
                .withUsergroups(judicialResult.getUsergroups())
                .withWelshLabel(judicialResult.getWelshLabel())
                .withIsFinancialResult(judicialResult.getIsFinancialResult())
                .withResultText(judicialResult.getResultText())
                .withTerminatesOffenceProceedings(judicialResult.getTerminatesOffenceProceedings())
                .withLifeDuration(judicialResult.getLifeDuration())
                .withPublishedAsAPrompt(judicialResult.getPublishedAsAPrompt())
                .withExcludedFromResults(judicialResult.getExcludedFromResults())
                .withAlwaysPublished(judicialResult.getAlwaysPublished())
                .withUrgent(judicialResult.getUrgent())
                .withD20(judicialResult.getD20());
    }

    private ApiNextHearing.Builder nextHearing(final NextHearing nextHearing) {
        final ApiNextHearing.Builder builder = ApiNextHearing.apiNextHearing();
        builder.withCourtCentre(courtCentre(nextHearing.getCourtCentre()).build())
                .withEstimatedMinutes(nextHearing.getEstimatedMinutes())
                .withJudiciary(nextHearing.getJudiciary() == null ? Collections.emptyList() :
                        nextHearing.getJudiciary().stream().map(jr -> judicialRole(jr).build()).collect(Collectors.toList()))
                .withListedStartDateTime(nextHearing.getListedStartDateTime())
                .withNextHearingCourtApplicationId(nextHearing.getNextHearingCourtApplicationId())
                .withNextHearingProsecutionCases(nextHearing.getNextHearingProsecutionCases() == null ? Collections.emptyList() :
                        nextHearing.getNextHearingProsecutionCases().stream().map(pc -> nextHearingProsecutionCase(pc).build()).collect(Collectors.toList()))
                .withReportingRestrictionReason(nextHearing.getReportingRestrictionReason())
                .withType(hearingType(nextHearing.getType()).build());
        if (nonNull(nextHearing.getHearingLanguage())) {
            builder.withHearingLanguage(HearingLanguage.valueOf(nextHearing.getHearingLanguage().name()));
        }
        if (nonNull(nextHearing.getJurisdictionType())) {
            builder.withJurisdictionType(JurisdictionType.valueOf(nextHearing.getJurisdictionType().name()));
        }
        return builder;
    }

    private ApiHearingType.Builder hearingType(final HearingType hearingType) {
        return ApiHearingType.apiHearingType().withDescription(hearingType.getDescription())
                .withWelshDescription(hearingType.getWelshDescription())
                .withId(hearingType.getId());
    }

    private ApiNextHearingProsecutionCase.Builder nextHearingProsecutionCase(final NextHearingProsecutionCase prosecutionCase) {
        return ApiNextHearingProsecutionCase.apiNextHearingProsecutionCase()
                .withDefendants(prosecutionCase.getDefendants() == null ? Collections.emptyList() :
                        prosecutionCase.getDefendants().stream().map(pc -> nextHearingDefendant(pc).build()).collect(Collectors.toList()))
                .withId(prosecutionCase.getId());
    }

    private ApiNextHearingDefendant.Builder nextHearingDefendant(final NextHearingDefendant nextHearingDefendant) {
        return ApiNextHearingDefendant.apiNextHearingDefendant().withId(nextHearingDefendant.getId())
                .withOffences(nextHearingDefendant.getOffences() == null ? Collections.emptyList() :
                        nextHearingDefendant.getOffences().stream().map(offence -> nextHearingOffence(offence).build()).collect(Collectors.toList()));
    }

    private ApiNextHearingOffence.Builder nextHearingOffence(final NextHearingOffence nextHearingOffence) {
        return ApiNextHearingOffence.apiNextHearingOffence().withId(nextHearingOffence.getId());
    }

    private ApiJudicialRole.Builder judicialRole(final JudicialRole judicialRole) {
        final ApiJudicialRole.Builder apiJudicialRole = ApiJudicialRole.apiJudicialRole();
        if (judicialRole.getJudicialRoleType() != null) {
            apiJudicialRole.withJudicialRoleType(judicialRoleType(judicialRole.getJudicialRoleType()).build());
        }
        return apiJudicialRole.withFirstName(judicialRole.getFirstName())
                .withIsBenchChairman(judicialRole.getIsBenchChairman())
                .withIsDeputy(judicialRole.getIsDeputy())
                .withJudicialId(judicialRole.getJudicialId())
                .withLastName(judicialRole.getLastName())
                .withMiddleName(judicialRole.getMiddleName())
                .withTitle(judicialRole.getTitle());
    }

    private ApiJudicialRoleType.Builder judicialRoleType(final JudicialRoleType judicialRoleType) {
        return ApiJudicialRoleType.apiJudicialRoleType().withJudicialRoleTypeId(judicialRoleType.getJudicialRoleTypeId())
                .withJudiciaryType(judicialRoleType.getJudiciaryType());
    }

    private ApiJudicialResultPrompt.Builder judicialResultPrompt(final JudicialResultPrompt judicialResultPrompt) {
        return ApiJudicialResultPrompt.apiJudicialResultPrompt()
                .withIsAvailableForCourtExtract("Y".equals(judicialResultPrompt.getCourtExtract()))
                .withIsFinancialImposition("Y".equals(judicialResultPrompt.getCourtExtract()))
                .withLabel(judicialResultPrompt.getLabel())
                .withPromptReference(judicialResultPrompt.getPromptReference())
                .withPromptSequence(judicialResultPrompt.getPromptSequence())
                .withUsergroups(judicialResultPrompt.getUsergroups())
                .withValue(judicialResultPrompt.getValue())
                .withWelshLabel(judicialResultPrompt.getWelshLabel())
                .withJudicialResultPromptTypeId(judicialResultPrompt.getJudicialResultPromptTypeId());
    }

    private ApiDelegatedPowers.Builder delegatePowers(final DelegatedPowers delegatedPowers) {
        return ApiDelegatedPowers.apiDelegatedPowers().withFirstName(delegatedPowers.getFirstName())
                .withLastName(delegatedPowers.getLastName())
                .withUserId(delegatedPowers.getUserId());
    }

    private ApiDefendantAlias.Builder defendantAlias(final DefendantAlias defendantAlias) {
        return ApiDefendantAlias.apiDefendantAlias()
                .withFirstName(defendantAlias.getFirstName())
                .withLastName(defendantAlias.getLastName())
                .withLegalEntityName(defendantAlias.getLegalEntityName())
                .withMiddleName(defendantAlias.getMiddleName())
                .withTitle(defendantAlias.getTitle());
    }

    private ApiAssociatedPerson.Builder associatedPerson(final AssociatedPerson associatedPerson) {
        final ApiAssociatedPerson.Builder apiAssociatedPerson = ApiAssociatedPerson.apiAssociatedPerson();
        if (associatedPerson.getPerson() != null) {
            apiAssociatedPerson.withPerson(person(associatedPerson.getPerson()).build());
        }
        return apiAssociatedPerson
                .withRole(associatedPerson.getRole());
    }

    private ApiPerson.Builder person(final Person person) {
        final ApiPerson.Builder apiPerson = ApiPerson.apiPerson();
        if (person.getAddress() != null) {
            apiPerson.withAddress(address(person.getAddress()).build());
        }
        if (person.getEthnicity() != null) {
            apiPerson.withEthnicity(ethnicity(person.getEthnicity()).build());
        }
        if (person.getContact() != null) {
            apiPerson.withContact(contactNumber(person.getContact()).build());
        }
        if (person.getDocumentationLanguageNeeds() != null) {
            apiPerson.withDocumentationLanguageNeeds(DocumentationLanguageNeeds.valueOf(person.getDocumentationLanguageNeeds().name()));
        }
        if (person.getTitle() != null) {
            apiPerson.withTitle(person.getTitle());
        }
        return apiPerson
                .withAdditionalNationalityCode(person.getAdditionalNationalityCode())
                .withAdditionalNationalityDescription(person.getAdditionalNationalityDescription())
                .withAdditionalNationalityId(person.getAdditionalNationalityId())
                .withDateOfBirth(person.getDateOfBirth())
                .withDisabilityStatus(person.getDisabilityStatus())
                .withFirstName(person.getFirstName())
                .withGender(Gender.valueOf(person.getGender().name()))
                .withOccupation(person.getOccupation())
                .withLastName(person.getLastName())
                .withMiddleName(person.getMiddleName())
                .withNationalInsuranceNumber(person.getNationalInsuranceNumber())
                .withInterpreterLanguageNeeds(person.getInterpreterLanguageNeeds())
                .withNationalityCode(person.getNationalityCode())
                .withNationalityDescription(person.getNationalityDescription())
                .withNationalityId(person.getNationalityId())
                .withSpecificRequirements(person.getSpecificRequirements())
                .withOccupationCode(person.getOccupationCode());
    }

    private ApiEthnicity.Builder ethnicity(final Ethnicity ethnicity) {
        return ApiEthnicity.apiEthnicity().withObservedEthnicityCode(ethnicity.getObservedEthnicityCode())
                .withObservedEthnicityDescription(ethnicity.getObservedEthnicityDescription())
                .withObservedEthnicityId(ethnicity.getObservedEthnicityId())
                .withSelfDefinedEthnicityCode(ethnicity.getSelfDefinedEthnicityCode())
                .withSelfDefinedEthnicityDescription(ethnicity.getSelfDefinedEthnicityDescription())
                .withSelfDefinedEthnicityId(ethnicity.getSelfDefinedEthnicityId());
    }

    private ApiContactNumber.Builder contactNumber(final ContactNumber contactNumber) {
        return ApiContactNumber.apiContactNumber().withFax(contactNumber.getFax())
                .withHome(contactNumber.getHome())
                .withMobile(contactNumber.getMobile())
                .withPrimaryEmail(contactNumber.getPrimaryEmail())
                .withSecondaryEmail(contactNumber.getSecondaryEmail())
                .withWork(contactNumber.getWork());
    }

    private List<ApiJudicialResult> filterJudicialResults(final List<JudicialResult> judicialResults) {
        return judicialResults.stream().filter(jr -> !jr.getPublishedForNows())
                .map(jr -> judicialResult(jr).build())
                .collect(Collectors.toList());
    }

    private List<ApiDefendantJudicialResult> filterDefendantJudicialResults(final List<DefendantJudicialResult> judicialResults) {
        return judicialResults.stream().filter(defendantJr -> !defendantJr.getJudicialResult().getPublishedForNows())
                .map(defendantJr -> defendantJudicialResult(judicialResult(defendantJr.getJudicialResult()), defendantJr.getMasterDefendantId()).build())
                .collect(Collectors.toList());
    }

    private ApiDefendantJudicialResult.Builder defendantJudicialResult(final ApiJudicialResult.Builder judicialResultBuilder, UUID masterDefendantId) {
        return ApiDefendantJudicialResult.apiDefendantJudicialResult()
                .withJudicialResult(judicialResultBuilder.build())
                .withMasterDefendantId(masterDefendantId);
    }
}