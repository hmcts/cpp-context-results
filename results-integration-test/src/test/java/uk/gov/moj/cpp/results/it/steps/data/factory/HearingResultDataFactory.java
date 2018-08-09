package uk.gov.moj.cpp.results.it.steps.data.factory;

import uk.gov.moj.cpp.domains.results.shareResults.Address;
import uk.gov.moj.cpp.domains.results.shareResults.Case;
import uk.gov.moj.cpp.domains.results.shareResults.CourtClerk;
import uk.gov.moj.cpp.domains.results.shareResults.DefenceAdvocate;
import uk.gov.moj.cpp.domains.results.shareResults.Defendant;
import uk.gov.moj.cpp.domains.results.shareResults.Interpreter;
import uk.gov.moj.cpp.domains.results.shareResults.Offence;
import uk.gov.moj.cpp.domains.results.shareResults.Person;
import uk.gov.moj.cpp.domains.results.shareResults.Plea;
import uk.gov.moj.cpp.domains.results.shareResults.Prompt;
import uk.gov.moj.cpp.domains.results.shareResults.SharedResultLine;
import uk.gov.moj.cpp.domains.results.shareResults.Verdict;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_ZONED_DATE_TIME;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.UUID;

public class HearingResultDataFactory {

    private static final UUID USER_ID = UUID.next();

    public static UUID getUserId() {
        return USER_ID;
    }

    public static DefenceAdvocate defenceAdvocateTemplate(UUID defendantId) {
        return DefenceAdvocate.defenceAdvocate()
                .setType("DEFENCEADVOCATE")
                .setFirstName(STRING.next())
                .setLastName(STRING.next())
                .setStatus(STRING.next())
                .setTitle(STRING.next())
                .setDefendantIds(Arrays.asList(defendantId));
    }

    public static SharedResultLine sharedResultLineTemplate(UUID caseId, UUID personId, UUID offenceId, String level) {
        return SharedResultLine.sharedResultLine()
                .setId(randomUUID())
                .setOffenceId(offenceId)
                .setCaseId(caseId)
                .setDefendantId(personId)
                .setLabel(STRING.next())
                .setLevel(level)
                .setLastSharedDateTime(PAST_ZONED_DATE_TIME.next())
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
                ));
    }

    public static Defendant defendantTemplate(UUID caseId, UUID defendantId, UUID offenceId, UUID personId) {
        return Defendant.defendant()
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
                .setCases(asList(
                        caseTemplate(caseId, offenceId)
                ));
    }

    public static Case caseTemplate(UUID caseId, UUID offenceId) {
        return Case.legalCase()
                .setId(caseId)
                .setBailStatus(STRING.next())
                .setUrn(STRING.next())
                .setCustodyTimeLimitDate(PAST_LOCAL_DATE.next())
                .setOffences(asList(offenceTemplate(offenceId)));
    }

    public static Offence offenceTemplate(UUID offenceId) {
        return Offence.offence()
                .setId(offenceId)
                .setCode(STRING.next())
                .setConvictionDate(PAST_LOCAL_DATE.next())
                .setEndDate(PAST_LOCAL_DATE.next())
                .setStartDate(PAST_LOCAL_DATE.next())
                .setWording(STRING.next())
                .setPlea(
                        Plea.plea()
                        .setDate(PAST_LOCAL_DATE.next())
                        .setEnteredHearingId(randomUUID())
                        .setId(randomUUID())
                        .setValue("GUILTY"))
                .setVerdict(
                        Verdict.verdict()
                        .setVerdictCategory(STRING.next())
                        .setVerdictDate(PAST_LOCAL_DATE.next())
                        .setVerdictDescription(STRING.next())
                        .setEnteredHearingId(randomUUID())
                        .setNumberOfJurors(INTEGER.next())
                        .setNumberOfSplitJurors(STRING.next())
                        .setUnanimous(BOOLEAN.next()));
    }
}
