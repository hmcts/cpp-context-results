package uk.gov.moj.cpp.results.domain.transformation.judicialresult.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.results.domain.transformation.judicialresult.domain.EventToTransform.DEFENDANT_ADDED_EVENT;
import static uk.gov.moj.cpp.results.domain.transformation.judicialresult.domain.EventToTransform.DEFENDANT_UPDATED_EVENT;
import static uk.gov.moj.cpp.results.domain.transformation.judicialresult.domain.EventToTransform.HEARING_RESULTS_ADDED;
import static uk.gov.moj.cpp.results.domain.transformation.judicialresult.domain.EventToTransform.POLICE_RESULT_GENERATED;
import static uk.gov.moj.cpp.results.domain.transformation.judicialresult.domain.EventToTransform.isEventToTransform;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class EventToTransformTest {

    @DataProvider
    public static Object[][] validEventToMatch() {
        return new Object[][]{
                {POLICE_RESULT_GENERATED.getEventName()},
                {HEARING_RESULTS_ADDED.getEventName()},
                {DEFENDANT_ADDED_EVENT.getEventName()},
                {DEFENDANT_UPDATED_EVENT.getEventName()}
        };
    }

    @Test
    @UseDataProvider("validEventToMatch")
    public void shouldReturnTrueIfEventNameIsAMatch(final String eventName) {
        assertThat(isEventToTransform(eventName), is(true));
    }

    @Test
    public void shouldReturnFalseIfEventNameIsNotAMatch() {
        assertThat(isEventToTransform(STRING.next()), is(false));
    }
}