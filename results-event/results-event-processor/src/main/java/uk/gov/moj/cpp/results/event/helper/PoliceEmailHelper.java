package uk.gov.moj.cpp.results.event.helper;

import org.apache.commons.collections.CollectionUtils;
import uk.gov.moj.cpp.results.domain.event.AmendmentType;
import uk.gov.moj.cpp.results.domain.event.ApplicationResultDetails;
import uk.gov.moj.cpp.results.domain.event.CaseResultDetails;
import uk.gov.moj.cpp.results.domain.event.DefendantResultDetails;
import uk.gov.moj.cpp.results.domain.event.JudicialResultDetails;
import uk.gov.moj.cpp.results.domain.event.OffenceResultDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class PoliceEmailHelper {
    private static final String BEGIN_PARAGRAPH = "<p style='line-height: 1.6'>";
    private static final String END_PARAGRAPH = "</p>";
    private static final String DEFENDANT_NAME_TEMPL = "$DEFENDANT_NAME<br />";
    private static final String OFFENCE_TEMPL = "$OFFENCE_COUNT_OR_INDEX_LABEL: $OFFENCE_COUNT_OR_INDEX_VALUE &nbsp;&nbsp;&nbsp;&nbsp;$OFFENCE_TITLE - $AMENDMENT_DETAILS <br />";
    private static final String APP_OFFENCE_TEMPL = "$OFFENCE_TITLE - $AMENDMENT_DETAILS <br />";
    private static final String AMENDMENT_DETAILS_TEMPL= "$AMENDMENT_TYPE: $RESULT_TITLES &nbsp;";
    private static final String APPLICATION_TEMPL = "$APPLICATION_TITLE - $AMENDMENT_DETAILS <br />";
    public static final String EMPTY_LINE = "<br />";
    public static final String OFFENCE_COUNT = "Offence count";
    public static final String OFFENCE_SEQUENCE_NUMBER = "Offence number";
    private static final String DEFENDANT_NAME = "$DEFENDANT_NAME";
    private static final String OFFENCE_COUNT_OR_INDEX_LABEL = "$OFFENCE_COUNT_OR_INDEX_LABEL";
    private static final String OFFENCE_COUNT_OR_INDEX_VALUE = "$OFFENCE_COUNT_OR_INDEX_VALUE";
    private static final String OFFENCE_TITLE = "$OFFENCE_TITLE";
    private static final String AMENDMENT_DETAILS = "$AMENDMENT_DETAILS";
    private static final String AMENDMENT_TYPE = "$AMENDMENT_TYPE";
    private static final String RESULT_TITLES = "$RESULT_TITLES";
    private static final String APPLICATION_TITLE = "$APPLICATION_TITLE";


    public String buildDefendantAmendmentDetails(final CaseResultDetails caseResultDetails) {
        final StringBuilder sb = new StringBuilder();
        sb.append(BEGIN_PARAGRAPH);
        if (isNotEmpty(caseResultDetails.getDefendantResultDetails())) {
            appendDefendantResultDetailsFromDefendants(caseResultDetails, sb);
        } else if (isNotEmpty(caseResultDetails.getApplicationResultDetails())) {
            appendDefendantResultDetailsFromApplications(caseResultDetails, sb);
        }
        sb.append(END_PARAGRAPH);
        return sb.toString();
    }

    public String buildApplicationAmendmentDetails(final List<ApplicationResultDetails> applicationResultDetailsList) {
        final StringBuilder sb = new StringBuilder();
        sb.append(BEGIN_PARAGRAPH);
        for (final ApplicationResultDetails applicationResultDetails: applicationResultDetailsList) {
            sb.append(APPLICATION_TEMPL.replace(APPLICATION_TITLE, applicationResultDetails.getApplicationTitle())
                    .replace(AMENDMENT_DETAILS, buildResultsAmendmentDetails(applicationResultDetails.getJudicialResultDetails())));
        }

        for (final ApplicationResultDetails applicationResultDetails: applicationResultDetailsList) {
            for (final OffenceResultDetails offenceResultDetails: applicationResultDetails.getOffenceResultDetails()) {

                sb.append(APP_OFFENCE_TEMPL.replace(OFFENCE_TITLE, offenceResultDetails.getOffenceTitle())
                        .replace(AMENDMENT_DETAILS, buildResultsAmendmentDetails(offenceResultDetails.getJudicialResultDetails())));
            }
        }

        sb.append(END_PARAGRAPH);
        return sb.toString();
    }

    private String buildResultsAmendmentDetails(final List<JudicialResultDetails> judicialResultDetailsList) {
        final StringBuilder sb = new StringBuilder();
        final Map<AmendmentType, List<JudicialResultDetails>> resultTitlesByAmendmentType = judicialResultDetailsList.stream()
                .collect(Collectors.groupingBy(JudicialResultDetails::getAmendmentType));

        buildResultTitlesText(AmendmentType.ADDED, resultTitlesByAmendmentType.get(AmendmentType.ADDED))
                .ifPresent(text -> sb.append(text).append(" "));

        buildResultTitlesText(AmendmentType.DELETED, resultTitlesByAmendmentType.get(AmendmentType.DELETED))
                .ifPresent(text -> sb.append(text).append(" "));

        buildResultTitlesText(AmendmentType.UPDATED, resultTitlesByAmendmentType.get(AmendmentType.UPDATED))
                .ifPresent(text -> sb.append(text).append(" "));

        return sb.toString();
    }

    private Optional<String> buildResultTitlesText(final AmendmentType amendmentType, final List<JudicialResultDetails> judicialResultDetails) {
        if (CollectionUtils.isEmpty(judicialResultDetails)) {
            return Optional.empty();
        }

        return Optional.of(AMENDMENT_DETAILS_TEMPL
                .replace(AMENDMENT_TYPE, amendmentTypeLabel(amendmentType))
                .replace(RESULT_TITLES, judicialResultDetails.stream()
                        .map(JudicialResultDetails::getResultTitle)
                        .collect(Collectors.joining(", "))));
    }

    private String amendmentTypeLabel(final AmendmentType amendmentType) {
        return amendmentType.name().charAt(0) + amendmentType.name().substring(1).toLowerCase();
    }

    private static void appendDefendantResultDetailsFromApplications(final CaseResultDetails caseResultDetails, final StringBuilder sb) {
        for (final ApplicationResultDetails applicationResultDetails : caseResultDetails.getApplicationResultDetails()) {
            sb.append(DEFENDANT_NAME_TEMPL.replace(DEFENDANT_NAME, applicationResultDetails.getApplicationSubjectFirstName()
                    .concat(" ")
                    .concat(applicationResultDetails.getApplicationSubjectLastName())));
            sb.append(EMPTY_LINE);
        }
    }

    private void appendDefendantResultDetailsFromDefendants(final CaseResultDetails caseResultDetails, final StringBuilder sb) {
        for (final DefendantResultDetails defendantResultDetails : caseResultDetails.getDefendantResultDetails()) {
            sb.append(DEFENDANT_NAME_TEMPL.replace(DEFENDANT_NAME, defendantResultDetails.getDefendantName()));
            for (final OffenceResultDetails offenceResultDetails : defendantResultDetails.getOffenceResultDetails()) {
                final boolean offenceCountIsNotEmpty = nonNull(offenceResultDetails.getOffenceCount()) && offenceResultDetails.getOffenceCount() > 0;
                final String offenceCountOrIndexLabel = offenceCountIsNotEmpty ? OFFENCE_COUNT : OFFENCE_SEQUENCE_NUMBER;
                final String offenceCountOrIndexValue = offenceCountIsNotEmpty ? offenceResultDetails.getOffenceCount().toString()
                        : offenceResultDetails.getOffenceNo().toString();

                sb.append(OFFENCE_TEMPL.replace(OFFENCE_COUNT_OR_INDEX_LABEL, offenceCountOrIndexLabel)
                        .replace(OFFENCE_COUNT_OR_INDEX_VALUE, offenceCountOrIndexValue)
                        .replace(OFFENCE_TITLE, offenceResultDetails.getOffenceTitle())
                        .replace(AMENDMENT_DETAILS, buildResultsAmendmentDetails(offenceResultDetails.getJudicialResultDetails())));

            }

            sb.append(EMPTY_LINE);
        }
    }


}
