package uk.gov.moj.cpp.results.persist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import java.util.Arrays;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.results.persist.entity.VariantDirectory;

@SuppressWarnings("CdiInjectionPointsInspection")
@RunWith(CdiTestRunner.class)
public class VariantDirectoryRepositoryTest extends BaseTransactionalTest {

    private static final UUID hearingId = UUID.randomUUID();
    private static final UUID materialId = UUID.randomUUID();
    
    @Inject
    private VariantDirectoryRepository variantDirectoryRepository;

    @Test
    public void shouldPersistVariantDirectoryData() {
        MatcherAssert.assertThat(this.variantDirectoryRepository.saveAndFlush(buildVariantDirectory()), notNullValue(VariantDirectory.class));
    }

    @Test
    public void shouldUpdateMaterialStatus() {
        assertThat(this.variantDirectoryRepository.saveAndFlush(buildVariantDirectory()).getStatus(), is("BUILDING"));
        assertThat(this.variantDirectoryRepository.updateStatus(materialId, "GENERATED"), is(1));
    }

    private static VariantDirectory buildVariantDirectory() {
        return new VariantDirectory(UUID.randomUUID(), hearingId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Arrays.asList(STRING.next()), materialId, STRING.next(), STRING.next(), "BUILDING");
    }
}