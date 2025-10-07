package uk.gov.moj.cpp.results.event.service;

import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S3776")
public class DcsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DcsService.class);

    private static final String SUBMIT_DCS_CASE_RECORD = "stagingdcs.submit-dcs-case-record";
    public static final String CASE_ID = "caseId";
    public static final String COURT_CENTRE = "courtCentre";
    public static final String HEARING_DATE = "hearingDate";
    public static final String PROSECUTION_AUTHORITY = "prosecutionAuthority";
    public static final String CASE_URN = "caseUrn";
    public static final String DEFENDANTS = "defendants";
    public static final String HEARINGS = "hearings";
    public static final String ID = "id";
    public static final String BAIL_STATUS = "bailStatus";
    public static final String INTERPRETER_LANGUAGE = "interpreterLanguage";
    public static final String INTERPRETER_INFORMATION = "interpreterInformation";
    public static final String FORENAME = "forename";
    public static final String SURNAME = "surname";
    public static final String MIDDLE_NAME = "middleName";
    public static final String DATE_OF_BIRTH = "dateOfBirth";
    public static final String DEFENDANT_PERSON = "defendantPerson";
    public static final String DEFENDANT_ORGANISATION = "defendantOrganisation";
    public static final String OFFENCES_DETAILS = "offencesDetails";
    public static final String ADDED_OFFENCES = "addedOffences";
    public static final String REMOVED_OFFENCES = "removedOffences";
    public static final String OFFENCE_ID = "offenceId";
    public static final String OFFENCE_CODE = "offenceCode";
    public static final String NAME = "name";

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    public void createCase(final DcsCreateCaseRequest request, final JsonEnvelope envelope) {

        final JsonObjectBuilder dcsCaseRecordBuilder = createObjectBuilder()
                .add(CASE_ID, String.valueOf(request.getCaseId()))
                .add(PROSECUTION_AUTHORITY, request.getProsecutionAuthority());
                addIfNotNull(dcsCaseRecordBuilder, CASE_URN, Optional.ofNullable(request.getCaseUrn()));

        final JsonArrayBuilder defendantsArrayBuilder = buildDefendantsArray(request);

        dcsCaseRecordBuilder.add(DEFENDANTS, defendantsArrayBuilder);

        JsonObject payload = dcsCaseRecordBuilder.build();
        LOGGER.info("Calling Staging DCS with case Id" + " - {}", request.getCaseId());
        LOGGER.info("Calling Staging DCS with payload" + " - {}", payload);

        sender.sendAsAdmin(Envelope.envelopeFrom(
                metadataFrom(envelope.metadata()).withName(SUBMIT_DCS_CASE_RECORD),
                payload
        ));
    }

    private JsonArrayBuilder buildDefendantsArray(final DcsCreateCaseRequest request) {

        JsonArrayBuilder defendantsBuilder = createArrayBuilder();
        for( DcsDefendant defendant : request.getDefendants()) {
            final JsonObjectBuilder defendantBuilder = createObjectBuilder()
                    .add(ID, String.valueOf(defendant.getId()));
                    addIfNotNull(defendantBuilder, BAIL_STATUS, Optional.ofNullable(defendant.getBailStatus()));
                    addIfNotNull(defendantBuilder, INTERPRETER_LANGUAGE, Optional.ofNullable(defendant.getInterpreterLanguage()));
                    addIfNotNull(defendantBuilder, INTERPRETER_INFORMATION, Optional.ofNullable(defendant.getInterpreterInformation()));

            DefendantPerson defendantPerson = defendant.getDefendantPerson();
            if (defendantPerson != null) {
                final JsonObjectBuilder personObjectBuilder = createObjectBuilder()
                        .add(FORENAME, defendantPerson.getForename())
                        .add(SURNAME, defendantPerson.getSurname());
                        addIfNotNull(personObjectBuilder, MIDDLE_NAME, Optional.ofNullable(defendantPerson.getMiddleName()));
                        addIfNotNull(personObjectBuilder, DATE_OF_BIRTH, Optional.ofNullable(defendantPerson.getDateOfBirth()));
                defendantBuilder.add(DEFENDANT_PERSON, personObjectBuilder);
            }

            DefendantOrganisation defendantOrganisation = defendant.getDefendantOrganisation();
            if (defendantOrganisation != null) {
                defendantBuilder.add(DEFENDANT_ORGANISATION, createObjectBuilder()
                        .add(NAME, defendantOrganisation.getName()));
            }

            final OffenceDetails offenceDetails = defendant.getOffencesDetails();
            if(offenceDetails != null) {
                JsonArrayBuilder addedOffencesBuilder = createOffencesBuilders(offenceDetails.getAddedOffences());
                JsonArrayBuilder removedOffencesBuilder = createOffencesBuilders(offenceDetails.getRemovedOffences());

                defendantBuilder.add(OFFENCES_DETAILS, createObjectBuilder()
                    .add(ADDED_OFFENCES, addedOffencesBuilder)
                    .add(REMOVED_OFFENCES, removedOffencesBuilder));
            }

            List<DcsHearing> hearings = defendant.getHearings();
            if (hearings != null && !hearings.isEmpty()) {
                JsonArrayBuilder hearingsArrayBuilder = createArrayBuilder();
                for (DcsHearing hearing : hearings) {
                    final JsonObjectBuilder hearingBuilder = createObjectBuilder();
                    addIfNotNull(hearingBuilder, COURT_CENTRE, Optional.ofNullable(hearing.getCourtCentre()));
                    if(nonNull(hearing.getHearingDate())){
                        hearingBuilder.add(HEARING_DATE, String.valueOf(hearing.getHearingDate()));
                    }
                    hearingsArrayBuilder.add(hearingBuilder);
                }
                defendantBuilder.add(HEARINGS, hearingsArrayBuilder);
            }

            defendantsBuilder.add(defendantBuilder);
        }

        return defendantsBuilder;
    }

    private JsonArrayBuilder createOffencesBuilders(Set<DcsOffence> offences) {
        JsonArrayBuilder offencesArrayBuilder = createArrayBuilder();
        for (DcsOffence offence : offences) {
            final JsonObjectBuilder dcsOffenceBuilder = createObjectBuilder()
                    .add(OFFENCE_ID, String.valueOf(offence.getOffenceId()));
                    addIfNotNull(dcsOffenceBuilder, OFFENCE_CODE, Optional.ofNullable(offence.getOffenceCode()));
            offencesArrayBuilder.add(dcsOffenceBuilder);
        }
        return offencesArrayBuilder;
    }

    public static void addIfNotNull(JsonObjectBuilder builder, String name, Optional<Object> optionalValue) {
        optionalValue.ifPresent(value -> builder.add(name, String.valueOf(value)));
    }
}
