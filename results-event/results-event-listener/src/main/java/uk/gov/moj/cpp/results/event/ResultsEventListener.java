package uk.gov.moj.cpp.results.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.minidev.json.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.HearingResultedDocumentRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@SuppressWarnings({"unchecked", "squid:S1612"})
@ServiceComponent(EVENT_LISTENER)
public class ResultsEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsEventListener.class);
    private static final String ID = "id";
    private static final String PARENT_APPLICATION_ID = "parentApplicationId";
    private static final String LINKED_CASE_ID = "linkedCaseId";
    private static final String IS_EJECTED = "isEjected";
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
        final LocalDate startDate = days.stream().map(day -> day.getSittingDay().toLocalDate()).min((d1, d2) -> d1.compareTo(d2)).orElse(null);
        final LocalDate endDate = days.stream().map(day -> day.getSittingDay().toLocalDate()).max((d1, d2) -> d1.compareTo(d2)).orElse(null);
        hearingResultedDocumentRepository.save(createHearingResultedDocument(event, hearingId, startDate, endDate));
        LOGGER.info("Hearing Event Document successfully stored for hearing id: {}", hearingId);
    }

    @Transactional
    @Handles("results.hearing-case-ejected")
    public void hearingCaseEjected(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final String hearingId = payload.getString("hearingId");
        final String caseId = payload.getString("caseId");
        final HearingResultedDocument document = hearingResultedDocumentRepository.findBy(fromString(hearingId));
        updateHearingResultPayload(document, caseId, PROSECUTION_CASES);
        updateHearingResultPayload(document, caseId, COURT_APPLICATIONS);
    }

    @Transactional
    @Handles("results.hearing-application-ejected")
    public void hearingApplicationEjected(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final String hearingId = payload.getString("hearingId");
        final String applicationId = payload.getString("applicationId");
        final HearingResultedDocument document = hearingResultedDocumentRepository.findBy(fromString(hearingId));
        updateHearingResultPayload(document, applicationId, COURT_APPLICATIONS);
    }

    private HearingResultedDocument createHearingResultedDocument(JsonEnvelope event, UUID hearingId, LocalDate startDate, LocalDate endDate) {
        final HearingResultedDocument document = new HearingResultedDocument();
        document.setHearingId(hearingId);
        document.setStartDate(startDate);
        document.setEndDate(endDate);
        document.setPayload(event.payloadAsJsonObject().toString());
        return document;
    }


    @SuppressWarnings({"squid:S1166","squid:S2139"})
    private void updateHearingResultPayload(final HearingResultedDocument document, final String caseOrApplicationId, final String caseOrApplicationNodeLocation) {
        updateHearingResultPayload(document,  caseOrApplicationNodeLocation, (node, childApplicationOrCaseNode) -> {
            if (!childApplicationOrCaseNode.isMissingNode() && childApplicationOrCaseNode.asText().equals(caseOrApplicationId)) {
                node.put(IS_EJECTED, true);
            }
        });
    }

    @SuppressWarnings({"squid:S1166","squid:S2139"})
    private void updateHearingResultPayload(final HearingResultedDocument document, final String caseOrApplicationNodeLocation, final BiConsumer<ObjectNode, JsonNode> biConsumer) {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final String payload = document.getPayload();
        final JsonNode hearingResultNode = mapper.valueToTree(JSONValue.parse(payload));
        final JsonNode caseOrCourtNode = hearingResultNode.path(HEARING).path(caseOrApplicationNodeLocation);
        if(!caseOrCourtNode.isMissingNode() && caseOrCourtNode.isArray()) {
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
