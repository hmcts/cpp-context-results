package uk.gov.moj.cpp.results.persist;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.results.persist.entity.HearingDefendantKey;
import uk.gov.moj.cpp.results.persist.entity.HearingDefendantResultedDocument;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.UUID;

@SuppressWarnings("CdiInjectionPointsInspection")
@RunWith(CdiTestRunner.class)
public class HearingDefendantResultedDocumentRepositoryTest extends BaseTransactionalTest {

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