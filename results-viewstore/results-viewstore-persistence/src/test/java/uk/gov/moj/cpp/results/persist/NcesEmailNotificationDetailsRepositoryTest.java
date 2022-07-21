package uk.gov.moj.cpp.results.persist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.results.persist.entity.NcesEmailNotificationDetailsEntity;

import java.util.List;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class NcesEmailNotificationDetailsRepositoryTest extends BaseTransactionalTest {

    @Inject
    private NcesEmailNotificationDetailsRepository ncesEmailNotificationDetailsRepository;

    private NcesEmailNotificationDetailsEntity ncesEmailNotificationDetails;

    @Override
    public void setUpBefore() {
        ncesEmailNotificationDetails = createNcesEmailNotificationDetailsEntity();
        ncesEmailNotificationDetailsRepository.save(ncesEmailNotificationDetails);
    }

    @Override
    public void tearDownAfter() {
        List<NcesEmailNotificationDetailsEntity> ncesEmailNotificationDetailsEntities = ncesEmailNotificationDetailsRepository.findAll();
        ncesEmailNotificationDetailsEntities.forEach(e -> ncesEmailNotificationDetailsRepository.remove(e));
    }

    @Test
    public void shouldFindTheNcesEmailNotificationDetailsByMaterialId() {
        final NcesEmailNotificationDetailsEntity ncesEmailNotificationDetailsEntity =
                ncesEmailNotificationDetailsRepository.findByMaterialId(ncesEmailNotificationDetails.getMaterialId());
        assertThat(ncesEmailNotificationDetailsEntity.getId(), is(ncesEmailNotificationDetails.getId()));
        assertThat(ncesEmailNotificationDetailsEntity.getMasterDefendantId(), is(ncesEmailNotificationDetails.getMasterDefendantId()));
        assertThat(ncesEmailNotificationDetailsEntity.getMaterialId(), is(ncesEmailNotificationDetails.getMaterialId()));
        assertThat(ncesEmailNotificationDetailsEntity.getNotificationId(), is(ncesEmailNotificationDetails.getNotificationId()));
        assertThat(ncesEmailNotificationDetailsEntity.getSubject(), is(ncesEmailNotificationDetails.getSubject()));
        assertThat(ncesEmailNotificationDetailsEntity.getSendTo(), is(ncesEmailNotificationDetails.getSendTo()));
    }

    private NcesEmailNotificationDetailsEntity createNcesEmailNotificationDetailsEntity() {
        NcesEmailNotificationDetailsEntity ncesEmailNotificationDetailsEntity = new NcesEmailNotificationDetailsEntity();
        ncesEmailNotificationDetailsEntity.setId(randomUUID());
        ncesEmailNotificationDetailsEntity.setMasterDefendantId(randomUUID());
        ncesEmailNotificationDetailsEntity.setMaterialId(randomUUID());
        ncesEmailNotificationDetailsEntity.setNotificationId(randomUUID());
        ncesEmailNotificationDetailsEntity.setSubject("subject");
        ncesEmailNotificationDetailsEntity.setSendTo("mail@email.com");

        return ncesEmailNotificationDetailsEntity;
    }
}
