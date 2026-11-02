package uk.gov.moj.cpp.results.persist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.results.persist.entity.HearingDefendantKey;
import uk.gov.moj.cpp.results.persist.entity.HearingDefendantResultedDocument;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class HearingDefendantResultedDocumentRepositoryTest {

    private static final String PERSISTENCE_UNIT = "results-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private HearingDefendantResultedDocumentRepository hearingDefendantResultedDocumentRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        hearingDefendantResultedDocumentRepository = new HearingDefendantResultedDocumentRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(hearingDefendantResultedDocumentRepository);
    }

    @Test
    public void shouldPersistDocument() {
        final HearingDefendantResultedDocument document = new HearingDefendantResultedDocument();
        document.setId(new HearingDefendantKey(randomUUID(), randomUUID()));
        document.setStartDate(LocalDate.of(2018, 12, 5));
        document.setEndDate(LocalDate.of(2018, 12, 10));
        document.setSummaryPayload(STRING.next());
        document.setDetailsPayload(STRING.next());

        final HearingDefendantResultedDocument saved = hearingDefendantResultedDocumentRepository.save(document);

        assertThat(saved, is(notNullValue()));
        assertThat(saved.getId(), is(document.getId()));
    }
}
