package uk.gov.moj.cpp.results.event.helper.results;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static uk.gov.justice.core.courts.OffenceDetails.offenceDetails;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.Offence;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

public class OffenceDetails {

    private static final int OFFENCE_DATE_CODE_DEFAULT_VALUE = 1;
    private static final String FINAL_DISPOSAL_N = "N";
    private static final String FINAL_DISPOSAL_Y = "Y";
    public static final String DEFAULT_CJS_VERDICT_CODE = "G";
    private static final int OFFENCE_DATE_CODE_4 = 4;

    public List<uk.gov.justice.core.courts.OffenceDetails> buildOffences(CourtApplicationCase courtApplicationCase, final CourtApplication courtApplication) {
        final List<Offence> offences = getOffences(courtApplicationCase);
        final List<Offence> updatedOffences = new MoveDefendantJudicialResultsHelper().buildOffenceAndDefendantJudicialResults(offences, new ArrayList<>(), new ArrayList<>());
        return buildOffenceForApplication(updatedOffences, courtApplication);
    }


    public List<uk.gov.justice.core.courts.OffenceDetails> buildOffences(CourtOrderOffence courtOrderOffence, CourtApplication courtApplication) {
        final List<Offence> updatedOffences = new MoveDefendantJudicialResultsHelper().buildOffenceAndDefendantJudicialResults(Lists.newArrayList(courtOrderOffence.getOffence()), new ArrayList<>(), new ArrayList<>());
        return buildOffenceForApplication(updatedOffences, courtApplication);
    }

    public List<uk.gov.justice.core.courts.OffenceDetails> buildOffences(final Defendant defendant, final List<DefendantJudicialResult> hearingLevelResults) {
        final List<Offence> offences = defendant.getOffences();
        final List<Offence> updatedOffences = new MoveDefendantJudicialResultsHelper().buildOffenceAndDefendantJudicialResults(offences, defendant.getDefendantCaseJudicialResults(), hearingLevelResults);
        return buildOffenceDetails(updatedOffences);
    }

    private List<uk.gov.justice.core.courts.OffenceDetails> buildOffenceDetails(List<Offence> updatedOffences) {
        final List<uk.gov.justice.core.courts.OffenceDetails> offenceDetailsList = new ArrayList<>();

        for (final Offence offence : updatedOffences) {
            final uk.gov.justice.core.courts.OffenceDetails.Builder offenceDetailsBuilder = offenceDetails()
                    .withArrestDate(offence.getArrestDate())
                    .withChargeDate(offence.getChargeDate())
                    .withConvictionDate(offence.getConvictionDate())
                    .withEndDate(offence.getEndDate())
                    .withFinalDisposal(getFinalDisposal(offence))
                    .withId(offence.getId())
                    .withModeOfTrial(offence.getModeOfTrial())
                    .withAllocationDecision(offence.getAllocationDecision())
                    .withOffenceCode(offence.getOffenceCode())
                    .withOffenceDateCode(nonNull(offence.getOffenceDateCode()) ? offence.getOffenceDateCode() : OFFENCE_DATE_CODE_DEFAULT_VALUE)
                    .withOffenceFacts(offence.getOffenceFacts())
                    .withOffenceSequenceNumber(offence.getOrderIndex())
                    .withPlea(offence.getPlea())
                    .withStartDate(offence.getStartDate())
                    .withJudicialResults(offence.getJudicialResults())
                    .withFinding(getVerdictCode(nonNull(offence.getVerdict()) && nonNull(offence.getVerdict().getVerdictType()) ? offence.getVerdict().getVerdictType().getCjsVerdictCode() : null, offence.getConvictionDate()))
                    .withWording(offence.getWording());

            offenceDetailsList.add(offenceDetailsBuilder.build());
        }
        return offenceDetailsList;
    }
    private List<uk.gov.justice.core.courts.OffenceDetails> buildOffenceForApplication(final List<Offence> updatedOffences, final CourtApplication courtApplication) {
        final List<uk.gov.justice.core.courts.OffenceDetails> offenceDetailsList = new ArrayList<>();
        ofNullable(courtApplication.getJudicialResults()).filter(results -> Objects.nonNull(courtApplication.getType().getCode()) && courtApplication.getType().getSpiOutApplicableFlag())
                .ifPresent(result -> offenceDetailsList.add(buildOffenceFromApplication(courtApplication)));
        offenceDetailsList.addAll(buildOffenceDetails(updatedOffences));
        return offenceDetailsList;
    }
    private uk.gov.justice.core.courts.OffenceDetails buildOffenceFromApplication(final CourtApplication courtApplication) {

            final uk.gov.justice.core.courts.OffenceDetails.Builder offenceDetailsBuilder = offenceDetails()
                    .withChargeDate(courtApplication.getApplicationReceivedDate())
                    .withPlea(courtApplication.getPlea())
                    .withModeOfTrial("")
                    .withFinalDisposal(getFinalDisposal(courtApplication))
                    .withConvictionDate(courtApplication.getConvictionDate())
                    .withFinding(getVerdictCode(nonNull(courtApplication.getVerdict()) && nonNull(courtApplication.getVerdict().getVerdictType()) ? courtApplication.getVerdict().getVerdictType().getCjsVerdictCode() : null, courtApplication.getConvictionDate()))
                    .withOffenceSequenceNumber(0)
                    .withOffenceCode(courtApplication.getType().getCode())
                    .withWording(StringUtils.defaultIfEmpty(courtApplication.getApplicationParticulars(), ""))
                    .withOffenceDateCode(getOffenceDateCode(courtApplication.getAllegationOrComplaintStartDate(), courtApplication.getAllegationOrComplaintEndDate()))
                    .withStartDate(defaultIfNull(courtApplication.getAllegationOrComplaintStartDate(), courtApplication.getApplicationReceivedDate()))
                    .withEndDate(courtApplication.getAllegationOrComplaintEndDate())
                    .withArrestDate(null)
                    .withId(courtApplication.getId())
                    .withJudicialResults(courtApplication.getJudicialResults());

        return offenceDetailsBuilder.build();
    }

    private int getOffenceDateCode(final LocalDate startDate, final LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return OFFENCE_DATE_CODE_4;
        }
        return OFFENCE_DATE_CODE_DEFAULT_VALUE;
    }

    private List<Offence> getOffences(final CourtApplicationCase courtApplicationCase) {

        return ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty).collect(Collectors.toList());

    }

    private String getFinalDisposal(final Offence offence) {
        return null != offence.getIsDisposed() && offence.getIsDisposed() ? FINAL_DISPOSAL_Y : FINAL_DISPOSAL_N;
    }

    private String getFinalDisposal(final CourtApplication courtApplication) {
        return courtApplication.getJudicialResults().stream().anyMatch(judicialResult -> JudicialResultCategory.FINAL.equals(judicialResult.getCategory())) ? "Y": "N";
    }

    private String getVerdictCode(final String cjsVerdictCode, final LocalDate convictedDate) {
        if (isNull(cjsVerdictCode) && nonNull(convictedDate)) {
            return DEFAULT_CJS_VERDICT_CODE;
        }else if (isNull(cjsVerdictCode)){
            return null;
        }

        return cjsVerdictCode;
    }

}
