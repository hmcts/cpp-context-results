package uk.gov.moj.cpp.results.domain.aggregate.application;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    public static final String WRITE_OFF_ONE_DAY_DEEMED_SERVED_REMOVED = "WRITE OFF ONE DAY DEEMED SERVED REMOVED";
    public static final String STATUTORY_DECLARATION_UPDATED  = "STATUTORY DECLARATION UPDATED" ;
    public static final String APPLICATION_TO_REOPEN_UPDATED  = "APPLICATION TO REOPEN UPDATED" ;
    public static final String APPEAL_APPLICATION_UPDATED  = "APPEAL APPLICATION UPDATED";
    public static final String AMEND_AND_RESHARE = "AMEND AND RESHARE- DUPLICATE ACCOUNT: WRITE OFF REQUIRED";
    public static final String STDEC = "STDEC";
    public static final String AACA = "AACA";
    public static final String AASA = "AASA";
    public static final String AACD = "AACD";
    public static final String AASD = "AASD";
    public static final String ACSD = "ACSD";
    public static final String ASV = "ASV";
    public static final String APA = "APA";
    public static final String AW = "AW";
    public static final String SV_SENTENCE_VARIED = "\nSV - Sentence varied";
    public static final String SENTENCE_VARIED = "\nSentence varied";
    public static final String APPEAL_WITHDRAWN = "APPEAL WITHDRAWN";
    public static final String APPEAL_ALLOWED = "APPEAL ALLOWED";
    public static final String APPLICATION_TO_REOPEN_GRANTED = "APPLICATION TO REOPEN GRANTED";

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
                    .put(G, APPLICATION_TO_REOPEN_GRANTED)
                    .put(ROPENED, APPLICATION_TO_REOPEN_GRANTED)
                    .put(DISM, "APPLICATION TO REOPEN DISMISSED")
                    .build())
            .put(APPEAL, ImmutableMap.<String, String>builder()
                    .put(AACD, APPEAL_DISMISSED)
                    .put(AASD, APPEAL_DISMISSED)
                    .put(ACSD, APPEAL_DISMISSED)
                    .put(ASV, "APPEAL DISMISSED SENTENCE VARIED")
                    .put(APA, "APPEAL ABANDONED")
                    .put(WDRN, APPEAL_WITHDRAWN)
                    .put(AACA, APPEAL_ALLOWED)
                    .put(AW, APPEAL_WITHDRAWN)
                    .put(AASA, APPEAL_ALLOWED)
                    .put(G, "APPEAL GRANTED")
                    .put(ROPENED, "APPEAL ROPENED")
                    .put(RFSD, "APPEAL REFUSED")
                    .put(DISM, APPEAL_DISMISSED)
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
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.APPEAL).get(NCESDecisionConstants.ACSD),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.APPEAL).get(NCESDecisionConstants.DISM),
                NCESDecisionConstants.APPLICATION_SUBJECT.get(NCESDecisionConstants.APPEAL).get(NCESDecisionConstants.RFSD)
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
