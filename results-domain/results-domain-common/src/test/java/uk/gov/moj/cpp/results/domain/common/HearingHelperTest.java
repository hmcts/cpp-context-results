package uk.gov.moj.cpp.results.domain.common;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.CIVIL_OFFENCE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.COURT_APPLICATIONS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.DEFENDANTS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.DEFENDANT_JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.GROUP_ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.IS_CIVIL;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.IS_EX_PARTE;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.IS_GROUP_MASTER;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.IS_GROUP_MEMBER;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.IS_GROUP_PROCEEDINGS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.IS_RESPONDENT;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.LISTING_NUMBER;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.OFFENCES;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.PROSECUTION_CASES;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.DEFENDANTS_WITH_WELSH_TRANSLATION_LIST;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.domains.HearingHelper;

import java.nio.charset.Charset;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingHelperTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingHelperTest.class.getName());

    private HearingHelper hearingHelper = new HearingHelper();

    @Test
    public void shouldTransformHearing() {
        final JsonObject hearingJson = getPayload("hearing.json");

        final JsonObject transformedObject = hearingHelper.transformedHearing(hearingJson);

        final JsonObject prosecutionCaseJson = transformedObject.getJsonArray(PROSECUTION_CASES).getValuesAs(JsonObject.class).get(0);
        final JsonObject defendantJson = prosecutionCaseJson.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).get(0);
        final JsonObject offenceJson = defendantJson.getJsonArray(OFFENCES).getValuesAs(JsonObject.class).get(0);
        final JsonObject applicationJson = transformedObject.getJsonArray(COURT_APPLICATIONS).getValuesAs(JsonObject.class).get(0);
        final JsonObject defendantWithWelshTranslationJson = transformedObject.getJsonArray(DEFENDANTS_WITH_WELSH_TRANSLATION_LIST).getValuesAs(JsonObject.class).get(0);

        assertThat(transformedObject.getJsonArray(DEFENDANT_JUDICIAL_RESULTS).size(), is(1));
        assertThat(defendantJson.getJsonArray(JUDICIAL_RESULTS).size(), is(2));
        assertThat(offenceJson.getJsonArray(JUDICIAL_RESULTS).size(), is(2));
        assertThat(applicationJson.getJsonArray(JUDICIAL_RESULTS).size(), is(2));
        assertThat(offenceJson.getJsonNumber(LISTING_NUMBER).intValue(), is(2));
        assertThat(defendantWithWelshTranslationJson, is(notNullValue()));

    }

    @Test
    public void shouldTransformHearingWithAllAttributes() {
        final JsonObject hearingJson = getPayload("hearingWithAllAttributes.json");

        final JsonObject transformedObject = hearingHelper.transformedHearing(hearingJson);

        final JsonObject prosecutionCaseJson = transformedObject.getJsonArray(PROSECUTION_CASES).getValuesAs(JsonObject.class).get(0);
        final JsonObject defendantJson = prosecutionCaseJson.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).get(0);
        final JsonObject offenceJson = defendantJson.getJsonArray(OFFENCES).getValuesAs(JsonObject.class).get(0);
        final JsonObject applicationJson = transformedObject.getJsonArray(COURT_APPLICATIONS).getValuesAs(JsonObject.class).get(0);
        final JsonObject defendantWithWelshTranslationJson = transformedObject.getJsonArray(DEFENDANTS_WITH_WELSH_TRANSLATION_LIST).getValuesAs(JsonObject.class).get(0);

        assertThat(transformedObject.getJsonArray(DEFENDANT_JUDICIAL_RESULTS).size(), is(1));
        assertThat(defendantJson.getJsonArray(JUDICIAL_RESULTS).size(), is(2));
        assertThat(offenceJson.getJsonArray(JUDICIAL_RESULTS).size(), is(2));
        assertThat(applicationJson.getJsonArray(JUDICIAL_RESULTS).size(), is(2));
        assertThat(offenceJson.getJsonNumber(LISTING_NUMBER).intValue(), is(2));
        assertThat(defendantWithWelshTranslationJson, is(notNullValue()));

    }

    @Test
    public void shouldTransformHearingForGroupCases() {
        final JsonObject hearingJson = getPayload("hearingWithGroupCases.json");
        final String groupId = "bb36fb3c-a6e9-493c-b2dc-8be11f2b3993";

        final JsonObject transformedObject = hearingHelper.transformedHearing(hearingJson);
        final JsonArray prosecutionCaseJson = transformedObject.getJsonArray(PROSECUTION_CASES);

        assertThat(transformedObject.getBoolean(IS_GROUP_PROCEEDINGS), is(true));
        assertThat(prosecutionCaseJson.size(), is(3));

        JsonObject case1 = prosecutionCaseJson.getJsonObject(0);
        assertThat(case1.getBoolean(IS_CIVIL), is(true));
        assertThat(case1.getString(GROUP_ID), is(groupId));
        assertThat(case1.getBoolean(IS_GROUP_MEMBER), is(true));
        assertThat(case1.getBoolean(IS_GROUP_MASTER), is(true));

        JsonObject civilOffence1 = case1.getJsonArray(DEFENDANTS).getJsonObject(0).getJsonArray(OFFENCES).getJsonObject(0).getJsonObject(CIVIL_OFFENCE);
        assertThat(civilOffence1.getBoolean(IS_EX_PARTE), is(true));
        assertThat(civilOffence1.getBoolean(IS_RESPONDENT), is(false));

        JsonObject case2 = prosecutionCaseJson.getJsonObject(1);
        assertThat(case2.getBoolean(IS_CIVIL), is(true));
        assertThat(case2.getString(GROUP_ID), is(groupId));
        assertThat(case2.getBoolean(IS_GROUP_MEMBER), is(true));
        assertThat(case2.getBoolean(IS_GROUP_MASTER), is(false));

        JsonObject civilOffence2 = case2.getJsonArray(DEFENDANTS).getJsonObject(0).getJsonArray(OFFENCES).getJsonObject(0).getJsonObject(CIVIL_OFFENCE);
        assertThat(civilOffence2.containsKey(IS_EX_PARTE), is(false));
        assertThat(civilOffence2.getBoolean(IS_RESPONDENT), is(true));

        JsonObject case3 = prosecutionCaseJson.getJsonObject(2);
        assertThat(case3.getBoolean(IS_CIVIL), is(true));
        assertThat(case3.getString(GROUP_ID), is(groupId));
        assertThat(case3.getBoolean(IS_GROUP_MEMBER), is(false));
        assertThat(case3.containsKey(IS_GROUP_MASTER), is(false));

        JsonObject civilOffence3 = case3.getJsonArray(DEFENDANTS).getJsonObject(0).getJsonArray(OFFENCES).getJsonObject(0).getJsonObject(CIVIL_OFFENCE);
        assertThat(civilOffence3.getBoolean(IS_EX_PARTE), is(true));
        assertThat(civilOffence3.containsKey(IS_RESPONDENT), is(false));
    }

    private static JsonObject getPayload(final String filename) {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(filename),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            LOGGER.info("error {}", e.getMessage());
        }

        return new StringToJsonObjectConverter().convert(response);
    }
}
