package uk.gov.moj.cpp.results.event.helper;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.AllResultDefinitions;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceCacheTest {
    private static final UUID NATIONALITY_ID = randomUUID();

    @Mock
    ReferenceDataService referenceDataService;

    @InjectMocks
    ReferenceCache referenceCache;

    @Test
    public void getNationalityByIsoCode() throws Exception {
        final JsonObject nationality = createObjectBuilder()
                .add("isoCode", "foo")
                .add("id", NATIONALITY_ID.toString())
                .build();
        final JsonObject responsePayload = createObjectBuilder().add("countryNationality", createArrayBuilder()
                .add(nationality)
                .build()).build();

        when(referenceDataService.getAllNationality(anyObject())).thenReturn(responsePayload);

        final Optional<JsonObject> nationalityResult = referenceCache.getNationalityById(NATIONALITY_ID);

        assertThat(nationalityResult, is(of(nationality)));
    }

    @Test
    public void getEmptyNationalityByIsoCode() throws Exception {
        when(referenceDataService.getAllNationality(anyObject())).thenReturn(createObjectBuilder().build());

        final Optional<JsonObject> nationalityResult = referenceCache.getNationalityById(NATIONALITY_ID);

        assertThat(nationalityResult, is(empty()));
    }

    @Test
    public void getResultDefinitionById_shouldReturnValue() {
        final AllResultDefinitions allResultDefinitions = new AllResultDefinitions()
                .setResultDefinitions(Collections.singletonList(
                        ResultDefinition.resultDefinition()
                                .setId(randomUUID())
                ));

        final LocalDate referenceDate = LocalDate.now();

        when(referenceDataService.loadAllResultDefinitions(null, referenceDate)).thenReturn(allResultDefinitions);

        final ResultDefinition results = referenceCache.getResultDefinitionById(null, referenceDate, allResultDefinitions.getResultDefinitions().get(0).getId());

        assertThat(results, is(allResultDefinitions.getResultDefinitions().get(0)));
    }

    @Test
    public void getResultDefinitionById_whenResultIdIsInvalid_thenReturnNull() {
        final AllResultDefinitions allResultDefinitions = new AllResultDefinitions();

        final LocalDate referenceDate = LocalDate.now();

        when(referenceDataService.loadAllResultDefinitions(null, referenceDate)).thenReturn(allResultDefinitions);

        final ResultDefinition results = referenceCache.getResultDefinitionById(null, referenceDate, randomUUID());

        assertThat(results, nullValue());
    }

    @Test
    public void getBailStatusObjectByCode() {

        final List<BailStatus> bailStatuses = new ArrayList<>();
        bailStatuses.add(BailStatus.bailStatus().withCode("A").withDescription("Not applicable").withId(randomUUID()).build());
        bailStatuses.add(BailStatus.bailStatus().withCode("B").withDescription("Conditional Bail").withId(randomUUID()).build());
        bailStatuses.add(BailStatus.bailStatus().withCode("C").withDescription("Custody or remanded into custody").withId(randomUUID()).build());

        when(referenceDataService.getAllBailStatuses(null)).thenReturn(bailStatuses);

        final Optional<BailStatus> bailStatusOptional = referenceCache.getBailStatusObjectByCode(null, "A");

        assertThat(bailStatusOptional.isPresent(), is(true));
        final BailStatus bailStatus = bailStatusOptional.get();
        assertThat(bailStatus.getId(), is(bailStatuses.get(0).getId()));
        assertThat(bailStatus.getCode(), is(bailStatuses.get(0).getCode()));
        assertThat(bailStatus.getDescription(), is(bailStatuses.get(0).getDescription()));

    }

    @Test
    public void getBailStatusObjectByCodeNotFound() {

        final List<BailStatus> bailStatuses = new ArrayList<>();

        when(referenceDataService.getAllBailStatuses(null)).thenReturn(bailStatuses);

        final Optional<BailStatus> bailStatusOptional = referenceCache.getBailStatusObjectByCode(null, "A");

        assertThat(bailStatusOptional.isPresent(), is(false));
    }
}

