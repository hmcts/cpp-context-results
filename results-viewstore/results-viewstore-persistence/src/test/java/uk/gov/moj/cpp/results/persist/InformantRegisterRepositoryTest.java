package uk.gov.moj.cpp.results.persist;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.RECORDED;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.results.persist.entity.InformantRegisterEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class InformantRegisterRepositoryTest extends BaseTransactionalJunit4Test {

    private static final UUID PROSECUTION_AUTHORITY_ID = randomUUID();
    private static final String PROSECUTION_AUTHORITY_OU_CODE = randomAlphanumeric(10);
    private static final UUID HEARING_ID = randomUUID();
    private static final ZonedDateTime REGISTER_TIME_1 = now();
    private static final ZonedDateTime REGISTER_TIME_2 = now().plusHours(1);

    @Inject
    private InformantRegisterRepository informantRegisterRepository;

    @Override
    public void setUpBefore() {
        informantRegisterRepository.save(createInformantRegister(REGISTER_TIME_1));
        informantRegisterRepository.save(createInformantRegister(REGISTER_TIME_2));
    }

    @Override
    public void tearDownAfter() {
        List<InformantRegisterEntity> informantRegisterEntities = informantRegisterRepository.findAll();
        informantRegisterEntities.forEach(ir -> informantRegisterRepository.remove(ir));
    }

    @Test
    public void shouldFindTheInformantRegisterRequestsByDateAndProsecutionAuthority() {
        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByRegisterDateAndProsecutionAuthorityCode(LocalDate.now(), "TFL");
        assertThat(informantRegisterEntities, hasSize(1));
    }

    @Test
    public void shouldFindTheInformantRegisterRequestsByDate() {
        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByRegisterDate(LocalDate.now());
        assertThat(informantRegisterEntities, hasSize(1));

    }

    @Test
    public void shouldFindInformantRegisterRequestsByProsecutionAuthorityAndRegisterDate() {

        final ZonedDateTime startDate = ZonedDateTime.now().plusDays(3);
        for (int i = 0; i < 5; i++) {
            informantRegisterRepository.save(createInformantRegister(startDate.plusDays(i)));
        }

        for (int i = 0; i < 5; i++) {
            final List<InformantRegisterEntity> informantRegisterEntities =
                    informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(PROSECUTION_AUTHORITY_OU_CODE, startDate.plusDays(i).toLocalDate(), startDate.plusDays(i).toLocalDate());
            assertThat(informantRegisterEntities, hasSize(1));
        }

    }

    @Test
    public void shouldFindInformantRegisterRequestsByProsecutionAuthorityAndRegisterDateRange() {

        Instant fixedInstant = Instant.parse("2026-01-01T10:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        final ZonedDateTime startDate = ZonedDateTime.now(fixedClock).plusDays(3);
        for (int i = 0; i < 5; i++) {
            informantRegisterRepository.save(createInformantRegister(startDate.plusDays(i)));
        }

        final List<InformantRegisterEntity> informantRegisterEntitiesAllResults =
                informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(PROSECUTION_AUTHORITY_OU_CODE, startDate.toLocalDate(), startDate.plusDays(4).toLocalDate());
        assertThat(informantRegisterEntitiesAllResults, hasSize(5));

        final List<InformantRegisterEntity> informantRegisterEntitiesOnlyFirstTwoDays =
                informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(PROSECUTION_AUTHORITY_OU_CODE, startDate.toLocalDate(), startDate.plusDays(1).toLocalDate());
        assertThat(informantRegisterEntitiesOnlyFirstTwoDays, hasSize(2));

        final List<InformantRegisterEntity> informantRegisterEntitiesOnlyLastTwoDays =
                informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(PROSECUTION_AUTHORITY_OU_CODE, startDate.plusDays(3).toLocalDate(), startDate.plusDays(4).toLocalDate());
        assertThat(informantRegisterEntitiesOnlyLastTwoDays, hasSize(2));

        final List<InformantRegisterEntity> informantRegisterEntitiesNoResultsForUnknownProsecutor =
                informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(randomAlphanumeric(10), startDate.toLocalDate(), startDate.plusDays(4).toLocalDate());
        assertThat(informantRegisterEntitiesNoResultsForUnknownProsecutor, hasSize(0));

        final List<InformantRegisterEntity> informantRegisterEntitiesNoResultsOutsideDateRange =
                informantRegisterRepository.findByProsecutionAuthorityOuCodeAndRegisterDateRange(PROSECUTION_AUTHORITY_OU_CODE, startDate.minusDays(2).toLocalDate(), startDate.minusDays(1).toLocalDate());
        assertThat(informantRegisterEntitiesNoResultsOutsideDateRange, hasSize(0));

    }

    private InformantRegisterEntity createInformantRegister(final ZonedDateTime registerTime) {
        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setId(randomUUID());
        informantRegisterEntity.setProsecutionAuthorityId(PROSECUTION_AUTHORITY_ID);
        informantRegisterEntity.setProsecutionAuthorityCode("TFL");
        informantRegisterEntity.setProsecutionAuthorityOuCode(PROSECUTION_AUTHORITY_OU_CODE);
        informantRegisterEntity.setStatus(RECORDED);
        informantRegisterEntity.setRegisterDate(registerTime.toLocalDate());
        informantRegisterEntity.setGeneratedDate(LocalDate.now());
        informantRegisterEntity.setRegisterTime(registerTime);
        informantRegisterEntity.setHearingId(HEARING_ID);

        return informantRegisterEntity;
    }
}
