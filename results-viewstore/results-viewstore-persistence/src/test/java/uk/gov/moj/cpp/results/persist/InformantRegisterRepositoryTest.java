package uk.gov.moj.cpp.results.persist;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.GENERATED;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.RECORDED;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.results.persist.entity.InformantRegisterEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InformantRegisterRepositoryTest {

    private static final String PERSISTENCE_UNIT = "results-test-persistence-unit";

    private static final UUID PROSECUTION_AUTHORITY_ID = randomUUID();
    private static final String PROSECUTION_AUTHORITY_OU_CODE = randomAlphanumeric(10);
    private static final UUID HEARING_ID = randomUUID();
    private static final ZonedDateTime REGISTER_TIME_1 = now();
    private static final ZonedDateTime REGISTER_TIME_2 = now().plusHours(1);

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private InformantRegisterRepository informantRegisterRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        informantRegisterRepository = new InformantRegisterRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(informantRegisterRepository);
        informantRegisterRepository.findAll().forEach(informantRegisterRepository::remove);
        informantRegisterRepository.save(createInformantRegister(REGISTER_TIME_1));
        informantRegisterRepository.save(createInformantRegister(REGISTER_TIME_2));
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
        final Instant fixedInstant = Instant.parse("2026-01-01T10:00:00Z");
        final Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

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

    @Test
    public void shouldRemoveInformantRegister() {
        assertThat(informantRegisterRepository.findAll(), hasSize(2));

        informantRegisterRepository.findAll().forEach(informantRegisterRepository::remove);

        assertThat(informantRegisterRepository.findAll(), hasSize(0));
    }

    @Test
    public void shouldFindByFileId() {
        final UUID fileId = randomUUID();
        final InformantRegisterEntity withFileId = createInformantRegister(REGISTER_TIME_1);
        withFileId.setFileId(fileId);
        informantRegisterRepository.save(withFileId);

        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByFileId(fileId);

        assertThat(informantRegisterEntities, hasSize(1));
    }

    @Test
    public void shouldFindByStatus() {
        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByStatus(RECORDED);

        assertThat(informantRegisterEntities, hasSize(2));
    }

    @Test
    public void shouldFindByProsecutionAuthorityIdAndStatusGenerated() {
        informantRegisterRepository.save(createGeneratedInformantRegister(REGISTER_TIME_1));

        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByProsecutionAuthorityIdAndStatusGenerated(PROSECUTION_AUTHORITY_ID);

        assertThat(informantRegisterEntities, hasSize(1));
    }

    @Test
    public void shouldFindByProsecutionAuthorityIdAndRegisterDateAndStatusGenerated() {
        informantRegisterRepository.save(createGeneratedInformantRegister(REGISTER_TIME_1));

        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByProsecutionAuthorityIdAndRegisterDateAndStatusGenerated(PROSECUTION_AUTHORITY_ID, LocalDate.now());

        assertThat(informantRegisterEntities, hasSize(1));
    }

    @Test
    public void shouldFindByHearingIdAndStatusRecorded() {
        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByHearingIdAndStatusRecorded(HEARING_ID);

        assertThat(informantRegisterEntities, hasSize(2));
    }

    private InformantRegisterEntity createGeneratedInformantRegister(final ZonedDateTime registerTime) {
        final InformantRegisterEntity informantRegisterEntity = createInformantRegister(registerTime);
        informantRegisterEntity.setStatus(GENERATED);
        return informantRegisterEntity;
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
