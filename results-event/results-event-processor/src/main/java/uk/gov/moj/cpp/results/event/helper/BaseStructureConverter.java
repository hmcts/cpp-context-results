package uk.gov.moj.cpp.results.event.helper;

import static java.lang.Integer.valueOf;
import static java.util.Optional.of;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.core.courts.BaseStructure.baseStructure;
import static uk.gov.justice.core.courts.CourtCentreWithLJA.courtCentreWithLJA;
import static uk.gov.justice.core.courts.SessionDay.sessionDay;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.BaseStructure;
import uk.gov.justice.core.courts.CourtCentreWithLJA;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.SessionDay;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

public class BaseStructureConverter implements Converter<PublicHearingResulted, BaseStructure> {

    private static final String RESULTS_HEARING_RESULTS_ADDED = "results.hearing-results-added";
    private static final String FIELD_NATIONAL_COURT_CODE = "lja";
    private static final String FIELD_OU_CODE = "oucode";


    ReferenceDataService referenceDataService;

    @Inject
    public BaseStructureConverter(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    @Override
    public BaseStructure convert(PublicHearingResulted source) {
        final Hearing hearing = source.getHearing();


        final List<HearingDay> hearingDays = hearing.getHearingDays();
        final List<SessionDay> sessionDays = hearingDays.stream().map(h -> sessionDay()
                .withListedDurationMinutes(h.getListedDurationMinutes())
                .withListingSequence(h.getListingSequence())
                .withSittingDay(getParseDate(h.getSittingDay())).build()).collect(Collectors.toList());

        final JsonObject payload = getJsonObjectFromReferenceDataService(hearing.getCourtCentre().getId());

        final Optional<String> nationalCourtCodeOptional = of(FIELD_NATIONAL_COURT_CODE).filter(payload::containsKey).map(payload::getString);
        final Integer nationalCourtCode =  nationalCourtCodeOptional.isPresent()? valueOf(nationalCourtCodeOptional.get()) :null;
        final String ouCode = of(FIELD_OU_CODE).filter(payload::containsKey).map(payload::getString).orElse(null);

        final CourtCentreWithLJA courtCentreWithLJA = courtCentreWithLJA().withCourtCentre(hearing.getCourtCentre()).withCourtHearingLocation(ouCode).withPsaCode(nationalCourtCode).build();
        return baseStructure().withCourtCentreWithLJA(courtCentreWithLJA).withId(hearing.getId()).withSessionDays(sessionDays).build();
    }

    private ZonedDateTime getParseDate(ZonedDateTime sittingDay) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        final String s = sittingDay.format(formatter);
        return ZonedDateTime.parse(s);
    }

    private JsonObject getJsonObjectFromReferenceDataService(final UUID courtId) {
        final JsonEnvelope event = envelopeFrom(metadataBuilder().withName(RESULTS_HEARING_RESULTS_ADDED).withId(courtId).build(), NULL);
        return referenceDataService.getOrgainsationUnit(courtId.toString(),event);
    }
}
