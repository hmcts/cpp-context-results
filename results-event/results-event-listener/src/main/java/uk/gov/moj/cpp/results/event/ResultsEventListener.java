package uk.gov.moj.cpp.results.event;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createReader;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.core.courts.HearingResultsAddedForDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.HearingResultedDocumentRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocumentKey;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unchecked", "squid:S1612"})
@ServiceComponent(EVENT_LISTENER)
public class ResultsEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsEventListener.class);
    private static final String ID = "id";
    private static final String PARENT_APPLICATION_ID = "parentApplicationId";
    private static final String LINKED_CASE_ID = "linkedCaseId";
    private static final String IS_EJECTED = "isEjected";
    private static final String EJECTED = "EJECTED";
    private static final String CASE_STATUS = "caseStatus";
    private static final String PROSECUTION_CASES = "prosecutionCases";
    private static final String COURT_APPLICATIONS = "courtApplications";
    public static final String HEARING = "hearing";

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
        saveHearingResultedDocument(event, hearingId, null, days);
    }

    @Handles("results.events.hearing-results-added-for-day")
    public void hearingResultsAddedForDay(final JsonEnvelope event) {
        final HearingResultsAddedForDay hearingResultsAddedForDay = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), HearingResultsAddedForDay.class);
        LOGGER.info("Hearing Event Document successfully stored for hearing id: {}, hearing day: {}", hearingResultsAddedForDay.getHearing().getId(), hearingResultsAddedForDay.getHearingDay());

        final UUID hearingId = hearingResultsAddedForDay.getHearing().getId();
        final List<HearingDay> days = hearingResultsAddedForDay.getHearing().getHearingDays();
        final LocalDate hearingDay = hearingResultsAddedForDay.getHearingDay();
        saveHearingResultedDocument(event, hearingId, hearingDay, days);
    }

    @Transactional
    @Handles("results.hearing-case-ejected")
    public void hearingCaseEjected(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final String hearingId = payload.getString("hearingId");
        final String caseId = payload.getString("caseId");
        final List<HearingResultedDocument> documents = hearingResultedDocumentRepository.findByHearingId(fromString(hearingId));
        documents.forEach(document -> {
            updateHearingResultPayload(document, caseId, PROSECUTION_CASES);
            updateHearingResultPayload(document, caseId, COURT_APPLICATIONS);
        });
    }

    @Transactional
    @Handles("results.hearing-application-ejected")
    public void hearingApplicationEjected(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final String hearingId = payload.getString("hearingId");
        final String applicationId = payload.getString("applicationId");
        final List<HearingResultedDocument> documents = hearingResultedDocumentRepository.findByHearingId(fromString(hearingId));
        documents.forEach(document -> updateHearingResultPayload(document, applicationId, COURT_APPLICATIONS));

    }

    private void saveHearingResultedDocument(final JsonEnvelope event, final UUID hearingId, final LocalDate hearingDay, final List<HearingDay> days){
        final LocalDate startDate = days.stream().map(day -> day.getSittingDay().toLocalDate()).min((d1, d2) -> d1.compareTo(d2)).orElse(null);
        final LocalDate endDate = days.stream().map(day -> day.getSittingDay().toLocalDate()).max((d1, d2) -> d1.compareTo(d2)).orElse(null);
        final LocalDate calculatedHearingDay = nonNull(hearingDay) ? hearingDay : startDate;
        hearingResultedDocumentRepository.save(createHearingResultedDocument(event, hearingId, calculatedHearingDay, startDate, endDate));
        LOGGER.info("Hearing Event Document successfully stored for hearing id: {}, hearing day: {}", hearingId, calculatedHearingDay);
    }


    private HearingResultedDocument createHearingResultedDocument(JsonEnvelope event, UUID hearingId, LocalDate hearingDay, LocalDate startDate, LocalDate endDate) {
        final HearingResultedDocument document = new HearingResultedDocument();
        document.setId(new HearingResultedDocumentKey(hearingId, hearingDay));
        document.setStartDate(startDate);
        document.setEndDate(endDate);
        document.setPayload(event.payloadAsJsonObject().toString());
        return document;
    }


    @SuppressWarnings({"squid:S1166", "squid:S2139"})
    private void updateHearingResultPayload(final HearingResultedDocument document, final String caseOrApplicationId, final String caseOrApplicationNodeLocation) {
        updateHearingResultPayload(document, caseOrApplicationNodeLocation, (node, childApplicationOrCaseNode) -> {
            if (!childApplicationOrCaseNode.isMissingNode() && childApplicationOrCaseNode.asText().equals(caseOrApplicationId)) {
                node.put(CASE_STATUS, EJECTED);
                node.put(IS_EJECTED, true);
            }
        });
    }

    @SuppressWarnings({"squid:S1166", "squid:S2139"})
    private void updateHearingResultPayload(final HearingResultedDocument document,
                                            final String caseOrApplicationNodeLocation,
                                            final BiConsumer<ObjectNode, JsonNode> biConsumer) {

        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final String payload = document.getPayload();

        try (final JsonReader payloadJsonReader = createReader(new StringReader(payload))) {

            final JsonNode hearingResultNode = mapper.valueToTree(payloadJsonReader.read());
            final JsonNode caseOrCourtNode = hearingResultNode.path(HEARING).path(caseOrApplicationNodeLocation);

            if (!caseOrCourtNode.isMissingNode() && caseOrCourtNode.isArray()) {

                final ArrayNode caseOrCourtArrayNode = (ArrayNode) caseOrCourtNode;

                caseOrCourtArrayNode.forEach(node -> {
                    final JsonNode courtApplicationOrCaseNode = node.get(ID);
                    biConsumer.accept((ObjectNode) node, courtApplicationOrCaseNode);
                    final JsonNode childApplicationOrCaseNode = node.path(PARENT_APPLICATION_ID);
                    biConsumer.accept((ObjectNode) node, childApplicationOrCaseNode);
                    final JsonNode applicationNodeWithLinkedCaseId = node.path(LINKED_CASE_ID);
                    biConsumer.accept((ObjectNode) node, applicationNodeWithLinkedCaseId);
                });

                try {
                    document.setPayload(mapper.readValue(hearingResultNode.toString(), JsonValue.class).toString());
                    hearingResultedDocumentRepository.save(document);
                } catch (IOException e) {
                    LOGGER.error("Hearing Result Document " + hearingResultNode.toString() + " unable to parse: " + e.getMessage(), e.getCause());
                    throw new IllegalStateException("Hearing Result Document " + hearingResultNode.toString() + " unable to parse: " + e.getMessage());
                }
            }
        }
    }
}
