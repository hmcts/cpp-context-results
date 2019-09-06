package uk.gov.moj.cpp.results.domain.transformation.util;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ALLOCATION_DECISION;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ALLOCATION_DECISION_DATE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.AQUITTAL_DATE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ARREST_DATE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.CHARGE_DATE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.CONVICTION_DATE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.COUNT;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.DATE_OF_INFORMATION;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.END_DATE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.INDICATED_PLEA;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.INDICATED_PLEA_DATE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.INDICATED_PLEA_VALUE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.MODE_OF_TRIAL;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.MOT_REASON_CODE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.MOT_REASON_DESCRIPTION;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.MOT_REASON_ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.NOTIFIED_PLEA;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OFFENCE_CODE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OFFENCE_DEFINITION_ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OFFENCE_FACTS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OFFENCE_ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OFFENCE_LEGISLATION;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OFFENCE_LEGISLATION_WELSH;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OFFENCE_TITLE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.OFFENCE_TITLE_WELSH;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ORDER_INDEX;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.ORIGINATING_HEARING_ID;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.PLEA;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.SEQUENCE_NUMBER;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.SOURCE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.START_DATE;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.VERDICT;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.VICTIMS;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.WORDING;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.WORDING_WELSH;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@SuppressWarnings({"squid:S1188", "squid:S3776"})
public class OffenceHelper {

    public static final String SUMMARY = "Summary";

    private OffenceHelper() {
    }

    public static JsonArray transformOffences(final JsonArray offenceJsonObjects,
                                              final JsonObject hearing) {
        final JsonArrayBuilder offenceList = createArrayBuilder();
        offenceJsonObjects.forEach(o -> {
            final JsonObject offence = (JsonObject) o;

            //add required fields,
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
                offenceBuilder.add(MODE_OF_TRIAL, derivedModeOfTrial(offence.getString(MODE_OF_TRIAL)));
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
                if (offence.containsKey(MODE_OF_TRIAL)) {
                    if ("IND".equalsIgnoreCase(offence.getString(MODE_OF_TRIAL))) {
                        offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), hearing.getString(ID), "4ba29b9f-9e57-32ed-b376-1840f4ba6c53", 20, "2", "Indictable-only offence"));
                    } else if ("EWAY".equalsIgnoreCase(offence.getString(MODE_OF_TRIAL))) {
                        offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), hearing.getString(ID), "78efce20-8a52-3272-9d22-2e7e6e3e565e", 70, "7", "No mode of Trial - Either way offence"));
                    } else {
                        offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), hearing.getString(ID), "b8c37e33-defd-351c-b91e-1e03e51657da", 10, "1", "Summary-only offence"));
                    }
                } else {
                    offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), hearing.getString(ID), "b8c37e33-defd-351c-b91e-1e03e51657da", 10, "1", "Summary-only offence"));
                }
            }

            if (offence.containsKey(INDICATED_PLEA)) {
                offenceBuilder.add(INDICATED_PLEA, transformIndicatedPlea(offence.getJsonObject(INDICATED_PLEA)));
                if (offence.getJsonObject(INDICATED_PLEA).containsKey(ALLOCATION_DECISION)) {
                    offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(INDICATED_PLEA), hearing.getString(ID)));
                }
            }

            if (offence.containsKey(AQUITTAL_DATE)) {
                offenceBuilder.add(AQUITTAL_DATE, offence.getString(AQUITTAL_DATE));
            }

            if (offence.containsKey(JUDICIAL_RESULTS)) {
                offenceBuilder.add(JUDICIAL_RESULTS, offence.getJsonArray(JUDICIAL_RESULTS));
            }

            if (offence.containsKey(VICTIMS)) {
                offenceBuilder.add(VICTIMS, offence.getJsonArray(VICTIMS));
            }

            offenceList.add(offenceBuilder.build());
        });
        return offenceList.build();

    }

    private static String derivedModeOfTrial(final String modeOfTrial) {
        return getDerivedModOfTrial().getOrDefault(modeOfTrial.toUpperCase(), SUMMARY);
    }

    private static Map<String, String> getDerivedModOfTrial() {
        final Map<String, String> derivedModeOfTrial = new HashMap<>();
        derivedModeOfTrial.put("EWAY", "Either Way");
        derivedModeOfTrial.put("IND", "Indictable");
        derivedModeOfTrial.put("SIMP", SUMMARY);
        derivedModeOfTrial.put("STRAFF", SUMMARY);
        derivedModeOfTrial.put("SNONIMP", SUMMARY);
        derivedModeOfTrial.put("CIVIL", SUMMARY);
        return derivedModeOfTrial;
    }


    private static JsonObject transformAllocationDecision(final JsonObject jsonObject, final String hearingId) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        if (jsonObject.containsKey(ORIGINATING_HEARING_ID)) {
            jsonObjectBuilder.add(ORIGINATING_HEARING_ID, jsonObject.getString(ORIGINATING_HEARING_ID));
        } else {
            jsonObjectBuilder.add(ORIGINATING_HEARING_ID, hearingId);
        }
        //required
        jsonObjectBuilder
                .add(ALLOCATION_DECISION_DATE, jsonObject.getString(INDICATED_PLEA_DATE))
                .add(MOT_REASON_ID, "f8eb278a-8bce-373e-b365-b45e939da38a")
                .add(MOT_REASON_DESCRIPTION, "Defendant chooses trial by jury")
                .add(MOT_REASON_CODE, "4")
                .add(SEQUENCE_NUMBER, 40);
        // add optional attributen
        if (jsonObject.getJsonObject(ALLOCATION_DECISION).containsKey("indicationOfSentence")) {
            jsonObjectBuilder.add("courtIndicatedSentence", createObjectBuilder()
                    .add("courtIndicatedSentenceTypeId", "d3d94468-02a4-3259-b55d-38e6d163e820")
                    .add("courtIndicatedSentenceDescription", jsonObject.getJsonObject(ALLOCATION_DECISION).getString("indicationOfSentence"))
                    .build());
        }
        return jsonObjectBuilder.build();
    }


    private static JsonObject transformAllocationDecision(final JsonObject jsonObject, final String hearingId, final String motReasonId, final int sequenceNumber, final String motReasonCode, final String motReasonDescription) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        if (jsonObject.containsKey(ORIGINATING_HEARING_ID)) {
            jsonObjectBuilder.add(ORIGINATING_HEARING_ID, jsonObject.getString(ORIGINATING_HEARING_ID));
        } else {
            jsonObjectBuilder.add(ORIGINATING_HEARING_ID, hearingId);
        }
        //required
        jsonObjectBuilder
                .add(ALLOCATION_DECISION_DATE, jsonObject.getString("pleaDate"))
                .add(MOT_REASON_ID, motReasonId)
                .add(MOT_REASON_DESCRIPTION, motReasonDescription)
                .add(MOT_REASON_CODE, motReasonCode)
                .add(SEQUENCE_NUMBER, sequenceNumber);

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
        // add optional attributen
        if (jsonObject.containsKey(ORIGINATING_HEARING_ID)) {
            jsonObjectBuilder.add(ORIGINATING_HEARING_ID, jsonObject.getString(ORIGINATING_HEARING_ID));
        }
        return jsonObjectBuilder.build();
    }

}