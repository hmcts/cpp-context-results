package uk.gov.moj.cpp.results.it.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpStatus.SC_OK;

public class EventGridStub {

    public static void stubEventGridEndpoint() {
        stubFor(post(urlMatching("/.*")).willReturn(aResponse().withStatus(SC_OK)));
    }

}
