package uk.gov.moj.cpp.results.query.view.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.results.persist.DefendantTrackingStatusRepository;
import uk.gov.moj.cpp.results.persist.entity.DefendantTrackingStatus;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantTrackingStatusServiceTest {
    private static final UUID DEFENDANT_1 = randomUUID();
    private static final UUID DEFENDANT_2 = randomUUID();

    private static final UUID OFFENCE_1 = randomUUID();
    private static final UUID OFFENCE_2 = randomUUID();

    private static final ZonedDateTime EM_LAST_MODIFIED_TIME_1 = new UtcClock().now().minusDays(1).now();
    private static final ZonedDateTime EM_LAST_MODIFIED_TIME_2 = new UtcClock().now().minusDays(2).now();

    private static final ZonedDateTime WA_LAST_MODIFIED_TIME_1 = new UtcClock().now().minusDays(3).now();
    private static final ZonedDateTime WA_LAST_MODIFIED_TIME_2 = new UtcClock().now().minusDays(4).now();

    private static final boolean EM_STATUS_TRUE = true;
    private static final boolean WA_STATUS_FALSE = false;

    @InjectMocks
    private DefendantTrackingStatusService defendantTrackingStatusService;

    @Mock
    private DefendantTrackingStatusRepository defendantTrackingStatusRepository;

    @Test
    public void shouldSearchAndReturnDefendantTrackingStatus() {
        final List<UUID> defendantIds = new ArrayList<>();
        defendantIds.add(DEFENDANT_1);
        defendantIds.add(DEFENDANT_2);

        final List<DefendantTrackingStatus> defendantTrackingStatusList = new ArrayList<>();
        defendantTrackingStatusList.add(getDefendant1TrackingStatus());
        defendantTrackingStatusList.add(getDefendant2TrackingStatus());

        when(defendantTrackingStatusRepository.findActiveDefendantTrackingStatusByDefendantIds(defendantIds)).thenReturn(defendantTrackingStatusList);

        final List<DefendantTrackingStatus> defendantTrackingStatusListFromDB = defendantTrackingStatusService.findDefendantTrackingStatus(defendantIds);
        assertThat(defendantTrackingStatusListFromDB.size(), is(2));

        final DefendantTrackingStatus defendantTrackingStatus1 = defendantTrackingStatusListFromDB.get(0);
        assertThat(defendantTrackingStatus1.getDefendantId(), is(DEFENDANT_1));
        assertThat(defendantTrackingStatus1.getOffenceId(), is(OFFENCE_1));
        assertThat(defendantTrackingStatus1.getEmLastModifiedTime(), is(EM_LAST_MODIFIED_TIME_1));
        assertThat(defendantTrackingStatus1.getEmStatus(), is(EM_STATUS_TRUE));
        assertThat(defendantTrackingStatus1.getWoaLastModifiedTime(), is(WA_LAST_MODIFIED_TIME_1));
        assertThat(defendantTrackingStatus1.getWoaStatus(), is(WA_STATUS_FALSE));


        final DefendantTrackingStatus defendantTrackingStatus2 = defendantTrackingStatusListFromDB.get(1);
        assertThat(defendantTrackingStatus2.getDefendantId(), is(DEFENDANT_2));
        assertThat(defendantTrackingStatus2.getOffenceId(), is(OFFENCE_2));
        assertThat(defendantTrackingStatus2.getEmLastModifiedTime(), is(EM_LAST_MODIFIED_TIME_2));
        assertThat(defendantTrackingStatus2.getEmStatus(), is(EM_STATUS_TRUE));
        assertThat(defendantTrackingStatus2.getWoaLastModifiedTime(), is(WA_LAST_MODIFIED_TIME_2));
        assertThat(defendantTrackingStatus2.getWoaStatus(), is(WA_STATUS_FALSE));

    }

    @Test
    public void shouldSearchAndReturnEmptyListOfDefendantTrackingStatus() {
        final List<UUID> defendantIds = new ArrayList<>();

        final List<DefendantTrackingStatus> defendantTrackingStatusList = new ArrayList<>();

        when(defendantTrackingStatusRepository.findActiveDefendantTrackingStatusByDefendantIds(defendantIds)).thenReturn(defendantTrackingStatusList);

        final List<DefendantTrackingStatus> defendantTrackingStatusListFromDB = defendantTrackingStatusService.findDefendantTrackingStatus(defendantIds);
        assertThat(defendantTrackingStatusListFromDB.size(), is(0));


    }

    private DefendantTrackingStatus getDefendant1TrackingStatus() {
        final DefendantTrackingStatus defendantTrackingStatusEntity = new DefendantTrackingStatus();
        defendantTrackingStatusEntity.setDefendantId(DEFENDANT_1);
        defendantTrackingStatusEntity.setOffenceId(OFFENCE_1);
        defendantTrackingStatusEntity.setEmStatus(EM_STATUS_TRUE);
        defendantTrackingStatusEntity.setEmLastModifiedTime(EM_LAST_MODIFIED_TIME_1);
        defendantTrackingStatusEntity.setWoaStatus(WA_STATUS_FALSE);
        defendantTrackingStatusEntity.setWoaLastModifiedTime(WA_LAST_MODIFIED_TIME_1);

        return defendantTrackingStatusEntity;
    }

    private DefendantTrackingStatus getDefendant2TrackingStatus() {
        final DefendantTrackingStatus defendantTrackingStatusEntity = new DefendantTrackingStatus();
        defendantTrackingStatusEntity.setDefendantId(DEFENDANT_2);
        defendantTrackingStatusEntity.setOffenceId(OFFENCE_2);
        defendantTrackingStatusEntity.setEmStatus(EM_STATUS_TRUE);
        defendantTrackingStatusEntity.setEmLastModifiedTime(EM_LAST_MODIFIED_TIME_2);
        defendantTrackingStatusEntity.setWoaStatus(WA_STATUS_FALSE);
        defendantTrackingStatusEntity.setWoaLastModifiedTime(WA_LAST_MODIFIED_TIME_2);
        return defendantTrackingStatusEntity;
    }

}