package uk.gov.moj.cpp.results.domain.aggregate;

import org.junit.Test;
import uk.gov.moj.cpp.domains.resultdetails.ApplicationResultDetails;
import uk.gov.moj.cpp.domains.resultdetails.CaseResultDetails;
import uk.gov.moj.cpp.domains.resultdetails.DefendantResultDetails;
import uk.gov.moj.cpp.domains.resultdetails.JudicialResultAmendmentType;
import uk.gov.moj.cpp.domains.resultdetails.JudicialResultDetails;
import uk.gov.moj.cpp.domains.resultdetails.OffenceResultDetails;
import uk.gov.moj.cpp.results.domain.event.AmendmentType;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CaseResultDetailsConverterTest {

    @Test
    public void convert() {
        UUID caseId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID judicialResultIdInCase1 = UUID.randomUUID();
        UUID judicialResultIdInCase2 = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID judicialResultIdInApp = UUID.randomUUID();
        String defendantName = "Defendant Name";
        String offenceTitle = "Offence 1";
        int offenceNo = 2;
        int offenceCount = 5;
        String resultTitle = "Result 1";
        String applicationTitle = "Application 1";
        String appResultTitle = "Result In App 1";
        UUID judicialResultTypeId = UUID.randomUUID();
        UUID judicialResultTypeIdInApp = UUID.randomUUID();
        UUID judicialResultIdInAppOffence1 = UUID.randomUUID();
        UUID judicialResultIdInAppOffence2 = UUID.randomUUID();


        final CaseResultDetails caseResultDetails = new CaseResultDetails(caseId, Arrays.asList(
                new DefendantResultDetails(defendantId, defendantName, Arrays.asList(
                        new OffenceResultDetails(offenceId, offenceNo, offenceCount, offenceTitle, Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInCase1, resultTitle, judicialResultTypeId, JudicialResultAmendmentType.ADDED),
                                new JudicialResultDetails(judicialResultIdInCase2, resultTitle, judicialResultTypeId, JudicialResultAmendmentType.NONE)
                        ))
                ))),
                Arrays.asList(
                        new ApplicationResultDetails(applicationId, applicationTitle, Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInApp, appResultTitle, judicialResultTypeIdInApp, JudicialResultAmendmentType.ADDED)
                        ), Arrays.asList(
                                new OffenceResultDetails(offenceId, offenceNo, offenceCount, offenceTitle, Arrays.asList(
                                        new JudicialResultDetails(judicialResultIdInAppOffence1, resultTitle, judicialResultTypeId, JudicialResultAmendmentType.ADDED),
                                        new JudicialResultDetails(judicialResultIdInAppOffence2, resultTitle, judicialResultTypeId, JudicialResultAmendmentType.NONE)
                                ))
                        ), "firstName", "lastName")
                )
        );


        uk.gov.moj.cpp.results.domain.event.CaseResultDetails resultDetails = CaseResultDetailsConverter.convert(caseResultDetails);
        assertThat(resultDetails.getCaseId(), is(caseId));
        assertThat(resultDetails.getDefendantResultDetails().size(), is(1));
        assertThat(resultDetails.getDefendantResultDetails().get(0).getId(), is(defendantId));
        assertThat(resultDetails.getDefendantResultDetails().get(0).getDefendantName(), is(defendantName));
        assertThat(resultDetails.getDefendantResultDetails().get(0).getOffenceResultDetails().size(), is(1));
        assertThat(resultDetails.getDefendantResultDetails().get(0).getOffenceResultDetails().get(0).getId(), is(offenceId));
        assertThat(resultDetails.getDefendantResultDetails().get(0).getOffenceResultDetails().get(0).getOffenceNo(), is(offenceNo));
        assertThat(resultDetails.getDefendantResultDetails().get(0).getOffenceResultDetails().get(0).getOffenceCount(), is(offenceCount));
        assertThat(resultDetails.getDefendantResultDetails().get(0).getOffenceResultDetails().get(0).getOffenceTitle(), is(offenceTitle));
        assertThat(resultDetails.getDefendantResultDetails().get(0).getOffenceResultDetails().get(0).getJudicialResultDetails().size(), is(1));
        assertThat(resultDetails.getDefendantResultDetails().get(0).getOffenceResultDetails().get(0).getJudicialResultDetails().get(0).getId(), is(judicialResultIdInCase1));
        assertThat(resultDetails.getDefendantResultDetails().get(0).getOffenceResultDetails().get(0).getJudicialResultDetails().get(0).getResultTitle(), is(resultTitle));
        assertThat(resultDetails.getDefendantResultDetails().get(0).getOffenceResultDetails().get(0).getJudicialResultDetails().get(0).getJudicialResultTypeId(), is(judicialResultTypeId));
        assertThat(resultDetails.getDefendantResultDetails().get(0).getOffenceResultDetails().get(0).getJudicialResultDetails().get(0).getAmendmentType(), is(AmendmentType.ADDED));

        assertThat(resultDetails.getApplicationResultDetails().size(), is(1));
        assertThat(resultDetails.getApplicationResultDetails().get(0).getId(), is(applicationId));
        assertThat(resultDetails.getApplicationResultDetails().get(0).getApplicationTitle(), is(applicationTitle));
        assertThat(resultDetails.getApplicationResultDetails().get(0).getJudicialResultDetails().size(), is(1));
        assertThat(resultDetails.getApplicationResultDetails().get(0).getJudicialResultDetails().get(0).getId(), is(judicialResultIdInApp));
        assertThat(resultDetails.getApplicationResultDetails().get(0).getJudicialResultDetails().get(0).getResultTitle(), is(appResultTitle));
        assertThat(resultDetails.getApplicationResultDetails().get(0).getJudicialResultDetails().get(0).getJudicialResultTypeId(), is(judicialResultTypeIdInApp));
        assertThat(resultDetails.getApplicationResultDetails().get(0).getJudicialResultDetails().get(0).getAmendmentType(), is(AmendmentType.ADDED));
        assertThat(resultDetails.getApplicationResultDetails().get(0).getOffenceResultDetails().get(0).getJudicialResultDetails().size(), is(1));
        assertThat(resultDetails.getApplicationResultDetails().get(0).getOffenceResultDetails().get(0).getJudicialResultDetails().get(0).getAmendmentType(), is(AmendmentType.ADDED));
        assertThat(resultDetails.getApplicationResultDetails().get(0).getOffenceResultDetails().get(0).getJudicialResultDetails().get(0).getResultTitle(), is(resultTitle));

    }
}