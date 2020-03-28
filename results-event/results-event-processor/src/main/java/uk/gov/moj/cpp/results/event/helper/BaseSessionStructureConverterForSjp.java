package uk.gov.moj.cpp.results.event.helper;

import static java.lang.Integer.parseInt;
import static java.util.UUID.fromString;
import static uk.gov.justice.core.courts.BaseStructure.baseStructure;
import static uk.gov.justice.core.courts.CourtCentreWithLJA.courtCentreWithLJA;
import static uk.gov.justice.core.courts.SessionDay.sessionDay;

import uk.gov.justice.core.courts.BaseStructure;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtCentreWithLJA;
import uk.gov.justice.core.courts.SessionDay;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.sjp.results.BaseSessionStructure;
import uk.gov.justice.sjp.results.PublicSjpResulted;
import uk.gov.justice.sjp.results.SessionLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BaseSessionStructureConverterForSjp implements Converter<PublicSjpResulted, BaseStructure> {


    @Override
    public BaseStructure convert(final PublicSjpResulted source) {

        final BaseSessionStructure baseSessionStructure = source.getSession();

        final UUID id = baseSessionStructure.getSessionId();
        SessionLocation sessionLocation = baseSessionStructure.getSessionLocation();
        SessionDay sessionDay = sessionDay()
                .withSittingDay(baseSessionStructure.getDateAndTimeOfSession())
                //.withListingSequence() //Not Available
                .withListedDurationMinutes(0)
                .build();

        final List<SessionDay> sessionDays = new ArrayList<>();
        sessionDays.add(sessionDay);

        final CourtCentre courtCentre =
                CourtCentre.courtCentre().withId(sessionLocation.getCourtId())
                        .withName(sessionLocation.getName())
                        .withRoomId(null != sessionLocation.getRoomId()? fromString(sessionLocation.getRoomId()): null)
                        .withRoomName(sessionLocation.getRoomName())
                        // .withWelshName("")  // Not available
                        // .withWelshRoomName("") //Not available
                        .withAddress(sessionLocation.getAddress())
                        .build();

        final CourtCentreWithLJA courtCentreWithLJA =  courtCentreWithLJA().withCourtCentre(courtCentre).withCourtHearingLocation(baseSessionStructure.getOuCode()).withPsaCode(null != sessionLocation.getLja()? parseInt(sessionLocation.getLja()): null).build();

        return baseStructure().withCourtCentreWithLJA(courtCentreWithLJA)
                .withId(id)
                .withSessionDays(sessionDays)
                .build();
    }
}
