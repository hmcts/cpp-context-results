package uk.gov.moj.cpp.results.material.client;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class MaterialUrlGeneratorTest {

    private static final String BASE_URI = "http://localhost:8080/material-query-api/query/api/rest/material";

    private final MaterialUrlGenerator materialUrlGenerator = new MaterialUrlGenerator();

    @Test
    public void shouldBuildPdfFileStreamUrl() {
        final UUID materialId = randomUUID();

        final String url = materialUrlGenerator.pdfFileStreamUrlFor(materialId);

        assertThat(url, is(BASE_URI + "/material/" + materialId + "?stream=true&requestPdf=true"));
    }

    @Test
    public void shouldBuildFileStreamUrlWithPdfFlagTrue() {
        final UUID materialId = randomUUID();

        final String url = materialUrlGenerator.fileStreamUrlFor(materialId, true);

        assertThat(url, is(BASE_URI + "/material/" + materialId + "?stream=true&requestPdf=true"));
    }

    @Test
    public void shouldBuildFileStreamUrlWithPdfFlagFalse() {
        final UUID materialId = randomUUID();

        final String url = materialUrlGenerator.fileStreamUrlFor(materialId, false);

        assertThat(url, is(BASE_URI + "/material/" + materialId));
    }

    @Test
    public void shouldBuildFileStreamUrlDefaultingToNonPdf() {
        final UUID materialId = randomUUID();

        final String url = materialUrlGenerator.fileStreamUrlFor(materialId);

        assertThat(url, is(BASE_URI + "/material/" + materialId));
    }
}
