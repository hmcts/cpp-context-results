package uk.gov.moj.cpp.results.domain.informant.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * T071 (FR-004): the verdict schema's {@code dependencies} block enforces that {@code verdictCode}
 * and {@code verdictDate} are mutually co-dependent. This schema-level constraint is the mechanism
 * by which the framework rejects a command carrying {@code verdictCode} without {@code verdictDate}
 * at the envelope-validation boundary — no production handler guard is required.
 *
 * <p>This is verified at the schema layer (not in {@code InformantRegisterHandlerTest}) because the
 * handler unit test invokes the handler method directly and therefore bypasses the framework's
 * envelope schema validation. The behaviour is additionally exercised end-to-end by the negative
 * integration test in {@code InformantRegisterDocumentRequestIT}.</p>
 */
class VerdictSchemaDependencyTest {

    private static final String VERDICT_SCHEMA = "/json/schema/informantRegisterDocument/verdict.json";

    private Schema schema;

    @BeforeEach
    void loadSchema() {
        try (InputStream in = getClass().getResourceAsStream(VERDICT_SCHEMA)) {
            schema = SchemaLoader.load(new JSONObject(new JSONTokener(in)));
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to load verdict schema from " + VERDICT_SCHEMA, e);
        }
    }

    @Test
    void verdictCodeWithVerdictDate_shouldValidate() {
        assertDoesNotThrow(() -> schema.validate(new JSONObject()
                .put("verdictCode", "G")
                .put("verdictDate", "2026-04-13")
                .put("verdictType", "FOUND_GUILTY")));
    }

    @Test
    void verdictCodeWithoutVerdictDate_shouldFailValidation() {
        final ValidationException exception = assertThrows(ValidationException.class,
                () -> schema.validate(new JSONObject().put("verdictCode", "G")));
        assertThat(exception.getMessage(), containsString("verdictDate"));
    }

    @Test
    void verdictDateWithoutVerdictCode_shouldFailValidation() {
        final ValidationException exception = assertThrows(ValidationException.class,
                () -> schema.validate(new JSONObject().put("verdictDate", "2026-04-13")));
        assertThat(exception.getMessage(), containsString("verdictCode"));
    }

    @Test
    void emptyVerdict_shouldValidate() {
        assertDoesNotThrow(() -> schema.validate(new JSONObject()));
    }
}
