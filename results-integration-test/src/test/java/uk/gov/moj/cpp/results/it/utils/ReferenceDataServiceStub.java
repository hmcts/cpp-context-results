package uk.gov.moj.cpp.results.it.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.getJsonResponse;
import static uk.gov.moj.cpp.results.it.utils.WireMockStubUtils.waitForStubToBeReady;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class ReferenceDataServiceStub {

    public static final String PROSECUTOR_WITH_SPI_OUT_FALSE = "prosecutorWithSpiOutFalse";

    public static void stubCountryNationalities() {
        stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/country-nationality";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse("stub-data/referencedata.query.country-nationality.json"))));
        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.country-nationality+json");
    }

    public static void stubGetOrgainsationUnit() {
        stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/organisation-units/";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse("stub-data/referencedata.query.organisation-unit-v2.json"))));
        waitForStubToBeReady(urlPath + "f8254db1-1683-483e-afb3-b87fde5a0a26", "application/vnd.referencedata.query.organisation-unit.v2+json");
    }

    public static void stubJudicialResults() {
        stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/result-definitions";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse("stub-data/referencedata.result-definitions.json"))));
        waitForStubToBeReady(urlPath, "application/vnd.referencedata.get-all-result-definitions+json");
    }

    public static void stubSpiOutFlag(final boolean spiOutFlag, final boolean policeFlag) {
        stubSpiOutFlag(spiOutFlag, policeFlag, null);
    }

    public static void stubSpiOutFlag(final boolean spiOutFlag, final boolean policeFlag, final String email) {
        stubPingFor("referencedata-service");


        final String urlPath = "/referencedata-service/query/api/rest/referencedata/prosecutors";

        JsonObjectBuilder prosecutorBodyBuilder = createObjectBuilder();
        prosecutorBodyBuilder
                .add("spiOutFlag", spiOutFlag)
                .add("policeFlag", policeFlag);
        if(nonNull(email)){
            prosecutorBodyBuilder.add("contactEmailAddress", email);
        }

        final JsonObject response = createObjectBuilder()
                .add("prosecutors", createArrayBuilder()
                        .add(prosecutorBodyBuilder.build())
                        .build())
                .build();

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(response.toString())));

        prosecutorBodyBuilder = createObjectBuilder();
        prosecutorBodyBuilder
                .add("spiOutFlag", false)
                .add("policeFlag", false);
        if(nonNull(email)){
            prosecutorBodyBuilder.add("contactEmailAddress", email);
        }

        final JsonObject responseFalse = createObjectBuilder()
                .add("prosecutors", createArrayBuilder()
                        .add(prosecutorBodyBuilder.build())
                        .build())
                .build();

        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("prosecutorCode", equalTo(PROSECUTOR_WITH_SPI_OUT_FALSE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(responseFalse.toString())));

        waitForStubToBeReady(urlPath, "application/vnd.referencedata.query.get.prosecutor+json");
        waitForStubToBeReady(urlPath + "?prosecutorCode=prosecutorWithSpiOutFalse", "application/vnd.referencedata.query.get.prosecutor+json");
    }

    public static void stubBailStatuses() {
        stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/bail-statuses";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse("stub-data/referencedata.bail-statuses.json"))));
        waitForStubToBeReady(urlPath, "application/vnd.referencedata.bail-statuses+json");
    }

    public static void stubModeOfTrialReasons() {
        stubPingFor("referencedata-service");

        final String urlPath = "/referencedata-service/query/api/rest/referencedata/mode-of-trial-reasons";

        stubFor(get(urlPathEqualTo(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(getJsonResponse("stub-data/referencedata.mode-of-trial-reasons.json"))));
        waitForStubToBeReady(urlPath, "application/vnd.referencedata.mode-of-trial-reasons+json");
    }




}
