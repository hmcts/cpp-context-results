package uk.gov.moj.cpp.results.domain.aggregate.application;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.HearingFinancialResultsTracked;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;

public class NCESDecisionConstants  implements Serializable {

    public static final String STAT_DEC = "STAT_DEC";
    public static final String REOPEN = "REOPEN";
    public static final String APPEAL = "APPEAL";
    public static final String RFSD = "RFSD";
    public static final String DISM = "DISM";
    public static final String WDRN = "WDRN";
    public static final String G = "G";
    public static final String APPEAL_DISMISSED = "APPEAL DISMISSED";
    public static final String ACON = "ACON";
    public static final String ACON_EMAIL_SUBJECT = "ACCOUNTS TO BE CONSOLIDATED";
    public static final String COMMA = ",";
    public static final String ROPENED = "ROPENED";
    public static final String WRITE_OFF_ONE_DAY_DEEMED_SERVED = "WRITE OFF ONE DAY DEEMED SERVED";
    public static final String STATUTORY_DECLARATION_UPDATED  = "STATUTORY DECLARATION UPDATED" ;
    public static final String APPLICATION_TO_REOPEN_UPDATED  = "APPLICATION TO REOPEN UPDATED" ;
    public static final String APPEAL_APPLICATION_UPDATED  = "APPEAL APPLICATION UPDATED";
    public static final String AMEND_AND_RESHARE = "AMEND AND RESHARE- DUPLICATE ACCOUNT: WRITE OFF REQUIRED";
    private static final String FIDICI = "FIDICI";
    private static final String FIDICI_VALUE = "Fined and detained in default of payment until court rises";
    private static final String FIDICTI = "FIDICTI";
    private static final String FIDICTI_VALUE = "Fined and detained in default of payment until time";
    private static final String FIDIPI = "FIDIPI";
    private static final String FIDIPI_VALUE = "Fined and detained in default of payment in police station";
    public static final String STDEC = "STDEC";
    private static final String STDEC_VALUE = "Statutory Declaration";
    public static final String AACA = "AACA";
    private static final String AACA_VALUE = "Appeal against conviction allowed ";
    public static final String AASA = "AASA";
    private static final String AASA_VALUE = "Appeal against sentence allowed";
    public static final String AACD = "AACD";
    private static final String AACD_VALUE = "Appeal against conviction dismissed";
    public static final String AASD = "AASD";
    private static final String AASD_VALUE = "Appeal against sentence dismissed";
    public static final String ACSD = "ACSD";
    private static final String ACSD_VALUE = "Appeal against conviction and sentence dismissed";
    public static final String ASV = "ASV";
    private static final String ASV_VALUE = "Appeal against conviction dismissed and sentence varied";
    public static final String APA = "APA";
    private static final String APA_VALUE = "Appeal abandoned";
    private static final String G_VALUE = "Granted";
    private static final String ROPENED_VALUE = "Case reopened";
    private static final String RFSD_VALUE = "Application refused";
    private static final String WDRN_VALUE = "Withdrawn";
    private static final String ACON_VALUE = "Account Consolidated";
    public static final String AW = "AW";
    public static final String SV_SENTENCE_VARIED = "\nSV - Sentence varied";
    public static final String SENTENCE_VARIED = "\nSentence varied";

    public static final Map<String, String> resultCodeToString = ImmutableMap.<String, String>builder()
            .put(FIDICI, FIDICI_VALUE)
            .put(FIDICTI, FIDICTI_VALUE)
            .put(FIDIPI, FIDIPI_VALUE)
            .put(G, G_VALUE)
            .put(STDEC, STDEC_VALUE)
            .put(ROPENED, ROPENED_VALUE)
            .put(AACA, AACA_VALUE)
            .put(AASA, AASA_VALUE)
            .put(RFSD, RFSD_VALUE)
            .put(WDRN, WDRN_VALUE)
            .put(AACD, AACD_VALUE)
            .put(AASD, AASD_VALUE)
            .put(ACSD, ACSD_VALUE)
            .put(ASV, ASV_VALUE)
            .put(APA, APA_VALUE)
            .put(ACON, ACON_VALUE)
            .build();

    public static final Map<String, String> APPLICATION_TYPES = ImmutableMap.<String, String>builder()
            .put(STAT_DEC, "APPLICATION FOR A STATUTORY DECLARATION RECEIVED")
            .put(REOPEN, "APPLICATION TO REOPEN RECEIVED")
            .put(APPEAL, "APPEAL APPLICATION RECEIVED")
            .build();

    public static final Map<String, Map<String, String>> APPLICATION_SUBJECT = ImmutableMap.<String, Map<String, String>>builder()
            .put(STAT_DEC, ImmutableMap.<String, String>builder()
                    .put(RFSD, "STATUTORY DECLARATION REFUSED")
                    .put(WDRN, "STATUTORY DECLARATION WITHDRAWN")
                    .put(G, "STATUTORY DECLARATION GRANTED")
                    .put(STDEC, "STATUTORY DECLARATION GRANTED")
                    .put(DISM, "STATUTORY DECLARATION DISMISSED")
                    .build())
            .put(REOPEN, ImmutableMap.<String, String>builder()
                    .put(RFSD, "APPLICATION TO REOPEN REFUSED")
                    .put(WDRN, "APPLICATION TO REOPEN WITHDRAWN")
                    .put(G, "APPLICATION TO REOPEN GRANTED")
                    .put(ROPENED, "APPLICATION TO REOPEN GRANTED")
                    .put(DISM, "APPLICATION TO REOPEN DISMISSED")
                    .build())
            .put(APPEAL, ImmutableMap.<String, String>builder()
                    .put(AACD, APPEAL_DISMISSED)
                    .put(AASD, APPEAL_DISMISSED)
                    .put(ACSD, APPEAL_DISMISSED)
                    .put(ASV, "APPEAL DISMISSED SENTENCE VARIED")
                    .put(APA, "APPEAL ABANDONED")
                    .put(WDRN, "APPEAL WITHDRAWN")
                    .put(AACA, "APPEAL ALLOWED")
                    .put(AW, "APPEAL WITHDRAWN")
                    .put(AASA, "APPEAL ALLOWED")
                    .put(G, "APPEAL GRANTED")
                    .put(ROPENED, "APPEAL ROPENED")
                    .build())
            .build();

    public static final Map<String, String> APPLICATION_UPDATED_SUBJECT = ImmutableMap.<String, String>builder()
            .put(STAT_DEC, STATUTORY_DECLARATION_UPDATED)
            .put(REOPEN, APPLICATION_TO_REOPEN_UPDATED)
            .put(APPEAL, APPEAL_APPLICATION_UPDATED)
            .build();

    public static List<String> getApplicationAppealAllowedSubjects() {
        return Arrays.asList(NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.APPEAL).get(NCESDecisionConstants.AACA),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.APPEAL).get(NCESDecisionConstants.AASA)
                );
    }

    public static List<String> getApplicationAppealSubjects() {
        return Arrays.asList(
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.APPEAL).get(NCESDecisionConstants.ASV),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.APPEAL).get(NCESDecisionConstants.AASD),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.APPEAL).get(NCESDecisionConstants.AW),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.APPEAL).get(NCESDecisionConstants.APA),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.APPEAL).get(NCESDecisionConstants.AACD),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.APPEAL).get(NCESDecisionConstants.ACSD)
        );
    }

    public static List<String> getApplicationGrantedSubjects() {
        return Arrays.asList(NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.STAT_DEC).get(NCESDecisionConstants.G),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.STAT_DEC).get(NCESDecisionConstants.STDEC),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.REOPEN).get(NCESDecisionConstants.G),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.REOPEN).get(NCESDecisionConstants.ROPENED),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.APPEAL).get(NCESDecisionConstants.G));
    }

    public static List<String> getApplicationNonGrantedSubjects() {
        return Arrays.asList(
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.STAT_DEC).get(NCESDecisionConstants.RFSD),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.STAT_DEC).get(NCESDecisionConstants.WDRN),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.STAT_DEC).get(NCESDecisionConstants.DISM),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.REOPEN).get(NCESDecisionConstants.RFSD),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.REOPEN).get(NCESDecisionConstants.WDRN),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.REOPEN).get(NCESDecisionConstants.DISM));
    }
}
