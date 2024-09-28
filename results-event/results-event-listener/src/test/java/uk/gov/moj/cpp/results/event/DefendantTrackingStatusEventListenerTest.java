package uk.gov.moj.cpp.results.event;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.results.courts.DefendantTrackingStatusUpdated;
import uk.gov.justice.results.courts.TrackingStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.DefendantTrackingStatusRepository;
import uk.gov.moj.cpp.results.persist.entity.DefendantTrackingStatus;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class DefendantTrackingStatusEventListenerTest {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ");

    private static final UUID DEFENDANT_ID1 = randomUUID();
    private static final UUID OFFENCE_ID1 = randomUUID();
    private static final UUID OFFENCE_ID2 = randomUUID();

    private static final ZonedDateTime EM_OFF1_LAST_MODIFIED_TIME = new UtcClock().now();
    private static final ZonedDateTime EM_OFF2_LAST_MODIFIED_TIME = new UtcClock().now().plusDays(1);

    private static final ZonedDateTime EM_OFF1_LAST_UPDATE_MODIFIED_TIME = new UtcClock().now().plusHours(2);

    private static final ZonedDateTime WOA_OFF1_LAST_MODIFIED_TIME = new UtcClock().now().minusDays(1);
    private static final ZonedDateTime WOA_OFF2_LAST_MODIFIED_TIME = new UtcClock().now().plusDays(2);
    @Mock
    private DefendantTrackingStatusRepository defendantTrackingStatusRepository;

    @InjectMocks
    private DefendantTrackingStatusEventListener defendantTrackingStatusEventListener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Captor
    private ArgumentCaptor<DefendantTrackingStatus> defendantTrackingStatusArgumentCaptor;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSaveEMDefendantTrackingStatusOnly() {

        final List<TrackingStatus> trackingStatuses = new ArrayList<>();
        final TrackingStatus trackingStatus1 = new TrackingStatus(EM_OFF1_LAST_MODIFIED_TIME, true, OFFENCE_ID1, null, null);
        final TrackingStatus trackingStatus2 = new TrackingStatus(EM_OFF2_LAST_MODIFIED_TIME, false, OFFENCE_ID2, null, null);
        trackingStatuses.add(trackingStatus1);
        trackingStatuses.add(trackingStatus2);

        final DefendantTrackingStatusUpdated defendantTrackingStatusUpdated = new DefendantTrackingStatusUpdated(DEFENDANT_ID1, trackingStatuses);

        when(defendantTrackingStatusRepository.findBy(OFFENCE_ID1)).thenReturn(null);

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.events.defendant-tracking-status-updated"),
                objectToJsonObjectConverter.convert(defendantTrackingStatusUpdated));

        defendantTrackingStatusEventListener.saveDefendantTrackingStatus(envelope);

        verify(this.defendantTrackingStatusRepository, times(2)).save(this.defendantTrackingStatusArgumentCaptor.capture());


        final List<DefendantTrackingStatus> defendantTrackingStatusList = defendantTrackingStatusArgumentCaptor.getAllValues();

        assertThat(defendantTrackingStatusList.size(), is(2));

        final DefendantTrackingStatus defendantTrackingStatus1 = defendantTrackingStatusList.get(0);
        assertThat(defendantTrackingStatus1.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatus1.getOffenceId(), is(OFFENCE_ID1));
        assertThat(defendantTrackingStatus1.getEmStatus(), is(true));
        assertThat(defendantTrackingStatus1.getEmLastModifiedTime().format(TIMESTAMP_FORMATTER), is(EM_OFF1_LAST_MODIFIED_TIME.format(TIMESTAMP_FORMATTER)));
        assertThat(defendantTrackingStatus1.getWoaStatus(), is(nullValue()));
        assertThat(defendantTrackingStatus1.getWoaLastModifiedTime(), is(nullValue()));

        final DefendantTrackingStatus defendantTrackingStatus2 = defendantTrackingStatusList.get(1);
        assertThat(defendantTrackingStatus2.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatus2.getOffenceId(), is(OFFENCE_ID2));
        assertThat(defendantTrackingStatus2.getEmStatus(), is(false));
        assertThat(defendantTrackingStatus2.getEmLastModifiedTime().format(TIMESTAMP_FORMATTER), is(EM_OFF2_LAST_MODIFIED_TIME.format(TIMESTAMP_FORMATTER)));
        assertThat(defendantTrackingStatus2.getWoaStatus(), is(nullValue()));
        assertThat(defendantTrackingStatus2.getWoaLastModifiedTime(), is(nullValue()));

    }

    @Test
    public void shouldSaveWADefendantTrackingStatusOnly() {

        final List<TrackingStatus> trackingStatuses = new ArrayList<>();
        final TrackingStatus trackingStatus1 = new TrackingStatus(null, null, OFFENCE_ID1, WOA_OFF1_LAST_MODIFIED_TIME, true);
        final TrackingStatus trackingStatus2 = new TrackingStatus(null, null, OFFENCE_ID2, WOA_OFF2_LAST_MODIFIED_TIME, false);
        trackingStatuses.add(trackingStatus1);
        trackingStatuses.add(trackingStatus2);

        final DefendantTrackingStatusUpdated defendantTrackingStatusUpdated = new DefendantTrackingStatusUpdated(DEFENDANT_ID1, trackingStatuses);

        when(defendantTrackingStatusRepository.findBy(OFFENCE_ID1)).thenReturn(null);

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.events.defendant-tracking-status-updated"),
                objectToJsonObjectConverter.convert(defendantTrackingStatusUpdated));

        defendantTrackingStatusEventListener.saveDefendantTrackingStatus(envelope);

        verify(this.defendantTrackingStatusRepository, times(2)).save(this.defendantTrackingStatusArgumentCaptor.capture());

        final List<DefendantTrackingStatus> defendantTrackingStatusList = defendantTrackingStatusArgumentCaptor.getAllValues();

        assertThat(defendantTrackingStatusList.size(), is(2));

        final DefendantTrackingStatus defendantTrackingStatus1 = defendantTrackingStatusList.get(0);
        assertThat(defendantTrackingStatus1.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatus1.getOffenceId(), is(OFFENCE_ID1));
        assertThat(defendantTrackingStatus1.getEmStatus(), is(nullValue()));
        assertThat(defendantTrackingStatus1.getEmLastModifiedTime(), is(nullValue()));
        assertThat(defendantTrackingStatus1.getWoaStatus(), is(true));
        assertThat(defendantTrackingStatus1.getWoaLastModifiedTime().format(TIMESTAMP_FORMATTER), is(WOA_OFF1_LAST_MODIFIED_TIME.format(TIMESTAMP_FORMATTER)));

        final DefendantTrackingStatus defendantTrackingStatus2 = defendantTrackingStatusList.get(1);
        assertThat(defendantTrackingStatus2.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatus2.getOffenceId(), is(OFFENCE_ID2));
        assertThat(defendantTrackingStatus2.getEmStatus(), is(nullValue()));
        assertThat(defendantTrackingStatus2.getEmLastModifiedTime(), is(nullValue()));
        assertThat(defendantTrackingStatus2.getWoaStatus(), is(false));
        assertThat(defendantTrackingStatus2.getWoaLastModifiedTime().format(TIMESTAMP_FORMATTER), is(WOA_OFF2_LAST_MODIFIED_TIME.format(TIMESTAMP_FORMATTER)));

    }


    @Test
    public void shouldSaveBothEMandWADefendantTrackingStatus() {

        final List<TrackingStatus> trackingStatuses = new ArrayList<>();
        final TrackingStatus trackingStatus1 = new TrackingStatus(EM_OFF1_LAST_MODIFIED_TIME, true, OFFENCE_ID1, WOA_OFF1_LAST_MODIFIED_TIME, true);
        final TrackingStatus trackingStatus2 = new TrackingStatus(EM_OFF2_LAST_MODIFIED_TIME, false, OFFENCE_ID2, WOA_OFF2_LAST_MODIFIED_TIME, false);
        trackingStatuses.add(trackingStatus1);
        trackingStatuses.add(trackingStatus2);

        final DefendantTrackingStatusUpdated defendantTrackingStatusUpdated = new DefendantTrackingStatusUpdated(DEFENDANT_ID1, trackingStatuses);

        when(defendantTrackingStatusRepository.findBy(OFFENCE_ID1)).thenReturn(null);

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.events.defendant-tracking-status-updated"),
                objectToJsonObjectConverter.convert(defendantTrackingStatusUpdated));

        defendantTrackingStatusEventListener.saveDefendantTrackingStatus(envelope);

        verify(this.defendantTrackingStatusRepository, times(2)).save(this.defendantTrackingStatusArgumentCaptor.capture());

        final List<DefendantTrackingStatus> defendantTrackingStatusList = defendantTrackingStatusArgumentCaptor.getAllValues();

        final DefendantTrackingStatus defendantTrackingStatus1 = defendantTrackingStatusList.get(0);
        assertThat(defendantTrackingStatus1.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatus1.getOffenceId(), is(OFFENCE_ID1));
        assertThat(defendantTrackingStatus1.getEmStatus(), is(true));
        assertThat(defendantTrackingStatus1.getEmLastModifiedTime().format(TIMESTAMP_FORMATTER), is(EM_OFF1_LAST_MODIFIED_TIME.format(TIMESTAMP_FORMATTER)));
        assertThat(defendantTrackingStatus1.getWoaStatus(), is(true));
        assertThat(defendantTrackingStatus1.getWoaLastModifiedTime().format(TIMESTAMP_FORMATTER), is(WOA_OFF1_LAST_MODIFIED_TIME.format(TIMESTAMP_FORMATTER)));

        final DefendantTrackingStatus defendantTrackingStatus2 = defendantTrackingStatusList.get(1);
        assertThat(defendantTrackingStatus2.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatus2.getOffenceId(), is(OFFENCE_ID2));
        assertThat(defendantTrackingStatus2.getEmStatus(), is(false));
        assertThat(defendantTrackingStatus2.getEmLastModifiedTime().format(TIMESTAMP_FORMATTER), is(EM_OFF2_LAST_MODIFIED_TIME.format(TIMESTAMP_FORMATTER)));
        assertThat(defendantTrackingStatus2.getWoaStatus(), is(false));
        assertThat(defendantTrackingStatus2.getWoaLastModifiedTime().format(TIMESTAMP_FORMATTER), is(WOA_OFF2_LAST_MODIFIED_TIME.format(TIMESTAMP_FORMATTER)));
    }


    @Test
    public void shouldUpdateDefendantTrackingStatus() {
        final List<TrackingStatus> trackingStatuses = new ArrayList<>();
        final TrackingStatus trackingStatus1 = new TrackingStatus(EM_OFF1_LAST_MODIFIED_TIME, true, OFFENCE_ID1, null, null);
        final TrackingStatus trackingStatus2 = new TrackingStatus(EM_OFF2_LAST_MODIFIED_TIME, false, OFFENCE_ID2, null, null);
        trackingStatuses.add(trackingStatus1);
        trackingStatuses.add(trackingStatus2);

        final DefendantTrackingStatusUpdated defendantTrackingStatusSaved = new DefendantTrackingStatusUpdated(DEFENDANT_ID1, trackingStatuses);

        defendantTrackingStatusEventListener.saveDefendantTrackingStatus(envelopeFrom(metadataWithRandomUUID("results.events.defendant-tracking-status-updated"),
                objectToJsonObjectConverter.convert(defendantTrackingStatusSaved)));

        final List<TrackingStatus> trackingStatusesUpdateOffence1 = new ArrayList<>();
        final TrackingStatus trackingStatusUpdateOffence1 = new TrackingStatus(EM_OFF1_LAST_UPDATE_MODIFIED_TIME, false, OFFENCE_ID1, null, null);
        trackingStatusesUpdateOffence1.add(trackingStatusUpdateOffence1);
        final DefendantTrackingStatusUpdated defendantTrackingStatusUpdated = new DefendantTrackingStatusUpdated(DEFENDANT_ID1, trackingStatusesUpdateOffence1);

        final DefendantTrackingStatus defendantTrackingStatusDefendant1True = defendantTrackingStatus(DEFENDANT_ID1, OFFENCE_ID1, true, EM_OFF1_LAST_MODIFIED_TIME, null, null);
        when(defendantTrackingStatusRepository.findBy(OFFENCE_ID1)).thenReturn(defendantTrackingStatusDefendant1True);

        defendantTrackingStatusEventListener.saveDefendantTrackingStatus(envelopeFrom(metadataWithRandomUUID("results.events.defendant-tracking-status-updated"),
                objectToJsonObjectConverter.convert(defendantTrackingStatusUpdated)));

        verify(this.defendantTrackingStatusRepository, times(3)).save(this.defendantTrackingStatusArgumentCaptor.capture());

        final DefendantTrackingStatus defendantTrackingStatusLastUpdate = defendantTrackingStatusArgumentCaptor.getValue();

        assertThat(defendantTrackingStatusLastUpdate.getDefendantId(), is(DEFENDANT_ID1));
        assertThat(defendantTrackingStatusLastUpdate.getOffenceId(), is(OFFENCE_ID1));
        assertThat(defendantTrackingStatusLastUpdate.getEmStatus(), is(false));
        assertThat(defendantTrackingStatusLastUpdate.getEmLastModifiedTime().format(TIMESTAMP_FORMATTER), is(EM_OFF1_LAST_UPDATE_MODIFIED_TIME.format(TIMESTAMP_FORMATTER)));
        assertThat(defendantTrackingStatusLastUpdate.getWoaStatus(), is(nullValue()));
        assertThat(defendantTrackingStatusLastUpdate.getWoaLastModifiedTime(), is(nullValue()));

    }


    private DefendantTrackingStatus defendantTrackingStatus(final UUID defendantId,
                                                            final UUID offenceId,
                                                            final Boolean emStatus,
                                                            final ZonedDateTime emLastModifiedTime,
                                                            final Boolean waStatus,
                                                            final ZonedDateTime waLastModifiedTime) {
        final DefendantTrackingStatus defendantTrackingStatus = new DefendantTrackingStatus();
        defendantTrackingStatus.setDefendantId(defendantId);
        defendantTrackingStatus.setOffenceId(offenceId);
        defendantTrackingStatus.setEmStatus(emStatus);
        defendantTrackingStatus.setEmLastModifiedTime(emLastModifiedTime);
        defendantTrackingStatus.setWoaStatus(waStatus);
        defendantTrackingStatus.setWoaLastModifiedTime(waLastModifiedTime);
        return defendantTrackingStatus;
    }
}
