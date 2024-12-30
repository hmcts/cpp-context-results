package uk.gov.moj.cpp.results.domain.aggregate;

import org.junit.jupiter.api.Test;

import uk.gov.moj.cpp.results.domain.event.AmendmentType;
import uk.gov.moj.cpp.results.domain.event.ApplicationCasesResultDetails;
import uk.gov.moj.cpp.results.domain.event.ApplicationResultDetails;
import uk.gov.moj.cpp.results.domain.event.CaseResultDetails;
import uk.gov.moj.cpp.results.domain.event.DefendantResultDetails;
import uk.gov.moj.cpp.results.domain.event.JudicialResultDetails;
import uk.gov.moj.cpp.results.domain.event.OffenceResultDetails;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.results.domain.event.AmendmentType.ADDED;
import static uk.gov.moj.cpp.results.domain.event.AmendmentType.UPDATED;

public class ResultReshareHelperHasResultsTest {

    @Test
    void shouldReturnTrueWhenCourtApplicationCasesResultsExists() {
        final CaseResultDetails caseResultDetails = new CaseResultDetails(
                asList(application(
                        emptyList(),//application results
                        emptyList(), //courtOrderOffences
                        asList(applicationCasesResultDetails(ADDED), applicationCasesResultDetails(UPDATED, ADDED)) //courtApplicationCases
                )),
                UUID.randomUUID(),
                emptyList()
        );

        boolean isReshare = ResultReshareHelper.hasResults(caseResultDetails);
        assertThat(isReshare, is(true));
    }

    @Test
    void shouldReturnTrueWhenCourtOrderOffencesResultsExists() {
        final CaseResultDetails caseResultDetails = new CaseResultDetails(
                asList(application(
                        emptyList(),//application results
                        asList(offence(ADDED, ADDED)), //courtOrderOffences
                        emptyList() //courtApplicationCases
                )),
                UUID.randomUUID(),
                emptyList()
        );

        boolean isReshare = ResultReshareHelper.hasResults(caseResultDetails);
        assertThat(isReshare, is(true));
    }

    @Test
    void shouldReturnTrueWhenApplicationResultsExists() {
        final CaseResultDetails caseResultDetails = new CaseResultDetails(
                asList(application(
                        asList(result(ADDED)),//application results
                        emptyList(), //courtOrderOffences
                        emptyList() //courtApplicationCases
                )),
                UUID.randomUUID(),
                emptyList()
        );

        boolean isReshare = ResultReshareHelper.hasResults(caseResultDetails);
        assertThat(isReshare, is(true));
    }

    @Test
    void shouldReturnTrueWhenDefendantResultsExists() {
        final CaseResultDetails caseResultDetails = new CaseResultDetails(
                asList(application(
                        emptyList(),//application results
                        emptyList(), //courtOrderOffences
                        emptyList() //courtApplicationCases
                )),
                UUID.randomUUID(),
                asList(defendantResults(offence(ADDED, ADDED), offence(ADDED)))
        );

        boolean isReshare = ResultReshareHelper.hasResults(caseResultDetails);
        assertThat(isReshare, is(true));
    }

    @Test
    void shouldReturnFalseWhenAnyResultNotExists() {
        final CaseResultDetails caseResultDetails = new CaseResultDetails(
                asList(application(
                        emptyList(),//application results
                        emptyList(), //courtOrderOffences
                        emptyList() //courtApplicationCases
                )),
                UUID.randomUUID(),
                emptyList()
        );

        boolean isReshare = ResultReshareHelper.hasResults(caseResultDetails);
        assertThat(isReshare, is(false));
    }


    private ApplicationResultDetails application(final List<JudicialResultDetails> applicationResults, final List<OffenceResultDetails> courtOrderOffences, final List<ApplicationCasesResultDetails> courtApplicationCases) {
        return new ApplicationResultDetails(
                courtApplicationCases,
                "First Name",
                "Last Name",
                "App 1",
                UUID.randomUUID(),
                applicationResults,
                courtOrderOffences
        );
    }

    private ApplicationCasesResultDetails applicationCasesResultDetails(final AmendmentType... amendmentTypes) {
        return new ApplicationCasesResultDetails(UUID.randomUUID(), Arrays.stream(amendmentTypes)
                .map(this::result).collect(Collectors.toList()), 1, 1, "off1");
    }

    private DefendantResultDetails defendantResults(final OffenceResultDetails... offences) {
        return new DefendantResultDetails("Def 1", UUID.randomUUID(), asList(offences));
    }

    private OffenceResultDetails offence(final AmendmentType... amendmentTypes) {
        return new OffenceResultDetails(UUID.randomUUID(), Arrays.stream(amendmentTypes)
                .map(this::result).collect(Collectors.toList()),1, 1, "");
    }

    private JudicialResultDetails result(final AmendmentType amendmentType) {
        return new JudicialResultDetails(amendmentType, UUID.randomUUID(), UUID.randomUUID(), "title");
    }
}