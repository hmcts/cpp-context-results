package uk.gov.moj.cpp.results.domain.common;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.external.ApiDefendant;
import uk.gov.justice.core.courts.external.ApiHearing;
import uk.gov.justice.core.courts.external.ApiJudicialResult;
import uk.gov.justice.core.courts.external.ApiOffence;
import uk.gov.justice.core.courts.external.JurisdictionType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.domains.HearingTransformer;

import java.nio.charset.Charset;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class HearingTransformerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingHelperTest.class.getName());

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private HearingTransformer hearingTransformer = new HearingTransformer();

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldTransformHearingWithOutVehicleCode() {
        final JsonObject hearingJson = getHearingJson("hearing.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        final ApiHearing apiHearing = hearingTransformer.hearing(hearing).build();
        final ApiDefendant apiDefendant = apiHearing.getProsecutionCases().get(0).getDefendants().get(0);

        assertThat(apiHearing.getDefendantJudicialResults().size(), is(1));
        assertThat(apiDefendant.getJudicialResults().size(), is(2));
        assertThat(apiDefendant.getOffences().get(0).getJudicialResults().size(), is(2));
        assertThat(apiHearing.getCourtApplications().get(0).getJudicialResults().size(), is(2));
        assertThat(apiHearing.getCourtCentre().getCode(), is("1234"));
        final ApiOffence apiOffence = apiDefendant.getOffences().get(0);
        assertThat(apiOffence.getOffenceFacts().getVehicleCode(), nullValue());
        final ApiJudicialResult apiJudicialResult = apiOffence.getJudicialResults().get(0);
        assertThat(apiJudicialResult.getNextHearing().getHearingLanguage().name(), is(HearingLanguage.ENGLISH.name()));
        assertThat(apiHearing.getJurisdictionType().name(), is(JurisdictionType.MAGISTRATES.name()));
    }

    @Test
    public void shouldTransformHearingWithVehicleCode() {
        final JsonObject hearingJson = getHearingJson("hearingWithVehicleCode.json");
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        final ApiHearing apiHearing = hearingTransformer.hearing(hearing).build();
        final ApiDefendant apiDefendant = apiHearing.getProsecutionCases().get(0).getDefendants().get(0);

        assertThat(apiHearing.getDefendantJudicialResults(), hasSize(1));
        assertThat(apiDefendant.getJudicialResults(), hasSize(2));
        assertThat(apiDefendant.getOffences().get(0).getJudicialResults(), hasSize(2));
        assertThat(apiHearing.getCourtApplications().get(0).getJudicialResults(), hasSize(2));
        assertThat(apiHearing.getCourtCentre().getCode(), is("1234"));
        final ApiOffence apiOffence = apiDefendant.getOffences().get(0);
        assertThat(apiOffence.getOffenceFacts().getVehicleCode().toString(), is("PASSENGER_CARRYING_VEHICLE"));
        final ApiJudicialResult apiJudicialResult = apiOffence.getJudicialResults().get(0);
        assertThat(apiJudicialResult.getNextHearing().getHearingLanguage(), nullValue());
        assertThat(apiHearing.getJurisdictionType(), nullValue());
    }


    private static JsonObject getHearingJson(final String resourceName) {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(resourceName),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            LOGGER.info("error {}", e.getMessage());
        }

        return new StringToJsonObjectConverter().convert(response);
    }
}
