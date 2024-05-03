package uk.gov.moj.cpp.results.event.helper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.results.domain.event.AmendmentType;
import uk.gov.moj.cpp.results.domain.event.ApplicationCasesResultDetails;
import uk.gov.moj.cpp.results.domain.event.ApplicationResultDetails;
import uk.gov.moj.cpp.results.domain.event.CaseResultDetails;
import uk.gov.moj.cpp.results.domain.event.DefendantResultDetails;
import uk.gov.moj.cpp.results.domain.event.JudicialResultDetails;
import uk.gov.moj.cpp.results.domain.event.OffenceResultDetails;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class PoliceEmailHelperTest {

    @InjectMocks
    private PoliceEmailHelper policeEmailHelper;


    @Test
    public void buildDefendantAmendmentDetails() {
        final String expectedOutput = "<p style='line-height: 1.6'>John Smith<br />Offence count: 1 &nbsp;" +
                "Offence 1 - Added: Result 1, Result 2 &nbsp; Deleted: Result 3 &nbsp; Updated: " +
                "Result 4 &nbsp;  <br /><br />John Doe<br />Offence number: 1 &nbsp;" +
                "Offence 1 - Added: Result 1 &nbsp; Updated: Result 2 &nbsp;  <br />" +
                "Offence number: 2 &nbsp;Offence 2 - Added: Result 3 &nbsp; " +
                "Deleted: Result 4 &nbsp;  </p>";

        CaseResultDetails.Builder caseResultDetailsBuilder = new CaseResultDetails.Builder();
        caseResultDetailsBuilder.withDefendantResultDetails(Arrays.asList(
                defendant("John Smith", offence("Offence 1", 1, null,
                        result("Result 1", AmendmentType.ADDED),
                        result("Result 2", AmendmentType.ADDED),
                        result("Result 3", AmendmentType.DELETED),
                        result("Result 4", AmendmentType.UPDATED))),
                defendant("John Doe",
                        offence("Offence 1", null, 1,
                                result("Result 1", AmendmentType.ADDED),
                                result("Result 2", AmendmentType.UPDATED)),
                        offence("Offence 2", 0, 2,
                                result("Result 3", AmendmentType.ADDED),
                                result("Result 4", AmendmentType.DELETED))
                )));
        caseResultDetailsBuilder.withCaseId(UUID.randomUUID());
        final String output = policeEmailHelper.buildDefendantAmendmentDetails(caseResultDetailsBuilder.build());

        assertThat(output, is(expectedOutput));

    }

    @Test
    public void buildDefendantAmendmentDetailsWithOnlyApplication_WhenDefendantResultDetailsListEmpty() {
        final String expectedOutput = "<p style='line-height: 1.6'>John Smith</p>";

        CaseResultDetails.Builder caseResultDetailsBuilder = new CaseResultDetails.Builder();
        caseResultDetailsBuilder.withDefendantResultDetails(Collections.emptyList());
        caseResultDetailsBuilder.withCaseId(UUID.randomUUID());
        caseResultDetailsBuilder.withApplicationResultDetails(Arrays.asList(
                ApplicationResultDetails.applicationResultDetails()
                        .withApplicationSubjectLastName("Smith")
                        .withApplicationSubjectFirstName("John")
                        .build()
        ));
        final String output = policeEmailHelper.buildDefendantAmendmentDetails(caseResultDetailsBuilder.build());

        assertThat(output, is(expectedOutput));

    }


    @Test
    public void buildApplicationAmendmentDetails() {
        final String expectedOutput = "<p style='line-height: 1.6'>Application 1 - Added: Result 1, Result 2 &nbsp; " +
                "Deleted: Result 3 &nbsp; Updated: Result 4 &nbsp;  <br />" +
                "Application 2 - Updated: Result 2, Result 2 &nbsp;  <br />" +
                "App Offence 1 - Added: Result 5, Result 6 &nbsp;  <br />" +
                "App Offence 2 - Added: Result 8 &nbsp; Updated: Result 7 &nbsp;  <br />" +
                "App Offence 3 - Added: Result 9, Result 10 &nbsp;  <br />" +
                "App Offence 4 - Added: Result 12 &nbsp; Updated: Result 11 &nbsp;  " +
                "</p>";

        final String output = policeEmailHelper.buildApplicationAmendmentDetails(Arrays.asList(
                application("Application 1",  Arrays.asList(
                        result("Result 1", AmendmentType.ADDED),
                        result("Result 2", AmendmentType.ADDED),
                        result("Result 3", AmendmentType.DELETED),
                        result("Result 4", AmendmentType.UPDATED)),
                        Arrays.asList(
                                appOffence("App Offence 1", result("Result 5", AmendmentType.ADDED), result("Result 6", AmendmentType.ADDED)),
                                appOffence("App Offence 2", result("Result 7", AmendmentType.UPDATED), result("Result 8", AmendmentType.ADDED))
                        ),
                        Arrays.asList(
                                appCaseOffenceResultDetails("App Offence 3", result("Result 9", AmendmentType.ADDED), result("Result 10", AmendmentType.ADDED)),
                                appCaseOffenceResultDetails("App Offence 4", result("Result 11", AmendmentType.UPDATED), result("Result 12", AmendmentType.ADDED))
                        )

                ),
                application("Application 2",  Arrays.asList(result("Result 2", AmendmentType.UPDATED),
                        result("Result 2", AmendmentType.UPDATED)), Collections.emptyList(), Collections.emptyList())));

        assertThat(output, is(expectedOutput));
    }

    private ApplicationResultDetails application(String title, List<JudicialResultDetails> judicialResultDetails, List<OffenceResultDetails> offenceResultDetails, List<ApplicationCasesResultDetails> applicationCasesResultDetails) {
        return ApplicationResultDetails.applicationResultDetails()
                .withApplicationTitle(title)
                .withJudicialResultDetails(judicialResultDetails)
                .withOffenceResultDetails(offenceResultDetails)
                .withApplicationCasesResultDetails(applicationCasesResultDetails)
                .build();
    }

    private DefendantResultDetails defendant(String defendantName, OffenceResultDetails... offenceResultDetails) {
        return DefendantResultDetails.defendantResultDetails()
                .withDefendantName(defendantName)
                .withOffenceResultDetails(Arrays.asList(offenceResultDetails))
                .build();
    }

    private ApplicationCasesResultDetails appCaseOffenceResultDetails(String title, JudicialResultDetails... judicialResultDetails) {
        return ApplicationCasesResultDetails.applicationCasesResultDetails()
                .withOffenceTitle(title)
                .withOffenceCount(0)
                .withOffenceNo(0)
                .withJudicialResultDetails(Arrays.asList(judicialResultDetails))
                .build();
    }

    private OffenceResultDetails appOffence(String title, JudicialResultDetails... judicialResultDetails) {
        return OffenceResultDetails.offenceResultDetails()
                .withOffenceTitle(title)
                .withOffenceCount(0)
                .withOffenceNo(0)
                .withJudicialResultDetails(Arrays.asList(judicialResultDetails))
                .build();
    }

    private OffenceResultDetails offence(String title, Integer count, Integer index, JudicialResultDetails... judicialResultDetails) {
        return OffenceResultDetails.offenceResultDetails()
                .withOffenceTitle(title)
                .withOffenceCount(count)
                .withOffenceNo(index)
                .withJudicialResultDetails(Arrays.asList(judicialResultDetails))
                .build();
    }

    private JudicialResultDetails result(String title, AmendmentType amendmentType) {
        return JudicialResultDetails.judicialResultDetails()
                .withAmendmentType(amendmentType)
                .withResultTitle(title)
                .build();
    }


}