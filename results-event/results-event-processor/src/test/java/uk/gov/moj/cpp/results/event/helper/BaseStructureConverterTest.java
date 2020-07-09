package uk.gov.moj.cpp.results.event.helper;

import static java.lang.Integer.valueOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.BaseStructure;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.SessionDay;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.util.List;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BaseStructureConverterTest {

    private static final String FIELD_NATIONAL_COURT_CODE = "lja";
    private static final String FIELD_OU_CODE = "oucode";

    @InjectMocks
    BaseStructureConverter baseStructureConverter;

    @Mock
    ReferenceDataService referenceDataService;

    @Mock
    private JsonObject jsonEnvelope;

    @Mock
    private BaseStructure baseStructure;

    @Test
    public void testConverter() throws Exception {
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsWithMagistratesTemplate();
        final Hearing hearing = shareResultsMessage.getHearing();
        final List<HearingDay> hearingDays = hearing.getHearingDays();

        when(referenceDataService.getOrgainsationUnit(anyString(), any())).thenReturn(getJsonObjectWithNationalCourtCodeAndOuCode());
        baseStructure = baseStructureConverter.convert(shareResultsMessage);
        assertThat(baseStructure.getId(), is(hearing.getId()));
        assertThat(baseStructure.getCourtCentreWithLJA().getCourtCentre(), is(hearing.getCourtCentre()));

        assertThat(baseStructure.getCourtCentreWithLJA().getCourtHearingLocation(), is(getJsonObjectWithNationalCourtCodeAndOuCode().getString(FIELD_OU_CODE)));
        assertThat(baseStructure.getCourtCentreWithLJA().getPsaCode(), is(valueOf(getJsonObjectWithNationalCourtCodeAndOuCode().getString(FIELD_NATIONAL_COURT_CODE))));

        final List<SessionDay> sessionDays = baseStructure.getSessionDays();
        assertThat(sessionDays, hasSize(hearingDays.size()));
        for (final SessionDay sessionDay : sessionDays) {
            final Optional<HearingDay> hearingDay = hearingDays.stream().filter(h -> h.getListedDurationMinutes().equals(sessionDay.getListedDurationMinutes())
                    && h.getListingSequence().equals(sessionDay.getListingSequence())
                    && h.getSittingDay().equals(sessionDay.getSittingDay())).findFirst();
            assertThat(hearingDay.isPresent(), is(false));
        }
    }

    private JsonObject getJsonObjectWithNationalCourtCodeAndOuCode() {
        return Json.createObjectBuilder()
                .add(FIELD_NATIONAL_COURT_CODE, "2574")
                .add(FIELD_OU_CODE, "B01BH00")
                .build();
    }

}