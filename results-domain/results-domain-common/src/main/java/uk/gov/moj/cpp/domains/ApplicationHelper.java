package uk.gov.moj.cpp.domains;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.domains.HearingHelper.filterJudicialResults;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.APPLICANT;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.APPLICATION_DECISION_SOUGHT_BY_DATE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.APPLICATION_OUTCOME;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.APPLICATION_PARTICULARS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.APPLICATION_RECEIVED_DATE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.APPLICATION_REFERENCE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.APPLICATION_STATUS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.BREACHED_ORDER;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.BREACHED_ORDER_DATE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.COURT_APPLICATION_PAYMENT;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.LINKED_CASE_ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ORDERING_COURT;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.OUT_OF_TIME_REASONS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.PARENT_APPLICATION_ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.REMOVAL_REASON;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.RESPONDENTS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.TYPE;

import java.util.stream.IntStream;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:MethodCyclomaticComplexity"})
public class ApplicationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationHelper.class);

    private ApplicationHelper() {
    }

    public static JsonArray transformApplications(final JsonArray applications) {

        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();

        IntStream.range(
                0,
                applications.size()
        ).mapToObj(
                index -> transformApplication(applications.getJsonObject(index))
        ).forEach(transformedPayloadObjectBuilder::add);

        return transformedPayloadObjectBuilder.build();
    }

    private static JsonObjectBuilder transformApplication(final JsonObject application) {
        LOGGER.info("Transforming Application {}", application.getString("id"));

        final JsonObjectBuilder transformApplicationBuilder = createObjectBuilder()
                .add(ID, application.getString(ID))
                .add(TYPE, application.getJsonObject(TYPE))
                .add(APPLICATION_RECEIVED_DATE, application.getString(APPLICATION_RECEIVED_DATE))
                .add(APPLICANT, application.getJsonObject(APPLICANT))
                .add(APPLICATION_STATUS, application.getString(APPLICATION_STATUS));

        if (application.containsKey(APPLICATION_REFERENCE)) {
            transformApplicationBuilder.add(APPLICATION_REFERENCE, application.getString(APPLICATION_REFERENCE));
        }

        if (application.containsKey(RESPONDENTS)) {
            transformApplicationBuilder.add(RESPONDENTS, application.getJsonArray(RESPONDENTS));
        }

        if (application.containsKey(APPLICATION_OUTCOME)) {
            transformApplicationBuilder.add(APPLICATION_OUTCOME, application.getJsonObject(APPLICATION_OUTCOME));
        }

        if (application.containsKey(LINKED_CASE_ID)) {
            transformApplicationBuilder.add(LINKED_CASE_ID, application.getString(LINKED_CASE_ID));
        }

        if (application.containsKey(PARENT_APPLICATION_ID)) {
            transformApplicationBuilder.add(PARENT_APPLICATION_ID, application.getString(PARENT_APPLICATION_ID));
        }

        if (application.containsKey(APPLICATION_PARTICULARS)) {
            transformApplicationBuilder.add(APPLICATION_PARTICULARS, application.getString(APPLICATION_PARTICULARS));
        }

        if (application.containsKey(COURT_APPLICATION_PAYMENT)) {
            transformApplicationBuilder.add(COURT_APPLICATION_PAYMENT, application.getJsonObject(COURT_APPLICATION_PAYMENT));
        }

        if (application.containsKey(APPLICATION_DECISION_SOUGHT_BY_DATE)) {
            transformApplicationBuilder.add(APPLICATION_DECISION_SOUGHT_BY_DATE, application.getString(APPLICATION_DECISION_SOUGHT_BY_DATE));
        }

        if (application.containsKey(OUT_OF_TIME_REASONS)) {
            transformApplicationBuilder.add(OUT_OF_TIME_REASONS, application.getString(OUT_OF_TIME_REASONS));
        }

        if (application.containsKey(BREACHED_ORDER)) {
            transformApplicationBuilder.add(BREACHED_ORDER, application.getString(BREACHED_ORDER));
        }

        if (application.containsKey(BREACHED_ORDER_DATE)) {
            transformApplicationBuilder.add(BREACHED_ORDER_DATE, application.getString(BREACHED_ORDER_DATE));
        }

        if (application.containsKey(ORDERING_COURT)) {
            transformApplicationBuilder.add(ORDERING_COURT, application.getJsonObject(ORDERING_COURT));
        }

        if (application.containsKey(REMOVAL_REASON)) {
            transformApplicationBuilder.add(REMOVAL_REASON, application.getString(REMOVAL_REASON));
        }

        if (application.containsKey(JUDICIAL_RESULTS)) {
            transformApplicationBuilder.add(JUDICIAL_RESULTS, filterJudicialResults(application.getJsonArray(JUDICIAL_RESULTS)));
        }

        return transformApplicationBuilder;
    }
}
