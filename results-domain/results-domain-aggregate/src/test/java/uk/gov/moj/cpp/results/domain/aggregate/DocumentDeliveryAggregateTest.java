package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.FAILED;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.PENDING;
import static uk.gov.moj.cpp.domains.DocumentDeliveryStatus.SUCCESS;
import static uk.gov.moj.cpp.domains.DocumentDeliveryType.NCES;
import static uk.gov.moj.cpp.domains.DocumentDeliveryType.POLICE_NOTIFICATION;
import static uk.gov.moj.cpp.domains.EmailDeliveryStatus.NOT_REQUIRED;
import static uk.gov.moj.cpp.domains.EmailDeliveryStatus.VALIDATION_FAILED;

import uk.gov.moj.cpp.results.domain.aggregate.DocumentDeliveryRequest.SjpCase;
import uk.gov.moj.cpp.results.domain.event.DocumentDeletedFromBlob;
import uk.gov.moj.cpp.results.domain.event.DocumentDeliveryRecorded;
import uk.gov.moj.cpp.results.domain.event.SjpCases;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentDeliveryAggregateTest {

    private static final String PAYLOAD_BLOB_URI = "http://localhost/results/sdg/requests/POLICE_NOTIFICATION_HEARING_RESULTS1.html";
    private static final String DOCUMENT_BLOB_URI = "http://localhost/results/sdg/documents/ENFDocumentOrder_1.pdf";

    private final UUID materialId = randomUUID();
    private final UUID firstCaseId = randomUUID();
    private final UUID secondCaseId = randomUUID();

    private DocumentDeliveryAggregate aggregate;

    @BeforeEach
    void setUp() {
        aggregate = new DocumentDeliveryAggregate();
    }

    @Test
    void shouldStartTrackingAnNcesDocumentWithEverySjpCaseItWasSentTo() {
        final List<Object> events = record(ncesPending(firstCaseId, secondCaseId));

        assertThat(events, hasSize(1));
        final DocumentDeliveryRecorded recorded = (DocumentDeliveryRecorded) events.get(0);
        assertThat(recorded.getMaterialId(), is(materialId));
        assertThat(recorded.getDocumentType(), is(NCES.name()));
        assertThat(recorded.getMaterialStatus(), is(PENDING.name()));
        assertThat(recorded.getDocumentBlobUri(), is(DOCUMENT_BLOB_URI));
        assertThat(recorded.getSjpCases().stream().map(SjpCases::getCaseId).toList(), contains(firstCaseId, secondCaseId));
        assertThat(recorded.getSjpCases().stream().map(SjpCases::getSjpStatus).toList(), contains(PENDING.name(), PENDING.name()));
    }

    @Test
    void shouldStartTrackingAPoliceNotificationWithBothBlobUris() {
        final List<Object> events = record(new DocumentDeliveryRequest(materialId, POLICE_NOTIFICATION.name(), PENDING.name(),
                PAYLOAD_BLOB_URI, DOCUMENT_BLOB_URI, null, null));

        final DocumentDeliveryRecorded recorded = (DocumentDeliveryRecorded) events.get(0);
        assertThat(recorded.getPayloadBlobUri(), is(PAYLOAD_BLOB_URI));
        assertThat(recorded.getDocumentBlobUri(), is(DOCUMENT_BLOB_URI));
        assertThat(recorded.getSjpCases(), is(nullValue()));
    }

    @Test
    void shouldTrackACaseListedTwiceOnce() {
        final List<Object> events = record(ncesPending(firstCaseId, firstCaseId));

        assertThat(((DocumentDeliveryRecorded) events.get(0)).getSjpCases(), hasSize(1));
    }

    @Test
    void shouldIgnoreAStatusOfAMaterialThatIsNotTracked() {
        // e.g. material-added of the file service path
        assertThat(record(materialStatus(SUCCESS.name())), is(empty()));
    }

    @Test
    void shouldNotStartTrackingWithoutADocumentType() {
        assertThat(record(new DocumentDeliveryRequest(materialId, null, PENDING.name(), null, DOCUMENT_BLOB_URI, null, null)), is(empty()));
    }

    @Test
    void shouldRecordTheMaterialOutcome() {
        record(ncesPending(firstCaseId));

        final DocumentDeliveryRecorded recorded = (DocumentDeliveryRecorded) record(materialStatus(FAILED.name())).get(0);

        assertThat(recorded.getMaterialStatus(), is(FAILED.name()));
        assertThat(recorded.getDocumentType(), is(nullValue()));
        assertThat(recorded.getDocumentBlobUri(), is(nullValue()));
        assertThat(recorded.getSjpCases(), is(nullValue()));
    }

    @Test
    void shouldNotTakeACompletedMaterialStatusBackToPending() {
        record(ncesPending(firstCaseId));
        record(materialStatus(SUCCESS.name()));

        assertThat(record(materialStatus(PENDING.name())), is(empty()));
    }

    @Test
    void shouldRaiseNothingForARedeliveredRecord() {
        record(ncesPending(firstCaseId));
        record(materialStatus(SUCCESS.name()));

        assertThat(record(ncesPending(firstCaseId)), is(empty()));
        assertThat(record(materialStatus(SUCCESS.name())), is(empty()));
    }

    @Test
    void shouldRecordTheEmailStatus() {
        record(ncesPending(firstCaseId));

        final DocumentDeliveryRecorded pending = (DocumentDeliveryRecorded) record(emailStatus(PENDING.name())).get(0);
        final DocumentDeliveryRecorded sent = (DocumentDeliveryRecorded) record(emailStatus(SUCCESS.name())).get(0);

        assertThat(pending.getEmailStatus(), is(PENDING.name()));
        assertThat(sent.getEmailStatus(), is(SUCCESS.name()));
        assertThat(sent.getMaterialStatus(), is(nullValue()));
        assertThat(record(emailStatus(PENDING.name())), is(empty()));
    }

    @Test
    void shouldStartAFlowThatSendsAnEmailWithoutAnEmailStatus() {
        final DocumentDeliveryRecorded started = (DocumentDeliveryRecorded) record(ncesPending(firstCaseId)).get(0);

        assertThat(started.getEmailStatus(), is(nullValue()));
    }

    @Test
    void shouldStartAFlowThatSendsNoEmailWithTheEmailNotRequired() {
        final DocumentDeliveryRecorded started = (DocumentDeliveryRecorded) record(new DocumentDeliveryRequest(materialId, NCES.name(),
                PENDING.name(), null, DOCUMENT_BLOB_URI, NOT_REQUIRED.name(), null)).get(0);

        assertThat(started.getEmailStatus(), is(NOT_REQUIRED.name()));
        assertThat(record(emailStatus(PENDING.name())), is(empty()));
    }

    @Test
    void shouldRecordThatNoEmailIsRequiredAndKeepIt() {
        // a flow that sends an email decides not to send it (e.g. no recipient address)
        record(ncesPending(firstCaseId));

        final DocumentDeliveryRecorded recorded = (DocumentDeliveryRecorded) record(emailStatus(NOT_REQUIRED.name())).get(0);

        assertThat(recorded.getEmailStatus(), is(NOT_REQUIRED.name()));
        assertThat(record(emailStatus(PENDING.name())), is(empty()));
        assertThat(record(emailStatus(NOT_REQUIRED.name())), is(empty()));
    }

    @Test
    void shouldUpdateOnlyTheSjpCaseTheOutcomeIsFor() {
        record(ncesPending(firstCaseId, secondCaseId));

        final DocumentDeliveryRecorded recorded = (DocumentDeliveryRecorded) record(sjpStatus(new SjpCase(secondCaseId, SUCCESS.name()))).get(0);

        assertThat(recorded.getSjpCases(), hasSize(1));
        assertThat(recorded.getSjpCases().get(0).getCaseId(), is(secondCaseId));
        assertThat(recorded.getSjpCases().get(0).getSjpStatus(), is(SUCCESS.name()));
        assertThat(recorded.getMaterialStatus(), is(nullValue()));
    }

    @Test
    void shouldIgnoreAnSjpCaseTheDocumentWasNotSentTo() {
        record(ncesPending(firstCaseId));

        assertThat(record(sjpStatus(new SjpCase(randomUUID(), SUCCESS.name()))), is(empty()));
    }

    @Test
    void shouldNotTakeACompletedSjpStatusBackToPending() {
        record(ncesPending(firstCaseId));
        record(sjpStatus(new SjpCase(firstCaseId, SUCCESS.name())));

        assertThat(record(sjpStatus(new SjpCase(firstCaseId, PENDING.name()))), is(empty()));
    }

    @Test
    void shouldNotReplaceAKnownBlobUri() {
        record(ncesPending(firstCaseId));

        assertThat(record(new DocumentDeliveryRequest(materialId, null, null, null, "http://localhost/other.pdf", null, null)), is(empty()));
    }

    @Test
    void shouldDeleteTheBlobsOnceMaterialAndEmailSucceedAndEverySjpCaseSucceeds() {
        record(ncesPending(firstCaseId, secondCaseId));
        assertThat(record(materialStatus(SUCCESS.name())), hasSize(1));
        assertThat(record(emailStatus(SUCCESS.name())), hasSize(1));
        // one SJP case is still PENDING
        assertThat(record(sjpStatus(new SjpCase(firstCaseId, SUCCESS.name()))), hasSize(1));

        final List<Object> events = record(sjpStatus(new SjpCase(secondCaseId, SUCCESS.name())));

        assertThat(events, hasSize(2));
        assertThat(events.get(0), instanceOf(DocumentDeliveryRecorded.class));
        final DocumentDeletedFromBlob deleted = (DocumentDeletedFromBlob) events.get(1);
        assertThat(deleted.getMaterialId(), is(materialId));
        assertThat(deleted.getPayloadBlobUri(), is(nullValue()));
        assertThat(deleted.getDocumentBlobUri(), is(DOCUMENT_BLOB_URI));
    }

    @Test
    void shouldDeleteTheBlobsWhicheverStatusCompletesTheDelivery() {
        // the statuses arrive in any order: here the material outcome comes last
        record(ncesPending(firstCaseId));
        record(sjpStatus(new SjpCase(firstCaseId, SUCCESS.name())));
        record(emailStatus(NOT_REQUIRED.name()));

        final List<Object> events = record(materialStatus(SUCCESS.name()));

        assertThat(events, hasSize(2));
        assertThat(events.get(1), instanceOf(DocumentDeletedFromBlob.class));
    }

    @Test
    void shouldDeleteBothBlobsOfAPoliceNotificationWithNoSjpCase() {
        record(new DocumentDeliveryRequest(materialId, POLICE_NOTIFICATION.name(), PENDING.name(),
                PAYLOAD_BLOB_URI, DOCUMENT_BLOB_URI, null, null));
        record(emailStatus(SUCCESS.name()));

        final List<Object> events = record(materialStatus(SUCCESS.name()));

        assertThat(events, hasSize(2));
        final DocumentDeletedFromBlob deleted = (DocumentDeletedFromBlob) events.get(1);
        assertThat(deleted.getPayloadBlobUri(), is(PAYLOAD_BLOB_URI));
        assertThat(deleted.getDocumentBlobUri(), is(DOCUMENT_BLOB_URI));
    }

    @Test
    void shouldNotDeleteTheBlobsWhileAnSjpCaseIsNotSuccessful() {
        record(ncesPending(firstCaseId, secondCaseId));
        record(materialStatus(SUCCESS.name()));
        record(emailStatus(SUCCESS.name()));
        record(sjpStatus(new SjpCase(firstCaseId, SUCCESS.name())));

        assertThat(record(sjpStatus(new SjpCase(secondCaseId, FAILED.name()))), hasSize(1));
    }

    @Test
    void shouldNotDeleteTheBlobsWhileTheEmailIsNotSent() {
        record(ncesPending());
        record(materialStatus(SUCCESS.name()));

        assertThat(record(emailStatus(PENDING.name())), hasSize(1));
        assertThat(record(emailStatus(FAILED.name())), hasSize(1));
    }

    @Test
    void shouldNotDeleteTheBlobsWhenTheMaterialFails() {
        record(ncesPending());
        record(emailStatus(NOT_REQUIRED.name()));

        assertThat(record(materialStatus(FAILED.name())), hasSize(1));
    }

    @Test
    void shouldDeleteTheBlobsOnlyOnce() {
        record(ncesPending());
        record(emailStatus(NOT_REQUIRED.name()));
        assertThat(record(materialStatus(SUCCESS.name())), hasSize(2));

        // a later change - e.g. a late email outcome - does not delete them again
        assertThat(record(emailStatus(SUCCESS.name())), hasSize(1));
    }

    @Test
    void shouldDeleteTheBlobsOfAnNcesDocumentSentToNoSjpCase() {
        // e.g. the migrated inactive NCES flow
        record(ncesPending());
        record(materialStatus(SUCCESS.name()));

        final List<Object> events = record(emailStatus(SUCCESS.name()));

        assertThat(events, hasSize(2));
        assertThat(((DocumentDeletedFromBlob) events.get(1)).getDocumentBlobUri(), is(DOCUMENT_BLOB_URI));
    }

    @Test
    void shouldDeleteTheBlobsWhenAFailedSjpFilingSucceedsOnRetry() {
        record(ncesPending(firstCaseId));
        record(materialStatus(SUCCESS.name()));
        record(emailStatus(SUCCESS.name()));
        assertThat(record(sjpStatus(new SjpCase(firstCaseId, FAILED.name()))), hasSize(1));

        final List<Object> events = record(sjpStatus(new SjpCase(firstCaseId, SUCCESS.name())));

        assertThat(events, hasSize(2));
        assertThat(events.get(1), instanceOf(DocumentDeletedFromBlob.class));
    }

    @Test
    void shouldNotDeleteTheBlobsWhenTheEmailFailsValidation() {
        record(ncesPending(firstCaseId));
        record(materialStatus(SUCCESS.name()));
        record(sjpStatus(new SjpCase(firstCaseId, SUCCESS.name())));

        assertThat(record(emailStatus(VALIDATION_FAILED.name())), hasSize(1));
    }

    @Test
    void shouldNotDeleteTheBlobsBeforeTheFlowDecidesOnTheEmail() {
        // material and every SJP case succeeded, but the email status is still undecided
        record(ncesPending(firstCaseId));
        record(materialStatus(SUCCESS.name()));

        assertThat(record(sjpStatus(new SjpCase(firstCaseId, SUCCESS.name()))), hasSize(1));
    }

    @Test
    void shouldNotRaiseADeletionForADocumentWithNoBlob() {
        record(new DocumentDeliveryRequest(materialId, NCES.name(), PENDING.name(), null, null, NOT_REQUIRED.name(), null));

        assertThat(record(materialStatus(SUCCESS.name())), hasSize(1));
    }

    @Test
    void shouldNotDeleteTheBlobsAgainWhenAnSjpStatusChangesAfterTheDeletion() {
        record(ncesPending(firstCaseId));
        record(emailStatus(NOT_REQUIRED.name()));
        record(materialStatus(SUCCESS.name()));
        assertThat(record(sjpStatus(new SjpCase(firstCaseId, SUCCESS.name()))), hasSize(2));

        assertThat(record(sjpStatus(new SjpCase(firstCaseId, FAILED.name()))), hasSize(1));
        assertThat(record(sjpStatus(new SjpCase(firstCaseId, SUCCESS.name()))), hasSize(1));
    }

    @Test
    void shouldNotDeleteTheBlobsAgainAfterTheStreamIsReplayed() {
        final List<Object> stream = new ArrayList<>();
        stream.addAll(record(ncesPending()));
        stream.addAll(record(emailStatus(NOT_REQUIRED.name())));
        stream.addAll(record(materialStatus(SUCCESS.name())));
        assertThat(stream.get(stream.size() - 1), instanceOf(DocumentDeletedFromBlob.class));

        // the aggregate is rebuilt from its events, as when it is loaded for the next command
        final DocumentDeliveryAggregate replayed = new DocumentDeliveryAggregate();
        stream.forEach(replayed::apply);

        // a later change - e.g. a late email outcome - completes the delivery again but deletes nothing
        assertThat(replayed.recordDocumentDelivery(emailStatus(SUCCESS.name())).toList(), hasSize(1));
    }

    private List<Object> record(final DocumentDeliveryRequest request) {
        return aggregate.recordDocumentDelivery(request).toList();
    }

    private DocumentDeliveryRequest ncesPending(final UUID... caseIds) {
        return new DocumentDeliveryRequest(materialId, NCES.name(), PENDING.name(), null, DOCUMENT_BLOB_URI, null,
                Arrays.stream(caseIds).map(caseId -> new SjpCase(caseId, PENDING.name())).toList());
    }

    private DocumentDeliveryRequest materialStatus(final String status) {
        return new DocumentDeliveryRequest(materialId, null, status, null, null, null, null);
    }

    private DocumentDeliveryRequest emailStatus(final String status) {
        return new DocumentDeliveryRequest(materialId, null, null, null, null, status, null);
    }

    private DocumentDeliveryRequest sjpStatus(final SjpCase sjpCase) {
        return new DocumentDeliveryRequest(materialId, null, null, null, null, null, List.of(sjpCase));
    }
}
