package uk.gov.moj.cpp.results.event.service;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;

public class ApplicationParameters {

    @Inject
    @Value(key = "police.email.template.id" ,defaultValue = "781b970d-a13e-4440-97c3-ecf22a4540d5")
    private String emailTemplateId;

    @Inject
    @Value(key = "common.platform.url", defaultValue = "http://steccm12wrpxy01.cpp.nonlive/")
    private String commonPlatformUrl;

    public String getEmailTemplateId() {
        return emailTemplateId;
    }

    public String getCommonPlatformUrl() {
        return commonPlatformUrl;
    }

}
