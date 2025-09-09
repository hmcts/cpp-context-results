package uk.gov.moj.cpp.results.persist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DefendantGobAccountsEntityRepositoryTest extends BaseTransactionalJunit4Test {

    @Inject
    private DefendantGobAccountsRepository defendantGobAccountsRepository;

    private DefendantGobAccountsEntity hearingFinancialDetails;

    @Override
    public void setUpBefore() {
        hearingFinancialDetails = createdHearingFinancialDetailsEntity();
        defendantGobAccountsRepository.save(hearingFinancialDetails);
    }

    @Override
    public void tearDownAfter() {
        List<DefendantGobAccountsEntity> hearingFinancialDetailsEntities = defendantGobAccountsRepository.findAll();
        hearingFinancialDetailsEntities.forEach(e -> defendantGobAccountsRepository.remove(e));
    }

    @Test
    public void shouldFindByAccountNumber() {
        final DefendantGobAccountsEntity defendantGobAccountsEntity = defendantGobAccountsRepository.findByAccountNumber(hearingFinancialDetails.getMasterDefendantId(), hearingFinancialDetails.getCaseReferences());
        assertThat(defendantGobAccountsEntity, is(notNullValue()));
        assertThat(defendantGobAccountsEntity.getId(), is(hearingFinancialDetails.getId()));
        assertThat(defendantGobAccountsEntity.getMasterDefendantId(), is(hearingFinancialDetails.getMasterDefendantId()));
        assertThat(defendantGobAccountsEntity.getCorrelationId(), is(hearingFinancialDetails.getCorrelationId()));
        assertThat(defendantGobAccountsEntity.getAccountNumber(), is(hearingFinancialDetails.getAccountNumber()));
        assertThat(defendantGobAccountsEntity.getCaseReferences(), is(hearingFinancialDetails.getCaseReferences()));
    }

    @Test
    public void shouldReturnLatestGobAccountWhenMultipleAccountsExist() {
        // Create multiple records with same masterDefendantId and caseReferences but different createdDateTime
        final UUID masterDefendantId = randomUUID();
        final String caseReferences = "case ref1, case ref2";
        
        // Create older record
        DefendantGobAccountsEntity olderEntity = new DefendantGobAccountsEntity();
        olderEntity.setId(randomUUID());
        olderEntity.setMasterDefendantId(masterDefendantId);
        olderEntity.setCorrelationId(randomUUID());
        olderEntity.setAccountNumber("olderAccountNumber");
        olderEntity.setCaseReferences(caseReferences);
        olderEntity.setCreatedDateTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsRepository.save(olderEntity);
        
        // Create newer record
        DefendantGobAccountsEntity newerEntity = new DefendantGobAccountsEntity();
        newerEntity.setId(randomUUID());
        newerEntity.setMasterDefendantId(masterDefendantId);
        newerEntity.setCorrelationId(randomUUID());
        newerEntity.setAccountNumber("newerAccountNumber");
        newerEntity.setCaseReferences(caseReferences);
        newerEntity.setCreatedDateTime(ZonedDateTime.parse("2023-01-02T10:00:00Z"));
        defendantGobAccountsRepository.save(newerEntity);
        
        // Query should return only the latest record
        final DefendantGobAccountsEntity result = defendantGobAccountsRepository.findByAccountNumber(masterDefendantId, caseReferences);
        
        assertThat(result, is(notNullValue()));
        assertThat(result.getAccountNumber(), is("newerAccountNumber"));
        assertThat(result.getCreatedDateTime(), is(ZonedDateTime.parse("2023-01-02T10:00:00Z")));
    }

    @Test
    public void shouldFindAccountWithPartialCaseReferences() {
        // Create a record with multiple case references
        final UUID masterDefendantId = randomUUID();
        final String storedCaseReferences = "case ref1, case ref2, case ref3";
        
        DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity();
        entity.setId(randomUUID());
        entity.setMasterDefendantId(masterDefendantId);
        entity.setCorrelationId(randomUUID());
        entity.setAccountNumber("accountNumber");
        entity.setCaseReferences(storedCaseReferences);
        entity.setCreatedDateTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsRepository.save(entity);
        
        // Query with partial case references should find the record
        final DefendantGobAccountsEntity result = defendantGobAccountsRepository.findByAccountNumber(masterDefendantId, "case ref1, case ref2");
        
        assertThat(result, is(notNullValue()));
        assertThat(result.getAccountNumber(), is("accountNumber"));
        assertThat(result.getCaseReferences(), is(storedCaseReferences));
    }

    @Test
    public void shouldAccountForSingleCaseReference() {
        // Create a record with single case reference
        final UUID masterDefendantId = randomUUID();
        final String storedCaseReferences = "case ref1";
        
        DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity();
        entity.setId(randomUUID());
        entity.setMasterDefendantId(masterDefendantId);
        entity.setCorrelationId(randomUUID());
        entity.setAccountNumber("accountNumber");
        entity.setCaseReferences(storedCaseReferences);
        entity.setCreatedDateTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsRepository.save(entity);
        
        // Query with exact single case reference should find the record
        final DefendantGobAccountsEntity result = defendantGobAccountsRepository.findByAccountNumber(masterDefendantId, "case ref1");
        
        assertThat(result, is(notNullValue()));
        assertThat(result.getAccountNumber(), is("accountNumber"));
        assertThat(result.getCaseReferences(), is(storedCaseReferences));
    }

    @Test
    public void shouldFindRAccountWithSubsetOfCaseReferences() {
        // Create a record with multiple case references
        final UUID masterDefendantId = randomUUID();
        final String storedCaseReferences = "case ref1, case ref2, case ref3";
        
        DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity();
        entity.setId(randomUUID());
        entity.setMasterDefendantId(masterDefendantId);
        entity.setCorrelationId(randomUUID());
        entity.setAccountNumber("accountNumber");
        entity.setCaseReferences(storedCaseReferences);
        entity.setCreatedDateTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsRepository.save(entity);
        
        // Query with a subset of case references should find the record
        final DefendantGobAccountsEntity result = defendantGobAccountsRepository.findByAccountNumber(masterDefendantId, "case ref2");
        
        assertThat(result, is(notNullValue()));
        assertThat(result.getAccountNumber(), is("accountNumber"));
        assertThat(result.getCaseReferences(), is(storedCaseReferences));
    }

    @Test
    public void shouldFindAccountWithMoreCaseReferencesThanStored() {
        // Create a record with fewer case references
        final UUID masterDefendantId = randomUUID();
        final String storedCaseReferences = "case ref1, case ref2";
        
        DefendantGobAccountsEntity entity = new DefendantGobAccountsEntity();
        entity.setId(randomUUID());
        entity.setMasterDefendantId(masterDefendantId);
        entity.setCorrelationId(randomUUID());
        entity.setAccountNumber("accountNumber");
        entity.setCaseReferences(storedCaseReferences);
        entity.setCreatedDateTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));
        defendantGobAccountsRepository.save(entity);
        
        // Query with more case references should find the record if stored references are subset
        final DefendantGobAccountsEntity result = defendantGobAccountsRepository.findByAccountNumber(masterDefendantId, "case ref1, case ref2, case ref3");
        
        assertThat(result, is(notNullValue()));
        assertThat(result.getAccountNumber(), is("accountNumber"));
        assertThat(result.getCaseReferences(), is(storedCaseReferences));
    }

    private DefendantGobAccountsEntity createdHearingFinancialDetailsEntity() {
        DefendantGobAccountsEntity defendantGobAccountsEntity = new DefendantGobAccountsEntity();
        defendantGobAccountsEntity.setId(randomUUID());
        defendantGobAccountsEntity.setMasterDefendantId(randomUUID());
        defendantGobAccountsEntity.setCorrelationId(randomUUID());
        defendantGobAccountsEntity.setAccountNumber("accountNumber");
        defendantGobAccountsEntity.setCaseReferences("case ref1, case ref2");
        defendantGobAccountsEntity.setCreatedDateTime(ZonedDateTime.parse("2023-01-01T10:00:00Z"));

        return defendantGobAccountsEntity;
    }
}