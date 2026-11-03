package uk.gov.moj.cpp.results.persist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocumentKey;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class HearingResultedDocumentRepositoryTest {

    private static final String PERSISTENCE_UNIT = "results-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private HearingResultedDocumentRepository hearingResultedDocumentRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        hearingResultedDocumentRepository = new HearingResultedDocumentRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(hearingResultedDocumentRepository);
    }

    @Test
    public void shouldPersistDocument() {
        final UUID hearingId = randomUUID();
        persistDocument(hearingId, LocalDate.now());
    }

    @Test
    public void shouldFindByHearingId() {
        final UUID hearingId = randomUUID();
        final LocalDate hearingDay = LocalDate.of(2018, 12, 5);
        persistDocument(hearingId, hearingDay);
        final List<HearingResultedDocument> hearingResultedDocumentList = hearingResultedDocumentRepository.findByHearingId(hearingId);

        assertThat(hearingResultedDocumentList.size(), is(1));
        assertThat(hearingResultedDocumentList.get(0).getId().getHearingId(), is(hearingId));
    }

    @Test
    public void shouldFindByHearingIdAndLatestHearingDay() {
        final UUID hearingId = randomUUID();
        final LocalDate hearingDay1 = LocalDate.of(2018, 12, 4);
        final LocalDate hearingDay2 = LocalDate.of(2018, 12, 5);
        final LocalDate hearingDay3 = LocalDate.of(2018, 12, 10);
        persistDocument(hearingId, hearingDay1);
        persistDocument(hearingId, hearingDay2);
        persistDocument(hearingId, hearingDay3);

        final Optional<HearingResultedDocument> hearingResultedDocument = hearingResultedDocumentRepository.findByHearingIdAndLatestHearingDay(hearingId);

        assertThat(hearingResultedDocument.isPresent(), is(true));
        assertThat(hearingResultedDocument.get().getId().getHearingDay(), is(hearingDay3));
    }

    @Test
    public void shouldFindByHearingIdAndHearingDay() {
        final UUID hearingId = randomUUID();
        final LocalDate hearingDay1 = LocalDate.of(2018, 12, 4);
        final LocalDate hearingDay2 = LocalDate.of(2018, 12, 5);
        final LocalDate hearingDay3 = LocalDate.of(2018, 12, 10);
        persistDocument(hearingId, hearingDay1);
        persistDocument(hearingId, hearingDay2);
        persistDocument(hearingId, hearingDay3);

        final Optional<HearingResultedDocument> hearingResultedDocument = hearingResultedDocumentRepository.findByHearingIdAndHearingDay(hearingId, hearingDay2);

        assertThat(hearingResultedDocument.isPresent(), is(true));
        assertThat(hearingResultedDocument.get().getId().getHearingDay(), is(hearingDay2));
    }

    @Test
    public void shouldReturnEmptyWhenFindByHearingIdAndLatestHearingDayNoHearingPresent() {
        final UUID hearingId = randomUUID();
        final LocalDate hearingDay1 = LocalDate.of(2018, 12, 4);
        final LocalDate hearingDay2 = LocalDate.of(2018, 12, 5);
        final LocalDate hearingDay3 = LocalDate.of(2018, 12, 10);
        persistDocument(hearingId, hearingDay1);
        persistDocument(hearingId, hearingDay2);
        persistDocument(hearingId, hearingDay3);

        final Optional<HearingResultedDocument> hearingResultedDocument = hearingResultedDocumentRepository.findByHearingIdAndLatestHearingDay(randomUUID());
        assertThat(hearingResultedDocument.isEmpty(), is(true));
    }

    @Test
    public void shouldFindByFromDate() {
        final UUID hearingId = randomUUID();
        persistDocument(hearingId, LocalDate.of(2018, 12, 5));

        final List<HearingResultedDocument> documents =
                hearingResultedDocumentRepository.findByFromDate(LocalDate.of(2018, 12, 1));

        assertThat(documents.size(), is(1));
        assertThat(documents.get(0).getId().getHearingId(), is(hearingId));
    }

    private void persistDocument(final UUID hearingId, final LocalDate hearingDay) {
        final HearingResultedDocument document = new HearingResultedDocument();

        document.setId(new HearingResultedDocumentKey(hearingId, hearingDay));
        document.setStartDate(LocalDate.of(2018, 12, 4));
        document.setEndDate(LocalDate.of(2018, 12, 10));
        document.setPayload(STRING.next());
        hearingResultedDocumentRepository.save(document);
    }
}
