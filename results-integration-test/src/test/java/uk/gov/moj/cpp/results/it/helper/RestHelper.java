package uk.gov.moj.cpp.results.it.helper;

import static com.jayway.restassured.RestAssured.given;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.results.it.utils.UriConstants.BASE_URI;

import java.io.IOException;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

public class RestHelper {

    private static final RequestSpecification REQUEST_SPECIFICATION = new RequestSpecBuilder().setBaseUri(BASE_URI).build();

    public static Response postCommand(final String uri, final String mediaType,
                                       final String jsonStringBody) throws IOException {
        return postCommandWithUserId(uri, mediaType, jsonStringBody, randomUUID().toString());
    }

    public static Response postCommandWithUserId(final String uri, final String mediaType,
                                                 final String jsonStringBody, final String userId) throws IOException {
        return given().spec(REQUEST_SPECIFICATION).and().contentType(mediaType).body(jsonStringBody)
                .header(USER_ID, userId).when().post(uri).then()
                .extract().response();
    }
}
