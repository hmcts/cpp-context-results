package uk.gov.moj.cpp.results.query.view;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.informantRegisterDocument.ProsecutorResult.prosecutorResult;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.core.courts.informantRegisterDocument.ProsecutorResult;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.persist.InformantRegisterRepository;
import uk.gov.moj.cpp.results.persist.entity.InformantRegisterEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.collect.Lists;

public class ProsecutorResultsQueryView {

    private static final String FIELD_START_DATE = "startDate";
    private static final String FIELD_END_DATE = "endDate";
    private static final String FIELD_OUCODE = "ouCode";

    @Inject
    private InformantRegisterRepository informantRegisterRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public JsonEnvelope getProsecutorResults(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final Optional<LocalDate> optionalStartDate = getString(payload, FIELD_START_DATE).map(LocalDate::parse);
        final Optional<LocalDate> optionalEndDate = getString(payload, FIELD_END_DATE).map(LocalDate::parse);
        final Optional<String> optionalOuCode = getString(payload, FIELD_OUCODE);

        if (!(optionalStartDate.isPresent() && optionalOuCode.isPresent())) {
            // this should not happen as API level validation would have stopped this
            return null;
        }

        final LocalDate startDate = optionalStartDate.get();
        final String ouCode = optionalOuCode.get();

        final List<InformantRegisterEntity> informantRegisterEntities = Lists.newArrayList();
        final LocalDate endDate = optionalEndDate.orElse(startDate);
        informantRegisterEntities.addAll(informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(ouCode, startDate, endDate));

        final List<InformantRegisterDocumentRequest> requests = informantRegisterEntities.stream()
                .map(InformantRegisterEntity::getPayload)
                .map(s -> stringToJsonObjectConverter.convert(s))
                .map(jo -> jsonObjectToObjectConverter.convert(jo, InformantRegisterDocumentRequest.class))
                .collect(toList());

        final ProsecutorResult.Builder builder = prosecutorResult()
                .withHearingVenues(requests.stream().map(InformantRegisterDocumentRequest::getHearingVenue).collect(toList()))
                .withStartDate(startDate);

        optionalEndDate.ifPresent(builder::withEndDate);

        if (isNotEmpty(requests)) {
            final InformantRegisterDocumentRequest firstRequest = requests.get(0);
            builder.withProsecutionAuthorityCode(firstRequest.getProsecutionAuthorityCode())
                    .withProsecutionAuthorityId(firstRequest.getProsecutionAuthorityId())
                    .withProsecutionAuthorityName(firstRequest.getProsecutionAuthorityName())
                    .withMajorCreditorCode(firstRequest.getMajorCreditorCode());
        }

        return envelopeFrom(envelope.metadata(), objectToJsonObjectConverter.convert(builder.build()));
    }

}
