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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    @Test
    public void givenCaseOffencesResultedWithFPInDifferentHearingsAndGobAccountsCreated_whenTheGetGobAccountsResolvedUsingHearingId_shouldGetMostRecentGobAccountForEachPreviousHearing() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID offenceId4 = randomUUID();

        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID hearingId3 = randomUUID();

        //hearing1 case  o1, o2 - GOB1 - C1 O1, O2, O3(Adj), O4(Adj)  [O1, O2][H1] GOB1
        final List<OffenceResultsDetails> caseOffences = getOffenceResultsDetails(List.of(offenceId1, offenceId2));
        correlationItemList.add(getCorrelation(hearingId1, caseOffences, "GOB1"));

        //hearing1 case amend o1 - GOB2 - C1 O1, O2 [O1][H1] GOB2    <-- Only O1 amended
        final List<OffenceResultsDetails> caseAmendOffenceId1 = getOffenceResultsDetails(List.of(offenceId1));
        correlationItemList.add(getCorrelation(hearingId1, caseAmendOffenceId1, "GOB2"));

        //hearing1 case amend o2 - GOB3 - C1 O1, O2  [O2][H1] GOB3    <-- Only O2 amended
        final List<OffenceResultsDetails> caseAmendOffenceId2 = getOffenceResultsDetails(List.of(offenceId2));
        correlationItemList.add(getCorrelation(hearingId1, caseAmendOffenceId2, "GOB3"));

        //hearing2 case o3, o4 - GOB4 - C1 O3, O4 [O3, O4][H2] GOB4 <-- Adjourned hearing
        final List<OffenceResultsDetails> hearing2caseOffences = getOffenceResultsDetails(List.of(offenceId3, offenceId4));
        correlationItemList.add(getCorrelation(hearingId2, hearing2caseOffences, "GOB4"));

        //hearing2 case amend o4 - GOB5 - C1 O3, O4 [O4][H2] GOB5 <-- Adjourned hearing (Only O4 amended)
        final List<OffenceResultsDetails> hearing2caseOffencesAmend = getOffenceResultsDetails(List.of(offenceId4));
        correlationItemList.add(getCorrelation(hearingId2, hearing2caseOffencesAmend, "GOB5"));

//        POST-2390:
//        App O1, O2, O3, O4      [O1, O2, O3, O4][H3] GOB6  <-- All offences are included post-2390
        final CorrelationItem correlationItemsAllOffences = getCorrelation(hearingId3, of(getOffenceResultsDetails(offenceId1, true),
                getOffenceResultsDetails(offenceId2, true),
                getOffenceResultsDetails(offenceId3, true),
                getOffenceResultsDetails(offenceId4, true)), "GOB6");
        correlationItemList.add(correlationItemsAllOffences);

        final List<String> oldGobAccountsForApp4 = getOldGobAccounts(correlationItemList, correlationItemsAllOffences.getAccountCorrelationId(), List.of(offenceId1, offenceId2, offenceId3, offenceId4), EMPTY_MAP);
        assertThat(oldGobAccountsForApp4.size(), is(2));
        assertThat(oldGobAccountsForApp4, containsInAnyOrder("GOB3", "GOB5"));
    }

    @Test
    public void hasPreviousCorrelationTrueForApplicationSameHearing() {
        final UUID currentAccountCorrelationId = randomUUID();
        final UUID previousAccountCorrelationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();

        final OffenceResultsDetails prevOffenceWithApp = offenceResultsDetails()
                .withOffenceId(offenceId)
                .withIsFinancial(true)
                .withApplicationId(applicationId)
                .withCreatedTime(ZonedDateTime.now())
                .build();

        final CorrelationItem previousCorrelation = CorrelationItem.correlationItem()
                .withAccountCorrelationId(previousAccountCorrelationId)
                .withHearingId(hearingId)
                .withAccountDivisionCode("DIV")
                .withAccountNumber("GOBAC1")
                .withCreatedTime(ZonedDateTime.now().minusDays(1))
                .withProsecutionCaseReferences(of("CASEREF"))
                .withOffenceResultsDetailsList(of(prevOffenceWithApp))
                .build();

        final LinkedList<CorrelationItem> correlationItemList = new LinkedList<>();
        correlationItemList.add(previousCorrelation);

        final OffenceResultsDetails prevAppResult = offenceResultsDetails()
                .withApplicationId(applicationId)
                .withApplicationType("APPEAL")
                .withResultCode("AACA")
                .withCreatedTime(ZonedDateTime.now().minusDays(2))
                .build();

        final Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails = new HashMap<>();
        applicationResultsDetails.put(applicationId, List.of(prevAppResult));

        final Boolean result = GobAccountHelper.hasPreviousCorrelation(correlationItemList, currentAccountCorrelationId, List.of(offenceId), applicationResultsDetails, hearingId);

        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnPreviousCorrelationTrueForSameHearing() {
        final UUID currentAccountCorrelationId = randomUUID();
        final UUID previousAccountCorrelationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();

        final CorrelationItem previousCorrelation = CorrelationItem.correlationItem()
                .withAccountCorrelationId(previousAccountCorrelationId)
                .withHearingId(hearingId)
                .withAccountDivisionCode("DIV01")
                .withAccountNumber("GOBAccount")
                .withCreatedTime(ZonedDateTime.now().minusDays(1))
                .withProsecutionCaseReferences(of("CASEREF1"))
                .withOffenceResultsDetailsList(of(getOffenceResultsDetails(offenceId, true)))
                .build();

        final LinkedList<CorrelationItem> correlationItemList = new LinkedList<>();
        correlationItemList.add(previousCorrelation);

        final Boolean result = GobAccountHelper.hasPreviousCorrelation(correlationItemList, currentAccountCorrelationId, List.of(offenceId), Collections.emptyMap(), hearingId);

        assertThat(result, is(true));
    }

    @Test
    public void shouldNotReturnPreviousCorrelationFalseForDifferentHearing() {
        final UUID currentAccountCorrelationId = randomUUID();
        final UUID previousAccountCorrelationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID offenceId = randomUUID();

        final CorrelationItem previousCorrelation = CorrelationItem.correlationItem()
                .withAccountCorrelationId(previousAccountCorrelationId)
                .withHearingId(hearingId)
                .withAccountDivisionCode("DIV01")
                .withAccountNumber("GOBAccount")
                .withCreatedTime(ZonedDateTime.now().minusDays(1))
                .withProsecutionCaseReferences(of("CASEREF1"))
                .withOffenceResultsDetailsList(of(getOffenceResultsDetails(offenceId, true)))
                .build();

        final LinkedList<CorrelationItem> correlationItemList = new LinkedList<>();
        correlationItemList.add(previousCorrelation);

        final Boolean result = GobAccountHelper.hasPreviousCorrelation(correlationItemList, currentAccountCorrelationId, List.of(offenceId), Collections.emptyMap(), hearingId1);

        assertThat(result, is(false));
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