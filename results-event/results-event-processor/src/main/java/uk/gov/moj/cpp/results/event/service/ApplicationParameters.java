package uk.gov.moj.cpp.results.event.service;

import uk.gov.justice.services.common.configuration.Value;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class ApplicationParameters {

    @Inject
    @Value(key = "police.email.template.id", defaultValue = "781b970d-a13e-4440-97c3-ecf22a4540d5")
    private String emailTemplateId;

    @Inject
    @Value(key = "police_email_hearing_results_template_id", defaultValue = "efc18c42-bea2-4124-8c02-7a7ae4556b73")
    private String policeEmailHearingResultsTemplateId;

    @Inject
    @Value(key = "police_email_hearing_results_with_application_template_id", defaultValue = "f6c999fd-0495-4502-90d6-f6dc4676da6f")
    private String policeEmailHearingResultsWithApplicationTemplateId;

    @Inject
    @Value(key = "police_email_hearing_results_amended_template_id", defaultValue = "c8b5a9dd-df0c-4f0d-83b1-b1c4c58dec13")
    private String policeEmailHearingResultsAmendedTemplateId;

    @Inject
    @Value(key = "police_email_hearing_results_amended_with_application_template_id", defaultValue = "f3359bce-8cfb-454f-a504-aa916ea9e9e9")
    private String policeEmailHearingResultsAmendedWithApplicationTemplateId;

    @Inject
    @Value(key = "ncesEmailNotificationTemplateId", defaultValue = "d3ce1ce3-5233-4b9d-b881-3857351fbfb0")
    private String ncesEmailNotificationTemplateId;

    @Inject
    @Value(key = "common.platform.url", defaultValue = "http://steccm12wrpxy01.cpp.nonlive/")
    private String commonPlatformUrl;

    @Inject
    @Value(key = "pcr_email_template_id")
    private String prisonCourtRegisterEmailTemplateId;

    @Inject
    @Value(key = "cr_email_template_id")
    private String courtRegisterEmailTemplateId;

    @Inject
    @Value(key = "ir_email_template_id")
    private String informantRegisterEmailTemplateId;

    @Inject
    @Value(key = "now_email_template_id")
    private String nowEmailTemplateId;

    @Inject
    @Value(key = "now_sla_email_template_id")
    private String nowSlaEmailTemplateId;

    @Inject
    @Value(key = "now_extradition_email_template_id")
    private String nowExtraditionEmailTemplateId;

    @Inject
    @Value(key = "now_extradition_sla_email_template_id")
    private String nowExtraditionSlaEmailTemplateId;

    public String getEmailTemplateId() {
        return emailTemplateId;
    }

    public String getPoliceEmailHearingResultsTemplateId() {
        return policeEmailHearingResultsTemplateId;
    }

    public String getPoliceEmailHearingResultsWithApplicationTemplateId() {
        return policeEmailHearingResultsWithApplicationTemplateId;
    }

    public String getPoliceEmailHearingResultsAmendedTemplateId() {
        return policeEmailHearingResultsAmendedTemplateId;
    }

    public String getPoliceEmailHearingResultsAmendedWithApplicationTemplateId() {
        return policeEmailHearingResultsAmendedWithApplicationTemplateId;
    }


    public String getEmailTemplateId(final String templateName) {
        final Map<String, String> emailTemplatesMap = new HashMap<>();
        emailTemplatesMap.put("pcr_standard", this.prisonCourtRegisterEmailTemplateId);
        emailTemplatesMap.put("ir_standard", this.informantRegisterEmailTemplateId);
        emailTemplatesMap.put("cr_standard", this.courtRegisterEmailTemplateId);
        emailTemplatesMap.put("now_standard_template", this.nowEmailTemplateId);
        emailTemplatesMap.put("now_sla_template", this.nowSlaEmailTemplateId);
        emailTemplatesMap.put("now_extradition_standard_template", this.nowExtraditionEmailTemplateId);
        emailTemplatesMap.put("now_extradition_sla_template", this.nowExtraditionSlaEmailTemplateId);

        return emailTemplatesMap.getOrDefault(templateName, "");
    }

    public String getCommonPlatformUrl() {
        return commonPlatformUrl;
    }

}
