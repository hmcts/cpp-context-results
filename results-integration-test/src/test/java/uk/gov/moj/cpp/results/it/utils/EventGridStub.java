package uk.gov.moj.cpp.results.it.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.apache.http.HttpStatus.SC_OK;

public class EventGridStub {

    public static void stubEventGridEndpoint() {

        stubFor(post(urlMatching("/.*")).willReturn(aResponse().withStatus(SC_OK)));
    }

}
