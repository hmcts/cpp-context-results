package uk.gov.moj.cpp.results.event.processor;

import static java.lang.Boolean.*;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.IndividualDefendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.domains.results.notification.Notification;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.CaseResultDetails;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotification;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequestedV2;
import uk.gov.moj.cpp.results.event.helper.BaseStructureConverter;
import uk.gov.moj.cpp.results.event.helper.CasesConverter;
import uk.gov.moj.cpp.results.event.helper.FixedListComparator;
import uk.gov.moj.cpp.results.event.helper.PoliceEmailHelper;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;
import uk.gov.moj.cpp.results.event.service.ApplicationParameters;
import uk.gov.moj.cpp.results.event.service.CacheService;
import uk.gov.moj.cpp.results.event.service.DocumentGeneratorService;
import uk.gov.moj.cpp.results.event.service.EmailNotification;
import uk.gov.moj.cpp.results.event.service.EventGridService;
import uk.gov.moj.cpp.results.event.service.NotificationNotifyService;
import uk.gov.moj.cpp.results.event.service.ProgressionService;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:S2221", "squid:S1132"})
public class ResultsEventProcessor {
    public static final String DELIMITER = ",";
    public static final String YES = "yes";
    public static final String NO = "no";
    public static final String AMEND_RESHARE1 = "Amend & Reshare";
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsEventProcessor.class);
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String HEARING_IDS = "hearingIds";
    private static final String CASE_ID = "caseId";
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_ID = "id";
    private static final String HEARING = "hearing";
    private static final String RESULTS_COMMAND_UPDATE_DEFENDANT_TRACKING_STATUS = "results.command.update-defendant-tracking-status";
    private static final String RESULTS_COMMAND_HANDLER_CASE_OR_APPLICATION_EJECTED = "results.case-or-application-ejected";
    private static final String CACHE_KEY_SUFFIX = "_result_";
    private static final String CACHE_KEY_SJP_PREFIX = "SJP_";
    private static final String CACHE_KEY_EXTERNAL_PREFIX = "EXT_";
    private static final String CACHE_KEY_INTERNAL_PREFIX = "INT_";
    private static final String SHARED_TIME = "sharedTime";
    private static final String URN = "URN";
    private static final String DEFENDANTS = "Defendants";
    private static final String SUBJECT = "Subject";
    private static final String APPLICATIONS = "Applications";
    private static final String APPLICATION = "Application";
    private static final String AMEND_RESHARE = "Amend_Reshare";
    private static final String COMMON_PLATFORM_URL = "common_platform_url";
    private static final String COMMON_PLATFORM_URL_CAAG = "common_platform_url_caag";
    public static final String PROSECUTION_CASEFILE_CASE_AT_A_GLANCE = "prosecution-casefile/case-at-a-glance/";
    public static final String SPC = " ";

    @Inject
    ReferenceDataService referenceDataService;

    @Inject
    ReferenceCache referenceCache;

    @Inject
    private Sender sender;

    @Inject
    private HearingHelper hearingHelper;

    @Inject
    private CacheService cacheService;

    @Inject
    private EventGridService eventGridService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private NotificationNotifyService notificationNotifyService;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private PoliceEmailHelper policeEmailHelper;


    private static final String RESULTS_NCES_SEND_EMAIL_NOT_FOUND = "results.event.send-nces-email-not-found";

    @Handles("public.hearing.resulted")
    public void hearingResulted(final JsonEnvelope envelope) {

        LOGGER.info("Hearing Resulted Event Received");

        final JsonObject hearingPayload = envelope.payloadAsJsonObject();
        final JsonString sharedTime = hearingPayload.getJsonString(SHARED_TIME);
        final JsonObject transformedHearing = hearingHelper.transformedHearing(hearingPayload.getJsonObject(HEARING));
        final JsonObject externalPayload = createObjectBuilder()
                .add(HEARING, transformedHearing)
                .add(SHARED_TIME, sharedTime)
                .build();
        final String hearingId = transformedHearing.getString(HEARING_ID);

        if (hearingPayload.getJsonObject(HEARING).getBoolean("isSJPHearing", false)) {

            final String cacheKeySjp = CACHE_KEY_SJP_PREFIX + hearingId + CACHE_KEY_SUFFIX;

            try {
                LOGGER.info("Adding external JSON document for hearing {} with sjp key {} to Redis Cache", hearingId, cacheKeySjp);
                cacheService.add(cacheKeySjp, hearingPayload.toString());
            } catch (Exception e) {
                LOGGER.error("Exception caught while attempting to connect to cache service: {} with sjp key {}", e, cacheKeySjp);
            }

            sendEventToGrid(envelope, hearingId, "SJP_Hearing_Resulted");
        } else {
            final String cacheKeyExternal = CACHE_KEY_EXTERNAL_PREFIX + hearingId + CACHE_KEY_SUFFIX;
            final String cacheKeyInternal = CACHE_KEY_INTERNAL_PREFIX + hearingId + CACHE_KEY_SUFFIX;

            try {
                LOGGER.info("Adding external JSON document for hearing {} with external key {} to Redis Cache", hearingId, cacheKeyExternal);
                cacheService.add(cacheKeyExternal, externalPayload.toString());

                LOGGER.info("Adding internal JSON document for hearing {} with internal key {} to Redis Cache", hearingId, cacheKeyInternal);
                cacheService.add(cacheKeyInternal, hearingPayload.toString());
            } catch (Exception e) {
                LOGGER.error("Exception caught while attempting to connect to cache service: {} with LAA key {} {}", e, cacheKeyExternal, cacheKeyInternal);
            }

            sendEventToGrid(envelope, hearingId, "Hearing_Resulted");
        }

        final Envelope<JsonObject> jsonObjectEnvelope = envelop(hearingPayload)
                .withName("results.command.add-hearing-result")
                .withMetadataFrom(envelope);
        sender.sendAsAdmin(jsonObjectEnvelope);
    }

    @Handles("results.hearing-results-added")
    public void hearingResultAdded(final JsonEnvelope resultsAddedEvent) {
        processResultsAdded(resultsAddedEvent, "results.create-results", Optional.empty());

        processDefendantTrackingStatus(resultsAddedEvent);
    }

    @Handles("results.events.hearing-results-added-for-day")
    public void hearingResultAddedForDay(final JsonEnvelope resultsAddedForDayEvent) {
        final LocalDate hearingDay = LocalDate.parse(resultsAddedForDayEvent.payloadAsJsonObject().getString("hearingDay"), ISO_LOCAL_DATE);
        processResultsAdded(resultsAddedForDayEvent, "results.command.create-results-for-day", Optional.of(hearingDay));

        processDefendantTrackingStatus(resultsAddedForDayEvent);
    }

    @Handles("results.event.police-result-generated")
    public void createResult(final JsonEnvelope envelope) {
        LOGGER.debug("results.event.police-result-generated {}", envelope.payload());

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("public.results.police-result-generated")
                .build();

        sender.sendAsAdmin(envelopeFrom(metadata, envelope.payloadAsJsonObject()));
    }

    @Handles("results.event.nces-email-notification-requested")
    public void handleNcesEmailNotificationRequested(final JsonEnvelope envelope) {
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

        final JsonObject requestJson = envelope.payloadAsJsonObject();
        final UUID materialId = UUID.fromString(envelope.payloadAsJsonObject().getString("materialId"));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Nces notification requested payload - {}", requestJson);
        }

        //generate and upload pdf
        documentGeneratorService.generateNcesDocument(sender, envelope, userId, materialId);
    }

    @Handles("results.event.nces-email-notification")
    public void handleSendNcesEmailNotification(final JsonEnvelope envelope) {
        final JsonObject requestJson = envelope.payloadAsJsonObject();
        final NcesEmailNotification ncesEmailNotification = jsonObjectToObjectConverter.convert(requestJson, NcesEmailNotification.class);
        LOGGER.info("Nces email notification payload - {}", requestJson);

        final EmailNotification emailNotification = EmailNotification.emailNotification()
                .withNotificationId(ncesEmailNotification.getNotificationId())
                .withTemplateId(ncesEmailNotification.getTemplateId())
                .withSendToAddress(ncesEmailNotification.getSendToAddress())
                .withSubject(ncesEmailNotification.getSubject())
                .withMaterialUrl(ncesEmailNotification.getMaterialUrl())
                .build();

        //Send Email
        LOGGER.info("send email notification - {}", emailNotification);
        if (isEmpty(emailNotification.getSendToAddress())) {
            final Envelope<JsonObject> jsonObjectEnvelope = envelop(envelope.payloadAsJsonObject())
                    .withName(RESULTS_NCES_SEND_EMAIL_NOT_FOUND)
                    .withMetadataFrom(envelope);
            sender.sendAsAdmin(jsonObjectEnvelope);
        } else {
            notificationNotifyService.sendNcesEmail(emailNotification, envelope);
        }
    }


    @Handles("public.progression.events.case-or-application-ejected")
    public void handleCaseOrApplicationEjected(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (payload.containsKey(HEARING_IDS)) {
            final JsonArray hearingIds = payload.getJsonArray(HEARING_IDS);
            if (payload.containsKey(PROSECUTION_CASE_ID)) {
                final String caseId = payload.getString(PROSECUTION_CASE_ID);
                final JsonObject caseEjectedCommandPayload = createObjectBuilder()
                        .add(HEARING_IDS, hearingIds)
                        .add(CASE_ID, caseId)
                        .build();

                raiseHandlerEvent(envelope, caseEjectedCommandPayload);
            } else {
                final String applicationId = payload.getString(APPLICATION_ID);
                final JsonObject applicationEjectedCommandPayload = createObjectBuilder()
                        .add(HEARING_IDS, hearingIds)
                        .add(APPLICATION_ID, applicationId)
                        .build();

                raiseHandlerEvent(envelope, applicationEjectedCommandPayload);
            }
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("The Payload has been ignored as it does not contain hearing ids : {}", envelope.toObfuscatedDebugString());
            }
        }
    }

    @Handles("results.event.police-notification-requested")
    public void handlePoliceNotificationRequested(final JsonEnvelope envelope) {
        final PoliceNotificationRequested policeNotificationRequested = this.jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PoliceNotificationRequested.class);
        final JsonObject notificationPayload = this.objectToJsonObjectConverter.convert(buildNotification(policeNotificationRequested));
        notificationNotifyService.sendEmailNotification(envelope, notificationPayload);
    }

    @Handles("results.event.police-notification-requested-v2")
    public void handlePoliceNotificationRequestedV2(final JsonEnvelope envelope) {
        final PoliceNotificationRequestedV2 policeNotificationRequestedV2 = this.jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PoliceNotificationRequestedV2.class);
        final JsonObject notificationPayload = this.objectToJsonObjectConverter.convert(buildPoliceNotificationV2(policeNotificationRequestedV2));
        notificationNotifyService.sendEmailNotification(envelope, notificationPayload);
    }

    private void sendEventToGrid(final JsonEnvelope envelope, final String hearingId, final String eventType) {
        final Optional<String> userId = envelope.metadata().userId();
        try {
            LOGGER.info("Adding Hearing Resulted for hearing {} and eventType {} to EventGrid", hearingId, eventType);
            userId.ifPresent(s -> eventGridService.sendHearingResultedEvent(UUID.fromString(s), hearingId, eventType));
        } catch (Exception e) {
            LOGGER.error("Exception caught while attempting to connect to EventGrid: {} for eventType {}", e, eventType);
        }
    }

    /**
     * Processes Results Added, original command without hearingDay or the new command (multi-day).
     *
     * @param envelope    - the event being processed.
     * @param commandName - the name of the command to hit (v1 or v2, where v2 supports multi-day)
     * @param hearingDay  - optional hearingDay, used in the latest command that supports multi day
     *                    hearings
     */
    private void processResultsAdded(final JsonEnvelope envelope, final String commandName, final Optional<LocalDate> hearingDay) {
        final JsonObject hearingResultPayload = envelope.payloadAsJsonObject();

        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(hearingResultPayload, PublicHearingResulted.class);

        final List<CourtApplication> courtApplications = publicHearingResulted.getHearing().getCourtApplications();
        final List<CaseDetails> caseDetails = new CasesConverter(referenceCache, referenceDataService, progressionService).convert(publicHearingResulted);

        if (isNotEmpty(caseDetails)) {
            publicHearingResulted.getHearing().getHearingDays().sort(comparing(HearingDay::getSittingDay).reversed());
            final JsonArrayBuilder caseDetailsJsonArrayBuilder = createArrayBuilder();
            caseDetails.forEach(c -> caseDetailsJsonArrayBuilder.add(objectToJsonObjectConverter.convert(c)));

            final JsonObjectBuilder resultJsonPayload = createObjectBuilder();
            resultJsonPayload.add("session", objectToJsonObjectConverter.convert(new BaseStructureConverter(referenceDataService).convert(publicHearingResulted)));
            resultJsonPayload.add("cases", caseDetailsJsonArrayBuilder.build());
            resultJsonPayload.add("jurisdictionType", publicHearingResulted.getHearing().getJurisdictionType().toString());

            hearingDay.ifPresent(localDate -> resultJsonPayload.add("hearingDay", localDate.toString()));
            if (isNotEmpty(courtApplications)) {
                final JsonArrayBuilder courtApplicationJsonArrayBuilder = createArrayBuilder();
                courtApplications.forEach(c -> courtApplicationJsonArrayBuilder.add(objectToJsonObjectConverter.convert(c)));
                resultJsonPayload.add("courtApplications", courtApplicationJsonArrayBuilder.build());
            }


            final Metadata metadata = metadataFrom(envelope.metadata())
                    .withName(commandName)
                    .build();
            final JsonObject payload = resultJsonPayload.build();
            sender.sendAsAdmin(envelopeFrom(metadata, payload));
        }
    }

    /**
     * Processes Defendant Tracking Status, to track the Electronic Monitoring or Warrant of Arrest for the defendant.
     * Calls "results.command.update-defendant-tracking-status" per defendant
     *
     * @param envelope - the event being processed.
     */
    @SuppressWarnings("squid:S1481")
    private void processDefendantTrackingStatus(final JsonEnvelope envelope) {

        final Hearing resultedHearing = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject().getJsonObject(HEARING),
                Hearing.class);


        ofNullable(resultedHearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty).collect(toList()).forEach(prosecutionCase ->

                prosecutionCase.getDefendants().forEach(defendant -> {
                    final JsonObjectBuilder defendantBuilder = createObjectBuilder().add("defendantId", defendant.getId().toString());
                    final JsonArrayBuilder offencesBuilder = createArrayBuilder();
                    defendant.getOffences().forEach(offence ->
                            offencesBuilder.add(objectToJsonObjectConverter.convert(offence)));
                    defendantBuilder.add("offences", offencesBuilder);

                    final Metadata metadata = metadataFrom(envelope.metadata())
                            .withName(RESULTS_COMMAND_UPDATE_DEFENDANT_TRACKING_STATUS)
                            .build();
                    final JsonObject payload = defendantBuilder.build();
                    sender.sendAsAdmin(envelopeFrom(metadata, payload));

                })
        );

    }

    private Notification buildNotification(final PoliceNotificationRequested policeNotificationRequested) {
        final Map<String, String> personalisationProperties = new HashMap<>();
        personalisationProperties.put(URN, policeNotificationRequested.getUrn());
        personalisationProperties.put(COMMON_PLATFORM_URL, applicationParameters.getCommonPlatformUrl());
        return new Notification(policeNotificationRequested.getNotificationId(), fromString(applicationParameters.getEmailTemplateId()), policeNotificationRequested.getPoliceEmailAddress(), personalisationProperties);
    }

    private Notification buildPoliceNotificationV2(final PoliceNotificationRequestedV2 policeNotificationRequestedV2) {
        final Map<String, String> personalisationProperties = new HashMap<>();
        String emailTemplateId = applicationParameters.getEmailTemplateId();
        personalisationProperties.put(URN, policeNotificationRequestedV2.getUrn());
        personalisationProperties.put(COMMON_PLATFORM_URL, applicationParameters.getCommonPlatformUrl());

        if (isNotEmpty(policeNotificationRequestedV2.getCaseDefendants())) {
            final boolean resultsAmended = !policeNotificationRequestedV2.getAmendReshare().isEmpty();
            final CaseResultDetails caseResultDetails = policeNotificationRequestedV2.getCaseResultDetails();

            if (resultsAmended && nonNull(caseResultDetails) && isNotEmpty(caseResultDetails.getDefendantResultDetails())) {
                personalisationProperties.put(DEFENDANTS, policeEmailHelper.buildDefendantAmendmentDetails(policeNotificationRequestedV2.getCaseResultDetails()));
            } else {
                personalisationProperties.put(DEFENDANTS, getCaseDefendants(policeNotificationRequestedV2.getCaseDefendants(), resultsAmended));
            }

            personalisationProperties.put(AMEND_RESHARE, resultsAmended ? YES : NO);

            final String sortedSubject = FixedListComparator.sortBasedOnFixedList(getCaseSubject(policeNotificationRequestedV2.getCaseDefendants()));
            final String caseApplication = policeNotificationRequestedV2.getApplicationTypeForCase();

            if (isNotEmpty(caseApplication)) {
                if (resultsAmended && nonNull(caseResultDetails) && nonNull(caseResultDetails.getApplicationResultDetails())) {
                    personalisationProperties.put(APPLICATIONS, policeEmailHelper.buildApplicationAmendmentDetails(caseResultDetails.getApplicationResultDetails()));
                } else {
                    personalisationProperties.put(APPLICATIONS, caseApplication);
                }

                emailTemplateId = getPoliceEmailTemplate(true, resultsAmended);
            } else {
                emailTemplateId = getPoliceEmailTemplate(false, resultsAmended);
            }

            personalisationProperties.put(SUBJECT, buildEmailSubject(resultsAmended, policeNotificationRequestedV2.getUrn(), policeNotificationRequestedV2.getDateOfHearing(), caseApplication, sortedSubject, policeNotificationRequestedV2.getCourtCentre()));
            personalisationProperties.put(COMMON_PLATFORM_URL_CAAG, applicationParameters.getCommonPlatformUrl().concat(PROSECUTION_CASEFILE_CASE_AT_A_GLANCE).concat(policeNotificationRequestedV2.getCaseId()));
        }

        return new Notification(policeNotificationRequestedV2.getNotificationId(),
                fromString(emailTemplateId),
                policeNotificationRequestedV2.getPoliceEmailAddress(), personalisationProperties);
    }

    private String getCaseSubject(final List<CaseDefendant> caseDefendants) {
        final String caseSubject = caseDefendants.stream()
                .flatMap(caseDefendant -> caseDefendant.getOffences().stream())
                .flatMap(offenceDetails -> {
                    if (offenceDetails.getJudicialResults() != null) {
                        return offenceDetails.getJudicialResults().stream();
                    } else {
                        return Stream.empty();
                    }
                })
                .filter(judicialResult -> {
                    String policeSubjectLineTitle = judicialResult.getPoliceSubjectLineTitle();
                    String resultText = judicialResult.getResultText();
                    return !(policeSubjectLineTitle != null && policeSubjectLineTitle.equals("Bail Conditions Cancelled")
                            && (resultText != null && (resultText.contains("Domestic Violence case") || resultText.contains("Vulnerable or Intimidated Victim"))));
                })
                .map(JudicialResult::getPoliceSubjectLineTitle)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.joining(DELIMITER));
        return caseSubject.isEmpty() ? "" : caseSubject;
    }


    private String buildEmailSubject(final Boolean resultsAmended, final String urn, final String hearingDate, final String caseApplication, final String subject, final String courtCentre) {
        final String dateTimeFormat = dateTimeFormat(hearingDate);
        final String formattedSubject = subject.replace(DELIMITER, " / ").trim();

        final StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(resultsAmended)) {
            sb.append(AMEND_RESHARE1).append(SPC);
        }

        sb.append(urn).append(SPC);
        sb.append(dateTimeFormat).append(SPC);

        if (Boolean.TRUE.equals(resultsAmended) && nonNull(courtCentre)) {
            sb.append(courtCentre).append(SPC);
        }

        if (isNotEmpty(caseApplication)) {
            sb.append(APPLICATION).append(SPC);
            if (!formattedSubject.isEmpty()) {
                sb.append("/").append(SPC);
            }
        }

        if (!formattedSubject.isEmpty()) {
            sb.append(formattedSubject);
        }


        return sb.toString().trim();

    }

    private String getPoliceEmailTemplate(final Boolean includeApplications, final Boolean includeResultsAmended) {
        if (TRUE.equals(includeApplications)) {
            if (TRUE.equals(includeResultsAmended)) {
                return applicationParameters.getPoliceEmailHearingResultsAmendedWithApplicationTemplateId();
            }
            return applicationParameters.getPoliceEmailHearingResultsWithApplicationTemplateId();
        } else if (TRUE.equals(includeResultsAmended)) {
            return applicationParameters.getPoliceEmailHearingResultsAmendedTemplateId();
        }
        return applicationParameters.getPoliceEmailHearingResultsTemplateId();
    }

    private String dateTimeFormat(final String hearingDate) {
        final DateTimeFormatter dateTimeFormatter = ofPattern("yyyy-MM-dd");
        final LocalDate date = LocalDate.parse(hearingDate.isEmpty() ? LocalDate.now().toString() : hearingDate, dateTimeFormatter);
        final DateTimeFormatter formatter = ofPattern("dd-MM-yyyy");
        return date.format(formatter);
    }

    private String getCaseDefendants(final List<CaseDefendant> caseDefendants, final boolean amended) {
        return caseDefendants.stream()
                .map(CaseDefendant::getIndividualDefendant)
                .map(IndividualDefendant::getPerson)
                .map(individual -> individual.getFirstName().concat(" ")
                        .concat(individual.getLastName()))
                .collect(Collectors.joining(amended ? "<br />": ", "));
    }

    private void raiseHandlerEvent(final JsonEnvelope envelope, final JsonObject commandPayload) {
        final Envelope<JsonObject> jsonObjectEnvelope = envelop(commandPayload)
                .withName(RESULTS_COMMAND_HANDLER_CASE_OR_APPLICATION_EJECTED)
                .withMetadataFrom(envelope);
        sender.sendAsAdmin(jsonObjectEnvelope);
    }
}
