package uk.gov.moj.cpp.results.event.helper;

import static java.lang.Integer.valueOf;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareHearingTemplateWithApplication;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsV2Template;

import uk.gov.justice.core.courts.BaseStructure;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.SessionDay;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BaseStructureConverterTest {

    private static final String FIELD_NATIONAL_COURT_CODE = "lja";
    private static final String FIELD_OU_CODE = "oucode";
    public static final String COURT_ROOM_OUCODE_FROM_REFDATA = "B01BH01";

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
        final PublicHearingResulted shareResultsMessage = basicShareResultsV2Template(JurisdictionType.MAGISTRATES);
        final Hearing hearing = shareResultsMessage.getHearing();
        final List<HearingDay> hearingDays = hearing.getHearingDays();

        when(referenceDataService.getOrganisationUnit(anyString(), any())).thenReturn(getJsonObjectWithNationalCourtCodeAndOuCode());
        when(referenceDataService.getCourtRoomOuCode(anyString())).thenReturn(getJsonObjectForCourtRoomRefDataResponse());
        baseStructure = baseStructureConverter.convert(shareResultsMessage);
        assertThat(baseStructure.getId(), is(hearing.getId()));
        assertThat(baseStructure.getCourtCentreWithLJA().getCourtCentre(), is(hearing.getCourtCentre()));

        assertThat(baseStructure.getCourtCentreWithLJA().getCourtHearingLocation(), is(COURT_ROOM_OUCODE_FROM_REFDATA));
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

    @Test
    public void testConverter2() {
        final UUID hearingId = randomUUID();

        final PublicHearingResulted shareResultsMessage = PublicHearingResulted.publicHearingResulted()
                .setHearing(basicShareHearingTemplateWithApplication(hearingId, JurisdictionType.MAGISTRATES))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));

        final Hearing hearing = shareResultsMessage.getHearing();
        final List<HearingDay> hearingDays = hearing.getHearingDays();

        when(referenceDataService.getOrganisationUnit(anyString(), any())).thenReturn(getJsonObjectWithNationalCourtCodeAndOuCode());
        when(referenceDataService.getCourtRoomOuCode(anyString())).thenReturn(getJsonObjectForCourtRoomRefDataResponse());
        baseStructure = baseStructureConverter.convert(shareResultsMessage);
        assertEquals(baseStructure.getId(), hearing.getId());
        assertEquals(baseStructure.getCourtCentreWithLJA().getCourtCentre(), hearing.getCourtCentre());

        assertEquals(COURT_ROOM_OUCODE_FROM_REFDATA, baseStructure.getCourtCentreWithLJA().getCourtHearingLocation());
        assertEquals(baseStructure.getCourtCentreWithLJA().getPsaCode(), valueOf(getJsonObjectWithNationalCourtCodeAndOuCode().getString(FIELD_NATIONAL_COURT_CODE)));

        final List<SessionDay> sessionDays = baseStructure.getSessionDays();
        assertEquals(sessionDays.size(), hearingDays.size());
        for (final SessionDay sessionDay : sessionDays) {
            final Optional<HearingDay> hearingDay = hearingDays.stream().filter(h -> h.getListedDurationMinutes().equals(sessionDay.getListedDurationMinutes())
                    && h.getListingSequence().equals(sessionDay.getListingSequence())
                    && h.getSittingDay().equals(sessionDay.getSittingDay())).findFirst();
            assertFalse(hearingDay.isPresent());
        }
        assertEquals("CC", baseStructure.getSourceType());
    }

    private JsonObject getJsonObjectWithNationalCourtCodeAndOuCode() {
        return Json.createObjectBuilder()
                .add(FIELD_NATIONAL_COURT_CODE, "2574")
                .add(FIELD_OU_CODE, "B01BH00")
                .build();
    }

    private JsonObject getJsonObjectForCourtRoomRefDataResponse() {
        return Json.createObjectBuilder()
                .add("ouCourtRoomCodes", Json.createArrayBuilder().add(COURT_ROOM_OUCODE_FROM_REFDATA).build())
                .build();
    }


}
