package uk.gov.moj.cpp.domains;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.domains.HearingHelper.filterJudicialResults;
import static uk.gov.moj.cpp.domains.OffenceHelper.transformOffences;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ALIASES;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.APPEAL_PROCEEDINGS_PENDING;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ASSOCIATED_DEFENCE_ORGANISATION;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ASSOCIATED_PERSONS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.BREACH_PROCEEDINGS_PENDING;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.CASE_MARKERS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.CASE_STATUS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.CRO_NUMBER;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.DEFENDANTS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.DEFENDANT_CASE_JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.INITIATION_CODE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.IS_CIVIL;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.GROUP_ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.IS_GROUP_MASTER;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.IS_GROUP_MEMBER;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.IS_YOUTH;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.LEGAL_ENTITY_DEFENDANT;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.MASTER_DEFENDANT_ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.MITIGATION;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.MITIGATION_WELSH;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.NUMBER_OF_PREVIOUS_CONVICTIONS_CITED;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.OFFENCES;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ORIGINATING_ORGANISATION;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.PERSON_DEFENDANT;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.PNC_ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.POLICE_OFFICER_IN_CASE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.PROCEEDINGS_CONCLUDED;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.PROSECUTION_AUTHORITY_REFERENCE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.PROSECUTION_CASE_ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.PROSECUTION_CASE_IDENTIFIER;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.STATEMENT_OF_FACTS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.STATEMENT_OF_FACTS_WELSH;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.SUMMONS_CODE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.WITNESS_STATEMENT;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.WITNESS_STATEMENT_WELSH;

import java.util.stream.IntStream;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S3776"})
public class ProsecutionCaseHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseHelper.class);

    private ProsecutionCaseHelper() {
    }

    public static JsonArray transformProsecutionCases(final JsonArray prosecutionCases) {

        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();

        IntStream.range(
                0,
                prosecutionCases.size()
        ).mapToObj(
                index -> transformProsecutionCase(prosecutionCases.getJsonObject(index))
        ).forEach(transformedPayloadObjectBuilder::add);

        return transformedPayloadObjectBuilder.build();
    }

    public static JsonObjectBuilder transformProsecutionCase(final JsonObject prosecutionCase) {

        LOGGER.info("Transforming Prosecution Case {}", prosecutionCase.getString("id"));

        final JsonObjectBuilder transformProsecutionCaseBuilder = createObjectBuilder()
                .add(ID, prosecutionCase.getString(ID))
                .add(PROSECUTION_CASE_IDENTIFIER, prosecutionCase.getJsonObject(PROSECUTION_CASE_IDENTIFIER))
                .add(INITIATION_CODE, prosecutionCase.getString(INITIATION_CODE))
                .add(DEFENDANTS, transformDefendants(prosecutionCase.getJsonArray(DEFENDANTS)));

        if (prosecutionCase.containsKey(IS_CIVIL)) {
            transformProsecutionCaseBuilder.add(IS_CIVIL, prosecutionCase.getBoolean(IS_CIVIL));
        }

        if (prosecutionCase.containsKey(GROUP_ID)) {
            transformProsecutionCaseBuilder.add(GROUP_ID, prosecutionCase.getString(GROUP_ID));
        }

        if (prosecutionCase.containsKey(IS_GROUP_MEMBER)) {
            transformProsecutionCaseBuilder.add(IS_GROUP_MEMBER, prosecutionCase.getBoolean(IS_GROUP_MEMBER));
        }

        if (prosecutionCase.containsKey(IS_GROUP_MASTER)) {
            transformProsecutionCaseBuilder.add(IS_GROUP_MASTER, prosecutionCase.getBoolean(IS_GROUP_MASTER));
        }

        if (prosecutionCase.containsKey(CASE_STATUS)) {
            transformProsecutionCaseBuilder.add(CASE_STATUS, prosecutionCase.getString(CASE_STATUS));
        }

        if (prosecutionCase.containsKey(ORIGINATING_ORGANISATION)) {
            transformProsecutionCaseBuilder.add(ORIGINATING_ORGANISATION, prosecutionCase.getString(ORIGINATING_ORGANISATION));
        }

        if (prosecutionCase.containsKey(POLICE_OFFICER_IN_CASE)) {
            transformProsecutionCaseBuilder.add(POLICE_OFFICER_IN_CASE, prosecutionCase.getJsonObject(POLICE_OFFICER_IN_CASE));
        }

        if (prosecutionCase.containsKey(STATEMENT_OF_FACTS)) {
            transformProsecutionCaseBuilder.add(STATEMENT_OF_FACTS, prosecutionCase.getString(STATEMENT_OF_FACTS));
        }

        if (prosecutionCase.containsKey(STATEMENT_OF_FACTS_WELSH)) {
            transformProsecutionCaseBuilder.add(STATEMENT_OF_FACTS_WELSH, prosecutionCase.getString(STATEMENT_OF_FACTS_WELSH));
        }

        if (prosecutionCase.containsKey(BREACH_PROCEEDINGS_PENDING)) {
            transformProsecutionCaseBuilder.add(BREACH_PROCEEDINGS_PENDING, prosecutionCase.getBoolean(BREACH_PROCEEDINGS_PENDING));
        }

        if (prosecutionCase.containsKey(APPEAL_PROCEEDINGS_PENDING)) {
            transformProsecutionCaseBuilder.add(APPEAL_PROCEEDINGS_PENDING, prosecutionCase.getBoolean(APPEAL_PROCEEDINGS_PENDING));
        }

        if (prosecutionCase.containsKey(CASE_MARKERS)) {
            transformProsecutionCaseBuilder.add(CASE_MARKERS, prosecutionCase.getJsonArray(CASE_MARKERS));
        }

        if (prosecutionCase.containsKey(SUMMONS_CODE)) {
            transformProsecutionCaseBuilder.add(SUMMONS_CODE, prosecutionCase.getString(SUMMONS_CODE));
        }

        return transformProsecutionCaseBuilder;
    }

    public static JsonArray transformDefendants(final JsonArray defendants) {

        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();

        IntStream.range(0, defendants.size()).mapToObj(index -> transformDefendant(defendants.getJsonObject(index))).forEach(transformedPayloadObjectBuilder::add);

        return transformedPayloadObjectBuilder.build();
    }

    private static JsonObjectBuilder transformDefendant(final JsonObject defendant) {

        final JsonObjectBuilder transformDefendantBuilder = createObjectBuilder()
                .add(ID, defendant.getJsonString(ID))
                .add(PROSECUTION_CASE_ID, defendant.getJsonString(PROSECUTION_CASE_ID))
                .add(OFFENCES, transformOffences(defendant.getJsonArray(OFFENCES)));

        if (defendant.containsKey(NUMBER_OF_PREVIOUS_CONVICTIONS_CITED)) {
            transformDefendantBuilder.add(NUMBER_OF_PREVIOUS_CONVICTIONS_CITED, defendant.getInt(NUMBER_OF_PREVIOUS_CONVICTIONS_CITED));
        }

        if (defendant.containsKey(PROSECUTION_AUTHORITY_REFERENCE)) {
            transformDefendantBuilder.add(PROSECUTION_AUTHORITY_REFERENCE, defendant.getString(PROSECUTION_AUTHORITY_REFERENCE));
        }

        if (defendant.containsKey(WITNESS_STATEMENT)) {
            transformDefendantBuilder.add(WITNESS_STATEMENT, defendant.getString(WITNESS_STATEMENT));
        }

        if (defendant.containsKey(WITNESS_STATEMENT_WELSH)) {
            transformDefendantBuilder.add(WITNESS_STATEMENT_WELSH, defendant.getString(WITNESS_STATEMENT_WELSH));
        }

        if (defendant.containsKey(MITIGATION)) {
            transformDefendantBuilder.add(MITIGATION, defendant.getString(MITIGATION));
        }

        if (defendant.containsKey(MITIGATION_WELSH)) {
            transformDefendantBuilder.add(MITIGATION_WELSH, defendant.getString(MITIGATION_WELSH));
        }

        if (defendant.containsKey(ASSOCIATED_PERSONS)) {
            transformDefendantBuilder.add(ASSOCIATED_PERSONS, defendant.getJsonArray(ASSOCIATED_PERSONS));
        }

        if (defendant.containsKey(ASSOCIATED_DEFENCE_ORGANISATION)) {
            transformDefendantBuilder.add(ASSOCIATED_DEFENCE_ORGANISATION, defendant.getJsonArray(ASSOCIATED_DEFENCE_ORGANISATION));
        }

        if (defendant.containsKey(PERSON_DEFENDANT)) {
            transformDefendantBuilder.add(PERSON_DEFENDANT, defendant.getJsonObject(PERSON_DEFENDANT));
        }

        if (defendant.containsKey(LEGAL_ENTITY_DEFENDANT)) {
            transformDefendantBuilder.add(LEGAL_ENTITY_DEFENDANT, defendant.getJsonObject(LEGAL_ENTITY_DEFENDANT));
        }

        if (defendant.containsKey(ALIASES)) {
            transformDefendantBuilder.add(ALIASES, defendant.getJsonArray(ALIASES));
        }

        if (defendant.containsKey(JUDICIAL_RESULTS)) {
            transformDefendantBuilder.add(JUDICIAL_RESULTS, filterJudicialResults(defendant.getJsonArray(JUDICIAL_RESULTS)));
        }

        if (defendant.containsKey(DEFENDANT_CASE_JUDICIAL_RESULTS)) {
            transformDefendantBuilder.add(JUDICIAL_RESULTS, filterJudicialResults(defendant.getJsonArray(DEFENDANT_CASE_JUDICIAL_RESULTS)));
        }

        if (defendant.containsKey(CRO_NUMBER)) {
            transformDefendantBuilder.add(CRO_NUMBER, defendant.getString(CRO_NUMBER));
        }

        if (defendant.containsKey(PNC_ID)) {
            transformDefendantBuilder.add(PNC_ID, defendant.getString(PNC_ID));
        }
        if (defendant.containsKey(PROCEEDINGS_CONCLUDED)) {
            transformDefendantBuilder.add(PROCEEDINGS_CONCLUDED, defendant.getBoolean(PROCEEDINGS_CONCLUDED));
        }

        if (defendant.containsKey(MASTER_DEFENDANT_ID)) {
            transformDefendantBuilder.add(MASTER_DEFENDANT_ID, defendant.getJsonString(MASTER_DEFENDANT_ID));
        }

        if (defendant.containsKey(IS_YOUTH)) {
            transformDefendantBuilder.add(IS_YOUTH, defendant.getBoolean(IS_YOUTH));
        }
        return transformDefendantBuilder;
    }
}