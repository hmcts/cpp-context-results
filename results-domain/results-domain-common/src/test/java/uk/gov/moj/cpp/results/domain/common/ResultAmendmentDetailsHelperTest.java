package uk.gov.moj.cpp.results.domain.common;

import org.apache.commons.collections.map.HashedMap;
import org.junit.Test;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.domains.ResultAmendmentDetailsHelper;
import uk.gov.moj.cpp.domains.resultdetails.ApplicationResultDetails;
import uk.gov.moj.cpp.domains.resultdetails.CaseResultDetails;
import uk.gov.moj.cpp.domains.resultdetails.DefendantResultDetails;
import uk.gov.moj.cpp.domains.resultdetails.JudicialResultAmendmentType;
import uk.gov.moj.cpp.domains.resultdetails.JudicialResultDetails;
import uk.gov.moj.cpp.domains.resultdetails.OffenceResultDetails;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ResultAmendmentDetailsHelperTest {

    @Test
    public void shouldHandleFirstSharedResults() {
        UUID caseId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID judicialResultIdInCase = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID judicialResultIdInApp = UUID.randomUUID();

        Map<UUID, CaseResultDetails> caseResultDetailsMap = new HashedMap();

        Hearing hearing = hearing(
                singletonList(
                        prosecutionCase(caseId,
                                defendant(defendantId,
                                        offence(offenceId, singletonList(judicialResult(judicialResultIdInCase)))))),
                singletonList(
                        application(applicationId, caseId,
                            judicialResult(judicialResultIdInApp))));


        List<CaseResultDetails> resultDetails = ResultAmendmentDetailsHelper.buildHearingResultDetails(hearing, caseResultDetailsMap);
        assertThat(resultDetails.size(), is(1));
        assertThat(resultDetails.get(0).getCaseId(), is(caseId));
        assertThat(resultDetails.get(0).getDefendantResultDetails().size(), is(1));

        verifyCaseResult(resultDetails.get(0), defendantId, offenceId, judicialResultIdInCase, JudicialResultAmendmentType.ADDED);
        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp, JudicialResultAmendmentType.ADDED);
    }

    @Test
    public void shouldHandleNewAddedResults() {
        UUID caseId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID judicialResultIdInCase1 = UUID.randomUUID();
        UUID judicialResultIdInCase2 = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID judicialResultIdInApp1 = UUID.randomUUID();
        UUID judicialResultIdInApp2 = UUID.randomUUID();

        final Map<UUID, CaseResultDetails> caseResultDetailsMap = new HashMap<>();
        caseResultDetailsMap.put(caseId, new CaseResultDetails(caseId, Arrays.asList(
                new DefendantResultDetails(defendantId, "Defendant Name", Arrays.asList(
                        new OffenceResultDetails(offenceId, 1, 1, "Offence 1", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInCase1, "Result 1", UUID.randomUUID(), JudicialResultAmendmentType.ADDED)
                        ))
                ))),
                Arrays.asList(
                        new ApplicationResultDetails(applicationId, "Application 1", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInApp1, "Result In App 1", UUID.randomUUID(), JudicialResultAmendmentType.ADDED)
                        ), emptyList(), "firstName", "lastName")
                )
        ));

        final Hearing hearing = hearing(
                singletonList(
                        prosecutionCase(caseId,
                                defendant(defendantId,
                                        offence(offenceId, judicialResults(judicialResultIdInCase1, judicialResultIdInCase2))))),
                singletonList(
                        application(applicationId, caseId,
                                judicialResult(judicialResultIdInApp1),
                                judicialResult(judicialResultIdInApp2))));

        List<CaseResultDetails> resultDetails = ResultAmendmentDetailsHelper.buildHearingResultDetails(hearing, caseResultDetailsMap);
        assertThat(resultDetails.size(), is(1));
        assertThat(resultDetails.get(0).getCaseId(), is(caseId));

        verifyCaseResult(resultDetails.get(0), defendantId, offenceId, judicialResultIdInCase1, JudicialResultAmendmentType.NONE);
        verifyCaseResult(resultDetails.get(0), defendantId, offenceId, judicialResultIdInCase2, JudicialResultAmendmentType.ADDED);
        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp1, JudicialResultAmendmentType.NONE);
        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp2, JudicialResultAmendmentType.ADDED);
    }

    @Test
    public void shouldHandleDeletedResults() {
        UUID caseId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID judicialResultIdInCase1 = UUID.randomUUID();
        UUID judicialResultIdInCase2 = UUID.randomUUID();
        UUID judicialResultIdInCase3 = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID judicialResultIdInApp1 = UUID.randomUUID();
        UUID judicialResultIdInApp2 = UUID.randomUUID();

        Map<UUID, CaseResultDetails> caseResultDetailsMap = new HashMap<>();
        caseResultDetailsMap.put(caseId, new CaseResultDetails(caseId, Arrays.asList(
                new DefendantResultDetails(defendantId, "Defendant Name", Arrays.asList(
                        new OffenceResultDetails(offenceId, 1, 1, "Offence 1", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInCase1, "Result 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE),
                                new JudicialResultDetails(judicialResultIdInCase2, "Result 2", UUID.randomUUID(), JudicialResultAmendmentType.ADDED),
                                new JudicialResultDetails(judicialResultIdInCase3, "Result 3 needs to be ignored", UUID.randomUUID(), JudicialResultAmendmentType.DELETED)
                        ))
                ))),
                Arrays.asList(
                        new ApplicationResultDetails(applicationId, "Application 1", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInApp1, "Result In App 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE),
                                new JudicialResultDetails(judicialResultIdInApp2, "Result In App 2", UUID.randomUUID(), JudicialResultAmendmentType.ADDED)
                        ), emptyList(), "firstName", "lastName")
                )
        ));

        Hearing hearing = hearing(
                singletonList(
                        prosecutionCase(caseId,
                                defendant(defendantId,
                                        offence(offenceId, Arrays.asList(judicialResult(judicialResultIdInCase1)))))),
                singletonList(
                        application(applicationId, caseId,
                                judicialResult(judicialResultIdInApp1))));

        List<CaseResultDetails> resultDetails = ResultAmendmentDetailsHelper.buildHearingResultDetails(hearing, caseResultDetailsMap);
        assertThat(resultDetails.size(), is(1));
        assertThat(resultDetails.get(0).getCaseId(), is(caseId));

        verifyCaseResult(resultDetails.get(0), defendantId, offenceId, judicialResultIdInCase1, JudicialResultAmendmentType.NONE);
        verifyCaseResult(resultDetails.get(0), defendantId, offenceId, judicialResultIdInCase2, JudicialResultAmendmentType.DELETED);
        verifyCaseResultNotExists(resultDetails.get(0), defendantId, offenceId, judicialResultIdInCase3);

        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp1, JudicialResultAmendmentType.NONE);
        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp2, JudicialResultAmendmentType.DELETED);
    }

    @Test
    public void shouldHandleAmendedResults() {
        UUID caseId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID judicialResultIdInCase1 = UUID.randomUUID();
        UUID judicialResultIdInCase2 = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID judicialResultIdInApp1 = UUID.randomUUID();
        UUID judicialResultIdInApp2 = UUID.randomUUID();

        final Map<UUID, CaseResultDetails> caseResultDetailsMap = new HashMap<>();
        caseResultDetailsMap.put(caseId, new CaseResultDetails(caseId, Arrays.asList(
                new DefendantResultDetails(defendantId, "Defendant Name", Arrays.asList(
                        new OffenceResultDetails(offenceId, 1, 1, "Offence 1", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInCase1, "Result 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE),
                                new JudicialResultDetails(judicialResultIdInCase2, "Result 2", UUID.randomUUID(), JudicialResultAmendmentType.ADDED)
                        ))
                ))),
                Arrays.asList(
                        new ApplicationResultDetails(applicationId, "Application 1", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInApp1, "Result In App 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE),
                                new JudicialResultDetails(judicialResultIdInApp2, "Result In App 2", UUID.randomUUID(), JudicialResultAmendmentType.ADDED)
                        ), emptyList(), "firstName", "lastName")
                )
        ));

        final Hearing hearing = hearing(
                singletonList(
                        prosecutionCase(caseId,
                                defendant(defendantId,
                                        offence(offenceId, Arrays.asList(
                                                judicialResult(judicialResultIdInCase1, true),
                                                judicialResult(judicialResultIdInCase2, false)))))),
                singletonList(
                        application(applicationId, caseId,
                                judicialResult(judicialResultIdInApp1, true),
                                judicialResult(judicialResultIdInApp2, false))));

        List<CaseResultDetails> resultDetails = ResultAmendmentDetailsHelper.buildHearingResultDetails(hearing, caseResultDetailsMap);
        assertThat(resultDetails.size(), is(1));
        assertThat(resultDetails.get(0).getCaseId(), is(caseId));

        verifyCaseResult(resultDetails.get(0), defendantId, offenceId, judicialResultIdInCase1, JudicialResultAmendmentType.UPDATED);
        verifyCaseResult(resultDetails.get(0), defendantId, offenceId, judicialResultIdInCase2, JudicialResultAmendmentType.NONE);
        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp1, JudicialResultAmendmentType.UPDATED);
        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp2, JudicialResultAmendmentType.NONE);
    }

    @Test
    public void shouldHandleComplexAmendment() {
        UUID caseId = UUID.randomUUID();
        UUID defendantId1 = UUID.randomUUID();
        UUID defendantId2 = UUID.randomUUID();
        UUID defendantId3 = UUID.randomUUID();
        UUID offenceId1 = UUID.randomUUID();
        UUID offenceId2 = UUID.randomUUID();
        UUID offenceId3 = UUID.randomUUID();
        UUID offenceId4 = UUID.randomUUID();
        UUID judicialResultIdInCase1 = UUID.randomUUID();
        UUID judicialResultIdInCase2 = UUID.randomUUID();
        UUID judicialResultIdInCase3 = UUID.randomUUID();
        UUID judicialResultIdInCase4 = UUID.randomUUID();
        UUID judicialResultIdInCase5 = UUID.randomUUID();
        UUID judicialResultIdInCase6 = UUID.randomUUID();
        UUID judicialResultIdInCase7 = UUID.randomUUID();
        UUID judicialResultIdInCase8 = UUID.randomUUID();


        UUID applicationId = UUID.randomUUID();
        UUID judicialResultIdInApp1 = UUID.randomUUID();
        UUID judicialResultIdInApp2 = UUID.randomUUID();

        final Map<UUID, CaseResultDetails> caseResultDetailsMap = new HashMap<>();
        caseResultDetailsMap.put(caseId, new CaseResultDetails(caseId, Arrays.asList(
                new DefendantResultDetails(defendantId1, "Defendant Name", Arrays.asList(
                        new OffenceResultDetails(offenceId1, 1, 1, "Offence 1", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInCase1, "Result 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE),
                                new JudicialResultDetails(judicialResultIdInCase2, "Result 2", UUID.randomUUID(), JudicialResultAmendmentType.ADDED)
                        ))
                )),
                new DefendantResultDetails(defendantId2, "Defendant Name 2", Arrays.asList(
                        new OffenceResultDetails(offenceId2, 1, 1, "Offence 2", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInCase3, "Result 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE),
                                new JudicialResultDetails(judicialResultIdInCase4, "Result 2", UUID.randomUUID(), JudicialResultAmendmentType.ADDED)
                        )),
                        new OffenceResultDetails(offenceId3, 1, 1, "Offence 3", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInCase5, "Result 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE)
                        ))
                )),
                new DefendantResultDetails(defendantId3, "Defendant Name 2", Arrays.asList(
                        new OffenceResultDetails(offenceId4, 1, 1, "Offence 4", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInCase6, "Result 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE)
                        ))
                ))),
                Arrays.asList(
                        new ApplicationResultDetails(applicationId, "Application 1", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInApp1, "Result In App 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE),
                                new JudicialResultDetails(judicialResultIdInApp2, "Result In App 2", UUID.randomUUID(), JudicialResultAmendmentType.ADDED)
                        ), emptyList(), "firstName", "lastName")
                )
        ));

        final Hearing hearing = hearing(
                singletonList(
                        prosecutionCase(caseId,
                                defendant(defendantId1,
                                        offence(offenceId1, Arrays.asList(
                                                judicialResult(judicialResultIdInCase2, true),
                                                judicialResult(judicialResultIdInCase7, true)))),
                                defendant(defendantId2,
                                        offence(offenceId2, Arrays.asList(
                                                judicialResult(judicialResultIdInCase3, true),
                                                judicialResult(judicialResultIdInCase4, false))),
                                        offence(offenceId3, Arrays.asList(
                                                judicialResult(judicialResultIdInCase8, true)))),
                                defendant(defendantId3,
                                        offence(offenceId4, Arrays.asList(
                                                judicialResult(judicialResultIdInCase6, false))))
                                )),
                singletonList(
                        application(applicationId, caseId,
                                judicialResult(judicialResultIdInApp1, false),
                                judicialResult(judicialResultIdInApp2, false))));

        List<CaseResultDetails> resultDetails = ResultAmendmentDetailsHelper.buildHearingResultDetails(hearing, caseResultDetailsMap);
        assertThat(resultDetails.size(), is(1));
        assertThat(resultDetails.get(0).getCaseId(), is(caseId));

        verifyCaseResult(resultDetails.get(0), defendantId1, offenceId1, judicialResultIdInCase1, JudicialResultAmendmentType.DELETED);
        verifyCaseResult(resultDetails.get(0), defendantId1, offenceId1, judicialResultIdInCase2, JudicialResultAmendmentType.UPDATED);
        verifyCaseResult(resultDetails.get(0), defendantId1, offenceId1, judicialResultIdInCase7, JudicialResultAmendmentType.ADDED);


        verifyCaseResult(resultDetails.get(0), defendantId2, offenceId2, judicialResultIdInCase3, JudicialResultAmendmentType.UPDATED);
        verifyCaseResult(resultDetails.get(0), defendantId2, offenceId2, judicialResultIdInCase4, JudicialResultAmendmentType.NONE);
        verifyCaseResult(resultDetails.get(0), defendantId2, offenceId3, judicialResultIdInCase5, JudicialResultAmendmentType.DELETED);
        verifyCaseResult(resultDetails.get(0), defendantId2, offenceId3, judicialResultIdInCase8, JudicialResultAmendmentType.ADDED);

        verifyCaseResult(resultDetails.get(0), defendantId3, offenceId4, judicialResultIdInCase6, JudicialResultAmendmentType.NONE);

        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp1, JudicialResultAmendmentType.NONE);
        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp2, JudicialResultAmendmentType.NONE);
    }


    @Test
    public void shouldHandleDifferentLevelResults() {
        UUID caseId = UUID.randomUUID();
        UUID defendantId1 = UUID.randomUUID();
        UUID defendantId2 = UUID.randomUUID();
        UUID defendantId3 = UUID.randomUUID();
        UUID offenceId1 = UUID.randomUUID();
        UUID offenceId2 = UUID.randomUUID();
        UUID offenceId3 = UUID.randomUUID();
        UUID offenceId4 = UUID.randomUUID();
        UUID clonedOffenceId1 = UUID.randomUUID();
        UUID judicialResultIdInCase1 = UUID.randomUUID();
        UUID judicialResultIdInCase2 = UUID.randomUUID();
        UUID judicialResultIdInCase3 = UUID.randomUUID();
        UUID judicialResultIdInCase4 = UUID.randomUUID();
        UUID judicialResultIdInCase5 = UUID.randomUUID();
        UUID judicialResultIdInCase6 = UUID.randomUUID();
        UUID judicialResultIdInCase7 = UUID.randomUUID();
        UUID judicialResultIdInCase8 = UUID.randomUUID();


        UUID applicationId = UUID.randomUUID();
        UUID judicialResultIdInApp1 = UUID.randomUUID();
        UUID judicialResultIdInApp2 = UUID.randomUUID();
        UUID judicialResultIdInApp3 = UUID.randomUUID();

        final Map<UUID, CaseResultDetails> caseResultDetailsMap = new HashMap<>();
        caseResultDetailsMap.put(caseId, new CaseResultDetails(caseId, Arrays.asList(
                new DefendantResultDetails(defendantId1, "Defendant Name", Arrays.asList(
                        new OffenceResultDetails(offenceId1, 1, 1, "Offence 1", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInCase1, "Result 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE),
                                new JudicialResultDetails(judicialResultIdInCase2, "Result 2", UUID.randomUUID(), JudicialResultAmendmentType.ADDED)
                        ))
                )),
                new DefendantResultDetails(defendantId2, "Defendant Name 2", Arrays.asList(
                        new OffenceResultDetails(offenceId2, 1, 1, "Offence 2", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInCase3, "Result 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE),
                                new JudicialResultDetails(judicialResultIdInCase4, "Result 2", UUID.randomUUID(), JudicialResultAmendmentType.ADDED)
                        )),
                        new OffenceResultDetails(offenceId3, 1, 1, "Offence 3", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInCase5, "Result 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE)
                        ))
                )),
                new DefendantResultDetails(defendantId3, "Defendant Name 2", Arrays.asList(
                        new OffenceResultDetails(offenceId4, 1, 1, "Offence 4", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInCase6, "Result 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE)
                        ))
                ))),
                Arrays.asList(
                        new ApplicationResultDetails(applicationId, "Application 1", Arrays.asList(
                                new JudicialResultDetails(judicialResultIdInApp1, "Result In App 1", UUID.randomUUID(), JudicialResultAmendmentType.NONE),
                                new JudicialResultDetails(judicialResultIdInApp3, "Result In App 3", UUID.randomUUID(), JudicialResultAmendmentType.ADDED)
                        ), Arrays.asList(
                                new OffenceResultDetails(clonedOffenceId1, 0, 0, "Cloned Offence",
                                        Arrays.asList(new JudicialResultDetails(judicialResultIdInApp2, "Result In App 2", UUID.randomUUID(), JudicialResultAmendmentType.ADDED)))), "firstName", "lastName")
                )
        ));

        final Hearing hearing = hearing(
                singletonList(
                        prosecutionCase(caseId,
                                defendant(defendantId1,
                                        offence(offenceId1, Arrays.asList(
                                                judicialResult(judicialResultIdInCase7, true)))),
                                defendant(defendantId2,
                                        offence(offenceId2, Arrays.asList(
                                                judicialResult(judicialResultIdInCase4, false))),
                                        offence(offenceId3, Arrays.asList(
                                                judicialResult(judicialResultIdInCase8, true)))),
                                defendant(defendantId3, Arrays.asList(judicialResult(judicialResultIdInCase6, offenceId4, false)),
                                        offence(offenceId4, null))
                        )),
                Arrays.asList(
                        new DefendantJudicialResult(judicialResult(judicialResultIdInCase2, offenceId1, true), defendantId1),
                        new DefendantJudicialResult(judicialResult(judicialResultIdInCase3, offenceId2, true), defendantId2)
                ),
                singletonList(
                        application(applicationId, caseId,
                                Arrays.asList(judicialResult(judicialResultIdInApp1, false)),
                                clonedOffenceId1, Arrays.asList(judicialResult(judicialResultIdInApp2, true)),
                                Arrays.asList(judicialResult(judicialResultIdInApp3, false)))));

        List<CaseResultDetails> resultDetails = ResultAmendmentDetailsHelper.buildHearingResultDetails(hearing, caseResultDetailsMap);
        assertThat(resultDetails.size(), is(1));
        assertThat(resultDetails.get(0).getCaseId(), is(caseId));

        verifyCaseResult(resultDetails.get(0), defendantId1, offenceId1, judicialResultIdInCase1, JudicialResultAmendmentType.DELETED);
        verifyCaseResult(resultDetails.get(0), defendantId1, offenceId1, judicialResultIdInCase2, JudicialResultAmendmentType.UPDATED);
        verifyCaseResult(resultDetails.get(0), defendantId1, offenceId1, judicialResultIdInCase7, JudicialResultAmendmentType.ADDED);


        verifyCaseResult(resultDetails.get(0), defendantId2, offenceId2, judicialResultIdInCase3, JudicialResultAmendmentType.UPDATED);
        verifyCaseResult(resultDetails.get(0), defendantId2, offenceId2, judicialResultIdInCase4, JudicialResultAmendmentType.NONE);
        verifyCaseResult(resultDetails.get(0), defendantId2, offenceId3, judicialResultIdInCase5, JudicialResultAmendmentType.DELETED);
        verifyCaseResult(resultDetails.get(0), defendantId2, offenceId3, judicialResultIdInCase8, JudicialResultAmendmentType.ADDED);

        verifyCaseResult(resultDetails.get(0), defendantId3, offenceId4, judicialResultIdInCase6, JudicialResultAmendmentType.NONE);

        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp1, JudicialResultAmendmentType.NONE);
        verifyApplicationOffenceResult(resultDetails.get(0), applicationId, clonedOffenceId1, judicialResultIdInApp2, JudicialResultAmendmentType.UPDATED);
        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp3, JudicialResultAmendmentType.NONE);
    }

    @Test
    public void shouldHandleApplicationOnlyResults() {
        UUID caseId = UUID.randomUUID();

        UUID applicationId = UUID.randomUUID();
        UUID judicialResultIdInApp1 = UUID.randomUUID();
        final Map<UUID, CaseResultDetails> caseResultDetailsMap = new HashMap<>();

        final Hearing hearing = hearing(
               null,
                singletonList(application(applicationId, caseId, judicialResult(judicialResultIdInApp1, false))));

        List<CaseResultDetails> resultDetails = ResultAmendmentDetailsHelper.buildHearingResultDetails(hearing, caseResultDetailsMap);
        assertThat(resultDetails.size(), is(1));
        assertThat(resultDetails.get(0).getCaseId(), is(caseId));
        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp1, JudicialResultAmendmentType.ADDED);
    }

    @Test
    public void shouldHandleApplicationWithCourtOrderOnlyResults() {
        UUID caseId = UUID.randomUUID();

        UUID applicationId = UUID.randomUUID();
        UUID judicialResultIdInApp1 = UUID.randomUUID();
        final Map<UUID, CaseResultDetails> caseResultDetailsMap = new HashMap<>();

        final Hearing hearing = hearing(
                null,
                singletonList(applicationWithCourtOrder(applicationId, caseId, judicialResult(judicialResultIdInApp1, false))));

        List<CaseResultDetails> resultDetails = ResultAmendmentDetailsHelper.buildHearingResultDetails(hearing, caseResultDetailsMap);
        assertThat(resultDetails.size(), is(1));
        assertThat(resultDetails.get(0).getCaseId(), is(caseId));
        verifyApplicationResult(resultDetails.get(0), applicationId, judicialResultIdInApp1, JudicialResultAmendmentType.ADDED);
    }



    private void verifyCaseResultNotExists(CaseResultDetails resultDetails, UUID defendantId, UUID offenceId, UUID resultId) {
        Optional<DefendantResultDetails>  defendantResultDetails = resultDetails.getDefendantResultDetails().stream()
                .filter(d -> d.getDefendantId().equals(defendantId))
                .findFirst();

        assertThat(defendantResultDetails.isPresent(), is(true));

        Optional<OffenceResultDetails>  offenceResultDetails = defendantResultDetails.get().getOffences().stream()
                .filter(o -> o.getOffenceId().equals(offenceId))
                .findFirst();

        assertThat(offenceResultDetails.isPresent(), is(true));

        Optional<JudicialResultDetails> judicialResultDetails = offenceResultDetails.get().getResults().stream()
                .filter(r -> r.getResultId().equals(resultId))
                .findFirst();

        assertThat(judicialResultDetails.isPresent(), is(false));
    }

    private void verifyCaseResult(CaseResultDetails resultDetails, UUID defendantId, UUID offenceId, UUID resultId, JudicialResultAmendmentType amendmentType) {
        Optional<DefendantResultDetails>  defendantResultDetails = resultDetails.getDefendantResultDetails().stream()
                .filter(d -> d.getDefendantId().equals(defendantId))
                .findFirst();

        assertThat(defendantResultDetails.isPresent(), is(true));

        Optional<OffenceResultDetails>  offenceResultDetails = defendantResultDetails.get().getOffences().stream()
                .filter(o -> o.getOffenceId().equals(offenceId))
                .findFirst();

        assertThat(offenceResultDetails.isPresent(), is(true));

        Optional<JudicialResultDetails> judicialResultDetails = offenceResultDetails.get().getResults().stream()
                .filter(r -> r.getResultId().equals(resultId))
                .findFirst();

        assertThat(judicialResultDetails.isPresent(), is(true));
        assertThat(judicialResultDetails.get().getAmendmentType(), is(amendmentType));
    }

    private void verifyApplicationResult(CaseResultDetails resultDetails, UUID applicationId, UUID resultId, JudicialResultAmendmentType amendmentType) {
        Optional<ApplicationResultDetails>  applicationResultDetails = resultDetails.getApplicationResultDetails().stream()
                .filter(a -> a.getApplicationId().equals(applicationId))
                .findFirst();

        assertThat(applicationResultDetails.isPresent(), is(true));

        Optional<JudicialResultDetails> judicialResultDetails = applicationResultDetails.get().getResults().stream()
                .filter(r -> r.getResultId().equals(resultId))
                .findFirst();

        assertThat(judicialResultDetails.isPresent(), is(true));
        assertThat(judicialResultDetails.get().getAmendmentType(), is(amendmentType));
    }

    private void verifyApplicationOffenceResult(CaseResultDetails resultDetails, UUID applicationId, UUID offenceId, UUID resultId, JudicialResultAmendmentType amendmentType) {
        Optional<ApplicationResultDetails>  applicationResultDetails = resultDetails.getApplicationResultDetails().stream()
                .filter(a -> a.getApplicationId().equals(applicationId))
                .findFirst();

        assertThat(applicationResultDetails.isPresent(), is(true));

        Optional<OffenceResultDetails> offenceResultDetails = applicationResultDetails.get().getCourtOrderOffenceResultDetails().stream()
                .filter(r -> r.getOffenceId().equals(offenceId))
                .findFirst();

        Optional<JudicialResultDetails> judicialResultDetails = offenceResultDetails.get().getResults().stream()
                .filter(r -> r.getResultId().equals(resultId))
                .findFirst();

        assertThat(judicialResultDetails.isPresent(), is(true));
        assertThat(judicialResultDetails.get().getAmendmentType(), is(amendmentType));
    }



    private Hearing hearing(final List<ProsecutionCase> cases, final List<DefendantJudicialResult> defendantJudicialResults, List<CourtApplication> applications) {
        return Hearing.hearing()
                .withProsecutionCases(cases)
                .withDefendantJudicialResults(defendantJudicialResults)
                .withCourtApplications(applications)
                .build();
    }

    private Hearing hearing(final List<ProsecutionCase> cases, List<CourtApplication> applications) {
        return Hearing.hearing()
                .withProsecutionCases(cases)
                .withCourtApplications(applications)
                .build();
    }


    private ProsecutionCase prosecutionCase(final UUID prosecutionCaseId, final Defendant... defendants) {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(Arrays.asList(defendants))
                .build();
    }

    private CourtApplication application(final UUID applicationId, final UUID prosecutionCaseId, final JudicialResult... results) {
        return CourtApplication.courtApplication()
                .withId(applicationId)
                .withCourtApplicationCases(Arrays.asList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .build()))
                .withJudicialResults(Arrays.asList(results))
                .withType(CourtApplicationType.courtApplicationType()
                        .withType("Application Title")
                        .build())
                .build();
    }

    private CourtApplication applicationWithCourtOrder(final UUID applicationId, final UUID prosecutionCaseId, final JudicialResult... results) {
        return CourtApplication.courtApplication()
                .withId(applicationId)
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(Arrays.asList(
                                CourtOrderOffence.courtOrderOffence()
                                        .withProsecutionCaseId(prosecutionCaseId)
                                        .withOffence(Offence.offence().build())
                                        .build()
                        ))
                        .build())
                .withJudicialResults(Arrays.asList(results))
                .withType(CourtApplicationType.courtApplicationType()
                        .withType("Application Title")
                        .build())
                .build();
    }

    private CourtApplication application(final UUID applicationId, final UUID prosecutionCaseId, final List<JudicialResult> courtApplicationCasesJudicialResults, final UUID courtOrderOffenceId,final List<JudicialResult> courtOrderResults, final List<JudicialResult> results) {
        return CourtApplication.courtApplication()
                .withId(applicationId)
                .withCourtApplicationCases(Arrays.asList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withOffences(Arrays.asList(offence(UUID.randomUUID(), courtApplicationCasesJudicialResults)))
                        .build()))
                .withCourtOrder(CourtOrder.courtOrder()
                        .withId(UUID.randomUUID())
                        .withCourtOrderOffences(Arrays.asList(
                                CourtOrderOffence.courtOrderOffence()
                                        .withProsecutionCaseId(prosecutionCaseId)
                                        .withOffence(offence(courtOrderOffenceId, courtOrderResults))
                                        .build()))
                        .build())
                .withJudicialResults(results)
                .withType(CourtApplicationType.courtApplicationType()
                        .withType("Application Title")
                        .build())
                .build();
    }

    private Defendant defendant(final UUID defendantId, final Offence... offences) {
        return Defendant.defendant()
                .withId(defendantId)
                .withMasterDefendantId(defendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withFirstName("First Name")
                                .withLastName("Last Name")
                                .build())
                        .build())
                .withOffences(Arrays.asList(offences))
                .build();
    }

    private Defendant defendant(final UUID defendantId, final List<JudicialResult> defendantCaseJudicialResults, final Offence... offences) {
        return Defendant.defendant()
                .withId(defendantId)
                .withMasterDefendantId(defendantId)
                .withDefendantCaseJudicialResults(defendantCaseJudicialResults)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withFirstName("First Name")
                                .withLastName("Last Name")
                                .build())
                        .build())
                .withOffences(Arrays.asList(offences))
                .build();
    }

    private Offence offence(final UUID offenceId, List<JudicialResult> judicialResults){
        return Offence.offence()
                .withId(offenceId)
                .withOrderIndex(1)
                .withCount(1)
                .withOffenceTitle("Offence Title")
                .withJudicialResults(judicialResults)
                .build();
    }

    private List<JudicialResult> judicialResults(final UUID... judicialResultIds) {
        return Arrays.stream(judicialResultIds)
                .map(judicialResultId -> judicialResult(judicialResultId))
                .collect(Collectors.toList());
    }

    private JudicialResult judicialResult(final UUID judicialResultId) {
        return JudicialResult.judicialResult()
                .withJudicialResultId(judicialResultId)
                .withLabel("Judicial Result Title")
                .withJudicialResultTypeId(UUID.randomUUID())
                .build();
    }

    private JudicialResult judicialResult(final UUID judicialResultId, final boolean isNewAmendment) {
        return JudicialResult.judicialResult()
                .withJudicialResultId(judicialResultId)
                .withLabel("Judicial Result Title")
                .withIsNewAmendment(isNewAmendment)
                .withJudicialResultTypeId(UUID.randomUUID())
                .build();
    }

    private JudicialResult judicialResult(final UUID judicialResultId, final UUID offenceId, final boolean isNewAmendment) {
        return JudicialResult.judicialResult()
                .withJudicialResultId(judicialResultId)
                .withLabel("Judicial Result Title")
                .withIsNewAmendment(isNewAmendment)
                .withJudicialResultTypeId(UUID.randomUUID())
                .withOffenceId(offenceId)
                .build();
    }
}