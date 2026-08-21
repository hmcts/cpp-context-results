package uk.gov.moj.cpp.results.persist;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.domains.constant.RegisterStatus.RECORDED;

import uk.gov.moj.cpp.results.persist.entity.InformantRegisterEntity;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The "status recorded" finders use JPQL row value constructors
 * ({@code (registerTime, hearingId) IN (...)}), which the shared
 * {@code HibernateTestEntityManagerProvider} rejects because it forces
 * {@code hibernate.jpa.compliance.query=true}. This test drives the same
 * repository against an isolated EntityManagerFactory with strict query
 * compliance disabled, matching how the production persistence unit runs.
 */
public class InformantRegisterRepositoryRowValueQueryTest {

    private static final UUID PROSECUTION_AUTHORITY_ID = randomUUID();
    private static final String PROSECUTION_AUTHORITY_OU_CODE = randomAlphanumeric(10);
    private static final UUID HEARING_ID = randomUUID();

    private static EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;
    private InformantRegisterRepository informantRegisterRepository;

    @BeforeAll
    static void createEntityManagerFactory() {
        entityManagerFactory = Persistence.createEntityManagerFactory("results-test-persistence-unit",
                Map.of("hibernate.jpa.compliance.query", "false",
                        "jakarta.persistence.jdbc.url", "jdbc:h2:mem:resultsrowvalue;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE"));
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        entityManagerFactory.close();
    }

    @BeforeEach
    void openEntityManagerAndSeedData() {
        entityManager = entityManagerFactory.createEntityManager();
        informantRegisterRepository = new InformantRegisterRepository();
        informantRegisterRepository.entityManager = entityManager;

        entityManager.getTransaction().begin();
        final ZonedDateTime registerTime = now();
        informantRegisterRepository.save(createInformantRegister(registerTime));
        informantRegisterRepository.save(createInformantRegister(registerTime.plusHours(1)));
    }

    @AfterEach
    void rollbackAndCloseEntityManager() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
        entityManager.close();
    }

    @Test
    public void shouldFindByStatusRecorded() {
        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByStatusRecorded();

        assertThat(informantRegisterEntities, hasSize(1));
    }

    @Test
    public void shouldFindByProsecutionAuthorityIdAndStatusRecorded() {
        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByProsecutionAuthorityIdAndStatusRecorded(PROSECUTION_AUTHORITY_ID);

        assertThat(informantRegisterEntities, hasSize(1));
    }

    @Test
    public void shouldFindByProsecutionAuthorityIdAndRegisterDateForStatusRecorded() {
        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByProsecutionAuthorityIdAndRegisterDateForStatusRecorded(PROSECUTION_AUTHORITY_ID, LocalDate.now());

        assertThat(informantRegisterEntities, hasSize(1));
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
