package uk.gov.moj.cpp.results.persist.entity;

import org.junit.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

public class ResultPromptTest {

    private final static ResultPrompt RESULT_PROMPT = ResultPrompt.builder()
            .withId(randomUUID())
            .withHearingResultId(randomUUID())
            .withLabel(STRING.next())
            .withValue(STRING.next())
            .build();

    @Test
    public void shouldHaveANoArgsConstructor() {
        assertThat(ResultPrompt.class, hasValidBeanConstructor());
    }

    @Test
    public void noArgsConstructorShouldInstantiateAnObject() {
        assertThat(new ResultPrompt(), not(nullValue()));
    }

    @Test
    public void shouldBeAbleToOverwriteFieldsFromBuilder(){
        ResultPrompt data = ResultPrompt.builder()
                .withId(randomUUID())
                .withHearingResultId(randomUUID())
                .withLabel(STRING.next())
                .withValue(STRING.next())
                .build();

        ResultPrompt subject = ResultPrompt.of(RESULT_PROMPT)
                .withId(data.getId())
                .withHearingResultId(data.getHearingResultId())
                .withLabel(data.getLabel())
                .withValue(data.getValue())
                .build();

        assertThat(subject.getHearingResultId(), is(data.getHearingResultId()));
        assertThat(subject.getLabel(), is(data.getLabel()));
        assertThat(subject.getValue(), is(data.getValue()));
    }
}