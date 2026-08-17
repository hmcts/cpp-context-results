package uk.gov.moj.cpp.results.persist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;

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
}
