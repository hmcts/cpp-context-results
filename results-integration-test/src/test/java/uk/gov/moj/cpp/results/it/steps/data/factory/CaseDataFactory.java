package uk.gov.moj.cpp.results.it.steps.data.factory;

import static com.google.common.collect.Lists.newArrayList;

import uk.gov.moj.cpp.results.it.steps.data.hearing.Offence;
import uk.gov.moj.cpp.results.it.steps.data.hearing.ProgressionCase;

import java.util.List;
import java.util.UUID;

public class CaseDataFactory {

    public static ProgressionCase caseDetails(final UUID caseId, String urn, UUID personId, UUID defendantId) {
        return new ProgressionCase(caseId, urn, newArrayList(personId), newArrayList(defendantId));
    }

    public static ProgressionCase caseDetails(final UUID caseId, String urn, UUID personId, UUID defendantId, Offence offence) {
        return new ProgressionCase(caseId, urn, newArrayList(personId), newArrayList(defendantId), newArrayList(offence));
    }

    public static ProgressionCase caseDetails(final UUID caseId, String urn, List<UUID> personIds, List<UUID> defendantIds, List<Offence> offences) {
        return new ProgressionCase(caseId, urn, personIds, defendantIds, offences);
    }
}
