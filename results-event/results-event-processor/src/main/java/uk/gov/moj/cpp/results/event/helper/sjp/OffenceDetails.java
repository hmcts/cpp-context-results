package uk.gov.moj.cpp.results.event.helper.sjp;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.core.courts.AllocationDecision.allocationDecision;
import static uk.gov.justice.core.courts.OffenceDetails.offenceDetails;
import static uk.gov.justice.core.courts.OffenceFacts.offenceFacts;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.VehicleCode;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.sjp.results.CaseOffence;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

public class OffenceDetails {

    private static final String FINAL_DISPOSAL_DEFAULT_VALUE = "N";

    ReferenceCache referenceCache;

    @Inject
    public OffenceDetails(final ReferenceCache referenceCache) {
        this.referenceCache = referenceCache;
    }

    public List<uk.gov.justice.core.courts.OffenceDetails> buildOffences(final uk.gov.justice.sjp.results.CaseDefendant sjpDefendant, final ZonedDateTime dateAndTimeOfSession, final UUID sessionId) {
        final List<CaseOffence> caseOffenceList = sjpDefendant.getOffences();

        final List<uk.gov.justice.core.courts.OffenceDetails> offenceDetailsList = new ArrayList<>();

        for (final CaseOffence caseOffence : caseOffenceList) {
            offenceDetailsList.add(offenceDetails()
                    .withAllocationDecision(buildAllocationDecision(caseOffence.getBaseOffenceDetails().getOffenceId(), sessionId, caseOffence.getModeOfTrial(), dateAndTimeOfSession))
                    .withArrestDate(caseOffence.getBaseOffenceDetails().getArrestDate())
                    .withChargeDate(caseOffence.getBaseOffenceDetails().getChargeDate())
                    .withConvictingCourt(caseOffence.getConvictingCourt())
                    .withConvictionDate(caseOffence.getConvictionDate().toLocalDate())
                    .withEndDate(caseOffence.getBaseOffenceDetails().getOffenceEndDate())
                    .withFinalDisposal(FINAL_DISPOSAL_DEFAULT_VALUE)
                    .withId(caseOffence.getBaseOffenceDetails().getOffenceId())
                    .withJudicialResults(new JudicialResult(referenceCache).buildJudicialResults(caseOffence.getResults(), dateAndTimeOfSession, sessionId)) //Not Available
                    .withModeOfTrial(valueOf(caseOffence.getModeOfTrial()))
                    .withFinding(caseOffence.getFinding())
                    .withOffenceCode(caseOffence.getBaseOffenceDetails().getOffenceCode())
                    .withOffenceDateCode(caseOffence.getBaseOffenceDetails().getOffenceDateCode()) // it should be zonedatetime
                    .withPlea(plea(caseOffence).orElse(null))
                    .withOffenceFacts(buildOffenceFacts(caseOffence))
                    .withOffenceSequenceNumber(caseOffence.getBaseOffenceDetails().getOffenceSequenceNumber())
                    .withStartDate(caseOffence.getInitiatedDate().toLocalDate())
                    .withWording(caseOffence.getBaseOffenceDetails().getOffenceWording())
                    .build());
        }
        return offenceDetailsList;
    }

    private Optional<Plea> plea(final CaseOffence caseOffence) {
        final uk.gov.justice.sjp.results.Plea plea = caseOffence.getPlea();
        if (plea != null && plea.getPleaType() != null) {
            return Optional.ofNullable(Plea.plea()
                    .withPleaValue(plea.getPleaType().toString())
                    .withOffenceId(caseOffence.getBaseOffenceDetails().getOffenceId())
                    .withPleaDate(plea.getPleaDate().toLocalDate()).build());
        }
        return Optional.empty();
    }

    private AllocationDecision buildAllocationDecision(final UUID offenceId, final UUID sessionId, final Integer modeOfTrial, final ZonedDateTime dateAndTimeOfSession) {
        final String convertedModeOfTrial = format("0%d", modeOfTrial);
        final JsonEnvelope context = envelopeFrom(metadataBuilder().withName("public.sjp.case-resulted").withId(randomUUID()).build(), NULL);
        final Optional<AllocationDecision> allocationDecisionFromRefData = referenceCache.getAllocationDecision(context, convertedModeOfTrial);
        if (allocationDecisionFromRefData.isPresent()) {
            final AllocationDecision allocationDecision = allocationDecisionFromRefData.get();
            return allocationDecision()
                    .withOriginatingHearingId(sessionId)
                    .withOffenceId(offenceId)
                    .withSequenceNumber(allocationDecision.getSequenceNumber())
                    .withAllocationDecisionDate(dateAndTimeOfSession.toLocalDate())
                    .withMotReasonCode(allocationDecision.getMotReasonCode())
                    .withMotReasonDescription(allocationDecision.getMotReasonDescription())
                    .withMotReasonId(allocationDecision.getMotReasonId())
                    .build();
        }
        return allocationDecision().build();
    }

    private OffenceFacts buildOffenceFacts(final CaseOffence caseOffence) {
        return offenceFacts()
                .withAlcoholReadingAmount(caseOffence.getBaseOffenceDetails().getAlcoholLevelAmount())
                .withAlcoholReadingMethodCode(caseOffence.getBaseOffenceDetails().getAlcoholLevelMethod())
                .withVehicleCode(vehicleCode(caseOffence).orElse(null))
                .withVehicleRegistration(caseOffence.getBaseOffenceDetails().getVehicleRegistrationMark())
                .build();
    }

    private Optional<VehicleCode> vehicleCode(final CaseOffence caseOffence) {
        final String vehicleCode = caseOffence.getBaseOffenceDetails().getVehicleCode();
        if (vehicleCode != null) {
            return VehicleCode.valueFor(vehicleCode);
        }
        return Optional.empty();
    }
}
