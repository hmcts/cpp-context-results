package uk.gov.moj.cpp.results.it;

import static io.restassured.RestAssured.given;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.results.it.utils.UriConstants.BASE_URI;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class DefendantGobAccountsIT {

    @Test
    public void shouldGetDefendantGobAccounts() {
        final UUID masterDefendantId = randomUUID();
        final String caseReferences = "caseRef1, caseRef2";

        // First, we need to create test data in the database
        // This would typically be done through the command side or test data setup
        // For now, we'll test the endpoint structure

        given()
                .baseUri(BASE_URI)
                .header("Content-Type", "application/vnd.results.query.defendant-gob-accounts+json")
                .header("Accept", "application/vnd.results.query.defendant-gob-accounts+json")
                .when()
                .get("/results-query-api/query/api/rest/results/defendant-gob-accounts?masterDefendantId={masterDefendantId}&caseReferences={caseReferences}",
                        masterDefendantId.toString(), caseReferences)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("masterDefendantId", equalTo(masterDefendantId.toString()))
                .body("caseReferences", equalTo(caseReferences))
                .body("accountNumber", notNullValue())
                .body("correlationId", notNullValue())
                .body("createdDateTime", notNullValue());
    }

    @Test
    public void shouldReturn404WhenNoDefendantGobAccountsFound() {
        final UUID nonExistentMasterDefendantId = randomUUID();
        final String caseReferences = "nonExistentCaseRef";

        given()
                .baseUri(BASE_URI)
                .header("Content-Type", "application/vnd.results.query.defendant-gob-accounts+json")
                .header("Accept", "application/vnd.results.query.defendant-gob-accounts+json")
                .when()
                .get("/results-query-api/query/api/rest/results/defendant-gob-accounts?masterDefendantId={masterDefendantId}&caseReferences={caseReferences}",
                        nonExistentMasterDefendantId.toString(), caseReferences)
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldReturn400WhenMasterDefendantIdIsMissing() {
        given()
                .baseUri(BASE_URI)
                .header("Content-Type", "application/vnd.results.query.defendant-gob-accounts+json")
                .header("Accept", "application/vnd.results.query.defendant-gob-accounts+json")
                .when()
                .get("/results-query-api/query/api/rest/results/hearing-financial-details?caseReferences=caseRef1")
                .then()
                .statusCode(400);
    }

    @Test
    public void shouldReturn400WhenCaseReferencesIsMissing() {
        final UUID masterDefendantId = randomUUID();

        given()
                .baseUri(BASE_URI)
                .header("Content-Type", "application/vnd.results.query.defendant-gob-accounts+json")
                .header("Accept", "application/vnd.results.query.defendant-gob-accounts+json")
                .when()
                .get("/results-query-api/query/api/rest/results/hearing-financial-details?masterDefendantId={masterDefendantId}",
                        masterDefendantId.toString())
                .then()
                .statusCode(400);
    }
}
