package uk.gov.moj.cpp.results.persist;

import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.results.persist.entity.HearingDefendantKey;
import uk.gov.moj.cpp.results.persist.entity.HearingDefendantResultedDocument;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.UUID;

@SuppressWarnings("CdiInjectionPointsInspection")
@RunWith(CdiTestRunner.class)
public class HearingResultedDocumentRepositoryTest extends BaseTransactionalTest {

    @Inject
    private HearingResultedDocumentRepository hearingResultedDocumentRepository;

    @Test
    public void shouldPersistDocument() {

        HearingResultedDocument document = new HearingResultedDocument();
        document.setHearingId(UUID.randomUUID());
        document.setStartDate(LocalDate.of(2018, 12, 05));
        document.setEndDate(LocalDate.of(2018, 12, 10));
        document.setPayload(STRING.next());
        hearingResultedDocumentRepository.save(document);
    }



}