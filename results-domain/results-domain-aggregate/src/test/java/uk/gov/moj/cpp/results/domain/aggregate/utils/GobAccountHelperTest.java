package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.util.Collections.EMPTY_MAP;
import static java.util.List.of;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.GobAccountHelper.getOldGobAccounts;

import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GobAccountHelperTest {

    private LinkedList<CorrelationItem> correlationItemList;

    @BeforeEach
    public void setup() {
        correlationItemList = new LinkedList<>();
    }

    @Test
    public void givenOffenceResultedFPInACaseAndGobExists_offenceResultedFPInAnApplicationShouldGetPreviousGobFromCase() {

        final UUID offenceId = randomUUID();
        final UUID hearingId = randomUUID();

        final OffenceResultsDetails caseOffence = getOffenceResultsDetails(offenceId, true);
        final CorrelationItem correlationItemCase = getCorrelation(hearingId, of(caseOffence), "GOB1");
        correlationItemList.add(correlationItemCase);

        final OffenceResultsDetails applicationOffence = getOffenceResultsDetails(offenceId, true);
        final CorrelationItem correlationItemApp = getCorrelation(hearingId, of(applicationOffence), "GOB2");
        correlationItemList.add(correlationItemApp);

        final String oldGobAccount = GobAccountHelper.getOldGobAccount(correlationItemList, correlationItemApp.getAccountCorrelationId(), applicationOffence.getOffenceId(), EMPTY_MAP);
        assertThat(oldGobAccount, is("GOB1"));
    }

    @Test
    public void givenOffenceResultedFPInACaseAndGobExists_offenceResultedFPInAnApp1_offenceResultedFPInAnApp2_ShouldGetPreviousGobFromApp1() {

        final UUID offenceId = randomUUID();
        final UUID hearingId = randomUUID();

        final OffenceResultsDetails caseOffence = getOffenceResultsDetails(offenceId, true);
        correlationItemList.add(getCorrelation(hearingId, List.of(caseOffence), "GOB1"));

        final OffenceResultsDetails application1Offence = getOffenceResultsDetails(offenceId, true);
        final CorrelationItem correlationItemApp = getCorrelation(hearingId, of(application1Offence), "GOB2");
        correlationItemList.add(correlationItemApp);

        final OffenceResultsDetails application2Offence = getOffenceResultsDetails(offenceId, true);
        final CorrelationItem correlationItemApp2 = getCorrelation(hearingId, of(application2Offence), "GOB3");
        correlationItemList.add(correlationItemApp2);

        final String oldGobAccount = GobAccountHelper.getOldGobAccount(correlationItemList, correlationItemApp2.getAccountCorrelationId(), application2Offence.getOffenceId(), EMPTY_MAP);
        assertThat(oldGobAccount, is("GOB2"));
    }

    @Test
    public void givenOffenceResultedFPInACaseAndGobExists_offenceResultedNonFPInAnApp1_offenceResultedFPInAnApp2_ShouldNotGetPreviousGobFromApp1() {

        final UUID offenceId = randomUUID();
        final UUID hearingId = randomUUID();

        final OffenceResultsDetails caseOffence = getOffenceResultsDetails(offenceId, true);
        correlationItemList.add(getCorrelation(hearingId, List.of(caseOffence), "GOB1"));

        final OffenceResultsDetails application1Offence = getOffenceResultsDetails(offenceId, false);
        final CorrelationItem correlationItemApp = getCorrelation(hearingId, of(application1Offence), "GOB2");
        correlationItemList.add(correlationItemApp);

        final OffenceResultsDetails application2Offence = getOffenceResultsDetails(offenceId, true);
        final CorrelationItem correlationItemApp2 = getCorrelation(hearingId, of(application2Offence), "GOB3");
        correlationItemList.add(correlationItemApp2);

        final String oldGobAccount = GobAccountHelper.getOldGobAccount(correlationItemList, correlationItemApp2.getAccountCorrelationId(), application2Offence.getOffenceId(), EMPTY_MAP);
        assertThat(oldGobAccount, is(nullValue()));
    }

    @Test
    public void givenMultipleOffencesResultedFPInASingleHearingAndGobCreated_shouldGetSingleAndMostRecentGobAccountFromPreviousGobs() {

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID hearingId = randomUUID();


        //case level o1, o2, o3 - GOB1
        final List<OffenceResultsDetails> caseOffences = getOffenceResultsDetails(List.of(offenceId1, offenceId2, offenceId3));
        correlationItemList.add(getCorrelation(hearingId, caseOffences, "GOB1"));

        //App1 level o1, o3 - GOB2
        final List<OffenceResultsDetails> application1Offences = getOffenceResultsDetails(List.of(offenceId1, offenceId3));
        correlationItemList.add(getCorrelation(hearingId, application1Offences, "GOB2"));

        //App2 level o2 - GOB3
        final OffenceResultsDetails application2Offence = getOffenceResultsDetails(offenceId2, true);
        final CorrelationItem correlationItemApp2 = getCorrelation(hearingId, of(application2Offence), "GOB3");
        correlationItemList.add(correlationItemApp2);

        final String oldGobAccount = GobAccountHelper.getOldGobAccount(correlationItemList, correlationItemApp2.getAccountCorrelationId(), application2Offence.getOffenceId(), EMPTY_MAP);
        assertThat("should get GobAccount GOB1 from Case", oldGobAccount, is("GOB1"));

        //App3 level o3 - GOB4
        final OffenceResultsDetails application3Offence = getOffenceResultsDetails(offenceId3, true);
        final CorrelationItem correlationItemApp3 = getCorrelation(hearingId, of(application3Offence), "GOB4");
        correlationItemList.add(correlationItemApp3);

        final String oldGobAccountForApp3 = GobAccountHelper.getOldGobAccount(correlationItemList, correlationItemApp3.getAccountCorrelationId(), application3Offence.getOffenceId(), EMPTY_MAP);
        assertThat("should get GobAccount GOB1 from App1", oldGobAccountForApp3, is("GOB2"));

        //App4 level o2, o3 - GOB4
        final List<OffenceResultsDetails> application4Offences = getOffenceResultsDetails(List.of(offenceId2, offenceId3));
        final CorrelationItem correlationItemApp4 = getCorrelation(hearingId, application4Offences, "GOB5");
        correlationItemList.add(correlationItemApp4);

        final List<String> oldGobAccountsForApp4 = getOldGobAccounts(correlationItemList, correlationItemApp4.getAccountCorrelationId(), List.of(offenceId2, offenceId3), EMPTY_MAP);
        assertThat(oldGobAccountsForApp4.size(), is(1));
        assertThat(oldGobAccountsForApp4, contains("GOB4"));
    }

    @Test
    public void givenMultipleOffencesResultedFPInDifferentHearingsAndGobCreated_shouldGetSingleAndMostRecentGobAccountFromPreviousGobs() {

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID hearingId3 = randomUUID();

        //case level o1, o2, o3 - GOB1
        final List<OffenceResultsDetails> caseOffences = getOffenceResultsDetails(List.of(offenceId1, offenceId2, offenceId3));
        correlationItemList.add(getCorrelation(hearingId1, caseOffences, "GOB1"));

        //case amend o1, o3 - GOB2
        final List<OffenceResultsDetails> caseAmendOffences = getOffenceResultsDetails(List.of(offenceId1, offenceId3));
        correlationItemList.add(getCorrelation(hearingId2, caseAmendOffences, "GOB2"));


        //App2 level o3 - GOB4
        final CorrelationItem correlationItemApp2 = getCorrelation(hearingId3, of(getOffenceResultsDetails(offenceId1, true), getOffenceResultsDetails(offenceId2, false),
                getOffenceResultsDetails(offenceId3, true)), "GOB3");
        correlationItemList.add(correlationItemApp2);

        final List<String> oldGobAccountsForApp4 = getOldGobAccounts(correlationItemList, correlationItemApp2.getAccountCorrelationId(), List.of(offenceId1, offenceId2, offenceId3), EMPTY_MAP);
        assertThat(oldGobAccountsForApp4.size(), is(2));
        assertThat(oldGobAccountsForApp4, containsInAnyOrder("GOB1", "GOB2"));
    }

    private static OffenceResultsDetails getOffenceResultsDetails(final UUID offenceId1, final boolean isFinancial) {
        return offenceResultsDetails().withOffenceId(offenceId1)
                .withIsFinancial(isFinancial)
                .withCreatedTime(ZonedDateTime.now())
                .build();
    }

    private static List<OffenceResultsDetails> getOffenceResultsDetails(final List<UUID> offenceIds) {
        return offenceIds.stream()
                .map(offenceId -> getOffenceResultsDetails(offenceId, true))
                .collect(Collectors.toList());
    }

    private static List<ImpositionOffenceDetails> getImpositionOffenceDetails(final List<UUID> offenceIds) {
        return offenceIds.stream()
                .map(offenceId -> ImpositionOffenceDetails.impositionOffenceDetails().withOffenceId(offenceId).build())
                .collect(Collectors.toList());
    }

    private static CorrelationItem getCorrelation(final UUID hearingId, final List<OffenceResultsDetails> caseOffences, final String gobAccount) {
        return CorrelationItem.correlationItem()
                .withAccountCorrelationId(randomUUID())
                .withHearingId(hearingId)
                .withAccountDivisionCode(randomAlphanumeric(8))
                .withCreatedTime(ZonedDateTime.now())
                .withProsecutionCaseReferences(of("CASEURN001"))
                .withAccountNumber(gobAccount)
                .withOffenceResultsDetailsList(caseOffences)
                .build();
    }

}