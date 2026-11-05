package uk.gov.moj.cpp.results.persist;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.results.persist.entity.DefendantTrackingStatus;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DefendantTrackingStatusRepositoryTest {

    private static final String PERSISTENCE_UNIT = "results-test-persistence-unit";

    private static final UUID DEFENDANT_ID1 = randomUUID();
    private static final UUID DEFENDANT_ID2 = randomUUID();

    private static final UUID OFFENCE_ID1 = randomUUID();
    private static final UUID OFFENCE_ID2 = randomUUID();
    private static final UUID OFFENCE_ID3 = randomUUID();
    private static final UUID OFFENCE_ID4 = randomUUID();

    private static final ZonedDateTime EM_LAST_MODIFIED_OFF1_DEF1 = now().minusDays(1);
    private static final ZonedDateTime EM_LAST_MODIFIED_OFF2_DEF1 = now().minusDays(2);

    private static final ZonedDateTime EM_LAST_MODIFIED_OFF3_DEF2 = now().minusDays(3);
    private static final ZonedDateTime EM_LAST_MODIFIED_OFF4_DEF2 = now().minusDays(4);

    private static final ZonedDateTime WA_LAST_MODIFIED_OFF1_DEF1 = now().minusDays(5);
    private static final ZonedDateTime WA_LAST_MODIFIED_OFF2_DEF1 = now().minusDays(6);

    private static final ZonedDateTime WA_LAST_MODIFIED_OFF3_DEF2 = now().minusDays(7);
    private static final ZonedDateTime WA_LAST_MODIFIED_OFF4_DEF2 = now().minusDays(8);

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private DefendantTrackingStatusRepository defendantTrackingStatusRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        defendantTrackingStatusRepository = new DefendantTrackingStatusRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(defendantTrackingStatusRepository);
        defendantTrackingStatusRepository.findAll().forEach(defendantTrackingStatusRepository::remove);
    }

    @Test
    public void shouldFindTwoActiveEmDTStatusWhenFindByDefendantIds() {
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID1, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF1_DEF1, WA_LAST_MODIFIED_OFF1_DEF1, true, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID2, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF2_DEF1, WA_LAST_MODIFIED_OFF2_DEF1, false, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID3, DEFENDANT_ID2, EM_LAST_MODIFIED_OFF3_DEF2, WA_LAST_MODIFIED_OFF3_DEF2, false, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID4, DEFENDANT_ID2, EM_LAST_MODIFIED_OFF4_DEF2, WA_LAST_MODIFIED_OFF4_DEF2, true, false));

        final List<UUID> defendantIdList = new ArrayList<>();
        defendantIdList.add(DEFENDANT_ID1);
        defendantIdList.add(DEFENDANT_ID2);

        final List<DefendantTrackingStatus> defendantTrackingStatusList =
                defendantTrackingStatusRepository.findActiveDefendantTrackingStatusByDefendantIds(defendantIdList);
        assertThat(defendantTrackingStatusList, hasSize(2));

        final DefendantTrackingStatus defendantTrackingStatus1 = defendantTrackingStatusList.get(0);
        assertThat(defendantTrackingStatus1.getOffenceId(), is(OFFENCE_ID1));
        assertThat(defendantTrackingStatus1.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatus1.getEmLastModifiedTime(), is(EM_LAST_MODIFIED_OFF1_DEF1));
        assertThat(defendantTrackingStatus1.getEmStatus(), is(true));
        assertThat(defendantTrackingStatus1.getWoaStatus(), is(false));

        final DefendantTrackingStatus defendantTrackingStatus2 = defendantTrackingStatusList.get(1);
        assertThat(defendantTrackingStatus2.getOffenceId(), is(OFFENCE_ID4));
        assertThat(defendantTrackingStatus2.getDefendantId(), is(DEFENDANT_ID2));
        assertThat(defendantTrackingStatus2.getEmLastModifiedTime(), is(EM_LAST_MODIFIED_OFF4_DEF2));
        assertThat(defendantTrackingStatus2.getEmStatus(), is(true));
        assertThat(defendantTrackingStatus1.getWoaStatus(), is(false));
    }

    @Test
    public void shouldFindOneActiveEmDTStatusWhenFindByDefendantIds() {
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID1, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF1_DEF1, WA_LAST_MODIFIED_OFF1_DEF1, true, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID2, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF2_DEF1, WA_LAST_MODIFIED_OFF2_DEF1, false, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID3, DEFENDANT_ID2, EM_LAST_MODIFIED_OFF3_DEF2, WA_LAST_MODIFIED_OFF3_DEF2, false, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID4, DEFENDANT_ID2, EM_LAST_MODIFIED_OFF4_DEF2, WA_LAST_MODIFIED_OFF4_DEF2, false, false));

        final List<UUID> defendantIdList = new ArrayList<>();
        defendantIdList.add(DEFENDANT_ID1);
        defendantIdList.add(DEFENDANT_ID2);

        final List<DefendantTrackingStatus> defendantTrackingStatusList =
                defendantTrackingStatusRepository.findActiveDefendantTrackingStatusByDefendantIds(defendantIdList);
        assertThat(defendantTrackingStatusList, hasSize(1));

        final DefendantTrackingStatus defendantTrackingStatus1 = defendantTrackingStatusList.get(0);
        assertThat(defendantTrackingStatus1.getOffenceId(), is(OFFENCE_ID1));
        assertThat(defendantTrackingStatus1.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatus1.getEmLastModifiedTime(), is(EM_LAST_MODIFIED_OFF1_DEF1));
        assertThat(defendantTrackingStatus1.getEmStatus(), is(true));
        assertThat(defendantTrackingStatus1.getWoaStatus(), is(false));
    }

    @Test
    public void shouldFindNoActiveEmOrWaDTStatusWhenFindByDefendantIds() {
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID1, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF1_DEF1, WA_LAST_MODIFIED_OFF1_DEF1, false, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID2, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF2_DEF1, WA_LAST_MODIFIED_OFF2_DEF1, false, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID3, DEFENDANT_ID2, EM_LAST_MODIFIED_OFF3_DEF2, WA_LAST_MODIFIED_OFF3_DEF2, false, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID4, DEFENDANT_ID2, EM_LAST_MODIFIED_OFF4_DEF2, WA_LAST_MODIFIED_OFF4_DEF2, false, false));

        final List<UUID> defendantIdList = new ArrayList<>();
        defendantIdList.add(DEFENDANT_ID1);
        defendantIdList.add(DEFENDANT_ID2);

        final List<DefendantTrackingStatus> defendantTrackingStatusList =
                defendantTrackingStatusRepository.findActiveDefendantTrackingStatusByDefendantIds(defendantIdList);
        assertThat(defendantTrackingStatusList, hasSize(0));
    }

    @Test
    public void shouldFindOneActiveWaDTStatusWhenFindByDefendantIds() {
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID1, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF1_DEF1, WA_LAST_MODIFIED_OFF1_DEF1, false, true));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID2, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF2_DEF1, WA_LAST_MODIFIED_OFF2_DEF1, false, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID3, DEFENDANT_ID2, EM_LAST_MODIFIED_OFF3_DEF2, WA_LAST_MODIFIED_OFF3_DEF2, false, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID4, DEFENDANT_ID2, EM_LAST_MODIFIED_OFF4_DEF2, WA_LAST_MODIFIED_OFF4_DEF2, false, false));

        final List<UUID> defendantIdList = new ArrayList<>();
        defendantIdList.add(DEFENDANT_ID1);
        defendantIdList.add(DEFENDANT_ID2);

        final List<DefendantTrackingStatus> defendantTrackingStatusList =
                defendantTrackingStatusRepository.findActiveDefendantTrackingStatusByDefendantIds(defendantIdList);
        assertThat(defendantTrackingStatusList, hasSize(1));

        final DefendantTrackingStatus defendantTrackingStatus1 = defendantTrackingStatusList.get(0);
        assertThat(defendantTrackingStatus1.getOffenceId(), is(OFFENCE_ID1));
        assertThat(defendantTrackingStatus1.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatus1.getEmLastModifiedTime(), is(EM_LAST_MODIFIED_OFF1_DEF1));
        assertThat(defendantTrackingStatus1.getEmStatus(), is(false));
        assertThat(defendantTrackingStatus1.getWoaStatus(), is(true));
    }

    @Test
    public void shouldFindTwoActiveWaDTStatusWhenFindByDefendantIds() {
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID1, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF1_DEF1, WA_LAST_MODIFIED_OFF1_DEF1, false, true));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID2, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF2_DEF1, WA_LAST_MODIFIED_OFF2_DEF1, false, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID3, DEFENDANT_ID2, EM_LAST_MODIFIED_OFF3_DEF2, WA_LAST_MODIFIED_OFF3_DEF2, false, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID4, DEFENDANT_ID2, EM_LAST_MODIFIED_OFF4_DEF2, WA_LAST_MODIFIED_OFF4_DEF2, false, true));

        final List<UUID> defendantIdList = new ArrayList<>();
        defendantIdList.add(DEFENDANT_ID1);
        defendantIdList.add(DEFENDANT_ID2);

        final List<DefendantTrackingStatus> defendantTrackingStatusList =
                defendantTrackingStatusRepository.findActiveDefendantTrackingStatusByDefendantIds(defendantIdList);
        assertThat(defendantTrackingStatusList, hasSize(2));

        final DefendantTrackingStatus defendantTrackingStatus1 = defendantTrackingStatusList.get(0);
        assertThat(defendantTrackingStatus1.getOffenceId(), is(OFFENCE_ID1));
        assertThat(defendantTrackingStatus1.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatus1.getEmLastModifiedTime(), is(EM_LAST_MODIFIED_OFF1_DEF1));
        assertThat(defendantTrackingStatus1.getEmStatus(), is(false));
        assertThat(defendantTrackingStatus1.getWoaStatus(), is(true));

        final DefendantTrackingStatus defendantTrackingStatus2 = defendantTrackingStatusList.get(1);
        assertThat(defendantTrackingStatus2.getOffenceId(), is(OFFENCE_ID4));
        assertThat(defendantTrackingStatus2.getDefendantId(), is(DEFENDANT_ID2));
        assertThat(defendantTrackingStatus2.getEmLastModifiedTime(), is(EM_LAST_MODIFIED_OFF4_DEF2));
        assertThat(defendantTrackingStatus2.getEmStatus(), is(false));
        assertThat(defendantTrackingStatus1.getWoaStatus(), is(true));
    }

    @Test
    public void shouldFindTwoActiveEmAndWADTStatusWhenFindByDefendantIds() {
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID1, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF1_DEF1, WA_LAST_MODIFIED_OFF1_DEF1, true, true));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID2, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF2_DEF1, WA_LAST_MODIFIED_OFF2_DEF1, true, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID3, DEFENDANT_ID2, EM_LAST_MODIFIED_OFF3_DEF2, WA_LAST_MODIFIED_OFF3_DEF2, false, false));
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID4, DEFENDANT_ID2, EM_LAST_MODIFIED_OFF4_DEF2, WA_LAST_MODIFIED_OFF4_DEF2, false, true));

        final List<UUID> defendantIdList = new ArrayList<>();
        defendantIdList.add(DEFENDANT_ID1);
        defendantIdList.add(DEFENDANT_ID2);

        final List<DefendantTrackingStatus> defendantTrackingStatusList =
                defendantTrackingStatusRepository.findActiveDefendantTrackingStatusByDefendantIds(defendantIdList);
        assertThat(defendantTrackingStatusList, hasSize(3));

        final DefendantTrackingStatus defendantTrackingStatus1 = defendantTrackingStatusList.get(0);
        assertThat(defendantTrackingStatus1.getOffenceId(), is(OFFENCE_ID1));
        assertThat(defendantTrackingStatus1.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatus1.getEmLastModifiedTime(), is(EM_LAST_MODIFIED_OFF1_DEF1));
        assertThat(defendantTrackingStatus1.getEmStatus(), is(true));
        assertThat(defendantTrackingStatus1.getWoaStatus(), is(true));

        final DefendantTrackingStatus defendantTrackingStatus2 = defendantTrackingStatusList.get(1);
        assertThat(defendantTrackingStatus2.getOffenceId(), is(OFFENCE_ID2));
        assertThat(defendantTrackingStatus2.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatus2.getEmLastModifiedTime(), is(EM_LAST_MODIFIED_OFF2_DEF1));
        assertThat(defendantTrackingStatus2.getEmStatus(), is(true));
        assertThat(defendantTrackingStatus2.getWoaStatus(), is(false));

        final DefendantTrackingStatus defendantTrackingStatus3 = defendantTrackingStatusList.get(2);
        assertThat(defendantTrackingStatus3.getOffenceId(), is(OFFENCE_ID4));
        assertThat(defendantTrackingStatus3.getDefendantId(), is(DEFENDANT_ID2));
        assertThat(defendantTrackingStatus3.getEmLastModifiedTime(), is(EM_LAST_MODIFIED_OFF4_DEF2));
        assertThat(defendantTrackingStatus3.getEmStatus(), is(false));
        assertThat(defendantTrackingStatus3.getWoaStatus(), is(true));
    }

    @Test
    public void shouldFindByOffenceId() {
        defendantTrackingStatusRepository.save(createDefendantTrackingStatus(OFFENCE_ID1, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF1_DEF1, WA_LAST_MODIFIED_OFF1_DEF1, true, false));

        final DefendantTrackingStatus found = defendantTrackingStatusRepository.findBy(OFFENCE_ID1);

        assertThat(found.getOffenceId(), is(OFFENCE_ID1));
        assertThat(found.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(found.getEmStatus(), is(true));
    }

    @Test
    public void shouldRemoveDefendantTrackingStatus() {
        final DefendantTrackingStatus defendantTrackingStatus =
                createDefendantTrackingStatus(OFFENCE_ID1, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF1_DEF1, WA_LAST_MODIFIED_OFF1_DEF1, true, false);
        defendantTrackingStatusRepository.save(defendantTrackingStatus);
        assertThat(defendantTrackingStatusRepository.findAll(), hasSize(1));

        defendantTrackingStatusRepository.remove(defendantTrackingStatus);

        assertThat(defendantTrackingStatusRepository.findAll(), hasSize(0));
    }

    @Test
    public void shouldRemoveAManagedDefendantTrackingStatus() {
        // save() returns the merged, managed instance, so remove() takes the contains==true branch
        final DefendantTrackingStatus managed = defendantTrackingStatusRepository.save(
                createDefendantTrackingStatus(OFFENCE_ID1, DEFENDANT_ID1, EM_LAST_MODIFIED_OFF1_DEF1, WA_LAST_MODIFIED_OFF1_DEF1, true, false));

        defendantTrackingStatusRepository.remove(managed);

        assertThat(defendantTrackingStatusRepository.findAll(), hasSize(0));
    }

    private DefendantTrackingStatus createDefendantTrackingStatus(final UUID offenceId,
                                                                  final UUID defendantId,
                                                                  final ZonedDateTime emLastModifiedTime,
                                                                  final ZonedDateTime waLastModifiedTime,
                                                                  final boolean emStatus,
                                                                  final boolean waStatus) {
        final DefendantTrackingStatus defendantTrackingStatus = new DefendantTrackingStatus();
        defendantTrackingStatus.setOffenceId(offenceId);
        defendantTrackingStatus.setDefendantId(defendantId);
        defendantTrackingStatus.setEmLastModifiedTime(emLastModifiedTime);
        defendantTrackingStatus.setEmStatus(emStatus);
        defendantTrackingStatus.setWoaLastModifiedTime(waLastModifiedTime);
        defendantTrackingStatus.setWoaStatus(waStatus);
        return defendantTrackingStatus;
    }
}
