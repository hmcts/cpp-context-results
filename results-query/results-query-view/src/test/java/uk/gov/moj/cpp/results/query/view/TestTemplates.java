package uk.gov.moj.cpp.results.query.view;

import static java.util.Arrays.asList;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SharedHearing;
import uk.gov.justice.core.courts.SharedVariant;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestTemplates {
    public static List<HearingResultedDocument> templateHearingResultDocuments(final int count) {
        final List<HearingResultedDocument> result = new ArrayList<>();
        for (int done = 0; done < count; done++) {
            result.add(templateHearingResultDocument());
        }
        return result;
    }

    public static HearingResultedDocument templateHearingResultDocument() {
        HearingResultedDocument hearingResultedDocument = (new HearingResultedDocument());
        hearingResultedDocument.setPayload(STRING.next());
        return hearingResultedDocument;
    }

    public static HearingResultsAdded templateHearingResultsAdded() {
        final SharedHearing hearing = SharedHearing.sharedHearing()
                .withProsecutionCases(asList(templateProsecutionCase()))
                .withType(HearingType.hearingType().withDescription(STRING.next()).build())
                .withHearingDays(asList(
                        HearingDay.hearingDay()
                                .withSittingDay(ZonedDateTime.now())
                                .build()
                ))
                .build();
        final ZonedDateTime sharedTime = ZonedDateTime.now();
        final List<SharedVariant> variants = new ArrayList<>();

        HearingResultsAdded result = new HearingResultsAdded(hearing, sharedTime, variants);

        return result;
    }

    public static ProsecutionCase templateProsecutionCase() {
        return ProsecutionCase.prosecutionCase().withProsecutionCaseIdentifier(
                ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build()
        ).withDefendants(asList(templateDefendant()))
                .build();
    }

    public static Defendant templateDefendant() {
        return Defendant.defendant()
                .withId(UUID.randomUUID())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(
                                Person.person()
                                        .withFirstName(STRING.next())
                                        .withLastName(STRING.next())
                                        .build()
                        )
                        .build())
                .build();
    }

}
