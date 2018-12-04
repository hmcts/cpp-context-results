package uk.gov.moj.cpp.results.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.domain.event.HearingResultsAdded;
import uk.gov.moj.cpp.results.persist.HearingResultedDocumentRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"unchecked", "squid:S1612"})
@ServiceComponent(EVENT_LISTENER)
public class ResultsEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsEventListener.class);

    @Inject
    private HearingResultedDocumentRepository hearingResultedDocumentRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Transactional
    @Handles("results.hearing-results-added")
    public void hearingResultsAdded(final JsonEnvelope event) {
        Objects.requireNonNull(event.payloadAsJsonObject(), "source");
        final HearingResultsAdded hearingResultsAdded = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), HearingResultsAdded.class);

        final UUID hearingId = hearingResultsAdded.getHearing().getId();
        final List<HearingDay> days = hearingResultsAdded.getHearing().getHearingDays();
        final LocalDate startDate = days.stream().map(day -> day.getSittingDay().toLocalDate()).min((d1, d2) -> d1.compareTo(d2)).orElse(null);
        final LocalDate endDate = days.stream().map(day -> day.getSittingDay().toLocalDate()).max((d1, d2) -> d1.compareTo(d2)).orElse(null);
        hearingResultedDocumentRepository.save(createHearingResultedDocument(event, hearingId, startDate, endDate));
        LOGGER.info("Hearing Event Document successfully stored for hearing id: {}", hearingId);
    }

    private HearingResultedDocument createHearingResultedDocument(JsonEnvelope event, UUID hearingId, LocalDate startDate, LocalDate endDate) {
        final HearingResultedDocument document = new HearingResultedDocument();
        document.setHearingId(hearingId);
        document.setStartDate(startDate);
        document.setEndDate(endDate);
        document.setPayload(event.payloadAsJsonObject().toString());
        return document;
    }
}
