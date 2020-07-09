package uk.gov.moj.cpp.domains;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.domains.HearingHelper.filterJudicialResults;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ALLOCATION_DECISION;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ALLOCATION_DECISION_DATE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.AQUITTAL_DATE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ARREST_DATE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.CHARGE_DATE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.CONVICTION_DATE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.COUNT;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.COURT_INDICATED_SENTENCE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.CUSTODY_TIME_LIMIT;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.DATE_OF_INFORMATION;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.END_DATE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.INDICATED_PLEA;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.INDICATED_PLEA_DATE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.INDICATED_PLEA_VALUE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.IS_DISCONTINUED;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.IS_INTRODUCEAFTERINITIALPROCEEDINGS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.LAA_APPLN_REFERENCE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.MODE_OF_TRIAL;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.MOT_REASON_CODE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.MOT_REASON_DESCRIPTION;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.MOT_REASON_ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.NOTIFIED_PLEA;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.OFFENCE_CODE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.OFFENCE_DEFINITION_ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.OFFENCE_FACTS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.OFFENCE_LEGISLATION;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.OFFENCE_LEGISLATION_WELSH;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.OFFENCE_TITLE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.OFFENCE_TITLE_WELSH;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ORDER_INDEX;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ORIGINATING_HEARING_ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.PLEA;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.PROCEEDINGS_CONCLUDED;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.SEQUENCE_NUMBER;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.SOURCE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.START_DATE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.VERDICT;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.VICTIMS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.WORDING;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.WORDING_WELSH;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S3776"})
public class OffenceHelper {

    private OffenceHelper() {
    }

    public static JsonArray transformOffences(final JsonArray offenceJsonObjects) {

        final JsonArrayBuilder offenceList = createArrayBuilder();
        offenceJsonObjects.forEach(o -> {
            final JsonObject offence = (JsonObject) o;
            final JsonObjectBuilder offenceBuilder = transformOffence(offence);
            offenceList.add(offenceBuilder.build());
        });
        return offenceList.build();

    }

    public static JsonObjectBuilder transformOffence(final JsonObject offence) {
        //add required fields
        final JsonObjectBuilder offenceBuilder = createObjectBuilder()
                .add(ID, offence.getString(ID))
                .add(OFFENCE_DEFINITION_ID, offence.getString(OFFENCE_DEFINITION_ID))
                .add(OFFENCE_CODE, offence.getString(OFFENCE_CODE))
                .add(OFFENCE_TITLE, offence.getString(OFFENCE_TITLE))
                .add(WORDING, offence.getString(WORDING))
                .add(START_DATE, offence.getString(START_DATE));

        // add optional fields
        if (offence.containsKey(OFFENCE_TITLE_WELSH)) {
            offenceBuilder.add(OFFENCE_TITLE_WELSH, offence.getString(OFFENCE_TITLE_WELSH));
        }

        if (offence.containsKey(OFFENCE_LEGISLATION)) {
            offenceBuilder.add(OFFENCE_LEGISLATION, offence.getString(OFFENCE_LEGISLATION));
        }

        if (offence.containsKey(OFFENCE_LEGISLATION_WELSH)) {
            offenceBuilder.add(OFFENCE_LEGISLATION_WELSH, offence.getString(OFFENCE_LEGISLATION_WELSH));
        }

        if (offence.containsKey(MODE_OF_TRIAL)) {
            offenceBuilder.add(MODE_OF_TRIAL, offence.getString(MODE_OF_TRIAL));
        }

        if (offence.containsKey(WORDING_WELSH)) {
            offenceBuilder.add(WORDING_WELSH, offence.getString(WORDING_WELSH));
        }

        if (offence.containsKey(END_DATE)) {
            offenceBuilder.add(END_DATE, offence.getString(END_DATE));
        }

        if (offence.containsKey(ARREST_DATE)) {
            offenceBuilder.add(ARREST_DATE, offence.getString(ARREST_DATE));
        }

        if (offence.containsKey(CHARGE_DATE)) {
            offenceBuilder.add(CHARGE_DATE, offence.getString(CHARGE_DATE));
        }

        if (offence.containsKey(ORDER_INDEX)) {
            offenceBuilder.add(ORDER_INDEX, offence.getInt(ORDER_INDEX));
        }

        if (offence.containsKey(DATE_OF_INFORMATION)) {
            offenceBuilder.add(DATE_OF_INFORMATION, offence.getString(DATE_OF_INFORMATION));
        }

        if (offence.containsKey(COUNT)) {
            offenceBuilder.add(COUNT, offence.getInt(COUNT));
        }

        if (offence.containsKey(CONVICTION_DATE)) {
            offenceBuilder.add(CONVICTION_DATE, offence.getString(CONVICTION_DATE));
        }

        if (offence.containsKey(NOTIFIED_PLEA)) {
            offenceBuilder.add(NOTIFIED_PLEA, offence.getJsonObject(NOTIFIED_PLEA));
        }

        if (offence.containsKey(VERDICT)) {
            offenceBuilder.add(VERDICT, offence.getJsonObject(VERDICT));
        }

        if (offence.containsKey(OFFENCE_FACTS)) {
            offenceBuilder.add(OFFENCE_FACTS, offence.getJsonObject(OFFENCE_FACTS));
        }

        if (offence.containsKey(PLEA)) {
            offenceBuilder.add(PLEA, offence.getJsonObject(PLEA));
        }

        if (offence.containsKey(INDICATED_PLEA)) {
            offenceBuilder.add(INDICATED_PLEA, transformIndicatedPlea(offence.getJsonObject(INDICATED_PLEA)));
        }

        if (offence.containsKey(AQUITTAL_DATE)) {
            offenceBuilder.add(AQUITTAL_DATE, offence.getString(AQUITTAL_DATE));
        }

        if (offence.containsKey(JUDICIAL_RESULTS)) {
            offenceBuilder.add(JUDICIAL_RESULTS, filterJudicialResults(offence.getJsonArray(JUDICIAL_RESULTS)));
        }

        if (offence.containsKey(VICTIMS)) {
            offenceBuilder.add(VICTIMS, offence.getJsonArray(VICTIMS));
        }

        if (offence.containsKey(ALLOCATION_DECISION)) {
            offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(ALLOCATION_DECISION), offence.getString(ID)));
        }

        if (offence.containsKey(IS_DISCONTINUED)) {
            offenceBuilder.add(IS_DISCONTINUED, offence.getBoolean(IS_DISCONTINUED));
        }
        if (offence.containsKey(IS_INTRODUCEAFTERINITIALPROCEEDINGS)) {
            offenceBuilder.add(IS_INTRODUCEAFTERINITIALPROCEEDINGS, offence.getBoolean(IS_INTRODUCEAFTERINITIALPROCEEDINGS));
        }

        if (offence.containsKey(LAA_APPLN_REFERENCE)) {
            offenceBuilder.add(LAA_APPLN_REFERENCE, offence.getJsonObject(LAA_APPLN_REFERENCE));
        }
        if (offence.containsKey(CUSTODY_TIME_LIMIT)) {
            offenceBuilder.add(CUSTODY_TIME_LIMIT, offence.getJsonObject(CUSTODY_TIME_LIMIT));
        }
        if (offence.containsKey(PROCEEDINGS_CONCLUDED)) {
            offenceBuilder.add(PROCEEDINGS_CONCLUDED, offence.getBoolean(PROCEEDINGS_CONCLUDED));
        }
        return offenceBuilder;

    }

    private static JsonObject transformAllocationDecision(final JsonObject jsonObject, final String offenceId) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        //required
        jsonObjectBuilder
                .add(ORIGINATING_HEARING_ID, jsonObject.getString(ORIGINATING_HEARING_ID))
                .add(OFFENCE_ID, offenceId)
                .add(ALLOCATION_DECISION_DATE, jsonObject.getString(ALLOCATION_DECISION_DATE))
                .add(MOT_REASON_ID, jsonObject.getString(MOT_REASON_ID))
                .add(SEQUENCE_NUMBER, jsonObject.getInt(SEQUENCE_NUMBER))
                .add(MOT_REASON_DESCRIPTION, jsonObject.getString(MOT_REASON_DESCRIPTION))
                .add(MOT_REASON_CODE, jsonObject.getString(MOT_REASON_CODE));
        // add optional attribute
        if (jsonObject.containsKey(COURT_INDICATED_SENTENCE)) {
            jsonObjectBuilder.add(COURT_INDICATED_SENTENCE, jsonObject.getJsonObject(COURT_INDICATED_SENTENCE))
                    .build();
        }
        return jsonObjectBuilder.build();
    }


    public static JsonObject transformIndicatedPlea(final JsonObject jsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        //required
        jsonObjectBuilder
                .add(OFFENCE_ID, jsonObject.getString(OFFENCE_ID))
                .add(INDICATED_PLEA_DATE, jsonObject.getString(INDICATED_PLEA_DATE))
                .add(INDICATED_PLEA_VALUE, jsonObject.getString(INDICATED_PLEA_VALUE))
                .add(SOURCE, jsonObject.getString(SOURCE));
        // add optional attributes
        if (jsonObject.containsKey(ORIGINATING_HEARING_ID)) {
            jsonObjectBuilder.add(ORIGINATING_HEARING_ID, jsonObject.getString(ORIGINATING_HEARING_ID));
        }
        return jsonObjectBuilder.build();
    }
}