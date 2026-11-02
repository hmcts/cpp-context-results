package uk.gov.moj.cpp.results.persist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DefendantGobAccountsEntityRepositoryTest {

    private static final String PERSISTENCE_UNIT = "results-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private DefendantGobAccountsRepository defendantGobAccountsRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        defendantGobAccountsRepository = new DefendantGobAccountsRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(defendantGobAccountsRepository);
        defendantGobAccountsRepository.findAll().forEach(defendantGobAccountsRepository::remove);
    }

    @Test
    public void shouldFindAccountNumber() {
        final DefendantGobAccountsEntity hearingFinancialDetails = createdHearingFinancialDetailsEntity(randomUUID());
        defendantGobAccountsRepository.save(hearingFinancialDetails);

        final Optional<DefendantGobAccountsEntity> result = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(hearingFinancialDetails.getMasterDefendantId(), hearingFinancialDetails.getHearingId());
        assertThat(result.isPresent(), is(true));

        final DefendantGobAccountsEntity defendantGobAccountsEntity = result.get();
        assertThat(defendantGobAccountsEntity.getId(), is(hearingFinancialDetails.getId()));
        assertThat(defendantGobAccountsEntity.getMasterDefendantId(), is(hearingFinancialDetails.getMasterDefendantId()));
        assertThat(defendantGobAccountsEntity.getHearingId(), is(hearingFinancialDetails.getHearingId()));
        assertThat(defendantGobAccountsEntity.getAccountCorrelationId(), is(hearingFinancialDetails.getAccountCorrelationId()));
        assertThat(defendantGobAccountsEntity.getAccountNumber(), is(hearingFinancialDetails.getAccountNumber()));
        assertThat(defendantGobAccountsEntity.getCaseReferences(), is(hearingFinancialDetails.getCaseReferences()));
    }

    @Test
    public void shouldReturnLatestGobAccountWhenMultipleAccountsExist() {
        final UUID masterDefendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final String caseReferences = createJsonArrayFromStrings("case ref1", "case ref2").toString();

        final DefendantGobAccountsEntity olderEntity = new DefendantGobAccountsEntity(masterDefendantId, randomUUID());
        olderEntity.setHearingId(hearingId);
        olderEntity.setAccountNumber("olderAccountNumber");
        olderEntity.setCaseReferences(caseReferences);
        olderEntity.setAccountRequestTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        olderEntity.setCreatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        olderEntity.setUpdatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsRepository.save(olderEntity);

        final DefendantGobAccountsEntity newerEntity = new DefendantGobAccountsEntity(masterDefendantId, randomUUID());
        newerEntity.setHearingId(hearingId);
        newerEntity.setAccountNumber("newerAccountNumber");
        newerEntity.setCaseReferences(caseReferences);
        newerEntity.setAccountRequestTime(ZonedDateTime.parse("2023-01-02T10:00:00Z"));
        newerEntity.setCreatedTime(ZonedDateTime.parse("2023-01-02T10:00:00Z"));
        newerEntity.setUpdatedTime(ZonedDateTime.parse("2023-01-02T10:00:00Z"));
        defendantGobAccountsRepository.save(newerEntity);

        final Optional<DefendantGobAccountsEntity> result = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(masterDefendantId, hearingId);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getAccountNumber(), is("newerAccountNumber"));
        assertThat(result.get().getAccountRequestTime(), is(ZonedDateTime.parse("2023-01-02T10:00:00Z")));
    }

    @Test
    public void shouldFindAccountWithMatchingHearingId() {
        final UUID masterDefendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final String storedCaseReferences = createJsonArrayFromStrings("case ref1", "case ref2", "case ref3").toString();

        final DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity(masterDefendantId, randomUUID());
        entity.setHearingId(hearingId);
        entity.setAccountNumber("accountNumber");
        entity.setCaseReferences(storedCaseReferences);
        entity.setAccountRequestTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setCreatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setUpdatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsRepository.save(entity);

        final Optional<DefendantGobAccountsEntity> result = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(masterDefendantId, hearingId);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getAccountNumber(), is("accountNumber"));
        assertThat(result.get().getCaseReferences(), is(storedCaseReferences));
    }

    @Test
    public void shouldFindAccountWithSingleCaseReference() {
        final UUID masterDefendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final String storedCaseReferences = createJsonArrayFromStrings("case ref1").toString();

        final DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity(masterDefendantId, randomUUID());
        entity.setHearingId(hearingId);
        entity.setAccountNumber("accountNumber");
        entity.setCaseReferences(storedCaseReferences);
        entity.setAccountRequestTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setCreatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setUpdatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsRepository.save(entity);

        final Optional<DefendantGobAccountsEntity> result = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(masterDefendantId, hearingId);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getAccountNumber(), is("accountNumber"));
        assertThat(result.get().getCaseReferences(), is(storedCaseReferences));
    }

    @Test
    public void shouldFindAccountWithMultipleCaseReferences() {
        final UUID masterDefendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final String storedCaseReferences = createJsonArrayFromStrings("case ref1", "case ref2", "case ref3").toString();

        final DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity(masterDefendantId, randomUUID());
        entity.setHearingId(hearingId);
        entity.setAccountNumber("accountNumber");
        entity.setCaseReferences(storedCaseReferences);
        entity.setAccountRequestTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setCreatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setUpdatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsRepository.save(entity);

        final Optional<DefendantGobAccountsEntity> result = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(masterDefendantId, hearingId);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getAccountNumber(), is("accountNumber"));
        assertThat(result.get().getCaseReferences(), is(storedCaseReferences));
    }

    @Test
    public void shouldNotFindAccountWithNonExistentHearingId() {
        final UUID masterDefendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final String storedCaseReferences = createJsonArrayFromStrings("case ref1", "case ref2").toString();

        final DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity(masterDefendantId, randomUUID());
        entity.setHearingId(hearingId);
        entity.setAccountNumber("accountNumber");
        entity.setCaseReferences(storedCaseReferences);
        entity.setAccountRequestTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setCreatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setUpdatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsRepository.save(entity);

        final UUID nonExistentHearingId = randomUUID();
        final Optional<DefendantGobAccountsEntity> result = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(masterDefendantId, nonExistentHearingId);

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void shouldFindByEmbeddedId() {
        final DefendantGobAccountsEntity entity = createdHearingFinancialDetailsEntity(randomUUID());
        defendantGobAccountsRepository.save(entity);

        final DefendantGobAccountsEntity found = defendantGobAccountsRepository.findBy(entity.getId());

        assertThat(found.getId(), is(entity.getId()));
        assertThat(found.getAccountNumber(), is(entity.getAccountNumber()));
    }

    @Test
    public void shouldRemoveDefendantGobAccount() {
        final DefendantGobAccountsEntity entity = createdHearingFinancialDetailsEntity(randomUUID());
        defendantGobAccountsRepository.save(entity);
        assertThat(defendantGobAccountsRepository.findAll(), hasSize(1));

        defendantGobAccountsRepository.remove(entity);

        assertThat(defendantGobAccountsRepository.findAll(), hasSize(0));
    }

    @Test
    public void shouldRemoveAManagedDefendantGobAccount() {
        // save() returns the merged, managed instance, so remove() takes the contains==true branch
        final DefendantGobAccountsEntity managed = defendantGobAccountsRepository.save(createdHearingFinancialDetailsEntity(randomUUID()));

        defendantGobAccountsRepository.remove(managed);

        assertThat(defendantGobAccountsRepository.findAll(), hasSize(0));
    }

    private DefendantGobAccountsEntity createdHearingFinancialDetailsEntity(final UUID hearingId) {
        final DefendantGobAccountsEntity defendantGobAccountsEntity = new DefendantGobAccountsEntity(randomUUID(), randomUUID());
        defendantGobAccountsEntity.setHearingId(hearingId);
        defendantGobAccountsEntity.setAccountNumber("accountNumber");
        defendantGobAccountsEntity.setCaseReferences(createJsonArrayFromStrings("case ref1", "case ref2").toString());
        defendantGobAccountsEntity.setAccountRequestTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsEntity.setCreatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsEntity.setUpdatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        return defendantGobAccountsEntity;
    }

    private JsonArray createJsonArrayFromStrings(final String... strings) {
        final JsonArrayBuilder builder = createArrayBuilder();
        for (final String str : strings) {
            builder.add(str);
        }
        return builder.build();
    }

    @Test
    public void chd05_findAccountNumber_noMatch_should_throw_NoResultException() {
        assertThrows(NoResultException.class,
                () -> defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(randomUUID(), randomUUID()));
    }

    @Test
    public void chd03_zonedDateTime_should_be_stored_as_jvm_local_wall_clock_in_zoneless_column() {
        inZonelessTableOnLondonJvm(entityManager -> {
            final DefendantGobAccountsEntity entity = createdHearingFinancialDetailsEntity(randomUUID());
            entity.setAccountRequestTime(ZonedDateTime.parse("2026-07-01T10:00:00Z"));
            entityManager.merge(entity);
            entityManager.flush();

            final Object stored = entityManager.createNativeQuery(
                    "select cast(account_request_time as varchar(40)) from defendant_gob_accounts").getSingleResult();

            assertThat(stored.toString(), startsWith("2026-07-01 11:00:00"));
        });
    }

    @Test
    public void chd03_existingRowWrittenAsLocalWallClock_should_read_back_as_the_same_instant() {
        inZonelessTableOnLondonJvm(entityManager -> {
            final DefendantGobAccountsEntity written = createdHearingFinancialDetailsEntity(randomUUID());
            entityManager.createNativeQuery("insert into defendant_gob_accounts (master_defendant_id, account_correlation_id, hearing_id, case_references, "
                            + "account_request_time, created_time, updated_time) values (:masterDefendantId, :accountCorrelationId, :hearingId, '[]', "
                            + "timestamp '2026-07-01 11:00:00', timestamp '2026-07-01 11:00:00', timestamp '2026-07-01 11:00:00')")
                    .setParameter("masterDefendantId", written.getMasterDefendantId())
                    .setParameter("accountCorrelationId", written.getAccountCorrelationId())
                    .setParameter("hearingId", written.getHearingId())
                    .executeUpdate();

            final DefendantGobAccountsEntity read = entityManager.find(DefendantGobAccountsEntity.class, written.getId());

            assertThat(read.getAccountRequestTime().toInstant(), is(Instant.parse("2026-07-01T10:00:00Z")));
        });
    }

    /**
     * Mirrors production, which the shared unit hides: a Liquibase-style TIMESTAMP column with no zone
     * (changeset 028), no hibernate.jdbc.time_zone, and a JVM on UK time.
     */
    private void inZonelessTableOnLondonJvm(final Consumer<EntityManager> test) {
        final TimeZone originalZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
        final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("results-test-production-timezone-unit");
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            entityManager.createNativeQuery("drop table if exists defendant_gob_accounts").executeUpdate();
            entityManager.createNativeQuery("create table defendant_gob_accounts (master_defendant_id uuid not null, "
                    + "account_correlation_id uuid not null, hearing_id uuid not null, account_number varchar(255), "
                    + "case_references varchar(4000) not null, account_request_time timestamp, created_time timestamp not null, "
                    + "updated_time timestamp not null, primary key (master_defendant_id, account_correlation_id))").executeUpdate();
            test.accept(entityManager);
        } finally {
            entityManager.getTransaction().rollback();
            entityManager.close();
            entityManagerFactory.close();
            TimeZone.setDefault(originalZone);
        }
    }
}
