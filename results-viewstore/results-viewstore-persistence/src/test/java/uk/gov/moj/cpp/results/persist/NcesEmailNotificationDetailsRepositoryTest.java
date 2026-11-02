package uk.gov.moj.cpp.results.persist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.results.persist.entity.NcesEmailNotificationDetailsEntity;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NcesEmailNotificationDetailsRepositoryTest {

    private static final String PERSISTENCE_UNIT = "results-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private NcesEmailNotificationDetailsRepository ncesEmailNotificationDetailsRepository;

    private NcesEmailNotificationDetailsEntity ncesEmailNotificationDetails;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        ncesEmailNotificationDetailsRepository = new NcesEmailNotificationDetailsRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(ncesEmailNotificationDetailsRepository);
        ncesEmailNotificationDetailsRepository.findAll().forEach(ncesEmailNotificationDetailsRepository::remove);
        ncesEmailNotificationDetails = createNcesEmailNotificationDetailsEntity();
        ncesEmailNotificationDetailsRepository.save(ncesEmailNotificationDetails);
    }

    @Test
    public void shouldFindTheNcesEmailNotificationDetailsByMaterialId() {
        final Optional<NcesEmailNotificationDetailsEntity> result =
                ncesEmailNotificationDetailsRepository.findByMaterialId(ncesEmailNotificationDetails.getMaterialId());

        assertThat(result.isPresent(), is(true));
        final NcesEmailNotificationDetailsEntity ncesEmailNotificationDetailsEntity = result.get();
        assertThat(ncesEmailNotificationDetailsEntity.getId(), is(ncesEmailNotificationDetails.getId()));
        assertThat(ncesEmailNotificationDetailsEntity.getMasterDefendantId(), is(ncesEmailNotificationDetails.getMasterDefendantId()));
        assertThat(ncesEmailNotificationDetailsEntity.getMaterialId(), is(ncesEmailNotificationDetails.getMaterialId()));
        assertThat(ncesEmailNotificationDetailsEntity.getNotificationId(), is(ncesEmailNotificationDetails.getNotificationId()));
        assertThat(ncesEmailNotificationDetailsEntity.getSubject(), is(ncesEmailNotificationDetails.getSubject()));
        assertThat(ncesEmailNotificationDetailsEntity.getSendTo(), is(ncesEmailNotificationDetails.getSendTo()));
    }

    @Test
    public void shouldRemoveNcesEmailNotificationDetails() {
        assertThat(ncesEmailNotificationDetailsRepository.findAll(), hasSize(1));

        ncesEmailNotificationDetailsRepository.remove(ncesEmailNotificationDetails);

        assertThat(ncesEmailNotificationDetailsRepository.findAll(), hasSize(0));
    }

    @Test
    public void shouldRemoveAManagedNcesEmailNotificationDetails() {
        // save() returns the merged, managed instance, so remove() takes the contains==true branch
        final NcesEmailNotificationDetailsEntity managed =
                ncesEmailNotificationDetailsRepository.save(createNcesEmailNotificationDetailsEntity());

        ncesEmailNotificationDetailsRepository.remove(managed);

        // the entity saved in @BeforeEach remains
        assertThat(ncesEmailNotificationDetailsRepository.findAll(), hasSize(1));
    }

    private NcesEmailNotificationDetailsEntity createNcesEmailNotificationDetailsEntity() {
        final NcesEmailNotificationDetailsEntity ncesEmailNotificationDetailsEntity = new NcesEmailNotificationDetailsEntity();
        ncesEmailNotificationDetailsEntity.setId(randomUUID());
        ncesEmailNotificationDetailsEntity.setMasterDefendantId(randomUUID());
        ncesEmailNotificationDetailsEntity.setMaterialId(randomUUID());
        ncesEmailNotificationDetailsEntity.setNotificationId(randomUUID());
        ncesEmailNotificationDetailsEntity.setSubject("subject");
        ncesEmailNotificationDetailsEntity.setSendTo("mail@email.com");
        return ncesEmailNotificationDetailsEntity;
    }
}
