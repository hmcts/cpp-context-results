package uk.gov.moj.cpp.results.it.helper;

import static io.restassured.RestAssured.given;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.results.it.utils.UriConstants.BASE_URI;

import java.io.IOException;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class RestHelper {

    private static final RequestSpecification REQUEST_SPECIFICATION = new RequestSpecBuilder().setBaseUri(BASE_URI).build();

    public static Response postCommand(final String uri, final String mediaType,
                                       final String jsonStringBody) {
        return postCommandWithUserId(uri, mediaType, jsonStringBody, randomUUID().toString());
    }

    public static Response postCommandWithUserId(final String uri, final String mediaType,
                                                 final String jsonStringBody, final String userId) {
        return given().spec(REQUEST_SPECIFICATION).and().contentType(mediaType).body(jsonStringBody)
                .header(USER_ID, userId).when().post(uri).then()
                .extract().response();
    }
}
