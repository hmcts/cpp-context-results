package uk.gov.moj.cpp.results.persist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocumentKey;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("CdiInjectionPointsInspection")
@RunWith(CdiTestRunner.class)
public class HearingResultedDocumentRepositoryTest extends BaseTransactionalTest {

    @Inject
    private HearingResultedDocumentRepository hearingResultedDocumentRepository;

    @Test
    public void shouldPersistDocument() {
        final UUID hearingId = UUID.randomUUID();
        persistDocument(hearingId, LocalDate.now());
    }


    @Test
    public void shouldFindByHearingId() {
        final UUID hearingId = UUID.randomUUID();
        final LocalDate hearingDay = LocalDate.of(2018, 12, 05);
        persistDocument(hearingId, hearingDay);
        final List<HearingResultedDocument> hearingResultedDocumentList = hearingResultedDocumentRepository.findByHearingId(hearingId);

        assertThat(hearingResultedDocumentList.size(), is(1));
        assertThat(hearingResultedDocumentList.get(0).getId().getHearingId(), is(hearingId));

    }

    @Test
    public void shouldFindByHearingIdAndLatestHearingDay(){
        final UUID hearingId = UUID.randomUUID();
        final LocalDate hearingDay1 = LocalDate.of(2018, 12, 04);
        final LocalDate hearingDay2 = LocalDate.of(2018, 12, 05);
        final LocalDate hearingDay3 = LocalDate.of(2018, 12, 10);
        persistDocument(hearingId, hearingDay1);
        persistDocument(hearingId, hearingDay2);
        persistDocument(hearingId, hearingDay3);

        final HearingResultedDocument hearingResultedDocument = hearingResultedDocumentRepository.findByHearingIdAndLatestHearingDay(hearingId);

        assertThat(hearingResultedDocument.getId().getHearingDay(), is(hearingDay3));
    }

    @Test
    public void shouldFindByHearingIdAndHearingDay(){
        final UUID hearingId = UUID.randomUUID();
        final LocalDate hearingDay1 = LocalDate.of(2018, 12, 04);
        final LocalDate hearingDay2 = LocalDate.of(2018, 12, 05);
        final LocalDate hearingDay3 = LocalDate.of(2018, 12, 10);
        persistDocument(hearingId, hearingDay1);
        persistDocument(hearingId, hearingDay2);
        persistDocument(hearingId, hearingDay3);

        final HearingResultedDocument hearingResultedDocument = hearingResultedDocumentRepository.findByHearingIdAndHearingDay(hearingId, hearingDay2);

        assertThat(hearingResultedDocument.getId().getHearingDay(), is(hearingDay2));
    }



    private void persistDocument(final UUID hearingId, final LocalDate hearingDay) {
        HearingResultedDocument document = new HearingResultedDocument();

        document.setId(new HearingResultedDocumentKey(hearingId, hearingDay));
        document.setStartDate(LocalDate.of(2018, 12, 04));
        document.setEndDate(LocalDate.of(2018, 12, 10));
        document.setPayload(STRING.next());
        hearingResultedDocumentRepository.save(document);
    }


}