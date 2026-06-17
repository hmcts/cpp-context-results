package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.InformantRegisterRecorded;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.results.courts.InformantRegisterGenerated;
import uk.gov.justice.results.courts.InformantRegisterNotificationIgnored;
import uk.gov.justice.results.courts.InformantRegisterNotified;
import uk.gov.justice.results.courts.InformantRegisterNotifiedV2;
import uk.gov.justice.results.courts.NotifyInformantRegister;
import uk.gov.justice.results.courts.InformantRegisterGeneratedV2;
import uk.gov.justice.results.courts.informantRegisterDocument.InformantRegisterRecipient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProsecutionAuthorityAggregate implements Aggregate {
    private static final long serialVersionUID = 102L;
    private List<InformantRegisterRecipient> informantRegisterRecipients;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(InformantRegisterRecorded.class).apply(e -> {
                }),
                when(InformantRegisterGenerated.class).apply(e -> {
                    final List<InformantRegisterDocumentRequest> informantRegisterWithRecipients = e.getInformantRegisterDocumentRequests().stream().filter(
                            informantRegisterDocumentRequest -> nonNull(informantRegisterDocumentRequest.getRecipients()) && !informantRegisterDocumentRequest.getRecipients().isEmpty())
                            .collect(Collectors.toList());
                    if (isNotEmpty(informantRegisterWithRecipients)) {
                        this.informantRegisterRecipients = informantRegisterWithRecipients.get(0).getRecipients().stream()
                                .map(r -> InformantRegisterRecipient.informantRegisterRecipient()
                                        .withRecipientName(r.getRecipientName())
                                        .withEmailAddress1(r.getEmailAddress1())
                                        .withEmailAddress2(r.getEmailAddress2())
                                        .withEmailTemplateName(r.getEmailTemplateName())
                                        .build())
                                .collect(Collectors.toList());
                    }
                }),
                when(InformantRegisterGeneratedV2.class).apply(e -> {
                    final List<uk.gov.justice.results.courts.informantRegisterDocument.InformantRegisterDocumentRequest> informantRegisterWithRecipients = e.getInformantRegisterDocumentRequests().stream().filter(
                            informantRegisterDocumentRequest -> nonNull(informantRegisterDocumentRequest.getRecipients()) && !informantRegisterDocumentRequest.getRecipients().isEmpty())
                            .collect(Collectors.toList());
                    if (isNotEmpty(informantRegisterWithRecipients)) {
                        this.informantRegisterRecipients = Collections.unmodifiableList(informantRegisterWithRecipients.get(0).getRecipients());
                    }
                }),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> notifyProsecutingAuthority(final NotifyInformantRegister notifyInformantRegister) {

        if (isEmpty(informantRegisterRecipients) || isBlank(notifyInformantRegister.getTemplateId()) || isNull(notifyInformantRegister.getFileId())) {
            return apply(Stream.of(InformantRegisterNotificationIgnored.informantRegisterNotificationIgnored()
                    .withFileId(notifyInformantRegister.getFileId())
                    .withTemplateId(notifyInformantRegister.getTemplateId())
                    .withProsecutionAuthorityId(notifyInformantRegister.getProsecutionAuthorityId()).build()));
        }

        final List<uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterRecipient> coreRecipients = informantRegisterRecipients.stream()
                .map(r -> uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterRecipient.informantRegisterRecipient()
                        .withRecipientName(r.getRecipientName())
                        .withEmailAddress1(r.getEmailAddress1())
                        .withEmailAddress2(r.getEmailAddress2())
                        .withEmailTemplateName(r.getEmailTemplateName())
                        .build())
                .collect(Collectors.toList());
        return apply(Stream.of(InformantRegisterNotifiedV2.informantRegisterNotifiedV2().withRecipients(coreRecipients)
                .withFileId(notifyInformantRegister.getFileId())
                .withTemplateId(notifyInformantRegister.getTemplateId())
                .withProsecutionAuthorityId(notifyInformantRegister.getProsecutionAuthorityId())
                        .withRegisterDate(notifyInformantRegister.getRegisterDate())
                .build()));
    }

    public void setInformantRegisterRecipients(final List<InformantRegisterRecipient> informantRegisterRecipients) {
        this.informantRegisterRecipients = Collections.unmodifiableList(informantRegisterRecipients);
    }

}
