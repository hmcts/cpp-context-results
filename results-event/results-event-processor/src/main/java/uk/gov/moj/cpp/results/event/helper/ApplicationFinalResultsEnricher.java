package uk.gov.moj.cpp.results.event.helper;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.core.courts.ApplicationStatus.FINALISED;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.APPLICATION_STATUS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.COURT_APPLICATIONS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.JUDICIAL_RESULTS;

import uk.gov.moj.cpp.results.event.service.ProgressionService;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Enriches the incoming hearing court application results with finalised application results if they are missing.
 * Finalised application results are fetched from the ProgressionService.
 */
public class ApplicationFinalResultsEnricher {
    private static final String HEARING = "hearing";

    @Inject
    private ProgressionService progressionService;

    /**
     * Enriches the incoming hearing court application results with finalised application results from rp
     *
     * @param hearingPayload the incoming hearing payload to enrich
     * @return the enriched hearing
     */
    public JsonObject enrichIfApplicationResultsMissing(final JsonObject hearingPayload) {
        final JsonObject hearing = hearingPayload.getJsonObject(HEARING);
        if (hearing.containsKey(COURT_APPLICATIONS)) {
            return createObjectBuilder(hearingPayload).add(HEARING, enrichHearing(hearing)).build();
        }
        return hearingPayload;
    }

    private JsonObject enrichHearing(final JsonObject incomingHearing) {
        JsonObjectBuilder builder = createObjectBuilder(incomingHearing);

        final JsonArrayBuilder courtApplicationsBuilder = createArrayBuilder();
        final JsonArray courtApplications = incomingHearing.getJsonArray(COURT_APPLICATIONS);
        for (int index = 0; index < courtApplications.size(); index++) {
            final JsonObject application = courtApplications.getJsonObject(index);
            final JsonObjectBuilder applicationBuilder = createObjectBuilder(application);
            final JsonArray judicialResults = application.getJsonArray(JUDICIAL_RESULTS);
            if (judicialResults == null || judicialResults.isEmpty()) {
                final UUID applicationID = UUID.fromString(application.getString(ID));
                getFinalisedApplicationResults(applicationID).ifPresent(results -> applicationBuilder.add(JUDICIAL_RESULTS, results));
            }
            courtApplicationsBuilder.add(applicationBuilder.build());
        }
        return builder.add(COURT_APPLICATIONS, courtApplicationsBuilder).build();
    }

    private Optional<JsonArray> getFinalisedApplicationResults(final UUID applicationID) {
        final Optional<JsonObject> applicationDetails = progressionService.getApplicationDetails(applicationID);
        return applicationDetails.map(details -> {
            final JsonObject courtApplication = applicationDetails.get().getJsonObject("courtApplication");
            if (courtApplication != null && courtApplication.getString(APPLICATION_STATUS, "").equals(FINALISED.toString())) {
                final JsonArrayBuilder judicialResultsBuilder = createArrayBuilder();

                final JsonArray judicialResults = courtApplication.getJsonArray(JUDICIAL_RESULTS);
                if (judicialResults == null || judicialResults.isEmpty()) {
                    return null;
                }
                for (int i = 0; i < judicialResults.size(); i++) {
                    final JsonObject judicialResult = judicialResults.getJsonObject(i);
                    judicialResultsBuilder.add(createObjectBuilder(judicialResult)
                            .remove("amendmentDate")
                            .remove("amendmentReason")
                            .remove("amendmentReasonId")
                            .build());
                }
                return judicialResultsBuilder.build();
            }
            return null;
        });
    }
}
