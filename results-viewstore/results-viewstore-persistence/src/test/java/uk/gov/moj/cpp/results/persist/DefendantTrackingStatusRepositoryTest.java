package uk.gov.moj.cpp.results.persist;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.results.persist.entity.DefendantTrackingStatus;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.runner.RunWith;
import org.junit.Test;

@RunWith(CdiTestRunner.class)
public class DefendantTrackingStatusRepositoryTest extends BaseTransactionalJunit4Test {

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

    @Inject
    private DefendantTrackingStatusRepository defendantTrackingStatusRepository;

    @Override
    public void setUpBefore() {
    }

    @Override
    public void tearDownAfter() {
        List<DefendantTrackingStatus> defendantTrackingStatusList = defendantTrackingStatusRepository.findAll();
        defendantTrackingStatusList.forEach(ir -> defendantTrackingStatusRepository.remove(ir));
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
