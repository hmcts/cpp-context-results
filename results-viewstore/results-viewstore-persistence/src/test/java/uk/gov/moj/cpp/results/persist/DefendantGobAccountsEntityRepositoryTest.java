package uk.gov.moj.cpp.results.persist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static javax.json.Json.createArrayBuilder;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.persistence.NoResultException;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DefendantGobAccountsEntityRepositoryTest extends BaseTransactionalJunit4Test {

    @Inject
    private DefendantGobAccountsRepository defendantGobAccountsRepository;

    private DefendantGobAccountsEntity hearingFinancialDetails;
    private UUID testHearingId;

    @Override
    public void setUpBefore() {
        testHearingId = randomUUID();
        hearingFinancialDetails = createdHearingFinancialDetailsEntity();
    }

    @Override
    public void tearDownAfter() {
        List<DefendantGobAccountsEntity> hearingFinancialDetailsEntities = defendantGobAccountsRepository.findAll();
        hearingFinancialDetailsEntities.forEach(e -> defendantGobAccountsRepository.remove(e));
    }

    @Test
    public void shouldFindAccountNumber() {
        // Save the entity in the test method
        defendantGobAccountsRepository.save(hearingFinancialDetails);
        
        List<DefendantGobAccountsEntity> allEntities = defendantGobAccountsRepository.findAll();

        // Query with masterDefendantId and hearingId
        final DefendantGobAccountsEntity defendantGobAccountsEntity = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(hearingFinancialDetails.getMasterDefendantId(), hearingFinancialDetails.getHearingId());
        assertThat(defendantGobAccountsEntity, is(notNullValue()));
        assertThat(defendantGobAccountsEntity.getId(), is(hearingFinancialDetails.getId()));
        assertThat(defendantGobAccountsEntity.getMasterDefendantId(), is(hearingFinancialDetails.getMasterDefendantId()));
        assertThat(defendantGobAccountsEntity.getHearingId(), is(hearingFinancialDetails.getHearingId()));
        assertThat(defendantGobAccountsEntity.getCorrelationId(), is(hearingFinancialDetails.getCorrelationId()));
        assertThat(defendantGobAccountsEntity.getAccountNumber(), is(hearingFinancialDetails.getAccountNumber()));
        assertThat(defendantGobAccountsEntity.getCaseReferences(), is(hearingFinancialDetails.getCaseReferences()));
    }

    @Test
    public void shouldReturnLatestGobAccountWhenMultipleAccountsExist() {
        // Create multiple records with same masterDefendantId and hearingId but different accountCreatedTime
        final UUID masterDefendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final String caseReferences = createJsonArrayFromStrings("case ref1", "case ref2").toString();

        // Create older record
        DefendantGobAccountsEntity olderEntity = new DefendantGobAccountsEntity();
        olderEntity.setMasterDefendantId(masterDefendantId);
        olderEntity.setHearingId(hearingId);
        olderEntity.setCorrelationId(randomUUID());
        olderEntity.setAccountNumber("olderAccountNumber");
        olderEntity.setCaseReferences(caseReferences);
        olderEntity.setAccountRequestTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        olderEntity.setCreatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        olderEntity.setUpdatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));

        defendantGobAccountsRepository.save(olderEntity);
        
        // Create newer record
        DefendantGobAccountsEntity newerEntity = new DefendantGobAccountsEntity();
        newerEntity.setMasterDefendantId(masterDefendantId);
        newerEntity.setHearingId(hearingId);
        newerEntity.setCorrelationId(randomUUID());
        newerEntity.setAccountNumber("newerAccountNumber");
        newerEntity.setCaseReferences(caseReferences);
        newerEntity.setAccountRequestTime(ZonedDateTime.parse("2023-01-02T10:00:00Z"));
        newerEntity.setCreatedTime(ZonedDateTime.parse("2023-01-02T10:00:00Z"));
        newerEntity.setUpdatedTime(ZonedDateTime.parse("2023-01-02T10:00:00Z"));
        defendantGobAccountsRepository.save(newerEntity);
        
        // Query should return only the latest record based on accountCreatedTime
        final DefendantGobAccountsEntity result = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(masterDefendantId, hearingId);
        
        assertThat(result, is(notNullValue()));
        assertThat(result.getAccountNumber(), is("newerAccountNumber"));
        assertThat(result.getAccountRequestTime(), is(ZonedDateTime.parse("2023-01-02T10:00:00Z")));
    }

    @Test
    public void shouldFindAccountWithMatchingHearingId() {
        // Create a record with specific masterDefendantId and hearingId
        final UUID masterDefendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final String storedCaseReferences = createJsonArrayFromStrings("case ref1", "case ref2", "case ref3").toString();
        
        DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity();
        entity.setMasterDefendantId(masterDefendantId);
        entity.setHearingId(hearingId);
        entity.setCorrelationId(randomUUID());
        entity.setAccountNumber("accountNumber");
        entity.setCaseReferences(storedCaseReferences);
        entity.setAccountRequestTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setCreatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setUpdatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));

        defendantGobAccountsRepository.save(entity);
        
        // Query with matching masterDefendantId and hearingId should find the record
        final DefendantGobAccountsEntity result = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(masterDefendantId, hearingId);
        
        assertThat(result, is(notNullValue()));
        assertThat(result.getAccountNumber(), is("accountNumber"));
        assertThat(result.getCaseReferences(), is(storedCaseReferences));
    }

    @Test
    public void shouldFindAccountWithSingleCaseReference() {
        // Create a record with single case reference
        final UUID masterDefendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final String storedCaseReferences = createJsonArrayFromStrings("case ref1").toString();
        
        DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity();
        entity.setMasterDefendantId(masterDefendantId);
        entity.setHearingId(hearingId);
        entity.setCorrelationId(randomUUID());
        entity.setAccountNumber("accountNumber");
        entity.setCaseReferences(storedCaseReferences);
        entity.setAccountRequestTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setCreatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setUpdatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));

        defendantGobAccountsRepository.save(entity);
        
        // Query with matching masterDefendantId and hearingId should find the record
        final DefendantGobAccountsEntity result = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(masterDefendantId, hearingId);
        
        assertThat(result, is(notNullValue()));
        assertThat(result.getAccountNumber(), is("accountNumber"));
        assertThat(result.getCaseReferences(), is(storedCaseReferences));
    }

    @Test
    public void shouldFindAccountWithMultipleCaseReferences() {
        // Create a record with multiple case references
        final UUID masterDefendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final String storedCaseReferences = createJsonArrayFromStrings("case ref1", "case ref2", "case ref3").toString();
        
        DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity();
        entity.setMasterDefendantId(masterDefendantId);
        entity.setHearingId(hearingId);
        entity.setCorrelationId(randomUUID());
        entity.setAccountNumber("accountNumber");
        entity.setCaseReferences(storedCaseReferences);
        entity.setAccountRequestTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setCreatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setUpdatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));

        defendantGobAccountsRepository.save(entity);
        
        // Query with matching masterDefendantId and hearingId should find the record
        final DefendantGobAccountsEntity result = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(masterDefendantId, hearingId);
        
        assertThat(result, is(notNullValue()));
        assertThat(result.getAccountNumber(), is("accountNumber"));
        assertThat(result.getCaseReferences(), is(storedCaseReferences));
    }

    @Test(expected = NoResultException.class)
    public void shouldNotFindAccountWithNonExistentHearingId() {
        // Create a record with specific masterDefendantId and hearingId
        final UUID masterDefendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final String storedCaseReferences = createJsonArrayFromStrings("case ref1", "case ref2").toString();
        
        DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity();
        entity.setMasterDefendantId(masterDefendantId);
        entity.setHearingId(hearingId);
        entity.setCorrelationId(randomUUID());
        entity.setAccountNumber("accountNumber");
        entity.setCaseReferences(storedCaseReferences);
        entity.setAccountRequestTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setCreatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        entity.setUpdatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));

        defendantGobAccountsRepository.save(entity);
        
        // Query with non-existent hearingId should not find the record
        final UUID nonExistentHearingId = randomUUID();
        final DefendantGobAccountsEntity result = defendantGobAccountsRepository.findAccountNumberByMasterDefendantIdAndHearingId(masterDefendantId, nonExistentHearingId);
    }

    private DefendantGobAccountsEntity createdHearingFinancialDetailsEntity() {
        DefendantGobAccountsEntity defendantGobAccountsEntity = new DefendantGobAccountsEntity();
        defendantGobAccountsEntity.setMasterDefendantId(randomUUID());
        defendantGobAccountsEntity.setHearingId(testHearingId);
        defendantGobAccountsEntity.setCorrelationId(randomUUID());
        defendantGobAccountsEntity.setAccountNumber("accountNumber");
        defendantGobAccountsEntity.setCaseReferences(createJsonArrayFromStrings("case ref1", "case ref2").toString());
        defendantGobAccountsEntity.setAccountRequestTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsEntity.setCreatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsEntity.setUpdatedTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));


        return defendantGobAccountsEntity;
    }

    private JsonArray createJsonArrayFromStrings(String... strings) {
        JsonArrayBuilder builder = createArrayBuilder();
        for (String str : strings) {
            builder.add(str);
        }
        return builder.build();
    }
}