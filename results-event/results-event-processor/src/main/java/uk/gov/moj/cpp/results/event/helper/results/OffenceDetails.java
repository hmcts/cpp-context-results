package uk.gov.moj.cpp.results.event.helper.results;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.core.courts.OffenceDetails.offenceDetails;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Offence;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OffenceDetails {

    private static final int OFFENCE_DATE_CODE_DEFAULT_VALUE = 1;
    private static final String FINAL_DISPOSAL_N = "N";
    private static final String FINAL_DISPOSAL_Y = "Y";
    public static final String DEFAULT_CJS_VERDICT_CODE = "G";


    public List<uk.gov.justice.core.courts.OffenceDetails> buildOffences(final Defendant defendant, final List<DefendantJudicialResult> hearingLevelResults) {
        final List<Offence> offences = defendant.getOffences();
        final List<uk.gov.justice.core.courts.OffenceDetails> offenceDetailsList = new ArrayList<>();
        final List<Offence> updatedOffences = new MoveDefendantJudicialResultsHelper().buildOffenceAndDefendantJudicialResults(offences, defendant.getDefendantCaseJudicialResults(), hearingLevelResults);

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

    private String getFinalDisposal(final Offence offence) {
        return null != offence.getIsDisposed() && offence.getIsDisposed() ? FINAL_DISPOSAL_Y : FINAL_DISPOSAL_N;
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
