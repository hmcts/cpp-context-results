package uk.gov.moj.cpp.results.test;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_ZONED_DATE_TIME;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.domains.results.shareResults.Variant.variant;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.UUID;

import uk.gov.moj.cpp.domains.results.shareResults.Address;
import uk.gov.moj.cpp.domains.results.shareResults.Attendee;
import uk.gov.moj.cpp.domains.results.shareResults.Case;
import uk.gov.moj.cpp.domains.results.shareResults.CourtCentre;
import uk.gov.moj.cpp.domains.results.shareResults.CourtClerk;
import uk.gov.moj.cpp.domains.results.shareResults.DefenceAdvocate;
import uk.gov.moj.cpp.domains.results.shareResults.Defendant;
import uk.gov.moj.cpp.domains.results.shareResults.Hearing;
import uk.gov.moj.cpp.domains.results.shareResults.Interpreter;
import uk.gov.moj.cpp.domains.results.shareResults.Offence;
import uk.gov.moj.cpp.domains.results.shareResults.Person;
import uk.gov.moj.cpp.domains.results.shareResults.Plea;
import uk.gov.moj.cpp.domains.results.shareResults.Prompt;
import uk.gov.moj.cpp.domains.results.shareResults.ProsecutionAdvocate;
import uk.gov.moj.cpp.domains.results.shareResults.ShareResultsMessage;
import uk.gov.moj.cpp.domains.results.shareResults.SharedResultLine;
import uk.gov.moj.cpp.domains.results.shareResults.VariantKey;
import uk.gov.moj.cpp.domains.results.shareResults.Verdict;

public class TestTemplates {

    private TestTemplates() {

    }

    public static ShareResultsMessage basicShareResultsTemplate() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID personId = randomUUID();
        final UUID hearingId = randomUUID();

        return ShareResultsMessage.shareResultsMessage()
                .setHearing(Hearing.hearing()
                        .setId(hearingId)
                        .setHearingDates(new ArrayList<>(asList(PAST_ZONED_DATE_TIME.next())))
                        .setHearingType(STRING.next())
                        .setStartDateTime(ZonedDateTime.now(ZoneId.of("UTC")))
                        .setCourtCentre(CourtCentre.courtCentre()
                                .setCourtCentreId(randomUUID())
                                .setCourtCentreName(STRING.next())
                                .setCourtRoomId(randomUUID())
                                .setCourtRoomName(STRING.next())
                        )
                        .setAttendees(new ArrayList<>(asList(Attendee.attendee()
                                        .setPersonId(randomUUID())
                                        .setType("JUDGE")
                                        .setFirstName(STRING.next())
                                        .setLastName(STRING.next())
                                        .setTitle(STRING.next()),
                                Attendee.attendee()
                                        .setPersonId(randomUUID())
                                        .setType("COURTCLERK")
                                        .setFirstName(STRING.next())
                                        .setLastName(STRING.next())
                                        .setTitle(STRING.next()),
                                DefenceAdvocate.defenceAdvocate()
                                        .setType("DEFENCEADVOCATE")
                                        .setFirstName(STRING.next())
                                        .setLastName(STRING.next())
                                        .setStatus(STRING.next())
                                        .setTitle(STRING.next())
                                        .setDefendantIds(new ArrayList<>(asList(defendantId))),
                                ProsecutionAdvocate.prosecutionAdvocate()
                                        .setType("PROSECUTIONADVOCATE")
                                        .setFirstName(STRING.next())
                                        .setLastName(STRING.next())
                                        .setTitle(STRING.next())
                                        .setStatus(STRING.next())
                                        .setCaseIds(new ArrayList<>(asList(caseId)))))
                        )
                        .setDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .setId(defendantId)
                                .setDefenceOrganisation(STRING.next())
                                .setInterpreter(Interpreter.interpreter()
                                        .setName(STRING.next())
                                        .setLanguage(STRING.next())
                                )
                                .setPerson(Person.person()
                                        .setId(personId)
                                        .setFirstName(STRING.next())
                                        .setLastName(STRING.next())
                                        .setDateOfBirth(PAST_LOCAL_DATE.next())
                                        .setNationality(STRING.next())
                                        .setGender(STRING.next())
                                        .setAddress(Address.address()
                                                .setAddress1(STRING.next())
                                                .setAddress2(STRING.next())
                                                .setAddress3(STRING.next())
                                                .setAddress4(STRING.next())
                                                .setPostCode(STRING.next())
                                        )
                                        .setEmail(STRING.next())
                                        .setFax(STRING.next())
                                        .setHomeTelephone(STRING.next())
                                        .setMobile(STRING.next())
                                        .setWorkTelephone(STRING.next())
                                )
                                .setCases(new ArrayList<>(asList(Case.legalCase()
                                        .setId(caseId)
                                        .setBailStatus(STRING.next())
                                        .setUrn(STRING.next())
                                        .setCustodyTimeLimitDate(PAST_LOCAL_DATE.next())
                                        .setOffences(new ArrayList<>(asList(Offence.offence()
                                                .setId(offenceId)
                                                .setCode(STRING.next())
                                                .setConvictionDate(PAST_LOCAL_DATE.next())
                                                .setEndDate(PAST_LOCAL_DATE.next())
                                                .setStartDate(PAST_LOCAL_DATE.next())
                                                .setWording(STRING.next())
                                                .setPlea(Plea.plea()
                                                        .setDate(PAST_LOCAL_DATE.next())
                                                        .setEnteredHearingId(randomUUID())
                                                        .setId(randomUUID())
                                                        .setValue("GUILTY"))
                                                .setVerdict(Verdict.verdict()
                                                        .setVerdictDescription(STRING.next())
                                                        .setVerdictCategory(STRING.next())
                                                        .setNumberOfJurors(INTEGER.next())
                                                        .setVerdictDate(PAST_LOCAL_DATE.next())
                                                        .setNumberOfSplitJurors(STRING.next())
                                                        .setUnanimous(BOOLEAN.next())
                                                        .setEnteredHearingId(randomUUID())
                                                )
                                        ))))
                                ))
                        )))
                        .setSharedResultLines(new ArrayList<>(asList(
                                SharedResultLine.sharedResultLine()
                                        .setId(randomUUID())
                                        .setOffenceId(offenceId)
                                        .setCaseId(caseId)
                                        .setDefendantId(defendantId)
                                        .setLabel(STRING.next())
                                        .setLevel("OFFENCE")
                                        .setLastSharedDateTime(PAST_ZONED_DATE_TIME.next().withZoneSameInstant(ZoneId.of("UTC")))
                                        .setOrderedDate(LocalDate.now())
                                        .setCourtClerk(CourtClerk.courtClerk()
                                                .setId(randomUUID())
                                                .setFirstName(STRING.next())
                                                .setLastName(STRING.next()))
                                        .setPrompts(asList(
                                                Prompt.prompt()
                                                        .setId(randomUUID())
                                                        .setLabel(STRING.next())
                                                        .setValue(STRING.next())
                                        )),
                                SharedResultLine.sharedResultLine()
                                        .setId(randomUUID())
                                        .setOffenceId(offenceId)
                                        .setCaseId(caseId)
                                        .setDefendantId(defendantId)
                                        .setLabel(STRING.next())
                                        .setLevel("DEFENDANT")
                                        .setLastSharedDateTime(PAST_ZONED_DATE_TIME.next().withZoneSameInstant(ZoneId.of("UTC")))
                                        .setOrderedDate(LocalDate.now())
                                        .setCourtClerk(CourtClerk.courtClerk()
                                                .setId(randomUUID())
                                                .setFirstName(STRING.next())
                                                .setLastName(STRING.next()))
                                        .setPrompts(asList(
                                                Prompt.prompt()
                                                        .setId(randomUUID())
                                                        .setLabel(STRING.next())
                                                        .setValue(STRING.next())
                                        )),
                                SharedResultLine.sharedResultLine()
                                        .setId(randomUUID())
                                        .setOffenceId(offenceId)
                                        .setCaseId(caseId)
                                        .setDefendantId(defendantId)
                                        .setLabel(STRING.next())
                                        .setLevel("CASE")
                                        .setLastSharedDateTime(PAST_ZONED_DATE_TIME.next().withZoneSameInstant(ZoneId.of("UTC")))
                                        .setOrderedDate(LocalDate.now())
                                        .setCourtClerk(CourtClerk.courtClerk()
                                                .setId(randomUUID())
                                                .setFirstName(STRING.next())
                                                .setLastName(STRING.next()))
                                        .setPrompts(asList(
                                                Prompt.prompt()
                                                        .setId(randomUUID())
                                                        .setLabel(STRING.next())
                                                        .setValue(STRING.next())
                                        ))))
                        ))
                .setVariants(new ArrayList<>(asList(
                        variant().setKey(VariantKey.variantKey()
                        .setDefendantId(defendantId)
                        .setHearingId(hearingId)
                        .setNowsTypeId(randomUUID())
                        .setUsergroups(new ArrayList<>(asList("Court Clerks", "Listings Officers", "Prison Admin"))))
                        .setDescription("Imprisonment Order")
                        .setMaterialId(randomUUID())
                        .setTemplateName("SingleTemplate")
                        .setStatus("BUILDING")
                )))
                .setSharedTime(ZonedDateTime.now(ZoneId.of("UTC")));
    }
}
