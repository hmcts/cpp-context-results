package uk.gov.moj.cpp.results.event.helper;

import static java.lang.Integer.valueOf;
import static java.util.UUID.fromString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicSJPCaseResulted;

import uk.gov.justice.core.courts.BaseStructure;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.sjp.results.BaseSessionStructure;
import uk.gov.justice.sjp.results.PublicSjpResulted;
import uk.gov.justice.sjp.results.SessionLocation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BaseSessionStructureConverterForSjpTest {

    private static final String FIELD_NATIONAL_COURT_CODE = "nationalCourtCode";
    private static final String FIELD_OU_CODE = "oucode";

    @InjectMocks
    BaseSessionStructureConverterForSjp baseSessionStructureConverterForSjp;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private BaseStructure baseStructure;

    @Test
    public void testBaseSessionStructureConverter() throws Exception {

        final PublicSjpResulted sjpCaseResulted = basicSJPCaseResulted();
        final BaseSessionStructure sjpBaseSessionStructure = sjpCaseResulted.getSession();

        baseStructure = baseSessionStructureConverterForSjp.convert(sjpCaseResulted);

        assertThat(baseStructure.getId(), is(sjpBaseSessionStructure.getSessionId()));
        assertThat(baseStructure.getSessionDays().get(0).getSittingDay(), is(sjpBaseSessionStructure.getDateAndTimeOfSession()));
        assertCourtCentre(baseStructure.getCourtCentreWithLJA().getCourtCentre(), sjpBaseSessionStructure.getSessionLocation());
        assertThat(baseStructure.getCourtCentreWithLJA().getCourtHearingLocation(), is("B22HM00"));
        assertThat(baseStructure.getCourtCentreWithLJA().getPsaCode(), is(valueOf(8505)));
    }

    private void assertCourtCentre(final CourtCentre courtCentre, final SessionLocation sessionLocation) {
        assertThat(courtCentre.getName(), is(sessionLocation.getName()));
        assertThat(courtCentre.getId(), is(sessionLocation.getCourtId()));
        assertThat(courtCentre.getRoomId(), is(fromString(sessionLocation.getRoomId())));
        assertThat(courtCentre.getAddress(), is(sessionLocation.getAddress()));
    }
}



