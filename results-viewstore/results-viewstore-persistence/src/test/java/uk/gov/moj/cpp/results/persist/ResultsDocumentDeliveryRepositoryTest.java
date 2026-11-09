package uk.gov.moj.cpp.results.persist;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.domains.DocumentDeliveryStatus;
import uk.gov.moj.cpp.domains.DocumentDeliveryType;
import uk.gov.moj.cpp.domains.EmailDeliveryStatus;
import uk.gov.moj.cpp.results.persist.entity.NcesSjpCase;
import uk.gov.moj.cpp.results.persist.entity.ResultsDocumentDeliveryEntity;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class ResultsDocumentDeliveryRepositoryTest extends BaseTransactionalJunit4Test {

    private static final String NCES_DOCUMENT_BLOB_URI = "http://localhost/results/sdg/documents/ENFDocumentOrder_1.pdf";
    private static final String POLICE_PAYLOAD_BLOB_URI = "http://localhost/results/sdg/requests/POLICE_NOTIFICATION_HEARING_RESULTS1.html";
    private static final String POLICE_DOCUMENT_BLOB_URI = "http://localhost/results/sdg/documents/POLICE_NOTIFICATION_HEARING_RESULTS1.html";

    private static final String NCES = DocumentDeliveryType.NCES.name();
    private static final String POLICE_NOTIFICATION = DocumentDeliveryType.POLICE_NOTIFICATION.name();
    private static final String PENDING = DocumentDeliveryStatus.PENDING.name();
    private static final String SUCCESS = DocumentDeliveryStatus.SUCCESS.name();
    private static final String FAILED = DocumentDeliveryStatus.FAILED.name();

    @Inject
    private ResultsDocumentDeliveryRepository resultsDocumentDeliveryRepository;

    private final UUID ncesMaterialId = randomUUID();
    private final UUID policeMaterialId = randomUUID();
    private final UUID firstCaseId = randomUUID();
    private final UUID secondCaseId = randomUUID();

    @Override
    public void setUpBefore() {
        final ResultsDocumentDeliveryEntity nces = new ResultsDocumentDeliveryEntity(ncesMaterialId, NCES, now(), null, NCES_DOCUMENT_BLOB_URI);
        nces.setMaterialStatus(PENDING);
        nces.getSjpCases().add(new NcesSjpCase(firstCaseId, NCES_DOCUMENT_BLOB_URI, PENDING));
        nces.getSjpCases().add(new NcesSjpCase(secondCaseId, NCES_DOCUMENT_BLOB_URI, PENDING));
        resultsDocumentDeliveryRepository.save(nces);

        final ResultsDocumentDeliveryEntity police = new ResultsDocumentDeliveryEntity(policeMaterialId, POLICE_NOTIFICATION, now(),
                POLICE_PAYLOAD_BLOB_URI, POLICE_DOCUMENT_BLOB_URI);
        police.setMaterialStatus(PENDING);
        resultsDocumentDeliveryRepository.save(police);
    }

    @Override
    public void tearDownAfter() {
        resultsDocumentDeliveryRepository.findAll().forEach(resultsDocumentDeliveryRepository::remove);
    }

    @Test
    public void shouldPersistAnNcesDocumentWithEverySjpCaseItWasSentTo() {
        final ResultsDocumentDeliveryEntity nces = resultsDocumentDeliveryRepository.findBy(ncesMaterialId);

        assertThat(nces.getDocumentType(), is(NCES));
        assertThat(nces.getMaterialStatus(), is(PENDING));
        assertThat(nces.getDocumentBlobUri(), is(NCES_DOCUMENT_BLOB_URI));
        assertThat(nces.getSjpCases(), hasSize(2));
        assertThat(nces.getSjpCases().stream().map(NcesSjpCase::getCaseId).toList(), containsInAnyOrder(firstCaseId, secondCaseId));
    }

    @Test
    public void shouldPersistBothBlobUrisOfAPoliceNotification() {
        final ResultsDocumentDeliveryEntity police = resultsDocumentDeliveryRepository.findBy(policeMaterialId);

        assertThat(police.getDocumentType(), is(POLICE_NOTIFICATION));
        assertThat(police.getPayloadBlobUri(), is(POLICE_PAYLOAD_BLOB_URI));
        assertThat(police.getDocumentBlobUri(), is(POLICE_DOCUMENT_BLOB_URI));
        assertThat(police.getSjpCases(), hasSize(0));
    }

    @Test
    public void shouldFindByDocumentBlobUri() {
        final List<ResultsDocumentDeliveryEntity> found = resultsDocumentDeliveryRepository.findByDocumentBlobUri(NCES_DOCUMENT_BLOB_URI);

        assertThat(found.stream().map(ResultsDocumentDeliveryEntity::getMaterialId).toList(), contains(ncesMaterialId));
    }

    @Test
    public void shouldUpdateTheStatusOfOnlyTheGivenSjpCase() {
        final ResultsDocumentDeliveryEntity nces = resultsDocumentDeliveryRepository.findBy(ncesMaterialId);
        assertThat(nces.updateSjpStatus(firstCaseId, SUCCESS), is(true));
        assertThat(nces.updateSjpStatus(secondCaseId, FAILED), is(true));
        resultsDocumentDeliveryRepository.save(nces);

        final ResultsDocumentDeliveryEntity reloaded = resultsDocumentDeliveryRepository.findBy(ncesMaterialId);
        assertThat(reloaded.findSjpCase(firstCaseId).map(NcesSjpCase::getSjpStatus).orElseThrow(), is(SUCCESS));
        assertThat(reloaded.findSjpCase(secondCaseId).map(NcesSjpCase::getSjpStatus).orElseThrow(), is(FAILED));
    }

    @Test
    public void shouldNotUpdateAnSjpCaseTheDocumentWasNotSentTo() {
        final ResultsDocumentDeliveryEntity nces = resultsDocumentDeliveryRepository.findBy(ncesMaterialId);

        assertThat(nces.updateSjpStatus(randomUUID(), SUCCESS), is(false));
        resultsDocumentDeliveryRepository.save(nces);

        final ResultsDocumentDeliveryEntity reloaded = resultsDocumentDeliveryRepository.findBy(ncesMaterialId);
        assertThat(reloaded.getSjpCases(), hasSize(2));
        assertThat(reloaded.getSjpCases().stream().allMatch(c -> PENDING.equals(c.getSjpStatus())), is(true));
    }

    @Test
    public void shouldUpdateTheMaterialStatus() {
        final ResultsDocumentDeliveryEntity police = resultsDocumentDeliveryRepository.findBy(policeMaterialId);
        police.setMaterialStatus(SUCCESS);
        resultsDocumentDeliveryRepository.save(police);

        assertThat(resultsDocumentDeliveryRepository.findBy(policeMaterialId).getMaterialStatus(), is(SUCCESS));
    }

    @Test
    public void shouldUpdateTheEmailStatus() {
        final ResultsDocumentDeliveryEntity police = resultsDocumentDeliveryRepository.findBy(policeMaterialId);
        assertThat(police.getEmailStatus(), is(nullValue()));
        police.setEmailStatus(EmailDeliveryStatus.PENDING.name());
        resultsDocumentDeliveryRepository.save(police);

        assertThat(resultsDocumentDeliveryRepository.findBy(policeMaterialId).getEmailStatus(), is(EmailDeliveryStatus.PENDING.name()));
    }
}
