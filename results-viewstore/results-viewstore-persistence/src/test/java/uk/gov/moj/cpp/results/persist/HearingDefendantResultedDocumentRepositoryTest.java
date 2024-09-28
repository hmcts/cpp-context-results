package uk.gov.moj.cpp.results.persist;

import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.results.persist.entity.HearingDefendantKey;
import uk.gov.moj.cpp.results.persist.entity.HearingDefendantResultedDocument;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("CdiInjectionPointsInspection")
@RunWith(CdiTestRunner.class)
public class HearingDefendantResultedDocumentRepositoryTest extends BaseTransactionalJunit4Test {

    @Inject
    private HearingDefendantResultedDocumentRepository hearingResultedDocumentRepository;

    @Test
    public void shouldPersistDocument() {

        HearingDefendantResultedDocument document = new HearingDefendantResultedDocument();
        document.setId(new HearingDefendantKey(UUID.randomUUID(), UUID.randomUUID()));
        document.setStartDate(LocalDate.of(2018, 12, 05));
        document.setEndDate(LocalDate.of(2018, 12, 10));
        document.setSummaryPayload(STRING.next());
        document.setDetailsPayload(STRING.next());
        hearingResultedDocumentRepository.save(document);


/*        final Hearing actualHearing = hearingRepository.findHearingByPersonIdAndHearingId(PERSON_ID, HEARING_ID);

        assertThat(actualHearing.getId(), is(HEARING_ID));
        assertThat(actualHearing.getPersonId(), is(PERSON_ID));
        assertThat(actualHearing.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(actualHearing.getCourtCode(), is(COURT_CODE));
        assertThat(actualHearing.getProsecutorName(), is(PROSECUTOR_NAME));
        assertThat(actualHearing.getDefenceName(), is(DEFENCE_NAME));
        assertThat(actualHearing.getStartDate(), is(HEARING_START_DATE));
        assertThat(actualHearing.getJudgeName(), is(JUDGE_NAME));*/
    }



}