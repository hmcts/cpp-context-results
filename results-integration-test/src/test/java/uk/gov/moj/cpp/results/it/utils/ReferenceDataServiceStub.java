package uk.gov.moj.cpp.results.it.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.getJsonResponse;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class ReferenceDataServiceStub {

    public static final String PROSECUTOR_WITH_SPI_OUT_FALSE = "prosecutorWithSpiOutFalse";

    public static void stubCountryNationalities() {
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/country-nationality";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse("stub-data/referencedata.query.country-nationality.json"))));
    }

    public static void stubGetOrgainsationUnit() {
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/organisation-units/";

        stubFor(get(urlPathMatching(urlPath + ".*"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse("stub-data/referencedata.query.organisation-unit-v2.json"))));
    }

    public static void stubJudicialResults() {
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/result-definitions";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse("stub-data/referencedata.result-definitions.json"))));
    }

    public static void stubSpiOutFlag(final boolean spiOutFlag, final boolean policeFlag) {
        stubSpiOutFlag(spiOutFlag, policeFlag, null);
    }

    public static void stubSpiOutFlag(final boolean spiOutFlag, final boolean policeFlag, final String policeEmailAddress) {
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/prosecutors";

        JsonObjectBuilder prosecutorBodyBuilder = createObjectBuilder();
        prosecutorBodyBuilder
                .add("spiOutFlag", spiOutFlag)
                .add("policeFlag", policeFlag);
        if (nonNull(policeEmailAddress)) {
            prosecutorBodyBuilder.add("contactEmailAddress", policeEmailAddress)
                    .add("mcContactEmailAddress", policeEmailAddress);
        }

        final JsonObject response = prosecutorBodyBuilder.build();

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(response.toString())));

        prosecutorBodyBuilder = createObjectBuilder();
        prosecutorBodyBuilder
                .add("spiOutFlag", false)
                .add("policeFlag", false);
        if (nonNull(policeEmailAddress)) {
            prosecutorBodyBuilder.add("contactEmailAddress", policeEmailAddress);
        }

        final JsonObject responseFalse = prosecutorBodyBuilder.build();

        final JsonObject prosecutorCodeResponse = createObjectBuilder()
                .add("prosecutors", createArrayBuilder()
                        .add(response)
                        .build())
                .build();

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("oucode", equalTo("prosecutorWithSpiOutFalse"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responseFalse.toString())));

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("prosecutorCode", equalTo("prosecutorWithSpiOutFalse"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(prosecutorCodeResponse.toString())));

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("prosecutorCode", equalTo("CITYPF"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(prosecutorCodeResponse.toString())));
    }

    public static void stubPoliceFlag(final String originatingOrganisation, final String prosecutionAuthority) {
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/prosecutors";

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("oucode", equalTo(originatingOrganisation))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse("stub-data/referencedata.query.organisation-unit-prosecution-authority.json"))));

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("prosecutorCode", equalTo(prosecutionAuthority))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse("stub-data/referencedata.query.organisation-unit-prosecution-authority.json"))));
    }

    public static void stubBailStatuses() {
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/bail-statuses";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse("stub-data/referencedata.bail-statuses.json"))));
    }

    public static void stubModeOfTrialReasons() {
        final String urlPath = "/referencedata-service/query/api/rest/referencedata/mode-of-trial-reasons";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse("stub-data/referencedata.mode-of-trial-reasons.json"))));
    }
}