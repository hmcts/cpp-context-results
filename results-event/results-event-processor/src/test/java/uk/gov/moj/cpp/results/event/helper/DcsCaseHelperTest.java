package uk.gov.moj.cpp.results.event.helper;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.PublishToDcs;
import uk.gov.moj.cpp.results.event.service.DcsCreateCaseRequest;
import uk.gov.moj.cpp.results.event.service.DcsDefendant;
import uk.gov.moj.cpp.results.event.service.DcsOffence;
import uk.gov.moj.cpp.results.event.service.DcsService;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcsCaseHelperTest {

    @Mock
    private DcsService dcsService;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @InjectMocks
    private DcsCaseHelper dcsCaseHelper;

    @BeforeEach
    void setUp(){
        when(featureControlGuard.isFeatureEnabled("StagingDcs")).thenReturn(true);
    }

    @Test
    void givenDcsFeatureNotEnabled_prepareAndSendToDCSIfEligible_shouldNotPrepareAndSendToDCS() {
        when(featureControlGuard.isFeatureEnabled("StagingDcs")).thenReturn(false);
        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);

        PublishToDcs publishToDcs = PublishToDcs.publishToDcs()
                .withCurrentHearing(shareResultsMessage.getHearing())
                .withSharedTime(shareResultsMessage.getSharedTime())
                .withHearingDay(shareResultsMessage.getHearingDay().get())
                .withIsReshare(shareResultsMessage.getIsReshare().get())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(publishToDcs));
        final JsonObjectBuilder jsonProsecutorBuilder = createObjectBuilder();
        jsonProsecutorBuilder
                .add("cpsFlag", true)
                .add("policeFlag", true);
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(0)).createCase(captor.capture(), any());
    }


    @Test
    void givenTwoCasesTwoDefsOneOffenceEachWithOnlyOneQRAndNoPreviousResults_prepareAndSendToDCSIfEligible_shouldCallDCSForTwoCasesWithAddedOffencesForEachDefendant() {

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);
        final List<String> prosecutorCodeList = shareResultsMessage.getHearing().getProsecutionCases().stream().map(prosecutionCase -> prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()).toList();

        PublishToDcs publishToDcs = PublishToDcs.publishToDcs()
                .withCurrentHearing(shareResultsMessage.getHearing())
                .withSharedTime(shareResultsMessage.getSharedTime())
                .withHearingDay(shareResultsMessage.getHearingDay().get())
                .withIsReshare(shareResultsMessage.getIsReshare().get())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(publishToDcs));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(2)).createCase(captor.capture(), any());
        List<DcsCreateCaseRequest> allValues = captor.getAllValues();
        assertEquals(2, allValues.size());

        DcsCreateCaseRequest dcsRequestForFirstCase = allValues.get(0);
        assertEquals("cccc1111-1e20-4c21-916a-81a6c90239e5", dcsRequestForFirstCase.getCaseId().toString());
        assertTrue(prosecutorCodeList.contains(dcsRequestForFirstCase.getProsecutionAuthority()));
        assertEquals(2, dcsRequestForFirstCase.getDefendants().size());
        //
        List<DcsDefendant> defendantsFromdcsRequest = dcsRequestForFirstCase.getDefendants();
        uk.gov.moj.cpp.results.event.service.OffenceDetails firstOffencesFromFirstDefendant = defendantsFromdcsRequest.get(0).getOffencesDetails();
        uk.gov.moj.cpp.results.event.service.OffenceDetails secondOffencesFromFirstDefendant = defendantsFromdcsRequest.get(1).getOffencesDetails();

        assertEquals(1, firstOffencesFromFirstDefendant.getAddedOffences().size());
        assertEquals(0, firstOffencesFromFirstDefendant.getRemovedOffences().size());
        assertEquals(1, secondOffencesFromFirstDefendant.getAddedOffences().size());
        assertEquals(0, secondOffencesFromFirstDefendant.getRemovedOffences().size());
        //
        DcsCreateCaseRequest dcsRequestForSecondCase = allValues.get(1);
        assertEquals("cccc2222-1e20-4c21-916a-81a6c90239e5", dcsRequestForSecondCase.getCaseId().toString());
        assertTrue(prosecutorCodeList.contains(dcsRequestForSecondCase.getProsecutionAuthority()));
        assertEquals(2, dcsRequestForSecondCase.getDefendants().size());
        List<DcsDefendant> defendantsFromSecondPayload = dcsRequestForSecondCase.getDefendants();
        uk.gov.moj.cpp.results.event.service.OffenceDetails firstOffencesFromFirstDefendantSecondCase = defendantsFromSecondPayload.get(0).getOffencesDetails();
        uk.gov.moj.cpp.results.event.service.OffenceDetails secondOffencesFromFirstDefendantSecondCase = defendantsFromSecondPayload.get(1).getOffencesDetails();

        assertEquals(1, firstOffencesFromFirstDefendantSecondCase.getAddedOffences().size());
        assertEquals(0, firstOffencesFromFirstDefendantSecondCase.getRemovedOffences().size());
        assertEquals(1, secondOffencesFromFirstDefendantSecondCase.getAddedOffences().size());
        assertEquals(0, secondOffencesFromFirstDefendantSecondCase.getRemovedOffences().size());

    }
    @Test
    void givenPublishToDcsWithNoResultsButPrevQR_prepareAndSendToDCSIfEligible_shouldCallDCSWithDeletedOffences() {

        final JsonObject shareResultsMessage = getPayload("results.event.publish-to-dcs-with-no-results-but-previous-qualifying-result.json");

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(1)).createCase(captor.capture(), any());
        List<DcsCreateCaseRequest> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());

        DcsCreateCaseRequest dcsRequest = allValues.get(0);
        assertEquals("cccc1111-1e20-4c21-916a-81a6c90239e5", dcsRequest.getCaseId().toString());
        assertEquals(1, dcsRequest.getDefendants().size());
        //
        List<DcsDefendant> defendantsFromdcsRequest = dcsRequest.getDefendants();
        uk.gov.moj.cpp.results.event.service.OffenceDetails firstOffencesFromFirstDefendant = defendantsFromdcsRequest.get(0).getOffencesDetails();

        assertEquals(0, firstOffencesFromFirstDefendant.getAddedOffences().size());
        assertEquals(1, firstOffencesFromFirstDefendant.getRemovedOffences().size());

    }

    @Test
    void givenPublishToDcsWithQRButNoPrvQR_prepareAndSendToDCSIfEligible_shouldCallDCSAddedOffences() {

        final JsonObject shareResultsMessage = getPayload("results.event.publish-to-dcs-with-qualifying-results-but-no-previous-qualifying-result.json");

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(1)).createCase(captor.capture(), any());
        List<DcsCreateCaseRequest> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());

        DcsCreateCaseRequest dcsRequest = allValues.get(0);
        assertEquals("cccc1111-1e20-4c21-916a-81a6c90239e5", dcsRequest.getCaseId().toString());
        assertEquals(1, dcsRequest.getDefendants().size());
        //
        List<DcsDefendant> defendantsFromdcsRequest = dcsRequest.getDefendants();
        uk.gov.moj.cpp.results.event.service.OffenceDetails firstOffencesFromFirstDefendant = defendantsFromdcsRequest.get(0).getOffencesDetails();

        assertEquals(1, firstOffencesFromFirstDefendant.getAddedOffences().size());
        assertEquals(0, firstOffencesFromFirstDefendant.getRemovedOffences().size());

    }
    @Test
    void givenPublishToDcsWithMultipleResultsNQrAndQrAmendedNqrToQr_prepareAndSendToDCSIfEligible_shouldNotCallDCS() {

        final JsonObject shareResultsMessage = getPayload("results.event.publish-to-dcs-with-more-than-one-results-with-nqr-and-qr-amended-prev-nqr-to-qualifying.json");

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(0)).createCase(captor.capture(), any());
    }

    @Test
    void givenPublishToDcsWithTwoQRSAndNoPrevResults_prepareAndSendToDCSIfEligible_shouldCallDCSOneAddedOffence() {

        final JsonObject shareResultsMessage = getPayload("results.event.publish-to-dcs-with-more-than-one-qaresults.json");

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(1)).createCase(captor.capture(), any());
        List<DcsCreateCaseRequest> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());
        DcsCreateCaseRequest dcsRequest = allValues.get(0);
        assertEquals("69827815-aa1f-4bfb-ad35-771bbc8658e7", dcsRequest.getCaseId().toString());
        assertEquals(1, dcsRequest.getDefendants().size());
        List<DcsDefendant> defendantsFromdcsRequest = dcsRequest.getDefendants();
        uk.gov.moj.cpp.results.event.service.OffenceDetails firstOffencesFromFirstDefendant = defendantsFromdcsRequest.get(0).getOffencesDetails();

        assertEquals(1, firstOffencesFromFirstDefendant.getAddedOffences().size());
        assertEquals(0, firstOffencesFromFirstDefendant.getRemovedOffences().size());
    }

    @Test
    void givenPublishToDcsWithPrevQRAndNQRAndAmendedWithQRs_prepareAndSendToDCSIfEligible_shouldNotCallDCS() {

        final JsonObject shareResultsMessage = getPayload("results.event.publish-to-dcs-with-more-than-one-qaresult-removed.json");

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(0)).createCase(captor.capture(), any());

    }

    @Test
    void givenPublishToDcsWithQRAndNQRWithNoPrevQR_prepareAndSendToDCSIfEligible_shouldCallDCSWithAddedOffences() {

        final JsonObject shareResultsMessage = getPayload("results.event.publish-to-dcs-with-qualifying-and-non-qualifying-results-but-no-previous-qualifying-result.json");

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(1)).createCase(captor.capture(), any());
        List<DcsCreateCaseRequest> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());

        DcsCreateCaseRequest dcsRequest = allValues.get(0);
        assertEquals("8d89b04a-afa2-414a-9bc3-d91b41aa4180", dcsRequest.getCaseId().toString());
        assertEquals(1, dcsRequest.getDefendants().size());
        //
        List<DcsDefendant> defendantsFromdcsRequest = dcsRequest.getDefendants();
        uk.gov.moj.cpp.results.event.service.OffenceDetails firstOffencesFromFirstDefendant = defendantsFromdcsRequest.get(0).getOffencesDetails();

        assertEquals(1, firstOffencesFromFirstDefendant.getAddedOffences().size());
        assertEquals(0, firstOffencesFromFirstDefendant.getRemovedOffences().size());

    }

    @Test
    void givenPublishToDcsWithPrevQRAmendedWithNQR_prepareAndSendToDCSIfEligible_shouldCallDCSWithDeletedOffences() {

        final JsonObject shareResultsMessage = getPayload("results.event.publish-to-dcs-with-not-qualifying-results-but-previous-qualifying-result.json");

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(1)).createCase(captor.capture(), any());
        List<DcsCreateCaseRequest> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());

        DcsCreateCaseRequest dcsRequest = allValues.get(0);
        assertEquals("cccc1111-1e20-4c21-916a-81a6c90239e5", dcsRequest.getCaseId().toString());
        assertEquals(1, dcsRequest.getDefendants().size());
        //
        List<DcsDefendant> defendantsFromdcsRequest = dcsRequest.getDefendants();
        uk.gov.moj.cpp.results.event.service.OffenceDetails firstOffencesFromFirstDefendant = defendantsFromdcsRequest.get(0).getOffencesDetails();

        assertEquals(0, firstOffencesFromFirstDefendant.getAddedOffences().size());
        assertEquals(1, firstOffencesFromFirstDefendant.getRemovedOffences().size());

    }

    @Test
    void givenPublishToDcsMiltipleResultsWithPrevQRAmendedWithAnotherNQR_prepareAndSendToDCSIfEligible_shouldNotCallDCS() {

        final JsonObject shareResultsMessage = getPayload("results.event.publish-to-dcs-with-added-another-non-qualifying-result-along-with-previous-qualifying-result.json");

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(0)).createCase(captor.capture(), any());
    }

    @Test
    void givenPublishToDcsWithPrevNQRAmendedWithAnotherQR_prepareAndSendToDCSIfEligible_shouldCallDCSWithAddedOffence() {

        final JsonObject shareResultsMessage = getPayload("results.event.publish-to-dcs-with-added-another-qualifying-result-along-with-previous-non-qualifying-result.json");

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(1)).createCase(captor.capture(), any());

        List<DcsCreateCaseRequest> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());

        DcsCreateCaseRequest dcsRequest = allValues.get(0);
        assertEquals("a181477a-c953-48d1-ae90-44ef263d40f0", dcsRequest.getCaseId().toString());
        assertEquals(1, dcsRequest.getDefendants().size());
        //
        List<DcsDefendant> defendantsFromdcsRequest = dcsRequest.getDefendants();
        uk.gov.moj.cpp.results.event.service.OffenceDetails offenceDetails = defendantsFromdcsRequest.get(0).getOffencesDetails();

        assertEquals(1, offenceDetails.getAddedOffences().size());
        assertEquals(0, offenceDetails.getRemovedOffences().size());
    }

    @Test
    void givenPublishToDcsForLegalEntityWithQR_prepareAndSendToDCSIfEligible_shouldCallDCSWithAddedOffences() {

        final JsonObject shareResultsMessage = getPayload("results.events.publish-to-dcs-with-added-deleted-offences-for-legal-entity.json");

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(shareResultsMessage));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(1)).createCase(captor.capture(), any());
        List<DcsCreateCaseRequest> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());
        DcsCreateCaseRequest dcsRequest = allValues.get(0);
        assertEquals("f5a2c7d0-feb1-4dc8-bbdc-1225d6e193e4", dcsRequest.getCaseId().toString());
        assertEquals(1, dcsRequest.getDefendants().size());
        //
        List<DcsDefendant> defendantsFromdcsRequest = dcsRequest.getDefendants();
        final DcsDefendant defendant = defendantsFromdcsRequest.get(0);
        assertEquals("test ltd", defendant.getDefendantOrganisation().getName());
        uk.gov.moj.cpp.results.event.service.OffenceDetails offenceDetails = defendant.getOffencesDetails();

        assertEquals(1, offenceDetails.getAddedOffences().size());
        assertEquals(0, offenceDetails.getRemovedOffences().size());

    }
    @Test
    void givenPublishToDcsWithTwoDefendantsWithTwoOffencesEachAndQRsEachWithNoPrevResults_prepareAndSendToDCSIfEligible_shouldCallDCSAddedOffences() {

        final JsonObject payload = getPayload("results.events.publish-to-dcs-with-multiple-defendants-with-qualified-results-added-offences.json");

        final PublicHearingResulted shareResultsMessage = jsonObjectToObjectConverter.convert(payload, PublicHearingResulted.class);

        PublishToDcs publishToDcs = PublishToDcs.publishToDcs()
                .withCurrentHearing(shareResultsMessage.getHearing())
                .withSharedTime(shareResultsMessage.getSharedTime())
                .withHearingDay(shareResultsMessage.getHearingDay().get())
                .withIsReshare(shareResultsMessage.getIsReshare().get())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(publishToDcs));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(1)).createCase(captor.capture(), any());
        List<DcsCreateCaseRequest> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());

        DcsCreateCaseRequest dcsCreateCaseRequest = allValues.get(0);
        assertEquals("64f5b330-9987-4c23-8b7a-eb33187e1f44", dcsCreateCaseRequest.getCaseId().toString());
        assertEquals(2, dcsCreateCaseRequest.getDefendants().size());
        //
        List<DcsDefendant> dcsDefendantList = dcsCreateCaseRequest.getDefendants();
        final DcsDefendant firstDefendant = dcsDefendantList.get(0);
        uk.gov.moj.cpp.results.event.service.OffenceDetails offencesFromFirstDefendant = firstDefendant.getOffencesDetails();

        final DcsDefendant secondDefendant = dcsDefendantList.get(1);
        uk.gov.moj.cpp.results.event.service.OffenceDetails offencesFromSecondDefendant = secondDefendant.getOffencesDetails();

        DcsOffence firstOffenceFromFirstDefendant = offencesFromFirstDefendant.getAddedOffences().stream().findFirst().orElse(null);
        assertNotNull(firstOffenceFromFirstDefendant);
        assertEquals("125b2cd0-a185-4da0-9487-ab7b68b1bcb7", firstOffenceFromFirstDefendant.getOffenceId().toString());

        DcsOffence secondOffenceFromFirstDefendant = offencesFromFirstDefendant.getAddedOffences().stream().skip(1).findFirst().orElse(null);
        assertNotNull(secondOffenceFromFirstDefendant);
        assertEquals("f9917511-590c-457d-b043-3a2ccc536d2e", secondOffenceFromFirstDefendant.getOffenceId().toString());

        assertEquals(2, offencesFromFirstDefendant.getAddedOffences().size());
        assertEquals(0, offencesFromFirstDefendant.getRemovedOffences().size());
        assertEquals(2, firstDefendant.getHearings().size());
        assertEquals("Birmingham Crown Court", firstDefendant.getHearings().get(0).getCourtCentre());
        assertEquals("Birmingham Crown Court", firstDefendant.getHearings().get(1).getCourtCentre());

        DcsOffence firstOffenceFromSecondDefendant = offencesFromSecondDefendant.getAddedOffences().stream().findFirst().orElse(null);
        assertNotNull(firstOffenceFromSecondDefendant);
        assertEquals("224c91a8-fb33-41d0-9314-ad6168523e9a", firstOffenceFromSecondDefendant.getOffenceId().toString());
        DcsOffence secondOffenceFromSecondDefendant = offencesFromSecondDefendant.getAddedOffences().stream().skip(1).findFirst().orElse(null);
        assertNotNull(secondOffenceFromSecondDefendant);
        assertEquals("ff057c12-8206-4595-9208-6dbc7fa8de8e", secondOffenceFromSecondDefendant.getOffenceId().toString());
        assertEquals(2, offencesFromSecondDefendant.getAddedOffences().size());
        assertEquals(0, offencesFromSecondDefendant.getRemovedOffences().size());

        assertEquals("2025-07-21", secondDefendant.getHearings().get(0).getHearingDate().toString());
        assertEquals("Birmingham Crown Court", secondDefendant.getHearings().get(0).getCourtCentre());
        assertEquals("2025-07-21", secondDefendant.getHearings().get(0).getHearingDate().toString());
    }

    @Test
    void givenPublishToDcsWithTwoDefsWithOneDefOffenceNotQualified_prepareAndSendToDCSIfEligible_shouldCallDCSWithOneDefendantAddedOffence() {
        final JsonObject payload = getPayload("results.events.publish-to-dcs-two-defendants-with-single-offence-and-one-defendant-offence-nq.json");

        final PublicHearingResulted shareResultsMessage = jsonObjectToObjectConverter.convert(payload, PublicHearingResulted.class);
        PublishToDcs publishToDcs = PublishToDcs.publishToDcs()
                .withCurrentHearing(shareResultsMessage.getHearing())
                .withSharedTime(shareResultsMessage.getSharedTime())
                .withHearingDay(shareResultsMessage.getHearingDay().get())
                .withIsReshare(shareResultsMessage.getIsReshare().get())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(publishToDcs));

        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(1)).createCase(captor.capture(), any());
        List<DcsCreateCaseRequest> allValues = captor.getAllValues();
        assertEquals( 1, allValues.size());

        DcsCreateCaseRequest dcsRequest = allValues.get(0);
        assertEquals("cccc1111-1e20-4c21-916a-81a6c90239e5", dcsRequest.getCaseId().toString());
        assertEquals(1, dcsRequest.getDefendants().size());
        //
        List<DcsDefendant> defendantsFromDcsRequest = dcsRequest.getDefendants();
        uk.gov.moj.cpp.results.event.service.OffenceDetails offenceDetails = defendantsFromDcsRequest.get(0).getOffencesDetails();

        assertEquals(1, offenceDetails.getAddedOffences().size());
        assertEquals(0, offenceDetails.getRemovedOffences().size());

        assertEquals("2025-02-28", defendantsFromDcsRequest.get(0).getHearings().get(0).getHearingDate().toString());
        assertEquals("Bexley Magistrates' Court", defendantsFromDcsRequest.get(0).getHearings().get(0).getCourtCentre());
    }

    @Test
    void givenPublishToDcsWithNullValues_prepareAndSendToDCSIfEligible_shouldNotCallDCS() {
        final JsonObject payload = getPayload("results.events.hearing-results-added-for-day-for-dcs-with-added-deleted-offences-with-null-flags.json");

        final PublishToDcs publishToDcs = jsonObjectToObjectConverter.convert(payload, PublishToDcs.class);

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(publishToDcs));
        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);

        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(0)).createCase(captor.capture(), any());
    }

    @Test
    void givenPublishToDcsWithTwoCasesAndOneNotQualified_prepareAndSendToDCSIfEligible_shouldCallDCSWithOneQualifyingCase() {
        final JsonObject payload = getPayload("results.events.publish-to-dcs-with-two-cases-and-one-case-not-qualified.json");

        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(payload, PublicHearingResulted.class);

        PublishToDcs publishToDcs = PublishToDcs.publishToDcs()
                .withCurrentHearing(publicHearingResulted.getHearing())
                .withSharedTime(publicHearingResulted.getSharedTime())
                .withHearingDay(publicHearingResulted.getHearingDay().get())
                .withIsReshare(publicHearingResulted.getIsReshare().get())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("results.event.publish-to-dcs"),
                objectToJsonObjectConverter.convert(publishToDcs));

        dcsCaseHelper.prepareAndSendToDCSIfEligible(envelope);
        //
        ArgumentCaptor<DcsCreateCaseRequest> captor = ArgumentCaptor.forClass(DcsCreateCaseRequest.class);
        verify(dcsService, times(1)).createCase(captor.capture(), any());
        List<DcsCreateCaseRequest> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());

        DcsCreateCaseRequest dcsCreateCaseRequest = allValues.get(0);
        assertEquals("cccc2222-1e20-4c21-916a-81a6c90239e6", dcsCreateCaseRequest.getCaseId().toString());
        assertEquals(2, dcsCreateCaseRequest.getDefendants().size());
        List<DcsDefendant> defendantsFromSecondPayload = dcsCreateCaseRequest.getDefendants();
        uk.gov.moj.cpp.results.event.service.OffenceDetails firstOffencesFromFirstDefendantSecondCase = defendantsFromSecondPayload.get(0).getOffencesDetails();
        uk.gov.moj.cpp.results.event.service.OffenceDetails secondOffencesFromFirstDefendantSecondCase = defendantsFromSecondPayload.get(1).getOffencesDetails();

        assertEquals("0aa7ad5b-4d82-40d2-8330-6af252335994", firstOffencesFromFirstDefendantSecondCase.getAddedOffences().stream().findFirst().get().getOffenceId().toString());
        assertEquals(1, firstOffencesFromFirstDefendantSecondCase.getAddedOffences().size());
        assertEquals(0, firstOffencesFromFirstDefendantSecondCase.getRemovedOffences().size());
        assertEquals("b5cec2cc-e87f-49c0-89eb-041f552467ab", secondOffencesFromFirstDefendantSecondCase.getAddedOffences().stream().findFirst().get().getOffenceId().toString());
        assertEquals(1, secondOffencesFromFirstDefendantSecondCase.getAddedOffences().size());
        assertEquals(0, secondOffencesFromFirstDefendantSecondCase.getRemovedOffences().size());

    }

    private static JsonObject getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = JsonObjects.createReader(new StringReader(request));
        return reader.readObject();
    }
}